/*
 * Copyright (c) 2015 - 2016 tastybento
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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Settings;

public class AdminCmdHandler extends BeaconzPluginDependent implements CommandExecutor, TabCompleter {

    public AdminCmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!player.isOp() && !player.hasPermission("beaconz.admin")) {
                senderMsg(sender, ChatColor.RED + Lang.errorYouDoNotHavePermission);
                return true;
            }
        }
        Team team = null;
        Game game = null;
        Player player = null;

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            ChatColor cc1 = ChatColor.GREEN;
            ChatColor cc2 = ChatColor.YELLOW;
            ChatColor cc3 = ChatColor.AQUA;
            senderMsg(sender, cc1 + Lang.helpLine);
            senderMsg(sender, cc2 + Lang.helpAdminTitle);
            senderMsg(sender, cc1 + Lang.helpLine);
            if (sender instanceof Player) {
                senderMsg(sender, cc1 + "/" + label + cc2 + " claim [unowned | <team>]" + cc3 + Lang.helpAdminClaim);
            }
            senderMsg(sender, cc1 + "/" + label + cc2 + " delete <gamename>" + cc3 + Lang.helpAdminDelete);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " distribution <decimal between 0 and 1>" + cc3 + Lang.helpAdminDistribution);
            if (sender instanceof Player) {
                senderMsg(sender, cc1 + "/" + label + cc2 + " join <gamename> <team>" + cc3 + Lang.helpAdminJoin);
            }
            senderMsg(sender, cc1 + "/" + label + cc2 + " games" + cc3 + Lang.helpAdminGames);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " kick <online playername> <gamename>" + cc3 + Lang.helpAdminKick);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " restart <gamename>" + cc3 + Lang.helpAdminRestart);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " regenerate <gamename>" + cc3 + Lang.helpAdminRegenerate);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " pause <gamename>" + cc3 + Lang.helpAdminPause);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " resume <gamename>" + cc3 + Lang.helpAdminResume);
            senderMsg(sender, cc1 + "/" + label + cc2 + " force_end <gamename>" + cc3 + Lang.helpAdminForceEnd);
            /*
            if (sender instanceof Player) {
                senderMsg(sender, cc1 + "/" + label + cc2 + " link <x> <z>" + cc3 + Lang.helpAdminLink);
            }*/
            senderMsg(sender, cc1 + "/" + label + cc2 + " list [all |<gamename>] [team]" + cc3 + Lang.helpAdminList);
            senderMsg(sender, cc1 + "/" + label + cc2 + " listparms <gamename>" + cc3 + Lang.helpAdminListParms);
            senderMsg(sender, cc1 + "/" + label + cc2 + " newgame <gamename> [<parm1:value> <parm2:value>...]" + cc3 + Lang.helpAdminNewGame.replace("[label]", label));
            senderMsg(sender, cc1 + "/" + label + cc2 + " reload" + cc3 + Lang.helpAdminReload);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " setgameparms <gamename> <parm1:value> <parm2:value>... " + cc3 + Lang.helpAdminSetGameParms.replace("[label]", label));
            if (sender instanceof Player) {
                senderMsg(sender, cc1 + "/" + label + cc2 + " setspawn <team>" + cc3 + Lang.helpAdminSetTeamSpawn);
                senderMsg(sender, cc1 + "/" + label + cc2 + " setspawn " + cc3 + Lang.helpAdminSetLobbySpawn);
                senderMsg(sender, cc1 + "/" + label + cc2 + " switch " + cc3 + Lang.helpAdminSwitch);
            }
            senderMsg(sender, cc1 + "/" + label + cc2 + " switch <online playername> " + cc3 + Lang.helpAdminSwitch);
            senderMsg(sender, cc1 + "/" + label + cc2 + " teams [all | <gamename>]" + cc3 + Lang.helpAdminTeams);
            //senderMsg(sender, cc1 + "/" + label + cc2 + " timertoggle [all | <gamename>]" + cc3 + Lang.helpAdminTimerToggle);

        } else {
            switch (args[0].toLowerCase()) {
            case "claim":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " claim [unowned | <team>]");
                } else {
                    if (!(sender instanceof Player)) {
                        senderMsg(sender, Lang.errorOnlyPlayers);
                    } else {
                        // Check the argument
                        player = (Player) sender;
                        game = getGameMgr().getGame(player.getLocation());
                        team = game.getScorecard().getTeam(args[1]);
                        if (team == null && !args[1].equalsIgnoreCase("unowned")) {
                            senderMsg(sender, ChatColor.RED + "/" + label + " claim [unowned, " + game.getScorecard().getTeamListString() + "]");
                        } else {
                            // See if the player is on a beacon
                            Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                            if (!getRegister().isBeacon(block)) {
                                senderMsg(sender, Lang.errorYouHaveToBeStandingOnABeacon);
                            } else {
                                Point2D newClaim = new Point2D.Double(block.getX(), block.getZ());
                                player.sendMessage(Lang.beaconClaimingBeaconAt.replace("[location]", newClaim.toString()));
                                if (!getRegister().getBeaconRegister().containsKey(newClaim)) {
                                    player.sendMessage(ChatColor.RED + Lang.errorNotInRegister + newClaim);
                                } else {
                                    BeaconObj beacon = getRegister().getBeaconRegister().get(newClaim);
                                    if (args[1].equalsIgnoreCase("unowned")) {
                                        getRegister().removeBeaconOwnership(beacon);
                                    } else {
                                        // Claim a beacon
                                        //getRegister().getBeaconRegister().get(newClaim).setOwnership(team);
                                        getRegister().setBeaconOwner(beacon, team);
                                        block.setType(game.getScorecard().getBlockID(team));
                                        // TODO block.setData(game.getScorecard().getBlockID(team).getData());
                                    }
                                    player.sendMessage(Lang.beaconClaimedForTeam.replace("[team]", args[1]));
                                }
                            }
                        }
                    }
                }
                break;
                
            case "distribution":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " distribution <fraction between 0 and 1> " + Lang.helpAdminDistribution);
                } else {
                    try {
                        double dist = Double.parseDouble(args[1]);
                        if (dist > 0D && dist < 1D) {
                            Settings.distribution = dist;
                            senderMsg(sender, ChatColor.GREEN + Lang.actionsDistributionSettingTo.replace("[value]", String.valueOf(dist)));
                            return true;
                        }
                    } catch (Exception e) {
                        senderMsg(sender, ChatColor.RED + label + " distribution <fraction> - must be less than 1");
                    }
                }
                break;
                 
            case "switch":
                // Switch teams within a game
                if (args.length == 1) {
                    if (!(sender instanceof Player)) {
                        senderMsg(sender, Lang.errorOnlyPlayers);
                    } else {
                        player = (Player) sender;
                        team = getGameMgr().getPlayerTeam(player);
                        if (team == null) {
                            senderMsg(sender, ChatColor.RED + Lang.errorYouMustBeInATeam);
                            return true;
                        }
                        game = getGameMgr().getGame(team);
                        if (game == null) {
                            senderMsg(sender, ChatColor.RED + Lang.errorYouMustBeInAGame);
                            return true;
                        }

                        // Get the next team in this game
                        for (Team newTeam : game.getScorecard().getTeams()) {
                            if (!newTeam.equals(team)) {
                                // Found an alternative
                                game.getScorecard().addTeamPlayer(newTeam, player);
                                senderMsg(sender, ChatColor.GREEN + Lang.actionsSwitchedToTeam.replace("[team]", newTeam.getDisplayName()));
                                // Remove any potion effects
                                for (PotionEffect effect : player.getActivePotionEffects())
                                    player.removePotionEffect(effect.getType());
                                return true;
                            }
                        }
                        senderMsg(sender, ChatColor.RED + Lang.errorNoSuchTeam);
                    }
                    return true;
                } else if (args.length == 2) {
                    player = getServer().getPlayer(args[1]);
                    if (player == null) {
                        senderMsg(sender, Lang.errorUnknownPlayer);
                    } else {
                        team = getGameMgr().getPlayerTeam(player);
                        if (team == null) {
                            senderMsg(sender, ChatColor.RED + Lang.errorNoSuchTeam);
                            return true;
                        }
                        game = getGameMgr().getGame(team);
                        if (game == null) {
                            senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame);
                            return true;
                        }

                        // Get the next team in this game
                        for (Team newTeam : game.getScorecard().getTeams()) {
                            if (!newTeam.equals(team)) {
                                // Found an alternative
                                game.getScorecard().addTeamPlayer(newTeam, player);
                                senderMsg(sender, ChatColor.GREEN + player.getName() + ": " + Lang.actionsSwitchedToTeam.replace("[team]", newTeam.getDisplayName()));
                                player.sendMessage(ChatColor.GREEN + Lang.actionsSwitchedToTeam.replace("[team]", newTeam.getDisplayName()));

                                // Remove any potion effects
                                for (PotionEffect effect : player.getActivePotionEffects())
                                    player.removePotionEffect(effect.getType());
                                return true;
                            }
                        }
                        senderMsg(sender, ChatColor.RED + Lang.errorNoSuchTeam);
                    }
                    return true;
                }
            case "join":
                if (args.length < 3) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " join <gamename> <team>" + Lang.helpAdminJoin);
                } else {
                    if (!(sender instanceof Player)) {
                        senderMsg(sender, Lang.errorOnlyPlayers);
                    } else {
                        player = (Player) sender;
                        team = null;
                        game = getGameMgr().getGame(args[1]);
                        if (game != null) {                            
                            if (game.getScorecard().getTeam(args[2]) != null) {
                                team = game.getScorecard().getTeam(args[2]);
                                game.getScorecard().addTeamPlayer(team, player);
                                player.setScoreboard(game.getScorecard().getScoreboard());
                                game.getScorecard().sendPlayersHome(player, false);
                                senderMsg(sender, ChatColor.GREEN + Lang.actionsYouAreInTeam.replace("[team]", team.getDisplayName()));
                            } 
                        } else {
                            senderMsg(sender, "/" + label + " join <gamename> <team> - " + Lang.errorNoSuchGame);
                        }
                    }
                }
                break;                

            case "games":
                senderMsg(sender, ChatColor.GREEN + Lang.adminGamesDefined);
                senderMsg(sender, ChatColor.AQUA + Lang.adminGamesTheLobby + " - " + getGameMgr().getLobby().displayCoords());
                int cnt = 0;
                for (Game g : getGameMgr().getGames().values()) {
                    cnt ++;
                    senderMsg(sender, ChatColor.AQUA + g.getName() + " - " + g.getRegion().displayCoords());
                }
                if (cnt == 0) senderMsg(sender, ChatColor.AQUA + Lang.adminGamesNoOthers);
                break;
                
            case "kick":
                if (args.length < 3) {
                    senderMsg(sender, ChatColor.RED + "/" + label + Lang.helpAdminKick);
                } else {
                    player = getServer().getPlayer(args[1]);
                    if (player == null && args[1] != "all") {
                        senderMsg(sender, Lang.errorUnknownPlayer);
                    } else {
                        game = getGameMgr().getGame(args[2]);
                        if (game == null) {
                            senderMsg(sender, Lang.errorNoSuchGame);
                        } else {
                            if (args[2].equals("all")) {
                                game.kickAll();
                                senderMsg(sender, Lang.adminKickAllPlayers.replace("[name]", game.getName()));
                            } else {
                                game.kick(sender, player);
                                senderMsg(sender, (Lang.adminKickPlayer.replace("[player]", player.getName()).replace("[name]", game.getName())));
                            }
                        }
                    }
                }
                break;

                /*
            case "restart":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " restart <gamename> - " + Lang.helpAdminRestart);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.restart();
                        senderMsg(sender, ChatColor.GREEN + Lang.adminRestart.replace("[name]", game.getName()));
                    }
                }
                break;
                 */
            case "delete":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " delete <gamename> - " + Lang.helpAdminDelete);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        senderMsg(sender, ChatColor.GREEN + Lang.adminDeletingGame.replace("[name]", game.getName()));
                        getGameMgr().delete(sender, game);
                        senderMsg(sender, ChatColor.GREEN + Lang.adminDeletedGame.replace("[name]", game.getName()));
                    }
                }
                break;

                /*
            case "pause":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " pause <gamename> " + Lang.helpAdminPause);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.pause();
                        senderMsg(sender, ChatColor.GREEN + (Lang.adminPaused.replace("[name]", game.getName()).replace("[label]", label)));
                    }
                }
                break;

            case "resume":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " resume <gamename> " + Lang.helpAdminResume);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.resume();
                        senderMsg(sender, ChatColor.GREEN + Lang.adminResume.replace("[name]", game.getName()));
                    }
                }
                break;
                 */
            case "force_end":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " force_end <gamename>" + Lang.helpAdminForceEnd);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.forceEnd();
                        senderMsg(sender, ChatColor.GREEN + Lang.adminForceEnd.replace("[name]", game.getName()));
                        //senderMsg(sender, ChatColor.GREEN + Lang.adminForceRestart.replace("[label]", label));
                    }
                }
                break;
                /*
            case "link":
                if (args.length < 3) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " link <x> <z> " + Lang.helpAdminLink);
                } else {
                    if (!(sender instanceof Player)) {
                        senderMsg(sender, Lang.errorOnlyPlayers);
                    } else {
                        player = (Player) sender;
                        BeaconObj origin = getRegister().getBeaconAt(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
                        if (origin == null) {
                            player.sendMessage(ChatColor.RED + Lang.errorYouHaveToBeStandingOnABeacon);
                        } else {
                            BeaconObj destination = getRegister().getBeaconAt(Integer.valueOf(args[1]), Integer.valueOf(args[2]));
                            if (destination == null) {
                                player.sendMessage(ChatColor.RED + Lang.errorNoBeaconThere);
                            } else {
                                origin.addOutboundLink(destination);
                            }
                        }
                    }
                }
                break;
                 */
            case "list":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " list [all |<gamename>] [team] " + Lang.helpAdminList);
                } else if (args.length == 3) {
                    listBeacons(sender, args[1], args[2]);
                } else {
                    listBeacons(sender, args[1]);
                }
                break;

            case "newgame":
                if (args.length < 2) {
                    senderMsg(sender, "/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...] - parameters are optional");
                    senderMsg(sender, "/" + label + " do /" + label + " newgame help for a list of the possible parameters");
                } else {
                    if (args[1].equalsIgnoreCase("help")) {
                        senderMsg(sender, "/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...]");
                        senderMsg(sender, ChatColor.GREEN  + "The optional parameters and their values are:");
                        senderMsg(sender, ChatColor.YELLOW  + "gamemode -  " + ChatColor.AQUA + " values can be either 'minigame' or 'strategy' - e.g gamemode:strategy");
                        senderMsg(sender, ChatColor.YELLOW  + "size -  " + ChatColor.AQUA + " length for the side of the game region - e.g. size:500");
                        senderMsg(sender, ChatColor.YELLOW  + "teams -  " + ChatColor.AQUA + " the number of teams in the game - e.g. teams:2");
                        senderMsg(sender, ChatColor.YELLOW  + "goal -  " + ChatColor.AQUA + "  one of 'area', 'beacons', 'links', 'triangles' - e.g. goal:links");
                        senderMsg(sender, ChatColor.YELLOW  + "goalvalue -  " + ChatColor.AQUA + "  the number objective for the goal - e.g goalvalue:100");
                        senderMsg(sender, ChatColor.YELLOW  + "countdown -  " + ChatColor.AQUA + "  the game's timer, in seconds. 0 means the timer runs up, open-ended; any other value meands the timer runs a countdown from that time. - e.g. countdown:600");
                        senderMsg(sender, ChatColor.YELLOW  + "scoretypes -  " + ChatColor.AQUA + "  the scores to be displayed on the sidebar. Can be any combination of goal names separated by '-' e.g scoretypes:area-triangles-beacons-links");
                        senderMsg(sender, ChatColor.YELLOW  + "distribution -  " + ChatColor.AQUA + "  overrides the system's default beacon distribution - specify a number between 0.01 and 0.99 for the probability of any one chunk containing a beacon.");                        
                    } else {
                        String [] parmargs = new String [args.length-2];
                        System.arraycopy(args, 2, parmargs, 0, parmargs.length);
                        game = getGameMgr().getGame(args[1]);
                        if (game != null) {
                            senderMsg(sender, Lang.errorAlreadyExists.replace("[name]", game.getName()));
                        } else {
                            //Check the parameters first, if any
                            if (parmargs.length > 0) {
                                String errormsg = setDefaultParms(parmargs);  // temporarily set up the given parameters as default
                                if (!errormsg.isEmpty()) {
                                    senderMsg(sender, ChatColor.RED + Lang.errorError + errormsg);
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters (just in case)
                                } else {
                                    senderMsg(sender, ChatColor.GREEN + Lang.adminNewGameBuilding);
                                    getGameMgr().newGame(args[1]);           // create the new game
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters
                                    senderMsg(sender, ChatColor.GREEN + Lang.generalSuccess);
                                }
                            } else {
                                senderMsg(sender, ChatColor.GREEN + Lang.adminNewGameBuilding);
                                getGameMgr().newGame(args[1]);
                                senderMsg(sender, ChatColor.GREEN + Lang.generalSuccess);
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
                senderMsg(sender, ChatColor.RED + Lang.adminReload);
                break;

            case "listparms":
                if (args.length < 2) {
                    senderMsg(sender, "/" + label + " listparms <gamename> " + Lang.helpAdminListParms);
                } else {
                    game = getGameMgr().getGame(args[1]);
                    if (game == null) {
                        senderMsg(sender, Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        senderMsg(sender, ChatColor.YELLOW + Lang.adminParmsMode + ": " + ChatColor.AQUA + game.getGamemode());
                        senderMsg(sender, ChatColor.YELLOW + Lang.adminParmsTeams + ": " + ChatColor.AQUA + game.getNbrTeams());
                        senderMsg(sender, ChatColor.YELLOW + Lang.adminParmsGoal + ": " + ChatColor.AQUA  + game.getGamegoal());
                        senderMsg(sender, ChatColor.YELLOW + Lang.adminParmsGoalValue + ": " + ChatColor.AQUA + (game.getGamegoalvalue() == 0 ? Lang.adminParmsUnlimited : String.format(Locale.US, "%,d", game.getGamegoalvalue())));
                        //senderMsg(sender, ChatColor.YELLOW + Lang.adminParmsCountdown + ": " + ChatColor.AQUA + (game.getCountdownTimer() == 0 ? Lang.adminParmsUnlimited : game.getCountdownTimer()));
                        senderMsg(sender, ChatColor.YELLOW + Lang.adminParmsScoreTypes + ": " + ChatColor.AQUA + game.getScoretypes());
                    }
                }
                return true;
                /*
            case "setgameparms":
                if (args.length < 2) {
                    senderMsg(sender, "/" + label + " setgameparms <gamename> <parm1:value> <parm2:value>");
                    senderMsg(sender, "setgameparms defines the game parameters, it DOES NOT restart the game");
                    senderMsg(sender, "use /" + label + " setgameparms help for a list of the possible parameters");
                    senderMsg(sender, "use /" + label + " restart <game> to restart a game using the new parameters");
                } else {
                    if (args[1].toLowerCase().equals("help")) {
                        senderMsg(sender, ChatColor.RED  + "/" + label + " setgameparms <gamename> <parm1:value> <parm2:value>... ");
                        senderMsg(sender, ChatColor.GREEN  + "The possible parameters and their values are:");
                        //senderMsg(sender, ChatColor.YELLOW  + "gamemode -  " + ChatColor.AQUA + " values can be either 'minigame' or 'strategy' - e.g gamemode:strategy");
                        senderMsg(sender, ChatColor.YELLOW  + "teams -  " + ChatColor.AQUA + " the number of teams in the game - e.g. teams:3");
                        senderMsg(sender, ChatColor.YELLOW  + "goal -  " + ChatColor.AQUA + "  one of 'area', 'beacons', 'links', 'triangles' - e.g. goal:links");
                        senderMsg(sender, ChatColor.YELLOW  + "goalvalue -  " + ChatColor.AQUA + "  the number objective for the goal - e.g goalvalue:100");
                        //senderMsg(sender, ChatColor.YELLOW  + "countdown -  " + ChatColor.AQUA + "  the game's timer, in seconds. 0 means the timer runs up, open-ended; any other value meands the timer runs a countdown from that time. - e.g. countdown:600");
                        senderMsg(sender, ChatColor.YELLOW  + "scoretypes -  " + ChatColor.AQUA + "  the scores to be displayed on the sidebar. Can be any combination of goal names separated by '-' e.g scoretypes:area-triangles-beacons-links");
                    } else {
                        String [] parmargs = new String [args.length-2];
                        System.arraycopy(args, 2, parmargs, 0, parmargs.length);
                        game = getGameMgr().getGame(args[1]);
                        if (game == null) {
                            senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                        } else {
                            String errormsg = this.setGameParms(game, parmargs);
                            if (!errormsg.isEmpty()) {
                                senderMsg(sender, ChatColor.RED + Lang.errorError + errormsg);
                            } else {
                                senderMsg(sender, ChatColor.GREEN + "Game parameters set.");
                                if (!(sender instanceof Player)) {
                                    // Console
                                    senderMsg(sender, "use " + ChatColor.GREEN + label + " restart " + game.getName() + ChatColor.RESET + " to restart the game using the new parameters");
                                } else {
                                    senderMsg(sender, "use " + ChatColor.GREEN + "/" + label + " restart " + game.getName() + ChatColor.RESET +" to restart the game using the new parameters");
                                }
                            }
                        }
                    }

                }
                break;
                 */
            case "setspawn":
                if (args.length > 2) {
                    //senderMsg(sender, ChatColor.RED + "/" + label + " setspawn <team> " + Lang.helpAdminSetTeamSpawn);
                    senderMsg(sender, ChatColor.RED + "/" + label + " setspawn " + Lang.helpAdminSetLobbySpawn);
                } else {
                    // Admin set team spawn
                    if (!(sender instanceof Player)) {
                        senderMsg(sender, ChatColor.RED + Lang.errorOnlyPlayers);
                        return true;
                    }
                    // Check if the player is in the lobby
                    player = (Player) sender;
                    if (args.length == 1 && getGameMgr().getLobby().isPlayerInRegion(player)) {
                        getGameMgr().getLobby().setSpawnPoint(player.getLocation());
                        senderMsg(sender, ChatColor.GREEN + Lang.generalSuccess + " (" + player.getLocation().getBlockX() + ","
                                + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ() + ")");
                        return true;
                    } else {
                        senderMsg(sender, ChatColor.RED + Lang.helpAdminSetLobbySpawn);
                        return true;
                    }
                    // Check team name given exists
                    /*
                    game = getGameMgr().getGame(player.getLocation());
                    if (game == null) {
                        senderMsg(sender, Lang.adminSetSpawnNeedToBeInGame);
                    } else {
                        team = game.getScorecard().getTeam(args[1]);
                        if (team == null) {
                            senderMsg(sender, ChatColor.RED + Lang.errorNoSuchTeam + " Use " + game.getScorecard().getTeamListString());
                            return true;
                        }
                        game.getScorecard().setTeamSpawnPoint(team, (player.getLocation()));
                        senderMsg(sender, ChatColor.GREEN + Lang.generalSuccess);
                    }*/
                }
                break;

            case "teams":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " teams [all | <gamename>] " + Lang.helpAdminTeams);
                } else {
                    boolean foundgame = false;
                    for (String gname : getGameMgr().getGames().keySet()) {
                        if (args[1].equalsIgnoreCase("all") || gname.equals(args[1])) {
                            foundgame = true;
                            game = getGameMgr().getGames().get(gname);
                            senderMsg(sender, ChatColor.GREEN + Lang.generalTeams + " - " + gname);
                            Scoreboard sb = game.getScorecard().getScoreboard();
                            if (sb == null) {
                                senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                            } else {
                                HashMap<Team, List<String>> teamMembers = game.getScorecard().getTeamMembers();
                                for (Team t : teamMembers.keySet()) {
                                    senderMsg(sender, "==== " + t.getDisplayName() + " ====");
                                    StringBuilder memberlist = new StringBuilder();
                                    for (String uuid : teamMembers.get(t)) {
                                        memberlist.append("[").append(getServer().getOfflinePlayer(UUID.fromString(uuid)).getName()).append("] ");
                                    }
                                    senderMsg(sender, ChatColor.WHITE + Lang.generalMembers + ": " + memberlist);
                                }
                            }
                        }
                    }
                    if (!foundgame) {
                        if (args[1].equalsIgnoreCase("all")) {
                            senderMsg(sender, Lang.errorNoGames);
                        } else {
                            senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                        }
                    }
                }
                break;
                /*
            case "timertoggle":
                if (args.length < 2) {
                    senderMsg(sender, ChatColor.RED + "/" + label + " timertoggle [all | <gamename>] " + Lang.helpAdminTimerToggle);
                } else {
                    if (args[1].toLowerCase().equals("all")) {
                        Settings.showTimer = ! Settings.showTimer;
                        for (Game g : getGameMgr().getGames().values()) {
                            g.getScorecard().toggleTimer();
                        }
                        senderMsg(sender, ChatColor.GREEN + Lang.generalSuccess);
                    } else {
                        game = getGameMgr().getGames().get(args[1]);
                        if (game == null) {
                            senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                        } else {
                            game.getScorecard().toggleTimer();
                            senderMsg(sender, ChatColor.GREEN + Lang.generalSuccess);
                        }
                    }
                }
                break;
                 */
            default:
                senderMsg(sender, ChatColor.RED + Lang.errorUnknownCommand);
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
        listBeacons(sender, name, "");
    }

    /**
     * Lists all beacons for a given game or for 'all' games
     * @param sender
     * @param name - either 'all' or a valid game name
     * @param search - team name to show
     */
    public void listBeacons(CommandSender sender, String name, String search) {
        // Run through all the beacons
        senderMsg(sender, ChatColor.GREEN + Lang.adminListBeaconsInGame.replace("[name]", name));
        boolean none = true;
        boolean noGame = true;
        for (BeaconObj b : getRegister().getBeaconRegister().values()) { 
            // Find the game this beacon is in
            Game game = getGameMgr().getGame(b.getLocation());
            if (name.equalsIgnoreCase("all") || game.getName().equalsIgnoreCase(name)) {
                noGame = false;
                // Find the team
                if (search.isEmpty() || (search.equalsIgnoreCase("unowned") && b.getOwnership() == null) || (b.getOwnership() != null && b.getOwnership().getName().equalsIgnoreCase(search))) {
                    none = false;
                    senderMsg(sender, game.getName() + ": " + b.getLocation().getBlockX() + "," + b.getLocation().getBlockY() + "," + b.getLocation().getBlockZ() + " >> " + Lang.generalTeam + ": " 
                            + (b.getOwnership() == null ? Lang.generalUnowned :b.getOwnership().getDisplayName()) + " >> " + Lang.generalLinks + ": " + b.getLinks().size());
                }
            }
        }
        if (none) {
            senderMsg(sender, Lang.generalNone);  
        }
        if (noGame) {
            if (name.equals("all")) {
                senderMsg(sender, Lang.errorNoGames);
            } else {
                senderMsg(sender, ChatColor.RED + Lang.errorNoSuchGame + " '" + name + "'");
            } 
        }
    }

    /**
     * setGameParms parses a list of colon-delimited arguments and
     * sets a game's parameters accordingly
     * Used by both newgame and setparms commands
     * @param game - if game is null, will set GameMgr's default parms; otherwise, will set the game's parms
     * @param args - the array of parameters
     * @return an error message if a problem was encountered, null otherwise
     */
    public String setGameParms(Game game, String[] args) {
        String errormsg = "";

        // Check the parameters given
        errormsg = checkParms(args);

        // If *ALL* arguments are OK, process them into the game
        if (errormsg.isEmpty()) {
            for (String arg : args) {
                String parm = arg.split(":")[0];
                String value = arg.split(":")[1];
                if (parm == null || value == null) {
                    errormsg = Lang.adminParmsArgumentsPairs;
                } else {

                    switch (parm.toLowerCase()) {
                        case "gamemode":
                            game.setGamemode(value);
                            break;
                        case "teams":
                            game.setNbrTeams(Integer.parseInt(value));
                            break;
                        case "goal":
                            game.setGamegoal(value);
                            break;
                        case "goalvalue":
                            game.setGamegoalvalue(Integer.parseInt(value));
                            break;
                        case "countdown":
                            game.setCountdownTimer(Integer.parseInt(value));
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
        int nteams = Settings.defaultTeamNumber;
        String ggoal = mode.equalsIgnoreCase("strategy") ? Settings.strategyGoal: Settings.minigameGoal;
        int gvalue = mode.equalsIgnoreCase("strategy") ? Settings.strategyGoalValue : Settings.minigameGoalValue;
        int timer = mode.equalsIgnoreCase("strategy") ? Settings.strategyTimer : Settings.minigameTimer;
        int gdistance = Settings.gameDistance;
        double gdistribution = Settings.distribution;
        String stypes = null;

        // Check the parameters given
        errormsg = checkParms(args);

        // If all arguments are OK, set them as the new defaults for creating games
        if (errormsg.isEmpty()) {
            for (String arg : args) {
                String parm = arg.split(":")[0];
                String value = arg.split(":")[1];
                if (parm == null || value == null) {
                    errormsg = Lang.adminParmsArgumentsPairs;
                } else {
                    switch (parm.toLowerCase()) {
                        case "gamemode":
                            mode = value;
                            break;
                        case "size":
                            gdistance = Integer.parseInt(value);
                            break;
                        case "teams":
                            nteams = Integer.parseInt(value);
                            break;
                        case "goal":
                            ggoal = value;
                            break;
                        case "goalvalue":
                            gvalue = Integer.parseInt(value);
                            break;
                        case "countdown":
                            timer = Integer.parseInt(value);
                            break;
                        case "scoretypes":
                            stypes = value.replace("-", ":");
                            break;
                        case "distribution":
                            gdistribution = Double.parseDouble(value);
                        default:
                            break;
                    }
                }
            }
        }

        // Any of the arguments to setGameDefaultParms CAN be null
        getGameMgr().setGameDefaultParms(mode, gdistance, nteams, ggoal, gvalue, timer, stypes, gdistribution);

        return errormsg;
    }

    /**
     * Checks that an array of arguments contains valid parameters for a game
     * @param args - the array of parameters
     * @return an error message if a problem was encountered, null otherwise
     */
    public String checkParms(String[] args) {
        StringBuilder errormsg = new StringBuilder();

        // Check that *ALL* arguments are valid parms
        for (String arg : args) {
            String parm = null;
            String value = null;
            try {
                parm = arg.split(":")[0];
                value = arg.split(":")[1];
            } catch (Exception e) {
            }
            if (parm == null || value == null) {
                errormsg = new StringBuilder(Lang.adminParmsArgumentsPairs);
            } else {
                switch (parm.toLowerCase()) {
                    case "gamemode":
                        if (!value.equals("strategy") && !value.equals("minigame")) {
                            errormsg.append("<< 'gamemode:' has to be either 'strategy' or 'minigame' >>");
                        }
                        break;
                    case "size":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = new StringBuilder("<< 'size:' value must be a number >>");
                        }
                        break;
                    case "teams":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = new StringBuilder("<< 'team:' value must be a number >>");
                        }
                        int number = NumberUtils.toInt(value);
                        if (number < 2 || number > 14) {
                            errormsg = new StringBuilder("<< 'team:' value must be between 2 and 14 >>");
                        }
                        break;
                    case "goal":
                        if (!value.equals("area") && !value.equals("beacons") &&
                                !value.equals("links") && !value.equals("triangles")) {
                            errormsg = new StringBuilder("<< 'goal:' has to be one of 'area', 'beacons', 'links' or 'triangles' >>");
                        }
                        break;
                    case "goalvalue":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = new StringBuilder("<< 'goalvalue:' value must be a number >>");
                        }
                        int number2 = NumberUtils.toInt(value);
                        if (number2 < 0) {
                            errormsg = new StringBuilder("<< 'goalvalue:' value cannot be negative >>");
                        }
                        break;
                    case "countdown":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = new StringBuilder("<< 'countdown:' value must be a number >>");
                        }
                        break;
                    case "scoretypes":
                        String stmsg = "<< 'scoretypes:' must be a list of the goals to display on the scoreboard, such as 'area-beacons'. Possible goals are 'area', 'beacons', 'links' and 'triangles' >>";
                        value = value.replace(" ", "");
                        String[] stypes = value.split("-");
                        if (stypes.length == 0) {
                            errormsg = new StringBuilder(stmsg);
                        } else {
                            for (String stype : stypes) {
                                if (!stype.equals("area") && !stype.equals("beacons") &&
                                        !stype.equals("links") && !stype.equals("triangles")) {
                                    errormsg = new StringBuilder(stmsg);
                                    break;
                                }
                            }
                        }
                        break;
                    case "distribution":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg = new StringBuilder("<< 'distribution must be a number between 0.01 and 0.99 >>");
                        }
                        double dist = NumberUtils.toDouble(value);
                        if (dist == 0) {
                            errormsg = new StringBuilder("<< 'distribution must be a number between 0.01 and 0.99 >>");
                        }
                        break;
                    default:
                        errormsg = new StringBuilder(Lang.adminParmsDoesNotExist.replace("[name]", parm));
                        break;
                }
            }
        }

        // All done
        return errormsg.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> options = new ArrayList<>();
        Player player = null;
        if (sender instanceof Player) {
            player = (Player)sender;
            if (!player.isOp() && !player.hasPermission("beaconz.admin")) {
                return options;
            }
        }
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");
        Game game = null;
        switch (args.length) {
        case 0:
        case 1:
            if (sender instanceof Player) {
                // Player options
                options.add("claim");
                options.add("join");
                //options.add("link");
                options.add("setspawn");
                options.add("switch");
            }
            // Console options
            options.add("delete");
            options.add("distribution");
            options.add("games");
            //options.add("kick");
            //options.add("restart");
            options.add("regenerate");
            //options.add("pause");
            //options.add("resume");
            options.add("force_end");
            options.add("list");
            options.add("listparms");
            options.add("newgame");
            options.add("reload");
            //options.add("setgameparms");
            options.add("teams");
            //options.add("timertoggle");
            break;
        case 2:
            if (sender instanceof Player) {
                // Add the player options
                game = getGameMgr().getGame(player.getLocation());
                if (args[0].equalsIgnoreCase("claim")) {
                    options.add("unowned");
                    options.addAll(game.getScorecard().getTeamsNames());
                }
                if (args[0].equalsIgnoreCase("join")) {
                    // List all the games
                    options.addAll(getGameMgr().getGames().keySet());
                }
            }
            // Complete the options with the console-only options
            // Player names
            //if (args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("switch")) {
            if (args[0].equalsIgnoreCase("switch")) {
                for (Player p : getServer().getOnlinePlayers()) {
                    options.add(p.getName());
                }
            }
            // Game name options
            if (args[0].equalsIgnoreCase("delete")
                    || args[0].equalsIgnoreCase("regenerate") //|| args[0].equalsIgnoreCase("restart")
                    //|| args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("resume")
                    || args[0].equalsIgnoreCase("force_end") || args[0].equalsIgnoreCase("listparms")
                    //|| args[0].equalsIgnoreCase("setgameparms")
                    || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("teams")
                    //|| args[0].equalsIgnoreCase("timertoggle")
                    ) {
                // List all the games
                options.addAll(getGameMgr().getGames().keySet());
            }

            if (args[0].equalsIgnoreCase("newgame")) {
                options.add("help");
            }
            // Options with "all"
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("teams") || args[0].equalsIgnoreCase("timertoggle")) {
                // List all the games
                options.add("all");
            }
            if (args[0].equalsIgnoreCase("setspawn")) {
                // List all the teams
                options.addAll(game.getScorecard().getTeamsNames());
            }
            break;
        case 3:
            if (sender instanceof Player) {
                if (args[0].equalsIgnoreCase("join")) {
                    // List all the teams
                    game = getGameMgr().getGame(args[1]);
                    if (game != null) {
                        options.addAll(game.getScorecard().getTeamsNames());
                    }
                }
            }
            /*
            if (args[0].equalsIgnoreCase("kick") ) {
                // List all the games
                options.addAll(getGameMgr().getGames().keySet());
            }*/
            if (args[0].equalsIgnoreCase("list") && !args[1].equalsIgnoreCase("all")) {
                // List all the teams in the game
                game = getGameMgr().getGame(args[1]);
                if (game != null) {
                    options.addAll(game.getScorecard().getTeamsNames());
                }
                options.add("unowned");
            }
        default:
            // For length > 2 setgameparms and newgame only
            if (args.length > 2 && args[0].equalsIgnoreCase("newgame")) {
                options.add("gamemode:strategy");
                options.add("gamemode:minigame");
                options.add("teams:");
                options.add("goal:area");
                options.add("goal:beacons");
                options.add("goal:links");
                options.add("goal:triangles");
                options.add("goalvalue:");
                options.add("countdown:");
                options.add("scoretypes:area");
                options.add("scoretypes:beacons");
                options.add("scoretypes:links");
                options.add("scoretypes:triangles");
            }
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

