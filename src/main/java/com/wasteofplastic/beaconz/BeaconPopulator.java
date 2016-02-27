/*
 * Copyright (c) 2015 tastybento, planetguy
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

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.BlockPopulator;

import com.wasteofplastic.include.it.unimi.dsi.util.XorShift;


/** 
 * BeaconPopulator class
 * @author TastyBento
 *
 * This is called every time a chunk is (re)generated in the world
 * The idea is to place a single beacon on a chunk if a XorShift 
 * generates a random number below the Settings.distribution threshold
 * If Settings.distribution were 1, every chunk would get a single beacon;
 * the lower it is, the fewer chunks get a beacon and the beacons
 * are more spread out in the world.
 *  
 *
 */
public class BeaconPopulator extends BlockPopulator {
    private final Beaconz plugin;

    public BeaconPopulator(Beaconz plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(World world, Random unused, Chunk source) {
        //Bukkit.getLogger().info("DEBUG: populator called. ");

    	// If we're regenerating this chunk from within the game (e.g. a "reset" command), don't do anything
    	if (Settings.dontpopulate.contains(source.getX() + ":" + source.getZ())) {  
            //plugin.getLogger().info("DEBUG: SKIPPING chunk: " + source.getX() + ":" + source.getZ());     
    		return;
    	}    	
    	
        // Don't bother to make anything if it is outside the border. Make it just a bit smaller than the border
    	// THERE IS NO BORDER ANYMORE, THIS MAY BE REMOVED
        if (Settings.borderSize > 0) {
            int minX = (Settings.xCenter - Settings.borderSize/2) / 16 + 1;
            int minZ = (Settings.zCenter - Settings.borderSize/2) / 16 + 1;
            int maxX = (Settings.xCenter + Settings.borderSize/2) / 16 - 1;
            int maxZ = (Settings.zCenter + Settings.borderSize/2) / 16 - 1;
            if (source.getX() < minX || source.getX() > maxX || source.getZ() < minZ || source.getZ() > maxZ) {
                return;
            }
        }
        // Make sure we're within the boundaries of a game
        if (plugin.getGameMgr() != null) {
        	// Don't do anything in the lobby
        	if (plugin.getGameMgr().getLobby().containsPoint(source.getX() * 16.0, source.getZ() * 16.0)) {
        		return;
        	}
        	// Don't do anything unless inside a region
        	Region region = plugin.getGameMgr().getRegion(source.getX() * 16.0, source.getZ() * 16.0);
        	if (region == null) {
        		return;
        	}
        }        
        
        //plugin.getLogger().info("DEBUG: Populating chunk: " + source.getX() + ":" + source.getZ());        
        
        // pseudo-randomly place a beacon
        XorShift gen=new XorShift(new long[] {
                (long)source.getX(),
                (long)source.getZ(),
                world.getSeed(),
                Settings.seedAdjustment
        });
        double nd = gen.nextDouble();
        //plugin.getLogger().info("DEBUG: next double = " + nd);
        if (nd < Settings.distribution) {
            int x = gen.nextInt(16);
            int z = gen.nextInt(16);
            // Check if there is already a beacon here, if so, don't make it again
            if (plugin.getRegister().getBeaconAt((source.getX() * 16 + x), (source.getZ()*16 + z)) != null) {
                plugin.getLogger().info("DEBUG: Beacon already at " + (source.getX() * 16 + x) + "," + (source.getZ()*16 + z));
                return;
            }
            
            plugin.getLogger().info("DEBUG: Creating beacon at " + (source.getX() * 16 + x) + "," + (source.getZ()*16 + z));
            
            int y = source.getChunkSnapshot().getHighestBlockYAt(x, z);            
            Block b = source.getBlock(x, y, z);
            // Don't make in the ocean or deep ocean because they are too easy to find.
            // Frozen ocean okay for now.
            if (b.getBiome().equals(Biome.OCEAN) || b.getBiome().equals(Biome.DEEP_OCEAN)) {
                return;
            }
            while (b.getType().equals(Material.AIR) || b.getType().equals(Material.LEAVES) || b.getType().equals(Material.LEAVES_2)) {
                y--;
                if (y == 0) {
                    // Oops, nothing here
                    return;
                }
                b = source.getBlock(x, y, z);
            }
            // Else make it into a beacon
            //beacons.add(new Vector(x,y,z));
            //Bukkit.getLogger().info("DEBUG: made beacon at " + (source.getX() * 16 + x) + " " + y + " " + (source.getZ()*16 + z) );
            b.setType(Material.BEACON);
            // Add the capstone
            b.getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
            // Create the pyramid
            b = b.getRelative(BlockFace.DOWN);

            // All diamond blocks for now
            b.setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.SOUTH).setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.SOUTH_EAST).setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.SOUTH_WEST).setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.EAST).setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.WEST).setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.NORTH).setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.NORTH_EAST).setType(Material.DIAMOND_BLOCK);
            b.getRelative(BlockFace.NORTH_WEST).setType(Material.DIAMOND_BLOCK);

            // Register the beacon
            plugin.getRegister().addBeacon(null, (source.getX() * 16 + x), y, (source.getZ()*16 + z));
        }
    }
}
