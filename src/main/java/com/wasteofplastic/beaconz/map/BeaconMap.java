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

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

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
     * No-op render method - this renderer only stores origin coordinates.
     * <p>
     * The actual rendering of the red X origin marker is handled by {@link TerritoryMapRenderer}
     * which retrieves the origin coordinates from the Register's map origin storage and draws
     * the marker as its final rendering step to ensure it appears on top of all other map elements.
     * <p>
     * This method must exist to satisfy the {@link MapRenderer} contract, but it performs no
     * rendering operations.
     *
     * @param map The MapView being rendered (unused)
     * @param canvas The MapCanvas to draw on (unused)
     * @param player The player viewing the map (unused)
     */
    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        // Intentionally empty - all rendering is delegated to TerritoryMapRenderer
    }
}
