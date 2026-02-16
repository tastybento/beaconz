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

/**
 * Handles the /beaconz join <game> command (undocumented - for admins only).
 */
public class JoinCommand extends BeaconzPluginDependent implements SubCommand {

    public JoinCommand(Beaconz plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(CommandSender sender, Player player, String[] args) {
        if (player == null) {
            sender.sendMessage(Lang.errorOnlyPlayers);
            return true;
        }

        if (!player.isOp()) {
            // Command handled, but not allowed for non-ops (undocumented feature)
            return true;
        }

        if (args.length == 0) {
            return true;
        }

        Component gamename = Component.text(args[0]);
        Game game = getGameMgr().getGame(gamename);
        if (game == null) {
            sender.sendMessage(Lang.errorNoSuchGame.append(Component.text(" '")).append(gamename).append(Component.text("'")));
        } else {
            game.join(player);
        }
        return true;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getPermission() {
        return null; // Op-only, checked manually
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Player player, String[] args) {
        return new ArrayList<>();
    }
}

