/*
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.map.BeaconMap;
import com.wasteofplastic.beaconz.map.TerritoryMapRenderer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Central registry and database for all game elements in the Beaconz world.
 * <p>
 * This class serves as the primary data store and provides quick lookup capabilities for:
 * <ul>
 *   <li><b>Beacons</b> - Natural diamond block beacons placed in the world</li>
 *   <li><b>Triangle Fields</b> - Territorial control areas formed by linking three beacons</li>
 *   <li><b>Beacon Links</b> - Connections between beacons owned by the same team</li>
 *   <li><b>Base Blocks</b> - Emerald blocks surrounding beacons for expansion and defense</li>
 *   <li><b>Maps</b> - Territory map items that display beacon ownership</li>
 * </ul>
 * <p>
 * <b>Core Responsibilities:</b>
 * <ul>
 *   <li>Persistence - Saves and loads all game data to/from beaconz.yml</li>
 *   <li>Spatial Indexing - Enables fast lookups by coordinate (Point2D) or block location</li>
 *   <li>Link Management - Tracks connections between beacons and validates new links</li>
 *   <li>Triangle Generation - Automatically creates control fields when 3 beacons form a triangle</li>
 *   <li>Collision Detection - Prevents overlapping enemy triangles and intersecting links</li>
 *   <li>Score Calculation - Recalculates team scores when territories change</li>
 * </ul>
 * <p>
 * <b>Data Structures:</b>
 * <ul>
 *   <li><code>beaconRegister</code> - Maps beacon coordinates (Point2D) to BeaconObj instances</li>
 *   <li><code>beaconLinks</code> - Maps Game instances to lists of BeaconLink objects</li>
 *   <li><code>triangleFields</code> - Set of all active TriangleField control areas</li>
 *   <li><code>baseBlocks</code> - Maps emerald block coordinates to their associated beacon</li>
 *   <li><code>baseBlocksInverse</code> - Maps beacons to their sets of base block coordinates</li>
 *   <li><code>beaconMaps</code> - Maps Minecraft map IDs to beacon objects for territory maps</li>
 * </ul>
 * <p>
 * <b>Triangle Field Formation:</b>
 * When a beacon link is created, the system automatically checks if it completes any triangles:
 * <ol>
 *   <li>New link A→B is created</li>
 *   <li>System checks all beacons linked to B</li>
 *   <li>For each beacon C linked to B, checks if A is linked to C</li>
 *   <li>If triangle A-B-C exists with all same-team beacons, attempts to create field</li>
 *   <li>Validates no enemy beacons/links inside, no intersecting enemy links</li>
 *   <li>If valid, creates TriangleField and updates team score</li>
 * </ol>
 * <p>
 * <b>File Format (beaconz.yml):</b>
 * Each beacon stores: location (x:y:z:owner), links to other beacons, defensive blocks,
 * map IDs, and defensive block details including level and placer UUID.
 *
 * @author tastybento
 */
public class Register extends BeaconzPluginDependent {

    private static final boolean DEBUG = false;

    /**
     * Constructs a new Register instance.
     *
     * @param beaconzPlugin the main Beaconz plugin instance
     */
    public Register(Beaconz beaconzPlugin) {
        super(Objects.requireNonNull(beaconzPlugin));
    }

    /** Maps Minecraft map item IDs to their associated beacon objects for territory display */
    private final HashMap<Integer, BeaconObj> beaconMaps = new HashMap<>();

    /**
     * Primary beacon lookup table - maps 2D coordinates (x,z) to beacon objects.
     * This is the authoritative registry of all beacons in the game.
     */
    private final HashMap<Point2D, BeaconObj> beaconRegister = new HashMap<>();

    /**
     * Set of all active triangle control fields in the game.
     * Triangles are formed when three same-team beacons are linked together.
     */
    private Set<TriangleField> triangleFields = new HashSet<>();

    /**
     * Maps each Game instance to its list of beacon links.
     * Links connect beacons owned by the same team and can form triangle fields.
     */
    private final HashMap<Game,List<BeaconLink>> beaconLinks = new HashMap<>();

    /**
     * Maps 2D coordinates of base blocks (emerald blocks around beacons) to their parent beacon.
     * Initially contains the 8 blocks adjacent to each beacon, can expand as players add more.
     * Used for fast lookup when checking if a block is part of a beacon's base.
     */
    private final HashMap<Point2D, BeaconObj> baseBlocks = new HashMap<>();

    /**
     * Inverse mapping: beacon to all its base block coordinates.
     * Used to efficiently retrieve all base blocks belonging to a specific beacon,
     * which is needed for saving to file and rendering operations.
     */
    private final HashMap<BeaconObj, Set<Point2D>> baseBlocksInverse = new HashMap<>();

    /**
     * Persists all game data to the beaconz.yml file.
     * <p>
     * This method serializes the entire game state including:
     * <ul>
     *   <li>All beacon locations and ownership</li>
     *   <li>Beacon links (connections between beacons)</li>
     *   <li>Base blocks (emerald blocks around beacons)</li>
     *   <li>Defensive blocks with levels and placer UUIDs</li>
     *   <li>Map item IDs associated with beacons</li>
     * </ul>
     * <p>
     * The method:
     * <ol>
     *   <li>Creates a backup of the existing file (beaconz.old)</li>
     *   <li>Iterates through all beacons in the registry</li>
     *   <li>Saves each beacon's data to the YAML configuration</li>
     *   <li>Avoids duplicate link storage (links are bidirectional)</li>
     *   <li>Writes the complete configuration to disk</li>
     * </ol>
     * <p>
     * <b>File Structure:</b>
     * <pre>
     * beacon:
     *   0:
     *     game: "GameName"
     *     location: "x:y:z:ownerTeamName"
     *     links: ["destX:destZ:timestamp", ...]
     *     baseblocks: ["x:z", ...]
     *     defensiveblocks:
     *       location_string: level
     *     maps: [mapId1, mapId2, ...]
     * </pre>
     * <p>
     * Links are only stored once (from beacon1 to beacon2) to reduce file size.
     * The reverse link is automatically created when loading.
     */
    public void saveRegister() {
        // Save the beacons
        File beaconzFile = new File(getBeaconzPlugin().getDataFolder(),"beaconz.yml");

        // Track which links have been stored to avoid duplicates (links are bidirectional)
        Set<BeaconLink> storedLinks = new HashSet<>();
        YamlConfiguration beaconzYml = new YamlConfiguration();

        // Backup the existing beacons file to prevent data loss
        if (beaconzFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"beaconz.old");
            beaconzFile.renameTo(backup);
        }

        // Iterate through all beacons and serialize their data
        int count = 0;
        for (BeaconObj beacon : beaconRegister.values()) {
            // Determine which game this beacon belongs to
            Game game = getGameMgr().getGame(beacon.getPoint());
            String gameName = game == null ? "None" :  PlainTextComponentSerializer.plainText().serialize(game.getName());
            beaconzYml.set("beacon." + count + ".game",gameName);

            // Store beacon ownership (team name or "unowned")
            String owner = "unowned";
            if (beacon.getOwnership() != null) {
                owner = beacon.getOwnership().getName();
            }

            // Store location as "x:y:z:owner" format
            beaconzYml.set("beacon." + count + ".location", beacon.getX() + ":" + beacon.getY() + ":" + beacon.getZ()
            + ":" + owner);

            // Store links to other beacons (only outbound links to avoid duplication)
            if (game != null) {
                List<String> beaconStringLinks = new ArrayList<>();
                if (beaconLinks.containsKey(game)) {
                    for (BeaconLink link : beaconLinks.get(game)) {
                        // Only store each link once - when this beacon is beacon1
                        // The reverse link will be auto-created during load
                        if (!storedLinks.contains(link) && link.getBeacon1().equals(beacon)) {
                            beaconStringLinks.add(link.getBeacon2().getX() +":" + link.getBeacon2().getZ()+ ":" + link.getTimeStamp());
                            storedLinks.add(link);
                        }
                    }
                    beaconzYml.set("beacon." + count + ".links", beaconStringLinks);
                }
            }

            // Store map ID if this beacon has an associated map item
            if (beacon.getId() != null) {
                beaconzYml.set("beacon." + count + ".id", beacon.getId());
            }

            // Save base blocks (emerald blocks around the beacon)
            List<String> plinthBlocksString = new ArrayList<>();
            for (Point2D point: baseBlocksInverse.get(beacon)) {
                plinthBlocksString.add((int)point.getX() + ":" + (int)point.getY());
            }
            beaconzYml.set("beacon." + count + ".baseblocks", plinthBlocksString);

            // Save defensive blocks with their levels and placers
            for (DefenseBlock defensiveBlock : beacon.getDefenseBlocks().values()) {
                String locationKey = Beaconz.getStringLocation(defensiveBlock.getBlock().getLocation()).replace('.', '_');
                beaconzYml.set("beacon." + count + ".defensiveblocks." + locationKey, defensiveBlock.getLevel());
                if (defensiveBlock.getPlacer() != null) {
                    beaconzYml.set("beacon." + count + ".defensiveblocksowner." + locationKey, defensiveBlock.getPlacer().toString());
                }
            }

            // Save map item IDs associated with this beacon
            List<String> maps = new ArrayList<>();
            for (Integer id : beaconMaps.keySet()) {
                if (beacon.equals(beaconMaps.get(id))) {
                    // Verify the map still exists on the server before saving
                    if (Bukkit.getMap(id) != null) {
                        maps.add(String.valueOf(id));
                    }
                }
            }
            beaconzYml.set("beacon." + count + ".maps", maps);
            count++;
        }

        // Write the configuration to disk
        try {
            beaconzYml.save(beaconzFile);
        } catch (IOException e) {
            getLogger().severe("Problem saving beacons file!");
            e.printStackTrace();
        }
    }

    /**
     * Loads all game data from the beaconz.yml file and reconstructs the game state.
     * <p>
     * This method deserializes persisted data and rebuilds all game structures:
     * <ul>
     *   <li>Beacons with their locations and ownership</li>
     *   <li>Base blocks (emerald blocks) around each beacon</li>
     *   <li>Defensive blocks with levels and placer information</li>
     *   <li>Map renderers for territory map items</li>
     *   <li>Beacon links (connections between same-team beacons)</li>
     *   <li>Triangle fields (automatically generated from links)</li>
     * </ul>
     * <p>
     * <b>Loading Process:</b>
     * <ol>
     *   <li>Clear existing data structures</li>
     *   <li>Load and parse beaconz.yml file</li>
     *   <li>Create beacon objects for each entry</li>
     *   <li>Load base blocks and defensive blocks</li>
     *   <li>Initialize map renderers for territory maps</li>
     *   <li>Reconstruct links between beacons (stored as strings)</li>
     *   <li>Sort links by timestamp (creation order)</li>
     *   <li>Add links in chronological order</li>
     *   <li>Auto-generate triangle fields from valid link combinations</li>
     *   <li>Recalculate scores for all games</li>
     * </ol>
     * <p>
     * Links are stored unidirectionally in the file but created bidirectionally in memory.
     * Triangle fields are not saved explicitly - they're regenerated from beacon links.
     * <p>
     * Beacons for deleted games are skipped during loading to prevent orphaned data.
     */
    public void loadRegister() {
        // Clear existing data to start fresh
        clear();

        File beaconzFile = new File(getBeaconzPlugin().getDataFolder(),"beaconz.yml");
        if (!beaconzFile.exists()) {
            return;
        }

        // Load the YAML configuration from file
        YamlConfiguration beaconzYml = new YamlConfiguration();
        try {
            beaconzYml.load(beaconzFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Problem with beaconz.yml formatting");
            e.printStackTrace();
        }

        // === PHASE 1: Load all beacons ===
        beaconLinks.clear();
        // Temporary storage for link data (will be processed after all beacons are loaded)
        HashMap<BeaconObj, List<String>> beaconStringLinks = new HashMap<>();
        ConfigurationSection configSec = beaconzYml.getConfigurationSection("beacon");

        if (configSec != null) {
            for (String beacon : configSec.getValues(false).keySet()) {
                // Parse beacon location string "x:y:z:owner"
                String info = configSec.getString(beacon + ".location","");
                String[] args = info.split(":");

                if (!info.isEmpty() && args.length == 4) {
                    if (NumberUtils.isNumber(args[0]) && NumberUtils.isNumber(args[1]) && NumberUtils.isNumber(args[2])) {
                        int x = Integer.parseInt(args[0]);
                        int y = Integer.parseInt(args[1]);
                        int z = Integer.parseInt(args[2]);

                        // Verify the game still exists at this location
                        Game game = getGameMgr().getGame(x, z);
                        if (game != null) {
                            // Resolve team ownership
                            Team team = null;
                            if (!args[3].equalsIgnoreCase("unowned")) {
                                team = game.getScorecard().getTeam(args[3]);
                            }

                            // Create the beacon object and add to registry
                            BeaconObj newBeacon = addBeacon(team, x, y, z);

                            // Store link data for later processing (after all beacons exist)
                            beaconStringLinks.put(newBeacon, configSec.getStringList(beacon + ".links"));

                            // Initialize the link array for this game if needed
                            if (beaconLinks.get(game) == null) {
                                List<BeaconLink> pairs = new ArrayList<>();
                                beaconLinks.put(game, pairs);
                            }

                            // Load base blocks (emerald blocks around the beacon)
                            List<String> baseBlocks = configSec.getStringList(beacon + ".baseblocks");
                            for (String baseBlock : baseBlocks) {
                                String[] args2 = baseBlock.split(":");
                                if (args2.length == 2) {
                                    if (NumberUtils.isNumber(args2[0]) && NumberUtils.isNumber(args2[1])) {
                                        int blockX = Integer.parseInt(args2[0]);
                                        int blockZ = Integer.parseInt(args2[1]);
                                        addBeaconBaseBlock(blockX, blockZ, newBeacon);
                                    }
                                }
                            }

                            // Load defensive blocks with their levels
                            ConfigurationSection defBlocks = configSec.getConfigurationSection(beacon + ".defensiveblocks");
                            if (defBlocks != null) {
                                for (String defenseBlock : defBlocks.getKeys(false)) {
                                    // Get the block at the stored location
                                    Block b = Beaconz.getLocationString(defenseBlock).getBlock();
                                    int level = defBlocks.getInt(defenseBlock);
                                    // Try to get the player who placed this defensive block
                                    String owner = configSec.getString(beacon + ".defensiveblocksowner." + defenseBlock);
                                    newBeacon.addDefenseBlock(b,level,owner);
                                }
                            }

                            // Load map item IDs and initialize renderers
                            List<String> maps = configSec.getStringList(beacon + ".maps");
                            for (String mapNumber: maps) {
                                int id = Integer.parseInt(mapNumber);
                                beaconMaps.put(id, newBeacon);
                                MapView map = Bukkit.getMap(id);
                                if (map != null) {
                                    // Remove old renderers and add fresh ones
                                    for (MapRenderer renderer : map.getRenderers()) {
                                        if (renderer instanceof TerritoryMapRenderer || renderer instanceof BeaconMap) {
                                            map.removeRenderer(renderer);
                                        }
                                    }
                                    map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
                                    map.addRenderer(new BeaconMap(getBeaconzPlugin()));
                                } else {
                                    getLogger().severe("Could not load map #" + id + " as it doesn't exist on this server. Skipping...");
                                }
                            }
                        }
                        // If game is null, beacon is from a deleted game - skip it
                    }
                }
            }
        }

        // === PHASE 2: Reconstruct beacon links ===
        // Now that all beacons exist, we can resolve link references
        long count = 0;
        for (BeaconObj beacon: beaconStringLinks.keySet()) {
            for (String link : beaconStringLinks.get(beacon)) {
                // Parse link string "destX:destZ:timestamp"
                String[] args = link.split(":");
                BeaconObj dest = beaconRegister.get(new Point2D.Double(Double.parseDouble(args[0]), Double.parseDouble(args[1])));
                if (dest != null) {
                    // Extract timestamp (or assign sequential timestamp if missing from old saves)
                    long linkTime = 0L;
                    if (args.length == 3) {
                        linkTime = Long.parseLong(args[2]);
                    } else {
                        // Old format without timestamp - assign sequential times
                        count += 1000;
                        linkTime = count;
                    }

                    // Create the link object
                    BeaconLink newBeaconPair = new BeaconLink(beacon, dest, linkTime);
                    Game game = getGameMgr().getGame(beacon.getPoint());
                    if (game != null) {
                        if (beaconLinks.get(game) == null) {
                            List<BeaconLink> pairs = new ArrayList<>();
                            beaconLinks.put(game, pairs);
                        }
                        // Check for duplicate links before adding
                        if (!beaconLinks.get(game).contains(newBeaconPair)) {
                            beaconLinks.get(game).add(newBeaconPair);
                        } else {
                            getLogger().warning("Removed duplicate link");
                        }
                    }
                }
            }
        }

        // === PHASE 3: Create beacon links and triangle fields ===
        // Process each game's links in chronological order
        for (Entry<Game, List<BeaconLink>> entry : beaconLinks.entrySet()) {
            // Sort by timestamp to recreate links in the same order they were made
            Collections.sort(entry.getValue());

            // Create the actual bidirectional links in the beacon objects
            for (BeaconLink beaconPair: entry.getValue()) {
                beaconPair.getBeacon1().addOutboundLink(beaconPair.getBeacon2());
            }

            // Recalculate scores and auto-generate triangle fields
            recalculateScore(entry.getKey());
        }
    }

    /**
     * Clears all data from the register across all games.
     * <p>
     * This removes:
     * <ul>
     *   <li>All beacon registrations</li>
     *   <li>All triangle fields</li>
     *   <li>All beacon links</li>
     *   <li>All map associations</li>
     *   <li>All base blocks</li>
     * </ul>
     * <p>
     * Typically used when reloading the plugin or resetting all games.
     */
    public void clear() {
        clear(null);
    }

    /**
     * Clears data for a specific region/game.
     * <p>
     * When a region is specified, only beacons, links, triangles, and maps
     * associated with that region are removed. Other game data remains intact.
     * <p>
     * This is used when deleting a game or regenerating a specific region.
     *
     * @param region the region to clear, or null to clear all data
     */
    public void clear(Region region) {
        if (region == null) {
            beaconMaps.clear();
            beaconRegister.clear();
            triangleFields.clear();
            //links.clear();
            beaconLinks.clear();
        } else {
            beaconMaps.entrySet().removeIf(en -> region.containsBeacon(en.getValue()));
            beaconRegister.entrySet().removeIf(en -> region.containsPoint(en.getKey()));
            triangleFields.removeIf(tri -> region.containsPoint(tri.a));
            beaconLinks.remove(region.getGame());
        }
    }

    /**
     * Creates a new link between two beacons and attempts to form triangle fields.
     * <p>
     * This is the primary method for establishing beacon connections, which are the
     * building blocks of territorial control. The process:
     * <ol>
     *   <li>Creates a BeaconLink object with current timestamp</li>
     *   <li>Adds link to the game's link registry</li>
     *   <li>Adds bidirectional connection to beacon objects</li>
     *   <li>Shows visual particle line between beacons</li>
     *   <li>Checks if link completes any triangles with existing links</li>
     *   <li>Attempts to create triangle fields for valid combinations</li>
     *   <li>Recalculates team scores if new fields were created</li>
     * </ol>
     * <p>
     * <b>Triangle Detection Algorithm:</b>
     * <pre>
     * For new link A→B:
     *   For each beacon C linked to B:
     *     If A is also linked to C:
     *       Triangle A-B-C exists → attempt field creation
     * </pre>
     * <p>
     * Links can fail if:
     * <ul>
     *   <li>Beacon already has maximum allowed outbound links (Settings.linkLimit)</li>
     *   <li>Link would create an invalid triangle (enemy beacons/links inside)</li>
     *   <li>Link intersects with enemy links</li>
     * </ul>
     *
     * @param startBeacon the source beacon for the link
     * @param endBeacon the destination beacon for the link
     * @return LinkResult containing: (fieldsMade, success, fieldsFailed)
     */
    public LinkResult addBeaconLink(BeaconObj startBeacon, BeaconObj endBeacon) {
        Game game = getGameMgr().getGame(startBeacon.getPoint());

        // Create link object with current timestamp
        BeaconLink beaconPair = new BeaconLink(startBeacon, endBeacon);

        // Initialize link list for this game if needed
        beaconLinks.computeIfAbsent(game, k -> new ArrayList<>());

        // Check for duplicate links (links are compared bidirectionally)
        if (!beaconLinks.get(game).contains(beaconPair)) {
            beaconLinks.get(game).add(beaconPair);

            // Try to add the link to the beacon's outbound link list
            // This can fail if the beacon has reached its link limit
            if (!startBeacon.addOutboundLink(endBeacon)) {
                return new LinkResult(0,false,0);
            }

            // Show visual particle line between the beacons
            new LineVisualizer(this.getBeaconzPlugin(), beaconPair, true);

            // Attempt to create triangle fields from this new link
            int fieldsMade = 0;
            int fieldsFailed = 0;

            // Triangle detection algorithm: check if new link A→B completes any triangles
            // For each beacon C linked to A:
            //   If B is also linked to C, then triangle A-B-C exists

            // Iterate through all beacons linked to the start beacon
            for (BeaconObj secondPoint : startBeacon.getLinks()) {
                // Check all beacons linked to this second beacon
                for (BeaconObj thirdPoint : secondPoint.getLinks()) {
                    // Skip if the third point is back to where we started (not a triangle)
                    if (!thirdPoint.equals(startBeacon)) {
                        // Check if the third point is the end beacon
                        // If so, we have a complete triangle: start → second → end → start
                        if (thirdPoint.equals(endBeacon)) {
                            // Triangle found! Attempt to create the field
                            // This validates no enemy beacons/links inside and no intersections
                            try {
                                if (getRegister().addTriangle(startBeacon.getPoint(), secondPoint.getPoint(),
                                        thirdPoint.getPoint(), startBeacon.getOwnership())) {
                                    fieldsMade++;
                                } else {
                                    fieldsFailed++;
                                }
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace();
                            }
                            // Continue checking - one link can complete multiple triangles
                        }
                    }
                }
            }
            // Return the result
            return new LinkResult(fieldsMade, true, fieldsFailed); 
        }
        // Failure
        return new LinkResult(0, false, 0); 
    }

    /**
     * Get the number of links a team has
     * @param team
     * @return number of links
     */
    public int getTeamLinks(Team team) {
        //getLogger().info("DEBUG: getting team links " + beaconLinks.get(getGameMgr().getGame(team)));
        int result = 0;
        if (getGameMgr().getGame(team) != null && beaconLinks.containsKey(getGameMgr().getGame(team))) {
            for (BeaconLink pair: beaconLinks.get(getGameMgr().getGame(team))) {
                if (pair.getOwner().equals(team)) {
                    result++;
                }
            }
        }
        return result;
    }

    /**
     * Gets all beacons owned by a specific team.
     * <p>
     * This iterates through the entire beacon registry and filters by ownership.
     *
     * @param team the team whose beacons to retrieve
     * @return list of BeaconObj instances owned by the team, or empty list if none
     */
    public List<BeaconObj> getTeamBeacons(Team team) {
        List<BeaconObj> teambeacons = new ArrayList<>();
        for (BeaconObj beacon : beaconRegister.values()) {
            if (beacon.getOwnership() != null && beacon.getOwnership().equals(team)) {
                teambeacons.add(beacon);
            }
        }
        return teambeacons;
    }

    /**
     * Counts the number of triangle control fields owned by a team.
     * <p>
     * Each triangle field provides territorial control and contributes to team score.
     *
     * @param team the team to count triangles for
     * @return the number of triangle fields owned by the team
     */
    public int getTeamTriangles(Team team) {
        int teamtriangles = 0;
        for (TriangleField triangle : triangleFields) {
            if (triangle.getOwner() != null && triangle.getOwner().equals(team)) {
                teamtriangles++;
            }
        }
        return teamtriangles;
    }

    /**
     * Calculates the total area controlled by a team across all their triangle fields.
     * <p>
     * This is the primary scoring metric in the game. Larger triangles provide more points.
     * The area is calculated by summing the areas of all triangles owned by the team.
     *
     * @param team the team to calculate area for
     * @return total area in square blocks controlled by the team
     */
    public int getTeamArea(Team team) {
        return (int)TriangleScorer.getScore(triangleFields, team);
    }

    /**
     * Registers a new beacon in the game world.
     * <p>
     * This method:
     * <ul>
     *   <li>Creates a BeaconObj instance</li>
     *   <li>Registers it at the specified coordinates</li>
     *   <li>Creates the initial 3x3 grid of base blocks (8 surrounding + center)</li>
     *   <li>Updates team score if the beacon is owned</li>
     * </ul>
     * <p>
     * <b>Initial Base Block Layout:</b>
     * <pre>
     * [E][E][E]
     * [E][B][E]   where B = beacon center, E = emerald base blocks
     * [E][E][E]
     * </pre>
     * <p>
     * The 8 surrounding blocks are the initial "base blocks" that can be expanded
     * by players placing additional emerald blocks adjacent to existing base blocks.
     *
     * @param owner the team that owns this beacon, or null for unowned
     * @param x the X coordinate of the beacon
     * @param y the Y coordinate (height) of the beacon
     * @param z the Z coordinate of the beacon
     * @return the newly created BeaconObj instance
     */
    public BeaconObj addBeacon(Team owner, int x, int y, int z) {
        // Create the beacon object
        BeaconObj beacon = new BeaconObj(getBeaconzPlugin(), x, y, z, owner);

        // Register the beacon and create the 3x3 initial base block grid
        for (int xx = x-1; xx <= x + 1; xx++) {
            for (int zz = z - 1; zz <= z + 1; zz++) {
                Point2D location = new Point2D.Double(xx,zz);

                if (xx == x && zz == z) {
                    // Center position - register the beacon itself
                    beaconRegister.put(location, beacon);
                } else {
                    // Surrounding 8 positions - register as base blocks (emerald blocks)
                    baseBlocks.put(location, beacon);

                    // Also maintain inverse mapping (beacon → set of base blocks)
                    Set<Point2D> points = baseBlocksInverse.get(beacon);
                    if (points == null) {
                        points = new HashSet<>();
                    }
                    points.add(location);
                    baseBlocksInverse.put(beacon, points);
                }
            }
        }

        // Update team scores if this is an owned beacon
        if (owner != null) {
            Game game = getGameMgr().getGame(x, z);
            game.getScorecard().refreshScores(owner);
        }

        return beacon;
    }

    /**
     * Creates a triangular control field between three same-team beacons.
     * <p>
     * This is the core method for territorial control. It validates that a valid
     * triangle can be formed and doesn't conflict with enemy territories before creating it.
     * <p>
     * <b>Validation Rules:</b>
     * <ol>
     *   <li>All three points must be registered beacons</li>
     *   <li>All three beacons must be owned by the same team</li>
     *   <li>Triangle cannot overlap enemy triangles (mutual containment check)</li>
     *   <li>Triangle sides cannot intersect enemy beacon links</li>
     *   <li>Triangle must not be a duplicate</li>
     * </ol>
     * <p>
     * <b>Overlap Detection:</b>
     * Two triangles "overlap" if either fully contains the other. Partial overlaps
     * where triangles share edges or vertices are allowed (this happens with friendly triangles).
     * <p>
     * <b>Link Intersection:</b>
     * Each of the three sides of the new triangle is checked against all enemy links.
     * If any side crosses an enemy link, the triangle cannot be created.
     * <p>
     * Upon successful creation, team scores are automatically recalculated.
     *
     * @param point2d first beacon coordinate
     * @param point2d2 second beacon coordinate
     * @param point2d3 third beacon coordinate
     * @param owner the team that owns all three beacons
     * @return true if triangle was successfully created, false if validation failed
     * @throws IllegalArgumentException if any point is not a beacon or beacons have different owners
     */
    public Boolean addTriangle(Point2D point2d, Point2D point2d2, Point2D point2d3, Team owner)  throws IllegalArgumentException {
        // Verify all three points are registered beacons
        if (beaconRegister.containsKey(point2d) && beaconRegister.containsKey(point2d2) && beaconRegister.containsKey(point2d3)) {
            // Verify all three beacons are owned by the same team
            if (beaconRegister.get(point2d).getOwnership().equals(owner)
                    && beaconRegister.get(point2d2).getOwnership().equals(owner)
                    && beaconRegister.get(point2d3).getOwnership().equals(owner)) {

                // Create the triangle object
                TriangleField triangle = new TriangleField(point2d, point2d2, point2d3, owner);

                // Check for conflicts with existing triangles
                for (TriangleField triangleField : triangleFields) {
                    // Prevent enemy triangle overlaps (mutual containment)
                    // If either triangle fully contains the other, reject
                    if (!triangle.getOwner().equals(triangleField.getOwner()) &&
                            (triangleField.contains(triangle) || triangle.contains(triangleField))) {
                        return false;
                    }

                    // Check for duplicate triangles (links are bidirectional, so this is possible)
                    if (triangle.equals(triangleField)) {
                        return false;
                    }
                }

                // Check if any triangle side intersects with enemy beacon links
                for (Line2D link: getEnemyLinks(owner)) {
                    for (Line2D side : triangle.getSides()) {
                        if (side.intersectsLine(link)) {
                            // Enemy link crosses through the triangle - reject
                            return false;
                        }
                    }
                }

                // All validations passed - add the triangle to the field set
                if (triangleFields.add(triangle)) {
                    // Successfully added! Update team scores
                    Game game = getGameMgr().getGame(point2d);
                    game.getScorecard().refreshScores(owner);
                    return true;
                }
            } else {
                // Beacons have different owners
                throw new IllegalArgumentException("beacons are not owned by the same team");
            }
        } else {
            // One or more points are not beacons
            throw new IllegalArgumentException("Location argument is not a beacon");
        }
        return false;
    }

    /**
     * @return the beaconRegister
     */
    public HashMap<Point2D, BeaconObj> getBeaconRegister() {
        return beaconRegister;
    }

    /**
     * @param triangleFields the triangleFields to set
     */
    public void setTriangleFields(Set<TriangleField> triangleFields) {
        this.triangleFields = triangleFields;
    }

    /**
     * @return the triangleFields
     */
    public Set<TriangleField> getTriangleFields() {
        return triangleFields;
    }

    /**
     * Checks if a block is part of a natural beacon
     * @param b
     * @return true if it is part of a beacon, false if not
     */
    public boolean isBeacon(Block b) {
        return getBeacon(b) != null;
    }

    /**
     * Checks if a beacon is within the range around point
     * @param point
     * @param range
     * @return true if beacon is there, false if not
     */
    public boolean isNearBeacon(Point2D point, int range) {
        int distSquared = range*range;
        for (Point2D beacon : beaconRegister.keySet()) {
            // Distance squared check is less computationally intensive than checking the square
            if (distSquared > point.distanceSq(beacon)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a list of all nearby beacons within range
     * @param location
     * @param range
     * @return list of nearby beacons
     */
    public List<BeaconObj> getNearbyBeacons(Location location, int range) {
        int distSquared = range*range;
        List<BeaconObj> result = new ArrayList<>();
        Point2D point = new Point2D.Double(location.getX(), location.getZ());
        for (Point2D beacon : beaconRegister.keySet()) {
            // Distance squared check is less computationally intensive than checking the square
            if (distSquared > point.distanceSq(beacon)) {
                result.add(beaconRegister.get(beacon));
            }
        }
        return result;
    }

    /**
     * Returns beacons by index
     * @param index
     * @return beacon at index
     */
    public BeaconObj getBeacon(int index) {
        if (index >= 0 && index < beaconMaps.size()) {
            return beaconMaps.get(index);
        }
        return null;
    }

    /**
     * Gets the beacon connected to block.
     * @param block
     * @return BeaconObj or null if none
     */
    public BeaconObj getBeacon(Block block) {
        if (DEBUG) getLogger().info("DEBUG: getBeacon ");
        // Quick check
        if (!block.getType().equals(Material.BEACON) && !block.getType().equals(Material.DIAMOND_BLOCK)
                && !block.getType().equals(Material.OBSIDIAN) &&  !block.getType().name().endsWith("STAINED_GLASS")
                && !block.getType().equals(Material.EMERALD_BLOCK)) {
            if (DEBUG) getLogger().info("DEBUG: wrong type ");
            return null;
        }
        Point2D point = new Point2D.Double(block.getLocation().getBlockX(),block.getLocation().getBlockZ());

        // Check plinth blocks
        if (block.getType().equals(Material.EMERALD_BLOCK)) {
            if (DEBUG) getLogger().info("DEBUG: emerald ");
            if (baseBlocks.containsKey(point)) {
                // Check height
                BeaconObj beacon = baseBlocks.get(point);
                if (beacon.getY() == block.getY() + 1) {
                    // Correct height
                    return beacon;
                } else {
                    return null;
                }
            }
        }

        // Check glass or obsidian
        if (block.getType().equals(Material.OBSIDIAN) || block.getType().name().endsWith("STAINED_GLASS")) {
            if (DEBUG) getLogger().info("DEBUG: obsidian or stained glass ");
            Block below = block.getRelative(BlockFace.DOWN);
            if (!below.getType().equals(Material.BEACON)) {
                getLogger().info("DEBUG: no beacon below here");
                return null;
            }
            point = new Point2D.Double(below.getLocation().getBlockX(),below.getLocation().getBlockZ());
            // Beacon below
            return beaconRegister.getOrDefault(point, null);
        }
        // Check beacons
        if (block.getType().equals(Material.BEACON)) {
            if (DEBUG) getLogger().info("DEBUG: beacon ");
            return beaconRegister.getOrDefault(point, null);
        }
        // Check the pyramid around the beacon
        if (DEBUG) getLogger().info("DEBUG: check pyramid ");
        // Look for a beacon
        for (int modX = -1; modX < 2; modX++) {
            for (int modZ = -1; modZ < 2; modZ++) {
                for (int modY = 1; modY < 3; modY++) {
                    Block test = block.getRelative(modX, modY, modZ);
                    if (DEBUG) getLogger().info("DEBUG: test is " + test.getType() + " and is at " + test.getLocation());
                    if (test.getType().equals(Material.BEACON)) {
                        if (DEBUG) getLogger().info("DEBUG: test is a beacon. Check if it's a known beacon ");
                        point = new Point2D.Double(test.getLocation().getBlockX(),test.getLocation().getBlockZ());
                        if (beaconRegister.containsKey(point)) {
                            if (DEBUG) getLogger().info("DEBUG: beacon found ");
                            return beaconRegister.get(point);
                        }
                    }
                }
            }
        }
        if (DEBUG) getLogger().info("DEBUG: no beacon found ");
        return null;
    }

    /**
     * Removes this beacon from team ownership and makes it unowned
     * @param beacon
     */
    public void removeBeaconOwnership(BeaconObj beacon) {
        removeBeaconOwnership(beacon, false);
    }

    /**
     * Removes this beacon from team ownership and makes it unowned
     * @param beacon
     * @param quiet - if true, then no messages are sent to the team
     */
    public void removeBeaconOwnership(BeaconObj beacon, Boolean quiet) {
        Game game = getGameMgr().getGame(beacon.getPoint());
        Team oldOwner = beacon.getOwnership();
        beacon.setOwnership(null);

        // Remove links to the beacon (and back)
        for (BeaconObj beaconObj : beacon.getLinks()) {
            beaconObj.removeLink(beacon);
        }
        if (!beaconLinks.isEmpty() && game != null) {
            if (beaconLinks.get(game) != null) {
                // Remove links from this register
                beaconLinks.get(game).removeIf(beaconPair -> beaconPair.getBeacon1().equals(beacon) || beaconPair.getBeacon2().equals(beacon));
                Iterator<BeaconLink> linkIterator = beaconLinks.get(game).iterator();
                int linkLossCount = 0;
                while (linkIterator.hasNext()) {
                    BeaconLink pair = linkIterator.next();
                    if (pair.getBeacon1().equals(beacon) || pair.getBeacon2().equals(beacon)) {
                        linkLossCount++;
                        linkIterator.remove();
                    }
                }
                // linkLossCount should always be a multiple of 2 because links go both ways
                // so divide it by two
                linkLossCount /= 2;
                // Tell folks what's going on
                if (oldOwner != null) {
                    if (linkLossCount == 1 && !quiet) {
                        getMessages().tellTeam(oldOwner, Lang.linkLostLink.color(NamedTextColor.RED));
                        getMessages().tellOtherTeams(oldOwner,Lang.linkTeamLostLink.replaceText(builder -> builder.matchLiteral("[team]").replacement(oldOwner.displayName())).color(NamedTextColor.GREEN));
                    } else if (linkLossCount > 1) {
                        String count = String.valueOf(linkLossCount);
                        getMessages().tellTeam(oldOwner, Lang.linkLostLinks.replaceText(builder -> builder.matchLiteral("[number]").replacement(Component.text(count))).color(NamedTextColor.RED));
                        getMessages().tellOtherTeams(oldOwner, Lang.linkTeamLostLinks.replaceText(builder -> builder.matchLiteral("[team]")
                                .replacement(oldOwner.displayName())).replaceText(builder -> builder.matchLiteral("[number]").replacement(Component.text(count))).color(NamedTextColor.GREEN));
                    }
                }
            }
        }
        beacon.removeLinks();

        // Get any control triangles that have been removed because of this
        //HashMap<Player, List<TriangleField>> players = new HashMap<Player, List<TriangleField>>();
        Iterator<TriangleField> it = triangleFields.iterator();
        while (it.hasNext()) {
            TriangleField triangle = it.next();
            if (triangle.hasVertex(beacon.getPoint())) {
                //getLogger().info("DEBUG: this beacon was part of a triangle");
                // Tell folks what's going on
                if (!quiet && triangle.getOwner() != null) {
                    getMessages().tellTeam(triangle.getOwner(), Lang.triangleYourTeamLostATriangle.color(NamedTextColor.RED));
                    getMessages().tellOtherTeams(triangle.getOwner(), Lang.triangleTeamLostATriangle.replaceText(builder -> builder.matchLiteral(
                            "[team]").replacement(triangle.getOwner().displayName())).color(NamedTextColor.GREEN));
                }
                // Find any players in the triangle being removed
                for (Player player: getServer().getOnlinePlayers()) {
                    if (getBeaconzWorld().equals(player.getWorld())) {
                        if (triangle.contains(new Point2D.Double(player.getLocation().getX(), player.getLocation().getZ()))) {
                            // Player is in triangle, remove effects
                            for (PotionEffect effect : getPml().getTriangleEffects(player.getUniqueId()))
                                player.removePotionEffect(effect.getType());
                        }
                    }
                }
                // Remove triangle
                it.remove();
            }
        }

        // Cap the beacon with obsidian
        getBeaconzWorld().getBlockAt(beacon.getX(), beacon.getHeight() + 1, beacon.getZ()).setType(Material.OBSIDIAN);

        // Refresh the scores
        recalculateScore(getGameMgr().getGame(beacon.getX(), beacon.getZ()));
        Scorecard sc = getGameMgr().getSC(beacon.getX(), beacon.getZ());
        if(sc!=null && oldOwner != null) sc.refreshScores(oldOwner);
    }

    /**
     * Registers a beacon at location
     * @param team
     * @param location
     */
    public void addBeacon(Team team, Location location) {
        addBeacon(team, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * @return the beaconMaps or null if not found
     */
    public BeaconObj getBeaconMap(int index) {
        return beaconMaps.get(index);
    }

    /**
     * @param beacon the beacon map to add
     */
    public void addBeaconMap(int i, BeaconObj beacon) {
        beacon.setId(i);
        //getLogger().info("DEBUG: storing beacon map # " + index + " for beacon at "+ beacon.getLocation());
        this.beaconMaps.put(i, beacon);
    }

    /**
     * @param index the map index to remove
     */
    public void removeBeaconMap(int index) {
        this.beaconMaps.remove(index);
    }

    /**
     * @return set of all the beacon maps
     */
    public Set<Integer> getBeaconMapIndex() {
        return beaconMaps.keySet();
    }

    /**
     * Finds all triangle fields that contain a specific coordinate.
     * <p>
     * Since triangle fields can overlap (friendly triangles sharing edges/vertices),
     * multiple triangles may contain the same point. This is used to:
     * <ul>
     *   <li>Determine which team buffs/debuffs to apply to a player at a location</li>
     *   <li>Calculate effect strength based on field overlap</li>
     *   <li>Display territory information</li>
     * </ul>
     * <p>
     * <b>Performance Note:</b> Currently uses brute-force iteration through all triangles.
     * For large numbers of triangles, consider implementing spatial indexing (quadtree, R-tree, etc.).
     *
     * @param x the X coordinate to check
     * @param y the Z coordinate to check (despite parameter name)
     * @return list of TriangleField objects containing this point (may be empty)
     */
    public List<TriangleField> getTriangle(int x, int y) {
        List<TriangleField> result = new ArrayList<>();

        // Iterate through all triangles and check containment
        for (TriangleField tri: triangleFields) {
            if (tri.contains(x, y) != null) {
                result.add(tri);
            }
        }
        return result;
    }

    /**
     * Returns the beacon at x,z or null if there is none
     * @param x
     * @param z
     * @return beacon object
     */
    public BeaconObj getBeaconAt(int x, int z) {
        Point2D point = new Point2D.Double(x,z);
        return beaconRegister.get(point);
    }

    /**
     * Sets the beacon ownership, team = null means it is unowned.
     * @param beacon
     * @param team
     */
    public void setBeaconOwner(BeaconObj beacon, Team team) {
        Team oldowner = beacon.getOwnership();
        beacon.setOwnership(team);
        // TODO : Add other things in the future as a result of the ownership change
        Game game = getGameMgr().getGame(beacon.getX(), beacon.getZ());
        if (oldowner != null) {
            game.getScorecard().refreshScores(oldowner);
        }
        game.getScorecard().refreshScores(team);
    }

    /**
     * Gets all enemy links not of team
     * @param team
     * @return set of links
     */
    public Set<Line2D> getEnemyLinks(Team team) {
        Set<Line2D> result = new HashSet<>();
        if (getGameMgr().getGame(team) != null && beaconLinks.containsKey(getGameMgr().getGame(team))) {
            for (BeaconLink pair: beaconLinks.get(getGameMgr().getGame(team))) {
                if (!pair.getOwner().equals(team)) {
                    result.add(pair.getLine());
                }
            }
        }
        return result;
    }

    /**
     * Adds a block to the defense block register. Blocks around a beacon are automatically added.
     * @param location
     * @param beacon
     */
    public void addBeaconDefenseBlock(Location location, BeaconObj beacon) {
        addBeaconBaseBlock(location.getBlockX(), location.getBlockZ(), beacon);
    }

    /**
     * Adds a block to the defense block register. Blocks around a beacon are automatically added.
     * @param x
     * @param z
     * @param beacon
     */
    public void addBeaconBaseBlock(int x, int z, BeaconObj beacon) {
        Point2D point = new Point2D.Double(x,z);
        baseBlocks.put(point, beacon);
        Set<Point2D> points = baseBlocksInverse.get(beacon);
        if (points == null) {
            points = new HashSet<>();
        }
        points.add(point);
        baseBlocksInverse.put(beacon, points);
    }

    /**
     * Get the beacon associated with this defensive block
     * @param point
     * @return beacon or null if it doesn't exist
     */
    public BeaconObj getBeaconAt(Point2D point) {
        return baseBlocks.get(point);
    }

    /**
     * Get the beacon associated with this location. World and Y coord is ignored.
     * @param location location of the beacon
     * @return beacon or null if it doesn't exist
     */
    public BeaconObj getBeaconAt(Location location) {
        if (location == null) {
            return null;
        }
        Point2D point = new Point2D.Double(location.getBlockX(),location.getBlockZ());
        //getLogger().info("DEBUG: " + point);
        return baseBlocks.get(point);
    }

    /**
     * Gets all the plinth blocks at this beacon
     * @param beacon
     * @return Set of points
     */
    public Set<Point2D> getDefensesAtBeacon(BeaconObj beacon) {
        return baseBlocksInverse.get(beacon);
    }

    /**
     * Check if this block is above an owned beacon or above a defense
     * @param loc
     * @return
     */
    public boolean isAboveBeacon(Location loc) {
        Point2D point = new Point2D.Double(loc.getBlockX(),loc.getBlockZ());
        if (baseBlocks.containsKey(point)) {
            BeaconObj beacon = baseBlocks.get(point);
            // Check ownership
            if (beacon.getOwnership() == null) {
                return false;
            }
            // Check height - if block is lower than the beacon, then it's not a part of it
            return beacon.getY() <= loc.getBlockY();
            // It's a defense block
        }
        return false;
    }

    /**
     * Removed Beaconz renderers from maps when the plugin is disabled
     */
    public void removeMapRenderers() {
        for (Integer id : beaconMaps.keySet()) {
            MapView map = Bukkit.getMap(id);
            if (map != null) {
                for (MapRenderer renderer : map.getRenderers()) {
                    if (renderer instanceof TerritoryMapRenderer || renderer instanceof BeaconMap) {
                        map.removeRenderer(renderer);
                    }
                }
            }
        }
    }



    /**
     * Recalculates the score for game. Used when a beacon is lost because that could enable the opposition to
     * then make a new triangle that they could not before. Also used when loading plugin.
     * Note that all links should already be in place.
     */
    public void recalculateScore(Game game) {
        // Run through the beacon pairs
        if (!beaconLinks.isEmpty() && beaconLinks.get(game) != null) {
            // Sort in order of age
            Collections.sort(beaconLinks.get(game));
            // Build the score
            // Go through all the links in this game
            for (BeaconLink firstPoint: beaconLinks.get(game)) {
                // Go to all the beacons this beacon is linked to
                for (BeaconObj secondPoint : firstPoint.getBeacon1().getLinks()) {
                    // Check the next set of links
                    for (BeaconObj thirdPoint : secondPoint.getLinks()) {
                        // Run through the 3rd point's links and see if they include the 1st point
                        if (!thirdPoint.equals(firstPoint)) {
                            if (thirdPoint.equals(firstPoint.getBeacon2())) {
                                // We have a winner
                                try {
                                    // Result is true if the triangle is made okay, otherwise, don't make the link and return false
                                    getRegister().addTriangle(firstPoint.getBeacon1().getPoint(), secondPoint.getPoint(),
                                            thirdPoint.getPoint(), firstPoint.getOwner());
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                }
                                // There could be more than one, so continue
                            }
                        }
                    }
                }
            }
        }
    }
}
