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

import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Dispenser;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Settings;

public class BeaconProtectionListener extends BeaconzPluginDependent implements Listener {

    private final static boolean DEBUG = false;
    /**
     * A bi-drectional hashmap to track players standing on beaconz
     */
    private static final BiMap<UUID, BeaconObj> standingOn = HashBiMap.create();

    public BeaconProtectionListener(Beaconz plugin) {
        super(plugin);
        // Work out if players are on beacons or not
        standingOn.clear();
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(getBeaconzWorld())) {
                BeaconObj beacon = getRegister().getBeaconAt(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
                if (beacon != null) {
                    // Add players to beacon standing
                    if (player.getLocation().getBlockY() >= beacon.getY() && (player.getLocation().getBlockY() < beacon.getY() + Settings.defenseHeight)) {
                        standingOn.put(player.getUniqueId(), beacon);
                    }
                }
            }
        }
        // Run a repeating task to move people off the beacon beam
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entry<UUID, BeaconObj> entry : standingOn.entrySet()) {
                    Player player = getServer().getPlayer(entry.getKey());
                    if (player != null && player.isOnline() && player.getWorld().equals(getBeaconzWorld()) && !getGameMgr().isPlayerInLobby(player)
                            && player.getLocation().getBlockY() > entry.getValue().getY() && player.getLocation().getBlockY() < entry.getValue().getY() + Settings.defenseHeight) {
                        // Do something
                        Random rand = new Random();
                        player.setVelocity(new Vector(rand.nextGaussian(), 1.2, rand.nextGaussian()));
                        getBeaconzWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1F, 1F);
                    }
                }
            }
        }.runTaskTimer(getBeaconzPlugin(), 0L, 20L);


    }

    /**
     * Handles damage to, but not breakage of a beacon. Warns players to clear a beacon before
     * capture can occur. See block break event for capture.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
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
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }

        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return;
        }
        // Check for obsidian/glass breakage - i.e., capture
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            // Check if this is a real beacon
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                // It is a real beacon
                // Check that the beacon is clear of blocks
                if (beacon.isNotClear() && (beacon.getOwnership() == null || !beacon.getOwnership().equals(team))) {
                    // You can't capture an uncleared beacon
                    player.sendMessage(Lang.errorClearAroundBeacon);
                    event.setCancelled(true);
                }
            }
        } else {
            // Attempt to break another part of the beacon
        }

    }

    /**
     * Protects the underlying beacon from any damage
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getLocation().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Check if the block is a beacon or the surrounding pyramid and remove it from the damaged blocks
        event.blockList().removeIf(block -> getRegister().isBeacon(block));
    }

    /**
     * Prevents LOGS from growing above the beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockSpread(BlockSpreadEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlock().getX(),event.getBlock().getZ());
        if (beacon != null && beacon.getY() < event.getBlock().getY()) {
            // TODO if (!Tag.LEAVES.isTagged(event.getBlock().getType())) {
                // For everything else, make sure there is air
                event.getBlock().setType(Material.AIR);
           // }
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
            return;
        }
        for (Block b : event.getBlocks()) {
            // If any block is part of a beacon cancel it
            if (getRegister().isBeacon(b)) {
                event.setCancelled(true);
                return;
            }
            Block testBlock = b.getRelative(event.getDirection());
            BeaconObj beacon = getRegister().getBeaconAt(testBlock.getX(),testBlock.getZ());
            if (beacon != null && beacon.getY() < testBlock.getY()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents blocks from being pulled off beacons by sticky pistons
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        for (Block b : event.getBlocks()) {
            // If any block is part of a beacon cancel it
            if (getRegister().isBeacon(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents the tipping of liquids over the beacon using a bucket
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlockClicked().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Block b = event.getBlockClicked().getRelative(event.getBlockFace());
        BeaconObj beacon = getRegister().getBeaconAt(b.getX(),b.getZ());
        if (beacon != null && beacon.getY() <= event.getBlockClicked().getY()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.beaconCannotPlaceLiquids);
        }
        if (getRegister().isAboveBeacon(b.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.beaconCannotPlaceLiquids);
        }
    }

    /**
     * Prevents the tipping of liquids over the beacon using a dispenser
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onDispense(final BlockDispenseEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getItem().getType());
        if (!event.getItem().getType().equals(Material.WATER_BUCKET) && !event.getItem().getType().equals(Material.LAVA_BUCKET)) {
            return;
        }
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getBlock().getType());
        if (!event.getBlock().getType().equals(Material.DISPENSER)) {
            return;
        }
        Dispenser dispenser = (Dispenser)event.getBlock().getState().getData();
        Block b = event.getBlock().getRelative(dispenser.getFacing());
        if (DEBUG)
            getLogger().info("DEBUG: " + b.getLocation());
        if (getRegister().isAboveBeacon(b.getLocation())) {
            world.playSound(b.getLocation(), Sound.BLOCK_STONE_BREAK, 1F, 2F);
            event.setCancelled(true);
        }
    }

    /**
     * Prevents liquid flowing over the beacon beam
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLiquidFlow(final BlockFromToEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Only bother with horizontal flows
        if (event.getToBlock().getX() != event.getBlock().getX() || event.getToBlock().getZ() != event.getBlock().getZ()) {
            BeaconObj beacon = getRegister().getBeaconAt(event.getToBlock().getX(),event.getToBlock().getZ());
            if (beacon != null && beacon.getY() < event.getToBlock().getY()) {
                event.setCancelled(true);
                return;
            }
            // Stop flows outside of the game area
            Game game = getGameMgr().getGame(event.getBlock().getLocation());
            if (game == null) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles placing of blocks around a beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        
        Player player = event.getPlayer();
        // Only Ops place blocks in the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        
        // Actually, ops can break blocks anywhere
        if (player.isOp()) {
            return;
        }
        
        // Check for placing blocks outside the play area
        // TODO: This assumes that adjacent games will always be greater than 5 blocks away
        Game game = getGameMgr().getGame(event.getBlock().getLocation());
        if (game == null) {
            event.setCancelled(true);
            player.sendMessage(Lang.errorYouCannotDoThat);
            return;
        }

        // Get the player's team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        // Stop placing blocks on a beacon
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlock().getX(),event.getBlock().getZ());
        if (beacon != null && beacon.getY() < event.getBlock().getY()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.errorYouCannotBuildThere);
        }
    }

    /**
     * Projects animals on beacons
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        //getLogger().info("DEBUG: " + event.getEventName());
        if (!(event.getEntity() instanceof Animals)) {
            return;
        }
        //getLogger().info("DEBUG: animals being hurt by " + event.getDamager());
        // Check beacon defense  

        BeaconObj beacon = getRegister().getBeaconAt(event.getEntity().getLocation());
        if (beacon != null && beacon.getOwnership() != null) {
            //getLogger().info("DEBUG: beacon found");
            if (event.getDamager() instanceof Player player) {
                //getLogger().info("DEBUG: damager is player");
                // Prevent enemies from hurting animals on beacons
                Game game = getGameMgr().getGame(player.getLocation());
                if (game != null) {
                    Team team = game.getScorecard().getTeam(player); 
                    if (!beacon.getOwnership().equals(team)) {
                        player.sendMessage(Lang.triangleThisBelongsTo.replaceText("[team]", beacon.getOwnership().displayName()));
                        event.setCancelled(true);
                    }
                }          
            } else {
                //getLogger().info("DEBUG: Damager is not player");
                event.setCancelled(true);
            }
        }
    }

    /**
     * Protects animals on a beacon from damage
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        World world = event.getEntity().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        //getLogger().info("DEBUG: " + event.getEntityType());
        if (!(event.getEntity() instanceof Animals || event.getEntity() instanceof LeashHitch)) {
            return;
        }
        // Check beacon defense            
        BeaconObj beacon = getRegister().getBeaconAt(event.getEntity().getLocation());
        if (beacon != null) {                
            event.setCancelled(true);
        }
    }

    /**
     * Projects all chests inside triangles or on beacons
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        //getLogger().info("DEBUG: inventory type = " + event.getInventory().getType());
        World world = event.getPlayer().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Check what type of inventory this is
        if (event.getInventory().getType().equals(InventoryType.PLAYER)) {
            return;
        }
        InventoryHolder invHolder = event.getInventory().getHolder();
        Location invLoc = null;
        if (invHolder instanceof Horse) {
            invLoc = ((Horse) invHolder).getLocation();
        } else if (invHolder instanceof Minecart) {
            invLoc = ((Minecart) invHolder).getLocation();
        } else {
            invLoc = event.getInventory().getLocation();
        }
        if (invLoc == null) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Game game = getGameMgr().getGame(player.getLocation());
        if (game != null) {
            Team team = game.getScorecard().getTeam(player);         
            // Check beacon defense            
            BeaconObj beacon = getRegister().getBeaconAt(invLoc);
            if (beacon != null) {
                if (!beacon.getOwnership().equals(team)) {
                    player.sendMessage(Lang.triangleThisBelongsTo.replaceText("[team]", beacon.getOwnership().displayName()));
                    event.setCancelled(true);
                }
            }
            // Check triangle
            /*
            for (TriangleField triangle : getRegister().getTriangle(invLoc.getBlockX(), invLoc.getBlockZ())) {
                if (!triangle.getOwner().equals(team)) {
                    player.sendMessage(Lang.triangleThisBelongsTo.replace("[team]", triangle.getOwner().getDisplayName()));
                    event.setCancelled(true);
                    return;
                }
            }*/
        }
    }

    /**
     * @return the standingOn
     */
    public static BiMap<UUID, BeaconObj> getStandingOn() {
        return standingOn;
    }
    
}
