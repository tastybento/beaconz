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

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class AdminCmdHandler extends BeaconzPluginDependent implements CommandExecutor {

    public AdminCmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @SuppressWarnings("deprecation")
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: Make this a permission
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You must be Op to use this command");
            return true;
        }
    	Team team = null;
    	Game game = null;
    	Player player = null;
    	
    	if (args.length == 0 || (args.length == 1 && args[0].toLowerCase().equals("help"))) {
            sender.sendMessage(ChatColor.GREEN + "Beaconz Admin Command");
            sender.sendMessage("/" + label + " claim [unowned | <team>] - force-claims a beacon in a game");
            sender.sendMessage("/" + label + " distribution <fraction between 0 and 1> - sets the global beacon distribution temporarily");
            sender.sendMessage("/" + label + " join <gamename> <team> - join a team in an active game");
            sender.sendMessage("/" + label + " games - list existing games");
            sender.sendMessage("/" + label + " kick <playername> <gamename>- kicks a player from the game");
    		sender.sendMessage("/" + label + " restart <gamename>- clears scoreboard, restarts timer; teams aren't changed");
    		sender.sendMessage("/" + label + " reset <gamename>- resets a game - score, teams, and beacons!");
    		sender.sendMessage("/" + label + " pause <gamename>- pauses the timer and scoreboard in a game");
    		sender.sendMessage("/" + label + " resume <gamename>- restarts a paused game");
    		sender.sendMessage("/" + label + " force_end <gamename>- forces a game to end immediately");
            sender.sendMessage("/" + label + " link <x> <z> - force-links a beacon you are standing on to one at x,z");
            sender.sendMessage("/" + label + " list [all |<gamename>] - lists all known beacons in the game | all games");
            sender.sendMessage("/" + label + " newgame <gamename>- creates a new game in an empty region");
            sender.sendMessage("/" + label + " reload - reloads the plugin, preserving existing games");
            sender.sendMessage("/" + label + " setspawn <team> - sets the spawn point for team");
            sender.sendMessage("/" + label + " teams [all | <gamename>] - shows teams and team members for a game");   
            sender.sendMessage("/" + label + " timertoggle [all | <gamename>] - toggles the scoreboard timer on and off");
            
    	} else {
    		 switch (args[0].toLowerCase()) {
	    		 case "claim":
	    			 if (args.length < 2) {
	    				 sender.sendMessage(ChatColor.RED + "/" + label + " claim [unowned | <team>]");
	    			 } else {	    				
	    				 if (!(sender instanceof Player)) {
	    					 sender.sendMessage("Only players can claim beacons");
	    				 } else {	    					 
	    					// Check the argument
	    					 player = (Player) sender;
	    					 game = getGameMgr().getGame(player.getLocation());
    						 team = game.getScorecard().getTeam(args[1]);
	    					 if (team == null && !args[1].equalsIgnoreCase("unowned")) {
	    						 sender.sendMessage(ChatColor.RED + "/" + label + " claim [unowned, " + game.getScorecard().getTeamListString() + "]");
	    					 } else {
	    						// See if the player is on a beacon
	    						 Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
	    						 if (!getRegister().isBeacon(block)) {
	    							 sender.sendMessage("You have to be standing on a beacon");
	    						 } else {
	    							 Point2D newClaim = new Point2D.Double(block.getX(), block.getZ());
	    							 player.sendMessage("Claiming beacon at " + newClaim);
	    							 if (!getRegister().getBeaconRegister().containsKey(newClaim)) {
	    								 player.sendMessage(ChatColor.RED + "Error: block isBeacon() but is not in the Register: " + newClaim);
	    							 } else {	    								 
	    								 BeaconObj beacon = getRegister().getBeaconRegister().get(newClaim);
	    								 if (args[1].equalsIgnoreCase("unowned")) {
	    									 getRegister().removeBeaconOwnership(beacon);
	    								 } else {
	    									 // Claim a beacon
	    									 //getRegister().getBeaconRegister().get(newClaim).setOwnership(team);
	    									 getRegister().setBeaconOwner(beacon, team);		                    	
	    									 block.setType(game.getScorecard().getBlockID(team).getItemType());
	    									 block.setData(game.getScorecard().getBlockID(team).getData());		                    		
	    								 }
	    								 player.sendMessage("Beacon claimed for team " + args[1]);
	    							 }     							 
	    						 }
	    					 }	    					 
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "distribution":
	    			 if (args.length < 2) {
			                sender.sendMessage(ChatColor.RED + "/" + label + " distribution <fraction between 0 and 1> - sets the distribution of beacons temporarily");
	    			 } else {
	    				 try {
	    					 double dist = Double.valueOf(args[1]);
	    					 if (dist > 0D && dist < 1D) {
	    						 Settings.distribution = dist;
	    						 sender.sendMessage(ChatColor.GREEN + "Setting beacon distribution to " + dist);
	    						 return true;
	    					 }
	    				 } catch (Exception e) {
	    					 sender.sendMessage(ChatColor.RED + label + " distribution <fraction> - must be less than 1");
	    				 }	    				 
	    			 }
	    			 break;
	    			 
	    		 case "join":
	    			 if (args.length < 3) {
			                sender.sendMessage(ChatColor.RED + "/" + label + " join <gamename> <team> - join a team in an active game");
	    			 } else {
	    				 if (!(sender instanceof Player)) {
			                    sender.sendMessage("Only available to players");			                 
			                } else {
		                		player = (Player) sender;
		                		team = null;
		                		if (getGameMgr().getGame(args[1]) != null) {
		                			game = getGameMgr().getGame(args[1]);
		                			if (game.getScorecard().getTeam(args[2]) != null) {
		                				team = game.getScorecard().getTeam(args[2]);
		                				game.getScorecard().addTeamPlayer(team, player);
		                				player.setScoreboard(game.getScorecard().getScoreboard());
		                				game.getScorecard().sendPlayersHome(player, false);
		                				sender.sendMessage(ChatColor.GREEN + "You joined " + team.getDisplayName());
		                			} else {
		                				sender.sendMessage(ChatColor.RED + "/" + label + " join " + args[1] + " + one of [" + game.getScorecard().getTeamListString() + "]");	
		                			}
		                		} else {
		                			sender.sendMessage("/" + label + " join <gamename> <team> - please use a valid game name");
		                		}
			                }	    				 
	    			 }
	    			 break;
	    			 
	    		 case "games":
	    			 sender.sendMessage(ChatColor.GREEN + "The following games/regions are defined:");
	    			 sender.sendMessage(ChatColor.AQUA + "The LOBBY - " + getGameMgr().getLobby().displayCoords());
	    			 int cnt = 0;
	    			 for (Game g : getGameMgr().getGames().values()) {
	    				 cnt ++;
	    				 sender.sendMessage(ChatColor.AQUA + g.getName() + " - " + g.getRegion().displayCoords());
	    			 }
	    			 if (cnt == 0) sender.sendMessage(ChatColor.AQUA + "...and no others.");
	    			 break;
	    			 
	    		 case "kick":
	    			 if (args.length < 3) {
			        		sender.sendMessage(ChatColor.RED + "/" + label + " kick <playername> <gamename>- kicks a player from the game");
	    			 } else {
	    				 player = Bukkit.getPlayer(args[1]);
	    				 if (player == null && args[1] != "all") {
	    					 sender.sendMessage("Could not find that player");
	    				 } else {
	    					 game = getGameMgr().getGame(args[2]);
	    					 if (game == null) {
	    						 sender.sendMessage("Could not find that game");	       							
	    					 } else {
	    						 if (args[2].equals("all")) {
	    							 game.kick();
	    							 sender.sendMessage("All players were kicked from game " + args[2]);	
	    						 } else {
	    							 game.kick(player);
	    							 sender.sendMessage(args[1] + " was kicked from game " + args[2]);	       								
	    						 }
	    					 }
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "restart":
	    			 if (args.length < 2) {
			        		sender.sendMessage(ChatColor.RED + "/" + label + " restart <gamename>- clears the scoreboard and restarts the timer; the teams aren't changed");
	    			 } else {
	    				 game = getGameMgr().getGames().get(args[1]);
	    				 if (game == null) {
	    					 sender.sendMessage("Could not find game " + args[1]);
	    				 } else {    		                			               
	    					 game.restart();
	    					 sender.sendMessage(ChatColor.GREEN + "Restarted game " + args[1] + ".");
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "reset":
	    			 if (args.length < 2) {
			        		sender.sendMessage(ChatColor.RED + "/" + label + " reset <gamename>- resets a game - score, teams, and beacons!");
	    			 } else {
	    				 game = getGameMgr().getGames().get(args[1]);
	    				 if (game == null) {
	    					 sender.sendMessage("Could not find game " + args[1]);
	    				 } else {
	    					 sender.sendMessage(ChatColor.GREEN + "Resetting game " + args[1] + ". This may take several minutes. Please wait for the 'reset complete' message.");			                
	    					 game.reset(sender);
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "pause":
	    			 if (args.length < 2) {
			        		sender.sendMessage(ChatColor.RED + "/" + label + " pause <gamename>- pauses the timer and scoreboard in a game");
	    			 } else {
	    				 game = getGameMgr().getGames().get(args[1]);
	    				 if (game == null) {
	    					 sender.sendMessage("Could not find game " + args[1]);
	    				 } else {    		                			               
	    					 game.pause();
	    					 sender.sendMessage(ChatColor.GREEN + "Paused the game " + args[1] + ". To restart, use " +  "/" + label + " resume <game>");
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "resume":
	    			 if (args.length < 2) {
			        		sender.sendMessage(ChatColor.RED + "/" + label + " resume <gamename>- restarts a paused game");
	    			 } else {
	    				 game = getGameMgr().getGames().get(args[1]);
	    				 if (game == null) {
	    					 sender.sendMessage("Could not find game " + args[1]);
	    				 } else {    		                			               
	    					 game.resume();
	    					 sender.sendMessage(ChatColor.GREEN + "Game " + args[1] + " is back ON!!");
	    				 }	    				 
	    			 }
	    			 break;
	    			 
	    		 case "force_end":
	    			 if (args.length < 2) {
	    				 sender.sendMessage(ChatColor.RED + "/" + label + " force_end <gamename>- forces a game to end immediately");
	    			 } else {	    				 
	    				 game = getGameMgr().getGames().get(args[1]);
	    				 if (game == null) {
	    					 sender.sendMessage("Could not find game " + args[1]);
	    				 } else {    		                			               
	    					 game.forceEnd();
	    					 sender.sendMessage(ChatColor.GREEN + "Game " + args[1] + " has been forcefully ended.");
	    					 sender.sendMessage(ChatColor.GREEN + "To restart the game, use " +  "/" + label + " game restart <gamename>");
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "link":
	    			 if (args.length < 3) {
			                sender.sendMessage(ChatColor.RED + "/" + label + " link <x> <z> - force-links a beacon you are standing on to one at x,z");
	    			 } else {
	    				 if (!(sender instanceof Player)) {
	    					 sender.sendMessage("Only available to players");			                 
	    				 } else {
	    					 player = (Player) sender;
	    					 BeaconObj origin = getRegister().getBeaconAt(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
	    					 if (origin == null) {
	    						 player.sendMessage(ChatColor.RED + "You are not standing on a beacon");
	    					 } else {
	    						 BeaconObj destination = getRegister().getBeaconAt(Integer.valueOf(args[1]), Integer.valueOf(args[2]));
	    						 if (destination == null) {
	    							 player.sendMessage(ChatColor.RED + "These coordinates do not point at a beacon");
	    						 } else {
	    							 origin.addOutboundLink(destination);
	    						 }
	    					 }
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "list":
	    			 if (args.length < 2) {
	    				 sender.sendMessage(ChatColor.RED + "/" + label + " list [all |<gamename>] - lists all known beacons in the game | all games");
	    			 } else {
	    				 listBeacons(sender, args[1]);
	    			 }
	    			 break;
	    			 
	    		 case "newgame":
	    			 if (args.length < 2) {
	    				 sender.sendMessage(ChatColor.RED + "/" + label + " newgame <gamename>- creates a new game");
	    			 } else {
	    				 game = getGameMgr().getGames().get(args[1]);
	    				 if (game != null) {
	    					 sender.sendMessage("Game " + args[1] + " already exists");
	    				 } else {		
	    					 sender.sendMessage(ChatColor.GREEN + "Building a new game. Please wait for the 'build complete' message.");	
	    					 getGameMgr().newGame(args[1], sender);	
	    					 sender.sendMessage(ChatColor.GREEN + "Game build complete.");
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "reload":
	                getRegister().saveRegister();
	                getGameMgr().saveAllGames();
	                this.getBeaconzPlugin().reloadConfig();
	                this.getBeaconzPlugin().loadConfig();
	        		getGameMgr().reload();
	                getRegister().loadRegister();
	        		sender.sendMessage(ChatColor.RED + "Beaconz plugin reloaded. All existing games were preserved.");
	    			 break;
	    			 
	    		 case "setspawn":
	    			 if (args.length < 2) {
			        	sender.sendMessage(ChatColor.RED + "/" + label + " setspawn <team> - sets the spawn point for team");
	    			 } else {
	    				 // Admin set team spawn
	    				 if (!(sender instanceof Player)) {
	    					 sender.sendMessage(ChatColor.RED + "You cannot execute this command from the console");
	    					 return true;
	    				 }
	    				 // Check team name given exists
	    				 player = (Player) sender;
	    				 game = getGameMgr().getGame(player.getLocation());
	    				 if (game == null) {
	    					 sender.sendMessage("You need to be in the region of an active game");
	    				 } else {
	    					 team = game.getScorecard().getTeam(args[1]);
	    					 if (team == null) {
	    						 sender.sendMessage(ChatColor.RED + "That team does not exist! Use " + game.getScorecard().getTeamListString());
	    						 return true;
	    					 }
	    					 game.getScorecard().setTeamSpawnPoint(team, (player.getLocation()));
	    					 sender.sendMessage(ChatColor.GREEN + "Setting " + team.getDisplayName() + "'s spawn point to your location!");		                	
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "teams":
	    			 if (args.length < 2) {
			        	sender.sendMessage(ChatColor.RED + "/" + label + " teams [all | <gamename>] - shows teams and team members for a game");
	    			 } else {
	    				 Boolean foundgame = false;
	    				 for (String gname : getGameMgr().getGames().keySet()) {
	    					 if (args[1].toLowerCase().equals("all") || gname.equals(args[1])) {
	    						 foundgame = true;
	    						 game = getGameMgr().getGames().get(gname);    	    	        		
	    						 sender.sendMessage(ChatColor.GREEN + "Teams in game " + gname);
	    						 Scoreboard sb = game.getScorecard().getScoreboard();
	    						 if (sb == null) {
	    							 sender.sendMessage("Could not find the scoreboard for game " + args[1]);    	        				
	    						 } else {    	    	        				
	    							 HashMap<Team, List<String>> teamMembers = game.getScorecard().getTeamMembers();	    	        					
	    							 for (Team t : teamMembers.keySet()) {
	    								 sender.sendMessage("==== Team " + t.getDisplayName() + " ====");
	    								 String memberlist = "";
	    								 for (String uuid : teamMembers.get(t)) {
	    									 memberlist = memberlist + "[" + Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuid)).getName() + "] ";
	    								 }
	    								 sender.sendMessage(ChatColor.WHITE + "Members: " + memberlist);   	        							    	    	        					
	    							 }
	    						 }    	    		                  	        				
	    					 } 
	    				 }
	    				 if (!foundgame) {
	    					 if (args[1].toLowerCase().equals("all")) {
	    						 sender.sendMessage("Could not find any games.");
	    					 } else {
	    						 sender.sendMessage("Could not find game " + args[1]);	
	    					 }	
	    				 }
	    			 }
	    			 break;
	    			 
	    		 case "timertoggle":
	    			 if (args.length < 2) {
	    				 sender.sendMessage(ChatColor.RED + "/" + label + " timertoggle [all | <gamename>] - toggles the scoreboard timer on and off for a given game");
	    			 } else {
	    				 if (args[1].toLowerCase().equals("all")) {
	    					 Settings.showTimer = ! Settings.showTimer;
	    					 for (Game g : getGameMgr().getGames().values()) {
	    						 g.getScorecard().toggleTimer();
	    					 }		        				
	    				 } else {
	    					 game = getGameMgr().getGames().get(args[1]);
	    					 if (game == null) {
	    						 sender.sendMessage("Could not find game " + args[1]);
	    					 } else {
	    						 game.getScorecard().toggleTimer();
	    					 }
	    				 }	    				 
	    			 }
	    			 break;
	    			 
	        	default:
	        		break;  
    		 }
    		
    	}    	

        return true;
    }

	public void listBeacons(CommandSender sender, String name) {
		int count = 0;
		int gamecnt = 0;
		for (Game game : getGameMgr().getGames().values()) {
			String gameName = game.getName();
			if (name.toLowerCase().equals("all") || gameName.toLowerCase().equals(name.toLowerCase())) {
				gamecnt++;
				count = 0;
				sender.sendMessage(ChatColor.GREEN + "Known beacons in game " + gameName + ":");
                for (BeaconObj b : getRegister().getBeaconRegister().values()) {
                	if (game.getRegion().containsBeacon(b)) {
                        count++;
                        sender.sendMessage(b.getLocation().getX() + ":" + b.getLocation().getY() + " >> Owner: " + (b.getOwnership() == null ? "unowned" :b.getOwnership().getDisplayName()) + " >> Links: " + b.getLinks().size());                		
                	}
                }
                if (count == 0) sender.sendMessage("None");
			}
		}
		if (gamecnt == 0) {
			if (name.equals("all")) {
				sender.sendMessage("Could not find any games");
			} else {
				sender.sendMessage("Could not find game " + name);
			}			
		}
	}
				
}
