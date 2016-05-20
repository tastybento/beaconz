/*
 * Copyright (c) 2015 - 2016 tastybento
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

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;

/**
 * Handles all death and respawn events
 * 
 * @author tastybento
 *
 */
public class PlayerDeathListener extends BeaconzPluginDependent implements Listener {

    private HashMap<UUID, Location> deadPlayers = new HashMap<UUID,Location>();
    private static final String LOBBY = "Lobby";

    /**
     * @param plugin
     */
    public PlayerDeathListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Saves the player's death and resets their spawn point to the team spawn
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        //getLogger().info("DEBUG: death");
        Player player = event.getEntity();
        // Check if player died in-game
        if (!player.getWorld().equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not in world");
            return;
        }
        // If in the lobby, ignore
        if (getGameMgr().isLocationInLobby(player.getLocation())) {
            //getLogger().info("DEBUG: died in lobby");
            return;
        }
        // Get game
        Game game = getGameMgr().getGame(player.getLocation());
        if (game != null) {
            Team team = game.getScorecard().getTeam(player);
            //getLogger().info("DEBUG: team is " + team.getDisplayName());
            // Store new spawn point
            Location spawnPoint = game.getScorecard().getTeamSpawnPoint(team);
            //getLogger().info("DEBUG: new spawn point is " + spawnPoint);
            if (event.getKeepInventory()) {
                // Store the inventory for this player because they will get it when they come back
                // Will also store their exp
                //getLogger().info("DEBUG: keep inventory is true");
                getBeaconzStore().storeInventory(player, game.getName(), spawnPoint);
            } else {
                //getLogger().info("DEBUG: keep inventory is false");
                // Their inventory is going to get dumped on the floor so they need to have their possessions removed
                getBeaconzStore().clearItems(player, game.getName(), spawnPoint);
            }
            if (!event.getKeepLevel()) {
                //getLogger().info("DEBUG: lose level! - new exp = " + event.getNewExp());
                // If they don't get to keep their level, their exp needs updating to what they'll get in this world.
                getBeaconzStore().setExp(player, game.getName(), event.getNewExp());
            } else {
                //getLogger().info("DEBUG: keep level");
            }
            // They are dead, so when they respawn they need to have full health (otherwise they will die repeatedly)
            getBeaconzStore().setHealth(player, game.getName(), player.getMaxHealth());
            // They will also get full food level
            getBeaconzStore().setFood(player, game.getName(), 20);
            // Make a note of their death status
            deadPlayers.put(player.getUniqueId(), spawnPoint);
        } else {
            //getLogger().info("DEBUG: game is null");
        }
    }

    /**
     * Respawns the player back at the beaconz lobby
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRespawn(final PlayerRespawnEvent event) {
        //getLogger().info("DEBUG: Respawn");
        if (!deadPlayers.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        // Set respawn location to Beaconz lobby
        event.setRespawnLocation(getGameMgr().getLobby().getSpawnPoint());
        deadPlayers.remove(event.getPlayer().getUniqueId());
        // Get from store
        getBeaconzStore().getInventory(event.getPlayer(), LOBBY);
    }

    
}
