/**
 * Custom world generation with beacon placement.
 * <p>
 * This package contains the custom chunk generator and block populator responsible
 * for creating the Beaconz world with strategically placed beacons.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.generator.BeaconzChunkGen} - Custom chunk generator for terrain</li>
 *   <li>{@link com.wasteofplastic.beaconz.generator.BeaconPopulator} - Block populator for beacon placement</li>
 * </ul>
 *
 * <h2>World Generation:</h2>
 * The Beaconz world is generated with:
 * <ul>
 *   <li><b>Custom Terrain:</b> Modified vanilla terrain generation</li>
 *   <li><b>Beacon Grid:</b> Beacons distributed in a grid pattern</li>
 *   <li><b>Configurable Density:</b> Beacon spacing controlled by settings</li>
 *   <li><b>Seed-based:</b> Reproducible terrain with configurable seed</li>
 * </ul>
 *
 * <h2>Beacon Placement:</h2>
 * Beacons are placed during chunk population:
 * <ol>
 *   <li><b>Grid Calculation:</b> Determine if chunk should have a beacon</li>
 *   <li><b>Location Selection:</b> Find suitable Y-coordinate</li>
 *   <li><b>Structure Creation:</b> Build beacon pyramid and capstone</li>
 *   <li><b>Registration:</b> Add beacon to global registry</li>
 * </ol>
 *
 * <h2>Beacon Structure:</h2>
 * Each generated beacon consists of:
 * <pre>
 * Y+2: Obsidian (capstone)
 * Y+1: Beacon block
 * Y:   3x3 diamond block pyramid (9 blocks)
 * </pre>
 *
 * <h2>Distribution System:</h2>
 * Beacons are distributed using:
 * <ul>
 *   <li><b>Grid-based:</b> Ensures even spacing across the world</li>
 *   <li><b>Distribution Factor:</b> Controls beacon density (default 0.1)</li>
 *   <li><b>Chunk Alignment:</b> Beacons aligned to chunk boundaries</li>
 *   <li><b>Height Detection:</b> Placed at appropriate Y-level for terrain</li>
 * </ul>
 *
 * <h2>World Properties:</h2>
 * The generated world has:
 * <ul>
 *   <li><b>Type:</b> NORMAL (vanilla-like terrain)</li>
 *   <li><b>Environment:</b> NORMAL (overworld)</li>
 *   <li><b>Seed:</b> Configurable via settings</li>
 *   <li><b>Name:</b> Configurable (default "beaconz_world")</li>
 * </ul>
 *
 * <h2>Generation Process:</h2>
 * <ol>
 *   <li><b>Server Startup:</b> BeaconzChunkGen created in onLoad()</li>
 *   <li><b>World Creation:</b> World created with custom generator</li>
 *   <li><b>Chunk Generation:</b> Terrain generated for each chunk</li>
 *   <li><b>Population:</b> Beacons placed during chunk population phase</li>
 *   <li><b>Registration:</b> Beacons added to global registry</li>
 * </ol>
 *
 * <h2>Configuration:</h2>
 * Generation controlled by config.yml settings:
 * <ul>
 *   <li>{@code world.name} - World name</li>
 *   <li>{@code world.distribution} - Beacon density</li>
 *   <li>{@code world.seedadjustment} - World seed</li>
 *   <li>{@code world.xcenter} - Game center X coordinate</li>
 *   <li>{@code world.zcenter} - Game center Z coordinate</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.wasteofplastic.beaconz.generator;
