/*
 * Copyright (c) 2015 - 2025 tastybento
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wasteofplastic.beaconz.core;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

import com.destroystokyo.paper.MaterialTags;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.game.Scorecard;
import com.wasteofplastic.beaconz.util.LineVisualizer;

/**
 * Represents a beacon in the Beaconz game world.
 * <p>
 * A beacon is the primary strategic object that teams compete to control. Each beacon:
 * <ul>
 *   <li>Has a fixed location (x, y, z coordinates)</li>
 *   <li>Can be owned by a team (or unowned/neutral)</li>
 *   <li>Can be linked to up to 8 other beacons</li>
 *   <li>Has a diamond block pyramid base with beacon block and capstone</li>
 *   <li>Can have defense blocks placed on it for protection</li>
 *   <li>Can be "mined" for resources (with cooldown timer)</li>
 *   <li>Can be "locked" with emerald blocks to prevent capture</li>
 * </ul>
 *
 * <h3>Beacon Structure:</h3>
 * <pre>
 * Y+2:  [Capstone] - Obsidian (unowned) or Team Glass (owned)
 * Y+1:  [Beacon]   - Beacon block (provides effects)
 * Y:    [Pyramid]  - 3x3 diamond block base
 * </pre>
 *
 * <h3>Linking System:</h3>
 * Beacons can link to other beacons to:
 * <ul>
 *   <li>Create strategic connections across the map</li>
 *   <li>Form triangular fields when 3 beacons are fully connected</li>
 *   <li>Control territory within those triangles</li>
 *   <li>Each beacon supports up to 8 outbound links</li>
 * </ul>
 *
 * <h3>Defense Blocks:</h3>
 * Players can place blocks on top of beacons for:
 * <ul>
 *   <li>Protection from mining/capture</li>
 *   <li>Defensive bonuses (dispensers shoot projectiles)</li>
 *   <li>Link distance reduction (certain blocks)</li>
 *   <li>Locking mechanism (emerald blocks prevent capture)</li>
 * </ul>
 *
 * <h3>Mining/Hacking System:</h3>
 * <ul>
 *   <li>Players can mine pyramid blocks for resources</li>
 *   <li>Cooldown timer prevents rapid mining</li>
 *   <li>Different rewards for team vs enemy beacons</li>
 *   <li>New beacons have no cooldown initially</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class BeaconObj extends BeaconzPluginDependent {
    /** The 2D location of this beacon (x, z coordinates) for geometric calculations */
    private final Point2D location;

    /** The X coordinate (East-West) of this beacon */
    private final int x;

    /** The Z coordinate (North-South) of this beacon */
    private final int z;

    /** The Y coordinate (height/altitude) of this beacon */
    private final int y;

    /**
     * Timestamp of the last mining/hacking operation on this beacon.
     * Used to enforce cooldown periods between mining attempts.
     */
    private long hackTimer;

    /** The team that owns this beacon, or null if unowned */
    private Team ownership;

    /** Unique identifier for this beacon in the beacon register */
    private Integer id = null;

    /**
     * Whether this is a newly captured beacon.
     * New beacons have no mining cooldown initially.
     */
    private boolean newBeacon = true;

    /**
     * The 8 cardinal and ordinal directions around a beacon.
     * Used for checking clearance and building the pyramid.
     */
    private static final List<BlockFace> FACES = new ArrayList<>(Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST));

    /** Debug flag for verbose logging */
    private static final boolean DEBUG = false;

    /**
     * Map of defense blocks placed on this beacon.
     * Key: The block location
     * Value: DefenseBlock object containing level and owner information
     */
    private HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();

    /**
     * Set of beacons that this beacon is linked to.
     * Links can be bidirectional - both beacons track the link.
     */
    private final Set<BeaconObj> links = new HashSet<>();

    /**
     * Constructs a new beacon object at the specified coordinates.
     * <p>
     * The beacon is initialized with:
     * <ul>
     *   <li>Location set to the provided coordinates</li>
     *   <li>Hack timer set to current time (no initial cooldown)</li>
     *   <li>Owner set to the specified team (can be null for unowned)</li>
     *   <li>Empty link set (no connections)</li>
     *   <li>New beacon flag set to true (no mining cooldown)</li>
     * </ul>
     *
     * Note: The beacon structure (pyramid, beacon block, capstone) is not
     * created by this constructor - that happens separately during world generation
     * or beacon placement.
     *
     * @param plugin The Beaconz plugin instance
     * @param x The X coordinate (East-West position)
     * @param y The Y coordinate (height/altitude)
     * @param z The Z coordinate (North-South position)
     * @param owner The team that owns this beacon (null for unowned)
     */
    public BeaconObj(Beaconz plugin, int x, int y, int z, Team owner) {
        super(plugin);
        // Store as Point2D for geometric calculations (links, triangles, etc.)
        this.location = new Point2D.Double(x,z);
        this.x = x;
        this.z = z;
        this.y = y;
        // Initialize hack timer to current time (no initial cooldown)
        this.hackTimer = System.currentTimeMillis();
        this.ownership = owner;
        // Clear any existing links (defensive programming)
        this.links.clear();
        // Mark as new beacon (no mining cooldown initially)
        this.newBeacon = true;
    }

    /**
     * Gets the X coordinate (East-West position) of this beacon.
     *
     * @return The X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the Y coordinate (height/altitude) of this beacon.
     *
     * @return The Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the Z coordinate (North-South position) of this beacon.
     *
     * @return The Z coordinate
     */
    public int getZ() {
        return z;
    }

    /**
     * Gets the 2D point location of this beacon.
     * <p>
     * This is used for geometric calculations such as:
     * <ul>
     *   <li>Distance calculations between beacons</li>
     *   <li>Triangle field creation</li>
     *   <li>Link line intersection detection</li>
     * </ul>
     *
     * @return The 2D point (x, z) location
     */
    public Point2D getPoint() {
        return location;
    }

    /**
     * Gets the full 3D Bukkit location of this beacon.
     * <p>
     * This creates a new Location object each time it's called.
     * The location points to the beacon block itself (y coordinate).
     *
     * @return The Bukkit location in the Beaconz world
     */
    public Location getLocation() {
        return new Location(getBeaconzWorld(),location.getX(),y,location.getY());
    }

    /**
     * Gets the set of beacons that this beacon is directly linked to.
     * <p>
     * Links are bidirectional - if beacon A links to beacon B,
     * both beacons will have each other in their links set.
     *
     * @return Unmodifiable view of the beacons linked to this beacon
     */
    public Set<BeaconObj> getLinks() {
        return links;
    }

    /**
     * Creates a bidirectional link between this beacon and another beacon.
     * <p>
     * This method:
     * <ol>
     *   <li>Checks if this beacon has reached the maximum of 8 outbound links</li>
     *   <li>Adds a link from the target beacon back to this beacon</li>
     *   <li>Adds a link from this beacon to the target beacon</li>
     * </ol>
     *
     * Links are used for:
     * <ul>
     *   <li>Creating strategic connections across the map</li>
     *   <li>Forming triangular fields when 3 beacons are fully connected</li>
     *   <li>Controlling territory within those triangles</li>
     * </ul>
     *
     * @param beacon The beacon to link to
     * @return true if link was created successfully, false if link limit (8) reached
     */
    public boolean addOutboundLink(BeaconObj beacon) {
        // Maximum of 8 outgoing links allowed per beacon
        if (links.size() == 8) {
            return false; // Link limit reached
        }

        // Create bidirectional link
        beacon.addLink(this);  // Add link from target back to this beacon
        links.add(beacon);      // Add link from this beacon to target

        return true;
    }

    /**
     * Adds a link from this beacon to another beacon (one direction only).
     * <p>
     * This is called by {@link #addOutboundLink(BeaconObj)} to create the
     * return link. It should not be called directly - use addOutboundLink instead
     * to ensure bidirectional linking.
     *
     * @param beacon The beacon to link to
     */
    public void addLink(BeaconObj beacon) {
        links.add(beacon);
    }

    /**
     * Gets the timestamp of the last mining/hacking operation.
     * <p>
     * This is used to enforce cooldown periods. If current time minus
     * this timestamp is less than the cooldown setting, mining is not allowed
     * and penalty effects are applied instead.
     *
     * @return The timestamp in milliseconds since epoch
     */
    public long getHackTimer() {
        return hackTimer;
    }

    /**
     * Resets the hack timer to the current time and marks beacon as no longer new.
     * <p>
     * This is called when:
     * <ul>
     *   <li>A player successfully mines the beacon</li>
     *   <li>The beacon becomes exhausted (cooldown triggered)</li>
     * </ul>
     *
     * After this call, mining will not be allowed until the cooldown period expires.
     */
    public void resetHackTimer() {
        this.hackTimer = System.currentTimeMillis();
        this.newBeacon = false;  // No longer a new beacon
    }

    /**
     * Gets the team that owns this beacon.
     *
     * @return The owning team, or null if beacon is unowned/neutral
     */
    public Team getOwnership() {
        return ownership;
    }

    /**
     * Sets the team that owns this beacon.
     * <p>
     * This is typically called when:
     * <ul>
     *   <li>A team captures the beacon (breaking obsidian)</li>
     *   <li>A team destroys enemy ownership (breaking team glass)</li>
     *   <li>Beacon is reset to neutral (set to null)</li>
     * </ul>
     *
     * @param ownership The team to set as owner, or null for unowned
     */
    public void setOwnership(Team ownership) {
        this.ownership = ownership;
    }

    /**
     * Gets the number of links from this beacon to other beacons.
     * <p>
     * This is used to enforce the maximum of 8 links per beacon.
     *
     * @return The number of active links (0-8)
     */
    public int getNumberOfLinks() {
        return links.size();
    }

    /**
     * Gets the display name for this beacon based on its coordinates.
     * <p>
     * The name format is "X, Z" (e.g., "100, -250").
     * This is used in:
     * <ul>
     *   <li>Beacon maps</li>
     *   <li>Chat messages</li>
     *   <li>Administrative commands</li>
     * </ul>
     *
     * @return The beacon name in "X, Z" format
     */
    public String getName() {
        return x + ", " + z;
    }

    /**
     * Sets the unique identifier for this beacon in the beacon register.
     * <p>
     * This is typically the index in the register's beacon list.
     * Used for serialization and lookup operations.
     *
     * @param indexOf The unique ID to assign
     */
    public void setId(int indexOf) {
        id = indexOf;
    }

    /**
     * Gets the unique identifier for this beacon in the beacon register.
     *
     * @return The beacon ID, or null if not yet registered
     */
    public Integer getId() {
        return id;
    }

    /**
     * Removes the bidirectional link between this beacon and the specified beacon.
     * <p>
     * This method:
     * <ol>
     *   <li>Devisualizes the link (removes particles/lines)</li>
     *   <li>Removes the beacon from this beacon's link set</li>
     * </ol>
     *
     * Note: This only removes the link from THIS beacon's perspective.
     * The calling code should also call removeLink on the other beacon
     * to fully remove a bidirectional link.
     *
     * @param beacon The beacon to unlink from
     */
    public void removeLink(BeaconObj beacon) {
        // Devisualize the link (remove particles/visual indicators)
        new LineVisualizer(this.getBeaconzPlugin(), new BeaconLink(this,beacon), false);
        // Remove the link from this beacon's set
        links.remove(beacon);
    }

    /**
     * Removes all links from this beacon.
     * <p>
     * This clears the entire link set, typically used when:
     * <ul>
     *   <li>Beacon is being reset</li>
     *   <li>Game is ending</li>
     *   <li>Beacon is being destroyed</li>
     * </ul>
     *
     * Warning: This does not remove links from other beacons back to this one.
     */
    public void removeLinks() {
        links.clear();
    }

    /**
     * Gets the Y coordinate (height) of this beacon.
     * <p>
     * This is an alias for {@link #getY()} for semantic clarity
     * when referring to altitude specifically.
     *
     * @return The height/altitude of the beacon
     */
    public int getHeight() {
        return y;
    }

    /**
     * Checks if this is a newly captured beacon.
     * <p>
     * New beacons have special properties:
     * <ul>
     *   <li>No mining cooldown initially</li>
     *   <li>Can be mined immediately after capture</li>
     * </ul>
     *
     * This flag is cleared when {@link #resetHackTimer()} is called.
     *
     * @return true if beacon is newly captured, false otherwise
     */
    public boolean isNewBeacon() {
        return newBeacon;
    }

    /**
     * Checks if the beacon area is clear of obstructions above it.
     * <p>
     * A beacon is considered "clear" when there are no blocks above the beacon base
     * in the 8 surrounding positions (N, S, E, W, NE, NW, SE, SW) all the way to the sky.
     * This includes checking defense blocks that may have been placed.
     * <p>
     * This check is important because:
     * <ul>
     *   <li>Beacons must be cleared before they can be captured</li>
     *   <li>Prevents griefing by placing blocks above beacons</li>
     *   <li>Ensures fair gameplay for capture attempts</li>
     * </ul>
     *
     * The method checks:
     * <ol>
     *   <li>All 8 surrounding positions (cardinal + ordinal directions)</li>
     *   <li>All registered defense block positions</li>
     *   <li>Compares highest block Y with beacon Y</li>
     * </ol>
     *
     * @return false if clear (no obstructions), true if NOT clear (obstructions exist)
     */
    public boolean isNotClear() {
        // Get the beacon block at the base level
        Block beacon = getBeaconzWorld().getBlockAt((int)location.getX(), y, (int)location.getY());

        if (DEBUG)
            getLogger().info("DEBUG: beacon block y = " + beacon.getY() + " " + beacon.getLocation());

        // STEP 1: Check all 8 surrounding positions for obstructions
        for (BlockFace face: FACES) {
            Block block = beacon.getRelative(face);

            if (DEBUG)
                getLogger().info("DEBUG: highest block at " + block.getX() + "," + block.getZ() +
                               " y = " + getHighestBlockYAt(block.getX(), block.getZ()));

            // Check if the highest block at this position is above the beacon level
            if (getHighestBlockYAt(block.getX(), block.getZ()) != beacon.getY()) {
                if (DEBUG)
                    getLogger().info("DEBUG: Beacon is not cleared");
                return true; // Found an obstruction - beacon is NOT clear
            }
        }

        if (DEBUG)
            getLogger().info("DEBUG: Checking defenses");

        // STEP 2: Check all defense block positions for obstructions
        // Defense blocks are allowed ON the beacon but not above them
        Block block;
        for (Point2D point: getRegister().getDefensesAtBeacon(this)) {
            if (DEBUG)
                getLogger().info("DEBUG: checking = " + (int)point.getX() + "," + y + ", " + (int)point.getY());

            block = getBeaconzWorld().getBlockAt((int)point.getX(), y, (int)point.getY());

            if (DEBUG)
                getLogger().info("DEBUG: Block Y = " + block.getY() + " and it is " + block.getType());

            // Check if there are blocks above this defense position
            if (block.getY() != getHighestBlockYAt((int)point.getX(), (int)point.getY())) {
                if (DEBUG)
                    getLogger().info("DEBUG: Beacon is not cleared");
                return true; // Found an obstruction above defense - beacon is NOT clear
            }
        }

        if (DEBUG)
            getLogger().info("DEBUG: Beacon is cleared");

        // All positions checked - no obstructions found
        return false; // Beacon IS clear
    }

    /**
     * Adds a defense block to this beacon's tracking system.
     * <p>
     * Defense blocks are blocks placed on top of beacons that provide:
     * <ul>
     *   <li>Protection from capture/mining</li>
     *   <li>Defensive bonuses (dispensers, etc.)</li>
     *   <li>Link distance reduction</li>
     *   <li>Locking mechanism (emerald blocks)</li>
     * </ul>
     *
     * @param block The block being added as defense
     * @param levelRequired The level requirement for this defense block
     * @param uuid The UUID of the player who placed the block
     */
    public void addDefenseBlock(Block block, int levelRequired, UUID uuid) {
        defenseBlocks.put(block, new DefenseBlock(block, levelRequired, uuid));
    }
    
    /**
     * Adds a defense block to this beacon's tracking system.
     * <p>
     * This overload accepts a UUID as a String, useful for deserialization
     * from configuration files or databases.
     *
     * @param block The block being added as defense
     * @param levelRequired The level requirement for this defense block
     * @param uuid The UUID of the player who placed the block (as String)
     */
    public void addDefenseBlock(Block block, int levelRequired, String uuid) {
        defenseBlocks.put(block, new DefenseBlock(block, levelRequired, uuid)); 
    }

    /**
     * Removes a defense block from this beacon's tracking system.
     * <p>
     * Called when:
     * <ul>
     *   <li>A player breaks a defense block</li>
     *   <li>A defense block is destroyed (creeper, etc.)</li>
     *   <li>Beacon is being reset</li>
     * </ul>
     *
     * @param block The defense block to remove
     */
    public void removeDefenseBlock(Block block) {
        defenseBlocks.remove(block);
    }

    /**
     * Gets the map of all defense blocks on this beacon.
     * <p>
     * The map contains:
     * <ul>
     *   <li>Key: The block location</li>
     *   <li>Value: DefenseBlock object with level and owner info</li>
     * </ul>
     *
     * @return The defense blocks map
     */
    public HashMap<Block, DefenseBlock> getDefenseBlocks() {
        return defenseBlocks;
    }

    /**
     * Sets the defense blocks map for this beacon.
     * <p>
     * This is typically used during deserialization when loading
     * beacon data from storage.
     *
     * @param defenseBlocks The defense blocks map to set
     */
    public void setDefenseBlocks(HashMap<Block,DefenseBlock> defenseBlocks) {
        this.defenseBlocks = defenseBlocks;
    }

    /**
     * Removes the longest link from this beacon and cleans up related triangles.
     * <p>
     * This method is called when a link-reducing block (like a special defense block)
     * is removed from a beacon. Since the link distance bonus is gone, the longest
     * link may now exceed the maximum link distance and must be removed.
     * <p>
     * The method performs several operations:
     * <ol>
     *   <li>Finds the furthest linked beacon using squared distance calculation</li>
     *   <li>Removes the bidirectional link between this and the furthest beacon</li>
     *   <li>Identifies all triangle fields that use both beacons as vertices</li>
     *   <li>Removes potion effects from players inside those triangles</li>
     *   <li>Deletes the affected triangles from the game register</li>
     * </ol>
     *
     * @return true if a link was removed, false if no links exist
     */
    public boolean removeLongestLink() {
        BeaconObj furthest = null;
        double maxDistance = 0;

        // STEP 1: Find the furthest linked beacon
        // Use distanceSq for performance (avoids sqrt calculation)
        for (BeaconObj link: links) {
            double distance = location.distanceSq(link.getPoint());
            if (distance > maxDistance) {
                maxDistance = distance;
                furthest = link;
            }
        }

        if (furthest != null) {
            // STEP 2: Remove the bidirectional link
            furthest.removeLink(this);  // Remove from other beacon
            removeLink(furthest);        // Remove from this beacon

            // STEP 3: Clean up triangle fields that used this link
            // Any triangle that has both beacons as vertices must be removed
            Iterator<TriangleField> it = getRegister().getTriangleFields().iterator();
            while (it.hasNext()) {
                TriangleField triangle = it.next();

                // Check if this triangle uses both beacons as vertices
                if (triangle.hasVertex(this.location) && triangle.hasVertex(furthest.location)) {

                    // STEP 4: Remove potion effects from players in this triangle
                    // Triangle fields provide buffs/debuffs that must be cleared
                    for (Player player: getServer().getOnlinePlayers()) {
                        // Only check players in the Beaconz world
                        if (getBeaconzWorld().equals(player.getWorld())) {
                            // Check if player is inside this triangle
                            if (triangle.contains(new Point2D.Double(player.getLocation().getX(), player.getLocation().getZ()))) {
                                // Player is inside - remove all triangle effects
                                for (PotionEffect effect : getPml().getTriangleEffects(player.getUniqueId()))
                                    player.removePotionEffect(effect.getType());
                            }
                        }
                    }

                    // STEP 5: Remove this triangle from the register
                    it.remove();
                }
            }
            return true; // Link was removed successfully
        }

        return false; // No links exist to remove
    }

    /**
     * Verifies and repairs the beacon structure to ensure it's complete and correct.
     * <p>
     * This method checks all critical components of a beacon and replaces any missing
     * or incorrect blocks. The beacon structure consists of:
     * <pre>
     * Y+2: [Capstone] - Obsidian (unowned) or Team Glass (owned)
     * Y+1: [Beacon]   - Beacon block
     * Y:   [Pyramid]  - 3x3 diamond block base (9 blocks total)
     * </pre>
     *
     * This method is typically called during:
     * <ul>
     *   <li>Server startup to repair any damage</li>
     *   <li>Admin commands to fix corrupted beacons</li>
     *   <li>Scheduled maintenance tasks</li>
     * </ul>
     *
     * The integrity check validates:
     * <ol>
     *   <li>Beacon block exists at (x, y, z)</li>
     *   <li>Capstone matches ownership (obsidian vs team glass)</li>
     *   <li>All 9 pyramid blocks are diamond blocks</li>
     * </ol>
     *
     * Any missing or incorrect blocks are logged as SEVERE and replaced automatically.
     */
    public void checkIntegrity() {
        // CHECK 1: Verify beacon block exists
        Block b = getBeaconzWorld().getBlockAt(x, y, z);
        if (!b.getType().equals(Material.BEACON)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing beacon block!");
            b.setType(Material.BEACON);
        }

        // CHECK 2a: Verify unowned capstone (obsidian)
        if (ownership == null && !b.getRelative(BlockFace.UP).getType().equals(Material.OBSIDIAN)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing capstone block!");
            b.getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
        }
        
        // CHECK 2b: Verify owned capstone (team glass)
        if (ownership != null && (!MaterialTags.STAINED_GLASS.isTagged(b.getRelative(BlockFace.UP))
                && b.getRelative(BlockFace.UP).getType() != Settings.teamBlock.get(ownership))) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing team glass block!");
            b.getRelative(BlockFace.UP).setType(Settings.teamBlock.get(ownership));
        }

        // CHECK 3: Verify 3x3 diamond pyramid base
        // Move down one block to the pyramid level
        b = b.getRelative(BlockFace.DOWN);

        // Center block
        if (!b.getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing diamond block!");
            b.setType(Material.DIAMOND_BLOCK);
        }

        // Cardinal directions (N, S, E, W)
        if (!b.getRelative(BlockFace.SOUTH).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing S diamond block!");
            b.getRelative(BlockFace.SOUTH).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.EAST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing E diamond block!");
            b.getRelative(BlockFace.EAST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.WEST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing W diamond block!");
            b.getRelative(BlockFace.WEST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.NORTH).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing N diamond block!");
            b.getRelative(BlockFace.NORTH).setType(Material.DIAMOND_BLOCK);
        }

        // Ordinal directions (NE, NW, SE, SW)
        if (!b.getRelative(BlockFace.SOUTH_EAST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing SE diamond block!");
            b.getRelative(BlockFace.SOUTH_EAST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.SOUTH_WEST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing SW diamond block!");
            b.getRelative(BlockFace.SOUTH_WEST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.NORTH_EAST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing NE diamond block!");
            b.getRelative(BlockFace.NORTH_EAST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.NORTH_WEST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing NW diamond block!");
            b.getRelative(BlockFace.NORTH_WEST).setType(Material.DIAMOND_BLOCK);
        }
    }

    /**
     * Gets the level of the highest defense block placed on this beacon.
     * <p>
     * Defense blocks have varying "level requirements" which determine:
     * <ul>
     *   <li>How difficult they are to place</li>
     *   <li>How effective they are as defenses</li>
     *   <li>Their strategic value</li>
     * </ul>
     *
     * This method also performs cleanup by removing any defense blocks that have
     * become air (destroyed by creative mode players or gravity).
     *
     * @return The highest level of any defense block on this beacon (0 if none)
     */
    public int getHighestBlockLevel() {
        int highestBlock = 0;
        Iterator<Entry<Block, DefenseBlock>> it = this.getDefenseBlocks().entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, DefenseBlock> defenseBlock = it.next();
            if (defenseBlock.getKey().getType().equals(Material.AIR)) {
                // Clean up if any blocks have been removed by creative mode or gravity
                it.remove();
            } else {
                highestBlock = Math.max(highestBlock, defenseBlock.getValue().getLevel());
            }
        } 
        return highestBlock;
    }
    
    /**
     * Gets the Y coordinate (height) of the highest defense block on this beacon.
     * <p>
     * This is used to:
     * <ul>
     *   <li>Determine how tall the defense tower is</li>
     *   <li>Check locking layers (see {@link #isLocked()})</li>
     *   <li>Calculate clearance requirements</li>
     * </ul>
     *
     * This method also performs cleanup by removing any defense blocks that have
     * become air (destroyed by creative mode players or gravity).
     *
     * @return The Y coordinate of the highest defense block (beacon Y if none)
     */
    public int getHighestBlockY() {
        int highestBlock = y;
        Iterator<Entry<Block, DefenseBlock>> it = this.getDefenseBlocks().entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, DefenseBlock> defenseBlock = it.next();
            if (defenseBlock.getKey().getType().equals(Material.AIR)) {
                // Clean up if any blocks have been removed by creative mode or gravity
                it.remove();
            } else {
                highestBlock = Math.max(highestBlock, defenseBlock.getValue().getBlock().getY());
            }
        } 
        return highestBlock;
    }
    
    
    /**
     * Checks if any layer above the beacon meets the locking criteria.
     * <p>
     * This is a mechanism to allow for permanent ownership of beacons. When a layer
     * has sufficient "locking blocks" (typically emerald blocks), those blocks become
     * unbreakable by all except the owner team.
     * <p>
     * The method checks each vertical level from the beacon up to the highest defense
     * block. If ANY level has zero or negative "blocks needed to lock" (meaning it's
     * fully locked), the entire beacon is considered locked.
     *
     * @return true if any level is fully locked, false if no levels are locked
     * @see #nbrToLock(int)
     */
    public Boolean isLocked () {
        boolean rc = false;
        int maxHeight = getHighestBlockY();

        // Check each vertical level from beacon base to highest defense
        for (int i = y; i <= maxHeight; i++) {
            // If nbrToLock is <= 0, this level is fully locked
            if (nbrToLock(i) <= 0) {
                rc = true;
                break; // One locked level is enough
            }
        }

        return rc;
    }
    
    /**
     * Calculates how many more "locking blocks" are needed at a given height to fully lock the beacon.
     * <p>
     * Locking blocks (default: emerald blocks) create an impenetrable layer when placed
     * around the beacon. The number required is proportional to the team size:
     * <ul>
     *   <li>Larger teams need more blocks (scales with Settings.nbrLockingBlocks)</li>
     *   <li>Smaller teams need fewer blocks (proportional to largest team)</li>
     *   <li>Maximum of 8 blocks required (all surrounding positions)</li>
     * </ul>
     *
     * Return values:
     * <ul>
     *   <li><b>Positive:</b> Number of blocks still needed</li>
     *   <li><b>Zero:</b> Exactly enough blocks (locked)</li>
     *   <li><b>Negative:</b> More than enough blocks (over-locked)</li>
     * </ul>
     *
     * The calculation:
     * <ol>
     *   <li>Gets max team size across all teams in the game</li>
     *   <li>Scales requirement: (maxBlocks * ownerTeamSize) / maxTeamSize</li>
     *   <li>Caps at 8 blocks (all surrounding positions)</li>
     *   <li>Counts actual locking blocks at the specified height</li>
     *   <li>Returns difference: required - present</li>
     * </ol>
     *
     * @param height The Y coordinate to check for locking blocks
     * @return Number of locking blocks still needed (can be negative if over-locked)
     */
    public int nbrToLock(int height) {
        int maxLocking = Settings.nbrLockingBlocks;
        Material lockingBlock = Material.EMERALD_BLOCK;
        int missing = maxLocking;

        // Only check heights above the beacon base
        if (height >= y) {
        
            // Get the configured locking block material (default: emerald)
            if (Material.getMaterial(Settings.lockingBlock.toUpperCase()) != null) {
                lockingBlock = Material.getMaterial(Settings.lockingBlock.toUpperCase());                
            }
            
            // STEP 1: Calculate required blocks proportional to team size
            int reqLocking = 3; // Default minimum
            int maxSize = ownership.getSize();

            // Find the largest team in the game for proportional scaling
            Scorecard sc = getGameMgr().getSC(ownership);
            if (sc != null) {
                for (Team t : sc.getTeams()) {
                    if (t.getSize() > maxSize) {
                        maxSize = t.getSize();
                    }                    
                }
            }

            // Scale requirement: larger teams need more blocks
            reqLocking = maxLocking * ownership.getSize() / maxSize; // Integer division

            // Cap at 8 blocks (all surrounding positions)
            if (reqLocking > 8) reqLocking = 8;

            // STEP 2: Count actual locking blocks at this height
            int lockBlocks = 0;
            Block b = getBeaconzWorld().getBlockAt(x, height, z);

            // Check center position
            if (b.getType().equals(lockingBlock)) lockBlocks++;

            // Check all 8 surrounding positions
            if (b.getRelative(BlockFace.SOUTH).getType().equals(lockingBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.SOUTH_EAST).getType().equals(lockingBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.SOUTH_WEST).getType().equals(lockingBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.EAST).getType().equals(lockingBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.WEST).getType().equals(lockingBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.NORTH).getType().equals(lockingBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.NORTH_EAST).getType().equals(lockingBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.NORTH_WEST).getType().equals(lockingBlock)) lockBlocks++;

            // STEP 3: Calculate how many more are needed
            missing = reqLocking - lockBlocks;
        }        
        
        return missing;
    }        
    
}
