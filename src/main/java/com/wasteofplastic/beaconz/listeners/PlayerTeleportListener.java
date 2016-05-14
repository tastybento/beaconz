/*
 * Copyright (c) 2015 tastybento
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

    private Set<UUID> barrierPlayers = new HashSet<UUID>();
    private HashMap<UUID, Vector> teleportingPlayers = new HashMap<UUID,Vector>();
    private final static boolean DEBUG = false;
    private static final String LOBBY = "Lobby";

    /**
     * @param plugin
     */
    public PlayerTeleportListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Sends player to lobby if they enter the world
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
        // Entering Beaconz world
        if (event.getPlayer().getWorld().equals((getBeaconzWorld()))) {
            getLogger().info("DEBUG: entering world");
            if (!getGameMgr().isPlayerInLobby(event.getPlayer())) {                
                // Send player to lobby
                getGameMgr().getLobby().tpToRegionSpawn(event.getPlayer());
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
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    /**
     * Handles using signs in the lobby to join games
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSignClick(final PlayerInteractEvent event) {
        //getLogger().info("DEBUG: click sign");
        if (getGameMgr().getLobby().isPlayerInRegion(event.getPlayer())) {
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || (event.getAction().equals(Action.LEFT_CLICK_BLOCK) && !event.getPlayer().isOp())) {
                if (event.getClickedBlock().getState() instanceof Sign) {
                    Sign sign = (Sign) event.getClickedBlock().getState();
                    if (Arrays.toString(sign.getLines()).toLowerCase().contains("beaconz")) {
                        Boolean foundgame = false;
                        for (int i = 1; i < 3; i++) {
                            String gamename = sign.getLine(i);
                            if (getGameMgr().getGame(gamename) != null) {
                                getGameMgr().getGame(gamename).join(event.getPlayer());
                                foundgame = true;
                                break;
                            }
                        }
                        if (!foundgame) {
                            event.getPlayer().sendMessage(Lang.notReady);
                        }
                    }
                }
            }
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getWorld().equals(getBeaconzWorld())
                && getGameMgr().getGame(event.getClickedBlock().getLocation()) == null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
        }

    }    

    // These next methods are taken from Essentials code

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: Teleporting event");
        // If player is teleporting, then ignore this event
        if (teleportingPlayers.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }    
        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            if (DEBUG)
                getLogger().info("DEBUG: from or to world is null");
            return;
        }
        if (event.getFrom().getWorld() != getBeaconzWorld()) {
            // Ignore
            return;
        }
        // Remove from standing - any teleport 
        BeaconProtectionListener.getStandingOn().remove(event.getPlayer().getUniqueId());
        // If player is pushed back because of the barrier, just return
        if (barrierPlayers.contains(event.getPlayer().getUniqueId())) {
            //getLogger().info("DEBUG: ignoring barrier teleport");
            barrierPlayers.remove(event.getPlayer().getUniqueId());
            return;
        }
        // Get the games associated with these locations
        final Game fromGame = getGameMgr().getGame(event.getFrom());
        final Game toGame = getGameMgr().getGame(event.getTo());

        // Check if the teleport is to or from the lobby
        final boolean fromLobby = getGameMgr().isLocationInLobby(event.getFrom());
        final boolean toLobby = getGameMgr().isLocationInLobby(event.getTo());
        // Teleporting out of Beaconz World
        if (!event.getTo().getWorld().equals(getBeaconzWorld()) && fromGame != null) {
            if (DEBUG)
                getLogger().info("DEBUG: Teleporting out of world");
            // Player is trying to exit, maybe trying to escape
            delayTeleport(event.getPlayer(), event.getFrom(), event.getTo(), fromGame.getName(), LOBBY);
            event.setCancelled(true);
            return;
        }
        /*
        // Teleporting into Beaconz World
        if (event.getTo().getWorld().equals(getBeaconzWorld()) && event.getFrom().getWorld().equals(getBeaconzWorld())) {
            getLogger().info("DEBUG: Teleporting into world");
            // Get from store
            getBeaconzPlugin().getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {

                @Override
                public void run() {
                    getBeaconzStore().getInventory(event.getPlayer(), event.getTo());
                }}, 5L);

            return;
        }*/
        // Teleporting to different game
        if (event.getTo().getWorld().equals(getBeaconzWorld()) && event.getFrom().getWorld().equals(getBeaconzWorld())) {
            if (DEBUG)
                getLogger().info("DEBUG: Teleporting within world");

            if (toLobby && fromLobby) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting within lobby");
                return;
            }

            if (toLobby && fromGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting to lobby from game " + fromGame.getName());
                delayTeleport(event.getPlayer(), event.getFrom(), event.getTo(), fromGame.getName(), LOBBY);
                event.setCancelled(true);
                /*
                // Store
                getBeaconzStore().storeInventory(event.getPlayer(), fromGame.getName(), event.getFrom());
                // Get from store
                getBeaconzStore().getInventory(event.getPlayer(), LOBBY);
                 */
                return;
            }

            if (fromLobby && toGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting from lobby to a game " + toGame.getName());
                // Store in lobby
                getBeaconzStore().storeInventory(event.getPlayer(), LOBBY, event.getFrom());
                // Get from store and move to last known position
                Location newTo = getBeaconzStore().getInventory(event.getPlayer(), toGame.getName());
                if (newTo != null) {
                    // Get a safe location
                    newTo = toGame.getRegion().findSafeSpot(newTo, 10);
                    event.setTo(newTo);
                }
                return;
            }
            if (DEBUG)
                getLogger().info("DEBUG: Not a lobby teleport");
            // Not a lobby
            if (fromGame != null && toGame != null && fromGame.equals(toGame)) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting within game");
                return;
            }

            if (fromGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting from game " + fromGame.getName());
                // Store
                getBeaconzStore().storeInventory(event.getPlayer(), fromGame.getName(), event.getFrom());
            }
            if (toGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting to game " + toGame.getName());
                // Check if player is in this game
                if (toGame.hasPlayer(event.getPlayer())) {
                    // Get from store
                    Location newTo = getBeaconzStore().getInventory(event.getPlayer(), toGame.getName());
                    if (newTo != null) {
                        if (DEBUG)
                            getLogger().info("DEBUG: Teleporting player to last location" + newTo);
                        newTo = toGame.getRegion().findSafeSpot(newTo, 10);
                        event.setTo(newTo);
                    }
                    /*
                    getBeaconzPlugin().getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {

                        @Override
                        public void run() {
                            getBeaconzStore().getInventory(event.getPlayer(), toGame.getName());
                        }}, 5L);
                     */
                } else {
                    if (DEBUG)
                        getLogger().info("DEBUG: Player is not in this game");
                    // Send them to the lobby
                    event.getPlayer().sendMessage(ChatColor.RED + Lang.errorNotInGame.replace("[game]", toGame.getName()));
                    event.setTo(getGameMgr().getLobby().getSpawnPoint());
                }
            }
            return;
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
        player.sendMessage(ChatColor.RED + Lang.doNotMove.replace("[number]", String.valueOf(Settings.teleportDelay)));
        teleportingPlayers.put(player.getUniqueId(), player.getLocation().toVector());
        getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {

            @Override
            public void run() {
                // Check if player moved
                if (player != null && !player.isDead() && teleportingPlayers.containsKey(player.getUniqueId())) {                      
                    if (player.getLocation().toVector().equals(teleportingPlayers.get(player.getUniqueId()))) {
                        // Store
                        getBeaconzStore().storeInventory(player, fromGame, from);
                        // Load lobby inv
                        getBeaconzStore().getInventory(player, toGame);
                        // Teleport
                        player.teleport(to);
                        // If this is the lobby run the entrance welcome
                        if (to.getWorld().equals(getBeaconzWorld()) && toGame.equals(LOBBY)) {
                            getGameMgr().getLobby().enterLobby(player);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + Lang.youMoved);
                    }
                }
                teleportingPlayers.remove(player.getUniqueId());
            }}, Settings.teleportDelay * 20L);
    }

    
}
