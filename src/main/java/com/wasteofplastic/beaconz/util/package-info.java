/**
 * Utility classes and helper functions.
 * <p>
 * This package contains general-purpose utility classes that provide helper functionality
 * used throughout the plugin.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.util.LineIterator} - Iterates over blocks in a line between two points</li>
 *   <li>{@link com.wasteofplastic.beaconz.util.LineVisualizer} - Creates particle effects along lines (beacon links)</li>
 *   <li>{@link com.wasteofplastic.beaconz.util.LinkResult} - Result wrapper for link operations</li>
 *   <li>{@link com.wasteofplastic.beaconz.util.Pair} - Generic pair/tuple utility class</li>
 *   <li>{@link com.wasteofplastic.beaconz.util.TriangleScorer} - Calculates scores for triangular fields</li>
 *   <li>{@link com.wasteofplastic.beaconz.storage.TinyDB} - Lightweight key-value database for player names</li>
 * </ul>
 *
 * <h2>Line Utilities:</h2>
 * <h3>LineIterator:</h3>
 * Provides block-by-block iteration along a line:
 * <ul>
 *   <li>Bresenham's line algorithm for 3D space</li>
 *   <li>Iterates over all blocks between two points</li>
 *   <li>Used for link validation and clearance checks</li>
 * </ul>
 *
 * <h3>LineVisualizer:</h3>
 * Creates visual feedback for beacon links:
 * <ul>
 *   <li>Spawns particle effects along link lines</li>
 *   <li>Different colors for different teams</li>
 *   <li>Temporary or persistent visualization</li>
 *   <li>Performance-optimized particle spawning</li>
 * </ul>
 *
 * <h2>Result Wrappers:</h2>
 * <h3>LinkResult:</h3>
 * Encapsulates the result of link operations:
 * <ul>
 *   <li>Success/failure status</li>
 *   <li>Error message if failed</li>
 *   <li>Link object if successful</li>
 *   <li>Type-safe result handling</li>
 * </ul>
 *
 * <h2>Data Structures:</h2>
 * <h3>Pair:</h3>
 * Generic pair/tuple for holding two related values:
 * <ul>
 *   <li>Immutable value object</li>
 *   <li>Type-safe with generics</li>
 *   <li>Useful for method return values</li>
 * </ul>
 *
 * <h2>Scoring Utilities:</h2>
 * <h3>TriangleScorer:</h3>
 * Calculates area and scores for triangular fields:
 * <ul>
 *   <li>Area calculation from three points</li>
 *   <li>Point-in-triangle tests</li>
 *   <li>Triangle validity checks</li>
 *   <li>Score computation based on area</li>
 * </ul>
 *
 * <h2>Database Utilities:</h2>
 * <h3>TinyDB:</h3>
 * Lightweight database for player name lookups:
 * <ul>
 *   <li>UUID to player name mapping</li>
 *   <li>YAML-based persistence</li>
 *   <li>Fast in-memory caching</li>
 *   <li>Automatic save on changes</li>
 * </ul>
 *
 * <h2>Design Principles:</h2>
 * Utility classes follow these principles:
 * <ul>
 *   <li><b>Stateless:</b> Most utilities are stateless functions</li>
 *   <li><b>Reusable:</b> Generic implementations for common needs</li>
 *   <li><b>Performance:</b> Optimized for frequent use</li>
 *   <li><b>Type Safety:</b> Use generics where appropriate</li>
 * </ul>
 *
 * @since 2.0.0
 */
package com.wasteofplastic.beaconz.util;
