/*
 * Copyright (c) 2015 tastybento
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
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;

/**
 * Handles projectile beacon defenses
 * @author tastybento
 *
 */
public class BeaconProjectileDefenseListener extends BeaconzPluginDependent implements Listener {
    private static final int RANGE = 10;
    private HashMap<UUID, Team> projectiles = new HashMap<UUID, Team>();
    /**
     * @param plugin
     */
    public BeaconProjectileDefenseListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Prevents block damage due to explosions from beacon fireballs
     * Will still hurt players, so that needs to be handled elsewhere
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        // Find out what is exploding
        Entity expl = e.getEntity();
        if (expl == null) {
            return;
        }
        // Check world
        if (!e.getEntity().getWorld().equals(getBeaconzWorld())) {
            return;
        }
        if (projectiles.containsKey(expl.getUniqueId())) {
            e.blockList().clear();
            projectiles.remove(expl.getUniqueId());
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    public void onAttackDamage(EntityDamageByEntityEvent event) {
        getLogger().info("DEBUG: entity damage by entity event");
        getLogger().info("DEBUG: entity = " + event.getEntityType());
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();
        if (damager != null && (damager instanceof Projectile) && projectiles.containsKey(damager.getUniqueId())) {
            // Only damage players - nothing else
            if (!(entity instanceof Player)) {
                getLogger().info("DEBUG: prevented damage to non-player entity " + entity.getType());
                event.setCancelled(true);
                return;
            }
            Player player = (Player)entity;
            // Get team that fired the projectile
            Team team = projectiles.get(damager.getUniqueId());
            // Only damage opposing team members
            Team playersTeam = getGameMgr().getPlayerTeam(player);
            if (team == null) {
                getLogger().info("DEBUG: prevented damage to non-player");
                event.setCancelled(true);
                return;
            }
            if (playersTeam.equals(team)) {
                getLogger().info("DEBUG: prevented damage to friendly team member");
                event.setCancelled(true);
                return;
            }
           // Else it's fine to hurt! 
            getLogger().info("DEBUG: die!");
        }
    }
    
    /**
     * Check if player comes within range of a beacon
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
        // Check if player is in a team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            return;
        }
        Location to = event.getTo();
        for (BeaconObj beacon : getRegister().getNearbyBeacons(to, RANGE)) {
            if (beacon.getOwnership() != null && !beacon.getOwnership().equals(team)) {
                // Offensive beacon
                // TODO: check if beacon has active defenses
                // Fire a fireball at the player from the beacon
                // Check line of sight
                Vector playerLoc = player.getLocation().toVector().add(new Vector(0,1,0)); 
                Vector beaconLoc = beacon.getLocation().toVector().add(new Vector(0,2,0));
                Vector direction = playerLoc.subtract(beaconLoc).normalize();
                BlockIterator iterator = new BlockIterator(player.getWorld(), beaconLoc, direction, 0, RANGE);                
                //getLogger().info("DEBUG: player's vector = " + player.getLocation().toVector());
                while (iterator.hasNext()) {
                    Block item = iterator.next();
                    if (item.getX() == player.getLocation().getBlockX() && item.getY() == player.getLocation().getBlockY() && item.getZ() == player.getLocation().getBlockZ()) {
                        //getLogger().info("DEBUG: Saw you directly!");
                        break;
                    }
                    //getLogger().info("DEBUG: Block is " + item.getType() + " " + item.getLocation().toVector());
                    if (!item.getType().equals(Material.AIR) && !item.isLiquid()) {
                        //getLogger().info("DEBUG: Cannot see you!");                        
                        return;
                    }
                } 
                //getLogger().info("DEBUG: Saw you!");
                //Fireball projectile = (Fireball)world.spawnEntity(beacon.getLocation().add(direction).add(new Vector(0,2,0)), EntityType.FIREBALL);
                //Projectile projectile = (Projectile)world.spawnEntity(beacon.getLocation().add(direction).add(new Vector(0,2,0)), EntityType.ARROW);
                Arrow projectile = world.spawnArrow(beacon.getLocation().add(direction).add(new Vector(0,2,0)), direction, 1F, 10F);
                projectile.setKnockbackStrength(1);
                //projectile.setVelocity(direction);
                projectiles.put(projectile.getUniqueId(), beacon.getOwnership());
            }
        }
    }


}
