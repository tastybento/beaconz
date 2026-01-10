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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Region;
import com.wasteofplastic.beaconz.Settings;
import com.wasteofplastic.beaconz.TriangleField;

import net.kyori.adventure.text.Component;

/**
 * Handles player and vehicle movement within the game world, enforcing territorial boundaries and effects.
 * <p>
 * This listener is responsible for:
 * <ul>
 *   <li>Preventing players from leaving game regions (enforcing invisible barriers)</li>
 *   <li>Applying team-specific potion effects when players enter/exit triangle fields</li>
 *   <li>Managing region transitions (enter/exit callbacks)</li>
 *   <li>Restricting certain actions (leashing, shearing, entity interaction) outside game areas</li>
 *   <li>Tracking and applying potion effects to both players and their vehicles</li>
 *   <li>Teleporting players who escape the valid play area back to the lobby</li>
 * </ul>
 * <p>
 * Triangle field effects scale based on field overlap - the more fields stacked, the stronger the effects.
 * Enemy fields apply debuffs while friendly fields provide buffs, creating strategic territorial gameplay.
 * <p>
 * The listener maintains state for:
 * <ul>
 *   <li>Active potion effects per player (to properly remove them when leaving fields)</li>
 *   <li>Players near barriers (to show barrier particles)</li>
 * </ul>
 *
 * @author tastybento
 */
public class PlayerMovementListener extends BeaconzPluginDependent implements Listener {

    /**
     * Maps player UUIDs to their currently active triangle field effects.
     * Used to track and remove effects when players leave fields.
     */
    private final HashMap<UUID, Collection<PotionEffect>> triangleEffects = new HashMap<>();

    /**
     * Set of player UUIDs who are currently near region barriers.
     * Used to show barrier particles and prevent spam.
     */
    private final Set<UUID> barrierPlayers = new HashSet<>();

    /**
     * Constructs a new PlayerMovementListener.
     *
     * @param plugin the Beaconz plugin instance
     */
    public PlayerMovementListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Prevents players from using leashes on entities outside the game area.
     * <p>
     * This ensures players can only interact with mobs within designated game regions,
     * preventing resource exploitation in the lobby or undefined areas.
     *
     * @param event the leash entity event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLeashUse(final PlayerLeashEntityEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Prevents players from interacting with entities outside the game area.
     * <p>
     * This blocks right-click interactions (feeding, mounting, trading, etc.)
     * on entities in the lobby or other non-game areas.
     *
     * @param event the player interact entity event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerHitEntity(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getRightClicked().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Prevents placing hanging entities (item frames, paintings) outside the game area.
     * <p>
     * This ensures players can only place decorative items within designated
     * game regions, preventing modification of lobby areas.
     *
     * @param event the hanging place event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHangingPlace(final HangingPlaceEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
            }
        }
    }


    /**
     * Prevents shearing entities (sheep, mooshrooms, snow golems) outside the game area.
     * <p>
     * This ensures players can only gather resources from entities within
     * designated game regions.
     *
     * @param event the player shear entity event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onShear(final PlayerShearEntityEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Prevents damaging vehicles (boats, minecarts) outside of the game area.
     * <p>
     * This protects vehicles in lobby areas and prevents players from
     * destroying vehicles outside designated game regions.
     *
     * @param event the vehicle damage event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleDamage(final VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) {
            return;
        }
        if (player.getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getVehicle().getLocation()) == null) {
                event.setCancelled(true);
                player.sendMessage(Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Handles movement inside a vehicle (boat, minecart, horse, etc.).
     * <p>
     * This method:
     * <ul>
     *   <li>Checks if vehicles are in the Beaconz world</li>
     *   <li>Applies slowness effects from triangle fields to non-living vehicles (boats, minecarts)</li>
     *   <li>Processes movement checks for all passengers in multi-passenger vehicles</li>
     * </ul>
     * <p>
     * Living vehicles (horses, pigs) automatically inherit potion effects from their riders,
     * but non-living vehicles need manual velocity adjustments.
     *
     * @param event the vehicle move event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(final VehicleMoveEvent event) {
        // Only process vehicles in the Beaconz world
        if (!event.getVehicle().getWorld().equals(getBeaconzWorld())) {
            return;
        }

        // Check if a player is driving the vehicle
        Entity passenger = event.getVehicle().getPassenger();
        if (passenger instanceof Player player) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // Apply slowness effects to non-living vehicles (boats, minecarts)
            // Living vehicles (horses, etc.) inherit effects automatically
            if ((!(event.getVehicle() instanceof LivingEntity))) {
                for (PotionEffect effect : getTriangleEffects(player.getUniqueId())) {
                    if (effect.getType().equals(PotionEffectType.SLOWNESS)) {
                        // Calculate slowdown based on effect amplifier
                        double delay = effect.getAmplifier();
                        event.getVehicle().setVelocity(event.getVehicle().getVelocity().divide(new Vector(delay,delay,delay)));
                        break;
                    }
                }
            }

            // Check if there are any other passengers in the vehicle
            // (e.g., multiple players in a boat)
            for (Player pl : getBeaconzWorld().getPlayers()) {
                if (!pl.equals(player) && pl.isInsideVehicle() && pl.getVehicle().getEntityId() == event.getVehicle().getEntityId()) {
                    // Process movement checks for each passenger
                    checkMove(pl, event.getVehicle().getWorld(), from, to);
                }
            }
        }
    }

    /**
     * Handles player movement events in the game world.
     * <p>
     * This is the main movement handler that:
     * <ul>
     *   <li>Only processes significant horizontal movement (X/Z coordinate changes)</li>
     *   <li>Filters to only handle the Beaconz world</li>
     *   <li>Delegates to checkMove for actual movement validation</li>
     * </ul>
     * <p>
     * Note: This does NOT detect teleportation - use PlayerTeleportListener for that.
     * Also note: Vertical (Y) only movement is ignored for performance.
     *
     * @param event the player move event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only proceed if there's been a change in X or Z coords (horizontal movement)
        // Ignore vertical-only movement (jumping, falling) for performance
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        World world = event.getTo().getWorld();
        // Only process movement in the Beaconz world
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // Delegate to checkMove and cancel event if needed
        event.setCancelled(checkMove(player, world, from, to));
    }

    /**
     * Validates and processes player movement between locations.
     * <p>
     * This method performs multiple checks:
     * <ol>
     *   <li>Shows barrier particles if player is near region boundary</li>
     *   <li>Prevents players from crossing region boundaries (invisible walls)</li>
     *   <li>Calls region enter/exit handlers when changing regions</li>
     *   <li>Teleports players who escape the play area back to lobby</li>
     *   <li>Removes potion effects when entering lobby</li>
     *   <li>Applies triangle field effects based on current position</li>
     * </ol>
     *
     * @param player the player who is moving
     * @param world the world the player is in
     * @param from the location the player is moving from
     * @param to the location the player is moving to
     * @return true if the movement should be canceled (boundary violation)
     */
    private boolean checkMove(Player player, World world, Location from, Location to) {
        // Determine which game regions the player is in (if any)
        Region regionFrom = getGameMgr().getRegion(from);
        Region regionTo = getGameMgr().getRegion(to);

        // Show barrier particles if player is near the edge of their region
        if (regionFrom != null) {
            regionFrom.showBarrier(player, 20);
        }

        // Prevent players from crossing region boundaries
        // Check if player is trying to leave a region by moving over a region boundary
        if (regionFrom != null && regionFrom != regionTo) {
            // Only prevent crossing if the movement is small (< 2.5 blocks)
            // Larger movements might be legitimate teleports
            if (from.distanceSquared(to) < 6.25) {
                Vector direction = player.getLocation().getDirection();
                barrierPlayers.add(player.getUniqueId());
                // Return true to cancel the event (player stays in place)
                // Note: We don't teleport the player - just block the movement
                player.sendMessage(Lang.errorRegionLimit);
                return true;
            }
        }

        // Process region transitions - call enter/exit handlers

        // Leaving a region
        if (regionFrom != null && regionFrom != regionTo) {
            regionFrom.exit(player);
        }

        // Entering a new region
        if (regionTo != null && regionFrom != regionTo) {
            regionTo.enter(player);
        }

        // Player managed to get outside all defined play areas
        if (regionTo == null && regionFrom == null) {
            // Non-ops get teleported back to lobby
            if (!player.isOp()) {
                player.teleport(getGameMgr().getLobby().getSpawnPoint());
                getLogger().warning(player.getName() + " managed to get outside of the game area and was teleported to the lobby.");
                return true;
            }
        }

        // Clear all potion effects when entering the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            // Remove all active potion effects
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
                // Also remove effects from player's vehicle if mounted
                if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                    le.removePotionEffect(effect.getType());
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return false;
        }

        // Check triangle field positions and apply effects
        // Get list of triangle fields at the "from" location
        List<TriangleField> fromTriangle = getRegister().getTriangle(from.getBlockX(), from.getBlockZ());
        // Get list of triangle fields at the "to" location
        List<TriangleField> toTriangle = getRegister().getTriangle(to.getBlockX(), to.getBlockZ());

        // Apply appropriate potion effects based on triangle field changes
        return applyTriangleEffects(player, fromTriangle, toTriangle);
    }

    /**
     * Applies or removes potion effects based on triangle field transitions.
     * <p>
     * Triangle fields provide team-based buffs and debuffs:
     * <ul>
     *   <li>Friendly fields (owned by player's team) provide beneficial effects</li>
     *   <li>Enemy fields (owned by opposing teams) apply harmful debuffs</li>
     *   <li>Effect strength scales with field overlap (more overlapping fields = stronger effects)</li>
     * </ul>
     * <p>
     * This method handles:
     * <ul>
     *   <li>Removing effects when leaving fields</li>
     *   <li>Notifying players of field level changes</li>
     *   <li>Applying appropriate effects based on team ownership</li>
     *   <li>Denying access to teamless players (non-ops)</li>
     * </ul>
     *
     * @param player the player moving between fields
     * @param fromTriangles list of triangle fields at the previous location (stacked fields)
     * @param toTriangles list of triangle fields at the new location (stacked fields)
     * @return true if the movement should be canceled (teamless player, non-op)
     */
    @SuppressWarnings("deprecation")
    public boolean applyTriangleEffects(Player player,
            List<TriangleField> fromTriangles, List<TriangleField> toTriangles) {
        // Get the player's team (null if not on a team)
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            // Only ops can move without being on a team
            return !player.isOp();
        }

        // Player is outside any triangle field (in open/neutral territory)
        if (fromTriangles.isEmpty() && toTriangles.isEmpty()) {
            // Remove all active potion effects
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
                // Also remove effects from player's vehicle if mounted
                if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                    le.removePotionEffect(effect.getType());
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return false;
        }

        // Player is leaving triangle fields (entering neutral territory)
        if (toTriangles.isEmpty()) {
            // Notify player they're leaving the field
            player.sendMessage(Lang.triangleLeaving.replaceText("[team]", fromTriangles.getFirst().getOwner().displayName()));

            // Remove all triangle field effects that were previously applied
            if (triangleEffects.containsKey(player.getUniqueId())) {
                for (PotionEffect effect : triangleEffects.get(player.getUniqueId())) {
                    player.removePotionEffect(effect.getType());
                    // Also remove from vehicle
                    if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                        le.removePotionEffect(effect.getType());
                    }
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return false;
        }

        // Player is entering a field or moving to a more densely stacked area
        if (fromTriangles.size() < toTriangles.size()) {
            // Notify player they're entering or powering up in the field
            player.sendMessage(
                    Lang.triangleEntering.replaceText("[team]", toTriangles.getFirst().getOwner().displayName())
                    .replaceText("[level]", Component.text(String.valueOf(toTriangles.size()))));
        } else if (toTriangles.size() < fromTriangles.size()) {
            // Player is moving to less densely stacked area (weaker effects)
            // Remove current effects first - weaker effects will be applied below
            if (triangleEffects.containsKey(player.getUniqueId())) {
                for (PotionEffect effect : triangleEffects.get(player.getUniqueId())) {
                    player.removePotionEffect(effect.getType());
                    // Also remove from vehicle
                    if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                        le.removePotionEffect(effect.getType());
                    }
                }
            }
            // Notify player of the level drop
            player.sendMessage(Lang.triangleDroppingToLevel.replaceText("[team]", toTriangles.getFirst().getOwner().displayName())
                    .replaceText("[level]", Component.text(String.valueOf(toTriangles.size()))));
        }

        // Apply the appropriate effects for the new field(s)
        applyEffects(player, toTriangles, team);
        return false;
    }

    /**
     * Applies the actual potion effects to a player based on triangle field ownership.
     * <p>
     * This method determines what effects to apply based on:
     * <ul>
     *   <li>Number of overlapping triangle fields (more fields = stronger effects)</li>
     *   <li>Team ownership (enemy vs friendly)</li>
     *   <li>Configured effect settings from Settings class</li>
     * </ul>
     * <p>
     * Effects are also applied to the player's vehicle if they are mounted.
     * The effects collection is stored in triangleEffects map for later removal.
     *
     * @param player the player to apply effects to
     * @param to list of triangle fields the player is now in
     * @param team the player's team (used to determine friendly vs enemy)
     */
    private void applyEffects(final Player player, final List<TriangleField> to, final Team team) {
        // Remove all effects if no valid field or team data
        if (to == null || to.isEmpty() || team == null) {
            if (triangleEffects.containsKey(player.getUniqueId())) {
                for (PotionEffect effect : triangleEffects.get(player.getUniqueId())) {
                    player.removePotionEffect(effect.getType());                   
                    // Also remove from player's vehicle if mounted
                    if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                        le.removePotionEffect(effect.getType());
                    }
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return;
        }

        // Determine the team that owns this triangle field
        Team triangleOwner = to.getFirst().getOwner();
        Collection<PotionEffect> effects = new ArrayList<>();

        // Apply debuffs if this is an enemy field
        if (triangleOwner != null && !triangleOwner.equals(team)) {
            // Accumulate effects based on field overlap count
            // More overlapping fields = more/stronger effects
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.enemyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.enemyFieldEffects.get(i));
                }
            }
            // Apply all accumulated effects to the player
            player.addPotionEffects(effects);

            // Also apply to player's vehicle if they're riding one
            if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                le.addPotionEffects(effects);
            }
        }

        // Apply buffs if this is a friendly field
        if (triangleOwner != null && triangleOwner.equals(team)) {
            // Accumulate effects based on field overlap count
            // More overlapping fields = more/stronger buffs
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.friendlyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.friendlyFieldEffects.get(i));
                }
            }
            // Apply all accumulated effects to the player
            player.addPotionEffects(effects);

            // Also apply to player's vehicle if they're riding one
            if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                le.addPotionEffects(effects);
            }
        }

        // Store the applied effects for later removal when player leaves the field
        triangleEffects.put(player.getUniqueId(), effects);
    }

    /**
     * Gets the active triangle field effects for a specific player.
     * <p>
     * Used primarily by the vehicle movement handler to apply slowness effects
     * to non-living vehicles (boats, minecarts).
     *
     * @param playerUUID the UUID of the player
     * @return the collection of active potion effects, or an empty list if none
     */
    public Collection<PotionEffect> getTriangleEffects(UUID playerUUID) {
        return triangleEffects.getOrDefault(playerUUID, Collections.emptyList());
    }

    /**
     * Gets the complete map of all players' active triangle field effects.
     * <p>
     * This is mainly used for testing and debugging purposes.
     *
     * @return the map of player UUIDs to their active potion effects
     */
    public HashMap<UUID, Collection<PotionEffect>> getTriangleEffects() {
        return triangleEffects;
    }

}
