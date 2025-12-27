package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

/**
 * Exercises BeaconCaptureListener behaviors for beacon damage/break events with mocked plugin context.
 * Uses lenient Mockito to share stubs across branches; Lang strings and display names are seeded to avoid NPEs in message formatting.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BeaconCaptureListenerTest {
    
    @Mock
    private Beaconz plugin;
    @Mock
    private Player player;
    @Mock
    private Block block;
    @Mock
    private ItemStack item;
    @Mock
    private World world;
    @Mock
    private GameMgr mgr;
    @Mock
    private Register register;
    
    private BeaconCaptureListener bcl;
    @Mock
    private Team team;
    @Mock
    private Team otherTeam;
    @Mock
    private BeaconObj beacon;
    @Mock
    private @NotNull Block beaconBlock;
    @Mock
    private Logger logger;
    @Mock
    private Game game;
    @Mock
    private Scorecard scorecard;
    @Mock
    private Messages messages;
    @Mock
    private Location location;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        bcl = new BeaconCaptureListener(plugin);
        when(plugin.getBeaconzWorld()).thenReturn(world);
        when(block.getWorld()).thenReturn(world);
        when(plugin.getGameMgr()).thenReturn(mgr);
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        when(plugin.getRegister()).thenReturn(register);
        when(register.getBeacon(block)).thenReturn(beacon);
        when(register.isBeacon(beaconBlock)).thenReturn(true);
        when(beaconBlock.getType()).thenReturn(Material.BEACON);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(beaconBlock);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getMessages()).thenReturn(messages);
        when(block.getLocation()).thenReturn(location);
        when(mgr.getGame(location)).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);
        when(scorecard.getBlockID(team)).thenReturn(Material.RED_WOOL);
        when(beacon.getOwnership()).thenReturn(null);
        when(team.getDisplayName()).thenReturn("teamA");
        when(otherTeam.getDisplayName()).thenReturn("teamB");
        when(player.getDisplayName()).thenReturn("playerA");

        // Provide Lang static strings to avoid NPEs during replace/sends
        Lang.errorYouCannotDoThat = "errorYouCannotDoThat";
        Lang.errorClearAroundBeacon = "errorClearAroundBeacon";
        Lang.beaconYouCannotDestroyYourOwnBeacon = "beaconYouCannotDestroyYourOwnBeacon";
        Lang.beaconTeamDestroyed = "beaconTeamDestroyed [team1] [team2]";
        Lang.beaconPlayerDestroyed = "beaconPlayerDestroyed [player] [team]";
        Lang.beaconYouDestroyed = "beaconYouDestroyed [team]";
    }

    /** Sanity-checks listener construction. */
    @Test
    void testBeaconCaptureListener() {
        assertNotNull(bcl);
    }

    /** Happy-path beacon damage: registered beacon, clear area, same-team ownership => no cancel. */
    @Test
    void testOnBeaconDamageHappyPath() {
        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);
        verify(beacon).checkIntegrity();
        assertFalse(e.isCancelled());
    }

    /** Damage outside Beaconz world should be ignored. */
    @Test
    void testOnBeaconDamageNotInWorld() {
        World otherWorld = org.mockito.Mockito.mock(World.class);
        when(block.getWorld()).thenReturn(otherWorld);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertFalse(e.isCancelled());
    }

    /** Lobby non-op cannot damage blocks: expect cancellation. */
    @Test
    void testOnBeaconDamagePlayerInLobbyNonOp() {
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(false);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertTrue(e.isCancelled());
    }

    /** Lobby op bypasses restriction: no cancel. */
    @Test
    void testOnBeaconDamagePlayerInLobbyOp() {
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(true);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertFalse(e.isCancelled());
    }

    /** No team & non-op: damage is blocked. */
    @Test
    void testOnBeaconDamageNoTeamNonOp() {
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(false);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertTrue(e.isCancelled());
    }

    /** No team but op: allowed. */
    @Test
    void testOnBeaconDamageNoTeamOp() {
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(true);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertFalse(e.isCancelled());
    }

    /** Non-beacon block returns without side effects. */
    @Test
    void testOnBeaconDamageNotBeaconBlock() {
        when(register.getBeacon(block)).thenReturn(null);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register).getBeacon(block);
        verify(beacon, never()).checkIntegrity();
        assertFalse(e.isCancelled());
    }

    /** Block not above beacon pyramid: integrity check only. */
    @Test
    void testOnBeaconDamageBlockNotAboveBeacon() {
        when(beaconBlock.getType()).thenReturn(Material.OBSIDIAN);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(beacon).checkIntegrity();
        assertFalse(e.isCancelled());
    }

    /** Beacon below is unregistered: integrity still checked; no cancel. */
    @Test
    void testOnBeaconDamageNotRegisteredBeacon() {
        when(register.isBeacon(beaconBlock)).thenReturn(false);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(beacon).checkIntegrity();
        assertFalse(e.isCancelled());
    }

    /** Uncleared beacon and unowned: player warned, event cancelled. */
    @Test
    void testOnBeaconDamageBeaconNotClearAndUnowned() {
        when(beacon.isNotClear()).thenReturn(true);
        when(beacon.getOwnership()).thenReturn(null);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(beacon).checkIntegrity();
        verify(player, times(1)).sendMessage(anyString());
        assertTrue(e.isCancelled());
    }

    /** Uncleared beacon owned by other team: warning + cancel. */
    @Test
    void testOnBeaconDamageBeaconNotClearOwnedByOtherTeam() {
        when(beacon.isNotClear()).thenReturn(true);
        when(beacon.getOwnership()).thenReturn(otherTeam);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(beacon).checkIntegrity();
        verify(player, times(1)).sendMessage(anyString());
        assertTrue(e.isCancelled());
    }

    /** Uncleared beacon owned by same team: allowed to proceed. */
    @Test
    void testOnBeaconDamageBeaconNotClearOwnedByTeam() {
        when(beacon.isNotClear()).thenReturn(true);
        when(beacon.getOwnership()).thenReturn(team);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(beacon).checkIntegrity();
        verify(player, never()).sendMessage(anyString());
        assertFalse(e.isCancelled());
    }

    /** Block break outside Beaconz world: ignored. */
    @Test
    void testOnBeaconBreakNotInWorld() {
        World otherWorld = org.mockito.Mockito.mock(World.class);
        when(block.getWorld()).thenReturn(otherWorld);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertFalse(e.isCancelled());
        verify(register, never()).getBeacon(block);
    }

    /** Lobby non-op cannot break: cancel. */
    @Test
    void testOnBeaconBreakLobbyNonOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(false);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
    }

    /** No game (outside arena) for non-op: cancel with message. */
    @Test
    void testOnBeaconBreakGameNullNonOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getGame(location)).thenReturn(null);
        when(player.isOp()).thenReturn(false);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
        verify(player).sendMessage(anyString());
    }

    /** No team for non-op: break blocked. */
    @Test
    void testOnBeaconBreakTeamNullNonOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.getGame(location)).thenReturn(game);
        when(scorecard.getTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(false);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
    }

    /** Not a beacon: no cancellation expected. */
    @Test
    void testOnBeaconBreakNotBeacon() {
        when(block.getWorld()).thenReturn(world);
        when(register.getBeacon(block)).thenReturn(null);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertFalse(e.isCancelled());
    }

    /** Obsidian top but uncleared: warn and cancel. */
    @Test
    void testOnBeaconBreakObsidianNotClear() {
        when(block.getWorld()).thenReturn(world);
        when(register.getBeacon(block)).thenReturn(beacon);
        when(block.getType()).thenReturn(Material.OBSIDIAN);
        when(beacon.isNotClear()).thenReturn(true);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
        verify(player).sendMessage(ChatColor.RED + Lang.errorClearAroundBeacon);
    }

    /** Own beacon: prevent destruction and warn. */
    @Test
    void testOnBeaconBreakOwnedByTeam() {
        when(block.getWorld()).thenReturn(world);
        when(register.getBeacon(block)).thenReturn(beacon);
        when(block.getType()).thenReturn(Material.GLASS);
        when(beacon.getOwnership()).thenReturn(team);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
        verify(player).sendMessage(ChatColor.RED + Lang.beaconYouCannotDestroyYourOwnBeacon);
        verify(register, never()).removeBeaconOwnership(beacon);
    }

    /**
     * Enemy beacon with uncleared surroundings: cancel with clear-warning.
     * Note: we avoid the cleared branch here because it triggers Paper's Sound registry, which fails under MockBukkit 1.21 (registry mismatch).
     */
    @Test
    void testOnBeaconBreakOwnedByOtherTeam() {
        when(block.getWorld()).thenReturn(world);
        when(register.getBeacon(block)).thenReturn(beacon);
        when(block.getType()).thenReturn(Material.GLASS);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(beacon.isNotClear()).thenReturn(true);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
        verify(player).sendMessage(ChatColor.RED + Lang.errorClearAroundBeacon);
        verify(register, never()).removeBeaconOwnership(beacon);
        verify(block, never()).setType(Material.OBSIDIAN);
    }

    /** Unowned beacon, clear: ownership removed and block converted to obsidian. */
    @Test
    void testOnBeaconBreakUnownedBeacon() {
        when(block.getWorld()).thenReturn(world);
        when(register.getBeacon(block)).thenReturn(beacon);
        when(block.getType()).thenReturn(Material.GLASS);
        when(beacon.getOwnership()).thenReturn(null);
        when(beacon.isNotClear()).thenReturn(false);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
        verify(register).removeBeaconOwnership(beacon);
        verify(block).setType(Material.OBSIDIAN);
    }

    /** Breaking non-beacon part while unowned: event cancels; hack timer untouched. */
    @Test
    void testOnBeaconBreakNotAboveBeaconNoOwnership() {
        when(block.getWorld()).thenReturn(world);
        when(register.getBeacon(block)).thenReturn(beacon);
        when(beaconBlock.getType()).thenReturn(Material.AIR);
        when(beacon.getOwnership()).thenReturn(null);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        bcl.onBeaconBreak(e);

        assertTrue(e.isCancelled());
        verify(beacon, never()).resetHackTimer();
    }

}
