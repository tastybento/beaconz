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

package com.wasteofplastic.beaconz;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tiny database for a hashmap that is not used very often, but could be very big so I
 * don't want it in memory. 
 * @author tastybento
 *
 */
public class TinyDB {
    private Beaconz plugin;
    private ConcurrentHashMap<String,UUID> treeMap;
    private boolean savingFlag;
    /**
     * Opens the database
     * @param plugin
     */
    public TinyDB(Beaconz plugin) {       
        this.plugin = plugin;
        this.treeMap = new ConcurrentHashMap<String,UUID>();
        File database = new File(plugin.getDataFolder(), "name-uuid.txt");
        if (!database.exists()) {
            try {
                database.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Async Saving of the DB
     */
    public void asyncSaveDB() {
        if (!savingFlag) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    saveDB();                
                }}.runTaskAsynchronously(plugin);
        }
    }

    /**
     * Saves the DB
     */
    public void saveDB() {
        savingFlag = true;
        try {
            File oldDB = new File(plugin.getDataFolder(), "name-uuid.txt");
            File newDB = new File(plugin.getDataFolder(), "name-uuid-new.txt");
            //File backup = new File(plugin.getDataFolder(), "name-uuid.bak");
            try(PrintWriter out = new PrintWriter(newDB)) {
                // Write the newest entries at the top
                for (Entry<String, UUID> entry: treeMap.entrySet()) {
                    out.println(entry.getKey());
                    out.println(entry.getValue().toString());
                }
                if (oldDB.exists()) {
                    // Go through the old file and remove any                     
                    try(BufferedReader br = new BufferedReader(new FileReader(oldDB))){
                        // Go through the old file and read it line by line and write to the new file
                        String line = br.readLine();
                        String uuid = br.readLine();
                        while (line != null) {
                            if (!treeMap.containsKey(line)) {                  
                                out.println(line);
                                out.println(uuid);
                            }                    
                            // Read next lines
                            line = br.readLine();
                            uuid = br.readLine();
                        } 
                        br.close();
                    }
                    
                }                
                out.close();      
            }
            
            // Move files around
            try {
                Files.move(newDB.toPath(), oldDB.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().severe("Problem saving name database! Could not rename files!");                
            }                         
        } catch (IOException e) {
            plugin.getLogger().severe("Problem saving name database!");
            e.printStackTrace();
        }
        savingFlag = false;
    }

    /**
     * Saves the player name to the database. Case insensitive!
     * @param playerName
     * @param playerUUID
     */
    public void savePlayerName(String playerName, UUID playerUUID) {
        treeMap.put(playerName.toLowerCase(), playerUUID);
        // This will be saved when everything shuts down
    }

    /**
     * Gets the UUID for this player name or null if not known. Case insensitive!
     * @param playerName
     * @return UUID of player, or null if unknown
     */
    public UUID getPlayerUUID(String playerName) {
        // Try cache
        if (treeMap.containsKey(playerName.toLowerCase())) {
            //plugin.getLogger().info("DEBUG: found in UUID cache");
            return treeMap.get(playerName.toLowerCase());
        }
        // Names and UUID's are stored in line pairs.
        try(BufferedReader br = new BufferedReader(new FileReader(new File(plugin.getDataFolder(), "name-uuid.txt")))) {
            String line = br.readLine();
            String uuid = br.readLine();
            while (line != null && !line.equalsIgnoreCase(playerName)) {                
                line = br.readLine();
                uuid = br.readLine();
            }
            if (line == null) {
                return null;
            }
            UUID result = UUID.fromString(uuid);
            // Add to cache
            treeMap.put(playerName.toLowerCase(), result);
            //plugin.getLogger().info("DEBUG: found in UUID database");
            return result;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return null;
    }
}
