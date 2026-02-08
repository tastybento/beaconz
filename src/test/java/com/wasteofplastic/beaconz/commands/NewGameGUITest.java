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

package com.wasteofplastic.beaconz.commands;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.commands.NewGameGUI.GameSettings;
import com.wasteofplastic.beaconz.config.Params.GameMode;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;

import net.kyori.adventure.text.Component;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Test class for NewGameGUI using MockBukkit.
 *
 * <p>Tests the GUI inventory interactions including:
 * <ul>
 *   <li>Opening and closing the main GUI</li>
 *   <li>Toggling game mode</li>
 *   <li>Incrementing/decrementing teams</li>
 *   <li>Cycling through goals</li>
 *   <li>Opening score types selector sub-panel</li>
 *   <li>Toggling score types in sub-panel</li>
 *   <li>Returning from sub-panel to main GUI with settings preserved</li>
 *   <li>Timer settings</li>
 *   <li>Reset to defaults</li>
 * </ul>
 */
class NewGameGUITest {

    private ServerMock server;
    private Beaconz plugin;
    private NewGameGUI gui;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Beaconz.class);
        gui = new NewGameGUI(plugin);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /**
     * Helper method to simulate clicking an inventory slot.
     * Uses the non-deprecated API.
     */
    @SuppressWarnings("deprecation")
    private void clickSlot(int slot) {
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, slot);
    }

    @Nested
    class MainGUITests {

        @Test
        void testOpenGUI_CreatesInventory() {
            gui.openGUI(player);

            assertNotNull(player.getOpenInventory());
            assertEquals(54, player.getOpenInventory().getTopInventory().getSize());
            assertEquals(Component.text("New Game Configuration"), player.getOpenInventory().title());
        }

        @Test
        void testOpenGUI_InitializesDefaultSettings() {
            gui.openGUI(player);

            GameSettings settings = gui.getSettings(player);
            assertNotNull(settings);
            assertEquals(GameMode.STRATEGY, settings.getGameMode());
            assertEquals(2, settings.getTeams());
            assertEquals(GameScoreGoal.AREA, settings.getGoal());
            assertFalse(settings.isTimed());
        }

        @Test
        void testOpenGUI_HasCorrectItems() {
            gui.openGUI(player);

            Inventory inv = player.getOpenInventory().getTopInventory();

            // Game mode
            assertNotNull(inv.getItem(10));
            assertEquals(Material.DIAMOND_SWORD, inv.getItem(10).getType());

            // Teams
            assertNotNull(inv.getItem(12));
            assertEquals(Material.PLAYER_HEAD, inv.getItem(12).getType());
            assertEquals(2, inv.getItem(12).getAmount());

            // Goal
            assertNotNull(inv.getItem(14));
            assertEquals(Material.MAP, inv.getItem(14).getType());

            // Score types
            assertNotNull(inv.getItem(16));
            assertEquals(Material.WRITABLE_BOOK, inv.getItem(16).getType());

            // Timer toggle
            assertNotNull(inv.getItem(28));
            assertEquals(Material.BARRIER, inv.getItem(28).getType());

            // Reset
            assertNotNull(inv.getItem(48));
            assertEquals(Material.TNT, inv.getItem(48).getType());

            // Create
            assertNotNull(inv.getItem(50));
            assertEquals(Material.EMERALD_BLOCK, inv.getItem(50).getType());
        }
    }

    @Nested
    class GameModeToggleTests {

        @Test
        void testToggleGameMode() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            // Click game mode slot
            clickSlot(10);

            assertEquals(GameMode.MINIGAME, settings.getGameMode());

            // Verify item changed
            Inventory inv = player.getOpenInventory().getTopInventory();
            assertEquals(Material.GOLDEN_SWORD, inv.getItem(10).getType());
        }

        @Test
        void testToggleGameModeTwice() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            clickSlot(10);
            assertEquals(GameMode.MINIGAME, settings.getGameMode());

            clickSlot(10);
            assertEquals(GameMode.STRATEGY, settings.getGameMode());
        }
    }

    @Nested
    class TeamsTests {

        @Test
        void testIncrementTeams() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            clickSlot(12);

            assertEquals(3, settings.getTeams());
            assertEquals(3, player.getOpenInventory().getTopInventory().getItem(12).getAmount());
        }

        @Test
        void testIncrementTeamsWrapsAt16() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);
            settings.teams = 16;

            clickSlot(12);

            assertEquals(2, settings.getTeams());
        }
    }

    @Nested
    class GoalTests {

        @Test
        void testCycleGoal() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            clickSlot(14);
            assertEquals(GameScoreGoal.BEACONS, settings.getGoal());
            assertEquals(Material.BEACON, player.getOpenInventory().getTopInventory().getItem(14).getType());

            clickSlot(14);
            assertEquals(GameScoreGoal.LINKS, settings.getGoal());
            assertEquals(Material.LEAD, player.getOpenInventory().getTopInventory().getItem(14).getType());

            clickSlot(14);
            assertEquals(GameScoreGoal.TRIANGLES, settings.getGoal());
            assertEquals(Material.NETHER_STAR, player.getOpenInventory().getTopInventory().getItem(14).getType());

            clickSlot(14);
            assertEquals(GameScoreGoal.AREA, settings.getGoal());
            assertEquals(Material.MAP, player.getOpenInventory().getTopInventory().getItem(14).getType());
        }
    }

    @Nested
    class ScoreTypesSelectorTests {

        @Test
        void testOpenScoreTypesSelector() {
            gui.openGUI(player);

            // Click score types item (slot 16)
            clickSlot(16);

            // Should open score types selector
            assertEquals(Component.text("Select Score Types"), player.getOpenInventory().title());
            assertEquals(27, player.getOpenInventory().getTopInventory().getSize());
        }

        @Test
        void testScoreTypesSelector_HasItems() {
            gui.openGUI(player);
            clickSlot(16);

            Inventory inv = player.getOpenInventory().getTopInventory();

            // Score type items at slots 10, 12, 14, 16
            assertNotNull(inv.getItem(10)); // AREA
            assertNotNull(inv.getItem(12)); // BEACONS
            assertNotNull(inv.getItem(14)); // LINKS
            assertNotNull(inv.getItem(16)); // TRIANGLES

            // Back button at slot 22
            assertNotNull(inv.getItem(22));
            assertEquals(Material.ARROW, inv.getItem(22).getType());
        }

        @Test
        void testScoreTypesSelector_ToggleScoreType() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            // Open selector
            clickSlot(16);

            // Initial state: only AREA enabled
            assertEquals(1, settings.getScoreTypes().size());
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.AREA));

            // Toggle BEACONS on (slot 12)
            clickSlot(12);

            // BEACONS should now be enabled
            assertEquals(2, settings.getScoreTypes().size());
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.AREA));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.BEACONS));
        }

        @Test
        void testScoreTypesSelector_ToggleMultiple() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            clickSlot(16);

            // Enable all score types
            clickSlot(12); // BEACONS
            clickSlot(14); // LINKS
            clickSlot(16); // TRIANGLES

            assertEquals(4, settings.getScoreTypes().size());
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.AREA));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.BEACONS));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.LINKS));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.TRIANGLES));
        }

        @Test
        void testScoreTypesSelector_ToggleOff() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            clickSlot(16);

            // Toggle AREA off (slot 10)
            clickSlot(10);

            assertFalse(settings.getScoreTypes().contains(GameScoreGoal.AREA));
            assertEquals(0, settings.getScoreTypes().size());
        }

        @Test
        void testScoreTypesSelector_BackButton() {
            gui.openGUI(player);

            clickSlot(16);
            assertEquals(Component.text("Select Score Types"), player.getOpenInventory().title());

            // Click back button (slot 22)
            clickSlot(22);

            // Should return to main GUI
            assertEquals(Component.text("New Game Configuration"), player.getOpenInventory().title());
        }

        @Test
        void testScoreTypesSelector_SettingsPreservedAfterReturning() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            // Open score types selector
            clickSlot(16);

            // Toggle BEACONS and LINKS on
            clickSlot(12);
            clickSlot(14);

            // Return to main GUI
            clickSlot(22);

            // Verify settings preserved
            assertNotNull(gui.getSettings(player));
            assertEquals(3, settings.getScoreTypes().size());
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.AREA));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.BEACONS));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.LINKS));
        }

        @Test
        void testScoreTypesSelector_AllSettingsPreservedDuringNavigation() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            // Set complex state in main GUI
            settings.gameMode = GameMode.MINIGAME;
            settings.teams = 8;
            settings.goal = GameScoreGoal.TRIANGLES;
            settings.setTimed(true);
            settings.timerDays = 2;
            settings.timerHours = 5;
            settings.timerMinutes = 30;

            // Navigate to score types selector
            clickSlot(16);

            // Toggle some score types
            clickSlot(12); // Enable BEACONS
            clickSlot(16); // Enable TRIANGLES

            // Return to main GUI
            clickSlot(22);

            // Verify ALL settings preserved
            GameSettings verify = gui.getSettings(player);
            assertNotNull(verify);
            assertEquals(GameMode.MINIGAME, verify.getGameMode());
            assertEquals(8, verify.getTeams());
            assertEquals(GameScoreGoal.TRIANGLES, verify.getGoal());
            assertTrue(verify.isTimed());
            assertEquals(2, verify.timerDays);
            assertEquals(5, verify.timerHours);
            assertEquals(30, verify.timerMinutes);
            assertEquals(3, verify.getScoreTypes().size());
            assertTrue(verify.getScoreTypes().contains(GameScoreGoal.AREA));
            assertTrue(verify.getScoreTypes().contains(GameScoreGoal.BEACONS));
            assertTrue(verify.getScoreTypes().contains(GameScoreGoal.TRIANGLES));
        }

        @Test
        void testScoreTypesSelector_MultipleNavigations() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            // First navigation: enable BEACONS
            clickSlot(16);
            clickSlot(12);
            clickSlot(22);

            assertEquals(2, settings.getScoreTypes().size());
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.BEACONS));

            // Second navigation: enable LINKS
            clickSlot(16);
            clickSlot(14);
            clickSlot(22);

            assertEquals(3, settings.getScoreTypes().size());
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.BEACONS));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.LINKS));

            // Third navigation: disable AREA
            clickSlot(16);
            clickSlot(10);
            clickSlot(22);

            assertEquals(2, settings.getScoreTypes().size());
            assertFalse(settings.getScoreTypes().contains(GameScoreGoal.AREA));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.BEACONS));
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.LINKS));
        }
    }

    @Nested
    class TimerTests {

        @Test
        void testToggleTimer() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            clickSlot(28);

            assertTrue(settings.isTimed());

            // Timer controls should be visible
            Inventory inv = player.getOpenInventory().getTopInventory();
            assertNotNull(inv.getItem(29)); // Days
            assertNotNull(inv.getItem(30)); // Hours
            assertNotNull(inv.getItem(31)); // Minutes
        }

        @Test
        void testTimerDaysIncrement() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);
            settings.setTimed(true);

            clickSlot(29);

            assertEquals(1, settings.timerDays);
        }
    }

    @Nested
    class ResetTests {

        @Test
        void testReset() {
            gui.openGUI(player);
            GameSettings settings = gui.getSettings(player);

            // Change everything
            settings.gameMode = GameMode.MINIGAME;
            settings.teams = 10;
            settings.goal = GameScoreGoal.LINKS;
            settings.scoreTypes.clear();
            settings.scoreTypes.add(GameScoreGoal.BEACONS);
            settings.setTimed(true);
            settings.timerDays = 5;

            // Click reset (slot 48)
            clickSlot(48);

            // Verify defaults restored
            assertEquals(GameMode.STRATEGY, settings.getGameMode());
            assertEquals(2, settings.getTeams());
            assertEquals(GameScoreGoal.AREA, settings.getGoal());
            assertFalse(settings.isTimed());
            assertEquals(1, settings.getScoreTypes().size());
            assertTrue(settings.getScoreTypes().contains(GameScoreGoal.AREA));
        }
    }

    @Nested
    class SessionManagementTests {

        @Test
        void testSessionCreatedOnOpen() {
            assertNull(gui.getSettings(player));

            gui.openGUI(player);

            assertNotNull(gui.getSettings(player));
        }

        @Test
        void testSessionClearedOnClose() {
            gui.openGUI(player);
            assertNotNull(gui.getSettings(player));

            player.closeInventory();

            assertNull(gui.getSettings(player));
        }

        @Test
        void testSessionPreservedDuringScoreTypesNavigation() {
            gui.openGUI(player);
            assertNotNull(gui.getSettings(player));

            // Navigate to score types
            clickSlot(16);

            // Session should still exist
            assertNotNull(gui.getSettings(player));

            // Return to main GUI
            clickSlot(22);

            // Session should still exist
            assertNotNull(gui.getSettings(player));
        }
    }
}
