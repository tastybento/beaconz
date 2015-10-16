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
    static World beaconzWorld;
    private static Beaconz plugin;
    static BlockPopulator bp;
    private Scorecard scorecard;

    @Override
    public void onEnable() {
	plugin = this;
	// Save the default config from the jar
	saveDefaultConfig();
	loadConfig();

	//getServer().getPluginManager().registerEvents(new WorldLoader(this), this);

	// Register command(s)
	getCommand("beaconz").setExecutor(new CmdHandler(this));

	// Create the block populator
	bp = new BeaconPopulator(this);

	// Run commands that need to be run 1 tick after start
	getServer().getScheduler().runTask(this, new Runnable() {

	    public void run() {	
		Beaconz.getBeaconzWorld();
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

		// Register the listeners - block break etc.
		BeaconListeners ev = new BeaconListeners(plugin);
		getServer().getPluginManager().registerEvents(ev, plugin);
	    }});
    }

    @Override
    public void onDisable()
    {
	// Save the register
	if (register != null) {
	    register.saveRegister();
	}
	getConfig().set("world.distribution", Settings.distribution);
	saveConfig();
    }

    /**
     * @return the register
     */
    public Register getRegister() {
	return register;
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

    /**
     * Loads settings from config.yml
     */
    public void loadConfig() {
	Settings.worldName = getConfig().getString("world.name", "beaconz");
	Settings.distribution = getConfig().getDouble("world.distribution", 0.01D);
	Settings.size = getConfig().getInt("world.size",0);
	Settings.xCenter = getConfig().getInt("world.xcenter",0);
	Settings.zCenter = getConfig().getInt("world.zcenter",0);
	Settings.randomSpawn = getConfig().getBoolean("world.randomspawn", true);

    }

    public static World getBeaconzWorld() {
	// Check to see if the world exists, and if not, make it
	if (beaconzWorld == null) {
	    // World doesn't exist, so make it
	    plugin.getLogger().info("World is '" + Settings.worldName + "'");
	    beaconzWorld = WorldCreator.name(Settings.worldName).type(WorldType.NORMAL).environment(World.Environment.NORMAL).createWorld();
	    beaconzWorld.getPopulators().add(bp);
	    beaconzWorld.getWorldBorder().setCenter(Settings.xCenter, Settings.zCenter);
	    if (Settings.size > 0) {
		beaconzWorld.getWorldBorder().setSize(Settings.size);
	    }
	} else {
	    // Used on reload
	    beaconzWorld.getWorldBorder().setCenter(Settings.xCenter, Settings.zCenter);
	    if (Settings.size > 0) {
		beaconzWorld.getWorldBorder().setSize(Settings.size);
	    }
	}
	return beaconzWorld;
    }
}
