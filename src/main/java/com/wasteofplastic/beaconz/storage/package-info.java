/**
 * Data persistence and storage systems.
 * <p>
 * This package handles all data persistence including player inventories, message queues,
 * and player name lookups. Data is stored to disk and restored on server restart.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.storage.BeaconzStore} - Player inventory and state persistence</li>
 *   <li>{@link com.wasteofplastic.beaconz.storage.Messages} - Offline message queue system</li>
 *   <li>{@link com.wasteofplastic.beaconz.storage.TinyDB} - Player name to UUID database</li>
 * </ul>
 *
 * <h2>Inventory Storage:</h2>
 * BeaconzStore manages player state across games:
 * <ul>
 *   <li><b>Lobby Inventory:</b> Items players have in the lobby</li>
 *   <li><b>Game Inventories:</b> Separate inventory for each game instance</li>
 *   <li><b>Experience:</b> XP levels and points</li>
 *   <li><b>Health:</b> Current and max health</li>
 *   <li><b>Hunger:</b> Food level and saturation</li>
 * </ul>
 *
 * When players switch games or respawn, their appropriate inventory is restored.
 * All data is saved to YAML files in the plugin data directory.
 *
 * <h2>Message Queue:</h2>
 * Messages provides asynchronous messaging for offline players:
 * <ul>
 *   <li>Queue messages for offline players</li>
 *   <li>Deliver on next login (2-second delay)</li>
 *   <li>Team broadcasts</li>
 *   <li>Cross-game notifications</li>
 *   <li>Message history persistence</li>
 * </ul>
 *
 * <h2>Player Name Database:</h2>
 * TinyDB maintains a UUID to player name mapping:
 * <ul>
 *   <li><b>Offline Lookups:</b> Show beacon owners when they're offline</li>
 *   <li><b>Admin Commands:</b> Reference players by name in commands</li>
 *   <li><b>Team Rosters:</b> Display team member names</li>
 *   <li><b>Scoreboards:</b> Show player names in rankings</li>
 * </ul>
 *
 * <h2>Data Formats:</h2>
 * <ul>
 *   <li><b>Inventories:</b> YAML format per game per player</li>
 *   <li><b>Messages:</b> YAML queue per player UUID</li>
 *   <li><b>Names:</b> Simple key-value mapping in YAML</li>
 * </ul>
 *
 * <h2>Persistence Timing:</h2>
 * Data is saved:
 * <ul>
 *   <li>On plugin disable (server shutdown/reload)</li>
 *   <li>On player logout (inventories)</li>
 *   <li>On game transitions (switching games)</li>
 *   <li>After major state changes (death, game end)</li>
 * </ul>
 *
 * @since 2.0.0
 */
package com.wasteofplastic.beaconz.storage;
