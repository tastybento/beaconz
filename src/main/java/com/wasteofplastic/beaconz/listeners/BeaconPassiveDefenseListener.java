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
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.DefenseBlock;
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
    private static final double MAXDISTANCESQRD = 64;

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
        // TODO: Check if it is the highest block
        event.blockList().removeIf(b -> getRegister().isAboveBeacon(b.getLocation()));
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
                getGameMgr().getLobby().tpToRegionSpawn(player, true);
                return;
            } else {
                player.sendMessage(ChatColor.RED + Lang.errorYouMustBeInATeam);
                return;
            }
        }

        Team team = sc.getTeam(player);
        Block block = event.getBlock();
        
        // Check to see if block is being placed adjacent to a beacon
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
        
        // Check if block is a beacon extension block
        if (block.getType().equals(Material.EMERALD_BLOCK)) {
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
                    if (block.getLocation().distanceSquared(adjacentBeacon.getLocation()) > MAXDISTANCESQRD) {
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
        
        // Check if the block is a beacon locking block
        Material lockingBlock = Material.EMERALD_BLOCK;
        if (Material.getMaterial(Settings.lockingBlock.toUpperCase()) != null) {
            lockingBlock = Material.getMaterial(Settings.lockingBlock.toUpperCase());                
        }
        if (block.getType().equals(lockingBlock)) {
            // Check if the team is placing a block on their own beacon 
            if (adjacentBeacon.getOwnership() != null && adjacentBeacon.getOwnership().equals(team)) {
                // Check if block was placed directly above the beacon's base
                if (Math.abs(block.getX()) - Math.abs(adjacentBeacon.getX()) < 2 && Math.abs(block.getZ()) - Math.abs(adjacentBeacon.getZ()) < 2) {
                    // Check if beacon is locked
                    int missing = adjacentBeacon.nbrToLock(block.getY());
                    if (missing == 0) {
                        player.sendMessage(ChatColor.YELLOW + Lang.beaconLockedJustNow.replace("[lockingBlock]", Settings.lockingBlock.toLowerCase()));                                
                    } else if (adjacentBeacon.isLocked()) {
                        player.sendMessage(ChatColor.YELLOW + Lang.beaconLockedAlready.replace("[lockingBlock]", Settings.lockingBlock.toLowerCase()));   
                    } else {
                        player.sendMessage(ChatColor.GREEN + Lang.beaconLockedWithNMoreBlocks.replace("[number]", missing + ""));
                    }                        
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
            levelPlaced = " [" + Lang.generalLevel + " " + levelRequired + "]";
        }
        // Check what type of block it is
        if (Settings.linkBlocks.containsKey(event.getBlock().getType())) {
            player.sendMessage(ChatColor.GREEN + Lang.beaconLinkBlockPlaced.replace("[range]", String.valueOf(Settings.linkBlocks.get(event.getBlock().getType())))); 
        }
        // Send message
        String message = Lang.defenseText.get(event.getBlock().getType());
        if (message == null) {
            player.sendMessage(ChatColor.GREEN + Lang.beaconDefensePlaced + levelPlaced);
        } else {
            player.sendMessage(ChatColor.GREEN + message + levelPlaced);
        }
        beacon.addDefenseBlock(event.getBlock(), levelRequired, player.getUniqueId());
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
                getGameMgr().getLobby().tpToRegionSpawn(player,true);
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
        DefenseBlock dBlock = beacon.getDefenseBlocks().get(event.getBlock());
        if (team.equals(beacon.getOwnership())) {
            // Check ownership
            if (dBlock.getPlacer() != null && !dBlock.getPlacer().equals(player.getUniqueId())) {
                if (Settings.removaldelta < 0) {
                    // Not your block
                    player.sendMessage(ChatColor.RED + Lang.errorYouCannotRemoveOtherPlayersBlocks);
                    event.setCancelled(true);
                } else if (Settings.removaldelta > 0 ) {
                    // Not your block
                    if (player.getLevel() < Settings.removaldelta + dBlock.getLevel()) {
                        player.sendMessage(ChatColor.RED + Lang.errorYouNeedToBeLevel.replace("[value]", String.valueOf(Settings.removaldelta + dBlock.getLevel()))); 
                        event.setCancelled(true); 
                    }
                }
            }            
        } else {
            // Get the highest defense block height
            int highestBlock = beacon.getHighestBlockLevel();
            
            // Check if beacon is locked
            if (beacon.isLocked()) {        
                player.sendMessage(ChatColor.YELLOW + Lang.beaconLocked);
                event.setCancelled(true);
                return;
            }

            // Check all blocks in the defense
            int level = dBlock.getLevel();
            
            if (player.getLevel() < highestBlock) {
                player.sendMessage(ChatColor.RED + Lang.errorYouNeedToBeLevel.replace("[value]", String.valueOf(highestBlock))); 
                event.setCancelled(true);
                return;
            }
            // Check that breakage is being done top-down
            if (level < highestBlock) {
                event.getPlayer().sendMessage(ChatColor.RED + Lang.beaconDefenseRemoveTopDown);
                event.setCancelled(true);
                return;
            }
        }

        // The block is broken
        beacon.removeDefenseBlock(block);        
               
        // Check if it was a link block
        if (Settings.linkBlocks.containsKey(block.getType())) {
            player.sendMessage(ChatColor.RED + Lang.beaconLinkBlockBroken.replace("[range]", String.valueOf(Settings.linkBlocks.get(event.getBlock().getType())))); 
            if (Settings.destroyLinkBlocks) {
                world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                block.setType(Material.AIR);
            }
            // Remove the longest link
            if (Settings.removeLongestLink && beacon.removeLongestLink()) {
                player.sendMessage(ChatColor.GOLD + Lang.beaconLinkLost);
                // Update score
                getGameMgr().getGame(team).getScorecard().refreshScores(team);
                getGameMgr().getGame(team).getScorecard().refreshSBdisplay(team);
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
                getGameMgr().getLobby().tpToRegionSpawn(player, true);
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
        /*
        if (team.equals(beacon.getOwnership())) {
            return;
        }*/
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
        // Check if it's a range extender that could be broken
        if (Settings.destroyLinkBlocks && Settings.linkBlocks.containsKey(event.getBlock().getType())) {
            // Player glass breaking sound
            world.playSound(event.getBlock().getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 2F);
            player.sendMessage(ChatColor.RED + Lang.beaconAmplifierBlocksCannotBeRecovered);
        }
          
        DefenseBlock dBlock = beacon.getDefenseBlocks().get(event.getBlock());
        if (team.equals(beacon.getOwnership())) {
            // Check ownership
            if (dBlock.getPlacer() != null && !dBlock.getPlacer().equals(player.getUniqueId())) {
                if (Settings.removaldelta < 0) {
                    // Not your block
                    player.sendMessage(ChatColor.RED + Lang.errorYouCannotRemoveOtherPlayersBlocks);
                    event.setCancelled(true); 
                } else if (Settings.removaldelta > 0 ) {
                    // Not your block
                    if (player.getLevel() < Settings.removaldelta + dBlock.getLevel()) {
                        player.sendMessage(ChatColor.RED + Lang.errorYouNeedToBeLevel.replace("[value]", String.valueOf(Settings.removaldelta + dBlock.getLevel()))); 
                        event.setCancelled(true);
                    }
                }
            }
            return;
        }
        // Get the level
        int level = dBlock.getLevel();
        // Check all blocks in the defense
        int highestBlock = 0;
        Iterator<Entry<Block, DefenseBlock>> it = beacon.getDefenseBlocks().entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, DefenseBlock> defenseBlock = it.next();
            if (defenseBlock.getKey().getType().equals(Material.AIR)) {
                // Clean up if any blocks have been removed by creatives, or moved due to gravity.
                it.remove();
            } else {
                highestBlock = Math.max(highestBlock, defenseBlock.getValue().getLevel());
            }
        }
        if (player.getLevel() < highestBlock) {
            player.sendMessage(ChatColor.RED + Lang.errorYouNeedToBeLevel.replace("[value]", String.valueOf(highestBlock))); 
            event.setCancelled(true);
            return;
        }
        //getLogger().info("DEBUG: highest block is " + highestBlock);
        // Check that breakage is being done top-down
        if (level < highestBlock) {
            event.getPlayer().sendMessage(ChatColor.RED + Lang.beaconDefenseRemoveTopDown);
            event.setCancelled(true);
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
