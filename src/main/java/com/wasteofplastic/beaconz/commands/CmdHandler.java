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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Scorecard;

public class CmdHandler extends BeaconzPluginDependent implements CommandExecutor {

    public CmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only available to players");
            return true;
        }
        Player player = (Player)sender;

        switch (args.length) {
        // Just the beaconz command
        case 0:
            player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
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
                sender.sendMessage("/" + label + " help - this help");
                sender.sendMessage("/" + label + " join <game> - join an ongoing game");
                sender.sendMessage("/" + label + " leave <game> - leave a game");
                sender.sendMessage("/" + label + " lobby - go the lobby area");
                sender.sendMessage("/" + label + " location - tells you where you are");
                sender.sendMessage("/" + label + " score - show the team scores");
                sender.sendMessage("/" + label + " scoreboard - toggles the scoreboard on and off");
                break;
            case "lobby":
                player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
                getGameMgr().getLobby().tpToRegionSpawn(player);
                break;
            case "location":
                if (getGameMgr().isPlayerInLobby(player)) {
                    sender.sendMessage(ChatColor.AQUA + "You're in the Beaconz Lobby at");
                    sender.sendMessage(ChatColor.AQUA + getGameMgr().getLobby().displayCoords());
                } else {
                    Game game = getGameMgr().getGame(player.getLocation());
                    if (game != null) {
                        sender.sendMessage(ChatColor.AQUA + "You're playing Beaconz game " + game.getName());
                        Scorecard sc = game.getScorecard();
                        if (sc != null && sc.getTeam(player) != null) {
                            sender.sendMessage(ChatColor.AQUA + "You're in the " + sc.getTeam(player).getDisplayName() + " team.");
                        } else {
                            sender.sendMessage(ChatColor.AQUA + "You need to join a team to play in this game.");
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
                        sender.sendMessage(ChatColor.GREEN + "You need to join a game in order to see the scores");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Game: " + game.getName());
                        Team team = game.getScorecard().getTeam(player);
                        if (team != null){
                            sender.sendMessage(ChatColor.GREEN + "You're in the " + team.getDisplayName() + " team");
                        } else {
                            sender.sendMessage(ChatColor.GREEN + "You still need to join a team");
                        }
                        showGameScores(sender, game);	        						        	    		
                    }
                }

                break;
            case "scoreboard":
                if (player.getScoreboard().getEntries().isEmpty()) {
                    Game game = getGameMgr().getGame(player.getLocation());
                    if (game != null) {
                        player.setScoreboard(game.getScorecard().getScoreboard()); 
                    } else {
                        player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
                    }	        			
                } else {
                    player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());	
                }
                break;
            default:
                break;
            }
            break;                	        	
        case 2: 
            Game game;
            switch (args[0].toLowerCase()) {
            case "join":
                String gamename = args[1];
                game = getGameMgr().getGame(gamename);
                if (game == null) {
                    sender.sendMessage(ChatColor.AQUA + "Could not find a game called " + gamename);
                } else {
                    game.join(player);
                }
                break;
            case "leave":
                game = getGameMgr().getGame(player.getLocation());        			
                if (game == null) {
                    sender.sendMessage(ChatColor.AQUA + "You are not currently in a game.");
                } else {
                    game.leave(player);
                }
                break;        			
            default:
                break;
            }
            break;
        default:
            sender.sendMessage(ChatColor.RED + "Error - unknown command. Do /" + label + " help");
            break;
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

}
