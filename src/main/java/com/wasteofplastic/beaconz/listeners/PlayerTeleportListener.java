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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Settings;

/**
 * Handles all teleportation events, e.g., player teleporting into world
 * 
 * @author tastybento
 *
 */
public class PlayerTeleportListener extends BeaconzPluginDependent implements Listener {

    private final Set<UUID> barrierPlayers = new HashSet<>();
    private final HashMap<UUID, Vector> teleportingPlayers = new HashMap<>();
    private final Set<UUID> directTeleportPlayers = new HashSet<>();
    private static final String LOBBY = "Lobby";

    /**
     * @param plugin
     */
    public PlayerTeleportListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles player entering the world
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        
        // Entering Beaconz world
        if (player.getWorld().equals((getBeaconzWorld()))) {
            //getLogger().info("DEBUG: entering world");
            
            // Write this player's name to the database
            getBeaconzPlugin().getNameStore().savePlayerName(player.getName(), player.getUniqueId());
            
            // Check for pending messages
            final List<String> messages = getMessages().getMessages(player.getUniqueId());
            if (messages != null) {
                // plugin.getLogger().info("DEBUG: Messages waiting!");
                getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
                    player.sendMessage(ChatColor.AQUA + Lang.titleBeaconzNews);
                    int i = 1;
                    for (String message : messages) {
                        player.sendMessage(i++ + ": " + message);
                    }
                    // Clear the messages
                    getMessages().clearMessages(player.getUniqueId());
                }, 40L);
            }
            
            // Send player to lobby
            if (!getGameMgr().isPlayerInLobby(event.getPlayer())) {                
                getGameMgr().getLobby().tpToRegionSpawn(event.getPlayer(), true);
                getGameMgr().getLobby().enterLobby(event.getPlayer());
            }
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    /**
     * Removes the scoreboard from the player, resets other world-specific items
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onWorldExit(final PlayerChangedWorldEvent event) {
        // Exiting Beaconz world
        if (event.getFrom().equals((getBeaconzWorld()))) {
            // Remove player from map and remove his scoreboard
            BeaconProtectionListener.getStandingOn().remove(event.getPlayer().getUniqueId());
            event.getPlayer().setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
                       
            // Remove any potion effects
            //TODO: Save and restore potion effects to keep player from getting rid of them by quickly jumping off-world and back. Same with Lobby.
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {
        
        // We only handle teleports originating in the BeaconzWorld
        if (event.getFrom().getWorld() != getBeaconzWorld()) {
        } else {
            // Let's start by getting some bearings
            final Player player  = event.getPlayer();
            final boolean teleporting = teleportingPlayers.containsKey(player.getUniqueId());
            final Game fromGame = getGameMgr().getGame(event.getFrom());
            final Game toGame = getGameMgr().getGame(event.getTo());
            final boolean fromLobby = getGameMgr().isLocationInLobby(event.getFrom());
            final boolean toLobby = getGameMgr().isLocationInLobby(event.getTo());

            // Remove from standing - any teleport 
            BeaconProtectionListener.getStandingOn().remove(player.getUniqueId());                
            
            // If player is pushed back because of the barrier, just return
            if (barrierPlayers.contains(player.getUniqueId())) {
                barrierPlayers.remove(player.getUniqueId());

            } else {
                // Teleporting out of a game
                if (fromGame != null) {           
                    // Ignore teleporting within a game
                    if (fromGame.equals(toGame)) {
                        return;
                    } else {
                        if (!teleporting) { 
                            // Player is exiting a game, see if he needs to be delayed
                            if (!fromGame.isGameRestart() && !fromGame.isOver() && !directTeleportPlayers.contains(player)) {
                                delayTeleport(player, event.getFrom(), event.getTo(), fromGame.getName(), LOBBY);
                                event.setCancelled(true);
                                return;
                            }
                        } else {                
                            // This is either a direct teleport or the final part of the delayed teleport
                            directTeleportPlayers.remove(player);
                            fromGame.getRegion().exit(player); // this may clear the inventory, so only store inventory after this line
                            getBeaconzStore().storeInventory(player, fromGame.getName(), event.getFrom());
                        }                
                    }
                }
                
                // Teleporting out of the lobby
                if (fromLobby) {
                    // Ignore teleporting within the lobby
                    if (toLobby) {
                        return;
                    } else {
                        getBeaconzStore().storeInventory(player, LOBBY, event.getFrom());
                        getGameMgr().getLobby().exit(player);
                    }
                }

                // Teleporting into a game
                if (toGame != null) {
                    // Ignore teleporting within a game
                    if (fromGame != null && fromGame.equals(toGame)) {
                        return;
                    } else {
                        // Make sure player is allowed to enter the game
                        if (toGame.hasPlayer(player) || player.isOp()) {
                            
                            // Restore inventory and get location
                            Location newTo = getBeaconzStore().getInventory(player, toGame.getName());
                            if (newTo != null) {
                                newTo = toGame.getRegion().findSafeSpot(newTo, 20);
                                event.setTo(newTo);
                            }
                            // Process region enter
                            toGame.getRegion().enter(player);
                            if (toGame.getGamemode().equals("minigame")) {
                                // Minigames give starting kit and initialXP every time player reenters
                                toGame.giveStartingKit(player);
                            }
     
                        } else {
                            // Player is not part of the game, send them to the lobby
                            player.sendMessage(ChatColor.RED + Lang.errorNotInGame.replace("[game]", toGame.getName()));
                            event.setTo(getGameMgr().getLobby().getSpawnPoint());
                        }
                    }
                }
                                
                // Teleporting into the lobby
                if (toLobby) {
                    // Ignore teleporting within the lobby
                    if (fromLobby) {
                    } else {
                        getBeaconzStore().getInventory(player, LOBBY);            
                        directTeleportPlayers.remove(player.getUniqueId());
                        getGameMgr().getLobby().enterLobby(player);

                    }
                }   
            }                                
        }
    }

    /**
     * Forces players to wait before they teleport out of the game
     * @param player
     * @param from
     * @param to
     * @param fromGame
     */
    private void delayTeleport(final Player player, final Location from, final Location to, final String fromGame, final String toGame) {
        long delay = 20L;
        if  (player.isOp()) {
            delay = 0L;
        } else {
            player.sendMessage(ChatColor.RED + Lang.teleportDoNotMove.replace("[number]", String.valueOf(Settings.teleportDelay)));
        }
        teleportingPlayers.put(player.getUniqueId(), player.getLocation().toVector());
        getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
            // Check if player moved
            if (player != null && !player.isDead() && teleportingPlayers.containsKey(player.getUniqueId())) {
                if (player.getLocation().toVector().equals(teleportingPlayers.get(player.getUniqueId()))) {

                    player.teleport(to);

                } else {
                    player.sendMessage(ChatColor.RED + Lang.teleportYouMoved);
                }
            }
            teleportingPlayers.remove(player.getUniqueId());
        }, Settings.teleportDelay * delay);
    }

    /**
     * @param directTeleportPlayer UUID of player to set for direct teleport
     */
    public void setDirectTeleportPlayer(UUID directTeleportPlayer) {
        //getLogger().info("DEBUG: added player to direct TP");
        this.directTeleportPlayers.add(directTeleportPlayer);
    }


}
