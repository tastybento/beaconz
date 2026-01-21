package com.wasteofplastic.beaconz.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;

/**
 * Listener class that manages player connection and disconnection events.
 * <p>
 * This class handles the critical player lifecycle events when players join or leave
 * the server, ensuring proper state management and game integration:
 * <ul>
 *   <li>Player name database synchronization</li>
 *   <li>Game state restoration on login</li>
 *   <li>Lobby vs game placement logic</li>
 *   <li>Team membership verification</li>
 *   <li>Message queue delivery</li>
 *   <li>Potion effect cleanup on logout</li>
 *   <li>Beacon standing tracking cleanup</li>
 * </ul>
 *
 * <h3>Join Workflow:</h3>
 * When a player joins the Beaconz world, the system:
 * <ol>
 *   <li>Saves the player's name to the database (for offline reference)</li>
 *   <li>Checks if they're joining in an active game area</li>
 *   <li>Determines if the game is valid and ongoing</li>
 *   <li>Verifies team membership if in a game</li>
 *   <li>Places player in lobby or rejoins them to their game</li>
 *   <li>Delivers any pending messages from the queue</li>
 * </ol>
 *
 * <h3>Placement Logic:</h3>
 * <ul>
 *   <li><b>No game / game over / restarting:</b> Send to lobby</li>
 *   <li><b>Game active but no team:</b> Send to lobby</li>
 *   <li><b>Game active with team:</b> Rejoin at last location</li>
 * </ul>
 *
 * <h3>Leave Workflow:</h3>
 * When a player leaves the server, the system:
 * <ol>
 *   <li>Removes beacon standing tracking (prevents memory leaks)</li>
 *   <li>Clears all potion effects (clean state for next login)</li>
 *   <li>Optionally stores inventory state (commented out currently)</li>
 * </ol>
 *
 * @author tastybento
 * @since 1.0
 */
public class PlayerJoinLeaveListener extends BeaconzPluginDependent implements Listener {

    /** Debug flag for verbose logging of join/leave events */
    private final static boolean DEBUG = false;

    /**
     * Constructs a new PlayerJoinLeaveListener.
     * <p>
     * Initializes the listener to handle player connection lifecycle events
     * and manage state transitions between lobby and active games.
     *
     * @param plugin The Beaconz plugin instance
     */
    public PlayerJoinLeaveListener(Beaconz plugin) {
        super(plugin);
    }
    /**
     * Handles player join events for players entering the Beaconz world.
     * <p>
     * This method is the primary entry point for players joining the server. It performs
     * several critical operations to ensure players are placed correctly and have their
     * state properly restored:
     * <ol>
     *   <li>Records the player's name in the database for offline lookups</li>
     *   <li>Determines the game state at the player's login location</li>
     *   <li>Validates game status (active, over, or restarting)</li>
     *   <li>Checks team membership if joining in a game area</li>
     *   <li>Places player in lobby or rejoins them to their game</li>
     *   <li>Delivers pending messages from the message queue</li>
     * </ol>
     *
     * <h3>Placement Decision Tree:</h3>
     * <pre>
     * Is player in Beaconz world?
     *   No  → Do nothing (wrong world)
     *   Yes → Is there a game at this location?
     *     No / game over / restarting → Send to lobby
     *     Yes → Does player have a team?
     *       No  → Send to lobby
     *       Yes → Rejoin game at last location
     * </pre>
     *
     * <h3>Message Delivery:</h3>
     * Any messages queued while the player was offline are delivered 2 seconds
     * (40 ticks) after login to ensure the player is fully loaded and ready to
     * receive them.
     *
     * @param event The PlayerJoinEvent containing the joining player information
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onJoin(final PlayerJoinEvent event) {
        // Only process players joining the Beaconz world
        // Players in other worlds are ignored
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            final Player player = event.getPlayer();
            final UUID playerUUID = player.getUniqueId();
            
            // STEP 1: Save player name to database
            // This allows offline references (e.g., showing who owns beacons when they're offline)
            getBeaconzPlugin().getNameStore().savePlayerName(player.getName(), player.getUniqueId());

            // STEP 2: Determine game state at login location
            // Check if the player logged in within an active game area
            Game game = getGameMgr().getGame(event.getPlayer().getLocation());

            // STEP 3: Validate game status and handle lobby placement
            if (game == null || game.isOver() || game.isGameRestart()) {
                // SCENARIO 1: No game exists, game is over, or game is restarting
                // Send player to the lobby for safety and to choose a game

                if (DEBUG) {
                    getLogger().info("DEBUG: Game is " + game);
                    if (game != null) {
                        getLogger().info("DEBUG: Game is over " + game.isOver());
                        getLogger().info("DEBUG: Game is restart " + game.isGameRestart());
                    }
                }

                // Teleport to lobby spawn point (true = send messages)
                getGameMgr().getLobby().tpToRegionSpawn(player, true);

                // Process lobby entry (restore lobby inventory, clear game state, etc.)
                getGameMgr().getLobby().enterLobby(event.getPlayer());
                
            } else {
                // SCENARIO 2: Active game exists at this location

                if (DEBUG)
                    getLogger().info("DEBUG: game exists");

                // STEP 4: Check team membership
                if (game.getScorecard().getTeam(player) == null) {
                    // SCENARIO 2A: Player is in a game area but has no team
                    // This can happen if they left mid-game or the game was reset

                    if (DEBUG)
                        getLogger().info("DEBUG: Player is not in a team");

                    // Send to lobby - they need to join a team before playing
                    getGameMgr().getLobby().tpToRegionSpawn(player, true);
                    getGameMgr().getLobby().enterLobby(event.getPlayer());
                    
                } else {
                    // SCENARIO 2B: Player is in a game area and has a team
                    // Rejoin them to continue playing

                    if (DEBUG) {
                        getLogger().info("DEBUG: Player is in team - " + game.getScorecard().getTeam(player));
                        getLogger().info("DEBUG: Player is in team - " + game.getScorecard().getTeam(player).getDisplayName());
                    }

                    // Join the game but don't teleport (false = stay at current location)
                    // This allows players to log back in where they logged out
                    game.join(player, false);
                }
            }

            // STEP 5: Deliver pending messages
            // Check if there are any messages waiting for the player
            if (DEBUG)
                getLogger().info("DEBUG: Checking messages for " + player.getName());

            final List<String> messages = getMessages().getMessages(playerUUID);
            if (messages != null) {
                // Messages exist - schedule delivery after a short delay
                // Delay ensures player is fully loaded and can see the messages
                getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
                    // Send header
                    player.sendMessage(Lang.titleBeaconzNews);

                    // Send each message with a number
                    int i = 1;
                    for (String message : messages) {
                        player.sendMessage(i++ + ": " + message);
                    }

                    // Clear the message queue now that they've been delivered
                    getMessages().clearMessages(playerUUID);
                }, 40L); // 40 ticks = 2 seconds delay
            }

            if (DEBUG)
                getLogger().info("DEBUG: no messages");
        }
    }

    /**
     * Handles player quit events for cleanup and state preservation.
     * <p>
     * This method ensures a clean disconnect by performing essential cleanup operations
     * to prevent memory leaks and state corruption. The operations performed are:
     * <ol>
     *   <li>Removes beacon standing tracking to free memory</li>
     *   <li>Clears all potion effects for clean state on next login</li>
     *   <li>Optionally stores inventory state (currently disabled)</li>
     * </ol>
     *
     * <h3>Cleanup Operations:</h3>
     * <ul>
     *   <li><b>Beacon Standing:</b> Prevents memory leaks from the beacon protection system</li>
     *   <li><b>Potion Effects:</b> Ensures players don't log back in with old buffs/debuffs</li>
     * </ul>
     *
     * <h3>Why Clean Potion Effects?</h3>
     * Potion effects from beacons, mining penalties, or combat should not persist across
     * login sessions. Clearing them ensures:
     * <ul>
     *   <li>No advantage from logging out with beneficial effects</li>
     *   <li>No disadvantage from logging out with negative effects</li>
     *   <li>Fresh state on each login</li>
     * </ul>
     *
     * Note: Inventory storage is currently commented out. If enabled, it would save
     * the player's inventory state when they log out from within a game.
     *
     * @param event The PlayerQuitEvent containing the leaving player information
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onLeave(final PlayerQuitEvent event) {

        // CLEANUP 1: Remove beacon standing tracking
        // This prevents memory leaks in the beacon protection system
        // The map tracks which beacon a player is standing on for field effects
        BeaconProtectionListener.getStandingOn().remove(event.getPlayer().getUniqueId());

        // CLEANUP 2: Remove all potion effects if in Beaconz world
        // This ensures players don't keep buffs/debuffs across login sessions
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            // Iterate through all active potion effects
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects()) {
                // Remove each effect by type
                event.getPlayer().removePotionEffect(effect.getType());
            }
        }

        // CLEANUP 3: Optional inventory storage (currently disabled)
        // This would save the player's inventory when they log out from a game
        final Game fromGame = getGameMgr().getGame(event.getPlayer().getLocation());
        if (fromGame != null) {
            // TODO: Consider re-enabling this if players need inventory persistence across logouts
            // Currently commented out - inventory is only saved on death or game transitions
            //getBeaconzStore().storeInventory(event.getPlayer(), fromGame.getName(), null);
        }
    }

}
