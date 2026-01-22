/**
 * Custom map rendering for beacons and territories.
 * <p>
 * This package provides custom MapView renderers that display beacon information
 * and territory overlays on in-game maps.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.map.BeaconMap} - Renders beacon information on maps</li>
 *   <li>{@link com.wasteofplastic.beaconz.map.TerritoryMapRenderer} - Renders territory overlays showing team control</li>
 *   <li>{@link com.wasteofplastic.beaconz.map.MapCoordinateConverter} - Converts world coordinates to map pixels</li>
 * </ul>
 *
 * <h2>Beacon Maps:</h2>
 * When players capture a beacon, they receive a map showing:
 * <ul>
 *   <li><b>Title:</b> "Beacon Map"</li>
 *   <li><b>Location:</b> Beacon coordinates (X, Z)</li>
 *   <li><b>Center Marker:</b> White pixel at beacon location</li>
 * </ul>
 *
 * These maps are used to:
 * <ul>
 *   <li>Identify which beacon a map represents</li>
 *   <li>Create links by right-clicking another beacon with the map</li>
 *   <li>Navigate to beacons</li>
 * </ul>
 *
 * <h2>Territory Maps:</h2>
 * Territory maps show color-coded overlays representing:
 * <ul>
 *   <li>Beacon locations (colored dots)</li>
 *   <li>Team ownership (beacon colors)</li>
 *   <li>Links between beacons (colored lines)</li>
 *   <li>Triangular fields (filled areas)</li>
 * </ul>
 *
 * <h2>Rendering System:</h2>
 * Maps are rendered using Bukkit's MapView API:
 * <ul>
 *   <li><b>MapCanvas:</b> Drawing surface (128x128 pixels)</li>
 *   <li><b>Conditional Rendering:</b> Only renders when held in hand</li>
 *   <li><b>World Check:</b> Only renders in Beaconz world</li>
 *   <li><b>Performance:</b> Minimal CPU usage with early-exit checks</li>
 * </ul>
 *
 * <h2>Coordinate System:</h2>
 * MapCoordinateConverter handles transformations:
 * <ul>
 *   <li><b>World Coordinates:</b> Minecraft X/Z coordinates</li>
 *   <li><b>Map Coordinates:</b> Pixel coordinates (0-127)</li>
 *   <li><b>Scaling:</b> Adjusts for map zoom level</li>
 *   <li><b>Centering:</b> Centers map on specific coordinates</li>
 * </ul>
 *
 * <h2>Map Types:</h2>
 * <ul>
 *   <li><b>Beacon Maps:</b> Individual beacon reference maps</li>
 *   <li><b>Territory Maps:</b> Strategic overview of game area</li>
 *   <li><b>Link Maps:</b> Special maps used for creating links</li>
 * </ul>
 *
 * <h2>Performance Optimization:</h2>
 * Renderers optimize performance by:
 * <ul>
 *   <li>Only rendering when maps are held</li>
 *   <li>Checking world before rendering</li>
 *   <li>Caching computed data where possible</li>
 *   <li>Using efficient pixel operations</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.wasteofplastic.beaconz.map;
