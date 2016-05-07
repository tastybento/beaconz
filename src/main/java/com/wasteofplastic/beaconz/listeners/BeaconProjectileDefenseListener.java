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
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.Dispenser;
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
        //getLogger().info("DEBUG: entity damage by entity event");
        // getLogger().info("DEBUG: entity = " + event.getEntityType());
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();
        if (damager != null && (damager instanceof Projectile) && projectiles.containsKey(damager.getUniqueId())) {
            // Get team that fired the projectile
            Team team = projectiles.get(damager.getUniqueId());
            // Remove the projectile from the hashmap
            projectiles.remove(damager.getUniqueId());
            // Only damage players or mobs
            if (!(entity instanceof Player)) {
                //getLogger().info("DEBUG: prevented damage to non-player entity " + entity.getType());
                if (!(entity instanceof Monster)) {
                    event.setCancelled(true);
                }
                return;
            }
            Player player = (Player)entity;

            // Only damage opposing team members
            Team playersTeam = getGameMgr().getPlayerTeam(player);
            if (team == null) {
                //getLogger().info("DEBUG: prevented damage to non-player");
                event.setCancelled(true);
                return;
            }
            if (playersTeam.equals(team)) {
                //getLogger().info("DEBUG: prevented damage to friendly team member");
                event.setCancelled(true);
                return;
            }
            // Else it's fine to hurt!
            //getLogger().info("DEBUG: die!");
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
        // Only proceed if there's been a move, not just a head move
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        World world = event.getTo().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        Player player = event.getPlayer();
        // Nothing from here on applies to Lobby...
        if (getGameMgr().isPlayerInLobby(player)) {
            return;
        }
        // Check if player is in a team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            return;
        }
        Location to = event.getTo();
        for (BeaconObj beacon : getRegister().getNearbyBeacons(to, RANGE)) {
            // Only deal with enemy-owned beacons
            if (beacon.getOwnership() != null && !beacon.getOwnership().equals(team)) {
                // Offensive beacon
                //getLogger().info("DEBUG: enemy beacon");
                // TODO: check if beacon has active defenses
                for (Entry<Block, Integer> defense : beacon.getDefenseBlocks().entrySet()) {
                    // Do different things depending on the type
                    Block block = defense.getKey();
                    //getLogger().info("DEBUG: defense block = " + block.getType());
                    switch (block.getType()) {
                    case AIR:
                        // Remove defense
                        beacon.removeDefenseBlock(block);
                        //getLogger().info("DEBUG: removed");
                        break;
                    case DISPENSER:
                        InventoryHolder ih = (InventoryHolder)block.getState();
                        if (ih.getInventory().contains(Material.ARROW) || ih.getInventory().contains(Material.TIPPED_ARROW)
                                || ih.getInventory().contains(Material.SPECTRAL_ARROW) || ih.getInventory().contains(Material.FIREBALL)) {
                            //getLogger().info("DEBUG: contains arrow");
                            Vector adjust = (event.getTo().toVector().subtract(event.getFrom().toVector()));
                            fireProjectile(block, defense.getValue(), event.getTo(), adjust, beacon.getOwnership());
                            //getLogger().info("DEBUG: velocity = " + adjust);
                        }
                        //getLogger().info("DEBUG: dispenser");
                        break;
                    default:
                    }
                }
            }
        }
    }

    private void fireProjectile(Block block, Integer value, Location target, Vector adjust, Team team) {
        // Check line of sight
        Vector playerLoc = target.toVector().add(new Vector(0.5D,1.75D,0.5D));
        // Get start location
        Vector defenseLoc = block.getLocation().toVector().add(new Vector(0.5D,0.5D,0.5D));
        //getLogger().info("DEBUG: DefenseLoc = " + defenseLoc);
        // Get the direction to fire the projectile
        Vector direction = playerLoc.subtract(defenseLoc).normalize();
        //getLogger().info("DEBUG: Direction = " + direction);
        // Get the direction the dispenser is facing
        BlockFace blockFace = BlockFace.UP;
        if (block.getType().equals(Material.DISPENSER)) {
            blockFace = ((Dispenser)block.getState().getData()).getFacing();
        }
        //getLogger().info("DEBUG: dispenser is facing " + blockFace);
        Block inFront = block.getRelative(blockFace);
        if (!inFront.isEmpty()) {
            return;
        }
        // Convert blockface to a location on the block
        Vector face = new Vector(0.5D,0.4D,0.5D);
        final double diff = 0.6D;
        boolean shoot = false;
        switch (blockFace) {
        case DOWN:
            // Negative Y
            if (direction.getY() < 0) {
                shoot = true;
            }
            face.add(new Vector(0, 0.1 - diff,0));
            break;
        case EAST:
            // Postive X
            // If X goes negative don't shoot
            if (direction.getX() > 0) {
                shoot = true;
            }
            face.add(new Vector(diff,0,0));
            break;
        case NORTH:
            // Negative Z
            // If Z goes positive then don't shoot
            if (direction.getZ() < 0) {
                shoot = true;
            }
            face.add(new Vector(0,0,-diff));
            break;
        case SOUTH:
            // Positive Z
            // If Z goes negative don't shoot
            if (direction.getZ() > 0) {
                shoot = true;
            }
            face.add(new Vector(0,0,diff));
            break;
        case UP:
            // Postive Y
            if (direction.getY() > 0) {
                shoot = true;
            }
            face.add(new Vector(0,diff + 0.1 ,0));
            break;
        case WEST:
            // Negative X
            // If X goes positive don't shoot
            if (direction.getX() < 0) {
                shoot = true;
            }
            face.add(new Vector(-diff,0,0));
            break;
        default:
            break;
        }
        if (!shoot) {
            //getLogger().info("DEBUG: player is not in view of the dispenser aim arc");
            return;
        }
        // Check to see if the player is visible
        BlockIterator iterator = new BlockIterator(target.getWorld(), defenseLoc.add(direction).add(face), direction, 0, RANGE);
        //getLogger().info("DEBUG: player's vector = " + target.toVector());
        while (iterator.hasNext()) {
            Block item = iterator.next();
            if (item.getX() == target.getBlockX() && item.getY() == target.getBlockY() && item.getZ() == target.getBlockZ()) {
                //if (item.getLocation().toVector().equals(player.getLocation().toVector())) {
                //getLogger().info("DEBUG: Saw you directly!");
                break;
            }
            /*
            if (item.getX() == player.getLocation().getBlockX() && item.getY() == player.getLocation().getBlockY() && item.getZ() == player.getLocation().getBlockZ()) {
                getLogger().info("DEBUG: Saw you directly!");
                break;
            }*/
            //getLogger().info("DEBUG: Block is " + item.getType() + " " + item.getLocation().toVector());
            if (!item.getType().equals(Material.AIR) && !item.isLiquid()) {
                //getLogger().info("DEBUG: Blocking is " + item.getType() + " " + item.getLocation().toVector());
                //getLogger().info("DEBUG: Cannot see you!");
                return;
            }
        }
        //getLogger().info("DEBUG: Saw you!");
        //Fireball projectile = (Fireball)world.spawnEntity(beacon.getLocation().add(direction).add(new Vector(0,2,0)), EntityType.FIREBALL);
        //Projectile projectile = (Projectile)world.spawnEntity(beacon.getLocation().add(direction).add(new Vector(0,2,0)), EntityType.ARROW);
        Location from = block.getLocation().add(face);
        // Change direction to fire where the player is moving to, not where they are
        // Get all the projectiles in the dispenser
        if (block.getType().equals(Material.DISPENSER)) {
            //getLogger().info("DEBUG: Checking dispenser for arrows");
            Projectile projectile = null;
            org.bukkit.block.Dispenser ih = (org.bukkit.block.Dispenser)block.getState();
            if (ih.getInventory().contains(Material.ARROW)) {
                //getLogger().info("DEBUG: regular arrow");
                projectile = block.getWorld().spawnArrow(from, direction.add(adjust), 1F, 10F);
                ((Arrow)projectile).setKnockbackStrength(1);
            } else if (ih.getInventory().contains(Material.TIPPED_ARROW)) {
                //getLogger().info("DEBUG: tipped arrow");
                projectile = block.getWorld().spawnArrow(from, direction.add(adjust), 1F, 10F, TippedArrow.class);
                ItemStack item = ih.getInventory().getItem(ih.getInventory().first(Material.TIPPED_ARROW));
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                ((TippedArrow)projectile).setBasePotionData(meta.getBasePotionData());
            } else if (ih.getInventory().contains(Material.SPECTRAL_ARROW)) {
                //getLogger().info("DEBUG: spectral arrow");
                projectile = block.getWorld().spawnArrow(from, direction.add(adjust), 1F, 10F, SpectralArrow.class);
                ((SpectralArrow)projectile).setKnockbackStrength(1);
            } else if (ih.getInventory().contains(Material.FIREBALL)) {
                //getLogger().info("DEBUG: fireball");
                projectile = (Projectile)block.getWorld().spawnEntity(from, EntityType.FIREBALL);
                ((Fireball)projectile).setDirection(direction.add(adjust));
            } else {
                return;
            }
            projectiles.put(projectile.getUniqueId(), team);
        }
    }
}
