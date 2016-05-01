package com.wasteofplastic.beaconz;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class used to abstract the index item in the chest. Encodes and decodes the info to be stored.
 *
 * @author tastybento
 *
 */
public class IndexItem {
    private static final boolean DEBUG = false;
    private UUID uuid;
    private String playerName;
    private String gameName;
    private int exp;
    private double health;
    private int foodLevel;
    private Location spawnPoint;
    private ItemStack indexItem;
    private DoubleChest chest;

    /**
     * @param gameName
     * @param exp
     * @param health
     * @param spawnPoint
     * @param player
     */
    public IndexItem(String gameName, int exp, double health, int foodLevel,
            Location spawnPoint, Player player) {
        this.gameName = gameName;
        this.exp = exp;
        this.health = health;
        this.foodLevel = foodLevel;
        this.spawnPoint = spawnPoint;
        this.uuid = player.getUniqueId();
        this.playerName = player.getName();
        updateItem();
    }

    public IndexItem(DoubleChest chest) {
        this.chest = chest;
        Inventory chestInv = chest.getInventory();
        // Get the experience
        ItemStack paper = chestInv.getItem(0);
        if (paper != null && paper.getType().equals(Material.PAPER)) {
            if (DEBUG) {
                Bukkit.getLogger().info("DEBUG: index item found");
            }
            ItemMeta itemMeta = paper.getItemMeta();
            List<String> lore = itemMeta.getLore();
            if (lore.size() == 7) {
                // UUID
                this.uuid = UUID.fromString(lore.get(0));
                // Game name
                this.gameName = lore.get(1);
                // Player name
                this.playerName = lore.get(2);
                // Exp
                String[] split = lore.get(3).split(":");
                if (split.length == 2 && NumberUtils.isNumber(split[1])) {
                    this.exp = Integer.valueOf(split[1]);
                    if (DEBUG) {
                        Bukkit.getLogger().info("DEBUG: Setting player's xp to " + exp);
                    }
                }
                // Health
                split = lore.get(4).split(":");
                if (split.length == 2 && NumberUtils.isNumber(split[1])) {
                    health = Double.valueOf(split[1]);
                    if (DEBUG)
                        Bukkit.getLogger().info("DEBUG: Setting player's health to " + health);
                }
                // Food
                split = lore.get(5).split(":");
                if (split.length == 2 && NumberUtils.isNumber(split[1])) {
                    foodLevel = Integer.valueOf(split[1]);
                    if (DEBUG)
                        Bukkit.getLogger().info("DEBUG: Setting player's food to " + foodLevel);
                }
                // Location
                spawnPoint = Beaconz.getLocationString(lore.get(6));
                if (DEBUG)
                    Bukkit.getLogger().info("DEBUG: Player's spawn point for " + gameName + " is " + spawnPoint);
                updateItem();
            } else {
                Bukkit.getLogger().severe("Index item in chest has incorrect writings");
            }
        } else {
            Bukkit.getLogger().severe("Index item in chest is not paper");
        }

    }

    /**
     * Updates the index item with its stats
     */
    private void updateItem() {
        if (this.indexItem == null) {
            this.indexItem = new ItemStack(Material.PAPER);
        }
        ItemMeta meta = indexItem.getItemMeta();
        List<String> lore = new ArrayList<String>();
        lore.add(uuid.toString());
        lore.add(gameName);
        lore.add(playerName);
        lore.add("xp:" + exp);
        lore.add("health:" + health);
        lore.add("food:" + foodLevel);
        lore.add(Beaconz.getStringLocation(spawnPoint));
        meta.setLore(lore);
        indexItem.setItemMeta(meta);
        // If chest is known, then store the item
        if (chest != null) {
            chest.getInventory().setItem(0, indexItem);
        }
    }

    /**
     * @return the exp
     */
    public int getExp() {
        return exp;
    }

    /**
     * @param exp the exp to set
     */
    public void setExp(int exp) {
        this.exp = exp;
        updateItem();
    }

    /**
     * @return the health
     */
    public double getHealth() {
        return health;
    }

    /**
     * @param health the health to set
     */
    public void setHealth(double health) {
        this.health = health;
        updateItem();
    }

    /**
     * @return the foodLevel
     */
    public int getFoodLevel() {
        return foodLevel;
    }

    /**
     * @param foodLevel the foodLevel to set
     */
    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
        updateItem();
    }

    /**
     * @return the spawnPoint
     */
    public Location getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * @param spawnPoint the spawnPoint to set
     */
    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
        updateItem();
    }

    /**
     * @return the playerName
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return the gameName
     */
    public String getGameName() {
        return gameName;
    }

    /**
     * @return the indexItem
     */
    public ItemStack getIndexItem() {
        return indexItem;
    }

}
