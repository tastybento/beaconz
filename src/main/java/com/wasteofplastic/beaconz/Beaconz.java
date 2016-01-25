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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

public class Beaconz extends JavaPlugin {
    private Register register;
    private World beaconzWorld;
    private static Beaconz plugin;
    static BlockPopulator beaconPopulator;
    private Scorecard scorecard;
    private Messages messages;

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

                // Load the scorecard - cannot be done until after the server starts
                scorecard = new Scorecard(plugin);
                // Add teams
                //addTeams();

                // Load the beacon register
                register = new Register(plugin);
                register.loadRegister();

                // Load the teams
               // scorecard.loadTeamMembers();

                // Create the block populator
                if (beaconPopulator == null) {
                    getBp();
                }
                // Create the world
                getBeaconzWorld();

                // Set the world border
                beaconzWorld.getWorldBorder().reset();
                setWorldBorder();

                // Register the listeners - block break etc.
                BeaconListeners ev = new BeaconListeners(plugin);
                getServer().getPluginManager().registerEvents(ev, plugin);
                getServer().getPluginManager().registerEvents(new ChatListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconDefenseListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new SkyListeners(plugin), plugin);

                // Create the corner beacons if world boarder is active
                if (Settings.borderSize > 0) {
                    createCorners();
                }

                // Load messages for players
                messages = new Messages(plugin);
            }

        });
    }


    /**
     * Sets the world border if the border exits
     */
    public void setWorldBorder() {
        beaconzWorld.getWorldBorder().setCenter(Settings.xCenter, Settings.zCenter);
        if (Settings.borderSize > 0) {
            beaconzWorld.getWorldBorder().setSize(Settings.borderSize);
        }
    }

    /**
     * Creates the corner-most beaconz so that the map can be theoretically be covered entirely (almost)
     */
    private void createCorners() {

        // Check corners
        Set<Point2D> corners = new HashSet<Point2D>();
        int xMin = Settings.xCenter - (Settings.borderSize /2) + 2;
        int xMax = Settings.xCenter + (Settings.borderSize /2) - 3;
        int zMin = Settings.zCenter - (Settings.borderSize /2) + 2;
        int zMax = Settings.zCenter + (Settings.borderSize /2) - 3;
        corners.add(new Point2D.Double(xMin,zMin));
        corners.add(new Point2D.Double(xMin,zMax));
        corners.add(new Point2D.Double(xMax,zMin));
        corners.add(new Point2D.Double(xMax,zMax));
        for (Point2D point : corners) {
            if (!register.isNearBeacon(point, 5)) {
                Block b = getBeaconzWorld().getHighestBlockAt((int)point.getX(), (int)point.getY());
                while (b.getType().equals(Material.AIR) || b.getType().equals(Material.LEAVES) || b.getType().equals(Material.LEAVES_2)) {          
                    if (b.getY() == 0) {
                        // Oops, nothing here
                        break;
                    }
                    b = b.getRelative(BlockFace.DOWN);
                }
                if (b.getY() > 3) {
                    // Create a beacon
                    //Bukkit.getLogger().info("DEBUG: made beacon at " + b.getLocation());
                    b.setType(Material.BEACON);
                    // Register the beacon
                    register.addBeacon(null, b.getLocation());
                    // Add the capstone
                    b.getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
                    // Create the pyramid
                    b = b.getRelative(BlockFace.DOWN);

                    // All diamond blocks for now
                    b.setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.SOUTH).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.SOUTH_EAST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.SOUTH_WEST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.EAST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.WEST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.NORTH).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.NORTH_EAST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.NORTH_WEST).setType(Material.DIAMOND_BLOCK);
                }
            }
        }
    }



    @Override
    public void onDisable()
    {
        if (register != null) {
            register.saveRegister();
        }
        if (scorecard != null) {
            scorecard.saveTeamMembers();
        }
        beaconzWorld.getPopulators().clear();
        if (beaconPopulator != null) {     
            //beaconzWorld.getPopulators().remove(beaconPopulator);
        }
        //getConfig().set("world.distribution", Settings.distribution);
        //saveConfig();
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
     * @return the scorecard
     */
    public Scorecard getScorecard() {
        return scorecard;
    }

    /**
     * Loads settings from config.yml
     * Clears all old settings
     */
    public void loadConfig() {
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
        Settings.teamChat = true;
        Settings.worldName = getConfig().getString("world.name", "beaconz");
        Settings.distribution = getConfig().getDouble("world.distribution", 0.05D);
        if (Settings.distribution < 0.001D) {
            Settings.distribution = 0.001D;
        }
        Settings.borderSize = getConfig().getInt("world.size",0);
        if (Settings.borderSize < 0) {
            Settings.borderSize = 0;
        }
        Settings.gameDistance = getConfig().getInt("world.distance", Settings.borderSize);
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
                    if (split.length == 3) {
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            if (NumberUtils.isNumber(split[1]) && NumberUtils.isNumber(split[2])) {
                                //getLogger().info("DEBUG: adding enemy effect " + type.toString());
                                effects.add(new PotionEffect(type, NumberUtils.toInt(split[1]), NumberUtils.toInt(split[2])));
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
                    if (split.length == 3) {
                        //getLogger().info("DEBUG: Potion found " + split[0]);
                        PotionEffectType type = PotionEffectType.getByName(split[0]);
                        if (type != null) {
                            //getLogger().info("DEBUG: Potion is known");
                            if (NumberUtils.isNumber(split[1]) && NumberUtils.isNumber(split[2])) {

                                //getLogger().info("DEBUG: adding friendly effect " + type.toString());
                                effects.add(new PotionEffect(type, NumberUtils.toInt(split[1]), NumberUtils.toInt(split[2])));
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
        beaconzWorld.setSpawnLocation(Settings.xCenter, beaconzWorld.getHighestBlockYAt(Settings.xCenter, Settings.zCenter), Settings.zCenter);
        return beaconzWorld;
    }

    /**
     * @return the messages
     */
    public Messages getMessages() {
        return messages;
    }

    public void newGame() {
        // Remove the world border
        beaconzWorld.getWorldBorder().reset();
        // Remove the old beacons from the register
        register.clear();
        // Move game to a new location
        nextGameLocation();
        // Regenerate the play area - do this before teleporting players to the spot
        regenerateGame();
        // Create the corners - TODO needs more work for the minigame
        //createCorners();
        // Reset default spawn location - needed to calculate the correct team teleport spawn points
        getBeaconzWorld();
        // Put the world border up
        setWorldBorder();
        // Reset scores (and teams, etc.)
        scorecard.reset();
    }

    /**
     * Cleans up the game area so it can be played again
     */
    private void regenerateGame() {
        int xMin = (Settings.xCenter - (Settings.borderSize /2) -15) / 16;
        int xMax = (Settings.xCenter + (Settings.borderSize /2) + 15) / 16;
        int zMin = (Settings.zCenter - (Settings.borderSize /2) -15) / 16;
        int zMax = (Settings.zCenter + (Settings.borderSize /2) + 15) / 16;
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                beaconzWorld.regenerateChunk(x, z);
            }
        }

    }

    public void nextGameLocation() {
        if (Settings.xCenter < Settings.zCenter) {
            if (-1 * Settings.xCenter < Settings.zCenter) {
                Settings.xCenter += Settings.gameDistance;
                return;
            }
            Settings.zCenter += Settings.gameDistance;
            return;
        }
        if (Settings.xCenter > Settings.zCenter) {
            if (-1 * Settings.xCenter >= Settings.zCenter) {
                Settings.xCenter -= Settings.gameDistance;
                return;
            }
            Settings.zCenter -= Settings.gameDistance;
            return;
        }
        if (Settings.xCenter <= 0) {
            Settings.zCenter += Settings.gameDistance;
            return;
        }
        Settings.zCenter -= Settings.gameDistance;
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
    
}
