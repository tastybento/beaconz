package com.wasteofplastic.beaconz;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

public class Beaconz extends JavaPlugin {
    BeaconPopulator beaconPop;
    Register register;
    static World beaconzWorld;
    private static Beaconz plugin;
    static BlockPopulator bp;
    private Scorecard scorecard;

    @Override
    public void onEnable() {
        plugin = this;
        // Save the default config from the jar
        saveDefaultConfig();
        loadConfig();

        //getServer().getPluginManager().registerEvents(new WorldLoader(this), this);

        // Register command(s)
        getCommand("beaconz").setExecutor(new CmdHandler(this));

        // Create the block populator
        bp = new BeaconPopulator(this);

        // Run commands that need to be run 1 tick after start
        getServer().getScheduler().runTask(this, new Runnable() {

            public void run() {
            Beaconz.getBeaconzWorld();
            // Load the scorecard
            scorecard = new Scorecard(plugin);
            // Add teams
            MaterialData teamBlock = new MaterialData(Material.STAINED_GLASS,(byte) 11);
            scorecard.addTeam("Blue", teamBlock);
            teamBlock = new MaterialData(Material.STAINED_GLASS,(byte) 14);
            scorecard.addTeam("Red", teamBlock);

            // Load the beacon register
            register = new Register(plugin);
            register.loadRegister();

            // Load the teams
            scorecard.loadTeamMembers();

            // Register the listeners - block break etc.
            BeaconListeners ev = new BeaconListeners(plugin);
            getServer().getPluginManager().registerEvents(ev, plugin);
        }});
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
    return bp;
    }

    /**
     * @return the scorecard
     */
    public Scorecard getScorecard() {
    return scorecard;
    }

    /**
     * Loads settings from config.yml
     */
    @SuppressWarnings("deprecation")
    public void loadConfig() {
        Settings.worldName = getConfig().getString("world.name", "beaconz");
        Settings.distribution = getConfig().getDouble("world.distribution", 0.05D);
        Settings.size = getConfig().getInt("world.size",0);
        Settings.xCenter = getConfig().getInt("world.xcenter",0);
        Settings.zCenter = getConfig().getInt("world.zcenter",0);
        Settings.randomSpawn = getConfig().getBoolean("world.randomspawn", true);
        Settings.seedAdjustment = getConfig().getLong("world.seedadjustment", 0);
        Settings.hackCoolDown = getConfig().getInt("world.hackcooldown", 1) * 60000; // Minutes in millis
        Settings.overHackEffects = getConfig().getStringList("world.overhackeffects");
        List<String> goodies = getConfig().getStringList("world.enemygoodies");
        Settings.enemyGoodies.clear();
        for (String goodie: goodies) {
            String[] split = goodie.split(":");
            int lastChance = 0;
            if (!Settings.enemyGoodies.isEmpty() ) {
                lastChance = Settings.enemyGoodies.lastKey();
            }
            // No durability option - Material/#:Qty:Chance
            if (split.length == 3) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[2])) {
                    chance = NumberUtils.toInt(split[2]) + lastChance;
                }
                // Get the qty
                int qty = 0;
                if (NumberUtils.isNumber(split[1])) {
                    qty = NumberUtils.toInt(split[1]);
                }
                // Try to get #
                if (NumberUtils.isNumber(split[0])) {
                    int itemType = NumberUtils.toInt(split[0]);
                    ItemStack item = new ItemStack(itemType, qty);
                    Settings.enemyGoodies.put(chance, item);
                } else {
                    try {
                        Material itemType = Material.valueOf(split[0]);
                        ItemStack item = new ItemStack(itemType, qty);
                        Settings.enemyGoodies.put(chance, item);
                    } catch (Exception e) {
                        getLogger().severe("Could not parse enemy beacon goodie material " + split[0] + " skipping...");
                    }
                }
            } else if (split.length == 4) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[3])) {
                    chance = NumberUtils.toInt(split[2]) + lastChance;
                }
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
                    Settings.enemyGoodies.put(chance, item);
                } else {
                    try {
                        Material itemType = Material.valueOf(split[0]);
                        ItemStack item = new ItemStack(itemType, qty);
                        item.setDurability((short)durability);
                    Settings.enemyGoodies.put(chance, item);
                    } catch (Exception e) {
                        getLogger().severe("Could not parse enemy beacon goodie material " + split[0] + " skipping...");
                    }
                }
            }

        }
        // Add the final element - any number above this value will return a null element
        Settings.enemyGoodies.put(Settings.enemyGoodies.lastKey() + 1, null);
        // Debug
        for (Entry<Integer, ItemStack> ent : Settings.enemyGoodies.entrySet()) {
            plugin.getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
        }
        // Team goodies
        goodies = getConfig().getStringList("world.teamgoodies");
        Settings.teamGoodies.clear();
        for (String goodie: goodies) {
            String[] split = goodie.split(":");
            int lastChance = 0;
            if (!Settings.teamGoodies.isEmpty() ) {
                lastChance = Settings.teamGoodies.lastKey();
            } // No durability option - Material/#:Qty:Chance
            if (split.length == 3) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[2])) {
                    chance = NumberUtils.toInt(split[2]) + lastChance;
                }
                // Get the qty
                int qty = 0;
                if (NumberUtils.isNumber(split[1])) {
                    qty = NumberUtils.toInt(split[1]);
                }
                // Try to get #
                if (NumberUtils.isNumber(split[0])) {
                    int itemType = NumberUtils.toInt(split[0]);
                    ItemStack item = new ItemStack(itemType, qty);
                    Settings.teamGoodies.put(chance, item);
                } else {
                    try {
                        Material itemType = Material.valueOf(split[0]);
                        ItemStack item = new ItemStack(itemType, qty);
                        Settings.teamGoodies.put(chance, item);
                    } catch (Exception e) {
                        getLogger().severe("Could not parse team beacon goodie material " + split[0] + " skipping...");
                    }
                }
            } else if (split.length == 4) {
                // Get chance
                int chance = 0;
                if (NumberUtils.isNumber(split[3])) {
                    chance = NumberUtils.toInt(split[2]) + lastChance;
                }
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
                    Settings.teamGoodies.put(chance, item);
                } else {
                    try {
                        Material itemType = Material.valueOf(split[0]);
                        ItemStack item = new ItemStack(itemType, qty);
                        item.setDurability((short)durability);
                        Settings.teamGoodies.put(chance, item);
                    } catch (Exception e) {
                        getLogger().severe("Could not parse team beacon goodie material " + split[0] + " skipping...");
                    }
                }
            }
        }

        // Add the final element - any number above this value will return a null element
        Settings.teamGoodies.put(Settings.teamGoodies.lastKey() + 1, null);
        // Own team
        for (Entry<Integer, ItemStack> ent : Settings.teamGoodies.entrySet()) {
            plugin.getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
        }
    }

    public static World getBeaconzWorld() {
        // Check to see if the world exists, and if not, make it
        if (beaconzWorld == null) {
            // World doesn't exist, so make it
            plugin.getLogger().info("World is '" + Settings.worldName + "'");
            beaconzWorld = WorldCreator.name(Settings.worldName).type(WorldType.NORMAL).environment(World.Environment.NORMAL).createWorld();
            beaconzWorld.getPopulators().add(bp);
        }
        beaconzWorld.setSpawnLocation(Settings.xCenter, beaconzWorld.getHighestBlockYAt(Settings.xCenter, Settings.zCenter), Settings.zCenter);
        beaconzWorld.getWorldBorder().setCenter(Settings.xCenter, Settings.zCenter);
        if (Settings.size > 0) {
            beaconzWorld.getWorldBorder().setSize(Settings.size);
        }
        return beaconzWorld;
    }
}
