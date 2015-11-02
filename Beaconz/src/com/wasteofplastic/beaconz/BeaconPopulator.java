package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
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
    private Beaconz plugin;

    /**
     * @param plugin
     */
    public BeaconPopulator(Beaconz plugin) {
	this.plugin = plugin;
    }

    @Override
    public void populate(World world, Random unused, Chunk source) {
	//plugin.getLogger().info("DEBUG: Chunk x=" + source.getX() + " z=" + source.getZ() + " seed=" + world.getSeed() + " seed adj =" + Settings.seedAdjustment);
	// pseudo-randomly place a beacon
	XorShift gen=new XorShift(new long[] {
		(long)source.getX(),
		(long)source.getZ(),
		world.getSeed(),
		Settings.seedAdjustment
	});
	double nd = gen.nextDouble();
	//plugin.getLogger().info("DEBUG: " + nd);
	if (nd < Settings.distribution) {
	    int x = gen.nextInt(16);
	    int z = gen.nextInt(16);
	    int y = source.getChunkSnapshot().getHighestBlockYAt(x, z) - 1;
	    Block b = source.getBlock(x, y, z);
	    // Don't make in the ocean or deep ocean because they are too easy to find.
	    // Frozen ocean okay for now.
	    if (b.getBiome().equals(Biome.OCEAN) || b.getBiome().equals(Biome.DEEP_OCEAN)) {
		return;
	    }
	    if (b.getType().equals(Material.LEAVES) || b.getType().equals(Material.LEAVES_2)
		    || b.getType().equals(Material.LOG) || b.getType().equals(Material.LOG_2)) {
		return;
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
	    plugin.getRegister().addBeacon(null, new Point2D.Double((source.getX() * 16 + x), (source.getZ()*16 + z)));
	}
    }
}
