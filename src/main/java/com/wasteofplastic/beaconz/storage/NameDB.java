package com.wasteofplastic.beaconz.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import com.wasteofplastic.beaconz.Beaconz;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Player name to UUID database using SQLite via HikariCP connection pool.
 * Simple database lookup without caching.
 */
public class NameDB {
    private final Beaconz plugin;
    private final HikariDataSource dataSource;

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
    private static final String COUNT_ALL =
        "SELECT COUNT(*) FROM " + TABLE_NAME;

    public NameDB(Beaconz plugin) {
        this.plugin = plugin;
        this.dataSource = plugin.getDataSource();

        if (this.dataSource == null) {
            plugin.getLogger().severe("Database not initialized! NameDB will not function properly.");
            return;
        }

        // Create the table if it doesn't exist
        try {
            createTable();
            plugin.getLogger().info("Loaded " + size() + " player names from database");
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
     * Saves the player name to the database. Case insensitive!
     */
    public void savePlayerName(String playerName, UUID playerUUID) {
        if (playerName == null || playerUUID == null || dataSource == null) {
            return;
        }

        String nameLower = playerName.toLowerCase();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_OR_REPLACE)) {
            stmt.setString(1, nameLower);
            stmt.setString(2, playerUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player name '" + playerName + "': " + e.getMessage());
        }
    }

    /**
     * Gets the UUID for this player name or null if not known. Case insensitive!
     */
    public UUID getPlayerUUID(String playerName) {
        if (playerName == null || dataSource == null) {
            return null;
        }

        String nameLower = playerName.toLowerCase();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_NAME)) {
            stmt.setString(1, nameLower);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    try {
                        return UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in database for player '" + playerName + "': " + uuidStr);
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get UUID for player '" + playerName + "': " + e.getMessage());
        }

        return null;
    }

    /**
     * Gets the number of entries in the database
     */
    public int size() {
        if (dataSource == null) {
            return 0;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_ALL)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get player name count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Checks if a player name is in the database
     */
    public boolean hasPlayer(String playerName) {
        return getPlayerUUID(playerName) != null;
    }
}
