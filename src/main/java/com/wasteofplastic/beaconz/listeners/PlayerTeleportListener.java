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
import com.wasteofplastic.beaconz.game.Game;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Params.GameMode;
import com.wasteofplastic.beaconz.config.Settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
    private static final Component LOBBY = Component.text("Lobby");

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
                    player.sendMessage(Lang.titleBeaconzNews.color(NamedTextColor.AQUA));
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

    /**
     * Handles all teleportation events within the Beaconz world.
     * <p>
     * This is the main teleportation handler that orchestrates complex multi-stage transitions:
     * <ul>
     *   <li>Game-to-game teleportation with inventory swapping</li>
     *   <li>Game-to-lobby transitions with teleport delays (combat logging prevention)</li>
     *   <li>Lobby-to-game teleportation with permission checks</li>
     *   <li>Intra-region teleports (no special handling needed)</li>
     * </ul>
     * <p>
     * The method handles several special cases:
     * <ul>
     *   <li>Barrier pushback teleports - Players hitting region boundaries (no-op)</li>
     *   <li>Delayed teleports - Two-stage teleport with movement detection</li>
     *   <li>Direct teleports - Admin/op instant teleportation</li>
     *   <li>Permission denied - Redirects to lobby if player can't access destination</li>
     * </ul>
     * <p>
     * Flow for game exit with delay:
     * <ol>
     *   <li>Detect non-admin player leaving active game</li>
     *   <li>Cancel event and start delay timer</li>
     *   <li>After delay, trigger new teleport event (teleporting flag set)</li>
     *   <li>Process inventory save and region exit</li>
     *   <li>Allow teleport to complete</li>
     * </ol>
     *
     * @param event the player teleport event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {

        // Only handle teleports originating in the Beaconz world
        if (event.getFrom().getWorld().equals(getBeaconzWorld())) {
            // Gather context information about this teleport
            final Player player  = event.getPlayer();
            final boolean teleporting = teleportingPlayers.containsKey(player.getUniqueId());
            final Game fromGame = getGameMgr().getGame(event.getFrom());
            final Game toGame = getGameMgr().getGame(event.getTo());
            final boolean fromLobby = getGameMgr().isLocationInLobby(event.getFrom());
            final boolean toLobby = getGameMgr().isLocationInLobby(event.getTo());

            // Clear beacon tracking - any teleport removes player from beacon
            BeaconProtectionListener.getStandingOn().remove(player.getUniqueId());

            // Handle barrier pushback teleports (player hitting invisible wall)
            // These should be processed without any inventory/region logic
            if (barrierPlayers.contains(player.getUniqueId())) {
                barrierPlayers.remove(player.getUniqueId());
                return; // Skip all other processing

            } else {
                // ===== TELEPORTING OUT OF A GAME =====
                if (fromGame != null) {
                    // Ignore teleporting within the same game (no inventory swap needed)
                    if (fromGame.equals(toGame)) {
                        return;
                    } else {
                        // Player is leaving a game
                        if (!teleporting) {
                            // First stage: Check if delay is needed
                            // Skip delay if: game is restarting, game is over, or player is admin
                            if (!fromGame.isGameRestart() && !fromGame.isOver() && !directTeleportPlayers.contains(player.getUniqueId())) {
                                // Cancel this teleport and start delay timer
                                Game lobby = getGameMgr().getGame(LOBBY);
                                delayTeleport(player, event.getFrom(), event.getTo(), fromGame, lobby);
                                event.setCancelled(true);
                                return;
                            } else {
                                // Direct teleport (no delay) - process exit immediately
                                fromGame.getRegion().exit(player); // Call exit handler (may clear inventory)
                                String gameName = PlainTextComponentSerializer.plainText().serialize(fromGame.getName());
                                getBeaconzStore().storeInventory(player, gameName, event.getFrom());
                            }
                        } else {
                            // Second stage: This is the completion of a delayed teleport
                            directTeleportPlayers.remove(player.getUniqueId());
                            fromGame.getRegion().exit(player); // Call exit handler (may clear inventory)
                            String gameName = PlainTextComponentSerializer.plainText().serialize(fromGame.getName());
                            getBeaconzStore().storeInventory(player, gameName, event.getFrom());
                        }                
                    }
                }

                // ===== TELEPORTING OUT OF THE LOBBY =====
                if (fromLobby) {
                    // Ignore teleporting within the lobby
                    if (toLobby) {
                        return;
                    } else {
                        // Leaving lobby - call exit handler
                        getGameMgr().getLobby().exit(player);
                    }
                }

                // ===== TELEPORTING INTO A GAME =====
                if (toGame != null) {
                    // Ignore teleporting within the same game
                    if (fromGame != null && fromGame.equals(toGame)) {
                        return;
                    } else {
                        // Verify player is allowed to enter this game
                        if (toGame.hasPlayer(player) || player.isOp()) {
                            // Restore game-specific inventory
                            String gameName = PlainTextComponentSerializer.plainText().serialize(toGame.getName());
                            Location newTo = getBeaconzStore().getInventory(player, gameName);
                            if (newTo != null) {
                                // Find a safe spawn location near the saved position
                                newTo = toGame.getRegion().findSafeSpot(newTo, 20);
                                event.setTo(newTo);
                            }
                            // Call region enter handler
                            toGame.getRegion().enter(player);

                            // Minigames always give fresh starting kit when entering
                            if (toGame.getGamemode() == GameMode.MINIGAME) {
                                toGame.giveStartingKit(player);
                            }

                        } else {
                            // Player is not authorized for this game - redirect to lobby
                            player.sendMessage(Lang.errorNotInGame
                                    .replaceText(builder -> builder.matchLiteral("[game]").replacement(toGame.getName()))
                                    .color(NamedTextColor.RED));
                            event.setTo(getGameMgr().getLobby().getSpawnPoint());
                        }
                    }
                }

                // ===== TELEPORTING INTO THE LOBBY =====
                if (toLobby) {
                    // Ignore teleporting within the lobby
                    if (fromLobby) {
                        // No action needed
                    } else {
                        // Entering lobby - call enter handler
                        directTeleportPlayers.remove(player.getUniqueId());
                        getGameMgr().getLobby().enterLobby(player);

                    }
                }   
            }                                
        }
    }

    /**
     * Implements a delayed teleport mechanism to prevent combat logging and instant escapes.
     * <p>
     * This method:
     * <ul>
     *   <li>Records the player's current position</li>
     *   <li>Notifies the player to stand still for the configured duration</li>
     *   <li>Starts a delayed task that checks for movement</li>
     *   <li>Cancels teleport if player moved, completes it if player stood still</li>
     * </ul>
     * <p>
     * Ops always teleport instantly (0 delay). The delay is configurable via Settings.teleportDelay.
     * <p>
     * This prevents players from escaping combat or dangerous situations by teleporting away.
     * Movement detection uses Vector comparison to catch any position changes.
     *
     * @param player the player attempting to teleport
     * @param from the location the player is teleporting from
     * @param to the destination location
     * @param fromGame the name of the game being exited
     * @param toGame the name of the destination (usually "Lobby")
     */
    private void delayTeleport(final Player player, final Location from, final Location to, final Game fromGame, final Game toGame) {
        long delay = 20L; // Default: 20 ticks per second of delay

        // Ops get instant teleportation
        if  (player.isOp()) {
            delay = 0L;
        } else {
            // Notify player they must stand still
            player.sendMessage(Lang.teleportDoNotMove
                    .replaceText(builder -> builder.matchLiteral("[number]").replacement(Component.text(String.valueOf(Settings.teleportDelay))))
                    .color(NamedTextColor.RED));
        }

        // Record player's starting position for movement detection
        teleportingPlayers.put(player.getUniqueId(), player.getLocation().toVector());

        // Schedule the delayed teleport check
        getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
            // Verify player is still valid and in the teleporting set
            if (player != null && !player.isDead() && teleportingPlayers.containsKey(player.getUniqueId())) {
                // Check if player's position is still the same
                if (player.getLocation().toVector().equals(teleportingPlayers.get(player.getUniqueId()))) {
                    // Player stood still - proceed with teleport
                    // This will trigger onTeleport again, but with teleporting flag set
                    player.teleportAsync(to);

                } else {
                    // Player moved - cancel the teleport
                    player.sendMessage(Lang.teleportYouMoved.color(NamedTextColor.RED));
                }
            }
            // Clean up tracking data
            teleportingPlayers.remove(player.getUniqueId());
        }, Settings.teleportDelay * delay); // Delay in ticks (configurable seconds * 20 ticks/sec)
    }

    /**
     * Marks a player for direct (instant) teleportation, bypassing the delay mechanism.
     * <p>
     * This is used by admin commands that need to teleport players immediately without
     * waiting for the configured delay period. The player will be removed from this set
     * after their next teleport completes.
     * <p>
     * Use cases:
     * <ul>
     *   <li>Admin commands forcing player movement</li>
     *   <li>Emergency teleportation during game issues</li>
     *   <li>Automatic teleportation during game restart/end</li>
     * </ul>
     *
     * @param directTeleportPlayer UUID of player to set for direct teleport
     */
    public void setDirectTeleportPlayer(UUID directTeleportPlayer) {
        this.directTeleportPlayers.add(directTeleportPlayer);
    }


}
