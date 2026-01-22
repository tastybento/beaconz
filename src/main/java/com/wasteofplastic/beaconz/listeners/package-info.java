/**
 * Event listeners for all game mechanics.
 * <p>
 * This package contains all Bukkit event handlers that implement the game's interactive
 * mechanics. Listeners are organized by the type of interaction they handle.
 *
 * <h2>Beacon Interaction Listeners:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.BeaconCaptureListener} - Beacon capture by breaking capstones</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.BeaconLinkListener} - Beacon linking with maps and experience costs</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.BeaconProtectionListener} - Beacon protection rules and mining mechanics</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.BeaconSurroundListener} - Beacon surroundings and clearance validation</li>
 * </ul>
 *
 * <h2>Defense Listeners:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.BeaconPassiveDefenseListener} - Passive defense effects (field effects, mining penalties)</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.BeaconProjectileDefenseListener} - Active defense (dispensers shooting projectiles)</li>
 * </ul>
 *
 * <h2>Player Lifecycle Listeners:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.PlayerJoinLeaveListener} - Player login/logout, name database updates</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.PlayerDeathListener} - Death and respawn mechanics, inventory handling</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener} - Safe teleportation with delays</li>
 * </ul>
 *
 * <h2>Movement and Environment Listeners:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener} - Triangle field effects as players move</li>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.SkyListeners} - Sky/weather events in the Beaconz world</li>
 * </ul>
 *
 * <h2>Communication Listeners:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.ChatListener} - Team chat and game communication</li>
 * </ul>
 *
 * <h2>Lobby Listeners:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.listeners.LobbyListener} - Sign-based game joining, mob spawning control</li>
 * </ul>
 *
 * <h2>Event Priority:</h2>
 * Most listeners use:
 * <ul>
 *   <li><b>LOWEST:</b> For death/respawn to capture state early</li>
 *   <li><b>LOW:</b> For most game mechanics</li>
 *   <li><b>NORMAL:</b> For general interactions</li>
 *   <li><b>HIGHEST:</b> For final validations</li>
 * </ul>
 *
 * <h2>Design Patterns:</h2>
 * All listeners:
 * <ul>
 *   <li>Extend {@link com.wasteofplastic.beaconz.BeaconzPluginDependent} for plugin access</li>
 *   <li>Implement {@code org.bukkit.event.Listener}</li>
 *   <li>Use {@code @EventHandler} annotations with appropriate priority</li>
 *   <li>Check world before processing (most only work in Beaconz world)</li>
 *   <li>Validate permissions before allowing actions</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.wasteofplastic.beaconz.listeners;
