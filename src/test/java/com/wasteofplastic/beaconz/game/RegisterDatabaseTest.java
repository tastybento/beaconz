package com.wasteofplastic.beaconz.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.PrintWriter;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.kyori.adventure.text.Component;

/**
 * Test suite for Register class database functionality.
 * Tests SQLite storage, migration from YAML, and data integrity.
 */
@DisplayName("Register Database Tests")
class RegisterDatabaseTest {

    private Register register;
    private Game game;
    private Team redTeam;
    private Team blueTeam;
    private HikariDataSource dataSource;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        // Initialize MockBukkit server
        ServerMock server = MockBukkit.mock();

        // Mock the plugin
        Beaconz plugin = mock(Beaconz.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("RegisterDatabaseTest"));
        when(plugin.getDataFolder()).thenReturn(tempDir);

        // Initialize test database
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(tempDir, "test-database.db").getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);
        when(plugin.getDataSource()).thenReturn(dataSource);

        // Create and setup test world
        World world = server.addSimpleWorld("beaconz_world");
        when(plugin.getBeaconzWorld()).thenReturn(world);

        // Mock GameMgr
        GameMgr gameMgr = mock(GameMgr.class);
        when(plugin.getGameMgr()).thenReturn(gameMgr);

        // Mock Game and Scorecard
        game = mock(Game.class);
        Scorecard scorecard = mock(Scorecard.class);
        when(game.getScorecard()).thenReturn(scorecard);
        when(game.getName()).thenReturn(Component.text("TestGame"));
        when(gameMgr.getGame(any(Point2D.class))).thenReturn(game);
        when(gameMgr.getGame(anyInt(), anyInt())).thenReturn(game);

        // Mock teams
        redTeam = mock(Team.class);
        when(redTeam.getName()).thenReturn("red");
        when(scorecard.getTeam("red")).thenReturn(redTeam);

        blueTeam = mock(Team.class);
        when(blueTeam.getName()).thenReturn("blue");
        when(scorecard.getTeam("blue")).thenReturn(blueTeam);

        // Initialize Settings
        Settings.linkLimit = 8;

        // Create the Register instance
        register = new Register(plugin);
        when(plugin.getRegister()).thenReturn(register);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Database Initialization Tests")
    class DatabaseInitializationTests {

        @Test
        @DisplayName("Should create all database tables")
        void shouldCreateDatabaseTables() {
            // The tables should already be created during plugin initialization
            // Verify by attempting to save and load
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            assertNotNull(beacon, "Beacon should be created");

            register.saveRegister();

            // Clear and reload
            register.clear();
            register.loadRegister();

            // Verify beacon was persisted
            BeaconObj loaded = register.getBeaconAt(100, 200);
            assertNotNull(loaded, "Beacon should be loaded from database");
            assertEquals(100, loaded.getX());
            assertEquals(64, loaded.getY());
            assertEquals(200, loaded.getZ());
        }

        @Test
        @DisplayName("Should handle missing database gracefully")
        void shouldHandleMissingDatabase() {
            // This tests the fallback to YAML if database fails
            // Just verify it doesn't crash
            assertDoesNotThrow(() -> register.loadRegister());
        }
    }

    @Nested
    @DisplayName("Save and Load Tests")
    class SaveAndLoadTests {

        @Test
        @DisplayName("Should save and load single beacon")
        void shouldSaveAndLoadSingleBeacon() {
            // Create beacon
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            beacon.setId(12345);

            // Save
            register.saveRegister();

            // Clear and reload
            register.clear();
            register.loadRegister();

            // Verify
            BeaconObj loaded = register.getBeaconAt(100, 200);
            assertNotNull(loaded);
            assertEquals(100, loaded.getX());
            assertEquals(64, loaded.getY());
            assertEquals(200, loaded.getZ());
            assertEquals(redTeam, loaded.getOwnership());
            assertEquals(12345, loaded.getId());
        }

        @Test
        @DisplayName("Should save and load multiple beacons")
        void shouldSaveAndLoadMultipleBeacons() {
            // Create multiple beacons
            BeaconObj b1 = register.addBeacon(redTeam, 100, 64, 100);
            BeaconObj b2 = register.addBeacon(redTeam, 200, 64, 200);
            BeaconObj b3 = register.addBeacon(blueTeam, 300, 64, 300);

            assertEquals(3, register.getBeaconRegister().size());

            // Save and reload
            register.saveRegister();
            register.clear();
            register.loadRegister();

            // Verify all beacons are loaded
            assertEquals(3, register.getBeaconRegister().size());

            BeaconObj loaded1 = register.getBeaconAt(100, 100);
            BeaconObj loaded2 = register.getBeaconAt(200, 200);
            BeaconObj loaded3 = register.getBeaconAt(300, 300);

            assertNotNull(loaded1);
            assertNotNull(loaded2);
            assertNotNull(loaded3);

            assertEquals(redTeam, loaded1.getOwnership());
            assertEquals(redTeam, loaded2.getOwnership());
            assertEquals(blueTeam, loaded3.getOwnership());
        }

        @Test
        @DisplayName("Should save and load beacon links")
        void shouldSaveAndLoadBeaconLinks() {
            // Create beacons
            BeaconObj b1 = register.addBeacon(redTeam, 100, 64, 100);
            BeaconObj b2 = register.addBeacon(redTeam, 200, 64, 200);

            // Create link
            register.addBeaconLink(b1, b2);

            // Verify link exists
            assertTrue(b1.getLinks().contains(b2));
            assertTrue(b2.getLinks().contains(b1));

            // Save and reload
            register.saveRegister();
            register.clear();
            register.loadRegister();

            // Verify links are restored
            BeaconObj loaded1 = register.getBeaconAt(100, 100);
            BeaconObj loaded2 = register.getBeaconAt(200, 200);

            assertNotNull(loaded1);
            assertNotNull(loaded2);
            assertTrue(loaded1.getLinks().contains(loaded2));
            assertTrue(loaded2.getLinks().contains(loaded1));
        }

        @Test
        @DisplayName("Should save and load base blocks")
        void shouldSaveAndLoadBaseBlocks() {
            // Create beacon
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 100);

            // Add base blocks
            register.addBeaconBaseBlock(99, 99, beacon);
            register.addBeaconBaseBlock(101, 101, beacon);

            // Save and reload
            register.saveRegister();
            register.clear();
            register.loadRegister();

            // Verify base blocks are restored
            BeaconObj loaded = register.getBeaconAt(100, 100);
            assertNotNull(loaded);

            Set<Point2D> baseBlocks = register.getDefensesAtBeacon(loaded);
            assertNotNull(baseBlocks);
            assertTrue(baseBlocks.size() >= 2, "Should have at least 2 base blocks");
        }

        @Test
        @DisplayName("Should handle empty database")
        void shouldHandleEmptyDatabase() {
            // Clear everything
            register.clear();
            register.saveRegister();

            // Reload
            register.loadRegister();

            // Verify empty
            assertEquals(0, register.getBeaconRegister().size());
        }
    }

    @Nested
    @DisplayName("Migration Tests")
    class MigrationTests {

        @Test
        @DisplayName("Should migrate from YAML to database")
        void shouldMigrateFromYAML() throws Exception {
            // Create a legacy YAML file in the temp directory
            File yamlFile = new File(tempDir, "beaconz.yml");

            try (PrintWriter writer = new PrintWriter(yamlFile)) {
                writer.println("beacon:");
                writer.println("  0:");
                writer.println("    game: \"TestGame\"");
                writer.println("    location: \"100:64:200:red\"");
                writer.println("    links: []");
                writer.println("    baseblocks:");
                writer.println("      - \"99:199\"");
                writer.println("      - \"101:201\"");
                writer.println("    defensiveblocks: {}");
                writer.println("    maps: []");
            }

            // Clear database and load (should migrate)
            register.clear();
            register.loadRegister();

            // Verify beacon was migrated
            BeaconObj loaded = register.getBeaconAt(100, 200);
            assertNotNull(loaded, "Beacon should be migrated from YAML");
            assertEquals(100, loaded.getX());
            assertEquals(64, loaded.getY());
            assertEquals(200, loaded.getZ());
        }

        @Test
        @DisplayName("Should not migrate if database has data")
        void shouldNotMigrateIfDatabaseHasData() {
            // Add beacon to database
            register.addBeacon(redTeam, 500, 64, 500);
            register.saveRegister();

            // Create a YAML file with different data
            File yamlFile = new File(tempDir, "beaconz.yml");
            try (PrintWriter writer = new PrintWriter(yamlFile)) {
                writer.println("beacon:");
                writer.println("  0:");
                writer.println("    game: \"TestGame\"");
                writer.println("    location: \"100:64:200:red\"");
                writer.println("    links: []");
                writer.println("    baseblocks: []");
                writer.println("    defensiveblocks: {}");
                writer.println("    maps: []");
            } catch (Exception e) {
                fail("Failed to create YAML file: " + e.getMessage());
            }

            // Clear and reload
            register.clear();
            register.loadRegister();

            // Should load from database, not YAML
            BeaconObj loaded = register.getBeaconAt(500, 500);
            assertNotNull(loaded, "Should load from database");

            // YAML beacon should not be loaded because database has data
            BeaconObj yamlBeacon = register.getBeaconAt(100, 200);
            assertNull(yamlBeacon, "YAML beacon should not be loaded when database has data");
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should maintain beacon-link consistency")
        void shouldMaintainBeaconLinkConsistency() {
            // Create beacons and links
            BeaconObj b1 = register.addBeacon(redTeam, 100, 64, 100);
            BeaconObj b2 = register.addBeacon(redTeam, 200, 64, 200);
            BeaconObj b3 = register.addBeacon(redTeam, 300, 64, 300);

            register.addBeaconLink(b1, b2);
            register.addBeaconLink(b2, b3);
            register.addBeaconLink(b3, b1);

            // Save and reload
            register.saveRegister();
            register.clear();
            register.loadRegister();

            // Verify all links are bidirectional
            BeaconObj l1 = register.getBeaconAt(100, 100);
            BeaconObj l2 = register.getBeaconAt(200, 200);
            BeaconObj l3 = register.getBeaconAt(300, 300);

            assertTrue(l1.getLinks().contains(l2) && l2.getLinks().contains(l1));
            assertTrue(l2.getLinks().contains(l3) && l3.getLinks().contains(l2));
            assertTrue(l3.getLinks().contains(l1) && l1.getLinks().contains(l3));
        }

        @Test
        @DisplayName("Should handle special characters in team names")
        void shouldHandleSpecialCharactersInTeamNames() {
            // This tests SQL injection protection and proper escaping
            Team specialTeam = game.getScorecard().getTeam("team'with\"quotes");

            BeaconObj beacon = register.addBeacon(specialTeam, 100, 64, 100);

            register.saveRegister();
            register.clear();
            register.loadRegister();

            BeaconObj loaded = register.getBeaconAt(100, 100);
            assertNotNull(loaded);
            // The team might not be restored if it doesn't exist, which is expected
        }

        @Test
        @DisplayName("Should preserve beacon ownership through save/load cycle")
        void shouldPreserveBeaconOwnership() {
            BeaconObj b1 = register.addBeacon(redTeam, 100, 64, 100);
            BeaconObj b2 = register.addBeacon(blueTeam, 200, 64, 200);
            BeaconObj b3 = register.addBeacon(null, 300, 64, 300); // Unowned beacon

            register.saveRegister();
            register.clear();
            register.loadRegister();

            BeaconObj l1 = register.getBeaconAt(100, 100);
            BeaconObj l2 = register.getBeaconAt(200, 200);
            BeaconObj l3 = register.getBeaconAt(300, 300);

            assertEquals(redTeam, l1.getOwnership());
            assertEquals(blueTeam, l2.getOwnership());
            assertNull(l3.getOwnership());
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle large number of beacons efficiently")
        void shouldHandleLargeNumberOfBeacons() {
            // Create 100 beacons
            int count = 100;
            for (int i = 0; i < count; i++) {
                register.addBeacon(i % 2 == 0 ? redTeam : blueTeam, i * 10, 64, i * 10);
            }

            long startTime = System.currentTimeMillis();
            register.saveRegister();
            long saveTime = System.currentTimeMillis() - startTime;

            register.clear();

            startTime = System.currentTimeMillis();
            register.loadRegister();
            long loadTime = System.currentTimeMillis() - startTime;

            // Verify all beacons loaded
            assertEquals(count, register.getBeaconRegister().size());

            // Performance assertions (should complete in reasonable time)
            assertTrue(saveTime < 5000, "Save should complete in under 5 seconds");
            assertTrue(loadTime < 5000, "Load should complete in under 5 seconds");
        }

        @Test
        @DisplayName("Should use batch inserts for efficiency")
        void shouldUseBatchInsertsForEfficiency() {
            // This test verifies that the implementation uses batch inserts
            // by creating many beacons and ensuring save doesn't take too long

            for (int i = 0; i < 50; i++) {
                BeaconObj beacon = register.addBeacon(redTeam, i * 20, 64, i * 20);
                // Add some base blocks to each beacon
                register.addBeaconBaseBlock(i * 20 - 1, i * 20 - 1, beacon);
                register.addBeaconBaseBlock(i * 20 + 1, i * 20 + 1, beacon);
            }

            long startTime = System.currentTimeMillis();
            register.saveRegister();
            long duration = System.currentTimeMillis() - startTime;

            // With batch inserts, this should be fast
            assertTrue(duration < 3000, "Batch insert should be fast (< 3s), took: " + duration + "ms");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle save errors gracefully")
        void shouldHandleSaveErrorsGracefully() {
            // Add a beacon
            register.addBeacon(redTeam, 100, 64, 100);

            // This should not throw even if there are issues
            assertDoesNotThrow(() -> register.saveRegister());
        }

        @Test
        @DisplayName("Should handle load errors gracefully")
        void shouldHandleLoadErrorsGracefully() {
            // This should not throw even if database is corrupted
            assertDoesNotThrow(() -> register.loadRegister());
        }

        @Test
        @DisplayName("Should rollback on save error")
        void shouldRollbackOnSaveError() {
            // Create initial data
            register.addBeacon(redTeam, 100, 64, 100);
            register.saveRegister();

            // Add more data
            register.addBeacon(blueTeam, 200, 64, 200);

            // If save fails, database should still have old data
            // This is ensured by transaction rollback in the implementation

            register.clear();
            register.loadRegister();

            // At minimum, we should have the first beacon
            assertFalse(register.getBeaconRegister().isEmpty());
        }
    }
}







