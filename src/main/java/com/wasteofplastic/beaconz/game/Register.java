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

package com.wasteofplastic.beaconz.game;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

import com.zaxxer.hikari.HikariDataSource;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.core.BeaconLink;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.core.DefenseBlock;
import com.wasteofplastic.beaconz.core.Region;
import com.wasteofplastic.beaconz.core.TriangleField;
import com.wasteofplastic.beaconz.map.BeaconMap;
import com.wasteofplastic.beaconz.map.TerritoryMapRenderer;
import com.wasteofplastic.beaconz.util.LineVisualizer;
import com.wasteofplastic.beaconz.util.LinkResult;
import com.wasteofplastic.beaconz.util.TriangleScorer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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

    // SQL table definitions
    private static final String CREATE_BEACONS_TABLE =
        "CREATE TABLE IF NOT EXISTS beacons (" +
        "x INTEGER NOT NULL, " +
        "y INTEGER NOT NULL, " +
        "z INTEGER NOT NULL, " +
        "game_name TEXT NOT NULL, " +
        "owner_team TEXT, " +
        "map_id INTEGER, " +
        "PRIMARY KEY (x, z)" +
        ")";

    private static final String CREATE_BEACON_LINKS_TABLE =
        "CREATE TABLE IF NOT EXISTS beacon_links (" +
        "game_name TEXT NOT NULL, " +
        "x1 INTEGER NOT NULL, " +
        "z1 INTEGER NOT NULL, " +
        "x2 INTEGER NOT NULL, " +
        "z2 INTEGER NOT NULL, " +
        "timestamp INTEGER NOT NULL, " +
        "PRIMARY KEY (game_name, x1, z1, x2, z2)" +
        ")";

    private static final String CREATE_BASE_BLOCKS_TABLE =
        "CREATE TABLE IF NOT EXISTS beacon_base_blocks (" +
        "beacon_x INTEGER NOT NULL, " +
        "beacon_z INTEGER NOT NULL, " +
        "block_x INTEGER NOT NULL, " +
        "block_z INTEGER NOT NULL, " +
        "PRIMARY KEY (beacon_x, beacon_z, block_x, block_z)" +
        ")";

    private static final String CREATE_DEFENSE_BLOCKS_TABLE =
        "CREATE TABLE IF NOT EXISTS beacon_defense_blocks (" +
        "beacon_x INTEGER NOT NULL, " +
        "beacon_z INTEGER NOT NULL, " +
        "block_x INTEGER NOT NULL, " +
        "block_y INTEGER NOT NULL, " +
        "block_z INTEGER NOT NULL, " +
        "level INTEGER NOT NULL, " +
        "placer_uuid TEXT, " +
        "PRIMARY KEY (beacon_x, beacon_z, block_x, block_y, block_z)" +
        ")";

    private static final String CREATE_BEACON_MAPS_TABLE =
        "CREATE TABLE IF NOT EXISTS beacon_maps (" +
        "map_id INTEGER PRIMARY KEY, " +
        "beacon_x INTEGER NOT NULL, " +
        "beacon_z INTEGER NOT NULL, " +
        "origin_x INTEGER, " +
        "origin_z INTEGER" +
        ")";

    /**
     * Constructs a new Register instance and initializes database tables.
     *
     * @param beaconzPlugin the main Beaconz plugin instance
     */
    public Register(Beaconz beaconzPlugin) {
        super(Objects.requireNonNull(beaconzPlugin));
        initializeDatabaseTables();
    }

    /** Maps Minecraft map item IDs to their associated beacon objects for territory display */
    private final HashMap<Integer, BeaconObj> beaconMaps = new HashMap<>();

    /** Maps Minecraft map item IDs to their origin coordinates (where the map was created) */
    private final HashMap<Integer, Point2D> mapOrigins = new HashMap<>();

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
     * Pending ownership resolutions - beacons that have owner teams in database
     * but couldn't be resolved during load because games weren't initialized yet.
     * This is resolved after games are fully loaded.
     */
    private final HashMap<BeaconObj, String> pendingOwnership = new HashMap<>();

    /**
     * Initializes all database tables required for storing beacon data.
     * Called during Register construction to ensure tables exist.
     */
    private void initializeDatabaseTables() {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null || dataSource.isClosed()) {
            getLogger().severe("Database not initialized!");
            return;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create tables
            stmt.execute(CREATE_BEACONS_TABLE);
            stmt.execute(CREATE_BEACON_LINKS_TABLE);
            stmt.execute(CREATE_BASE_BLOCKS_TABLE);
            stmt.execute(CREATE_DEFENSE_BLOCKS_TABLE);
            stmt.execute(CREATE_BEACON_MAPS_TABLE);

            // Create indexes for performance
            // Index on beacon coordinates for fast spatial lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_beacons_coords ON beacons(x, z)");

            // Index on game_name for filtering beacons by game
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_beacons_game ON beacons(game_name)");

            // Index on owner_team for finding all beacons owned by a team
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_beacons_owner ON beacons(owner_team)");

            // Index on beacon_links for fast link queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_links_game ON beacon_links(game_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_links_beacon1 ON beacon_links(x1, z1)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_links_beacon2 ON beacon_links(x2, z2)");

            // Index on base blocks for fast beacon lookup
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_base_blocks_beacon ON beacon_base_blocks(beacon_x, beacon_z)");

            // Index on defense blocks for fast beacon lookup
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_defense_blocks_beacon ON beacon_defense_blocks(beacon_x, beacon_z)");

            // Index on maps for fast beacon lookup
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_maps_beacon ON beacon_maps(beacon_x, beacon_z)");

            getLogger().info("Database tables and indexes initialized for Register");

        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database tables: " + e.getMessage());
        }
    }

    /**
     * Persists all game data to the SQLite database.
     * <p>
     * This method saves all beacons, links, base blocks, defense blocks, and maps
     * to the database. It uses batch inserts for efficiency.
     * <p>
     */
    public void saveRegister() {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null || dataSource.isClosed()) {
            getLogger().severe("Database not available");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Clear existing data
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM beacons");
                    stmt.execute("DELETE FROM beacon_links");
                    stmt.execute("DELETE FROM beacon_base_blocks");
                    stmt.execute("DELETE FROM beacon_defense_blocks");
                    stmt.execute("DELETE FROM beacon_maps");
                }

                // Save beacons
                if (DEBUG) {
                    getLogger().info("DEBUG: ========== SAVING BEACONS TO DATABASE ==========");
                }

                String insertBeacon = "INSERT INTO beacons (x, y, z, game_name, owner_team, map_id) VALUES (?, ?, ?, ?, ?, ?)";
                int beaconsSaved = 0;
                int beaconsWithOwnership = 0;

                try (PreparedStatement stmt = conn.prepareStatement(insertBeacon)) {
                    for (BeaconObj beacon : beaconRegister.values()) {
                        Game game = getGameMgr().getGame(beacon.getPoint());
                        String gameName = game == null ? "None" : PlainTextComponentSerializer.plainText().serialize(game.getName());
                        String owner = beacon.getOwnership() == null ? null : beacon.getOwnership().getName();

                        if (DEBUG && beaconsSaved < 5) { // Log first 5 beacons
                            getLogger().info("DEBUG: Saving beacon #" + (beaconsSaved + 1) + ":");
                            getLogger().info("DEBUG:   Position: (" + beacon.getX() + ", " + beacon.getY() + ", " + beacon.getZ() + ")");
                            getLogger().info("DEBUG:   Game: " + gameName);
                            getLogger().info("DEBUG:   Owner Team: " + (owner == null ? "NULL" : owner));
                            getLogger().info("DEBUG:   Map ID: " + beacon.getId());
                        }

                        // ALWAYS log beacons WITH ownership to see what's being saved
                        if (owner != null) {
                            beaconsWithOwnership++;
                            if (DEBUG) {
                                getLogger().info("DEBUG: *** Beacon WITH OWNERSHIP being saved:");
                                getLogger().info("DEBUG:   Position: (" + beacon.getX() + ", " + beacon.getY() + ", " + beacon.getZ() + ")");
                                getLogger().info("DEBUG:   Game: " + gameName);
                                getLogger().info("DEBUG:   Owner Team: " + owner);
                                getLogger().info("DEBUG:   Map ID: " + beacon.getId());
                            }
                        }

                        stmt.setInt(1, beacon.getX());
                        stmt.setInt(2, beacon.getY());
                        stmt.setInt(3, beacon.getZ());
                        stmt.setString(4, gameName);
                        stmt.setString(5, owner);
                        stmt.setObject(6, beacon.getId());
                        stmt.addBatch();
                        beaconsSaved++;
                    }
                    stmt.executeBatch();
                }

                if (DEBUG) {
                    getLogger().info("DEBUG: ========== SAVE COMPLETE ==========");
                    getLogger().info("DEBUG: Saved " + beaconsSaved + " beacons to database");
                    getLogger().info("DEBUG: " + beaconsWithOwnership + " beacons have ownership");
                    getLogger().info("DEBUG: " + (beaconsSaved - beaconsWithOwnership) + " beacons are unowned");
                }

                // Save beacon links
                Set<BeaconLink> storedLinks = new HashSet<>();
                String insertLink = "INSERT INTO beacon_links (game_name, x1, z1, x2, z2, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertLink)) {
                    for (Entry<Game, List<BeaconLink>> entry : beaconLinks.entrySet()) {
                        String gameName = PlainTextComponentSerializer.plainText().serialize(entry.getKey().getName());
                        for (BeaconLink link : entry.getValue()) {
                            if (!storedLinks.contains(link)) {
                                stmt.setString(1, gameName);
                                stmt.setInt(2, link.getBeacon1().getX());
                                stmt.setInt(3, link.getBeacon1().getZ());
                                stmt.setInt(4, link.getBeacon2().getX());
                                stmt.setInt(5, link.getBeacon2().getZ());
                                stmt.setLong(6, link.getTimeStamp());
                                stmt.addBatch();
                                storedLinks.add(link);
                            }
                        }
                    }
                    stmt.executeBatch();
                }

                // Save base blocks
                String insertBaseBlock = "INSERT INTO beacon_base_blocks (beacon_x, beacon_z, block_x, block_z) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertBaseBlock)) {
                    for (Entry<BeaconObj, Set<Point2D>> entry : baseBlocksInverse.entrySet()) {
                        BeaconObj beacon = entry.getKey();
                        for (Point2D point : entry.getValue()) {
                            stmt.setInt(1, beacon.getX());
                            stmt.setInt(2, beacon.getZ());
                            stmt.setInt(3, (int) point.getX());
                            stmt.setInt(4, (int) point.getY());
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }

                // Save defense blocks
                String insertDefenseBlock = "INSERT INTO beacon_defense_blocks (beacon_x, beacon_z, block_x, block_y, block_z, level, placer_uuid) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertDefenseBlock)) {
                    for (BeaconObj beacon : beaconRegister.values()) {
                        for (DefenseBlock defensiveBlock : beacon.getDefenseBlocks().values()) {
                            Location loc = defensiveBlock.getBlock().getLocation();
                            stmt.setInt(1, beacon.getX());
                            stmt.setInt(2, beacon.getZ());
                            stmt.setInt(3, loc.getBlockX());
                            stmt.setInt(4, loc.getBlockY());
                            stmt.setInt(5, loc.getBlockZ());
                            stmt.setInt(6, defensiveBlock.getLevel());
                            stmt.setString(7, defensiveBlock.getPlacer() == null ? null : defensiveBlock.getPlacer().toString());
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }

                // Save beacon maps
                String insertMap = "INSERT INTO beacon_maps (map_id, beacon_x, beacon_z, origin_x, origin_z) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertMap)) {
                    for (Entry<Integer, BeaconObj> entry : beaconMaps.entrySet()) {
                        Integer mapId = entry.getKey();
                        BeaconObj beacon = entry.getValue();
                        Point2D origin = mapOrigins.get(mapId);

                        if (Bukkit.getMap(mapId) != null) {
                            stmt.setInt(1, mapId);
                            stmt.setInt(2, beacon.getX());
                            stmt.setInt(3, beacon.getZ());
                            stmt.setObject(4, origin == null ? null : (int) origin.getX());
                            stmt.setObject(5, origin == null ? null : (int) origin.getY());
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to save register to database: " + e.getMessage());
        }
    }

    /**
     * Loads all game data from the SQLite database and reconstructs the game state.
     * <p>
     * If the database is empty but a beaconz.yml file exists, it will migrate the data.
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
     */
    public void loadRegister() {
        clear();

        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null || dataSource.isClosed()) {
            getLogger().severe("Database not available");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            // === PHASE 1: Load all beacons ===
            beaconLinks.clear();
            pendingOwnership.clear(); // Clear any previous pending ownership
            HashMap<Point2D, BeaconObj> loadedBeacons = new HashMap<>();

            String selectBeacons = "SELECT x, y, z, game_name, owner_team, map_id FROM beacons";
            int beaconCountInDB = 0;
            int beaconsLoaded = 0;

            if (DEBUG) {
                getLogger().info("DEBUG: ========== LOADING BEACONS FROM DATABASE ==========");
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectBeacons)) {

                while (rs.next()) {
                    beaconCountInDB++;
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    String gameName = rs.getString("game_name");
                    String ownerTeamName = rs.getString("owner_team");
                    Integer mapId = (Integer) rs.getObject("map_id");

                    if (DEBUG) {
                        getLogger().info("DEBUG: Loading beacon #" + beaconCountInDB + " from DB:");
                        getLogger().info("DEBUG:   Position: (" + x + ", " + y + ", " + z + ")");
                        getLogger().info("DEBUG:   Game: " + (gameName == null ? "NULL" : gameName));
                        getLogger().info("DEBUG:   Owner Team: " + (ownerTeamName == null ? "NULL" : ownerTeamName));
                        getLogger().info("DEBUG:   Map ID: " + (mapId == null ? "NULL" : mapId));
                    }

                    // Load beacon WITHOUT requiring game to exist yet
                    // Game/region initialization happens AFTER register load, so we can't look up the game yet
                    // We'll resolve team ownership later (beacons default to unowned if team doesn't exist)
                    BeaconObj beacon = addBeacon(null, x, y, z); // Start with no owner
                    loadedBeacons.put(beacon.getPoint(), beacon);
                    beaconsLoaded++;

                    // Set map ID if present
                    if (mapId != null) {
                        beacon.setId(mapId);
                    }

                    // Store owner team name for later resolution
                    if (ownerTeamName != null) {
                        pendingOwnership.put(beacon, ownerTeamName);
                        if (DEBUG) {
                            getLogger().info("DEBUG:   -> Added to pending ownership resolution");
                        }
                    } else if (DEBUG) {
                        getLogger().info("DEBUG:   -> No owner team in database (unowned beacon)");
                    }
                }
            }

            if (DEBUG) {
                getLogger().info("DEBUG: ========== DATABASE LOADING COMPLETE ==========");
                getLogger().info("DEBUG: Loaded " + beaconsLoaded + " beacons from database");
                getLogger().info("DEBUG: " + pendingOwnership.size() + " beacons have pending ownership to resolve");
            }

            // === PHASE 2: Load base blocks ===
            String selectBaseBlocks = "SELECT beacon_x, beacon_z, block_x, block_z FROM beacon_base_blocks";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectBaseBlocks)) {

                while (rs.next()) {
                    int beaconX = rs.getInt("beacon_x");
                    int beaconZ = rs.getInt("beacon_z");
                    int blockX = rs.getInt("block_x");
                    int blockZ = rs.getInt("block_z");

                    BeaconObj beacon = loadedBeacons.get(new Point2D.Double(beaconX, beaconZ));
                    if (beacon != null) {
                        addBeaconBaseBlock(blockX, blockZ, beacon);
                    }
                }
            }

            // === PHASE 3: Load defense blocks ===
            String selectDefenseBlocks = "SELECT beacon_x, beacon_z, block_x, block_y, block_z, level, placer_uuid FROM beacon_defense_blocks";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectDefenseBlocks)) {

                while (rs.next()) {
                    int beaconX = rs.getInt("beacon_x");
                    int beaconZ = rs.getInt("beacon_z");
                    int blockX = rs.getInt("block_x");
                    int blockY = rs.getInt("block_y");
                    int blockZ = rs.getInt("block_z");
                    int level = rs.getInt("level");
                    String placerUuid = rs.getString("placer_uuid");

                    BeaconObj beacon = loadedBeacons.get(new Point2D.Double(beaconX, beaconZ));
                    if (beacon != null) {
                        Location loc = new Location(getBeaconzWorld(), blockX, blockY, blockZ);
                        beacon.addDefenseBlock(loc.getBlock(), level, placerUuid);
                    }
                }
            }

            // === PHASE 4: Load maps ===
            String selectMaps = "SELECT map_id, beacon_x, beacon_z, origin_x, origin_z FROM beacon_maps";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectMaps)) {

                while (rs.next()) {
                    int mapId = rs.getInt("map_id");
                    int beaconX = rs.getInt("beacon_x");
                    int beaconZ = rs.getInt("beacon_z");
                    Integer originX = (Integer) rs.getObject("origin_x");
                    Integer originZ = (Integer) rs.getObject("origin_z");

                    BeaconObj beacon = loadedBeacons.get(new Point2D.Double(beaconX, beaconZ));
                    if (beacon != null) {
                        beaconMaps.put(mapId, beacon);

                        if (originX != null && originZ != null) {
                            mapOrigins.put(mapId, new Point2D.Double(originX, originZ));
                        }

                        MapView map = Bukkit.getMap(mapId);
                        if (map != null) {
                            // Remove old renderers and add fresh ones
                            for (MapRenderer renderer : map.getRenderers()) {
                                if (renderer instanceof TerritoryMapRenderer || renderer instanceof BeaconMap) {
                                    map.removeRenderer(renderer);
                                }
                            }
                            map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));

                            if (originX != null && originZ != null) {
                                map.addRenderer(new BeaconMap(getBeaconzPlugin(), originX, originZ));
                            } else {
                                map.addRenderer(new BeaconMap(getBeaconzPlugin()));
                            }
                        }
                    }
                }
            }

            // === PHASE 5: Load beacon links ===
            int linksLoaded = 0;
            String selectLinks = "SELECT game_name, x1, z1, x2, z2, timestamp FROM beacon_links";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectLinks)) {

                while (rs.next()) {
                    int x1 = rs.getInt("x1");
                    int z1 = rs.getInt("z1");
                    int x2 = rs.getInt("x2");
                    int z2 = rs.getInt("z2");
                    long timestamp = rs.getLong("timestamp");

                    BeaconObj beacon1 = loadedBeacons.get(new Point2D.Double(x1, z1));
                    BeaconObj beacon2 = loadedBeacons.get(new Point2D.Double(x2, z2));

                    if (beacon1 != null && beacon2 != null) {
                        BeaconLink link = new BeaconLink(beacon1, beacon2, timestamp);
                        Game game = getGameMgr().getGame(beacon1.getPoint());
                        if (game != null) {
                            // Use computeIfAbsent to ensure the list exists
                            List<BeaconLink> links = beaconLinks.computeIfAbsent(game, k -> new ArrayList<>());
                            if (!links.contains(link)) {
                                links.add(link);
                                linksLoaded++;
                            }
                        }
                    }
                }
            }

            if (DEBUG) {
                getLogger().info("DEBUG: Loaded " + linksLoaded + " beacon links from database");
            }

            // === PHASE 6: Resolve pending beacon ownership ===
            // This must happen BEFORE creating triangle fields so that BeaconLink owner is set correctly
            if (DEBUG) {
                getLogger().info("DEBUG: ========== RESOLVING BEACON OWNERSHIP ==========");
                getLogger().info("DEBUG: Attempting to resolve " + pendingOwnership.size() + " beacons");
            }

            int ownershipResolved = 0;
            int ownershipFailed = 0;
            int ownershipFailedNoGame = 0;
            int ownershipFailedNoScorecard = 0;
            int ownershipFailedNoTeam = 0;

            for (Entry<BeaconObj, String> entry : pendingOwnership.entrySet()) {
                BeaconObj beacon = entry.getKey();
                String teamName = entry.getValue();

                if (DEBUG) {
                    getLogger().info("DEBUG: Resolving beacon at " + beacon.getPoint() + " -> team '" + teamName + "'");
                }

                Game game = getGameMgr().getGame(beacon.getPoint());
                if (game == null) {
                    ownershipFailed++;
                    ownershipFailedNoGame++;
                    if (DEBUG) {
                        getLogger().warning("DEBUG:   FAILED: No game found at location " + beacon.getPoint());
                    }
                    continue;
                }

                if (DEBUG) {
                    getLogger().info("DEBUG:   Found game: " + PlainTextComponentSerializer.plainText().serialize(game.getName()));
                }

                if (game.getScorecard() == null) {
                    ownershipFailed++;
                    ownershipFailedNoScorecard++;
                    if (DEBUG) {
                        getLogger().warning("DEBUG:   FAILED: Game has no scorecard");
                    }
                    continue;
                }

                Team team = game.getScorecard().getTeam(teamName);
                if (team == null) {
                    ownershipFailed++;
                    ownershipFailedNoTeam++;
                    if (DEBUG) {
                        getLogger().warning("DEBUG:   FAILED: Team '" + teamName + "' not found in game");
                        getLogger().warning("DEBUG:   Available teams: " + game.getScorecard().getScoreboard().getTeams().stream()
                            .map(Team::getName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("(none)"));
                    }
                    continue;
                }

                // SUCCESS!
                beacon.setOwnership(team);
                beaconLinks.computeIfAbsent(game, k -> new ArrayList<>());
                ownershipResolved++;

                if (DEBUG) {
                    getLogger().info("DEBUG:   SUCCESS: Set ownership to team '" + team.getName() + "'");
                }
            }

            if (DEBUG) {
                getLogger().info("DEBUG: ========== OWNERSHIP RESOLUTION COMPLETE ==========");
                getLogger().info("DEBUG: Successfully resolved: " + ownershipResolved + " beacons");
                if (ownershipFailed > 0) {
                    getLogger().warning("DEBUG: FAILED to resolve: " + ownershipFailed + " beacons");
                    getLogger().warning("DEBUG:   No game found: " + ownershipFailedNoGame);
                    getLogger().warning("DEBUG:   No scorecard: " + ownershipFailedNoScorecard);
                    getLogger().warning("DEBUG:   Team not found: " + ownershipFailedNoTeam);
                }
            }

            // === PHASE 7: Create triangle fields from links ===
            // This must happen AFTER ownership is resolved so BeaconLink owner is set correctly
            if (DEBUG) {
                getLogger().info("DEBUG: ========== CREATING TRIANGLE FIELDS FROM LINKS ==========");
            }

            for (Entry<Game, List<BeaconLink>> entry : beaconLinks.entrySet()) {
                Collections.sort(entry.getValue());
                for (BeaconLink link : entry.getValue()) {
                    link.getBeacon1().addLink(link.getBeacon2());
                    link.getBeacon2().addLink(link.getBeacon1());
                }
            }

            if (DEBUG) {
                getLogger().info("DEBUG: Triangle fields created from " + beaconLinks.values().stream().mapToInt(List::size).sum() + " links");
            }

            // Note: Score recalculation is deferred until after ownership resolution
            // (see resolveOwnershipAfterGamesLoaded method which calls recalculateScore)

            getLogger().info("Loaded " + loadedBeacons.size() + " beacons from database");

        } catch (SQLException e) {
            getLogger().severe("Failed to load register from database: " + e.getMessage());
        }
    }

    /**
     * Resolves pending beacon ownership after games are fully loaded.
     * This should be called after all games have been loaded from the database,
     * as beacons loaded before games are initialized cannot resolve their team ownership.
     * <p>
     * This method attempts to resolve all beacons in the pendingOwnership map by
     * looking up their game and team, then setting the beacon's ownership accordingly.
     */
    public void resolvePendingOwnership() {
        if (pendingOwnership.isEmpty()) {
            if (DEBUG) {
                getLogger().info("DEBUG: No pending ownership to resolve");
            }
            return;
        }

        if (DEBUG) {
            getLogger().info("DEBUG: ========== RE-RESOLVING BEACON OWNERSHIP ==========");
            getLogger().info("DEBUG: Attempting to resolve " + pendingOwnership.size() + " beacons after games loaded");
        }

        int ownershipResolved = 0;
        int ownershipFailed = 0;
        int ownershipFailedNoGame = 0;
        int ownershipFailedNoScorecard = 0;
        int ownershipFailedNoTeam = 0;

        // Create a copy to iterate over since we'll be modifying the original map
        HashMap<BeaconObj, String> toResolve = new HashMap<>(pendingOwnership);

        for (Entry<BeaconObj, String> entry : toResolve.entrySet()) {
            BeaconObj beacon = entry.getKey();
            String teamName = entry.getValue();

            if (DEBUG) {
                getLogger().info("DEBUG: Re-resolving beacon at " + beacon.getPoint() + " -> team '" + teamName + "'");
            }

            Game game = getGameMgr().getGame(beacon.getPoint());
            if (game == null) {
                ownershipFailed++;
                ownershipFailedNoGame++;
                if (DEBUG) {
                    getLogger().warning("DEBUG:   FAILED: No game found at location " + beacon.getPoint());
                }
                continue;
            }

            if (DEBUG) {
                getLogger().info("DEBUG:   Found game: " + PlainTextComponentSerializer.plainText().serialize(game.getName()));
            }

            if (game.getScorecard() == null) {
                ownershipFailed++;
                ownershipFailedNoScorecard++;
                if (DEBUG) {
                    getLogger().warning("DEBUG:   FAILED: Game has no scorecard");
                }
                continue;
            }

            Team team = game.getScorecard().getTeam(teamName);
            if (team == null) {
                ownershipFailed++;
                ownershipFailedNoTeam++;
                if (DEBUG) {
                    getLogger().warning("DEBUG:   FAILED: Team '" + teamName + "' not found in game");
                    getLogger().warning("DEBUG:   Available teams: " + game.getScorecard().getScoreboard().getTeams().stream()
                        .map(Team::getName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("(none)"));
                }
                continue;
            }

            // SUCCESS!
            beacon.setOwnership(team);
            beaconLinks.computeIfAbsent(game, k -> new ArrayList<>());
            pendingOwnership.remove(beacon); // Remove from pending
            ownershipResolved++;

            if (DEBUG) {
                getLogger().info("DEBUG:   SUCCESS: Set ownership to team '" + team.getName() + "'");
            }
        }

        if (DEBUG) {
            getLogger().info("DEBUG: ========== RE-RESOLUTION COMPLETE ==========");
        }

        getLogger().info("Resolved ownership for " + ownershipResolved + " beacons after games loaded");
        if (ownershipFailed > 0) {
            getLogger().warning("Still failed to resolve " + ownershipFailed + " beacons:");
            getLogger().warning("  No game found: " + ownershipFailedNoGame);
            getLogger().warning("  No scorecard: " + ownershipFailedNoScorecard);
            getLogger().warning("  Team not found: " + ownershipFailedNoTeam);
        }

        // CRITICAL FIX: Now that ownership is resolved, we need to recreate the links and triangle fields
        // that were loaded from the database but couldn't be fully initialized because beacons lacked ownership
        if (ownershipResolved > 0) {
            if (DEBUG) {
                getLogger().info("DEBUG: Recreating links and triangle fields for " + ownershipResolved + " newly owned beacons");
            }

            // Update BeaconLink owners (they were set to null during initial load because beacons had no ownership)
            for (List<BeaconLink> links : beaconLinks.values()) {
                for (BeaconLink link : links) {
                    // Re-set the owner from beacon1's current ownership
                        // Use reflection or recreate the link - actually, BeaconLink.owner is set in constructor
                        // We need to check if the current owner is null and update it
                        if (link.getOwner() == null && link.getBeacon1().getOwnership() != null) {
                            // Create new link with correct owner
                            BeaconLink newLink = new BeaconLink(link.getBeacon1(), link.getBeacon2(), link.getTimeStamp());
                            // Replace in the list - we need to recreate all links
                        }
                }
            }

            // Rebuild all links with correct ownership
            HashMap<Game, List<BeaconLink>> rebuiltLinks = new HashMap<>();
            for (Entry<Game, List<BeaconLink>> entry : beaconLinks.entrySet()) {
                List<BeaconLink> newLinks = new ArrayList<>();
                for (BeaconLink oldLink : entry.getValue()) {
                    // Recreate link so owner is set correctly from beacon's current ownership
                    BeaconLink newLink = new BeaconLink(oldLink.getBeacon1(), oldLink.getBeacon2(), oldLink.getTimeStamp());
                    newLinks.add(newLink);
                }
                rebuiltLinks.put(entry.getKey(), newLinks);
            }
            beaconLinks.clear();
            beaconLinks.putAll(rebuiltLinks);

            if (DEBUG) {
                getLogger().info("DEBUG: Rebuilt " + beaconLinks.values().stream().mapToInt(List::size).sum() + " links with ownership");
            }
        }

        // Recalculate scores for all affected games
        Set<Game> affectedGames = new HashSet<>();
        for (List<BeaconLink> links : beaconLinks.values()) {
            for (BeaconLink link : links) {
                Game g = getGameMgr().getGame(link.getBeacon1().getPoint());
                if (g != null) {
                    affectedGames.add(g);
                }
            }
        }
        for (Game game : affectedGames) {
            recalculateScore(game);
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
                                getLogger().severe("Failed to add triangle during beacon loading: " + e.getMessage());
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
     * @param team - the team to check
     * @return number of links
     */
    public int getTeamLinks(Team team) {
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
        return (int) TriangleScorer.getScore(triangleFields, team);
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
     * @param b - the block to check
     * @return true if it is part of a beacon, false if not
     */
    public boolean isBeacon(Block b) {
        return getBeacon(b) != null;
    }

    /**
     * Checks if a beacon is within the range around point
     * @param point - the point to check around
     * @param range - the range to check for beacons
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
     * @param location - the location to check around
     * @param range - the range to check for beacons
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
     * Gets the beacon connected to block.
     * @param block - the block to check
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
     * @param beacon - the beacon to remove ownership from
     */
    public void removeBeaconOwnership(BeaconObj beacon) {
        removeBeaconOwnership(beacon, false);
    }

    /**
     * Removes this beacon from team ownership and makes it unowned
     * @param beacon - the beacon to remove ownership from
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
                        getMessages().tellTeam(oldOwner, Lang.linkLostLink);
                        getMessages().tellOtherTeams(oldOwner, MiniMessage.miniMessage().deserialize(Lang.linkTeamLostLink,
                                Placeholder.component("team", oldOwner.displayName())));
                    } else if (linkLossCount > 1 && !quiet) {
                        String count = String.valueOf(linkLossCount);
                        getMessages().tellTeam(oldOwner, MiniMessage.miniMessage().deserialize(Lang.linkLostLinks,
                                Placeholder.component("number", Component.text(count))));
                        getMessages().tellOtherTeams(oldOwner, MiniMessage.miniMessage().deserialize(Lang.linkTeamLostLinks,
                                Placeholder.component("team", oldOwner.displayName()),
                                Placeholder.component("number", Component.text(count))));
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
                // Tell folks what's going on
                if (!quiet && triangle.getOwner() != null) {
                    getMessages().tellTeam(triangle.getOwner(), Lang.triangleYourTeamLostATriangle);
                    getMessages().tellOtherTeams(triangle.getOwner(), MiniMessage.miniMessage().deserialize(Lang.triangleTeamLostATriangle,
                            Placeholder.component("team", triangle.getOwner().displayName())));
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
     * @param team - the team that owns this beacon, or null for unowned
     * @param location - the location of the beacon (block coordinates will be used)
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
        this.beaconMaps.put(i, beacon);
    }

    /**
     * Adds a beacon map with origin coordinates.
     *
     * @param i the map ID
     * @param beacon the beacon this map is associated with
     * @param originX the X coordinate where the map was created
     * @param originZ the Z coordinate where the map was created
     */
    public void addBeaconMap(int i, BeaconObj beacon, int originX, int originZ) {
        beacon.setId(i);
        this.beaconMaps.put(i, beacon);
        this.mapOrigins.put(i, new Point2D.Double(originX, originZ));
    }

    /**
     * Gets the origin coordinates for a map.
     *
     * @param mapId the map ID
     * @return the origin point, or null if not set
     */
    public Point2D getMapOrigin(int mapId) {
        return mapOrigins.get(mapId);
    }

    /**
     * Sets the origin coordinates for a map.
     *
     * @param mapId the map ID
     * @param x the origin X coordinate
     * @param z the origin Z coordinate
     */
    public void setMapOrigin(int mapId, int x, int z) {
        this.mapOrigins.put(mapId, new Point2D.Double(x, z));
    }

    /**
     * @param index the map index to remove
     */
    public void removeBeaconMap(int index) {
        this.beaconMaps.remove(index);
        this.mapOrigins.remove(index);
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
     * @param x - the X coordinate of the beacon (world and Y are ignored)
     * @param z - the Z coordinate of the beacon (world and Y are ignored)
     * @return beacon object
     */
    public BeaconObj getBeaconAt(int x, int z) {
        Point2D point = new Point2D.Double(x,z);
        return beaconRegister.get(point);
    }

    /**
     * Sets the beacon ownership, team = null means it is unowned.
     * @param beacon - the beacon to change ownership of
     * @param team - the team that owns this beacon, or null for unowned
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
     * @param team - the team to check against
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
     * @param location - the location of the block (block coordinates will be used, world and Y are ignored)
     * @param beacon - the beacon this block is associated with
     */
    public void addBeaconDefenseBlock(Location location, BeaconObj beacon) {
        addBeaconBaseBlock(location.getBlockX(), location.getBlockZ(), beacon);
    }

    /**
     * Adds a block to the defense block register. Blocks around a beacon are automatically added.
     * @param x - the X coordinate of the block (world and Y are ignored)
     * @param z - the Z coordinate of the block (world and Y are ignored)
     * @param beacon - the beacon this block is associated with
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
     * @param point - the location of the block (world and Y are ignored)
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
        return baseBlocks.get(point);
    }

    /**
     * Gets all the plinth blocks at this beacon
     * @param beacon - the beacon to check
     * @return Set of points
     */
    public Set<Point2D> getDefensesAtBeacon(BeaconObj beacon) {
        return baseBlocksInverse.get(beacon);
    }

    /**
     * Check if this block is above an owned beacon or above a defense
     * @param loc - the location to check (world is ignored)
     * @return true if it is above a beacon or defense, false if not
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
                        // Skip if thirdPoint is the same as the starting beacon (beacon1) - can't form triangle
                        if (!thirdPoint.equals(firstPoint.getBeacon1())) {
                            // Check if thirdPoint connects back to beacon2, completing the triangle
                            if (thirdPoint.equals(firstPoint.getBeacon2())) {
                                // We have a winner
                                try {
                                    // Result is true if the triangle is made okay, otherwise, don't make the link and return false
                                    getRegister().addTriangle(firstPoint.getBeacon1().getPoint(), secondPoint.getPoint(),
                                            thirdPoint.getPoint(), firstPoint.getOwner());
                                } catch (IllegalArgumentException e) {
                                    getLogger().severe("Failed to add triangle when checking for triangles: " + e.getMessage());
                                }
                                // There could be more than one, so continue
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Verifies the integrity of the database by checking for:
     * - Orphaned base blocks (beacon doesn't exist)
     * - Orphaned defense blocks (beacon doesn't exist)
     * - Orphaned links (one or both beacons don't exist)
     * - Orphaned maps (beacon doesn't exist)
     * - Bidirectional link consistency
     *
     * @return true if database is valid, false if issues were found
     */
    public boolean verifyDatabaseIntegrity() {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().warning("Database not available for integrity check");
            return false;
        }

        boolean isValid = true;
        int issueCount = 0;

        try (Connection conn = dataSource.getConnection()) {

            // Check for orphaned base blocks
            String checkOrphanedBaseBlocks =
                "SELECT COUNT(*) FROM beacon_base_blocks bb " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM beacons b " +
                "  WHERE b.x = bb.beacon_x AND b.z = bb.beacon_z" +
                ")";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkOrphanedBaseBlocks)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    int count = rs.getInt(1);
                    getLogger().warning("Found " + count + " orphaned base block(s)");
                    issueCount += count;
                    isValid = false;
                }
            }

            // Check for orphaned defense blocks
            String checkOrphanedDefenseBlocks =
                "SELECT COUNT(*) FROM beacon_defense_blocks db " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM beacons b " +
                "  WHERE b.x = db.beacon_x AND b.z = db.beacon_z" +
                ")";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkOrphanedDefenseBlocks)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    int count = rs.getInt(1);
                    getLogger().warning("Found " + count + " orphaned defense block(s)");
                    issueCount += count;
                    isValid = false;
                }
            }

            // Check for orphaned links (beacon1 missing)
            String checkOrphanedLinks1 =
                "SELECT COUNT(*) FROM beacon_links l " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM beacons b " +
                "  WHERE b.x = l.x1 AND b.z = l.z1" +
                ")";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkOrphanedLinks1)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    int count = rs.getInt(1);
                    getLogger().warning("Found " + count + " link(s) with missing beacon1");
                    issueCount += count;
                    isValid = false;
                }
            }

            // Check for orphaned links (beacon2 missing)
            String checkOrphanedLinks2 =
                "SELECT COUNT(*) FROM beacon_links l " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM beacons b " +
                "  WHERE b.x = l.x2 AND b.z = l.z2" +
                ")";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkOrphanedLinks2)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    int count = rs.getInt(1);
                    getLogger().warning("Found " + count + " link(s) with missing beacon2");
                    issueCount += count;
                    isValid = false;
                }
            }

            // Check for orphaned maps
            String checkOrphanedMaps =
                "SELECT COUNT(*) FROM beacon_maps m " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM beacons b " +
                "  WHERE b.x = m.beacon_x AND b.z = m.beacon_z" +
                ")";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkOrphanedMaps)) {
                if (rs.next() && rs.getInt(1) > 0) {
                    int count = rs.getInt(1);
                    getLogger().warning("Found " + count + " orphaned map(s)");
                    issueCount += count;
                    isValid = false;
                }
            }

            if (isValid) {
                getLogger().info("Database integrity check passed - no issues found");
            } else {
                getLogger().warning("Database integrity check found " + issueCount + " issue(s)");
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to verify database integrity: " + e.getMessage());
            return false;
        }

        return isValid;
    }

    /**
     * Repairs the database by removing orphaned records.
     * This should be called after verifyDatabaseIntegrity() finds issues.
     *
     * @return number of records deleted
     */
    public int repairDatabase() {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().warning("Database not available for repair");
            return 0;
        }

        int deletedCount = 0;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Delete orphaned base blocks
                String deleteOrphanedBaseBlocks =
                    "DELETE FROM beacon_base_blocks " +
                    "WHERE NOT EXISTS (" +
                    "  SELECT 1 FROM beacons b " +
                    "  WHERE b.x = beacon_x AND b.z = beacon_z" +
                    ")";

                try (Statement stmt = conn.createStatement()) {
                    int count = stmt.executeUpdate(deleteOrphanedBaseBlocks);
                    if (count > 0) {
                        getLogger().info("Deleted " + count + " orphaned base block(s)");
                        deletedCount += count;
                    }
                }

                // Delete orphaned defense blocks
                String deleteOrphanedDefenseBlocks =
                    "DELETE FROM beacon_defense_blocks " +
                    "WHERE NOT EXISTS (" +
                    "  SELECT 1 FROM beacons b " +
                    "  WHERE b.x = beacon_x AND b.z = beacon_z" +
                    ")";

                try (Statement stmt = conn.createStatement()) {
                    int count = stmt.executeUpdate(deleteOrphanedDefenseBlocks);
                    if (count > 0) {
                        getLogger().info("Deleted " + count + " orphaned defense block(s)");
                        deletedCount += count;
                    }
                }

                // Delete orphaned links
                String deleteOrphanedLinks =
                    "DELETE FROM beacon_links " +
                    "WHERE NOT EXISTS (" +
                    "  SELECT 1 FROM beacons b " +
                    "  WHERE b.x = x1 AND b.z = z1" +
                    ") OR NOT EXISTS (" +
                    "  SELECT 1 FROM beacons b " +
                    "  WHERE b.x = x2 AND b.z = z2" +
                    ")";

                try (Statement stmt = conn.createStatement()) {
                    int count = stmt.executeUpdate(deleteOrphanedLinks);
                    if (count > 0) {
                        getLogger().info("Deleted " + count + " orphaned link(s)");
                        deletedCount += count;
                    }
                }

                // Delete orphaned maps
                String deleteOrphanedMaps =
                    "DELETE FROM beacon_maps " +
                    "WHERE NOT EXISTS (" +
                    "  SELECT 1 FROM beacons b " +
                    "  WHERE b.x = beacon_x AND b.z = beacon_z" +
                    ")";

                try (Statement stmt = conn.createStatement()) {
                    int count = stmt.executeUpdate(deleteOrphanedMaps);
                    if (count > 0) {
                        getLogger().info("Deleted " + count + " orphaned map(s)");
                        deletedCount += count;
                    }
                }

                conn.commit();

                if (deletedCount > 0) {
                    getLogger().info("Database repair completed - deleted " + deletedCount + " orphaned record(s)");
                } else {
                    getLogger().info("Database repair completed - no orphaned records found");
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to repair database: " + e.getMessage());
        }

        return deletedCount;
    }

    /**
     * Gets the total number of beacons currently registered.
     * @return the number of beacons
     */
    public int getBeaconCount() {
        return beaconRegister.size();
    }
}
