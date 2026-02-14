package com.wasteofplastic.beaconz.commands.subcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.commands.SubCommand;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.game.Game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Handles the /beaconz leave <game> command.
 */
public class LeaveCommand extends BeaconzPluginDependent implements SubCommand {

    public LeaveCommand(Beaconz plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, Player player, String[] args) {
        if (player == null) {
            sender.sendMessage(Lang.errorOnlyPlayers);
            return true;
        }

        if (!player.hasPermission(getPermission())) {
            player.sendMessage(Lang.errorYouDoNotHavePermission);
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("/beaconz leave <game> ").append(Lang.helpLeave));
            return true;
        }

        Component gamename = Component.text(args[0]);
        Game game = getGameMgr().getGame(gamename);
        if (game == null) {
            sender.sendMessage(Lang.errorNoSuchGame.append(Component.text(" '")).append(gamename).append(Component.text("'")));
        } else {
            game.leave(player);
        }
        return true;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getPermission() {
        return "beaconz.player.leave";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Player player, String[] args) {
        List<String> options = new ArrayList<>();

        if (player == null || !player.hasPermission(getPermission())) {
            return options;
        }

        if (args.length == 1) {
            // List all the games this player is in
            for (Game game : getGameMgr().getGames().values()) {
                if (game.getScorecard().inTeam(player)) {
                    String plainText = PlainTextComponentSerializer.plainText().serialize(game.getName());
                    options.add(plainText);
                }
            }
        }

        return options;
    }
}

