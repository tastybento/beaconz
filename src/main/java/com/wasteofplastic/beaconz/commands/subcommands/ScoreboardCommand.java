package com.wasteofplastic.beaconz.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.commands.SubCommand;
import com.wasteofplastic.beaconz.game.Game;

/**
 * Handles the /beaconz sb (scoreboard toggle) command.
 */
public class ScoreboardCommand extends BeaconzPluginDependent implements SubCommand {

    public ScoreboardCommand(Beaconz plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, Player player, String[] args) {
        if (player == null) {
            return true;
        }

        // Toggle scoreboard visibility
        Game game = getGameMgr().getGame(player.getLocation());
        if (game != null && game.getScorecard() != null) {
            // Check if player currently has the game scoreboard
            if (player.getScoreboard().equals(game.getScorecard().getScoreboard())) {
                // Hide scoreboard by giving them a blank one
                player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
            } else {
                // Show the game scoreboard
                player.setScoreboard(game.getScorecard().getScoreboard());
            }
        } else {
            // Not in a game, just toggle to blank
            player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
        }
        return true;
    }

    @Override
    public String getName() {
        return "sb";
    }

    @Override
    public String getPermission() {
        return null; // No special permission required
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Player player, String[] args) {
        return new ArrayList<>();
    }
}

