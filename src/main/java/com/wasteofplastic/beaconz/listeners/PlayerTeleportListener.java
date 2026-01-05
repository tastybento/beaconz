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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Manages all player teleportation and world transition events in the Beaconz game system.
 * <p>
 * This listener handles the complex logic of moving players between different game areas:
 * <ul>
 *   <li>World entry/exit - Managing transitions in/out of the Beaconz world</li>
 *   <li>Game-to-game teleportation - Moving between different game instances</li>
 *   <li>Game-to-lobby transitions - Exiting games to return to the lobby</li>
 *   <li>Lobby-to-game teleportation - Entering games from the lobby</li>
 * </ul>
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li><b>Inventory Management</b> - Stores and restores player inventories for each game/lobby context</li>
 *   <li><b>Teleport Delays</b> - Enforces waiting periods when leaving active games (prevents combat logging)</li>
 *   <li><b>Permission Validation</b> - Ensures players can only enter games they're part of</li>
 *   <li><b>State Cleanup</b> - Removes potion effects, scoreboards, and tracking data during transitions</li>
 *   <li><b>Message Delivery</b> - Shows pending messages when players enter the world</li>
 *   <li><b>Safe Spawning</b> - Finds safe locations when restoring player positions</li>
 *   <li><b>Region Callbacks</b> - Triggers enter/exit handlers for regions</li>
 * </ul>
 * <p>
 * The class maintains three tracking sets:
 * <ul>
 *   <li>barrierPlayers - Players being pushed back by region barriers (no-op teleports)</li>
 *   <li>teleportingPlayers - Players in the middle of delayed teleports (tracks position to detect movement)</li>
 *   <li>directTeleportPlayers - Players with admin/op privileges who can teleport instantly</li>
 * </ul>
 * <p>
 * Teleport flow example: Player leaving an active game
 * <ol>
 *   <li>Player initiates teleport (e.g., /spawn)</li>
 *   <li>Event is cancelled and delay timer starts (unless op/admin)</li>
 *   <li>Player must stand still for configured duration</li>
 *   <li>After delay, inventory is saved with game context</li>
 *   <li>Region exit handler is called</li>
 *   <li>Player is teleported to destination</li>
 *   <li>Destination inventory is restored (lobby or other game)</li>
 *   <li>Region enter handler is called</li>
 * </ol>
 *
 * @author tastybento
 */
public class PlayerTeleportListener extends BeaconzPluginDependent implements Listener {

    /**
     * Set of players currently being pushed back by region barriers.
     * These teleports should be processed without triggering normal teleport logic.
     */
    private final Set<UUID> barrierPlayers = new HashSet<>();

    /**
     * Maps player UUIDs to their locations when they initiated a delayed teleport.
     * Used to detect if player moved during the waiting period (which cancels the teleport).
     */
    private final HashMap<UUID, Vector> teleportingPlayers = new HashMap<>();

    /**
     * Set of players who should teleport directly without delay.
     * Used for ops and admin commands that need instant teleportation.
     */
    private final Set<UUID> directTeleportPlayers = new HashSet<>();

    /** Constant identifier for lobby inventory storage */
    private static final String LOBBY = "Lobby";

    /**
     * Constructs a new PlayerTeleportListener.
     *
     * @param plugin the Beaconz plugin instance
     */
    public PlayerTeleportListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles player entering the Beaconz world.
     * <p>
     * This method:
     * <ul>
     *   <li>Saves the player's name to the database for UUID lookup</li>
     *   <li>Delivers any pending messages queued while player was offline</li>
     *   <li>Teleports player to lobby if they're not already there</li>
     *   <li>Removes all potion effects to start with clean state</li>
     * </ul>
     * <p>
     * Messages are delivered after a 2-second delay (40 ticks) to allow the world to fully load.
     *
     * @param event the player changed world event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();

        // Only process when entering the Beaconz world
        if (player.getWorld().equals((getBeaconzWorld()))) {
            // Save player name to database for future UUID->name lookups
            getBeaconzPlugin().getNameStore().savePlayerName(player.getName(), player.getUniqueId());

            // Check if there are any messages queued for this player
            final List<String> messages = getMessages().getMessages(player.getUniqueId());
            if (messages != null) {
                // Deliver messages after a delay to ensure world is fully loaded
                getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
                    // Show header
                    player.sendMessage(Component.text( Lang.titleBeaconzNews).color(NamedTextColor.AQUA));
                    // Show each message with a number prefix
                    int i = 1;
                    for (String message : messages) {
                        player.sendMessage(i++ + ": " + message);
                    }
                    // Clear the messages now that they've been delivered
                    getMessages().clearMessages(player.getUniqueId());
                }, 40L); // 2 second delay (40 ticks)
            }

            // Ensure player is sent to lobby if not already there
            if (!getGameMgr().isPlayerInLobby(event.getPlayer())) {
                getGameMgr().getLobby().tpToRegionSpawn(event.getPlayer(), true);
                getGameMgr().getLobby().enterLobby(event.getPlayer());
            }

            // Remove all potion effects when entering world
            // This ensures clean state and prevents effect exploitation
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    /**
     * Handles player exiting the Beaconz world.
     * <p>
     * This method performs cleanup when a player leaves the Beaconz world:
     * <ul>
     *   <li>Removes the player from beacon tracking (standing on beacon data)</li>
     *   <li>Resets the player's scoreboard to a clean state</li>
     *   <li>Removes all active potion effects</li>
     * </ul>
     * <p>
     * Note: Potion effects are removed to prevent players from keeping game buffs/debuffs
     * in other worlds. Consider saving and restoring effects in future versions to prevent
     * exploit of jumping between worlds to clear negative effects.
     *
     * @param event the player changed world event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onWorldExit(final PlayerChangedWorldEvent event) {
        // Only process when exiting the Beaconz world
        if (event.getFrom().equals((getBeaconzWorld()))) {
            // Remove player from beacon tracking map
            BeaconProtectionListener.getStandingOn().remove(event.getPlayer().getUniqueId());

            // Reset scoreboard to prevent conflicts in other worlds
            event.getPlayer().setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());

            // Remove all potion effects
            // TODO: Save and restore potion effects to prevent exploit where players
            // jump to another world and back to clear negative effects
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {

        // We only handle teleports originating in the BeaconzWorld
        if (event.getFrom().getWorld().equals(getBeaconzWorld())) {
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
                            if (!fromGame.isGameRestart() && !fromGame.isOver() && !directTeleportPlayers.contains(player.getUniqueId())) {
                                delayTeleport(player, event.getFrom(), event.getTo(), fromGame.getName(), LOBBY);
                                event.setCancelled(true);
                                return;
                            } else {
                                // For direct teleporting
                                fromGame.getRegion().exit(player); // this may clear the inventory, so only store inventory after this line
                                getBeaconzStore().storeInventory(player, fromGame.getName(), event.getFrom());
                            }
                        } else {
                            // This is either a direct teleport or the final part of the delayed teleport
                            directTeleportPlayers.remove(player.getUniqueId());
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
                            player.sendMessage(Component.text(Lang.errorNotInGame.replace("[game]", toGame.getName())).color(NamedTextColor.RED));
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
            player.sendMessage(Component.text(Lang.teleportDoNotMove.replace("[number]", String.valueOf(Settings.teleportDelay))).color(NamedTextColor.RED));
        }
        teleportingPlayers.put(player.getUniqueId(), player.getLocation().toVector());
        getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
            // Check if player moved
            if (player != null && !player.isDead() && teleportingPlayers.containsKey(player.getUniqueId())) {
                if (player.getLocation().toVector().equals(teleportingPlayers.get(player.getUniqueId()))) {

                    player.teleportAsync(to);

                } else {
                    player.sendMessage(Component.text(Lang.teleportYouMoved).color(NamedTextColor.RED));
                }
            }
            teleportingPlayers.remove(player.getUniqueId());
        }, Settings.teleportDelay * delay);
    }

    /**
     * @param directTeleportPlayer UUID of player to set for direct teleport
     */
    public void setDirectTeleportPlayer(UUID directTeleportPlayer) {
        this.directTeleportPlayers.add(directTeleportPlayer);
    }


}
