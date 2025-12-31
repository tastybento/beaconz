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

package com.wasteofplastic.beaconz.listeners;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;

/**
 * Protects beacons by making the blocks around them harder to break and immune to explosions.
 *
 * <p>This listener implements a "beacon defense zone" concept where blocks within a certain
 * range of beacons are protected from damage. This serves multiple purposes:
 * <ul>
 *   <li>Prevents easy griefing of beacon pyramids</li>
 *   <li>Makes beacons more defensible strategic objectives</li>
 *   <li>Creates interesting gameplay where attacking beacons requires sustained effort</li>
 *   <li>Protects beacons from accidental explosion damage</li>
 *   <li>Prevents trees from obscuring beacon beams</li>
 * </ul>
 *
 * <p><b>Protection Mechanics:</b>
 * <ul>
 *   <li>Blocks within {@link #RANGE} blocks horizontally of a beacon are protected</li>
 *   <li>Only blocks below the beacon level are protected (up to RANGE blocks down)</li>
 *   <li>Breaking protected blocks has a {@link #PROBABILITY} chance to be cancelled</li>
 *   <li>Failed break attempts damage the player's tool</li>
 *   <li>Explosions cannot damage protected blocks</li>
 *   <li>Trees cannot grow above beacons (prevents beam obstruction)</li>
 * </ul>
 *
 * @author tastybento
 */
public class BeaconSurroundListener extends BeaconzPluginDependent implements Listener {

    /** Debug mode flag - set to false in production */
    private final static boolean DEBUG = true;

    /**
     * Horizontal range (in blocks) around beacons where blocks become harder to break.
     * Forms a cylinder of protection around each beacon.
     */
    private static final int RANGE = 10;

    /**
     * Probability that a block break attempt will be cancelled (1.0 = 100% protection).
     * This makes blocks extremely difficult to break near beacons.
     */
    private static final double PROBABILITY = 1D;

    /**
     * Percentage of tool durability damage applied on failed break attempts (0.0 = no damage).
     * Currently set to 0 to avoid excessive tool wear.
     */
    private static final double DAMAGE = 0D;

    /**
     * Set of materials that are protected around beacons.
     * These are typically structural/foundational blocks that form beacon pyramids
     * and the surrounding terrain. The beacon itself is also protected.
     */
    private static final Set<Material> protectedMaterials = Set.of(
            Material.BARRIER,           // Admin protection blocks
            Material.BEACON,            // The beacon itself
            Material.BEDROCK,           // World foundation
            Material.CLAY,              // Natural terrain
            Material.COBBLESTONE,       // Common building material
            Material.DIRT,              // Natural terrain
            Material.GRASS_BLOCK,       // Natural terrain
            Material.DIRT_PATH,         // Pathways
            Material.MOSSY_COBBLESTONE, // Decorative/natural
            Material.OBSIDIAN,          // Strong material
            Material.RED_SANDSTONE,     // Building material
            Material.SANDSTONE,         // Building material
            Material.TERRACOTTA,        // Building material
            Material.STONE);            // Natural terrain

    /**
     * Constructs a new beacon surround listener.
     *
     * @param plugin the main Beaconz plugin instance
     */
    public BeaconSurroundListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles player attempts to damage blocks around a beacon.
     *
     * <p>This method intercepts block damage events and applies special protection rules
     * to blocks near beacons. The protection creates a defensive zone that makes it
     * difficult to grief or destroy beacon pyramids.
     *
     * <p><b>Protection Rules:</b>
     * <ul>
     *   <li>Only applies to blocks in the protected materials list</li>
     *   <li>Only applies within {@link #RANGE} blocks horizontally of a beacon</li>
     *   <li>Only protects blocks BELOW the beacon level (prevents tower griefing)</li>
     *   <li>Allows breaking blocks more than RANGE blocks below the beacon</li>
     *   <li>Has a {@link #PROBABILITY} chance to cancel the break attempt</li>
     *   <li>Can damage the player's tool on failed attempts (if DAMAGE > 0)</li>
     * </ul>
     *
     * <p><b>Y-Level Protection Zone:</b>
     * <pre>
     *     Beacon Y=100
     *          ^
     *          |  No protection (can break above beacon)
     *     Y=100 +--------------------------------- Beacon Level
     *          |
     *     Y=99  |  PROTECTED ZONE (1 block below allowed)
     *          |  (cancels break attempts)
     *          |
     *     Y=90  +--------------------------------- RANGE blocks below
     *          |
     *          v  No protection (can break far below)
     * </pre>
     *
     * @param event the block damage event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
        // Debug logging to track when this event fires
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());

        // Step 1: Verify this is happening in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return; // Not our world, ignore
        }

        // Step 2: Check if this block type is one we protect
        // Only specific materials (like cobblestone, dirt, etc.) are protected
        if (!protectedMaterials.contains(event.getBlock().getType())) {
            return; // Not a protected material, allow normal breaking
        }

        // Step 3: Find any beacons within RANGE blocks of this block
        List<BeaconObj> beacons = getRegister().getNearbyBeacons(event.getBlock().getLocation(), RANGE);
        if (beacons.isEmpty()) {
            return; // No nearby beacons, no protection applies
        }

        // Step 4: Determine the protection zone Y-level
        // Find the lowest beacon Y-coordinate among nearby beacons
        int lowestY = getBeaconzWorld().getMaxHeight();
        for (BeaconObj beacon : beacons) {
            lowestY = Math.min(lowestY, beacon.getY());
        }

        // Step 5: Check if block is in the protected Y-range
        // Protection applies from (beaconY - 1) down to (beaconY - RANGE)
        // You can break:
        //   - Above or at beacon level (>= lowestY - 1)
        //   - Far below the range (< lowestY - RANGE)
        if (event.getBlock().getY() >= (lowestY-1) || event.getBlock().getY() < lowestY - RANGE) {
            return; // Outside protected zone, allow breaking
        }

        // Step 6: Apply probabilistic protection
        // Use random chance to determine if this break attempt succeeds
        Random rand = new Random();
        if (rand.nextDouble() < PROBABILITY) {
            // Block the damage - player cannot break this block
            event.setCancelled(true);

            // Visual feedback would go here (particle effects, etc.)
            //ParticleEffect.PORTAL.display(0F, 0F, 0F, 1F, 10, event.getBlock().getLocation(), 10D);

            // Step 7: Optional tool durability damage
            // Punish failed break attempts by damaging the player's tool
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item != null && !item.getType().equals(Material.AIR)) {
                short maxDurability = item.getType().getMaxDurability();
                if (DEBUG)
                    getLogger().info("DEBUG: max durability = " + maxDurability);

                // Only damage items that have durability (tools/armor)
                if (maxDurability > 0) {
                    short durability = item.getDurability();
                    if (DEBUG)
                        getLogger().info("DEBUG: durability = " + durability);

                    // Calculate damage amount as percentage of max durability
                    short damage = (short)((double)maxDurability * DAMAGE);
                    if (DEBUG)
                        getLogger().info("DEBUG: damager = " + damage);

                    durability += damage;

                    // Check if tool should break from this damage
                    if (durability >= maxDurability) {
                        // Tool breaks - remove from inventory
                        event.getPlayer().getInventory().setItemInMainHand(null);
                        event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(),
                                Sound.ENTITY_ITEM_BREAK, 1F, 1F);
                    } else {
                        // Tool damaged but not broken - play damage sound
                        event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(),
                                Sound.ENTITY_ITEM_BREAK, 2F, 2F);
                    }

                    // Apply the durability change
                    item.setDurability(durability);
                }
            }
        }
        // If random check fails (< PROBABILITY), the event proceeds normally
    }

    /**
     * Protects blocks around beacons from explosion damage.
     *
     * <p>This method intercepts explosion events (TNT, creepers, etc.) and prevents
     * them from damaging blocks in the protected zone around beacons. This serves
     * multiple purposes:
     * <ul>
     *   <li>Prevents griefing via TNT cannons or creeper suicide attacks</li>
     *   <li>Protects beacon pyramids from accidental explosion damage</li>
     *   <li>Maintains beacon integrity during combat</li>
     *   <li>Forces attackers to manually break blocks rather than using explosives</li>
     * </ul>
     *
     * <p><b>Implementation Notes:</b>
     * <ul>
     *   <li>Works by removing protected blocks from the explosion's block list</li>
     *   <li>Blocks are removed BEFORE damage is applied, preventing any damage</li>
     *   <li>Uses iterator pattern to safely modify list during iteration</li>
     *   <li>Protection applies to same Y-range as manual breaking protection</li>
     * </ul>
     *
     * <p><b>Example Scenario:</b>
     * <pre>
     * TNT explodes near beacon:
     *   1. Event fires with list of 50 blocks to damage
     *   2. 20 blocks are within RANGE of a beacon
     *   3. Those 20 blocks are removed from the damage list
     *   4. Explosion only damages the remaining 30 blocks
     *   5. Beacon pyramid remains intact
     * </pre>
     *
     * @param event the entity explosion event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        // Debug logging to track explosion events
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());

        // Step 1: Verify this explosion is in the Beaconz world
        World world = event.getLocation().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return; // Different world, ignore
        }

        // Step 2: Check each block that would be damaged by the explosion
        // Use iterator to safely remove items while iterating
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();

            // Step 3: Only protect materials in our protected list
            if (protectedMaterials.contains(block.getType())) {
                if (DEBUG)
                    getLogger().info("DEBUG: " + block.getLocation());

                // Step 4: Check if this block is near any beacon
                List<BeaconObj> beacons = getRegister().getNearbyBeacons(block.getLocation(), RANGE);
                if (!beacons.isEmpty()) {
                    // Step 5: Find the lowest beacon Y-coordinate
                    // This determines the protected zone's vertical extent
                    int lowestY = getBeaconzWorld().getMaxHeight();
                    for (BeaconObj beacon : beacons) {
                        lowestY = Math.min(lowestY, beacon.getY());
                    }

                    // Step 6: Block is protected - remove from explosion damage list
                    // Note: Unlike manual breaking, explosions get FULL protection
                    // (no Y-level checks or probability - always protected)
                    it.remove();
                }
            }
        }
        // After this method, the explosion will damage only the non-protected blocks
    }

    /**
     * Prevents trees from growing above beacons to maintain clear beam lines.
     *
     * <p>Beacon beams are a critical visual element in the game, indicating beacon
     * ownership and activity. Trees growing above beacons would obstruct these beams
     * and make it difficult to identify beacon locations from a distance.
     *
     * <p><b>Why This Matters:</b>
     * <ul>
     *   <li>Beacon beams must be visible for strategic gameplay</li>
     *   <li>Players need to see beacon ownership from afar</li>
     *   <li>Obstructed beams make the map harder to read</li>
     *   <li>Natural tree growth could accidentally obscure important beacons</li>
     * </ul>
     *
     * <p><b>Implementation Details:</b>
     * <ul>
     *   <li>Checks ALL blocks that will be placed by tree growth</li>
     *   <li>Cancels entire tree growth if ANY block would be above a beacon</li>
     *   <li>Uses the beacon register's isAboveBeacon method for efficient checking</li>
     *   <li>Applies to saplings, mushrooms, and other natural structure growth</li>
     * </ul>
     *
     * <p><b>Example Scenario:</b>
     * <pre>
     * Sapling next to beacon grows:
     *   1. StructureGrowEvent fires with list of blocks to place
     *   2. Check each block location
     *   3. One block would be placed at Y=101 directly above beacon at Y=100
     *   4. Cancel the entire tree growth
     *   5. Sapling remains, beacon beam stays clear
     * </pre>
     *
     * @param e the structure grow event (trees, mushrooms, etc.)
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTreeGrow(final StructureGrowEvent e) {
        // Debug logging to track tree growth events
        if (DEBUG)
            getLogger().info("DEBUG: " + e.getEventName());

        // Step 1: Verify this is happening in the Beaconz world
        World world = e.getLocation().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return; // Different world, allow normal tree growth
        }

        // Step 2: Check each block that will be placed by the growing structure
        // This includes trunk, leaves, branches, etc.
        for (BlockState b : e.getBlocks()) {
            // Step 3: Check if this block position is directly above any beacon
            // A beacon needs a clear vertical path for its beam to the sky
            if (getRegister().isAboveBeacon(b.getLocation())) {
                // Step 4: Cancel the ENTIRE structure growth
                // We don't selectively allow some blocks - it's all or nothing
                // This prevents partial trees that might still obscure the beam
                e.setCancelled(true);

                // Break out of loop early - no need to check remaining blocks
                // since we're already cancelling the whole event
                break;
            }
        }
        // If we get here without cancelling, the tree/structure grows normally
    }
}

