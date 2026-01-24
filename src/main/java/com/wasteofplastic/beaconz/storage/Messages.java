/*
 * Copyright (c) 2015 - 2025 tastybento
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.game.Game;
import com.wasteofplastic.beaconz.game.Scorecard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Handles offline messaging to players and teams
 *
 * @author tastybento
 *
 */
public class Messages extends BeaconzPluginDependent {

    // Offline Messages
    private final HashMap<UUID, List<String>> messages = new HashMap<>();
    private YamlConfiguration messageStore;


    /**
     * @param plugin
     */
    public Messages(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Returns what messages are waiting for the player or null if none
     *
     * @param playerUUID
     * @return
     */
    public List<String> getMessages(UUID playerUUID) {
        return messages.get(playerUUID);
    }

    /**
     * Clears any messages for player
     *
     * @param playerUUID
     */
    public void clearMessages(UUID playerUUID) {
        messages.remove(playerUUID);
    }

    public void saveMessages() {
        if (messageStore == null) {
            return;
        }
        getLogger().info("Saving offline messages...");
        try {
            // Convert to a serialized string
            final HashMap<String, Object> offlineMessages = new HashMap<>();
            for (UUID p : messages.keySet()) {
                offlineMessages.put(p.toString(), messages.get(p));
            }
            // Convert to YAML
            messageStore.set("messages", offlineMessages);
            File messageFile = new File(getDataFolder(), "messages.yml");
            messageStore.save(messageFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loadMessages() {
        getLogger().info("Loading offline messages...");
        try {
            File messageFile = new File(getDataFolder(), "messages.yml");
            messageStore.load(messageFile);
            if (messageStore.getConfigurationSection("messages") == null) {
                messageStore.createSection("messages"); // This is only used to
                // create
            }
            HashMap<String, Object> temp = (HashMap<String, Object>) messageStore.getConfigurationSection("messages").getValues(true);
            for (String s : temp.keySet()) {
                List<String> messageList = messageStore.getStringList("messages." + s);
                if (!messageList.isEmpty()) {
                    messages.put(UUID.fromString(s), messageList);
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Provides the messages for the player
     *
     * @param playerUUID
     * @return List of messages
     */
    public List<String> get(UUID playerUUID) {
        return messages.get(playerUUID);
    }

    /**
     * Stores a message for player
     *
     * @param playerUUID
     * @param playerMessages
     */
    @Deprecated
    public void put(UUID playerUUID, List<String> playerMessages) {
        messages.put(playerUUID, playerMessages);

    }

    /**
     * Tells all of a player's team members (online or offline) that something happened. Only works if the player is in the game area.
     *
     * @param player - the originating player, always an online player
     * @param message - the message to send
     */
    @Deprecated
    public void tellTeam(Player player, String message) {
        Scorecard sc = getGameMgr().getSC(player);
        if (sc != null) {
            Team team = sc.getTeam(player);
            if (team != null) {
                tellTeam(player, team, message);
            }
        }
    }
    
    /**
     * Tells all of a player's team members (online or offline) that something happened. Only works if the player is in the game area.
     *
     * @param player - the originating player, always an online player
     * @param message - the message to send
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
     * Tells a message to all members of team, regardless of whether they are online or offline
     * @param team
     * @param message
     */
    @Deprecated
    public void tellTeam(Team team, String message) {
        tellTeam(null, team, message);
    }

    /**
     * Tells a message to all members of team, regardless of whether they are online or offline.
     * Ignores player
     * @param player player sending the message
     * @param team team
     * @param message message to send
     */
    @Deprecated
    public void tellTeam(Player player, Team team, String message) {
        // Tell other players
        Game game = getGameMgr().getGame(team);
        if (game != null) {
            HashMap<Team, List<UUID>> teamMembers = game.getScorecard().getTeamMembers();
            if (teamMembers != null) {
                List<UUID> members = teamMembers.get(team);
                if (members != null) {
                    for (UUID uuid : members) {
                        Player member = Bukkit.getPlayer(uuid);
                        if (player == null || !player.getUniqueId().equals(uuid)) {
                            if (member != null) {
                                member.sendMessage(ChatColor.GOLD + "[" + game.getName() + "] " + message);
                            } else {
                                setMessage(uuid, ChatColor.GOLD + "[" + game.getName() + "] " + message);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Tells a message to all members of team, regardless of whether they are online or offline.
     * Ignores player
     * @param player player sending the message
     * @param team team
     * @param message message to send
     */
    public void tellTeam(Player player, Team team, Component message) {
        // Tell other players
        Game game = getGameMgr().getGame(team);
        if (game != null) {
            HashMap<Team, List<UUID>> teamMembers = game.getScorecard().getTeamMembers();
            if (teamMembers != null) {
                List<UUID> members = teamMembers.get(team);
                if (members != null) {
                    for (UUID uuid : members) {
                        Player member = Bukkit.getPlayer(uuid);
                        if (player == null || !player.getUniqueId().equals(uuid)) {
                            if (member != null) {
                                member.sendMessage(Component.text("[" + game.getName() + "] ").append(message).color(NamedTextColor.GOLD));
                            } else {
                                setMessage(uuid, ChatColor.GOLD + "[" + game.getName() + "] " + message);
                            }
                        }
                    }
                }
            }
        }
    }
    
    

    /**
     * Broadcast a message to all teams other than this one
     * @param team
     * @param message
     */
    public void tellOtherTeams(Team team, String message) {
        for (Team otherTeam : team.getScoreboard().getTeams()) {
            if (!team.equals(otherTeam)) {
                // Tell other players
                tellTeam(otherTeam, message);
            }
        }

    }

    /**
     * Sets a message for the player to receive next time they login
     *
     * @param uuid the player's UUID
     * @param message the message to store
     */
    public void setMessage(UUID uuid, String message) {
        // getLogger().info("DEBUG: received message - " + message);
        Player player = getServer().getPlayer(uuid);
        // Check if player is online
        if (player != null) {
            if (player.isOnline()) {
                // player.sendMessage(message);
                return;
            }
        }
        // Player is offline so store the message
        // getLogger().info("DEBUG: player is offline - storing message");
        List<String> playerMessages = get(uuid);
        if (playerMessages != null) {
            playerMessages.add(message);
        } else {
            playerMessages = new ArrayList<>(Collections.singletonList(message));
        }
        put(uuid, playerMessages);
    }

    public void tellTeam(Team team, @NotNull Component message) {
       this.tellTeam(null, team, message);
    }

    public void tellOtherTeams(Team team, @NotNull Component message) {
        for (Team otherTeam : team.getScoreboard().getTeams()) {
            if (!team.equals(otherTeam)) {
                // Tell other players
                tellTeam(otherTeam, message);
            }
        }
        
    }
}
