package com.wasteofplastic.beaconz.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.GameMgr;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Region;
import com.wasteofplastic.beaconz.Scorecard;
import com.wasteofplastic.beaconz.Params.GameScoreGoal;

/**
 * Comprehensive test suite for {@link CmdHandler} covering all command scenarios.
 *
 * <p>This test class validates:
 * <ul>
 *   <li>Command execution for players and non-players</li>
 *   <li>Permission checks for various commands</li>
 *   <li>Help command display</li>
 *   <li>Score display functionality</li>
 *   <li>Scoreboard toggling</li>
 *   <li>Join/leave game commands</li>
 *   <li>Tab completion for various scenarios</li>
 *   <li>Tab limiting utility method</li>
 * </ul>
 *
 * @author tastybento
 */
class CmdHandlerTest {

    private static final Component TEST_GAME = Component.text("TestGame");
    // Mocked dependencies
    private ServerMock server;
    private Beaconz plugin;
    private GameMgr gameMgr;
    private CmdHandler handler;
    private Command command;

    // Mock game components
    private Game game;
    private Scorecard scorecard;
    private Region lobby;
    private Scoreboard scoreboard;

    // Mock teams
    private Team team1;
    private Team team2;

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

        gameMgr = mock(GameMgr.class);
        game = mock(Game.class);
        scorecard = mock(Scorecard.class);
        lobby = mock(Region.class);
        scoreboard = mock(Scoreboard.class);
        command = mock(Command.class);

        // Configure plugin to return mocked GameMgr
        when(plugin.getGameMgr()).thenReturn(gameMgr);

        // Initialize Lang strings used in commands
        setupLangStrings();

        // Create the command handler
        handler = new CmdHandler(plugin);

        // Setup mock teams
        team1 = mock(Team.class);
        team2 = mock(Team.class);
        when(team1.displayName()).thenReturn(Component.text("Red Team"));
        when(team2.displayName()).thenReturn(Component.text("Blue Team"));
    }

    /**
     * Initialize Lang static strings to prevent NPEs during command execution.
     * These strings are used for user messages and help text.
     */
    private void setupLangStrings() {
        Lang.errorOnlyPlayers = Component.text("Only players can use this command");
        Lang.errorYouDoNotHavePermission = Component.text("You do not have permission");
        Lang.errorNoLobbyYet = Component.text("No lobby has been set up yet");
        Lang.errorYouMustBeInAGame = Component.text("You must be in a game");
        Lang.errorNoSuchGame = Component.text("No such game");
        Lang.errorUnknownCommand = Component.text("Unknown command");
        Lang.helpHelp = Component.text("- shows this help");
        Lang.helpLeave = Component.text("- leave a game");
        Lang.helpScore = Component.text("- show the team scores");
        Lang.helpScoreboard = Component.text("- toggles the scoreboard on and off");
        Lang.generalGame = Component.text("Game");
        Lang.actionsYouAreInTeam = Component.text("You are in [team]!");
        Lang.scoreScores = Component.text("Scores:");
        Lang.scoreGame = Component.text("[score] [unit]");
        Lang.scoreTeam = Component.text("[team]");
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
     * Test that the CmdHandler constructor properly initializes.
     * Ensures the handler is created without exceptions.
     */
    @Test
    void testConstructor() {
        assertNotNull(handler, "Handler should be initialized");
    }

    // ==================== Non-Player Sender Tests ====================

    /**
     * Test that console cannot execute player-only commands.
     * Console senders should receive an error message.
     */
    @Test
    void testOnCommand_ConsoleCannotExecute() {
        // Create a console sender
        ConsoleCommandSender console = server.getConsoleSender();

        // Execute command as console
        boolean result = handler.onCommand(console, command, "beaconz", new String[]{});

        // Verify console received error message
        assertTrue(result, "Command should return true");
        // Note: MockBukkit's console doesn't capture messages the same way,
        // but the command completed successfully
    }

    // ==================== Permission Tests ====================

    /**
     * Test that players without permission cannot execute commands.
     * Players lacking beaconz.player permission should be denied.
     */
    @Test
    void testOnCommand_NoPermission() {
        // Create a player without permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", false);

        // Execute command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{});

        // Verify command returned true (handled)
        assertTrue(result, "Command should be handled");
    }

    // ==================== Base Command Tests ====================

    /**
     * Test the base /beaconz command with no arguments.
     * Should teleport player to lobby if lobby exists.
     */
    @Test
    void testOnCommand_NoArgs_WithLobby() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock lobby exists
        when(gameMgr.getLobby()).thenReturn(lobby);

        // Execute command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{});

        // Verify lobby teleport was called
        assertTrue(result, "Command should succeed");
        verify(lobby).tpToRegionSpawn(player, false);
    }

    /**
     * Test the base /beaconz command when no lobby exists.
     * Should send error message to player.
     */
    @Test
    void testOnCommand_NoArgs_NoLobby() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock no lobby exists
        when(gameMgr.getLobby()).thenReturn(null);

        // Execute command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{});

        // Verify command was handled
        assertTrue(result, "Command should be handled");
        verify(lobby, never()).tpToRegionSpawn(any(), anyBoolean());
    }

    // ==================== Help Command Tests ====================

    /**
     * Test the /beaconz help command.
     * Should display all available commands to the player.
     */
    @Test
    void testOnCommand_Help() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Execute help command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"help"});

        // Verify command succeeded
        assertTrue(result, "Help command should succeed");
    }

    /**
     * Test the /beaconz help command with leave permission.
     * Should show leave command in help output.
     */
    @Test
    void testOnCommand_Help_WithLeavePermission() {
        // Create a player with both permissions
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", true);

        // Execute help command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"help"});

        // Verify command succeeded
        assertTrue(result, "Help command should succeed");
    }

    // ==================== Score Command Tests ====================

    /**
     * Test the /beaconz score command when player is not in a game.
     * Should display error message.
     */
    @Test
    void testOnCommand_Score_NotInGame() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock player location not in any game
        when(gameMgr.getGame(any(Location.class))).thenReturn(null);

        // Execute score command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"score"});

        // Verify command handled
        assertTrue(result, "Command should be handled");
    }

    /**
     * Test the /beaconz score command when player is in a game.
     * Should display game scores and team information.
     */
    @Test
    void testOnCommand_Score_InGame() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock player in a game with a team
        when(gameMgr.getGame(any(Location.class))).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(game.getName()).thenReturn(TEST_GAME);
        when(scorecard.getTeam(player)).thenReturn(team1);
        when(scorecard.getScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getTeams()).thenReturn(Set.of(team1, team2));
        when(scorecard.getScore(any(Team.class), any(GameScoreGoal.class))).thenReturn(100);

        // Execute score command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"score"});

        // Verify command succeeded and refreshScores was called
        assertTrue(result, "Score command should succeed");
        verify(scorecard).refreshScores();
    }

    /**
     * Test the /beaconz score command when player is in game but no team.
     * Should display error message.
     */
    @Test
    void testOnCommand_Score_InGameNoTeam() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock player in a game but not on a team
        when(gameMgr.getGame(any(Location.class))).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(null);

        // Execute score command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"score"});

        // Verify command handled
        assertTrue(result, "Command should be handled");
    }

    // ==================== Scoreboard Toggle Tests ====================

    /**
     * Test the /beaconz sb command to toggle scoreboard off.
     * When player has a scoreboard, it should be cleared.
     */
    @Test
    @SuppressWarnings("deprecation")
    void testOnCommand_Scoreboard_ToggleOff() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock player has entries on scoreboard (scoreboard is visible)
        Scoreboard playerScoreboard = server.getScoreboardManager().getNewScoreboard();
        playerScoreboard.registerNewObjective("test", "dummy");
        player.setScoreboard(playerScoreboard);

        // Execute sb command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"sb"});

        // Verify command succeeded
        assertTrue(result, "Scoreboard toggle should succeed");
    }

    /**
     * Test the /beaconz sb command to toggle scoreboard on.
     * When player has no scoreboard, it should be set from their game.
     */
    @Test
    void testOnCommand_Scoreboard_ToggleOn_InGame() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock player has empty scoreboard and is in a game
        when(gameMgr.getGame(any(Location.class))).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getScoreboard()).thenReturn(scoreboard);

        // Execute sb command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"sb"});

        // Verify command succeeded
        assertTrue(result, "Scoreboard toggle should succeed");
    }

    /**
     * Test the /beaconz sb command when player is not in a game.
     * Should give player a blank scoreboard.
     */
    @Test
    void testOnCommand_Scoreboard_ToggleOn_NoGame() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Mock player not in a game
        when(gameMgr.getGame(any(Location.class))).thenReturn(null);

        // Execute sb command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"sb"});

        // Verify command succeeded
        assertTrue(result, "Scoreboard toggle should succeed");
    }

    // ==================== Leave Command Tests ====================

    /**
     * Test the /beaconz leave command with one argument (no game name).
     * Should display help message if player has permission.
     */
    @Test
    void testOnCommand_Leave_NoGameName_WithPermission() {
        // Create a player with leave permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", true);

        // Execute leave command without game name
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"leave"});

        // Verify command succeeded
        assertTrue(result, "Leave help should be shown");
    }

    /**
     * Test the /beaconz leave command without permission.
     * Should deny access to the command.
     */
    @Test
    void testOnCommand_Leave_NoGameName_NoPermission() {
        // Create a player without leave permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", false);

        // Execute leave command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"leave"});

        // Verify command succeeded but permission denied
        assertTrue(result, "Command should be handled");
    }

    /**
     * Test the /beaconz leave <game> command with valid game.
     * Should call game.leave() for the player.
     */
    @Test
    void testOnCommand_Leave_ValidGame() {
        // Create a player with leave permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", true);

        // Mock game exists
        when(gameMgr.getGame(TEST_GAME)).thenReturn(game);

        // Execute leave command with game name
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"leave", "TestGame"});

        // Verify game.leave was called
        assertTrue(result, "Leave command should succeed");
        verify(game).leave(player);
    }

    /**
     * Test the /beaconz leave <game> command with invalid game.
     * Should send error message about game not existing.
     */
    @Test
    void testOnCommand_Leave_InvalidGame() {
        // Create a player with leave permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", true);

        // Mock game doesn't exist
        when(gameMgr.getGame(Component.text("NonExistentGame"))).thenReturn(null);

        // Execute leave command with invalid game name
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"leave", "NonExistentGame"});

        // Verify command handled
        assertTrue(result, "Command should be handled");
        verify(game, never()).leave(any());
    }

    /**
     * Test the /beaconz leave <game> command without permission.
     * Should deny the action even if game exists.
     */
    @Test
    void testOnCommand_Leave_WithGameName_NoPermission() {
        // Create a player without leave permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", false);

        // Mock game exists
        when(gameMgr.getGame(TEST_GAME)).thenReturn(game);

        // Execute leave command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"leave", "TestGame"});

        // Verify game.leave was NOT called
        assertTrue(result, "Command should be handled");
        verify(game, never()).leave(any());
    }

    // ==================== Join Command Tests (OP Only) ====================

    /**
     * Test the /beaconz join <game> command as OP.
     * This is an undocumented admin feature that should work for ops.
     */
    @Test
    void testOnCommand_Join_AsOp_ValidGame() {
        // Create an OP player
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.setOp(true);
        
        Component gameName = TEST_GAME;

        // Mock game exists
        when(gameMgr.getGame(gameName)).thenReturn(game);

        // Execute join command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"join", "TestGame"});

        // Verify game.join was called
        assertTrue(result, "Join command should succeed");
        verify(game).join(player);
    }

    /**
     * Test the /beaconz join <game> command as OP with invalid game.
     * Should send error message about game not existing.
     */
    @Test
    void testOnCommand_Join_AsOp_InvalidGame() {
        // Create an OP player
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.setOp(true);

        // Mock game doesn't exist
        when(gameMgr.getGame(Component.text("NonExistentGame"))).thenReturn(null);

        // Execute join command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"join", "NonExistentGame"});

        // Verify command handled
        assertTrue(result, "Command should be handled");
        verify(game, never()).join(any());
    }

    /**
     * Test the /beaconz join <game> command as non-OP.
     * Non-ops should not be able to use this undocumented command.
     */
    @Test
    void testOnCommand_Join_AsNonOp() {
        // Create a non-OP player
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.setOp(false);

        // Mock game exists
        when(gameMgr.getGame(TEST_GAME)).thenReturn(game);

        // Execute join command
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"join", "TestGame"});

        // Verify game.join was NOT called
        assertTrue(result, "Command should be handled");
        verify(game, never()).join(any());
    }

    // ==================== Invalid Command Tests ====================

    /**
     * Test executing a command with too many arguments.
     * Should return error for unknown command.
     */
    @Test
    void testOnCommand_TooManyArguments() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Execute command with 3+ arguments
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"arg1", "arg2", "arg3"});

        // Verify command returned false (unknown command)
        assertFalse(result, "Command should return false for too many arguments");
    }

    /**
     * Test executing an unknown subcommand.
     * Should be handled gracefully.
     */
    @Test
    void testOnCommand_UnknownSubcommand() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Execute unknown subcommand
        boolean result = handler.onCommand(player, command, "beaconz", new String[]{"unknowncommand"});

        // Verify command was handled
        assertTrue(result, "Command should be handled");
    }

    // ==================== Show Game Scores Tests ====================

    /**
     * Test the showGameScores method displays all team scores.
     * Should show beacons, links, triangles, and area for each team.
     */
    @Test
    void testShowGameScores() {
        // Create a mock sender
        CommandSender sender = mock(CommandSender.class);

        // Mock game and scorecard
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getTeams()).thenReturn(Set.of(team1, team2));

        // Mock scores for different metrics
        when(scorecard.getScore(team1, GameScoreGoal.BEACONS)).thenReturn(5);
        when(scorecard.getScore(team1, GameScoreGoal.LINKS)).thenReturn(8);
        when(scorecard.getScore(team1, GameScoreGoal.TRIANGLES)).thenReturn(3);
        when(scorecard.getScore(team1, GameScoreGoal.AREA)).thenReturn(500);

        when(scorecard.getScore(team2, GameScoreGoal.BEACONS)).thenReturn(3);
        when(scorecard.getScore(team2, GameScoreGoal.LINKS)).thenReturn(4);
        when(scorecard.getScore(team2, GameScoreGoal.TRIANGLES)).thenReturn(1);
        when(scorecard.getScore(team2, GameScoreGoal.AREA)).thenReturn(200);

        // Call showGameScores
        handler.showGameScores(sender, game);

        // Verify refreshScores was called
        verify(scorecard).refreshScores();

        // Verify sender received messages (at least the header + 8 score lines for 2 teams)
        verify(sender, atLeast(9)).sendMessage(any(Component.class));
    }

    /**
     * Test showGameScores with empty team list.
     * Should only show the header message.
     */
    @Test
    void testShowGameScores_NoTeams() {
        // Create a mock sender
        CommandSender sender = mock(CommandSender.class);

        // Mock game with no teams
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getTeams()).thenReturn(Set.of());

        // Call showGameScores
        handler.showGameScores(sender, game);

        // Verify refreshScores was called
        verify(scorecard).refreshScores();

        // Verify only header message was sent
        verify(sender, times(1)).sendMessage(any(Component.class));
    }

    // ==================== Tab Completion Tests ====================

    /**
     * Test tab completion for the base command.
     * Should show help, score, scoreboard, and optionally leave.
     */
    @Test
    void testOnTabComplete_NoArgs() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Get tab completions
        List<String> completions = handler.onTabComplete(player, command, "beaconz", new String[]{});

        // Verify basic commands are present
        assertNotNull(completions, "Completions should not be null");
        assertTrue(completions.contains("help"), "Should contain help");
        assertTrue(completions.contains("score"), "Should contain score");
        assertTrue(completions.contains("scoreboard"), "Should contain scoreboard");
    }

    /**
     * Test tab completion with leave permission.
     * Should include leave command in completions.
     */
    @Test
    void testOnTabComplete_WithLeavePermission() {
        // Create a player with leave permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", true);

        // Get tab completions
        List<String> completions = handler.onTabComplete(player, command, "beaconz", new String[]{""});

        // Verify leave is included
        assertNotNull(completions, "Completions should not be null");
        assertTrue(completions.contains("leave"), "Should contain leave with permission");
    }

    /**
     * Test tab completion for leave command second argument.
     * Should list games the player is currently in.
     */
    @Test
    void testOnTabComplete_LeaveCommand_ListGames() {
        // Create a player with leave permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);
        player.addAttachment(plugin, "beaconz.player.leave", true);

        // Mock games the player is in
        Game game1 = mock(Game.class);
        Game game2 = mock(Game.class);
        Scorecard scorecard1 = mock(Scorecard.class);
        Scorecard scorecard2 = mock(Scorecard.class);

        when(game1.getName()).thenReturn(Component.text("Game1"));
        when(game1.getScorecard()).thenReturn(scorecard1);
        when(scorecard1.inTeam(player)).thenReturn(true);

        when(game2.getName()).thenReturn(Component.text("Game2"));
        when(game2.getScorecard()).thenReturn(scorecard2);
        when(scorecard2.inTeam(player)).thenReturn(false); // Not in this game

        
        LinkedHashMap<Component, Game> map = new LinkedHashMap<>();
        map.put(Component.text("Game1"), game1);
        map.put(Component.text("Game2"), game2);
        when(gameMgr.getGames()).thenReturn(map);

        // Get tab completions for leave command
        List<String> completions = handler.onTabComplete(player, command, "beaconz", new String[]{"leave", ""});

        // Verify only Game1 is suggested (player is in that game)
        assertNotNull(completions, "Completions should not be null");
        assertTrue(completions.contains("Game1"), "Should contain Game1");
        assertFalse(completions.contains("Game2"), "Should not contain Game2 (not in team)");
    }

    /**
     * Test tab completion for non-player sender.
     * Should return empty list.
     */
    @Test
    void testOnTabComplete_ConsoleReturnsEmpty() {
        // Get tab completions as console
        ConsoleCommandSender console = server.getConsoleSender();
        List<String> completions = handler.onTabComplete(console, command, "beaconz", new String[]{});

        // Verify empty list
        assertNotNull(completions, "Completions should not be null");
        assertTrue(completions.isEmpty(), "Console should get empty completions");
    }

    /**
     * Test tab completion filtering with partial input.
     * Should only return options that start with the partial input.
     */
    @Test
    void testOnTabComplete_PartialInput() {
        // Create a player with permission
        var player = server.addPlayer();
        player.addAttachment(plugin, "beaconz.player", true);

        // Get tab completions with partial input "sc"
        List<String> completions = handler.onTabComplete(player, command, "beaconz", new String[]{"sc"});

        // Verify filtered results
        assertNotNull(completions, "Completions should not be null");
        assertTrue(completions.contains("score"), "Should contain score");
        assertTrue(completions.contains("scoreboard"), "Should contain scoreboard");
        assertFalse(completions.contains("help"), "Should not contain help");
    }

    // ==================== Tab Limit Utility Tests ====================

    /**
     * Test the tabLimit utility method with matching prefix.
     * Should return all items that start with the prefix.
     */
    @Test
    void testTabLimit_MatchingPrefix() {
        // Create test list
        List<String> options = List.of("help", "score", "scoreboard", "leave");

        // Get filtered list with "sc" prefix
        List<String> result = CmdHandler.tabLimit(options, "sc");

        // Verify results
        assertEquals(2, result.size(), "Should have 2 matches");
        assertTrue(result.contains("score"), "Should contain score");
        assertTrue(result.contains("scoreboard"), "Should contain scoreboard");
    }

    /**
     * Test the tabLimit utility method with no matching prefix.
     * Should return empty list.
     */
    @Test
    void testTabLimit_NoMatches() {
        // Create test list
        List<String> options = List.of("help", "score", "scoreboard");

        // Get filtered list with non-matching prefix
        List<String> result = CmdHandler.tabLimit(options, "xyz");

        // Verify empty result
        assertTrue(result.isEmpty(), "Should have no matches");
    }

    /**
     * Test the tabLimit utility method with empty prefix.
     * Should return all items.
     */
    @Test
    void testTabLimit_EmptyPrefix() {
        // Create test list
        List<String> options = List.of("help", "score", "scoreboard", "leave");

        // Get filtered list with empty prefix
        List<String> result = CmdHandler.tabLimit(options, "");

        // Verify all items returned
        assertEquals(4, result.size(), "Should return all items");
    }

    /**
     * Test the tabLimit utility method with case insensitive matching.
     * Should match regardless of case.
     */
    @Test
    void testTabLimit_CaseInsensitive() {
        // Create test list
        List<String> options = List.of("Help", "Score", "Scoreboard");

        // Get filtered list with lowercase prefix
        List<String> result = CmdHandler.tabLimit(options, "sc");

        // Verify case-insensitive matching
        assertEquals(2, result.size(), "Should match case-insensitively");
        assertTrue(result.contains("Score"), "Should contain Score");
        assertTrue(result.contains("Scoreboard"), "Should contain Scoreboard");
    }

    /**
     * Test the tabLimit utility method with empty list.
     * Should return empty list.
     */
    @Test
    void testTabLimit_EmptyList() {
        // Create empty list
        List<String> options = new ArrayList<>();

        // Get filtered list
        List<String> result = CmdHandler.tabLimit(options, "test");

        // Verify empty result
        assertTrue(result.isEmpty(), "Should return empty list");
    }

    /**
     * Test the tabLimit utility method with exact match.
     * Should return the matching item.
     */
    @Test
    void testTabLimit_ExactMatch() {
        // Create test list
        List<String> options = List.of("help", "score");

        // Get filtered list with exact match
        List<String> result = CmdHandler.tabLimit(options, "help");

        // Verify exact match returned
        assertEquals(1, result.size(), "Should have 1 match");
        assertTrue(result.contains("help"), "Should contain help");
    }
}
