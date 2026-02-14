/*
 * Copyright (c) 2015 - 2026 tastybento
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

package com.wasteofplastic.beaconz.commands.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Params.GameMode;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI handler for creating new games with customizable parameters.
 *
 * <p>Provides an interactive inventory-based interface for configuring:
 * <ul>
 *   <li>Game mode (Strategy/Minigame)</li>
 *   <li>Number of teams (2-16)</li>
 *   <li>Game goal (Area/Beacons/Links/Triangles)</li>
 *   <li>Score types to display</li>
 *   <li>Timer settings (days, hours, minutes)</li>
 * </ul>
 *
 * @author tastybento
 * @since 2.0
 */
public class NewGameGUI extends BeaconzPluginDependent implements Listener {

    private static final String GUI_TITLE = "New Game Configuration";
    private static final int GUI_SIZE = 54; // 6 rows

    // Slot positions
    private static final int SLOT_GAMEMODE = 10;
    private static final int SLOT_TEAMS = 12;
    private static final int SLOT_GOAL = 14;
    private static final int SLOT_SCORETYPES = 16;
    private static final int SLOT_SIZE = 19;
    private static final int SLOT_TIMER = 28;
    private static final int SLOT_TIMER_DAYS = 29;
    private static final int SLOT_TIMER_HOURS = 30;
    private static final int SLOT_TIMER_MINUTES = 31;
    private static final int SLOT_RESET = 48;
    private static final int SLOT_CREATE = 50;

    // Track active GUI sessions
    private final Map<UUID, GameSettings> activeSessions = new HashMap<>();
    private final Map<UUID, PendingGameCreation> pendingCreations = new HashMap<>();
    private final Set<UUID> navigatingToSubMenu = new HashSet<>();
    private NewGameCommand commandHandler;

    /**
     * Constructs a new GUI handler.
     *
     * @param plugin the Beaconz plugin instance
     */
    public NewGameGUI(Beaconz plugin) {
        super(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Sets the command handler (to avoid circular dependency).
     *
     * @param handler the command handler
     */
    public void setCommandHandler(NewGameCommand handler) {
        this.commandHandler = handler;
    }

    /**
     * Opens the game configuration GUI for a player.
     *
     * @param player the player to show the GUI to
     */
    public void openGUI(Player player) {
        GameSettings settings = new GameSettings();
        activeSessions.put(player.getUniqueId(), settings);

        Inventory gui = createGUI(settings);
        player.openInventory(gui);
    }

    /**
     * Reopens the main GUI with existing settings (used when returning from sub-menus).
     *
     * @param player the player
     * @param settings the existing settings to preserve
     */
    private void reopenMainGUI(Player player, GameSettings settings) {
        Inventory gui = createGUI(settings);
        player.openInventory(gui);
    }

    /**
     * Creates the GUI inventory with all configuration items.
     *
     * @param settings the current game settings
     * @return the populated inventory
     */
    private Inventory createGUI(GameSettings settings) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, Component.text(GUI_TITLE));

        // Game Mode toggle
        gui.setItem(SLOT_GAMEMODE, createGameModeItem(settings));

        // Teams selector
        gui.setItem(SLOT_TEAMS, createTeamsItem(settings));

        // Goal selector
        gui.setItem(SLOT_GOAL, createGoalItem(settings));

        // Score types selector
        gui.setItem(SLOT_SCORETYPES, createScoreTypesItem(settings));

        // Size selector
        gui.setItem(SLOT_SIZE, createSizeItem(settings));

        // Timer toggle
        gui.setItem(SLOT_TIMER, createTimerToggleItem(settings));

        // Timer values (only if timed)
        if (settings.isTimed()) {
            gui.setItem(SLOT_TIMER_DAYS, createTimerDaysItem(settings));
            gui.setItem(SLOT_TIMER_HOURS, createTimerHoursItem(settings));
            gui.setItem(SLOT_TIMER_MINUTES, createTimerMinutesItem(settings));
        }

        // Reset to defaults
        gui.setItem(SLOT_RESET, createResetItem());

        // Create game
        gui.setItem(SLOT_CREATE, createCreateItem());

        return gui;
    }

    /**
     * Creates the game mode toggle item.
     */
    private ItemStack createGameModeItem(GameSettings settings) {
        Material material = settings.gameMode == GameMode.STRATEGY
            ? Material.DIAMOND_SWORD : Material.GOLDEN_SWORD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Game Mode: " + settings.gameMode.getName())
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to toggle between:").color(NamedTextColor.GRAY));
        lore.add(Component.text("• Strategy").color(NamedTextColor.AQUA));
        lore.add(Component.text("• Minigame").color(NamedTextColor.AQUA));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the teams selector item.
     */
    private ItemStack createTeamsItem(GameSettings settings) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, settings.teams);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Number of Teams: " + settings.teams)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left-Click: +1 team").color(NamedTextColor.GREEN));
        lore.add(Component.text("Right-Click: -1 team").color(NamedTextColor.RED));
        lore.add(Component.text("Range: 2-16 teams").color(NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the goal selector item.
     */
    private ItemStack createGoalItem(GameSettings settings) {
        Material material = switch (settings.goal) {
            case AREA -> Material.MAP;
            case BEACONS -> Material.BEACON;
            case LINKS -> Material.LEAD;
            case TRIANGLES -> Material.NETHER_STAR;
            default -> Material.MAP; // fallback
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Game Goal: " + settings.goal.getName())
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to cycle through:").color(NamedTextColor.GRAY));
        lore.add(Component.text("• Area").color(NamedTextColor.AQUA));
        lore.add(Component.text("• Beacons").color(NamedTextColor.AQUA));
        lore.add(Component.text("• Links").color(NamedTextColor.AQUA));
        lore.add(Component.text("• Triangles").color(NamedTextColor.AQUA));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the score types selector item.
     */
    private ItemStack createScoreTypesItem(GameSettings settings) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Score Display Types")
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to toggle each:").color(NamedTextColor.GRAY));
        for (GameScoreGoal scoreType : GameScoreGoal.values()) {
            boolean enabled = settings.scoreTypes.contains(scoreType);
            Component line = Component.text("• " + scoreType.getName())
                .color(enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY);
            lore.add(line);
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click to open selector").color(NamedTextColor.YELLOW));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the size selector item.
     */
    private ItemStack createSizeItem(GameSettings settings) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Game Size: " + settings.size)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left-Click: +100").color(NamedTextColor.GREEN));
        lore.add(Component.text("Right-Click: -100").color(NamedTextColor.RED));
        lore.add(Component.text("Shift+Click: +/-1000").color(NamedTextColor.YELLOW));
        lore.add(Component.text("Region side length").color(NamedTextColor.GRAY));
        lore.add(Component.text("Recommended: 500-20000").color(NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the timer toggle item.
     */
    private ItemStack createTimerToggleItem(GameSettings settings) {
        Material material = settings.isTimed() ? Material.CLOCK : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String status = settings.isTimed() ? "Timed Game" : "Open-Ended";
        meta.displayName(Component.text("Timer: " + status)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (settings.isTimed()) {
            int totalSeconds = settings.getTimerSeconds();
            int days = totalSeconds / 86400;
            int hours = (totalSeconds % 86400) / 3600;
            int minutes = (totalSeconds % 3600) / 60;
            lore.add(Component.text("Duration: " + days + "d " + hours + "h " + minutes + "m")
                .color(NamedTextColor.AQUA));
        } else {
            lore.add(Component.text("No time limit").color(NamedTextColor.AQUA));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Click to toggle").color(NamedTextColor.YELLOW));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the timer days adjustment item.
     */
    private ItemStack createTimerDaysItem(GameSettings settings) {
        ItemStack item = new ItemStack(Material.SUNFLOWER, Math.max(1, settings.timerDays));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Days: " + settings.timerDays)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left-Click: +1 day").color(NamedTextColor.GREEN));
        lore.add(Component.text("Right-Click: -1 day").color(NamedTextColor.RED));
        lore.add(Component.text("Shift+Click: +/-10").color(NamedTextColor.YELLOW));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the timer hours adjustment item.
     */
    private ItemStack createTimerHoursItem(GameSettings settings) {
        ItemStack item = new ItemStack(Material.CLOCK, Math.max(1, settings.timerHours));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Hours: " + settings.timerHours)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left-Click: +1 hour").color(NamedTextColor.GREEN));
        lore.add(Component.text("Right-Click: -1 hour").color(NamedTextColor.RED));
        lore.add(Component.text("Range: 0-23 hours").color(NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the timer minutes adjustment item.
     */
    private ItemStack createTimerMinutesItem(GameSettings settings) {
        ItemStack item = new ItemStack(Material.REDSTONE, Math.max(1, settings.timerMinutes));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Minutes: " + settings.timerMinutes)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Left-Click: +1 minute").color(NamedTextColor.GREEN));
        lore.add(Component.text("Right-Click: -1 minute").color(NamedTextColor.RED));
        lore.add(Component.text("Shift+Click: +/-10").color(NamedTextColor.YELLOW));
        lore.add(Component.text("Range: 0-59 minutes").color(NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the reset to defaults item.
     */
    private ItemStack createResetItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Reset to Defaults")
            .color(NamedTextColor.RED)
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to reset all settings to:").color(NamedTextColor.GRAY));
        lore.add(Component.text("• Strategy mode").color(NamedTextColor.YELLOW));
        lore.add(Component.text("• 2 teams").color(NamedTextColor.YELLOW));
        lore.add(Component.text("• Size 20000").color(NamedTextColor.YELLOW));
        lore.add(Component.text("• Area goal").color(NamedTextColor.YELLOW));
        lore.add(Component.text("• Area score type").color(NamedTextColor.YELLOW));
        lore.add(Component.text("• Open-ended timer").color(NamedTextColor.YELLOW));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the create game item.
     */
    private ItemStack createCreateItem() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Create Game")
            .color(NamedTextColor.GREEN)
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to create the game").color(NamedTextColor.GRAY));
        lore.add(Component.text("with current settings").color(NamedTextColor.GRAY));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handles inventory click events.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        beaconzPlugin.getLogger().info(event.getEventName());
        if (!(event.getWhoClicked() instanceof Player player)) return;
        beaconzPlugin.getLogger().info("DEBUG: Player clicked in inventory");
        Component viewTitle = event.getView().title();

        // Check if it's the main GUI
        if (viewTitle.equals(Component.text(GUI_TITLE))) {
            beaconzPlugin.getLogger().info("DEBUG: Main GUI click detected");
            handleMainGUIClick(event, player);
            return;
        }

        // Check if it's the score types selector
        if (viewTitle.equals(Component.text("Select Score Types"))) {
            beaconzPlugin.getLogger().info("DEBUG: Select Score Types click detected");
            handleScoreTypeSelectorClick(event, player);
        }
    }

    /**
     * Handles clicks in the main GUI.
     */
    private void handleMainGUIClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        GameSettings settings = activeSessions.get(player.getUniqueId());
        if (settings == null) return;

        int slot = event.getRawSlot();

        // Only process clicks in the top inventory (slots 0-53)
        if (slot < 0 || slot >= GUI_SIZE) return;

        ClickType click = event.getClick();

        switch (slot) {
            case SLOT_GAMEMODE -> handleGameModeClick(player, settings);
            case SLOT_TEAMS -> handleTeamsClick(player, settings, click);
            case SLOT_GOAL -> handleGoalClick(player, settings);
            case SLOT_SCORETYPES -> handleScoreTypesClick(player, settings);
            case SLOT_SIZE -> handleSizeClick(player, settings, click);
            case SLOT_TIMER -> handleTimerToggleClick(player, settings);
            case SLOT_TIMER_DAYS -> handleTimerDaysClick(player, settings, click);
            case SLOT_TIMER_HOURS -> handleTimerHoursClick(player, settings, click);
            case SLOT_TIMER_MINUTES -> handleTimerMinutesClick(player, settings, click);
            case SLOT_RESET -> handleResetClick(player, settings);
            case SLOT_CREATE -> handleCreateClick(player, settings);
        }
    }

    /**
     * Handles clicks in the score types selector.
     */
    private void handleScoreTypeSelectorClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        GameSettings settings = activeSessions.get(player.getUniqueId());
        if (settings == null) return;

        int slot = event.getRawSlot();

        // Only process clicks in the top inventory (slots 0-26)
        if (slot < 0 || slot >= 27) return;

        // Back button
        if (slot == 22) {
            navigatingToSubMenu.add(player.getUniqueId());
            reopenMainGUI(player, settings);
            return;
        }
        // Score type toggles (slots 10, 12, 14, 16)
        GameScoreGoal[] goals = new GameScoreGoal[]{
            GameScoreGoal.AREA,
            GameScoreGoal.BEACONS,
            GameScoreGoal.LINKS,
            GameScoreGoal.TRIANGLES
        };
        for (int i = 0; i < goals.length; i++) {
            if (slot == 10 + i * 2) {
                GameScoreGoal goal = goals[i];
                if (settings.scoreTypes.contains(goal)) {
                    settings.scoreTypes.remove(goal);
                } else {
                    settings.scoreTypes.add(goal);
                }
                navigatingToSubMenu.add(player.getUniqueId());
                openScoreTypesSelector(player, settings);
                return;
            }
        }
    }

    /**
     * Handles inventory close events.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Component viewTitle = event.getView().title();

        // If closing main GUI and navigating to sub-menu, don't clear session
        if (viewTitle.equals(Component.text(GUI_TITLE))) {
            if (navigatingToSubMenu.remove(player.getUniqueId())) {
                // Keep session active when navigating to score types selector
                return;
            }
            // Otherwise, clear the session
            activeSessions.remove(player.getUniqueId());
        }
        // Note: We don't clear session when closing score types selector
        // because we want to keep it for when they return to main GUI
    }

    private void handleGameModeClick(Player player, GameSettings settings) {
        settings.gameMode = settings.gameMode == GameMode.STRATEGY ? GameMode.MINIGAME : GameMode.STRATEGY;
        refreshGUI(player, settings);
    }

    private void handleTeamsClick(Player player, GameSettings settings, ClickType click) {
        if (click.isLeftClick()) {
            settings.teams++;
            if (settings.teams > 16) settings.teams = 2;
        } else if (click.isRightClick()) {
            settings.teams--;
            if (settings.teams < 2) settings.teams = 16;
        }
        refreshGUI(player, settings);
    }

    private void handleGoalClick(Player player, GameSettings settings) {
        // Only cycle through the main goals (not TIME)
        GameScoreGoal[] goals = new GameScoreGoal[]{
            GameScoreGoal.AREA,
            GameScoreGoal.BEACONS,
            GameScoreGoal.LINKS,
            GameScoreGoal.TRIANGLES
        };
        int currentIndex = Arrays.asList(goals).indexOf(settings.goal);
        settings.goal = goals[(currentIndex + 1) % goals.length];
        refreshGUI(player, settings);
    }

    private void handleScoreTypesClick(Player player, GameSettings settings) {
        navigatingToSubMenu.add(player.getUniqueId());
        openScoreTypesSelector(player, settings);
    }

    private void handleSizeClick(Player player, GameSettings settings, ClickType click) {
        int change = click.isShiftClick() ? 1000 : 100;
        if (click.isLeftClick()) {
            settings.size += change;
        } else if (click.isRightClick()) {
            settings.size = Math.max(100, settings.size - change);
        }
        refreshGUI(player, settings);
    }

    private void handleTimerToggleClick(Player player, GameSettings settings) {
        settings.setTimed(!settings.isTimed());
        refreshGUI(player, settings);
    }

    private void handleTimerDaysClick(Player player, GameSettings settings, ClickType click) {
        int change = click.isShiftClick() ? 10 : 1;
        if (click.isLeftClick()) {
            settings.timerDays += change;
        } else if (click.isRightClick()) {
            settings.timerDays = Math.max(0, settings.timerDays - change);
        }
        refreshGUI(player, settings);
    }

    private void handleTimerHoursClick(Player player, GameSettings settings, ClickType click) {
        if (click.isLeftClick()) {
            settings.timerHours = (settings.timerHours + 1) % 24;
        } else if (click.isRightClick()) {
            settings.timerHours = (settings.timerHours - 1 + 24) % 24;
        }
        refreshGUI(player, settings);
    }

    private void handleTimerMinutesClick(Player player, GameSettings settings, ClickType click) {
        int change = click.isShiftClick() ? 10 : 1;
        if (click.isLeftClick()) {
            settings.timerMinutes = (settings.timerMinutes + change) % 60;
        } else if (click.isRightClick()) {
            settings.timerMinutes = (settings.timerMinutes - change + 60) % 60;
        }
        refreshGUI(player, settings);
    }

    private void handleResetClick(Player player, GameSettings settings) {
        settings.reset();
        refreshGUI(player, settings);
    }

    private void handleCreateClick(Player player, GameSettings settings) {
        player.closeInventory();
        if (commandHandler != null) {
            commandHandler.createGameFromGUI(player, settings);
        }
    }

    private void openScoreTypesSelector(Player player, GameSettings settings) {
        Inventory selector = Bukkit.createInventory(null, 27, Component.text("Select Score Types"));

        // Only show the main goals (not TIME)
        GameScoreGoal[] goals = new GameScoreGoal[]{
            GameScoreGoal.AREA,
            GameScoreGoal.BEACONS,
            GameScoreGoal.LINKS,
            GameScoreGoal.TRIANGLES
        };
        for (int i = 0; i < goals.length; i++) {
            GameScoreGoal goal = goals[i];
            boolean enabled = settings.scoreTypes.contains(goal);

            Material material = switch (goal) {
                case AREA -> Material.MAP;
                case BEACONS -> Material.BEACON;
                case LINKS -> Material.LEAD;
                case TRIANGLES -> Material.NETHER_STAR;
                default -> Material.MAP; // fallback
            };

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            meta.displayName(Component.text(goal.getName())
                .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(enabled ? "Enabled" : "Disabled")
                .color(enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
            lore.add(Component.empty());
            lore.add(Component.text("Click to toggle").color(NamedTextColor.YELLOW));
            meta.lore(lore);

            item.setItemMeta(meta);
            selector.setItem(10 + i * 2, item);
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("Back").color(NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        selector.setItem(22, back);

        player.openInventory(selector);
    }


    private void refreshGUI(Player player, GameSettings settings) {
        player.getOpenInventory().getTopInventory().clear();
        Inventory newGui = createGUI(settings);
        for (int i = 0; i < GUI_SIZE; i++) {
            player.getOpenInventory().getTopInventory().setItem(i, newGui.getItem(i));
        }
    }

    /**
     * Gets the game settings for a player.
     *
     * @param player the player
     * @return the settings, or null if not in GUI
     */
    public GameSettings getSettings(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    /**
     * Stores pending game creation data.
     *
     * @param player the player
     * @param settings the game settings
     * @param params the parameter array
     */
    public void setPendingGameCreation(Player player, GameSettings settings, String[] params) {
        pendingCreations.put(player.getUniqueId(), new PendingGameCreation(settings, params));
    }

    /**
     * Gets pending game creation data.
     *
     * @param player the player
     * @return the pending creation data, or null
     */
    public PendingGameCreation getPendingCreation(Player player) {
        return pendingCreations.remove(player.getUniqueId());
    }

    /**
     * Stores pending game creation data.
     */
    public static class PendingGameCreation {
        public final GameSettings settings;
        public final String[] params;

        public PendingGameCreation(GameSettings settings, String[] params) {
            this.settings = settings;
            this.params = params;
        }
    }

    /**
     * Stores game configuration settings.
     */
    public static class GameSettings {
        GameMode gameMode = GameMode.STRATEGY;
        int teams = 2;
        GameScoreGoal goal = GameScoreGoal.AREA;
        List<GameScoreGoal> scoreTypes = new ArrayList<>(List.of(GameScoreGoal.AREA));
        int size = 20000;
        boolean timed = false;
        int timerDays = 0;
        int timerHours = 0;
        int timerMinutes = 10;

        public void reset() {
            gameMode = GameMode.STRATEGY;
            teams = 2;
            goal = GameScoreGoal.AREA;
            scoreTypes = new ArrayList<>(List.of(GameScoreGoal.AREA));
            size = 20000;
            timed = false;
            timerDays = 0;
            timerHours = 0;
            timerMinutes = 10;
        }

        public boolean isTimed() {
            return timed;
        }

        public void setTimed(boolean timed) {
            this.timed = timed;
        }

        public int getTimerSeconds() {
            return (timerDays * 86400) + (timerHours * 3600) + (timerMinutes * 60);
        }

        public GameMode getGameMode() {
            return gameMode;
        }

        public int getTeams() {
            return teams;
        }

        public GameScoreGoal getGoal() {
            return goal;
        }

        public List<GameScoreGoal> getScoreTypes() {
            return scoreTypes;
        }
    }
}
