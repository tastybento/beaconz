/*
 * Copyright (c) 2015 - 2025 tastybento
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

package com.wasteofplastic.beaconz.map;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.Lang;


public class BeaconMap extends MapRenderer {
    private final Beaconz plugin;

    /**
     * @param plugin
     */
    public BeaconMap(Beaconz plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(MapView map, MapCanvas canvas, Player player) {
        // Only render when on this world
        if (!map.getWorld().equals(plugin.getBeaconzWorld())) {
            return;
        }
        // Only render if the map is in a hand
        ItemStack inMainHand = player.getInventory().getItemInMainHand();
        ItemStack inOffHand = player.getInventory().getItemInOffHand();
        if (inMainHand.getType().equals(Material.FILLED_MAP) || inOffHand.getType().equals(Material.FILLED_MAP)) {
            //Bukkit.getLogger().info("DEBUG: render");
            // here's where you do your drawing - see the Javadocs for the MapCanvas class for
            // the methods you can use
            canvas.drawText(10, 10, MinecraftFont.Font, Lang.beaconMapBeaconMap);
            // Get the text
            BeaconObj beacon = plugin.getRegister().getBeaconMap(map.getId());
            if (beacon != null) {
                canvas.drawText(10, 20, MinecraftFont.Font, Lang.generalLocation + ": " + beacon.getName());
                canvas.setPixel(64, 64, (byte) 64);
            } else {
                canvas.drawText(10, 20, MinecraftFont.Font, Lang.beaconMapUnknownBeacon);
            }
        }
    }
}
