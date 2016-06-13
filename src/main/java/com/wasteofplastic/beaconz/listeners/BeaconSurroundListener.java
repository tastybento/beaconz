/*
 * Copyright (c) 2015 - 2016 tastybento
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;

/**
 * Handles actions on blocks surrounding a beacon.
 * @author tastybento
 *
 */
public class BeaconSurroundListener extends BeaconzPluginDependent implements Listener {

    private final static boolean DEBUG = false;
    // THis is the range around beacons that blocks become harder to break
    private static final int RANGE = 10;
    // Make breaking blocks 90% harder
    private static final double PROBABILITY = 1D;
    private static final double DAMAGE = 0D;
    private static final Set<Material> protectedMaterials = new HashSet<Material>();
    static {
        protectedMaterials.add(Material.BARRIER);
        protectedMaterials.add(Material.BEACON);
        protectedMaterials.add(Material.BEDROCK);
        protectedMaterials.add(Material.CLAY);
        protectedMaterials.add(Material.COBBLESTONE);
        protectedMaterials.add(Material.DIRT);
        protectedMaterials.add(Material.GRASS);
        protectedMaterials.add(Material.GRASS_PATH);
        protectedMaterials.add(Material.HARD_CLAY);
        protectedMaterials.add(Material.MOSSY_COBBLESTONE);
        protectedMaterials.add(Material.OBSIDIAN);
        protectedMaterials.add(Material.RED_SANDSTONE);
        protectedMaterials.add(Material.SANDSTONE);
        protectedMaterials.add(Material.SOIL);
        protectedMaterials.add(Material.STAINED_CLAY);
        protectedMaterials.add(Material.STONE);
    }

    public BeaconSurroundListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles damage of blocks around a beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Only care about these blocks
        if (!protectedMaterials.contains(event.getBlock().getType())) {
            return;
        }
        // See if this block is near a beacon
        List<BeaconObj> beacons = getRegister().getNearbyBeacons(event.getBlock().getLocation(), RANGE);
        if (beacons.isEmpty()) {
            return;         
        }
        // Check if this block is lower than beacon
        int lowestY = getBeaconzWorld().getMaxHeight();
        for (BeaconObj beacon : beacons) {
            lowestY = Math.min(lowestY, beacon.getY());
        }
        // You're allowed to dig one below the height of the beacon
        if (event.getBlock().getY() >= (lowestY-1) || event.getBlock().getY() < lowestY - RANGE) {
            // You can break below the range okay or above the beacon
            return;
        }
        // Stop damage for a random distribution
        Random rand = new Random();
        if (rand.nextDouble() < PROBABILITY) {
            // Make it harder to break the blocks
            event.setCancelled(true);
            //ParticleEffect.PORTAL.display(0F, 0F, 0F, 1F, 10, event.getBlock().getLocation(), 10D);
            // Reduce durability of tool
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item != null && !item.getType().equals(Material.AIR)) {
                short maxDurability = item.getType().getMaxDurability();
                if (DEBUG)
                    getLogger().info("DEBUG: max durability = " + maxDurability);
                if (maxDurability > 0) {
                    short durability = item.getDurability();
                    if (DEBUG)
                        getLogger().info("DEBUG: durability = " + durability);
                    short damage = (short)((double)maxDurability * DAMAGE);
                    if (DEBUG)
                        getLogger().info("DEBUG: damager = " + damage);
                    durability += damage;                    
                    if (durability >= maxDurability) {
                        event.getPlayer().getInventory().setItemInMainHand(null);
                        event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK, 1F, 1F);

                    } else {
                        event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ITEM_BREAK,2F,2F);
                    }
                    item.setDurability(durability);

                }
            }
        }
    }

    /**
     * Protects the underlying beacon from any damage
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getLocation().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Check if the block is a beacon or the surrounding pyramid and remove it from the damaged blocks
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (protectedMaterials.contains(block.getType())) {
                if (DEBUG)
                    getLogger().info("DEBUG: " + block.getLocation());
                // See if this block is near a beacon
                List<BeaconObj> beacons = getRegister().getNearbyBeacons(block.getLocation(), RANGE);
                if (!beacons.isEmpty()) {
                    // Check if this block is lower than beacon
                    int lowestY = getBeaconzWorld().getMaxHeight();
                    for (BeaconObj beacon : beacons) {
                        lowestY = Math.min(lowestY, beacon.getY());
                    }
                    // Else no effect
                    it.remove();
                }
            }
        }
    }
}

