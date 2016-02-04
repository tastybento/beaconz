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

package com.wasteofplastic.beaconz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class Scorecard extends BeaconzPluginDependent{
	private Beaconz plugin;
	private Boolean gameON;
    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private Scoreboard emptyscoreboard;
    private Score scoreentry;
    private Score scoreline;
    private Objective scoreobjective;
    private Integer timer;
	private Integer timerinterval;
	private String timertype;
	private String displaytime;
	private Integer sidebarline;
    private String gamemode;
	private String gamegoal;
	private String goalstr;
	private Integer gamegoalvalue;
	private String showscoretypes;
	private Long starttimemilis;
	private BukkitTask timertaskid;
    private HashMap<Team, MaterialData> teamBlock;
    private HashMap<Team, Location> teamSpawnPoint = new HashMap<Team, Location>();
    private HashMap<Team, HashMap<String,Integer>> score = new HashMap<Team, HashMap<String,Integer>>();
    //private HashMap<Team, List<UUID>> teamMembers;
    //private HashMap<UUID, String> teamLookup;

    /** 
     * Scorecard controls all aspects of 
     *    (i) the Scoreboard displayed on screen
     *    (ii) the Teams associated to the Scoreboards
     *    (iii) the timer for this instance of the game
     *    (iv) the goal for this instance of the game
     *    
     *  Note that a single Scoreboard is shown for all teams and that the Scoreboard can show as many different "score types" as we want
     *  Keep in mind that the side bar has as maximum of 16 lines, including the title
     *  Currently we have 4 "score types" defined: area, beacons, links, triangles. 
     *  The command for newgame should let the admin specify which "score types" will be displayed; this will allow different instances of the game to be run with different scorecards and goals
     *  The gamegoal has to be one of the "score types". The newgame command should also let the admin specify the game goal
     *  Since all this can vary from one instance of the game to another, it makes little sense to keep these definitions in config.yml (maybe only as defaults)"
     * @param beaconzPlugin
     */
    public Scorecard(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        this.plugin = beaconzPlugin;
        this.manager = beaconzPlugin.getServer().getScoreboardManager();
        initialize(true);
    }
    
    /**
     * Handles plugin Reload
     */
    public void reload() {
    	initialize(false);
    	refreshScores();
    }
    
    /**
     * Initializes the game with the scoreboard
     * 
     */
    public void initialize(Boolean newGame) {
        this.teamBlock = new HashMap<Team, MaterialData>();              
        this.starttimemilis = ((System.currentTimeMillis()+500)/1000)*1000;
        this.timerinterval = 5;
           
        // prepare the timer, scoretypes, scores and score values per game mode
        gamemode = Settings.gamemode;
        timer = 0;
        if (gamemode.equals("strategy")) {
        	timer = Settings.strategyTimer;
        	showscoretypes = Settings.strategyScoreTypes;
        	gamegoal = Settings.strategyGoal;
        	gamegoalvalue = Settings.strategyGoalValue;        	
        }
        if (gamemode.equals("minigame")) {
        	timer = Settings.minigameTimer;
        	showscoretypes = Settings.minigameScoreTypes;
        	gamegoal = Settings.minigameGoal;
        	gamegoalvalue = Settings.minigameGoalValue;        	
        }
        
        timertype = timer == 0 ? "openended" : "countdown";                               
        
        // Define the scoreboard
        try {
            scoreboard.clearSlot(DisplaySlot.SIDEBAR);                    
        } catch (Exception e){ };
        try {
            scoreobjective.unregister();                    
        } catch (Exception e){ };
        
        scoreboard = manager.getNewScoreboard();
        emptyscoreboard = manager.getNewScoreboard(); // careful to never add anything to this scoreboard!
        scoreobjective = scoreboard.registerNewObjective("score", "dummy");
        scoreobjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebarline = 15;
        
        // Include the timer and the goal
        scoreobjective.setDisplayName(ChatColor.GREEN + "Beaconz " + gamemode + "! 00d 00:00:00");
        goalstr = "";
        if (gamegoalvalue == 0) {
        	goalstr = "<< Get the most " + gamegoal + "!! >>";
        } else {
        	goalstr = "<< Get " + gamegoalvalue + " " + gamegoal + "!! >>";
        }
        goalstr = ChatColor.GREEN + goalstr;
        scoreline = scoreobjective.getScore(goalstr);
        scoreline.setScore(sidebarline);                
        
        // Start the game
        gameON = true;    
        
        // Reset the score
        score.clear();
        
        // Create the teams and enable scoreboards
        addTeamsFromFile();
        loadTeamMembers();
    	setPlayersScoreboard();
        
        // Send everyone home, restart the Game 
        if (newGame) sendPlayersHome();
        
        // Start the timer
        runtimer();          
    }
    
    /**
     * Adds teams to the game from the config file
     */
    @SuppressWarnings("deprecation")
	protected void addTeamsFromFile() {
        for (String teamName: getBeaconzPlugin().getConfig().getConfigurationSection("teams").getValues(false).keySet()) {
            MaterialData teamBlock = new MaterialData(Material.STAINED_GLASS,(byte) getBeaconzPlugin().getConfig().getInt("teams." + teamName + ".glasscolor"));
            Team team = addTeam(teamName, teamBlock);
            team.setDisplayName(ChatColor.translateAlternateColorCodes('&', getBeaconzPlugin().getConfig().getString("teams." + teamName + ".displayname", teamName)));
        }
    }    
    
    /**
     * Updates the Scoreboard values 
     * 
     */
    public void refreshScores() {
        for (Team team: scoreboard.getTeams()) {
        	refreshScores(team);
        }
    }
    public void refreshScores(Team team) {
    	Integer defaultvalue = null;
    	if (score.get(team) == null) defaultvalue = 0;   // if team doesn't have score, create it and set to 0
    	for (String st : showscoretypes.split(":")) {
    		refreshScores(team, st, defaultvalue);
    	}

    }
    public void refreshScores(Team team, String scoretype) {
    	refreshScores(team, scoretype, null);
    }
    public void refreshScores(Team team, String scoretype, Integer value) {
    	if (gameON) {
    		if (value == null) {
        		switch (scoretype) {
    	    		case ("area"): {
    	    			value = getRegister().getTeamArea(team);    
    	    			break;
    	    		}
    	    		case ("beacons"): {
    	    			value = getRegister().getTeamBeacons(team).size();	    	    	
    	    			break;
    	    		}
    	    		case ("links"): {
    	    			value = getRegister().getTeamLinks(team).size()/2;
    	    			break;
    	    		}
    	    		case ("triangles"): {
    	    			value = getRegister().getTeamTriangles(team).size();
    	    			break;
    	    		}
    	    		default:
    	    			break;
        		}   
        	}
    		//Update the score - putscore will call refreshSBDisplay()
    		putScore(team, scoretype, value);
    		
    		// See if we have a winner
    		if (timertype.equals("openended") && scoretype.equals(gamegoal) && value >= gamegoalvalue) {
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
        	refreshSBdisplay(team, null);
        }
    }
    public void refreshSBdisplay(Team team) {  	
    	if (score.get(team) != null) {
        	for (String st : showscoretypes.split(":")) {
        		refreshSBdisplay(team, st);	
        	}
    	}    	    	
    }
    public void refreshSBdisplay(Team team, String scoretype) {    	    	   	
		// The setScore values are actually line numbers on the scoreboard
		// the actual scores go in the score description
    	
    	// Refresh the team scores for the given score type, if it can be shown
    	if (gameON && showscoretypes.contains(scoretype)) {
            String teamcolor = team.getDisplayName().toUpperCase();
        	HashMap<String,Integer> stypes = score.get(team);
        	int sv = 0;
        	if (stypes != null && stypes.get(scoretype) != null) sv = stypes.get(scoretype);
            String scorestring = fixScoreString(teamcolor, scoretype, sv, 8);
            
            String oldentry = sbEntry(teamcolor, scoretype);
            int line = scoreobjective.getScore(oldentry).getScore();
    		scoreboard.resetScores(oldentry);
    		scoreentry = scoreobjective.getScore(scorestring);
    		scoreentry.setScore(line);	
    	}
    }    
    
    /**
     * Adds a team to the scoreboard and returns the team that was made
     * @param teamName
     * @param teamBlock
     * @return team
     */
    public Team addTeam(String teamName, MaterialData teamBlock) {
        Team team = scoreboard.registerNewTeam(teamName);
        team.setAllowFriendlyFire(false);
        team.setPrefix(ChatColor.DARK_PURPLE + "[" + teamName + "] ");
        // Store the block for this team
        this.teamBlock.put(team, teamBlock);
        String teamcolor = teamName.toUpperCase();        
        
		// Now it gets tricky... the setScore values are actually line numbers on the scoreboard
		// the actual scores go in the score description
		for (String st : showscoretypes.split(":")) {
			sidebarline -= 1;
			if (sidebarline > 0 ) {
				String scorestring = fixScoreString(teamcolor, st, 0, 8);
				scoreentry = scoreobjective.getScore(scorestring);
				scoreentry.setScore(sidebarline);		
			} else {
				getLogger().info("Could not show new team scores on the sidebar, ran out of lines. Team = " + teamName);
			}
		}        
		refreshScores(team);
        return team;
    }
    
    /** 
     * Fixes the string to show on the sidebar
     * Since the sidebar only shows scores in decreasing order, the only way to sort them
     * the way we want is to use the scores for line numbers and keep our own
     * scores in the score description.
     * This method takes a team color, a score's name, a score value and a max length
     * and returns a string to be displayed in the sidebar.
     * For instance, fixScoreString ("RED", "beacons", 10, 8) will return "______10 RED beacons"
     */
    public String fixScoreString (String teamcolor, String scorename, Integer score, Integer maxlen) {
    	String fixedstring = "";
    	String padstring = "____________________".substring(0, maxlen - 1 - score.toString().length());
		fixedstring = ChatColor.GRAY + padstring + ChatColor.valueOf(teamcolor) + score + " " + teamcolor + " " + scorename;
		return fixedstring;
    }
    
    /** 
     * Returns the first scoreboard Entry for a given team + score type - and *** there can be only ONE *** (yes, that's a reference to Highlander)
     *  
     */
    public String sbEntry (String teamcolor, String scorename) {
    	String scoreboardentry = "";
    	for (String entry : scoreboard.getEntries()) {
    		if (entry.contains(teamcolor.toUpperCase() + " " + scorename)) {
    			scoreboardentry = entry;
        		break;	
    		}    			
    	}
    	return scoreboardentry;
    }
    

    /**
     * When resetting the game, move all players to their spawn positions
     * Also make sure they each have a scoreboard
     * NOTE: THIS CURRENTLY TAKES EVERY PLAYER IN THE BEACONZ WORLD; WILL NEED TO BE ADJUSTED TO REGIONS WHEN WE HAVE MULTIPLE INSTANCES OF THE GAME IN THE SAME WORLD    
     */
    public void setPlayersScoreboard() {
        for (Player player : plugin.getBeaconzWorld().getPlayers()) { 
            player.setScoreboard(scoreboard);
        }    	
    }
    
    public void sendPlayersHome() {
        for (Player player : plugin.getBeaconzWorld().getPlayers()) { 
            Team team = getTeam(player);
            Location spawn = getTeamSpawnPoint(team);
            player.teleport(spawn);
            player.sendMessage("You are a member of " + team.getDisplayName() + " team!");
            getServer().dispatchCommand(getServer().getConsoleSender(),
                    "title " + player.getName() + " title {text:\"New Game!\", color:gold}");
            getServer().dispatchCommand(getServer().getConsoleSender(),
                    "title " + player.getName() + " subtitle {text:\"" + team.getDisplayName() + " team\", color:blue}");
        }
    }
    
    
    /**
     * Returns an ONLINE PLAYER's team. 
     * If the player does not have a team, he is put in one
     * @param player
     * @return Team
     */
    public Team getTeam(Player player) {
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team == null) {
            // New player!
            int minSize=Integer.MAX_VALUE;
            for (Team t: scoreboard.getTeams()) {
                if(t.getSize() < minSize) {
                    minSize=t.getSize();
                    team=t;
                }
            }
            team.addEntry(player.getName());
        }
        return team;
    }

    /**
     * Returns an OFFLINE PLAYER's team if it exists, null otherwise
     * @param member
     */
    public Team getTeam(OfflinePlayer member) {
        // Run through the teams and find the player
        for (Team team : scoreboard.getTeams()) {
            for (String playername : team.getEntries()) {
                if (playername.equals(member.getName())) {
                    return team;
                }
            }
        }
        return null;
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
     * @return block type
     */
    public MaterialData getBlockID(Team team) {
        return teamBlock.get(team);
    }

    /**
     * Get the team defined by this block
     * @param b - block
     * @return Team, or null if it doesn't exist
     */
	public Team getTeamFromBlock(Block b) {
		
		// ?? WHY STORE MATERIALDATA IN THE HASHMAP AND NOT SIMPLY THE BLOCK?
		
        for (Entry<Team, MaterialData> md: teamBlock.entrySet()) {        	
            //if (md.getValue().getItemType().equals(b.getType()) && md.getValue().getData() == b.getData()) {
            //    return md.getKey();
            //}        
            if (md.getValue().getItemType().equals(b.getType()) &&
            	md.getValue().toItemStack().getItemMeta().getDisplayName().equals(b.getState().getData().toItemStack().getItemMeta().getDisplayName()) &&
            	md.getValue().toItemStack().getItemMeta().getLore().equals(b.getState().getData().toItemStack().getItemMeta().getLore())
            	) {
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
        String result = "";
        for (Team team : scoreboard.getTeams()) {
            if (result.isEmpty()) {
                result = team.getName();
            } else {
                result += ", " + team.getName();
            }
        }
        return result;
    }

    /**
     * @return the scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }
    
    /**
     * @return the empty scoreboard
     */
    public Scoreboard getEmptyScoreboard (){
    	return emptyscoreboard;
    }
    		

    /**
     * Loads all the team members in UUID format
     */
    public void loadTeamMembers() {
        File teamFile = new File(getBeaconzPlugin().getDataFolder(),"teams.yml");
        if (!teamFile.exists()) {
            saveTeamMembers();
        }
        YamlConfiguration teamsYml = new YamlConfiguration();
        try {
            teamsYml.load(teamFile);
        } catch (FileNotFoundException e) {
            // Catch block
            e.printStackTrace();
        } catch (IOException e) {
            // Catch block
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Problem with teams.yml formatting");
            e.printStackTrace();
        }
        for (Team team: scoreboard.getTeams()) {
            List<String> members = teamsYml.getStringList(team.getName());
            //List<UUID> membersUUID = new ArrayList<UUID>();
            for (String uuid : members) {
                try {
                    UUID memberUUID = UUID.fromString(uuid);
                    OfflinePlayer player = getBeaconzPlugin().getServer().getOfflinePlayer(memberUUID);
                    team.addEntry(player.getName());
                    //teamLookup.put(memberUUID, team.getName());
                } catch (Exception e) {
                    getLogger().severe("Error loading team member " + team.toString() + " " + uuid + " - skipping");
                }
            }
            // Get spawn point
            String location = teamsYml.getString("spawnlocations." + team.getName());
            Location loc = null;
            if (location == null) {
                // No team spawn point stored so pick a default location
                loc = getDefaultTeamSpawnPoint(team);
            } else {
                loc = getLocationString(location);
                if (loc == null) {
                    loc = getDefaultTeamSpawnPoint(team);
                } 
            }
            //getLogger().info("Setting " + team.getName() + "'s spawn point to " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            teamSpawnPoint.put(team, loc);
            //teamMembers.put(team,membersUUID);
        }
    }

    /**
     * Saves the teams to the config file
     */
    public void saveTeamMembers() {
        File teamsFile = new File(getBeaconzPlugin().getDataFolder(),"teams.yml");
        YamlConfiguration teamsYml = new YamlConfiguration();
        // Backup the beacons file just in case
        if (teamsFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"teams.old");
            teamsFile.renameTo(backup);
        }   

        for (Team team: scoreboard.getTeams()) {
            List<String> teamMembers = new ArrayList<String>();
            for (String entry : team.getEntries()) {
            	Player player = Bukkit.getServer().getPlayer(entry);
            	if (player != null) {
                    try {
                        teamMembers.add(player.getUniqueId().toString());
                        //teamLookup.put(memberUUID, team.getName());
                    } catch (Exception e) {
                        getLogger().severe("Error saving team member " + team.toString() + " " + player.getName() + " - skipping");
                    }            		
            	}
            }
            teamsYml.set(team.getName(), teamMembers);
            // Save the team spawn location
            teamsYml.set("spawnpoint." + team.getName(), getStringLocation(teamSpawnPoint.get(team)));
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
    public HashMap<Team, HashMap<String,Integer>> getScore() {
    	return score;   
    }
    
    /**
     * Return the game mode
     */
    public String getGameMode() {
    	return gamemode;   
    }    
    
    /**
     * Return the timer
     */
    public String getTimer(String type) {
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
     */
    public Integer getScore(Team team, String scoretype) {
        if (score.containsKey(team)) {
        	HashMap<String,Integer> scores = score.get(team);
        	if (scores.containsKey(scoretype)) return scores.get(scoretype);
        }
        return 0;
    }    
    
    /**
     * Set the score for a team
     * @param team
     * @param score
     */
    public void putScore(Team team, String scoretype, int value) {
    	if (gameON && team != null && scoretype != null) {    		
        	HashMap<String,Integer> stypes = score.get(team);
        	if (stypes == null) stypes = new HashMap<String,Integer>(); 
        	stypes.put(scoretype, value);
            score.put(team, stypes);  
            //getLogger().info("saved " + scoretype + " value " + value + " for " + team.getDisplayName());
        	refreshSBdisplay(team, scoretype);
    	}
    } 

    /**
     * Adds score to team
     * @param owner
     * @param area
     */
    public void addScore(Team team, String scoretype, int value) {
        if (score.containsKey(team)) {
        	HashMap<String,Integer> stypes = score.get(team); 
        	if (stypes != null && stypes.containsKey(scoretype)) {
        		value  += stypes.get(scoretype);
                if (value < 0 ) value = 0;
        	}
        }        
        putScore(team, scoretype, value); 
    }  

    /**
     * Subtracts score from team
     * @param owner
     * @param area
     */
    public void subtractScore(Team team, String scoretype, int value) {
        addScore(team, scoretype, -value);
    }

    
    /**
     * Returns the location where a team should spawn
     * @param playerTeam
     * @return Location
     */
    public Location getDefaultTeamSpawnPoint(Team playerTeam) {
        Location teleportTo = getBeaconzWorld().getSpawnLocation();
        BlockFace blockFace = BlockFace.NORTH;
        // We allow up to 8 teams
        int direction = 0;
        for (Team team : scoreboard.getTeams()) {
            if (team.equals(playerTeam)) {
                switch (direction) {
                case 0:
                    blockFace = BlockFace.NORTH;
                    break;
                case 1:
                    blockFace = BlockFace.SOUTH;
                    break;
                case 2:
                    blockFace = BlockFace.EAST;
                    break;
                case 3:
                    blockFace = BlockFace.WEST;
                    break;
                case 4:
                    blockFace = BlockFace.NORTH_EAST;
                    break;
                case 5:
                    blockFace = BlockFace.NORTH_WEST;
                    break;
                case 6:
                    blockFace = BlockFace.SOUTH_EAST;
                    break;
                case 7:
                    blockFace = BlockFace.SOUTH_WEST;
                    break;
                }
            }
            direction++;
        }
        teleportTo = teleportTo.getBlock().getRelative(blockFace, Settings.borderSize / 4).getLocation();
        teleportTo = getBeaconzWorld().getHighestBlockAt(teleportTo).getLocation().add(0.5, 0, 0.5);
        // This will result in bedrock blocks being created up and up if the bedrock is covered...
        // TODO these spawn points need special protection, or something. An enemy team could place a lot of blocks
        teleportTo.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        return teleportTo;
    }

    /**
     * Player in team or not
     * @param player
     * @return true if in team, false if not
     */
    public boolean inTeam(Player player) {
        if (scoreboard.getEntryTeam(player.getName()) != null) {
            return true;
        }
        return false;
    }

    /**
<<<<<<< HEAD
<<<<<<< Upstream, based on upstream/master
     * Sets the team's spawn location
     * @param team
     * @param location
     */
    public void setTeamSpawnPoint(Team team, Location location) {
        teamSpawnPoint.put(team, location);
    }

    /**
     * @param playerTeam
     * @return Location of the team's spawn point or null if team is unknown or there is no spawn point
     */
    public Location getTeamSpawnPoint(Team playerTeam) {
        return teamSpawnPoint.get(playerTeam);
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
    public Team frontRunner (String scoretype) {
    	Integer maxscore = 0;
    	Team topteam = null;
        for (Team team: scoreboard.getTeams()) {
            HashMap<String,Integer> stypes = score.get(team); 
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
	 * Ends the game     
	 */
    public void endGame () {    	
    	// Stop timer
    	if (timertaskid != null) timertaskid.cancel();
    	// Stop keeping score
    	gameON = false;
    	// Change the objective line in the scoreboard
		scoreboard.resetScores(goalstr);
        scoreline = scoreobjective.getScore(ChatColor.GREEN + "<< GAME OVER >>");
        scoreline.setScore(15);
    	// Wait a second to let all other messages display first
		getBeaconzPlugin().getServer().getScheduler().runTaskLaterAsynchronously(getBeaconzPlugin(), new Runnable() {
			@Override
			public void run() {
		    	// Announce winner to all players
        		Team winner = frontRunner(gamegoal);   
		        String titleline = "Game over!!";
    			String subtitleline = "There were no winners!";     		
        		if (winner != null) {        		
        			titleline = winner.getDisplayName().toUpperCase() + " TEAM WINS!!!!!";
        			subtitleline = "Congratulations";
        		}	
		        for (Team team : scoreboard.getTeams()) {
		        	for (String entry : team.getEntries()) {
		            	Player player = Bukkit.getServer().getPlayer(entry);
		            	if (player != null) {	            		
		                	getServer().dispatchCommand(getServer().getConsoleSender(),
		                            "title " + player.getName() + " title {text:\"" + titleline + " \", color:gold}");   
		                    getServer().dispatchCommand(getServer().getConsoleSender(),
		                            "title " + player.getName() + " subtitle {text:\"" + subtitleline + " \", color:gold}");
							player.sendMessage(ChatColor.GREEN + "===================================================");
							player.sendMessage(ChatColor.YELLOW + titleline);
							player.sendMessage(ChatColor.YELLOW + subtitleline);
							player.sendMessage(ChatColor.GREEN + "===================================================");			            	
		            	}        		
		        	}
		        }				
			}			
		}, 20); 	    	
    }
	    
    /**
     * Timer
     * @param interval (in seconds)
     * 
     * This runs a countdown if Settings.minigameTimer or Settings.strategyTimer > 0, open-ended clock otherwise
     * 
     */
	public void runtimer () {		
        if (timertaskid != null) timertaskid.cancel();
		timertaskid = getBeaconzPlugin().getServer().getScheduler().runTaskTimerAsynchronously(getBeaconzPlugin(), new Runnable() {
			@Override
			public void run() {
				
				Long seconds = 0L;

				if (timertype.equals("openended")) {
					seconds = (System.currentTimeMillis() - starttimemilis) / 1000;
					Integer t = timerinterval;
					seconds = ((seconds+t-1)/t)*t;
				} else {
					timer = timer - timerinterval;
					if (timer < 1) {
						// Beacon timer ran out
						timer = 0;
						timertaskid.cancel();
						endGame();
					}
					seconds = timer + 0L;
				}

				// display the timer
				long s = seconds % 60;
			    long m = (seconds / 60) % 60;
			    long h = (seconds / (60 * 60)) % 24;
			    long d = (seconds / (60 * 60 * 24)) %100;
			    displaytime = String.format("%02dd %02d:%02d:%02d", d,h,m,s);	
			    
				if (Settings.showTimer) {
					String objName = scoreobjective.getDisplayName();
					if (!objName.contains(":")) objName = objName + "! 00d 00:00:00";
				    objName = objName.substring(0, objName.length() - displaytime.length()) + displaytime;
				    scoreobjective.setDisplayName(objName);			    				        					
				} else {
					scoreobjective.setDisplayName(ChatColor.GREEN + "Beaconz " + gamemode);
				}
			}			
		}, 20, timerinterval*20); 
	}	
}
