package com.wasteofplastic.beaconz.generator;

import java.util.Random;
import java.util.Set;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.Beaconz;

/**
 * Generates the beaconz world
 */
public class BeaconzChunkGen extends ChunkGenerator {
    
    private static final Set<Biome> OCEANS = Set.of(Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.FROZEN_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN);
    private static final int X = 7; // Center of chunk
    private static final int Z = 7;
    private final Beaconz plugin;
    
    public BeaconzChunkGen(Beaconz plugin) {
        super();
        this.plugin = plugin;
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        double chance = plugin.getConfig().getDouble("world.distribution", 0.1D);
        if (random.nextDouble() < chance) {
            int maxY = chunkData.getHeight(HeightMap.MOTION_BLOCKING_NO_LEAVES, X, Z);
            maxY = Math.max(maxY, worldInfo.getMinHeight() + 1); // Just in case
            Biome biome = chunkData.getBiome(X, maxY, Z);
            if (OCEANS.contains(biome)) {
                // No beacons in oceans
                return;
            }
            // Else make it into a pre-beacon
            // Add the capstone
            chunkData.setBlock(X, maxY, Z, Material.OBSIDIAN);
            // Beacon
            chunkData.setBlock(X, maxY - 1, Z, Material.BEACON);
            // Create the pyramid
            // All diamond blocks for now
            chunkData.setBlock(X, maxY - 2, Z, Material.DIAMOND_BLOCK.createBlockData());
            chunkData.setRegion(X-1, maxY - 3, Z-1, X+2, maxY - 2, Z+2, Material.DIAMOND_BLOCK);
            // Register the beacon
            int posX = (chunkX << 4)  + X;
            int posZ = (chunkZ << 4) + Z;
            plugin.getRegister().addBeacon(null, posX , maxY - 1, posZ);
        }
    }
    
    @Override
    public boolean shouldGenerateNoise() {
        return true;
    }
    
    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }
    
    @Override
    public boolean shouldGenerateCaves() {
        return true;
    }
    
    @Override
    public boolean shouldGenerateDecorations() {
        return true;
    }
    
    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }
    
    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }
}
