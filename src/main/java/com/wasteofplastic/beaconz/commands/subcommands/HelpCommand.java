package com.wasteofplastic.beaconz.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.commands.SubCommand;
import com.wasteofplastic.beaconz.config.Lang;

import net.kyori.adventure.text.Component;

/**
 * Handles the /beaconz help command.
 */
public class HelpCommand extends BeaconzPluginDependent implements SubCommand {

    public HelpCommand(Beaconz plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, Player player, String[] args) {
        sender.sendMessage(Component.text("/beaconz help ").append(Lang.helpHelp));
        if (player != null && player.hasPermission("beaconz.player.leave")) {
            sender.sendMessage(Component.text("/beaconz leave <game> ").append(Lang.helpLeave));
        }
        sender.sendMessage(Component.text("/beaconz score ").append(Lang.helpScore));
        sender.sendMessage(Component.text("/beaconz sb ").append(Lang.helpScoreboard));
        return true;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getPermission() {
        return null; // No special permission required
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Player player, String[] args) {
        return new ArrayList<>();
    }
}

