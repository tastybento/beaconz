package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;
import org.bukkit.scoreboard.Team;

/**
 * Overlays all beacons, links, and triangles onto a map. Overlapping triangles make progressively darker colors.
 */
public class TerritoryMapRenderer extends MapRenderer {

    private static final String MAP_UNCLAIMED_PERMISSION = "beaconz.map.unclaimed";

    // cache for getMapPaletteColorForTeam
    private static Byte[][] mapPaletteColors = new Byte[16][];

    private static final int TICKS_PER_REFRESH = 20; // update map once per second

    private final Beaconz beaconz;
    private int tick = 0;

    // todo: cache needs to be reset whenever the scale of a map changes
    private Map<Point2D, CachedBeacon> beaconRegisterCache; // to detect if beacons have changed since last render (re-computing triangle fields is not cheap)
    private Byte[][] pixelCache = new Byte[128][]; // last render

    public TerritoryMapRenderer(Beaconz beaconz) {
        this.beaconz = beaconz;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        tick++;
        if (tick > TICKS_PER_REFRESH) tick = 0;
        if (tick != 0) return;
        Map<Point2D, CachedBeacon> newCache = makeBeaconCache();
        MapCoordinateConverter coordConverter = new MapCoordinateConverter(map);
        if (beaconRegisterCache != null && beaconRegisterCache.equals(newCache)) {
            // Beacons haven't changed since we last rendered, so we'll just use the same pixels as last time.
            renderFromPixelCache(canvas);
            if (canvas.getCursors().size() == 0) {
                setCursors(coordConverter, canvas.getCursors(), player.hasPermission(MAP_UNCLAIMED_PERMISSION));
            }
        } else {
            beaconRegisterCache = newCache;
            pixelCache = new Byte[128][];
            renderToPixelCache(coordConverter);
            renderFromPixelCache(canvas);
            setCursors(coordConverter, canvas.getCursors(), player.hasPermission(MAP_UNCLAIMED_PERMISSION));
        }
    }

    private void renderToPixelCache(MapCoordinateConverter coordConverter) {
        // triangles
        int count = 0;
        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                count ++;
                if (count % 3 == 0) {
                    int xBlock = coordConverter.pixelXToBlockX((byte) x);
                    int zBlock = coordConverter.pixelZToBlockZ((byte) z);
                    List<TriangleField> triangles = beaconz.getRegister().getTriangle(xBlock, zBlock);
                    if (triangles != null && !triangles.isEmpty()) {
                        TriangleField triangleField = triangles.get(0);
                        Scorecard scoreCard = beaconz.getGameMgr().getSC(x,z);
                        if (scoreCard != null) {
                            MaterialData materialData = scoreCard.getBlockID(triangleField.getOwner());
                            if (materialData != null) {
                                byte color = getMapPaletteColorForTeam(materialData.getData(), triangles.size());
                                if (pixelCache[x] == null) pixelCache[x] = new Byte[128];
                                pixelCache[x][z] = color;
                            }
                        }
                    }
                }
            }
        }
        // lines
        for (Map.Entry<Point2D, CachedBeacon> entry : beaconRegisterCache.entrySet()) {
            CachedBeacon value = entry.getValue();
            Team owner = value.owner;
            if (owner == null || value.links == null || value.links.isEmpty()) continue;
            Scorecard scoreCard = beaconz.getGameMgr().getSC(entry.getKey());
            if (scoreCard != null) {
                MaterialData blockID = scoreCard.getBlockID(owner);
                if (blockID != null) {
                    byte data = blockID.getData();
                    byte color = getMapPaletteColorForTeam(data, 1);
                    for (BeaconObj link : value.links) {
                        renderLineToPixelCache(color, coordConverter, entry.getKey(), link.getLocation());
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setCursors(MapCoordinateConverter coordConverter, MapCursorCollection cursors, boolean showUnclaimedBeacons) {
        for (int i = 0; i < cursors.size(); i++) {
            cursors.removeCursor(cursors.getCursor(i));
        }
        for (Map.Entry<Point2D, CachedBeacon> entry : beaconRegisterCache.entrySet()) {
            Team team = entry.getValue().owner;
            if (!showUnclaimedBeacons && team == null) continue;
            Point2D point = entry.getKey();
            int x = coordConverter.blockXToPixelX((int) point.getX());
            x = x * 2 - 128; // Pixels range from 0 to 127, but cursors range from -128 to 127. (wtf)
            if (x < -128 || x >= 128) continue;
            int z = coordConverter.blockZToPixelZ((int) point.getY());
            z = z * 2 - 128;
            if (z < -128 || z >= 128) continue;
            int color = 16;
            if (team != null) {
                Scorecard sc = beaconz.getGameMgr().getSC(point);
                if (sc != null) {
                    color = sc.getBlockID(team).getData();	
                }
            }
            cursors.addCursor(x, z, (byte)0);
            MapCursor cursor = cursors.getCursor(cursors.size() - 1);
            TeamCursor teamCursor = TEAM_CURSORS[color];
            cursor.setDirection(teamCursor.direction);
            cursor.setType(teamCursor.type);
        }
    }

    private void renderLineToPixelCache(byte color, MapCoordinateConverter coordConverter, Point2D start, Point2D finish) {
        int startX = coordConverter.blockXToPixelX((int) start.getX());
        int startZ = coordConverter.blockZToPixelZ((int) start.getY());
        int finishX = coordConverter.blockXToPixelX((int) finish.getX());
        int finishZ = coordConverter.blockZToPixelZ((int) finish.getY());
        int diffX = finishX - startX;
        int diffZ = finishZ - startZ;
        double step = 1 / Math.sqrt(diffX * diffX + diffZ * diffZ);
        for (double progress = 0; progress <= 1.0; progress += step) {
            int x = (int)(startX + diffX * progress);
            if (x < 0 || x >= 128) continue;
            int z = (int)(startZ + diffZ * progress);
            if (z < 0 || z >= 128) continue;
            if (pixelCache[x] == null) pixelCache[x] = new Byte[128];
            pixelCache[x][z] = color;
        }
    }

    private Map<Point2D, CachedBeacon> makeBeaconCache() {
        HashMap<Point2D, BeaconObj> current = beaconz.getRegister().getBeaconRegister();
        Map<Point2D, CachedBeacon> result = new HashMap<>(current.size());
        for (Map.Entry<Point2D, BeaconObj> entry : current.entrySet()) {
            result.put(entry.getKey(), new CachedBeacon(entry.getValue()));
        }
        return result;
    }

    private void renderFromPixelCache(MapCanvas canvas) {
        // todo: figure out whether calls to getBasePixel are actually necessary
        for (int x = 0; x < 128; x++) {
            if (pixelCache[x] == null) {
                for (int z = 0; z < 128; z++) {
                    canvas.setPixel(x, z, canvas.getBasePixel(x, z));
                }
            } else {
                for (int z = 0; z < 128; z++) {
                    if (pixelCache[x][z] == null) {
                        canvas.setPixel(x, z, canvas.getBasePixel(x, z));
                    } else {
                        canvas.setPixel(x, z, pixelCache[x][z]);
                    }
                }
            }
        }
    }

    /**
     * Returns the color of the map pixel to use for a world location owned by the team with the given glassColor.
     *
     * @param glassColor        a number between 0 and 15, see config.yml for complete list
     * @param numberOfTriangles how many triangles are overlapping at the location of the pixel. More triangles make
     *                          darker colors. e.g. for the red team, one triangle is bright red, two triangles is
     *                          a slightly darker red, etc. all the way to black.
     * @return color of pixel (as an index of MapPalette.colors)
     */
    private static byte getMapPaletteColorForTeam(byte glassColor, int numberOfTriangles) {
        numberOfTriangles--;
        if (mapPaletteColors[glassColor] == null) {
            DyeColor dyeColor = DyeColor.getByData(glassColor);
            Color color = dyeColor.getColor();
            List<Byte> colors = new ArrayList<>();
            byte previous = MapPalette.matchColor(0, 0, 0);
            for (double m = 1.0; m >= 0.0; m -= (1.0 / 256)) {
                byte b = MapPalette.matchColor((int) (m * color.getRed()), (int) (m * color.getGreen()), (int) (m * color.getBlue()));
                if (b != previous && !MapPalette.getColor(b).equals(MapPalette.getColor(previous))) {
                    colors.add(b);
                    previous = b;
                }
            }
            mapPaletteColors[glassColor] = colors.toArray(new Byte[colors.size()]);
        }
        Byte[] colors = mapPaletteColors[glassColor];
        if (numberOfTriangles >= colors.length) numberOfTriangles = colors.length - 1;
        return colors[numberOfTriangles];
    }

    private static class CachedBeacon {
        private Team owner;
        private Set<BeaconObj> links;

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

            return links.equals(that.links) && !(owner != null ? !owner.equals(that.owner) : that.owner != null);

        }

        @Override
        public int hashCode() {
            int result = owner != null ? owner.hashCode() : 0;
            result = 31 * result + links.hashCode();
            return result;
        }
    }

    private static class TeamCursor {
        public MapCursor.Type type;
        public byte direction; // 0 to 15

        public TeamCursor(MapCursor.Type type, int direction) {
            this.type = type;
            this.direction = (byte)direction;
        }
    }

    private static final TeamCursor[] TEAM_CURSORS = new TeamCursor[] {
        new TeamCursor(MapCursor.Type.WHITE_POINTER, 0), // white team = white pointer down
        new TeamCursor(MapCursor.Type.RED_POINTER, 0), // orange team = red pointer down
        new TeamCursor(MapCursor.Type.BLUE_POINTER, 0), // magenta team = blue pointer left
        new TeamCursor(MapCursor.Type.BLUE_POINTER, 4), // light blue team = blue pointer left
        new TeamCursor(MapCursor.Type.WHITE_POINTER, 4), // yellow team = white pointer left
        new TeamCursor(MapCursor.Type.GREEN_POINTER, 0), // lime team = green pointer down
        new TeamCursor(MapCursor.Type.RED_POINTER, 4), // pink team = red pointer left
        new TeamCursor(MapCursor.Type.WHITE_POINTER, 8), // gray team = white pointer up
        new TeamCursor(MapCursor.Type.WHITE_POINTER, 12), // light gray team = white pointer right
        new TeamCursor(MapCursor.Type.BLUE_POINTER, 8), // cyan team = blue pointer up
        new TeamCursor(MapCursor.Type.RED_POINTER, 8), // purple team = red pointer up
        new TeamCursor(MapCursor.Type.BLUE_POINTER, 12), // blue team = blue pointer right
        new TeamCursor(MapCursor.Type.GREEN_POINTER, 4), // brown team = green pointer left
        new TeamCursor(MapCursor.Type.GREEN_POINTER, 8), // green team = green pointer up
        new TeamCursor(MapCursor.Type.RED_POINTER, 12), // red team = red pointer right
        new TeamCursor(MapCursor.Type.GREEN_POINTER, 12), // black team = green pointer right
        new TeamCursor(MapCursor.Type.WHITE_CROSS, 0) // unclaimed beacons are crosses
    };
}
