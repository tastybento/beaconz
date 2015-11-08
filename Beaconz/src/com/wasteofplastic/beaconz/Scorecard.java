package com.wasteofplastic.beaconz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
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
    //private HashMap<Team, List<UUID>> teamMembers;
    //private HashMap<UUID, String> teamLookup;

    public Scorecard(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        this.manager = beaconzPlugin.getServer().getScoreboardManager();
        this.scoreboard = manager.getNewScoreboard();
        this.teamBlock = new HashMap<Team, MaterialData>();
        //this.teamLookup = new HashMap<UUID, String>();
        //this.teamMembers = new HashMap<Team, List<UUID>>();

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
        for (Team team: scoreboard.getTeams()) {
            List<String> members = getBeaconzPlugin().getConfig().getStringList(team.getName());
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
            getBeaconzPlugin().getConfig().set(team.getName(), teamMembers);
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
}
