package com.wasteofplastic.beaconz.core;

import java.util.UUID;

import org.bukkit.block.Block;

/**
 * Represents a defensive block placed by a player to protect a beacon.
 *
 * <p>Defense blocks are part of the beacon protection system where players can place
 * blocks around their beacon to defend it. Each defense block has:
 * <ul>
 *   <li>A reference to the actual Bukkit Block</li>
 *   <li>A level requirement that must be met to place or remove it</li>
 *   <li>A record of which player placed it (UUID)</li>
 * </ul>
 *
 * <p><b>Storage:</b> DefenseBlocks are stored in a {@code HashMap<Block, DefenseBlock>}
 * in {@link BeaconObj#getDefenseBlocks()}, where the Block itself is the key. This design
 * allows for fast lookup when players interact with blocks.
 *
 * <p><b>Level System:</b> The level field represents the player level required to:
 * <ul>
 *   <li>Place this type of defense block</li>
 *   <li>Remove or damage this defense block (for enemy players)</li>
 *   <li>Remove other players' blocks (based on {@code Settings.removaldelta})</li>
 * </ul>
 *
 * <p><b>Ownership Rules:</b>
 * <ul>
 *   <li>Players can always remove their own defense blocks</li>
 *   <li>Team members may need extra levels to remove teammates' blocks</li>
 *   <li>Enemy players must meet level requirements and follow top-down removal rules</li>
 * </ul>
 *
 * <p><b>Equality:</b> The {@link #equals(Object)} method compares against Block objects,
 * not other DefenseBlock objects. This allows DefenseBlock instances to be compared
 * directly with Bukkit Block instances for convenience.
 *
 * @author tastybento
 * @see BeaconObj#addDefenseBlock(Block, int, UUID)
 * @see BeaconObj#removeDefenseBlock(Block)
 * @see BeaconObj#getDefenseBlocks()
 */
public class DefenseBlock {

    /** The UUID of the player who placed this defense block */
    private UUID placer;

    /** The level requirement for this defense block */
    private int level;

    /** The actual Bukkit block being defended */
    private Block block;
    
    /**
     * Creates a new defense block with a UUID.
     *
     * <p>This constructor is used when adding defense blocks during gameplay,
     * where the player's UUID is readily available.
     *
     * @param block the Bukkit block being added as a defense
     * @param level the level requirement for this defense block
     * @param uuid the UUID of the player who placed the block
     */
    public DefenseBlock(Block block, int level, UUID uuid) {
        this.block = block;
        this.level = level;
        this.placer = uuid;
    }
    
    /**
     * Creates a new defense block with a UUID string.
     *
     * <p>This constructor is used when loading defense blocks from persistent storage,
     * where the UUID is stored as a string. If the UUID string is invalid, the placer
     * will be set to null.
     *
     * @param block the Bukkit block being added as a defense
     * @param level the level requirement for this defense block
     * @param uuid the UUID string of the player who placed the block
     */
    public DefenseBlock(Block block, int level, String uuid) {
        this.block = block;
        this.level = level;
        try {
            this.placer = UUID.fromString(uuid);
        } catch (Exception e) {
            this.placer = null;
        }
    }

    /**
     * Compares this DefenseBlock with a Block object for equality.
     *
     * <p><b>Important:</b> This method compares against {@link Block} objects,
     * NOT other DefenseBlock objects. This design allows for convenient lookups
     * when checking if a Block is a defense block:
     *
     * <pre>
     * // Allows this pattern:
     * if (defenseBlockMap.containsKey(block)) { ... }
     * </pre>
     *
     * <p>The method delegates to the Block's equals method, ensuring consistency
     * with Bukkit's block equality semantics (location-based comparison).
     *
     * @param obj the object to compare with (expected to be a Block)
     * @return true if obj is a Block and equals this defense block's block
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Block b)) {
            return false;
        }
        return b.equals(this.block);
    }
    
    /**
     * Gets the UUID of the player who placed this defense block.
     *
     * @return the UUID of the placer, or null if unknown or invalid
     */
    public UUID getPlacer() {
        return placer;
    }

    /**
     * Sets the UUID of the player who placed this defense block.
     *
     * @param placer the UUID of the placer
     */
    public void setPlacer(UUID placer) {
        this.placer = placer;
    }

    /**
     * Gets the level requirement for this defense block.
     *
     * <p>This level is used to determine:
     * <ul>
     *   <li>What player level is required to place this block</li>
     *   <li>What level enemy players need to break/damage this block</li>
     *   <li>The difficulty of removing this block in top-down order</li>
     * </ul>
     *
     * @return the level requirement
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the level requirement for this defense block.
     *
     * @param level the level requirement
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Gets the Bukkit block associated with this defense block.
     *
     * @return the block
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Sets the Bukkit block associated with this defense block.
     *
     * @param block the block to set
     */
    public void setBlock(Block block) {
        this.block = block;
    }
}
