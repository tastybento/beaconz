/*
 * Copyright (c) 2015 - 2026 tastybento
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

import com.wasteofplastic.beaconz.Beaconz;

/**
 * Custom map renderer that stores origin coordinates for beacon maps.
 * <p>
 * This renderer maintains the origin coordinates (where the map was created)
 * but delegates the actual rendering of the red X marker to {@link TerritoryMapRenderer}
 * to ensure the marker appears on top of all other map elements.
 * <p>
 * The origin coordinates are persisted via the Register's map origin storage
 * and survive server restarts.
 *
 * @author tastybento
 * @see TerritoryMapRenderer
 * @since 1.0
 */
public class BeaconMap extends MapRenderer {

    /**
     * Reference to the main plugin instance.
     * Used to access the beacon register and world information.
     */
    private final Beaconz plugin;

    /**
     * X coordinate of the beacon that generated this map (origin point).
     * This is marked with a red X on the rendered map.
     */
    private Integer originX;

    /**
     * Z coordinate of the beacon that generated this map (origin point).
     * This is marked with a red X on the rendered map.
     */
    private Integer originZ;

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
     * Constructs a new BeaconMap renderer with a specific origin point.
     * <p>
     * The origin point is the beacon location where this map was created,
     * and will be marked with a red X on the rendered map.
     *
     * @param plugin The Beaconz plugin instance for accessing game data
     * @param originX The X coordinate of the origin beacon
     * @param originZ The Z coordinate of the origin beacon
     */
    public BeaconMap(Beaconz plugin, int originX, int originZ) {
        this.plugin = plugin;
        this.originX = originX;
        this.originZ = originZ;
    }

    /**
     * Sets the origin coordinates for this map.
     * The origin is the beacon location where this map was created.
     *
     * @param x The X coordinate of the origin beacon
     * @param z The Z coordinate of the origin beacon
     */
    public void setOrigin(int x, int z) {
        this.originX = x;
        this.originZ = z;
    }

    /**
     * Gets the X coordinate of the origin beacon.
     *
     * @return The origin X coordinate, or null if not set
     */
    public Integer getOriginX() {
        return originX;
    }

    /**
     * Gets the Z coordinate of the origin beacon.
     *
     * @return The origin Z coordinate, or null if not set
     */
    public Integer getOriginZ() {
        return originZ;
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
    /**
     * Performs validation checks for map rendering.
     * <p>
     * This method validates that:
     * <ul>
     *   <li>The map is in the Beaconz world</li>
     *   <li>The player is holding a filled map</li>
     * </ul>
     * <p>
     * The actual rendering of the origin marker is handled by {@link TerritoryMapRenderer}
     * which retrieves the origin coordinates from the Register's map origin storage.
     *
     * @param map The MapView being rendered (contains map ID and world)
     * @param canvas The MapCanvas to draw on (unused, kept for interface compatibility)
     * @param player The player viewing the map (used to check held items)
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
            // Note: The red X origin marker is now drawn by TerritoryMapRenderer
            // to ensure it appears on top of all other map elements
        }
    }
}
