/**
 * Configuration, settings, and localization.
 * <p>
 * This package centralizes all configuration-related functionality including game settings,
 * localized messages, and game parameters/enums.
 *
 * <h2>Main Components:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.config.Settings} - Global configuration settings from config.yml</li>
 *   <li>{@link com.wasteofplastic.beaconz.config.Lang} - Localization and language strings</li>
 *   <li>{@link com.wasteofplastic.beaconz.config.Params} - Game parameters, enums, and constants</li>
 * </ul>
 *
 * <h2>Configuration System:</h2>
 * Settings are loaded from {@code config.yml} on plugin startup and stored in static fields
 * for fast access throughout the plugin. Categories include:
 * <ul>
 *   <li><b>General:</b> Game mode, team chat, teleport delays</li>
 *   <li><b>World:</b> World name, distribution, seed, game center coordinates</li>
 *   <li><b>Lobby:</b> Location, radius, height, spawn settings</li>
 *   <li><b>Teams:</b> Default number, colors, sizes</li>
 *   <li><b>Links:</b> Max links, distance limits, experience costs</li>
 *   <li><b>Mining:</b> Cooldowns, exhaustion chance, rewards</li>
 *   <li><b>Defense:</b> Height limits, level requirements</li>
 *   <li><b>Triangles:</b> Field effects for allies and enemies</li>
 *   <li><b>Scoreboard:</b> Goals, timers, score types</li>
 * </ul>
 *
 * <h2>Localization:</h2>
 * Lang provides localized messages and supports multiple languages:
 * <ul>
 *   <li>Messages stored in {@code locale/} directory</li>
 *   <li>Default locale configurable in config.yml</li>
 *   <li>Adventure API Component format for rich text</li>
 *   <li>Color codes and formatting supported</li>
 * </ul>
 *
 * <h2>Game Parameters:</h2>
 * Params defines enums and constants used throughout the game:
 * <ul>
 *   <li><b>GameMode:</b> STRATEGY vs MINIGAME modes</li>
 *   <li><b>GameScoreGoal:</b> Scoring objectives (beacons, links, triangles, area)</li>
 *   <li><b>Team Colors:</b> Team identification colors</li>
 *   <li><b>Constants:</b> Game-wide constant values</li>
 * </ul>
 *
 * <h2>Configuration Validation:</h2>
 * Settings are validated on load to ensure:
 * <ul>
 *   <li>Percentages are within 0-100 range</li>
 *   <li>Required positive values are positive</li>
 *   <li>Coordinates aligned to chunk/region boundaries</li>
 *   <li>Material and potion effect names are valid</li>
 * </ul>
 *
 * Invalid values are replaced with sensible defaults and logged as warnings.
 *
 * @since 2.0.0
 */
package com.wasteofplastic.beaconz.config;
