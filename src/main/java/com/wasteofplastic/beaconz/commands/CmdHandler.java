/*
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

package com.wasteofplastic.beaconz.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;
import com.wasteofplastic.beaconz.game.Game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class CmdHandler extends BeaconzPluginDependent implements CommandExecutor, TabCompleter {

    public CmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.errorOnlyPlayers);
            return true;
        }
        if (!player.hasPermission("beaconz.player")) {
            sender.sendMessage(Lang.errorYouDoNotHavePermission.color(NamedTextColor.RED));
            return true;
        }
        switch (args.length) {
        // Just the beaconz command
        case 0:
            player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
            if (getGameMgr().getLobby() == null) {
                player.sendMessage(Lang.errorNoLobbyYet.color(NamedTextColor.RED));
                return true;
            }
            getGameMgr().getLobby().tpToRegionSpawn(player, false);
            break;

            // One argument after the beaconz command
        case 1:
            switch (args[0].toLowerCase()) {
            case "help":
                sender.sendMessage(Component.text("/" + label + " help ").append(Lang.helpHelp));
                if (player.hasPermission("beaconz.player.leave")) {
                    sender.sendMessage(Component.text("/" + label + " leave <game> ").append(Lang.helpLeave));
                }
                sender.sendMessage(Component.text("/" + label + " score " ).append(Lang.helpScore));
                sender.sendMessage(Component.text("/" + label + " sb ").append( Lang.helpScoreboard));
                break;
            case "score":
                Game game = getGameMgr().getGame(player.getLocation());
                if (game == null || game.getScorecard() == null || game.getScorecard().getTeam(player) == null) {
                    sender.sendMessage(Lang.errorYouMustBeInAGame.color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Lang.generalGame.append(Component.text(": ")).color(NamedTextColor.GREEN)
                        .append(game.getName().color(NamedTextColor.YELLOW)));
                    Team team = game.getScorecard().getTeam(player);
                    if (team != null){
                        sender.sendMessage(Lang.actionsYouAreInTeam
                                .replaceText(builder -> builder.matchLiteral("[team]").replacement(team.displayName()))
                                .color(NamedTextColor.GREEN));
                    }
                    showGameScores(sender, game);
                }
                break;
            case "sb":
                if (player.getScoreboard().getEntries().isEmpty()) {
                    game = getGameMgr().getGame(player.getLocation());
                    if (game != null) {
                        player.setScoreboard(game.getScorecard().getScoreboard());
                    } else {
                        player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
                    }
                } else {
                    player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
                }
                break;
            case "leave":
                if (player.hasPermission("beaconz.player.leave")) {
                    sender.sendMessage(Component.text("/" + label + " leave <game> " + Lang.helpLeave));
                } else {
                    player.sendMessage(Lang.errorYouDoNotHavePermission.color(NamedTextColor.RED));
                }
                break;
            default:
                break;
            }
            break;
        case 2:           
            switch (args[0].toLowerCase()) {
            case "join":
                onJoin(sender, player, args);
                break;
            case "leave":
                onLeave(sender, player, args);
                break;
            default:
                break;
            }
            break;
        default:
            sender.sendMessage(Lang.errorUnknownCommand.color(NamedTextColor.RED));
            return false;
        }
        return true;
    }

    private boolean onJoin(CommandSender sender, Player player, String[] args) {
     // beaconz join command (undocumented) so admins can make players join any game
        if (player.isOp()) {
            Component gamename = Component.text(args[1]);
            Game game = getGameMgr().getGame(gamename);
            if (game == null) {
                sender.sendMessage(Lang.errorNoSuchGame.append(Component.text(" '")).append(gamename).append(Component.text("'")).color(NamedTextColor.RED));
                return false;
            } else {
                game.join(player);
                return true;
            }                   
        }    
        return false;
    }

    private boolean onLeave(CommandSender sender, Player player, String[] args) {
        if (player.hasPermission("beaconz.player.leave")) {
            Component gamename = Component.text(args[1]);
            Game game = getGameMgr().getGame(gamename);
            if (game == null) {
                sender.sendMessage(Lang.errorNoSuchGame.append(Component.text(" '")).append(gamename).append(Component.text("'")).color(NamedTextColor.RED));
                return false;
            } else {
                game.leave(player);
                return true;
            }
        } else {
            player.sendMessage(Lang.errorYouDoNotHavePermission.color(NamedTextColor.RED));
        }
        return false;
    }

    /**
     * Displays the scores for a game
     */
    public void showGameScores(CommandSender sender, Game game) {
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
            sender.sendMessage(Lang.scoreTeam.replaceText(builder ->
                builder.matchLiteral("[team]").replacement(team.displayName())));

            for (GameScoreGoal scoreType : scoreTypes) {
                int score = game.getScorecard().getScore(team, scoreType);
                sender.sendMessage(Lang.scoreGame
                        .replaceText(builder -> builder.matchLiteral("[score]")
                                .replacement(Component.text(score)))
                        .replaceText(builder -> builder.matchLiteral("[unit]")
                                .replacement(Component.text(scoreType.getName())))
                        .color(NamedTextColor.AQUA));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
            String alias, String[] args) {
        if (!(sender instanceof Player p)) {
            return new ArrayList<>();
        }
        final List<String> options = new ArrayList<>();
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");

        switch (args.length) {
        case 0:
        case 1:
            options.add("help");
            if (p.hasPermission("beaconz.player.leave")) {
                options.add("leave");
            }
            options.add("score");
            options.add("scoreboard");
            break;
        case 2:
            if (p.hasPermission("beaconz.player.leave") && args[0].equalsIgnoreCase("leave")) {
                // List all the games this player is in
                List<String> inGames = new ArrayList<>();
                for (Game game : getGameMgr().getGames().values()) {
                    if (game.getScorecard().inTeam(p)) {
                        String plainText = PlainTextComponentSerializer.plainText().serialize(game.getName());
                        inGames.add(plainText);
                    }
                }
                options.addAll(inGames);
            }
            break;
        }
        return tabLimit(options, lastArg);
    }

    /**
     * Returns all of the items that begin with the given start,
     * ignoring case.  Intended for tabcompletion.
     *
     * @param list
     * @param start
     * @return List of items that start with the letters
     */
    public static List<String> tabLimit(final List<String> list, final String start) {
        final List<String> returned = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                returned.add(s);
            }
        }

        return returned;
    }
}

