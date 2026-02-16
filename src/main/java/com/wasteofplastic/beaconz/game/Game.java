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

package com.wasteofplastic.beaconz.game;

import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Params;
import com.wasteofplastic.beaconz.config.Params.GameMode;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.core.Region;
import com.wasteofplastic.beaconz.listeners.BeaconLinkListener;
import com.zaxxer.hikari.HikariDataSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Represents a single game instance in the Beaconz world.
 * <p>
 * A Game is the core entity that manages gameplay within a specific Region. It handles:
 * <ul>
 *   <li><b>Game Lifecycle</b> - Creation, starting, pausing, restarting, and ending games</li>
 *   <li><b>Player Management</b> - Joining, leaving, kicking players; team assignments</li>
 *   <li><b>Game Configuration</b> - Game mode, goals, team count, distance, scoring</li>
 *   <li><b>Persistence</b> - Saving/loading game state to/from games.yml</li>
 *   <li><b>Team Coordination</b> - Managing the scorecard and team interactions</li>
 *   <li><b>Inventory Management</b> - Providing starting kits, clearing inventories</li>
 * </ul>
 * <p>
 * <b>Game-Region Relationship:</b>
 * Each Game belongs to exactly one Region. A Game cannot exist without a Region.
 * The Region defines the physical boundaries where the game takes place.
 * <p>
 * <b>Game Modes:</b>
 * <ul>
 *   <li><b>minigame</b> - Short matches with starting XP, fresh kits each entry</li>
 *   <li><b>persistent</b> - Long-term games where progress is maintained</li>
 * </ul>
 * <p>
 * <b>Game Goals:</b>
 * Games can have different winning conditions defined by:
 * <ul>
 *   <li>gamegoal - The type of goal (e.g., "area", "beacons", "time")</li>
 *   <li>gamegoalvalue - The threshold value to achieve the goal</li>
 * </ul>
 * <p>
 * <b>Lifecycle States:</b>
 * <ul>
 *   <li>Active - Game is running, players can join/play</li>
 *   <li>Paused - Game timer is paused, gameplay continues</li>
 *   <li>Restarting - Game is being reset, players moved to lobby</li>
 *   <li>Over - Game has ended, awaiting restart or deletion</li>
 * </ul>
 * <p>
 * <b>Persistence:</b>
 * Game configuration is saved to games.yml with structure:
 * <pre>
 * game:
 *   GameName:
 *     region: "x1:z1:x2:z2"
 *     gamemode: "minigame"
 *     gamedistance: 100
 *     nbrteams: 4
 *     gamegoal: "area"
 *     goalvalue: 10000
 *     starttime: 1234567890000
 *     countdowntimer: 3600
 *     scoretypes: "area,beacons"
 * </pre>
 *
 * @author tastybento
 */
public class Game extends BeaconzPluginDependent {

    /** The physical region where this game takes place */
    private final Region region;

    /** Unique name identifying this game instance */
    private final Component gameName;

    /** Scorecard managing teams, scores, and game timer */
    private final Scorecard scorecard;
    
    /**
     * Game parameters
     */
    private Params params;

    /** Unix timestamp (in milliseconds) when the game started */
    private Long startTime;

    /** Unix timestamp (in milliseconds) when the game was created */
    private Long gameCreateTime;

    /** Flag indicating the game is currently restarting (prevents double-processing) */
    private boolean gameRestart;

    /** Flag indicating the game has ended */
    private boolean isOver;


    /**
     * Constructs a new Game instance with specified configuration.
     * <p>
     * This constructor:
     * <ol>
     *   <li>Initializes the game with the provided region and name</li>
     *   <li>Calculates the start time (rounded to nearest second)</li>
     *   <li>Records the creation timestamp</li>
     *   <li>Establishes the bidirectional region-game relationship</li>
     *   <li>Stores all game parameters</li>
     *   <li>Creates the scorecard which initializes teams</li>
     * </ol>
     * <p>
     * <b>Time Initialization:</b>
     * startTime is rounded to the nearest second to avoid millisecond precision issues
     * in displays and calculations. The formula ((currentTime + 500) / 1000) * 1000
     * rounds to the nearest 1000ms (1 second).
     * <p>
     * <b>Important:</b> Each game belongs to exactly one Region; a game cannot exist without a region.
     *
     * @param plugin the main plugin instance
     * @param region the physical region where this game takes place
     * @param gameName unique identifier for this game
     * @param params game parameters
     */
    public Game(Beaconz plugin, Region region, Component gameName, Params params) {
        super(plugin);
        this.params = params;
        this.region = region;
        this.gameName = gameName;

        // Round start time to nearest second for cleaner displays
        this.startTime = ((System.currentTimeMillis()+500)/1000)*1000;
        this.gameCreateTime = System.currentTimeMillis();

        // Establish bidirectional relationship between region and game
        region.setGame(this);

        // Store all game configuration parameters
        setGameParms(params, startTime, gameCreateTime);

        // Create the scorecard which manages teams, scores, and game timer
        scorecard = new Scorecard(beaconzPlugin, this);
    }


    /**
     * Handles a plugin reload event.
     * <p>
     * When the plugin is reloaded, ongoing games maintain their state but need
     * to refresh their scorecard displays and team references. This ensures
     * that scoreboard displays continue to function after a reload.
     * <p>
     * <b>What is preserved:</b> Game state, player positions, beacon ownership, teams
     * <br><b>What is refreshed:</b> Scorecard displays and team object references
     */
    public void reload() {
        scorecard.reload();
    }

    /**
     * Restarts the game from a clean slate while preserving teams.
     * <p>
     * This method performs a full game restart:
     * <ol>
     *   <li>Sets the restart flag to prevent concurrent modifications</li>
     *   <li>Teleports all players to lobby (inventory not saved)</li>
     *   <li>Removes ownership from all beacons in this game's region</li>
     *   <li>Resets the game timer to current time</li>
     *   <li>Reloads the scorecard (resets scores but keeps teams)</li>
     *   <li>Clears the restart and game-over flags</li>
     * </ol>
     * <p>
     * <b>Important Notes:</b>
     * <ul>
     *   <li>All beacons become "unowned" but remain physically in place</li>
     *   <li>Teams are preserved - players remain on the same teams</li>
     *   <li>Players will receive starting kits when they rejoin</li>
     *   <li>Inventory is NOT saved during restart (fresh start)</li>
     * </ul>
     * <p>
     * <b>TODO:</b> Determine best approach for giving starting kits to existing team members
     */
    public void restart() {
        // Set restart flag to prevent double-processing
        gameRestart = true;
        
        // Move all players in game to lobby, do not save inventory
        getRegion().sendAllPlayersToLobby(false);
        
        //TODO - figure out how to give players starting kits when they come in, since they will already be in teams...
        
        // Reset all beacons to "unowned" state
        for (BeaconObj beacon : getRegister().getBeaconRegister().values()) {
            if (this.getRegion().containsBeacon(beacon)) {
                // Remove ownership but don't notify (quiet mode during restart)
                getRegister().removeBeaconOwnership(beacon, true);
            }
        }

        // Reset the game timer to current time
        startTime = ((System.currentTimeMillis()+500)/1000)*1000;

        // Reload scorecard (resets scores, keeps teams)
        scorecard.reload();

        // Clear flags to allow normal gameplay
        gameRestart = false;
        isOver = false;
    }

    /**
     * Gets the restart flag status.
     * <p>
     * This flag is used to prevent certain operations during game restart,
     * such as saving inventories or processing normal game events.
     *
     * @return true if the game is currently in the restart process
     */
    public boolean isGameRestart() {
        return gameRestart;
    }

    /**
     * Pauses the game timer.
     * <p>
     * This stops the countdown timer but does NOT prevent gameplay.
     * Players can still capture beacons, create links, etc.
     * Only the time-based game ending is suspended.
     * <p>
     * <b>Use Case:</b> Admin intervention, server issues, waiting for players
     */
    public void pause() {
        scorecard.pause();
    }

    /**
     * Resumes the game timer after a pause.
     * <p>
     * The countdown timer continues from where it was paused.
     * All game state remains unchanged.
     */
    public void resume() {
        scorecard.resume();
    }

    /**
     * Force-ends the game immediately regardless of win conditions.
     * <p>
     * This method:
     * <ul>
     *   <li>Sets the game-over flag to true</li>
     *   <li>Triggers the scorecard's end game sequence</li>
     *   <li>Displays final scores and winner</li>
     *   <li>Does NOT delete the game (use {@link #delete()} for that)</li>
     * </ul>
     * <p>
     * <b>Use Case:</b> Admin commands, server shutdown, emergency stop
     */
    public void forceEnd() {
        isOver = true;
        scorecard.endGame();
    }

    /**
     * Persists the game configuration and state to the SQLite database.
     * <p>
     * This method saves:
     * <ul>
     *   <li>Region boundaries (as separate coordinate columns)</li>
     *   <li>Game mode (minigame/persistent)</li>
     *   <li>Game parameters (distance, teams, goals, timers)</li>
     *   <li>Game state (start time, creation time, isOver flag)</li>
     *   <li>Score configuration (score types as comma-separated string)</li>
     *   <li>Team membership (via scorecard)</li>
     * </ul>
     * <p>
     * Uses UPSERT (INSERT OR REPLACE) to handle both new games and updates.
     * All operations are wrapped in a transaction for data integrity.
     * <p>
     * <b>Note:</b> Team membership is saved separately via scorecard.saveTeamMembers()
     * <br><b>Thread Safety:</b> This method is NOT thread-safe. Should be called from main thread.
     */
    public void save() {
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot save game: " + gameName);
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Convert score types list to comma-separated string
                String scoreTypesStr = params.getScoretypes().stream()
                    .map(GameScoreGoal::name)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

                // Get region corners
                Point2D[] corners = region.corners();
                String plainGameName = PlainTextComponentSerializer.plainText().serialize(gameName);

                // UPSERT game data (INSERT OR REPLACE for SQLite)
                String sql = "INSERT OR REPLACE INTO games " +
                    "(game_name, region_x1, region_z1, region_x2, region_z2, gamemode, " +
                    "game_distance, nbr_teams, game_goal, goal_value, start_time, " +
                    "creation_time, countdown_timer, score_types, game_over, game_distribution) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, plainGameName);
                    stmt.setDouble(2, corners[0].getX());
                    stmt.setDouble(3, corners[0].getY());
                    stmt.setDouble(4, corners[1].getX());
                    stmt.setDouble(5, corners[1].getY());
                    stmt.setString(6, params.getGamemode().name());
                    stmt.setInt(7, params.getSize());
                    stmt.setInt(8, params.getTeams());
                    stmt.setString(9, params.getGoal().name());
                    stmt.setInt(10, params.getGoalvalue());
                    stmt.setLong(11, startTime);
                    stmt.setLong(12, gameCreateTime);
                    stmt.setInt(13, scorecard.getCountdownTimer());
                    stmt.setString(14, scoreTypesStr);
                    stmt.setInt(15, isOver ? 1 : 0);
                    stmt.setDouble(16, params.getDistribution());

                    stmt.executeUpdate();
                }

                conn.commit();
                getLogger().info("Game '" + plainGameName + "' saved to database");

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to save game " + gameName + " to database: " + e.getMessage());
        }

        // Save team membership data separately
        scorecard.saveTeamMembers();
    }

    /**
     * Permanently deletes this game instance from the database.
     * <p>
     * This method performs a complete cleanup:
     * <ol>
     *   <li>Ends the game if still active (via {@link #forceEnd()})</li>
     *   <li>Deletes all team data and membership</li>
     *   <li>Teleports all players in the game to lobby (inventory not saved)</li>
     *   <li>Removes this game's data from the database</li>
     * </ol>
     * <p>
     * <b>Warning:</b> This operation is irreversible. All game data is permanently lost.
     * <p>
     * <b>Important Notes:</b>
     * <ul>
     *   <li>Beacons remain in the world (physical blocks not removed)</li>
     *   <li>The Region object may still exist (game-region link is broken)</li>
     *   <li>Player inventories are NOT saved during deletion</li>
     *   <li>Team memberships are permanently removed</li>
     * </ul>
     */
    public void delete() {
        
        // End the game if it's still active
        if (!isOver) forceEnd();
        
        // Delete all team data and memberships
        scorecard.deleteTeamMembers();
        
        // Teleport any players in the game to the lobby, do not save inventory
        region.sendAllPlayersToLobby(false);
        
        // Delete from database
        HikariDataSource dataSource = getBeaconzPlugin().getDataSource();
        if (dataSource == null) {
            getLogger().severe("Database not initialized! Cannot delete game: " + gameName);
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            String plainGameName = PlainTextComponentSerializer.plainText().serialize(gameName);
            String sql = "DELETE FROM games WHERE game_name = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, plainGameName);
                int deleted = stmt.executeUpdate();

                if (deleted > 0) {
                    getLogger().info("Game '" + plainGameName + "' deleted from database");
                } else {
                    getLogger().warning("Game '" + plainGameName + "' not found in database");
                }
            }

        } catch (SQLException e) {
            getLogger().severe("Failed to delete game " + gameName + " from database: " + e.getMessage());
        }

    }

    /**
     * Handles a player joining this game with default behavior (teleport to team home).
     * <p>
     * This is a convenience method that calls {@link #join(Player, boolean)} with goHome=true.
     *
     * @param player the player joining the game
     * @see #join(Player, boolean) for detailed joining workflow
     */
    public void join(Player player) {
        join(player, true);
    }

    /**
     * Handles a player joining or returning to this game.
     * <p>
     * <b>Join Workflow:</b>
     * <ol>
     *   <li>Determine if player is new or returning (checks team membership)</li>
     *   <li>Send appropriate welcome message</li>
     *   <li>Assign player to a team (balanced assignment if new player)</li>
     *   <li>Teleport to team home location (if goHome=true)</li>
     *   <li>Call region.enter() to process region entry</li>
     *   <li>Refresh scorecard displays for all players</li>
     *   <li>Give starting kit to new players</li>
     * </ol>
     * <p>
     * <b>New vs Returning Players:</b>
     * <ul>
     *   <li><b>New:</b> Receives "Welcome to game", gets team assignment, receives starting kit</li>
     *   <li><b>Returning:</b> Receives "Welcome back", keeps existing team, inventory preserved</li>
     * </ul>
     * <p>
     * <b>Team Assignment:</b>
     * The scorecard automatically balances teams when assigning new players.
     * Players are assigned to the team with the fewest members.
     * <p>
     * <b>goHome Parameter:</b>
     * <ul>
     *   <li><b>true</b> - Player is teleported to team home spawn point (triggers region.enter via event)</li>
     *   <li><b>false</b> - Player stays at current location (region.enter called directly)</li>
     * </ul>
     *
     * @param player the player joining the game
     * @param goHome if true, teleports player to team home; if false, player stays at current location
     */
    public void join(Player player, boolean goHome) {
        // Determine if this is a new player or returning player
        boolean newPlayer = !scorecard.inTeam(player);

        // Send appropriate welcome message
        if (newPlayer) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.titleWelcomeToGame,
                    Placeholder.component("name", gameName)));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.titleWelcomeBackToGame,
                    Placeholder.component("name", gameName)));
        }

        // Assign player to a team (balanced assignment if new)
        scorecard.assignTeam(player);

        // Handle teleportation and region entry
        if (goHome) {
            // Teleport to team home - this triggers PlayerTeleportListener, which calls region.enter()
            scorecard.sendPlayersHome(player, false);
        } else {
            // Player not teleporting, so manually process region entry
            region.enter(player);
        }

        // Update scorecard displays for all players
        scorecard.refreshScores();

        // Give starting kit to new players only
        if (newPlayer) {
            giveStartingKit(player);
        }
    }

    /**
     * Gives a player the starting kit and clears their inventory.
     * <p>
     * <b>Starting Kit Contents:</b>
     * The kit is defined in {@link Settings#newbieKit} and typically includes:
     * <ul>
     *   <li>Basic tools (pickaxe, shovel)</li>
     *   <li>Building blocks</li>
     *   <li>Food/supplies</li>
     *   <li>Emerald blocks (for beacon expansion)</li>
     * </ul>
     * <p>
     * <b>Minigame Mode:</b>
     * In minigame mode, also sets the player's XP to {@link Settings#initialXP}.
     * This XP is used as currency for creating beacon links.
     * <p>
     * <b>Overflow Handling:</b>
     * If the player's inventory is too full to receive all items,
     * excess items are dropped at the player's location.
     * <p>
     * <b>Important:</b> This completely clears the player's inventory first.
     * Any existing items are permanently lost.
     *
     * @param player the player to give the starting kit to
     */
    public void giveStartingKit(Player player) {
        // For minigame mode, set starting XP level (used as link currency)
        if (params.getGamemode() == GameMode.MINIGAME) {
            BeaconLinkListener.setTotalExperience(player, Settings.initialXP);
        }

        // Clear inventory to ensure fresh start
        player.getInventory().clear();

        // Add each item from the starting kit
        for (ItemStack item : Settings.newbieKit) {
            // Try to add item to inventory
            HashMap<Integer, ItemStack> tooBig = player.getInventory().addItem(item);

            // If inventory is full, drop overflow items at player's location
            if (!tooBig.isEmpty()) {
                for (ItemStack items : tooBig.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), items);
                }
            }
        }
    }
    
    /**
     * Kicks all players from this game.
     * <p>
     * Iterates through all online players and removes them from this game.
     * Players are teleported to lobby without inventory saving.
     * <p>
     * <b>Use Case:</b> Game shutdown, emergency evacuation, game restart
     */
    public void kickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            leave(null, player);
        }
    }

    /**
     * Kicks a specific player from this game (admin command).
     * <p>
     * This is a convenience method for admin commands that removes a player
     * from the game and sends confirmation to the command sender.
     *
     * @param sender the admin who issued the kick command
     * @param player the player to kick from the game
     */
    public void kick(CommandSender sender, Player player) {
        leave(sender, player);
    }

    /**
     * Removes a player from this game permanently.
     * <p>
     * This method:
     * <ol>
     *   <li>Checks if player is actually in a team</li>
     *   <li>Removes player from their team</li>
     *   <li>Resets player's scoreboard to default</li>
     *   <li>Removes any beacon maps from player's inventory</li>
     *   <li>Teleports player to lobby (inventory not saved)</li>
     *   <li>Sends success/error message to command sender</li>
     * </ol>
     * <p>
     * <b>Map Cleanup:</b>
     * The method scans the player's inventory for filled maps and removes any
     * beacon maps from the register. This prevents map duplication exploits.
     * <p>
     * <b>Important Notes:</b>
     * <ul>
     *   <li>Player is permanently removed from team (not just exiting region)</li>
     *   <li>Inventory is NOT saved when leaving</li>
     *   <li>Scoreboard is reset to server default</li>
     *   <li>Maps are removed from beacon map registry</li>
     * </ul>
     *
     * @param sender the command sender (receives confirmation messages), can be null
     * @param player the player being removed from the game
     */
    public void leave(CommandSender sender, Player player) {
        // Get the player's current team
        Team team = scorecard.getTeam(player);

        if (team != null) {
            // Player will no longer be a part of the game (not just exiting the region)
            scorecard.removeTeamPlayer(player);

            // Reset scoreboard to server default
            player.setScoreboard(scorecard.getManager().getNewScoreboard());

            // Remove any beacon maps from player's inventory
            for (ItemStack item: player.getInventory()) {
                if (item != null && item.getType().equals(Material.FILLED_MAP)) {
                    if (item.getItemMeta() instanceof MapMeta mapMeta) {
                        // Check if the map has an ID associated with it
                        if (mapMeta.hasMapId()) {
                            // Remove it from the beacon map register
                            getRegister().removeBeaconMap(mapMeta.getMapId());
                        }
                    }
                }
            }

            // Send success message to command sender
            sender.sendMessage(Lang.generalSuccess);
        } else if (sender != null){
            // Player wasn't in this game
            sender.sendMessage(MiniMessage.miniMessage().deserialize(Lang.errorNotInGame,
                    Placeholder.component("game", gameName)));
        }

        // Teleport player to lobby, do not save inventory
        getRegion().sendToLobby(player, false);
    }
    
    /**
     * Removes a player from this game (convenience method).
     * <p>
     * Sends messages to the player themselves (player is both actor and target).
     *
     * @param player the player leaving the game
     * @see #leave(CommandSender, Player) for detailed removal process
     */
    public void leave(Player player) {
        leave(player, player);
    }

    // ========== Getter Methods ==========

    /**
     * Gets the unique name of this game.
     * <p>
     * Game names are used as identifiers in commands and file storage.
     *
     * @return the game's name
     */
    public Component getName() {
        return gameName;
    }

    /**
     * Gets the physical region where this game takes place.
     * <p>
     * The region defines the boundaries of the game area.
     *
     * @return the game's region
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Gets the scorecard managing teams and scores for this game.
     * <p>
     * The scorecard handles all team-related operations and score tracking.
     *
     * @return the game's scorecard
     */
    public Scorecard getScorecard() {
        return scorecard;
    }

    /**
     * Gets the game mode.
     *
     * @return "minigame" or "persistent"
     */
    public GameMode getGamemode() {return params.getGamemode();}

    /**
     * Gets the minimum distance between beacons.
     *
     * @return minimum beacon distance in blocks
     */
    public int getGamedistance() {return params.getSize();}

    /**
     * Gets the beacon distribution factor.
     *
     * @return distribution value (0.0-1.0)
     */
    public double getGamedistribution() {return params.getDistribution();}

    /**
     * Gets the number of teams in this game.
     *
     * @return team count
     */
    public int getNbrTeams() {return params.getTeams();}

    /**
     * Gets the game goal type.
     *
     * @return goal type (e.g., "area", "beacons", "time")
     */
    public GameScoreGoal getGamegoal() {return params.getGoal();}

    /**
     * Gets the game goal value threshold.
     *
     * @return goal value to achieve victory
     */
    public int getGamegoalvalue() {return params.getGoalvalue();}

    /**
     * Gets the countdown timer duration.
     *
     * @return timer in seconds (0 = unlimited)
     */
    public int getCountdownTimer() {return params.getCountdown();}

    /**
     * Gets the game start timestamp.
     *
     * @return Unix timestamp in milliseconds when game started
     */
    public Long getStartTime() {return startTime;}

    /**
     * Gets the game creation timestamp.
     *
     * @return Unix timestamp in milliseconds when game was created
     */
    public Long getCreateTime() {return gameCreateTime;}

    /**
     * Gets the comma-separated score types being tracked.
     *
     * @return score types string (e.g., "area,beacons,triangles")
     */
    public List<GameScoreGoal> getScoretypes() {return params.getScoretypes();}

    // ========== Setter Methods ==========

    /**
     * Sets all game parameters at once.
     * <p>
     * This is typically called during game creation or loading from file.
     * All parameters are stored as instance variables.
     *
     * @param parameters game parameters
     * @param startTime Unix timestamp when game started
     * @param createTime Unix timestamp when game was created
     */
    public void setGameParms(Params parameters, Long startTime, Long createTime) {
        this.params = parameters;
        this.startTime = startTime;
        this.gameCreateTime = createTime;
    }

    /**
     * Sets the game mode.
     *
     * @param gm "minigame" or "persistent"
     */
    public void setGamemode(GameMode gm) {params.setGamemode(gm);}

    /**
     * Sets the minimum beacon distance.
     *
     * @param gd distance in blocks
     */
    public void setGamedistance(int gd) {params.setSize(gd);}

    /**
     * Sets the number of teams.
     *
     * @param tn team count
     */
    public void setNbrTeams(int tn) {params.setTeams(tn);}

    /**
     * Sets the game goal type.
     *
     * @param gg goal type (e.g., "area", "beacons")
     */
    public void setGamegoal(GameScoreGoal gg) {params.setGoal(gg);}

    /**
     * Sets the game goal value.
     *
     * @param gv threshold value to win
     */
    public void setGamegoalvalue(int gv) {params.setGoalvalue(gv);}

    /**
     * Sets the countdown timer duration.
     *
     * @param cd duration in seconds (0 = unlimited)
     */
    public void setCountdownTimer(int cd) {params.setCountdown(cd);}
    

    /**
     * Sets the game start time.
     *
     * @param stt Unix timestamp in milliseconds
     */
    public void setStartTime(Long stt) {startTime = stt;}

    /**
     * Sets the score types to track.
     *
     * @param sct comma-separated score types
     */
    public void setScoretypes(List<GameScoreGoal> sct) {params.setScoretypes(sct);}

    /**
     * Sets the beacon distribution factor.
     *
     * @param gdist distribution value (0.0-1.0)
     */
    public void setGamedistribution(double gdist) {params.setDistribution(gdist);}

    // ========== Utility Methods ==========


    /**
     * Converts a Bukkit Location to a simple string representation.
     * <p>
     * This is a utility method for serializing locations to configuration files.
     * <p>
     * <b>Format:</b> "worldname:x:y:z:yaw:pitch"
     * <p>
     * Yaw and pitch are stored as integer bits using {@link Float#floatToIntBits(float)}
     * to avoid floating-point precision issues in YAML files.
     * <p>
     * <b>Null Handling:</b> Returns empty string if location or world is null.
     *
     * @param l the location to convert
     * @return string representation, or empty string if null
     */
    static public String getStringLocation(final Location l) {
        if (l == null || l.getWorld() == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" + Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
    }

    /**
     * Checks if this game has a specific player as a member.
     * <p>
     * This checks actual team membership, not just physical presence in the region.
     * A player can be in the region without being part of the game.
     *
     * @param player the player to check
     * @return true if player is a member of any team in this game
     */
    public boolean hasPlayer(Player player) {
        return scorecard.inTeam(player);
    }

    /**
     * Checks if the game has ended.
     * <p>
     * A game is "over" when it has reached its goal condition or been force-ended.
     * Over games typically cannot have new players join until restarted.
     *
     * @return true if game is over
     */
    public boolean isOver() {
        return isOver;
    }

    /**
     * Sets the game over flag.
     * <p>
     * This is typically called by the scorecard when a win condition is met.
     * <p>
     * <b>Note:</b> Setting this flag doesn't trigger end-game logic.
     * Use {@link #forceEnd()} to properly end a game.
     *
     * @param isOver true to mark game as over
     */
    public void setOver(boolean isOver) {
        this.isOver = isOver;
    }


}
