/*
 * Copyright (c) 2015 - 2016 tastybento, planetguy
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

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
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
 * Note added by EBaldino: in order to be able to regenerate and repopulate chunks for specific game regions,
 * and considering that other plugins may regenerate a chunk in an active game area, I am removing this from
 * the world's populators and calling it explicitly in WorldListener.onChunkLoad
 *
 */
public class BeaconPopulator extends BlockPopulator {
    private final Beaconz plugin;
    private static final boolean DEBUG = false;

    public BeaconPopulator(Beaconz plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(World world, Random unused, Chunk source) {
        Boolean cornerBeacon = false;
        Integer cornerX = null;
        Integer cornerZ = null;
        
        if (plugin.getRegister() == null) {
            // Not ready!
            return;
        }

        // Make sure we're within the boundaries of a game
        if (plugin.getGameMgr() != null) {
            int X = source.getX();
            int Z = source.getZ();
            
            if (plugin.getGameMgr().getLobby() == null) {
                // No lobby yet
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG:no lobby yet");
                return;
            }
            // Don't do anything in the lobby
            if (plugin.getGameMgr().getLobby().containsPoint(X * 16, Z * 16)) {
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG: no beaconz in lobby");
                return;
            }
            if (plugin.getGameMgr().getLobby().containsPoint(X * 16 + 15, Z * 16 + 15)) {
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG: no beaconz in lobby");
                return;
            }            
            
            // Don't do anything unless inside a region
            // Check min coords
            Region region1 = plugin.getGameMgr().getRegion(X * 16, Z * 16);
            if (region1 == null) {
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG: non-region");
                return;
            }
            // Check max coords of this chunk
            Region region2 = plugin.getGameMgr().getRegion(X * 16 + 15, Z * 16 + 15);
            if (region2 == null || region1 != region2) {
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG: non-region");
                return;
            }
            // If we're in the corner chunk of a region, get the coordinates offset to build the corner beacon
            int cX = X << 4;
            int cZ = Z << 4;
            int xMin = (int) region1.corners()[0].getX();
            int xMax = (int) region1.corners()[1].getX()-16;
            int zMin = (int) region1.corners()[0].getY();
            int zMax = (int) region1.corners()[1].getY()-16;
            if (cX >= xMin && cX < xMin + 16 && cZ >= zMin && cZ < zMin + 16) {cornerX = 1;  cornerZ = 1; }
            if (cX <= xMax && cX > xMax - 16 && cZ <= zMax && cZ > zMax - 16) {cornerX = 14; cornerZ = 14;} 
            if (cX <= xMax && cX > xMax - 16 && cZ >= zMin && cZ < zMin + 16) {cornerX = 14; cornerZ = 1; } 
            if (cX >= xMin && cX < xMin + 16 && cZ <= zMax && cZ > zMax - 16) {cornerX = 1;  cornerZ = 14;}  
            cornerBeacon = (cornerX != null && cornerZ != null);

        } else {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: game manager not ready");
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Populating chunk: " + source.getX() + ":" + source.getZ());

        // pseudo-randomly place a beacon
        XorShift gen=new XorShift(new long[] {
                source.getX(),
                source.getZ(),
                //world.getSeed(), world seed is always the same, was causing beacons to be placed always on the same spot - using currentTimeMillis instead...
                System.currentTimeMillis(),
                Settings.seedAdjustment
        });                
        double nd = gen.nextDouble();
        
        // Compare the pseudo-random double generated with the game's beacon distribution threshold        
        Double distribution = plugin.getGameMgr().getRegion(source.getX() << 4, source.getZ() << 4).getGame().getGamedistribution();
        if (distribution == null) distribution = Settings.distribution;
        if (nd < distribution || cornerBeacon) {
            int x;
            int z;
            if (cornerBeacon) {
                // We're building a corner beacon, the relative build coordinates are given by cornerX and cornerZ
                x = cornerX;
                z = cornerZ;
            } else {
                // Otherwise, pick a random relative position in the chunk
                x = gen.nextInt(15);
                z = gen.nextInt(15);
            }
            
            // Check if there is already a beacon here, if so, don't make it again
            // This should never happen...
            if (plugin.getRegister() != null) {
                if (plugin.getRegister().getBeaconAt((source.getX() * 16 + x), (source.getZ()*16 + z)) != null) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Beacon already at " + (source.getX() * 16 + x) + "," + (source.getZ()*16 + z));
                    return;
                }
            }
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Creating beacon at " + (source.getX() * 16 + x) + "," + (source.getZ()*16 + z));

            // Figure out at which height the beacon should be placed
            int y = source.getChunkSnapshot().getHighestBlockYAt(x, z);
            Block b = source.getBlock(x, y, z);
            if (b.getType().equals(Material.SNOW)) {
                // There can be snow in trees, so need to move down to ground level
                while (y > 0 && (source.getBlock(x, y, z).getType().equals(Material.SNOW) 
                        || source.getBlock(x, y, z).getType().equals(Material.AIR)
                        || Tag.LEAVES.isTagged(source.getBlock(x, y, z).getType()))) {
                    y--;
                }
                b = source.getBlock(x, y, z);
            }      
            // Don't make in the ocean or deep ocean because they are too easy to find.
            // Frozen ocean okay for now.
            if (b.getBiome().equals(Biome.OCEAN) || b.getBiome().equals(Biome.DEEP_OCEAN)) {
                return;
            }
            while (b.getType().equals(Material.AIR) 
                    || Tag.LEAVES.isTagged(b.getType())
                    || b.getType().equals(Material.BROWN_MUSHROOM_BLOCK) 
                    || b.getType().equals(Material.RED_MUSHROOM_BLOCK) 
                    || b.getType().equals(Material.OBSIDIAN)) {
                // if found an obsidian, we only keep going down if it's NOT capping a beacon .. this shouldn't really happen either, since we're regenerating the chunk... 
                // ... but, just in case, it should help avoid the creation of diamond towers, which were plentiful during testing...
                if (b.getType().equals(Material.OBSIDIAN) && !b.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
                    break;
                }
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
