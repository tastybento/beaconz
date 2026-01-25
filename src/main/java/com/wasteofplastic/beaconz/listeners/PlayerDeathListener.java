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
import com.wasteofplastic.beaconz.config.Params.GameMode;
import com.wasteofplastic.beaconz.game.Game;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Listener class that manages all player death and respawn mechanics.
 * <p>
 * This class handles the critical lifecycle events when players die and respawn,
 * ensuring proper state management across different game modes:
 * <ul>
 *   <li>Inventory preservation and restoration</li>
 *   <li>Experience point management</li>
 *   <li>Health and hunger restoration</li>
 *   <li>Spawn point determination (lobby vs team spawn)</li>
 *   <li>Game mode-specific behavior (MINIGAME vs standard)</li>
 * </ul>
 *
 * <h3>Death Workflow:</h3>
 * <ol>
 *   <li>Player dies in the Beaconz world</li>
 *   <li>System determines appropriate respawn location (lobby or team spawn)</li>
 *   <li>Player exits game region (clears game state)</li>
 *   <li>Inventory is stored for restoration</li>
 *   <li>Death is recorded with respawn location</li>
 *   <li>Health and hunger are reset to full</li>
 * </ol>
 *
 * <h3>Respawn Workflow:</h3>
 * <ol>
 *   <li>Player respawns at lobby spawn point</li>
 *   <li>Lobby inventory is restored</li>
 *   <li>Player can rejoin games when ready</li>
 * </ol>
 *
 * <h3>Game Mode Handling:</h3>
 * <ul>
 *   <li><b>MINIGAME mode:</b> Drops are cleared, inventory preserved</li>
 *   <li><b>Standard mode:</b> Drops may occur based on keepInventory setting</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class PlayerDeathListener extends BeaconzPluginDependent implements Listener {

    /**
     * Tracks players who have died and their designated respawn locations.
     * <p>
     * This map is used to:
     * <ul>
     *   <li>Identify players in the respawn cycle</li>
     *   <li>Store the appropriate respawn location (lobby or team spawn)</li>
     *   <li>Enable proper inventory restoration on respawn</li>
     * </ul>
     *
     * Entries are removed once the player respawns successfully.
     */
    private final HashMap<UUID, Location> deadPlayers = new HashMap<>();

    /**
     * Constant identifier for the lobby game context.
     * Used when storing/retrieving inventory for players in the lobby area.
     */
    protected static final String LOBBY = "Lobby";

    /**
     * Constructs a new PlayerDeathListener.
     * <p>
     * Initializes the listener to handle death events and manage the respawn cycle
     * for players in the Beaconz world.
     *
     * @param plugin The Beaconz plugin instance
     */
    public PlayerDeathListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles player death events in the Beaconz world.
     * <p>
     * This method is the core of the death management system. When a player dies,
     * it performs several critical operations:
     * <ol>
     *   <li>Determines the appropriate respawn location (lobby or team spawn)</li>
     *   <li>Processes region exit to clear game state</li>
     *   <li>Stores the player's inventory for restoration</li>
     *   <li>Handles drop clearing for MINIGAME mode</li>
     *   <li>Manages experience points based on keepLevel setting</li>
     *   <li>Resets health and hunger to prevent death loops</li>
     *   <li>Records the death for respawn processing</li>
     * </ol>
     *
     * <h3>Spawn Point Logic:</h3>
     * <ul>
     *   <li>In lobby: Respawn at lobby spawn point</li>
     *   <li>In game with team: Respawn at team spawn point</li>
     *   <li>In game without team: Respawn at lobby spawn point</li>
     * </ul>
     *
     * <h3>Inventory Handling:</h3>
     * <ul>
     *   <li>Always stored before any modifications</li>
     *   <li>Cleared if drops occur (!keepInventory and not in lobby)</li>
     *   <li>In MINIGAME mode, drops are cleared but inventory is preserved</li>
     * </ul>
     *
     * Note: This uses LOWEST priority to ensure other plugins can modify the event first.
     *
     * @param event The PlayerDeathEvent containing death information and settings
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {

        Player player = event.getEntity();

        // Only process deaths in the Beaconz world
        if (player.getWorld().equals(getBeaconzWorld())) {
            
            // STEP 1: Determine respawn location
            // Default to lobby spawn point
            Location spawnPoint = getGameMgr().getLobby().getSpawnPoint();
            String gameName = LOBBY;
            
            // Check if player died in an active game (not in lobby)
            if (!getGameMgr().getLobby().isPlayerInRegion(player)) {
                // Player died in a game area
                Game game = getGameMgr().getGame(player.getLocation());
                if (game != null) {
                    // Get the game name for inventory storage
                    gameName = PlainTextComponentSerializer.plainText().serialize(game.getName());

                    // Try to get the player's team spawn point
                    Team team = game.getScorecard().getTeam(player);
                    if (team != null) {
                        // Player has a team - respawn at team spawn
                        spawnPoint = game.getScorecard().getTeamSpawnPoint(team);
                    }
                    // If team is null, spawnPoint remains at lobby
                }
            }
            
            // At this point we have:
            // - gameName: Either "Lobby" or the game's name
            // - spawnPoint: Either lobby spawn or team spawn

            // STEP 2: Process region exit (must happen before inventory storage)
            // This clears the player's game state and may modify inventory
            if (!gameName.equals(LOBBY)) {
                getGameMgr().getGame(gameName).getRegion().exit(player);

                // In MINIGAME mode, clear all drops to prevent item loss
                // Players will get their inventory back from storage
                if (getGameMgr().getGame(gameName).getGamemode().equals(GameMode.MINIGAME)) {
                    event.getDrops().clear();
                }
            }            
            
            // STEP 3: Store the player's current inventory
            // They will receive this when they respawn/rejoin
            getBeaconzStore().storeInventory(player, gameName, spawnPoint);
            
            // STEP 4: Handle inventory clearing if drops will occur
            // If keepInventory is false and not in lobby, items will drop
            // We need to clear the stored inventory to match the empty state
            if (!event.getKeepInventory() && !gameName.equals(LOBBY)) {
                getBeaconzStore().clearItems(player, gameName, spawnPoint);
            }
            
            // STEP 5: Handle experience points
            // If keepLevel is false, update stored XP to match what they'll have after death
            if (!event.getKeepLevel()) {
                getBeaconzStore().setExp(player, gameName, event.getNewExp());
            }  

            // STEP 6: Reset health and hunger to full
            // This prevents death loops where players respawn with low health
            // MAX_HEALTH ensures we use the player's actual max health (may have modifiers)
            getBeaconzStore().setHealth(player, gameName, player.getAttribute(Attribute.MAX_HEALTH).getValue());
            getBeaconzStore().setFood(player, gameName, 20); // 20 = full hunger bar

            // STEP 7: Record the death with designated respawn location
            // This allows the respawn handler to restore the correct state
            deadPlayers.put(player.getUniqueId(), spawnPoint);
        }        
    }

    /**
     * Handles player respawn events after death in the Beaconz world.
     * <p>
     * This method completes the death/respawn cycle by:
     * <ol>
     *   <li>Verifying the player died in the Beaconz world (tracked in deadPlayers)</li>
     *   <li>Forcing respawn at the lobby spawn point</li>
     *   <li>Restoring the player's lobby inventory</li>
     *   <li>Removing the death tracking entry</li>
     * </ol>
     *
     * <h3>Why Lobby Spawn?</h3>
     * All players respawn at the lobby regardless of where they died. This ensures:
     * <ul>
     *   <li>Players don't respawn directly back into combat</li>
     *   <li>Players can prepare before rejoining their game</li>
     *   <li>Inventory is consistent (lobby inventory)</li>
     *   <li>Players can choose to join a different game</li>
     * </ul>
     *
     * <h3>Inventory Restoration:</h3>
     * The lobby inventory is restored, which typically contains:
     * <ul>
     *   <li>Game selection items (if configured)</li>
     *   <li>Information items</li>
     *   <li>Basic lobby equipment</li>
     * </ul>
     *
     * When the player rejoins a game, their game-specific inventory will be
     * restored by the game join mechanics.
     * <p>
     * Note: This uses LOWEST priority to run early in the event processing chain,
     * before other plugins modify the respawn behavior.
     *
     * @param event The PlayerRespawnEvent containing respawn information
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRespawn(final PlayerRespawnEvent event) {
        // Check if this player died in the Beaconz world
        // If they're not in deadPlayers, they either didn't die in Beaconz
        // or this is not a respawn from death (could be from /spawn, etc.)
        if (!deadPlayers.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }

        // Force respawn location to the Beaconz lobby
        // This overrides any bed spawn, team spawn, or world spawn
        event.setRespawnLocation(getGameMgr().getLobby().getSpawnPoint());

        // Clean up: Remove from death tracking
        // The player has completed the respawn cycle
        deadPlayers.remove(event.getPlayer().getUniqueId());
        
        // Restore the lobby inventory
        // This gives them the standard lobby equipment/items
        // When they rejoin a game, the game will restore their game inventory
        getBeaconzStore().getInventory(event.getPlayer(), LOBBY);
    }
}
