/**
 * Game management and state coordination.
 * <p>
 * This package contains classes responsible for managing game instances, scoring,
 * and the global registry of all game objects.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.game.Game} - Individual game instance with teams, regions, and lifecycle</li>
 *   <li>{@link com.wasteofplastic.beaconz.game.GameMgr} - Manager for multiple concurrent game instances</li>
 *   <li>{@link com.wasteofplastic.beaconz.game.Scorecard} - Team scoring, management, and leaderboard</li>
 *   <li>{@link com.wasteofplastic.beaconz.game.Register} - Global registry of all beacons, links, and triangular fields</li>
 * </ul>
 *
 * <h2>Game Lifecycle:</h2>
 * <ol>
 *   <li><b>Creation:</b> {@code GameMgr.newGame()} creates a new game instance</li>
 *   <li><b>Configuration:</b> Teams are added, regions initialized</li>
 *   <li><b>Active Play:</b> Players join teams, capture beacons, create links</li>
 *   <li><b>Scoring:</b> Teams accumulate points through various goals</li>
 *   <li><b>Victory:</b> First team to reach goal or highest score at timeout wins</li>
 *   <li><b>Cleanup:</b> Game data saved, resources released</li>
 * </ol>
 *
 * <h2>Multi-Game Support:</h2>
 * The GameMgr allows multiple independent games to run simultaneously:
 * <ul>
 *   <li>Each game has its own region and beacons</li>
 *   <li>Players can switch between games via lobby</li>
 *   <li>Inventories are stored separately per game</li>
 *   <li>Shared lobby region for all games</li>
 * </ul>
 *
 * <h2>Scoring System:</h2>
 * Teams can score points through multiple goals:
 * <ul>
 *   <li><b>Beacons:</b> Number of beacons owned</li>
 *   <li><b>Links:</b> Number of beacon links created</li>
 *   <li><b>Triangles:</b> Number of triangular fields formed</li>
 *   <li><b>Area:</b> Total territory area controlled (square blocks)</li>
 * </ul>
 *
 * <h2>Global Registry:</h2>
 * The Register maintains the master list of all game objects:
 * <ul>
 *   <li>All beacons in all games</li>
 *   <li>All links between beacons</li>
 *   <li>All triangular fields</li>
 *   <li>Defense blocks on beacons</li>
 *   <li>Beacon map associations</li>
 * </ul>
 *
 * This data is persisted to disk and loaded on server startup.
 *
 * @since 2.0.0
 */
package com.wasteofplastic.beaconz.game;
