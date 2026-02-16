package com.wasteofplastic.beaconz.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Interface for sub-commands in the beaconz command system.
 */
public interface SubCommand {

    /**
     * Execute the sub-command.
     *
     * @param sender the command sender
     * @param player the player executing the command (if applicable)
     * @param args the command arguments (excluding the subcommand name itself)
     * @return true if the command was executed successfully
     */
    boolean execute(CommandSender sender, Player player, String[] args);

    /**
     * Get the name of this sub-command.
     *
     * @return the command name
     */
    String getName();

    /**
     * Get the permission required to execute this command.
     *
     * @return the permission string, or null if no permission is required
     */
    String getPermission();

    /**
     * Get tab completion suggestions for this sub-command.
     *
     * @param sender the command sender
     * @param player the player (if applicable)
     * @param args the current arguments
     * @return list of suggestions
     */
    List<String> onTabComplete(CommandSender sender, Player player, String[] args);
}

