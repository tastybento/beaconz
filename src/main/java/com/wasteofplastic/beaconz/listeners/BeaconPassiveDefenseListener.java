/*
 * Copyright (c) 2015 - 2016 tastybento
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

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Scorecard;
import com.wasteofplastic.beaconz.Settings;

/**
 * Handles beacon defenses
 * @author tastybento
 *
 */
public class BeaconPassiveDefenseListener extends BeaconzPluginDependent implements Listener {

    /**
     * Maximum distance squared an emerald block can be placed from the beacon
     */
    private static final double MAXDISTANCE = 64;

    /**
     * @param plugin
     */
    public BeaconPassiveDefenseListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Protects the beacon defenses from any damage
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getLocation().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Remove any blocks that are defensive
        Iterator<Block> it = event.blockList().iterator();
        while(it.hasNext()) {
            Block b = it.next();
            if (getRegister().isAboveBeacon(b.getLocation())) {
                // TODO: Check if it is the highest block
                it.remove();
            }
        }
    }

    /**
     * Prevents blocks from being piston pushed above a beacon or a piston being used to remove beacon blocks
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        for (Block b : event.getBlocks()) {
            Block whereItWillBe = b.getRelative(event.getDirection());
            if (getRegister().isAboveBeacon(whereItWillBe.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }


    /**
     * Prevents sticky piston damage
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        for (Block b : event.getBlocks()) {
            if (getRegister().isAboveBeacon(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Handles placing of blocks around a beacon
     * @param event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        //getLogger().info("DEBUG: This is a beacon");
        Player player = event.getPlayer();
        // Only Ops can break or place blocks in the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        // Get the player's team
        Scorecard sc = getGameMgr().getSC(player);
        if (sc == null || sc.getTeam(player) == null) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + Lang.errorYouMustBeInAGame);
                getGameMgr().getLobby().tpToRegionSpawn(player);
                return;
            } else {
                player.sendMessage(ChatColor.RED + Lang.errorYouMustBeInATeam);
                return;
            }
        }
        Team team = sc.getTeam(player);
        // Check if block is a beacon extension block
        Block block = event.getBlock();
        if (block.getType().equals(Material.EMERALD_BLOCK)) {
            // Check to see if it is being placed adjacent to a beacon
            Block adjBlock = block.getRelative(BlockFace.NORTH);
            BeaconObj adjacentBeacon = getRegister().getBeaconAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
            if (adjacentBeacon == null) {
                adjBlock = block.getRelative(BlockFace.SOUTH);
                adjacentBeacon = getRegister().getBeaconAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
                if (adjacentBeacon == null) {
                    adjBlock = block.getRelative(BlockFace.EAST);
                    adjacentBeacon = getRegister().getBeaconAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
                    if (adjacentBeacon == null) {
                        adjBlock = block.getRelative(BlockFace.WEST);
                        adjacentBeacon = getRegister().getBeaconAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
                    }
                }
            }
            if (adjacentBeacon != null) {
                // Check block is at the right height
                if (block.getY() + 1 == adjacentBeacon.getY()) {
                    // Check if the team is placing a block on their own beacon or not
                    if (adjacentBeacon.getOwnership() == null || !adjacentBeacon.getOwnership().equals(team)) {
                        player.sendMessage(ChatColor.RED + Lang.beaconYouCanOnlyExtend);
                        event.setCancelled(true);
                        return;
                    }
                    // Check the distance away from the main beacon
                    if (block.getLocation().distanceSquared(adjacentBeacon.getLocation()) > MAXDISTANCE) {
                        player.sendMessage(ChatColor.RED + Lang.beaconCannotBeExtended);
                        event.setCancelled(true);
                        return;
                    }
                    // Check what blocks are above the emerald block
                    int highestBlock = getHighestBlockYAt(block.getX(), block.getZ());
                    //getLogger().info("DEBUG: highest block y = " + highestBlock + " difference = " + (highestBlock - adjacentBeacon.getY()));
                    if (highestBlock > adjacentBeacon.getY()) {
                        event.getPlayer().sendMessage(ChatColor.RED + Lang.errorClearAboveBeacon);
                        event.setCancelled(true);
                        return;
                    }
                    // Extend beacon
                    getRegister().addBeaconDefenseBlock(block.getLocation(), adjacentBeacon);
                    player.sendMessage(ChatColor.GREEN + Lang.beaconExtended);
                    // TODO: give experience?
                    return;
                }
            }
        }

        // Check if the block is a defensive block
        BeaconObj beacon = getRegister().getBeaconAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null) {
            return;
        }
        // Check blocks below the beacon
        if (beacon.getY() > block.getY()) {
            return;
        }

        // Check if the team is placing a block on their own beacon or not
        if (!beacon.getOwnership().equals(team)) {
            player.sendMessage(ChatColor.RED + Lang.errorCanOnlyPlaceBlocks);
            event.setCancelled(true);
            return;
        }
        // Check if the height exceeds the allowed height
        if (beacon.getY() + Settings.defenseHeight - 1 < block.getY()) {
            player.sendMessage(ChatColor.RED + Lang.errorCanOnlyPlaceBlocksUpTo.replace("[value]", String.valueOf(Settings.defenseHeight)));
            event.setCancelled(true);
            return;
        }
        // Check if the player has the experience level required to place the block
        int level = block.getY() - beacon.getY();
        int levelRequired = 0;
        //getLogger().info("DEBUG: level = " + level);
        try {
            levelRequired = Settings.defenseLevels.get(level);
            if (player.getLevel() < levelRequired) {
                player.sendMessage(ChatColor.RED + Lang.errorYouNeedToBeLevel.replace("[value]",String.valueOf(levelRequired))); 
                event.setCancelled(true);
                return;
            }
        } catch (Exception e) {
            getLogger().severe("Defense level for height " + level + " does not exist!");
        }
        String levelPlaced = "";
        if (levelRequired > 0) {
            levelPlaced = " [" + Lang.level + " " + levelRequired + "]";
        }
        // Check what type of block it is
        MaterialData md = new MaterialData(event.getBlock().getType(), event.getBlock().getData());
        if (Settings.linkBlocks.containsKey(md)) {
            player.sendMessage(ChatColor.GREEN + Lang.linkBlockPlaced); 
        }
        // Send message
        String message = Lang.defenseText.get(event.getBlock().getType());
        if (message == null) {
            player.sendMessage(ChatColor.GREEN + Lang.defensePlaced + levelPlaced);
        } else {
            player.sendMessage(ChatColor.GREEN + message + levelPlaced);
        }
        // Handle dispensers
        //if (event.getBlock().getType().equals(Material.DISPENSER)) {
        beacon.addDefenseBlock(event.getBlock(), levelRequired);
        //}
    }

    /**
     * Handle breakage of the top part of a beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconBreak(BlockBreakEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        //getLogger().info("DEBUG: This is a beacon");
        Player player = event.getPlayer();
        // Only Ops can break or place blocks in the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        // Get the player's team
        Scorecard sc = getGameMgr().getSC(player);
        if (sc == null || sc.getTeam(player) == null) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + Lang.errorYouMustBeInAGame);
                getGameMgr().getLobby().tpToRegionSpawn(player);
                return;
            } else {
                player.sendMessage(ChatColor.RED + Lang.errorYouMustBeInATeam);
                return;
            }
        }
        Team team = sc.getTeam(player);
        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeaconAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null) {
            return;
        }
        // Check height
        if (block.getY() < beacon.getHeight()) {
            //getLogger().info("DEBUG: below beacon");
            return;
        }
        // Check if this block is a defense block
        if (!beacon.getDefenseBlocks().containsKey(event.getBlock())) {
            // No it is not
            return;
        }
        // If not same team, then blocks have to be removed in a specific way
        if (!team.equals(beacon.getOwnership())) {
            // Get the level
            int level = beacon.getDefenseBlocks().get(event.getBlock());
            // Check all blocks in the defense
            Iterator<Entry<Block, Integer>> it = beacon.getDefenseBlocks().entrySet().iterator();
            int highestBlock = 0;
            while (it.hasNext()) {
                Entry<Block,Integer> ent = it.next();
                if (player.getLevel() < ent.getValue()) {
                    player.sendMessage(ChatColor.RED + Lang.errorYouNeedToBeLevel.replace("[value]", String.valueOf(ent.getValue()))); 
                    event.setCancelled(true);
                    return;
                }
                highestBlock = Math.max(highestBlock, ent.getValue());
            }        
            // Check that breakage is being done top-down
            if (level < highestBlock) {
                event.getPlayer().sendMessage(ChatColor.RED + Lang.defenseRemoveTopDown);
                event.setCancelled(true);
                return;
            }
        }
        // The block is broken
        beacon.removeDefenseBlock(block);
         // Check if it was a link block
        MaterialData md = new MaterialData(block.getType(), block.getData());
        if (Settings.linkBlocks.containsKey(md)) {
            player.sendMessage(ChatColor.GOLD + Lang.linkBlockBroken);
            world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
            // Remove the longest link
            if (beacon.removeLongestLink()) {
                player.sendMessage(ChatColor.GOLD + Lang.linkLost);
            }
        }

    }

    /**
     * Handles damage to, but not breakage of blocks placed around a beacon.
     * Warns players to clear blocks top-down.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onDefenseDamage(BlockDamageEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        //getLogger().info("DEBUG: This is a beacon");
        Player player = event.getPlayer();
        // Only Ops can break or place blocks in the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        // Get the player's team
        Scorecard sc = getGameMgr().getSC(player);
        if (sc == null || sc.getTeam(player) == null) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + Lang.errorYouMustBeInAGame);
                getGameMgr().getLobby().tpToRegionSpawn(player);
                return;
            } else {
                player.sendMessage(ChatColor.RED + Lang.errorYouMustBeInATeam);
                return;
            }
        }
        Team team = sc.getTeam(player);
        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeaconAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null) {
            return;
        }
        // Check height
        if (block.getY() < beacon.getHeight()) {
            //getLogger().info("DEBUG: below beacon");
            return;
        }
        // If same team, then do nothing
        if (team.equals(beacon.getOwnership())) {
            return;
        }
        /*
        for (Block b : beacon.getDefenseBlocks().keySet()) {
            //getLogger().info("DEBUG: block " + b.getType() + " at " + b.getLocation().toVector());
        }
         */
        // Check if this block is a defense block
        if (!beacon.getDefenseBlocks().containsKey(event.getBlock())) {
            //getLogger().info("DEBUG: not a defense block");
            // No it is not
            return;
        }
        // Get the level
        int level = beacon.getDefenseBlocks().get(event.getBlock());
        //getLogger().info("DEBUG: block level is " + level);
        // Check all blocks in the defense
        Iterator<Entry<Block, Integer>> it = beacon.getDefenseBlocks().entrySet().iterator();
        int highestBlock = 0;
        while (it.hasNext()) {
            Entry<Block,Integer> ent = it.next();
            //getLogger().info("DEBUG: Checking block " + ent.getKey().getType() + " level " + ent.getValue());
            if (player.getLevel() < ent.getValue()) {
                player.sendMessage(ChatColor.RED + Lang.errorYouNeedToBeLevel.replace("[value]", String.valueOf(ent.getValue()))); 
                event.setCancelled(true);
                return;
            }
            highestBlock = Math.max(highestBlock, ent.getValue());
        }  
        //getLogger().info("DEBUG: highest block is " + highestBlock);
        // Check that breakage is being done top-down
        if (level < highestBlock) {
            event.getPlayer().sendMessage(ChatColor.RED + Lang.defenseRemoveTopDown);
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Gets the highest defense block in the world at x,z starting at the max height a defense block can be
     * @param block
     * @param y
     * @return
     */
    /*
    private int getHighestDefenseBlockYAt(Block block, int y) {
        while (y > 0 && getBeaconzWorld().getBlockAt(block.getX(), y, block.getZ()).getType().equals(Material.AIR)) {
            y--;
        };
        //getLogger().info("DEBUG: highest block is at " + y);
        return y;
    }
     */
    /**
     * Prevents the tipping of liquids over beacons
     * @param event
     */
    /*
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlockClicked().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        if (event.getBlockClicked().getY() == BLOCK_HEIGHT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot do that here!");
        }
    }
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockFlow(final BlockFromToEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!event.getBlock().isLiquid() || !world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        if (getRegister().isAboveBeacon(event.getToBlock().getLocation())) {
            event.setCancelled(true);
            //getLogger().info("DEBUG: stopping flow");
        }
    }

}
