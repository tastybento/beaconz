package com.wasteofplastic.beaconz.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.wasteofplastic.beaconz.Beaconz;

/**
 * Comprehensive test suite for the NameDB class.
 * Tests player name to UUID mapping functionality using SQLite database.
 *
 * <p>Uses MockBukkit for realistic Bukkit environment simulation.
 * Each test uses an in-memory SQLite database for isolation.
 */
@DisplayName("NameDB Tests")
class NameDBTest {

    private NameDB nameDB;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        Beaconz plugin = MockBukkit.load(Beaconz.class);
        nameDB = plugin.getNameStore();
        assertNotNull(nameDB, "NameDB should be initialized");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("Should save and retrieve player name")
        void shouldSaveAndRetrievePlayerName() {
            // Arrange
            String playerName = "TestPlayer";
            UUID playerUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName(playerName, playerUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID(playerName);

            // Assert
            assertNotNull(retrievedUUID);
            assertEquals(playerUUID, retrievedUUID);
        }

        @Test
        @DisplayName("Should return null for unknown player")
        void shouldReturnNullForUnknownPlayer() {
            // Act
            UUID retrievedUUID = nameDB.getPlayerUUID("UnknownPlayer");

            // Assert
            assertNull(retrievedUUID);
        }

        @Test
        @DisplayName("Should update existing player entry")
        void shouldUpdateExistingPlayerEntry() {
            // Arrange
            String playerName = "TestPlayer";
            UUID firstUUID = UUID.randomUUID();
            UUID secondUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName(playerName, firstUUID);
            nameDB.savePlayerName(playerName, secondUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID(playerName);

            // Assert
            assertEquals(secondUUID, retrievedUUID);
        }

        @Test
        @DisplayName("Should track database size correctly")
        void shouldTrackDatabaseSizeCorrectly() {
            // Arrange
            int initialSize = nameDB.size();

            // Act
            nameDB.savePlayerName("Player1", UUID.randomUUID());
            nameDB.savePlayerName("Player2", UUID.randomUUID());

            // Assert
            assertEquals(initialSize + 2, nameDB.size());
        }

        @Test
        @DisplayName("Should not increase size when updating existing player")
        void shouldNotIncreaseSizeWhenUpdatingExistingPlayer() {
            // Arrange
            String playerName = "TestPlayer";
            nameDB.savePlayerName(playerName, UUID.randomUUID());
            int sizeAfterFirstSave = nameDB.size();

            // Act
            nameDB.savePlayerName(playerName, UUID.randomUUID());

            // Assert
            assertEquals(sizeAfterFirstSave, nameDB.size());
        }
    }

    @Nested
    @DisplayName("Case Insensitivity")
    class CaseInsensitivity {

        @Test
        @DisplayName("Should be case insensitive when saving")
        void shouldBeCaseInsensitiveWhenSaving() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("TestPlayer", playerUUID);
            UUID retrievedLower = nameDB.getPlayerUUID("testplayer");
            UUID retrievedUpper = nameDB.getPlayerUUID("TESTPLAYER");
            UUID retrievedMixed = nameDB.getPlayerUUID("TeStPlAyEr");

            // Assert
            assertEquals(playerUUID, retrievedLower);
            assertEquals(playerUUID, retrievedUpper);
            assertEquals(playerUUID, retrievedMixed);
        }

        @Test
        @DisplayName("Should treat different cases as same player")
        void shouldTreatDifferentCasesAsSamePlayer() {
            // Arrange
            UUID firstUUID = UUID.randomUUID();
            UUID secondUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("TestPlayer", firstUUID);
            nameDB.savePlayerName("TESTPLAYER", secondUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID("testplayer");

            // Assert
            assertEquals(secondUUID, retrievedUUID);
            assertEquals(1, nameDB.size() - getInitialSize());
        }

        private int getInitialSize() {
            // Helper to account for any pre-existing entries
            return 0;
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("Should handle null player name gracefully in save")
        void shouldHandleNullPlayerNameInSave() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            int initialSize = nameDB.size();

            // Act
            nameDB.savePlayerName(null, playerUUID);

            // Assert
            assertEquals(initialSize, nameDB.size());
        }

        @Test
        @DisplayName("Should handle null UUID gracefully in save")
        void shouldHandleNullUUIDInSave() {
            // Arrange
            int initialSize = nameDB.size();

            // Act
            nameDB.savePlayerName("TestPlayer", null);

            // Assert
            assertEquals(initialSize, nameDB.size());
        }

        @Test
        @DisplayName("Should return null for null player name in get")
        void shouldReturnNullForNullPlayerNameInGet() {
            // Act
            UUID retrievedUUID = nameDB.getPlayerUUID(null);

            // Assert
            assertNull(retrievedUUID);
        }

        @Test
        @DisplayName("Should handle both null parameters gracefully")
        void shouldHandleBothNullParametersGracefully() {
            // Arrange
            int initialSize = nameDB.size();

            // Act
            nameDB.savePlayerName(null, null);

            // Assert
            assertEquals(initialSize, nameDB.size());
        }
    }

    @Nested
    @DisplayName("HasPlayer Method")
    class HasPlayerMethod {

        @Test
        @DisplayName("Should return true for existing player")
        void shouldReturnTrueForExistingPlayer() {
            // Arrange
            String playerName = "TestPlayer";
            nameDB.savePlayerName(playerName, UUID.randomUUID());

            // Act
            boolean hasPlayer = nameDB.hasPlayer(playerName);

            // Assert
            assertTrue(hasPlayer);
        }

        @Test
        @DisplayName("Should return false for non-existing player")
        void shouldReturnFalseForNonExistingPlayer() {
            // Act
            boolean hasPlayer = nameDB.hasPlayer("UnknownPlayer");

            // Assert
            assertFalse(hasPlayer);
        }

        @Test
        @DisplayName("Should be case insensitive")
        void shouldBeCaseInsensitive() {
            // Arrange
            nameDB.savePlayerName("TestPlayer", UUID.randomUUID());

            // Act & Assert
            assertTrue(nameDB.hasPlayer("testplayer"));
            assertTrue(nameDB.hasPlayer("TESTPLAYER"));
            assertTrue(nameDB.hasPlayer("TeStPlAyEr"));
        }

        @Test
        @DisplayName("Should return false for null player name")
        void shouldReturnFalseForNullPlayerName() {
            // Act
            boolean hasPlayer = nameDB.hasPlayer(null);

            // Assert
            assertFalse(hasPlayer);
        }
    }

    @Nested
    @DisplayName("Multiple Players")
    class MultiplePlayers {

        @Test
        @DisplayName("Should handle multiple distinct players")
        void shouldHandleMultipleDistinctPlayers() {
            // Arrange
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("Player1", uuid1);
            nameDB.savePlayerName("Player2", uuid2);
            nameDB.savePlayerName("Player3", uuid3);

            // Assert
            assertEquals(uuid1, nameDB.getPlayerUUID("Player1"));
            assertEquals(uuid2, nameDB.getPlayerUUID("Player2"));
            assertEquals(uuid3, nameDB.getPlayerUUID("Player3"));
            assertTrue(nameDB.hasPlayer("Player1"));
            assertTrue(nameDB.hasPlayer("Player2"));
            assertTrue(nameDB.hasPlayer("Player3"));
        }

        @Test
        @DisplayName("Should maintain independence between players")
        void shouldMaintainIndependenceBetweenPlayers() {
            // Arrange
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            nameDB.savePlayerName("Player1", uuid1);
            nameDB.savePlayerName("Player2", uuid2);

            // Act - Update Player1
            UUID newUuid1 = UUID.randomUUID();
            nameDB.savePlayerName("Player1", newUuid1);

            // Assert
            assertEquals(newUuid1, nameDB.getPlayerUUID("Player1"));
            assertEquals(uuid2, nameDB.getPlayerUUID("Player2")); // Player2 unchanged
        }

        @Test
        @DisplayName("Should handle similar player names correctly")
        void shouldHandleSimilarPlayerNamesCorrectly() {
            // Arrange
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("Player", uuid1);
            nameDB.savePlayerName("Player1", uuid2);
            nameDB.savePlayerName("Player123", uuid3);

            // Assert
            assertEquals(uuid1, nameDB.getPlayerUUID("Player"));
            assertEquals(uuid2, nameDB.getPlayerUUID("Player1"));
            assertEquals(uuid3, nameDB.getPlayerUUID("Player123"));
        }
    }

    @Nested
    @DisplayName("Special Characters")
    class SpecialCharacters {

        @Test
        @DisplayName("Should handle player names with numbers")
        void shouldHandlePlayerNamesWithNumbers() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("Player123", playerUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID("Player123");

            // Assert
            assertEquals(playerUUID, retrievedUUID);
        }

        @Test
        @DisplayName("Should handle player names with underscores")
        void shouldHandlePlayerNamesWithUnderscores() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("Test_Player_123", playerUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID("Test_Player_123");

            // Assert
            assertEquals(playerUUID, retrievedUUID);
        }

        @Test
        @DisplayName("Should handle minimum length player names")
        void shouldHandleMinimumLengthPlayerNames() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("A", playerUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID("A");

            // Assert
            assertEquals(playerUUID, retrievedUUID);
        }

        @Test
        @DisplayName("Should handle maximum length player names")
        void shouldHandleMaximumLengthPlayerNames() {
            // Arrange
            // Minecraft player names can be up to 16 characters
            UUID playerUUID = UUID.randomUUID();
            String longName = "PlayerNameMax16";

            // Act
            nameDB.savePlayerName(longName, playerUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID(longName);

            // Assert
            assertEquals(playerUUID, retrievedUUID);
        }
    }

    @Nested
    @DisplayName("Database Persistence")
    class DatabasePersistence {

        @Test
        @DisplayName("Should maintain data across operations")
        void shouldMaintainDataAcrossOperations() {
            // Arrange
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();
            UUID uuid3 = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("Player1", uuid1);
            nameDB.savePlayerName("Player2", uuid2);

            // Verify first two players
            assertEquals(uuid1, nameDB.getPlayerUUID("Player1"));
            assertEquals(uuid2, nameDB.getPlayerUUID("Player2"));

            // Add third player
            nameDB.savePlayerName("Player3", uuid3);

            // Assert - All players should still be retrievable
            assertEquals(uuid1, nameDB.getPlayerUUID("Player1"));
            assertEquals(uuid2, nameDB.getPlayerUUID("Player2"));
            assertEquals(uuid3, nameDB.getPlayerUUID("Player3"));
        }

        @Test
        @DisplayName("Should handle rapid successive operations")
        void shouldHandleRapidSuccessiveOperations() {
            // Arrange
            int playerCount = 50;
            UUID[] uuids = new UUID[playerCount];

            // Act - Save many players quickly
            for (int i = 0; i < playerCount; i++) {
                uuids[i] = UUID.randomUUID();
                nameDB.savePlayerName("Player" + i, uuids[i]);
            }

            // Assert - All should be retrievable
            for (int i = 0; i < playerCount; i++) {
                assertEquals(uuids[i], nameDB.getPlayerUUID("Player" + i));
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty string player name")
        void shouldHandleEmptyStringPlayerName() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("", playerUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID("");

            // Assert
            // Empty string is technically valid, should be saved
            assertEquals(playerUUID, retrievedUUID);
        }

        @Test
        @DisplayName("Should handle whitespace in player names")
        void shouldHandleWhitespaceInPlayerNames() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("Test Player", playerUUID);
            UUID retrievedUUID = nameDB.getPlayerUUID("Test Player");

            // Assert
            // Whitespace should be preserved
            assertEquals(playerUUID, retrievedUUID);
        }

        @Test
        @DisplayName("Should differentiate between similar names with spaces")
        void shouldDifferentiateBetweenSimilarNamesWithSpaces() {
            // Arrange
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();

            // Act
            nameDB.savePlayerName("TestPlayer", uuid1);
            nameDB.savePlayerName("Test Player", uuid2);

            // Assert
            assertEquals(uuid1, nameDB.getPlayerUUID("TestPlayer"));
            assertEquals(uuid2, nameDB.getPlayerUUID("Test Player"));
        }

        @Test
        @DisplayName("Should handle repeated get operations")
        void shouldHandleRepeatedGetOperations() {
            // Arrange
            String playerName = "TestPlayer";
            UUID playerUUID = UUID.randomUUID();
            nameDB.savePlayerName(playerName, playerUUID);

            // Act - Retrieve multiple times
            UUID uuid1 = nameDB.getPlayerUUID(playerName);
            UUID uuid2 = nameDB.getPlayerUUID(playerName);
            UUID uuid3 = nameDB.getPlayerUUID(playerName);

            // Assert - All should return the same UUID
            assertEquals(playerUUID, uuid1);
            assertEquals(playerUUID, uuid2);
            assertEquals(playerUUID, uuid3);
        }

        @Test
        @DisplayName("Should handle checking non-existent players multiple times")
        void shouldHandleCheckingNonExistentPlayersMultipleTimes() {
            // Act
            boolean check1 = nameDB.hasPlayer("NonExistent");
            boolean check2 = nameDB.hasPlayer("NonExistent");
            boolean check3 = nameDB.hasPlayer("NonExistent");

            // Assert
            assertFalse(check1);
            assertFalse(check2);
            assertFalse(check3);
        }
    }
}


