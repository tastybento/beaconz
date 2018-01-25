/*
 * Copyright (c) 2015 - 2016
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

package com.wasteofplastic.beaconz;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.beaconz.listeners.PlayerMovementListener;

/**
 * Base class for classes that depend on a {@link org.bukkit.plugin.Plugin}.
 *
 * Delegates the often-used methods to the plugin so subclasses don't have to refer to the field when
 * logging, getting the register, etc.
 *
 * Sadly this cannot be used universally as some classes (BeaconMap, BeaconPopulator) need to extend existing
 * classes and Java does not (yet?) support multiple inheritance.
 */
public abstract class BeaconzPluginDependent {
    private final Beaconz beaconzPlugin;

    public final Beaconz getBeaconzPlugin() {
        return beaconzPlugin;
    }

    public BeaconzPluginDependent(Beaconz beaconzPlugin) {
        this.beaconzPlugin = beaconzPlugin;
    }

    public final Register getRegister() {
        return this.beaconzPlugin.getRegister();
    }

    public final GameMgr getGameMgr() {
        return this.beaconzPlugin.getGameMgr();
    }

    public final Logger getLogger() {
        return this.beaconzPlugin.getLogger();
    }

    public final BlockPopulator getBlockPopulator() {
        return this.beaconzPlugin.getBp();
    }

    public final World getBeaconzWorld() {
        return this.beaconzPlugin.getBeaconzWorld();
    }

    public final Server getServer() {
        return this.beaconzPlugin.getServer();
    }

    public final File getDataFolder() {
        return this.beaconzPlugin.getDataFolder();
    }

    public final Messages getMessages() {
        return this.beaconzPlugin.getMessages();
    }

    public final Boolean senderMsg(CommandSender sender, String msg) {
        return this.beaconzPlugin.senderMsg(sender, msg);
    }

    /**
     * Gets the highest block in the world at x,z starting at the max height block can be
     * @param x
     * @param z
     * @return height of first non-air block
     */
    public final int getHighestBlockYAt(int x, int z) {
        return this.beaconzPlugin.getHighestBlockYAt(x, z);
    }

    /**
     * @return the inventory swap object
     */
    public final BeaconzStore getBeaconzStore() {
        return this.beaconzPlugin.getBeaconzStore();
    }
    
    /**
     * Runs commands for a player or on a player
     * @param player
     * @param commands
     */
    public void runCommands(Player player, List<String> commands) {
        this.beaconzPlugin.runCommands(player, commands);
    }
    
    /**
     * Gives player item rewards
     * @param player
     * @param itemRewards
     * @return a list of what was given to the player
     */
    public List<ItemStack> giveItems(Player player, List<String> itemRewards) {
        return this.beaconzPlugin.giveItems(player, itemRewards);
    }
    
    /**
     * Get the PlayerMovementListener object
     * @return PlayerMovementListner object
     */
    public PlayerMovementListener getPml() {
        return this.beaconzPlugin.getPml();
    }
}
