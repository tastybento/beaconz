/*
 * Copyright (c) 2015 - 2026 tastybento, planetguy
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

package com.wasteofplastic.beaconz.generator;

import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.core.Region;


/**
 * BeaconPopulator class
 * @author tastybento
 * <p>
 * This is called every time a chunk is generated in the world
 * The idea is to place a single beacon on a chunk if a XorShift
 * generates a random number below the Settings.distribution threshold
 * If Settings.distribution were 1, every chunk would get a single beacon;
 * the lower it is, the fewer chunks get a beacon and the beacons
 * are more spread out in the world.
 */
public class BeaconPopulator extends BlockPopulator {
    private final Beaconz plugin;
    private static final boolean DEBUG = false;

    public BeaconPopulator(Beaconz plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random unused, int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
        boolean cornerBeacon = false;
        Integer cornerX = null;
        Integer cornerZ = null;

        if (plugin.getRegister() == null) {
            // Not ready!
            return;
        }

        // Make sure we're within the boundaries of a game
        if (plugin.getGameMgr() != null) {

            if (plugin.getGameMgr().getLobby() == null) {
                // No lobby yet
                if (DEBUG)
                    plugin.getLogger().info("DEBUG:no lobby yet");
                return;
            }
            // Don't do anything in the lobby
            if (plugin.getGameMgr().getLobby().containsPoint(chunkX * 16, chunkZ * 16)) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: no beaconz in lobby");
                return;
            }
            if (plugin.getGameMgr().getLobby().containsPoint(chunkX * 16 + 15, chunkZ * 16 + 15)) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: no beaconz in lobby");
                return;
            }            

            // Don't do anything unless inside a region
            // Check min coords
            Region region1 = plugin.getGameMgr().getRegion(chunkX * 16, chunkZ * 16);
            if (region1 == null) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: non-region");
                return;
            }
            // Check max coords of this chunk
            Region region2 = plugin.getGameMgr().getRegion(chunkX * 16 + 15, chunkZ * 16 + 15);
            if (region2 == null || region1 != region2) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: non-region");
                return;
            }
            // If we're in the corner chunk of a region, get the coordinates offset to build the corner beacon
            int cX = chunkX << 4;
            int cZ = chunkZ << 4;
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
            plugin.getLogger().info("DEBUG: Populating chunk: " + chunkX + ":" + chunkZ);

        // pseudo-randomly place a beacon
        // Mix all seed components using XOR and bit rotation
        long seed4 = Settings.seedAdjustment;

        long combinedSeed = chunkX ^
                Long.rotateLeft(chunkZ, 17) ^
                Long.rotateLeft(seed4, 42);

        RandomGenerator gen = RandomGeneratorFactory.of("Xoshiro256PlusPlus").create(combinedSeed);
        double nd = gen.nextDouble();

        // Compare the pseudo-random double generated with the game's beacon distribution threshold
        Region region = plugin.getGameMgr().getRegion(chunkX << 4, chunkZ << 4);
        if (region == null || region.getGame() == null) {
            return;
        }
        double distribution = region.getGame().getGamedistribution();
        if (nd < distribution || cornerBeacon) {
            int x;
            int z;
            if (cornerBeacon) {
                // We're building a corner beacon, the relative build coordinates are given by cornerX and cornerZ
                x = cornerX;
                z = cornerZ;
            } else {
                // Otherwise, pick a random relative position in the chunk
                x = gen.nextInt(16);
                z = gen.nextInt(16);
            }

            // Check if there is already a beacon here, if so, don't make it again
            // This should never happen...
            if (plugin.getRegister() != null) {
                if (plugin.getRegister().getBeaconAt((chunkX * 16 + x), (chunkZ * 16 + z)) != null) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Beacon already at " + (chunkX * 16 + x) + "," + (chunkZ * 16 + z));
                    return;
                }
            }
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Creating beacon at " + (chunkX * 16 + x) + "," + (chunkZ * 16 + z));

            // Calculate world coordinates
            int worldX = chunkX * 16 + x;
            int worldZ = chunkZ * 16 + z;

            // Figure out at which height the beacon should be placed
            // Scan downward from max height to find the highest non-air block
            int y = worldInfo.getMaxHeight() - 1;
            while (y > worldInfo.getMinHeight() && limitedRegion.getType(worldX, y, worldZ) == Material.AIR) {
                y--;
            }
            
            if (y <= worldInfo.getMinHeight()) {
                // No solid blocks found
                return;
            }

            // Check the block at this position
            Material blockType = limitedRegion.getType(worldX, y, worldZ);
            Biome biome = limitedRegion.getBiome(worldX, y, worldZ);

            if (blockType == Material.SNOW) {
                // There can be snow in trees, so need to move down to ground level
                while (y > worldInfo.getMinHeight() &&
                        (limitedRegion.getType(worldX, y, worldZ) == Material.SNOW
                        || limitedRegion.getType(worldX, y, worldZ) == Material.AIR
                        || Tag.LEAVES.isTagged(limitedRegion.getType(worldX, y, worldZ)))) {
                    y--;
                }
                blockType = limitedRegion.getType(worldX, y, worldZ);
                biome = limitedRegion.getBiome(worldX, y, worldZ);
            }
            // Don't make in the ocean or deep ocean because they are too easy to find.
            // Frozen ocean okay for now.
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN) {
                return;
            }
            while (blockType == Material.AIR
                    || Tag.LEAVES.isTagged(blockType)
                    || blockType == Material.BROWN_MUSHROOM_BLOCK
                    || blockType == Material.RED_MUSHROOM_BLOCK
                    || blockType == Material.OBSIDIAN) {
                // if found an obsidian, we only keep going down if it's NOT capping a beacon
                if (blockType == Material.OBSIDIAN) {
                    int belowY = y - 1;
                    if (belowY < worldInfo.getMinHeight()
                            || !limitedRegion.isInRegion(worldX, belowY, worldZ)
                            || limitedRegion.getType(worldX, belowY, worldZ) != Material.BEACON) {
                        break;
                    }
                }
                y--;
                if (y <= worldInfo.getMinHeight()) {
                    // Oops, nothing here
                    return;
                }
                // Verify the new y position is within the LimitedRegion before accessing it
                if (!limitedRegion.isInRegion(worldX, y, worldZ)) {
                    return;
                }
                blockType = limitedRegion.getType(worldX, y, worldZ);
            }

            // Else make it into a beacon
            // Verify beacon position is within the LimitedRegion
            if (!limitedRegion.isInRegion(worldX, y, worldZ)) {
                return;
            }
            limitedRegion.setType(worldX, y, worldZ, Material.BEACON);
            // Add the capstone
            if (y + 1 <= worldInfo.getMaxHeight() && limitedRegion.isInRegion(worldX, y + 1, worldZ)) {
                limitedRegion.setType(worldX, y + 1, worldZ, Material.OBSIDIAN);
            }

            // Create the pyramid (one level below the beacon)
            int pyramidY = y - 1;

            // All diamond blocks for now - 3x3 pyramid base
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int blockX = worldX + dx;
                    int blockZ = worldZ + dz;
                    if (limitedRegion.isInRegion(blockX, pyramidY, blockZ)) {
                        limitedRegion.setType(blockX, pyramidY, blockZ, Material.DIAMOND_BLOCK);
                    }
                }
            }

            // Register the beacon
            plugin.getRegister().addBeacon(null, worldX, y, worldZ);
        }
    }
}
