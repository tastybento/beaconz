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

import java.awt.geom.Point2D;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scoreboard.Team;

/**
 * Handles beacon defenses
 * @author tastybento
 *
 */
public class BeaconDefenseListener extends BeaconzPluginDependent implements Listener {

    /**
     * @param plugin
     */
    public BeaconDefenseListener(Beaconz plugin) {
        super(plugin);
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
        // Check if the block is a defensive block
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeaconDefenseAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null) {
            return;
        }
        // Check blocks below the beacon
        if (beacon.getY() > block.getY()) {
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
        // Check if the team is placing a block on their own beacon or not
        if (!beacon.getOwnership().equals(team)) {
            player.sendMessage(ChatColor.RED + "You can only place blocks on a captured beacon!");
            event.setCancelled(true);
            return;
        }
        // Check if the height exceeds the allowed height
        if (beacon.getY() + Settings.defenseHeight - 1 < block.getY()) {
            player.sendMessage(ChatColor.RED + "You can only place blocks up to " + Settings.defenseHeight + " high around the beacon!");
            event.setCancelled(true);
            return;
        }
        // Check what type of block it is:
        // TODO: Add implications of placement.
        switch (event.getBlock().getType()) {
        case ACACIA_DOOR:
        case BIRCH_DOOR:
        case DARK_OAK_DOOR:
        case JUNGLE_DOOR:
        case SPRUCE_DOOR:
            break;
        case ACACIA_FENCE:
        case BIRCH_FENCE:
        case DARK_OAK_FENCE:
        case JUNGLE_FENCE:
        case SPRUCE_FENCE:
        case FENCE:
            player.sendMessage(ChatColor.GREEN + "Fence protection!");
            break;
        case ACACIA_FENCE_GATE:
        case BIRCH_FENCE_GATE:
        case DARK_OAK_FENCE_GATE:
        case JUNGLE_FENCE_GATE:
        case SPRUCE_FENCE_GATE:
        case FENCE_GATE:
            player.sendMessage(ChatColor.GREEN + "Gate protection!");
            break;
        case ACACIA_STAIRS:
        case BIRCH_WOOD_STAIRS:
        case DARK_OAK_STAIRS:
        case JUNGLE_WOOD_STAIRS:
        case SPRUCE_WOOD_STAIRS:
        case WOOD_STAIRS:
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
        // Get the player's team
        Team team = getScorecard().getTeam(player);
        if (team == null) {
            // TODO: Probably should put the player in a team
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
            return;
        }
        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeaconDefenseAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null) {
            return;
        }
        // If same team, then do nothing
        if (team.equals(beacon.getOwnership())) {
            return;
        }
        // Check height
        if (block.getY() < beacon.getHeight()) {
            getLogger().info("DEBUG: below beacon");
            return;
        }
        // Check that breakage is being done top-down
        if (block.getY() != getHighestBlockYAt(block)) {
            event.getPlayer().sendMessage(ChatColor.RED + "Remove blocks top-down");
            event.setCancelled(true);
            return;
        }
        // TODO: Add any other effects?
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
        // Get the player's team
        Team team = getScorecard().getTeam(player);
        if (team == null) {
            // TODO: Probably should put the player in a team
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
            return;
        }
        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeaconDefenseAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null) {
            return;
        }
        // If same team, then do nothing
        if (team.equals(beacon.getOwnership())) {
            return;
        }
        // Check height
        if (block.getY() < beacon.getHeight()) {
            getLogger().info("DEBUG: below beacon");
            return;
        }
        // Check that breakage is being done top-down
        if (block.getY() != getHighestBlockYAt(block)) {
            event.getPlayer().sendMessage(ChatColor.RED + "Remove blocks top-down");
            event.setCancelled(true);
            return;
        }
        // TODO: Add any other effects?
    }

    /**
     * Gets the highest block in the world at x,z ignoring the topermost block at 255 because that is used for glass
     * @param block
     * @return Y value
     */
    private int getHighestBlockYAt(Block block) {
        int y = 254;
        while (y > 0 && getBeaconzWorld().getBlockAt(block.getX(), y, block.getZ()).getType().equals(Material.AIR)) {
            y--;
        };
        //getLogger().info("DEBUG: highest block is at " + y);
        return y;
    }
}
