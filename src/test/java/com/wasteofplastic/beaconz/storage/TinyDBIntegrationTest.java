package com.wasteofplastic.beaconz.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import com.wasteofplastic.beaconz.Beaconz;

/**
 * Integration test to verify TinyDB works correctly with the SQLite database.
 */
@DisplayName("TinyDB Integration Tests")
class TinyDBIntegrationTest {

    private ServerMock server;
    private Beaconz plugin;
    private TinyDB tinyDB;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Beaconz.class);
        tinyDB = plugin.getNameStore();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should save and retrieve player name")
    void shouldSaveAndRetrievePlayerName() {
        // Given
        String playerName = "TestPlayer";
        UUID playerUUID = UUID.randomUUID();

        // When
        tinyDB.savePlayerName(playerName, playerUUID);

        // Then - should be immediately available from cache
        UUID retrieved = tinyDB.getPlayerUUID(playerName);
        assertEquals(playerUUID, retrieved, "Should retrieve the UUID from cache");
    }

    @Test
    @DisplayName("Should handle case insensitive lookups")
    void shouldHandleCaseInsensitiveLookups() {
        // Given
        String playerName = "MixedCasePlayer";
        UUID playerUUID = UUID.randomUUID();

        // When
        tinyDB.savePlayerName(playerName, playerUUID);

        // Then
        assertEquals(playerUUID, tinyDB.getPlayerUUID("mixedcaseplayer"));
        assertEquals(playerUUID, tinyDB.getPlayerUUID("MIXEDCASEPLAYER"));
        assertEquals(playerUUID, tinyDB.getPlayerUUID("MixedCasePlayer"));
    }

    @Test
    @DisplayName("Should persist data across instances")
    void shouldPersistDataAcrossInstances() throws InterruptedException {
        // Given
        String playerName = "PersistentPlayer";
        UUID playerUUID = UUID.randomUUID();

        // When
        tinyDB.savePlayerName(playerName, playerUUID);

        // Force save to database
        tinyDB.saveDB();

        // Wait a bit for async operations
        Thread.sleep(100);

        // Create a new instance (simulating plugin reload)
        TinyDB newInstance = new TinyDB(plugin);

        // Then
        UUID retrieved = newInstance.getPlayerUUID(playerName);
        assertEquals(playerUUID, retrieved, "Should retrieve the UUID from database after reload");
    }

    @Test
    @DisplayName("Should return correct size")
    void shouldReturnCorrectSize() {
        // Given
        assertEquals(0, tinyDB.size(), "Should start with 0 entries");

        // When
        tinyDB.savePlayerName("Player1", UUID.randomUUID());
        tinyDB.savePlayerName("Player2", UUID.randomUUID());

        // Then
        assertEquals(2, tinyDB.size(), "Should have 2 entries");
    }

    @Test
    @DisplayName("Should check if player exists")
    void shouldCheckIfPlayerExists() {
        // Given
        String playerName = "ExistingPlayer";
        UUID playerUUID = UUID.randomUUID();

        // When
        assertFalse(tinyDB.hasPlayer(playerName), "Should not have player initially");
        tinyDB.savePlayerName(playerName, playerUUID);

        // Then
        assertTrue(tinyDB.hasPlayer(playerName), "Should have player after saving");
        assertTrue(tinyDB.hasPlayer("existingplayer"), "Should be case insensitive");
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // When/Then - should not throw exceptions
        assertDoesNotThrow(() -> tinyDB.savePlayerName(null, UUID.randomUUID()));
        assertDoesNotThrow(() -> tinyDB.savePlayerName("Player", null));
        assertDoesNotThrow(() -> tinyDB.savePlayerName(null, null));

        assertNull(tinyDB.getPlayerUUID(null));
        assertFalse(tinyDB.hasPlayer(null));
    }

    @Test
    @DisplayName("Should update existing player UUID")
    void shouldUpdateExistingPlayerUUID() {
        // Given
        String playerName = "ChangingPlayer";
        UUID oldUUID = UUID.randomUUID();
        UUID newUUID = UUID.randomUUID();

        // When
        tinyDB.savePlayerName(playerName, oldUUID);
        assertEquals(oldUUID, tinyDB.getPlayerUUID(playerName));

        tinyDB.savePlayerName(playerName, newUUID);

        // Then
        assertEquals(newUUID, tinyDB.getPlayerUUID(playerName), "Should update to new UUID");
        assertEquals(1, tinyDB.size(), "Should still have only 1 entry");
    }
}

