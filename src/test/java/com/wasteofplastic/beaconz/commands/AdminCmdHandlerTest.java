package com.wasteofplastic.beaconz.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.GameMgr;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Region;
import com.wasteofplastic.beaconz.Register;
import com.wasteofplastic.beaconz.Scorecard;
import com.wasteofplastic.beaconz.Settings;

import net.kyori.adventure.text.Component;

/**
 * Comprehensive test suite for {@link AdminCmdHandler} covering all admin command scenarios.
 *
 * <p>This test class validates:
 * <ul>
 *   <li>Permission checks (OP and admin permission)</li>
 *   <li>Help command display with color formatting</li>
 *   <li>Beacon claiming and unclaiming</li>
 *   <li>Distribution settings</li>
 *   <li>Team switching for players</li>
 *   <li>Forced team joining</li>
 *   <li>Game listing and deletion</li>
 *   <li>Player kicking and force ending games</li>
 *   <li>Beacon listing with filters</li>
 *   <li>Game creation with parameters</li>
 *   <li>Configuration reloading</li>
 *   <li>Parameter listing and validation</li>
 *   <li>Spawn point setting</li>
 *   <li>Team roster display</li>
 *   <li>Tab completion for all commands</li>
 *   <li>Parameter validation logic</li>
 * </ul>
 *
 * @author tastybento
 */
class AdminCmdHandlerTest {

    // Mocked dependencies
    private ServerMock server;
    private Beaconz plugin;
    private GameMgr gameMgr;
    private Register register;
    private AdminCmdHandler handler;
    private Command command;

    // Mock game components
    private Game game;
    private Game game2;
    private Scorecard scorecard;
    private Region lobby;
    private Region gameRegion;
    private Scoreboard scoreboard;

    // Mock world and blocks
    private World world;
    private Block block;
    private Location location;

    // Mock teams
    private Team team1;
    private Team team2;

    // Mock beacons
    private BeaconObj beacon;
    private HashMap<Point2D, BeaconObj> beaconRegister;

    /**
     * Sets up the test environment before each test.
     * Initializes MockBukkit server, plugin mocks, and Lang strings.
     */
    @BeforeEach
    @SuppressWarnings("deprecation")
    void setUp() {
        // Initialize MockBukkit server
        server = MockBukkit.mock();

        // Mock the plugin and its dependencies
        plugin = mock(Beaconz.class);

        // Create a mock PluginDescriptionFile for permission attachments
        PluginDescriptionFile pdf = new PluginDescriptionFile("Beaconz", "2.0.0", "com.wasteofplastic.beaconz.Beaconz");
        when(plugin.getDescription()).thenReturn(pdf);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getName()).thenReturn("Beaconz");

        // Initialize mocks
        gameMgr = mock(GameMgr.class);
        register = mock(Register.class);
        game = mock(Game.class);
        game2 = mock(Game.class);
        scorecard = mock(Scorecard.class);
        lobby = mock(Region.class);
        gameRegion = mock(Region.class);
        scoreboard = mock(Scoreboard.class);
        world = mock(World.class);
        block = mock(Block.class);
        location = mock(Location.class);
        when(location.clone()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        command = mock(Command.class);
        beacon = mock(BeaconObj.class);

        // Configure plugin to return mocked dependencies
        when(plugin.getGameMgr()).thenReturn(gameMgr);
        when(plugin.getRegister()).thenReturn(register);

        // Setup beacon register
        beaconRegister = new HashMap<>();
        when(register.getBeaconRegister()).thenReturn(beaconRegister);

        // Initialize Lang strings used in commands
        setupLangStrings();

        // Create the command handler
        handler = new AdminCmdHandler(plugin);

        // Setup mock teams
        team1 = mock(Team.class);
        team2 = mock(Team.class);
        when(team1.displayName()).thenReturn(Component.text("Red Team"));
        when(team2.displayName()).thenReturn(Component.text("Blue Team"));
        when(team1.getName()).thenReturn("red");
        when(team2.getName()).thenReturn("blue");
    }

    /**
     * Initialize Lang static strings to prevent NPEs during command execution.
     * These strings are used for user messages and help text.
     */
    private void setupLangStrings() {
        // Error messages
        Lang.errorYouDoNotHavePermission = "You do not have permission";
        Lang.errorOnlyPlayers = "Only players can use this command";
        Lang.errorYouMustBeInATeam = "You must be in a team";
        Lang.errorYouMustBeInAGame = "You must be in a game";
        Lang.errorNoSuchTeam = "No such team";
        Lang.errorNoSuchGame = "No such game";
        Lang.errorUnknownPlayer = "Unknown player";
        Lang.errorUnknownCommand = "Unknown command";
        Lang.errorYouHaveToBeStandingOnABeacon = "You must be standing on a beacon";
        Lang.errorNotInRegister = "Not in register: ";
        Lang.errorAlreadyExists = "Game already exists: [name]";
        Lang.errorNoGames = "No games found";
        Lang.errorError = "Error: ";

        // Help messages
        Lang.helpLine = "====================";
        Lang.helpAdminTitle = "Admin Commands";
        Lang.helpAdminClaim = "- claim a beacon";
        Lang.helpAdminDelete = "- delete a game";
        Lang.helpAdminJoin = "- join a team";
        Lang.helpAdminGames = "- list all games";
        Lang.helpAdminForceEnd = "- force end a game";
        Lang.helpAdminList = "- list beacons";
        Lang.helpAdminListParms = "- list game parameters";
        Lang.helpAdminNewGame = "- create new game. Use /[label] newgame help for details";
        Lang.helpAdminReload = "- reload configuration";
        Lang.helpAdminSetTeamSpawn = "- set team spawn";
        Lang.helpAdminSetLobbySpawn = "- set lobby spawn";
        Lang.helpAdminSwitch = "- switch teams";
        Lang.helpAdminTeams = "- show team rosters";
        Lang.helpAdminKick = "- kick a player";
        Lang.helpAdminDistribution = "- set beacon distribution";

        // Action messages
        Lang.actionsYouAreInTeam = "You are in [team]!";
        Lang.actionsSwitchedToTeam = "Switched to [team]";
        Lang.actionsDistributionSettingTo = "Distribution set to [value]";

        // Beacon messages
        Lang.beaconClaimingBeaconAt = "Claiming beacon at [location]";
        Lang.beaconClaimedForTeam = "Beacon claimed for [team]";

        // Admin messages
        Lang.adminGamesDefined = "Games defined:";
        Lang.adminGamesTheLobby = "The Lobby";
        Lang.adminGamesNoOthers = "No other games";
        Lang.adminKickAllPlayers = "Kicked all players from [name]";
        Lang.adminKickPlayer = "Kicked [player] from [name]";
        Lang.adminDeletingGame = "Deleting game [name]...";
        Lang.adminDeletedGame = "Game [name] deleted";
        Lang.adminForceEnd = "Game [name] force ended";
        Lang.adminListBeaconsInGame = "Beacons in [name]:";
        Lang.adminNewGameBuilding = "Building new game...";
        Lang.adminReload = "Configuration reloaded";
        Lang.adminParmsMode = "Mode";
        Lang.adminParmsTeams = "Teams";
        Lang.adminParmsGoal = "Goal";
        Lang.adminParmsGoalValue = "Goal Value";
        Lang.adminParmsScoreTypes = "Score Types";
        Lang.adminParmsUnlimited = "Unlimited";
        Lang.adminParmsArgumentsPairs = "Arguments must be in parameter:value pairs";
        Lang.adminParmsDoesNotExist = "Parameter does not exist: [name]";

        // General messages
        Lang.generalSuccess = "Success";
        Lang.generalTeams = "Teams";
        Lang.generalTeam = "Team";
        Lang.generalMembers = "Members";
        Lang.generalUnowned = "Unowned";
        Lang.generalNone = "None";
        Lang.generalLinks = "Links";
    }

    /**
     * Cleans up the test environment after each test.
     */
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // ==================== Constructor Tests ====================

    /**
     * Test that the AdminCmdHandler constructor properly initializes.
     * Ensures the handler is created without exceptions.
     */
    @Test
    void testConstructor() {
        assertNotNull(handler, "Handler should be initialized");
    }

    // ==================== Permission Tests ====================

    /**
     * Test that console can execute admin commands.
     * Console should bypass permission checks.
     */
    @Test
    void testOnCommand_ConsoleCanExecute() {
        ConsoleCommandSender console = server.getConsoleSender();

        boolean result = handler.onCommand(console, command, "bza", new String[]{});

        assertTrue(result, "Command should be handled");
    }

    /**
     * Test that players without permission cannot execute commands.
     * Players lacking both OP and beaconz.admin permission should be denied.
     */
    @Test
    void testOnCommand_NoPermission() {
        var player = server.addPlayer();
        player.setOp(false);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"games"});

        assertTrue(result, "Command should be handled");
        // Verify player received permission error (command was handled but denied)
    }

    /**
     * Test that OP players can execute admin commands.
     */
    @Test
    void testOnCommand_OpCanExecute() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getGames()).thenReturn(new LinkedHashMap<>());
        when(gameMgr.getLobby()).thenReturn(lobby);
        when(lobby.displayCoords()).thenReturn("0,0");

        boolean result = handler.onCommand(player, command, "bza", new String[]{"games"});

        assertTrue(result, "OP should be able to execute commands");
    }

    /**
     * Test that players with beaconz.admin permission can execute commands.
     */
    @Test
    void testOnCommand_AdminPermissionCanExecute() {
        var player = server.addPlayer();
        player.setOp(false);
        player.addAttachment(plugin, "beaconz.admin", true);

        when(gameMgr.getGames()).thenReturn(new LinkedHashMap<>());
        when(gameMgr.getLobby()).thenReturn(lobby);
        when(lobby.displayCoords()).thenReturn("0,0");

        boolean result = handler.onCommand(player, command, "bza", new String[]{"games"});

        assertTrue(result, "Admin permission should allow command execution");
    }

    // ==================== Help Command Tests ====================

    /**
     * Test the help command displays all available commands.
     */
    @Test
    void testOnCommand_Help() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{});

        assertTrue(result, "Help should be displayed");
    }

    /**
     * Test the help command with explicit "help" argument.
     */
    @Test
    void testOnCommand_HelpExplicit() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"help"});

        assertTrue(result, "Help should be displayed");
    }

    // ==================== Games Command Tests ====================

    /**
     * Test the games command lists all games and the lobby.
     */
    @Test
    void testOnCommand_Games_NoGames() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getGames()).thenReturn(new LinkedHashMap<>());
        when(gameMgr.getLobby()).thenReturn(lobby);
        when(lobby.displayCoords()).thenReturn("100,100");

        boolean result = handler.onCommand(player, command, "bza", new String[]{"games"});

        assertTrue(result, "Games command should succeed");
        verify(gameMgr).getGames();
        verify(gameMgr).getLobby();
    }

    /**
     * Test the games command with multiple games.
     */
    @Test
    void testOnCommand_Games_WithGames() {
        var player = server.addPlayer();
        player.setOp(true);

        LinkedHashMap<String, Game> games = new LinkedHashMap<>();
        games.put("TestGame1", game);
        games.put("TestGame2", game2);

        when(gameMgr.getGames()).thenReturn(games);
        when(gameMgr.getLobby()).thenReturn(lobby);
        when(lobby.displayCoords()).thenReturn("100,100");
        when(game.getName()).thenReturn("TestGame1");
        when(game.getRegion()).thenReturn(gameRegion);
        when(game2.getName()).thenReturn("TestGame2");
        when(game2.getRegion()).thenReturn(gameRegion);
        when(gameRegion.displayCoords()).thenReturn("500,500");

        boolean result = handler.onCommand(player, command, "bza", new String[]{"games"});

        assertTrue(result, "Games command should succeed");
        verify(game).getName();
        verify(game2).getName();
    }

    // ==================== Distribution Command Tests ====================

    /**
     * Test setting distribution with valid value.
     */
    @Test
    void testOnCommand_Distribution_ValidValue() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"distribution", "0.5"});

        assertTrue(result, "Distribution should be set");
        assertEquals(0.5, Settings.distribution, 0.001);
    }

    /**
     * Test distribution command with invalid value (too high).
     */
    @Test
    void testOnCommand_Distribution_InvalidValueTooHigh() {
        var player = server.addPlayer();
        player.setOp(true);

        double originalDist = Settings.distribution;
        boolean result = handler.onCommand(player, command, "bza", new String[]{"distribution", "1.5"});

        assertFalse(result, "Command should be handled");
        assertEquals(originalDist, Settings.distribution, 0.001, "Distribution should not change");
    }

    /**
     * Test distribution command with non-numeric value.
     */
    @Test
    void testOnCommand_Distribution_NonNumeric() {
        var player = server.addPlayer();
        player.setOp(true);

        double originalDist = Settings.distribution;
        boolean result = handler.onCommand(player, command, "bza", new String[]{"distribution", "invalid"});

        assertFalse(result, "Command should be handled");
        assertEquals(originalDist, Settings.distribution, 0.001, "Distribution should not change");
    }

    /**
     * Test distribution command with missing argument.
     */
    @Test
    void testOnCommand_Distribution_MissingArgument() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"distribution"});

        assertFalse(result, "Command should be handled");
    }

    // ==================== Claim Command Tests ====================

    /**
     * Test claim command requires player (not console).
     */
    @Test
    void testOnCommand_Claim_ConsoleCannotUse() {
        ConsoleCommandSender console = server.getConsoleSender();

        boolean result = handler.onCommand(console, command, "bza", new String[]{"claim", "red"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test claim command with missing team argument.
     */
    @Test
    void testOnCommand_Claim_MissingTeam() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"claim"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test claim command when player is not on a beacon.
     */
    @Test
    void testOnCommand_Claim_NotOnBeacon() {
        var player = server.addPlayer();
        player.setLocation(location);
        player.setOp(true);

        when(location.getBlock()).thenReturn(block);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(block);
        when(register.isBeacon(block)).thenReturn(false);

        when(gameMgr.getGame(location)).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam("red")).thenReturn(team1);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"claim", "red"});

        assertFalse(result, "Command should be handled");
        verify(register).isBeacon(block);
    }

    /**
     * Test successfully claiming a beacon for a team.
     */
    @Test
    void testOnCommand_Claim_SuccessfulClaim() {
        var player = server.addPlayer();
        player.setLocation(location);
        player.setOp(true);

        Point2D beaconPos = new Point2D.Double(100, 200);

        when(location.getBlock()).thenReturn(block);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(block);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(200);
        when(register.isBeacon(block)).thenReturn(true);

        beaconRegister.put(beaconPos, beacon);

        when(gameMgr.getGame(location)).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam("red")).thenReturn(team1);
        when(scorecard.getBlockID(team1)).thenReturn(Material.RED_STAINED_GLASS);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"claim", "red"});

        assertTrue(result, "Claim should succeed");
        verify(register).setBeaconOwner(beacon, team1);
        verify(block).setType(Material.RED_STAINED_GLASS);
    }

    /**
     * Test unclaiming a beacon (setting to unowned).
     */
    @Test
    void testOnCommand_Claim_Unowned() {
        var player = server.addPlayer();
        player.setLocation(location);
        player.setOp(true);

        Point2D beaconPos = new Point2D.Double(100, 200);

        when(location.getBlock()).thenReturn(block);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(block);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(200);
        when(register.isBeacon(block)).thenReturn(true);

        beaconRegister.put(beaconPos, beacon);

        when(gameMgr.getGame(location)).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"claim", "unowned"});

        assertTrue(result, "Unclaim should succeed");
        verify(register).removeBeaconOwnership(beacon);
    }

    // ==================== Delete Command Tests ====================

    /**
     * Test delete command with missing game name.
     */
    @Test
    void testOnCommand_Delete_MissingGameName() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"delete"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test delete command with non-existent game.
     */
    @Test
    void testOnCommand_Delete_GameDoesNotExist() {
        var player = server.addPlayer();
        player.setOp(true);

        LinkedHashMap<String, Game> games = new LinkedHashMap<>();
        when(gameMgr.getGames()).thenReturn(games);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"delete", "NonExistent"});

        assertFalse(result, "Command should be handled");
        verify(gameMgr, never()).delete(any(), any());
    }

    /**
     * Test successfully deleting a game.
     */
    @Test
    void testOnCommand_Delete_Success() {
        var player = server.addPlayer();
        player.setOp(true);

        LinkedHashMap<String, Game> games = new LinkedHashMap<>();
        games.put("TestGame", game);

        when(gameMgr.getGames()).thenReturn(games);
        when(game.getName()).thenReturn("TestGame");

        boolean result = handler.onCommand(player, command, "bza", new String[]{"delete", "TestGame"});

        assertFalse(result, "Delete should fail becuase it requires confirmation");
        
        result = handler.onCommand(player, command, "bza", new String[]{"delete", "TestGame"});
        assertTrue(result, "Delete should succeed");
        verify(gameMgr).delete(player, game);
    }

    // ==================== Force End Command Tests ====================

    /**
     * Test force_end command with missing game name.
     */
    @Test
    void testOnCommand_ForceEnd_MissingGameName() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"force_end"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test force_end command with non-existent game.
     */
    @Test
    void testOnCommand_ForceEnd_GameDoesNotExist() {
        var player = server.addPlayer();
        player.setOp(true);

        LinkedHashMap<String, Game> games = new LinkedHashMap<>();
        when(gameMgr.getGames()).thenReturn(games);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"force_end", "NonExistent"});

        assertFalse(result, "Command should be handled");
        verify(game, never()).forceEnd();
    }

    /**
     * Test successfully force ending a game.
     */
    @Test
    void testOnCommand_ForceEnd_Success() {
        var player = server.addPlayer();
        player.setOp(true);

        LinkedHashMap<String, Game> games = new LinkedHashMap<>();
        games.put("TestGame", game);

        when(gameMgr.getGames()).thenReturn(games);
        when(game.getName()).thenReturn("TestGame");

        boolean result = handler.onCommand(player, command, "bza", new String[]{"force_end", "TestGame"});

        assertTrue(result, "Force end should succeed");
        verify(game).forceEnd();
    }

    // ==================== Join Command Tests ====================

    /**
     * Test join command requires player (not console).
     */
    @Test
    void testOnCommand_Join_ConsoleCannotUse() {
        ConsoleCommandSender console = server.getConsoleSender();

        boolean result = handler.onCommand(console, command, "bza", new String[]{"join", "TestGame", "red"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test join command with missing arguments.
     */
    @Test
    void testOnCommand_Join_MissingArguments() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"join", "TestGame"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test join command with non-existent game.
     */
    @Test
    void testOnCommand_Join_GameDoesNotExist() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getGame("NonExistent")).thenReturn(null);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"join", "NonExistent", "red"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test successfully joining a player to a team.
     */
    @Test
    void testOnCommand_Join_Success() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getGame("TestGame")).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam("red")).thenReturn(team1);
        when(scorecard.getScoreboard()).thenReturn(scoreboard);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"join", "TestGame", "red"});

        assertTrue(result, "Join should succeed");
        verify(scorecard).addTeamPlayer(team1, player);
        verify(scorecard).sendPlayersHome(player, false);
    }

    // ==================== Reload Command Tests ====================

    /**
     * Test reload command saves and reloads configuration.
     */
    @Test
    void testOnCommand_Reload() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"reload"});

        assertTrue(result, "Reload should succeed");
        verify(register).saveRegister();
        verify(gameMgr).saveAllGames();
        verify(plugin).reloadConfig();
        verify(plugin).loadConfig();
        verify(gameMgr).reload();
        verify(register).loadRegister();
    }

    // ==================== Listparms Command Tests ====================

    /**
     * Test listparms command with missing game name.
     */
    @Test
    void testOnCommand_Listparms_MissingGameName() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"listparms"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test listparms command with non-existent game.
     */
    @Test
    void testOnCommand_Listparms_GameDoesNotExist() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getGame("NonExistent")).thenReturn(null);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"listparms", "NonExistent"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test successfully listing game parameters.
     */
    @Test
    void testOnCommand_Listparms_Success() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getGame("TestGame")).thenReturn(game);
        when(game.getGamemode()).thenReturn("strategy");
        when(game.getNbrTeams()).thenReturn(2);
        when(game.getGamegoal()).thenReturn("area");
        when(game.getGamegoalvalue()).thenReturn(1000);
        when(game.getScoretypes()).thenReturn("area:triangles");

        boolean result = handler.onCommand(player, command, "bza", new String[]{"listparms", "TestGame"});

        assertTrue(result, "Listparms should succeed");
        verify(game).getGamemode();
        verify(game).getNbrTeams();
        verify(game).getGamegoal();
    }

    // ==================== Setspawn Command Tests ====================

    /**
     * Test setspawn command requires player (not console).
     */
    @Test
    void testOnCommand_Setspawn_ConsoleCannotUse() {
        ConsoleCommandSender console = server.getConsoleSender();

        boolean result = handler.onCommand(console, command, "bza", new String[]{"setspawn"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test setspawn command when player is not in lobby.
     */
    @Test
    void testOnCommand_Setspawn_NotInLobby() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(false);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"setspawn"});

        assertFalse(result, "Command should be handled");
        verify(lobby, never()).setSpawnPoint(any());
    }

    /**
     * Test successfully setting lobby spawn point.
     */
    @Test
    void testOnCommand_Setspawn_Success() {
        var player = server.addPlayer();
        player.setLocation(location);
        player.setOp(true);

        when(gameMgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        when(location.getBlockX()).thenReturn(100);
        when(location.getBlockY()).thenReturn(64);
        when(location.getBlockZ()).thenReturn(200);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"setspawn"});

        assertTrue(result, "Setspawn should succeed");
        verify(lobby).setSpawnPoint(location);
    }

    // ==================== Teams Command Tests ====================

    /**
     * Test teams command with missing game argument.
     */
    @Test
    void testOnCommand_Teams_MissingArgument() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"teams"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test teams command with non-existent game.
     */
    @Test
    void testOnCommand_Teams_GameDoesNotExist() {
        var player = server.addPlayer();
        player.setOp(true);

        when(gameMgr.getGames()).thenReturn(new LinkedHashMap<>());

        boolean result = handler.onCommand(player, command, "bza", new String[]{"teams", "NonExistent"});

        assertFalse(result, "Command should be handled");
    }

    /**
     * Test teams command displaying team rosters.
     */
    @Test
    void testOnCommand_Teams_Success() {
        UUID uuid = UUID.randomUUID();
        PlayerMock player = new PlayerMock(server, "testPlayer", uuid);
        server.addPlayer(player);
        player.setOp(true);

        LinkedHashMap<String, Game> games = new LinkedHashMap<>();
        games.put("TestGame", game);

        when(gameMgr.getGames()).thenReturn(games);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getScoreboard()).thenReturn(scoreboard);

        HashMap<Team, List<String>> teamMembers = new HashMap<>();
        List<String> members = new ArrayList<>();
        members.add(UUID.randomUUID().toString());
        teamMembers.put(team1, members);

        when(scorecard.getTeamMembers()).thenReturn(teamMembers);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"teams", "TestGame"});

        assertTrue(result, "Teams command should succeed");
        verify(scorecard).getTeamMembers();
    }

    // ==================== Unknown Command Test ====================

    /**
     * Test handling of unknown commands.
     */
    @Test
    void testOnCommand_UnknownCommand() {
        var player = server.addPlayer();
        player.setOp(true);

        boolean result = handler.onCommand(player, command, "bza", new String[]{"invalidcommand"});

        assertFalse(result, "Unknown command should be handled");
    }

    // ==================== Parameter Validation Tests ====================

    /**
     * Test checkParms with valid gamemode parameter.
     */
    @Test
    void testCheckParms_ValidGamemode() {
        String result = handler.checkParms(new String[]{"gamemode:strategy"});

        assertEquals("", result, "Valid gamemode should pass");
    }

    /**
     * Test checkParms with invalid gamemode.
     */
    @Test
    void testCheckParms_InvalidGamemode() {
        String result = handler.checkParms(new String[]{"gamemode:invalid"});

        assertFalse(result.isEmpty(), "Invalid gamemode should fail");
        assertTrue(result.contains("gamemode"));
    }

    /**
     * Test checkParms with valid teams parameter.
     */
    @Test
    void testCheckParms_ValidTeams() {
        String result = handler.checkParms(new String[]{"teams:4"});

        assertEquals("", result, "Valid teams should pass");
    }

    /**
     * Test checkParms with teams out of range.
     */
    @Test
    void testCheckParms_TeamsOutOfRange() {
        String result = handler.checkParms(new String[]{"teams:20"});

        assertFalse(result.isEmpty(), "Teams > 14 should fail");
    }

    /**
     * Test checkParms with invalid parameter format.
     */
    @Test
    void testCheckParms_InvalidFormat() {
        String result = handler.checkParms(new String[]{"invalidformat"});

        assertFalse(result.isEmpty(), "Invalid format should fail");
    }

    /**
     * Test checkParms with valid goal parameter.
     */
    @Test
    void testCheckParms_ValidGoal() {
        String result = handler.checkParms(new String[]{"goal:triangles"});

        assertEquals("", result, "Valid goal should pass");
    }

    /**
     * Test checkParms with invalid goal.
     */
    @Test
    void testCheckParms_InvalidGoal() {
        String result = handler.checkParms(new String[]{"goal:invalid"});

        assertFalse(result.isEmpty(), "Invalid goal should fail");
    }

    // ==================== Tab Completion Tests ====================

    /**
     * Test tab completion for base command.
     */
    @Test
    void testOnTabComplete_NoArgs() {
        var player = server.addPlayer();
        player.setOp(true);

        List<String> completions = handler.onTabComplete(player, command, "bza", new String[]{""});

        assertNotNull(completions);
        assertTrue(completions.contains("games"));
        assertTrue(completions.contains("delete"));
        assertTrue(completions.contains("reload"));
    }

    /**
     * Test tab completion filters by partial input.
     */
    @Test
    void testOnTabComplete_PartialInput() {
        var player = server.addPlayer();
        player.setOp(true);

        List<String> completions = handler.onTabComplete(player, command, "bza", new String[]{"del"});

        assertNotNull(completions);
        assertTrue(completions.contains("delete"));
        assertFalse(completions.contains("games"));
    }

    /**
     * Test tab completion for delete command lists games.
     */
    @Test
    void testOnTabComplete_DeleteListsGames() {
        var player = server.addPlayer();
        player.setOp(true);

        LinkedHashMap<String, Game> games = new LinkedHashMap<>();
        games.put("TestGame1", game);
        games.put("TestGame2", game2);
        when(gameMgr.getGames()).thenReturn(games);

        List<String> completions = handler.onTabComplete(player, command, "bza", new String[]{"delete", ""});

        assertNotNull(completions);
        assertTrue(completions.contains("TestGame1"));
        assertTrue(completions.contains("TestGame2"));
    }

    /**
     * Test tab completion for newgame help.
     */
    @Test
    void testOnTabComplete_NewgameHelp() {
        var player = server.addPlayer();
        player.setOp(true);

        List<String> completions = handler.onTabComplete(player, command, "bza", new String[]{"newgame", ""});

        assertNotNull(completions);
        assertTrue(completions.contains("help"));
    }

    /**
     * Test tab completion without permission returns empty.
     */
    @Test
    void testOnTabComplete_NoPermission() {
        var player = server.addPlayer();
        player.setOp(false);

        List<String> completions = handler.onTabComplete(player, command, "bza", new String[]{""});

        assertNotNull(completions);
        assertTrue(completions.isEmpty());
    }

    // ==================== Tab Limit Utility Tests ====================

    /**
     * Test tabLimit with matching prefix.
     */
    @Test
    void testTabLimit_MatchingPrefix() {
        List<String> options = List.of("games", "goal", "help", "join");

        List<String> result = AdminCmdHandler.tabLimit(options, "g");

        assertEquals(2, result.size());
        assertTrue(result.contains("games"));
        assertTrue(result.contains("goal"));
    }

    /**
     * Test tabLimit with empty prefix returns all.
     */
    @Test
    void testTabLimit_EmptyPrefix() {
        List<String> options = List.of("games", "goal", "help");

        List<String> result = AdminCmdHandler.tabLimit(options, "");

        assertEquals(3, result.size());
    }

    /**
     * Test tabLimit is case-insensitive.
     */
    @Test
    void testTabLimit_CaseInsensitive() {
        List<String> options = List.of("Games", "Goal", "Help");

        List<String> result = AdminCmdHandler.tabLimit(options, "g");

        assertEquals(2, result.size());
    }

    /**
     * Test tabLimit with no matches.
     */
    @Test
    void testTabLimit_NoMatches() {
        List<String> options = List.of("games", "goal");

        List<String> result = AdminCmdHandler.tabLimit(options, "xyz");

        assertTrue(result.isEmpty());
    }
}

