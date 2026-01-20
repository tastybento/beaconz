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

package com.wasteofplastic.beaconz.listeners;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
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
import com.wasteofplastic.beaconz.Params.GameMode;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles all death and respawn events
 * 
 * @author tastybento
 *
 */
public class PlayerDeathListener extends BeaconzPluginDependent implements Listener {

    private final HashMap<UUID, Location> deadPlayers = new HashMap<>();
    protected static final String LOBBY = "Lobby";

    /**
     * @param plugin
     */
    public PlayerDeathListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Saves the player's death and resets their spawn point to the team spawn
     * for when they reenter the game
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {

        Player player = event.getEntity();
        if (player.getWorld().equals(getBeaconzWorld())) {            
            
            // Get the respawn location - first the default
            Location spawnPoint = getGameMgr().getLobby().getSpawnPoint();
            String gameName = LOBBY;
            
            // Then the actual game's, if player died in a game
            if (!getGameMgr().getLobby().isPlayerInRegion(player)) {
                Game game = getGameMgr().getGame(player.getLocation());
                if (game != null) {
                    gameName = PlainTextComponentSerializer.plainText().serialize(game.getName());
                    Team team = game.getScorecard().getTeam(player);                    
                    if (team != null) spawnPoint = game.getScorecard().getTeamSpawnPoint(team);
                }
            }
            
            // Now we have proper gameName and spawnPoint, let's take care of business
            
            // Process region exit - this may clear the inventory, so do this before the storeInventory call
            if (!gameName.equals(LOBBY)) {
                getGameMgr().getGame(gameName).getRegion().exit(player);
                if (getGameMgr().getGame(gameName).getGamemode().equals(GameMode.MINIGAME)) {                
                    event.getDrops().clear();
                }
            }            
            
            // Store the inventory because they will get it when they come back
            getBeaconzStore().storeInventory(player, gameName, spawnPoint); 
            
            // If their inventory is going to get dumped on the floor, they need to have their possessions removed - NOTE: this clears the store
            if (!event.getKeepInventory() && !gameName.equals(LOBBY)) {                
                getBeaconzStore().clearItems(player, gameName, spawnPoint);
            }
            
            // If they don't get to keep their level, their exp needs updating to what they'll get in this world.
            if (!event.getKeepLevel()) {
                getBeaconzStore().setExp(player, gameName, event.getNewExp());
            }  

            // They are dead, so when they respawn they need to have full health (otherwise they will die repeatedly)
            getBeaconzStore().setHealth(player, gameName, player.getAttribute(Attribute.MAX_HEALTH).getValue());
            getBeaconzStore().setFood(player, gameName,  20); 

            // Make a note of their death status
            deadPlayers.put(player.getUniqueId(), spawnPoint);            
        }        
    }

    /**
     * Respawns the player back at the beaconz lobby
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRespawn(final PlayerRespawnEvent event) {
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
