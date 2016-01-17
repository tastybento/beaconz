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
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class Scorecard extends BeaconzPluginDependent{
    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private Score scorearea;
    private Score scorebeacons;
    private Score scorelinks;
    private Score scoretriangles;
    //private Score scoretimer;
    private Objective objective;
	private Integer timerinterval;
	private Long starttime;
    private HashMap<Team, MaterialData> teamBlock;
    private HashMap<Team, Location> teamSpawnPoint = new HashMap<Team, Location>();
    private HashMap<Team, HashMap<String,Integer>> score = new HashMap<Team, HashMap<String,Integer>>();
    //private HashMap<Team, List<UUID>> teamMembers;
    //private HashMap<UUID, String> teamLookup;

    /** 
     * Scorecard controls all aspects of 
     *    (i) the Scoreboard displayed on screen
     *    (ii) the Teams associated to the Scoreboards
     *  Note that a single Scoreboard is shown for all teams
     *  and that the Scoreboard can show as many different "score types" as we want
     *  (such as countdown timer, total area, nbr of beacons, nbr. of triangles, total protection strength, nbr of adjacent areas, etc.)
     *  So far we've implemented total area and nbr of beacons, next is the countdown timer, the others are just ideas.
     * @param beaconzPlugin
     */
    public Scorecard(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        this.manager = beaconzPlugin.getServer().getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();
        this.teamBlock = new HashMap<Team, MaterialData>();
        this.score.clear();
        //this.teamLookup = new HashMap<UUID, String>();
        //this.teamMembers = new HashMap<Team, List<UUID>>();
        this.starttime = ((System.currentTimeMillis()+500)/1000)*1000;
        this.timerinterval = 5;

        objective = scoreboard.registerNewObjective("teamscore", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GREEN + "Beaconz!");
        //scoretimer = objective.getScore(ChatColor.YELLOW + "Timer ");
        this.runtimer();
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
    	String [] typelist = {"area", "beacons", "links", "triangles"};
    	Integer defaultvalue = null;
    	if (score.get(team) == null) defaultvalue = 0;   // if team doesn't have score, create it and set to 0
    	for (String st : typelist) {
    		refreshScores(team, st, defaultvalue);
    	}

    }
    public void refreshScores(Team team, String st) {
    	refreshScores(team, st, null);
    }
    public void refreshScores(Team team, String st, Integer value) {
    	if (value == null) {
    		switch (st) {
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
    	//getLogger().info("putScore: team " + team.getDisplayName() + " type " + st + " value " + value);
		putScore(team, st, value);  // putscore will call refreshSBDisplay()

    }
    
    /**
     * Updates the Scoreboard display 
     * 
     * typelist can be changed to an argument to specify which "score types" to display
     */
    public void refreshSBdisplay() {
        for (Team team: scoreboard.getTeams()) {
        	refreshSBdisplay(team, null);
        }
    }
    public void refreshSBdisplay(Team team) {
    	String [] typelist = {"area", "beacons", "links", "triangles"};    	
    	if (score.get(team) != null) {
        	for (String st : typelist) {
        		refreshSBdisplay(team, st);	
        	}
    	}    	    	
    }
    public void refreshSBdisplay(Team team, String st) {
    	
    	// fist the timer
    	//scoretimer.setScore(0);
    	objective.setDisplayName(ChatColor.GREEN + "Beaconz!   " + "00:00:00");
    	
    	// then the team scores for the given score type
    	HashMap<String,Integer> stypes = score.get(team);
		int sv = stypes.get(st) == null ? 0 : stypes.get(st); 		
        String teamcolor = team.getDisplayName().toUpperCase();
        //getLogger().info("updating: " + teamcolor + " type " + st + " value " + sv);
		switch (st) {
    		case ("area"): {	
    			scorearea = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " area:");
    			scorearea.setScore(sv);
    			break;
    		}
    		case ("beacons"): {
    			scorebeacons = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " beacons:");
    			scorebeacons.setScore(sv);
    			break;
    		}
    		case ("links"): {
    			scorelinks = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " links:");
    			scorelinks.setScore(sv);
    			break;
    		}
    		case ("triangles"): {
    			scoretriangles = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " triangles:");
    			scoretriangles.setScore(sv);
    			break;
    		}
    		default:
    			break;
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
        // Add lines for the team on the displayed scoreboard
        String teamcolor = teamName.toUpperCase();
		scorearea = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " area:");
		scorebeacons = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " beacons:");	
		scorelinks = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " links:");
		scoretriangles = objective.getScore(ChatColor.valueOf(teamcolor) + teamcolor + " triangles:");
		scorearea.setScore(0);
		scorebeacons.setScore(0);
		scorelinks.setScore(0);
		scoretriangles.setScore(0);
		refreshScores(team);
        return team;
    }

    /**
     * Returns the player's team. If the player does not have a team, he is put in one
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
            getLogger().info("Setting " + team.getName() + "'s spawn point to " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
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
     * @param member
     * @return member's team if it exists, null otherwise
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
     * Return the scoreboard manager 
     */
    public ScoreboardManager getManager() {
    	return manager;
    }
    
    /**
     * @return the scores
     */
    public HashMap<Team, HashMap<String,Integer>> getScore() {
    	return score;   
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
    	if (team != null && scoretype != null) {    		
        	HashMap<String,Integer> stypes = score.get(team);
        	if (stypes == null) stypes = new HashMap<String,Integer>(); 
        	stypes.put(scoretype, value);
            score.put(team, stypes);  
            //getLogger().info("saved " + scoretype + " value " + value + " for " + team.getDisplayName());
    	}
    	refreshSBdisplay(team, scoretype);
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
     * Clears the score and removes team members from teams
     */
    public void clear() {
        // Clear the score
        score.clear();     
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
     * Timer
     * @param interval (in seconds)
     */
	public void runtimer () {
		getBeaconzPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getBeaconzPlugin(), new Runnable() {
			@Override
			public void run() {
				Long seconds = (System.currentTimeMillis() - starttime) / 1000;
				Integer t = timerinterval;
				seconds = ((seconds+t-1)/t)*t;
				long s = seconds % 60;
			    long m = (seconds / 60) % 60;
			    long h = (seconds / (60 * 60)) % 24;
			    String elapsed = String.format("%d:%02d:%02d", h,m,s);
				objective.setDisplayName(ChatColor.GREEN + "Beaconz!   " + elapsed);				
			}			
		}, 20, timerinterval*20); 
	}	
}
