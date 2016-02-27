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

import org.apache.commons.lang.math.NumberUtils;
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
            ChatColor cc1 = ChatColor.GREEN;
            ChatColor cc2 = ChatColor.YELLOW;
            ChatColor cc3 = ChatColor.AQUA;
            sender.sendMessage(cc1 + "======================================================");
            sender.sendMessage(cc2 + "Beaconz Admin Commands");
            sender.sendMessage(cc1 + "======================================================");
            sender.sendMessage(cc1 + "/" + label + cc2 + " claim [unowned | <team>]" + cc3 + " - force-claims a beacon in a game");
            sender.sendMessage(cc1 + "/" + label + cc2 + " distribution <decimal between 0 and 1>" + cc3 + " - sets global beacon distribution temporarily");
            sender.sendMessage(cc1 + "/" + label + cc2 + " join <gamename> <team>" + cc3 + " - join a team in an active game");
            sender.sendMessage(cc1 + "/" + label + cc2 + " games" + cc3 + " - list existing games");
            sender.sendMessage(cc1 + "/" + label + cc2 + " kick <playername> <gamename>" + cc3 + "- kicks a player from the game");
            sender.sendMessage(cc1 + "/" + label + cc2 + " restart <gamename>" + cc3 + " - restarts the game with currently defined parameters - clears scoreboard, cleans out all beacons, restarts timer; teams aren't changed");
            sender.sendMessage(cc1 + "/" + label + cc2 + " reset <gamename>" + cc3 + " - resets score, teams, and repopulates the beacons!");
            sender.sendMessage(cc1 + "/" + label + cc2 + " pause <gamename>" + cc3 + " - pauses the timer and scoreboard in a game");
            sender.sendMessage(cc1 + "/" + label + cc2 + " resume <gamename>" + cc3 + " - resume a paused game");
            sender.sendMessage(cc1 + "/" + label + cc2 + " force_end <gamename>" + cc3 + " - forces a game to end immediately");
            sender.sendMessage(cc1 + "/" + label + cc2 + " link <x> <z>" + cc3 + " - force-links a beacon you are standing on to one at x,z");
            sender.sendMessage(cc1 + "/" + label + cc2 + " list [all |<gamename>]" + cc3 + " - lists all known beacons in the game | all games");
            sender.sendMessage(cc1 + "/" + label + cc2 + " newgame <gamename> [<parm1:value> <parm2:value>...]" + cc3 + " - creates a new game in an empty region; parameters are optional - do /" + label + " newgame help for a list of the possible parameters");           
            sender.sendMessage(cc1 + "/" + label + cc2 + " reload" + cc3 + " - reloads the plugin, preserving existing games");
            sender.sendMessage(cc1 + "/" + label + cc2 + " setgameparms <gamename> <parm1:value> <parm2:value>... " + cc3 + "- defines a game's parameters - DOES NOT restart the game (use restart for that) - do /" + label + " setgameparms help for a list of the possible parameters");
            sender.sendMessage(cc1 + "/" + label + cc2 + " setspawn <team>" + cc3 + " - sets the spawn point for team");
            sender.sendMessage(cc1 + "/" + label + cc2 + " teams [all | <gamename>]" + cc3 + " - shows teams and team members for a game");   
            sender.sendMessage(cc1 + "/" + label + cc2 + " timertoggle [all | <gamename>]" + cc3 + " - toggles the scoreboard timer on and off");

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
                    sender.sendMessage(ChatColor.RED + "/" + label + " kick <playername> <gamename> - kicks a player from the game");
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
                    sender.sendMessage(ChatColor.RED + "/" + label + " restart <gamename> - restarts the game with currently defined parameters - clears scoreboard, cleans out all beacons, restarts the timer; teams aren't changed");
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
                    sender.sendMessage(ChatColor.RED + "/" + label + " reset <gamename> - resets score, teams, and repopulates the beacons!");
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
                    sender.sendMessage(ChatColor.RED + "/" + label + " pause <gamename> - pauses the timer and scoreboard in a game");
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
                    sender.sendMessage(ChatColor.RED + "/" + label + " resume <gamename> - resumes a paused game");
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
                    sender.sendMessage(ChatColor.RED + "/" + label + " force_end <gamename> - forces a game to end immediately");
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
                    sender.sendMessage("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...] - parameters are optional");
                    sender.sendMessage("/" + label + " do /" + label + " newgame help for a list of the possible parameters");
                } else {
                    if (args[1].toLowerCase().equals("help")) {
                        sender.sendMessage("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...]");
                        sender.sendMessage(ChatColor.GREEN  + "The optional parameters and their values are:");
                        sender.sendMessage(ChatColor.YELLOW  + "gamemode -  " + ChatColor.AQUA + " values can be either 'minigame' or 'strategy' - e.g gamemode:strategy");
                        sender.sendMessage(ChatColor.YELLOW  + "teams -  " + ChatColor.AQUA + " the number of teams in the game - e.g. teams:3");
                        sender.sendMessage(ChatColor.YELLOW  + "goal -  " + ChatColor.AQUA + "  one of 'area', 'beacons', 'links', 'triangles' - e.g. goal:links");
                        sender.sendMessage(ChatColor.YELLOW  + "goalvalue -  " + ChatColor.AQUA + "  the number objective for the goal - e.g goalvalue:100 - if it is 0, the winner is the team with the most of 'goal' when the countdown timer runs out.");
                        sender.sendMessage(ChatColor.YELLOW  + "countdown -  " + ChatColor.AQUA + "  the game's timer, in seconds. 0 means the timer runs up, open-ended; any other value meands the timer runs a countdown from that time. - e.g. countdown:600");
                        sender.sendMessage(ChatColor.YELLOW  + "scoretypes -  " + ChatColor.AQUA + "  the scores to be displayed on the sidebar. Can be any combination of goal names separated by '-' e.g scoretypes:area-triangles-beacons-links");
                    } else {
                        String [] parmargs = new String [args.length-2];
                        System.arraycopy(args, 2, parmargs, 0, parmargs.length);
                        game = getGameMgr().getGame(args[1]);
                        if (game != null) {
                            sender.sendMessage("Game " + args[1] + " already exists");
                        } else {
                             //Check the parameters first, if any                                                        
                            if (parmargs.length > 0) {
                                String errormsg = setDefaultParms(parmargs);  // temporarily set up the given parameters as default
                                if (!errormsg.isEmpty()) {
                                    sender.sendMessage(ChatColor.RED + "Error: " + errormsg);                 
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters (just in case)
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "Building a new game with given parameters. Please wait for the 'build complete' message.");
                                    getGameMgr().newGame(args[1], sender);   // create the new game
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters
                                    sender.sendMessage(ChatColor.GREEN + "Game build complete.");
                                }                                
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Building a new game with default parameters. Please wait for the 'build complete' message."); 
                                getGameMgr().newGame(args[1], sender);  
                                sender.sendMessage(ChatColor.GREEN + "Game build complete.");
                            }
                        }
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

            case "setgameparms":
                if (args.length < 2) {
                    sender.sendMessage("/" + label + " setgameparms <gamename> <parm1:value> <parm2:value>");
                    sender.sendMessage("setgameparms defines the game parameters, it DOES NOT restart the game");
                    sender.sendMessage("use /" + label + " setgameparms help for a list of the possible parameters");
                    sender.sendMessage("use /" + label + " restart <game> to restart a game using the new parameters");
                } else {
                    if (args[1].toLowerCase().equals("help")) {
                        sender.sendMessage(ChatColor.RED  + "/" + label + " setgameparms <gamename> <parm1:value> <parm2:value>... ");
                        sender.sendMessage(ChatColor.GREEN  + "The possible parameters and their values are:");
                        sender.sendMessage(ChatColor.YELLOW  + "gamemode -  " + ChatColor.AQUA + " values can be either 'minigame' or 'strategy' - e.g gamemode:strategy");
                        sender.sendMessage(ChatColor.YELLOW  + "teams -  " + ChatColor.AQUA + " the number of teams in the game - e.g. teams:3");
                        sender.sendMessage(ChatColor.YELLOW  + "goal -  " + ChatColor.AQUA + "  one of 'area', 'beacons', 'links', 'triangles' - e.g. goal:links");
                        sender.sendMessage(ChatColor.YELLOW  + "goalvalue -  " + ChatColor.AQUA + "  the number objective for the goal - e.g goalvalue:100 - if it is 0, the winner is the team with the most of 'goal' when the countdown timer runs out.");
                        sender.sendMessage(ChatColor.YELLOW  + "countdown -  " + ChatColor.AQUA + "  the game's timer, in seconds. 0 means the timer runs up, open-ended; any other value meands the timer runs a countdown from that time. - e.g. countdown:600");
                        sender.sendMessage(ChatColor.YELLOW  + "scoretypes -  " + ChatColor.AQUA + "  the scores to be displayed on the sidebar. Can be any combination of goal names separated by '-' e.g scoretypes:area-triangles-beacons-links");
                    } else {
                        String [] parmargs = new String [args.length-2];
                        System.arraycopy(args, 2, parmargs, 0, parmargs.length);
                        game = getGameMgr().getGame(args[1]);
                        if (game == null) {
                            sender.sendMessage("Could not find game " + args[1]);
                        } else {
                            String errormsg = this.setGameParms(game, parmargs);
                            if (!errormsg.isEmpty()) {
                                sender.sendMessage(ChatColor.RED + "Error: " + errormsg);                                
                            } else {
                                sender.sendMessage("Game parameters set.");
                            }
                        }
                    }
                    
                }
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

    /**
     * Lists all beacons for a given game or for 'all' games
     * @param sender
     * @param name - either 'all' or a valid game name
     */
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

    /**
     * setGameParms parses a list of colon-delimited arguments and 
     * sets a game's parameters accordingly
     * Used by both newgame and setparms commands
     * @param game - if game is null, will set GameMgr's default parms; otherwise, will set the game's parms
     * @param arguments
     * @return an error message if a problem was encountered, null otherwise
     */
    public String setGameParms(Game game, String[] args) {
        String errormsg = "";
        // We will be checking gamemode vs timer type ('countdown'), so let's get the current ones for the game
        String gm = game.getGamemode();
        Integer tt = game.getCountdownTimer();
        if (gm == null) gm = Settings.gamemode;
        if (tt == null) {
            tt = gm.toLowerCase().equals("strategy") ? Settings.strategyTimer : Settings.minigameTimer;
        }
        
        // Check the parameters given
        errormsg = checkParms(args, gm, tt);
        
        // If *ALL* arguments are OK, process them into the game
        if (errormsg.isEmpty()) {
            for (int i=0; i < args.length; i++) {
                String parm = args[i].split(":")[0];
                String value = args[i].split(":")[1];
                if (parm == null || value == null) {
                    errormsg = "Arguments must be given in pairs, separated by colons.";
                } else {
                    
                    switch (parm.toLowerCase()) {
                        case "gamemode":
                            game.setGamemode(value);
                            break;
                        case "teams":
                            game.setNbrTeams(Integer.valueOf(value));
                            break;
                        case "goal":
                            game.setGamegoal(value);
                            break;
                        case "goalvalue":
                            game.setGamegoalvalue(Integer.valueOf(value));
                            break;
                        case "countdown":
                            game.setCountdownTimer(tt);
                            break;
                        case "scoretypes":
                            game.setScoretypes(value.replace("-", ":"));
                            break;
                        default:
                            break;
                    }                               
                }                   
            }
        }
        
        // All done.
        return errormsg;           
    }
    
    /**
     * Sets the default parameters to be used in all new games
     * @param args - the array of parameters 
     * @return an error message if a problem was encountered, null otherwise
     */
    public String setDefaultParms(String[] args) {
        String errormsg = "";
        
        // Get default parameters
        String mode = Settings.gamemode;
        int nteams = Settings.default_teams;
        String ggoal = mode.toLowerCase().equals("strategy") ? Settings.strategyGoal: Settings.minigameGoal;
        int gvalue = mode.toLowerCase().equals("strategy") ? Settings.strategyGoalValue : Settings.minigameGoalValue;
        int timer = mode.toLowerCase().equals("strategy") ? Settings.strategyTimer : Settings.minigameTimer;
        String stypes = null; 

        // Check the parameters given
        errormsg = checkParms(args, mode, timer);        
        
        // If all arguments are OK, set them as the new defaults for creating games
        if (errormsg.isEmpty()) {
            for (int i=0; i < args.length; i++) {
                String parm = args[i].split(":")[0];
                String value = args[i].split(":")[1];
                if (parm == null || value == null) {
                    errormsg = "Arguments must be given in pairs, separated by colons.";
                } else {                    
                    switch (parm.toLowerCase()) {
                        case "gamemode":
                            mode = value;
                            break;
                        case "teams":
                            nteams = Integer.valueOf(value);
                            break;
                        case "goal":
                            ggoal = value;
                            break;
                        case "goalvalue":
                            gvalue = Integer.valueOf(value);
                            break;
                        case "countdown":
                            timer = Integer.valueOf(value);
                            break;
                        case "scoretypes":
                            stypes = value.replace("-", ":");
                            break;
                        default:
                            break;
                    }                               
                }                   
            }
        }
        
        // Any of the arguments to setGameDefaultParms CAN be null
        getGameMgr().setGameDefaultParms(mode, nteams, ggoal, gvalue, timer, stypes);
        
        return errormsg;
    }

    /**
     * Checks that an array of arguments contains valid parameters for a game
     * @param args - the array of parameters
     * @param gamemode - the game's current gamemode, or the default in Settings
     * @param timertype - the game's current timer type, or the default in Settings
     * @return an error message if a problem was encountered, null otherwise
     */
    public String checkParms(String[] args, String gamemode, int timer) {
        String errormsg = "";

        // Check that *ALL* arguments are valid parms
        for (int i=0; i < args.length; i++) {
            String parm = null;
            String value = null;
            try {
                parm = args[i].split(":")[0];
                value = args[i].split(":")[1];                
            } catch (Exception e) {}   
            if (parm == null || value == null) {
                errormsg = "Arguments must be given in param:value pairs.";
            } else {
                switch (parm.toLowerCase()) {
                    case "gamemode":
                        if (!value.equals("strategy") && !value.equals("minigame")) {
                            errormsg = errormsg + "<< 'gamemode:' has to be either 'strategy' or 'minigame' >>";
                        } else {
                            gamemode = value;
                        }
                        break;
                    case "teams":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = "<< 'team:' value must be a number >>";
                        }
                        break;
                    case "goal":
                        if (!value.equals("area") && !value.equals("beacons") &&
                                !value.equals("links") && !value.equals("triangles")) {
                            errormsg = "<< 'goal:' has to be one of 'area', 'beacons', 'links' or 'triangles' >>";
                        }
                        break;
                    case "goalvalue":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = "<< 'goalvalue:' value must be a number >>";
                        }
                        break;
                    case "countdown":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = "<< 'countdown:' value must be a number >>";
                        } else {
                            timer = Integer.valueOf(value);
                        }
                        break;
                    case "scoretypes":
                        String stmsg = "<< 'scoretypes:' must be a list of the goals to display on the scoreboard, such as 'area-beacons'. Possible goals are 'area', 'beacons', 'links' and 'triangles' >>";
                        value = value.replace(" ", "");
                        String[] stypes = value.split("-");
                        if (stypes.length == 0) {
                            errormsg = stmsg;
                        } else {
                            for (int st = 0; st < stypes.length; st++) {
                                if (!stypes[st].equals("area") && !stypes[st].equals("beacons") &&
                                        !stypes[st].equals("links") && !stypes[st].equals("triangles")) {
                                    errormsg = stmsg;
                                }
                            }							
                        }
                        break;
                    default:
                        errormsg = "Parameter " + parm + " does not exist.";
                        break;
               }				
            }			
        }

        // Then we check gamemode vs timer type
        if (gamemode.equals("strategy")) {
            timer = 0;            
        } else {
            if (timer < 1) errormsg = "For a minigame, the countdown timer must by > 0. Please change either the gamemode or the countdown timer.";
        }
        
        // All done
        return errormsg;
    }

}
