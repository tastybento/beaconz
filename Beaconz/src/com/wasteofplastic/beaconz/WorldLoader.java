package com.wasteofplastic.beaconz;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Class to force the world loading before plugins.
 */
public class WorldLoader extends BeaconzPluginDependent implements Listener {
    private boolean worldLoaded = false;

    public WorldLoader(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(final ChunkLoadEvent event) {
        if (worldLoaded) {
            return;
        }
        getLogger().info("DEBUG: " + event.getWorld().getName());
        if (event.getWorld().getName().equals(Settings.worldName)) {
            return;
        }
        // Load the world
        worldLoaded = true;
        Beaconz.getBeaconzWorld();
    }
}
