package com.wasteofplastic.beaconz.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.wasteofplastic.beaconz.GlobalTestSetup;
import com.wasteofplastic.beaconz.config.Params.GameMode;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;

import net.kyori.adventure.text.Component;

/**
 * Comprehensive test suite for {@link Params} class.
 *
 * <p>This test class validates:
 * <ul>
 *   <li>Default constructor initialization</li>
 *   <li>Parameterized constructor</li>
 *   <li>String array constructor with valid parameters</li>
 *   <li>String array constructor validation and error handling</li>
 *   <li>Getter methods with null fallback to Settings</li>
 *   <li>Setter methods</li>
 *   <li>Enum getName() methods</li>
 * </ul>
 *
 * @author GitHub Copilot
 */
@ExtendWith(GlobalTestSetup.class)
class ParamsTest {

    @BeforeAll
    static void setUpBeforeClass() {
        // Initialize Settings static fields with test values
        Settings.gamemode = GameMode.STRATEGY;
        Settings.borderSize = 20000;
        Settings.defaultTeamNumber = 2;
        Settings.strategyGoal = GameScoreGoal.AREA;
        Settings.minigameGoal = GameScoreGoal.BEACONS;
        Settings.strategyGoalValue = 3000000;
        Settings.minigameGoalValue = 10;
        Settings.strategyTimer = 0;
        Settings.minigameTimer = 600;
        Settings.strategyScoreTypes = List.of(GameScoreGoal.AREA, GameScoreGoal.BEACONS);
        Settings.minigameScoreTypes = List.of(GameScoreGoal.BEACONS, GameScoreGoal.TRIANGLES);
        Settings.distribution = 0.2;

        // Initialize Lang components used in validation
        Lang.adminParmsArgumentsPairs = Component.text("Arguments must be given in pairs, separated by colons.");
        Lang.adminParmsDoesNotExist = Component.text("Parameter [name] does not exist.");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor initializes with expected values")
        void testDefaultConstructor() {
            Params params = new Params();

            assertEquals(GameMode.STRATEGY, params.getGamemode());
            assertEquals(20000, params.getSize());
            assertEquals(2, params.getTeams());
            assertEquals(GameScoreGoal.AREA, params.getGoal());
            assertEquals(3000000, params.getGoalvalue());
            assertEquals(0, params.getCountdown());
            assertEquals(List.of(GameScoreGoal.AREA), params.getScoretypes());
            assertEquals(0.2D, params.getDistribution());
        }

        @Test
        @DisplayName("Parameterized constructor sets all fields correctly")
        void testParameterizedConstructor() {
            List<GameScoreGoal> scoreTypes = List.of(GameScoreGoal.AREA, GameScoreGoal.BEACONS);

            Params params = new Params(
                GameMode.MINIGAME,
                10000,
                4,
                GameScoreGoal.TRIANGLES,
                500,
                300,
                scoreTypes,
                0.5D
            );

            assertEquals(GameMode.MINIGAME, params.getGamemode());
            assertEquals(10000, params.getSize());
            assertEquals(4, params.getTeams());
            assertEquals(GameScoreGoal.TRIANGLES, params.getGoal());
            assertEquals(500, params.getGoalvalue());
            assertEquals(300, params.getCountdown());
            assertEquals(scoreTypes, params.getScoretypes());
            assertEquals(0.5D, params.getDistribution());
        }

        @Test
        @DisplayName("String array constructor with valid gamemode:strategy")
        void testStringArrayConstructorValidStrategy() throws IOException {
            String[] args = {"gamemode:strategy", "size:15000", "teams:3"};

            Params params = new Params(args);

            assertEquals(GameMode.STRATEGY, params.getGamemode());
            assertEquals(15000, params.getSize());
            assertEquals(3, params.getTeams());
        }

        @Test
        @DisplayName("String array constructor with valid gamemode:minigame")
        void testStringArrayConstructorValidMinigame() throws IOException {
            String[] args = {"gamemode:minigame"};

            Params params = new Params(args);

            assertEquals(GameMode.MINIGAME, params.getGamemode());
        }

        @Test
        @DisplayName("String array constructor with valid goal types")
        void testStringArrayConstructorValidGoals() throws IOException {
            String[] args1 = {"goal:area"};
            Params params1 = new Params(args1);
            assertEquals(GameScoreGoal.AREA, params1.getGoal());

            String[] args2 = {"goal:triangles"};
            Params params2 = new Params(args2);
            assertEquals(GameScoreGoal.TRIANGLES, params2.getGoal());

            String[] args3 = {"goal:links"};
            Params params3 = new Params(args3);
            assertEquals(GameScoreGoal.LINKS, params3.getGoal());
        }

        @Test
        @DisplayName("String array constructor with valid distribution values")
        void testStringArrayConstructorValidDistribution() throws IOException {
            String[] args = {"distribution:0.75"};

            Params params = new Params(args);
            assertEquals(0.75, params.getDistribution());
        }

        @Test
        @DisplayName("String array constructor with multiple valid parameters")
        void testStringArrayConstructorMultipleValidParams() throws IOException {
            String[] args = {
                "gamemode:minigame",
                "size:5000",
                "teams:8",
                "goal:triangles",
                "distribution:0.3"
            };

            Params params = new Params(args);

            assertEquals(GameMode.MINIGAME, params.getGamemode());
            assertEquals(5000, params.getSize());
            assertEquals(8, params.getTeams());
            assertEquals(GameScoreGoal.TRIANGLES, params.getGoal());
            assertEquals(0.3, params.getDistribution());
        }

        @Test
        @DisplayName("Empty string array constructor succeeds")
        void testEmptyStringArrayConstructor() throws IOException {
            String[] args = {};
            Params params = new Params(args);
            assertNotNull(params);
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("getGamemode returns Settings value when null")
        void testGetGamemodeNullFallback() {
            Params params = new Params(null, null, null, null, null, null, null, null);
            assertEquals(Settings.gamemode, params.getGamemode());
        }

        @Test
        @DisplayName("getSize returns Settings value when null")
        void testGetSizeNullFallback() {
            Params params = new Params(null, null, null, null, null, null, null, null);
            assertEquals(Settings.borderSize, params.getSize());
        }

        @Test
        @DisplayName("getTeams returns Settings value when null")
        void testGetTeamsNullFallback() {
            Params params = new Params(null, null, null, null, null, null, null, null);
            assertEquals(Settings.defaultTeamNumber, params.getTeams());
        }

        @Test
        @DisplayName("getGoal returns Settings strategy value when gamemode is STRATEGY and goal is null")
        void testGetGoalNullFallbackStrategy() {
            Params params = new Params(GameMode.STRATEGY, null, null, null, null, null, null, null);
            assertEquals(Settings.strategyGoal, params.getGoal());
        }

        @Test
        @DisplayName("getGoal returns Settings minigame value when gamemode is MINIGAME and goal is null")
        void testGetGoalNullFallbackMinigame() {
            Params params = new Params(GameMode.MINIGAME, null, null, null, null, null, null, null);
            assertEquals(Settings.minigameGoal, params.getGoal());
        }

        @Test
        @DisplayName("getGoalvalue returns Settings strategy value when gamemode is STRATEGY and goalvalue is null")
        void testGetGoalvalueNullFallbackStrategy() {
            Params params = new Params(GameMode.STRATEGY, null, null, null, null, null, null, null);
            assertEquals(Settings.strategyGoalValue, params.getGoalvalue());
        }

        @Test
        @DisplayName("getGoalvalue returns Settings minigame value when gamemode is MINIGAME and goalvalue is null")
        void testGetGoalvalueNullFallbackMinigame() {
            Params params = new Params(GameMode.MINIGAME, null, null, null, null, null, null, null);
            assertEquals(Settings.minigameGoalValue, params.getGoalvalue());
        }

        @Test
        @DisplayName("getCountdown returns Settings strategy value when gamemode is STRATEGY and countdown is null")
        void testGetCountdownNullFallbackStrategy() {
            Params params = new Params(GameMode.STRATEGY, null, null, null, null, null, null, null);
            assertEquals(Settings.strategyTimer, params.getCountdown());
        }

        @Test
        @DisplayName("getCountdown returns Settings minigame value when gamemode is MINIGAME and countdown is null")
        void testGetCountdownNullFallbackMinigame() {
            Params params = new Params(GameMode.MINIGAME, null, null, null, null, null, null, null);
            assertEquals(Settings.minigameTimer, params.getCountdown());
        }

        @Test
        @DisplayName("getScoretypes returns Settings strategy value when gamemode is STRATEGY and scoretypes is null")
        void testGetScoretypesNullFallbackStrategy() {
            Params params = new Params(GameMode.STRATEGY, null, null, null, null, null, null, null);
            assertEquals(Settings.strategyScoreTypes, params.getScoretypes());
        }

        @Test
        @DisplayName("getScoretypes returns Settings minigame value when gamemode is MINIGAME and scoretypes is null")
        void testGetScoretypesNullFallbackMinigame() {
            Params params = new Params(GameMode.MINIGAME, null, null, null, null, null, null, null);
            assertEquals(Settings.minigameScoreTypes, params.getScoretypes());
        }

        @Test
        @DisplayName("getDistribution returns Settings value when null")
        void testGetDistributionNullFallback() {
            Params params = new Params(null, null, null, null, null, null, null, null);
            assertEquals(Settings.distribution, params.getDistribution());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        private Params params;

        @BeforeEach
        void setUp() {
            params = new Params();
        }

        @Test
        @DisplayName("setGamemode updates gamemode")
        void testSetGamemode() {
            params.setGamemode(GameMode.MINIGAME);
            assertEquals(GameMode.MINIGAME, params.getGamemode());
        }

        @Test
        @DisplayName("setSize updates size")
        void testSetSize() {
            params.setSize(15000);
            assertEquals(15000, params.getSize());
        }

        @Test
        @DisplayName("setTeams updates teams")
        void testSetTeams() {
            params.setTeams(5);
            assertEquals(5, params.getTeams());
        }

        @Test
        @DisplayName("setGoal updates goal")
        void testSetGoal() {
            params.setGoal(GameScoreGoal.LINKS);
            assertEquals(GameScoreGoal.LINKS, params.getGoal());
        }

        @Test
        @DisplayName("setGoalvalue updates goalvalue")
        void testSetGoalvalue() {
            params.setGoalvalue(1000);
            assertEquals(1000, params.getGoalvalue());
        }

        @Test
        @DisplayName("setCountdown updates countdown")
        void testSetCountdown() {
            params.setCountdown(500);
            assertEquals(500, params.getCountdown());
        }

        @Test
        @DisplayName("setScoretypes updates scoretypes")
        void testSetScoretypes() {
            List<GameScoreGoal> newScoreTypes = List.of(GameScoreGoal.TRIANGLES, GameScoreGoal.LINKS);
            params.setScoretypes(newScoreTypes);
            assertEquals(newScoreTypes, params.getScoretypes());
        }

        @Test
        @DisplayName("setDistribution updates distribution")
        void testSetDistribution() {
            params.setDistribution(0.8);
            assertEquals(0.8, params.getDistribution());
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("GameMode.STRATEGY getName returns correct value")
        void testGameModeStrategyGetName() {
            String name = GameMode.STRATEGY.getName();
            assertNotNull(name);
            assertEquals("Strategy", name);
        }

        @Test
        @DisplayName("GameMode.MINIGAME getName returns correct value")
        void testGameModeMinigameGetName() {
            String name = GameMode.MINIGAME.getName();
            assertNotNull(name);
            assertEquals("Minigame", name);
        }

        @Test
        @DisplayName("GameScoreGoal.AREA getName returns correct value")
        void testGameScoreGoalAreaGetName() {
            String name = GameScoreGoal.AREA.getName();
            assertNotNull(name);
            assertEquals("Area", name);
        }

        @Test
        @DisplayName("GameScoreGoal.BEACONS getName returns correct value")
        void testGameScoreGoalBeaconsGetName() {
            String name = GameScoreGoal.BEACONS.getName();
            assertNotNull(name);
            assertEquals("Beacons", name);
        }

        @Test
        @DisplayName("GameScoreGoal.TIME getName returns correct value")
        void testGameScoreGoalTimeGetName() {
            String name = GameScoreGoal.TIME.getName();
            assertNotNull(name);
            assertEquals("Time", name);
        }

        @Test
        @DisplayName("GameScoreGoal.TRIANGLES getName returns correct value")
        void testGameScoreGoalTrianglesGetName() {
            String name = GameScoreGoal.TRIANGLES.getName();
            assertNotNull(name);
            assertEquals("Triangles", name);
        }

        @Test
        @DisplayName("GameScoreGoal.LINKS getName returns correct value")
        void testGameScoreGoalLinksGetName() {
            String name = GameScoreGoal.LINKS.getName();
            assertNotNull(name);
            assertEquals("Links", name);
        }

        @Test
        @DisplayName("All GameMode values are defined")
        void testAllGameModeValues() {
            GameMode[] modes = GameMode.values();
            assertEquals(2, modes.length);
            assertEquals(GameMode.STRATEGY, modes[0]);
            assertEquals(GameMode.MINIGAME, modes[1]);
        }

        @Test
        @DisplayName("All GameScoreGoal values are defined")
        void testAllGameScoreGoalValues() {
            GameScoreGoal[] goals = GameScoreGoal.values();
            assertEquals(5, goals.length);
            assertEquals(GameScoreGoal.AREA, goals[0]);
            assertEquals(GameScoreGoal.BEACONS, goals[1]);
            assertEquals(GameScoreGoal.TIME, goals[2]);
            assertEquals(GameScoreGoal.TRIANGLES, goals[3]);
            assertEquals(GameScoreGoal.LINKS, goals[4]);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Case insensitive parameter names")
        void testCaseInsensitiveParameters() throws IOException {
            String[] args1 = {"GAMEMODE:strategy"};
            Params params1 = new Params(args1);
            assertEquals(GameMode.STRATEGY, params1.getGamemode());

            String[] args2 = {"Size:10000"};
            Params params2 = new Params(args2);
            assertEquals(10000, params2.getSize());
        }

        @Test
        @DisplayName("Case insensitive parameter values")
        void testCaseInsensitiveValues() throws IOException {
            String[] args1 = {"gamemode:STRATEGY"};
            Params params1 = new Params(args1);
            assertEquals(GameMode.STRATEGY, params1.getGamemode());

            String[] args2 = {"goal:AREA"};
            Params params2 = new Params(args2);
            assertEquals(GameScoreGoal.AREA, params2.getGoal());
        }

        @Test
        @DisplayName("Valid teams boundary values")
        void testValidTeamsBoundaries() throws IOException {
            String[] args1 = {"teams:2"};
            Params params1 = new Params(args1);
            assertEquals(2, params1.getTeams());

            String[] args2 = {"teams:14"};
            Params params2 = new Params(args2);
            assertEquals(14, params2.getTeams());
        }

        @Test
        @DisplayName("Valid distribution boundary values")
        void testValidDistributionBoundaries() throws IOException {
            String[] args1 = {"distribution:0.01"};
            Params params1 = new Params(args1);
            assertEquals(0.01, params1.getDistribution());

            String[] args2 = {"distribution:0.99"};
            Params params2 = new Params(args2);
            assertEquals(0.99, params2.getDistribution());
        }

        @Test
        @DisplayName("Complete game setup with all working parameters")
        void testCompleteGameSetup() throws IOException {
            String[] args = {
                "gamemode:minigame",
                "size:8000",
                "teams:6",
                "goal:area",
                "goalvalue:500",
                "countdown:1200",
                "distribution:0.4"
            };

            Params params = new Params(args);

            assertEquals(GameMode.MINIGAME, params.getGamemode());
            assertEquals(8000, params.getSize());
            assertEquals(6, params.getTeams());
            assertEquals(GameScoreGoal.AREA, params.getGoal());
            assertEquals(500, params.getGoalvalue());
            assertEquals(1200, params.getCountdown());
            assertEquals(0.4, params.getDistribution());
        }

        @Test
        @DisplayName("Partial parameters use Settings fallback correctly")
        void testPartialParametersWithFallback() throws IOException {
            String[] args = {
                "gamemode:strategy",
                "teams:10"
            };

            Params params = new Params(args);

            assertEquals(GameMode.STRATEGY, params.getGamemode());
            assertEquals(10, params.getTeams());
            // These should fall back to Settings
            assertEquals(Settings.borderSize, params.getSize());
            assertEquals(Settings.strategyGoal, params.getGoal());
            assertEquals(Settings.strategyGoalValue, params.getGoalvalue());
            assertEquals(Settings.strategyTimer, params.getCountdown());
            assertEquals(Settings.distribution, params.getDistribution());
        }

        @Test
        @DisplayName("Strategy mode uses strategy defaults")
        void testStrategyModeDefaults() {
            Params params = new Params(GameMode.STRATEGY, null, null, null, null, null, null, null);

            assertEquals(Settings.strategyGoal, params.getGoal());
            assertEquals(Settings.strategyGoalValue, params.getGoalvalue());
            assertEquals(Settings.strategyTimer, params.getCountdown());
            assertEquals(Settings.strategyScoreTypes, params.getScoretypes());
        }

        @Test
        @DisplayName("Minigame mode uses minigame defaults")
        void testMinigameModeDefaults() {
            Params params = new Params(GameMode.MINIGAME, null, null, null, null, null, null, null);

            assertEquals(Settings.minigameGoal, params.getGoal());
            assertEquals(Settings.minigameGoalValue, params.getGoalvalue());
            assertEquals(Settings.minigameTimer, params.getCountdown());
            assertEquals(Settings.minigameScoreTypes, params.getScoretypes());
        }
    }

    @Nested
    @DisplayName("Validation Error Tests")
    class ValidationTests {

        @Test
        @DisplayName("Invalid gamemode throws IOException")
        void testInvalidGamemode() {
            String[] args = {"gamemode:invalid"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("gamemode"));
        }

        @Test
        @DisplayName("Non-numeric size throws IOException")
        void testNonNumericSize() {
            String[] args = {"size:notanumber"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("size"));
        }

        @Test
        @DisplayName("Teams below minimum throws IOException")
        void testTeamsBelowMinimum() {
            String[] args = {"teams:1"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("team"));
            assertTrue(exception.getMessage().contains("2 and 14"));
        }

        @Test
        @DisplayName("Teams above maximum throws IOException")
        void testTeamsAboveMaximum() {
            String[] args = {"teams:15"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("team"));
            assertTrue(exception.getMessage().contains("2 and 14"));
        }

        @Test
        @DisplayName("Non-numeric teams throws IOException")
        void testNonNumericTeams() {
            String[] args = {"teams:abc"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("team"));
        }

        @Test
        @DisplayName("Invalid goal throws IOException")
        void testInvalidGoal() {
            String[] args = {"goal:invalid"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("goal"));
        }

        @Test
        @DisplayName("Non-numeric goalvalue throws IOException")
        void testNonNumericGoalvalue() {
            String[] args = {"goalvalue:notanumber"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("goalvalue"));
        }

        @Test
        @DisplayName("Negative goalvalue throws IOException")
        void testNegativeGoalvalue() {
            String[] args = {"goalvalue:-100"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("goalvalue"));
            assertTrue(exception.getMessage().contains("negative"));
        }

        @Test
        @DisplayName("Non-numeric countdown throws IOException")
        void testNonNumericCountdown() {
            String[] args = {"countdown:notanumber"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("countdown"));
        }

        @Test
        @DisplayName("Non-numeric distribution throws IOException")
        void testNonNumericDistribution() {
            String[] args = {"distribution:notanumber"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("distribution"));
        }

        @Test
        @DisplayName("Zero distribution throws IOException")
        void testZeroDistribution() {
            String[] args = {"distribution:0"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("distribution"));
        }

        @Test
        @DisplayName("Distribution below minimum throws IOException")
        void testDistributionBelowMinimum() {
            String[] args = {"distribution:0.005"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("distribution"));
        }

        @Test
        @DisplayName("Distribution above maximum throws IOException")
        void testDistributionAboveMaximum() {
            String[] args = {"distribution:1.5"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("distribution"));
        }

        @Test
        @DisplayName("Unknown parameter throws IOException")
        void testUnknownParameter() {
            String[] args = {"unknownparam:value"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("does not exist") ||
                      exception.getMessage().contains("unknownparam"));
        }

        @Test
        @DisplayName("Missing colon throws IOException")
        void testMissingColon() {
            String[] args = {"gamemodevalue"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("pairs") ||
                      exception.getMessage().contains("colon"));
        }

        @Test
        @DisplayName("Invalid scoretype throws IOException")
        void testInvalidScoretype() {
            String[] args = {"scoretypes:area-invalid-beacons"};
            IOException exception = assertThrows(IOException.class, () -> new Params(args));
            assertTrue(exception.getMessage().contains("scoretypes"));
        }
    }

    @Nested
    @DisplayName("Fixed Implementation Tests")
    class FixedImplementationTests {

        @Test
        @DisplayName("goalvalue parameter is now set correctly")
        void testGoalvalueSet() throws IOException {
            String[] args = {"goalvalue:100"};
            Params params = new Params(args);
            assertEquals(100, params.getGoalvalue());
        }

        @Test
        @DisplayName("countdown parameter is now set correctly")
        void testCountdownSet() throws IOException {
            String[] args = {"countdown:300"};
            Params params = new Params(args);
            assertEquals(300, params.getCountdown());
        }

        @Test
        @DisplayName("beacons goal is now supported")
        void testBeaconsGoal() throws IOException {
            String[] args = {"goal:beacons"};
            Params params = new Params(args);
            assertEquals(GameScoreGoal.BEACONS, params.getGoal());
        }

        @Test
        @DisplayName("scoretypes parameter is now set correctly")
        void testScoretypesSet() throws IOException {
            String[] args = {"scoretypes:area-beacons-triangles"};
            Params params = new Params(args);

            List<GameScoreGoal> expected = List.of(
                GameScoreGoal.AREA,
                GameScoreGoal.BEACONS,
                GameScoreGoal.TRIANGLES
            );
            assertEquals(expected, params.getScoretypes());
        }

        @Test
        @DisplayName("scoretypes with single value")
        void testScoretypesSingle() throws IOException {
            String[] args = {"scoretypes:links"};
            Params params = new Params(args);
            assertEquals(List.of(GameScoreGoal.LINKS), params.getScoretypes());
        }

        @Test
        @DisplayName("scoretypes with all values")
        void testScoretypesAll() throws IOException {
            String[] args = {"scoretypes:area-beacons-triangles-links"};
            Params params = new Params(args);

            List<GameScoreGoal> expected = List.of(
                GameScoreGoal.AREA,
                GameScoreGoal.BEACONS,
                GameScoreGoal.TRIANGLES,
                GameScoreGoal.LINKS
            );
            assertEquals(expected, params.getScoretypes());
        }

        @Test
        @DisplayName("Zero goalvalue is valid")
        void testZeroGoalvalue() throws IOException {
            String[] args = {"goalvalue:0"};
            Params params = new Params(args);
            assertEquals(0, params.getGoalvalue());
        }

        @Test
        @DisplayName("Zero countdown is valid (unlimited)")
        void testZeroCountdown() throws IOException {
            String[] args = {"countdown:0"};
            Params params = new Params(args);
            assertEquals(0, params.getCountdown());
        }
    }
}
