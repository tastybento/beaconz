/*
 * Copyright (c) 2015 tastybento
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

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.material.Dispenser;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.LinkResult;
import com.wasteofplastic.beaconz.Region;
import com.wasteofplastic.beaconz.Settings;
import com.wasteofplastic.beaconz.TriangleField;
import com.wasteofplastic.beaconz.map.BeaconMap;
import com.wasteofplastic.beaconz.map.TerritoryMapRenderer;

public class BeaconListeners extends BeaconzPluginDependent implements Listener {

    /**
     * A bi-drectional hashmap to track players standing on beaconz
     */
    private BiMap<UUID, BeaconObj> standingOn = HashBiMap.create();
    private HashMap<UUID, Collection<PotionEffect>> triangleEffects = new HashMap<UUID, Collection<PotionEffect>>();
    private HashMap<UUID, Location> deadPlayers = new HashMap<UUID,Location>();
    private Set<UUID> barrierPlayers = new HashSet<UUID>();
    private final static boolean DEBUG = false;

    public BeaconListeners(Beaconz plugin) {
        super(plugin);
        // Work out if players are on beacons or not
        standingOn.clear();
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(getBeaconzWorld())) {
                BeaconObj beacon = getRegister().getBeaconAt(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
                if (beacon != null) {
                    // Add players to beacon standing
                    standingOn.put(player.getUniqueId(), beacon);
                }
            }
        }
        // Run a repeating task to apply effects
        // Run a task to continuously apply effects if they exist
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player: getServer().getOnlinePlayers()) {
                    // Only apply if in the world, not in the lobby and if there's an affect to apply
                    if (player.getWorld().equals(getBeaconzWorld()) && triangleEffects.containsKey(player.getUniqueId())
                            && !getGameMgr().isPlayerInLobby(player)) {
                        player.addPotionEffects(triangleEffects.get(player.getUniqueId()));
                    }
                }
            }
        }.runTaskTimer(getBeaconzPlugin(), 0L, 20L);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInit(WorldInitEvent event)
    {
        //Bukkit.getLogger().info("On World Init called");
        if (event.getWorld().equals(getBeaconzWorld())) {
            if (!getBeaconzWorld().getPopulators().contains(getBlockPopulator())) {
                event.getWorld().getPopulators().add(getBlockPopulator());
            }
        }
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
                if (!beacon.isClear()) {
                    // You can't capture an uncleared beacon
                    player.sendMessage(ChatColor.GOLD + "Clear around the beacon to capture!");
                    event.setCancelled(true);
                    return;
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
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            if (getRegister().isBeacon(it.next())) {
                it.remove();
            }
        }
    }

    /**
     * Prevents trees from growing above the beacon
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
            switch (event.getBlock().getType()) {
            // Allow leaves to grow over the beacon
            case LEAVES:
            case LEAVES_2:
                break;
            default:
                // For everything else, make sure there is air
                event.getBlock().setType(Material.AIR);
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
     * Prevents the tipping of liquids over the beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
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
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place liquids above a beacon!");
        }
        if (getRegister().isAboveBeacon(b.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place liquids above a beacon!");
        }
    }

    /**
     * Prevents the tipping of liquids over the beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
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
     * Removes any residual beaconz effects when player logs out
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onLeave(final PlayerQuitEvent event) {
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    /**
     * Processes players coming directly into the game
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(final PlayerJoinEvent event) {
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            final Player player = event.getPlayer();
            final UUID playerUUID = player.getUniqueId();
            // Check if game is still in progress
            Game game = getGameMgr().getGame(event.getPlayer().getLocation());
            if (game == null) {
                // Send player to BeaconzWorld lobby area
                getGameMgr().getLobby().tpToRegionSpawn(player);
            } else {
                // Join the game but stay at the last location
                game.join(player, false);
            }
            // Check messages
            // Load any messages for the player
            if (DEBUG)
                getLogger().info("DEBUG: Checking messages for " + player.getName());
            final List<String> messages = getMessages().getMessages(playerUUID);
            if (messages != null) {
                // plugin.getLogger().info("DEBUG: Messages waiting!");
                getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        player.sendMessage(ChatColor.AQUA + "Beaconz News");
                        int i = 1;
                        for (String message : messages) {
                            player.sendMessage(i++ + ": " + message);
                        }
                        // Clear the messages
                        getMessages().clearMessages(playerUUID);
                    }
                }, 40L);
            }
            if (DEBUG)
                getLogger().info("DEBUG: no messages");
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
        // Entering Beaconz world
        if (event.getPlayer().getWorld().equals((getBeaconzWorld()))) {
            // Send player to lobby
            getGameMgr().getLobby().tpToRegionSpawn(event.getPlayer());
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldExit(final PlayerChangedWorldEvent event) {
        // Exiting Beaconz world
        if (event.getFrom().equals((getBeaconzWorld()))) {
            // Remove player from map and remove his scoreboard
            standingOn.remove(event.getPlayer().getUniqueId());
            event.getPlayer().setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
            // Remove any potion effects
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
    }

    /**
     * Prevents liquid flowing over the beacon beam
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
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
        // Check for placing blocks outside the play area
        // TODO: This assumes that adjacent games will always be greater than 5 blocks away
        Game game = getGameMgr().getGame(event.getBlock().getLocation());
        if (game == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot do that!");
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
        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);
        // Stop placing blocks on a beacon
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlock().getX(),event.getBlock().getZ());
        if (beacon != null && beacon.getY() < event.getBlock().getY()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot build on top of a beacon!");
            return;
        }
    }


    /**
     * Handle breakage of the top part of a beacon
     * @param event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconBreak(BlockBreakEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        if (DEBUG)
            getLogger().info("DEBUG: This is a beacon");
        Player player = event.getPlayer();

        // Only Ops can break blocks in the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        // Prevent breakage of blocks outside the game area
        Game game = getGameMgr().getGame(event.getBlock().getLocation());
        if (DEBUG)
            getLogger().info("DEBUG: game = " + game);
        if (game == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot do that!");
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

        // Apply triangle effects
        //applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);

        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return;
        }
        // Cancel any breakage
        event.setCancelled(true);
        // Check for obsidian/glass breakage - i.e., capture
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            if (DEBUG)
                getLogger().info("DEBUG:beacon below");
            // Check if this is a real beacon
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                if (DEBUG)
                    getLogger().info("DEBUG: registered beacon");
                // It is a real beacon
                if (block.getType().equals(Material.OBSIDIAN)) {
                    // Check that the beacon is clear of blocks
                    if (!beacon.isClear()) {
                        // You can't capture an uncleared beacon
                        player.sendMessage(ChatColor.RED + "Clear around the beacon first!");
                        event.setCancelled(true);
                        return;
                    }
                    if (DEBUG)
                        getLogger().info("DEBUG: obsidian");
                    //Claiming unowned beacon
                    block.setType(getGameMgr().getSC(player).getBlockID(team).getItemType());
                    block.setData(getGameMgr().getSC(player).getBlockID(team).getData());
                    // Register the beacon to this team
                    getRegister().setBeaconOwner(beacon,team);
                    player.sendMessage(ChatColor.GREEN + "You captured a beacon! Mine the beacon for more beacon maps.");
                    giveBeaconMap(player,beacon);
                } else {
                    if (DEBUG)
                        getLogger().info("DEBUG: another block");
                    Team beaconTeam = beacon.getOwnership();
                    if (beaconTeam != null) {
                        if (DEBUG)
                            getLogger().info("DEBUG: known team block");
                        if (team.equals(beaconTeam)) {
                            // You can't destroy your own beacon
                            player.sendMessage(ChatColor.RED + "You cannot destroy your own beacon");
                            event.setCancelled(true);
                            return;
                        }
                        // Check that the beacon is clear of blocks
                        if (!beacon.isClear()) {
                            // You can't capture an uncleared beacon
                            player.sendMessage(ChatColor.RED + "Clear around the beacon first!");
                            event.setCancelled(true);
                            return;
                        }
                        // Enemy team has lost a beacon!
                        // Taunt other teams
                        getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team destroyed " + beaconTeam.getDisplayName() + "'s beacon!");
                        getMessages().tellTeam(player, player.getDisplayName() + " destroyed one of " + beaconTeam.getDisplayName() + "'s beacons!");
                        player.sendMessage(ChatColor.GREEN + "You destroyed " + beaconTeam.getDisplayName() + " team's beacon!");
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, 1F, 1F);
                        getRegister().removeBeaconOwnership(beacon);
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);
                        // Remove any standers
                        if (standingOn.containsValue(beacon)) {
                            standingOn.inverse().remove(beacon);
                        }
                    } else {
                        getRegister().removeBeaconOwnership(beacon);
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);
                        if (DEBUG)
                            getLogger().info("DEBUG: unknown team block");
                    }
                }
            }
        } else {
            // Attempt to break another part of the beacon
            // Only do on owned beacons
            if (beacon.getOwnership() != null) {
                // Check for cool down, if it's still cooling down, don't do anything
                if (beacon.isNewBeacon() || System.currentTimeMillis() > beacon.getHackTimer() + Settings.mineCoolDown) {
                    // Give something to the player if they have enough experience
                    // Remove experience
                    if (DEBUG)
                        getLogger().info("DEBUG: player has " + player.getTotalExperience() + " and needs " + Settings.beaconMineExpRequired);
                    if (!testForExp(player, Settings.beaconMineExpRequired)) {
                        player.sendMessage(ChatColor.RED + "You do not have enough experience to mine this beacon!");
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1F, 1F);
                        return;
                    }
                    Random rand = new Random();
                    if (beacon.getOwnership().equals(team)) {
                        // Own team
                        //getLogger().info("DEBUG: own team");
                        for (Entry<Integer, ItemStack> ent : Settings.teamGoodies.entrySet()) {
                            getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
                        }
                        int value = rand.nextInt(Settings.teamGoodies.lastKey()) + 1;
                        //getLogger().info("DEBUG: value = " + value);
                        Entry<Integer, ItemStack> en = Settings.teamGoodies.ceilingEntry(value);
                        //getLogger().info("DEBUG: en = " + en);
                        if (en != null && en.getValue() != null) {
                            if (en.getValue().getType().equals(Material.MAP)) {
                                giveBeaconMap(player,beacon);
                            } else {
                                player.getWorld().dropItem(event.getPlayer().getLocation(), en.getValue());
                            }
                            if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                beacon.resetHackTimer();
                                player.sendMessage(ChatColor.GREEN + "Success! Beacon is exhausted. Try again in " + (Settings.mineCoolDown/60000) + " minute(s)");
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDERCHEST_CLOSE, 1F, 1F);
                            } else {
                                player.sendMessage(ChatColor.GREEN + "Success!");
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                            }
                            // Remove exp
                            removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            player.sendMessage(ChatColor.RED + "Failure!");
                        }
                    } else {
                        // Enemy
                        int value = rand.nextInt(Settings.enemyGoodies.lastKey()) + 1;
                        Entry<Integer, ItemStack> en = Settings.enemyGoodies.ceilingEntry(value);
                        if (en != null && en.getValue() != null) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());
                            if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                beacon.resetHackTimer();
                                player.sendMessage(ChatColor.GREEN + "Success! Beacon is exhausted. Try again in " + (Settings.mineCoolDown/60000) + " minute(s)");
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDERCHEST_CLOSE, 1F, 1F);
                            } else {
                                player.sendMessage(ChatColor.GREEN + "Success!");
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                            }
                            // Remove exp
                            removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            player.getWorld().spawnEntity(player.getLocation(),EntityType.ENDERMITE);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMITE_AMBIENT, 1F, 1F);
                            player.sendMessage(ChatColor.RED + "Failure! Watch out!");
                        }
                    }
                } else {
                    // Damage player
                    int num = (int) (beacon.getHackTimer() + Settings.mineCoolDown - System.currentTimeMillis())/50;
                    for (String effect : Settings.minePenalty) {
                        String[] split = effect.split(":");
                        if (split.length == 2) {
                            int amplifier = 1;
                            if (NumberUtils.isNumber(split[1])) {
                                amplifier = Integer.valueOf(split[1]);
                                if (DEBUG)
                                    getLogger().info("DEBUG: Amplifier is " + amplifier);
                            }
                            PotionEffectType potionEffectType = PotionEffectType.getByName(split[0]);
                            if (potionEffectType != null) {
                                player.addPotionEffect(new PotionEffect(potionEffectType, num,amplifier));
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1F, 1F);
                                if (DEBUG)
                                    getLogger().info("DEBUG: Applying " + potionEffectType.toString() + ":" + amplifier + " for " + num + " ticks");
                            }
                        } else {
                            getLogger().warning("Unknown hack cooldown effect" + effect);
                        }


                    }
                }
            }
        }
    }

    /**
     * Handles using signs in the lobby to join games
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSignClick(final PlayerInteractEvent event) {
        if (getGameMgr().getLobby().isPlayerInRegion(event.getPlayer())) {
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                if (event.getClickedBlock().getState() instanceof Sign) {
                    Sign sign = (Sign) event.getClickedBlock().getState();
                    if (Arrays.toString(sign.getLines()).toLowerCase().contains("beaconz")) {
                        Boolean foundgame = false;
                        for (int i = 1; i < 3; i++) {
                            String gamename = sign.getLine(i);
                            if (getGameMgr().getGame(gamename) != null) {
                                getGameMgr().getGame(gamename).join(event.getPlayer());
                                foundgame = true;
                                break;
                            }
                        }
                        if (!foundgame) {
                            event.getPlayer().sendMessage("That sign does not point to an active game");
                        }
                    }
                }
            }
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getWorld().equals(getBeaconzWorld())
                && getGameMgr().getGame(event.getClickedBlock().getLocation()) == null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot do that!");
        }

    }


    /**
     * @param event
     */
    /*
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getInventory().getLocation().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getInventory().getLocation()) == null) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage(ChatColor.RED + "You cannot do that!");
            }
        }
    }
     */
    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLeashUse(final PlayerLeashEntityEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot do that!");
            }
        }
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerHitEntity(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getRightClicked().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot do that!");
            }
        }
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHangingPlace(final HangingPlaceEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot do that!");
            }
        }
    }


    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onShear(final PlayerShearEntityEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot do that!");
            }
        }
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleDamage(final VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getAttacker();
        if (player.getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getVehicle().getLocation()) == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You cannot do that!");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(final VehicleMoveEvent event) {
        if (!event.getVehicle().getWorld().equals(getBeaconzWorld())) {
            return;
        }
        // Check if a player is in it
        Entity passenger = event.getVehicle().getPassenger();
        if (passenger != null && passenger instanceof Player) {
            Player player = (Player)passenger;
            Location from = event.getFrom();
            Location to = event.getTo();
            if (checkMove(player, event.getVehicle().getWorld(), from, to)) {
                // Vehicle should stop moving
                Vector direction = event.getVehicle().getLocation().getDirection();
                event.getVehicle().teleport(event.getVehicle().getLocation().add(from.toVector().subtract(to.toVector()).normalize()));
                event.getVehicle().getLocation().setDirection(direction);
                event.getVehicle().setVelocity(new Vector(0,0,0));
            }
        }
    }

    /**
     * Handles the event of hitting a beacon with paper or a map
     * @param event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPaperMapUse(final PlayerInteractEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: paper map " + event.getEventName());
        if (Settings.pairLinking) {
            // Not used if pair linking is used
            return;
        }
        if (!event.hasItem()) {
            return;
        }
        if (!event.getItem().getType().equals(Material.PAPER) && !event.getItem().getType().equals(Material.MAP)) {
            return;
        }
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        World world = event.getClickedBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        // Ignore player in lobby
        if (getGameMgr().isPlayerInLobby(player)) {
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
        // Check if the block is a beacon or the surrounding pyramid
        Block b = event.getClickedBlock();
        final BeaconObj beacon = getRegister().getBeacon(b);
        if (beacon == null) {
            if (DEBUG)
                getLogger().info("DEBUG: not a beacon");
            return;
        }
        // Check the team
        if (beacon.getOwnership() == null || !beacon.getOwnership().equals(team)) {
            player.sendMessage(ChatColor.RED + "You must capture this beacon first!");
            event.setCancelled(true);
            return;
        }
        if (event.getItem().getType().equals(Material.PAPER)) {
            // Give map to player
            // Remove one paper
            event.getItem().setAmount(event.getItem().getAmount() - 1);
            giveBeaconMap(player, beacon);
            // Stop the beacon inventory opening
            event.setCancelled(true);
            return;
        } else {
            // Map!
            BeaconObj mappedBeacon = getRegister().getBeaconMap(event.getItem().getDurability());
            if (mappedBeacon == null) {
                // This is not a beacon map
                return;
            }
            // Check the team
            if (mappedBeacon.getOwnership() == null || !mappedBeacon.getOwnership().equals(team)) {
                player.sendMessage(ChatColor.RED + "Origin beacon is not owned by " + team.getDisplayName() + "!");
                return;
            }
            event.setCancelled(true);
            if (Settings.linkDistance >= 0 && Settings.expDistance > 0) {
                // Check if the player has sufficient experience to link the beacons
                double distance = beacon.getPoint().distance(mappedBeacon.getPoint());
                distance -= Settings.linkDistance;
                if (distance > 0) {
                    if (!testForExp(player, (int)(distance/Settings.expDistance))) {
                        player.sendMessage(ChatColor.RED + "You do not have enough experience to link to this beacon!");
                        player.sendMessage(ChatColor.RED + "You can link up to " + (int)(Settings.expDistance * getTotalExperience(player)) + " blocks away.");
                        player.sendMessage(ChatColor.RED + "This beacon is " + (int)distance + " blocks away.");
                        return;
                    }
                }
                if (linkBeacons(player, team, beacon, mappedBeacon)) {
                    player.sendMessage(ChatColor.GREEN + "The map disintegrates!");
                    player.setItemInHand(null);
                    removeExp(player, (int)(distance/Settings.expDistance));
                }
            } else {
                // No exp required
                if (linkBeacons(player, team, beacon, mappedBeacon)) {
                    player.sendMessage(ChatColor.GREEN + "The map disintegrates!");
                    player.setItemInHand(null);
                }
            }

        }
    }

    /**
     * Puts a beacon map in the player's main hand
     * @param player
     * @param beacon
     */
    private void giveBeaconMap(Player player, BeaconObj beacon) {
        // Make a map!
        player.sendMessage(ChatColor.GREEN + "You have a beacon map! Take it to another beacon to link them up!");
        MapView map = Bukkit.createMap(getBeaconzWorld());
        //map.setWorld(getBeaconzWorld());
        map.setCenterX(beacon.getX());
        map.setCenterZ(beacon.getZ());
        map.getRenderers().clear();
        map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
        map.addRenderer(new BeaconMap(getBeaconzPlugin()));
        ItemStack newMap = new ItemStack(Material.MAP);
        newMap.setDurability(map.getId());
        ItemMeta meta = newMap.getItemMeta();
        meta.setDisplayName("Beacon map for " + beacon.getName());
        newMap.setItemMeta(meta);
        // Each map is unique and the durability defines the map ID, register it
        getRegister().addBeaconMap(map.getId(), beacon);
        //getLogger().info("DEBUG: beacon id = " + beacon.getId());
        // Put map into hand
        ItemStack inHand = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(newMap);
        player.getInventory().addItem(inHand);
    }

    /**
     * Make sure all player held maps have triangle overlays. (todo: make sure all maps on item frames do as well)
     * There seem to be some bugs around this. It doesn't always take on the first try.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMapHold(final PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItem(event.getNewSlot());
        if (itemInHand == null) return;
        if (!Material.MAP.equals(itemInHand.getType())) {
            return;
        }
        if (!player.getWorld().equals(getBeaconzWorld())) {
            return;
        }
        @SuppressWarnings("deprecation")
        MapView map = Bukkit.getMap(itemInHand.getDurability());
        for (MapRenderer renderer : map.getRenderers()) {
            if (renderer instanceof TerritoryMapRenderer) {
                return;
            }
        }
        map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
    }

    /**
     * Tries to link two beacons. Failure reasons could be:
     * 1. trying to link a beacon to itself
     * 2. beacon having 8 links already
     * 3. link already exists
     * 4.link crosses opposition team's links
     *
     * @param player
     * @param team
     * @param beacon
     * @param otherBeacon
     * @return true if link is made successfully
     */
    private boolean linkBeacons(Player player, Team team, BeaconObj beacon,
            BeaconObj otherBeacon) {
        if (beacon.equals(otherBeacon)) {
            player.sendMessage(ChatColor.RED + "You cannot link a beacon to itself!");
            return false;
        }
        if (beacon.getNumberOfLinks() == 8) {
            player.sendMessage(ChatColor.RED + "This beacon already has 8 outbound links!");
            return false;
        }
        // Check if this link already exists
        if (beacon.getLinks().contains(otherBeacon)) {
            player.sendMessage(ChatColor.RED + "Link already exists!");
            return false;
        }
        // Proposed link
        Line2D proposedLink = new Line2D.Double(beacon.getPoint(), otherBeacon.getPoint());
        // Check if the link crosses opposition team's links
        if (DEBUG)
            getLogger().info("DEBUG: Check if the link crosses opposition team's links");
        for (Line2D line : getRegister().getEnemyLinks(team)) {
            if (DEBUG)
                getLogger().info("DEBUG: checking line " + line.getP1() + " to " + line.getP2());
            if (line.intersectsLine(proposedLink)) {
                player.sendMessage(ChatColor.RED + "Link cannot cross enemy link!");
                return false;
            }
        }

        // Link the two beacons!
        LinkResult result = beacon.addOutboundLink(otherBeacon);
        if (result.isSuccess()) {
            player.sendMessage(ChatColor.GREEN + "Link created!");
            player.sendMessage(ChatColor.GREEN + "This beacon now has " + beacon.getNumberOfLinks() + " links");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_LARGE_BLAST, 1F, 1F);
            //player.getWorld().spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
            if (Settings.pairLinking) {
                // Tell the other player if it was done via a pairing
                if (standingOn.containsValue(otherBeacon)) {
                    Player otherPlayer = getServer().getPlayer(standingOn.inverse().get(otherBeacon));
                    if (otherPlayer != null) {
                        otherPlayer.sendMessage(ChatColor.GREEN + "Link created!");

                        otherPlayer.getWorld().playSound(otherPlayer.getLocation(), Sound.ENTITY_FIREWORK_LARGE_BLAST, 1F, 1F);
                        //otherPlayer.getWorld().spawnEntity(otherPlayer.getLocation(), EntityType.EXPERIENCE_ORB);
                    }
                    // Tell the team
                    getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " and " + otherPlayer.getDisplayName()
                            + ChatColor.GREEN + " created a link!");
                    // Taunt other teams
                    getMessages().tellOtherTeams(team, ChatColor.GOLD + team.getDisplayName() + " team made a link!");
                }
            } else {
                // Tell the team
                getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " created a link!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Link could not be created!");
            return false;
        }
        if (result.getFieldsMade() > 0) {
            if (result.getFieldsMade() == 1) {
                player.sendMessage(ChatColor.GOLD + "Triangle created! New score = " + String.format(Locale.US, "%,d",getGameMgr().getSC(player).getScore(team, "area")));
                getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " created a triangle! New team score = " + String.format(Locale.US, "%,d", getGameMgr().getSC(player).getScore(team, "area")));
                // Taunt other teams
                getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team made a triangle!");
            } else {
                player.sendMessage(ChatColor.GOLD + String.valueOf(result.getFieldsMade()) + " triangles created! New score = " + String.format(Locale.US, "%,d", getGameMgr().getSC(player).getScore(team, "area")));
                getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " created " + String.valueOf(result.getFieldsMade()) + " triangles! New team score = " + String.format(Locale.US, "%,d", getGameMgr().getSC(player).getScore(team, "area")));
                // Taunt other teams
                getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team made " + String.valueOf(result.getFieldsMade()) + " triangles!");
            }
            /*
            for (int i = 0; i < result.getFieldsMade(); i++) {
                player.getWorld().spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
            }*/
        }
        if (result.getFieldsFailedToMake() > 0) {
            if (result.getFieldsFailedToMake() == 1) {
                player.sendMessage(ChatColor.RED + "One triangle could not be created because of overlapping enemy elements!");
            } else {
                player.sendMessage(ChatColor.RED + String.valueOf(result.getFieldsFailedToMake()) + " triangle could not be created because of overlapping enemy elements!");
            }
        }
        return true;
    }


    /**
     * Handle player movement
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Remember that teleporting is not detected as player movement..
        // If we want to catch movement by teleportation, we have to keep track of the players to-from by ourselves
        // Only proceed if there's been a change in X or Z coords
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        World world = event.getTo().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        event.setCancelled(checkMove(player, world, from, to));
    }

    /**
     * Handles checking of player movement.
     * @param player
     * @param world
     * @param from
     * @param to
     * @return true if the event should be canceled
     */
    private boolean checkMove(Player player, World world, Location from,
            Location to) {
        Region regionFrom = getGameMgr().getRegion(from);
        Region regionTo = getGameMgr().getRegion(to);

        // Check if a player is close to a barrier
        if (regionFrom != null) {
            regionFrom.showBarrier(player, 20);
        }

        // Check if player is trying to leave a region by moving over a region boundary
        // And send him back to whence he came
        if (regionFrom != null && regionFrom != regionTo) {
            if (from.distanceSquared(to) < 6.25) {
                //float pitch = player.getLocation().getPitch();
                //float yaw = player.getLocation().getYaw();
                Vector direction = player.getLocation().getDirection();
                barrierPlayers.add(player.getUniqueId());
                player.teleport(player.getLocation().add(from.toVector().subtract(to.toVector()).normalize()));
                //player.getLocation().setPitch(pitch);
                //player.getLocation().setPitch(yaw);
                player.getLocation().setDirection(direction);
                player.setVelocity(new Vector(0,0,0));
                player.sendMessage(ChatColor.YELLOW + "That's the limit of the game region, you can't go any further that way.");
                return true;
            }
        }

        // Check if player changed regions and process region exit and enter methods

        // Leaving
        if (regionFrom != null && regionFrom != regionTo) {
            regionFrom.exit(player);
        }
        // Entering
        if (regionTo != null && regionFrom != regionTo) {
            regionTo.enter(player);
        }
        // Outside play area
        if (regionTo == null && regionFrom == null) {
            if (!player.isOp()) {
                player.teleport(getGameMgr().getLobby().getSpawnPoint());
                getLogger().warning(player.getName() + " managed to get outside of the game area and was teleported to the lobby.");
                return true;
            }
        }

        // Nothing from here on applies to Lobby...
        if (getGameMgr().isPlayerInLobby(player)) {
            triangleEffects.remove(player.getUniqueId());
            return false;
        }
        // Get the player's team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            if (player.isOp()) {
                return false;
            } else {
                return true;
            }
        }

        // Run the following if players can create links by standing on them in pairs (or more)
        if (Settings.pairLinking) {
            // Check if player is standing on a beacon
            BeaconObj beacon = getRegister().getBeaconAt(to.getBlockX(), to.getBlockZ());
            if (beacon != null) {
                // Check if the beacon is captured by this team
                if (beacon.getOwnership() != null && beacon.getOwnership().equals(team)) {
                    // Check if they are on the same beacon
                    BeaconObj currentBeacon = standingOn.get(player.getUniqueId());
                    if (currentBeacon == null || !currentBeacon.equals(beacon)) {
                        standingOn.remove(player.getUniqueId());
                        // Check if any other team players are on beacons and link to them
                        for (UUID others : standingOn.keySet()) {
                            Player otherplayer = getServer().getPlayer(others);
                            if (otherplayer != null) {
                                // Check team
                                Team otherteam = getGameMgr().getPlayerTeam(player);
                                if (otherteam != null && otherteam.equals(team)) {
                                    // Only do if beacons are different locations
                                    if (otherplayer.getLocation().getBlockX() != to.getBlockX()
                                            && otherplayer.getLocation().getBlockZ() != to.getBlockZ()) {
                                        // Check if this link already exists
                                        if (!beacon.getLinks().contains(standingOn.get(others))) {
                                            // Try to link
                                            linkBeacons(player,team,beacon,standingOn.get(others));
                                        }
                                    }
                                }
                            }
                        }
                        // Forceput removes any other player's on this beacon
                        standingOn.forcePut(player.getUniqueId(), beacon);
                    }
                }
            } else {
                // Remove player from map
                standingOn.remove(player.getUniqueId());
            }
        }
        // Check the From
        List<TriangleField> fromTriangle = getRegister().getTriangle(from.getBlockX(), from.getBlockZ());
        // Check the To
        List<TriangleField> toTriangle = getRegister().getTriangle(to.getBlockX(), to.getBlockZ());
        // Outside any field
        if (fromTriangle.isEmpty() && toTriangle.isEmpty()) {
            return false;
        }
        // Check if to is not a triangle
        if (toTriangle.size() == 0) {
            // Leaving a control triangle
            player.sendMessage("Leaving " + fromTriangle.get(0).getOwner().getDisplayName() + "'s control area");
            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());
            triangleEffects.remove(player.getUniqueId());
            return false;
        }
        // Entering a field, or moving to a stronger field
        if (fromTriangle.size() < toTriangle.size()) {
            player.sendMessage("Now entering " + toTriangle.get(0).getOwner().getDisplayName() + "'s area of control level " + toTriangle.size());
        } else if (toTriangle.size() < fromTriangle.size()) {
            // Remove all current effects - the lower set will be applied below
            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());
            player.sendMessage(toTriangle.get(0).getOwner().getDisplayName() + "'s control level dropping to " + toTriangle.size());
        }

        // Apply triangle effects
        applyEffects(player, toTriangle, team);

        return false;
    }

    /**
     * Applies triangle effects to a player
     * @param player
     * @param to
     * @param team
     */
    private void applyEffects(final Player player, final List<TriangleField> to, final Team team) {
        if (to == null || to.isEmpty() || team == null) {
            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());
            triangleEffects.remove(player.getUniqueId());
            return;
        }
        // Update the active effects on the player
        // Add bad stuff
        // Enemy team
        Team triangleOwner = to.get(0).getOwner();
        Collection<PotionEffect> effects = new ArrayList<PotionEffect>();
        if (triangleOwner != null && !triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.enemyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.enemyFieldEffects.get(i));
                }
            }
        }
        // Friendly triangle
        if (triangleOwner != null && triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.friendlyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.friendlyFieldEffects.get(i));
                }
            }
            player.addPotionEffects(effects);
        }
        triangleEffects.put(player.getUniqueId(), effects);
    }

    /**
     * Tests if a player has the required experience to perform the action. If so, the experience
     * is deducted. This function updates the client's UI exp bar.
     * @param player
     * @param xpRequired
     * @return true if sufficient experience points otherwise false
     */
    public boolean testForExp(Player player , int xpRequired){
        return getTotalExperience(player) >= xpRequired ? true : false;
    }

    /**
     * Removes the experience from the player
     * @param player
     * @param xpRequired
     * @return
     */
    public void removeExp(Player player , int xpRequired){
        int xp = getTotalExperience(player);
        if (xp >= xpRequired) {
            setTotalExperience(player, xp - xpRequired);
        }
    }

    // These next methods are taken from Essentials code

    //This method is used to update both the recorded total experience and displayed total experience.
    //We reset both types to prevent issues.
    public static void setTotalExperience(final Player player, final int exp)
    {
        if (exp < 0)
        {
            throw new IllegalArgumentException("Experience is negative!");
        }
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        //This following code is technically redundant now, as bukkit now calulcates levels more or less correctly
        //At larger numbers however... player.getExp(3000), only seems to give 2999, putting the below calculations off.
        int amount = exp;
        while (amount > 0)
        {
            final int expToLevel = getExpAtLevel(player);
            amount -= expToLevel;
            if (amount >= 0)
            {
                // give until next level
                player.giveExp(expToLevel);
            }
            else
            {
                // give the rest
                amount += expToLevel;
                player.giveExp(amount);
                amount = 0;
            }
        }
    }

    private static int getExpAtLevel(final Player player)
    {
        return getExpAtLevel(player.getLevel());
    }

    //new Exp Math from 1.8
    public  static  int getExpAtLevel(final int level)
    {
        if (level <= 15)
        {
            return (2*level) + 7;
        }
        if ((level >= 16) && (level <=30))
        {
            return (5 * level) -38;
        }
        return (9*level)-158;

    }

    public static int getExpToLevel(final int level)
    {
        int currentLevel = 0;
        int exp = 0;

        while (currentLevel < level)
        {
            exp += getExpAtLevel(currentLevel);
            currentLevel++;
        }
        if (exp < 0)
        {
            exp = Integer.MAX_VALUE;
        }
        return exp;
    }

    //This method is required because the bukkit player.getTotalExperience() method, shows exp that has been 'spent'.
    //Without this people would be able to use exp and then still sell it.
    public static int getTotalExperience(final Player player)
    {
        int exp = Math.round(getExpAtLevel(player) * player.getExp());
        int currentLevel = player.getLevel();

        while (currentLevel > 0)
        {
            currentLevel--;
            exp += getExpAtLevel(currentLevel);
        }
        if (exp < 0)
        {
            exp = Integer.MAX_VALUE;
        }
        return exp;
    }

    /**
     * Gets the experience required to obtain the next level
     * @param player
     * @return experience
     */
    public static int getExpUntilNextLevel(final Player player)
    {
        int exp = Math.round(getExpAtLevel(player) * player.getExp());
        int nextLevel = player.getLevel();
        return getExpAtLevel(nextLevel) - exp;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: Teleporting event");
        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            if (DEBUG)
                getLogger().info("DEBUG: from or to world is null");
            return;
        }
        // If player is pushed back because of the barrier, just return
        if (barrierPlayers.contains(event.getPlayer().getUniqueId())) {
            //getLogger().info("DEBUG: ignoring barrier teleport");
            barrierPlayers.remove(event.getPlayer().getUniqueId());
            return;
        }
        // Get the games associated with these locations
        final Game fromGame = getGameMgr().getGame(event.getFrom());
        final Game toGame = getGameMgr().getGame(event.getTo());

        // Check if the teleport is to or from the lobby
        final boolean fromLobby = getGameMgr().isLocationInLobby(event.getFrom());
        final boolean toLobby = getGameMgr().isLocationInLobby(event.getTo());
        // Teleporting out of Beaconz World
        if (!event.getTo().getWorld().equals(getBeaconzWorld()) && fromGame != null) {
            if (DEBUG)
                getLogger().info("DEBUG: Teleporting out of world");
            // Store
            getBeaconzStore().storeInventory(event.getPlayer(), fromGame.getName(), event.getFrom());
            // Load lobby inv
            getBeaconzStore().getInventory(event.getPlayer(), "Lobby");
            return;
        }
        /*
        // Teleporting into Beaconz World
        if (event.getTo().getWorld().equals(getBeaconzWorld()) && event.getFrom().getWorld().equals(getBeaconzWorld())) {
            getLogger().info("DEBUG: Teleporting into world");
            // Get from store
            getBeaconzPlugin().getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {

                @Override
                public void run() {
                    getBeaconzStore().getInventory(event.getPlayer(), event.getTo());
                }}, 5L);

            return;
        }*/
        // Teleporting to different game
        if (event.getTo().getWorld().equals(getBeaconzWorld()) && event.getFrom().getWorld().equals(getBeaconzWorld())) {
            if (DEBUG)
                getLogger().info("DEBUG: Teleporting within world");

            if (toLobby && fromLobby) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting within lobby");
                return;
            }

            if (toLobby && fromGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting to lobby from game " + fromGame.getName());
                // Store
                getBeaconzStore().storeInventory(event.getPlayer(), fromGame.getName(), event.getFrom());
                // Get from store
                getBeaconzStore().getInventory(event.getPlayer(), "Lobby");
                return;
            }

            if (fromLobby && toGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting from lobby to a game " + toGame.getName());
                // Store in lobby
                getBeaconzStore().storeInventory(event.getPlayer(), "Lobby", event.getFrom());
                // Get from store and move to last known position
                Location newTo = getBeaconzStore().getInventory(event.getPlayer(), toGame.getName());
                if (newTo != null) {
                    // Get a safe location
                    newTo = toGame.getRegion().findSafeSpot(newTo, 10);
                    event.setTo(newTo);
                }
                return;
            }
            if (DEBUG)
                getLogger().info("DEBUG: Not a lobby teleport");
            // Not a lobby
            if (fromGame != null && toGame != null && fromGame.equals(toGame)) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting within game");
                return;
            }

            if (fromGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting from game " + fromGame.getName());
                // Store
                getBeaconzStore().storeInventory(event.getPlayer(), fromGame.getName(), event.getFrom());
            }
            if (toGame != null) {
                if (DEBUG)
                    getLogger().info("DEBUG: Teleporting to game " + toGame.getName());
                // Check if player is in this game
                if (toGame.hasPlayer(event.getPlayer())) {
                    // Get from store
                    Location newTo = getBeaconzStore().getInventory(event.getPlayer(), toGame.getName());
                    if (newTo != null) {
                        if (DEBUG)
                            getLogger().info("DEBUG: Teleporting player to last location" + newTo);
                        newTo = toGame.getRegion().findSafeSpot(newTo, 10);
                        event.setTo(newTo);
                    }
                    /*
                    getBeaconzPlugin().getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {

                        @Override
                        public void run() {
                            getBeaconzStore().getInventory(event.getPlayer(), toGame.getName());
                        }}, 5L);
                     */
                } else {
                    if (DEBUG)
                        getLogger().info("DEBUG: Player is not in this game");
                    // Send them to the lobby
                    event.getPlayer().sendMessage(ChatColor.RED + "You are not in the game '" + toGame.getName() + "'! Going to the lobby...");
                    event.setTo(getGameMgr().getLobby().getSpawnPoint());
                }
            }
            return;
        }
    }

    /**
     * Saves the player's death and resets their spawn point to the team spawn
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        //getLogger().info("DEBUG: death");
        Player player = event.getEntity();
        // Check if player died in-game
        if (!player.getWorld().equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not in world");
            return;
        }
        // If in the lobby, ignore
        if (getGameMgr().isLocationInLobby(player.getLocation())) {
            //getLogger().info("DEBUG: died in lobby");
            return;
        }
        // Get game
        Game game = getGameMgr().getGame(player.getLocation());
        if (game != null) {
            Team team = game.getScorecard().getTeam(player);
            //getLogger().info("DEBUG: team is " + team.getDisplayName());
            // Store new spawn point
            Location spawnPoint = game.getScorecard().getTeamSpawnPoint(team);
            //getLogger().info("DEBUG: new spawn point is " + spawnPoint);
            if (event.getKeepInventory()) {
                // Store the inventory for this player because they will get it when they come back
                // Will also store their exp
                //getLogger().info("DEBUG: keep inventory is true");
                getBeaconzStore().storeInventory(player, game.getName(), spawnPoint);
            } else {
                //getLogger().info("DEBUG: keep inventory is false");
                // Their inventory is going to get dumped on the floor so they need to have their possessions removed
                getBeaconzStore().clearItems(player, game.getName(), spawnPoint);
            }
            if (!event.getKeepLevel()) {
                //getLogger().info("DEBUG: lose level! - new exp = " + event.getNewExp());
                // If they don't get to keep their level, their exp needs updating to what they'll get in this world.
                getBeaconzStore().setExp(player, game.getName(), event.getNewExp());
            } else {
                //getLogger().info("DEBUG: keep level");
            }
            // They are dead, so when they respawn they need to have full health (otherwise they will die repeatedly)
            getBeaconzStore().setHealth(player, game.getName(), player.getMaxHealth());
            // They will also get full food level
            getBeaconzStore().setFood(player, game.getName(), 20);
            // Make a note of their death status
            deadPlayers.put(player.getUniqueId(), spawnPoint);
        } else {
            //getLogger().info("DEBUG: game is null");
        }
    }

    /**
     * Respawns the player back at the beaconz lobby
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRespawn(final PlayerRespawnEvent event) {
        //getLogger().info("DEBUG: Respawn");
        if (!deadPlayers.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        // Set respawn location to Beaconz lobby
        event.setRespawnLocation(getGameMgr().getLobby().getSpawnPoint());
        deadPlayers.remove(event.getPlayer().getUniqueId());
        // Get from store
        getBeaconzStore().getInventory(event.getPlayer(), "Lobby");
    }

    /**
     * Projects all chests inside triangles or on beacons
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        // Check what type of inventory this is
        if (event.getInventory().getType().equals(InventoryType.PLAYER)) {
            return;
        }
        Location invLoc = event.getInventory().getLocation();
        if (invLoc == null) {
            return;
        }
        World world = event.getPlayer().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = (Player) event.getPlayer();
        Game game = getGameMgr().getGame(player.getLocation());
        if (game != null) {
            Team team = game.getScorecard().getTeam(player);         
            // Check beacon defense            
            BeaconObj beacon = getRegister().getBeaconDefenseAt(invLoc);
            if (beacon != null) {
                if (!beacon.getOwnership().equals(team)) {
                    player.sendMessage(ChatColor.RED + "This belongs to " + beacon.getOwnership().getDisplayName() + "!");
                    event.setCancelled(true);
                    return;
                }
            }
            // Check triangle
            for (TriangleField triangle : getRegister().getTriangle(invLoc.getBlockX(), invLoc.getBlockZ())) {
                if (!triangle.getOwner().equals(team)) {
                    player.sendMessage(ChatColor.RED + "This belongs to " + triangle.getOwner().getDisplayName() + "!");
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

}
