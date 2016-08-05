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
        // TODO: Make this a permission
        if (sender instanceof Player) {
            Player player = (Player)sender;
            if (!player.isOp() && !player.hasPermission("beaconz.admin")) {
                sender.sendMessage(ChatColor.RED + Lang.errorYouDoNotHavePermission);
                return true;
            }
        }
        Team team = null;
        Game game = null;
        Player player = null;

        if (args.length == 0 || (args.length == 1 && args[0].toLowerCase().equals("help"))) {
            ChatColor cc1 = ChatColor.GREEN;
            ChatColor cc2 = ChatColor.YELLOW;
            ChatColor cc3 = ChatColor.AQUA;
            sender.sendMessage(cc1 + Lang.helpLine);
            sender.sendMessage(cc2 + Lang.helpAdminTitle);
            sender.sendMessage(cc1 + Lang.helpLine);
            if (sender instanceof Player) {
                sender.sendMessage(cc1 + "/" + label + cc2 + " claim [unowned | <team>]" + cc3 + Lang.helpAdminClaim);
            }
            sender.sendMessage(cc1 + "/" + label + cc2 + " delete <gamename>" + cc3 + Lang.helpAdminDelete);
            //sender.sendMessage(cc1 + "/" + label + cc2 + " distribution <decimal between 0 and 1>" + cc3 + Lang.helpAdminDistribution);
            if (sender instanceof Player) {
                sender.sendMessage(cc1 + "/" + label + cc2 + " join <gamename> <team>" + cc3 + Lang.helpAdminJoin);
            }
            sender.sendMessage(cc1 + "/" + label + cc2 + " games" + cc3 + Lang.helpAdminGames);
            sender.sendMessage(cc1 + "/" + label + cc2 + " kick <online playername> <gamename>" + cc3 + Lang.helpAdminKick);
            sender.sendMessage(cc1 + "/" + label + cc2 + " restart <gamename>" + cc3 + Lang.helpAdminRestart);
            sender.sendMessage(cc1 + "/" + label + cc2 + " reset <gamename>" + cc3 + Lang.helpAdminRegenerate);
            sender.sendMessage(cc1 + "/" + label + cc2 + " pause <gamename>" + cc3 + Lang.helpAdminPause);
            sender.sendMessage(cc1 + "/" + label + cc2 + " resume <gamename>" + cc3 + Lang.helpAdminResume);
            sender.sendMessage(cc1 + "/" + label + cc2 + " force_end <gamename>" + cc3 + Lang.helpAdminForceEnd);
            if (sender instanceof Player) {
                sender.sendMessage(cc1 + "/" + label + cc2 + " link <x> <z>" + cc3 + Lang.helpAdminLink);
            }
            sender.sendMessage(cc1 + "/" + label + cc2 + " list [all |<gamename>] [team]" + cc3 + Lang.helpAdminList);
            sender.sendMessage(cc1 + "/" + label + cc2 + " listparms <gamename>" + cc3 + Lang.helpAdminListParms);
            sender.sendMessage(cc1 + "/" + label + cc2 + " newgame <gamename> [<parm1:value> <parm2:value>...]" + cc3 + Lang.helpAdminNewGame.replace("[label]", label));
            sender.sendMessage(cc1 + "/" + label + cc2 + " reload" + cc3 + Lang.helpAdminReload);
            sender.sendMessage(cc1 + "/" + label + cc2 + " setgameparms <gamename> <parm1:value> <parm2:value>... " + cc3 + Lang.helpAdminSetGameParms.replace("[label]", label));
            if (sender instanceof Player) {
                sender.sendMessage(cc1 + "/" + label + cc2 + " setspawn <team>" + cc3 + Lang.helpAdminSetTeamSpawn);
                sender.sendMessage(cc1 + "/" + label + cc2 + " setspawn " + cc3 + Lang.helpAdminSetLobbySpawn);
                sender.sendMessage(cc1 + "/" + label + cc2 + " switch " + cc3 + Lang.helpAdminSwitch);
            }
            sender.sendMessage(cc1 + "/" + label + cc2 + " switch <online playername> " + cc3 + Lang.helpAdminSwitch);
            sender.sendMessage(cc1 + "/" + label + cc2 + " teams [all | <gamename>]" + cc3 + Lang.helpAdminTeams);
            sender.sendMessage(cc1 + "/" + label + cc2 + " timertoggle [all | <gamename>]" + cc3 + Lang.helpAdminTimerToggle);

        } else {
            switch (args[0].toLowerCase()) {
            case "claim":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " claim [unowned | <team>]");
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Lang.errorOnlyPlayers);
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
                                sender.sendMessage(Lang.errorYouHaveToBeStandingOnABeacon);
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
                                        block.setType(game.getScorecard().getBlockID(team).getItemType());
                                        block.setData(game.getScorecard().getBlockID(team).getData());
                                    }
                                    player.sendMessage(Lang.beaconClaimedForTeam.replace("[team]", args[1]));
                                }
                            }
                        }
                    }
                }
                break;
                /*
            case "distribution":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " distribution <fraction between 0 and 1> " + Lang.helpAdminDistribution);
                } else {
                    try {
                        double dist = Double.valueOf(args[1]);
                        if (dist > 0D && dist < 1D) {
                            Settings.distribution = dist;
                            sender.sendMessage(ChatColor.GREEN + Lang.distributionSettingTo.replace("[value]", String.valueOf(dist)));
                            return true;
                        }
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + label + " distribution <fraction> - must be less than 1");
                    }
                }
                break;
                 */
            case "switch":
                // Switch teams within a game
                if (args.length == 1) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Lang.errorOnlyPlayers);
                        return true;
                    } else {
                        player = (Player) sender;
                        team = getGameMgr().getPlayerTeam(player);
                        if (team == null) {
                            sender.sendMessage(ChatColor.RED + Lang.errorYouMustBeInATeam);
                            return true;
                        }
                        game = getGameMgr().getGame(team);
                        if (game == null) {
                            sender.sendMessage(ChatColor.RED + Lang.errorYouMustBeInAGame);
                            return true;
                        }

                        // Get the next team in this game
                        for (Team newTeam : game.getScorecard().getTeams()) {
                            if (!newTeam.equals(team)) {
                                // Found an alternative
                                game.getScorecard().addTeamPlayer(newTeam, player);
                                sender.sendMessage(ChatColor.GREEN + Lang.actionsSwitchedToTeam.replace("[team]", newTeam.getDisplayName()));
                                // Remove any potion effects
                                for (PotionEffect effect : player.getActivePotionEffects())
                                    player.removePotionEffect(effect.getType());
                                return true;
                            }
                        }
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchTeam);
                        return true;
                    }
                } else if (args.length == 2) {
                    player = getServer().getPlayer(args[1]);
                    if (player == null) {
                        sender.sendMessage(Lang.errorUnknownPlayer);
                        return true;
                    } else {
                        team = getGameMgr().getPlayerTeam(player);
                        if (team == null) {
                            sender.sendMessage(ChatColor.RED + Lang.errorNoSuchTeam);
                            return true;
                        }
                        game = getGameMgr().getGame(team);
                        if (game == null) {
                            sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame);
                            return true;
                        }

                        // Get the next team in this game
                        for (Team newTeam : game.getScorecard().getTeams()) {
                            if (!newTeam.equals(team)) {
                                // Found an alternative
                                game.getScorecard().addTeamPlayer(newTeam, player);
                                sender.sendMessage(ChatColor.GREEN + player.getName() + ": " + Lang.actionsSwitchedToTeam.replace("[team]", newTeam.getDisplayName()));
                                player.sendMessage(ChatColor.GREEN + Lang.actionsSwitchedToTeam.replace("[team]", newTeam.getDisplayName()));

                                // Remove any potion effects
                                for (PotionEffect effect : player.getActivePotionEffects())
                                    player.removePotionEffect(effect.getType());
                                return true;
                            }
                        }
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchTeam);
                        return true;
                    }
                }
            case "join":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " join <gamename> <team>" + Lang.helpAdminJoin);
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Lang.errorOnlyPlayers);
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
                                sender.sendMessage(ChatColor.GREEN + Lang.actionsYouAreInTeam.replace("[team]", team.getDisplayName()));
                            } else {
                                sender.sendMessage(ChatColor.RED + "/" + label + " join " + args[1] + " [" + game.getScorecard().getTeamListString() + "]");
                            }
                        } else {
                            sender.sendMessage("/" + label + " join <gamename> <team> - " + Lang.errorNoSuchGame);
                        }
                    }
                }
                break;

            case "games":
                sender.sendMessage(ChatColor.GREEN + Lang.adminGamesDefined);
                sender.sendMessage(ChatColor.AQUA + Lang.adminGamesTheLobby + " - " + getGameMgr().getLobby().displayCoords());
                int cnt = 0;
                for (Game g : getGameMgr().getGames().values()) {
                    cnt ++;
                    sender.sendMessage(ChatColor.AQUA + g.getName() + " - " + g.getRegion().displayCoords());
                }
                if (cnt == 0) sender.sendMessage(ChatColor.AQUA + Lang.adminGamesNoOthers);
                break;

            case "kick":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "/" + label + Lang.helpAdminKick);
                } else {
                    player = getServer().getPlayer(args[1]);
                    if (player == null && args[1] != "all") {
                        sender.sendMessage(Lang.errorUnknownPlayer);
                    } else {
                        game = getGameMgr().getGame(args[2]);
                        if (game == null) {
                            sender.sendMessage(Lang.errorNoSuchGame);
                        } else {
                            if (args[2].equals("all")) {
                                game.kick();
                                sender.sendMessage(Lang.adminKickAllPlayers.replace("[name]", game.getName()));
                            } else {
                                game.kick(player);
                                sender.sendMessage((Lang.adminKickPlayer.replace("[player]", player.getName()).replace("[name]", game.getName())));
                            }
                        }
                    }
                }
                break;

            case "restart":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " restart <gamename> - " + Lang.helpAdminRestart);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.restart();
                        sender.sendMessage(ChatColor.GREEN + Lang.adminRestart.replace("[name]", game.getName()));
                    }
                }
                break;

            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " delete <gamename> - " + Lang.helpAdminDelete);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + Lang.adminDeletingGame.replace("[name]", game.getName()));
                        getGameMgr().delete(sender, game);
                        sender.sendMessage(ChatColor.GREEN + Lang.adminDeletedGame.replace("[name]", game.getName()));
                    }
                }
                break;

            case "regenerate":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " regenerates <gamename> - " + Lang.helpAdminRegenerate);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + Lang.adminRegeneratingGame.replace("[name]", game.getName()));
                        game.reset(sender);
                    }
                }
                break;

            case "pause":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " pause <gamename> " + Lang.helpAdminPause);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.pause();
                        sender.sendMessage(ChatColor.GREEN + (Lang.adminPaused.replace("[name]", game.getName()).replace("[label]", label)));
                    }
                }
                break;

            case "resume":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " resume <gamename> " + Lang.helpAdminResume);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.resume();
                        sender.sendMessage(ChatColor.GREEN + Lang.adminResume.replace("[name]", game.getName()));
                    }
                }
                break;

            case "force_end":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " force_end <gamename>" + Lang.helpAdminForceEnd);
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        game.forceEnd();
                        sender.sendMessage(ChatColor.GREEN + Lang.adminForceEnd.replace("[name]", game.getName()));
                        sender.sendMessage(ChatColor.GREEN + Lang.adminForceRestart.replace("[label]", label));
                    }
                }
                break;

            case "link":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " link <x> <z> " + Lang.helpAdminLink);
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Lang.errorOnlyPlayers);
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

            case "list":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " list [all |<gamename>] [team] " + Lang.helpAdminList);
                } else if (args.length == 3) {
                    listBeacons(sender, args[1], args[2]);
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
                            sender.sendMessage(Lang.errorAlreadyExists.replace("[name]", game.getName()));
                        } else {
                            //Check the parameters first, if any
                            if (parmargs.length > 0) {
                                String errormsg = setDefaultParms(parmargs);  // temporarily set up the given parameters as default
                                if (!errormsg.isEmpty()) {
                                    sender.sendMessage(ChatColor.RED + Lang.errorError + errormsg);
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters (just in case)
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + Lang.adminNewGameBuilding);
                                    getGameMgr().newGame(args[1]);   // create the new game
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters
                                    sender.sendMessage(ChatColor.GREEN + Lang.generalSuccess);
                                }
                            } else {
                                sender.sendMessage(ChatColor.GREEN + Lang.adminNewGameBuilding);
                                getGameMgr().newGame(args[1]);
                                sender.sendMessage(ChatColor.GREEN + Lang.generalSuccess);
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
                sender.sendMessage(ChatColor.RED + Lang.adminReload);
                break;

            case "listparms":
                if (args.length < 2) {
                    sender.sendMessage("/" + label + " listparms <gamename> " + Lang.helpAdminListParms);
                } else {
                    game = getGameMgr().getGame(args[1]);
                    if (game == null) {
                        sender.sendMessage(Lang.errorNoSuchGame + "'" + args[1] + "'");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + Lang.adminParmsMode + ": " + ChatColor.AQUA + game.getGamemode());
                        sender.sendMessage(ChatColor.YELLOW + Lang.adminParmsTeams + ": " + ChatColor.AQUA + game.getNbrTeams());
                        sender.sendMessage(ChatColor.YELLOW + Lang.adminParmsGoal + ": " + ChatColor.AQUA  + game.getGamegoal());
                        sender.sendMessage(ChatColor.YELLOW + Lang.adminParmsGoalValue + ": " + ChatColor.AQUA + (game.getGamegoalvalue() == 0 ? Lang.adminParmsUnlimited : game.getGamegoalvalue()));
                        sender.sendMessage(ChatColor.YELLOW + Lang.adminParmsCountdown + ": " + ChatColor.AQUA + (game.getCountdownTimer() == 0 ? Lang.adminParmsUnlimited : game.getCountdownTimer()));
                        sender.sendMessage(ChatColor.YELLOW + Lang.adminParmsScoreTypes + ": " + ChatColor.AQUA + game.getScoretypes());
                    }
                }
                return true;

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
                            sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                        } else {
                            String errormsg = this.setGameParms(game, parmargs);
                            if (!errormsg.isEmpty()) {
                                sender.sendMessage(ChatColor.RED + Lang.errorError + errormsg);
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Game parameters set.");
                                if (!(sender instanceof Player)) {
                                    // Console
                                    sender.sendMessage("use " + ChatColor.GREEN + label + " restart " + game.getName() + ChatColor.RESET + " to restart the game using the new parameters");
                                } else {
                                    sender.sendMessage("use " + ChatColor.GREEN + "/" + label + " restart " + game.getName() + ChatColor.RESET +" to restart the game using the new parameters");
                                }
                            }
                        }
                    }

                }
                break;

            case "setspawn":
                if (args.length > 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " setspawn <team> " + Lang.helpAdminSetTeamSpawn);
                    sender.sendMessage(ChatColor.RED + "/" + label + " setspawn " + Lang.helpAdminSetLobbySpawn);
                } else {
                    // Admin set team spawn
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + Lang.errorOnlyPlayers);
                        return true;
                    }
                    // Check if the player is in the lobby
                    player = (Player) sender;
                    if (args.length == 1 && getGameMgr().getLobby().isPlayerInRegion(player)) {
                        getGameMgr().getLobby().setSpawnPoint(player.getLocation());
                        sender.sendMessage(ChatColor.GREEN + Lang.generalSuccess + " (" + player.getLocation().getBlockX() + ","
                                + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ() + ")");
                        return true;
                    }
                    // Check team name given exists
                    game = getGameMgr().getGame(player.getLocation());
                    if (game == null) {
                        sender.sendMessage(Lang.adminSetSpawnNeedToBeInGame);
                    } else {
                        team = game.getScorecard().getTeam(args[1]);
                        if (team == null) {
                            sender.sendMessage(ChatColor.RED + Lang.errorNoSuchTeam + " Use " + game.getScorecard().getTeamListString());
                            return true;
                        }
                        game.getScorecard().setTeamSpawnPoint(team, (player.getLocation()));
                        sender.sendMessage(ChatColor.GREEN + Lang.generalSuccess);
                    }
                }
                break;

            case "teams":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " teams [all | <gamename>] " + Lang.helpAdminTeams);
                } else {
                    Boolean foundgame = false;
                    for (String gname : getGameMgr().getGames().keySet()) {
                        if (args[1].toLowerCase().equals("all") || gname.equals(args[1])) {
                            foundgame = true;
                            game = getGameMgr().getGames().get(gname);
                            sender.sendMessage(ChatColor.GREEN + Lang.generalTeams + " - " + gname);
                            Scoreboard sb = game.getScorecard().getScoreboard();
                            if (sb == null) {
                                sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                            } else {
                                HashMap<Team, List<String>> teamMembers = game.getScorecard().getTeamMembers();
                                for (Team t : teamMembers.keySet()) {
                                    sender.sendMessage("==== " + t.getDisplayName() + " ====");
                                    String memberlist = "";
                                    for (String uuid : teamMembers.get(t)) {
                                        memberlist = memberlist + "[" + getServer().getOfflinePlayer(UUID.fromString(uuid)).getName() + "] ";
                                    }
                                    sender.sendMessage(ChatColor.WHITE + Lang.generalMembers + ": " + memberlist);
                                }
                            }
                        }
                    }
                    if (!foundgame) {
                        if (args[1].toLowerCase().equals("all")) {
                            sender.sendMessage(Lang.errorNoGames);
                        } else {
                            sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
                        }
                    }
                }
                break;

            case "timertoggle":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " timertoggle [all | <gamename>] " + Lang.helpAdminTimerToggle);
                } else {
                    if (args[1].toLowerCase().equals("all")) {
                        Settings.showTimer = ! Settings.showTimer;
                        for (Game g : getGameMgr().getGames().values()) {
                            g.getScorecard().toggleTimer();
                        }
                    } else {
                        game = getGameMgr().getGames().get(args[1]);
                        if (game == null) {
                            sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + "'" + args[1] + "'");
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
        sender.sendMessage(ChatColor.GREEN + Lang.adminListBeaconsInGame.replace("[name]", name));
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
                    sender.sendMessage(game.getName() + ": " + b.getLocation().getBlockX() + "," + b.getLocation().getBlockY() + "," + b.getLocation().getBlockZ() + " >> " + Lang.generalTeam + ": " 
                            + (b.getOwnership() == null ? Lang.generalUnowned :b.getOwnership().getDisplayName()) + " >> " + Lang.generalLinks + ": " + b.getLinks().size());
                }
            }
        }
        if (none) {
            sender.sendMessage(Lang.generalNone);  
        }
        if (noGame) {
            if (name.equals("all")) {
                sender.sendMessage(Lang.errorNoGames);
            } else {
                sender.sendMessage(ChatColor.RED + Lang.errorNoSuchGame + " '" + name + "'");
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

        // Check the parameters given
        errormsg = checkParms(args);

        // If *ALL* arguments are OK, process them into the game
        if (errormsg.isEmpty()) {
            for (int i=0; i < args.length; i++) {
                String parm = args[i].split(":")[0];
                String value = args[i].split(":")[1];
                if (parm == null || value == null) {
                    errormsg = Lang.adminParmsArgumentsPairs;
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
                        game.setCountdownTimer(Integer.valueOf(value));
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
        errormsg = checkParms(args);

        // If all arguments are OK, set them as the new defaults for creating games
        if (errormsg.isEmpty()) {
            for (int i=0; i < args.length; i++) {
                String parm = args[i].split(":")[0];
                String value = args[i].split(":")[1];
                if (parm == null || value == null) {
                    errormsg = Lang.adminParmsArgumentsPairs;
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
    public String checkParms(String[] args) {
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
                errormsg = Lang.adminParmsArgumentsPairs;
            } else {
                switch (parm.toLowerCase()) {
                case "gamemode":
                    if (!value.equals("strategy") && !value.equals("minigame")) {
                        errormsg = errormsg + "<< 'gamemode:' has to be either 'strategy' or 'minigame' >>";
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
                    errormsg = Lang.adminParmsDoesNotExist.replace("[name]", parm);
                    break;
                }
            }
        }

        // All done
        return errormsg;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> options = new ArrayList<String>();
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");
        Player player = null;
        Game game = null;
        switch (args.length) {
        case 0:
        case 1:
            if (sender instanceof Player) {
                // Player options
                options.add("claim");
                options.add("join");
                options.add("link");
                options.add("setspawn");
                options.add("switch");
            }
            // Console options
            options.add("delete");
            options.add("distribution");
            options.add("games");
            options.add("kick");
            options.add("restart");
            options.add("regenerate");
            options.add("pause");
            options.add("resume");
            options.add("force_end");
            options.add("list");
            options.add("listparms");
            options.add("newgame");
            options.add("reload");
            options.add("setgameparms");
            options.add("teams");
            options.add("timertoggle");
            break;
        case 2:
            if (sender instanceof Player) {
                // Add the player options
                player = (Player)sender;
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
            if (args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("switch")) {
                for (Player p : getServer().getOnlinePlayers()) {
                    options.add(p.getName());
                }
            }
            // Game name options
            if (args[0].equalsIgnoreCase("delete")
                    || args[0].equalsIgnoreCase("regenerate") || args[0].equalsIgnoreCase("restart")
                    || args[0].equalsIgnoreCase("pause") || args[0].equalsIgnoreCase("resume")
                    || args[0].equalsIgnoreCase("force_end") || args[0].equalsIgnoreCase("listparms")
                    || args[0].equalsIgnoreCase("setgameparms")
                    || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("teams")
                    || args[0].equalsIgnoreCase("timertoggle")) {
                // List all the games
                options.addAll(getGameMgr().getGames().keySet());
            }
            if (args[0].equalsIgnoreCase("setgameparms")) {
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
                player = (Player)sender;           
                if (args[0].equalsIgnoreCase("join")) {
                    // List all the teams
                    game = getGameMgr().getGame(args[1]);
                    if (game != null) {
                        options.addAll(game.getScorecard().getTeamsNames());
                    }
                }
            }
            if (args[0].equalsIgnoreCase("kick") ) {
                // List all the games
                options.addAll(getGameMgr().getGames().keySet());
            }
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
            if (args.length > 2 && (args[0].equalsIgnoreCase("setgameparms") || args[0].equalsIgnoreCase("newgame"))) {
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
        final List<String> returned = new ArrayList<String>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                returned.add(s);
            }
        }
        return returned;
    }
}

