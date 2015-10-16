package com.wasteofplastic.beaconz;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class WorldLoader implements Listener {
    private Beaconz plugin;
    private boolean worldLoaded = false;

    /**
     * Class to force world loading before plugins.
     * @param plugin
     */
    public WorldLoader(Beaconz plugin) {
	this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(final ChunkLoadEvent event) {
	if (worldLoaded) {
	    return;
	}
	plugin.getLogger().info("DEBUG: " + event.getWorld().getName());
	if (event.getWorld().getName().equals(Settings.worldName)) {
	    return;
	}
	// Load the world
	worldLoaded = true;
	Beaconz.getBeaconzWorld();
    }
}
