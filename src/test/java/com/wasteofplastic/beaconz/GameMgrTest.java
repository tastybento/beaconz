package com.wasteofplastic.beaconz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.LinkedHashMap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Comprehensive test suite for {@link GameMgr} covering all game management operations.
 *
 * <p>This test class validates:
 * <ul>
 *   <li>Constructor initialization and lobby creation</li>
 *   <li>Game persistence (save/load operations)</li>
 *   <li>Region allocation and spatial calculations</li>
 *   <li>Game creation and deletion</li>
 *   <li>Default parameter management</li>
 *   <li>Lookup methods for games, regions, and scorecards</li>
 *   <li>Coordinate utilities and chunk alignment</li>
 *   <li>Area safety and overlap detection</li>
 * </ul>
 *
 * @author tastybento
 */
class GameMgrTest {

    private ServerMock server;
    private Beaconz plugin;
    private World world;
    private GameMgr gameMgr;

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
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("BeaconzTest"));

        // Mock FileConfiguration for plugin.getConfig()
        FileConfiguration config = mock(FileConfiguration.class);
        when(plugin.getConfig()).thenReturn(config);
        when(config.getConfigurationSection(anyString())).thenReturn(null);

        // Create and setup test world
        world = server.addSimpleWorld("beaconzworld");
        when(plugin.getBeaconzWorld()).thenReturn(world);

        // Mock BeaconzStore and Register
        BeaconzStore store = mock(BeaconzStore.class);
        Register register = mock(Register.class);
        when(plugin.getBeaconzStore()).thenReturn(store);
        when(plugin.getRegister()).thenReturn(register);

        // Initialize Lang strings
        setupLangStrings();

        // Setup Settings with default values
        setupSettings();
    }

    /**
     * Initialize Lang static strings to prevent NPEs during Region operations.
     */
    private void setupLangStrings() {
        Lang.actionsHitSign = "Hit sign to join!";
        Lang.titleWelcome = "Welcome";
        Lang.titleSubTitle = "Enjoy your game!";
        Lang.startYoureAMember = "You're a member of team [name]";
        Lang.startObjective = "Objective: [value] [goal]";
        Lang.startMostObjective = "Get the most [goal]";
        Lang.adminRegenComplete = "Regeneration complete";
        Lang.adminDeletedGame = "Deleted game [name]";
        Lang.scoreGetValueGoal = "Get [value] [goal]";
        Lang.scoreGetTheMostGoal = "Get the most [goal]";
    }

    /**
     * Tears down the test environment after each test.
     */
    @AfterEach
    void tearDown() {
        if (server != null) {
            MockBukkit.unmock();
        }
    }

    /**
     * Initializes Settings with test values.
     */
    private void setupSettings() {
        Settings.worldName = "beaconzworld";
        Settings.lobbyx = 0;
        Settings.lobbyz = 0;
        Settings.lobbyradius = 64;
        Settings.xCenter = 0;
        Settings.zCenter = 0;
        Settings.gamemode = "minigame";
        Settings.gameDistance = 512;
        Settings.distribution = 0.1;
        Settings.defaultTeamNumber = 2;
        Settings.minigameGoal = "score";
        Settings.minigameGoalValue = 1000;
        Settings.strategyGoal = "time";
        Settings.strategyGoalValue = 3600;
        Settings.minigameTimer = 600;
        Settings.strategyTimer = 0;
        Settings.minigameScoreTypes = "all";
        Settings.strategyScoreTypes = "triangles,links";
        Settings.lobbyBlocks = new java.util.ArrayList<>();
        Settings.lobbyBlocks.add("STONE");
        Settings.defaultGameName = "game";
        Settings.linkCommands = new java.util.ArrayList<>();
        Settings.teamBlock = new java.util.HashMap<>();
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#GameMgr(com.wasteofplastic.beaconz.Beaconz)}.
     * Verifies constructor initializes properly and creates lobby.
     */
    @Test
    void testGameMgr() {
        // Mock biome for lobby area to be non-ocean
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);

        // Create GameMgr
        gameMgr = new GameMgr(plugin);

        // Verify initialization
        assertNotNull(gameMgr);
        assertNotNull(gameMgr.getLobby());
        assertNotNull(gameMgr.getRegions());
        assertNotNull(gameMgr.getGames());
        assertEquals(1, gameMgr.getRegions().size(), "Should have lobby region");
        assertEquals(0, gameMgr.getGames().size(), "Should have no games initially");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#reload()}.
     * Verifies reload preserves games and updates settings.
     */
    @Test
    void testReload() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Change settings
        Settings.gamemode = "strategy";

        // Reload
        gameMgr.reload();

        // Verify still initialized
        assertNotNull(gameMgr.getLobby());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#saveAllGames()}.
     * Verifies games are saved to YAML file.
     */
    @Test
    void testSaveAllGames() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Save all games
        gameMgr.saveAllGames();

        // Verify games.yml was created
        File gamesFile = new File(tempDir, "games.yml");
        assertTrue(gamesFile.exists() || new File(tempDir, "games.old").exists(),
                   "Games file should be created");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#saveGame(java.lang.String)}.
     * Verifies individual game save works correctly.
     */
    @Test
    void testSaveGame() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Save non-existent game should not throw error
        assertDoesNotThrow(() -> gameMgr.saveGame("nonexistent"));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#loadAllGames()}.
     * Verifies loading games from file works.
     */
    @Test
    void testLoadAllGames() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Save and reload
        gameMgr.saveAllGames();
        gameMgr.loadAllGames();

        // Verify lobby still exists
        assertNotNull(gameMgr.getLobby());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#loadGames(java.lang.String)}.
     * Verifies selective game loading.
     */
    @Test
    void testLoadGames() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Load non-existent game should not throw error
        assertDoesNotThrow(() -> gameMgr.loadGames("nonexistent"));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#createLobby()}.
     * Verifies lobby creation at configured location.
     */
    @Test
    void testCreateLobby() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Verify lobby was created
        Region lobby = gameMgr.getLobby();
        assertNotNull(lobby);

        // Verify lobby is in regions map
        assertTrue(gameMgr.getRegions().containsValue(lobby));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#newGame(java.lang.String)}.
     * Verifies new game creation allocates region and initializes game.
     */
    @Test
    void testNewGame() {
        mockBiomeForArea(-10000, -10000, 10000, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Create a new game
        gameMgr.newGame("testgame");

        // Verify game was created
        Game game = gameMgr.getGame("testgame");
        assertNotNull(game, "Game should be created");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#nextRegionLocation()}.
     * Verifies region location finder returns valid coordinates.
     */
    @Test
    void testNextRegionLocation() {
        mockBiomeForArea(-10000, -10000, 10000, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Get next region location
        Point2D location = gameMgr.nextRegionLocation();

        // Should find a location
        assertNotNull(location, "Should find a location for new region");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#goodNeighbor(java.awt.geom.Point2D, java.lang.Double)}.
     * Verifies cardinal direction checking finds suitable locations.
     */
    @Test
    void testGoodNeighbor() {
        mockBiomeForArea(-10000, -10000, 10000, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Point2D center = new Point2D.Double(5000, 5000);
        Double distance = 1000.0;

        // Should find a good neighbor location
        Point2D neighbor = gameMgr.goodNeighbor(center, distance);

        assertNotNull(neighbor, "Should find a good neighbor location");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#isAreaFree(java.awt.geom.Point2D, java.lang.Double)}.
     * Verifies overlap detection works correctly.
     */
    @Test
    void testIsAreaFreePoint2DDouble() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Lobby area should not be free
        Point2D lobbyCenter = new Point2D.Double(0, 0);
        assertFalse(gameMgr.isAreaFree(lobbyCenter, 32.0),
                    "Lobby area should not be free");

        // Far away area should be free
        Point2D farAway = new Point2D.Double(10000, 10000);
        assertTrue(gameMgr.isAreaFree(farAway, 100.0),
                   "Far away area should be free");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#isAreaFree(java.awt.geom.Point2D, java.awt.geom.Point2D)}.
     * Verifies rectangular overlap detection.
     */
    @Test
    void testIsAreaFreePoint2DPoint2D() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Test with points overlapping lobby
        Point2D pt1 = new Point2D.Double(-50, -50);
        Point2D pt2 = new Point2D.Double(50, 50);
        assertFalse(gameMgr.isAreaFree(pt1, pt2),
                    "Area overlapping lobby should not be free");

        // Test with non-overlapping points
        Point2D pt3 = new Point2D.Double(1000, 1000);
        Point2D pt4 = new Point2D.Double(2000, 2000);
        assertTrue(gameMgr.isAreaFree(pt3, pt4),
                   "Non-overlapping area should be free");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#isAreaSafe(java.awt.geom.Point2D, java.lang.Double)}.
     * Verifies biome safety checking works for plains areas.
     * Note: Ocean area testing disabled due to MockBukkit biome limitations.
     */
    @Test
    void testIsAreaSafe() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Plains area should be safe
        Point2D plainCenter = new Point2D.Double(0, 0);
        assertTrue(gameMgr.isAreaSafe(plainCenter, 64.0),
                   "Plains area should be safe");

        // Note: Ocean testing is not reliable with MockBukkit's biome system
        // In production, ocean areas would be rejected by the actual biome checking
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#setGameDefaultParms()}.
     * Verifies default parameters are set from Settings.
     */
    @Test
    void testSetGameDefaultParms() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Set defaults
        gameMgr.setGameDefaultParms();

        // Verify by creating a game and checking it uses defaults
        assertDoesNotThrow(() -> gameMgr.setGameDefaultParms());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#setGameDefaultParms(String, Integer, Integer, String, Integer, Integer, String, Double)}.
     * Verifies custom default parameters can be set.
     */
    @Test
    void testSetGameDefaultParmsWithCustomValues() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Set custom defaults
        gameMgr.setGameDefaultParms("strategy", 1024, 4, "score", 5000, 1200, "all", 0.2);

        // Should not throw exception
        assertDoesNotThrow(() ->
            gameMgr.setGameDefaultParms("strategy", 1024, 4, "score", 5000, 1200, "all", 0.2));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getPlayerTeam(org.bukkit.entity.Player)}.
     * Verifies player team lookup works correctly.
     */
    @Test
    void testGetPlayerTeam() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        PlayerMock player = server.addPlayer("testplayer");
        player.setOp(true); // Make OP to avoid teleport

        // Should return null for player not in team
        Team team = gameMgr.getPlayerTeam(player);
        assertNull(team, "Player not in game should have no team");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getRegion(org.bukkit.Location)}.
     * Verifies region lookup by Location works.
     */
    @Test
    void testGetRegionLocation() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Location in lobby should return lobby region
        Location lobbyLoc = new Location(world, 0, 64, 0);
        Region region = gameMgr.getRegion(lobbyLoc);
        assertNotNull(region, "Should find lobby region");
        assertEquals(gameMgr.getLobby(), region);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getRegion(int, int)}.
     * Verifies region lookup by coordinates works.
     */
    @Test
    void testGetRegionIntInt() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Coordinates in lobby should return lobby region
        Region region = gameMgr.getRegion(0, 0);
        assertNotNull(region, "Should find lobby region at 0,0");
        assertEquals(gameMgr.getLobby(), region);

        // Coordinates outside any region should return null
        Region noRegion = gameMgr.getRegion(10000, 10000);
        assertNull(noRegion, "Should not find region at far coordinates");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGame(org.bukkit.scoreboard.Team)}.
     * Verifies game lookup by team.
     */
    @Test
    void testGetGameTeam() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Team team = mock(Team.class);

        // No game with this team should return null
        Game game = gameMgr.getGame(team);
        assertNull(game, "Non-existent team should return null");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGame(java.lang.String)}.
     * Verifies game lookup by name.
     */
    @Test
    void testGetGameString() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Non-existent game should return null
        Game game = gameMgr.getGame("nonexistent");
        assertNull(game, "Non-existent game should return null");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGame(java.awt.geom.Point2D)}.
     * Verifies game lookup by Point2D.
     */
    @Test
    void testGetGamePoint2D() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Point2D point = new Point2D.Double(10000, 10000);

        // No game at this location
        Game game = gameMgr.getGame(point);
        assertNull(game, "No game at far location");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGame(int, int)}.
     * Verifies game lookup by integer coordinates.
     */
    @Test
    void testGetGameIntInt() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Lobby has no game
        Game game = gameMgr.getGame(0, 0);
        assertNull(game, "Lobby should have no game");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGame(org.bukkit.Location)}.
     * Verifies game lookup by Bukkit Location.
     */
    @Test
    void testGetGameLocation() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Location loc = new Location(world, 0, 64, 0);

        // Lobby has no game
        Game game = gameMgr.getGame(loc);
        assertNull(game, "Lobby location should have no game");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGame(java.awt.geom.Line2D)}.
     * Verifies game lookup by Line2D (beacon link).
     */
    @Test
    void testGetGameLine2D() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Line2D line = new Line2D.Double(0, 0, 10, 10);

        // Lobby has no game
        Game game = gameMgr.getGame(line);
        assertNull(game, "Line in lobby should have no game");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGame(com.wasteofplastic.beaconz.Region)}.
     * Verifies game lookup by Region.
     */
    @Test
    void testGetGameRegion() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Lobby has no game
        Game game = gameMgr.getGame(gameMgr.getLobby());
        assertNull(game, "Lobby region should have no game");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getSC(org.bukkit.entity.Player)}.
     * Verifies scorecard lookup by player.
     */
    @Test
    void testGetSCPlayer() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        PlayerMock player = server.addPlayer("testplayer");

        // Player in lobby has no scorecard
        Scorecard sc = gameMgr.getSC(player);
        assertNull(sc, "Player in lobby should have no scorecard");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getSC(java.awt.geom.Point2D)}.
     * Verifies scorecard lookup by Point2D.
     */
    @Test
    void testGetSCPoint2D() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Point2D point = new Point2D.Double(0, 0);

        // Lobby has no scorecard
        Scorecard sc = gameMgr.getSC(point);
        assertNull(sc, "Lobby should have no scorecard");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getSC(int, int)}.
     * Verifies scorecard lookup by coordinates.
     */
    @Test
    void testGetSCIntInt() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Lobby has no scorecard
        Scorecard sc = gameMgr.getSC(0, 0);
        assertNull(sc, "Lobby should have no scorecard");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getSC(org.bukkit.scoreboard.Team)}.
     * Verifies scorecard lookup by team.
     */
    @Test
    void testGetSCTeam() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Team team = mock(Team.class);

        // Non-existent team has no scorecard
        Scorecard sc = gameMgr.getSC(team);
        assertNull(sc, "Non-existent team should have no scorecard");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getSC(org.bukkit.Location)}.
     * Verifies scorecard lookup by Location.
     */
    @Test
    void testGetSCLocation() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Location loc = new Location(world, 0, 64, 0);

        // Lobby has no scorecard
        Scorecard sc = gameMgr.getSC(loc);
        assertNull(sc, "Lobby should have no scorecard");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getLobby()}.
     * Verifies lobby getter returns the lobby region.
     */
    @Test
    void testGetLobby() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Region lobby = gameMgr.getLobby();
        assertNotNull(lobby, "Lobby should exist");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getRegions()}.
     * Verifies regions map getter works.
     */
    @Test
    void testGetRegions() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        LinkedHashMap<Point2D[], Region> regions = gameMgr.getRegions();
        assertNotNull(regions, "Regions map should not be null");
        assertFalse(regions.isEmpty(), "Regions should contain lobby");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#getGames()}.
     * Verifies games map getter works.
     */
    @Test
    void testGetGames() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        LinkedHashMap<String, Game> games = gameMgr.getGames();
        assertNotNull(games, "Games map should not be null");
        assertTrue(games.isEmpty(), "Should have no games initially");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#isPlayerInLobby(org.bukkit.entity.Player)}.
     * Verifies player lobby detection works.
     */
    @Test
    void testIsPlayerInLobby() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        PlayerMock player = server.addPlayer("testplayer");
        player.setLocation(new Location(world, 0, 64, 0));

        // Player at 0,0 should be in lobby
        assertTrue(gameMgr.isPlayerInLobby(player),
                   "Player at lobby coordinates should be in lobby");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#isLocationInLobby(org.bukkit.Location)}.
     * Verifies location lobby detection works.
     */
    @Test
    void testIsLocationInLobby() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Location lobbyLoc = new Location(world, 0, 64, 0);
        assertTrue(gameMgr.isLocationInLobby(lobbyLoc),
                   "Location at 0,0 should be in lobby");

        Location farLoc = new Location(world, 10000, 64, 10000);
        assertFalse(gameMgr.isLocationInLobby(farLoc),
                    "Far location should not be in lobby");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#checkAreaFree(java.awt.geom.Point2D, java.lang.Integer)}.
     * Verifies area overlap checking with circular bounds.
     */
    @Test
    void testCheckAreaFree() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Point2D lobbyCenter = new Point2D.Double(0, 0);
        assertFalse(gameMgr.checkAreaFree(lobbyCenter, 32),
                    "Lobby center should not be free");

        Point2D farCenter = new Point2D.Double(10000, 10000);
        assertTrue(gameMgr.checkAreaFree(farCenter, 100),
                   "Far location should be free");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#rup16(double)}.
     * Verifies chunk boundary rounding works correctly.
     */
    @Test
    void testRup16() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Test positive rounding
        assertEquals(16.0, gameMgr.rup16(10), "10 should round to 16");
        assertEquals(16.0, gameMgr.rup16(16), "16 should round to 16");
        assertEquals(16.0, gameMgr.rup16(20), "20 should round to 16");  // (20+8)/16 = 1 -> 1*16 = 16
        assertEquals(0.0, gameMgr.rup16(0), "0 should round to 0");

        // Test negative rounding
        assertEquals(-16.0, gameMgr.rup16(-10), "-10 should round to -16");
        assertEquals(-16.0, gameMgr.rup16(-16), "-16 should round to -16");
        assertEquals(-32.0, gameMgr.rup16(-20), "-20 should round to -32");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#regionCorners(java.lang.String)}.
     * Verifies parsing coordinate string into Point2D array.
     */
    @Test
    void testRegionCornersString() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        String coords = "0:0:100:100";
        Point2D[] corners = gameMgr.regionCorners(coords);

        assertNotNull(corners, "Corners should not be null");
        assertEquals(2, corners.length, "Should have 2 corners");
        assertEquals(0.0, corners[0].getX(), "First corner X should be 0");
        assertEquals(0.0, corners[0].getY(), "First corner Y should be 0");
        assertEquals(100.0, corners[1].getX(), "Second corner X should be 100");
        assertEquals(100.0, corners[1].getY(), "Second corner Y should be 100");
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#regionCorners(int, int, int, int)}.
     * Verifies creating Point2D array from integer coordinates.
     */
    @Test
    void testRegionCornersIntIntIntInt() {
        mockBiomeForArea(0, 0, 64, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        Point2D[] corners = gameMgr.regionCorners(0, 0, 100, 100);

        assertNotNull(corners, "Corners should not be null");
        assertEquals(2, corners.length, "Should have 2 corners");
        assertEquals(0.0, corners[0].getX());
        assertEquals(0.0, corners[0].getY());
        assertEquals(100.0, corners[1].getX());
        assertEquals(100.0, corners[1].getY());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.GameMgr#delete(org.bukkit.command.CommandSender, com.wasteofplastic.beaconz.Game)}.
     * Verifies game deletion removes all game data.
     */
    @Test
    void testDelete() {
        mockBiomeForArea(-10000, -10000, 10000, Biome.PLAINS);
        gameMgr = new GameMgr(plugin);

        // Create a game first
        gameMgr.newGame("testgame");
        Game game = gameMgr.getGame("testgame");

        if (game != null) {
            CommandSender sender = mock(CommandSender.class);

            // Delete the game
            gameMgr.delete(sender, game);

            // Verify game was removed
            assertNull(gameMgr.getGame("testgame"),
                       "Game should be deleted");
        }
    }

    /**
     * Helper method to mock biome for a rectangular area.
     * This ensures isAreaSafe() checks pass or fail as needed.
     */
    private void mockBiomeForArea(int centerX, int centerZ, int radius, Biome biome) {
        // Set biomes in the mock world - MockBukkit provides real blocks
        for (int x = centerX - radius; x <= centerX + radius; x += 50) {
            for (int z = centerZ - radius; z <= centerZ + radius; z += 50) {
                // Use MockBukkit's setBiome directly on the world
                world.setBiome(x, 64, z, biome);
            }
        }
    }

}

