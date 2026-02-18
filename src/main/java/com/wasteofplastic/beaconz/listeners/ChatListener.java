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

package com.wasteofplastic.beaconz.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.game.Scorecard;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;


/**
 * This class is to catch chats and implement team chat.
 * As it can be called asynchronously (and usually it is when a player chats)
 * it cannot access any HashMaps or Bukkit APIs without running the risk of a clash with another thread
 * or the main thread. As such:
 * To handle team chat, a thread-safe hashmap is used to store whether team chat is on for a player or not
 * and if it is, the team chat itself is queued to run on the next server tick, i.e., in the main thread
 * This all ensures it's thread-safe.
 * @author tastybento
 *
 */
public class ChatListener extends BeaconzPluginDependent implements Listener {

    // List of which admins are spying or not on team chat
    private final Set<UUID> spies;

    /**
     * @param plugin Beaconz plugin instance
     */
    public ChatListener(Beaconz plugin) {
        super(plugin);
        // Initialize spies
        spies = new HashSet<>();
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(final AsyncChatEvent event) {
        // Team chat
        if (getBeaconzWorld() == null) {
            return;
        }
        if (Settings.teamChat && event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            // Cancel the event
            event.setCancelled(true);
            // Queue the sync task because you cannot use HashMaps asynchronously. Delaying to the next tick
            // won't be a major issue for synchronous events either.
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            Bukkit.getScheduler().runTask(beaconzPlugin, () -> teamChat(event.getPlayer(), message));
        }
    }

    private void teamChat(final Player player, String message) {
        // Only act if the player is in a team
        Scorecard sc = getGameMgr().getSC(player);
        if (sc == null) {
            return;
        }
        Team team = sc.getTeam(player);
        if (team != null) {
            @SuppressWarnings("deprecation")
            Set<OfflinePlayer> teamMembers = team.getPlayers();
            // Build the team chat message using Components
            Component teamMessage = Component.text("[", NamedTextColor.LIGHT_PURPLE)
                    .append(team.displayName())
                    .append(Component.text("]<", NamedTextColor.LIGHT_PURPLE))
                    .append(player.displayName())
                    .append(Component.text("> " + message, NamedTextColor.LIGHT_PURPLE));
            // Tell only the team members if they are online
            boolean onLine = false;
            for (OfflinePlayer teamPlayer : teamMembers) {
                if (teamPlayer != null && teamPlayer.isOnline()) {
                    ((Player)teamPlayer).sendMessage(teamMessage);
                    if (!teamPlayer.getUniqueId().equals(player.getUniqueId())) {
                        onLine = true;
                    }
                }
            }
            // Spy function
            if (onLine) {
                Component spyMessage = Component.text("[TCSpy] ", NamedTextColor.RED)
                        .append(Component.text(message, NamedTextColor.WHITE));
                for (Player onlinePlayer: beaconzPlugin.getServer().getOnlinePlayers()) {
                    if (spies.contains(onlinePlayer.getUniqueId())) {
                        onlinePlayer.sendMessage(spyMessage);
                    }
                }
            }
            if (!onLine) {
                // Tell everyone
                for (Player onlinePlayer: getServer().getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player)) {
                        onlinePlayer.sendMessage(teamMessage);
                    }
                }
            }
        } else {
            // Tell everyone - no team, just broadcast player name and message
            Component broadcastMessage = player.displayName()
                    .append(Component.text(": " + message));
            for (Player onlinePlayer: getServer().getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.sendMessage(broadcastMessage);
                }
            }
        }
    }

    /**
     * Toggles team chat spy. Spy must also have the spy permission to see chats
     * @param playerUUID - UUID of player toggling spy
     * @return true if toggled on, false if toggled off
     */
    public boolean toggleSpy(UUID playerUUID) {
        if (spies.contains(playerUUID)) {
            spies.remove(playerUUID);
            return false;
        } else {
            spies.add(playerUUID);
            return true;
        }
    }
}
