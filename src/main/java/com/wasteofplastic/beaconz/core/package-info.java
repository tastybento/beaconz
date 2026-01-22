/**
 * Core game domain entities and data structures.
 * <p>
 * This package contains the fundamental domain objects that represent the primary game elements
 * in Beaconz. These are the building blocks upon which all game mechanics are built.
 *
 * <h2>Core Entities:</h2>
 * <ul>
 *   <li>{@link com.wasteofplastic.beaconz.core.BeaconObj} - Represents a beacon with its location, ownership, links, and defenses</li>
 *   <li>{@link com.wasteofplastic.beaconz.core.BeaconLink} - Represents a connection between two beacons</li>
 *   <li>{@link com.wasteofplastic.beaconz.core.DefenseBlock} - Represents a defense block placed on a beacon</li>
 *   <li>{@link com.wasteofplastic.beaconz.core.Region} - Represents a game region (lobby or game area)</li>
 *   <li>{@link com.wasteofplastic.beaconz.core.TriangleField} - Represents a triangular territory field formed by three linked beacons</li>
 * </ul>
 *
 * <h2>Key Concepts:</h2>
 *
 * <h3>Beacons:</h3>
 * Beacons are the central strategic objects. Each beacon:
 * <ul>
 *   <li>Has a fixed location (x, y, z coordinates)</li>
 *   <li>Can be owned by a team (or unowned/neutral)</li>
 *   <li>Can link to up to 8 other beacons</li>
 *   <li>Has a diamond pyramid base with beacon block and capstone</li>
 *   <li>Can have defense blocks for protection</li>
 *   <li>Can be "mined" for resources with cooldown timer</li>
 *   <li>Can be "locked" with emerald blocks to prevent capture</li>
 * </ul>
 *
 * <h3>Links:</h3>
 * Beacons can be linked together to:
 * <ul>
 *   <li>Create strategic connections across the map</li>
 *   <li>Form triangular fields when 3 beacons are fully connected</li>
 *   <li>Control territory and provide team bonuses</li>
 *   <li>Each beacon supports up to 8 outbound links</li>
 * </ul>
 *
 * <h3>Triangular Fields:</h3>
 * When three beacons are all linked to each other, they form a triangle that:
 * <ul>
 *   <li>Creates controlled territory for the owning team</li>
 *   <li>Provides beneficial potion effects to team members inside</li>
 *   <li>Applies negative effects to enemies inside</li>
 *   <li>Contributes to the team's area score</li>
 * </ul>
 *
 * <h3>Regions:</h3>
 * Regions define bounded areas in the world:
 * <ul>
 *   <li><b>Lobby Region:</b> Safe zone where players join games</li>
 *   <li><b>Game Regions:</b> Playing areas with beacons and team spawns</li>
 * </ul>
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li><b>Immutable Locations:</b> Beacon positions are fixed after creation</li>
 *   <li><b>Bidirectional Links:</b> Links are tracked from both endpoints</li>
 *   <li><b>Lazy Initialization:</b> Triangles calculated on-demand from links</li>
 *   <li><b>Value Objects:</b> Most entities are value objects with clear identity</li>
 * </ul>
 *
 * @since 2.0.0
 */
package com.wasteofplastic.beaconz.core;

