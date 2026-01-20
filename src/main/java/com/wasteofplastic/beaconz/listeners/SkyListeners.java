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

package com.wasteofplastic.beaconz.listeners;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Lang;

/**
 * Class to protect the top layer of blocks in the sky (the glass lines) so that they can't be broken or
 * abused by players
 * @author tastybento
 *
 */
public class SkyListeners extends BeaconzPluginDependent implements Listener {

    private final static int BLOCK_HEIGHT = 255;
    /**
     * @param plugin
     */
    public SkyListeners(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Protects damage to blocks
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockDamage(BlockDamageEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp() && event.getBlock().getY() == BLOCK_HEIGHT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
        }
    }

    /**
     * Protects the sky blocks from explosion damage of any kind
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        World world = event.getEntity().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        event.blockList().removeIf(block -> block.getY() == BLOCK_HEIGHT);
    }

    /**
     * Prevents trees from growing into this space
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockSpread(BlockSpreadEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        if (event.getBlock().getY() == BLOCK_HEIGHT) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents blocks from being piston pushed into this height
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        // Only case about blocks being pushed up
        if (!event.getDirection().equals(BlockFace.UP)) {
            return;
        }
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        for (Block b : event.getBlocks()) {
            if (b.getRelative(event.getDirection()).getY() == BLOCK_HEIGHT) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents the tipping of liquids at this height
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        World world = event.getBlockClicked().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        if (event.getBlockClicked().getY() == BLOCK_HEIGHT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
        }
    }

    /**
     * Prevents placing of blocks at this height
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp() && (event.getBlock().getY() == BLOCK_HEIGHT || event.getBlockAgainst().getY() == BLOCK_HEIGHT)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
        }
    }


    /**
     * Prevents breakage of blocks at this height
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconBreak(BlockBreakEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isOp() && event.getBlock().getY() == BLOCK_HEIGHT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
        }
    }
}
