package com.wasteofplastic.beaconz.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.beaconz.Beaconz;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Player name to UUID database using SQLite via HikariCP connection pool.
 * Maintains an in-memory cache for fast lookups with background database persistence.
 * Supports migration from legacy file-based format.
 */
public class TinyDB {
    private final Beaconz plugin;
    private final ConcurrentHashMap<String, UUID> cache;
    private final HikariDataSource dataSource;
    private final Path legacyDatabasePath;
    private volatile boolean saving = false;

    private static final String TABLE_NAME = "player_names";
    private static final String CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
        "name TEXT PRIMARY KEY NOT NULL, " +
        "uuid TEXT NOT NULL" +
        ")";
    private static final String INSERT_OR_REPLACE =
        "INSERT OR REPLACE INTO " + TABLE_NAME + " (name, uuid) VALUES (?, ?)";
    private static final String SELECT_BY_NAME =
        "SELECT uuid FROM " + TABLE_NAME + " WHERE name = ?";
    private static final String SELECT_ALL =
        "SELECT name, uuid FROM " + TABLE_NAME;
    private static final String COUNT_ALL =
        "SELECT COUNT(*) FROM " + TABLE_NAME;

    public TinyDB(Beaconz plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.dataSource = plugin.getDataSource();
        this.legacyDatabasePath = plugin.getDataFolder().toPath().resolve("name-uuid.jsonl");

        if (this.dataSource == null) {
            plugin.getLogger().severe("Database not initialized! TinyDB will not function properly.");
            return;
        }

        // Create the table if it doesn't exist
        try {
            createTable();

            // Try to migrate from legacy file format first
            migrateLegacyData();

            // Load existing data into cache on startup
            loadCache();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize player name database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the player_names table if it doesn't exist
     */
    private void createTable() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE);
        }
    }

    /**
     * Migrates data from legacy file format to SQLite database
     */
    private void migrateLegacyData() {
        if (!Files.exists(legacyDatabasePath)) {
            return;
        }

        plugin.getLogger().info("Found legacy player name database, migrating to SQLite...");

        try (Stream<String> lines = Files.lines(legacyDatabasePath);
             Connection conn = dataSource.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(INSERT_OR_REPLACE)) {
                lines.forEach(line -> {
                    String[] parts = line.split("\t");
                    if (parts.length == 2) {
                        try {
                            UUID uuid = UUID.fromString(parts[1]);
                            stmt.setString(1, parts[0].toLowerCase());
                            stmt.setString(2, uuid.toString());
                            stmt.addBatch();
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in legacy database: " + line);
                        } catch (SQLException e) {
                            plugin.getLogger().warning("Failed to migrate entry: " + line);
                        }
                    }
                });

                stmt.executeBatch();
                conn.commit();

                plugin.getLogger().info("Successfully migrated legacy player name database");

                // Rename the old file to indicate it's been migrated
                try {
                    Files.move(legacyDatabasePath,
                              legacyDatabasePath.resolveSibling("name-uuid.jsonl.migrated"));
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not rename legacy database file: " + e.getMessage());
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read legacy database file: " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to migrate legacy database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all entries from database into cache on startup
     */
    private void loadCache() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {

            while (rs.next()) {
                String name = rs.getString("name");
                String uuidStr = rs.getString("uuid");
                try {
                    cache.put(name.toLowerCase(), UUID.fromString(uuidStr));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in database for player '" + name + "': " + uuidStr);
                }
            }

            plugin.getLogger().info("Loaded " + cache.size() + " player names from database");
        }
    }

    // ...existing code...

    /**
     * Saves the cache to the database asynchronously
     */
    public void asyncSaveDB() {
        if (saving) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                saveDB();
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Saves all cached entries to the database
     */
    public void saveDB() {
        if (dataSource == null) {
            return;
        }

        saving = true;

        try (Connection conn = dataSource.getConnection()) {
            // Disable auto-commit for batch insert
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(INSERT_OR_REPLACE)) {
                for (var entry : cache.entrySet()) {
                    stmt.setString(1, entry.getKey());
                    stmt.setString(2, entry.getValue().toString());
                    stmt.addBatch();
                }

                stmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save player name database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            saving = false;
        }
    }

    /**
     * Saves the player name to the database. Case insensitive!
     */
    public void savePlayerName(String playerName, UUID playerUUID) {
        if (playerName == null || playerUUID == null) {
            return;
        }

        String nameLower = playerName.toLowerCase();
        cache.put(nameLower, playerUUID);

        // Save to database in background
        if (dataSource != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (Connection conn = dataSource.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(INSERT_OR_REPLACE)) {
                        stmt.setString(1, nameLower);
                        stmt.setString(2, playerUUID.toString());
                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Failed to save player name '" + playerName + "': " + e.getMessage());
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    /**
     * Gets the UUID for this player name or null if not known. Case insensitive!
     */
    public UUID getPlayerUUID(String playerName) {
        if (playerName == null) {
            return null;
        }
        return cache.get(playerName.toLowerCase());
    }

    /**
     * Gets the number of cached entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Checks if a player name is in the database
     */
    public boolean hasPlayer(String playerName) {
        if (playerName == null) {
            return false;
        }
        return cache.containsKey(playerName.toLowerCase());
    }
}
