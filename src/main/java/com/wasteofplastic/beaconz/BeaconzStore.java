package com.wasteofplastic.beaconz;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wasteofplastic.beaconz.listeners.BeaconListeners;

public class BeaconzStore extends BeaconzPluginDependent {
    private World beaconzStoreWorld;
    private HashMap<UUID,HashMap<String,Location>> index = new HashMap<UUID, HashMap<String,Location>>();
    private List<Location> emptyChests = new ArrayList<Location>();
    private int lastX;
    private int lastY;
    private int lastZ;
    private YamlConfiguration ymlIndex;
    private File indexFile;

    public BeaconzStore(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        // Make the world if it doesn't exist already
        beaconzStoreWorld = WorldCreator.name(Settings.worldName + "_store").type(WorldType.FLAT).environment(World.Environment.NORMAL).createWorld();
        beaconzStoreWorld.setDifficulty(Difficulty.PEACEFUL);              
        beaconzStoreWorld.setSpawnFlags(false, false);
        indexFile = new File(beaconzPlugin.getDataFolder(),"inventories.yml");
        // See if there is an index file
        if (indexFile.exists()) {
            loadIndex();
        } else {
            rebuildIndex();
            saveIndex();
        }
    }

    /**
     * Saves the location of all the chests to a file
     */
    public void saveIndex() {
        ymlIndex = new YamlConfiguration();
        try {
            // Save the index
            HashMap<String, Location> locations = new HashMap<String, Location>();
            for (UUID uuid: index.keySet()) {
                locations = index.get(uuid);
                for (String gameName: locations.keySet()) {
                    ymlIndex.set("index." + uuid.toString() + "." + gameName, getStringLocation(locations.get(gameName)));
                }
            }
            // Save the empty chest locations
            List<String> tempList = new ArrayList<String>();
            for (Location loc : emptyChests) {
                tempList.add(getStringLocation(loc));
            }
            ymlIndex.set("emptyChests", tempList);
            // Save file
            ymlIndex.save(indexFile);
        } catch (Exception e) {
            // Something went wrong
            getLogger().severe("Could not save inventory index file!");
            e.printStackTrace();
        }
    }

    /**
     * Loads the location of all the chests from a file
     */
    public void loadIndex() {
        index.clear();
        emptyChests.clear();
        ymlIndex = new YamlConfiguration();
        try {
            ymlIndex.load(indexFile);
            // Parse
            ConfigurationSection players = ymlIndex.getConfigurationSection("index");
            HashMap<String, Location> locations = new HashMap<String, Location>();
            for (String uuid : players.getValues(false).keySet()) {
                UUID playerUUID = UUID.fromString(uuid);
                locations.clear();
                ConfigurationSection chestLocations = players.getConfigurationSection(uuid);
                for (String gameName : chestLocations.getValues(false).keySet()) {
                    locations.put(gameName, getLocationString(chestLocations.getString(gameName)));
                }
                index.put(playerUUID, locations);
            }
            // Get empty chests
            List<String> tempList = ymlIndex.getStringList("emptyChests");
            for (String loc : tempList) {
                emptyChests.add(getLocationString(loc)); 
            }
        } catch (Exception e) {
            // Something went wrong
            getLogger().severe("Could not load inventory index file, rebuilding");
            rebuildIndex();
            saveIndex();
        } 
    }

    /**
     * Rebuilds the index by going through chests in the world
     */
    public void rebuildIndex() {
        getLogger().info("DEBUG: Rebuilding index");
        index.clear();
        emptyChests.clear();
        // Load the store
        Block chestBlock = beaconzStoreWorld.getBlockAt(0, 4, 0);
        lastX = 0;
        lastY = 4;
        lastZ = 0;
        while (chestBlock.getType().equals(Material.CHEST)) {
            Chest chest = (Chest)chestBlock.getState();
            InventoryHolder ih = chest.getInventory().getHolder();
            if (ih instanceof DoubleChest){
                DoubleChest dc = (DoubleChest) ih;
                ItemStack indexItem = dc.getInventory().getItem(0);
                if (indexItem != null) {
                    ItemMeta meta = indexItem.getItemMeta();
                    List<String> info = meta.getLore();
                    // UUID, gameName, name
                    if (!info.isEmpty()) {
                        UUID uuid = UUID.fromString(info.get(0));
                        HashMap<String, Location> temp = new HashMap<String, Location>();
                        if (index.containsKey(uuid)) {
                            temp = index.get(uuid);              
                        }
                        temp.put(info.get(1), chestBlock.getLocation());
                        index.put(uuid, temp);
                    }
                } else {
                    // Make a note of these for use later
                    emptyChests.add(chestBlock.getLocation());
                }
            }
            // Get next block
            lastY++;
            if (lastY == beaconzStoreWorld.getMaxHeight()) {
                lastY = 4;
                lastX++;
            }
            chestBlock = beaconzStoreWorld.getBlockAt(lastX, lastY, lastZ);
        }
        // After this, lastX, Y, Z should point to the next free spot
        getLogger().info("DEBUG: last = " + lastX + "," + lastY + "," + lastZ);
    }

    /**
     * Gets items from a storage chest in the storage world
     * @param player
     */
    public void getInventory(Player player, String gameName) {
        getLogger().info("DEBUG: getInventory for " + player.getName() + " going to " + gameName);
        // Clear the inventory
        getLogger().info("DEBUG: clearing player's inventory - I hope it's saved somewhere!");
        player.getInventory().clear();
        if (index.containsKey(player.getUniqueId())) {
            getLogger().info("DEBUG: player has chest(s)");
            if (index.get(player.getUniqueId()).containsKey(gameName)) {
                getLogger().info("DEBUG: chest is for this game " + gameName);
                // They have a chest for this game, so give them the contents
                Block chestBlock = beaconzStoreWorld.getBlockAt(index.get(player.getUniqueId()).get(gameName));
                Chest chest = (Chest)chestBlock.getState();
                InventoryHolder ih = chest.getInventory().getHolder();
                if (ih instanceof DoubleChest) {
                    Inventory chestInv = ((DoubleChest) ih).getInventory();
                    // Get the experience
                    ItemStack paper = chestInv.getItem(0);
                    if (paper != null) {
                        getLogger().info("DEBUG: index item found");
                        ItemMeta itemMeta = paper.getItemMeta();
                        List<String> lore = itemMeta.getLore();
                        if (lore.size()>3) {
                            String[] split = lore.get(3).split(":");
                            if (split.length == 2 && NumberUtils.isNumber(split[1])) {
                                int xp = Integer.valueOf(split[1]);
                                getLogger().info("DEBUG: Setting player's xp to " + xp);
                                BeaconListeners.setTotalExperience(player, xp);
                            }
                        }
                    }
                    for (int i = 1; i < chestInv.getSize(); i++) {
                        // Give to player
                        player.getInventory().setItem(i-1, chestInv.getItem(i));
                        // Remove from chest
                        chestInv.setItem(i, null);
                    }                    
                }
            } else {
                getLogger().info("DEBUG: chest is not for game " + gameName);
            }
        } else {
            getLogger().info("DEBUG: player does not have any chests");
        }
    }

    /**
     * Puts the player's inventory into the right chest
     * @param player
     */
    public void storeInventory(Player player, String gameName) {
        getLogger().info("DEBUG: storeInventory for " + player.getName() + " going to " + gameName);
        Block chestBlock = beaconzStoreWorld.getBlockAt(lastX, lastY, lastZ);
        // Find out if they have a chest already
        if (!index.containsKey(player.getUniqueId())) {
            getLogger().info("DEBUG: player has no chest");
            // Make a chest            
            // Check if there are any free chests
            if (!emptyChests.isEmpty()) {
                getLogger().info("DEBUG: there is an empty chest available");
                chestBlock = emptyChests.get(0).getBlock();
                emptyChests.remove(0);
            } else {
                // There is no chest, so make one 
                getLogger().info("DEBUG: Making chest");
                chestBlock.setType(Material.CHEST);
                chestBlock.getRelative(0, 0, 1).setType(Material.CHEST);
                lastY++;
                if (lastY == beaconzStoreWorld.getMaxHeight()) {
                    lastY = 4;
                    lastX++;
                }
                getLogger().info("DEBUG: last = " + lastX + "," + lastY + "," + lastZ);
            }
            // Store in index
            HashMap<String,Location> placeHolder = new HashMap<String,Location>();
            placeHolder.put(gameName,chestBlock.getLocation());
            index.put(player.getUniqueId(), placeHolder);
        } else {
            getLogger().info("DEBUG: player has chest");
            if (index.get(player.getUniqueId()).containsKey(gameName)) {
                getLogger().info("DEBUG: player has chest for " + gameName);
                // There is a chest already! So use it
                chestBlock = index.get(player.getUniqueId()).get(gameName).getBlock();
                if (!chestBlock.getType().equals(Material.CHEST)) {
                    getLogger().severe("Chest at " + index.get(player.getUniqueId()).get(gameName) + " is not there anymore!");
                }
            } else {
                getLogger().info("DEBUG: Player does not have chest for " + gameName);
                // Check if there are any free chests
                if (!emptyChests.isEmpty()) {
                    getLogger().info("DEBUG: there is an empty chest available");
                    chestBlock = emptyChests.get(0).getBlock();
                    emptyChests.remove(0);
                } else {
                    // There is no chest, so make one 
                    getLogger().info("DEBUG: Making chest");
                    chestBlock.setType(Material.CHEST);
                    chestBlock.getRelative(0, 0, 1).setType(Material.CHEST);
                    lastY++;
                    if (lastY == beaconzStoreWorld.getMaxHeight()) {
                        lastY = 0;
                        lastX++;
                    }
                    getLogger().info("DEBUG: last = " + lastX + "," + lastY + "," + lastZ);
                }
                HashMap<String,Location> placeHolder = index.get(player.getUniqueId());
                placeHolder.put(gameName,chestBlock.getLocation());
                index.put(player.getUniqueId(), placeHolder);
            }
        }
        // Actually store the items in the chest
        Chest chest = (Chest)chestBlock.getState();
        InventoryHolder ih = chest.getInventory().getHolder();
        if (ih instanceof DoubleChest){
            Inventory chestInv = ((DoubleChest) ih).getInventory();
            // Clear any current inventory
            chestInv.clear();
            // Create the index item
            getLogger().info("DEBUG: creating index item");
            ItemStack indexItem = new ItemStack(Material.PAPER);
            List<String> lore = new ArrayList<String>();
            lore.add(player.getUniqueId().toString());
            lore.add(gameName);
            lore.add(player.getName());
            lore.add("xp:" + BeaconListeners.getTotalExperience(player));
            ItemMeta meta = indexItem.getItemMeta();
            meta.setLore(lore);
            indexItem.setItemMeta(meta);
            int itemIndex = 0;
            chestInv.setItem(itemIndex++, indexItem);
            getLogger().info("DEBUG: copying player's inventory to chest");
            // Copy the player's items to the chest
            for (ItemStack item: player.getInventory()) {
                chestInv.setItem(itemIndex++, item);
            }
            // Clear the player's inventory
            player.getInventory().clear();
            BeaconListeners.setTotalExperience(player, 0);

            // Done!
            getLogger().info("DEBUG: Done!");
        }
    }

    /**
     * Marks all the chests related to a particular game as empty. Chests are not actually emptied until they are reused.
     * @param gameName
     */
    public void removeGame(String gameName) {
        for (UUID uuid: index.keySet()) {
            Iterator<String> it = index.get(uuid).keySet().iterator();
            while (it.hasNext()) {
                String game = it.next();
                if (game.equals(gameName)) {
                    // Add to empty chest location
                    emptyChests.add(index.get(uuid).get(gameName));
                    it.remove();                    
                }
            }
        }
    }

    /**
     * Converts a serialized location to a Location. Returns null if string is
     * empty
     * 
     * @param s
     *            - serialized location in format "world:x:y:z"
     * @return Location
     */
    static public Location getLocationString(final String s) {
        if (s == null || s.trim() == "") {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 4) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            if (w == null) {
                return null;
            }
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            return new Location(w, x, y, z);
        } else if (parts.length == 6) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            if (w == null) {
                return null;
            }
            final int x = Integer.parseInt(parts[1]);
            final int y = Integer.parseInt(parts[2]);
            final int z = Integer.parseInt(parts[3]);
            final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4]));
            final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5]));
            return new Location(w, x, y, z, yaw, pitch);
        }
        return null;
    }

    /**
     * Converts a location to a simple string representation
     * If location is null, returns empty string
     * 
     * @param l
     * @return String of location
     */
    static public String getStringLocation(final Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ() + ":" + Float.floatToIntBits(location.getYaw()) + ":" + Float.floatToIntBits(location.getPitch());
    }

}
