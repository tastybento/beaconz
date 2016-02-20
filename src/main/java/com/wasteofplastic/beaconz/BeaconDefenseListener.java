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

import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        // Only allow blocks at the top of defenses to be removed by explosions
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
            // Only allow blocks at the top be removed by pistons.
            /*
            Block testBlock = b.getRelative(event.getDirection());
            BeaconObj beacon = getRegister().getBeaconAt(testBlock.getX(),testBlock.getZ());
            if (beacon != null && beacon.getY() < testBlock.getY()) {
                event.setCancelled(true);
            }
             */
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
                player.sendMessage(ChatColor.RED + "You must join a game to play in this world");            
                getGameMgr().getLobby().tpToRegionSpawn(player);
                return;        		
        	} else {
        		player.sendMessage(ChatColor.RED + "You are not in a team.");
        		return;
        	} 
        }
        Team team = sc.getTeam(player);
        // Check if block is a beacon extension block
        Block block = event.getBlock();
        if (block.getType().equals(Material.EMERALD_BLOCK)) {
            // Check to see if it is being placed adjacent to a beacon
            Block adjBlock = block.getRelative(BlockFace.NORTH);
            BeaconObj adjacentBeacon = getRegister().getBeaconDefenseAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
            if (adjacentBeacon == null) {
                adjBlock = block.getRelative(BlockFace.SOUTH);
                adjacentBeacon = getRegister().getBeaconDefenseAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
                if (adjacentBeacon == null) {
                    adjBlock = block.getRelative(BlockFace.EAST);
                    adjacentBeacon = getRegister().getBeaconDefenseAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
                    if (adjacentBeacon == null) {
                        adjBlock = block.getRelative(BlockFace.WEST);
                        adjacentBeacon = getRegister().getBeaconDefenseAt(new Point2D.Double(adjBlock.getX(), adjBlock.getZ()));
                    }
                }
            }
            if (adjacentBeacon != null) {
                // Check block is at the right height
                if (block.getY() + 1 == adjacentBeacon.getY()) {
                    // Check if the team is placing a block on their own beacon or not
                    if (adjacentBeacon.getOwnership() == null || !adjacentBeacon.getOwnership().equals(team)) {
                        player.sendMessage(ChatColor.RED + "You can only extend a captured beacon!");
                        event.setCancelled(true);
                        return;
                    }
                    // Check what blocks are above the emerald block
                    int highestBlock = getHighestDefenseBlockYAt(block, this.getBeaconzWorld().getMaxHeight()-1) + 1;
                    //getLogger().info("DEBUG: highest block y = " + highestBlock + " difference = " + (highestBlock - adjacentBeacon.getY()));
                    if (highestBlock - adjacentBeacon.getY() > Settings.defenseHeight) {
                        event.getPlayer().sendMessage(ChatColor.RED + "Too many blocks above this block!");
                        event.setCancelled(true);
                        return;
                    }
                    // Extend beacon
                    getRegister().addBeaconDefenseBlock(block.getLocation(), adjacentBeacon);
                    player.sendMessage(ChatColor.GREEN + "You extended the beacon!");
                    // TODO: give experience?
                    return;
                }
            }
        }

        // Check if the block is a defensive block
        BeaconObj beacon = getRegister().getBeaconDefenseAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null) {
            return;
        }
        // Check blocks below the beacon
        if (beacon.getY() > block.getY()) {
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
        // Check if the player has the experience level required to place the block
        int level = block.getY() - beacon.getY();
        int levelRequired = 0;
        getLogger().info("DEBUG: level = " + level);
        try {
            levelRequired = Settings.defenseLevels.get(level);
            if (player.getLevel() < levelRequired) {
                player.sendMessage(ChatColor.RED + "You need to be level " + levelRequired + " to place blocks that high around the beacon!");
                event.setCancelled(true);
                return;
            }
        } catch (Exception e) {
            getLogger().severe("Defense level for height " + level + " does not exist!");
        }
        String levelPlaced = "";
        if (levelRequired > 0) {
            levelPlaced = " [Level " + levelRequired + "]";
        }
        // Check what type of block it is:
        // TODO: Add implications of placement.
        switch (event.getBlock().getType()) {
        case ACACIA_FENCE:
        case BIRCH_FENCE:
        case DARK_OAK_FENCE:
        case JUNGLE_FENCE:
        case SPRUCE_FENCE:
        case FENCE:
            player.sendMessage(ChatColor.GREEN + "Fence protection!" + levelPlaced);
            break;
        case ACACIA_FENCE_GATE:
        case BIRCH_FENCE_GATE:
        case DARK_OAK_FENCE_GATE:
        case JUNGLE_FENCE_GATE:
        case SPRUCE_FENCE_GATE:
        case FENCE_GATE:
            player.sendMessage(ChatColor.GREEN + "Gate protection!" + levelPlaced);
            break;
        case BEACON:
            player.sendMessage(ChatColor.GREEN + "Dummy?" + levelPlaced);
            break;
        case BEDROCK:
            player.sendMessage(ChatColor.GREEN + "Ultimate defense! Hope you didn't make a mistake!" + levelPlaced);
            break;
        case BED_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Sleep, sleep, sleep" + levelPlaced);
            break;
        case BOOKSHELF:
            player.sendMessage(ChatColor.GREEN + "Knowledge is power!" + levelPlaced);
            break;
        case BREWING_STAND:
            player.sendMessage(ChatColor.GREEN + "Potion attack!" + levelPlaced);
            break;
        case CAKE_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Hunger benefits" + levelPlaced);
            break;
        case CARPET:
            player.sendMessage(ChatColor.GREEN + "Hmm, pretty!" + levelPlaced);
            break;
        case CAULDRON:
            player.sendMessage(ChatColor.GREEN + "Witch's brew!" + levelPlaced);
            break;
        case CHEST:
            player.sendMessage(ChatColor.GREEN + "I wonder what you will put in it" + levelPlaced);
            break;
        case COAL_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Energy up!" + levelPlaced);
            break;
        case DAYLIGHT_DETECTOR:
            player.sendMessage(ChatColor.GREEN + "Let night be day!" + levelPlaced);
            break;
        case DETECTOR_RAIL:
            break;
        case DIAMOND_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Fortune will smile upon you!" + levelPlaced);
            break;
        case DISPENSER:
            player.sendMessage(ChatColor.GREEN + "Load it up!" + levelPlaced);
            break;
        case DRAGON_EGG:
            player.sendMessage(ChatColor.GREEN + "The end is nigh!" + levelPlaced);
            break;
        case DROPPER:
            player.sendMessage(ChatColor.GREEN + "Drip, drop, drip" + levelPlaced);
            break;
        case EMERALD_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Place adjacent to the beacon base to extend the beacon!" + levelPlaced);
            break;
        case EMERALD_ORE:
            break;
        case ENCHANTMENT_TABLE:
            player.sendMessage(ChatColor.GREEN + "Magic will occur" + levelPlaced);
            break;
        case ENDER_CHEST:
            player.sendMessage(ChatColor.GREEN + "I wonder what is inside?" + levelPlaced);
            break;
        case ENDER_STONE:
            player.sendMessage(ChatColor.GREEN + "End attack!" + levelPlaced);
            break;
        case FLOWER_POT:
            player.sendMessage(ChatColor.GREEN + "I wonder what this will do..." + levelPlaced);
            break;
        case FURNACE:
            player.sendMessage(ChatColor.GREEN + "Fire attack! If it's hot." + levelPlaced);
            break;
        case GLASS:
            player.sendMessage(ChatColor.GREEN + "I can see clearly now" + levelPlaced);
            break;
        case GLOWSTONE:
            player.sendMessage(ChatColor.GREEN + "Glow, glow" + levelPlaced);
            break;
        case GOLD_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Money, money, money" + levelPlaced);
            break;
        case ICE:
            player.sendMessage(ChatColor.GREEN + "Brrr, it's cold" + levelPlaced);
            break;
        case IRON_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Protection" + levelPlaced);
            break;
        case IRON_DOOR_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Knock, knock" + levelPlaced);
            break;
        case JACK_O_LANTERN:
            player.sendMessage(ChatColor.GREEN + "Boo!" + levelPlaced);
            break;
        case LADDER:
            player.sendMessage(ChatColor.GREEN + "Up we go!" + levelPlaced);
            break;
        case LAPIS_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Everything is blue!" + levelPlaced);
            break;
        case LAVA:
            player.sendMessage(ChatColor.GREEN + "Hot stuff!" + levelPlaced);
            break;
        case LEAVES:
        case LEAVES_2:
            player.sendMessage(ChatColor.GREEN + "Camoflage" + levelPlaced);
            break;
        case LEVER:
            player.sendMessage(ChatColor.GREEN + "I wonder what this does!" + levelPlaced);
            break;
        case LOG:
        case LOG_2:
            player.sendMessage(ChatColor.GREEN + "It's a tree!" + levelPlaced);
            break;
        case MELON_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Hungry?" + levelPlaced);
            break;
        case MOB_SPAWNER:
            player.sendMessage(ChatColor.GREEN + "That's what I'm talking about!" + levelPlaced);
            break;
        case MYCEL:
            player.sendMessage(ChatColor.GREEN + "Smelly!" + levelPlaced);
            break;
        case NETHERRACK:
        case NETHER_BRICK:
        case NETHER_BRICK_STAIRS:
        case NETHER_FENCE:
            player.sendMessage(ChatColor.GREEN + "That's not from around here!" + levelPlaced);
            break;
        case NOTE_BLOCK:
            player.sendMessage(ChatColor.GREEN + "I hear things?" + levelPlaced);
            break;
        case OBSIDIAN:
            player.sendMessage(ChatColor.GREEN + "Tough protection!" + levelPlaced);
            break;
        case PACKED_ICE:
            player.sendMessage(ChatColor.GREEN + "Cold, so cold..." + levelPlaced);
            break;
        case PISTON_BASE:
        case PISTON_STICKY_BASE:
            player.sendMessage(ChatColor.GREEN + "Pushy!" + levelPlaced);
            break;
        case POWERED_RAIL:
            player.sendMessage(ChatColor.GREEN + "Power to the people!" + levelPlaced);
            break;
        case PRISMARINE:
            player.sendMessage(ChatColor.GREEN + "Aqua" + levelPlaced);
            break;
        case PUMPKIN:
            player.sendMessage(ChatColor.GREEN + "Farming?" + levelPlaced);
            break;
        case QUARTZ_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Pretty" + levelPlaced);
            break;
        case RAILS:
            player.sendMessage(ChatColor.GREEN + "Where do they go?" + levelPlaced);
            break;
        case REDSTONE_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Power up!" + levelPlaced);
            break;
        case REDSTONE_COMPARATOR:
        case REDSTONE_COMPARATOR_OFF:
        case REDSTONE_COMPARATOR_ON:
            player.sendMessage(ChatColor.GREEN + "What's the question?" + levelPlaced);
            break;
        case REDSTONE_LAMP_OFF:
        case REDSTONE_LAMP_ON:
            player.sendMessage(ChatColor.GREEN + "Light?" + levelPlaced);
            break;
        case REDSTONE_TORCH_OFF:
        case REDSTONE_TORCH_ON:
            player.sendMessage(ChatColor.GREEN + "Power gen" + levelPlaced);
            break;
        case REDSTONE_WIRE:
            player.sendMessage(ChatColor.GREEN + "Does it glow?" + levelPlaced);
            break;
        case RED_SANDSTONE:
        case RED_SANDSTONE_STAIRS:
            player.sendMessage(ChatColor.RED + "It's red" + levelPlaced);
            break;
        case SAND:
            if (block.getData() == (byte)1) {
                player.sendMessage(ChatColor.YELLOW + "It's red" + levelPlaced);
                break; 
            }
        case SANDSTONE:
        case SANDSTONE_STAIRS:
            player.sendMessage(ChatColor.YELLOW + "It's yellow" + levelPlaced);
            break;
        case SEA_LANTERN:
            player.sendMessage(ChatColor.GREEN + "Nice! Sea attack!" + levelPlaced);
            break;
        case SIGN_POST:
            player.sendMessage(ChatColor.GREEN + "Warning message set!" + levelPlaced);
            break;
        case SKULL:
            player.sendMessage(ChatColor.GREEN + "Death to this entity!" + levelPlaced);
            break;
        case SLIME_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Boing, boing, boing!" + levelPlaced);
            break;
        case SNOW_BLOCK:
            player.sendMessage(ChatColor.GREEN + "Cold!" + levelPlaced);
            break;
        case SOUL_SAND:
            player.sendMessage(ChatColor.GREEN + "<scream>" + levelPlaced);
            break;
        case SPONGE:
            player.sendMessage(ChatColor.GREEN + "Slurp!" + levelPlaced);
            break;
        case STAINED_GLASS:
        case STAINED_GLASS_PANE:
            player.sendMessage(ChatColor.GREEN + "Pretty!" + levelPlaced);
            break;
        case STANDING_BANNER:
            player.sendMessage(ChatColor.GREEN + "Be proud!" + levelPlaced);
            break;
        case STATIONARY_LAVA:
        case STATIONARY_WATER:
            player.sendMessage(ChatColor.GREEN + "A moat?" + levelPlaced);
            break;
        case STONE_PLATE:
            player.sendMessage(ChatColor.GREEN + "A trap?" + levelPlaced);
            break;
        case THIN_GLASS:
            player.sendMessage(ChatColor.GREEN + "Not much protection..." + levelPlaced);
            break;
        case TNT:
            player.sendMessage(ChatColor.GREEN + "Explosive protection!" + levelPlaced);
            break;
        case WALL_SIGN:
            player.sendMessage(ChatColor.GREEN + "Send a message!" + levelPlaced);
            break;
        case WEB:
            player.sendMessage(ChatColor.GREEN + "Slow down the enemy!" + levelPlaced);
            break;
       case WOOD_PLATE:
            player.sendMessage(ChatColor.GREEN + "Trap?" + levelPlaced);
            break;
        case WOOL:
            player.sendMessage(ChatColor.GREEN + "Keep warm!" + levelPlaced);
            break;
        case WORKBENCH:
            player.sendMessage(ChatColor.GREEN + "That's helpful!" + levelPlaced);
            break;
        default:
            player.sendMessage(ChatColor.GREEN + "Defense placed" + levelPlaced);
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
                player.sendMessage(ChatColor.RED + "You must join a game to play in this world");            
                getGameMgr().getLobby().tpToRegionSpawn(player);
                return;        		
        	} else {
        		player.sendMessage(ChatColor.RED + "You are not in a team.");
        		return;
        	} 
        }
        Team team = sc.getTeam(player);
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
            //getLogger().info("DEBUG: below beacon");
            return;
        }
        // Check that breakage is being done top-down
        if (block.getY() < getHighestDefenseBlockYAt(block, beacon.getY() + Settings.defenseHeight - 1)) {
            event.getPlayer().sendMessage(ChatColor.RED + "Remove blocks top-down");
            event.setCancelled(true);
            return;
        }
        // Check if the player has the experience level required to break the block
        int level = block.getY() - beacon.getY();
        int levelRequired = 0;
        getLogger().info("DEBUG: level = " + level);
        try {
            levelRequired = Settings.attackLevels.get(level);
            if (player.getLevel() < levelRequired) {
                player.sendMessage(ChatColor.RED + "You need to be level " + levelRequired + " to break blocks that high around the beacon!");
                event.setCancelled(true);
                return;
            }
        } catch (Exception e) {
            getLogger().severe("Attack level for height " + level + " does not exist!");
        }
        // TODO : give exp? Rewards?
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
                player.sendMessage(ChatColor.RED + "You must join a game to play in this world");            
                getGameMgr().getLobby().tpToRegionSpawn(player);
                return;        		
        	} else {
        		player.sendMessage(ChatColor.RED + "You are not in a team.");
        		return;
        	} 
        }
        Team team = sc.getTeam(player);
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
            //getLogger().info("DEBUG: below beacon");
            return;
        }
        // Check that breakage is being done top-down
        if (block.getY() < getHighestDefenseBlockYAt(block, beacon.getY() + Settings.defenseHeight - 1)){
            event.getPlayer().sendMessage(ChatColor.RED + "Remove blocks top-down");
            event.setCancelled(true);
            return;
        }
        // Check if the player has the experience level required to break the block
        int level = block.getY() - beacon.getY();
        int levelRequired = 0;
        getLogger().info("DEBUG: level = " + level);
        try {
            levelRequired = Settings.attackLevels.get(level);
            if (player.getLevel() < levelRequired) {
                player.sendMessage(ChatColor.RED + "You need to be level " + levelRequired + " to attempt to break blocks that high!");
                event.setCancelled(true);
                return;
            }
        } catch (Exception e) {
            getLogger().severe("Attack level for height " + level + " does not exist!");
        }
    }

    /**
     * Gets the highest defense block in the world at x,z starting at the max height a defense block can be
     * @param block
     * @param y
     * @return
     */
    private int getHighestDefenseBlockYAt(Block block, int y) {
        while (y > 0 && getBeaconzWorld().getBlockAt(block.getX(), y, block.getZ()).getType().equals(Material.AIR)) {
            y--;
        };
        //getLogger().info("DEBUG: highest block is at " + y);
        return y;
    }
}
