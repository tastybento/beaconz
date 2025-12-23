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


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.beaconz.commands.AdminCmdHandler;
import com.wasteofplastic.beaconz.commands.CmdHandler;
import com.wasteofplastic.beaconz.dynmap.OurServerListener;
import com.wasteofplastic.beaconz.listeners.BeaconCaptureListener;
import com.wasteofplastic.beaconz.listeners.BeaconLinkListener;
import com.wasteofplastic.beaconz.listeners.BeaconPassiveDefenseListener;
import com.wasteofplastic.beaconz.listeners.BeaconProjectileDefenseListener;
import com.wasteofplastic.beaconz.listeners.BeaconProtectionListener;
import com.wasteofplastic.beaconz.listeners.BeaconSurroundListener;
import com.wasteofplastic.beaconz.listeners.ChatListener;
import com.wasteofplastic.beaconz.listeners.LobbyListener;
import com.wasteofplastic.beaconz.listeners.PlayerDeathListener;
import com.wasteofplastic.beaconz.listeners.PlayerJoinLeaveListener;
import com.wasteofplastic.beaconz.listeners.PlayerMovementListener;
import com.wasteofplastic.beaconz.listeners.PlayerTeleportListener;
import com.wasteofplastic.beaconz.listeners.SkyListeners;
import com.wasteofplastic.beaconz.listeners.WorldListener;

public class Beaconz extends JavaPlugin {
    private Register register;
    private World beaconzWorld;
    private static Beaconz plugin;
    static BlockPopulator beaconPopulator;
    private GameMgr gameMgr;
    private Messages messages;
    private BeaconzStore beaconzStore;
    private Lang locale;
    protected PlayerMovementListener pml;
    private TinyDB nameStore;
    private PlayerTeleportListener teleportListener;
    public Boolean ignoreChunkLoad;


    @Override
    public void onEnable() {
        plugin = this;
        // Save the default config from the jar
        saveDefaultConfig();
        loadConfig();

        // Register command(s)
        getCommand("beaconz").setExecutor(new CmdHandler(this));
        getCommand("badmin").setExecutor(new AdminCmdHandler(this));

        // Metrics
        try {
            final Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (final IOException localIOException) {}

        // Start the name store
        nameStore = new TinyDB(this);

        // Run commands that need to be run 1 tick after start
        getServer().getScheduler().runTask(this, new Runnable() {

            @Override
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

                // Create the inventory store 
                beaconzStore = new BeaconzStore(plugin);

                // Register the listeners - block break etc. 
                getServer().getPluginManager().registerEvents(new BeaconLinkListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconCaptureListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new ChatListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconPassiveDefenseListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconProjectileDefenseListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconProtectionListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new PlayerDeathListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(plugin), plugin);
                pml = new PlayerMovementListener(plugin);
                getServer().getPluginManager().registerEvents(pml, plugin);
                teleportListener = new PlayerTeleportListener(plugin);
                getServer().getPluginManager().registerEvents(teleportListener, plugin);
                getServer().getPluginManager().registerEvents(new SkyListeners(plugin), plugin);
                getServer().getPluginManager().registerEvents(new WorldListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new BeaconSurroundListener(plugin), plugin);
                getServer().getPluginManager().registerEvents(new LobbyListener(plugin), plugin);
                ignoreChunkLoad = false; // used in WorldListener and other classes

                // Load messages for players
                messages = new Messages(plugin);

                /* Get dynmap */
                if (Settings.useDynmap) {
                    PluginManager pm = getServer().getPluginManager();
                    Plugin dynmap = pm.getPlugin("dynmap");
                    if(dynmap != null) {
                        getLogger().info("Hooking into dynmap.");
                        getServer().getPluginManager().registerEvents(new OurServerListener(plugin, dynmap), plugin); 
                    }
                }
                // Make first game
                if (gameMgr.getGames().isEmpty()) {
                    gameMgr.newGame(Settings.defaultGameName);
                }
            }

        });
    }


    @Override
    public void onDisable()
    {
        if (register != null) {
            register.saveRegister();
            // Remove all map renderers
            register.removeMapRenderers();
        }
        if (beaconzStore != null) {
            beaconzStore.saveInventories();
        }

        getGameMgr().saveAllGames();
        /* 
        beaconzWorld.getPopulators().clear();
        if (beaconPopulator != null) {
            beaconzWorld.getPopulators().remove(beaconPopulator);
        }
         */
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
     * @return the pml
     */
    public PlayerMovementListener getPml() {
        return pml;
    }


    /**
     * Loads settings from config.yml
     * Clears all old settings
     */
    public void loadConfig() {
        // Use scoreboard
        Settings.useScoreboard = getConfig().getBoolean("general.usescoreboard");
        // Show timer
        Settings.showTimer = getConfig().getBoolean("general.showtimer");
        // Dynmap
        Settings.useDynmap = getConfig().getBoolean("general.usedynmap");
        // Destroy link blocks when they are removed
        Settings.destroyLinkBlocks = getConfig().getBoolean("links.destroylinkblocks",true);
        // Remove longest link if range extender block removed
        Settings.removeLongestLink = getConfig().getBoolean("links.removelongestlink");
        // Link rewards
        Settings.linkRewards = getConfig().getStringList("links.linkrewards");
        // Link commands
        Settings.linkCommands = getConfig().getStringList("links.linkcommands");
        // Default language
        locale = new Lang(this);
        locale.loadLocale(getConfig().getString("general.defaultlocale","en-US"));
        // Default game name
        Settings.defaultGameName = getConfig().getString("world.defaultgamename", "Beaconz");
        // Height for lobby platform
        Settings.lobbyHeight = getConfig().getInt("lobby.lobbyheight", 200);
        Settings.lobbyBlocks = getConfig().getStringList("lobby.lobbyblocks");
        if (Settings.lobbyBlocks.isEmpty()) {
            Settings.lobbyBlocks.add("GLASS");
        }
        Settings.allowLobbyAnimalSpawn = getConfig().getBoolean("lobby.allowAnimalSpawn");
        Settings.allowLobbyEggs = getConfig().getBoolean("lobby.allowEggs");
        Settings.allowLobbyMobSpawn = getConfig().getBoolean("lobby.allowMobSpawn");
        // The maximum distance the beacon can link without extending link blocks
        Settings.linkLimit = getConfig().getInt("links.linklimit", 500);
        // Link blocks enable links to reach further for less experience
        Settings.linkBlocks = new HashMap<Material, Integer>();
        if (getConfig().contains("links.linkblocks")) {
            for (String material: getConfig().getConfigurationSection("links.linkblocks").getKeys(false)) {
                //getLogger().info("DEBUG: reading " + material);
                try {
                    Material mat = Material.getMaterial(material.toUpperCase());
                    if (mat != null) {
                        int value = getConfig().getInt("links.linkblocks." + material);
                        //getLogger().info("DEBUG: value = " + value);
                        Settings.linkBlocks.put(mat, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // Max number of links a beacon can have
        Settings.maxLinks = getConfig().getInt("links.maxlinks", 6);

        // The Locking block
        Settings.lockingBlock = getConfig().getString("general.lockingBlock", "EMERALD_BLOCK");
        Settings.nbrLockingBlocks = getConfig().getInt("nbrLockingBlocks", 6);

        // Load teleport delay
        Settings.teleportDelay = getConfig().getInt("general.teleportdelay",5);
        // get the lobby coords and size, adjust to match chunk size
        Settings.lobbyx = (getConfig().getInt("lobby.lobbyx", 0) / 16) * 16;
        Settings.lobbyz = (getConfig().getInt("lobby.lobbyz", 0) / 16) * 16;
        Settings.lobbyradius = (getConfig().getInt("lobby.lobbyradius", 32) / 16) * 16;
        if (Settings.lobbyradius == 0) Settings.lobbyradius = 32;

        // Get the default number of teams
        Settings.defaultTeamNumber = getConfig().getInt("teams.defaultNumber", 2);

        // Get the default game mode
        Settings.gamemode = getConfig().getString("general.gamemode", "strategy");
        Settings.gamemode = cleanString(Settings.gamemode, "minigame:strategy", "strategy");

        // Get the default score types to show
        Settings.minigameScoreTypes = getConfig().getString("scoreboard.sidebar.minigame", "beacons:links:triangles");
        Settings.minigameScoreTypes = cleanString(Settings.minigameScoreTypes, "beacons:links:triangles:area", "beacons:links:triangles");
        Settings.strategyScoreTypes = getConfig().getString("scoreboard.sidebar.strategy", "area:triangles");
        Settings.strategyScoreTypes = cleanString(Settings.strategyScoreTypes, "beacons:links:triangles:area", "beacons:triangles:area");

        // Get the default goals for each game mode
        String mgGoal = getConfig().getString("scoreboard.goal.minigame", "triangles:0");
        String sgGoal = getConfig().getString("scoreboard.goal.strategy", "area:3000000");
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
        Settings.minigameTimer = getConfig().getInt("scoreboard.timer.minigame", 600);
        Settings.strategyTimer = getConfig().getInt("scoreboard.timer.strategy", 0);

        Settings.expDistance = getConfig().getDouble("links.expdistance", 5);
        Settings.beaconMineExhaustChance = getConfig().getInt("mining.beaconmineexhaustchance", 10);
        if (Settings.beaconMineExhaustChance < 0) {
            Settings.beaconMineExhaustChance = 0;
        } else if (Settings.beaconMineExhaustChance > 100) {
            Settings.beaconMineExhaustChance = 100;
        }
        Settings.beaconMineExpRequired = getConfig().getInt("mining.beaconmineexp", 10);
        if (Settings.beaconMineExpRequired < 0) {
            Settings.beaconMineExpRequired = 0;
        }
        Settings.defenseHeight = getConfig().getInt("defense.defenseheight", 8);
        if (Settings.defenseHeight < 1) {
            Settings.defenseHeight = 1;
        }
        Settings.removaldelta = getConfig().getInt("defense.removaldelta", -1);
        // Load the defense and attack levels
        // The end result is a list of what levels are required for a player to have to build or attack at that
        // height above a beacon.
        // This is a list where the index is the height (minus 1), and the value is the level required.
        if (getConfig().contains("defense.defenselevel")) {
            Settings.defenseLevels = new ArrayList<Integer>();
            // Zero the index
            for (int i = 0; i < Settings.defenseHeight; i++) {
                Settings.defenseLevels.add(0);
            }
            // Load from the config
            for (String level : getConfig().getConfigurationSection("defense.defenselevel").getValues(false).keySet()) {
                try {
                    int index = Integer.valueOf(level) - 1;
                    if (index >= 0) {
                        int levelReq = getConfig().getInt("defense.defenselevel." + level, 0);
                        Settings.defenseLevels.add(index, levelReq);
                    } else {
                        getLogger().severe("Level in defense.defenselevel must be an integer value or 1 or more");
                    }
                } catch (Exception e) {
                    getLogger().warning("Level in defense.defenselevel must be an integer value. This is not valid:" + level);
                }

            }
            // Go through zeros and set to the previous value
            for (int i = 1; i < Settings.defenseHeight; i++) {
                if (Settings.defenseLevels.get(i) == 0) {
                    Settings.defenseLevels.set(i, Settings.defenseLevels.get(i-1));
                }
            }
        }
        if (getConfig().contains("defense.attacklevel")) {
            Settings.attackLevels = new ArrayList<Integer>();
            // Zero the index
            for (int i = 0; i < Settings.defenseHeight; i++) {
                Settings.attackLevels.add(0);
            }
            // Load from the config
            for (String level : getConfig().getConfigurationSection("defense.attacklevel").getValues(false).keySet()) {
                try {
                    int index = Integer.valueOf(level) - 1;
                    if (index >= 0) {
                        int levelReq = getConfig().getInt("defense.attacklevel." + level, 0);
                        Settings.attackLevels.add(index, levelReq);
                    } else {
                        getLogger().severe("Level in defense.attacklevel must be an integer value or 1 or more");
                    }
                } catch (Exception e) {
                    getLogger().warning("Level in defense.attacklevel must be an integer value. This is not valid:" + level);
                }

            }
            // Go through zeros and set to the previous value
            for (int i = 1; i < Settings.defenseHeight; i++) {
                if (Settings.attackLevels.get(i) == 0) {
                    Settings.attackLevels.set(i, Settings.attackLevels.get(i-1));
                }
            }
        }
        Settings.teamChat = getConfig().getBoolean("general.teamchat", false);
        Settings.worldName = getConfig().getString("world.name", "beaconz");
        Settings.distribution = getConfig().getDouble("world.distribution", 0.03D);
        if (Settings.distribution < 0.001D) {
            Settings.distribution = 0.001D;
        }
        Settings.gameDistance = getConfig().getInt("world.distance", 2000);
        Settings.xCenter = getConfig().getInt("world.xcenter",2000);
        Settings.zCenter = getConfig().getInt("world.zcenter",2000);
        Settings.seedAdjustment = getConfig().getLong("world.seedadjustment", 0);
        Settings.mineCoolDown = getConfig().getInt("mining.minecooldown", 1) * 60000; // Minutes in millis
        ConfigurationSection enemyFieldSection = getConfig().getConfigurationSection("triangles.enemyfieldeffects");
        // Step through the numbers
        Settings.enemyFieldEffects = new HashMap<Integer, List<PotionEffect>>();
        for (Entry<String, Object> part : enemyFieldSection.getValues(false).entrySet()) {
            if (NumberUtils.isNumber(part.getKey())) {
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<PotionEffect>();
                List<String> effectsList = getConfig().getStringList("triangles.enemyfieldeffects." + part.getKey());
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
        ConfigurationSection friendlyFieldSection = getConfig().getConfigurationSection("triangles.friendlyfieldeffects");
        // Step through the numbers
        for (Entry<String, Object> part : friendlyFieldSection.getValues(false).entrySet()) {
            //getLogger().info("DEBUG: Field: " + part.getKey());
            if (NumberUtils.isNumber(part.getKey())) {
                //getLogger().info("DEBUG: Field is a number");
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<PotionEffect>();
                List<String> effectsList = getConfig().getStringList("triangles.friendlyfieldeffects." + part.getKey());
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
        Settings.minePenalty = getConfig().getStringList("mining.minepenalty");
        List<String> goodies = getConfig().getStringList("mining.enemygoodies");
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
                ItemStack item = getItemFromString(split[0].toUpperCase());
                if (item != null) {
                    Settings.enemyGoodies.put(chance, item);
                } else {
                    getLogger().severe("Could not import " + split[0] + " as enemy goodie, skipping...");
                }
            }
        }
        // Debug
        /*
        for (Entry<Integer, ItemStack> ent : Settings.enemyGoodies.entrySet()) {
            plugin.getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
        }
         */
        // Team goodies
        goodies = getConfig().getStringList("mining.teamgoodies");
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
                ItemStack item = getItemFromString(split[0].toUpperCase());
                if (item != null) {
                    Settings.teamGoodies.put(chance, item);
                } else {
                    getLogger().severe("Could not import " + split[0] + " as a team goodie, skipping...");
                }
            }
        }

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

        // Set the initial XP for minigames
        Settings.initialXP = getConfig().getInt("world.initialXP", 100);

    }

    /**
     * Format is Material:Qty or Material:Data:Qty or Integer:Qty or Integer:Data:Qty
     * @param item
     * @return
     */
    @SuppressWarnings("deprecation")
    private ItemStack getItemFromString(String goodie) {
        String[] split = goodie.split(":");
        // No durability option - Material/#:Qty
        if (split.length == 2) {
            // Get the qty
            int qty = 0;
            if (NumberUtils.isNumber(split[1])) {
                qty = NumberUtils.toInt(split[1]);
            }
            try {
                Material itemType = Material.valueOf(split[0]);
                return new ItemStack(itemType, qty);
            } catch (Exception e) {
                getLogger().severe("Could not parse " + split[0] + " skipping...");
                return null;
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
        }
        // This is not allowed in this function as it can be called async
        //beaconzWorld.setSpawnLocation(Settings.xCenter, beaconzWorld.getHighestBlockYAt(Settings.xCenter, Settings.zCenter), Settings.zCenter);
        /*
        if (!beaconzWorld.getPopulators().contains(getBp())) {
            beaconzWorld.getPopulators().add(getBp());
        }
         */
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
    static public String getStringLocation(final Location l) {
        if (l == null || l.getWorld() == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + strDbl(l.getX(),2) + ":" + strDbl(l.getY(),2) + ":" + strDbl(l.getZ(),2) 
        + ":" + String.valueOf(Float.floatToIntBits(l.getYaw())) + ":" + String.valueOf(Float.floatToIntBits(l.getPitch()));        
    }

    static private String strDbl (final Double dbl, int places) {
        return (double)((int)(dbl * Math.pow(10.0, places))) / Math.pow(10.0, places) + "";
    }

    /**
     * Converts a serialized location to a Location. Returns null if string is
     * empty
     *
     * @param s
     *            - serialized location in format "world:x:y:z:yaw:pitch"
     * @return Location
     */
    static public Location getLocationString(final String s) {
        if (s == null || s.trim() == "") {
            return null;
        }
        final String[] parts = s.split(":");
        if (parts.length == 6) {
            final World w = Bukkit.getServer().getWorld(parts[0]);
            if (w == null) {
                return null;
            }
            final double x = Double.valueOf(parts[1].replace('_', '.'));
            final double y = Double.valueOf(parts[2].replace('_', '.'));
            final double z = Double.valueOf(parts[3].replace('_', '.'));
            final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4].replace('_', '.')));
            final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5].replace('_', '.')));
            return new Location(w, x, y, z, yaw, pitch);
        } else {
            Bukkit.getLogger().severe("Format of location string is wrong!");
        }
        return null;
    }

    /**
     * Runs commands for a player or on a player
     * @param player
     * @param commands
     */
    public void runCommands(Player player, List<String> commands) {
        for (String cmd : commands) {
            if (cmd.startsWith("[SELF]")) {
                getLogger().info("Running command '" + cmd + "' as " + player.getName());
                cmd = cmd.substring(6,cmd.length()).replace("[player]", player.getName()).trim();
                try {
                    player.performCommand(cmd);
                } catch (Exception e) {
                    getLogger().severe("Problem executing island command executed by player - skipping!");
                    getLogger().severe("Command was : " + cmd);
                    getLogger().severe("Error was: " + e.getMessage());
                    e.printStackTrace();
                }

                continue;
            }
            // Substitute in any references to player
            try {
                if (!getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.replace("[player]", player.getName()))) {
                    getLogger().severe("Problem executing challenge reward commands - skipping!");
                    getLogger().severe("Command was : " + cmd);
                }
            } catch (Exception e) {
                getLogger().severe("Problem executing challenge reward commands - skipping!");
                getLogger().severe("Command was : " + cmd);
                getLogger().severe("Error was: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Gives player item rewards
     * @param player
     * @param itemRewards
     * @return a list of what was given to the player
     */
    public List<ItemStack> giveItems(Player player, List<String> itemRewards) {
        List<ItemStack> rewardedItems = new ArrayList<ItemStack>();
        Material rewardItem;
        int rewardQty;
        // Build the item stack of rewards to give the player
        for (final String s : itemRewards) {
            final String[] element = s.split(":");
            if (element.length == 2) {
                try {
                    rewardItem = Material.getMaterial(element[0].toUpperCase());
                    rewardQty = Integer.parseInt(element[1]);
                    ItemStack item = new ItemStack(rewardItem, rewardQty);
                    rewardedItems.add(item);
                    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack[] { item });
                    if (!leftOvers.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
                    }                  
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);                    
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + Lang.errorError);
                    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " as reward!");
                    String materialList = "";
                    boolean hint = false;
                    for (Material m : Material.values()) {
                        materialList += m.toString() + ",";
                        if (element[0].length() > 3) {
                            if (m.toString().startsWith(element[0].substring(0, 3))) {
                                plugin.getLogger().severe("Did you mean " + m.toString() + "?");
                                hint = true;
                            }
                        }
                    }
                    if (!hint) {
                        plugin.getLogger().severe("Sorry, I have no idea what " + element[0] + " is. Pick from one of these:");
                        plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
                    }
                }
            } else if (element.length == 3) {
                try {
                    rewardItem = Material.getMaterial(element[0].toUpperCase());
                    rewardQty = Integer.parseInt(element[2]);                    
                    // Check for POTION
                    if (rewardItem.equals(Material.POTION)) {
                        givePotion(player, rewardedItems, element, rewardQty);
                    } else {
                        ItemStack item = null;
                        int rewMod = Integer.parseInt(element[1]);
                        item = new ItemStack(rewardItem, rewardQty, (short) rewMod);
                        if (item != null) {
                            rewardedItems.add(item);
                            final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(
                                    new ItemStack[] { item });
                            if (!leftOvers.isEmpty()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
                            }
                        }
                    }
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "There was a problem giving your reward. Ask Admin to check log!");
                    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " for challenge reward!");
                    String materialList = "";
                    boolean hint = false;
                    for (Material m : Material.values()) {
                        materialList += m.toString() + ",";
                        if (m.toString().startsWith(element[0].substring(0, 3))) {
                            plugin.getLogger().severe("Did you mean " + m.toString() + "? If so, put that in challenges.yml.");
                            hint = true;
                        }
                    }
                    if (!hint) {
                        plugin.getLogger().severe("Sorry, I have no idea what " + element[0] + " is. Pick from one of these:");
                        plugin.getLogger().severe(materialList.substring(0, materialList.length() - 1));
                    }
                    //}
                    return null;
                }
            } else if (element.length == 6) {
                //plugin.getLogger().info("DEBUG: 6 element reward");
                // Potion format = POTION:name:level:extended:splash:qty
                try {
                    rewardItem = Material.getMaterial(element[0].toUpperCase());
                    rewardQty = Integer.parseInt(element[5]);
                    // Check for POTION
                    if (rewardItem.equals(Material.POTION)) {
                        givePotion(player, rewardedItems, element, rewardQty);
                    }
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "There was a problem giving your reward. Ask Admin to check log!");
                    plugin.getLogger().severe("Problem with reward potion: " + s);
                    plugin.getLogger().severe("Format POTION:NAME:<LEVEL>:<EXTENDED>:<SPLASH/LINGER>:QTY");
                    plugin.getLogger().severe("LEVEL, EXTENDED and SPLASH are optional");
                    plugin.getLogger().severe("LEVEL is a number");
                    plugin.getLogger().severe("Examples:");
                    plugin.getLogger().severe("POTION:STRENGTH:1:EXTENDED:SPLASH:1");
                    plugin.getLogger().severe("POTION:JUMP:2:NOTEXTENDED:NOSPLASH:1");
                    plugin.getLogger().severe("POTION:WEAKNESS:::::1   -  any weakness potion");
                    plugin.getLogger().severe("Available names are:");
                    String potionNames = "";
                    for (PotionType p : PotionType.values()) {
                        potionNames += p.toString() + ", ";
                    }
                    plugin.getLogger().severe(potionNames.substring(0, potionNames.length()-2));
                    return null;
                }
            }
        }
        return rewardedItems;
    }

    private void givePotion(Player player, List<ItemStack> rewardedItems, String[] element, int rewardQty) {
        ItemStack item = getPotion(element, rewardQty);
        rewardedItems.add(item);
        final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(item);
        if (!leftOvers.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
        }
    }

    /**
     * Converts a serialized potion to a ItemStack of that potion
     * @param element
     * @param rewardQty
     * @param configFile that is being used
     * @return ItemStack of the potion
     */
    public static ItemStack getPotion(String[] element, int rewardQty) {
        // Check for potion aspects
        boolean splash = false;
        boolean extended = false;
        boolean linger = false;
        int level = 1;

        if (element.length > 2) {
            // Add level etc.
            if (!element[2].isEmpty()) {
                try {
                    level = Integer.valueOf(element[2]);
                } catch (Exception e) {
                    level = 1;
                }
            }
        }
        if (element.length > 3) {
            //plugin.getLogger().info("DEBUG: level = " + Integer.valueOf(element[2]));
            if (element[3].equalsIgnoreCase("EXTENDED")) {
                //plugin.getLogger().info("DEBUG: Extended");
                extended = true;
            }
        }
        if (element.length > 4) {
            if (element[4].equalsIgnoreCase("SPLASH")) {
                //plugin.getLogger().info("DEBUG: splash");
                splash = true;                
            }
            if (element[4].equalsIgnoreCase("LINGER")) {
                //plugin.getLogger().info("DEBUG: linger");
                linger = true;
            } 
        }

        // 1.9
        try {
            ItemStack result = new ItemStack(Material.POTION, rewardQty);
            if (splash) {
                result = new ItemStack(Material.SPLASH_POTION, rewardQty);
            }
            if (linger) {
                result = new ItemStack(Material.LINGERING_POTION, rewardQty);
            }
            PotionMeta potionMeta = (PotionMeta) result.getItemMeta();
            try {
                PotionData potionData = new PotionData(PotionType.valueOf(element[1].toUpperCase()), extended, level > 1 ? true: false);
                potionMeta.setBasePotionData(potionData); 
            } catch (IllegalArgumentException iae) {
                Bukkit.getLogger().severe("Potion parsing problem with " + element[1] +": " + iae.getMessage());
                potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
            }
            result.setItemMeta(potionMeta);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().severe("Potion effect '" + element[1] + "' is unknown - skipping!");
            Bukkit.getLogger().severe("Use one of the following:");
            for (PotionType name : PotionType.values()) {
                Bukkit.getLogger().severe(name.name());
            }
            return new ItemStack(Material.POTION, rewardQty);
        } 
    }


    /**
     * @return the nameStore
     */
    public TinyDB getNameStore() {
        return nameStore;
    }


    /**
     * @return the teleportListener
     */
    public PlayerTeleportListener getTeleportListener() {
        return teleportListener;
    }

    /**
     * Reloads the world after it has been unloaded.
     */
    public void reloadBeaconzWorld() {
        beaconzWorld = null;
        getBeaconzWorld();
    }

    /**
     * Sends a message to the command sender
     * Allows for null senders and messages
     * @param sender
     * @param msg
     * @return
     */
    public Boolean senderMsg(CommandSender sender, String msg) {
        if (sender!=null && msg!=null && !msg.isEmpty()) {
            sender.sendMessage(msg);
            return true;
        } else {
            return false;
        }
    }   
}
