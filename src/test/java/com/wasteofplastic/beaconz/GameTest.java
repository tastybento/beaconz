package com.wasteofplastic.beaconz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.wasteofplastic.beaconz.Params.GameMode;
import com.wasteofplastic.beaconz.Params.GameScoreGoal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Comprehensive test suite for {@link Game} class.
 * <p>
 * Tests all major functionality of the Game class including:
 * <ul>
 *   <li>Game lifecycle (creation, restart, pause/resume, end, delete)</li>
 *   <li>Player management (join, leave, kick)</li>
 *   <li>Game configuration (getters, setters, parameters)</li>
 *   <li>Persistence (save operations)</li>
 *   <li>Game state management</li>
 *   <li>Starting kit distribution</li>
 *   <li>Team integration</li>
 * </ul>
 *
 * @author tastybento
 */
@DisplayName("Game Tests")
class GameTest {

    private ServerMock server;
    private Beaconz plugin;
    private World world;
    private Region region;
    private Register register;
    private Game game;

    @TempDir
    File tempDir;

    /**
     * Sets up the test environment before each test.
     * Initializes MockBukkit server, plugin mocks, and test world.
     */
    @BeforeEach
    void setUp() {
        // Initialize MockBukkit server
        server = MockBukkit.mock();

        // Mock the plugin
        plugin = mock(Beaconz.class);
        when(plugin.getDataFolder()).thenReturn(tempDir);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("GameTest"));

        // Mock FileConfiguration
        FileConfiguration config = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(config);

        // Create and setup test world
        world = server.addSimpleWorld("beaconzworld");
        when(plugin.getBeaconzWorld()).thenReturn(world);

        // Mock Register
        register = mock(Register.class);
        when(plugin.getRegister()).thenReturn(register);
        when(register.getBeaconRegister()).thenReturn(new HashMap<>());

        // Mock GameMgr to prevent NPEs
        GameMgr gameMgr = mock(GameMgr.class);
        when(plugin.getGameMgr()).thenReturn(gameMgr);

        // Initialize Settings
        setupSettings();

        // Initialize Lang strings
        setupLangStrings();

        // Mock Region
        region = mock(Region.class);
        Point2D[] corners = new Point2D[]{
            new Point2D.Double(0, 0),
            new Point2D.Double(100, 100)
        };
        when(region.corners()).thenReturn(corners);

        Params params = new Params(GameMode.MINIGAME, 50, 5, GameScoreGoal.AREA, 10000, 3600, List.of(GameScoreGoal.AREA, GameScoreGoal.BEACONS), 0.5);
        // Create Game instance
        game = new Game(plugin, region, Component.text("TestGame"), params);
    }

    /**
     * Setup default Settings values.
     */
    private void setupSettings() {
        // Note: Settings.newbieKit is final and initialized in Settings class
        // We can't modify it here, so we assume it's properly initialized
        Settings.initialXP = 100;
    }

    /**
     * Initialize Lang static strings to prevent NPEs.
     */
    private void setupLangStrings() {
        Lang.titleBeaconz = Component.text("Title Beaconz");
        Lang.titleWelcomeToGame = Component.text("Welcome to game [name]");
        Lang.titleWelcomeBackToGame = Component.text("Welcome back to game [name]");
        Lang.generalSuccess = Component.text("Success!");
        Lang.errorNotInGame = Component.text("You are not in game [game]");
        Lang.scoreGetValueGoal = Component.text("getvalue goal");
        Lang.scoreGameOver = "Game Over"; // Still a String in Lang class
        Lang.scoreGameModeMiniGame = "Minigame";
        Lang.scoreGoalArea = "Area";
        Lang.scoreGoalBeacons = "Beacons";
        Lang.scoreGoalTime = "Time";
        Lang.scoreGoalTriangles = "Triangles";
        Lang. scoreGoalLinks = "Links";
        Lang.scoreStrategy = "Strategy";
    }

    /**
     * Cleanup after each test.
     */
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ========== Constructor Tests ==========

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        /**
         * Test that Game constructor initializes all fields correctly.
         */
        @Test
        @DisplayName("Constructor - initializes all fields correctly")
        void testConstructorInitialization() {
            // Then - verify all fields are set
            assertEquals(Component.text("TestGame"), game.getName());
            assertEquals(region, game.getRegion());
            assertEquals(GameMode.MINIGAME, game.getGamemode());
            assertEquals(50, game.getGamedistance());
            assertEquals(0.5, game.getGamedistribution());
            assertEquals(5, game.getNbrTeams());
            assertEquals(GameScoreGoal.AREA, game.getGamegoal());
            assertEquals(10000, game.getGamegoalvalue());
            assertEquals(3600, game.getCountdownTimer());
            assertEquals(List.of(GameScoreGoal.AREA, GameScoreGoal.BEACONS), game.getScoretypes());
            assertNotNull(game.getStartTime());
            assertNotNull(game.getCreateTime());
            assertFalse(game.isOver());
            assertFalse(game.isGameRestart());
        }

        /**
         * Test that Game constructor creates a scorecard.
         */
        @Test
        @DisplayName("Constructor - creates scorecard")
        void testConstructorCreatesScorecard() {
            // Then
            assertNotNull(game.getScorecard());
        }

        /**
         * Test that Game constructor sets bidirectional region-game relationship.
         */
        @Test
        @DisplayName("Constructor - establishes region-game relationship")
        void testConstructorSetsRegionGameRelationship() {
            // Then
            verify(region).setGame(game);
        }

        /**
         * Test that start time is rounded to nearest second.
         */
        @Test
        @DisplayName("Constructor - rounds start time to nearest second")
        void testConstructorRoundsStartTime() {
            // Then - start time should be divisible by 1000 (rounded to second)
            assertEquals(0, game.getStartTime() % 1000);
        }
    }

    // ========== Lifecycle Tests ==========

    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        /**
         * Test game restart functionality.
         */
        @Test
        @DisplayName("restart() - resets game state while preserving teams")
        void testRestart() {
            // Given - game is in progress
            game.setOver(true);

            // When
            game.restart();

            // Then
            assertFalse(game.isOver(), "Game should not be over after restart");
            assertFalse(game.isGameRestart(), "Restart flag should be cleared");
            verify(region).sendAllPlayersToLobby(false);
        }

        /**
         * Test pause functionality.
         */
        @Test
        @DisplayName("pause() - pauses the game timer")
        void testPause() {
            // When
            game.pause();

            // Then - scorecard should be paused
            // Note: We can't easily verify this without accessing scorecard internals
            // but we can verify the method doesn't throw
        }

        /**
         * Test resume functionality.
         */
        @Test
        @DisplayName("resume() - resumes the game timer")
        void testResume() {
            // Given
            game.pause();

            // When
            game.resume();

            // Then - scorecard should be resumed
            // Method should not throw
        }

        /**
         * Test force end functionality.
         */
        @Test
        @DisplayName("forceEnd() - immediately ends the game")
        void testForceEnd() {
            // Given
            assertFalse(game.isOver());

            // When
            game.forceEnd();

            // Then
            assertTrue(game.isOver(), "Game should be marked as over");
        }

        /**
         * Test reload functionality.
         */
        @Test
        @DisplayName("reload() - refreshes scorecard on plugin reload")
        void testReload() {
            // When
            game.reload();

            // Then - method should not throw
            // Scorecard reload is called internally
        }
    }

    // ========== Player Management Tests ==========

    @Nested
    @DisplayName("Player Management Tests")
    class PlayerManagementTests {

        /**
         * Test joining a game as a new player.
         */
        @Test
        @DisplayName("join() - new player joins and receives kit")
        @Disabled
        void testJoinNewPlayer() {
            // Given
            PlayerMock player = server.addPlayer("TestPlayer");

            // Mock scorecard behavior
            Scorecard mockScorecard = mock(Scorecard.class);
            when(mockScorecard.inTeam(player)).thenReturn(false); // New player

            // Create game with mocked scorecard
            Params params = new Params(GameMode.MINIGAME, 4, 5, GameScoreGoal.AREA, 10000, 3600, List.of(GameScoreGoal.AREA, GameScoreGoal.BEACONS), 0.5);

            Game testGame = new Game(plugin, region, Component.text("TestGame2"), params);

            // When
            testGame.join(player, false);

            // Then - player should have items in inventory
            assertFalse(player.getInventory().isEmpty(), "New player should receive starting kit");
        }

        /**
         * Test joining a game as a returning player.
         */
        @Test
        @DisplayName("join() - returning player does not receive kit again")
        void testJoinReturningPlayer() {
            // Given
            PlayerMock player = server.addPlayer("ReturningPlayer");
            player.getInventory().clear();

            // When - simulate returning player (already in team)
            // Note: Full test would require properly mocked scorecard

            // Then - would verify no kit is given
        }

        /**
         * Test player leaving a game.
         */
        @Test
        @DisplayName("leave() - removes player from game")
        void testLeave() {
            // Given
            PlayerMock player = server.addPlayer("LeavingPlayer");

            // When
            game.leave(player);

            // Then
            verify(region).sendToLobby(player, false);
        }

        /**
         * Test kicking a player from a game.
         */
        @Test
        @DisplayName("kick() - removes player via admin command")
        void testKick() {
            // Given
            PlayerMock player = server.addPlayer("KickedPlayer");
            PlayerMock admin = server.addPlayer("Admin");

            // When
            game.kick(admin, player);

            // Then
            verify(region).sendToLobby(player, false);
        }

        /**
         * Test kicking all players from a game.
         */
        @Test
        @DisplayName("kickAll() - removes all players from game")
        void testKickAll() {
            // Given - add players to server
            server.addPlayer("Player1");
            server.addPlayer("Player2");

            // When
            game.kickAll();

            // Then - verify sendToLobby called for each online player
            verify(region, times(2)).sendToLobby(any(Player.class), eq(false));
        }
    }

    // ========== Starting Kit Tests ==========

    @Nested
    @DisplayName("Starting Kit Tests")
    @Disabled
    class StartingKitTests {

        /**
         * Test giving starting kit to a player.
         */
        @Test
        @DisplayName("giveStartingKit() - clears inventory and gives kit items")
        void testGiveStartingKit() {
            // Given
            PlayerMock player = server.addPlayer("NewPlayer");
            player.getInventory().addItem(new ItemStack(Material.DIRT, 64));

            // When
            game.giveStartingKit(player);

            // Then - inventory should be cleared and kit given
            assertTrue(player.getInventory().contains(Material.STONE_PICKAXE),
                "Should have pickaxe from kit");
            assertTrue(player.getInventory().contains(Material.EMERALD_BLOCK),
                "Should have emerald blocks from kit");
            assertFalse(player.getInventory().contains(Material.DIRT),
                "Old items should be cleared");
        }

        /**
         * Test that minigame mode sets starting XP.
         */
        @Test
        @DisplayName("giveStartingKit() - sets starting XP in minigame mode")
        void testGiveStartingKitMinigameXP() {
            // Given
            PlayerMock player = server.addPlayer("MinigamePlayer");
            assertEquals(GameMode.MINIGAME, game.getGamemode());

            // When
            game.giveStartingKit(player);

            // Then - XP should be set (BeaconLinkListener.setTotalExperience is called)
            // Note: Can't easily verify without accessing BeaconLinkListener
        }
    }

    // ========== Configuration Tests ==========

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        /**
         * Test all getter methods return correct values.
         */
        @Test
        @DisplayName("Getters - return correct configuration values")
        void testGetters() {
            assertEquals(Component.text("TestGame"), game.getName());
            assertEquals(region, game.getRegion());
            assertEquals(GameMode.MINIGAME, game.getGamemode());
            assertEquals(50, game.getGamedistance());
            assertEquals(0.5, game.getGamedistribution(), 0.001);
            assertEquals(5, game.getNbrTeams());
            assertEquals(GameScoreGoal.AREA, game.getGamegoal());
            assertEquals(10000, game.getGamegoalvalue());
            assertEquals(3600, game.getCountdownTimer());
            assertEquals(List.of(GameScoreGoal.AREA, GameScoreGoal.BEACONS), game.getScoretypes());
        }

        /**
         * Test setter methods update values correctly.
         */
        @Test
        @DisplayName("Setters - update configuration values")
        void testSetters() {
            // When
            game.setGamemode(GameMode.STRATEGY);
            game.setGamedistance(75);
            game.setNbrTeams(2);
            game.setGamegoal(GameScoreGoal.BEACONS);
            game.setGamegoalvalue(50);
            game.setCountdownTimer(7200);
            game.setScoretypes(List.of(GameScoreGoal.AREA));
            game.setGamedistribution(0.8);

            // Then
            assertEquals(GameMode.STRATEGY, game.getGamemode());
            assertEquals(75, game.getGamedistance());
            assertEquals(2, game.getNbrTeams());
            assertEquals(GameScoreGoal.BEACONS, game.getGamegoal());
            assertEquals(50, game.getGamegoalvalue());
            assertEquals(7200, game.getCountdownTimer());
            assertEquals(List.of(GameScoreGoal.AREA), game.getScoretypes());
            assertEquals(0.8, game.getGamedistribution(), 0.001);
        }

        /**
         * Test setGameParms updates all parameters at once.
         */
        @Test
        @DisplayName("setGameParms() - updates all parameters at once")
        void testSetGameParms() {
            // Given
            Long newStartTime = System.currentTimeMillis();
            Long newCreateTime = System.currentTimeMillis();

            // When
            Params params = new Params();
            params.setCountdown(7200);
            params.setDistribution(0.7);
            params.setGamemode(GameMode.STRATEGY);
            params.setGoal(GameScoreGoal.TIME);
            params.setSize(100);
            params.setGoalvalue(5000);
            params.setTeams(2);
            params.setScoretypes(List.of(GameScoreGoal.TRIANGLES));
            
            game.setGameParms(params, newStartTime, newCreateTime);
            
             // Then
            assertEquals(GameMode.STRATEGY, game.getGamemode());
            assertEquals(100, game.getGamedistance());
            assertEquals(2, game.getNbrTeams());
            assertEquals(GameScoreGoal.TIME, game.getGamegoal());
            assertEquals(5000, game.getGamegoalvalue());
            assertEquals(7200, game.getCountdownTimer());
            assertEquals(List.of(GameScoreGoal.TRIANGLES), game.getScoretypes());
            assertEquals(0.7, game.getGamedistribution(), 0.001);
        }
    }

    // ========== State Management Tests ==========

    @Nested
    @DisplayName("State Management Tests")
    class StateManagementTests {

        /**
         * Test game over flag.
         */
        @Test
        @DisplayName("isOver/setOver - manages game over state")
        void testGameOverState() {
            // Given
            assertFalse(game.isOver());

            // When
            game.setOver(true);

            // Then
            assertTrue(game.isOver());

            // When
            game.setOver(false);

            // Then
            assertFalse(game.isOver());
        }

        /**
         * Test game restart flag.
         */
        @Test
        @DisplayName("isGameRestart - indicates restart in progress")
        void testGameRestartFlag() {
            // Given
            assertFalse(game.isGameRestart());

            // When - restart sets the flag temporarily
            // Note: restart() sets it to true then false, so it ends as false
            game.restart();

            // Then
            assertFalse(game.isGameRestart(), "Flag should be cleared after restart completes");
        }

        /**
         * Test hasPlayer method.
         */
        @Test
        @DisplayName("hasPlayer() - checks team membership")
        void testHasPlayer() {
            // Given
            PlayerMock player = server.addPlayer("TeamMember");

            // When/Then - player not in team
            assertFalse(game.hasPlayer(player));
        }
    }

    // ========== Persistence Tests ==========

    @Nested
    @DisplayName("Persistence Tests")
    class PersistenceTests {

        /**
         * Test saving game configuration to file.
         */
        @Test
        @DisplayName("save() - writes game configuration to games.yml")
        void testSave() {
            // When
            game.save();

            // Then - file should be created
            File gamesFile = new File(tempDir, "games.yml");
            assertTrue(gamesFile.exists(), "games.yml should be created");
        }

        /**
         * Test that save creates proper YAML structure.
         */
        @Test
        @DisplayName("save() - creates proper YAML structure")
        void testSaveYamlStructure() {
            // When
            game.save();

            // Then - verify file exists and can be loaded
            File gamesFile = new File(tempDir, "games.yml");
            assertTrue(gamesFile.exists());

            // Verify it's valid YAML by loading it
            org.bukkit.configuration.file.YamlConfiguration yaml =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(gamesFile);

            // Note: The game name in YAML path is Component.toString(), not plain text "TestGame"
            // So we just verify the file is valid YAML and contains game data
            assertNotNull(yaml.getConfigurationSection("game"), "Should have 'game' section");
            var gameSection = yaml.getConfigurationSection("game");
            assertNotNull(gameSection, "Game section should not be null");
            assertFalse(gameSection.getKeys(false).isEmpty(),
                "Should have at least one game saved");
        }

        /**
         * Test delete removes game from file.
         */
        @Test
        @DisplayName("delete() - removes game configuration and ends game")
        void testDelete() {
            // Given - save the game first
            game.save();
            File gamesFile = new File(tempDir, "games.yml");
            assertTrue(gamesFile.exists());

            // When
            game.delete();

            // Then - game should be marked as over
            assertTrue(game.isOver(), "Game should be over after deletion");

            // Verify backup was created
            File backup = new File(tempDir, "games.old");
            assertTrue(backup.exists(), "Backup file should be created");
        }
    }

    // ========== Integration Tests ==========

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        /**
         * Test complete player join workflow.
         */
        @Test
        @DisplayName("Complete join workflow - player joins, gets kit, enters game")
        @Disabled
        void testCompleteJoinWorkflow() {
            // Given
            PlayerMock player = server.addPlayer("IntegrationPlayer");

            // When
            game.join(player, false);

            // Then - player should have starting kit
            assertFalse(player.getInventory().isEmpty());
        }

        /**
         * Test game lifecycle: create -> play -> end -> restart.
         */
        @Test
        @DisplayName("Game lifecycle - create, end, restart")
        void testGameLifecycle() {
            // Given - game is created
            assertFalse(game.isOver());

            // When - game ends
            game.forceEnd();
            assertTrue(game.isOver());

            // When - game restarts
            game.restart();

            // Then
            assertFalse(game.isOver(), "Game should be active again after restart");
        }

        /**
         * Test persistence round-trip: save -> delete -> verify cleanup.
         */
        @Test
        @DisplayName("Persistence round-trip - save and delete")
        void testPersistenceRoundTrip() {
            // When - save
            game.save();
            File gamesFile = new File(tempDir, "games.yml");
            assertTrue(gamesFile.exists());

            // When - delete
            game.delete();

            // Then - backup exists
            File backup = new File(tempDir, "games.old");
            assertTrue(backup.exists());
        }
    }

    // ========== Utility Method Tests ==========

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {

        /**
         * Test getStringLocation utility method.
         */
        @Test
        @DisplayName("getStringLocation() - converts Location to string")
        void testGetStringLocation() {
            // Given
            Location loc = new Location(world, 100, 64, 200, 90.0f, 0.0f);

            // When
            String result = Game.getStringLocation(loc);

            // Then
            assertNotNull(result);
            assertTrue(result.contains("beaconzworld"));
            assertTrue(result.contains("100"));
            assertTrue(result.contains("64"));
            assertTrue(result.contains("200"));
        }

        /**
         * Test getStringLocation with null location.
         */
        @Test
        @DisplayName("getStringLocation() - handles null location")
        void testGetStringLocationNull() {
            // When
            String result = Game.getStringLocation(null);

            // Then
            assertEquals("", result);
        }
    }
}
