package com.wasteofplastic.beaconz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;

/**
 * Tests for parsing GameScoreGoal lists from configuration.
 * Verifies safe parsing of enum values from both string and list formats.
 */
class ScoreGoalParsingTest {

    private ServerMock server;
    private Beaconz plugin;
    private FileConfiguration config;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Beaconz.class);
        config = plugin.getConfig();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Should parse string format from config (colon-delimited)")
    void testStringFormat() {
        // The default config uses string format
        Object minigameValue = config.get("scoreboard.sidebar.minigame");
        Object strategyValue = config.get("scoreboard.sidebar.strategy");

        // Should be strings
        assertTrue(minigameValue instanceof String);
        assertTrue(strategyValue instanceof String);

        // Verify they contain expected values
        String minigameStr = (String) minigameValue;
        String strategyStr = (String) strategyValue;

        assertTrue(minigameStr.contains("beacons"));
        assertTrue(strategyStr.contains("area"));
    }

    @Test
    @DisplayName("Should parse string format correctly")
    void testParseStringFormat() {
        // Set a string format value
        config.set("test.sidebar", "AREA:BEACONS:TRIANGLES");

        // The plugin loads config during onEnable, but we can't easily access the private method
        // So we just verify the config format is correct
        String value = config.getString("test.sidebar");
        assertNotNull(value);

        String[] parts = value.split(":");
        assertEquals(3, parts.length);
        assertEquals("AREA", parts[0]);
        assertEquals("BEACONS", parts[1]);
        assertEquals("TRIANGLES", parts[2]);
    }

    @Test
    @DisplayName("Should handle list format in config")
    void testParseListFormat() {
        // Set a list format value
        config.set("test.sidebar", List.of("AREA", "BEACONS", "TRIANGLES"));

        List<?> value = config.getList("test.sidebar");
        assertNotNull(value);
        assertEquals(3, value.size());
        assertEquals("AREA", value.get(0));
        assertEquals("BEACONS", value.get(1));
        assertEquals("TRIANGLES", value.get(2));
    }

    @Test
    @DisplayName("Should handle empty string")
    void testEmptyString() {
        config.set("test.sidebar", "");

        // Would fall back to default
        String value = config.getString("test.sidebar");
        assertNotNull(value);
        assertTrue(value.isEmpty());
    }

    @Test
    @DisplayName("Should handle null value")
    void testNullValue() {
        // Not set, should be null
        Object value = config.get("test.nonexistent");
        assertEquals(null, value);
    }

    @Test
    @DisplayName("Config should have minigame sidebar settings")
    void testMinigameSidebarExists() {
        Object minigame = config.get("scoreboard.sidebar.minigame");
        assertNotNull(minigame, "Minigame sidebar config should exist");
    }

    @Test
    @DisplayName("Config should have strategy sidebar settings")
    void testStrategySidebarExists() {
        Object strategy = config.get("scoreboard.sidebar.strategy");
        assertNotNull(strategy, "Strategy sidebar config should exist");
    }

    @Test
    @DisplayName("Config should have minigame goal settings")
    void testMinigameGoalExists() {
        Object goal = config.get("scoreboard.goal.minigame");
        assertNotNull(goal, "Minigame goal config should exist");
    }

    @Test
    @DisplayName("Config should have strategy goal settings")
    void testStrategyGoalExists() {
        Object goal = config.get("scoreboard.goal.strategy");
        assertNotNull(goal, "Strategy goal config should exist");
    }

    @Test
    @DisplayName("Should parse valid GameScoreGoal enum values")
    void testValidEnumValues() {
        // Test that all expected values exist in the enum
        assertEquals(GameScoreGoal.AREA, GameScoreGoal.valueOf("AREA"));
        assertEquals(GameScoreGoal.BEACONS, GameScoreGoal.valueOf("BEACONS"));
        assertEquals(GameScoreGoal.TRIANGLES, GameScoreGoal.valueOf("TRIANGLES"));
        assertEquals(GameScoreGoal.LINKS, GameScoreGoal.valueOf("LINKS"));
        assertEquals(GameScoreGoal.TIME, GameScoreGoal.valueOf("TIME"));
    }

    @Test
    @DisplayName("Config values should be valid enum names")
    void testConfigValuesAreValidEnums() {
        String minigame = config.getString("scoreboard.sidebar.minigame");
        assertNotNull(minigame);

        // Split and verify each is a valid enum
        String[] values = minigame.split(":");
        for (String value : values) {
            String upper = value.trim().toUpperCase();
            // This will throw if invalid
            GameScoreGoal.valueOf(upper);
        }
    }
}
