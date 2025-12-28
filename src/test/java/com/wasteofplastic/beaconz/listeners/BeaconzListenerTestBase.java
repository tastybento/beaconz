package com.wasteofplastic.beaconz.listeners;

import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.GameMgr;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Messages;
import com.wasteofplastic.beaconz.Register;
import com.wasteofplastic.beaconz.Scorecard;

/**
 * Base class for listener tests providing common mocks and setup.
 * Subclasses can override setupMocks() to customize behavior for specific tests.
 *
 * <p>Uses lenient Mockito to allow shared stubs across test methods.
 * Lang static strings are initialized to prevent NPEs during message formatting.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BeaconzListenerTestBase {

    // Core plugin dependencies
    @Mock
    protected Beaconz plugin;
    @Mock
    protected GameMgr mgr;
    @Mock
    protected Register register;
    @Mock
    protected Messages messages;
    @Mock
    protected Logger logger;

    // Game & scoring
    @Mock
    protected Game game;
    @Mock
    protected Scorecard scorecard;

    // World & blocks
    @Mock
    protected World world;
    @Mock
    protected Block block;
    @Mock
    protected @NotNull Block beaconBlock;
    @Mock
    protected Location location;

    // Players & teams
    @Mock
    protected Player player;
    @Mock
    protected Team team;
    @Mock
    protected Team otherTeam;
    @Mock
    protected PlayerInventory inventory;

    // Items
    @Mock
    protected ItemStack item;

    // Beacons
    @Mock
    protected BeaconObj beacon;
    @Mock
    protected BeaconObj otherBeacon;

    // Server mock
    protected ServerMock server;
    private AutoCloseable closeable;

    /**
     * Sets up common mocks before each test.
     * Subclasses can override to add custom setup, but should call super.setUp().
     */
    @BeforeEach
    void setUp() throws Exception {
        // Processes the @Mock annotations and initializes the field
        closeable = MockitoAnnotations.openMocks(this);
        server = MockBukkit.mock();

        setupLangStrings();
        setupPluginMocks();
        setupWorldMocks();
        setupPlayerMocks();
        setupBeaconMocks();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        MockBukkit.unmock();
    }

    /**
     * Initialize Lang static strings to avoid NPEs during message replacement.
     * These are commonly referenced throughout listener code.
     */
    protected void setupLangStrings() {
        // Common error messages
        Lang.errorYouCannotDoThat = "errorYouCannotDoThat";
        Lang.errorClearAroundBeacon = "errorClearAroundBeacon";
        Lang.errorYouMustBeInAGame = "errorYouMustBeInAGame";
        Lang.errorYouMustBeInATeam = "errorYouMustBeInATeam";
        Lang.errorCanOnlyPlaceBlocks = "errorCanOnlyPlaceBlocks";
        Lang.errorCanOnlyPlaceBlocksUpTo = "errorCanOnlyPlaceBlocksUpTo [value]";
        Lang.errorYouNeedToBeLevel = "errorYouNeedToBeLevel [value]";
        Lang.errorTooFar = "errorTooFar [max]";
        Lang.errorNotEnoughExperience = "errorNotEnoughExperience";
        Lang.errorYouCannotRemoveOtherPlayersBlocks = "errorYouCannotRemoveOtherPlayersBlocks";

        // Beacon capture/destroy messages
        Lang.beaconYouCannotDestroyYourOwnBeacon = "beaconYouCannotDestroyYourOwnBeacon";
        Lang.beaconTeamDestroyed = "beaconTeamDestroyed [team1] [team2]";
        Lang.beaconPlayerDestroyed = "beaconPlayerDestroyed [player] [team]";
        Lang.beaconYouDestroyed = "beaconYouDestroyed [team]";
        Lang.beaconYouMustCapturedBeacon = "beaconYouMustCapturedBeacon";

        // Beacon linking messages
        Lang.beaconOriginNotOwned = "beaconOriginNotOwned [team]";
        Lang.beaconYouNeedThisMuchExp = "beaconYouNeedThisMuchExp [number]";
        Lang.beaconYouHaveThisMuchExp = "beaconYouHaveThisMuchExp [number]";
        Lang.beaconTheMapDisintegrates = "beaconTheMapDisintegrates";

        // Defense-related messages
        Lang.beaconYouCanOnlyExtend = "beaconYouCanOnlyExtend";
        Lang.beaconCannotBeExtended = "beaconCannotBeExtended";
        Lang.beaconExtended = "beaconExtended";
        Lang.beaconLockedJustNow = "beaconLockedJustNow [lockingBlock]";
        Lang.beaconLockedAlready = "beaconLockedAlready [lockingBlock]";
        Lang.beaconLockedWithNMoreBlocks = "beaconLockedWithNMoreBlocks [number]";
        Lang.beaconLinkBlockPlaced = "beaconLinkBlockPlaced [range]";
        Lang.beaconDefensePlaced = "beaconDefensePlaced";
        Lang.beaconLinkBlockBroken = "beaconLinkBlockBroken [range]";
        Lang.beaconLinkLost = "beaconLinkLost";
        Lang.beaconDefenseRemoveTopDown = "beaconDefenseRemoveTopDown";
        Lang.beaconLocked = "beaconLocked";
        Lang.beaconAmplifierBlocksCannotBeRecovered = "beaconAmplifierBlocksCannotBeRecovered";
        Lang.generalLevel = "Level";
    }

    /**
     * Setup plugin, managers, and core dependencies.
     */
    protected void setupPluginMocks() {
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getGameMgr()).thenReturn(mgr);
        when(plugin.getRegister()).thenReturn(register);
        when(plugin.getMessages()).thenReturn(messages);
        when(plugin.getBeaconzWorld()).thenReturn(world);
    }

    /**
     * Setup world, blocks, and locations.
     */
    protected void setupWorldMocks() {
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(beaconBlock);
        when(beaconBlock.getType()).thenReturn(Material.BEACON);

        when(mgr.getGame(location)).thenReturn(game);
        when(register.getBeacon(block)).thenReturn(beacon);
        when(register.isBeacon(beaconBlock)).thenReturn(true);
    }

    /**
     * Setup player, team, and inventory mocks.
     */
    protected void setupPlayerMocks() {
        when(player.getDisplayName()).thenReturn("playerA");
        when(player.getInventory()).thenReturn(inventory);

        when(team.getDisplayName()).thenReturn("teamA");
        when(otherTeam.getDisplayName()).thenReturn("teamB");

        when(mgr.getPlayerTeam(player)).thenReturn(team);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);
        when(scorecard.getBlockID(team)).thenReturn(Material.RED_WOOL);
    }

    /**
     * Setup beacon ownership and state mocks.
     */
    protected void setupBeaconMocks() {
        when(beacon.getOwnership()).thenReturn(null);
    }
}

