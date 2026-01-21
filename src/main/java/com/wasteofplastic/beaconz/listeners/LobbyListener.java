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

import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Settings;

import net.kyori.adventure.text.Component;

/**
 * Listener class that manages all lobby-related events and interactions.
 * <p>
 * The lobby is a safe zone where players wait between games and can:
 * <ul>
 *   <li>Join games by clicking signs that represent active game instances</li>
 *   <li>Be protected from mob spawning (configurable)</li>
 *   <li>Place game signs (admins only)</li>
 * </ul>
 *
 * <h3>Sign System:</h3>
 * Game signs in the lobby follow a specific format:
 * <ol>
 *   <li>Line 0: Must contain the configured sign keyword from Lang.adminSignKeyword</li>
 *   <li>Lines 1-3: Can contain game names that players can click to join</li>
 * </ol>
 *
 * When a player left-clicks a properly formatted sign, they are automatically
 * joined to the corresponding game (if it exists and is not over).
 *
 * <h3>Mob Control:</h3>
 * The lobby can be configured to prevent mob spawning to maintain a safe environment:
 * <ul>
 *   <li>Monster/Slime spawning can be disabled (Settings.allowLobbyMobSpawn)</li>
 *   <li>Animal spawning can be disabled (Settings.allowLobbyAnimalSpawn)</li>
 *   <li>Spawn eggs can be restricted (Settings.allowLobbyEggs)</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class LobbyListener extends BeaconzPluginDependent implements Listener {

    /** Debug flag for verbose logging of lobby events */
    private final static boolean DEBUG = false;

    /**
     * Constructs a new LobbyListener.
     * <p>
     * Initializes the listener to handle lobby sign interactions and mob spawning control.
     *
     * @param plugin The Beaconz plugin instance
     */
    public LobbyListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles player interactions with game signs in the lobby.
     * <p>
     * This method allows players to join games by left-clicking signs that are properly
     * formatted with game information. The sign format is:
     * <ul>
     *   <li>Line 0: Sign keyword (from Lang.adminSignKeyword)</li>
     *   <li>Lines 1-3: Game names that can be joined</li>
     * </ul>
     *
     * Validation checks performed:
     * <ol>
     *   <li>Must be clicking a block (not air)</li>
     *   <li>Must be a left-click action</li>
     *   <li>Block must be a sign</li>
     *   <li>Must be in the Beaconz world</li>
     *   <li>Player must be in the lobby region</li>
     *   <li>Game must exist and not be over</li>
     * </ol>
     *
     * Creative mode players are prevented from clicking signs to avoid accidentally
     * breaking them.
     *
     * @param event The PlayerInteractEvent for clicking blocks
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSignClick(final PlayerInteractEvent event) {
        // Verify the player clicked a block (not air)
        if (!event.hasBlock()) {
            return;
        }

        // Only process left-clicks (right-click would be for editing)
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getClickedBlock() == null) {
            return;
        }

        // Verify the clicked block is a sign (any type of sign)
        if (!Tag.SIGNS.isTagged(event.getClickedBlock().getType())) {
            return;
        }

        // Quick world check - only process events in the Beaconz world
        if (!event.getClickedBlock().getWorld().equals(getBeaconzWorld())) {
            return; 
        }

        // Verify the player is actually in the lobby region
        if (getGameMgr().getLobby().isPlayerInRegion(event.getPlayer())) {
            // Prevent creative mode players from accidentally breaking signs
            // Creative mode left-clicks can break blocks instantly
            if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                event.getPlayer().sendMessage(Lang.adminUseSurvival);
                event.setCancelled(true);
            }

            // Read the sign content to find game names
            Sign sign = (Sign) event.getClickedBlock().getState();
            @NotNull SignSide side = sign.getSide(Side.FRONT);

            // Check if line 0 contains the configured sign keyword
            if (side.line(0).equals(Lang.adminSignKeyword)) {
                // Check lines 1-3 for game names
                for (int i = 1; i < 4; i++) {
                    Component gamename = side.line(i);

                    // Check if a game with this name exists
                    if (getGameMgr().getGame(gamename) != null) {
                        // Verify the game is not already over
                        if (getGameMgr().getGame(gamename).isOver()) {
                            event.getPlayer().sendMessage(Lang.scoreGameOver);
                        } else {
                            // Join the player to the game
                            getGameMgr().getGame(gamename).join(event.getPlayer());
                        }
                        return; // Found a valid game, stop checking other lines
                    }
                }
                // Sign has the keyword but no valid games found
                event.getPlayer().sendMessage(Lang.errorNotReady);
                event.getPlayer().sendMessage(Lang.errorNoSuchGame);
            }
        }
    } 

    /**
     * Validates and confirms when admins place game signs in the lobby.
     * <p>
     * This handler provides immediate feedback to administrators when they create
     * a sign that will allow players to join games. It validates that:
     * <ul>
     *   <li>The sign is placed in the Beaconz world</li>
     *   <li>The sign is placed in the lobby region</li>
     *   <li>Line 0 contains the configured sign keyword</li>
     *   <li>At least one of lines 1-3 contains a valid, existing game name</li>
     * </ul>
     *
     * If validation succeeds, the admin receives a confirmation message.
     * If the sign has the keyword but no valid games, an error is shown.
     * <p>
     * Note: This is informational only and doesn't prevent sign creation.
     * Invalid signs will simply not function when players click them.
     *
     * @param event The SignChangeEvent when a sign's text is modified
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSignPlace(final SignChangeEvent event) {
        // Quick world check - only process events in the Beaconz world
        if (!event.getBlock().getWorld().equals(getBeaconzWorld())) {
            return; 
        }

        // Verify the sign is being placed in the lobby region
        if (getGameMgr().getLobby().isPlayerInRegion(event.getPlayer())) {
            // Check if line 0 contains the configured sign keyword
            if (event.line(0) != null && event.line(0).contains(Lang.adminSignKeyword)) {
                // Check lines 1-3 for valid game names
                for (int i = 1; i < 4; i++) {
                    // Verify a game with this name exists in the system
                    if (event.line(i) != null && getGameMgr().getGame(event.line(i)) != null) {
                        // Success! Inform the admin that the sign is valid and functional
                        event.getPlayer().sendMessage(Lang.adminGameSignPlaced
                                .append(Component.text(" - ")
                                        .append(event.line(i))));
                        return; // Found at least one valid game, sign is functional
                    }
                }
                // Sign has the keyword but no valid game names found
                // Warn the admin that the sign won't work
                event.getPlayer().sendMessage(Lang.errorNoSuchGame);
            }
        }
    }

    /**
     * Controls mob spawning in the lobby area based on configuration settings.
     * <p>
     * The lobby is typically a safe zone, and this handler enforces spawn restrictions
     * to maintain that safety. Three categories of spawning are controlled:
     * <ol>
     *   <li><b>Spawn Eggs:</b> Can be completely disabled via Settings.allowLobbyEggs</li>
     *   <li><b>Monsters/Slimes:</b> Can be disabled via Settings.allowLobbyMobSpawn</li>
     *   <li><b>Animals:</b> Can be disabled via Settings.allowLobbyAnimalSpawn</li>
     * </ol>
     *
     * This allows fine-grained control over the lobby environment:
     * <ul>
     *   <li>Completely safe lobby (no mobs, no animals, no eggs)</li>
     *   <li>Peaceful lobby with animals (animals allowed, monsters blocked)</li>
     *   <li>Natural lobby (all spawning allowed)</li>
     * </ul>
     *
     * The checks are performed in priority order to minimize processing:
     * spawn eggs first, then monsters, then animals.
     *
     * @param e The CreatureSpawnEvent for any entity attempting to spawn
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (DEBUG) {
            getLogger().info(e.getEventName());
        }

        // Quick location check - only process spawns in the lobby region
        // If not in lobby or lobby isn't loaded yet, allow normal spawning
        if (!getGameMgr().getLobby().contains(e.getLocation())) {
            return;
        }

        // PRIORITY 1: Check spawn eggs (most restrictive)
        // This prevents players from bypassing mob restrictions with spawn eggs
        if (!Settings.allowLobbyEggs &&
                (e.getSpawnReason().equals(SpawnReason.SPAWNER_EGG) ||
                 e.getSpawnReason().equals(SpawnReason.EGG))) {
            e.setCancelled(true);
            return; // Spawn egg blocked - no further checks needed
        }

        // PRIORITY 2: Check hostile mobs (monsters and slimes)
        if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime) {
            if (!Settings.allowLobbyMobSpawn) {
                // Hostile mobs are not allowed to spawn in the lobby
                e.setCancelled(true);
                return;
            }
            // If we reach here, hostile mob spawning is allowed
        }

        // PRIORITY 3: Check passive mobs (animals)
        // Animals include cows, pigs, chickens, horses, etc.
        if (e.getEntity() instanceof Animals) {
            if (!Settings.allowLobbyAnimalSpawn) {
                // Animals are not allowed to spawn in the lobby
                e.setCancelled(true);
            }
            // If we reach here, animal spawning is allowed
        }

        // Note: Other entity types (villagers, armor stands, etc.) are not restricted
        // and will spawn normally in the lobby
    }

}
