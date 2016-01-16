/*
 * Copyright (c) 2015 tastybento
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

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;


public class BeaconMap extends MapRenderer {
    private final Beaconz plugin;

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
