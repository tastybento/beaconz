/*
 * Copyright (c) 2015 - 2026 tastybento
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.core.DefenseBlock;

/**
 * Listener class that implements automated projectile defense systems for beacons.
 * <p>
 * This class manages the automated turret-style defense mechanism where beacons can shoot
 * projectiles at approaching enemy players. The defense system works as follows:
 * <ul>
 *   <li>Dispensers placed on beacons act as defensive turrets</li>
 *   <li>When enemy players enter range (10 blocks), dispensers automatically fire</li>
 *   <li>Supports multiple projectile types: arrows, tipped arrows, spectral arrows, and fireballs</li>
 *   <li>Implements friendly fire prevention - team members are not damaged</li>
 *   <li>Calculates line of sight and facing direction for realistic targeting</li>
 *   <li>Prevents block damage from defensive fireballs</li>
 * </ul>
 *
 * The system tracks fired projectiles to apply team-based damage rules and manages
 * the lifecycle of defensive weapons. It also handles both walking and vehicle-based
 * player movement to ensure consistent defense activation.
 *
 * @author tastybento
 * @since 1.0
 */
public class BeaconProjectileDefenseListener extends BeaconzPluginDependent implements Listener {

    /** Maximum range in blocks at which beacon defenses can detect and fire at players */
    private static final int RANGE = 10;

    /**
     * Tracks projectiles fired by beacon defenses mapped to the team that owns the beacon.
     * This allows the damage handler to determine if friendly fire should be prevented.
     * Projectiles are removed from this map once they hit a target or are cleaned up.
     */
    private final HashMap<UUID, Team> projectiles = new HashMap<>();
    /**
     * Constructs a new BeaconProjectileDefenseListener.
     * <p>
     * Initializes the listener to handle beacon defense projectile firing and damage control.
     * The projectiles map is initialized empty and will be populated as defenses fire.
     *
     * @param plugin The Beaconz plugin instance
     */
    public BeaconProjectileDefenseListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Prevents block damage from fireball explosions caused by beacon defenses.
     * <p>
     * This handler intercepts explosion events from fireballs fired by beacon defense systems
     * and clears the block damage list to prevent terrain destruction. The fireballs can still
     * damage players (handled by {@link #onAttackDamage(EntityDamageByEntityEvent)}), but won't
     * create craters or destroy structures.
     * <p>
     * When a tracked defensive fireball explodes:
     * <ol>
     *   <li>The block damage list is cleared to prevent terrain damage</li>
     *   <li>The projectile is removed from tracking (it's completed its purpose)</li>
     * </ol>
     *
     * This ensures beacon defenses are effective against players without causing
     * collateral damage to the game world.
     *
     * @param e The EntityExplodeEvent containing the exploding entity and affected blocks
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        // Identify which entity is exploding
        Entity expl = e.getEntity();
        if (expl == null) {
            return; // No entity associated with explosion (e.g., bed explosion)
        }

        // Quick world check - only process events in the Beaconz world
        if (!e.getEntity().getWorld().equals(getBeaconzWorld())) {
            return;
        }

        // Check if this explosion is from one of our tracked defensive projectiles
        if (projectiles.containsKey(expl.getUniqueId())) {
            // Clear all block damage to prevent terrain destruction
            e.blockList().clear();

            // Remove the projectile from tracking - it's done its job
            projectiles.remove(expl.getUniqueId());
        }
    }

    /**
     * Implements friendly fire prevention for beacon defense projectiles.
     * <p>
     * This handler intercepts damage events from beacon defense projectiles and applies
     * team-based damage rules. The logic ensures that:
     * <ul>
     *   <li>Players are not damaged by projectiles from their own team's beacons</li>
     *   <li>Players ARE damaged by projectiles from enemy team's beacons</li>
     *   <li>Projectiles with unknown team ownership are cancelled (safety measure)</li>
     * </ul>
     *
     * The handler uses the {@link #projectiles} map to determine which team fired
     * each projectile, then compares it against the damaged player's team. After
     * processing, the projectile is removed from tracking as it has hit its target.
     *
     * @param event The EntityDamageByEntityEvent for damage caused by projectiles
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onAttackDamage(EntityDamageByEntityEvent event) {
        // Only protect players - other entities can be damaged freely
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Quick world check - only process events in the Beaconz world
        if (!event.getEntity().getWorld().equals(getBeaconzWorld())) {
            return;
        }

        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        // Check if the damage is from one of our tracked defensive projectiles
        if (damager != null && (damager instanceof Projectile) && projectiles.containsKey(damager.getUniqueId())) {
            // Retrieve which team fired this projectile
            Team team = projectiles.get(damager.getUniqueId());

            // Clean up - remove the projectile from tracking now that it's hit something
            projectiles.remove(damager.getUniqueId());

            Player player = (Player)entity;

            // Determine the player's team affiliation
            Team playersTeam = getGameMgr().getPlayerTeam(player);

            // Safety check: if team ownership is unknown, cancel damage
            if (team == null) {
                event.setCancelled(true);
                return;
            }

            // Friendly fire prevention: cancel damage if player is on the same team
            if (playersTeam.equals(team)) {
                event.setCancelled(true);
            }
            // If teams differ, allow the damage (enemy hit)
        }
    }

    /**
     * Detects when players move within range of beacon defenses while walking.
     * <p>
     * This handler monitors player movement and triggers beacon defense systems when
     * enemy players approach within range. The handler:
     * <ol>
     *   <li>Filters out "head-only" movements (looking around without walking)</li>
     *   <li>Validates the player is in the Beaconz world</li>
     *   <li>Delegates to {@link #fireOnPlayer(Player, Location, Location)} for defense logic</li>
     * </ol>
     *
     * Note: This handler does NOT detect teleportation - only physical movement.
     * Teleporting players won't trigger defenses until they move again after teleporting.
     *
     * @param event The PlayerMoveEvent containing movement from/to locations
     * @see #onVehicleMove(VehicleMoveEvent) for vehicle-based movement detection
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Filter out head-only movements (player looking around without walking)
        // We only care about actual position changes in X, Y, or Z coordinates
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // Quick world check - only process events in the Beaconz world
        World world = event.getTo().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Delegate to the main defense firing logic
        Player player = event.getPlayer();
        fireOnPlayer(player, event.getFrom(), event.getTo());
    }

    /**
     * Fires a projectile from a defensive block toward a target player.
     * <p>
     * This method implements sophisticated targeting logic that includes:
     * <ul>
     *   <li>Line of sight verification - only fires if target is visible</li>
     *   <li>Dispenser facing validation - only fires if target is in the dispenser's arc</li>
     *   <li>Lead targeting - aims where the player is moving, not where they are</li>
     *   <li>Multiple projectile types - arrows, tipped arrows, spectral arrows, fireballs</li>
     *   <li>Obstruction checking - won't fire if blocked</li>
     * </ul>
     *
     * The firing sequence:
     * <ol>
     *   <li>Calculate vector from dispenser to player's head position</li>
     *   <li>Determine if player is within the dispenser's facing arc</li>
     *   <li>Check for obstructions between dispenser and player</li>
     *   <li>Select and spawn appropriate projectile type from dispenser inventory</li>
     *   <li>Track the projectile for team-based damage rules</li>
     * </ol>
     *
     * @param block The defensive block (typically a dispenser) that will fire
     * @param target The target player's current location
     * @param aim The movement vector for lead targeting (where player is heading)
     * @param team The team that owns this defensive beacon
     */
    private void fireProjectile(Block block, Location target, Vector aim, Team team) {
        // Calculate the player's head position (add 1.75 blocks for eye height)
        // Also center the position in the block (add 0.5 to X and Z)
        Vector playerLoc = target.toVector().add(new Vector(0.5D, 1.75D, 0.5D));

        // Calculate the center of the defensive block as the firing origin
        Vector defenseLoc = block.getLocation().toVector().add(new Vector(0.5D, 0.5D, 0.5D));

        // Calculate the normalized direction vector from defense to player
        Vector direction = playerLoc.subtract(defenseLoc).normalize();

        // Determine which direction the dispenser is facing
        BlockFace blockFace = BlockFace.UP;
        if (block.getType().equals(Material.DISPENSER)) {
            BlockData blockData = block.getBlockData();
            if (blockData instanceof Directional directional) {
                blockFace = directional.getFacing();
            }
        }

        // Check if there's a block directly in front of the dispenser
        Block inFront = block.getRelative(blockFace);
        if (!inFront.isEmpty()) {
            return; // Can't fire through a solid block
        }

        // Calculate the spawn position for the projectile on the face of the block
        // Start at center (0.5, 0.4, 0.5) and adjust based on which face is firing
        Vector face = new Vector(0.5D, 0.4D, 0.5D);
        final double diff = 0.6D; // Offset from block center to face edge
        boolean shoot = false;

        // Verify the target is within the dispenser's firing arc
        // Each face can only shoot in its direction (dispensers have limited arc)
        switch (blockFace) {
        case DOWN:
            // Fires downward - only shoot if player is below (negative Y direction)
            if (direction.getY() < 0) {
                shoot = true;
            }
            face.add(new Vector(0, 0.1 - diff, 0));
            break;
        case EAST:
            // Fires east - only shoot if player is to the east (positive X direction)
            if (direction.getX() > 0) {
                shoot = true;
            }
            face.add(new Vector(diff, 0, 0));
            break;
        case NORTH:
            // Fires north - only shoot if player is to the north (negative Z direction)
            if (direction.getZ() < 0) {
                shoot = true;
            }
            face.add(new Vector(0, 0, -diff));
            break;
        case SOUTH:
            // Fires south - only shoot if player is to the south (positive Z direction)
            if (direction.getZ() > 0) {
                shoot = true;
            }
            face.add(new Vector(0, 0, diff));
            break;
        case UP:
            // Fires upward - only shoot if player is above (positive Y direction)
            if (direction.getY() > 0) {
                shoot = true;
            }
            face.add(new Vector(0, diff + 0.1, 0));
            break;
        case WEST:
            // Fires west - only shoot if player is to the west (negative X direction)
            if (direction.getX() < 0) {
                shoot = true;
            }
            face.add(new Vector(-diff, 0, 0));
            break;
        default:
            break;
        }

        if (!shoot) {
            // Player is not within the dispenser's firing arc
            return;
        }

        // Perform line-of-sight check to ensure no obstructions
        // Iterate through blocks from defense to target
        BlockIterator iterator = new BlockIterator(target.getWorld(),
                defenseLoc.add(direction).add(face), direction, 0, RANGE);

        while (iterator.hasNext()) {
            Block item = iterator.next();

            // Check if we've reached the target block
            if (item.getX() == target.getBlockX()
                    && item.getY() == target.getBlockY()
                    && item.getZ() == target.getBlockZ()) {
                break; // Clear line of sight confirmed
            }

            // Check for obstructions (solid blocks or liquids block line of sight)
            if (!item.getType().equals(Material.AIR) && !item.isLiquid()) {
                return; // Obstruction found - can't see the target
            }
        }

        // Target is visible! Calculate firing position
        Location from = block.getLocation().add(face);

        // Apply lead targeting: aim where the player is moving, not where they are
        // The 'aim' vector represents the player's velocity/movement direction

        // Fire projectile from dispenser inventory
        if (block.getType().equals(Material.DISPENSER)) {
            Projectile projectile = null;
            org.bukkit.block.Dispenser ih = (org.bukkit.block.Dispenser)block.getState();

            // Check dispenser inventory for different projectile types (in priority order)
            if (ih.getInventory().contains(Material.ARROW)) {
                // Regular arrow - knockback is handled through arrow mechanics
                projectile = block.getWorld().spawnArrow(from, direction.add(aim), 1F, 10F);

            } else if (ih.getInventory().contains(Material.TIPPED_ARROW)) {
                // Tipped arrow with potion effects
                projectile = block.getWorld().spawnArrow(from, direction.add(aim), 1F, 10F, Arrow.class);

                // Copy the potion effects from the arrow item to the fired arrow
                ItemStack item = ih.getInventory().getItem(ih.getInventory().first(Material.TIPPED_ARROW));
                if (item != null && item.getItemMeta() instanceof PotionMeta meta) {
                    // Copy potion type using modern API
                    if (meta.getBasePotionType() != null) {
                        ((Arrow)projectile).setBasePotionType(meta.getBasePotionType());
                    }
                    // Copy custom potion effects as well
                    if (meta.hasCustomEffects()) {
                        for (var effect : meta.getCustomEffects()) {
                            ((Arrow)projectile).addCustomEffect(effect, true);
                        }
                    }
                }

            } else if (ih.getInventory().contains(Material.SPECTRAL_ARROW)) {
                // Spectral arrow (causes glowing effect) - knockback is handled through arrow mechanics
                projectile = block.getWorld().spawnArrow(from, direction.add(aim), 1F, 10F, SpectralArrow.class);

            } else if (ih.getInventory().contains(Material.FIRE_CHARGE)) {
                // Fireball (explosive projectile)
                projectile = (Projectile)block.getWorld().spawnEntity(from, EntityType.FIREBALL);
                ((Fireball)projectile).setDirection(direction.add(aim));

            } else {
                return; // No valid ammunition in dispenser
            }

            // Track this projectile for team-based damage rules
            projectiles.put(projectile.getUniqueId(), team);
        }
    }

    /**
     * Detects when players move within range of beacon defenses while riding vehicles.
     * <p>
     * This handler complements {@link #onPlayerMove(PlayerMoveEvent)} by detecting movement
     * when players are in vehicles (boats, minecarts, horses, etc.). Without this handler,
     * players could approach beacons in vehicles without triggering defenses.
     * <p>
     * The handler:
     * <ol>
     *   <li>Filters out head-only movements (same as walking detection)</li>
     *   <li>Validates the vehicle has a passenger and it's a player</li>
     *   <li>Delegates to {@link #fireOnPlayer(Player, Location, Location)} for defense logic</li>
     * </ol>
     *
     * Note: Like player movement, this does NOT detect teleportation of vehicles.
     *
     * @param event The VehicleMoveEvent containing vehicle movement information
     * @see #onPlayerMove(PlayerMoveEvent) for walking-based movement detection
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(VehicleMoveEvent event) {
        // Filter out head-only movements (vehicle orientation changes without position change)
        // We only care about actual position changes in X, Y, or Z coordinates
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // Quick world check - only process events in the Beaconz world
        World world = event.getTo().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Verify the vehicle has passengers
        if (event.getVehicle().getPassengers().isEmpty()) {
            return;
        }

        // Verify the first passenger is a player (not another entity)
        if (!(event.getVehicle().getPassengers().getFirst() instanceof Player player)) {
            return;
        }

        // Delegate to the main defense firing logic
        fireOnPlayer(player, event.getFrom(), event.getTo());
    }

    /**
     * Main defense activation logic that determines if and how to fire at a moving player.
     * <p>
     * This method is called by both {@link #onPlayerMove(PlayerMoveEvent)} and
     * {@link #onVehicleMove(VehicleMoveEvent)} whenever a player changes position.
     * It handles the complex logic of:
     * <ul>
     *   <li>Filtering out lobby players (defenses don't fire in the lobby)</li>
     *   <li>Verifying player team membership</li>
     *   <li>Finding nearby beacons within firing range</li>
     *   <li>Identifying enemy-owned beacons</li>
     *   <li>Iterating through defensive blocks on those beacons</li>
     *   <li>Cleaning up destroyed defense blocks</li>
     *   <li>Checking dispenser ammunition</li>
     *   <li>Calculating movement vectors for lead targeting</li>
     * </ul>
     *
     * The method performs defense maintenance by removing AIR blocks from the defense
     * registry (handles cases where defenses were destroyed by creative mode, gravity, etc.).
     * <p>
     * For each enemy beacon within range:
     * <ol>
     *   <li>Iterate through all registered defense blocks</li>
     *   <li>Remove any blocks that have turned to AIR (destroyed defenses)</li>
     *   <li>For dispensers with valid ammunition, calculate movement vector and fire</li>
     * </ol>
     *
     * @param player The player who is moving
     * @param from The location the player moved from
     * @param to The location the player moved to
     */
    private void fireOnPlayer(Player player, Location from, Location to) {
        // Skip defense activation for players in the lobby area
        // The lobby is a safe zone where combat mechanics don't apply
        if (getGameMgr().isPlayerInLobby(player)) {
            return;
        }

        // Verify the player is assigned to a team
        // Players without team assignment can't trigger defenses
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            return;
        }

        // Find all beacons within firing range of the player's destination
        // RANGE is typically 10 blocks - the maximum detection distance for defenses
        for (BeaconObj beacon : getRegister().getNearbyBeacons(to, RANGE)) {
            // Only activate defenses on enemy-owned beacons
            // Beacons without ownership or owned by the player's team won't fire
            if (beacon.getOwnership() != null && !beacon.getOwnership().equals(team)) {
                // This is an enemy beacon - check its defensive blocks

                // Use an iterator to safely remove destroyed defenses during iteration
                Iterator<Entry<Block, DefenseBlock>> it = beacon.getDefenseBlocks().entrySet().iterator();
                while (it.hasNext()) {
                    Entry<Block, DefenseBlock> block = it.next();

                    // Handle different defense block types
                    switch (block.getKey().getType()) {
                    case AIR:
                        // Defense block has been destroyed (creative mode deletion, gravity, etc.)
                        // Remove it from the registry to keep data clean
                        it.remove();
                        break;

                    case DISPENSER:
                        // Active dispenser defense - check if it has ammunition
                        InventoryHolder ih = (InventoryHolder)block.getKey().getState();

                        // Check for any valid projectile types in the dispenser
                        if (ih.getInventory().contains(Material.ARROW)
                                || ih.getInventory().contains(Material.TIPPED_ARROW)
                                || ih.getInventory().contains(Material.SPECTRAL_ARROW)
                                || ih.getInventory().contains(Material.FIRE_CHARGE)) {

                            // Calculate the player's movement vector for lead targeting
                            // This allows the dispenser to aim where the player is going, not where they are
                            Vector adjust = (to.toVector().subtract(from.toVector()));

                            // Fire the projectile with lead targeting
                            fireProjectile(block.getKey(), to, adjust, beacon.getOwnership());
                        }
                        // If dispenser is empty, no action taken (could be refilled later)
                        break;

                    default:
                        // Other block types are not active defenses (yet)
                        // Future expansion could add more defense types here
                    }
                }
            }
        }

    }
}
