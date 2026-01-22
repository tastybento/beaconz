/**
 * Third-party plugin integrations.
 * <p>
 * This package contains integration code for external plugins and services,
 * isolating these dependencies from the core plugin logic.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.integration.OurServerListener} - Dynmap integration</li>
 *   <li>{@link com.wasteofplastic.beaconz.integration.Metrics} - bStats metrics collection</li>
 * </ul>
 *
 * <h2>Dynmap Integration:</h2>
 * OurServerListener provides real-time map visualization:
 * <ul>
 *   <li><b>Beacon Markers:</b> Shows beacon locations on Dynmap</li>
 *   <li><b>Team Colors:</b> Beacons colored by owning team</li>
 *   <li><b>Territory Overlay:</b> Displays triangular fields</li>
 *   <li><b>Link Lines:</b> Shows connections between beacons</li>
 *   <li><b>Auto-Update:</b> Real-time updates as game state changes</li>
 * </ul>
 *
 * Features enabled when {@code general.usedynmap: true} in config.yml:
 * <ul>
 *   <li>Beacon ownership markers</li>
 *   <li>Team territory visualization</li>
 *   <li>Strategic map for players</li>
 *   <li>Web-based game viewing</li>
 * </ul>
 *
 * <h2>bStats Metrics:</h2>
 * Metrics class collects anonymous usage statistics:
 * <ul>
 *   <li><b>Server Count:</b> Number of servers using Beaconz</li>
 *   <li><b>Player Count:</b> Active player statistics</li>
 *   <li><b>Version Distribution:</b> Which versions are in use</li>
 *   <li><b>Configuration Stats:</b> Common configuration patterns</li>
 * </ul>
 *
 * Data collected is:
 * <ul>
 *   <li>Anonymous (no personally identifiable information)</li>
 *   <li>Used to improve the plugin</li>
 *   <li>Viewable at https://bstats.org/plugin/bukkit/Beaconz</li>
 *   <li>Can be disabled in bStats global config</li>
 * </ul>
 *
 * <h2>Integration Pattern:</h2>
 * External integrations follow a consistent pattern:
 * <ol>
 *   <li><b>Optional:</b> Plugin works without integrations</li>
 *   <li><b>Configurable:</b> Can be enabled/disabled in config</li>
 *   <li><b>Isolated:</b> Integration code isolated in this package</li>
 *   <li><b>Fail-safe:</b> Missing plugins don't cause errors</li>
 * </ol>
 *
 * <h2>Dependency Detection:</h2>
 * Integrations check for plugin availability:
 * <pre>
 * Plugin dynmap = pm.getPlugin("dynmap");
 * if (dynmap != null) {
 *     // Register integration
 * }
 * </pre>
 *
 * <h2>Adding New Integrations:</h2>
 * To add a new plugin integration:
 * <ol>
 *   <li>Create integration class in this package</li>
 *   <li>Add config option to enable/disable</li>
 *   <li>Check for plugin availability before registering</li>
 *   <li>Handle missing plugin gracefully</li>
 *   <li>Document in README and config comments</li>
 * </ol>
 *
 * <h2>Future Integrations:</h2>
 * Potential future integrations:
 * <ul>
 *   <li>PlaceholderAPI - Custom placeholders for scoreboards</li>
 *   <li>Vault - Economy integration for beacon costs</li>
 *   <li>WorldGuard - Region protection integration</li>
 *   <li>Citizens - NPC guides in lobby</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.wasteofplastic.beaconz.integration;
