package com.wasteofplastic.beaconz.map;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Scorecard;
import com.wasteofplastic.beaconz.TriangleField;

/**
 * Overlays all beacons, links, and triangles onto a map. Overlapping triangles make progressively darker colors.
 * This renderer is responsible for drawing:
 * - Territory triangles with team colors (darker when overlapping)
 * - Link lines between connected beacons
 * - Beacon location cursors
 * - Player position and direction
 */
public class TerritoryMapRenderer extends MapRenderer {

    /** Permission required to see unclaimed beacons on the map */
    private static final String MAP_UNCLAIMED_PERMISSION = "beaconz.map.unclaimed";

    /**
     * Cache for color gradients - stores an array of color bytes for each material type.
     * Each material gets a gradient from bright to dark for showing overlapping triangles.
     * Key: Team material (e.g., RED_WOOL), Value: Array of color bytes from bright to dark
     */
    private static final Map<Material, byte[]> mapPaletteColors = new EnumMap<>(Material.class);

    /** Number of game ticks between map refreshes (20 ticks = 1 second) */
    private static final int TICKS_PER_REFRESH = 5;

    /** Reference to the main plugin instance */
    private final Beaconz beaconz;

    /** Current tick counter for refresh timing */
    private int tick = 0;

    /**
     * Cache of beacon state from the last render.
     * Used to detect if beacons have changed since last render (re-computing triangle fields is expensive).
     * TODO: This cache needs to be reset whenever the scale of a map changes.
     */
    private Map<Point2D, CachedBeacon> beaconRegisterCache;

    /**
     * Pixel cache storing the last rendered state of the map (128x128 pixel grid).
     * Each pixel stores a map palette color byte or null if not set.
     */
    private Byte[][] pixelCache = new Byte[128][];

    /**
     * Constructs a new territory map renderer
     * @param beaconz the main plugin instance
     */
    public TerritoryMapRenderer(Beaconz beaconz) {
        this.beaconz = beaconz;
    }

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        // Safety checks - ensure map and world are valid
        if (map == null) {
            return;
        }
        if (map.getWorld() == null) {
            return;
        }

        // Only render maps in the Beaconz game world
        if (!map.getWorld().equals(beaconz.getBeaconzWorld())) {
            return;
        }

        // Only render if the player is actually holding a map in either hand
        ItemStack inMainHand = player.getInventory().getItemInMainHand();
        ItemStack inOffHand = player.getInventory().getItemInOffHand();
        if (inMainHand.getType().equals(Material.FILLED_MAP) || inOffHand.getType().equals(Material.FILLED_MAP)) {
            // Throttle rendering to once per second (20 ticks)
            tick++;
            if (tick > TICKS_PER_REFRESH) tick = 0;
            if (tick != 0) return; // Skip rendering this tick

            // Create a snapshot of the current beacon state
            Map<Point2D, CachedBeacon> newCache = makeBeaconCache();
            MapCoordinateConverter coordConverter = new MapCoordinateConverter(map);

            // Check if beacons have changed since last render
            if (beaconRegisterCache != null && beaconRegisterCache.equals(newCache)) {
                // Beacons haven't changed - reuse cached pixels for performance
                renderFromPixelCache(canvas);
            } else {
                // Beacons have changed - recompute the entire map
                beaconRegisterCache = newCache;
                pixelCache = new Byte[128][]; // Clear old cache
                renderToPixelCache(coordConverter); // Render triangles and links to cache
                renderFromPixelCache(canvas); // Apply cache to canvas
            }

            // Add beacon location markers to the map
            setCursors(canvas, coordConverter, canvas.getCursors(), player.hasPermission(MAP_UNCLAIMED_PERMISSION));

            // Add player position cursor
            int x = coordConverter.blockXToPixelX(player.getLocation().getBlockX());
            if (x < 0 || x > 127) return; // Player off map
            int z = coordConverter.blockZToPixelZ(player.getLocation().getBlockZ());
            if (z < 0 || z > 127) return; // Player off map

            // Convert from pixel coordinates (0-127) to cursor coordinates (-128 to 127)
            x = x * 2 - 128;
            z = z * 2 - 128;

            // Add the player cursor with their current facing direction
            byte dir = direction(player);
            canvas.getCursors().addCursor(x, z, dir);
        }
    }

    /**
     * Calculates the map cursor direction byte based on the player's yaw rotation.
     * Divides the 360-degree circle into 8 sectors (45 degrees each) and returns
     * the appropriate direction value for the map cursor.
     *
     * @param player the player whose direction to calculate
     * @return direction byte (0x0, 0x2, 0x4, 0x6, 0x8, 0xA, 0xC, 0xE)
     */
    private byte direction(Player player){
        // Get player's yaw and normalize to 0-360 degrees
        // Adding 90 adjusts for Minecraft's coordinate system
        double rotation = (player.getLocation().getYaw() + 90) % 360;
        if (rotation < 0) {
            rotation += 360.0;
        }

        // Map rotation to one of 8 directional cursors
        // Each sector is 45 degrees (360 / 8 = 45)
        if (0 <= rotation && rotation < 22.5) {
            return 0xC; // South to East
        } else if (22.5 <= rotation && rotation < 67.5) {
            return 0xE; // Southwest to Southeast
        } else if (67.5 <= rotation && rotation < 112.5) {
            return 0x0; // West to East
        } else if (112.5 <= rotation && rotation < 157.5) {
            return 0x2; // Northwest to Southwest
        } else if (157.5 <= rotation && rotation < 202.5) {
            return 0x4; // North to West
        } else if (202.5 <= rotation && rotation < 247.5) {
            return 0x6; // Northeast to Northwest
        } else if (247.5 <= rotation && rotation < 292.5) {
            return 0x8; // East to North
        } else if (292.5 <= rotation && rotation < 337.5) {
            return 0xA; // Southeast to Northeast
        } else { // 337.5 <= rotation < 360.0
            return 0xC; // South to East
        }
    }
    /**
     * Renders triangles and links to the pixel cache.
     * This method performs the expensive computation of determining what color each pixel should be.
     * Results are cached to avoid recomputing when beacons haven't changed.
     *
     * @param coordConverter converter for translating between world blocks and map pixels
     */
    private void renderToPixelCache(MapCoordinateConverter coordConverter) {
        // PHASE 1: Render territory triangles
        int count = 0;
        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                // Convert pixel coordinates to world block coordinates
                int xBlock = coordConverter.pixelXToBlockX((byte) x);
                int zBlock = coordConverter.pixelZToBlockZ((byte) z);
                count++;

                // Sample every 3rd pixel for performance (triangles are large enough that this works)
                if (count % 3 == 0) {
                    // Check if this block is within any team's territory triangle
                    List<TriangleField> triangles = beaconz.getRegister().getTriangle(xBlock, zBlock);
                    if (triangles != null && !triangles.isEmpty()) {
                        // Get the primary (first) triangle for this location
                        TriangleField triangleField = triangles.getFirst();
                        Scorecard scoreCard = beaconz.getGameMgr().getSC(xBlock, zBlock);
                        if (scoreCard != null) {
                            // Get the team's material (wool/concrete color)
                            Material material = scoreCard.getBlockID(triangleField.getOwner());
                            if (material != null) {
                                // Get color with darkness based on how many triangles overlap here
                                // More overlapping triangles = darker color
                                byte color = getMapPaletteColorForTeam(material, triangles.size());
                                if (pixelCache[x] == null) pixelCache[x] = new Byte[128];
                                pixelCache[x][z] = color;
                            }
                        }
                    }
                }

                // Mark game boundaries (areas outside any active game)
                Game game = beaconz.getGameMgr().getGame(xBlock, zBlock);
                if (game == null) {
                    // Outside game area - set to black (color index 0)
                    if (pixelCache[x] == null) pixelCache[x] = new Byte[128];
                    pixelCache[x][z] = (byte)0;
                }
            }
        }

        // PHASE 2: Render link lines between connected beacons
        for (Map.Entry<Point2D, CachedBeacon> entry : beaconRegisterCache.entrySet()) {
            CachedBeacon value = entry.getValue();
            Team owner = value.owner;

            // Skip unowned beacons and beacons with no links
            if (owner == null || value.links.isEmpty()) continue;

            Scorecard scoreCard = beaconz.getGameMgr().getSC(entry.getKey());
            if (scoreCard != null) {
                Material blockID = scoreCard.getBlockID(owner);
                if (blockID != null) {
                    // Get the team color (brightness level 1 for clean link lines)
                    byte color = getMapPaletteColorForTeam(blockID, 1);

                    // Draw a line from this beacon to each linked beacon
                    for (BeaconObj link : value.links) {
                        renderLineToPixelCache(color, coordConverter, entry.getKey(), link.getPoint());
                    }
                }
            }
        }
    }

    /**
     * Places the beacon cursors on the map to mark beacon locations.
     * Each beacon gets a colored banner cursor based on its team ownership.
     *
     * @param canvas the map canvas to draw on
     * @param coordConverter coordinate converter for the map
     * @param cursors map cursor collection to add cursors to
     * @param showUnclaimedBeacons whether to show unclaimed beacons (requires permission)
     */
    private void setCursors(MapCanvas canvas, MapCoordinateConverter coordConverter, MapCursorCollection cursors, boolean showUnclaimedBeacons) {
        // Clear existing cursors (except player cursor which is added later)
        for (int i = 0; i < cursors.size(); i++) {
            cursors.removeCursor(cursors.getCursor(i));
        }

        // Add a cursor for each beacon
        for (Map.Entry<Point2D, CachedBeacon> entry : beaconRegisterCache.entrySet()) {
            Team team = entry.getValue().owner;

            // Skip unclaimed beacons if player doesn't have permission to see them
            if (!showUnclaimedBeacons && team == null) continue;

            Point2D point = entry.getKey();

            // Convert beacon world position to map pixel coordinates
            int x = coordConverter.blockXToPixelX((int) point.getX());
            if (x < 0 || x > 127) continue; // Beacon off map
            int z = coordConverter.blockZToPixelZ((int) point.getY());
            if (z < 0 || z > 127) continue; // Beacon off map

            // Only show cursors on discovered areas of the map
            if (canvas.getBasePixelColor(x, z) != null) {
                // Convert from pixel coordinates (0-127) to cursor coordinates (-128 to 127)
                x = x * 2 - 128;
                z = z * 2 - 128;

                // Determine cursor color based on team ownership
                int color = 16; // Default to unclaimed (index 16 = red X)
                if (team != null) {
                    Scorecard sc = beaconz.getGameMgr().getSC(point);
                    if (sc != null) {
                        Material material = sc.getBlockID(team);
                        if (material != null) {
                            // Map team material to color index (0-15)
                            color = getTeamColorIndex(material);
                        }
                    }
                }

                // Get the cursor type and direction for this team color
                TeamCursor teamCursor = TEAM_CURSORS[color];

                // Add the cursor to the map
                MapCursor cursor = cursors.addCursor(x, z, (byte)0);
                cursor.setDirection(teamCursor.direction);
                cursor.setType(teamCursor.type);
            }
        }
    }

    /**
     * Renders a link line between two beacons using Bresenham-like algorithm.
     * The line is drawn pixel by pixel in the cache, showing connections between linked beacons.
     *
     * @param color the map palette color byte to use for the line
     * @param coordConverter coordinate converter for the map
     * @param start start point of the line (beacon position)
     * @param finish end point of the line (linked beacon position)
     */
    private void renderLineToPixelCache(byte color, MapCoordinateConverter coordConverter, Point2D start, Point2D finish) {
        // Convert world coordinates to pixel coordinates
        int startX = coordConverter.blockXToPixelX((int) start.getX());
        int startZ = coordConverter.blockZToPixelZ((int) start.getY());
        int finishX = coordConverter.blockXToPixelX((int) finish.getX());
        int finishZ = coordConverter.blockZToPixelZ((int) finish.getY());

        // Calculate the line vector
        int diffX = finishX - startX;
        int diffZ = finishZ - startZ;

        // Calculate step size based on line length (ensures smooth line without gaps)
        double step = 1 / Math.sqrt(diffX * diffX + diffZ * diffZ);

        // Draw the line by interpolating from start to finish
        for (double progress = 0; progress <= 1.0; progress += step) {
            // Calculate current pixel position along the line
            int x = (int)(startX + diffX * progress);
            if (x < 0 || x >= 128) continue; // Skip if off map
            int z = (int)(startZ + diffZ * progress);
            if (z < 0 || z >= 128) continue; // Skip if off map

            // Set the pixel color in the cache
            if (pixelCache[x] == null) pixelCache[x] = new Byte[128];
            pixelCache[x][z] = color;
        }
    }

    /**
     * Creates a snapshot of the current beacon state for change detection.
     * By comparing this cache with the previous one, we can avoid expensive recomputation
     * when beacons haven't changed.
     *
     * @return a map of beacon positions to cached beacon data (owner and links)
     */
    private Map<Point2D, CachedBeacon> makeBeaconCache() {
        // Get all current beacons from the register
        HashMap<Point2D, BeaconObj> current = beaconz.getRegister().getBeaconRegister();
        Map<Point2D, CachedBeacon> result = new HashMap<>(current.size());

        // Create a lightweight cache entry for each beacon
        for (Map.Entry<Point2D, BeaconObj> entry : current.entrySet()) {
            result.put(entry.getKey(), new CachedBeacon(entry.getValue()));
        }
        return result;
    }

    /**
     * Sets the pixels on the map canvas from the cache.
     * This is a fast operation that applies pre-computed colors to the map.
     * Only updates pixels that are on discovered (non-black) portions of the map.
     *
     * @param canvas the map canvas to draw on
     */
    @SuppressWarnings({"removal"})
    private void renderFromPixelCache(MapCanvas canvas) {
        // Iterate through all 128x128 pixels
        for (int x = 0; x < 128; x++) {
            if (pixelCache[x] != null) {
                for (int z = 0; z < 128; z++) {
                    if (pixelCache[x][z] != null) {
                        // Only draw on discovered areas of the map
                        java.awt.Color baseColor = canvas.getBasePixelColor(x, z);
                        if (baseColor != null) {
                            // Convert palette index to Color and set the pixel
                            // Note: MapPalette methods are deprecated but still functional
                            canvas.setPixelColor(x, z, MapPalette.getColor(pixelCache[x][z]));
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the color of the map pixel to use for a world location owned by the team with the given material.
     * Creates a gradient of colors from bright to dark based on the number of overlapping triangles.
     *
     * @param material        the team's material (e.g., WHITE_WOOL, RED_WOOL, etc.)
     * @param numberOfTriangles how many triangles are overlapping at the location of the pixel. More triangles make
     *                          darker colors. e.g. for the red team, one triangle is bright red, two triangles is
     *                          a slightly darker red, etc. all the way to black.
     * @return color of pixel (as an index of MapPalette.colors)
     */
    @SuppressWarnings({"removal"})
    private static byte getMapPaletteColorForTeam(Material material, int numberOfTriangles) {
        // Adjust triangle count (subtract 1 because arrays are 0-indexed)
        numberOfTriangles--;
        if (numberOfTriangles < 0) numberOfTriangles = 0;

        // Check if we've already generated the color gradient for this material
        if (!mapPaletteColors.containsKey(material)) {
            // Get the base color for this team's material
            Color baseColor = getMaterialColor(material);
            List<Byte> colors = new ArrayList<>();
            byte previous = MapPalette.matchColor(0, 0, 0);

            // Create a gradient from full brightness (100%) down to black (0%)
            // Iterating in small steps (1/256) to find all distinct palette colors
            for (double m = 1.0; m >= 0.0; m -= (1.0 / 256)) {
                // Scale the base color by multiplier m to make it darker
                byte b = MapPalette.matchColor(
                    (int) (m * baseColor.getRed()),
                    (int) (m * baseColor.getGreen()),
                    (int) (m * baseColor.getBlue())
                );

                // Only add this color if it's different from the previous one
                if (b != previous) {
                    java.awt.Color currentColor = MapPalette.getColor(b);
                    java.awt.Color previousColor = MapPalette.getColor(previous);
                    // Double-check the colors are actually different (not just different indices)
                    if (!currentColor.equals(previousColor)) {
                        colors.add(b);
                        previous = b;
                    }
                }
            }

            // Convert the List<Byte> to a primitive byte array for efficient storage
            byte[] colorArray = new byte[colors.size()];
            for (int i = 0; i < colors.size(); i++) {
                colorArray[i] = colors.get(i);
            }

            // Cache this gradient for future use
            mapPaletteColors.put(material, colorArray);
        }

        // Get the cached gradient for this material
        byte[] colors = mapPaletteColors.get(material);

        // Clamp the triangle count to the available gradient range
        if (numberOfTriangles >= colors.length) numberOfTriangles = colors.length - 1;

        // Return the appropriate shade based on overlap count
        return colors[numberOfTriangles];
    }

    /**
     * Maps a Material to a java.awt.Color for team representation.
     * Supports the 16 standard Minecraft dye colors across multiple block types
     * (wool, concrete, terracotta, and stained glass).
     *
     * @param material the team material (wool, concrete, terracotta, or stained glass)
     * @return the corresponding RGB color for rendering on the map
     */
    private static Color getMaterialColor(Material material) {
        // Map common team materials to their corresponding colors
        // Each case handles all 4 variants (wool, concrete, terracotta, glass) of each color
        return switch (material) {
            case WHITE_WOOL, WHITE_CONCRETE, WHITE_TERRACOTTA, WHITE_STAINED_GLASS -> new Color(255, 255, 255); // Pure white
            case ORANGE_WOOL, ORANGE_CONCRETE, ORANGE_TERRACOTTA, ORANGE_STAINED_GLASS -> new Color(255, 165, 0); // Orange
            case MAGENTA_WOOL, MAGENTA_CONCRETE, MAGENTA_TERRACOTTA, MAGENTA_STAINED_GLASS -> new Color(255, 0, 255); // Magenta/Fuchsia
            case LIGHT_BLUE_WOOL, LIGHT_BLUE_CONCRETE, LIGHT_BLUE_TERRACOTTA, LIGHT_BLUE_STAINED_GLASS -> new Color(0, 255, 255); // Aqua/Cyan
            case YELLOW_WOOL, YELLOW_CONCRETE, YELLOW_TERRACOTTA, YELLOW_STAINED_GLASS -> new Color(255, 255, 0); // Yellow
            case LIME_WOOL, LIME_CONCRETE, LIME_TERRACOTTA, LIME_STAINED_GLASS -> new Color(0, 255, 0); // Lime green
            case PINK_WOOL, PINK_CONCRETE, PINK_TERRACOTTA, PINK_STAINED_GLASS -> new Color(255, 192, 203); // Pink
            case GRAY_WOOL, GRAY_CONCRETE, GRAY_TERRACOTTA, GRAY_STAINED_GLASS -> new Color(128, 128, 128); // Gray
            case LIGHT_GRAY_WOOL, LIGHT_GRAY_CONCRETE, LIGHT_GRAY_TERRACOTTA, LIGHT_GRAY_STAINED_GLASS -> new Color(192, 192, 192); // Light gray/Silver
            case CYAN_WOOL, CYAN_CONCRETE, CYAN_TERRACOTTA, CYAN_STAINED_GLASS -> new Color(0, 139, 139); // Dark cyan
            case PURPLE_WOOL, PURPLE_CONCRETE, PURPLE_TERRACOTTA, PURPLE_STAINED_GLASS -> new Color(128, 0, 128); // Purple
            case BLUE_WOOL, BLUE_CONCRETE, BLUE_TERRACOTTA, BLUE_STAINED_GLASS -> new Color(0, 0, 255); // Blue
            case BROWN_WOOL, BROWN_CONCRETE, BROWN_TERRACOTTA, BROWN_STAINED_GLASS -> new Color(139, 69, 19); // Brown
            case GREEN_WOOL, GREEN_CONCRETE, GREEN_TERRACOTTA, GREEN_STAINED_GLASS -> new Color(0, 128, 0); // Dark green
            case RED_WOOL, RED_CONCRETE, RED_TERRACOTTA, RED_STAINED_GLASS -> new Color(255, 0, 0); // Red
            case BLACK_WOOL, BLACK_CONCRETE, BLACK_TERRACOTTA, BLACK_STAINED_GLASS -> new Color(32, 32, 32); // Near-black (pure black is 0,0,0)
            default -> new Color(255, 255, 255); // Default to white for unknown materials
        };
    }

    /**
     * Maps a Material to a team color index (0-15) for cursor display.
     * The index determines which cursor type and color from TEAM_CURSORS array to use.
     * Index 16 is reserved for unclaimed beacons (red X marker).
     *
     * @param material the team material (wool, concrete, terracotta, or stained glass)
     * @return the team color index (0-15), or 16 for unknown materials
     */
    private static int getTeamColorIndex(Material material) {
        // Map materials to indices that correspond to the TEAM_CURSORS array
        return switch (material) {
            case WHITE_WOOL, WHITE_CONCRETE, WHITE_TERRACOTTA, WHITE_STAINED_GLASS -> 0;  // White team
            case ORANGE_WOOL, ORANGE_CONCRETE, ORANGE_TERRACOTTA, ORANGE_STAINED_GLASS -> 1;  // Orange team
            case MAGENTA_WOOL, MAGENTA_CONCRETE, MAGENTA_TERRACOTTA, MAGENTA_STAINED_GLASS -> 2;  // Magenta team
            case LIGHT_BLUE_WOOL, LIGHT_BLUE_CONCRETE, LIGHT_BLUE_TERRACOTTA, LIGHT_BLUE_STAINED_GLASS -> 3;  // Light blue team
            case YELLOW_WOOL, YELLOW_CONCRETE, YELLOW_TERRACOTTA, YELLOW_STAINED_GLASS -> 4;  // Yellow team
            case LIME_WOOL, LIME_CONCRETE, LIME_TERRACOTTA, LIME_STAINED_GLASS -> 5;  // Lime team
            case PINK_WOOL, PINK_CONCRETE, PINK_TERRACOTTA, PINK_STAINED_GLASS -> 6;  // Pink team
            case GRAY_WOOL, GRAY_CONCRETE, GRAY_TERRACOTTA, GRAY_STAINED_GLASS -> 7;  // Gray team
            case LIGHT_GRAY_WOOL, LIGHT_GRAY_CONCRETE, LIGHT_GRAY_TERRACOTTA, LIGHT_GRAY_STAINED_GLASS -> 8;  // Light gray team
            case CYAN_WOOL, CYAN_CONCRETE, CYAN_TERRACOTTA, CYAN_STAINED_GLASS -> 9;  // Cyan team
            case PURPLE_WOOL, PURPLE_CONCRETE, PURPLE_TERRACOTTA, PURPLE_STAINED_GLASS -> 10;  // Purple team
            case BLUE_WOOL, BLUE_CONCRETE, BLUE_TERRACOTTA, BLUE_STAINED_GLASS -> 11;  // Blue team
            case BROWN_WOOL, BROWN_CONCRETE, BROWN_TERRACOTTA, BROWN_STAINED_GLASS -> 12;  // Brown team
            case GREEN_WOOL, GREEN_CONCRETE, GREEN_TERRACOTTA, GREEN_STAINED_GLASS -> 13;  // Green team
            case RED_WOOL, RED_CONCRETE, RED_TERRACOTTA, RED_STAINED_GLASS -> 14;  // Red team
            case BLACK_WOOL, BLACK_CONCRETE, BLACK_TERRACOTTA, BLACK_STAINED_GLASS -> 15;  // Black team
            default -> 16; // Unknown materials default to unclaimed (red X marker)
        };
    }

    /**
     * Lightweight cache of beacon state for change detection.
     * Stores only the essential properties needed to detect if a beacon has changed:
     * - Team ownership
     * - Set of linked beacons
     *
     * By comparing the current state with a cached state, we can avoid expensive
     * re-rendering when nothing has changed.
     */
    private static class CachedBeacon {
        /** The team that owns this beacon (null if unclaimed) */
        private final Team owner;

        /** Set of beacons that this beacon is linked to */
        private final Set<BeaconObj> links;

        /**
         * Creates a cache snapshot of a beacon's current state
         * @param beaconObj the beacon to cache
         */
        public CachedBeacon(BeaconObj beaconObj) {
            this.owner = beaconObj.getOwnership();
            this.links = new HashSet<>();
            this.links.addAll(beaconObj.getLinks());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CachedBeacon that = (CachedBeacon) o;

            // Two cached beacons are equal if they have the same owner and same links
            return links.equals(that.links) && Objects.equals(owner, that.owner);

        }

        @Override
        public int hashCode() {
            int result = owner != null ? owner.hashCode() : 0;
            result = 31 * result + links.hashCode();
            return result;
        }
    }

    /**
     * Record defining a team's cursor appearance on the map.
     * Combines the cursor type (shape/color) with direction (rotation).
     *
     * @param type the MapCursor type (banner color or target X)
     * @param direction rotation in increments of 22.5° (0-15, where 0 is down, 4 is left, 8 is up, 12 is right)
     */
    private record TeamCursor(MapCursor.Type type, byte direction) {}

    /**
     * Array of cursor configurations for each team color (indices 0-15) plus unclaimed beacons (index 16).
     * Each team gets a colored banner pointing in a specific direction to distinguish them on the map.
     * The directions are varied to make it easier to see overlapping beacons.
     */
    private static final TeamCursor[] TEAM_CURSORS = new TeamCursor[] {
        new TeamCursor(MapCursor.Type.BANNER_WHITE, (byte)0),   // [0] White team = white banner down
        new TeamCursor(MapCursor.Type.BANNER_RED, (byte)0),     // [1] Orange team = red banner down
        new TeamCursor(MapCursor.Type.BANNER_BLUE, (byte)0),    // [2] Magenta team = blue banner down
        new TeamCursor(MapCursor.Type.BANNER_BLUE, (byte)4),    // [3] Light blue team = blue banner left
        new TeamCursor(MapCursor.Type.BANNER_WHITE, (byte)4),   // [4] Yellow team = white banner left
        new TeamCursor(MapCursor.Type.BANNER_GREEN, (byte)0),   // [5] Lime team = green banner down
        new TeamCursor(MapCursor.Type.BANNER_RED, (byte)4),     // [6] Pink team = red banner left
        new TeamCursor(MapCursor.Type.BANNER_WHITE, (byte)8),   // [7] Gray team = white banner up
        new TeamCursor(MapCursor.Type.BANNER_WHITE, (byte)12),  // [8] Light gray team = white banner right
        new TeamCursor(MapCursor.Type.BANNER_BLUE, (byte)8),    // [9] Cyan team = blue banner up
        new TeamCursor(MapCursor.Type.BANNER_RED, (byte)8),     // [10] Purple team = red banner up
        new TeamCursor(MapCursor.Type.BANNER_BLUE, (byte)12),   // [11] Blue team = blue banner right
        new TeamCursor(MapCursor.Type.BANNER_GREEN, (byte)4),   // [12] Brown team = green banner left
        new TeamCursor(MapCursor.Type.BANNER_GREEN, (byte)8),   // [13] Green team = green banner up
        new TeamCursor(MapCursor.Type.BANNER_RED, (byte)12),    // [14] Red team = red banner right
        new TeamCursor(MapCursor.Type.BANNER_GREEN, (byte)12),  // [15] Black team = green banner right
        new TeamCursor(MapCursor.Type.TARGET_X, (byte)0)        // [16] Unclaimed beacons = red X marker
    };
}
