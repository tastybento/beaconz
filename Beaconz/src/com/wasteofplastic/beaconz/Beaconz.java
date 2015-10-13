package com.wasteofplastic.beaconz;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

public class Beaconz extends JavaPlugin {
    BeaconPopulator beaconPop;
    Register register;
    World beaconzWorld;
    BlockPopulator bp;
    private Scorecard scorecard;

    @Override
    public void onEnable() {
	final Beaconz plugin = this;
	// Save the default config from the jar
	saveDefaultConfig();

	// Create the block populator
	bp = new BeaconPopulator(this);
	// Check to see if the world exists, and if not, make it
	final String worldName = getConfig().getString("world.name", "beaconz");
	if (getServer().getWorld(worldName) == null) {
	    // Used when first running because world does not exist at this point
	    getServer().getScheduler().runTask(this, new Runnable() {

		public void run() {
		    // World doesn't exist, so make it
		    getLogger().info("World is '" + worldName + "'");
		    beaconzWorld = WorldCreator.name(worldName).type(WorldType.NORMAL).environment(World.Environment.NORMAL).createWorld();
		    beaconzWorld.getPopulators().add(bp);
		    
		}});
	} else {
	    // Used on reload
	    beaconzWorld = getServer().getWorld(worldName);
	}
	
	// Register the listeners - block break etc.
	BeaconListeners ev = new BeaconListeners(this);
	getServer().getPluginManager().registerEvents(ev, this);
	
	// Register command(s)
	getCommand("beaconz").setExecutor(new CmdHandler(this));
	
	// Run commands that need to be run 1 tick after start
	getServer().getScheduler().runTask(this, new Runnable() {

	    public void run() {
		// Load the scorecard
		scorecard = new Scorecard(plugin);
		// Add teams
		MaterialData teamBlock = new MaterialData(Material.STAINED_GLASS,(byte) 11);
		scorecard.addTeam("Blue", teamBlock);
		teamBlock = new MaterialData(Material.STAINED_GLASS,(byte) 14);
		scorecard.addTeam("Red", teamBlock);
		// Load the beacon register
		register = new Register(plugin);
		register.loadRegister();
	    }});
	
    }
    
    @Override
    public void onDisable()
    {
	// Save the register
	if (register != null) {
	    register.saveRegister();
	}
    }

    /**
     * @return the register
     */
    public Register getRegister() {
        return register;
    }

    /**
     * @return the beaconzWorld
     */
    public World getBeaconzWorld() {
        return beaconzWorld;
    }

    /**
     * @return the bp
     */
    public BlockPopulator getBp() {
        return bp;
    }

    /**
     * @return the scorecard
     */
    public Scorecard getScorecard() {
        return scorecard;
    }

}
