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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.commands.subcommands.HelpCommand;
import com.wasteofplastic.beaconz.commands.subcommands.JoinCommand;
import com.wasteofplastic.beaconz.commands.subcommands.LeaveCommand;
import com.wasteofplastic.beaconz.commands.subcommands.ScoreCommand;
import com.wasteofplastic.beaconz.commands.subcommands.ScoreboardCommand;
import com.wasteofplastic.beaconz.config.Lang;
import org.jspecify.annotations.NonNull;

/**
 * Main command handler for /beaconz commands.
 * Delegates to SubCommand implementations for each subcommand.
 */
public class CmdHandler extends BeaconzPluginDependent implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public CmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        registerSubCommands();
    }

    /**
     * Register all subcommands.
     */
    private void registerSubCommands() {
        registerSubCommand(new HelpCommand(getBeaconzPlugin()));
        registerSubCommand(new ScoreCommand(getBeaconzPlugin()));
        registerSubCommand(new ScoreboardCommand(getBeaconzPlugin()));
        registerSubCommand(new LeaveCommand(getBeaconzPlugin()));
        registerSubCommand(new JoinCommand(getBeaconzPlugin()));
    }

    /**
     * Register a subcommand.
     */
    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.errorOnlyPlayers);
            return true;
        }

        if (!player.hasPermission("beaconz.player")) {
            sender.sendMessage(Lang.errorYouDoNotHavePermission);
            return true;
        }

        // No arguments - teleport to lobby
        if (args.length == 0) {
            player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
            if (getGameMgr().getLobby() == null) {
                player.sendMessage(Lang.errorNoLobbyYet);
                return true;
            }
            getGameMgr().getLobby().tpToRegionSpawn(player, false);
            return true;
        }

        // Get the subcommand
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(Lang.errorUnknownCommand);
            return true; // Command was handled (error message sent)
        }

        // Check permission if required
        if (subCommand.getPermission() != null && !player.hasPermission(subCommand.getPermission())) {
            player.sendMessage(Lang.errorYouDoNotHavePermission);
            return true;
        }

        // Execute the subcommand (args without the subcommand name)
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);

        return subCommand.execute(sender, player, subArgs);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        final List<String> options = new ArrayList<>();
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");

        // First argument - list available subcommands
        if (args.length <= 1) {
            for (SubCommand subCommand : subCommands.values()) {
                // Only show commands the player has permission for
                if (subCommand.getPermission() == null || player.hasPermission(subCommand.getPermission())) {
                    options.add(subCommand.getName());
                }
            }
            return tabLimit(options, lastArg);
        }

        // Delegate to subcommand for further tab completion
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand != null) {
            // Check permission
            if (subCommand.getPermission() == null || player.hasPermission(subCommand.getPermission())) {
                // Get subcommand args (without the subcommand name)
                String[] subArgs = new String[args.length - 1];
                System.arraycopy(args, 1, subArgs, 0, args.length - 1);

                List<String> suggestions = subCommand.onTabComplete(sender, player, subArgs);
                return tabLimit(suggestions, lastArg);
            }
        }

        return new ArrayList<>();
    }

    /**
     * Returns all of the items that begin with the given start,
     * ignoring case. Intended for tab completion.
     *
     * @param list - list of items to check for matches
     * @param start - the start of the item to match, case insensitive
     * @return List of items that start with the letters
     */
    static List<String> tabLimit(final List<String> list, final String start) {
        final List<String> returned = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(start.toLowerCase())) {
                returned.add(s);
            }
        }

        return returned;
    }
}

