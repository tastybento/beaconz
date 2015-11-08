package com.wasteofplastic.beaconz;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

public class BeaconMap extends MapRenderer {
    private Beaconz plugin;

    /**
     * @param plugin
     */
    public BeaconMap(Beaconz plugin) {
	this.plugin = plugin;
    }

    public void render(MapView map, MapCanvas canvas, Player player) {
	// here's where you do your drawing - see the Javadocs for the MapCanvas class for
	// the methods you can use
	canvas.drawText(10, 10, MinecraftFont.Font, "Beacon Map #" + map.getId());
	// Get the text
	BeaconObj beacon = plugin.getRegister().getBeaconMap(map.getId());
	if (beacon != null) {
	    canvas.drawText(10, 20, MinecraftFont.Font, "Location: " + beacon.getName());
	    canvas.setPixel(64, 64, (byte) 64);
	} else {
	    canvas.drawText(10, 20, MinecraftFont.Font, "Unknown beacon");
	}
	// Draw the links and triangles?
	for (int x = 0; x< 128; x++) {
	    for (int z = 0; z<128; z++) {
		// TODO magic happens
	    }
	}
    }

}
