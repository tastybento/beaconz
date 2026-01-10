package com.wasteofplastic.beaconz.listeners;

import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
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
import org.mockbukkit.mockbukkit.attribute.AttributeInstanceMock;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzStore;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.GameMgr;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Messages;
import com.wasteofplastic.beaconz.Register;
import com.wasteofplastic.beaconz.Scorecard;
import com.wasteofplastic.beaconz.TinyDB;

import net.kyori.adventure.text.Component;

/**
 * Base class for listener tests providing common mocks and setup.
 * Subclasses can override setupMocks() to customize behavior for specific tests.
 *
 * <p>Uses lenient Mockito to allow shared stubs across test methods.
 * Lang static strings are initialized to prevent NPEs during message formatting.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class CommonTestBase {

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
    @Mock
    protected BeaconzStore store;
    @Mock
    protected TinyDB nameStore;

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
        Lang.errorYouCannotDoThat = Component.text("errorYouCannotDoThat");
        Lang.errorClearAroundBeacon = Component.text("errorClearAroundBeacon");
        Lang.errorYouMustBeInAGame = Component.text("errorYouMustBeInAGame");
        Lang.errorYouMustBeInATeam = Component.text("errorYouMustBeInATeam");
        Lang.errorCanOnlyPlaceBlocks = Component.text("errorCanOnlyPlaceBlocks");
        Lang.errorCanOnlyPlaceBlocksUpTo = Component.text("errorCanOnlyPlaceBlocksUpTo [value]");
        Lang.errorYouNeedToBeLevel = Component.text("errorYouNeedToBeLevel [value]");
        Lang.errorTooFar = Component.text("errorTooFar [max]");
        Lang.errorNotEnoughExperience = Component.text("errorNotEnoughExperience");
        Lang.errorYouCannotRemoveOtherPlayersBlocks = Component.text("errorYouCannotRemoveOtherPlayersBlocks");

        // Beacon capture/destroy messages
        Lang.beaconYouCannotDestroyYourOwnBeacon = Component.text("beaconYouCannotDestroyYourOwnBeacon");
        Lang.beaconTeamDestroyed = Component.text("beaconTeamDestroyed [team1] [team2]");
        Lang.beaconPlayerDestroyed = Component.text("beaconPlayerDestroyed [player] [team]");
        Lang.beaconYouDestroyed = Component.text("beaconYouDestroyed [team]");
        Lang.beaconYouMustCapturedBeacon = Component.text("beaconYouMustCapturedBeacon");

        // Beacon linking messages
        Lang.beaconOriginNotOwned = Component.text("beaconOriginNotOwned [team]");
        Lang.beaconYouNeedThisMuchExp = Component.text("beaconYouNeedThisMuchExp [number]");
        Lang.beaconYouHaveThisMuchExp = Component.text("beaconYouHaveThisMuchExp [number]");
        Lang.beaconTheMapDisintegrates = Component.text("beaconTheMapDisintegrates");

        // Defense-related messages
        Lang.beaconYouCanOnlyExtend = Component.text("beaconYouCanOnlyExtend");
        Lang.beaconCannotBeExtended = Component.text("beaconCannotBeExtended");
        Lang.beaconExtended = Component.text("beaconExtended");
        Lang.beaconLockedJustNow = Component.text("beaconLockedJustNow [lockingBlock]");
        Lang.beaconLockedAlready = Component.text("beaconLockedAlready [lockingBlock]");
        Lang.beaconLockedWithNMoreBlocks = Component.text("beaconLockedWithNMoreBlocks [number]");
        Lang.beaconLinkBlockPlaced = Component.text("beaconLinkBlockPlaced [range]");
        Lang.beaconDefensePlaced = Component.text("beaconDefensePlaced");
        Lang.beaconLinkBlockBroken = Component.text("beaconLinkBlockBroken [range]");
        Lang.beaconLinkLost = Component.text("beaconLinkLost");
        Lang.beaconDefenseRemoveTopDown = Component.text("beaconDefenseRemoveTopDown");
        Lang.beaconLocked = Component.text("beaconLocked");
        Lang.beaconAmplifierBlocksCannotBeRecovered = Component.text("beaconAmplifierBlocksCannotBeRecovered");
        Lang.generalLevel = Component.text("Level");
        Lang.triangleEntering = Component.text("[team] triangleEntering");
        Lang.triangleLeaving = Component.text("[team] triangleLeaving");
        Lang.triangleDroppingToLevel = Component.text("[team] triangleDroppingToLevel");
        
        // Lobby
        Lang.adminUseSurvival = Component.text("adminUseSurvival");
        Lang.adminSignKeyword = Component.text("adminkeyword");
        Lang.errorNotReady = Component.text("errorNotReady");
        Lang.errorNoSuchGame = Component.text("errorNoSuchGame");
        Lang.adminGameSignPlaced = Component.text("adminGameSignPlaced");
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
        when(plugin.getBeaconzStore()).thenReturn(store);
        when(plugin.getNameStore()).thenReturn(nameStore);
        when(plugin.getServer()).thenReturn(server);
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
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(player.getDisplayName()).thenReturn("playerA");
        when(player.getInventory()).thenReturn(inventory);
        AttributeInstanceMock health = new AttributeInstanceMock(Attribute.MAX_HEALTH, 20D);
        when(player.getAttribute(Attribute.MAX_HEALTH)).thenReturn(health);

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

