/*
 * Copyright (c) 2015 - 2025 tastybento
 */

package com.wasteofplastic.beaconz;

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
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.wasteofplastic.beaconz.Params.GameScoreGoal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Scorecard extends BeaconzPluginDependent{
    private static final Integer MAXSCORELENGTH = 10;
    private boolean gameON;
    private Game game;
    private String gameName;
    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private Score scoreentry;
    private Score scoreline;
    private Objective scoreobjective;
    private int countdownTimer;
    private int timerinterval;
    private Boolean showtimer;
    private String timertype;
    private String displaytime;
    private Integer sidebarline;
    private String goalstr;
    private Long starttimemilis;
    private BukkitTask timertaskid;
    private HashMap<Team, Location> teamSpawnPoint = new HashMap<>();
    private HashMap<Team, HashMap<GameScoreGoal,Integer>> score = new HashMap<>();
    private HashMap<Team, List<UUID>> teamMembers = new HashMap<>();
    private HashMap<UUID, Team> teamLookup = new HashMap<>();
    private HashMap<Team, Material> teamBlocks = new HashMap<>();

    /**
     * Scorecard controls all aspects of
     *    (i) the Scoreboard displayed on screen
     *    (ii) the Teams associated to the Scoreboards
     *    (iii) the timer for this instance of the game
     *    (iv) the goal for this instance of the game
     * <p>
     *  Note that a single Scoreboard is shown for all teams and that the Scoreboard can show as many different "score types" as we want
     *  Keep in mind that the side bar has as maximum of 16 lines, including the title
     *  Currently we have 4 "score types" defined: area, beacons, links, triangles.
     *  The command for newgame should let the admin specify which "score types" will be displayed; this will allow different instances of the game to be run with different scorecards and goals
     *  The game.getGamegoal() has to be one of the "score types". The newgame command should also let the admin specify the game goal
     *  Since all this can vary from one instance of the game to another, it makes little sense to keep these definitions in config.yml (maybe only as defaults)"
     * @param beaconzPlugin
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
     * Handles plugin Reload
     */
    public void reload() {
        saveTeamMembers();
        initialize(false);
        refreshScores();
        refreshSBdisplay();
    }

    /**
     * Initializes the scoreboard, starts the game
     * Prepares the timer, scoretypes, scores and score values per game mode
     *
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
        scoreobjective = scoreboard.registerNewObjective("score", "beaconz");
        scoreobjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebarline = 15;

        // Set up the scoreboard with the goal
        scoreobjective.displayName(Lang.titleBeaconz.append(Component.text(" " + game.getGamemode().name() + "! 00d 00:00:00").color(NamedTextColor.GREEN)));

        goalstr = "";
        if (game.getGamegoalvalue() == 0) {
            goalstr = PlainTextComponentSerializer.plainText().serialize(Lang.scoreGetTheMostGoal.color(NamedTextColor.GREEN)).replace("[goal]", game.getGamegoal().name());
        } else {
            String value = String.format(Locale.US, "%,d", game.getGamegoalvalue());
            goalstr = PlainTextComponentSerializer.plainText().serialize(Lang.scoreGetValueGoal.color(NamedTextColor.GREEN)).replace("[value]", value).replace("[goal]", game.getGamegoal().name());
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
     * Pauses the game
     */
    public void pause() {
        gameON = false;
    }

    /**
     * Resumes the game
     */
    public void resume() {
        gameON = true;
    }

    /**
     * Adds teams to the game from the config file
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
        if (teamcnt == 0) getLogger().severe("Scorecard.addTeams did not add any teams. Game: " + game.getName());
    }

    /**
     * Updates the Scoreboard values for all teams
     *
     */
    public void refreshScores() {
        for (Team team: scoreboard.getTeams()) {
            refreshScores(team);
        }
    }

    /**
     * Updates the Scoreboard values for team
     * @param team
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
     * Refresh a specific score type for team.
     * @param team
     * @param scoretype - can be area, beacons, links, triangles
     * @param value - default value
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
            team.setPrefix(ChatColor.valueOf(teamChatColor(team)) + "[" + teamDisplayName +"] " + ChatColor.RESET);
            team.setDisplayName(teamDisplayName);
            // Store the block for the team
            //getLogger().info("DEBUG: [" +game.getName() + "] Adding team " + team.getDisplayName() + " team block = " + teamBlock);
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
        String teamcolor = teamChatColor(team);
        String fixedstring = "";
        String formattedScore = String.format(Locale.US, "%,d", score);
        String padstring = "____________________".substring(0, Math.max(0, maxlen - 1 - formattedScore.length()));
        fixedstring = ChatColor.GRAY + padstring + ChatColor.valueOf(teamcolor) + formattedScore + " " + team.getDisplayName() + " " + scoretype;
        return fixedstring;
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
        //getLogger().info("DEBUG: Send player home method called");
        Team team = getTeam(player);
        if (!ingameOnly || game.getRegion().isPlayerInRegion(player)) {
            Location loc = teamSpawnPoint.get(team);
            // Teleport player to a captured beacon
            //getLogger().info("DEBUG: Team is " + team.getName());
            List<BeaconObj> beaconz = getRegister().getTeamBeacons(team);
            //getLogger().info("DEBUG: # of beaconz = " + beaconz.size());
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
            //getLogger().info("saved " + scoretype + " value " + value + " for " + team.getDisplayName());
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
        //getLogger().info("Team spawn: " + team.getDisplayName() + " >> " + teamSP);

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
     * Converts a serialized location to a Location. Returns null if string is
     * empty
     *
     * @param s - serialized location in format "world:x:y:z:yaw:pitch"
     * @return Location
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
     * Converts a location to a simple string representation
     * If location is null, returns empty string
     *
     * @param l
     * @return
     */
    static public String getStringLocation(final Location l) {
        if (l == null || l.getWorld() == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" + Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
    }


    /**
     * Returns the team with the highest score of a given type
     *
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

    public String teamChatColor(Team team) {
        // IMPORTANT: The a team's display name is ALWAYS the team's name in uppercase, PRECEEDED BY the ChatColor
        String tn = team.getName().toUpperCase();
        String cc = "WHITE";
        switch (tn) {
        case "RED":       {cc = "RED"; break;}
        case "BLUE":      {cc = "BLUE"; break;}
        case "WHITE":     {cc = "WHITE"; break;}
        case "ORANGE":    {cc = "DARK_RED"; break;}
        case "MAGENTA":   {cc = "RED"; break;}
        case "LIGHTBLUE": {cc = "AQUA"; break;}
        case "YELLOW":    {cc = "YELLOW"; break;}
        case "LIME":      {cc = "GREEN"; break;}
        case "PINK":      {cc = "RED"; break;}
        case "GRAY":      {cc = "DARK_GRAY"; break;}
        case "LIGHTGRAY": {cc = "GRAY"; break;}
        case "CYAN":      {cc = "BLUE"; break;}
        case "PURPLE":    {cc = "DARK_PURPLE"; break;}
        case "BROWN":     {cc = "GOLD"; break;}
        case "GREEN":     {cc = "DARK_GREEN"; break;}
        case "BLACK":     {cc = "BLACK"; break;}
        default:
        }
        return cc;
    }

    /**
     * Ends the game
     */
    public void endGame() {
        //getLogger().info("DEBUG: end game called");
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
            //getLogger().info("DEBUG: telling team results");
            for (Team team : scoreboard.getTeams()) {
                //getLogger().info("DEBUG: team = " + team.getDisplayName());
                for (String entry : team.getEntries()) {
                    //getLogger().info("DEBUG: entry = " + entry);
                    UUID uuid = getBeaconzPlugin().getNameStore().getPlayerUUID(entry);
                    //getLogger().info("DEBUG: uuid = " + uuid);
                    if (uuid != null) {
                        Player player = Bukkit.getServer().getPlayer(uuid);
                        if (player != null) {
                            // Tell players in the game
                            if (game.getRegion().isPlayerInRegion(player)) {
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
                        //getLogger().info("DEBUG: countdown timer expired - ending game");
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
