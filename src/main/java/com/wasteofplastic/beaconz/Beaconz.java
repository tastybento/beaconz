/*
 * Copyright (c) 2015 - 2025 tastybento
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

import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.game.GameMgr;
import com.wasteofplastic.beaconz.game.Register;
import com.wasteofplastic.beaconz.generator.BeaconPopulator;
import com.wasteofplastic.beaconz.integration.Metrics;
import com.wasteofplastic.beaconz.storage.BeaconzStore;
import com.wasteofplastic.beaconz.storage.Messages;
import com.wasteofplastic.beaconz.storage.TinyDB;
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
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.beaconz.config.Params.GameMode;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;
import com.wasteofplastic.beaconz.commands.AdminCmdHandler;
import com.wasteofplastic.beaconz.commands.CmdHandler;
import com.wasteofplastic.beaconz.integration.dynmap.OurServerListener;
import com.wasteofplastic.beaconz.generator.BeaconzChunkGen;
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

/**
 * Main plugin class for the Beaconz strategic team-based game.
 * <p>
 * Beaconz is a Minecraft mini-game where teams compete to control beacons scattered
 * across a custom-generated world. Teams can:
 * <ul>
 *   <li>Capture beacons by breaking obsidian capstones</li>
 *   <li>Link beacons together to form strategic connections</li>
 *   <li>Create triangular fields that control territory</li>
 *   <li>Mine beacons for resources</li>
 *   <li>Build defenses to protect their beacons</li>
 * </ul>
 *
 * <h3>Architecture:</h3>
 * The plugin manages several key components:
 * <ul>
 *   <li><b>GameMgr:</b> Manages multiple game instances</li>
 *   <li><b>Register:</b> Tracks all beacons, links, and triangular fields</li>
 *   <li><b>BeaconzWorld:</b> Custom world with beacon generation</li>
 *   <li><b>BeaconzStore:</b> Handles inventory persistence across games</li>
 *   <li><b>Messages:</b> Queues messages for offline players</li>
 * </ul>
 *
 * <h3>Lifecycle:</h3>
 * <ol>
 *   <li><b>onLoad():</b> Create chunk generator</li>
 *   <li><b>onEnable():</b> Load config, create world, register listeners</li>
 *   <li><b>onDisable():</b> Save all data (register, inventories, games)</li>
 * </ol>
 *
 * @author tastybento
 * @since 1.0
 */
public class Beaconz extends JavaPlugin {
    /** The beacon register that tracks all beacons, links, and triangular fields */
    private Register register;

    /** The custom Beaconz world where games take place */
    private World beaconzWorld;

    /** Singleton instance of the plugin */
    private static Beaconz plugin;

    /** Custom chunk generator for creating the Beaconz world with beacons */
    private ChunkGenerator chunkGenerator;

    /** Block populator responsible for placing beacons during world generation */
    private BlockPopulator beaconPopulator;

    /** Game manager that handles multiple game instances */
    private GameMgr gameMgr;

    /** Message queue system for delivering messages to offline players */
    private Messages messages;

    /** Inventory storage system for persisting player inventories across games */
    private BeaconzStore beaconzStore;

    /** Player movement listener that handles triangle field effects */
    protected PlayerMovementListener pml;

    /** Player name database for offline player lookups */
    private TinyDB nameStore;

    /** Teleport listener for managing safe teleportation */
    private PlayerTeleportListener teleportListener;

    /**
     * Called when the plugin is loaded (before worlds are loaded).
     * <p>
     * This is the earliest point in the plugin lifecycle. We use it to:
     * <ol>
     *   <li>Store the singleton plugin instance for global access</li>
     *   <li>Create the chunk generator (must exist before world loads)</li>
     * </ol>
     *
     * The chunk generator is created here because it must be available when
     * the Beaconz world is loaded by the server.
     */
    @Override
    public void onLoad() {
        // Store singleton instance for static access across the plugin
        plugin = this;

        // Create the chunk generator for the Beaconz world
        // This must happen in onLoad() before worlds are loaded
        chunkGenerator = new BeaconzChunkGen(this);
    }

    /**
     * Called when the plugin is enabled (after all worlds are loaded).
     * <p>
     * This is the main initialization method that sets up the entire plugin:
     * <ol>
     *   <li>Load configuration from config.yml</li>
     *   <li>Register commands (/beaconz and /badmin)</li>
     *   <li>Initialize metrics tracking</li>
     *   <li>Start player name database</li>
     *   <li>Schedule delayed initialization (1 tick later)</li>
     * </ol>
     *
     * <h3>Delayed Initialization (1 tick later):</h3>
     * Most initialization happens 1 tick after enable to ensure all plugins
     * and worlds are fully loaded. This includes:
     * <ul>
     *   <li>Creating/loading the Beaconz world</li>
     *   <li>Initializing the game manager and lobby</li>
     *   <li>Loading the beacon register from disk</li>
     *   <li>Registering all event listeners</li>
     *   <li>Loading player message queues</li>
     *   <li>Hooking into Dynmap (if enabled)</li>
     *   <li>Creating the default game</li>
     * </ul>
     */
    @Override
    public void onEnable() {
        // INITIALIZATION PHASE 1: Configuration and Commands

        // Save the default config from the jar if it doesn't exist
        saveDefaultConfig();

        // Load configuration values into Settings class
        loadConfig();

        // Register player commands
        getCommand("beaconz").setExecutor(new CmdHandler(this));

        // Register admin commands
        getCommand("badmin").setExecutor(new AdminCmdHandler(this));

        // INITIALIZATION PHASE 2: Services

        // Initialize bStats metrics tracking
        try {
            final Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (final IOException localIOException) {
            // Metrics failure is not critical - silently continue
        }

        // Start the player name database for offline player lookups
        nameStore = new TinyDB(this);

        // INITIALIZATION PHASE 3: Delayed Setup (1 tick later)
        // Schedule most initialization for 1 tick after enable to ensure
        // all plugins and worlds are fully loaded by the server
        getServer().getScheduler().runTask(this, () -> {
            
            // Create the game manager and lobby region
            gameMgr = new GameMgr(plugin);

            // Load the beacon register from disk (or create new if first run)
            getRegister();
            
            // Create the block populator for beacon generation
            getBp();

            // Create/load the Beaconz world
            getBeaconzWorld();

            // Create the inventory storage system
            beaconzStore = new BeaconzStore(plugin);

            // LISTENER REGISTRATION
            // Register all event listeners in order of typical execution

            // Beacon interaction listeners
            getServer().getPluginManager().registerEvents(new BeaconLinkListener(plugin), plugin);
            getServer().getPluginManager().registerEvents(new BeaconCaptureListener(plugin), plugin);

            // Communication listener
            getServer().getPluginManager().registerEvents(new ChatListener(plugin), plugin);

            // Beacon defense listeners
            getServer().getPluginManager().registerEvents(new BeaconPassiveDefenseListener(plugin), plugin);
            getServer().getPluginManager().registerEvents(new BeaconProjectileDefenseListener(plugin), plugin);
            getServer().getPluginManager().registerEvents(new BeaconProtectionListener(plugin), plugin);

            // Player lifecycle listeners
            getServer().getPluginManager().registerEvents(new PlayerDeathListener(plugin), plugin);
            getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(plugin), plugin);

            // Player movement listener (stored for triangle effect access)
            pml = new PlayerMovementListener(plugin);
            getServer().getPluginManager().registerEvents(pml, plugin);

            // Teleport listener (stored for reference)
            teleportListener = new PlayerTeleportListener(plugin);
            getServer().getPluginManager().registerEvents(teleportListener, plugin);

            // World/environment listeners
            getServer().getPluginManager().registerEvents(new SkyListeners(plugin), plugin);
            getServer().getPluginManager().registerEvents(new BeaconSurroundListener(plugin), plugin);

            // Lobby listener for sign-based game joining
            getServer().getPluginManager().registerEvents(new LobbyListener(plugin), plugin);

            // Load player message queues
            messages = new Messages(plugin);

            // OPTIONAL: Dynmap Integration
            if (Settings.useDynmap) {
                PluginManager pm = getServer().getPluginManager();
                Plugin dynmap = pm.getPlugin("dynmap");
                if(dynmap != null) {
                    getLogger().info("Hooking into dynmap.");
                    getServer().getPluginManager().registerEvents(new OurServerListener(plugin, dynmap), plugin);
                }
            }

            // GAME CREATION
            // Create the default game if no games exist (first run)
            if (gameMgr.getGames().isEmpty()) {
                gameMgr.newGame(Settings.defaultGameName);
            }

        });
    }

    /**
     * Called when the plugin is disabled (server shutdown or plugin reload).
     * <p>
     * This method performs critical cleanup and data persistence:
     * <ol>
     *   <li>Save beacon register (all beacons, links, triangles)</li>
     *   <li>Remove map renderers to prevent memory leaks</li>
     *   <li>Save all player inventories</li>
     *   <li>Save all game states</li>
     * </ol>
     *
     * All data is saved to disk to ensure no progress is lost when the
     * server restarts or the plugin is reloaded.
     */
    @Override
    public void onDisable()
    {
        // Save beacon register (beacons, links, triangular fields)
        if (register != null) {
            register.saveRegister();

            // Remove all custom map renderers to prevent memory leaks
            // Maps will get their renderers back when players rejoin
            register.removeMapRenderers();
        }

        // Save all player inventories to disk
        if (beaconzStore != null) {
            beaconzStore.saveInventories();
        }

        // Save all game states (teams, scores, configurations)
        getGameMgr().saveAllGames();
    }


    /**
     * Gets the game manager instance, creating it if it doesn't exist.
     * <p>
     * The GameMgr handles:
     * <ul>
     *   <li>Multiple concurrent game instances</li>
     *   <li>Lobby region management</li>
     *   <li>Player-to-game assignment</li>
     *   <li>Game lifecycle (creation, start, end)</li>
     * </ul>
     *
     * This lazy initialization ensures the game manager exists even if
     * accessed before onEnable completes.
     *
     * @return The game manager instance
     */
    public GameMgr getGameMgr() {
        if (gameMgr == null) {
            gameMgr = new GameMgr(plugin);
        }
        return gameMgr;
    }

    /**
     * Gets the beacon register instance, creating and loading it if it doesn't exist.
     * <p>
     * The Register tracks all game data:
     * <ul>
     *   <li>All beacons in the world</li>
     *   <li>Links between beacons</li>
     *   <li>Triangular field territories</li>
     *   <li>Defense blocks</li>
     *   <li>Beacon maps</li>
     * </ul>
     *
     * On first access, the register is loaded from disk. If no saved data exists
     * (first run), an empty register is created.
     *
     * @return The beacon register instance
     */
    public Register getRegister() {
        if (register == null) {
            register = new Register(plugin);
            // Load saved data from disk (or create empty if first run)
            register.loadRegister();
        }
        return register;
    }

    /**
     * Gets the beacon populator instance, creating it if it doesn't exist.
     * <p>
     * The BeaconPopulator is responsible for placing beacons during chunk generation.
     * It uses a grid-based distribution system to ensure beacons are evenly spaced
     * across the world.
     *
     * @return The beacon populator instance
     */
    public BlockPopulator getBp() {
        if (beaconPopulator == null) {
            beaconPopulator = new BeaconPopulator(this);
        }
        return beaconPopulator;
    }

    /**
     * Gets the player movement listener instance.
     * <p>
     * The PlayerMovementListener tracks players moving through triangular fields
     * and applies/removes triangle-based potion effects. It's stored as a field
     * so other classes can access the triangle effect tracking.
     *
     * @return The player movement listener instance
     */
    public PlayerMovementListener getPml() {
        return pml;
    }


    /**
     * Loads all configuration settings from config.yml into the Settings class.
     * <p>
     * This method reads and validates all game configuration including:
     * <ul>
     *   <li><b>General:</b> Game mode, team chat, locking blocks, teleport delay</li>
     *   <li><b>World:</b> World name, distribution, distance, seed, game center coordinates</li>
     *   <li><b>Lobby:</b> Location, radius, height, blocks, spawn settings</li>
     *   <li><b>Teams:</b> Default number of teams, team colors</li>
     *   <li><b>Links:</b> Max links, link limit, link blocks, experience distance, rewards</li>
     *   <li><b>Mining:</b> Experience cost, cooldown, exhaustion chance, penalties, rewards</li>
     *   <li><b>Defense:</b> Defense height, level requirements for building/attacking</li>
     *   <li><b>Triangles:</b> Enemy and friendly field effects by depth</li>
     *   <li><b>Scoreboard:</b> Goals, timers, score types for each game mode</li>
     * </ul>
     *
     * <h3>Configuration Validation:</h3>
     * The method performs validation and correction:
     * <ul>
     *   <li>Ensures percentages are 0-100</li>
     *   <li>Ensures positive values where required</li>
     *   <li>Aligns coordinates to chunk/region boundaries</li>
     *   <li>Fills missing level requirements with previous values</li>
     *   <li>Validates material names and potion effect names</li>
     * </ul>
     *
     * Invalid or missing values are replaced with sensible defaults and warnings
     * are logged for administrator attention.
     */
    @SuppressWarnings("deprecation")
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
        Lang locale = new Lang(this);
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
        Settings.linkBlocks = new HashMap<>();
        if (getConfig().contains("links.linkblocks")) {
            for (String material: getConfig().getConfigurationSection("links.linkblocks").getKeys(false)) {
                try {
                    Material mat = Material.getMaterial(material.toUpperCase());
                    if (mat != null) {
                        int value = getConfig().getInt("links.linkblocks." + material);
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
        Settings.gamemode = (GameMode) getConfig().get("general.gamemode", GameMode.STRATEGY);

        // Get the default score types to show
        Settings.minigameScoreTypes = (List<GameScoreGoal>) getConfig().getList("scoreboard.sidebar.minigame", List.of(GameScoreGoal.BEACONS, GameScoreGoal.LINKS, GameScoreGoal.TRIANGLES));
        Settings.strategyScoreTypes = (List<GameScoreGoal>) getConfig().getList("scoreboard.sidebar.strategy", List.of(GameScoreGoal.AREA, GameScoreGoal.TRIANGLES));

        // Get the default goals for each game mode
        String mgGoal = getConfig().getString("scoreboard.goal.minigame", "triangles:0");
        String sgGoal = getConfig().getString("scoreboard.goal.strategy", "area:3000000");
        Settings.minigameGoal = GameScoreGoal.valueOf(mgGoal.split(":")[0].toUpperCase());
        Settings.strategyGoal = GameScoreGoal.valueOf(sgGoal.split(":")[0].toUpperCase());
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
            Settings.defenseLevels = new ArrayList<>();
            // Zero the index
            for (int i = 0; i < Settings.defenseHeight; i++) {
                Settings.defenseLevels.add(0);
            }
            // Load from the config
            for (String level : getConfig().getConfigurationSection("defense.defenselevel").getValues(false).keySet()) {
                try {
                    int index = Integer.parseInt(level) - 1;
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
            Settings.attackLevels = new ArrayList<>();
            // Zero the index
            for (int i = 0; i < Settings.defenseHeight; i++) {
                Settings.attackLevels.add(0);
            }
            // Load from the config
            for (String level : getConfig().getConfigurationSection("defense.attacklevel").getValues(false).keySet()) {
                try {
                    int index = Integer.parseInt(level) - 1;
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
        Settings.distribution = getConfig().getDouble("world.distribution", 0.1D);
        if (Settings.distribution < 0.001D) {
            Settings.distribution = 0.001D;
        }
        // Distance should always be a multiple of 512
        int alignment = 512;
        Settings.gameDistance = (getConfig().getInt("world.distance", 20000) + (alignment - 1)) & ~(alignment - 1);
        // Place center of the game based on gameDistance and region boundaries        
        Settings.xCenter = getConfig().getInt("world.xcenter", Settings.gameDistance + alignment);
        Settings.zCenter = getConfig().getInt("world.zcenter", Settings.gameDistance + alignment);
        Settings.seedAdjustment = getConfig().getLong("world.seedadjustment", System.currentTimeMillis());
        Settings.mineCoolDown = getConfig().getInt("mining.minecooldown", 1) * 60000L; // Minutes in millis
        ConfigurationSection enemyFieldSection = getConfig().getConfigurationSection("triangles.enemyfieldeffects");
        // Step through the numbers
        Settings.enemyFieldEffects = new HashMap<>();
        for (Entry<String, Object> part : enemyFieldSection.getValues(false).entrySet()) {
            if (NumberUtils.isNumber(part.getKey())) {
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<>();
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
                                // Adding enemy effect
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
        Settings.friendlyFieldEffects = new HashMap<>();
        ConfigurationSection friendlyFieldSection = getConfig().getConfigurationSection("triangles.friendlyfieldeffects");
        // Step through the numbers
        for (Entry<String, Object> part : friendlyFieldSection.getValues(false).entrySet()) {
            if (NumberUtils.isNumber(part.getKey())) {
                // It is a number, now get the string list
                List<PotionEffect> effects = new ArrayList<>();
                List<String> effectsList = getConfig().getStringList("triangles.friendlyfieldeffects." + part.getKey());
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
     * @param item the string to parse
     * @return the ItemStack or null if error
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
     * Gets the Beaconz world instance, creating it if it doesn't exist.
     * <p>
     * The Beaconz world is a custom-generated world featuring:
     * <ul>
     *   <li>Grid-distributed beacons across the landscape</li>
     *   <li>Custom terrain generation via {@link BeaconzChunkGen}</li>
     *   <li>Configurable seed and world center</li>
     *   <li>Normal environment but with beacon structures</li>
     * </ul>
     *
     * On first access, the world is created using:
     * <ul>
     *   <li>World name from Settings.worldName</li>
     *   <li>Seed from Settings.seedAdjustment</li>
     *   <li>Custom chunk generator (BeaconzChunkGen)</li>
     * </ul>
     *
     * Note: This method can be called asynchronously during chunk generation,
     * so it cannot modify world properties that require the main thread.
     *
     * @return The Beaconz world, or null if world creation fails
     */
    public World getBeaconzWorld() {
        // Check if world is already loaded
        if (beaconzWorld == null) {
            // World doesn't exist yet - create it
            getLogger().info("World is '" + Settings.worldName + "'");
            try {
                beaconzWorld = WorldCreator
                        .name(Settings.worldName)
                        .type(WorldType.NORMAL)
                        .environment(World.Environment.NORMAL)
                        .seed(Settings.seedAdjustment)
                        .generator(chunkGenerator)  // Custom beacon world generator
                        .createWorld();
            } catch (Exception e) {
                // World creation failed (likely during async chunk generation)
                getLogger().info("Could not make world yet..");
                return null;
            }
        }

        // Note: Cannot set spawn location here as this method may be called async
        // during chunk generation. Spawn location is set during onEnable.

        return beaconzWorld;
    }

    /**
     * Gets the inventory storage system.
     * <p>
     * BeaconzStore handles:
     * <ul>
     *   <li>Saving player inventories when switching games</li>
     *   <li>Restoring inventories when rejoining games</li>
     *   <li>Lobby inventory separate from game inventories</li>
     *   <li>Experience point persistence</li>
     *   <li>Health and hunger state</li>
     * </ul>
     *
     * @return The inventory storage instance
     */
    public BeaconzStore getBeaconzStore() {
        return beaconzStore;
    }


    /**
     * Gets the player message queue system.
     * <p>
     * Messages handles:
     * <ul>
     *   <li>Queuing messages for offline players</li>
     *   <li>Delivering messages when players log in</li>
     *   <li>Team broadcast messages</li>
     *   <li>Cross-game notifications</li>
     * </ul>
     *
     * @return The messages instance
     */
    public Messages getMessages() {
        return messages;
    }

    /**
     * Gets the teleport listener instance.
     * <p>
     * The PlayerTeleportListener ensures safe teleportation by:
     * <ul>
     *   <li>Preventing teleports into unsafe locations</li>
     *   <li>Applying configured teleport delays</li>
     *   <li>Managing cross-world teleports</li>
     * </ul>
     *
     * @return The teleport listener instance
     */
    public PlayerTeleportListener getTeleportListener() {
        return teleportListener;
    }

    /**
     * Gets the player name database.
     * <p>
     * TinyDB stores player names mapped to UUIDs for:
     * <ul>
     *   <li>Showing beacon ownership when owners are offline</li>
     *   <li>Admin commands that reference players by name</li>
     *   <li>Team rosters and scoreboards</li>
     * </ul>
     *
     * @return The player name database instance
     */
    public TinyDB getNameStore() {
        return nameStore;
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
        boolean removestr = false;
        // Remove extraneous text
        for (String str : strToClean.split(":")) {
            removestr = true;
            for (String bv : basevalues.split(":")) {
                if (bv.equals(str)) {
                    removestr = false;
                    break;
                }
            }
            if (removestr) strToClean = strToClean.replace(str, "");
        }
        //Reassemble the string
        StringBuilder newString = new StringBuilder();
        for (String str: strToClean.split(":")) {
            if (!str.isEmpty()) newString.append(str).append(":");
        }
        if (!newString.isEmpty()) newString = new StringBuilder(newString.substring(0, newString.length() - 1));
        if (newString.isEmpty()) newString = new StringBuilder(defaultIfEmpty);
        return newString.toString();
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
        + ":" + Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
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
            final double x = Double.parseDouble(parts[1].replace('_', '.'));
            final double y = Double.parseDouble(parts[2].replace('_', '.'));
            final double z = Double.parseDouble(parts[3].replace('_', '.'));
            final float yaw = Float.intBitsToFloat(Integer.parseInt(parts[4].replace('_', '.')));
            final float pitch = Float.intBitsToFloat(Integer.parseInt(parts[5].replace('_', '.')));
            return new Location(w, x, y, z, yaw, pitch);
        } else {
            plugin.getLogger().severe("Format of location string is wrong!");
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
                cmd = cmd.substring(6).replace("[player]", player.getName()).trim();
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
        List<ItemStack> rewardedItems = new ArrayList<>();
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
                    final HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(item);
                    if (!leftOvers.isEmpty()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
                    }                  
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);                    
                } catch (Exception e) {
                    player.sendMessage(Lang.errorError);
                    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " as reward!");
                    StringBuilder materialList = new StringBuilder();
                    boolean hint = false;
                    for (Material m : Material.values()) {
                        materialList.append(m.toString()).append(",");
                        if (element[0].length() > 3) {
                            if (m.toString().startsWith(element[0].substring(0, 3))) {
                                plugin.getLogger().severe("Did you mean " + m + "?");
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
                                    item);
                            if (!leftOvers.isEmpty()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftOvers.get(0));
                            }
                        }
                    }
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "There was a problem giving your reward. Ask Admin to check log!");
                    plugin.getLogger().severe("Could not give " + element[0] + ":" + element[1] + " to " + player.getName() + " for challenge reward!");
                    StringBuilder materialList = new StringBuilder();
                    boolean hint = false;
                    for (Material m : Material.values()) {
                        materialList.append(m.toString()).append(",");
                        if (m.toString().startsWith(element[0].substring(0, 3))) {
                            plugin.getLogger().severe("Did you mean " + m + "? If so, put that in challenges.yml.");
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
                    StringBuilder potionNames = new StringBuilder();
                    for (PotionType p : PotionType.values()) {
                        potionNames.append(p.toString()).append(", ");
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
     * @param element - array of potion parameters
     * @param rewardQty - quantity of potions
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
                    level = Integer.parseInt(element[2]);
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
                PotionData potionData = new PotionData(PotionType.valueOf(element[1].toUpperCase()), extended, level > 1);
                potionMeta.setBasePotionData(potionData); 
            } catch (IllegalArgumentException iae) {
                plugin.getLogger().severe("Potion parsing problem with " + element[1] +": " + iae.getMessage());
                potionMeta.setBasePotionData(new PotionData(PotionType.WATER));
            }
            result.setItemMeta(potionMeta);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Potion effect '" + element[1] + "' is unknown - skipping!");
            plugin.getLogger().severe("Use one of the following:");
            for (PotionType name : PotionType.values()) {
                plugin.getLogger().severe(name.name());
            }
            return new ItemStack(Material.POTION, rewardQty);
        } 
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
     *
     * @param sender sender
     * @param msg message
     */
    public void senderMsg(CommandSender sender, String msg) {
        if (sender!=null && msg!=null && !msg.isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    /**
     * Supply the default chunk generator for Beaconz world
     * @return the chunk generator
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (chunkGenerator == null) {
            chunkGenerator = new BeaconzChunkGen(this);
        }
        return chunkGenerator;
    }
}
