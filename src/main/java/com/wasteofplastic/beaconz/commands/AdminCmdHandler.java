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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
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
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Params;
import com.wasteofplastic.beaconz.Settings;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

    private Set<UUID> deleteConfirm = new HashSet<>();

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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Permission check: Only OPs or players with admin permission can use these commands
        if (sender instanceof Player player) {
            if (!player.isOp() && !player.hasPermission("beaconz.admin")) {
                sender.sendMessage(Lang.errorYouDoNotHavePermission.color(NamedTextColor.RED));
                return true;
            }
        }

        // Display help menu if no arguments or "help" command
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            return showHelp(sender, label);
        } else {
            // Process specific commands
            return switch (args[0].toLowerCase()) {
            case "claim" -> onClaim(sender, label, args);          
            case "distribution" -> onDistribution(sender, label, args);
            case "switch" -> onSwitch(sender, args);
            case "join" -> onJoin(sender, label, args);         
            case "games" -> onGames(sender);
            case "kick" -> onKick(sender, label, args);
            case "delete" -> onDelete(sender, label, args);
            case "force_end" -> onForceEnd(sender, label, args);
            case "list" -> onList(sender, label, args);
            case "newgame" -> onNewGame(sender, label, args);
            case "reload" -> onReload(sender);
            case "listparms" -> onListParms(sender, label, args);
            case "setspawn" -> onSetSpawn(sender, label, args);
            case "teams" ->  onTeams(sender, label, args);
            default -> {
                // Unknown command - show error
                sender.sendMessage(Lang.errorUnknownCommand.color(NamedTextColor.RED));
                yield false;
            }
            };
        }
    }

    /**
     * Handles the switch command to move a player between teams.
     *
     * <p>Switches a player to an alternative team in their current game.
     * If no arguments are provided, switches the command sender.
     * If a player name is provided, an admin can switch another player.
     * Clears all potion effects when a player switches teams.
     *
     * @param sender the command sender
     * @param args arguments: either empty (self) or player name (other player)
     * @return true if the switch was successful, false otherwise
     */
    private boolean onSwitch(CommandSender sender, String[] args) {
        // SWITCH COMMAND: Move a player to a different team within their current game
        // Can be used on self (1 arg) or on another player (2 args)
        if (args.length == 1) {
            // Switch sender's own team
            if (!(sender instanceof Player)) {
                sender.sendMessage(Lang.errorOnlyPlayers);
                return false;
            } else {
                Player player = (Player)sender;
                Team team = getGameMgr().getPlayerTeam(player);

                // Validate player is in a team
                if (team == null) {
                    sender.sendMessage(Lang.errorYouMustBeInATeam.color(NamedTextColor.RED));
                    return false;
                }

                // Get the game from current team
                Game game = getGameMgr().getGame(team);
                if (game == null) {
                    sender.sendMessage(Lang.errorYouMustBeInAGame.color(NamedTextColor.RED));
                    return false;
                }

                // Find next available team (first team that isn't the current one)
                for (Team newTeam : game.getScorecard().getTeams()) {
                    if (!newTeam.equals(team)) {
                        // Found an alternative team - switch to it
                        game.getScorecard().addTeamPlayer(newTeam, player);
                        sender.sendMessage(Lang.actionsSwitchedToTeam
                                .replaceText(builder -> builder.matchLiteral("[team]").replacement(newTeam.displayName()))
                                .color(NamedTextColor.GREEN));

                        // Clear all potion effects when switching teams
                        for (PotionEffect effect : player.getActivePotionEffects())
                            player.removePotionEffect(effect.getType());
                        return true;
                    }
                }
                sender.sendMessage(Lang.errorNoSuchTeam.color(NamedTextColor.RED));
            }
            return false;
        } else if (args.length == 2) {
            // Switch another player's team (admin forcing a switch)
            Player player = getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(Lang.errorUnknownPlayer);
                return false;
            } else {
                Team team = getGameMgr().getPlayerTeam(player);
                if (team == null) {
                    sender.sendMessage(Lang.errorNoSuchTeam.color(NamedTextColor.RED));
                    return false;
                }
                Game game = getGameMgr().getGame(team);
                if (game == null) {
                    sender.sendMessage(Lang.errorNoSuchGame.color(NamedTextColor.RED));
                    return false;
                }

                // Get the next team in this game
                for (Team newTeam : game.getScorecard().getTeams()) {
                    if (!newTeam.equals(team)) {
                        // Found an alternative - switch the player
                        game.getScorecard().addTeamPlayer(newTeam, player);

                        // Notify both admin and player
                        sender.sendMessage(Component.text(player.getName() + ": ")
                                .append(Lang.actionsSwitchedToTeam)
                                .replaceText(builder -> builder.matchLiteral("[team]").replacement(newTeam.displayName()))
                                .color(NamedTextColor.GREEN));
                        player.sendMessage(Lang.actionsSwitchedToTeam
                                .replaceText(builder -> builder.matchLiteral("[team]").replacement(newTeam.displayName()))
                                .color(NamedTextColor.GREEN));

                        // Remove any potion effects
                        for (PotionEffect effect : player.getActivePotionEffects())
                            player.removePotionEffect(effect.getType());
                        return true;
                    }
                }
                sender.sendMessage(Lang.errorNoSuchTeam.color(NamedTextColor.RED));
            }
            return false;
        }
        return false;
    }

    /**
     * Handles the join command to force a player into a specific team.
     *
     * <p>Forces the command sender to join a specified team in a specified game.
     * This is an admin bypass that doesn't respect normal join restrictions.
     * Sets the player's scoreboard and sends them to their team spawn.
     *
     * <p><b>Usage:</b> /admin join &lt;gamename&gt; &lt;team&gt;
     *
     * @param sender the command sender (must be a player)
     * @param label the command label
     * @param args arguments: [1] = game name, [2] = team name
     * @return true if the join was successful, false otherwise
     */
    private boolean onJoin(CommandSender sender, String label, String[] args) {
        // JOIN COMMAND: Force a player to join a specific team in a specific game
        // Admin bypass for normal join restrictions
        if (args.length < 3) {
            sender.sendMessage(Component.text("/" + label + " join <gamename> <team>" )
                    .append(Lang.helpAdminJoin).color(NamedTextColor.RED));
            return false;
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Lang.errorOnlyPlayers);
                return false;
            } else {
                Game game = getGameMgr().getGame(args[1]);
                // Validate game exists
                if (game != null) {
                    if (game.getScorecard().getTeam(args[2]) != null) {
                        // Team exists - add player to it
                        final Team joinTeam = game.getScorecard().getTeam(args[2]);
                        game.getScorecard().addTeamPlayer(joinTeam, player);
                        player.setScoreboard(game.getScorecard().getScoreboard());
                        game.getScorecard().sendPlayersHome(player, false);
                        sender.sendMessage(Lang.actionsYouAreInTeam
                                .replaceText(builder -> builder.matchLiteral("[team]").replacement(joinTeam.displayName()))
                                .color(NamedTextColor.GREEN));
                        return true;
                    }
                } else {
                    sender.sendMessage(Component.text("/" + label + " join <gamename> <team> - ").append(Lang.errorNoSuchGame));
                }
            }
        }
        return false;
    }

    /**
     * Handles the games command to list all active games.
     *
     * <p>Displays all currently active games with their region coordinates,
     * along with the lobby region. Useful for administrators monitoring
     * active game instances.
     *
     * @param sender the command sender to receive the game listing
     * @return always returns true
     */
    private boolean onGames(CommandSender sender) {
        // GAMES COMMAND: List all active games and their regions including the lobby
        sender.sendMessage(Lang.adminGamesDefined.color(NamedTextColor.GREEN));
        sender.sendMessage(Lang.adminGamesTheLobby.append(Component.text(" - " + getGameMgr().getLobby().displayCoords())).color(NamedTextColor.AQUA));

        // Count and display all games
        int cnt = 0;
        for (Game g : getGameMgr().getGames().values()) {
            cnt ++;
            sender.sendMessage(Component.text(g.getName() + " - " + g.getRegion().displayCoords()).color(NamedTextColor.AQUA));
        }
        if (cnt == 0) sender.sendMessage(Lang.adminGamesNoOthers.color(NamedTextColor.AQUA));
        return true;
    }

    /**
     * Handles the kick command to remove a player from a game.
     *
     * <p>Removes a player from a game and sends them to the lobby.
     * Can kick a specific player or all players from a game.
     *
     * <p><b>Usage:</b> /admin kick &lt;player|all&gt; &lt;gamename&gt;
     *
     * @param sender the command sender
     * @param label the command label
     * @param args arguments: [1] = player name or "all", [2] = game name
     * @return true if the kick was successful, false otherwise
     */
    private boolean onKick(CommandSender sender, String label, String[] args) {
        // KICK COMMAND: Remove a player from a game (sends them to lobby)
        if (args.length < 3) {
            sender.sendMessage(Component.text("/" + label).append(Lang.helpAdminKick.color(NamedTextColor.RED)));
            return false;
        } else {
            Player player = getServer().getPlayer(args[1]);

            // Check if player exists or if "all" was specified
            if (player == null && !args[1].equals("all")) {
                sender.sendMessage(Lang.errorUnknownPlayer);
                return false;
            } else {
                Game game = getGameMgr().getGame(args[2]);
                if (game == null) {
                    sender.sendMessage(Lang.errorNoSuchGame);
                    return false;
                } else {
                    // Process kick - either all players or specific player
                    if (args[2].equals("all")) {
                        game.kickAll();
                        sender.sendMessage(Lang.adminKickAllPlayers
                                .replaceText(builder -> builder.matchLiteral("[name]").replacement(game.getName())));
                        return true;
                    } else {
                        if (player != null) {
                            game.kick(sender, player);
                            sender.sendMessage(Lang.adminKickPlayer
                                    .replaceText(builder -> builder.matchLiteral("[player]").replacement(player.name()))
                                    .replaceText(builder -> builder.matchLiteral("[name]").replacement(game.getName())));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Handles the delete command to permanently remove a game.
     *
     * <p>Deletes a game and all its associated data and regions.
     * This operation cannot be undone. All players in the game are kicked.
     *
     * <p><b>Usage:</b> /admin delete &lt;gamename&gt;
     *
     * @param sender the command sender
     * @param label the command label
     * @param args arguments: [1] = game name to delete
     * @return true if deletion was successful, false otherwise
     */
    private boolean onDelete(CommandSender sender, String label, String[] args) {
        // DELETE COMMAND: Permanently delete a game and its region
        // WARNING: This cannot be undone!
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " delete <gamename> - ").append(Lang.helpAdminDelete).color(NamedTextColor.RED));
            return false;
        } else {
            Game game = getGameMgr().getGames().get(Component.text(args[1]));
            if (game == null) {
                sender.sendMessage(Lang.errorNoSuchGame.append(Component.text("'" + args[1] + "'")).color(NamedTextColor.RED));
                return false;
            } else {
                // Check if this has been entered twice
                UUID uuid = (sender instanceof Player player) ? player.getUniqueId() : null;
                if (!this.deleteConfirm.contains(uuid)) {
                    this.deleteConfirm.add(uuid);
                    Bukkit.getScheduler().runTaskLater(beaconzPlugin, () -> {
                        if (deleteConfirm.remove(uuid)) {
                            sender.sendMessage(Lang.errorRequestCanceled.color(NamedTextColor.RED));
                        }
                    }, 200L); // 10 seconds
                    sender.sendMessage(Lang.adminDeleteGameConfirm
                            .replaceText(builder -> builder.matchLiteral("[name]").replacement(game.getName()))
                            .color(NamedTextColor.LIGHT_PURPLE));
                    return false;
                }
                this.deleteConfirm.remove(uuid);

                // Confirm deletion started
                sender.sendMessage(Lang.adminDeletingGame
                        .replaceText(builder -> builder.matchLiteral("[name]").replacement(game.getName()))
                        .color(NamedTextColor.GREEN));
                getGameMgr().delete(sender, game);
                // Confirm deletion completed
                sender.sendMessage(Lang.adminDeletedGame
                        .replaceText(builder -> builder.matchLiteral("[name]").replacement(game.getName()))
                        .color(NamedTextColor.GREEN));
                return true;
            }
        }
    }

    /**
     * Handles the force_end command to immediately end a game.
     *
     * <p>Forces a game to end immediately, declares the winning team,
     * and cleans up the game. Useful for testing or ending stalled games.
     *
     * <p><b>Usage:</b> /admin force_end &lt;gamename&gt;
     *
     * @param sender the command sender
     * @param label the command label
     * @param args arguments: [1] = game name to force end
     * @return true if the command was successful, false otherwise
     */
    private boolean onForceEnd(CommandSender sender, String label, String[] args) {
        // FORCE_END COMMAND: Immediately end a game and declare a winner
        // Useful for testing or ending stalled games
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " force_end <gamename>").append(Lang.helpAdminForceEnd).color(NamedTextColor.RED));
            return false;
        } else {
            Component gameName = Component.text(args[1]);
            Game game = getGameMgr().getGames().get(gameName);
            if (game == null) {
                sender.sendMessage(Lang.errorNoSuchGame.append(Component.text("'" + args[1] + "'")).color(NamedTextColor.RED));
                return false;
            } else {
                game.forceEnd();
                sender.sendMessage(Lang.adminForceEnd
                        .replaceText(builder -> builder.matchLiteral("[name]").replacement(game.getName()))
                        .color(NamedTextColor.GREEN));
                return true;
            }
        }

    }

    /**
     * Handles the list command to display beacons in a game.
     *
     * <p>Lists all beacons in a specified game or all games.
     * Optionally filters by team ownership.
     *
     * <p><b>Usage:</b> /admin list [all|&lt;gamename&gt;] [team]
     *
     * @param sender the command sender to receive the beacon listing
     * @param label the command label
     * @param args arguments: [1] = game name or "all", [2] = optional team filter
     * @return true if the command was successful, false otherwise
     */
    private boolean onList(CommandSender sender, String label, String[] args) {
        // LIST COMMAND: Display all beacons in a game or across all games
        // Optional filter by team name or "unowned"
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " list [all |<gamename>] [team] ").append(Lang.helpAdminList.color(NamedTextColor.RED)));
            return false;
        } else if (args.length == 3) {
            // List with team filter
            listBeacons(sender, args[1], args[2]);
            return true;
        } else {
            // List all beacons in game
            listBeacons(sender, args[1]);
            return true;
        }
    }

    /**
     * Handles the newgame command to create a new game with optional parameters.
     *
     * <p>Creates a new game with optional custom parameters. Parameters override
     * the default game settings. Supports gamemode, size, teams, goal, goalvalue,
     * countdown, scoretypes, and distribution parameters.
     *
     * <p><b>Usage:</b> /admin newgame &lt;gamename&gt; [&lt;parm:value&gt; ...]
     * <p><b>Example:</b> /admin newgame MyGame gamemode:strategy teams:4 goal:links
     *
     * @param sender the command sender
     * @param label the command label
     * @param args arguments: [1] = game name, [2+] = optional parameters
     * @return true if the game was created successfully, false otherwise
     */
    private boolean onNewGame(CommandSender sender, String label, String[] args) {
        // NEWGAME COMMAND: Create a new game with optional custom parameters
        // Parameters can override defaults: gamemode, size, teams, goal, goalvalue, countdown, scoretypes, distribution
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...] - parameters are optional"));
            sender.sendMessage(Component.text("/" + label + " do /" + label + " newgame help for a list of the possible parameters"));
            return false;
        } else {
            if (args[1].equalsIgnoreCase("help")) {
                // Display detailed help for all game parameters
                sender.sendMessage(Component.text("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...]"));
                sender.sendMessage(Component.text("The optional parameters and their values are:").color(NamedTextColor.GREEN));

                // Document each parameter with examples
                sender.sendMessage(Component.text("gamemode -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text(" values can be either 'minigame' or 'strategy' - e.g gamemode:strategy")).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("size -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text(" length for the side of the game region - e.g. size:500")).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("teams -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text(" the number of teams in the game - e.g. teams:2")).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("goal -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text("  one of 'area', 'beacons', 'links', 'triangles' - e.g. goal:links")).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("goalvalue -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text("  the number objective for the goal - e.g goalvalue:100")).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("countdown -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text("  the game's timer, in seconds. 0 means the timer runs up, open-ended; any other value meands the timer runs a countdown from that time. - e.g. countdown:600")).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("scoretypes -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text("  the scores to be displayed on the sidebar. Can be any combination of goal names separated by '-' e.g scoretypes:area-triangles-beacons-links")).color(NamedTextColor.AQUA));
                sender.sendMessage(Component.text("distribution -  ").color(NamedTextColor.YELLOW)
                        .append(Component.text("  overrides the system's default beacon distribution - specify a number between 0.01 and 0.99 for the probability of any one chunk containing a beacon.")).color(NamedTextColor.AQUA));
                return true;
            } else {
                // Parse and create game with parameters
                String [] parmargs = new String [args.length-2];
                System.arraycopy(args, 2, parmargs, 0, parmargs.length);
                Game game = getGameMgr().getGame(args[1]);

                // Check if game name already exists
                if (game != null) {
                    sender.sendMessage(Lang.errorAlreadyExists
                            .replaceText(builder -> builder.matchLiteral("[name]").replacement(game.getName()))
                            .color(NamedTextColor.RED));
                    return false;
                } else {
                    // Check and validate parameters if provided
                    if (parmargs.length > 0) {
                        try {
                        Params params = new Params(parmargs);
                        sender.sendMessage(Lang.adminNewGameBuilding.color(NamedTextColor.GREEN));
                        getGameMgr().newGame(args[1]);           // create the new game
                        getGameMgr().setGameDefaultParms(params);
                        sender.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN));
                        return true;
                        } catch (IOException e) {
                            sender.sendMessage(Lang.errorError.append(Component.text(e.getMessage())).color(NamedTextColor.RED));
                            getGameMgr().setGameDefaultParms();      // restore the default parameters (just in case)
                            return false;
                        }
                     } else {
                        // Create game with default parameters
                        sender.sendMessage(Lang.adminNewGameBuilding.color(NamedTextColor.GREEN));
                        getGameMgr().newGame(args[1]);
                        sender.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN));
                        return true;
                    }
                }
            }
        }

    }

    /**
     * Handles the reload command to save and reload plugin configuration.
     *
     * <p>Saves all beacon registrations and game data, then reloads the
     * plugin configuration and all registered games. Useful after editing
     * configuration files or to recover from corrupted game state.
     *
     * @param sender the command sender
     * @return always returns true
     */
    private boolean onReload(CommandSender sender) {
        // RELOAD COMMAND: Save current state and reload all configuration
        // Saves: beacon register, game data
        // Reloads: config.yml, game parameters, beacon register
        getRegister().saveRegister();
        getGameMgr().saveAllGames();
        this.getBeaconzPlugin().reloadConfig();
        this.getBeaconzPlugin().loadConfig();
        getGameMgr().reload();
        getRegister().loadRegister();
        sender.sendMessage(Lang.adminReload.color(NamedTextColor.RED));
        return true;

    }

    /**
     * Handles the listparms command to display game parameters.
     *
     * <p>Shows detailed parameters for a specific game including gamemode,
     * number of teams, game goal, goal value, and score types displayed
     * on the scoreboard.
     *
     * <p><b>Usage:</b> /admin listparms &lt;gamename&gt;
     *
     * @param sender the command sender to receive the parameter listing
     * @param label the command label
     * @param args arguments: [1] = game name
     * @return true if successful, false if game not found
     */
    private boolean onListParms(CommandSender sender, String label, String[] args) {
        // LISTPARMS COMMAND: Display all parameters for a specific game
        // Shows: mode, teams, goal, goal value, score types
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " listparms <gamename> ").append(Lang.helpAdminListParms));
            return false;
        } else {
            Game game = getGameMgr().getGame(args[1]);
            if (game == null) {
                sender.sendMessage(Lang.errorNoSuchGame.append(Component.text("'" + args[1] + "'")).color(NamedTextColor.RED));
                return false;
            } else {
                // Display each game parameter with color formatting
                System.out.println(game.getGamemode().getName());
                sender.sendMessage(Lang.adminParmsMode.append(Component.text(": ")).color(NamedTextColor.YELLOW)
                        .append(Component.text(game.getGamemode().getName())).color(NamedTextColor.AQUA));
                sender.sendMessage(Lang.adminParmsTeams.append(Component.text(": ")).color(NamedTextColor.YELLOW)
                        .append(Component.text(String.valueOf(game.getNbrTeams()))).color(NamedTextColor.AQUA));
                sender.sendMessage(Lang.adminParmsGoal.append(Component.text(": ")).color(NamedTextColor.YELLOW)
                        .append(Component.text(game.getGamegoal().getName())).color(NamedTextColor.AQUA));
                sender.sendMessage(Lang.adminParmsGoalValue.append(Component.text(": ")).color(NamedTextColor.YELLOW)
                        .append(game.getGamegoalvalue() == 0 ? Lang.adminParmsUnlimited : Component.text(String.format(Locale.US, "%,d", game.getGamegoalvalue()))).color(NamedTextColor.AQUA));
                game.getScoretypes().forEach(goal -> 
                sender.sendMessage(Lang.adminParmsScoreTypes.append(Component.text(": ")).color(NamedTextColor.YELLOW)
                        .append(Component.text(goal.getName())).color(NamedTextColor.AQUA)));
                return true;
            }
        }


    }

    /**
     * Handles the setspawn command to set the lobby spawn point.
     *
     * <p>Sets the spawn point where players teleport when joining the lobby.
     * The command sender must be standing in the lobby region.
     *
     * <p><b>Usage:</b> /admin setspawn
     *
     * @param sender the command sender (must be a player in the lobby)
     * @param label the command label
     * @param args unused
     * @return true if spawn point was set, false if not in lobby region
     */
    private boolean onSetSpawn(CommandSender sender, String label, String[] args) {
        // SETSPAWN COMMAND: Set the lobby spawn point where players teleport when joining
        // Must be executed by a player standing in the lobby region
        if (args.length > 2) {
            sender.sendMessage(Component.text("/" + label + " setspawn ").append(Lang.helpAdminSetLobbySpawn).color(NamedTextColor.RED));
            return false;
        } else {
            // Admin set team spawn
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Lang.errorOnlyPlayers.color(NamedTextColor.RED));
                return false;
            }
            // Check if the player is in the lobby region
            if (args.length == 1 && getGameMgr().getLobby().isPlayerInRegion(player)) {
                // Set spawn to player's current location
                getGameMgr().getLobby().setSpawnPoint(player.getLocation());
                sender.sendMessage(Lang.generalSuccess.append(Component.text(" (" + player.getLocation().getBlockX() + ","
                        + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ() + ")")).color(NamedTextColor.GREEN));
                return true;
            } else {
                sender.sendMessage(Lang.helpAdminSetLobbySpawn.color(NamedTextColor.RED));
                return false;
            }
        }
    }

    /**
     * Handles the teams command to display team rosters.
     *
     * <p>Lists all teams in a specific game or all games, showing each team's
     * members. Useful for monitoring team composition and player assignments.
     *
     * <p><b>Usage:</b> /admin teams [all|&lt;gamename&gt;]
     *
     * @param sender the command sender to receive the team listing
     * @param label the command label
     * @param args arguments: [1] = "all" or specific game name
     * @return true if successful, false if no games found
     */
    private boolean onTeams(CommandSender sender, String label, String[] args) {
        // TEAMS COMMAND: Display team rosters showing all team members
        // Can view a specific game or all games
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " teams [all | <gamename>] ").append(Lang.helpAdminTeams).color(NamedTextColor.RED));
            return false;
        } else {
            boolean foundgame = false;

            // Iterate through games to find matches
            for (Entry<Component, Game> en : getGameMgr().getGames().entrySet()) {
                String gname = PlainTextComponentSerializer.plainText().serialize(en.getKey());
                if (args[1].equalsIgnoreCase("all") || gname.equals(args[1])) {
                    Game game = en.getValue();
                    sender.sendMessage(Lang.generalTeams.append(Component.text(" - " + gname).color(NamedTextColor.GREEN)));

                    Scoreboard sb = game.getScorecard().getScoreboard();
                    if (sb == null) {
                        sender.sendMessage(Lang.errorNoSuchGame.append(Component.text("'" + args[1] + "'").color(NamedTextColor.RED)));
                        return false;
                    } else {
                        // Display each team and its members
                        HashMap<Team, List<UUID>> teamMembers = game.getScorecard().getTeamMembers();
                        for (Team t : teamMembers.keySet()) {
                            sender.sendMessage(Component.text("==== ")
                                    .append(t.displayName())
                                    .append(Component.text(" ====")));

                            // Build member list from UUIDs
                            StringBuilder memberlist = new StringBuilder();
                            for (UUID uuid : teamMembers.get(t)) {
                                memberlist.append("[").append(getServer().getOfflinePlayer(uuid).getName()).append("] ");
                            }
                            sender.sendMessage(Lang.generalMembers.append(Component.text(": " + memberlist).color(NamedTextColor.WHITE)));
                        }
                        return true;
                    }
                }
            }

            // Handle case where no matching game was found
            if (!foundgame) {
                if (args[1].equalsIgnoreCase("all")) {
                    sender.sendMessage(Lang.errorNoGames);
                } else {
                    sender.sendMessage(Lang.errorNoSuchGame.append(Component.text("'" + args[1] + "'")).color(NamedTextColor.RED));
                }
            }
        }
        return false;
    }

    /**
     * Handles the distribution command to set beacon spawn probability.
     *
     * <p>Sets the probability (0.01 to 0.99) that any given chunk will contain
     * a beacon. This controls how densely beacons are distributed across the game region.
     *
     * <p><b>Usage:</b> /admin distribution &lt;value&gt;
     *
     * @param sender the command sender
     * @param label the command label
     * @param args arguments: [1] = distribution value (0.01 to 0.99)
     * @return true if the distribution was set successfully, false otherwise
     */
    private boolean onDistribution(CommandSender sender, String label, String[] args) {
        // DISTRIBUTION COMMAND: Set beacon spawn probability (0.0 to 1.0)
        // Controls how frequently beacons generate in chunks
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " distribution <fraction between 0 and 1> " + Lang.helpAdminDistribution).color(NamedTextColor.RED));
            return false;
        } else {
            try {
                double dist = Double.parseDouble(args[1]);
                if (dist > 0D && dist < 1D) {
                    Settings.distribution = dist;
                    sender.sendMessage(Lang.actionsDistributionSettingTo
                            .replaceText(builder -> builder.matchLiteral("[value]").replacement(Component.text(String.valueOf(dist))))
                            .color(NamedTextColor.GREEN));
                    return true;
                }
            } catch (Exception e) {
                sender.sendMessage(Component.text(label + " distribution <fraction> - must be less than 1").color(NamedTextColor.RED));
            }
        }
        return false;
    }

    /**
     * Handles the claim command to forcibly claim a beacon for a team.
     *
     * <p>Allows admins to claim or unclaim beacons for teams. The player must be
     * standing on the beacon block. Claimed beacons change color to match the team.
     *
     * <p><b>Usage:</b> /admin claim [unowned|&lt;team&gt;]
     *
     * @param sender the command sender (must be a player)
     * @param label the command label
     * @param args arguments: [1] = "unowned" or team name
     * @return true if claim was successful, false otherwise
     */
    private boolean onClaim(CommandSender sender, String label, String[] args) {
        // CLAIM COMMAND: Admin beacon claim - forcibly assign beacons to teams or mark as unowned
        // Requires player to be standing on the beacon block
        if (args.length < 2) {
            sender.sendMessage(Component.text("/" + label + " claim [unowned | <team>]").color(NamedTextColor.RED));
            return false;
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Lang.errorOnlyPlayers);
                return false;
            } else {
                // Check the argument
                Game game = getGameMgr().getGame(player.getLocation());
                Team team = game.getScorecard().getTeam(args[1]);
                if (team == null && !args[1].equalsIgnoreCase("unowned")) {
                    sender.sendMessage(Component.text("/" + label + " claim [unowned, " + game.getScorecard().getTeamListString() + "]").color(NamedTextColor.RED));
                    return false;
                } else {
                    // Check if player is standing on a beacon
                    Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                    if (!getRegister().isBeacon(block)) {
                        sender.sendMessage(Lang.errorYouHaveToBeStandingOnABeacon);
                        return false;
                    } else {
                        Point2D newClaim = new Point2D.Double(block.getX(), block.getZ());
                        player.sendMessage(Lang.beaconClaimingBeaconAt
                                .replaceText(builder -> builder.matchLiteral("[location]").replacement(Component.text(newClaim.toString()))));

                        // Verify beacon is registered
                        if (!getRegister().getBeaconRegister().containsKey(newClaim)) {
                            player.sendMessage(Lang.errorNotInRegister.append(Component.text(newClaim.toString())).color(NamedTextColor.RED));
                            return false;
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
                            player.sendMessage(Lang.beaconClaimedForTeam
                                    .replaceText(builder -> builder.matchLiteral("[team]").replacement(Component.text(args[1]))));
                            return true;
                        }
                    }
                }
            }
        }

    }

    /**
     * Displays the admin command help menu.
     *
     * <p>Shows a formatted list of all available admin commands with their syntax
     * and descriptions. Uses color-coded formatting for readability:
     * <ul>
     *   <li>GREEN for command names</li>
     *   <li>YELLOW for command syntax</li>
     *   <li>AQUA for command descriptions</li>
     * </ul>
     *
     * <p>Dynamically filters commands based on sender type (player vs. console).
     *
     * @param sender the command sender to receive the help menu
     * @param label the command label/alias used
     * @return always returns true
     */
    private boolean showHelp(CommandSender sender, String label) {
        // Define colors for help messages (GREEN for command, YELLOW for syntax, AQUA for description)
        NamedTextColor green = NamedTextColor.GREEN;
        NamedTextColor yellow = NamedTextColor.YELLOW;
        NamedTextColor aqua = NamedTextColor.AQUA;

        // Display help header
        sender.sendMessage(Lang.helpLine.color(green));
        sender.sendMessage(Lang.helpAdminTitle.color(yellow));
        sender.sendMessage(Lang.helpLine.color(green));

        // Player-only commands (require physical presence in game world)
        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" claim [unowned | <team>]").color(yellow))
                    .append(Lang.helpAdminClaim).color(aqua));
        }

        // Console-compatible commands
        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" delete <gamename>").color(yellow))
                .append(Lang.helpAdminDelete).color(aqua));

        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" join <gamename> <team>").color(yellow))
                    .append(Lang.helpAdminJoin).color(aqua));
        }

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" games").color(yellow))
                .append(Lang.helpAdminGames).color(aqua));

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" force_end <gamename>").color(yellow))
                .append(Lang.helpAdminForceEnd).color(aqua));

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" list [all |<gamename>] [team]").color(yellow))
                .append(Lang.helpAdminList).color(aqua));

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" listparms <gamename>").color(yellow))
                .append(Lang.helpAdminListParms).color(aqua));

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" newgame <gamename> [<parm1:value> <parm2:value>...]").color(yellow))
                .append(Lang.helpAdminNewGame
                        .replaceText(builder -> builder.matchLiteral("[label]").replacement(Component.text(label))))
                .color(aqua));

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" reload").color(yellow))
                .append(Lang.helpAdminReload).color(aqua));

        // Spawn-related commands (player-only)
        if (sender instanceof Player) {
            sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" setspawn <team>").color(yellow))
                    .append(Lang.helpAdminSetTeamSpawn).color(aqua));
            sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" setspawn ").color(yellow))
                    .append(Lang.helpAdminSetLobbySpawn).color(aqua));
            sender.sendMessage(Component.text("/" + label).color(green)
                    .append(Component.text(" switch ").color(yellow))
                    .append(Lang.helpAdminSwitch).color(aqua));
        }

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" switch <online playername> ").color(yellow))
                .append(Lang.helpAdminSwitch).color(aqua));

        sender.sendMessage(Component.text("/" + label).color(green)
                .append(Component.text(" teams [all | <gamename>]").color(yellow))
                .append(Lang.helpAdminTeams).color(aqua));
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
        sender.sendMessage(Lang.adminListBeaconsInGame
                .replaceText(builder -> builder.matchLiteral("[name]").replacement(Component.text(name)))
                .color(NamedTextColor.GREEN));

        boolean none = true;      // Track if any beacons were found
        boolean noGame = true;    // Track if the specified game exists

        // Iterate through all registered beacons
        for (BeaconObj b : getRegister().getBeaconRegister().values()) {
            // Find the game this beacon is in
            Game game = getGameMgr().getGame(b.getLocation());
            String gameName = PlainTextComponentSerializer.plainText().serialize(game.getName());
            // Check if this beacon matches the game filter
            if (name.equalsIgnoreCase("all") || gameName.equalsIgnoreCase(name)) {
                noGame = false;

                // Check if this beacon matches the team filter
                if (search.isEmpty() ||
                        (search.equalsIgnoreCase("unowned") && b.getOwnership() == null) ||
                        (b.getOwnership() != null && b.getOwnership().getName().equalsIgnoreCase(search))) {

                    none = false;

                    // Serialize team display name to plain text for display
                    Component ownershipDisplay = b.getOwnership() == null ?
                            Lang.generalUnowned : b.getOwnership().displayName();

                    // Display beacon information
                    sender.sendMessage(Component.text(game.getName() + ": " +
                            b.getLocation().getBlockX() + "," +
                            b.getLocation().getBlockY() + "," +
                            b.getLocation().getBlockZ() + " >> ")
                            .append(Lang.generalTeam)
                            .append(Component.text(": "))
                            .append(ownershipDisplay)
                            .append(Component.text(" >> "))
                            .append(Lang.generalLinks)
                            .append(Component.text(": " + b.getLinks().size())));
                }
            }
        }

        // Handle cases where no beacons or games were found
        if (none) {
            sender.sendMessage(Lang.generalNone);
        }
        if (noGame) {
            if (name.equals("all")) {
                sender.sendMessage(Lang.errorNoGames);
            } else {
                sender.sendMessage(Lang.errorNoSuchGame.append(Component.text(" '" + name + "'")).color(NamedTextColor.RED));
            }
        }
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
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
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
            options.add("regenerate");
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
            if (sender instanceof Player p) {
                // Add the player options
                game = getGameMgr().getGame(p.getLocation());
                if (args[0].equalsIgnoreCase("claim")) {
                    options.add("unowned");
                    options.addAll(game.getScorecard().getTeamsNames());
                }
                if (args[0].equalsIgnoreCase("join")) {
                    // List all the games
                    options.addAll(getGameMgr().getAllGameNames());
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
                options.addAll(getGameMgr().getAllGameNames());
            }

            if (args[0].equalsIgnoreCase("newgame")) {
                options.add("help");
            }
            // Options with "all"
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("teams") || args[0].equalsIgnoreCase("timertoggle")) {
                // List all the games
                options.add("all");
            }
            if (args[0].equalsIgnoreCase("setspawn") && game != null) {
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
            if (args[0].equalsIgnoreCase("list") && !args[1].equalsIgnoreCase("all")) {
                // List all the teams in the game
                game = getGameMgr().getGame(args[1]);
                if (game != null) {
                    options.addAll(game.getScorecard().getTeamsNames());
                }
                options.add("unowned");
            }
            // For arguments 3+, provide parameter templates for newgame command
            if (args[0].equalsIgnoreCase("newgame")) {
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



