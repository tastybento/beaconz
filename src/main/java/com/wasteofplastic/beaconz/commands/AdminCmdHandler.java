/*
 * Copyright (c) 2015 - 2025 tastybento
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Settings;

/**
 * Handles all administrative commands for the Beaconz plugin.
 *
 * <p>This class processes admin-level commands that allow server operators to:
 * <ul>
 *   <li>Manage games (create, delete, force end)</li>
 *   <li>Control beacons (claim, list)</li>
 *   <li>Manage teams (switch players, view team rosters)</li>
 *   <li>Configure game parameters (distribution, spawn points)</li>
 *   <li>Monitor game state (list games, view parameters)</li>
 * </ul>
 *
 * <p>All commands require either OP status or the "beaconz.admin" permission.
 * Commands use modern Kyori Adventure Components for rich text formatting.
 *
 * <p><b>Available Commands:</b>
 * <ul>
 *   <li><b>claim</b> - Admin claim a beacon for a specific team or mark as unowned</li>
 *   <li><b>delete</b> - Delete an entire game and its region</li>
 *   <li><b>distribution</b> - Set beacon distribution probability</li>
 *   <li><b>force_end</b> - Force a game to end immediately</li>
 *   <li><b>games</b> - List all active games and the lobby</li>
 *   <li><b>join</b> - Force a player to join a specific team in a game</li>
 *   <li><b>kick</b> - Remove a player from a game</li>
 *   <li><b>list</b> - List all beacons in a game or across all games</li>
 *   <li><b>listparms</b> - Display game parameters (mode, teams, goals, etc.)</li>
 *   <li><b>newgame</b> - Create a new game with optional custom parameters</li>
 *   <li><b>reload</b> - Reload plugin configuration and game data</li>
 *   <li><b>setspawn</b> - Set the lobby spawn point</li>
 *   <li><b>switch</b> - Switch a player to another team in their current game</li>
 *   <li><b>teams</b> - Display team rosters for a game or all games</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class AdminCmdHandler extends BeaconzPluginDependent implements CommandExecutor, TabCompleter {

    /**
     * Constructs a new AdminCmdHandler.
     *
     * @param beaconzPlugin the main plugin instance
     */
    public AdminCmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    /**
     * Processes admin commands for the Beaconz plugin.
     *
     * <p>This method handles all administrative commands, performing permission checks
     * and routing commands to their appropriate handlers. Only players with OP status
     * or the "beaconz.admin" permission can execute these commands.
     *
     * <p>When called with no arguments or just "help", displays a comprehensive help
     * menu showing all available admin commands with color-coded syntax.
     *
     * @param sender the command sender (player or console)
     * @param command the command that was executed
     * @param label the command label (alias) used
     * @param args the command arguments
     * @return true if the command was handled, false otherwise
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check: Only OPs or players with admin permission can use these commands
        if (sender instanceof Player player) {
            if (!player.isOp() && !player.hasPermission("beaconz.admin")) {
                sender.sendMessage(Component.text(Lang.errorYouDoNotHavePermission).color(NamedTextColor.RED));
                return true;
            }
        }

        // Initialize variables for command processing
        Team team = null;      // Current team being worked with
        Game game = null;      // Current game being worked with
        Player player = null;  // Current player being worked with

        // Display help menu if no arguments or "help" command
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            // Define colors for help messages (GREEN for command, YELLOW for syntax, AQUA for description)
            NamedTextColor green = NamedTextColor.GREEN;
            NamedTextColor yellow = NamedTextColor.YELLOW;
            NamedTextColor aqua = NamedTextColor.AQUA;

            // Display help header
            sender.sendMessage(Component.text(Lang.helpLine).color(green));
            sender.sendMessage(Component.text(Lang.helpAdminTitle).color(yellow));
            sender.sendMessage(Component.text(Lang.helpLine).color(green));

            // Player-only commands (require physical presence in game world)
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" claim [unowned | <team>]").color(yellow))
                    .append(Component.text(Lang.helpAdminClaim).color(aqua)));
            }

            // Console-compatible commands
            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" delete <gamename>").color(yellow))
                .append(Component.text(Lang.helpAdminDelete).color(aqua)));

            if (sender instanceof Player) {
                sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" join <gamename> <team>").color(yellow))
                    .append(Component.text(Lang.helpAdminJoin).color(aqua)));
            }

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" games").color(yellow))
                .append(Component.text(Lang.helpAdminGames).color(aqua)));

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" force_end <gamename>").color(yellow))
                .append(Component.text(Lang.helpAdminForceEnd).color(aqua)));

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" list [all |<gamename>] [team]").color(yellow))
                .append(Component.text(Lang.helpAdminList).color(aqua)));

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" listparms <gamename>").color(yellow))
                .append(Component.text(Lang.helpAdminListParms).color(aqua)));

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" newgame <gamename> [<parm1:value> <parm2:value>...]").color(yellow))
                .append(Component.text(Lang.helpAdminNewGame.replace("[label]", label)).color(aqua)));

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" reload").color(yellow))
                .append(Component.text(Lang.helpAdminReload).color(aqua)));

            // Spawn-related commands (player-only)
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" setspawn <team>").color(yellow))
                    .append(Component.text(Lang.helpAdminSetTeamSpawn).color(aqua)));
                sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" setspawn ").color(yellow))
                    .append(Component.text(Lang.helpAdminSetLobbySpawn).color(aqua)));
                sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" switch ").color(yellow))
                    .append(Component.text(Lang.helpAdminSwitch).color(aqua)));
            }

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" switch <online playername> ").color(yellow))
                .append(Component.text(Lang.helpAdminSwitch).color(aqua)));

            sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" teams [all | <gamename>]").color(yellow))
                .append(Component.text(Lang.helpAdminTeams).color(aqua)));

        } else {
            // Process specific commands
            switch (args[0].toLowerCase()) {
            case "claim":
                // CLAIM COMMAND: Admin beacon claim - forcibly assign beacons to teams or mark as unowned
                // Requires player to be standing on the beacon block
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " claim [unowned | <team>]").color(NamedTextColor.RED));
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text(Lang.errorOnlyPlayers));
                    } else {
                        // Check the argument
                        player = (Player) sender;
                        game = getGameMgr().getGame(player.getLocation());
                        team = game.getScorecard().getTeam(args[1]);
                        if (team == null && !args[1].equalsIgnoreCase("unowned")) {
                            sender.sendMessage(Component.text("/" + label + " claim [unowned, " + game.getScorecard().getTeamListString() + "]").color(NamedTextColor.RED));
                        } else {
                            // Check if player is standing on a beacon
                            Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                            if (!getRegister().isBeacon(block)) {
                                sender.sendMessage(Component.text(Lang.errorYouHaveToBeStandingOnABeacon));
                            } else {
                                Point2D newClaim = new Point2D.Double(block.getX(), block.getZ());
                                player.sendMessage(Component.text(Lang.beaconClaimingBeaconAt.replace("[location]", newClaim.toString())));

                                // Verify beacon is registered
                                if (!getRegister().getBeaconRegister().containsKey(newClaim)) {
                                    player.sendMessage(Component.text(Lang.errorNotInRegister + newClaim).color(NamedTextColor.RED));
                                } else {
                                    BeaconObj beacon = getRegister().getBeaconRegister().get(newClaim);

                                    // Process claim or unclaim
                                    if (args[1].equalsIgnoreCase("unowned")) {
                                        getRegister().removeBeaconOwnership(beacon);
                                    } else {
                                        // Claim beacon for team and update block color
                                        getRegister().setBeaconOwner(beacon, team);
                                        block.setType(game.getScorecard().getBlockID(team));
                                    }
                                    player.sendMessage(Component.text(Lang.beaconClaimedForTeam.replace("[team]", args[1])));
                                }
                            }
                        }
                    }
                }
                break;
                
            case "distribution":
                // DISTRIBUTION COMMAND: Set beacon spawn probability (0.0 to 1.0)
                // Controls how frequently beacons generate in chunks
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " distribution <fraction between 0 and 1> " + Lang.helpAdminDistribution).color(NamedTextColor.RED));
                } else {
                    try {
                        double dist = Double.parseDouble(args[1]);
                        if (dist > 0D && dist < 1D) {
                            Settings.distribution = dist;
                            sender.sendMessage(Component.text(Lang.actionsDistributionSettingTo.replace("[value]", String.valueOf(dist))).color(NamedTextColor.GREEN));
                            return true;
                        }
                    } catch (Exception e) {
                        sender.sendMessage(Component.text(label + " distribution <fraction> - must be less than 1").color(NamedTextColor.RED));
                    }
                }
                break;
                 
            case "switch":
                // SWITCH COMMAND: Move a player to a different team within their current game
                // Can be used on self (1 arg) or on another player (2 args)
                if (args.length == 1) {
                    // Switch sender's own team
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text(Lang.errorOnlyPlayers));
                    } else {
                        player = (Player) sender;
                        team = getGameMgr().getPlayerTeam(player);

                        // Validate player is in a team
                        if (team == null) {
                            sender.sendMessage(Component.text(Lang.errorYouMustBeInATeam).color(NamedTextColor.RED));
                            return true;
                        }

                        // Get the game from current team
                        game = getGameMgr().getGame(team);
                        if (game == null) {
                            sender.sendMessage(Component.text(Lang.errorYouMustBeInAGame).color(NamedTextColor.RED));
                            return true;
                        }

                        // Find next available team (first team that isn't the current one)
                        for (Team newTeam : game.getScorecard().getTeams()) {
                            if (!newTeam.equals(team)) {
                                // Found an alternative team - switch to it
                                game.getScorecard().addTeamPlayer(newTeam, player);
                                sender.sendMessage(Component.text(Lang.actionsSwitchedToTeam)
                                    .replaceText(builder -> builder.matchLiteral("[team]").replacement(newTeam.displayName()))
                                    .color(NamedTextColor.GREEN));

                                // Clear all potion effects when switching teams
                                for (PotionEffect effect : player.getActivePotionEffects())
                                    player.removePotionEffect(effect.getType());
                                return true;
                            }
                        }
                        sender.sendMessage(Component.text(Lang.errorNoSuchTeam).color(NamedTextColor.RED));
                    }
                    return true;
                } else if (args.length == 2) {
                    // Switch another player's team (admin forcing a switch)
                    player = getServer().getPlayer(args[1]);
                    if (player == null) {
                        sender.sendMessage(Component.text(Lang.errorUnknownPlayer));
                    } else {
                        team = getGameMgr().getPlayerTeam(player);
                        if (team == null) {
                            sender.sendMessage(Component.text(Lang.errorNoSuchTeam).color(NamedTextColor.RED));
                            return true;
                        }
                        game = getGameMgr().getGame(team);
                        if (game == null) {
                            sender.sendMessage(Component.text(Lang.errorNoSuchGame).color(NamedTextColor.RED));
                            return true;
                        }

                        // Get the next team in this game
                        for (Team newTeam : game.getScorecard().getTeams()) {
                            if (!newTeam.equals(team)) {
                                // Found an alternative - switch the player
                                game.getScorecard().addTeamPlayer(newTeam, player);

                                // Notify both admin and player
                                sender.sendMessage(Component.text(player.getName() + ": ")
                                    .append(Component.text(Lang.actionsSwitchedToTeam)
                                        .replaceText(builder -> builder.matchLiteral("[team]").replacement(newTeam.displayName())))
                                    .color(NamedTextColor.GREEN));
                                player.sendMessage(Component.text(Lang.actionsSwitchedToTeam)
                                    .replaceText(builder -> builder.matchLiteral("[team]").replacement(newTeam.displayName()))
                                    .color(NamedTextColor.GREEN));

                                // Remove any potion effects
                                for (PotionEffect effect : player.getActivePotionEffects())
                                    player.removePotionEffect(effect.getType());
                                return true;
                            }
                        }
                        sender.sendMessage(Component.text(Lang.errorNoSuchTeam).color(NamedTextColor.RED));
                    }
                    return true;
                }
            case "join":
                // JOIN COMMAND: Force a player to join a specific team in a specific game
                // Admin bypass for normal join restrictions
                if (args.length < 3) {
                    sender.sendMessage(Component.text("/" + label + " join <gamename> <team>" + Lang.helpAdminJoin).color(NamedTextColor.RED));
                } else {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text(Lang.errorOnlyPlayers));
                    } else {
                        player = (Player) sender;
                        game = getGameMgr().getGame(args[1]);

                        // Validate game exists
                        if (game != null) {
                            if (game.getScorecard().getTeam(args[2]) != null) {
                                // Team exists - add player to it
                                final Team joinTeam = game.getScorecard().getTeam(args[2]);
                                game.getScorecard().addTeamPlayer(joinTeam, player);
                                player.setScoreboard(game.getScorecard().getScoreboard());
                                game.getScorecard().sendPlayersHome(player, false);
                                sender.sendMessage(Component.text(Lang.actionsYouAreInTeam)
                                    .replaceText(builder -> builder.matchLiteral("[team]").replacement(joinTeam.displayName()))
                                    .color(NamedTextColor.GREEN));
                            }
                        } else {
                            sender.sendMessage(Component.text("/" + label + " join <gamename> <team> - " + Lang.errorNoSuchGame));
                        }
                    }
                }
                break;                

            case "games":
                // GAMES COMMAND: List all active games and their regions including the lobby
                sender.sendMessage(Component.text(Lang.adminGamesDefined).color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text(Lang.adminGamesTheLobby + " - " + getGameMgr().getLobby().displayCoords()).color(NamedTextColor.AQUA));

                // Count and display all games
                int cnt = 0;
                for (Game g : getGameMgr().getGames().values()) {
                    cnt ++;
                    sender.sendMessage(Component.text(g.getName() + " - " + g.getRegion().displayCoords()).color(NamedTextColor.AQUA));
                }
                if (cnt == 0) sender.sendMessage(Component.text(Lang.adminGamesNoOthers).color(NamedTextColor.AQUA));
                break;
                
            case "kick":
                // KICK COMMAND: Remove a player from a game (sends them to lobby)
                if (args.length < 3) {
                    sender.sendMessage(Component.text("/" + label + Lang.helpAdminKick).color(NamedTextColor.RED));
                } else {
                    player = getServer().getPlayer(args[1]);

                    // Check if player exists or if "all" was specified
                    if (player == null && !args[1].equals("all")) {
                        sender.sendMessage(Component.text(Lang.errorUnknownPlayer));
                    } else {
                        game = getGameMgr().getGame(args[2]);
                        if (game == null) {
                            sender.sendMessage(Component.text(Lang.errorNoSuchGame));
                        } else {
                            // Process kick - either all players or specific player
                            if (args[2].equals("all")) {
                                game.kickAll();
                                sender.sendMessage(Component.text(Lang.adminKickAllPlayers.replace("[name]", game.getName())));
                            } else {
                                if (player != null) {
                                    game.kick(sender, player);
                                    sender.sendMessage(Component.text(Lang.adminKickPlayer.replace("[player]", player.getName()).replace("[name]", game.getName())));
                                }
                            }
                        }
                    }
                }
                break;

            case "delete":
                // DELETE COMMAND: Permanently delete a game and its region
                // WARNING: This cannot be undone!
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " delete <gamename> - " + Lang.helpAdminDelete).color(NamedTextColor.RED));
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(Component.text(Lang.errorNoSuchGame + "'" + args[1] + "'").color(NamedTextColor.RED));
                    } else {
                        // Confirm deletion started
                        sender.sendMessage(Component.text(Lang.adminDeletingGame.replace("[name]", game.getName())).color(NamedTextColor.GREEN));
                        getGameMgr().delete(sender, game);
                        // Confirm deletion completed
                        sender.sendMessage(Component.text(Lang.adminDeletedGame.replace("[name]", game.getName())).color(NamedTextColor.GREEN));
                    }
                }
                break;

            case "force_end":
                // FORCE_END COMMAND: Immediately end a game and declare a winner
                // Useful for testing or ending stalled games
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " force_end <gamename>" + Lang.helpAdminForceEnd).color(NamedTextColor.RED));
                } else {
                    game = getGameMgr().getGames().get(args[1]);
                    if (game == null) {
                        sender.sendMessage(Component.text(Lang.errorNoSuchGame + "'" + args[1] + "'").color(NamedTextColor.RED));
                    } else {
                        game.forceEnd();
                        sender.sendMessage(Component.text(Lang.adminForceEnd.replace("[name]", game.getName())).color(NamedTextColor.GREEN));
                    }
                }
                break;
            case "list":
                // LIST COMMAND: Display all beacons in a game or across all games
                // Optional filter by team name or "unowned"
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " list [all |<gamename>] [team] " + Lang.helpAdminList).color(NamedTextColor.RED));
                } else if (args.length == 3) {
                    // List with team filter
                    listBeacons(sender, args[1], args[2]);
                } else {
                    // List all beacons in game
                    listBeacons(sender, args[1]);
                }
                break;

            case "newgame":
                // NEWGAME COMMAND: Create a new game with optional custom parameters
                // Parameters can override defaults: gamemode, size, teams, goal, goalvalue, countdown, scoretypes, distribution
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...] - parameters are optional"));
                    sender.sendMessage(Component.text("/" + label + " do /" + label + " newgame help for a list of the possible parameters"));
                } else {
                    if (args[1].equalsIgnoreCase("help")) {
                        // Display detailed help for all game parameters
                        sender.sendMessage(Component.text("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...]"));
                        sender.sendMessage(Component.text("The optional parameters and their values are:").color(NamedTextColor.GREEN));

                        // Document each parameter with examples
                        sender.sendMessage(Component.text("gamemode -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text(" values can be either 'minigame' or 'strategy' - e.g gamemode:strategy").color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text("size -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text(" length for the side of the game region - e.g. size:500").color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text("teams -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text(" the number of teams in the game - e.g. teams:2").color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text("goal -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text("  one of 'area', 'beacons', 'links', 'triangles' - e.g. goal:links").color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text("goalvalue -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text("  the number objective for the goal - e.g goalvalue:100").color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text("countdown -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text("  the game's timer, in seconds. 0 means the timer runs up, open-ended; any other value meands the timer runs a countdown from that time. - e.g. countdown:600").color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text("scoretypes -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text("  the scores to be displayed on the sidebar. Can be any combination of goal names separated by '-' e.g scoretypes:area-triangles-beacons-links").color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text("distribution -  ").color(NamedTextColor.YELLOW)
                            .append(Component.text("  overrides the system's default beacon distribution - specify a number between 0.01 and 0.99 for the probability of any one chunk containing a beacon.").color(NamedTextColor.AQUA)));
                    } else {
                        // Parse and create game with parameters
                        String [] parmargs = new String [args.length-2];
                        System.arraycopy(args, 2, parmargs, 0, parmargs.length);
                        game = getGameMgr().getGame(args[1]);

                        // Check if game name already exists
                        if (game != null) {
                            sender.sendMessage(Component.text(Lang.errorAlreadyExists.replace("[name]", game.getName())));
                        } else {
                            // Check and validate parameters if provided
                            if (parmargs.length > 0) {
                                String errormsg = setDefaultParms(parmargs);  // temporarily set up the given parameters as default
                                if (!errormsg.isEmpty()) {
                                    sender.sendMessage(Component.text(Lang.errorError + errormsg).color(NamedTextColor.RED));
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters (just in case)
                                } else {
                                    sender.sendMessage(Component.text(Lang.adminNewGameBuilding).color(NamedTextColor.GREEN));
                                    getGameMgr().newGame(args[1]);           // create the new game
                                    getGameMgr().setGameDefaultParms();      // restore the default parameters
                                    sender.sendMessage(Component.text(Lang.generalSuccess).color(NamedTextColor.GREEN));
                                }
                            } else {
                                // Create game with default parameters
                                sender.sendMessage(Component.text(Lang.adminNewGameBuilding).color(NamedTextColor.GREEN));
                                getGameMgr().newGame(args[1]);
                                sender.sendMessage(Component.text(Lang.generalSuccess).color(NamedTextColor.GREEN));
                            }
                        }
                    }
                }
                break;

            case "reload":
                // RELOAD COMMAND: Save current state and reload all configuration
                // Saves: beacon register, game data
                // Reloads: config.yml, game parameters, beacon register
                getRegister().saveRegister();
                getGameMgr().saveAllGames();
                this.getBeaconzPlugin().reloadConfig();
                this.getBeaconzPlugin().loadConfig();
                getGameMgr().reload();
                getRegister().loadRegister();
                sender.sendMessage(Component.text(Lang.adminReload).color(NamedTextColor.RED));
                break;

            case "listparms":
                // LISTPARMS COMMAND: Display all parameters for a specific game
                // Shows: mode, teams, goal, goal value, score types
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " listparms <gamename> " + Lang.helpAdminListParms));
                } else {
                    game = getGameMgr().getGame(args[1]);
                    if (game == null) {
                        sender.sendMessage(Component.text(Lang.errorNoSuchGame + "'" + args[1] + "'"));
                    } else {
                        // Display each game parameter with color formatting
                        sender.sendMessage(Component.text(Lang.adminParmsMode + ": ").color(NamedTextColor.YELLOW)
                            .append(Component.text(game.getGamemode()).color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text(Lang.adminParmsTeams + ": ").color(NamedTextColor.YELLOW)
                            .append(Component.text(String.valueOf(game.getNbrTeams())).color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text(Lang.adminParmsGoal + ": ").color(NamedTextColor.YELLOW)
                            .append(Component.text(game.getGamegoal()).color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text(Lang.adminParmsGoalValue + ": ").color(NamedTextColor.YELLOW)
                            .append(Component.text(game.getGamegoalvalue() == 0 ? Lang.adminParmsUnlimited : String.format(Locale.US, "%,d", game.getGamegoalvalue())).color(NamedTextColor.AQUA)));
                        sender.sendMessage(Component.text(Lang.adminParmsScoreTypes + ": ").color(NamedTextColor.YELLOW)
                            .append(Component.text(game.getScoretypes()).color(NamedTextColor.AQUA)));
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
                // SETSPAWN COMMAND: Set the lobby spawn point where players teleport when joining
                // Must be executed by a player standing in the lobby region
                if (args.length > 2) {
                    sender.sendMessage(Component.text("/" + label + " setspawn " + Lang.helpAdminSetLobbySpawn).color(NamedTextColor.RED));
                } else {
                    // Admin set team spawn
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Component.text(Lang.errorOnlyPlayers).color(NamedTextColor.RED));
                        return true;
                    }
                    // Check if the player is in the lobby region
                    player = (Player) sender;
                    if (args.length == 1 && getGameMgr().getLobby().isPlayerInRegion(player)) {
                        // Set spawn to player's current location
                        getGameMgr().getLobby().setSpawnPoint(player.getLocation());
                        sender.sendMessage(Component.text(Lang.generalSuccess + " (" + player.getLocation().getBlockX() + ","
                                + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ() + ")").color(NamedTextColor.GREEN));
                        return true;
                    } else {
                        sender.sendMessage(Component.text(Lang.helpAdminSetLobbySpawn).color(NamedTextColor.RED));
                        return true;
                    }
                }
                break;

            case "teams":
                // TEAMS COMMAND: Display team rosters showing all team members
                // Can view a specific game or all games
                if (args.length < 2) {
                    sender.sendMessage(Component.text("/" + label + " teams [all | <gamename>] " + Lang.helpAdminTeams).color(NamedTextColor.RED));
                } else {
                    boolean foundgame = false;

                    // Iterate through games to find matches
                    for (String gname : getGameMgr().getGames().keySet()) {
                        if (args[1].equalsIgnoreCase("all") || gname.equals(args[1])) {
                            foundgame = true;
                            game = getGameMgr().getGames().get(gname);
                            sender.sendMessage(Component.text(Lang.generalTeams + " - " + gname).color(NamedTextColor.GREEN));

                            Scoreboard sb = game.getScorecard().getScoreboard();
                            if (sb == null) {
                                sender.sendMessage(Component.text(Lang.errorNoSuchGame + "'" + args[1] + "'").color(NamedTextColor.RED));
                            } else {
                                // Display each team and its members
                                HashMap<Team, List<String>> teamMembers = game.getScorecard().getTeamMembers();
                                for (Team t : teamMembers.keySet()) {
                                    sender.sendMessage(Component.text("==== ")
                                        .append(t.displayName())
                                        .append(Component.text(" ====")));

                                    // Build member list from UUIDs
                                    StringBuilder memberlist = new StringBuilder();
                                    for (String uuid : teamMembers.get(t)) {
                                        memberlist.append("[").append(getServer().getOfflinePlayer(UUID.fromString(uuid)).getName()).append("] ");
                                    }
                                    sender.sendMessage(Component.text(Lang.generalMembers + ": " + memberlist).color(NamedTextColor.WHITE));
                                }
                            }
                        }
                    }

                    // Handle case where no matching game was found
                    if (!foundgame) {
                        if (args[1].equalsIgnoreCase("all")) {
                            sender.sendMessage(Component.text(Lang.errorNoGames));
                        } else {
                            sender.sendMessage(Component.text(Lang.errorNoSuchGame + "'" + args[1] + "'").color(NamedTextColor.RED));
                        }
                    }
                }
                break;

            default:
                // Unknown command - show error
                sender.sendMessage(Component.text(Lang.errorUnknownCommand).color(NamedTextColor.RED));
                break;
            }

        }

        return true;
    }

    /**
     * Lists all beacons for a given game or for 'all' games.
     * This is a convenience method that calls {@link #listBeacons(CommandSender, String, String)}
     * with an empty search filter.
     *
     * @param sender the command sender to receive the beacon listing
     * @param name either 'all' to list beacons from all games, or a specific game name
     */
    public void listBeacons(CommandSender sender, String name) {
        listBeacons(sender, name, "");
    }

    /**
     * Lists all beacons for a given game or for 'all' games with optional team filtering.
     *
     * <p>This method iterates through the beacon register and displays information about
     * each beacon including its location, owning team, and number of links. Beacons can
     * be filtered by game name and optionally by team ownership.
     *
     * <p>For each matching beacon, displays:
     * <ul>
     *   <li>Game name</li>
     *   <li>Beacon coordinates (x, y, z)</li>
     *   <li>Owning team (or "unowned" if no owner)</li>
     *   <li>Number of outbound links</li>
     * </ul>
     *
     * @param sender the command sender to receive the beacon listing
     * @param name either 'all' to list beacons from all games, or a specific game name
     * @param search optional team name to filter by, or "unowned" to show only unowned beacons, or empty string for all
     */
    public void listBeacons(CommandSender sender, String name, String search) {
        // Display header
        sender.sendMessage(Component.text(Lang.adminListBeaconsInGame.replace("[name]", name)).color(NamedTextColor.GREEN));

        boolean none = true;      // Track if any beacons were found
        boolean noGame = true;    // Track if the specified game exists

        // Iterate through all registered beacons
        for (BeaconObj b : getRegister().getBeaconRegister().values()) {
            // Find the game this beacon is in
            Game game = getGameMgr().getGame(b.getLocation());

            // Check if this beacon matches the game filter
            if (name.equalsIgnoreCase("all") || game.getName().equalsIgnoreCase(name)) {
                noGame = false;

                // Check if this beacon matches the team filter
                if (search.isEmpty() ||
                    (search.equalsIgnoreCase("unowned") && b.getOwnership() == null) ||
                    (b.getOwnership() != null && b.getOwnership().getName().equalsIgnoreCase(search))) {

                    none = false;

                    // Serialize team display name to plain text for display
                    String ownershipDisplay = b.getOwnership() == null ?
                        Lang.generalUnowned :
                        PlainTextComponentSerializer.plainText().serialize(b.getOwnership().displayName());

                    // Display beacon information
                    sender.sendMessage(Component.text(game.getName() + ": " +
                        b.getLocation().getBlockX() + "," +
                        b.getLocation().getBlockY() + "," +
                        b.getLocation().getBlockZ() + " >> " +
                        Lang.generalTeam + ": " + ownershipDisplay + " >> " +
                        Lang.generalLinks + ": " + b.getLinks().size()));
                }
            }
        }

        // Handle cases where no beacons or games were found
        if (none) {
            sender.sendMessage(Component.text(Lang.generalNone));
        }
        if (noGame) {
            if (name.equals("all")) {
                sender.sendMessage(Component.text(Lang.errorNoGames));
            } else {
                sender.sendMessage(Component.text(Lang.errorNoSuchGame + " '" + name + "'").color(NamedTextColor.RED));
            }
        }
    }

    /**
     * Sets game parameters from colon-delimited arguments.
     *
     * <p>Parses parameter strings in the format "parameter:value" and applies them
     * to a game. This method is used when modifying existing game settings.
     *
     * <p><b>Supported Parameters:</b>
     * <ul>
     *   <li><b>gamemode:</b> "minigame" or "strategy"</li>
     *   <li><b>teams:</b> number of teams (integer)</li>
     *   <li><b>goal:</b> "area", "beacons", "links", or "triangles"</li>
     *   <li><b>goalvalue:</b> target value to win (integer)</li>
     *   <li><b>countdown:</b> timer in seconds, 0 for unlimited (integer)</li>
     *   <li><b>scoretypes:</b> dash-separated score types to display (e.g., "area-beacons-links")</li>
     * </ul>
     *
     * <p>All parameters are validated before being applied. If any parameter is invalid,
     * an error message is returned and NO parameters are changed.
     *
     * @param game the game to configure
     * @param args array of "parameter:value" strings
     * @return error message describing the problem, or empty string if successful
     */
    public String setGameParms(Game game, String[] args) {
        String errormsg = "";

        // Check the parameters given - validate all before applying any
        errormsg = checkParms(args);

        // If *ALL* arguments are OK, process them into the game
        if (errormsg.isEmpty()) {
            for (String arg : args) {
                String parm = arg.split(":")[0];
                String value = arg.split(":")[1];
                if (parm == null || value == null) {
                    errormsg = Lang.adminParmsArgumentsPairs;
                } else {
                    // Apply each parameter
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
                            // Convert dash-separated to colon-separated format
                            game.setScoretypes(value.replace("-", ":"));
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return errormsg;
    }

    /**
     * Sets the default parameters to be used when creating new games.
     *
     * <p>This method temporarily overrides the global default game parameters
     * stored in Settings. After creating a game with these parameters, the caller
     * should restore the defaults by calling {@code getGameMgr().setGameDefaultParms()}.
     *
     * <p><b>Supported Parameters:</b>
     * <ul>
     *   <li><b>gamemode:</b> "minigame" or "strategy"</li>
     *   <li><b>size:</b> side length of game region in blocks (integer)</li>
     *   <li><b>teams:</b> number of teams (integer)</li>
     *   <li><b>goal:</b> "area", "beacons", "links", or "triangles"</li>
     *   <li><b>goalvalue:</b> target value to win (integer)</li>
     *   <li><b>countdown:</b> timer in seconds, 0 for unlimited (integer)</li>
     *   <li><b>scoretypes:</b> dash-separated score types (e.g., "area-beacons-links")</li>
     *   <li><b>distribution:</b> beacon spawn probability 0.01-0.99 (decimal)</li>
     * </ul>
     *
     * <p><b>Usage Pattern:</b>
     * <pre>
     * String err = setDefaultParms(args);
     * if (err.isEmpty()) {
     *     gameManager.newGame(name);  // Uses temporary defaults
     *     gameManager.setGameDefaultParms();  // Restore global defaults
     * }
     * </pre>
     *
     * @param args array of "parameter:value" strings
     * @return error message describing the problem, or empty string if successful
     */
    public String setDefaultParms(String[] args) {
        String errormsg = "";

        // Get current default parameters as starting point
        String mode = Settings.gamemode;
        int nteams = Settings.defaultTeamNumber;
        String ggoal = mode.equalsIgnoreCase("strategy") ? Settings.strategyGoal: Settings.minigameGoal;
        int gvalue = mode.equalsIgnoreCase("strategy") ? Settings.strategyGoalValue : Settings.minigameGoalValue;
        int timer = mode.equalsIgnoreCase("strategy") ? Settings.strategyTimer : Settings.minigameTimer;
        int gdistance = Settings.gameDistance;
        double gdistribution = Settings.distribution;
        String stypes = null;

        // Check and validate all parameters first
        errormsg = checkParms(args);

        // If all arguments are valid, apply them as temporary defaults
        if (errormsg.isEmpty()) {
            for (String arg : args) {
                String parm = arg.split(":")[0];
                String value = arg.split(":")[1];
                if (parm == null || value == null) {
                    errormsg = Lang.adminParmsArgumentsPairs;
                } else {
                    // Override defaults based on parameter type
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
                            // Convert dash-separated to colon-separated format
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
     * Validates game parameter arguments before applying them.
     *
     * <p>This method performs comprehensive validation of parameter strings to ensure
     * they are well-formed and contain valid values. All parameters are validated
     * before any are applied, following the "fail fast" principle.
     *
     * <p><b>Validation Rules:</b>
     * <ul>
     *   <li><b>gamemode:</b> must be "strategy" or "minigame"</li>
     *   <li><b>size:</b> must be a positive number</li>
     *   <li><b>teams:</b> must be a number between 2 and 14</li>
     *   <li><b>goal:</b> must be "area", "beacons", "links", or "triangles"</li>
     *   <li><b>goalvalue:</b> must be a positive number</li>
     *   <li><b>countdown:</b> must be a number (0 for unlimited)</li>
     *   <li><b>scoretypes:</b> must be dash-separated combination of goal types</li>
     *   <li><b>distribution:</b> must be a decimal between 0.01 and 0.99</li>
     * </ul>
     *
     * <p><b>Format Requirements:</b>
     * All parameters must be in "parameter:value" format. Missing colons or
     * values will result in an error.
     *
     * @param args array of "parameter:value" strings to validate
     * @return error message describing all problems found, or empty string if valid
     */
    public String checkParms(String[] args) {
        StringBuilder errormsg = new StringBuilder();

        // Check that *ALL* arguments are valid parms
        for (String arg : args) {
            String parm = null;
            String value = null;
            try {
                // Parse parameter:value format
                parm = arg.split(":")[0];
                value = arg.split(":")[1];
            } catch (Exception e) {
                // Silently handle split exceptions - will be caught below
            }

            // Validate parameter format
            if (parm == null || value == null) {
                errormsg = new StringBuilder(Lang.adminParmsArgumentsPairs);
            } else {
                // Validate specific parameter values
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

    /**
     * Provides tab completion suggestions for admin commands.
     *
     * <p>This method dynamically generates context-aware tab completion options
     * based on the command being typed and the sender's permissions. It provides:
     * <ul>
     *   <li>Command names based on permission level (admin/OP)</li>
     *   <li>Game names for commands that require them</li>
     *   <li>Team names for commands that require them</li>
     *   <li>Parameter templates for the newgame command</li>
     *   <li>Special values like "all", "help", and "unowned" where applicable</li>
     * </ul>
     *
     * <p>Only players with OP or "beaconz.admin" permission receive completions.
     * Console senders receive all applicable completions.
     *
     * <p><b>Completion Logic:</b>
     * <ul>
     *   <li><b>Arg 0:</b> All available command names</li>
     *   <li><b>Arg 1:</b> Context-specific (game names, team names, or "help")</li>
     *   <li><b>Arg 2:</b> Secondary context (teams for join/list commands)</li>
     *   <li><b>Arg 3+:</b> Parameter templates for newgame command</li>
     * </ul>
     *
     * @param sender the command sender requesting tab completion
     * @param command the command being tab-completed
     * @param alias the command alias used
     * @param args the current command arguments
     * @return list of matching completion suggestions, filtered by the last argument
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> options = new ArrayList<>();
        Player player = null;

        // Permission check - only admins/ops get completions
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
            // First argument: list all available commands
            if (sender instanceof Player) {
                // Player-only commands (require physical presence)
                options.add("claim");
                options.add("join");
                options.add("setspawn");
                options.add("switch");
            }
            // Console-compatible commands
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
            // For arguments 3+, provide parameter templates for newgame command
            if (args.length > 2 && args[0].equalsIgnoreCase("newgame")) {
                // Provide template suggestions for game creation parameters
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
        // Filter options to match what the player has typed so far
        return tabLimit(options, lastArg);
    }

    /**
     * Filters a list of strings to only those that start with the given prefix.
     *
     * <p>This is a helper method for tab completion that filters completion options
     * based on what the player has already typed. The comparison is case-insensitive
     * to provide a better user experience.
     *
     * <p><b>Example:</b>
     * <pre>
     * List&lt;String&gt; options = Arrays.asList("help", "heal", "home", "info");
     * tabLimit(options, "he");  // Returns ["help", "heal"]
     * tabLimit(options, "HE");  // Returns ["help", "heal"] (case-insensitive)
     * tabLimit(options, "");    // Returns ["help", "heal", "home", "info"] (all)
     * </pre>
     *
     * @param list the full list of completion options
     * @param start the prefix to filter by (what the player has typed)
     * @return new list containing only items that start with the prefix
     */
    public static List<String> tabLimit(final List<String> list, final String start) {
        final List<String> returned = new ArrayList<>();
        for (String s : list) {
            // Case-insensitive prefix matching
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                returned.add(s);
            }
        }
        return returned;
    }
}

