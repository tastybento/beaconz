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

package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class BeaconListeners extends BeaconzPluginDependent implements Listener {

    /**
     * A bi-drectional hashmap to track players standing on beaconz
     */
    private BiMap<UUID, BeaconObj> standingOn = HashBiMap.create();;

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
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        //getLogger().info("DEBUG: This is a beacon");
        Player player = event.getPlayer();
        // Get the player's team
        Team team = getScorecard().getTeam(player);
        if (team == null) {
            // TODO: Probably should put the player in a team
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
            return;
        }
        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);

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
                    player.sendMessage(ChatColor.RED + "Clear around the beacon first!");
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
        //getLogger().info("DEBUG: " + event.getEventName());
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
            //getLogger().info("DEBUG: not right world");
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
            //getLogger().info("DEBUG: not right world");
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
     * Prevents the tipping of liquids over the beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlockClicked().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlockClicked().getX(),event.getBlockClicked().getZ());
        if (beacon != null && beacon.getY() <= event.getBlockClicked().getY()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place liquids above a beacon!");
        }
    }

    /**
     * Puts player into the Beaconz scoreboard if they are in the Beaconz world
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(final PlayerJoinEvent event) {
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            // Assign the player to the scoreboard
            final Player player = event.getPlayer();
            final UUID playerUUID = player.getUniqueId();
            player.setScoreboard(getScorecard().getScoreboard());
            // Check messages
            // Load any messages for the player
            // plugin.getLogger().info("DEBUG: Checking messages for " +
            // player.getName());
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
            } // else {
            // plugin.getLogger().info("no messages");
            // }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        // Entering Beaconz world
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            event.getPlayer().setScoreboard(getScorecard().getScoreboard());
        } else {
            // Leaving Beaconz world
            // Remove player from map
            standingOn.remove(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Prevents liquid flowing over the beacon beam
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLiquidFlow(final BlockFromToEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        // Only bother with horizontal flows
        if (event.getToBlock().getX() != event.getBlock().getX() || event.getToBlock().getZ() != event.getBlock().getZ()) {
            //getLogger().info("DEBUG: " + event.getEventName());
            BeaconObj beacon = getRegister().getBeaconAt(event.getToBlock().getX(),event.getToBlock().getZ());
            if (beacon != null && beacon.getY() < event.getToBlock().getY()) {
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
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        Player player = event.getPlayer();
        // Get the player's team
        Team team = getScorecard().getTeam(player);
        if (team == null) {
            // TODO: Probably should put the player in a team
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
            return;
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

        // Check if the block is the surrounding pyramid
        Block block = event.getBlock().getRelative(BlockFace.DOWN);
        // Check that it's diamond
        if (!block.getType().equals(Material.DIAMOND_BLOCK)) {
            return;
        }
        beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return;
        }
        //getLogger().info("DEBUG: This is a beacon");

        // Check if the team is placing a block on their own beacon or not
        if (beacon.getOwnership() != null && beacon.getOwnership().equals(team)) {
            // Check what type of block it is:
            // TODO: Add implications of placement.
            switch (event.getBlock().getType()) {
            case ACACIA_DOOR:
            case BIRCH_DOOR:
            case DARK_OAK_DOOR:
                break;
            case ACACIA_FENCE:
            case BIRCH_FENCE:
            case DARK_OAK_FENCE:
                player.sendMessage(ChatColor.GREEN + "Fence protection!");
                break;
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
                player.sendMessage(ChatColor.GREEN + "Gate protection!");
                break;
            case ACACIA_STAIRS:
            case BIRCH_WOOD_STAIRS:
            case DARK_OAK_STAIRS:
                break;
            case ACTIVATOR_RAIL:
                break;
            case ANVIL:
                break;
            case ARMOR_STAND:
                break;
            case BANNER:
                break;
            case BEACON:
                player.sendMessage(ChatColor.GREEN + "Dummy?");
                break;
            case BEDROCK:
                player.sendMessage(ChatColor.GREEN + "Ultimate defense! Hope you didn't make a mistake!");
                break;
            case BED_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Sleep, sleep, sleep");
                break;
            case BOOKSHELF:
                player.sendMessage(ChatColor.GREEN + "Knowledge is power!");
                break;
            case BREWING_STAND:
                player.sendMessage(ChatColor.GREEN + "Potion attack!");
                break;
            case BRICK:
                break;
            case BRICK_STAIRS:
                break;
            case CAKE_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Hunger benefits");
                break;
            case CARPET:
                player.sendMessage(ChatColor.GREEN + "Hmm, pretty!");
                break;
            case CAULDRON:
                player.sendMessage(ChatColor.GREEN + "Witch's brew!");
                break;
            case CHEST:
                player.sendMessage(ChatColor.GREEN + "I wonder what you will put in it");
                break;
            case COAL_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Energy up!");
                break;
            case COBBLESTONE:
                break;
            case COBBLESTONE_STAIRS:
                break;
            case COBBLE_WALL:
                break;
            case DAYLIGHT_DETECTOR:
                player.sendMessage(ChatColor.GREEN + "Let night be day!");
                break;
            case DETECTOR_RAIL:
                break;
            case DIAMOND_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Fortune will smile upon you!");
                break;
            case DIAMOND_ORE:
                break;
            case DIODE_BLOCK_OFF:
                break;
            case DIRT:
                break;
            case DISPENSER:
                player.sendMessage(ChatColor.GREEN + "Load it up!");
                break;
            case DOUBLE_STEP:
                break;
            case DOUBLE_STONE_SLAB2:
                break;
            case DRAGON_EGG:
                player.sendMessage(ChatColor.GREEN + "The end is nigh!");
                break;
            case DROPPER:
                player.sendMessage(ChatColor.GREEN + "Drip, drop, drip");
                break;
            case EMERALD_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Commerce with thrive!");
                break;
            case EMERALD_ORE:
                break;
            case ENCHANTMENT_TABLE:
                player.sendMessage(ChatColor.GREEN + "Magic will occur");
                break;
            case ENDER_CHEST:
                player.sendMessage(ChatColor.GREEN + "I wonder what is inside?");
                break;
            case ENDER_STONE:
                player.sendMessage(ChatColor.GREEN + "End attack!");
                break;
            case FENCE:
                break;
            case FENCE_GATE:
                break;
            case FLOWER_POT:
                player.sendMessage(ChatColor.GREEN + "I wonder what this will do...");
                break;
            case FURNACE:
                player.sendMessage(ChatColor.GREEN + "Fire attack! If it's hot.");
                break;
            case GLASS:
                player.sendMessage(ChatColor.GREEN + "I can see clearly now");
                break;
            case GLOWSTONE:
                player.sendMessage(ChatColor.GREEN + "Glow, glow");
                break;
            case GOLD_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Money, money, money");
                break;
            case GOLD_ORE:
                break;
            case GRASS:
                break;
            case GRAVEL:
                break;
            case HARD_CLAY:
                break;
            case HAY_BLOCK:
                break;
            case HOPPER:
                break;
            case ICE:
                player.sendMessage(ChatColor.GREEN + "Brrr, it's cold");
                break;
            case IRON_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Protection");
                break;
            case IRON_DOOR_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Knock, knock");
                break;
            case IRON_FENCE:
                break;
            case IRON_ORE:
                break;
            case IRON_TRAPDOOR:
                break;
            case ITEM_FRAME:
                break;
            case JACK_O_LANTERN:
                player.sendMessage(ChatColor.GREEN + "Boo!");
                break;
            case JUKEBOX:
                break;
            case JUNGLE_DOOR:
                break;
            case JUNGLE_FENCE:
                break;
            case JUNGLE_FENCE_GATE:
                break;
            case JUNGLE_WOOD_STAIRS:
                break;
            case LADDER:
                player.sendMessage(ChatColor.GREEN + "Up we go!");
                break;
            case LAPIS_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Everything is blue!");
                break;
            case LAVA:
                player.sendMessage(ChatColor.GREEN + "Hot stuff!");
                break;
            case LEAVES:
            case LEAVES_2:
                player.sendMessage(ChatColor.GREEN + "Camoflage");
                break;
            case LEVER:
                player.sendMessage(ChatColor.GREEN + "I wonder what this does!");
                break;
            case LOG:
            case LOG_2:
                player.sendMessage(ChatColor.GREEN + "It's a tree!");
                break;
            case MELON_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Hungry?");
                break;
            case MOB_SPAWNER:
                player.sendMessage(ChatColor.GREEN + "That's what I'm talking about!");
                break;
            case MOSSY_COBBLESTONE:
                break;
            case MYCEL:
                player.sendMessage(ChatColor.GREEN + "Smelly!");
                break;
            case NETHERRACK:
            case NETHER_BRICK:
            case NETHER_BRICK_STAIRS:
            case NETHER_FENCE:
                player.sendMessage(ChatColor.GREEN + "That's not from around here!");
                break;
            case NOTE_BLOCK:
                player.sendMessage(ChatColor.GREEN + "I hear things?");
                break;
            case OBSIDIAN:
                player.sendMessage(ChatColor.GREEN + "Tough protection!");
                break;
            case PACKED_ICE:
                player.sendMessage(ChatColor.GREEN + "Cold, so cold...");
                break;
            case PISTON_BASE:
            case PISTON_STICKY_BASE:
                player.sendMessage(ChatColor.GREEN + "Pushy!");
                break;
            case POWERED_RAIL:
                player.sendMessage(ChatColor.GREEN + "Power to the people!");
                break;
            case PRISMARINE:
                player.sendMessage(ChatColor.GREEN + "Aqua");
                break;
            case PUMPKIN:
                player.sendMessage(ChatColor.GREEN + "Farming?");
                break;
            case QUARTZ_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Pretty");
                break;
            case QUARTZ_ORE:
                break;
            case QUARTZ_STAIRS:
                break;
            case RAILS:
                player.sendMessage(ChatColor.GREEN + "Where do they go?");
                break;
            case REDSTONE_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Power up!");
                break;
            case REDSTONE_COMPARATOR:
            case REDSTONE_COMPARATOR_OFF:
            case REDSTONE_COMPARATOR_ON:
                player.sendMessage(ChatColor.GREEN + "What's the question?");
                break;
            case REDSTONE_LAMP_OFF:
            case REDSTONE_LAMP_ON:
                player.sendMessage(ChatColor.GREEN + "Light?");
                break;
            case REDSTONE_ORE:
                break;
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
                player.sendMessage(ChatColor.GREEN + "Power gen");
                break;
            case REDSTONE_WIRE:
                player.sendMessage(ChatColor.GREEN + "Does it glow?");
                break;
            case RED_SANDSTONE:
            case RED_SANDSTONE_STAIRS:
                player.sendMessage(ChatColor.RED + "It's red");
                break;
            case SAND:
            case SANDSTONE:
            case SANDSTONE_STAIRS:
                player.sendMessage(ChatColor.YELLOW + "It's yellow");
                break;
            case SEA_LANTERN:
                player.sendMessage(ChatColor.GREEN + "Nice! Sea attack!");
                break;
            case SIGN_POST:
                player.sendMessage(ChatColor.GREEN + "Warning message set!");
                break;
            case SKULL:
                player.sendMessage(ChatColor.GREEN + "Death to this entity!");
                break;
            case SLIME_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Boing, boing, boing!");
                break;
            case SMOOTH_BRICK:
                break;
            case SMOOTH_STAIRS:
                break;
            case SNOW_BLOCK:
                player.sendMessage(ChatColor.GREEN + "Cold!");
                break;
            case SOUL_SAND:
                player.sendMessage(ChatColor.GREEN + "<scream>");
                break;
            case SPONGE:
                player.sendMessage(ChatColor.GREEN + "Slurp!");
                break;
            case SPRUCE_DOOR:
                break;
            case SPRUCE_FENCE:
                break;
            case SPRUCE_FENCE_GATE:
                break;
            case SPRUCE_WOOD_STAIRS:
                break;
            case STAINED_CLAY:
                break;
            case STAINED_GLASS:
                player.sendMessage(ChatColor.GREEN + "Pretty!");
                break;
            case STAINED_GLASS_PANE:
                break;
            case STANDING_BANNER:
                player.sendMessage(ChatColor.GREEN + "Be proud!");
                break;
            case STATIONARY_LAVA:
            case STATIONARY_WATER:
                player.sendMessage(ChatColor.GREEN + "A moat?");
                break;
            case STEP:
                break;
            case STONE:
                break;
            case STONE_PLATE:
                player.sendMessage(ChatColor.GREEN + "A trap?");
                break;
            case STONE_SLAB2:
                break;
            case THIN_GLASS:
                player.sendMessage(ChatColor.GREEN + "Not much protection...");
                break;
            case TNT:
                player.sendMessage(ChatColor.GREEN + "Explosive protection!");
                break;
            case TRAP_DOOR:
                break;
            case TRIPWIRE:
                break;
            case TRIPWIRE_HOOK:
                break;
            case VINE:
                break;
            case WALL_BANNER:
                break;
            case WALL_SIGN:
                player.sendMessage(ChatColor.GREEN + "Goodbye message set");
                break;
            case WEB:
                player.sendMessage(ChatColor.GREEN + "Slow down the enemy!");
                break;
            case WOOD:
                break;
            case WOODEN_DOOR:
                break;
            case WOOD_DOOR:
                break;
            case WOOD_DOUBLE_STEP:
                break;
            case WOOD_PLATE:
                player.sendMessage(ChatColor.GREEN + "Trap?");
                break;
            case WOOD_STAIRS:
                break;
            case WOOD_STEP:
                break;
            case WOOL:
                player.sendMessage(ChatColor.GREEN + "Keep warm!");
                break;
            case WORKBENCH:
                player.sendMessage(ChatColor.GREEN + "That's helpful!");
                break;
            default:
                break;
            }

        } else {
            player.sendMessage(ChatColor.RED + "You can only place blocks on a captured beacon!");
            event.setCancelled(true);
        }
    }


    /**
     * Handle breakage of the top part of a beacon
     * @param event
     */
    @SuppressWarnings("deprecation")
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
        // Get the player's team
        Team team = getScorecard().getTeam(player);
        if (team == null) {
            // TODO: Probably should put the player in a team
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
            return;
        }
        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);

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
            //getLogger().info("DEBUG:beacon below");
            // Check if this is a real beacon
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                //getLogger().info("DEBUG: registered beacon");
                // It is a real beacon
                if (block.getType().equals(Material.OBSIDIAN)) {
                    // Check that the beacon is clear of blocks
                    if (!beacon.isClear()) {
                        // You can't capture an uncleared beacon
                        player.sendMessage(ChatColor.RED + "Clear around the beacon first!");
                        event.setCancelled(true);
                        return;
                    }
                    //getLogger().info("DEBUG: obsidian");
                    //Claiming unowned beacon
                    block.setType(getScorecard().getBlockID(team).getItemType());
                    block.setData(getScorecard().getBlockID(team).getData());
                    // Register the beacon to this team
                    getRegister().setBeaconOwner(beacon,team);
                    player.sendMessage(ChatColor.GREEN + "You captured a beacon!");
                } else {
                    //getLogger().info("DEBUG: another block");
                    Team beaconTeam = beacon.getOwnership();
                    if (beaconTeam != null) {
                        //getLogger().info("DEBUG: known team block");
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
                        getMessages().tellTeam(player.getUniqueId(), player.getDisplayName() + " destroyed one of " + beaconTeam.getDisplayName() + "'s beacons!");
                        player.sendMessage(ChatColor.GREEN + "You destroyed " + beaconTeam.getDisplayName() + " team's beacon!");
                
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
                        getLogger().info("DEBUG: unknown team block");
                    }
                }
            }
        } else {
            // Attempt to break another part of the beacon
            // Only do on owned beacons
            if (beacon.getOwnership() != null) {
                // Check for cool down, if it's still cooling down, don't do anything
                if (beacon.isNewBeacon() || System.currentTimeMillis() > beacon.getHackTimer() + Settings.hackCoolDown) {
                    // Give something to the player
                    Random rand = new Random();
                    int value = rand.nextInt(100) + 1;
                    //getLogger().info("DEBUG: random number = " + value);
                    if (beacon.getOwnership().equals(getScorecard().getTeam(player))) {
                        // Own team
                        /*
            for (Entry<Integer, ItemStack> ent : Settings.teamGoodies.entrySet()) {
            getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
            }*/
                        Entry<Integer, ItemStack> en = Settings.teamGoodies.floorEntry(value);
                        if (en != null && en.getValue() != null) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());
                            beacon.resetHackTimer();
                        } else {
                            player.getWorld().spawnEntity(player.getLocation(),EntityType.ENDERMITE);
                            beacon.resetHackTimer();
                            //getLogger().info("DEBUG: failed - max value was " + Settings.teamGoodies.lastKey());
                        }
                    } else {
                        // Enemy
                        /*
            for (Entry<Integer, ItemStack> ent : Settings.enemyGoodies.entrySet()) {
            getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
            }*/
                        Entry<Integer, ItemStack> en = Settings.enemyGoodies.floorEntry(value);
                        if (en != null && en.getValue() != null) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());
                            beacon.resetHackTimer();
                        }
                    }
                } else {
                    // Damage player
                    int num = (int) (beacon.getHackTimer() + Settings.hackCoolDown - System.currentTimeMillis())/50;
                    for (String effect : Settings.overHackEffects) {
                        String[] split = effect.split(":");
                        if (split.length == 2) {
                            int amplifier = 0;
                            if (NumberUtils.isNumber(split[1])) {
                                amplifier = Integer.valueOf(split[1]);
                            }
                            PotionEffectType potionEffectType = PotionEffectType.getByName(effect);
                            if (potionEffectType != null) {
                                player.addPotionEffect(new PotionEffect(potionEffectType, num,amplifier));
                                getLogger().info("DEBUG: Applying " + potionEffectType.toString() + ":" + amplifier + " for " + num + " ticks");
                            }
                        }


                    }
                }
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
        //getLogger().info("DEBUG: paper map " + event.getEventName());
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
            //getLogger().info("DEBUG: not right world");
            return;
        }
        Player player = event.getPlayer();
        // Get the player's team
        Team team = getScorecard().getTeam(player);
        if (team == null) {
            // TODO: Probably should put the player in a team
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
            return;
        }
        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);

        // Check if the block is a beacon or the surrounding pyramid
        Block b = event.getClickedBlock();
        final BeaconObj beacon = getRegister().getBeacon(b);
        if (beacon == null) {
            //getLogger().info("DEBUG: not a beacon");
            return;
        }
        // Check the team
        if (beacon.getOwnership() == null || !beacon.getOwnership().equals(team)) {
            player.sendMessage(ChatColor.RED + "You must capture this beacon first!");
            event.setCancelled(true);
            return;
        }
        if (event.getItem().getType().equals(Material.PAPER)) {
            // Make a map!
            player.sendMessage(ChatColor.GREEN + "You made a beacon map! Take it to another beacon to link them up!");
            int amount = event.getItem().getAmount() - 1;
            MapView map = Bukkit.createMap(getBeaconzWorld());
            //map.setWorld(getBeaconzWorld());
            map.setCenterX(beacon.getX());
            map.setCenterZ(beacon.getZ());
            map.getRenderers().clear();
            map.addRenderer(new BeaconMap(getBeaconzPlugin()));
            event.getItem().setType(Material.MAP);
            event.getItem().setAmount(1);
            event.getItem().setDurability(map.getId());
            // Each map is unique and the durability defines the map ID, register it
            getRegister().addBeaconMap(map.getId(), beacon);
            //getLogger().info("DEBUG: beacon id = " + beacon.getId());
            if (amount > 0) {
                HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(new ItemStack(Material.PAPER, amount));
                if (!leftOver.isEmpty()) {
                    for (ItemStack stack: leftOver.values()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), stack);
                    }
                }
            }
            ItemMeta meta = event.getItem().getItemMeta();
            meta.setDisplayName("Beacon map for " + beacon.getName());
            event.getItem().setItemMeta(meta);
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
            event.setCancelled(true);
            if (linkBeacons(player, team, beacon, mappedBeacon)) {
                player.sendMessage(ChatColor.GREEN + "The map disintegrates!");
                player.setItemInHand(null);
            }
        }
    }


    /**
     * Tries to link two beacons
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
        Line2D proposedLink = new Line2D.Double(beacon.getLocation(), otherBeacon.getLocation());
        // Check if the link crosses opposition team's links
        //getLogger().info("DEBUG: Check if the link crosses opposition team's links");
        for (Line2D line : getRegister().getEnemyLinks(team)) {
            //getLogger().info("DEBUG: checking line " + line.getP1() + " to " + line.getP2());
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
            player.getWorld().playSound(player.getLocation(), Sound.FIREWORK_LARGE_BLAST, 1F, 1F);
            player.getWorld().spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
            if (Settings.pairLinking) {
                // Tell the other player if it was done via a pairing
                if (standingOn.containsValue(otherBeacon)) {
                    Player otherPlayer = getServer().getPlayer(standingOn.inverse().get(otherBeacon));
                    if (otherPlayer != null) {
                        otherPlayer.sendMessage(ChatColor.GREEN + "Link created!");
       
                        otherPlayer.getWorld().playSound(otherPlayer.getLocation(), Sound.FIREWORK_LARGE_BLAST, 1F, 1F);
                        otherPlayer.getWorld().spawnEntity(otherPlayer.getLocation(), EntityType.EXPERIENCE_ORB);
                    }
                    // Tell the team
                    getMessages().tellTeam(player.getUniqueId(), player.getDisplayName() + ChatColor.GREEN + " and " + otherPlayer.getDisplayName()
                            + ChatColor.GREEN + " created a link!");
                    // Taunt other teams
                    getMessages().tellOtherTeams(team, ChatColor.GOLD + team.getDisplayName() + " team made a link!");
                }
            } else {
                // Tell the team
                getMessages().tellTeam(player.getUniqueId(), player.getDisplayName() + ChatColor.GREEN + " created a link!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Link could not be created!");
            return false;
        }
        if (result.getFieldsMade() > 0) {
            if (result.getFieldsMade() == 1) {
                player.sendMessage(ChatColor.GREEN + "Triangle created! New score = " + getScorecard().getScore(team));
                getMessages().tellTeam(player.getUniqueId(), player.getDisplayName() + ChatColor.GREEN + " created a triangle! New team score = " + getScorecard().getScore(team));
                // Taunt other teams
                getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team made a tringle!");
            } else {
                player.sendMessage(ChatColor.GREEN + String.valueOf(result.getFieldsMade()) + " triangles created! New score = " + getScorecard().getScore(team));
                getMessages().tellTeam(player.getUniqueId(), player.getDisplayName() + ChatColor.GREEN + " created " + String.valueOf(result.getFieldsMade()) + " triangles! New team score = " + getScorecard().getScore(team));
                // Taunt other teams
                getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team made " + String.valueOf(result.getFieldsMade()) + " triangles!");
            }
            for (int i = 0; i < result.getFieldsMade(); i++) {
                player.getWorld().spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
            }
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

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        World world = event.getTo().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Only proceed if there's been a change in X or Z coords
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        // Get the player's team
        Team team = getScorecard().getTeam(event.getPlayer());
        if (team == null) {
            return;
        }
        // Run the following if players can create links by standing on them in pairs (or more)
        if (Settings.pairLinking) {
            // Check if player is standing on a beacon
            BeaconObj beacon = getRegister().getBeaconAt(event.getTo().getBlockX(), event.getTo().getBlockZ());
            if (beacon != null) {
                // Check if the beacon is captured by this team
                if (beacon.getOwnership() != null && beacon.getOwnership().equals(team)) {
                    // Check if they are on the same beacon
                    BeaconObj currentBeacon = standingOn.get(event.getPlayer().getUniqueId());
                    if (currentBeacon == null || !currentBeacon.equals(beacon)) {
                        standingOn.remove(event.getPlayer().getUniqueId());
                        // Check if any other team players are on beacons and link to them
                        for (UUID others : standingOn.keySet()) {
                            Player player = getServer().getPlayer(others);
                            if (player != null) {
                                // Check team
                                if (getScorecard().getTeam(player).equals(team)) {
                                    // Only do if beacons are different locations
                                    if (player.getLocation().getBlockX() != event.getTo().getBlockX() 
                                            && player.getLocation().getBlockZ() != event.getTo().getBlockZ()) {
                                        // Check if this link already exists
                                        if (!beacon.getLinks().contains(standingOn.get(others))) {
                                            // Try to link
                                            linkBeacons(event.getPlayer(),team,beacon,standingOn.get(others));
                                        }
                                    }
                                }
                            }
                        }
                        // Forceput removes any other player's on this beacon
                        standingOn.forcePut(event.getPlayer().getUniqueId(), beacon); 
                    }
                }
            } else {
                // Remove player from map
                standingOn.remove(event.getPlayer().getUniqueId());
            }
        }
        // Check the From
        List<TriangleField> from = getRegister().getTriangle(event.getFrom().getBlockX(), event.getFrom().getBlockZ());
        // Check the To
        List<TriangleField> to = getRegister().getTriangle(event.getTo().getBlockX(), event.getTo().getBlockZ());
        // Outside any field
        if (from.isEmpty() && to.isEmpty()) {
            return;
        }
        // Check if to is not a triangle
        if (to.size() == 0) {
            // Leaving a control triangle
            event.getPlayer().sendMessage("Leaving " + from.get(0).getOwner().getDisplayName() + "'s control area");
            return;
        }
        // Apply triangle effects
        applyEffects(event.getPlayer(), to, team);

        // Entering a field, or moving to a stronger field
        if (from.size() < to.size()) {
            event.getPlayer().sendMessage("Now entering " + to.get(0).getOwner().getDisplayName() + "'s area of control level " + to.size());
            return;
        }
        if (to.size() < from.size()) {
            event.getPlayer().sendMessage(to.get(0).getOwner().getDisplayName() + "'s control level dropping to " + to.size());
            return;
        }
    }

    /**
     * Applies triangle effects to a player
     * @param player
     * @param to
     * @param team
     */
    private void applyEffects(Player player, List<TriangleField> to, Team team) {
        if (to.isEmpty() || team == null) {
            return;
        }
        // Apply bad stuff
        // Enemy team
        Team triangleOwner = to.get(0).getOwner();
        Collection<PotionEffect> effects = new ArrayList<PotionEffect>();
        if (triangleOwner != null && !triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.enemyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.enemyFieldEffects.get(i));
                }
            }
            player.addPotionEffects(effects);
        }
        effects.clear();
        // Friendly triangle
        if (triangleOwner != null && triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.friendlyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.friendlyFieldEffects.get(i));
                }
            }
            player.addPotionEffects(effects);
        }         
    }
}
