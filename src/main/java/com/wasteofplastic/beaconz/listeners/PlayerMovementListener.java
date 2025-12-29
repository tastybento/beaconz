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

import org.bukkit.ChatColor;
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

/**
 * Handles movement by the player and game boundary protection
 * @author tastybento
 *
 */
public class PlayerMovementListener extends BeaconzPluginDependent implements Listener {

    private final HashMap<UUID, Collection<PotionEffect>> triangleEffects = new HashMap<>();
    private final Set<UUID> barrierPlayers = new HashSet<>();

    public PlayerMovementListener(Beaconz plugin) {
        super(plugin);
    }

    /*
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInteractBlock(final PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getWorld().equals(getBeaconzWorld())
                && getGameMgr().getGame(event.getClickedBlock().getLocation()) == null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
        }
    }
     */
    /**
     * Prevents use of leashes outside the game area
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLeashUse(final PlayerLeashEntityEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Prevents hitting items outside the game area
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerHitEntity(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getRightClicked().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Prevents hanging items outside the game area
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHangingPlace(final HangingPlaceEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
            }
        }
    }


    /**
     * Prevents shearing outside the game area
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onShear(final PlayerShearEntityEvent event) {
        if (event.getEntity().getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getEntity().getLocation()) == null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Prevents damaging vehicles outside of the game area
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleDamage(final VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) {
            return;
        }
        if (player.getWorld().equals(getBeaconzWorld())) {
            if (getGameMgr().getGame(event.getVehicle().getLocation()) == null) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
            }
        }
    }

    /**
     * Handles movement inside a vehicle (or on a vehicle)
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(final VehicleMoveEvent event) {
        if (!event.getVehicle().getWorld().equals(getBeaconzWorld())) {
            return;
        }
        // Check if a player is in it
        Entity passenger = event.getVehicle().getPassenger();
        if (passenger instanceof Player player) {
            Location from = event.getFrom();
            Location to = event.getTo();
            /*
            if (checkMove(player, event.getVehicle().getWorld(), from, to)) {
                // Vehicle should stop moving
                Vector direction = event.getVehicle().getLocation().getDirection();
                event.getVehicle().teleport(event.getVehicle().getLocation().add(from.toVector().subtract(to.toVector()).normalize()));
                event.getVehicle().getLocation().setDirection(direction);
                event.getVehicle().setVelocity(new Vector(0,0,0));
            }*/
            // Check potion effects for boats etc.
            if ((!(event.getVehicle() instanceof LivingEntity))) {
                for (PotionEffect effect : getPml().getTriangleEffects(player.getUniqueId())) {
                    if (effect.getType().equals(PotionEffectType.SLOWNESS)) {
                        double delay = effect.getAmplifier();
                        event.getVehicle().setVelocity(event.getVehicle().getVelocity().divide(new Vector(delay,delay,delay)));
                        break;
                    }
                }
            }
            // Check if there are any other passengers
            for (Player pl : getBeaconzWorld().getPlayers()) {
                if (!pl.equals(player) && pl.isInsideVehicle() && pl.getVehicle().getEntityId() == event.getVehicle().getEntityId()) {
                    //getLogger().info(pl.getName() + " inside vehicle of id " + pl.getVehicle().getEntityId());
                    //getLogger().info("Event id = " + event.getVehicle().getEntityId());
                    checkMove(pl, event.getVehicle().getWorld(), from, to);
                }
            }
        }
    }

    /**
     * Handle player movement
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Remember that teleporting is not detected as player movement..
        // If we want to catch movement by teleportation, we have to keep track of the players to-from by ourselves
        // Only proceed if there's been a change in X or Z coords
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        World world = event.getTo().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        event.setCancelled(checkMove(player, world, from, to));
    }

    /**
     * Handles checking of player movement.
     * @param player
     * @param world
     * @param from
     * @param to
     * @return true if the event should be canceled
     */
    private boolean checkMove(Player player, World world, Location from, Location to) {
        Region regionFrom = getGameMgr().getRegion(from);
        Region regionTo = getGameMgr().getRegion(to);

        // Check if a player is close to a barrier
        if (regionFrom != null) {
            regionFrom.showBarrier(player, 20);
        }

        // Check if player is trying to leave a region by moving over a region boundary
        // And send him back to whence he came
        if (regionFrom != null && regionFrom != regionTo) {
            if (from.distanceSquared(to) < 6.25) {
                //float pitch = player.getLocation().getPitch();
                //float yaw = player.getLocation().getYaw();
                Vector direction = player.getLocation().getDirection();
                barrierPlayers.add(player.getUniqueId());
                //player.teleport(player.getLocation().add(from.toVector().subtract(to.toVector().multiply(4)).normalize()));
                //player.getLocation().setPitch(pitch);
                //player.getLocation().setPitch(yaw);
                //player.getLocation().setDirection(direction);
                //player.setVelocity(new Vector(0,0,0));
                // There's no need to tp the player anywhere, just return true so onPlayerMove can cancel the event
                // Also, teleporting the player back can cause problems in some situations
                player.sendMessage(ChatColor.YELLOW + Lang.errorRegionLimit);
                return true;
            }
        }

        // Check if player changed regions and process region exit and enter methods

        // Leaving
        if (regionFrom != null && regionFrom != regionTo) {
            regionFrom.exit(player);
        }
        // Entering
        if (regionTo != null && regionFrom != regionTo) {
            regionTo.enter(player);
        }
        // Outside play area
        if (regionTo == null && regionFrom == null) {
            if (!player.isOp()) {
                player.teleport(getGameMgr().getLobby().getSpawnPoint());
                getLogger().warning(player.getName() + " managed to get outside of the game area and was teleported to the lobby.");
                return true;
            }
        }

        // Nothing from here on applies to Lobby...
        if (getGameMgr().isPlayerInLobby(player)) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
                // Check vehicle
                if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                    //getLogger().info("DEBUG: living vehicle remove");
                    le.removePotionEffect(effect.getType());
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return false;
        }
        // Check if a player is standing on an enemy beacon
        /*
        BeaconObj beacon = getRegister().getBeaconAt(to.getBlockX(), to.getBlockZ());
        if (beacon != null && beacon.getOwnership() != null) {
            BeaconProtectionListener.getStandingOn().put(player.getUniqueId(), beacon);
        } else {
            BeaconProtectionListener.getStandingOn().remove(player.getUniqueId());
        }*/
        // Check the From
        List<TriangleField> fromTriangle = getRegister().getTriangle(from.getBlockX(), from.getBlockZ());
        // Check the To
        List<TriangleField> toTriangle = getRegister().getTriangle(to.getBlockX(), to.getBlockZ());
        return applyTriangleEffects(player, fromTriangle, toTriangle);
    }

    /**
     * Correctly affect a player with potions
     * @param player
     * @param fromTriangles - list of triangles player is covered by
     * @param toTriangles - list of new triangles player is covered by
     * @return true if the event should be canceled
     */
    public boolean applyTriangleEffects(Player player,
            List<TriangleField> fromTriangles, List<TriangleField> toTriangles) {
        // Get the player's team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            return !player.isOp();
        }
        // Outside any field
        if (fromTriangles.isEmpty() && toTriangles.isEmpty()) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
                // Check vehicle
                if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                    //getLogger().info("DEBUG: living vehicle remove");
                    le.removePotionEffect(effect.getType());
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return false;
        }
        // Check if to is not a triangle
        if (toTriangles.isEmpty()) {
            // Leaving a control triangle
            player.sendMessage(Lang.triangleLeaving.replace("[team]", fromTriangles.getFirst().getOwner().getDisplayName()));
            if (triangleEffects.containsKey(player.getUniqueId())) {
                for (PotionEffect effect : triangleEffects.get(player.getUniqueId())) {
                    player.removePotionEffect(effect.getType());
                    // Check vehicle
                    if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                        //getLogger().info("DEBUG: living vehicle remove");
                        le.removePotionEffect(effect.getType());
                    }
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return false;
        }
        // Entering a field, or moving to a stronger field
        if (fromTriangles.size() < toTriangles.size()) {
            player.sendMessage((Lang.triangleEntering.replace("[team]", toTriangles.getFirst().getOwner().getDisplayName())).replace("[level]",String.valueOf(toTriangles.size())));
        } else if (toTriangles.size() < fromTriangles.size()) {
            // Remove all current effects - the lower set will be applied below
            if (triangleEffects.containsKey(player.getUniqueId())) {
                for (PotionEffect effect : triangleEffects.get(player.getUniqueId())) {
                    player.removePotionEffect(effect.getType());
                    // Check vehicle
                    if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                        //getLogger().info("DEBUG: living vehicle remove");
                        le.removePotionEffect(effect.getType());
                    }
                }
            }
            player.sendMessage((Lang.triangleDroppingToLevel.replace("[team]", toTriangles.getFirst().getOwner().getDisplayName())).replace("[level]",String.valueOf(toTriangles.size())));
        }

        // Apply triangle effects
        applyEffects(player, toTriangles, team);
        return false;
    }

    /**
     * Applies triangle effects to a player
     * @param player
     * @param to
     * @param team
     */
    private void applyEffects(final Player player, final List<TriangleField> to, final Team team) {
        //getLogger().info("DEBUG: applying effects");
        if (to == null || to.isEmpty() || team == null) {
            if (triangleEffects.containsKey(player.getUniqueId())) {
                for (PotionEffect effect : triangleEffects.get(player.getUniqueId())) {
                    player.removePotionEffect(effect.getType());                   
                    // Check vehicle
                    if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                        //getLogger().info("DEBUG: living vehicle remove");
                        le.removePotionEffect(effect.getType());
                    }
                }
            }
            triangleEffects.remove(player.getUniqueId());
            return;
        }
        // Update the active effects on the player
        // Add bad stuff
        // Enemy team
        Team triangleOwner = to.getFirst().getOwner();
        Collection<PotionEffect> effects = new ArrayList<>();
        if (triangleOwner != null && !triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.enemyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.enemyFieldEffects.get(i));
                }
            }
            player.addPotionEffects(effects);
            // Check vehicle
            if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                //getLogger().info("DEBUG: living vehicle");
                le.addPotionEffects(effects);
            }
        }
        // Friendly triangle
        if (triangleOwner != null && triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.friendlyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.friendlyFieldEffects.get(i));
                }
            }
            player.addPotionEffects(effects);
            // Check vehicle
            if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity le) {
                //getLogger().info("DEBUG: living vehicle");
                le.addPotionEffects(effects);
            }
        }
        triangleEffects.put(player.getUniqueId(), effects);
    }

    /**
     * @return the triangleEffects or an empty list if there is none
     */
    public Collection<PotionEffect> getTriangleEffects(UUID playerUUID) {
        return triangleEffects.getOrDefault(playerUUID, Collections.emptyList());
    }

}
