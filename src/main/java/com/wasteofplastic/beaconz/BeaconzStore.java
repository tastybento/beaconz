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
    private final YamlConfiguration ymlIndex;
    private final File invFile;
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
     * Saves to a file
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
        List<?> items = ymlIndex.getList(gameName + "." + player.getUniqueId() + ".inventory");
        if (items != null) player.getInventory().setContents(items.toArray(new ItemStack[0]));
        double health = ymlIndex.getDouble(gameName + "." + player.getUniqueId() + ".health", 20D);
        if (health > 20D) {
            health = 20D;
        }
        if (health <= 0D) {
            health = 1D;
        }
        player.setHealth(health);
        int food = ymlIndex.getInt(gameName + "." + player.getUniqueId() + ".food", 20);
        if (food > 20) {
            food = 20;
        }
        if (food <= 0) {
            food = 1;
        }
        player.setFoodLevel(food);
        BeaconLinkListener.setTotalExperience(player, ymlIndex.getInt(gameName + "." + player.getUniqueId() + ".exp", 0));
        // Get Spawn Point
        return (Location)(ymlIndex.get(gameName + "." + player.getUniqueId() + ".location"));
    }

    /**
     * Store the player's inventory in the game
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
     * @param game
     * @param from
     * @param storeInv - whether the inventory should be stored or not
     */
    public void storeInventory(Player player, String gameName, Location from, boolean storeInv) {
        if (DEBUG)
            getLogger().info("DEBUG: storeInventory for " + player.getName() + " leaving " + gameName + " from " + from);
        // Copy the player's items to the chest
        List<ItemStack> contents = Arrays.asList(player.getInventory().getContents());  
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".inventory", contents);
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".health", player.getHealth());
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".food", player.getFoodLevel());
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".exp", BeaconLinkListener.getTotalExperience(player));
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".location", player.getLocation());
        saveInventories();
        // Clear the player's inventory
        player.getInventory().clear();
        BeaconLinkListener.setTotalExperience(player, 0);
        // Done!
        if (DEBUG)
            getLogger().info("DEBUG: Done!");
    }

    /**
     * Removes all inventories for this game
     * @param gameName
     */
    public void removeGame(String gameName) {
        ymlIndex.set(gameName, null);
        saveInventories();
    }

    /**
     * Clears all the items for player in game name, sets the respawn point
     * @param player - the player
     * @param gameName - the game name
     * @param from - the location to set as respawn point
     */
    public void clearItems(Player player, String gameName, Location from) {
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".inventory", null);
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".location", from);
        saveInventories();
    }

    /**
     * Sets the player's food level in game
     * @param player
     * @param gameName
     * @param foodLevel
     */
    public void setFood(Player player, String gameName, int foodLevel) {
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".food", foodLevel);
        saveInventories();
    }
    
    /**
     * Sets player's health in game
     * @param player
     * @param gameName
     * @param maxHealth
     */
    public void setHealth(Player player, String gameName, double maxHealth) {
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".health", maxHealth);
        saveInventories();
    }

    /**
     * Sets player's exp in game
     * @param player
     * @param gameName
     * @param newExp
     */
    public void setExp(Player player, String gameName, int newExp) {
        ymlIndex.set(gameName + "." + player.getUniqueId() + ".exp", newExp);
        saveInventories();
    }

}
