package com.wasteofplastic.beaconz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

public class Scorecard {
    private Beaconz plugin;
    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private HashMap<Team, MaterialData> teamBlock;
    /**
     * @param plugin
     */
    public Scorecard(Beaconz plugin) {
	this.plugin = plugin;
	this.manager = plugin.getServer().getScoreboardManager();
	scoreboard = manager.getNewScoreboard();
	this.teamBlock = new HashMap<Team, MaterialData>();
	Objective objective = scoreboard.registerNewObjective("Team score", "blocks");
	objective.setDisplaySlot(DisplaySlot.SIDEBAR); 
	//Setting the display name of the scoreboard/objective
	objective.setDisplayName("Blocks Owned");
    }

    public void addTeam(String teamName, MaterialData teamBlock) {
	Team team = scoreboard.registerNewTeam(teamName);
	team.setAllowFriendlyFire(false);
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
     * @param string
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
    public String listTeams() {	
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


}
