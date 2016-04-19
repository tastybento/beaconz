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

package com.wasteofplastic.beaconz;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.wasteofplastic.beaconz.commands.AdminCmdHandler;
import com.wasteofplastic.beaconz.commands.CmdHandler;
import com.wasteofplastic.beaconz.listeners.BeaconDefenseListener;
import com.wasteofplastic.beaconz.listeners.BeaconListeners;
import com.wasteofplastic.beaconz.listeners.BeaconProjectileDefenseListener;
import com.wasteofplastic.beaconz.listeners.ChatListener;
import com.wasteofplastic.beaconz.listeners.SkyListeners;

public class Beaconz extends JavaPlugin {
    private Register register;
    private World beaconzWorld;
    private static Beaconz plugin;
    static BlockPopulator beaconPopulator;
    private GameMgr gameMgr;
    private Messages messages;
    private BeaconzStore beaconzStore;


    @Override
    public void onEnable() {
        plugin = this;
        // Save the default config from the jar
        saveDefaultConfig();
        loadConfig();

        // Register command(s)
        getCommand("beaconz").setExecutor(new CmdHandler(this));
        getCommand("badmin").setExecutor(new AdminCmdHandler(this));

        //getServer().getPluginManager().registerEvents(new WorldLoader(this), this);


        // Run commands that need to be run 1 tick after start
        getServer().getScheduler().runTask(this, new Runnable() {            

            public void run() {            	
            	
            	// Start the game manager and create the lobby region
            	gameMgr = new GameMgr(plugin); 

                // Load the beacon register
                register = new Register(plugin);
                register.loadRegister();                                
                
                // Create the block populator
                getBp();
                
                // Create the world
                getBeaconzWorld();

                // Create the store world
                beaconzStore = new BeaconzStore(plugin);
                // Register the listeners - block break etc.
                BeaconListeners ev = new BeaconListeners(plugin);
                getServer().getPluginManager().registerEvents(ev, plugin);
                getServer().getPluginManager().registerEvents(new ChatListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconDefenseListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconProjectileDefenseListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new SkyListeners(plugin), plugin);

                // Load messages for players
                messages = new Messages(plugin);
            }

        });
    }


    @Override
    public void onDisable()
    {
        if (register != null) {
            register.saveRegister();
        }
        if (beaconzStore != null) {
            beaconzStore.saveIndex();
        }
        getGameMgr().saveAllGames();
        beaconzWorld.getPopulators().clear();
        if (beaconPopulator != null) {     
            //beaconzWorld.getPopulators().remove(beaconPopulator);
        }
        //getConfig().set("world.distribution", Settings.distribution);
        //saveConfig();
    }

    
    /** 
     * @return the gameMgr
     */
    public GameMgr getGameMgr() {
    	return gameMgr;
    }
    
    /**
     * @return the register
     */
    public Register getRegister() {
        return register;
    }

    /**
     * @return the bp
     */
    public BlockPopulator getBp() {
        if (beaconPopulator == null) {
            beaconPopulator = new BeaconPopulator(this);
        }
        return beaconPopulator;
    }
    
    /**
     * Loads settings from config.yml
     * Clears all old settings
     */
    public void loadConfig() {
    	// get the lobby coords and size, adjust to match chunk size
    	Settings.lobbyx = (getConfig().getInt("world.lobbyx", 0) / 16) * 16;
    	Settings.lobbyz = (getConfig().getInt("world.lobbyz", 0) / 16) * 16;
    	Settings.lobbyradius = (getConfig().getInt("world.lobbyradius", 32) / 16) * 16;
    	if (Settings.lobbyradius == 0) Settings.lobbyradius = 32;
    	
    	// Get the default number of teams
    	Settings.default_teams = getConfig().getInt("default_teams", 2);
    	
    	// Get the default game mode 
    	Settings.gamemode = getConfig().getString("gamemode", "minigame");
    	Settings.gamemode = cleanString(Settings.gamemode, "minigame:strategy", "strategy");
    	
    	// Get the default score types to show
    	Settings.minigameScoreTypes = getConfig().getString("sidebar.minigame", "beacons:links:triangles");
    	Settings.minigameScoreTypes = cleanString(Settings.minigameScoreTypes, "beacons:links:triangles:area", "beacons:links:triangles");
    	Settings.strategyScoreTypes = getConfig().getString("sidebar.strategy", "area:triangles");
    	Settings.strategyScoreTypes = cleanString(Settings.strategyScoreTypes, "beacons:links:triangles:area", "beacons:triangles:area");
    	
    	// Get the default goals for each game mode
    	String mgGoal = getConfig().getString("goals.minigame", "triangles:0");
    	String sgGoal = getConfig().getString("goals.strategy", "area:10000");
    	Settings.minigameGoal = mgGoal.split(":")[0];
    	Settings.strategyGoal = sgGoal.split(":")[0];
    	try{
        	Settings.minigameGoalValue = Integer.valueOf(mgGoal.split(":")[1]);    		
    	} catch (Exception e) {
    		Settings.minigameGoalValue = 10;
    	}
    	try{
    		Settings.strategyGoalValue = Integer.valueOf(sgGoal.split(":")[1]);
    	} catch (Exception e) {
    		Settings.strategyGoalValue = 10000;
    	}   	    	
    	// Check that the goals are actually among the chosen score types; if not, revert to defaults
    	if (!Settings.minigameScoreTypes.contains(Settings.minigameGoal)) {
    		Settings.minigameGoal = "triangles";
    		Settings.minigameGoalValue = 0;
    		if (!Settings.minigameScoreTypes.contains(Settings.minigameGoal)) Settings.minigameScoreTypes = Settings.minigameScoreTypes + ":" + Settings.minigameGoal;
    	}
    	if (!Settings.strategyScoreTypes.contains(Settings.strategyGoal)) {
    		Settings.strategyGoal = "area";
    		Settings.strategyGoalValue = 10000;
    		if (!Settings.strategyScoreTypes.contains(Settings.strategyGoal)) Settings.strategyScoreTypes = Settings.strategyScoreTypes + ":" + Settings.strategyGoal;
    	}    	
        
    	// Get the default timers for each game mode
    	Settings.minigameTimer = getConfig().getInt("timer.minigame", 600);
    	Settings.strategyTimer = getConfig().getInt("timer.strategy", 60000);
    	
    	Settings.linkDistance = getConfig().getDouble("world.linkdistance", 0);
        Settings.expDistance = getConfig().getDouble("world.expdistance", 10);
        Settings.beaconMineExhaustChance = getConfig().getInt("world.beaconmineexhaustchance", 10);
        if (Settings.beaconMineExhaustChance < 0) {
            Settings.beaconMineExhaustChance = 0;
        } else if (Settings.beaconMineExhaustChance > 100) {
            Settings.beaconMineExhaustChance = 100;
        }
        Settings.beaconMineExpRequired = getConfig().getInt("world.beaconmineexp", 10);
        if (Settings.beaconMineExpRequired < 0) {
            Settings.beaconMineExpRequired = 0;
        }
        Settings.defenseHeight = getConfig().getInt("world.defenseheight", 8);
        if (Settings.defenseHeight < 1) {
            Settings.defenseHeight = 1;
        }
        // Load the defense and attack levels
        // The end result is a list of what levels are required for a player to have to build or attack at that
        // height above a beacon.
        // This is a list where the index is the height (minus 1), and the value is the level required.
        if (getConfig().contains("world.defenselevel")) {
            Settings.defenseLevels = new ArrayList<Integer>();
            // Zero the index
            for (int i = 0; i < Settings.defenseHeight; i++) {
                Settings.defenseLevels.add(0);
            }
            // Load from the config
            for (String level : getConfig().getConfigurationSection("world.defenselevel").getValues(false).keySet()) {
                try {
                    int index = Integer.valueOf(level) - 1;
                    if (index >= 0) {
                        int levelReq = getConfig().getInt("world.defenselevel." + level, 0);
                        Settings.defenseLevels.add(index, levelReq);
                    } else {
                        getLogger().severe("Level in world.deferencelevel must be an integer value or 1 or more");
                    }
                } catch (Exception e) {
                    getLogger().warning("Level in world.deferencelevel must be an integer value. This is not valid:" + level);
                }

            }
            // Go through zeros and set to the previous value
            for (int i = 1; i < Settings.defenseHeight; i++) {
                if (Settings.defenseLevels.get(i) == 0) {
                    Settings.defenseLevels.set(i, Settings.defenseLevels.get(i-1));
                }
            }
        }
        if (getConfig().contains("world.attacklevel")) {
            Settings.attackLevels = new ArrayList<Integer>();
            // Zero the index
            for (int i = 0; i < Settings.defenseHeight; i++) {
                Settings.attackLevels.add(0);
            }
            // Load from the config
            for (String level : getConfig().getConfigurationSection("world.attacklevel").getValues(false).keySet()) {
                try {
                    int index = Integer.valueOf(level) - 1;
                    if (index >= 0) {
                        int levelReq = getConfig().getInt("world.attacklevel." + level, 0);
                        Settings.attackLevels.add(index, levelReq);
                    } else {
                        getLogger().severe("Level in world.attacklevel must be an integer value or 1 or more");
                    }
                } catch (Exception e) {
                    getLogger().warning("Level in world.attacklevel must be an integer value. This is not valid:" + level);
                }

            }
            // Go through zeros and set to the previous value
            for (int i = 1; i < Settings.defenseHeight; i++) {
                if (Settings.attackLevels.get(i) == 0) {
                    Settings.attackLevels.set(i, Settings.attackLevels.get(i-1));
                }
            }
        }
        Settings.pairLinking = getConfig().getBoolean("world.pairs", true);
        Settings.teamChat = getConfig().getBoolean("world.teamchat", false);
        Settings.worldName = getConfig().getString("world.name", "beaconz");
        Settings.distribution = getConfig().getDouble("world.distribution", 0.05D);
        if (Settings.distribution < 0.001D) {
            Settings.distribution = 0.001D;
        }
        Settings.gameDistance = getConfig().getInt("world.distance", 400);
        Settings.xCenter = getConfig().getInt("world.xcenter",0);
        Settings.zCenter = getConfig().getInt("world.zcenter",0);
        Settings.randomSpawn = getConfig().getBoolean("world.randomspawn", true);
        Settings.seedAdjustment = getConfig().getLong("world.seedadjustment", 0);
        Settings.mineCoolDown = getConfig().getInt("world.minecooldown", 1) * 60000; // Minutes in millis
        ConfigurationSection enemyFieldSection = getConfig().getConfigurationSection("world.enemyfieldeffects");
        // Step through the numbers
        Settings.enemyFieldEffects = new HashMap<Integer, List<PotionEffect>>();
        for (Entry<String, Object> part : enemyFieldSection.getValues(false).entrySet()) {
            if (NumberUtils.isNumber(part.getKey())) {
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<PotionEffect>();
                List<String> effectsList = getConfig().getStringList("world.enemyfieldeffects." + part.getKey());
                for (String effectString : effectsList) {
                    String[] split = effectString.split(":");
                    if (split.length == 1 || split.length > 2) {
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            effects.add(new PotionEffect(type, Integer.MAX_VALUE, 1));  
                        }
                    }
                    if (split.length == 2) {
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            if (NumberUtils.isNumber(split[1])) {
                                //getLogger().info("DEBUG: adding enemy effect " + type.toString());
                                effects.add(new PotionEffect(type, Integer.MAX_VALUE, NumberUtils.toInt(split[1])));
                            } else {
                                effects.add(new PotionEffect(type, Integer.MAX_VALUE, 1));
                            }
                        }

                    }

                }
                Settings.enemyFieldEffects.put(NumberUtils.toInt(part.getKey()), effects);               
            }
        }
        Settings.friendlyFieldEffects = new HashMap<Integer, List<PotionEffect>>();
        ConfigurationSection friendlyFieldSection = getConfig().getConfigurationSection("world.friendlyfieldeffects");
        // Step through the numbers
        for (Entry<String, Object> part : friendlyFieldSection.getValues(false).entrySet()) {
            //getLogger().info("DEBUG: Field: " + part.getKey());
            if (NumberUtils.isNumber(part.getKey())) {
                //getLogger().info("DEBUG: Field is a number");
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<PotionEffect>();
                List<String> effectsList = getConfig().getStringList("world.friendlyfieldeffects." + part.getKey());
                //getLogger().info("DEBUG: Effects list: " + effectsList);
                for (String effectString : effectsList) {
                    String[] split = effectString.split(":");
                    if (split.length == 1 || split.length > 2) {
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            effects.add(new PotionEffect(type, Integer.MAX_VALUE, 1));  
                        }
                    }
                    if (split.length == 2) {
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            if (NumberUtils.isNumber(split[1])) {
                                //getLogger().info("DEBUG: adding enemy effect " + type.toString());
                                effects.add(new PotionEffect(type, Integer.MAX_VALUE, NumberUtils.toInt(split[1])));
                            } else {
                                effects.add(new PotionEffect(type, Integer.MAX_VALUE, 1));
                            }
                        }

                    }

                }
                Settings.friendlyFieldEffects.put(NumberUtils.toInt(part.getKey()), effects);               
            }
        }
        Settings.minePenalty = getConfig().getStringList("world.minepenalty");
        List<String> goodies = getConfig().getStringList("world.enemygoodies");
        Settings.enemyGoodies.clear();
        for (String goodie: goodies) {
            String[] split = goodie.split("=");
            int lastChance = 0;
            if (!Settings.enemyGoodies.isEmpty() ) {
                lastChance = Settings.enemyGoodies.lastKey();
            }
            if (split.length == 2) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[1])) {
                    chance = NumberUtils.toInt(split[1]) + lastChance;
                }
                ItemStack item = getItemFromString(split[0]);
                if (item != null) {
                    Settings.enemyGoodies.put(chance, item);
                }
            }
        }
        // Add the final element - any number above this value will return a null element
        Settings.enemyGoodies.put(Settings.enemyGoodies.lastKey() + 1, null);
        // Debug
        /*
        for (Entry<Integer, ItemStack> ent : Settings.enemyGoodies.entrySet()) {
            plugin.getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
        }
         */
        // Team goodies
        goodies = getConfig().getStringList("world.teamgoodies");
        Settings.teamGoodies.clear();
        for (String goodie: goodies) {
            String[] split = goodie.split("=");
            int lastChance = 0;
            if (!Settings.teamGoodies.isEmpty() ) {
                lastChance = Settings.teamGoodies.lastKey();
            }
            if (split.length == 2) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[1])) {
                    chance = NumberUtils.toInt(split[1]) + lastChance;
                }
                ItemStack item = getItemFromString(split[0]);
                if (item != null) {
                    Settings.teamGoodies.put(chance, item);
                }
            }
        }

        // Add the final element - any number above this value will return a null element
        Settings.teamGoodies.put(Settings.teamGoodies.lastKey() + 1, null);
        // Own team
        /*
        for (Entry<Integer, ItemStack> ent : Settings.teamGoodies.entrySet()) {
            plugin.getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
        }
         */
        // Add initial inventory
        List<String> newbieKit = getConfig().getStringList("world.newbiekit");
        Settings.newbieKit.clear();
        for (String item : newbieKit) {
            Settings.newbieKit.add(getItemFromString(item));
        }
    }

    /**
     * Format is Material:Qty or Material:Data:Qty or Integer:Qty or Integer:Data:Qty
     * @param item
     * @return
     */
    private ItemStack getItemFromString(String goodie) {
        String[] split = goodie.split(":");
        // No durability option - Material/#:Qty
        if (split.length == 2) {
            // Get the qty
            int qty = 0;
            if (NumberUtils.isNumber(split[1])) {
                qty = NumberUtils.toInt(split[1]);
            }
            // Try to get #
            if (NumberUtils.isNumber(split[0])) {
                int itemType = NumberUtils.toInt(split[0]);
                return new ItemStack(itemType, qty);
            } else {
                try {
                    Material itemType = Material.valueOf(split[0]);
                    return new ItemStack(itemType, qty); 
                } catch (Exception e) {
                    getLogger().severe("Could not parse " + split[0] + " skipping...");
                    return null;
                }
            }
        } else if (split.length == 3) {
            // Get the qty
            int qty = 0;
            if (NumberUtils.isNumber(split[2])) {
                qty = NumberUtils.toInt(split[2]);
            }
            // Get the durability
            int durability = 0;
            if (NumberUtils.isNumber(split[1])) {
                durability = NumberUtils.toInt(split[1]);
            }
            // Try to get #
            if (NumberUtils.isNumber(split[0])) {
                int itemType = NumberUtils.toInt(split[0]);
                ItemStack item = new ItemStack(itemType, qty);
                item.setDurability((short)durability);
                return item;
            } else {
                try {
                    Material itemType = Material.valueOf(split[0]);
                    ItemStack item = new ItemStack(itemType, qty);
                    item.setDurability((short)durability);
                    return item;
                } catch (Exception e) {
                    getLogger().severe("Could not parse " + split[0] + " skipping...");
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Get the world that Beaconz runs in and if it doesn't exist, make it
     * @return
     */
    public World getBeaconzWorld() {
        // Check to see if the world exists, and if not, make it
        if (beaconzWorld == null) {
            // World doesn't exist, so make it
            getLogger().info("World is '" + Settings.worldName + "'");
            try {
                beaconzWorld = WorldCreator.name(Settings.worldName).type(WorldType.NORMAL).environment(World.Environment.NORMAL).createWorld();            
            } catch (Exception e) {
                getLogger().info("Could not make world yet..");
                return null;
            }
            beaconzWorld.getPopulators().add(getBp());
        }
        // This is not allowed in this function as it can be called async
        //beaconzWorld.setSpawnLocation(Settings.xCenter, beaconzWorld.getHighestBlockYAt(Settings.xCenter, Settings.zCenter), Settings.zCenter);
        return beaconzWorld;
    }

    /**
     * @return the beaconzStore
     */
    public BeaconzStore getBeaconzStore() {
        return beaconzStore;
    }


    /**
     * @return the messages
     */
    public Messages getMessages() {
        return messages;
    }


    /**
     * Cleans a ":"-delimited string of any extraneous elements
     * Used to ignore user typos on a list of parameters
     * so that "becons:links:triangles" becomes "links:triangles"
     * 
     */
    public String cleanString(String strToClean, String basevalues, String defaultIfEmpty) {
    	strToClean = strToClean + ":";
    	basevalues = basevalues + ":";
    	Boolean removestr = false;
    	// Remove extraneous text
    	for (String str : strToClean.split(":")) {
    		removestr = true;
    		for (String bv : basevalues.split(":")) {
        		if (bv.equals(str)) removestr = false;        			
    		}
    		if (removestr) strToClean = strToClean.replace(str, "");
    	}    		
        //Reassemble the string
        String newString = "";
        for (String str: strToClean.split(":")) {
        	if (!str.isEmpty()) newString = newString + str + ":";
        }
        if (newString.length() > 0) newString = newString.substring(0, newString.length()-1);
    	if (newString.isEmpty()) newString = defaultIfEmpty;
    	return newString;
    }
    
    /**
     * Gets the highest block in the world at x,z starting at the max height block can be
     * @param x
     * @param z
     * @return height of first non-air block
     */
    public int getHighestBlockYAt(int x, int z) {
        for (int y = 254; y > 0; y--) {
            if (!getBeaconzWorld().getBlockAt(x, y, z).getType().equals(Material.AIR)) {
                return y+1;
            }
        }
        return 0;
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

}
