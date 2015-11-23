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

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.generator.BlockPopulator;

import com.wasteofplastic.include.it.unimi.dsi.util.XorShift;

public class BeaconPopulator extends BlockPopulator {
    private final Beaconz plugin;

    public BeaconPopulator(Beaconz plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(World world, Random unused, Chunk source) {
        //Bukkit.getLogger().info("DEBUG: populator called. ");
        //plugin.getLogger().info("DEBUG: Chunk x=" + source.getX() + " z=" + source.getZ() + " Settings.xCenter=" + Settings.xCenter
        //        + " Settings.size =" + Settings.size);

        // Don't bother to make anything if it is outside the border. Make it just a bit smaller than the border
        if (Settings.borderSize > 0) {
            int minX = (Settings.xCenter - Settings.borderSize/2) / 16 + 1;
            int minZ = (Settings.zCenter - Settings.borderSize/2) / 16 + 1;
            int maxX = (Settings.xCenter + Settings.borderSize/2) / 16 - 1;
            int maxZ = (Settings.zCenter + Settings.borderSize/2) / 16 - 1;
            if (source.getX() < minX || source.getX() > maxX || source.getZ() < minZ || source.getZ() > maxZ) {
                return;
            }
        }
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
            // Check if there is already a beacon here, if not, don't make it again
            if (plugin.getRegister().getBeaconAt((source.getX() * 16 + x), (source.getZ()*16 + z)) != null) {
                return;
            }
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
            Bukkit.getLogger().info("DEBUG: made beacon at " + (source.getX() * 16 + x) + " " + y + " " + (source.getZ()*16 + z) );
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
