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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;

public class CmdHandler extends BeaconzPluginDependent implements CommandExecutor, TabCompleter {

    public CmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Lang.errorOnlyPlayers);
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("beaconz.player")) {
            sender.sendMessage(ChatColor.RED + Lang.errorYouDoNotHavePermission);
            return true;
        }
        switch (args.length) {
        // Just the beaconz command
        case 0:
            player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
            if (getGameMgr().getLobby() == null) {
                player.sendMessage(ChatColor.RED + "Hmm, there is no lobby yet...");
                return true;
            }
            getGameMgr().getLobby().tpToRegionSpawn(player);
            break;

            // One argument after the beaconz command
        case 1:
            switch (args[0].toLowerCase()) {
            case "help":
                sender.sendMessage("/" + label + " help " + Lang.helpHelp);
                //sender.sendMessage("/" + label + " join <game> " + Lang.helpJoin);
                //sender.sendMessage("/" + label + " leave <game> " + Lang.helpLeave);
                //sender.sendMessage("/" + label + " lobby " + Lang.helpLobby);
                //sender.sendMessage("/" + label + " location " + Lang.helpLocation);
                //sender.sendMessage("/" + label + " score " + Lang.helpScore);
                sender.sendMessage("/" + label + " sb " + Lang.helpScoreboard);
                break;
            /*
            case "lobby":
                player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
                getGameMgr().getLobby().tpToRegionSpawn(player);
                break;
            case "location":
                if (getGameMgr().isPlayerInLobby(player)) {
                    sender.sendMessage(ChatColor.AQUA + Lang.cmdLocation);
                    sender.sendMessage(ChatColor.AQUA + getGameMgr().getLobby().displayCoords());
                } else {
                    Game game = getGameMgr().getGame(player.getLocation());
                    if (game != null) {
                        sender.sendMessage(ChatColor.AQUA + Lang.cmdYourePlaying.replace("[game]", game.getName()));
                        Scorecard sc = game.getScorecard();
                        if (sc != null && sc.getTeam(player) != null) {
                            sender.sendMessage(ChatColor.AQUA + Lang.youAreInTeam.replace("[team]", sc.getTeam(player).getDisplayName()));
                        } else {
                            sender.sendMessage(ChatColor.AQUA + Lang.errorYouMustBeInATeam);
                        }
                        sender.sendMessage(ChatColor.AQUA + game.getRegion().displayCoords());
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "You're in the Beaconz world, but not participating in a game.");
                        sender.sendMessage(ChatColor.YELLOW + "Return to the lobby with /beacons lobby");
                    }
                }
                break;
            case "score":
                if (sender.isOp()) {
                    int gamecnt = 0;
                    for (Game game : getGameMgr().getGames().values()) {
                        sender.sendMessage(ChatColor.GREEN + "Game: " + game.getName());
                        showGameScores(sender, game);
                        gamecnt++;
                    }
                    if (gamecnt ==0 ) {
                        sender.sendMessage(ChatColor.GREEN + "No games are currently defined.");
                    }
                } else {
                    Game game = getGameMgr().getGame(player.getLocation());
                    if (game == null || game.getScorecard() == null || game.getScorecard().getTeam(player) == null) {
                        sender.sendMessage(ChatColor.GREEN + Lang.errorYouMustBeInAGame);
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Game: " + game.getName());
                        Team team = game.getScorecard().getTeam(player);
                        if (team != null){
                            sender.sendMessage(ChatColor.GREEN + Lang.youAreInTeam.replace("[team]", team.getDisplayName()));
                        } else {
                            sender.sendMessage(ChatColor.GREEN + Lang.errorYouMustBeInAGame);
                        }
                        showGameScores(sender, game);
                    }
                }

                break;
                */
            case "sb":
                if (player.getScoreboard().getEntries().isEmpty()) {
                    Game game = getGameMgr().getGame(player.getLocation());
                    if (game != null) {
                        player.setScoreboard(game.getScorecard().getScoreboard());
                    } else {
                        player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
                    }
                } else {
                    player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
                }
                break;
            default:
                break;
            }
            break;
            /*
        case 2:
            Game game;
            switch (args[0].toLowerCase()) {
            case "join":
                String gamename = args[1];
                game = getGameMgr().getGame(gamename);
                if (game == null) {
                    sender.sendMessage(ChatColor.AQUA + Lang.errorNoSuchGame + " '" + gamename + "'");
                } else {
                    game.join(player);
                }
                break;
            case "leave":
                game = getGameMgr().getGame(player.getLocation());
                if (game == null) {
                    sender.sendMessage(ChatColor.AQUA + Lang.errorYouMustBeInAGame);
                } else {
                    game.leave(player);
                }
                break;
            default:
                break;
            }
            break;
            */
        default:
            sender.sendMessage(ChatColor.RED + Lang.errorUnknownCommand);
            return false;
        }
        return true;
    }

    /**
     * Displays the scores for a game
     */
    public void showGameScores(CommandSender sender, Game game) {
        sender.sendMessage(ChatColor.AQUA + "Game mode: " + game.getGamemode());
        sender.sendMessage(ChatColor.AQUA + "Game time: " + game.getScorecard().getDisplayTime("long"));
        sender.sendMessage(ChatColor.AQUA + "Scores:");
        for (Team t : game.getScorecard().getScoreboard().getTeams()) {
            sender.sendMessage(ChatColor.AQUA + "  " + t.getDisplayName() + ChatColor.AQUA + ": " + game.getScorecard().getScore(t, "beacons") + " beacons");
            sender.sendMessage(ChatColor.AQUA + "  " + t.getDisplayName() + ChatColor.AQUA + ": " + game.getScorecard().getScore(t, "links") + " links");
            sender.sendMessage(ChatColor.AQUA + "  " + t.getDisplayName() + ChatColor.AQUA + ": " + game.getScorecard().getScore(t, "triangles") + " triangles");
            sender.sendMessage(ChatColor.AQUA + "  " + t.getDisplayName() + ChatColor.AQUA + ": " + game.getScorecard().getScore(t, "area") + " total area");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
            String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<String>();
        }
        final List<String> options = new ArrayList<String>();
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");

        switch (args.length) {
        case 0:
        case 1:
            options.add("help");
            options.add("join");
            options.add("leave");
            options.add("lobby");
            options.add("location");
            options.add("score");
            options.add("scoreboard");
            break;
        case 2:
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("leave")) {
                // List all the games
                options.addAll(getGameMgr().getGames().keySet());
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
        final List<String> returned = new ArrayList<String>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                returned.add(s);
            }
        }

        return returned;
    }
}