/*
 * Copyright (c) 2016 tastybento
 *
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

package com.wasteofplastic.beaconz.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.listeners.BeaconLinkListener;
import com.zaxxer.hikari.HikariDataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Enables inventory switching between games. Handles food, experience and spawn points.
 * Uses SQLite database for persistence
 * @author tastybento
 *
 */
public class BeaconzStore extends BeaconzPluginDependent {
    private static final boolean DEBUG = false;

    public BeaconzStore(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        initializeDatabase();
    }

    /**
     * Initializes the player_inventories database table.
     * Creates the table if it doesn't exist with all necessary columns.
     */
    private void initializeDatabase() {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! BeaconzStore will not function properly.");
            return;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create player_inventories table
            String createTable = "CREATE TABLE IF NOT EXISTS player_inventories (" +
                "player_uuid TEXT NOT NULL, " +
                "game_name TEXT NOT NULL, " +
                "inventory BLOB, " +
                "health REAL NOT NULL DEFAULT 20.0, " +
                "food INTEGER NOT NULL DEFAULT 20, " +
                "exp INTEGER NOT NULL DEFAULT 0, " +
                "location_world TEXT, " +
                "location_x REAL, " +
                "location_y REAL, " +
                "location_z REAL, " +
                "location_yaw REAL, " +
                "location_pitch REAL, " +
                "PRIMARY KEY (player_uuid, game_name)" +
                ")";

            stmt.execute(createTable);

            // Create indexes for performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_inv_game ON player_inventories(game_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_inv_uuid ON player_inventories(player_uuid)");

            getLogger().info("Database table initialized for player inventories");

        } catch (SQLException e) {
            getLogger().severe("Failed to initialize player_inventories database table: " + e.getMessage());
        }
    }

    /**
     * Gets items for world. Changes the inventory of player immediately.
     * @param player - the player
     * @param gameName - the game name for the inventory being retrieved
     * @return last location of the player in the game or null if there is none
     */
    public Location getInventory(Player player, String gameName) {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot get inventory for " + player.getName());
            return null;
        }

        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT inventory, health, food, exp, location_world, location_x, location_y, location_z, " +
                        "location_yaw, location_pitch FROM player_inventories WHERE player_uuid = ? AND game_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, gameName);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Restore inventory
                        byte[] inventoryData = rs.getBytes("inventory");
                        if (inventoryData != null) {
                            try (ByteArrayInputStream bis = new ByteArrayInputStream(inventoryData);
                                 BukkitObjectInputStream bois = new BukkitObjectInputStream(bis)) {
                                ItemStack[] items = (ItemStack[]) bois.readObject();
                                player.getInventory().setContents(items);
                            } catch (Exception e) {
                                getLogger().warning("Failed to deserialize inventory for " + player.getName() + ": " + e.getMessage());
                            }
                        }

                        // Restore health
                        double health = rs.getDouble("health");
                        if (health > 20D) health = 20D;
                        if (health <= 0D) health = 1D;
                        player.setHealth(health);

                        // Restore food
                        int food = rs.getInt("food");
                        if (food > 20) food = 20;
                        if (food <= 0) food = 1;
                        player.setFoodLevel(food);

                        // Restore experience
                        BeaconLinkListener.setTotalExperience(player, rs.getInt("exp"));

                        // Get spawn point
                        String worldName = rs.getString("location_world");
                        if (worldName != null) {
                            org.bukkit.World world = getBeaconzPlugin().getServer().getWorld(worldName);
                            if (world != null) {
                                double x = rs.getDouble("location_x");
                                double y = rs.getDouble("location_y");
                                double z = rs.getDouble("location_z");
                                float yaw = rs.getFloat("location_yaw");
                                float pitch = rs.getFloat("location_pitch");
                                return new Location(world, x, y, z, yaw, pitch);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Failed to get inventory for " + player.getName() + " in game " + gameName + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Store the player's inventory in the game
     * @param player - the player
     * @param gameName - the game name for the inventory being stored
     * @param from - the last position of the player in this game
     */
    public void storeInventory(Player player, String gameName, Location from) {
        storeInventory(player, gameName, from, true);
    }

    /**
     * Puts the player's inventory into the database
     * @param player - the player
     * @param gameName - the game name for the inventory being stored
     * @param from - the last position of the player in this game
     * @param storeInv - whether the inventory should be stored or not
     */
    public void storeInventory(Player player, String gameName, Location from, boolean storeInv) {
        if (DEBUG)
            getLogger().info("DEBUG: storeInventory for " + player.getName() + " leaving " + gameName + " from " + from);

        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot store inventory for " + player.getName());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            // Serialize inventory to byte array
            byte[] inventoryData = null;
            if (storeInv) {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     BukkitObjectOutputStream boos = new BukkitObjectOutputStream(bos)) {
                    boos.writeObject(player.getInventory().getContents());
                    inventoryData = bos.toByteArray();
                } catch (Exception e) {
                    getLogger().warning("Failed to serialize inventory for " + player.getName() + ": " + e.getMessage());
                }
            }

            // UPSERT player data
            String sql = "INSERT OR REPLACE INTO player_inventories " +
                        "(player_uuid, game_name, inventory, health, food, exp, location_world, " +
                        "location_x, location_y, location_z, location_yaw, location_pitch) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, gameName);
                stmt.setBytes(3, inventoryData);
                stmt.setDouble(4, player.getHealth());
                stmt.setInt(5, player.getFoodLevel());
                stmt.setInt(6, player.calculateTotalExperiencePoints());

                if (from != null) {
                    stmt.setString(7, from.getWorld().getName());
                    stmt.setDouble(8, from.getX());
                    stmt.setDouble(9, from.getY());
                    stmt.setDouble(10, from.getZ());
                    stmt.setFloat(11, from.getYaw());
                    stmt.setFloat(12, from.getPitch());
                } else {
                    stmt.setNull(7, java.sql.Types.VARCHAR);
                    stmt.setNull(8, java.sql.Types.REAL);
                    stmt.setNull(9, java.sql.Types.REAL);
                    stmt.setNull(10, java.sql.Types.REAL);
                    stmt.setNull(11, java.sql.Types.REAL);
                    stmt.setNull(12, java.sql.Types.REAL);
                }

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to store inventory for " + player.getName() + " in game " + gameName + ": " + e.getMessage());
        }

        // Clear the player's inventory
        player.getInventory().clear();
        BeaconLinkListener.setTotalExperience(player, 0);

        if (DEBUG)
            getLogger().info("DEBUG: Done!");
    }

    /**
     * Removes all inventories for this game
     * @param gameName - the game name
     */
    public void removeGame(String gameName) {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot remove game inventories");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String sql = "DELETE FROM player_inventories WHERE game_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, gameName);
                int deleted = stmt.executeUpdate();
                getLogger().info("Removed " + deleted + " player inventory record(s) for game " + gameName);
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to remove game inventories for " + gameName + ": " + e.getMessage());
        }
    }

    /**
     * Clears all the items for player in game name, sets the respawn point
     * @param player - the player
     * @param gameName - the game name
     * @param from - the location to set as respawn point
     */
    public void clearItems(Player player, String gameName, Location from) {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot clear items for " + player.getName());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE player_inventories SET inventory = NULL, " +
                        "location_world = ?, location_x = ?, location_y = ?, location_z = ?, " +
                        "location_yaw = ?, location_pitch = ? " +
                        "WHERE player_uuid = ? AND game_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (from != null) {
                    stmt.setString(1, from.getWorld().getName());
                    stmt.setDouble(2, from.getX());
                    stmt.setDouble(3, from.getY());
                    stmt.setDouble(4, from.getZ());
                    stmt.setFloat(5, from.getYaw());
                    stmt.setFloat(6, from.getPitch());
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR);
                    stmt.setNull(2, java.sql.Types.REAL);
                    stmt.setNull(3, java.sql.Types.REAL);
                    stmt.setNull(4, java.sql.Types.REAL);
                    stmt.setNull(5, java.sql.Types.REAL);
                    stmt.setNull(6, java.sql.Types.REAL);
                }
                stmt.setString(7, player.getUniqueId().toString());
                stmt.setString(8, gameName);

                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to clear items for " + player.getName() + " in game " + gameName + ": " + e.getMessage());
        }
    }

    /**
     * Sets the player's food level in game
     * @param player - the player
     * @param gameName - the game name
     * @param foodLevel - the food level
     */
    public void setFood(Player player, String gameName, int foodLevel) {
        updatePlayerStat(player, gameName, "food", foodLevel);
    }
    
    /**
     * Sets player's health in game
     * @param player - the player
     * @param gameName - the game name
     * @param maxHealth - the health
     */
    public void setHealth(Player player, String gameName, double maxHealth) {
        updatePlayerStat(player, gameName, "health", maxHealth);
    }

    /**
     * Sets player's exp in game
     * @param player - the player
     * @param gameName - the game name
     * @param newExp - the experience points
     */
    public void setExp(Player player, String gameName, int newExp) {
        updatePlayerStat(player, gameName, "exp", newExp);
    }

    /**
     * Helper method to update a single player stat in the database
     * @param player - the player
     * @param gameName - the game name
     * @param column - the column to update (food, health, or exp)
     * @param value - the value to set
     */
    private void updatePlayerStat(Player player, String gameName, String column, Object value) {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot update " + column + " for " + player.getName());
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE player_inventories SET " + column + " = ? WHERE player_uuid = ? AND game_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (value instanceof Integer) {
                    stmt.setInt(1, (Integer) value);
                } else if (value instanceof Double) {
                    stmt.setDouble(1, (Double) value);
                }
                stmt.setString(2, player.getUniqueId().toString());
                stmt.setString(3, gameName);

                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    // No existing record, create one with default values
                    insertDefaultPlayerRecord(player, gameName);
                    // Try update again
                    try (PreparedStatement stmt2 = conn.prepareStatement(sql)) {
                        if (value instanceof Integer) {
                            stmt2.setInt(1, (Integer) value);
                        } else if (value instanceof Double) {
                            stmt2.setDouble(1, (Double) value);
                        }
                        stmt2.setString(2, player.getUniqueId().toString());
                        stmt2.setString(3, gameName);
                        stmt2.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to update " + column + " for " + player.getName() + " in game " + gameName + ": " + e.getMessage());
        }
    }

    /**
     * Creates a default player record if one doesn't exist
     * @param player - the player
     * @param gameName - the game name
     */
    private void insertDefaultPlayerRecord(Player player, String gameName) throws SQLException {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) return;

        try (Connection conn = dataSource.getConnection()) {
            String sql = "INSERT OR IGNORE INTO player_inventories " +
                        "(player_uuid, game_name, inventory, health, food, exp) " +
                        "VALUES (?, ?, NULL, 20.0, 20, 0)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, gameName);
                stmt.executeUpdate();
            }
        }
    }

}
