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

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class Scorecard extends BeaconzPluginDependent{
    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private HashMap<Team, MaterialData> teamBlock;
    private HashMap<Team, Integer> score = new HashMap<Team, Integer>();
    //private HashMap<Team, List<UUID>> teamMembers;
    //private HashMap<UUID, String> teamLookup;

    public Scorecard(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        this.manager = beaconzPlugin.getServer().getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();
        this.teamBlock = new HashMap<Team, MaterialData>();
        this.score.clear();
        //this.teamLookup = new HashMap<UUID, String>();
        //this.teamMembers = new HashMap<Team, List<UUID>>();

        Objective objective = scoreboard.registerNewObjective("teamscore", "blocks");
        //objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        //Setting the display name of the scoreboard/objective
        objective.setDisplayName(" blocks");
    }

    public void addTeam(String teamName, MaterialData teamBlock) {
        Team team = scoreboard.registerNewTeam(teamName);
        team.setAllowFriendlyFire(false);
        team.setPrefix(ChatColor.DARK_PURPLE + "[" + teamName + "] ");
        // Store the block for this team
        this.teamBlock.put(team, teamBlock);
    }

    @SuppressWarnings("deprecation")
    public Team getTeam(Player player) {
    return scoreboard.getPlayerTeam(player);
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
        for (Entry<Team, MaterialData> md: teamBlock.entrySet()) {
            if (md.getValue().getItemType().equals(b.getType()) && md.getValue().getData() == b.getData()) {
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
            return;
        }
        YamlConfiguration teamsYml = new YamlConfiguration();
        try {
            teamsYml.load(teamFile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Problem with beaconz.yml formatting");
            e.printStackTrace();
        }
        for (Team team: scoreboard.getTeams()) {
            List<String> members = teamsYml.getStringList(team.getName());
            //List<UUID> membersUUID = new ArrayList<UUID>();
            for (String uuid : members) {
                try {
                    UUID memberUUID = UUID.fromString(uuid);
                    OfflinePlayer player = getBeaconzPlugin().getServer().getOfflinePlayer(memberUUID);
                    team.addPlayer(player);
                    //teamLookup.put(memberUUID, team.getName());
                } catch (Exception e) {
                    getLogger().severe("Error loading team member " + team.toString() + " " + uuid + " - skipping");
                }
            }
            //teamMembers.put(team,membersUUID);
        }
    }

    /**
     * Saves the teams to the config file
     */
    @SuppressWarnings("deprecation")
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
            for (OfflinePlayer player : team.getPlayers()) {
                try {
                    teamMembers.add(player.getUniqueId().toString());
                    //teamLookup.put(memberUUID, team.getName());
                } catch (Exception e) {
                    getLogger().severe("Error saving team member " + team.toString() + " " + player.getName() + " - skipping");
                }
            }
            teamsYml.set(team.getName(), teamMembers);
        }
        try {
            teamsYml.save(teamsFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * @param member
     * @return member's team if it exists, null otherwise
     */
    @SuppressWarnings("deprecation")
    public Team getTeam(OfflinePlayer member) {
        // Run through the teams and find the player
        for (Team team : scoreboard.getTeams()) {
            for (OfflinePlayer player : team.getPlayers()) {
            if (player.equals(member)) {
                return team;
            }
            }
        }
        return null;
    }
    
    /**
     * @return the score
     */
    public HashMap<Team, Integer> getScore() {
        return score;
    }

    /**
     * Set the score for a team
     * @param team
     * @param score
     */
    public void setScore(Team team, int score) {
        this.score.put(team, score);
    } 
    
    /**
     * @param team
     * @return score for faction
     */
    public int getScore(Team team) {
        if (score.containsKey(team)) {
            return score.get(team);
        }
        return 0;
    }

    /**
     * Adds score to team
     * @param owner
     * @param area
     */
    public void addScore(Team owner, int area) {
        if (score.containsKey(owner)) {
            int s = score.get(owner);
            s += area;
            score.put(owner, s);
        } else {
            score.put(owner, area);
        } 
    }  
    
    /**
     * Removes score from team
     * @param owner
     * @param area
     */
    public void removeScore(Team owner, int area) {
        addScore(owner, -area);
        if (score.get(owner) < 0) {
            score.put(owner, 0);
        }
    }

}
