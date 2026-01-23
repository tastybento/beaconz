/*
 * Copyright (c) 2015 - 2025 tastybento
 */

package com.wasteofplastic.beaconz.game;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.core.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Manages all scoring, team, and game state aspects for a Beaconz game instance.
 *
 * <p>The Scorecard is the central hub for game management, responsible for:
 * <ul>
 *   <li><b>Scoreboard Display</b> - Real-time score display on player screens</li>
 *   <li><b>Team Management</b> - Team creation, player assignment, and team properties</li>
 *   <li><b>Score Tracking</b> - Multiple score types (area, beacons, links, triangles)</li>
 *   <li><b>Timer Management</b> - Countdown or open-ended game timers</li>
 *   <li><b>Game Goals</b> - Win condition tracking and game ending</li>
 *   <li><b>Team Spawns</b> - Team-specific spawn point management</li>
 *   <li><b>Persistence</b> - Loading and saving team membership</li>
 * </ul>
 *
 * <p><b>Architecture:</b>
 * Each Game instance has exactly one Scorecard that manages all aspects of that game's
 * teams, scores, and display. The Scorecard creates a Bukkit {@link Scoreboard} which
 * is shown to all players in the game.
 *
 * <p><b>Score Types:</b>
 * The Scorecard supports multiple scoring metrics:
 * <ul>
 *   <li>{@link GameScoreGoal#AREA} - Territory controlled in square blocks</li>
 *   <li>{@link GameScoreGoal#BEACONS} - Number of beacons owned</li>
 *   <li>{@link GameScoreGoal#LINKS} - Number of beacon links created</li>
 *   <li>{@link GameScoreGoal#TRIANGLES} - Number of triangular fields created</li>
 * </ul>
 *
 * <p><b>Timer Modes:</b>
 * <ul>
 *   <li><b>Countdown</b> - Game ends when timer reaches zero</li>
 *   <li><b>Open-ended</b> - Game continues until goal reached (timer counts up)</li>
 * </ul>
 *
 * <p><b>Game Flow:</b>
 * <ol>
 *   <li>Constructor creates Scorecard and calls {@link #initialize(Boolean)}</li>
 *   <li>Teams are added from config via {@link #addTeams()}</li>
 *   <li>Timer starts via {@link #runtimer()}, updating every 5 seconds</li>
 *   <li>Scores are updated via {@link #refreshScores()} when beacons change</li>
 *   <li>Game ends via {@link #endGame()} when goal reached or timer expires</li>
 * </ol>
 *
 * <p><b>Team Management:</b>
 * Teams are created from the config file and stored in the Bukkit Scoreboard.
 * Each team has:
 * <ul>
 *   <li>Name and display name with color</li>
 *   <li>Block material (for beacon ownership visualization)</li>
 *   <li>List of member UUIDs</li>
 *   <li>Spawn point location</li>
 *   <li>Scores for each score type</li>
 * </ul>
 *
 * <p><b>Display Constraints:</b>
 * The sidebar scoreboard has a maximum of 16 lines including the title.
 * The current implementation uses:
 * <ul>
 *   <li>Line 1: Title with game mode and timer</li>
 *   <li>Line 2: Game goal description</li>
 *   <li>Lines 3-15: Team scores (up to 4 score types per team)</li>
 * </ul>
 *
 * <p><b>Persistence:</b>
 * Team membership is saved to {@code teams.yml} to preserve assignments across:
 * <ul>
 *   <li>Server restarts</li>
 *   <li>Plugin reloads</li>
 *   <li>Game pauses</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * The Scorecard is NOT thread-safe. All methods should be called from the main
 * server thread. The timer runs on the Bukkit scheduler but updates are synchronized
 * to the main thread.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * // Create scorecard for a game
 * Scorecard scorecard = new Scorecard(plugin, game);
 *
 * // Add player to smallest team
 * scorecard.addTeamPlayer(player);
 *
 * // Update scores when beacon captured
 * Team team = ...;
 * scorecard.refreshScores(team);
 *
 * // Pause game
 * scorecard.pause();
 * </pre>
 *
 * @author tastybento
 * @see Game
 * @see Register
 * @see GameScoreGoal
 */
public class Scorecard extends BeaconzPluginDependent {
    /** Maximum length for score strings displayed on scoreboard (for formatting) */
    private static final Integer MAXSCORELENGTH = 10;

    /** Whether the game is currently running (false when paused) */
    private boolean gameON;

    /** The game instance this scorecard manages */
    private Game game;

    /** Plain text version of the game name (for file storage) */
    private String gameName;

    /** Bukkit's scoreboard manager for creating scoreboards */
    private ScoreboardManager manager;

    /** The actual scoreboard displayed to players */
    private Scoreboard scoreboard;

    /** Score entry used for updating scoreboard lines */
    private Score scoreentry;

    /** Score line used for displaying individual score entries */
    private Score scoreline;

    /** The scoreboard objective shown in the sidebar */
    private Objective scoreobjective;

    /** Countdown timer in seconds (decrements each interval) */
    private int countdownTimer;

    /** How often (in seconds) the timer updates */
    private int timerinterval;

    /** Whether to show the timer on scoreboard display */
    private Boolean showtimer;

    /** Type of timer: "countdown" or "openended" */
    private String timertype;

    /** Formatted time string for display (e.g., "00d 00:00:00") */
    private String displaytime;

    /** Current sidebar line number for adding scoreboard entries */
    private Integer sidebarline;

    /** Formatted goal description string shown on scoreboard */
    private String goalstr;

    /** Game start time in milliseconds (for elapsed time calculation) */
    private Long starttimemilis;

    /** Bukkit task ID for the running timer task (for cancellation) */
    private BukkitTask timertaskid;

    /** Maps teams to their spawn point locations */
    private HashMap<Team, Location> teamSpawnPoint = new HashMap<>();

    /**
     * Nested map storing scores for each team and score type.
     * Structure: Team → (ScoreType → Score Value)
     */
    private HashMap<Team, HashMap<GameScoreGoal,Integer>> score = new HashMap<>();

    /** Maps teams to lists of member UUIDs for persistence */
    private HashMap<Team, List<UUID>> teamMembers = new HashMap<>();

    /** Reverse lookup map: UUID → Team for fast player team lookup */
    private HashMap<UUID, Team> teamLookup = new HashMap<>();

    /** Maps teams to their block material type (for beacon visualization) */
    private HashMap<Team, Material> teamBlocks = new HashMap<>();

    /**
     * Constructs a new Scorecard for the specified game.
     *
     * <p>This constructor creates and initializes a complete game management system including:
     * <ul>
     *   <li>Scoreboard creation and setup</li>
     *   <li>Team loading from configuration</li>
     *   <li>Timer initialization and start</li>
     *   <li>Score tracking initialization for all score types</li>
     *   <li>Player team membership loading from persistence</li>
     * </ul>
     *
     * <p><b>Initialization Process:</b>
     * <ol>
     *   <li>Stores game reference and extracts game name</li>
     *   <li>Gets Bukkit scoreboard manager</li>
     *   <li>Calls {@link #initialize(Boolean)} with newGame=true</li>
     *   <li>Sets up teams from config via {@link #addTeams()}</li>
     *   <li>Loads team members from teams.yml</li>
     *   <li>Starts the game timer via {@link #runtimer()}</li>
     * </ol>
     *
     * <p><b>Important:</b> The constructor immediately starts the game. The game is set to
     * running state (gameON = true) and the timer begins counting.
     *
     * @param beaconzPlugin the main plugin instance for accessing server and config
     * @param game the game instance this scorecard will manage
     * @throws IllegalStateException if scoreboard manager is not available
     */
    public Scorecard(Beaconz beaconzPlugin, Game game) {
        super(beaconzPlugin);
        this.game = game;
        // For the scorecard, the game name is a String
        this.gameName =PlainTextComponentSerializer.plainText().serialize(game.getName());
        this.manager = beaconzPlugin.getServer().getScoreboardManager();
        initialize(true);
    }

    /**
     * Handles plugin reload by saving state and reinitializing.
     *
     * <p>This method preserves game state across plugin reloads by:
     * <ol>
     *   <li>Saving team members to teams.yml</li>
     *   <li>Reinitializing scoreboard and timer (without resetting)</li>
     *   <li>Refreshing scores from current game state</li>
     *   <li>Updating scoreboard display</li>
     * </ol>
     *
     * <p>Called by the plugin when /beaconz reload is executed.
     */
    public void reload() {
        saveTeamMembers();
        initialize(false);
        refreshScores();
        refreshSBdisplay();
    }

    /**
     * Initializes or reinitializes the scoreboard and game state.
     *
     * <p>This method sets up all components needed for game management:
     * <ul>
     *   <li><b>Timer</b> - Configures countdown or open-ended timer</li>
     *   <li><b>Scoreboard</b> - Creates new Bukkit scoreboard with objective</li>
     *   <li><b>Display</b> - Sets up sidebar with title and goal</li>
     *   <li><b>Teams</b> - Loads teams from config and previous members</li>
     *   <li><b>Scores</b> - Resets score tracking to zero</li>
     *   <li><b>Timer Task</b> - Starts the timer countdown/countup</li>
     * </ul>
     *
     * <p><b>Scoreboard Setup:</b>
     * <ul>
     *   <li>Line 15: Game goal (e.g., "Get 10 beacons!")</li>
     *   <li>Lines 1-14: Team scores (auto-allocated per team/score type)</li>
     *   <li>Title: "Beaconz [GameMode]! 00d 00:00:00" (includes timer)</li>
     * </ul>
     *
     * <p><b>Timer Types:</b>
     * <ul>
     *   <li><b>countdown</b> - countdownTimer > 0, decrements to zero</li>
     *   <li><b>openended</b> - countdownTimer == 0, counts up from start</li>
     * </ul>
     *
     * <p><b>newGame Parameter:</b>
     * When true, performs full initialization including team member loading.
     * When false, reinitializes without loading members (used for reload).
     *
     * @param newGame true for new game initialization, false for reload
     */
    public void initialize(Boolean newGame) {
        timerinterval = 5;
        showtimer = Settings.showTimer;
        starttimemilis = game.getStartTime();
        countdownTimer = game.getCountdownTimer();
        timertype = countdownTimer == 0 ? "openended" : "countdown";
        // Define the scoreboard
        try {
            scoreboard.clearSlot(DisplaySlot.SIDEBAR);
        } catch (Exception e){ }
        try {
            scoreobjective.unregister();
        } catch (Exception e){ }

        scoreboard = manager.getNewScoreboard();
        //scoreobjective = scoreboard.registerNewObjective("score", "beaconz");
        scoreobjective = scoreboard.registerNewObjective("score", Criteria.DUMMY, Lang.titleBeaconz);
        scoreobjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebarline = 15;

        // Set up the scoreboard with the goal
        scoreobjective.displayName(Lang.titleBeaconz.append(Component.text(" " + game.getGamemode().getName() + "! 00d 00:00:00").color(NamedTextColor.GREEN)));

        goalstr = "";
        if (game.getGamegoalvalue() == 0) {
            goalstr = PlainTextComponentSerializer.plainText().serialize(Lang.scoreGetTheMostGoal.color(NamedTextColor.GREEN)).replace("[goal]", game.getGamegoal().getName());
        } else {
            String value = String.format(Locale.US, "%,d", game.getGamegoalvalue());
            goalstr = PlainTextComponentSerializer.plainText().serialize(Lang.scoreGetValueGoal.color(NamedTextColor.GREEN)).replace("[value]", value).replace("[goal]", game.getGamegoal().getName());
        }
        scoreline = scoreobjective.getScore(goalstr);
        scoreline.setScore(sidebarline);

        // Start the game
        gameON = true;

        // Reset the score
        score.clear();

        // Create the teams and enable scoreboards
        teamBlocks = new HashMap<>();
        addTeams();
        loadTeamMembers();

        // Start the timer
        runtimer();
    }

    /**
     * Pauses the game by stopping score updates.
     *
     * <p>When paused:
     * <ul>
     *   <li>Score refreshes are disabled (scores frozen)</li>
     *   <li>Timer continues running (for countdown mode)</li>
     *   <li>Win condition checking is disabled</li>
     *   <li>Players remain in their teams</li>
     * </ul>
     *
     * <p>The game can be resumed with {@link #resume()}.
     */
    public void pause() {
        gameON = false;
    }

    /**
     * Resumes a paused game.
     *
     * <p>When resumed:
     * <ul>
     *   <li>Score updates are re-enabled</li>
     *   <li>Win condition checking resumes</li>
     *   <li>Scores are refreshed from current state</li>
     * </ul>
     *
     * <p>Note: The timer never actually stops, only score processing pauses.
     */
    public void resume() {
        gameON = true;
    }

    /**
     * Adds teams to the game from the plugin configuration file.
     *
     * <p>Teams are loaded from {@code config.yml} under the path:
     * <pre>
     * teams:
     *   names:
     *     red:
     *       glasscolor: RED_STAINED_GLASS
     *       displayname: "&cRed Team"
     *     blue:
     *       glasscolor: BLUE_STAINED_GLASS
     *       displayname: "&9Blue Team"
     * </pre>
     *
     * <p><b>Team Creation Process:</b>
     * <ol>
     *   <li>Reads team configuration section</li>
     *   <li>For each team in config:
     *     <ul>
     *       <li>Gets block material (glasscolor)</li>
     *       <li>Translates display name color codes (&amp; to §)</li>
     *       <li>Truncates names to 16 characters (Minecraft limit)</li>
     *       <li>Calls {@link #addTeam(String, String, Material, boolean)}</li>
     *     </ul>
     *   </li>
     *   <li>Stops after adding game.getNbrTeams() teams</li>
     * </ol>
     *
     * <p><b>Important:</b> Team display names MUST include the color code prefix
     * for proper scoreboard rendering and team identification.
     *
     * <p><b>Error Handling:</b>
     * If no teams are added (config missing or empty), logs a severe error
     * with the game name for debugging.
     */
    protected void addTeams() {
        ConfigurationSection csect = getBeaconzPlugin().getConfig().getConfigurationSection("teams.names");
        int teamcnt = 0;
        if (csect != null) {
            for (String teamName: csect.getValues(false).keySet()) {
                Material teamBlock = Material.getMaterial(csect.getString(teamName + ".glasscolor"));
                //IMPORTANT: The a team's display name must ALWAYS be the team's name, PRECEEDED BY the ChatColor
                String teamDisplayName = ChatColor.translateAlternateColorCodes('&', csect.getString(teamName + ".displayname", teamName));
                if (teamName.length() > 16) {
                    teamName = teamName.substring(0, 16);
                }
                if (teamDisplayName.length() > 16) {
                    teamDisplayName = teamDisplayName.substring(0, 16);
                }
                addTeam(teamName, teamDisplayName, teamBlock, false);
                teamcnt ++;
                if (teamcnt == game.getNbrTeams()) {
                    break;
                }
            }
        }
        if (teamcnt == 0) getLogger().severe("Scorecard.addTeams did not add any teams. Game: " + PlainTextComponentSerializer.plainText().serialize(game.getName()));
    }

    /**
     * Updates scoreboard values for all teams and all score types.
     *
     * <p>This method iterates through all teams and refreshes their scores by
     * querying the {@link Register} for current game state (beacons, links, area, etc.).
     *
     * <p>Called when:
     * <ul>
     *   <li>Beacons are captured or lost</li>
     *   <li>Links are created or destroyed</li>
     *   <li>Territory changes (triangles)</li>
     *   <li>Plugin reload</li>
     * </ul>
     *
     * <p>For each team, calls {@link #refreshScores(Team)}.
     */
    public void refreshScores() {
        for (Team team: scoreboard.getTeams()) {
            refreshScores(team);
        }
    }

    /**
     * Updates scoreboard values for a specific team across all score types.
     *
     * <p>Refreshes all score types configured for the game (area, beacons, links, triangles).
     * If the team has no existing scores, initializes them to zero.
     *
     * @param team the team to update scores for
     */
    public void refreshScores(Team team) {
        if (team != null) {
            int defaultvalue = 0;
            if (score.get(team) == null) defaultvalue = 0;   // if team doesn't have score, create it and set to 0
            for (GameScoreGoal st :  game.getScoretypes()) {
                refreshScores(team, st, defaultvalue);
            }
        }
    }

    /**
     * Refreshes a specific score type for a team and checks for win condition.
     *
     * <p><b>Score Calculation by Type:</b>
     * <ul>
     *   <li><b>AREA</b> - Total square blocks of territory controlled</li>
     *   <li><b>BEACONS</b> - Number of beacons owned by team</li>
     *   <li><b>LINKS</b> - Number of beacon-to-beacon links</li>
     *   <li><b>TRIANGLES</b> - Number of triangular fields created</li>
     * </ul>
     *
     * <p><b>Win Condition:</b>
     * If the game has a goal value (not zero) and this score type matches the game goal,
     * checks if the team has reached the goal value. If so, ends the game immediately
     * via {@link #endGame()}.
     *
     * <p><b>Game State:</b>
     * Only updates scores when {@code gameON == true}. When paused, scores are not
     * recalculated from the Register.
     *
     * <p><b>Side Effects:</b>
     * Calls {@link #putScore(Team, GameScoreGoal, int)} which updates the score
     * and refreshes the scoreboard display.
     *
     * @param team the team to update
     * @param scoretype the specific score type to update (AREA, BEACONS, LINKS, TRIANGLES)
     * @param value default value if score doesn't exist (typically 0)
     */
    public void refreshScores(Team team, GameScoreGoal scoretype, int value) {
        if (gameON) {
            switch (scoretype) {
            case GameScoreGoal.AREA: {
                value = getRegister().getTeamArea(team);
                break;
            }
            case GameScoreGoal.BEACONS: {
                value = getRegister().getTeamBeacons(team).size();
                break;
            }
            case GameScoreGoal.LINKS: {
                value = getRegister().getTeamLinks(team);
                break;
            }
            case GameScoreGoal.TRIANGLES: {
                value = getRegister().getTeamTriangles(team);
                break;
            }
            default:
                break;
            }
            //Update the score - putscore will call refreshSBDisplay()
            putScore(team, scoretype, value);

            // See if we have a winner
            // If the gamegoal value is zero, then the game is never ending
            if (game.getGamegoalvalue() > 0 && scoretype.equals(game.getGamegoal()) && value >= game.getGamegoalvalue()) {
                // ending game
                endGame();
            }
        }
    }

    /**
     * Updates the Scoreboard display
     *
     */
    public void refreshSBdisplay() {
        for (Team team: scoreboard.getTeams()) {
            refreshSBdisplay(team);
        }
    }
    public void refreshSBdisplay(Team team) {
        if (score.get(team) != null) {
            for (GameScoreGoal st : game.getScoretypes()) {
                refreshSBdisplay(team, st);
            }
        }
    }
    public void refreshSBdisplay(Team team, GameScoreGoal scoretype) {
        // The setScore values are actually line numbers on the scoreboard
        // the actual scores go in the score description

        // Refresh the team scores for the given score type, if it can be shown
        if (gameON && game.getScoretypes().contains(scoretype)) {
            HashMap<GameScoreGoal, Integer> stypes = score.get(team);
            int sv = 0;
            if (stypes != null && stypes.get(scoretype) != null) sv = stypes.get(scoretype);
            String scorestring = fixScoreString(team, scoretype, sv, MAXSCORELENGTH);

            String oldentry = sbEntry(team, scoretype);
            int line = scoreobjective.getScore(oldentry).getScore();
            scoreboard.resetScores(oldentry);
            scoreentry = scoreobjective.getScore(scorestring);
            scoreentry.setScore(line);
        }
    }

    /**
     * Adds a team to the scoreboard and returns the team that was made
     *
     * @param teamName
     * @param teamBlock
     * @param save      - if true, saves game to file after adding team
     */
    public void addTeam(String teamName, String teamDisplayName, Material teamBlock, Boolean save) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            // Create the team
            team = scoreboard.registerNewTeam(teamName);
            team.setAllowFriendlyFire(false);
            team.prefix(Component.text("[" + teamDisplayName +"] ").color(teamChatColor(team)));
            team.displayName(Component.text(teamDisplayName));
            // Store the block for the team
            teamBlocks.put(team, teamBlock);
            // Get a new spawnpoint for the new team
            Location loc = makeTeamSpawnPoint(team);
            teamSpawnPoint.put(team, loc);
            // Now it gets tricky... the setScore values are actually line numbers on the scoreboard
            // the actual scores go in the score description
            for (GameScoreGoal st : game.getScoretypes()) {
                sidebarline -= 1;
                if (sidebarline > 0 ) {
                    String scorestring = fixScoreString(team, st, 0, 8);
                    scoreentry = scoreobjective.getScore(scorestring);
                    scoreentry.setScore(sidebarline);
                } else {
                    getLogger().warning("Could not show new team scores on the sidebar, ran out of lines. Team = " + teamName);
                }
            }
        }
        //Refresh the scores, save the game and return
        //refreshScores(team);
        if (save) game.save();

    }

    /**
     * Fixes the string to show on the sidebar
     * Since the sidebar only shows scores in decreasing order, the only way to sort them
     * the way we want is to use the scores for line numbers and keep our own
     * scores in the score description.
     * This method takes a team, a score's name, a score value and a max length
     * and returns a string to be displayed in the sidebar.
     * For instance, fixScoreString (redteam, "beacons", 10, 8) will return "______10 RED beacons"
     */
    public String fixScoreString (Team team, GameScoreGoal scoretype, Integer score, Integer maxlen) {
        TextColor teamcolor = teamChatColor(team);
        String formattedScore = String.format(Locale.US, "%,d", score);
        String padstring = "____________________".substring(0, Math.max(0, maxlen - 1 - formattedScore.length()));
        Component fixed = Component.text(padstring).color(NamedTextColor.GRAY).append(Component.text(formattedScore + " ").color(teamcolor))
                .append(team.displayName()).append(Component.text(" " + scoretype.getName()));
        return PlainTextComponentSerializer.plainText().serialize(fixed);
    }

    /**
     * Returns the first scoreboard Entry for a given team + score type - and *** there can be only ONE ***
     *
     */
    public String sbEntry (Team team, GameScoreGoal scoretype) {
        String scoreboardentry = "";
        String teamName = PlainTextComponentSerializer.plainText().serialize(team.displayName());
        for (String entry : scoreboard.getEntries()) {
            if (entry.contains(teamName + " " + scoretype)) {
                scoreboardentry = entry;
                break;
            }
        }
        return scoreboardentry;
    }

    /**
     * Adds a player to a team. If the player was on another team in this game, they will be removed from the other team
     */
    public void addTeamPlayer(Team team, Player player) {
        // first add to the actual team
        // This automatically removes the player from any other Teams
        team.addEntry(player.getName());
        // then update the team lists
        removeTeamPlayer(player);
        teamLookup.put(player.getUniqueId(), team);
        List<UUID> members = teamMembers.get(team);
        if (members == null) members = new ArrayList<>();
        members.add(player.getUniqueId());
        teamMembers.put(team, members);
        game.save();
    }

    /**
     * Removes a player from all teams (there should be only one)
     */
    public void removeTeamPlayer(Player player) {
        // Remove player from the teamLookup index
        teamLookup.remove(player.getUniqueId());
        // Go through all the team and remove the player if he exists
        for (Entry<Team, List<UUID>> team : teamMembers.entrySet()) {
            if (team.getValue() != null) {
                team.getValue().remove(player.getUniqueId());
                teamMembers.put(team.getKey(), team.getValue());
            }
        }
        game.save();
    }

    /**
     * Send players to their team spawn location
     */
    public void sendPlayersHome(Boolean ingameOnly) {
        if (teamLookup != null) {
            for (UUID uuid : teamLookup.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    sendPlayersHome(player, ingameOnly);
                }
            }
        }
    }

    /**
     * Sends a player to their team spawn point
     * @param player
     * @param ingameOnly
     */
    public void sendPlayersHome(Player player, boolean ingameOnly) {
        Team team = getTeam(player);
        if (!ingameOnly || game.getRegion().isPlayerInRegion(player)) {
            Location loc = teamSpawnPoint.get(team);
            // Teleport player to a captured beacon
            List<BeaconObj> beaconz = getRegister().getTeamBeacons(team);
            if (beaconz.size() > 0) {
                Random rand = new Random();
                loc = beaconz.get(rand.nextInt(beaconz.size())).getLocation().add(new Vector(0,1,0));
            }
            loc = game.getRegion().findSafeSpot(loc, 20);  // in case other players have boobytrapped the spawnpoint
            final int RADIUS = 2;
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    Chunk chunk = loc.getWorld().getChunkAt(loc.getChunk().getX() + x, loc.getChunk().getZ() + z);
                    chunk.load();
                    // Remove any hostile mobs
                    for (Entity entity: chunk.getEntities()) {
                        if (entity instanceof Monster) {
                            entity.remove();
                        }
                    }
                }
            }
            player.teleport(loc);
            player.setScoreboard(scoreboard);
        }
    }


    /**
     * Returns a player's team (even if the player is offline)
     * If the player does not have a team, he is NO LONGER put in one - use assignTeam(player) for that
     * @param player
     * @return Team or null if none
     */
    public Team getTeam(Player player) {
        return teamLookup.get(player.getUniqueId());
    }

    /**
     * Puts a player in a team if he isn't already in one
     * Returns the player's team
     *
     * @param player
     */
    public void assignTeam(Player player) {
        Team team = teamLookup.get(player.getUniqueId());
        if (team == null) {
            // New player!
            int minSize=Integer.MAX_VALUE;
            for (Team t: scoreboard.getTeams()) {
                if(t.getSize() < minSize) {
                    minSize=t.getSize();
                    team=t;
                }
            }
            addTeamPlayer(team, player);
        }
    }


    /**
     * Gets a team from a team name, works even if the case is wrong too or if it is partial
     * @param teamName
     * @return team, or null if not found
     */
    /**
     * Gets a team by name with support for partial name matching.
     *
     * <p><b>Matching Algorithm:</b>
     * <ol>
     *   <li>First tries exact match (case-sensitive)</li>
     *   <li>If no exact match, tries prefix matching (case-insensitive)</li>
     *   <li>Returns first team whose name starts with the search string</li>
     * </ol>
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>"red" → matches "red" team</li>
     *   <li>"Red" → matches "red" team (case-insensitive prefix)</li>
     *   <li>"re" → matches "red" team (partial match)</li>
     *   <li>"blu" → matches "blue" team</li>
     * </ul>
     *
     * <p>This partial matching makes commands more user-friendly by allowing
     * abbreviated team names.
     *
     * @param teamName the team name or partial name to search for
     * @return the matching Team, or null if no match found
     */
    public Team getTeam(String teamName) {
        if (scoreboard.getTeam(teamName) != null) {
            return scoreboard.getTeam(teamName);
        } else {
            for (Team team : scoreboard.getTeams()) {
                if (team.getName().toLowerCase().startsWith(teamName.toLowerCase())) {
                    return team;
                }
            }
        }
        return null;
    }

    /**
     * Returns the type of block for this team, e.g., blue glass
     * @param team
     * @return block type or null if it does not exist
     */
    public Material getBlockID(Team team) {
        return teamBlocks.get(team);
    }

    /**
     * Get the team defined by this block
     * @param b - block
     * @return Team, or null if it doesn't exist
     */
    public Team getTeamFromBlock(Block b) {
        for (Entry<Team, Material> md: teamBlocks.entrySet()) {
            if (md.getValue() == b.getType()) {
                return md.getKey();
            }
        }
        return null;
    }

    /**
     * Provide a user readable comma delimited list of the team names for use in commands
     * @return team list
     */
    public String getTeamListString() {
        StringBuilder result = new StringBuilder();
        for (Team team : scoreboard.getTeams()) {
            if (result.length() == 0) {
                result = new StringBuilder(team.getName());
            } else {
                result.append(", ").append(team.getName());
            }
        }
        return result.toString();
    }

    /**
     * Return the scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * Return the teamMembers hashmap
     */
    public HashMap<Team, List<UUID>> getTeamMembers() {
        return teamMembers;
    }
    public HashMap<UUID, Team> getTeamLookup() {
        return teamLookup;
    }

    /**
     * Loads all the team members in UUID format
     * The teams were added to the scoreboard by addTeamsFromFile()
     */
    public void loadTeamMembers() {
        File teamFile = new File(getBeaconzPlugin().getDataFolder(),"teams.yml");
        if (!teamFile.exists()) {
            saveTeamMembers();
        }
        YamlConfiguration teamsYml = new YamlConfiguration();
        try {
            teamsYml.load(teamFile);
        } catch (IOException e) {
            // Catch block
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Problem with teams.yml formatting");
            e.printStackTrace();
        }
        for (Team team: scoreboard.getTeams()) {
            List<String> members = teamsYml.getStringList(gameName + "." + team.getName() + ".members");
            List<UUID> memberList = new ArrayList<>();
            for (String uuidText : members) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    memberList.add(uuid);
                    OfflinePlayer player = getBeaconzPlugin().getServer().getOfflinePlayer(uuid);
                    if (player != null) {
                        team.addEntry(player.getName());
                        teamLookup.put(uuid, team);
                    } else {
                        getLogger().severe("Error loading team member " + team.getName() + " " + uuid + " - skipping");
                    }
                } catch (Exception e) {
                    getLogger().severe("Error loading team member " + team.getName() + " " + uuidText + " - skipping");
                }
            }
            teamMembers.put(team, memberList);
        }
    }

    /**
     * Saves the teams to the config file
     */
    public void saveTeamMembers() {
        File teamsFile = new File(getBeaconzPlugin().getDataFolder(),"teams.yml");
        YamlConfiguration teamsYml = YamlConfiguration.loadConfiguration(teamsFile);
        // Backup the teams file just in case
        if (teamsFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"teams.old");
            teamsFile.renameTo(backup);
        }
        for (Team team: scoreboard.getTeams()) {
            // Save the team members
            if (teamMembers.containsKey(team)) {
                List<String> members = teamMembers.get(team).stream().map(UUID::toString).toList();
                teamsYml.set(gameName + "." + team.getName() + ".members", members);
            }
            // Save the team spawn location
            teamsYml.set(gameName + "." + team.getName() + "." + "spawnpoint", getStringLocation(teamSpawnPoint.get(team)));
        }
        try {
            teamsYml.save(teamsFile);
        } catch (IOException e) {
            // Catch block
            e.printStackTrace();
        }
    }

    /**
     * Return the scoreboard manager
     */
    public ScoreboardManager getManager() {
        return manager;
    }

    /**
     * Return the scores
     */
    public HashMap<Team, HashMap<GameScoreGoal,Integer>> getScore() {
        return score;
    }

    /**
     * Toggle showtimer
     */
    public void toggleTimer() {
        showtimer = ! showtimer;
    }

    /**
     * Return the game's start time
     */
    public Long getStartTime() {
        return starttimemilis;
    }

    /**
     * Return the current countdown timer
     */
    public int getCountdownTimer() {
        return countdownTimer;
    }

    /**
     * Return the timer for display
     */
    public String getDisplayTime(String type) {
        // type is either "short" or "long"
        if (type == "short") {
            return displaytime;
        } else {
            return displaytime + " (" + timertype + ")";
        }
    }

    /**
     * @param team
     * @return
     * @return score for team
     * TODO: simplify this
     */
    public Integer getScore(Team team, GameScoreGoal scoretype) {
        if (score.containsKey(team)) {
            HashMap<GameScoreGoal,Integer> scores = score.get(team);
            if (scores.containsKey(scoretype)) return scores.get(scoretype);
        }
        return 0;
    }

    /**
     * Set the score for a team
     * @param team
     * @param scoretype
     * @param value
     */
    public void putScore(Team team, GameScoreGoal scoretype, int value) {
        if (gameON && team != null && scoretype != null) {
            HashMap<GameScoreGoal,Integer> stypes = score.get(team);
            if (stypes == null) stypes = new HashMap<>();
            stypes.put(scoretype, value);
            score.put(team, stypes);
            refreshSBdisplay(team, scoretype);
        }
    }

    /**
     * Adds score to team
     * @param team
     * @param scoretype
     * @param value
     */
    public void addScore(Team team, GameScoreGoal scoretype, int value) {
        if (score.containsKey(team)) {
            HashMap<GameScoreGoal,Integer> stypes = score.get(team);
            if (stypes != null && stypes.containsKey(scoretype)) {
                value  += stypes.get(scoretype);
                if (value < 0 ) value = 0;
            }
        }
        putScore(team, scoretype, value);
    }

    /**
     * Subtracts score from team
     * @param team
     * @param scoretype
     * @param value
     */
    public void subtractScore(Team team, GameScoreGoal scoretype, int value) {
        addScore(team, scoretype, -value);
    }


    /**
     * Returns the location where a team should spawn, based on the region's spawn point
     * @param playerTeam
     * @return Location
     */
    public Location makeTeamSpawnPoint(Team team) {
        Region region = game.getRegion();
        Location teamSP = null;

        // First try to get the team's spawn point from teams.yml
        File teamFile = new File(getBeaconzPlugin().getDataFolder(),"teams.yml");
        if (!teamFile.exists()) {
            saveTeamMembers();
        }
        YamlConfiguration teamsYml = new YamlConfiguration();
        try {
            teamsYml.load(teamFile);
        } catch (IOException e) {
            // Catch block
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Problem with teams.yml formatting");
            e.printStackTrace();
        }
        String location = teamsYml.getString(gameName + "." + team.getName() + ".spawnpoint");
        if (location != null) {
            teamSP = getLocationString(location);
        }

        // Failing that, create a default spawn point
        if (teamSP == null) {
            teamSP = region.getSpawnPoint();
            BlockFace blockFace = BlockFace.NORTH;
            // We allow up to 8 teams
            int direction = 0;
            for (Team t : scoreboard.getTeams()) {
                if (t.equals(team)) {
                    blockFace = switch (direction) {
                    case 0 -> BlockFace.NORTH;
                    case 1 -> BlockFace.SOUTH;
                    case 2 -> BlockFace.EAST;
                    case 3 -> BlockFace.WEST;
                    case 4 -> BlockFace.NORTH_EAST;
                    case 5 -> BlockFace.NORTH_WEST;
                    case 6 -> BlockFace.SOUTH_EAST;
                    case 7 -> BlockFace.SOUTH_WEST;
                    default -> blockFace;
                    };
                }
                direction++;
            }
            teamSP = teamSP.getBlock().getRelative(blockFace, game.getRegion().getRadius() / 4).getLocation();
            teamSP = region.findSafeSpot(teamSP, 20);
        }

        // This will result in bedrock blocks being created up and up if the bedrock is covered...
        // TODO these spawn points need special protection, or something. An enemy team could place a lot of blocks
        //teamSP.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        return teamSP;
    }

    /**
     * Player in team or not
     * @param player
     * @return true if in team, false if not
     */
    public boolean inTeam(Player player) {
        return scoreboard.getEntryTeam(player.getName()) != null;
    }

    /**
     * Sets the team's spawn location
     */
    public void setTeamSpawnPoint(Team team, Location location) {
        teamSpawnPoint.put(team, location);
        game.save();
    }

    /**
     * @param Team
     * @return Location of the team's spawn point or null if team is unknown or there is no spawn point
     */
    public Location getTeamSpawnPoint(Team Team) {
        return teamSpawnPoint.get(Team);
    }

    /**
     * Deserializes a location string back to a Location object.
     *
     * <p><b>Expected Format:</b>
     * <pre>
     * "world:x:y:z:yaw:pitch"
     * </pre>
     *
     * <p><b>Parsing Process:</b>
     * <ol>
     *   <li>Splits string on colon delimiter</li>
     *   <li>Validates exactly 6 parts exist</li>
     *   <li>Looks up world by name (must exist on server)</li>
     *   <li>Parses integer coordinates (x, y, z)</li>
     *   <li>Converts integer bits back to float for yaw/pitch</li>
     *   <li>Constructs and returns Location</li>
     * </ol>
     *
     * <p><b>Error Handling:</b>
     * Returns null if:
     * <ul>
     *   <li>String is null or empty</li>
     *   <li>Format doesn't have exactly 6 parts</li>
     *   <li>World doesn't exist on server</li>
     *   <li>Number parsing fails</li>
     * </ul>
     *
     * <p><b>Note:</b> The yaw and pitch use Float.intBitsToFloat to restore
     * the exact floating-point values that were saved via Float.floatToIntBits
     * in {@link #getStringLocation(Location)}.
     *
     * @param s the serialized location string
     * @return deserialized Location, or null if invalid/unparseable
     * @see #getStringLocation(Location)
     */
    static public Location getLocationString(final String s) {
        if (s == null || s.trim() == "") {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 6) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            if (w == null) {
                return null;
            }
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
            final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
            return new Location(w, x, y, z, yaw, pitch);
        }
        return null;
    }

    /**
     * Converts a location to a simple string representation for persistence.
     *
     * <p><b>Format:</b>
     * <pre>
     * "world:x:y:z:yaw:pitch"
     * </pre>
     *
     * <p><b>Example:</b>
     * <pre>
     * "world:100:64:200:1119092224:0"
     * </pre>
     *
     * <p><b>Field Details:</b>
     * <ul>
     *   <li><b>world</b> - World name as string</li>
     *   <li><b>x, y, z</b> - Block coordinates (integers)</li>
     *   <li><b>yaw, pitch</b> - Rotation as integer bits (via Float.floatToIntBits)</li>
     * </ul>
     *
     * <p>The yaw and pitch are stored as integer representations of their float
     * bit patterns to preserve exact floating-point values across serialization.
     *
     * <p><b>Null Handling:</b>
     * Returns empty string if location or world is null.
     *
     * @param l the location to serialize
     * @return string representation of location, or empty string if null
     * @see #getLocationString(String)
     */
    static public String getStringLocation(final Location l) {
        if (l == null || l.getWorld() == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" + Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
    }


    /**
     * Determines which team has the highest score for a given score type.
     *
     * <p>Iterates through all teams and finds the one with the maximum score
     * for the specified type. Useful for:
     * <ul>
     *   <li>Determining game leader</li>
     *   <li>Checking win conditions</li>
     *   <li>Displaying rankings</li>
     *   <li>Tie-breaking logic</li>
     * </ul>
     *
     * <p><b>Tie Handling:</b>
     * If multiple teams have the same highest score, returns the first one
     * found (iteration order is undefined).
     *
     * <p><b>Edge Cases:</b>
     * <ul>
     *   <li>Returns null if no teams have scores for the type</li>
     *   <li>Returns null if all teams have score of 0</li>
     *   <li>Returns null if no teams exist</li>
     * </ul>
     *
     * @param scoretype the score type to check (AREA, BEACONS, LINKS, or TRIANGLES)
     * @return the team with the highest score, or null if no valid leader
     */
    public Team frontRunner (GameScoreGoal scoretype) {
        Integer maxscore = 0;
        Team topteam = null;
        for (Team team: scoreboard.getTeams()) {
            HashMap<GameScoreGoal,Integer> stypes = score.get(team);
            if (stypes != null && stypes.containsKey(scoretype)) {
                if (stypes.get(scoretype) > maxscore) {
                    maxscore = stypes.get(scoretype);
                    topteam = team;
                }
            }
        }
        return topteam;
    }

    /**
     * Gets the Adventure API text color for a team based on its name.
     *
     * <p><b>Color Mapping:</b>
     * Maps team names (case-insensitive) to {@link TextColor} for chat messages
     * and scoreboard display. Supports all 16 Minecraft dye colors plus custom RGB.
     *
     * <p><b>Supported Colors:</b>
     * <ul>
     *   <li>RED, BLUE, WHITE, BLACK, GRAY, LIGHTGRAY - Named colors</li>
     *   <li>YELLOW, LIME, GREEN - Greens and yellows</li>
     *   <li>ORANGE, BROWN - Warm colors</li>
     *   <li>PINK, MAGENTA, PURPLE - Purples and pinks</li>
     *   <li>CYAN, LIGHTBLUE - Blues and cyans</li>
     * </ul>
     *
     * <p><b>Custom RGB Colors:</b>
     * Some colors use TextColor.color(r, g, b) for precise matching:
     * <ul>
     *   <li>ORANGE: rgb(255, 165, 0)</li>
     *   <li>MAGENTA: rgb(255, 0, 255)</li>
     *   <li>PINK: rgb(255, 105, 180)</li>
     *   <li>CYAN: rgb(0, 255, 255)</li>
     *   <li>BROWN: rgb(150, 75, 0)</li>
     * </ul>
     *
     * <p><b>Important:</b> Team names are converted to uppercase before matching.
     * The team's display name should include the color code prefix for consistency.
     *
     * @param team the team to get color for
     * @return the TextColor for this team, or WHITE as default
     */
    public TextColor teamChatColor(Team team) {
        // IMPORTANT: The a team's display name is ALWAYS the team's name in uppercase, PRECEEDED BY the ChatColor
        String tn = team.getName().toUpperCase();
        return switch (tn) {
        case "RED" -> NamedTextColor.RED;
        case "BLUE" -> NamedTextColor.BLUE;
        case "WHITE" -> NamedTextColor.WHITE; 
        case "ORANGE" -> TextColor.color(255, 165, 0); 
        case "MAGENTA" -> TextColor.color(255, 0, 255); 
        case "LIGHTBLUE" -> NamedTextColor.AQUA; 
        case "YELLOW" -> NamedTextColor.YELLOW; 
        case "LIME" -> NamedTextColor.GREEN; 
        case "PINK" -> TextColor.color(255, 105, 180); 
        case "GRAY" -> NamedTextColor.DARK_GRAY; 
        case "LIGHTGRAY" -> NamedTextColor.GRAY; 
        case "CYAN" -> TextColor.color(0, 255, 255); 
        case "PURPLE" -> NamedTextColor.DARK_PURPLE; 
        case "BROWN" -> TextColor.color(150, 75, 0);
        case "GREEN" -> NamedTextColor.DARK_GREEN; 
        case "BLACK" -> NamedTextColor.BLACK; 
        default -> NamedTextColor.WHITE;
        };
    }

    /**
     * Ends the game
     */
    public void endGame() {
        // Stop timer
        if (timertaskid != null) timertaskid.cancel();
        // Stop keeping score
        gameON = false;
        // Set game over to true
        game.setOver(true);
        // Change the objective line in the scoreboard
        scoreboard.resetScores(goalstr);
        scoreline = scoreobjective.getScore(Lang.scoreGameOver);
        scoreline.setScore(15);
        // Wait a second to let all other messages display first
        getBeaconzPlugin().getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
            // Announce winner to all players
            Team winner = frontRunner(game.getGamegoal());
            Component titleline = Component.text(Lang.scoreGameOver);
            Component subtitleline = Lang.scoreNoWinners;
            if (winner != null) {
                titleline = Lang.scoreTeamWins.replaceText("[team]", winner.displayName());
                subtitleline = Lang.scoreCongratulations;
            }
            for (Team team : scoreboard.getTeams()) {
                for (String entry : team.getEntries()) {
                    UUID uuid = getBeaconzPlugin().getNameStore().getPlayerUUID(entry);
                    if (uuid != null) {
                        Player player = Bukkit.getServer().getPlayer(uuid);
                        if (player != null) {
                            // Tell players in the game
                            if (game.getRegion().isPlayerInRegion(player)) {
                                // TODO - update this!
                                getServer().dispatchCommand(getServer().getConsoleSender(),
                                        "title " + player.getName() + " title {\"text\":\"" + titleline + "\", \"color\":\"" + "gold" + "\"}");
                                getServer().dispatchCommand(getServer().getConsoleSender(),
                                        "title " + player.getName() + " subtitle {\"text\":\"" + subtitleline + "\", \"color\":\"" + "gold" + "\"}");
                                player.sendMessage(Lang.helpLine);
                                player.sendMessage(titleline);
                                player.sendMessage(subtitleline);
                                player.sendMessage(Lang.helpLine);                                
                            } else {
                                player.sendMessage(Component.text("[").append(game.getName()).append(Component.text("] ").append(titleline.color(NamedTextColor.YELLOW))));                        
                            }
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1F, 1F);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1F, 1F);
                        } else {
                            // Offline player
                            getMessages().setMessage(uuid, "[" + game.getName() + "] " + titleline);
                        }
                    }
                }
            }  
            // If working with QueueMgr, tell it the game ended:
            /*
            if (getServer().getPluginManager().getPlugin("QueueMgr") != null) {
                QueueMgrInterface qMgr = getServer().getServicesManager().load(QueueMgrInterface.class);
                if (qMgr != null && winner != null) {
                    qMgr.endGame(game.getName());
                    qMgr.setWinnerVar(game.getName(), winner.getDisplayName().toUpperCase(), null);
                    qMgr.setGameOverVar1(game.getName(), "THAT WAS A GREAT GAME!", null);
                    qMgr.setGameOverVar2(game.getName(), "THANK YOU FOR PLAYING!", null);
                }
            }
             */
        }, 30);        
    }

    /**
     * Timer
     *
     * This runs a countdown if Settings.minigameTimer or Settings.strategyTimer > 0, open-ended clock otherwise
     *
     */
    public void runtimer () {
        if (timertaskid != null) timertaskid.cancel();
        timertaskid = getBeaconzPlugin().getServer().getScheduler().runTaskTimerAsynchronously(getBeaconzPlugin(), () -> {

            if (gameON) {
                long seconds = 0L;
                int t = timerinterval;

                if (timertype.equals("openended")) {
                    seconds = (System.currentTimeMillis() - starttimemilis) / 1000;
                    seconds = ((seconds+t-1)/t)*t;
                } else {
                    countdownTimer = countdownTimer - t;
                    if (countdownTimer < 1) {
                        // Beacon timer ran out
                        countdownTimer = 0;
                        timertaskid.cancel();
                        endGame();
                    }
                    seconds = (long) countdownTimer;
                }

                // display the timer
                long s = seconds % 60;
                long m = (seconds / 60) % 60;
                long h = (seconds / (60 * 60)) % 24;
                long d = (seconds / (60 * 60 * 24)) %100;
                displaytime = String.format("%02dd %02d:%02d:%02d", d,h,m,s);

                if (showtimer) {
                    String objName = scoreobjective.getDisplayName();
                    if (!objName.contains(":")) objName = objName + "! 00d 00:00:00";
                    objName = objName.substring(0, objName.length() - displaytime.length()) + displaytime;
                    scoreobjective.setDisplayName(objName);
                } else {
                    scoreobjective.setDisplayName(ChatColor.GREEN + "Beaconz " + game.getGamemode());
                }
            }
        }, 20, timerinterval* 20L);
    }

    /**
     * Deletes the team members
     */
    public void deleteTeamMembers() {
        File teamsFile = new File(getBeaconzPlugin().getDataFolder(),"teams.yml");
        YamlConfiguration teamsYml = YamlConfiguration.loadConfiguration(teamsFile);
        // Backup the teams file just in case
        if (teamsFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"teams.old");
            teamsFile.renameTo(backup);
        }
        teamsYml.set(gameName, null);
        // Remove from hashmap
        /*
        for (Team team : teamMembers.keySet()) {
            for (UUID name : teamMembers.get(team)) {
                team.removeEntry(name);
            }
            teamMembers.put(team, new ArrayList<>());
        }*/
        // Clear all the players from the teamLookup.
        teamLookup.clear();

        try {
            teamsYml.save(teamsFile);
        } catch (IOException e) {
            // Catch block
            e.printStackTrace();
        }

    }

    /**
     * Returns all the teams in this game
     * @return Set of teams
     */
    public Set<Team> getTeams() {
        return scoreboard.getTeams();
    }

    /**
     * @return List of team names for this game
     */
    public List<String> getTeamsNames() {
        List<String> teamNames = new ArrayList<>();
        for (Team team: scoreboard.getTeams()) {
            teamNames.add(team.getName());
        }
        return teamNames;
    }
}
