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

import java.awt.Color;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.config.Lang;

/**
 * Custom map renderer that displays beacon information on in-game maps.
 * <p>
 * This renderer extends Bukkit's {@link MapRenderer} to create specialized "beacon maps"
 * that show players information about specific beacons. These maps are given to players
 * when they capture a beacon and can be used for:
 * <ul>
 *   <li>Identifying beacon locations by name</li>
 *   <li>Creating links between beacons (when right-clicking another beacon with the map)</li>
 *   <li>Visual reference with a center marker showing the beacon position</li>
 * </ul>
 *
 * <h3>Map Display Format:</h3>
 * <pre>
 * Line 1 (y=10): "Beacon Map" (title)
 * Line 2 (y=20): "Location: [Beacon Name]" or "Unknown Beacon"
 * Center Mark:   White pixel at (64, 64) showing beacon center
 * </pre>
 *
 * <h3>Rendering Conditions:</h3>
 * The map only renders when:
 * <ul>
 *   <li>The map view is in the Beaconz world</li>
 *   <li>The player is holding a filled map in either hand</li>
 * </ul>
 *
 * This prevents unnecessary rendering when maps are in inventory or in other worlds.
 *
 * @author tastybento
 * @see BeaconObj
 * @see org.bukkit.map.MapRenderer
 * @since 1.0
 */
public class BeaconMap extends MapRenderer {

    /**
     * Reference to the main plugin instance.
     * Used to access the beacon register and world information.
     */
    private final Beaconz plugin;

    /**
     * Constructs a new BeaconMap renderer.
     * <p>
     * This renderer is attached to MapViews when beacon maps are created,
     * typically when a player captures a beacon or receives a map as a reward.
     *
     * @param plugin The Beaconz plugin instance for accessing game data
     */
    public BeaconMap(Beaconz plugin) {
        this.plugin = plugin;
    }

    /**
     * Renders the beacon map display onto the canvas.
     * <p>
     * This method is called by Bukkit whenever the map needs to be rendered for a player.
     * It draws beacon-specific information including:
     * <ol>
     *   <li>Map title ("Beacon Map")</li>
     *   <li>Beacon location name or "Unknown Beacon" if not found</li>
     *   <li>Center marker (white pixel at map center)</li>
     * </ol>
     *
     * <h3>Performance Optimization:</h3>
     * The method performs early-exit checks to minimize rendering overhead:
     * <ul>
     *   <li>Skips rendering if not in the Beaconz world</li>
     *   <li>Skips rendering if map is not being actively held</li>
     * </ul>
     *
     * <h3>Map Coordinate System:</h3>
     * <ul>
     *   <li>Origin (0,0) is top-left corner</li>
     *   <li>Map size is typically 128x128 pixels</li>
     *   <li>Center is at (64, 64)</li>
     *   <li>Text is drawn at specified (x, y) coordinates</li>
     * </ul>
     *
     * @param map The MapView being rendered (contains map ID and world)
     * @param canvas The MapCanvas to draw on (provides drawing methods)
     * @param player The player viewing the map (used to check held items)
     * @see org.bukkit.map.MapCanvas
     * @see org.bukkit.map.MinecraftFont
     */
    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        // VALIDATION 1: World check
        // Only render maps in the Beaconz world to avoid wasting resources
        // on maps in other worlds where beacons don't exist
        if (!map.getWorld().equals(plugin.getBeaconzWorld())) {
            return;
        }

        // VALIDATION 2: Hand check
        // Only render if the map is actively being held
        // This prevents unnecessary rendering for maps in inventory/frames
        ItemStack inMainHand = player.getInventory().getItemInMainHand();
        ItemStack inOffHand = player.getInventory().getItemInOffHand();

        // Check both hands for a filled map
        if (inMainHand.getType().equals(Material.FILLED_MAP) || inOffHand.getType().equals(Material.FILLED_MAP)) {

            // RENDERING: Draw map content

            // Draw title at top of map (10 pixels from left, 10 pixels from top)
            canvas.drawText(10, 10, MinecraftFont.Font, Lang.beaconMapBeaconMap);

            // Look up the beacon associated with this map ID
            // Each beacon map has a unique ID that links to a specific beacon
            BeaconObj beacon = plugin.getRegister().getBeaconMap(map.getId());

            if (beacon != null) {
                // SCENARIO 1: Beacon found - display its information

                // Draw beacon location name (10 pixels from left, 20 pixels from top)
                // This appears below the title
                canvas.drawText(10, 20, MinecraftFont.Font, Lang.generalLocation + ": " + beacon.getName());

                // Draw center marker - a single white pixel at the map's center (64, 64)
                // This visually indicates where the beacon is located on the map
                canvas.setPixelColor(64, 64, Color.WHITE);

            } else {
                // SCENARIO 2: Beacon not found - display error message
                // This can happen if:
                // - The beacon was destroyed
                // - The map ID is invalid
                // - The beacon register was cleared
                canvas.drawText(10, 20, MinecraftFont.Font, Lang.beaconMapUnknownBeacon);
            }
        }
    }
}
