/**
 * Root package for the Beaconz strategic team-based mini-game plugin.
 * <p>
 * Beaconz is a Bukkit/Spigot plugin that creates a competitive strategic game where teams
 * compete to control beacons scattered across a custom-generated world. Teams can:
 * <ul>
 *   <li>Capture beacons by breaking obsidian capstones</li>
 *   <li>Link beacons together to form strategic connections</li>
 *   <li>Create triangular fields that control territory and provide buffs/debuffs</li>
 *   <li>Mine beacons for resources with cooldown timers</li>
 *   <li>Build defenses to protect their beacons from enemies</li>
 * </ul>
 *
 * <h2>Plugin Architecture:</h2>
 * This root package contains only the main plugin entry point and base infrastructure:
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.Beaconz} - Main plugin class managing lifecycle and initialization</li>
 *   <li>{@link com.wasteofplastic.beaconz.BeaconzPluginDependent} - Base class providing plugin access to all components</li>
 * </ul>
 *
 * <h2>Sub-packages:</h2>
 * The plugin functionality is organized into specialized sub-packages:
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.core} - Core game entities (beacons, links, regions, triangles)</li>
 *   <li>{@link com.wasteofplastic.beaconz.game} - Game management and state (instances, scoring, registry)</li>
 *   <li>{@link com.wasteofplastic.beaconz.config} - Configuration and localization</li>
 *   <li>{@link com.wasteofplastic.beaconz.storage} - Data persistence (inventories, messages, player names)</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners} - Event handlers for all game mechanics</li>
 *   <li>{@link com.wasteofplastic.beaconz.commands} - Player and admin command handlers</li>
 *   <li>{@link com.wasteofplastic.beaconz.map} - Custom map renderers for beacons and territories</li>
 *   <li>{@link com.wasteofplastic.beaconz.generator} - Custom world generation with beacon placement</li>
 *   <li>{@link com.wasteofplastic.beaconz.util} - Utility classes and helper functions</li>
 *   <li>{@link com.wasteofplastic.beaconz.integration} - Third-party plugin integrations (Dynmap, bStats)</li>
 * </ul>
 *
 * <h2>Getting Started:</h2>
 * The plugin lifecycle begins in {@link com.wasteofplastic.beaconz.Beaconz}:
 * <ol>
 *   <li><b>onLoad():</b> Creates the chunk generator before worlds load</li>
 *   <li><b>onEnable():</b> Initializes all systems, loads configuration, registers listeners</li>
 *   <li><b>onDisable():</b> Saves all data (beacons, inventories, game states)</li>
 * </ol>
 *
 * @since 1.0
 * @author tastybento
 * @version 2.0.0
 */
package com.wasteofplastic.beaconz;
