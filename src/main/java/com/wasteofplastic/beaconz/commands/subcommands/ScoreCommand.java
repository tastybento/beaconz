package com.wasteofplastic.beaconz.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.commands.SubCommand;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;
import com.wasteofplastic.beaconz.game.Game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * Handles the /beaconz score command.
 */
public class ScoreCommand extends BeaconzPluginDependent implements SubCommand {

    public ScoreCommand(Beaconz plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, Player player, String[] args) {
        if (player == null) {
            sender.sendMessage(Lang.errorOnlyPlayers);
            return true;
        }

        Game game = getGameMgr().getGame(player.getLocation());
        if (game == null || game.getScorecard() == null || game.getScorecard().getTeam(player) == null) {
            sender.sendMessage(Lang.errorYouMustBeInAGame);
        } else {
            sender.sendMessage(Lang.generalGame.append(Component.text(": "))
                .append(game.getName()));
            Team team = game.getScorecard().getTeam(player);
            if (team != null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(Lang.actionsYouAreInTeam,
                    Placeholder.component("team", team.displayName())));
            }
            showGameScores(sender, game);
        }
        return true;
    }

    @Override
    public String getName() {
        return "score";
    }

    @Override
    public String getPermission() {
        return null; // No special permission required
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Player player, String[] args) {
        return new ArrayList<>();
    }

    /**
     * Displays the scores for a game
     */
    private void showGameScores(CommandSender sender, Game game) {
        // Refresh scores
        game.getScorecard().refreshScores();
        sender.sendMessage(Lang.scoreScores.color(NamedTextColor.AQUA));

        // Score types to display in order
        GameScoreGoal[] scoreTypes = {
            GameScoreGoal.BEACONS,
            GameScoreGoal.LINKS,
            GameScoreGoal.TRIANGLES,
            GameScoreGoal.AREA
        };

        for (Team team : game.getScorecard().getScoreboard().getTeams()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(Lang.scoreTeam,
                Placeholder.component("team", team.displayName())));

            for (GameScoreGoal scoreType : scoreTypes) {
                int score = game.getScorecard().getScore(team, scoreType);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(Lang.scoreGame,
                    Placeholder.component("score", Component.text(score)),
                    Placeholder.component("unit", Component.text(scoreType.getName()))));
            }
        }
    }
}

