package com.wasteofplastic.beaconz;

import java.util.Random;
import java.util.Set;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Generates the beaconz world
 */
public class BeaconzChunkGen extends ChunkGenerator {
    
    private static final Set<Biome> OCEANS = Set.of(Biome.OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.DEEP_FROZEN_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN, Biome.DEEP_OCEAN, Biome.FROZEN_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN);
    private final Beaconz plugin;
    
    public BeaconzChunkGen(Beaconz plugin) {
        super();
        this.plugin = plugin;
    }

    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        if (random.nextDouble() < 0.1) {
            int x = 7;
            int z = 7;
            int maxY = chunkData.getHeight(HeightMap.WORLD_SURFACE_WG, x, z);
            maxY = Math.max(maxY, worldInfo.getMinHeight() + 1); // Just in case
            Biome biome = chunkData.getBiome(x, maxY, z);
            if (OCEANS.contains(biome)) {
                // No beacons in oceans
                return;
            }
            // Else make it into a pre-beacon
            // Add the capstone
            chunkData.setBlock(x, maxY, z, Material.OBSIDIAN);
            // Beacon
            chunkData.setBlock(x, maxY - 1, z, Material.BEACON);
            // Create the pyramid
            // All diamond blocks for now
            chunkData.setBlock(x, maxY - 2, z, Material.DIAMOND_BLOCK.createBlockData());
            chunkData.setRegion(x-1, maxY - 3, z-1, x+2, maxY - 2, z+2, Material.DIAMOND_BLOCK);
            // Register the beacon
            int posX = (chunkX << 4)  + x;
            int posZ = (chunkZ << 4) + z;
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
