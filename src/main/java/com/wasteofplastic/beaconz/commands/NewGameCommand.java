/*
 * Copyright (c) 2015 - 2026 tastybento
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Params;
import com.wasteofplastic.beaconz.game.Game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * Handles the newgame admin command to create new game instances.
 *
 * <p>This class manages the creation of new games with optional custom parameters.
 * Parameters can override defaults for gamemode, size, teams, goal, goalvalue,
 * countdown, scoretypes, and distribution.
 *
 * <p><b>Usage:</b> /admin newgame &lt;gamename&gt; [&lt;parm:value&gt; ...]
 * <p><b>Example:</b> /admin newgame MyGame gamemode:strategy teams:4 goal:links
 *
 * <p><b>Available Parameters:</b>
 * <ul>
 *   <li><b>gamemode</b> - 'minigame' or 'strategy' (e.g., gamemode:strategy)</li>
 *   <li><b>size</b> - Length for the side of the game region (e.g., size:500)</li>
 *   <li><b>teams</b> - Number of teams in the game (e.g., teams:2)</li>
 *   <li><b>goal</b> - One of 'area', 'beacons', 'links', 'triangles' (e.g., goal:links)</li>
 *   <li><b>goalvalue</b> - The number objective for the goal (e.g., goalvalue:100)</li>
 *   <li><b>countdown</b> - The game's timer in seconds; 0 means open-ended (e.g., countdown:600)</li>
 *   <li><b>scoretypes</b> - Scores to display on sidebar, separated by '-' (e.g., scoretypes:area-triangles-beacons-links)</li>
 *   <li><b>distribution</b> - Beacon distribution probability 0.01-0.99 (e.g., distribution:0.05)</li>
 * </ul>
 *
 * @author tastybento
 * @since 2.0
 */
public class NewGameCommand extends BeaconzPluginDependent {

    /**
     * Creates a new NewGameCommand handler.
     *
     * @param plugin the Beaconz plugin instance
     */
    public NewGameCommand(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Executes the newgame command to create a new game with optional parameters.
     *
     * <p>Creates a new game with optional custom parameters. Parameters override
     * the default game settings.
     *
     * @param sender the command sender
     * @param label the command label used
     * @param args command arguments: [0] = "newgame", [1] = game name, [2+] = optional parameters
     * @return true if the game was created successfully, false otherwise
     */
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            showUsage(sender, label);
            return false;
        }

        if (args[1].equalsIgnoreCase("help")) {
            showHelp(sender, label);
            return true;
        }

        return createGame(sender, args);
    }

    /**
     * Displays usage information for the newgame command.
     *
     * @param sender the command sender to receive the usage message
     * @param label the command label used
     */
    private void showUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...] - parameters are optional"));
        sender.sendMessage(Component.text("/" + label + " do /" + label + " newgame help for a list of the possible parameters"));
    }

    /**
     * Displays detailed help for all available game parameters.
     *
     * @param sender the command sender to receive the help information
     * @param label the command label used
     */
    private void showHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " newgame <gamename> [<parm1:value> <parm2:value>...]"));
        sender.sendMessage(Component.text("The optional parameters and their values are:"));

        // Document each parameter with examples
        sender.sendMessage(Component.text("gamemode -  ")
                .append(Component.text(" values can be either 'minigame' or 'strategy' - e.g gamemode:strategy")));
        sender.sendMessage(Component.text("size -  ")
                .append(Component.text(" length for the side of the game region - e.g. size:500")));
        sender.sendMessage(Component.text("teams -  ")
                .append(Component.text(" the number of teams in the game - e.g. teams:2")));
        sender.sendMessage(Component.text("goal -  ")
                .append(Component.text("  one of 'area', 'beacons', 'links', 'triangles' - e.g. goal:links")));
        sender.sendMessage(Component.text("goalvalue -  ")
                .append(Component.text("  the number objective for the goal - e.g goalvalue:100")));
        sender.sendMessage(Component.text("countdown -  ")
                .append(Component.text("  the game's timer, in seconds. 0 means the timer runs up, open-ended; any other value meands the timer runs a countdown from that time. - e.g. countdown:600")));
        sender.sendMessage(Component.text("scoretypes -  ")
                .append(Component.text("  the scores to be displayed on the sidebar. Can be any combination of goal names separated by '-' e.g scoretypes:area-triangles-beacons-links")));
        sender.sendMessage(Component.text("distribution -  ")
                .append(Component.text("  overrides the system's default beacon distribution - specify a number between 0.01 and 0.99 for the probability of any one chunk containing a beacon.")));
    }

    /**
     * Creates a new game with the specified name and optional parameters.
     *
     * @param sender the command sender
     * @param args command arguments containing game name and optional parameters
     * @return true if the game was created successfully, false otherwise
     */
    private boolean createGame(CommandSender sender, String[] args) {
        String gameName = args[1];
        Game game = getGameMgr().getGame(gameName);

        // Check if game name already exists
        if (game != null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(Lang.errorAlreadyExists,
                    Placeholder.component("name", game.getName())));
            return false;
        }

        // Parse optional parameters
        String[] parmargs = new String[args.length - 2];
        System.arraycopy(args, 2, parmargs, 0, parmargs.length);

        // Create game with parameters if provided
        if (parmargs.length > 0) {
            return createGameWithParameters(sender, gameName, parmargs);
        } else {
            return createGameWithDefaults(sender, gameName);
        }
    }

    /**
     * Creates a new game with custom parameters.
     *
     * @param sender the command sender
     * @param gameName the name for the new game
     * @param parmargs array of parameter strings in "key:value" format
     * @return true if successful, false if parameters are invalid
     */
    private boolean createGameWithParameters(CommandSender sender, String gameName, String[] parmargs) {
        try {
            Params params = new Params(parmargs);
            sender.sendMessage(Lang.adminNewGameBuilding);
            getGameMgr().newGame(gameName);
            getGameMgr().setGameDefaultParms(params);
            sender.sendMessage(Lang.generalSuccess);
            return true;
        } catch (IOException e) {
            sender.sendMessage(Lang.errorError.append(Component.text(e.getMessage())));
            getGameMgr().setGameDefaultParms(); // Restore default parameters
            return false;
        }
    }

    /**
     * Creates a new game with default parameters.
     *
     * @param sender the command sender
     * @param gameName the name for the new game
     * @return true (always successful)
     */
    private boolean createGameWithDefaults(CommandSender sender, String gameName) {
        sender.sendMessage(Lang.adminNewGameBuilding);
        getGameMgr().newGame(gameName);
        sender.sendMessage(Lang.generalSuccess);
        return true;
    }

    /**
     * Provides tab completion suggestions for the newgame command.
     *
     * <p>Suggestions include:
     * <ul>
     *   <li>Argument 1: "help"</li>
     *   <li>Arguments 2+: Parameter templates (gamemode:, teams:, goal:, etc.)</li>
     * </ul>
     *
     * @param args the command arguments typed so far
     * @return list of tab completion suggestions
     */
    public List<String> tabComplete(String[] args) {
        List<String> options = new ArrayList<>();

        if (args.length == 2) {
            // Suggest "help" for the game name argument
            options.add("help");
        } else if (args.length >= 3) {
            // Provide parameter templates for game creation
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

        return options;
    }
}
