/*
 * Copyright (c) 2016 tastybento
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

package com.wasteofplastic.beaconz;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.beaconz.listeners.BeaconLinkListener;

/**
 * Enables inventory switching between games. Handles food, experience and spawn points.
 * @author tastybento
 *
 */
public class BeaconzStore extends BeaconzPluginDependent {
    private YamlConfiguration ymlIndex;
    private File invFile;
    private static final boolean DEBUG = false;

    public BeaconzStore(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        ymlIndex = new YamlConfiguration();
        invFile = new File(beaconzPlugin.getDataFolder(),"game_inv.yml");
        try {
            if (!invFile.exists()) {
                ymlIndex.save(invFile);
            }
            ymlIndex.load(invFile);
        } catch (Exception e) {
            getLogger().severe("Cannot save or load game_inv.yml!");
        }
    }

    /**
     * Saves the location of all the chests to a file
     */
    public void saveInventories() {
        try {
            ymlIndex.save(invFile);
        } catch (IOException e) {
            getLogger().severe("Problem saving game inventories file!");
        }
    }

    /**
     * Gets items for world. Changes the inventory of player immediately.
     * @param player
     * @param gameName
     * @return last location of the player in the game or null if there is none
     */
    public Location getInventory(Player player, String gameName) {
        // Get inventory
        List<?> items = ymlIndex.getList(gameName + "." + player.getUniqueId().toString() + ".inventory");
        if (items != null) player.getInventory().setContents(items.toArray(new ItemStack[items.size()]));
        double health = ymlIndex.getDouble(gameName + "." + player.getUniqueId().toString() + ".health", 20D);
        if (health > 20D) {
            health = 20D;
        }
        if (health < 0D) {
            health = 0D;
        }
        player.setHealth(health);
        int food = ymlIndex.getInt(gameName + "." + player.getUniqueId().toString() + ".food", 20); 
        if (food > 20) {
            food = 20;
        }
        if (food < 0) {
            food = 0;
        }
        player.setFoodLevel(food);
        BeaconLinkListener.setTotalExperience(player, ymlIndex.getInt(gameName + "." + player.getUniqueId().toString() + ".exp", 0));
        // Get Spawn Point
        return (Location)(ymlIndex.get(gameName + "." + player.getUniqueId().toString() + ".location"));
    }

    /**
     * Puts the player's inventory into the right chest
     * @param player
     * @param gameName - the game name for the inventory being store
     * @param from - the last position of the player in this game
     */
    public void storeInventory(Player player, String gameName, Location from) {
        storeInventory(player, gameName, from, true);
    }

    /**
     * Puts the player's inventory into the right chest
     * @param player
     * @param gameName
     * @param from
     * @param storeInv - whether the inventory should be stored or not
     */
    public void storeInventory(Player player, String gameName, Location from, boolean storeInv) {
        if (DEBUG)
            getLogger().info("DEBUG: storeInventory for " + player.getName() + " leaving " + gameName + " from " + from);
        // Copy the player's items to the chest
        List<ItemStack> contents = Arrays.asList(player.getInventory().getContents());  
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".inventory", contents);
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".health", player.getHealth());
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".food", player.getFoodLevel());
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".exp", BeaconLinkListener.getTotalExperience(player));
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".location", player.getLocation());
        saveInventories();
        // Clear the player's inventory
        player.getInventory().clear();
        BeaconLinkListener.setTotalExperience(player, 0);
        // Done!
        if (DEBUG)
            getLogger().info("DEBUG: Done!");
    }

    /**
     * Marks all the chests related to a particular game as empty. Chests are not actually emptied until they are reused.
     * @param gameName
     */
    public void removeGame(String gameName) {
        ymlIndex.set(gameName, null);
        saveInventories();
    }

    /**
     * Clears all the items for player in game name, sets the respawn point
     * @param player
     * @param name
     * @param spawnPoint
     */
    public void clearItems(Player player, String gameName, Location from) {
        getLogger().info("DEBUG: clear inventory for " + player.getName() + " in " + gameName);
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".inventory", null);
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".location", from);
        saveInventories();
    }

    /**
     * Sets the player's food level in game
     * @param player
     * @param gameName
     * @param foodLevel
     */
    public void setFood(Player player, String gameName, int foodLevel) {
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".food", foodLevel);
        saveInventories();
    }
    
    /**
     * Sets player's health in game
     * @param player
     * @param gameName
     * @param maxHealth
     */
    public void setHealth(Player player, String gameName, double maxHealth) {
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".health", maxHealth);
        saveInventories();
    }

    /**
     * Sets player's exp in game
     * @param player
     * @param gameName
     * @param newExp
     */
    public void setExp(Player player, String gameName, int newExp) {
        ymlIndex.set(gameName + "." + player.getUniqueId().toString() + ".exp", newExp);
        saveInventories();
    }
}
