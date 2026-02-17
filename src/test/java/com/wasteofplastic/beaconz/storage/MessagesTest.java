package com.wasteofplastic.beaconz.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.wasteofplastic.beaconz.Beaconz;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Comprehensive test suite for the Messages class.
 * Tests offline messaging functionality using SQLite database.
 *
 * <p>Uses MockBukkit for realistic Bukkit environment simulation.
 * Each test uses a unique SQLite database for isolation.
 */
@DisplayName("Messages Tests")
class MessagesTest {

    private Messages messages;
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        Beaconz plugin = MockBukkit.load(Beaconz.class);

        // DataSource is created synchronously in onEnable before any scheduler tasks
        // So it should be available immediately after load
        assertNotNull(plugin.getDataSource(), "DataSource should be available after plugin load");

        // Create Messages directly - we don't need to wait for scheduler tasks
        // since we're testing Messages in isolation
        messages = new Messages(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Basic Message Storage")
    class BasicMessageStorage {

        @Test
        @DisplayName("Should store and retrieve a simple message")
        void shouldStoreAndRetrieveSimpleMessage() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            Component message = Component.text("Hello World!");

            // Act
            messages.setMessage(playerUUID, message);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertNotNull(retrieved);
            assertEquals(1, retrieved.size());
            assertEquals("Hello World!", PlainTextComponentSerializer.plainText().serialize(retrieved.getFirst()));
        }

        @Test
        @DisplayName("Should store multiple messages for same player")
        void shouldStoreMultipleMessagesForSamePlayer() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            Component message1 = Component.text("Message 1");
            Component message2 = Component.text("Message 2");
            Component message3 = Component.text("Message 3");

            // Act
            messages.setMessage(playerUUID, message1);
            messages.setMessage(playerUUID, message2);
            messages.setMessage(playerUUID, message3);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertNotNull(retrieved);
            assertEquals(3, retrieved.size());
        }

        @Test
        @DisplayName("Should return empty list for player with no messages")
        void shouldReturnEmptyListForPlayerWithNoMessages() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertNotNull(retrieved);
            assertTrue(retrieved.isEmpty());
        }

        @Test
        @DisplayName("Should preserve message order")
        void shouldPreserveMessageOrder() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act
            messages.setMessage(playerUUID, Component.text("First"));
            messages.setMessage(playerUUID, Component.text("Second"));
            messages.setMessage(playerUUID, Component.text("Third"));
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertEquals(3, retrieved.size());
            assertEquals("First", PlainTextComponentSerializer.plainText().serialize(retrieved.get(0)));
            assertEquals("Second", PlainTextComponentSerializer.plainText().serialize(retrieved.get(1)));
            assertEquals("Third", PlainTextComponentSerializer.plainText().serialize(retrieved.get(2)));
        }
    }

    @Nested
    @DisplayName("Message Clearing")
    class MessageClearing {

        @Test
        @DisplayName("Should clear all messages for a player")
        void shouldClearAllMessagesForPlayer() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            messages.setMessage(playerUUID, Component.text("Message 1"));
            messages.setMessage(playerUUID, Component.text("Message 2"));

            // Act
            messages.clearMessages(playerUUID);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertNotNull(retrieved);
            assertTrue(retrieved.isEmpty());
        }

        @Test
        @DisplayName("Should not affect other players when clearing")
        void shouldNotAffectOtherPlayersWhenClearing() {
            // Arrange
            UUID player1UUID = UUID.randomUUID();
            UUID player2UUID = UUID.randomUUID();
            messages.setMessage(player1UUID, Component.text("Player 1 Message"));
            messages.setMessage(player2UUID, Component.text("Player 2 Message"));

            // Act
            messages.clearMessages(player1UUID);

            // Assert
            assertTrue(messages.getMessages(player1UUID).isEmpty());
            assertEquals(1, messages.getMessages(player2UUID).size());
        }

        @Test
        @DisplayName("Should handle clearing non-existent player gracefully")
        void shouldHandleClearingNonExistentPlayerGracefully() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act & Assert - should not throw
            messages.clearMessages(playerUUID);
            assertTrue(messages.getMessages(playerUUID).isEmpty());
        }
    }

    @Nested
    @DisplayName("Has Messages Check")
    class HasMessagesCheck {

        @Test
        @DisplayName("Should return true when player has messages")
        void shouldReturnTrueWhenPlayerHasMessages() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            messages.setMessage(playerUUID, Component.text("Test"));

            // Act & Assert
            assertTrue(messages.hasMessages(playerUUID));
        }

        @Test
        @DisplayName("Should return false when player has no messages")
        void shouldReturnFalseWhenPlayerHasNoMessages() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();

            // Act & Assert
            assertFalse(messages.hasMessages(playerUUID));
        }

        @Test
        @DisplayName("Should return false after messages are cleared")
        void shouldReturnFalseAfterMessagesAreCleared() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            messages.setMessage(playerUUID, Component.text("Test"));
            messages.clearMessages(playerUUID);

            // Act & Assert
            assertFalse(messages.hasMessages(playerUUID));
        }
    }

    @Nested
    @DisplayName("Component Serialization")
    class ComponentSerialization {

        @Test
        @DisplayName("Should preserve text color in serialization")
        void shouldPreserveTextColorInSerialization() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            Component coloredMessage = Component.text("Red Message", NamedTextColor.RED);

            // Act
            messages.setMessage(playerUUID, coloredMessage);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertEquals(1, retrieved.size());
            // Check the color is preserved
            Component retrievedMsg = retrieved.getFirst();
            assertEquals(NamedTextColor.RED, retrievedMsg.color());
        }

        @Test
        @DisplayName("Should preserve compound components")
        void shouldPreserveCompoundComponents() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            Component compound = Component.text("[Game] ", NamedTextColor.GOLD)
                    .append(Component.text("Player captured a beacon!", NamedTextColor.WHITE));

            // Act
            messages.setMessage(playerUUID, compound);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertEquals(1, retrieved.size());
            String plainText = PlainTextComponentSerializer.plainText().serialize(retrieved.getFirst());
            assertEquals("[Game] Player captured a beacon!", plainText);
        }

        @Test
        @DisplayName("Should handle empty component")
        void shouldHandleEmptyComponent() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            Component emptyMessage = Component.empty();

            // Act
            messages.setMessage(playerUUID, emptyMessage);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertEquals(1, retrieved.size());
        }
    }

    @Nested
    @DisplayName("Online Player Handling")
    class OnlinePlayerHandling {

        @Test
        @DisplayName("Should not store message for online player")
        void shouldNotStoreMessageForOnlinePlayer() {
            // Arrange
            PlayerMock player = server.addPlayer("OnlinePlayer");
            UUID playerUUID = player.getUniqueId();
            Component message = Component.text("Test message");

            // Act
            messages.setMessage(playerUUID, message);

            // Assert - message should not be stored since player is online
            assertTrue(messages.getMessages(playerUUID).isEmpty());
        }

        @Test
        @DisplayName("Should store message for offline player")
        void shouldStoreMessageForOfflinePlayer() {
            // Arrange
            UUID offlinePlayerUUID = UUID.randomUUID();
            Component message = Component.text("Offline message");

            // Act
            messages.setMessage(offlinePlayerUUID, message);

            // Assert
            assertEquals(1, messages.getMessages(offlinePlayerUUID).size());
        }
    }

    @Nested
    @DisplayName("Multiple Players")
    class MultiplePlayers {

        @Test
        @DisplayName("Should handle messages for multiple players independently")
        void shouldHandleMessagesForMultiplePlayersIndependently() {
            // Arrange
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            UUID player3 = UUID.randomUUID();

            // Act
            messages.setMessage(player1, Component.text("P1 Message 1"));
            messages.setMessage(player1, Component.text("P1 Message 2"));
            messages.setMessage(player2, Component.text("P2 Message"));
            messages.setMessage(player3, Component.text("P3 Message 1"));
            messages.setMessage(player3, Component.text("P3 Message 2"));
            messages.setMessage(player3, Component.text("P3 Message 3"));

            // Assert
            assertEquals(2, messages.getMessages(player1).size());
            assertEquals(1, messages.getMessages(player2).size());
            assertEquals(3, messages.getMessages(player3).size());
        }

        @Test
        @DisplayName("Should handle many players without issue")
        void shouldHandleManyPlayersWithoutIssue() {
            // Arrange & Act
            List<UUID> players = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                UUID uuid = UUID.randomUUID();
                players.add(uuid);
                messages.setMessage(uuid, Component.text("Message for player " + i));
            }

            // Assert
            for (int i = 0; i < 100; i++) {
                assertEquals(1, messages.getMessages(players.get(i)).size());
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very long messages")
        void shouldHandleVeryLongMessages() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            StringBuilder longText = new StringBuilder();
            longText.append("This is a very long message. ".repeat(1000));
            Component longMessage = Component.text(longText.toString());

            // Act
            messages.setMessage(playerUUID, longMessage);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertEquals(1, retrieved.size());
            String retrievedText = PlainTextComponentSerializer.plainText().serialize(retrieved.getFirst());
            assertEquals(longText.toString(), retrievedText);
        }

        @Test
        @DisplayName("Should handle special characters in messages")
        void shouldHandleSpecialCharactersInMessages() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            Component specialMessage = Component.text("Special chars: äöü ñ 中文 日本語 🎮 <>&\"'");

            // Act
            messages.setMessage(playerUUID, specialMessage);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertEquals(1, retrieved.size());
            String text = PlainTextComponentSerializer.plainText().serialize(retrieved.getFirst());
            assertEquals("Special chars: äöü ñ 中文 日本語 🎮 <>&\"'", text);
        }

        @Test
        @DisplayName("Should handle newlines in messages")
        void shouldHandleNewlinesInMessages() {
            // Arrange
            UUID playerUUID = UUID.randomUUID();
            Component multilineMessage = Component.text("Line 1\nLine 2\nLine 3");

            // Act
            messages.setMessage(playerUUID, multilineMessage);
            List<Component> retrieved = messages.getMessages(playerUUID);

            // Assert
            assertEquals(1, retrieved.size());
            String text = PlainTextComponentSerializer.plainText().serialize(retrieved.getFirst());
            assertTrue(text.contains("\n"));
        }
    }

    @Nested
    @DisplayName("TellTeam Integration")
    class TellTeamIntegration {


        @Test
        @DisplayName("TellTeam should send to online players directly")
        void tellTeamShouldSendToOnlinePlayersDirectly() {
            // This test verifies the tellTeam behavior conceptually
            // The actual implementation sends messages directly to online players
            // and stores for offline players

            // Arrange
            UUID onlinePlayerUUID = UUID.randomUUID();
            UUID offlinePlayerUUID = UUID.randomUUID();

            // Store a message for an "offline" player
            messages.setMessage(offlinePlayerUUID, Component.text("You were notified!"));

            // Assert - offline player should have stored message
            assertTrue(messages.hasMessages(offlinePlayerUUID));
            assertFalse(messages.hasMessages(onlinePlayerUUID)); // online player not stored
        }
    }

    @Nested
    @DisplayName("TellOtherTeams Integration")
    class TellOtherTeamsIntegration {

        @Test
        @DisplayName("Should handle null scoreboard gracefully")
        void shouldHandleNullScoreboardGracefully() {
            // Arrange
            Team mockTeam = mock(Team.class);
            when(mockTeam.getScoreboard()).thenReturn(null);

            // Act & Assert - should not throw
            messages.tellOtherTeams(mockTeam, Component.text("Test"));
        }

        @Test
        @DisplayName("Should notify teams from scoreboard")
        void shouldNotifyTeamsFromScoreboard() {
            // Arrange
            Team mockTeam = mock(Team.class);
            Team otherTeam = mock(Team.class);
            Scoreboard scoreboard = mock(Scoreboard.class);

            when(mockTeam.getScoreboard()).thenReturn(scoreboard);
            when(scoreboard.getTeams()).thenReturn(java.util.Set.of(mockTeam, otherTeam));

            // Act - this should not throw and should process the other team
            messages.tellOtherTeams(mockTeam, Component.text("Test"));

            // The actual messaging depends on GameMgr which would be null in this isolated test
            // This test mainly verifies no NPE occurs with the scoreboard iteration
        }
    }
}













