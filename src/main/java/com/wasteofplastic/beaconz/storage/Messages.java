/*
 * Copyright (c) 2015 - 2026 tastybento
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.game.Game;
import com.wasteofplastic.beaconz.game.Scorecard;
import com.zaxxer.hikari.HikariDataSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

/**
 * Handles offline messaging to players and teams.
 * Messages are stored as serialized Components in the SQL database.
 *
 * @author tastybento
 */
public class Messages extends BeaconzPluginDependent {

    /**
     * Creates a new Messages handler and initializes the database table.
     *
     * @param plugin the plugin instance
     */
    public Messages(Beaconz plugin) {
        super(plugin);
        initializeDatabase();
    }

    /**
     * Initializes the player_messages database table.
     * Creates the table if it doesn't exist.
     */
    private void initializeDatabase() {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Messages will not function properly.");
            return;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create player_messages table
            // Each message is stored as a separate row with its JSON-serialized Component
            String createTable = "CREATE TABLE IF NOT EXISTS player_messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT NOT NULL, " +
                "message_json TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))" +
                ")";

            stmt.execute(createTable);

            // Create index for fast lookup by player UUID
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_messages_uuid ON player_messages(player_uuid)");

            getLogger().info("Database table initialized for player messages");

        } catch (SQLException e) {
            getLogger().severe("Failed to initialize player_messages database table: " + e.getMessage());
        }
    }

    /**
     * Returns all pending messages for the player.
     *
     * @param playerUUID the player's UUID
     * @return List of Component messages, or empty list if none
     */
    public List<Component> getMessages(UUID playerUUID) {
        List<Component> result = new ArrayList<>();
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            return result;
        }

        String sql = "SELECT message_json FROM player_messages WHERE player_uuid = ? ORDER BY created_at ASC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("message_json");
                    try {
                        Component message = GsonComponentSerializer.gson().deserialize(json);
                        result.add(message);
                    } catch (Exception e) {
                        getLogger().warning("Failed to deserialize message for " + playerUUID + ": " + e.getMessage());
                    }
                }
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to get messages for " + playerUUID + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Clears all pending messages for the player.
     *
     * @param playerUUID the player's UUID
     */
    public void clearMessages(UUID playerUUID) {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            return;
        }

        String sql = "DELETE FROM player_messages WHERE player_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            getLogger().severe("Failed to clear messages for " + playerUUID + ": " + e.getMessage());
        }
    }

    /**
     * Tells all of a player's team members (online or offline) that something happened.
     * Only works if the player is in the game area.
     *
     * @param player the originating player, always an online player
     * @param message the message to send
     */
    public void tellTeam(Player player, Component message) {
        Scorecard sc = getGameMgr().getSC(player);
        if (sc != null) {
            Team team = sc.getTeam(player);
            if (team != null) {
                tellTeam(player, team, message);
            }
        }
    }

    /**
     * Tells a message to all members of team, regardless of whether they are online or offline.
     * Ignores the originating player.
     *
     * @param player player sending the message (can be null to include all team members)
     * @param team team to notify
     * @param message message to send
     */
    public void tellTeam(Player player, Team team, Component message) {
        Game game = getGameMgr().getGame(team);
        if (game == null) {
            return;
        }

        // Prefix the message with the game name
        Component prefixedMessage = (Component.text("[").append(game.getName()).append(Component.text("] "))).color(NamedTextColor.GOLD)
                .append(message);

        var teamMembers = game.getScorecard().getTeamMembers();
        if (teamMembers == null) {
            return;
        }

        List<UUID> members = teamMembers.get(team);
        if (members == null) {
            return;
        }

        for (UUID uuid : members) {
            // Skip the originating player
            if (player != null && player.getUniqueId().equals(uuid)) {
                continue;
            }

            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(prefixedMessage);
            } else {
                // Store for offline delivery
                setMessage(uuid, prefixedMessage);
            }
        }
    }

    /**
     * Tells a message to all members of a team.
     *
     * @param team team to notify
     * @param message message to send
     */
    public void tellTeam(Team team, @NotNull Component message) {
        this.tellTeam(null, team, message);
    }

    /**
     * Tells a message to all teams except the specified one.
     *
     * @param team the team to exclude
     * @param message message to send
     */
    public void tellOtherTeams(Team team, @NotNull Component message) {
        var scoreboard = team.getScoreboard();
        if (scoreboard == null) {
            return;
        }
        for (Team otherTeam : scoreboard.getTeams()) {
            if (!team.equals(otherTeam)) {
                tellTeam(otherTeam, message);
            }
        }
    }

    /**
     * Stores a message for the player to receive next time they login.
     * If the player is online, this method does nothing (the caller should send directly).
     *
     * @param uuid the player's UUID
     * @param message the message to store as a Component
     */
    public void setMessage(UUID uuid, Component message) {
        Player player = getServer().getPlayer(uuid);
        // Don't store if player is online - they should receive messages directly
        if (player != null && player.isOnline()) {
            return;
        }

        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot store message for " + uuid);
            return;
        }

        // Serialize the Component to JSON
        String json = GsonComponentSerializer.gson().serialize(message);

        String sql = "INSERT INTO player_messages (player_uuid, message_json) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, json);
            stmt.executeUpdate();

        } catch (SQLException e) {
            getLogger().severe("Failed to store message for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Checks if a player has any pending messages.
     *
     * @param playerUUID the player's UUID
     * @return true if the player has pending messages
     */
    public boolean hasMessages(UUID playerUUID) {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM player_messages WHERE player_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to check messages for " + playerUUID + ": " + e.getMessage());
        }

        return false;
    }
}
