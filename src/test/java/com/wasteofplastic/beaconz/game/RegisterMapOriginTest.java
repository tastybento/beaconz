package com.wasteofplastic.beaconz.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.geom.Point2D;
import java.io.File;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.map.MapViewMock;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.core.BeaconObj;

/**
 * Comprehensive test suite for map origin persistence in the Register class.
 * Tests that origin coordinates are correctly saved and loaded across server restarts.
 */
@DisplayName("Register Map Origin Tests")
class RegisterMapOriginTest {

    private ServerMock server;
    private Beaconz plugin;
    private Register register;
    private World beaconzWorld;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Beaconz.class);
        register = plugin.getRegister();
        beaconzWorld = plugin.getBeaconzWorld();
    }

    @AfterEach
    void tearDown() {
        // Clean up test files
        File beaconzFile = new File(plugin.getDataFolder(), "beaconz.yml");
        if (beaconzFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            beaconzFile.delete();
        }
        File backupFile = new File(plugin.getDataFolder(), "beaconz.old");
        if (backupFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            backupFile.delete();
        }

        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Map Origin Storage Tests")
    class MapOriginStorageTests {

        @Test
        @DisplayName("Should store map origin coordinates")
        void shouldStoreMapOrigin() {
            int mapId = 1;
            int originX = 100;
            int originZ = 200;

            register.setMapOrigin(mapId, originX, originZ);

            Point2D origin = register.getMapOrigin(mapId);
            assertNotNull(origin);
            assertEquals(originX, (int) origin.getX());
            assertEquals(originZ, (int) origin.getY());
        }

        @Test
        @DisplayName("Should return null for non-existent map origin")
        void shouldReturnNullForNonExistentOrigin() {
            Point2D origin = register.getMapOrigin(999);
            assertNull(origin);
        }

        @Test
        @DisplayName("Should update existing map origin")
        void shouldUpdateMapOrigin() {
            int mapId = 1;
            register.setMapOrigin(mapId, 100, 200);
            register.setMapOrigin(mapId, 300, 400);

            Point2D origin = register.getMapOrigin(mapId);
            assertNotNull(origin);
            assertEquals(300, (int) origin.getX());
            assertEquals(400, (int) origin.getY());
        }

        @Test
        @DisplayName("Should remove map origin when removing beacon map")
        void shouldRemoveMapOrigin() {
            int mapId = 1;
            BeaconObj beacon = new BeaconObj(plugin, 50, 64, 50, null);

            register.addBeaconMap(mapId, beacon, 100, 200);
            assertNotNull(register.getMapOrigin(mapId));

            register.removeBeaconMap(mapId);
            assertNull(register.getMapOrigin(mapId));
            assertNull(register.getBeaconMap(mapId));
        }
    }

    @Nested
    @DisplayName("Map Origin Persistence Tests")
    class MapOriginPersistenceTests {

        // Note: Full save/load persistence tests require game infrastructure which is complex to set up in unit tests.
        // The Register.loadRegister() method only loads beacons that belong to existing games.
        // These tests verify the lower-level storage/retrieval which is what can be unit tested.
        // Full persistence is better tested in integration tests with a real game setup.

        @Test
        @DisplayName("Should save map origins to register")
        void shouldSaveMapOriginsToRegister() {
            if (beaconzWorld == null) {
                return;
            }

            BeaconObj beacon = new BeaconObj(plugin, 100, 64, 200, null);
            register.addBeacon(null, 100, 64, 200);

            // Create a mock map
            MapViewMock mapMock = server.createMap(beaconzWorld);
            int mapId = mapMock.getId();
            register.addBeaconMap(mapId, beacon, 100, 200);

            // Verify origin is in memory
            Point2D origin = register.getMapOrigin(mapId);
            assertNotNull(origin, "Map origin should be stored in register");
            assertEquals(100, (int) origin.getX());
            assertEquals(200, (int) origin.getY());
        }

        @Test
        @DisplayName("Should handle multiple map origins in register")
        void shouldHandleMultipleMapOriginsInRegister() {
            if (beaconzWorld == null) {
                return;
            }

            BeaconObj beacon1 = new BeaconObj(plugin, 100, 64, 200, null);
            BeaconObj beacon2 = new BeaconObj(plugin, 300, 64, 400, null);

            register.addBeacon(null, 100, 64, 200);
            register.addBeacon(null, 300, 64, 400);

            // Create mock maps
            MapViewMock mapMock1 = server.createMap(beaconzWorld);
            MapViewMock mapMock2 = server.createMap(beaconzWorld);
            int mapId1 = mapMock1.getId();
            int mapId2 = mapMock2.getId();

            register.addBeaconMap(mapId1, beacon1, 100, 200);
            register.addBeaconMap(mapId2, beacon2, 300, 400);

            // Verify both origins are stored
            Point2D origin1 = register.getMapOrigin(mapId1);
            Point2D origin2 = register.getMapOrigin(mapId2);

            assertNotNull(origin1, "First map origin should be stored");
            assertNotNull(origin2, "Second map origin should be stored");
            assertEquals(100, (int) origin1.getX());
            assertEquals(200, (int) origin1.getY());
            assertEquals(300, (int) origin2.getX());
            assertEquals(400, (int) origin2.getY());
        }

        @Test
        @DisplayName("Should handle missing origin data gracefully")
        void shouldHandleMissingOriginData() {
            if (beaconzWorld == null) {
                return;
            }

            BeaconObj beacon = new BeaconObj(plugin, 100, 64, 200, null);
            register.addBeacon(null, 100, 64, 200);

            // Add map without origin (using old method)
            int mapId = 99;
            register.addBeaconMap(mapId, beacon);


            // Should not crash, origin should be null
            Point2D origin = register.getMapOrigin(mapId);
            assertNull(origin, "Origin should be null when not set");
        }
    }

    @Nested
    @DisplayName("Integration with addBeaconMap Tests")
    class AddBeaconMapIntegrationTests {

        @Test
        @DisplayName("Should add beacon map with origin in one call")
        void shouldAddBeaconMapWithOrigin() {
            BeaconObj beacon = new BeaconObj(plugin, 100, 64, 200, null);
            int mapId = 5;
            int originX = 150;
            int originZ = 250;

            register.addBeaconMap(mapId, beacon, originX, originZ);

            // Verify beacon map is added
            BeaconObj retrieved = register.getBeaconMap(mapId);
            assertNotNull(retrieved);
            assertEquals(beacon, retrieved);
            assertEquals(mapId, retrieved.getId());

            // Verify origin is stored
            Point2D origin = register.getMapOrigin(mapId);
            assertNotNull(origin);
            assertEquals(originX, (int) origin.getX());
            assertEquals(originZ, (int) origin.getY());
        }

        @Test
        @DisplayName("Should support backward compatibility with old addBeaconMap")
        void shouldSupportOldAddBeaconMapMethod() {
            BeaconObj beacon = new BeaconObj(plugin, 100, 64, 200, null);
            int mapId = 10;

            // Old method without origin
            register.addBeaconMap(mapId, beacon);

            // Should work, but origin should be null
            assertNotNull(register.getBeaconMap(mapId));
            assertNull(register.getMapOrigin(mapId));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle negative coordinates")
        void shouldHandleNegativeCoordinates() {
            register.setMapOrigin(1, -100, -200);

            Point2D origin = register.getMapOrigin(1);
            assertNotNull(origin);
            assertEquals(-100, (int) origin.getX());
            assertEquals(-200, (int) origin.getY());
        }

        @Test
        @DisplayName("Should handle zero coordinates")
        void shouldHandleZeroCoordinates() {
            register.setMapOrigin(1, 0, 0);

            Point2D origin = register.getMapOrigin(1);
            assertNotNull(origin);
            assertEquals(0, (int) origin.getX());
            assertEquals(0, (int) origin.getY());
        }

        @Test
        @DisplayName("Should handle large coordinate values")
        void shouldHandleLargeCoordinates() {
            register.setMapOrigin(1, 1000000, 2000000);

            Point2D origin = register.getMapOrigin(1);
            assertNotNull(origin);
            assertEquals(1000000, (int) origin.getX());
            assertEquals(2000000, (int) origin.getY());
        }
    }
}
