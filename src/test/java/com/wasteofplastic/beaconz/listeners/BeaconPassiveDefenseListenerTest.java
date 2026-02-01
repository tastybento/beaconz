package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.core.DefenseBlock;
import com.wasteofplastic.beaconz.core.Region;

import net.kyori.adventure.text.Component;

/**
 * Comprehensive tests for BeaconPassiveDefenseListener covering defense placement, breaking, and protection mechanics.
 * Extends BeaconzListenerTestBase for shared test infrastructure.
 *
 * <p>Tests cover: explosion protection, piston protection, block placement validation,
 * defense block breaking rules, damage handling, and liquid flow prevention.
 */
class BeaconPassiveDefenseListenerTest extends CommonTestBase {

    private BeaconPassiveDefenseListener listener;

    @Mock
    private EntityExplodeEvent explodeEvent;
    @Mock
    private BlockPistonExtendEvent pistonExtendEvent;
    @Mock
    private BlockPistonRetractEvent pistonRetractEvent;
    @Mock
    private BlockFromToEvent blockFlowEvent;
    @Mock
    private Block adjacentBlock;
    @Mock
    private Block toBlock;
    @Mock
    private BlockState blockState;
    @Mock
    private DefenseBlock defenseBlock;
    @Mock
    private Region lobby;

    @Override
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        listener = new BeaconPassiveDefenseListener(plugin);

        // Setup additional Lang strings for defense listener
        Lang.errorYouMustBeInAGame = Component.text("errorYouMustBeInAGame");
        Lang.errorYouMustBeInATeam = Component.text("errorYouMustBeInATeam");
        Lang.beaconYouCanOnlyExtend = Component.text("beaconYouCanOnlyExtend");
        Lang.beaconCannotBeExtended = Component.text("beaconCannotBeExtended");
        Lang.errorClearAboveBeacon = Component.text("errorClearAboveBeacon");
        Lang.beaconExtended = Component.text("beaconExtended");
        Lang.beaconLockedJustNow = Component.text("beaconLockedJustNow [lockingBlock]");
        Lang.beaconLockedAlready = Component.text("beaconLockedAlready [lockingBlock]");
        Lang.beaconLockedWithNMoreBlocks = Component.text("beaconLockedWithNMoreBlocks [number]");
        Lang.errorCanOnlyPlaceBlocks = Component.text("errorCanOnlyPlaceBlocks");
        Lang.errorCanOnlyPlaceBlocksUpTo = Component.text("errorCanOnlyPlaceBlocksUpTo [value]");
        Lang.errorYouNeedToBeLevel = Component.text("errorYouNeedToBeLevel [value]");
        Lang.generalLevel = Component.text("Level");
        Lang.beaconLinkBlockPlaced = Component.text("beaconLinkBlockPlaced [range]");
        Lang.beaconDefensePlaced = Component.text("beaconDefensePlaced");
        Lang.beaconLinkBlockBroken = Component.text("beaconLinkBlockBroken [range]");
        Lang.beaconLinkLost = Component.text("beaconLinkLost");
        Lang.beaconDefenseRemoveTopDown = Component.text("beaconDefenseRemoveTopDown");
        Lang.errorYouCannotRemoveOtherPlayersBlocks = Component.text("errorYouCannotRemoveOtherPlayersBlocks");
        Lang.beaconLocked = Component.text("beaconLocked");
        Lang.beaconAmplifierBlocksCannotBeRecovered = Component.text("beaconAmplifierBlocksCannotBeRecovered");

        // Setup Settings defaults
        Settings.defenseHeight = 10;
        Settings.defenseLevels = List.of(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50);
        Settings.attackLevels = List.of(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50);
        Settings.linkBlocks = new HashMap<>();
        Settings.linkBlocks.put(Material.DIAMOND_BLOCK, 100);
        Settings.lockingBlock = "EMERALD_BLOCK";
        Settings.removaldelta = 0;
        Settings.destroyLinkBlocks = false;
        Settings.removeLongestLink = false;

        // Initialize defenseText HashMap to avoid NPE
        Lang.defenseText = new java.util.HashMap<>();
        Lang.defenseText.put(Material.STONE, Component.text("Stone defense block"));
        Lang.defenseText.put(Material.GLASS, Component.text("Glass defense block"));
        Lang.defenseText.put(Material.OBSIDIAN, Component.text("Obsidian defense block"));

        // Setup block state mock
        when(block.getState()).thenReturn(blockState);

        // Setup lobby mock for teleportation
        when(mgr.getLobby()).thenReturn(lobby);
        when(mgr.getSC(player)).thenReturn(scorecard);
    }

    /** Sanity check: listener construction. */
    @Test
    void testBeaconPassiveDefenseListener() {
        assertNotNull(listener);
    }

    // ==================== onExplode Tests ====================

    /** Explosion in wrong world: no protection applied. */
    @Test
    void testOnExplodeWrongWorld() {
        when(explodeEvent.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(mock(org.bukkit.World.class));
        List<Block> blockList = new ArrayList<>();
        when(explodeEvent.blockList()).thenReturn(blockList);

        listener.onExplode(explodeEvent);

        assertTrue(blockList.isEmpty(), "Should not modify block list in wrong world");
    }

    /** Explosion removes blocks above beacons from damage list. */
    @Test
    void testOnExplodeProtectsBeaconBlocks() {
        when(explodeEvent.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);

        Block protectedBlock = mock(Block.class);
        Block unprotectedBlock = mock(Block.class);
        Location protectedLoc = mock(Location.class);
        Location unprotectedLoc = mock(Location.class);

        when(protectedBlock.getLocation()).thenReturn(protectedLoc);
        when(unprotectedBlock.getLocation()).thenReturn(unprotectedLoc);
        when(register.isAboveBeacon(protectedLoc)).thenReturn(true);
        when(register.isAboveBeacon(unprotectedLoc)).thenReturn(false);

        List<Block> blockList = new ArrayList<>();
        blockList.add(protectedBlock);
        blockList.add(unprotectedBlock);
        when(explodeEvent.blockList()).thenReturn(blockList);

        listener.onExplode(explodeEvent);

        assertEquals(1, blockList.size(), "Protected block should be removed from explosion list");
        assertTrue(blockList.contains(unprotectedBlock), "Unprotected block should remain");
    }

    // ==================== onPistonPush Tests ====================

    /** Piston push in wrong world: no cancellation. */
    @Test
    void testOnPistonPushWrongWorld() {
        when(block.getWorld()).thenReturn(mock(org.bukkit.World.class));
        when(pistonExtendEvent.getBlock()).thenReturn(block);

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent, never()).setCancelled(anyBoolean());
    }

    /** Piston tries to push block into beacon area: cancelled. */
    @Test
    void testOnPistonPushIntoBeaconArea() {
        when(block.getWorld()).thenReturn(world);
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.UP);

        Block movingBlock = mock(Block.class);
        Block destination = mock(Block.class);
        Location destLocation = mock(Location.class);

        when(movingBlock.getRelative(BlockFace.UP)).thenReturn(destination);
        when(destination.getLocation()).thenReturn(destLocation);
        when(register.isAboveBeacon(destLocation)).thenReturn(true);

        when(pistonExtendEvent.getBlocks()).thenReturn(List.of(movingBlock));

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent).setCancelled(true);
    }

    /** Piston push away from beacon: allowed. */
    @Test
    void testOnPistonPushAwayFromBeacon() {
        when(block.getWorld()).thenReturn(world);
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.NORTH);

        Block movingBlock = mock(Block.class);
        Block destination = mock(Block.class);
        Location destLocation = mock(Location.class);

        when(movingBlock.getRelative(BlockFace.NORTH)).thenReturn(destination);
        when(destination.getLocation()).thenReturn(destLocation);
        when(register.isAboveBeacon(destLocation)).thenReturn(false);

        when(pistonExtendEvent.getBlocks()).thenReturn(List.of(movingBlock));

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent, never()).setCancelled(true);
    }

    // ==================== onPistonPull Tests ====================

    /** Piston pull in wrong world: no cancellation. */
    @Test
    void testOnPistonPullWrongWorld() {
        when(block.getWorld()).thenReturn(mock(org.bukkit.World.class));
        when(pistonRetractEvent.getBlock()).thenReturn(block);

        listener.onPistonPull(pistonRetractEvent);

        verify(pistonRetractEvent, never()).setCancelled(anyBoolean());
    }

    /** Sticky piston tries to pull beacon block: cancelled. */
    @Test
    void testOnPistonPullBeaconBlock() {
        when(block.getWorld()).thenReturn(world);
        when(pistonRetractEvent.getBlock()).thenReturn(block);

        Block pulledBlock = mock(Block.class);
        Location pulledLocation = mock(Location.class);

        when(pulledBlock.getLocation()).thenReturn(pulledLocation);
        when(register.isAboveBeacon(pulledLocation)).thenReturn(true);

        when(pistonRetractEvent.getBlocks()).thenReturn(List.of(pulledBlock));

        listener.onPistonPull(pistonRetractEvent);

        verify(pistonRetractEvent).setCancelled(true);
    }

    // ==================== onBlockPlace Tests ====================

    /** Place block in wrong world: ignored. */
    @Test
    void testOnBlockPlaceWrongWorld() {
        when(block.getWorld()).thenReturn(mock(org.bukkit.World.class));
        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
    }

    /** Non-op player in lobby cannot place blocks. */
    @Test
    void testOnBlockPlaceLobbyNonOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(false);
        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
    }

    /** Op player in lobby can place blocks. */
    @Test
    void testOnBlockPlaceLobbyOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(true);
        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
    }

    /** Player not in game (non-op): cancelled with message (no teleport as Lobby doesn't exist). */
    @Test
    void testOnBlockPlaceNotInGameNonOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(null);
        when(player.isOp()).thenReturn(false);
        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(any(Component.class));
        verify(lobby).tpToRegionSpawn(player, true);
    }

    /** Player in game but no team (op): gets message but not cancelled. */
    @Test
    void testOnBlockPlaceNoTeamOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(true);
        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
        verify(player).sendMessage(Lang.errorYouMustBeInATeam);
    }

    /**
     * Emerald block extension: player tries to extend own beacon.
     * Note: Complex test with multiple adjacency checks - tests NORTH adjacency.
     */
    @Test
    void testOnBlockPlaceEmeraldExtension() {
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.EMERALD_BLOCK);
        when(block.getY()).thenReturn(64);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        // Setup adjacent beacon
        when(block.getRelative(BlockFace.NORTH)).thenReturn(adjacentBlock);
        when(adjacentBlock.getX()).thenReturn(100);
        when(adjacentBlock.getZ()).thenReturn(101);
        when(beacon.getY()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(team);
        when(beacon.getLocation()).thenReturn(location);
        when(location.distanceSquared(any())).thenReturn(4.0); // Within range
        when(register.getBeaconAt(new Point2D.Double(100, 101))).thenReturn(beacon);
        when(plugin.getHighestBlockYAt(100, 100)).thenReturn(64);

        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        verify(register).addBeaconDefenseBlock(any(Location.class), eq(beacon));
        verify(player).sendMessage(Lang.beaconExtended);
    }

    /** Emerald extension but wrong team: cancelled. */
    @Test
    void testOnBlockPlaceEmeraldExtensionWrongTeam() {
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.EMERALD_BLOCK);
        when(block.getY()).thenReturn(64);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        when(block.getRelative(BlockFace.NORTH)).thenReturn(adjacentBlock);
        when(adjacentBlock.getX()).thenReturn(100);
        when(adjacentBlock.getZ()).thenReturn(101);
        when(beacon.getY()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(register.getBeaconAt(new Point2D.Double(100, 101))).thenReturn(beacon);

        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.beaconYouCanOnlyExtend);
    }

    /** Place defense block on own beacon: allowed with level check. */
    @Test
    void testOnBlockPlaceDefenseBlockSufficientLevel() {
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.STONE);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        when(beacon.getY()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(team);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);
        when(player.getLevel()).thenReturn(10);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        // No adjacent beacon for extension
        when(block.getRelative(any(BlockFace.class))).thenReturn(adjacentBlock);
        when(adjacentBlock.getX()).thenReturn(99);
        when(adjacentBlock.getZ()).thenReturn(99);
        when(register.getBeaconAt(any(Point2D.Double.class))).thenReturn(null, null, null, null, beacon);

        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled());
        verify(beacon).addDefenseBlock(eq(block), eq(5), any(UUID.class));
    }

    /** Place defense block without sufficient level: cancelled. */
    @Test
    void testOnBlockPlaceDefenseBlockInsufficientLevel() {
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.STONE);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        when(beacon.getY()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(team);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);
        when(player.getLevel()).thenReturn(3); // Below required level 5

        when(block.getRelative(any(BlockFace.class))).thenReturn(adjacentBlock);
        when(register.getBeaconAt(any(Point2D.Double.class))).thenReturn(null, null, null, null, beacon);

        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(argThat((Component component) ->
            component.equals(Lang.errorYouNeedToBeLevel.replaceText(builder ->
                builder.matchLiteral("[value]").replacement(Component.text("5"))))
        ));
    }

    /** Place defense block on enemy beacon: cancelled. */
    @Test
    void testOnBlockPlaceDefenseBlockEnemyBeacon() {
        when(block.getWorld()).thenReturn(world);
        when(block.getType()).thenReturn(Material.STONE);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        when(beacon.getY()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        when(block.getRelative(any(BlockFace.class))).thenReturn(adjacentBlock);
        when(register.getBeaconAt(any(Point2D.Double.class))).thenReturn(null, null, null, null, beacon);

        BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, adjacentBlock, item, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.errorCanOnlyPlaceBlocks);
    }

    // ==================== onBeaconBreak Tests ====================

    /** Break block in wrong world: ignored. */
    @Test
    void testOnBeaconBreakWrongWorld() {
        when(block.getWorld()).thenReturn(mock(org.bukkit.World.class));
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertFalse(event.isCancelled());
    }

    /** Non-op in lobby cannot break: cancelled. */
    @Test
    void testOnBeaconBreakLobbyNonOp() {
        when(block.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(false);
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
    }

    /** Break defense block from own beacon: allowed if placed by same player. */
    @Test
    void testOnBeaconBreakOwnDefenseBlock() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(block.getType()).thenReturn(Material.STONE);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        UUID playerUUID = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUUID);
        when(beacon.getY()).thenReturn(64);
        when(beacon.getHeight()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(team);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getPlacer()).thenReturn(playerUUID);
        when(defenseBlock.getLevel()).thenReturn(1);
        defenseBlocks.put(block, defenseBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        when(block.getRelative(any())).thenReturn(block);

        listener.onBeaconBreak(event);

        assertFalse(event.isCancelled());
        verify(beacon).removeDefenseBlock(block);
    }

    /** Break defense block placed by teammate with removaldelta < 0: blocked. */
    @Test
    void testOnBeaconBreakTeammateBlockRemovalDeltaNegative() {
        Settings.removaldelta = -1;

        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(beacon.getY()).thenReturn(64);
        when(beacon.getHeight()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(team);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getPlacer()).thenReturn(UUID.randomUUID()); // Different player
        when(defenseBlock.getLevel()).thenReturn(1);
        defenseBlocks.put(block, defenseBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        when(block.getRelative(any())).thenReturn(block);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.errorYouCannotRemoveOtherPlayersBlocks);
    }

    /** Break enemy defense block from locked beacon: cancelled. */
    @Test
    void testOnBeaconBreakEnemyBlockLockedBeacon() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(block.getType()).thenReturn(Material.STONE);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        when(beacon.getY()).thenReturn(64);
        when(beacon.getHeight()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(beacon.isLocked()).thenReturn(true);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getLevel()).thenReturn(1);
        defenseBlocks.put(block, defenseBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        when(block.getRelative(any())).thenReturn(block);

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.beaconLocked);
    }

    /** Break enemy defense block: must have sufficient level. */
    @Test
    void testOnBeaconBreakEnemyBlockInsufficientLevel() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);
        when(player.getLevel()).thenReturn(5);

        when(beacon.getY()).thenReturn(64);
        when(beacon.getHeight()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(beacon.isLocked()).thenReturn(false);
        when(beacon.getHighestBlockLevel()).thenReturn(10);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        when(block.getRelative(any())).thenReturn(block);

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getLevel()).thenReturn(10);
        defenseBlocks.put(block, defenseBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(argThat((Component component) ->
            component.equals(Lang.errorYouNeedToBeLevel.replaceText(builder -> builder.matchLiteral("[value]").replacement(Component.text("10"))))
        ));
    }

    /** Break enemy defense block not from top: cancelled. */
    @Test
    void testOnBeaconBreakEnemyBlockNotTopDown() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(block.getType()).thenReturn(Material.STONE);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);
        when(player.getLevel()).thenReturn(20);

        when(beacon.getY()).thenReturn(64);
        when(beacon.getHeight()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(beacon.isLocked()).thenReturn(false);
        when(beacon.getHighestBlockLevel()).thenReturn(10);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getLevel()).thenReturn(5); // Not highest
        defenseBlocks.put(block, defenseBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        when(block.getRelative(any())).thenReturn(block);

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.beaconDefenseRemoveTopDown);
    }

    // ==================== onDefenseDamage Tests ====================

    /** Damage block in wrong world: ignored. */
    @Test
    void testOnDefenseDamageWrongWorld() {
        when(block.getWorld()).thenReturn(mock(org.bukkit.World.class));
        BlockDamageEvent event = new BlockDamageEvent(player, block, BlockFace.UP, item, false);

        listener.onDefenseDamage(event);

        assertFalse(event.isCancelled());
    }

    /** Damage own defense block: allowed. */
    @Test
    void testOnDefenseDamageOwnBlock() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);

        UUID playerUUID = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUUID);
        when(beacon.getY()).thenReturn(64);
        when(beacon.getHeight()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(team);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getPlacer()).thenReturn(playerUUID);
        when(defenseBlock.getLevel()).thenReturn(1);
        defenseBlocks.put(block, defenseBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        BlockDamageEvent event = new BlockDamageEvent(player, block, BlockFace.UP, item, false);

        listener.onDefenseDamage(event);

        assertFalse(event.isCancelled());
    }

    /** Damage enemy defense not from top: warned and cancelled. */
    @Test
    void testOnDefenseDamageEnemyNotTopDown() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(block.getType()).thenReturn(Material.STONE);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getSC(player)).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);
        when(player.getLevel()).thenReturn(20);

        when(beacon.getY()).thenReturn(64);
        when(beacon.getHeight()).thenReturn(65);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(register.getBeaconAt(new Point2D.Double(100, 100))).thenReturn(beacon);

        Block otherBlock = mock(Block.class);
        DefenseBlock otherDefBlock = mock(DefenseBlock.class);
        when(otherBlock.getType()).thenReturn(Material.STONE);
        when(otherDefBlock.getLevel()).thenReturn(10);

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getLevel()).thenReturn(5);
        defenseBlocks.put(block, defenseBlock);
        defenseBlocks.put(otherBlock, otherDefBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        BlockDamageEvent event = new BlockDamageEvent(player, block, BlockFace.UP, item, false);

        listener.onDefenseDamage(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.beaconDefenseRemoveTopDown);
    }

    // ==================== onBlockFlow Tests ====================

    /** Liquid flow in wrong world: not blocked. */
    @Test
    void testOnBlockFlowWrongWorld() {
        when(block.getWorld()).thenReturn(mock(org.bukkit.World.class));
        when(block.isLiquid()).thenReturn(true);
        when(blockFlowEvent.getBlock()).thenReturn(block);
        when(blockFlowEvent.getToBlock()).thenReturn(toBlock);

        listener.onBlockFlow(blockFlowEvent);

        verify(blockFlowEvent, never()).setCancelled(anyBoolean());
    }

    /** Non-liquid block: ignored. */
    @Test
    void testOnBlockFlowNotLiquid() {
        when(block.getWorld()).thenReturn(world);
        when(block.isLiquid()).thenReturn(false);
        when(blockFlowEvent.getBlock()).thenReturn(block);

        listener.onBlockFlow(blockFlowEvent);

        verify(blockFlowEvent, never()).setCancelled(anyBoolean());
    }

    /** Liquid tries to flow onto beacon area: blocked. */
    @Test
    void testOnBlockFlowOntoBeacon() {
        when(block.getWorld()).thenReturn(world);
        when(block.isLiquid()).thenReturn(true);
        when(blockFlowEvent.getBlock()).thenReturn(block);
        when(blockFlowEvent.getToBlock()).thenReturn(toBlock);

        Location toLoc = mock(Location.class);
        when(toBlock.getLocation()).thenReturn(toLoc);
        when(register.isAboveBeacon(toLoc)).thenReturn(true);

        listener.onBlockFlow(blockFlowEvent);

        verify(blockFlowEvent).setCancelled(true);
    }

    /** Liquid flows away from beacon: allowed. */
    @Test
    void testOnBlockFlowAwayFromBeacon() {
        when(block.getWorld()).thenReturn(world);
        when(block.isLiquid()).thenReturn(true);
        when(blockFlowEvent.getBlock()).thenReturn(block);
        when(blockFlowEvent.getToBlock()).thenReturn(toBlock);

        Location toLoc = mock(Location.class);
        when(toBlock.getLocation()).thenReturn(toLoc);
        when(register.isAboveBeacon(toLoc)).thenReturn(false);

        listener.onBlockFlow(blockFlowEvent);

        verify(blockFlowEvent, never()).setCancelled(true);
    }

    // ==================== Adjacency Logic Tests ====================

    /** Test findAdjacentBeacon finds beacon to the NORTH. */
    @Test
    void testFindAdjacentBeaconNorth() {
        Block northBlock = org.mockito.Mockito.mock(Block.class);
        when(block.getRelative(BlockFace.NORTH)).thenReturn(northBlock);
        when(northBlock.getX()).thenReturn(100);
        when(northBlock.getZ()).thenReturn(199);
        
        when(register.getBeaconAt(new Point2D.Double(100, 199))).thenReturn(beacon);

        var result = listener.findAdjacentBeacon(block);

        assertTrue(result.isPresent());
        assertEquals(beacon, result.get());
    }

    /** Test findAdjacentBeacon finds beacon to the SOUTH. */
    @Test
    void testFindAdjacentBeaconSouth() {
        Block southBlock = org.mockito.Mockito.mock(Block.class);
        when(block.getRelative(BlockFace.NORTH)).thenReturn(org.mockito.Mockito.mock(Block.class));
        when(block.getRelative(BlockFace.SOUTH)).thenReturn(southBlock);
        when(southBlock.getX()).thenReturn(100);
        when(southBlock.getZ()).thenReturn(201);

        when(register.getBeaconAt(new Point2D.Double(100, 201))).thenReturn(beacon);

        var result = listener.findAdjacentBeacon(block);

        assertTrue(result.isPresent());
        assertEquals(beacon, result.get());
    }

    /** Test findAdjacentBeacon finds beacon to the EAST. */
    @Test
    void testFindAdjacentBeaconEast() {
        Block eastBlock = org.mockito.Mockito.mock(Block.class);
        when(block.getRelative(BlockFace.NORTH)).thenReturn(org.mockito.Mockito.mock(Block.class));
        when(block.getRelative(BlockFace.SOUTH)).thenReturn(org.mockito.Mockito.mock(Block.class));
        when(block.getRelative(BlockFace.EAST)).thenReturn(eastBlock);
        when(eastBlock.getX()).thenReturn(101);
        when(eastBlock.getZ()).thenReturn(200);

        when(register.getBeaconAt(new Point2D.Double(101, 200))).thenReturn(beacon);

        var result = listener.findAdjacentBeacon(block);

        assertTrue(result.isPresent());
        assertEquals(beacon, result.get());
    }

    /** Test findAdjacentBeacon finds beacon to the WEST. */
    @Test
    void testFindAdjacentBeaconWest() {
        Block westBlock = org.mockito.Mockito.mock(Block.class);
        when(block.getRelative(BlockFace.NORTH)).thenReturn(org.mockito.Mockito.mock(Block.class));
        when(block.getRelative(BlockFace.SOUTH)).thenReturn(org.mockito.Mockito.mock(Block.class));
        when(block.getRelative(BlockFace.EAST)).thenReturn(org.mockito.Mockito.mock(Block.class));
        when(block.getRelative(BlockFace.WEST)).thenReturn(westBlock);
        when(westBlock.getX()).thenReturn(99);
        when(westBlock.getZ()).thenReturn(200);

        when(register.getBeaconAt(new Point2D.Double(99, 200))).thenReturn(beacon);

        var result = listener.findAdjacentBeacon(block);

        assertTrue(result.isPresent());
        assertEquals(beacon, result.get());
    }

    /** Test findAdjacentBeacon returns empty when no adjacent beacons. */
    @Test
    void testFindAdjacentBeaconNoneFound() {
        when(block.getRelative(any(BlockFace.class))).thenReturn(org.mockito.Mockito.mock(Block.class));
        when(register.getBeaconAt(any(Point2D.class))).thenReturn(null);

        var result = listener.findAdjacentBeacon(block);

        assertFalse(result.isPresent());
    }

    /** Test findAdjacentBeacon handles null block gracefully. */
    @Test
    void testFindAdjacentBeaconNullBlock() {
        var result = listener.findAdjacentBeacon(null);

        assertFalse(result.isPresent());
    }

    /** Test findAdjacentBeacon returns first beacon found (NORTH priority). */
    @Test
    void testFindAdjacentBeaconPriority() {
        Block northBlock = org.mockito.Mockito.mock(Block.class);
        Block southBlock = org.mockito.Mockito.mock(Block.class);

        when(block.getRelative(BlockFace.NORTH)).thenReturn(northBlock);
        when(block.getRelative(BlockFace.SOUTH)).thenReturn(southBlock);

        when(northBlock.getX()).thenReturn(100);
        when(northBlock.getZ()).thenReturn(199);
        when(southBlock.getX()).thenReturn(100);
        when(southBlock.getZ()).thenReturn(201);

        // Both have beacons - NORTH should be returned first
        when(register.getBeaconAt(new Point2D.Double(100, 199))).thenReturn(beacon);
        when(register.getBeaconAt(new Point2D.Double(100, 201))).thenReturn(mock(com.wasteofplastic.beaconz.core.BeaconObj.class));

        var result = listener.findAdjacentBeacon(block);

        // Should return the NORTH beacon (first in search order)
        assertTrue(result.isPresent());
        assertEquals(beacon, result.get());
    }

    /** Test getBeaconAtLocation returns beacon when found. */
    @Test
    void testGetBeaconAtLocationFound() {
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(200);
        when(register.getBeaconAt(new Point2D.Double(100, 200))).thenReturn(beacon);

        var result = listener.getBeaconAtLocation(block);

        assertNotNull(result);
        assertEquals(beacon, result);
    }

    /** Test getBeaconAtLocation returns null when no beacon. */
    @Test
    void testGetBeaconAtLocationNotFound() {
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(200);
        when(register.getBeaconAt(any(Point2D.class))).thenReturn(null);

        var result = listener.getBeaconAtLocation(block);

        assertNull(result);
    }

    /** Test getBeaconAtLocation handles null block gracefully. */
    @Test
    void testGetBeaconAtLocationNullBlock() {
        var result = listener.getBeaconAtLocation(null);

        assertNull(result);
    }

    // ==================== Locking Block Position Tests ====================

    /** Test isBlockDirectlyAboveBeacon returns true when block is at beacon center. */
    @Test
    void testIsBlockDirectlyAboveBeaconAtCenter() {
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(200);
        when(beacon.getX()).thenReturn(100);
        when(beacon.getZ()).thenReturn(200);

        boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

        assertTrue(result, "Block at beacon center should be directly above");
    }

    /** Test isBlockDirectlyAboveBeacon returns true for all 8 positions around beacon center. */
    @Test
    void testIsBlockDirectlyAboveBeaconAllFootprintPositions() {
        when(beacon.getX()).thenReturn(100);
        when(beacon.getZ()).thenReturn(200);

        // Test all 9 positions in the 3x3 footprint (including center)
        int[][] footprintPositions = {
            {100, 200}, // center
            {99, 199},  // NW corner
            {100, 199}, // N edge
            {101, 199}, // NE corner
            {99, 200},  // W edge
            {101, 200}, // E edge
            {99, 201},  // SW corner
            {100, 201}, // S edge
            {101, 201}  // SE corner
        };

        for (int[] pos : footprintPositions) {
            when(block.getX()).thenReturn(pos[0]);
            when(block.getZ()).thenReturn(pos[1]);

            boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

            assertTrue(result, String.format("Block at (%d, %d) should be within footprint of beacon at (100, 200)",
                pos[0], pos[1]));
        }
    }

    /** Test isBlockDirectlyAboveBeacon returns false when block is 2 blocks away in X direction. */
    @Test
    void testIsBlockDirectlyAboveBeaconTooFarX() {
        when(block.getX()).thenReturn(102); // 2 blocks away
        when(block.getZ()).thenReturn(200);
        when(beacon.getX()).thenReturn(100);
        when(beacon.getZ()).thenReturn(200);

        boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

        assertFalse(result, "Block 2 blocks away in X should not be directly above");
    }

    /** Test isBlockDirectlyAboveBeacon returns false when block is 2 blocks away in Z direction. */
    @Test
    void testIsBlockDirectlyAboveBeaconTooFarZ() {
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(202); // 2 blocks away
        when(beacon.getX()).thenReturn(100);
        when(beacon.getZ()).thenReturn(200);

        boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

        assertFalse(result, "Block 2 blocks away in Z should not be directly above");
    }

    /** Test isBlockDirectlyAboveBeacon returns false when block is 2 blocks away diagonally. */
    @Test
    void testIsBlockDirectlyAboveBeaconTooFarDiagonal() {
        when(block.getX()).thenReturn(102); // 2 blocks away in X
        when(block.getZ()).thenReturn(202); // 2 blocks away in Z
        when(beacon.getX()).thenReturn(100);
        when(beacon.getZ()).thenReturn(200);

        boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

        assertFalse(result, "Block 2 blocks away diagonally should not be directly above");
    }

    /** Test isBlockDirectlyAboveBeacon handles negative coordinates correctly. */
    @Test
    void testIsBlockDirectlyAboveBeaconNegativeCoordinates() {
        when(block.getX()).thenReturn(-100);
        when(block.getZ()).thenReturn(-200);
        when(beacon.getX()).thenReturn(-100);
        when(beacon.getZ()).thenReturn(-200);

        boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

        assertTrue(result, "Should work with negative coordinates");
    }

    /** Test isBlockDirectlyAboveBeacon edge case: exactly at boundary (1 block offset). */
    @Test
    void testIsBlockDirectlyAboveBeaconAtBoundary() {
        when(beacon.getX()).thenReturn(100);
        when(beacon.getZ()).thenReturn(200);

        // Test all 4 cardinal edges at exactly 1 block offset
        int[][] edgePositions = {
            {100, 199}, // North edge (-1 Z)
            {100, 201}, // South edge (+1 Z)
            {99, 200},  // West edge (-1 X)
            {101, 200}  // East edge (+1 X)
        };

        for (int[] pos : edgePositions) {
            when(block.getX()).thenReturn(pos[0]);
            when(block.getZ()).thenReturn(pos[1]);

            boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

            assertTrue(result, String.format("Block at boundary (%d, %d) should be within footprint",
                pos[0], pos[1]));
        }
    }

    /** Test isBlockDirectlyAboveBeacon just outside boundary (2 blocks offset). */
    @Test
    void testIsBlockDirectlyAboveBeaconJustOutsideBoundary() {
        when(beacon.getX()).thenReturn(100);
        when(beacon.getZ()).thenReturn(200);

        // Test positions just outside the 3x3 footprint
        int[][] outsidePositions = {
            {100, 198}, // 2 blocks North
            {100, 202}, // 2 blocks South
            {98, 200},  // 2 blocks West
            {102, 200}, // 2 blocks East
            {98, 198},  // NW corner (2 blocks away)
            {102, 202}  // SE corner (2 blocks away)
        };

        for (int[] pos : outsidePositions) {
            when(block.getX()).thenReturn(pos[0]);
            when(block.getZ()).thenReturn(pos[1]);

            boolean result = listener.isBlockDirectlyAboveBeacon(block, beacon);

            assertFalse(result, String.format("Block at (%d, %d) should be outside footprint",
                pos[0], pos[1]));
        }
    }

    // ==================== Top-Down Removal Tests ====================

    /** Test isBlockAtHighestLevel returns true when block is at the highest level. */
    @Test
    void testIsBlockAtHighestLevelAtTop() {
        when(defenseBlock.getLevel()).thenReturn(5);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, 5);

        assertTrue(result, "Block at level 5 should be removable when highest is 5");
    }

    /** Test isBlockAtHighestLevel returns false when block is below highest level. */
    @Test
    void testIsBlockAtHighestLevelBelowTop() {
        when(defenseBlock.getLevel()).thenReturn(3);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, 5);

        assertFalse(result, "Block at level 3 should NOT be removable when highest is 5");
    }

    /** Test isBlockAtHighestLevel returns true when block is above highest level (edge case). */
    @Test
    void testIsBlockAtHighestLevelAboveTop() {
        when(defenseBlock.getLevel()).thenReturn(6);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, 5);

        assertTrue(result, "Block at level 6 should be removable when highest is 5 (shouldn't happen but handles gracefully)");
    }

    /** Test isBlockAtHighestLevel returns true at level 0. */
    @Test
    void testIsBlockAtHighestLevelZero() {
        when(defenseBlock.getLevel()).thenReturn(0);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, 0);

        assertTrue(result, "Block at level 0 should be removable when highest is 0");
    }

    /** Test isBlockAtHighestLevel returns false when block is much lower than highest. */
    @Test
    void testIsBlockAtHighestLevelMuchLower() {
        when(defenseBlock.getLevel()).thenReturn(1);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, 10);

        assertFalse(result, "Block at level 1 should NOT be removable when highest is 10");
    }

    /** Test isBlockAtHighestLevel returns false when block is one level below highest. */
    @Test
    void testIsBlockAtHighestLevelOneBelowTop() {
        when(defenseBlock.getLevel()).thenReturn(4);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, 5);

        assertFalse(result, "Block at level 4 should NOT be removable when highest is 5 (top-down enforcement)");
    }

    /** Test isBlockAtHighestLevel with negative levels (edge case). */
    @Test
    void testIsBlockAtHighestLevelNegative() {
        when(defenseBlock.getLevel()).thenReturn(-1);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, -1);

        assertTrue(result, "Block at level -1 should be removable when highest is -1 (handles edge case)");
    }

    /** Test top-down removal scenario: multiple blocks at same level. */
    @Test
    void testIsBlockAtHighestLevelMultipleBlocksAtTopLevel() {
        // Simulate scenario where multiple blocks are at the highest level (level 5)
        when(defenseBlock.getLevel()).thenReturn(5);

        boolean result = listener.isBlockAtHighestLevel(defenseBlock, 5);

        assertTrue(result, "When multiple blocks are at level 5 (highest), all should be removable");
    }

    /** Test top-down removal enforces strict ordering. */
    @Test
    void testTopDownRemovalStrictOrdering() {
        // Test that levels 1-4 cannot be removed when level 5 exists
        int highestLevel = 5;

        for (int level = 1; level < highestLevel; level++) {
            when(defenseBlock.getLevel()).thenReturn(level);

            boolean result = listener.isBlockAtHighestLevel(defenseBlock, highestLevel);

            assertFalse(result, String.format("Level %d should NOT be removable when highest is %d",
                level, highestLevel));
        }
    }

    /** Test top-down removal allows removal after top is destroyed. */
    @Test
    void testTopDownRemovalAfterTopDestroyed() {
        // After level 5 is destroyed, level 4 becomes the highest and can be removed
        when(defenseBlock.getLevel()).thenReturn(4);

        boolean resultBefore = listener.isBlockAtHighestLevel(defenseBlock, 5);
        boolean resultAfter = listener.isBlockAtHighestLevel(defenseBlock, 4);

        assertFalse(resultBefore, "Level 4 should NOT be removable when level 5 exists");
        assertTrue(resultAfter, "Level 4 should be removable when it becomes the highest");
    }

    // ==================== Defense Block Cleanup Tests ====================

    /** Test cleanupStaleDefenseBlocks removes AIR blocks. */
    @Test
    void testCleanupStaleDefenseBlocksRemovesAirBlocks() {
        HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();

        // Create AIR block that should be removed
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);
        when(airBlock.getX()).thenReturn(100);
        when(airBlock.getY()).thenReturn(65);
        when(airBlock.getZ()).thenReturn(200);

        DefenseBlock airDefense = mock(DefenseBlock.class);
        when(airDefense.getLevel()).thenReturn(3);

        defenseBlocks.put(airBlock, airDefense);
        when(beacon.getDefenseBlocks()).thenReturn(defenseBlocks);

        int removed = listener.cleanupStaleDefenseBlocks(beacon);

        assertEquals(1, removed, "Should remove 1 AIR block");
        assertTrue(defenseBlocks.isEmpty(), "Defense blocks map should be empty after cleanup");
    }

    /** Test cleanupStaleDefenseBlocks keeps solid blocks. */
    @Test
    void testCleanupStaleDefenseBlocksKeepsSolidBlocks() {
        HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();

        // Create solid block that should be kept
        Block solidBlock = mock(Block.class);
        when(solidBlock.getType()).thenReturn(Material.STONE);

        DefenseBlock solidDefense = mock(DefenseBlock.class);
        when(solidDefense.getLevel()).thenReturn(2);

        defenseBlocks.put(solidBlock, solidDefense);
        when(beacon.getDefenseBlocks()).thenReturn(defenseBlocks);

        int removed = listener.cleanupStaleDefenseBlocks(beacon);

        assertEquals(0, removed, "Should not remove solid blocks");
        assertEquals(1, defenseBlocks.size(), "Solid block should remain");
    }

    /** Test cleanupStaleDefenseBlocks handles mixed AIR and solid blocks. */
    @Test
    void testCleanupStaleDefenseBlocksMixedBlocks() {
        HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();

        // AIR block 1
        Block airBlock1 = mock(Block.class);
        when(airBlock1.getType()).thenReturn(Material.AIR);
        when(airBlock1.getX()).thenReturn(100);
        when(airBlock1.getY()).thenReturn(65);
        when(airBlock1.getZ()).thenReturn(200);
        defenseBlocks.put(airBlock1, mock(DefenseBlock.class));

        // Solid block
        Block solidBlock = mock(Block.class);
        when(solidBlock.getType()).thenReturn(Material.OBSIDIAN);
        defenseBlocks.put(solidBlock, mock(DefenseBlock.class));

        // AIR block 2
        Block airBlock2 = mock(Block.class);
        when(airBlock2.getType()).thenReturn(Material.AIR);
        when(airBlock2.getX()).thenReturn(101);
        when(airBlock2.getY()).thenReturn(66);
        when(airBlock2.getZ()).thenReturn(201);
        defenseBlocks.put(airBlock2, mock(DefenseBlock.class));

        when(beacon.getDefenseBlocks()).thenReturn(defenseBlocks);

        int removed = listener.cleanupStaleDefenseBlocks(beacon);

        assertEquals(2, removed, "Should remove 2 AIR blocks");
        assertEquals(1, defenseBlocks.size(), "Only solid block should remain");
        assertTrue(defenseBlocks.containsKey(solidBlock), "Solid block should still be in map");
    }

    /** Test cleanupStaleDefenseBlocks handles empty defense map. */
    @Test
    void testCleanupStaleDefenseBlocksEmptyMap() {
        HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(beacon.getDefenseBlocks()).thenReturn(defenseBlocks);

        int removed = listener.cleanupStaleDefenseBlocks(beacon);

        assertEquals(0, removed, "Should return 0 when map is empty");
    }

    /** Test cleanupStaleDefenseBlocks handles null beacon. */
    @Test
    void testCleanupStaleDefenseBlocksNullBeacon() {
        int removed = listener.cleanupStaleDefenseBlocks(null);

        assertEquals(0, removed, "Should handle null beacon gracefully");
    }

    /** Test cleanupStaleDefenseBlocks handles all AIR blocks. */
    @Test
    void testCleanupStaleDefenseBlocksAllAir() {
        HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();

        // Create 3 AIR blocks
        for (int i = 0; i < 3; i++) {
            Block airBlock = mock(Block.class);
            when(airBlock.getType()).thenReturn(Material.AIR);
            when(airBlock.getX()).thenReturn(100 + i);
            when(airBlock.getY()).thenReturn(65 + i);
            when(airBlock.getZ()).thenReturn(200 + i);
            defenseBlocks.put(airBlock, mock(DefenseBlock.class));
        }

        when(beacon.getDefenseBlocks()).thenReturn(defenseBlocks);

        int removed = listener.cleanupStaleDefenseBlocks(beacon);

        assertEquals(3, removed, "Should remove all 3 AIR blocks");
        assertTrue(defenseBlocks.isEmpty(), "Map should be empty after cleanup");
    }

    /** Test cleanupStaleDefenseBlocks handles different block types. */
    @Test
    void testCleanupStaleDefenseBlocksDifferentMaterials() {
        HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();

        // Add various block types
        Material[] materials = {Material.STONE, Material.OBSIDIAN, Material.DIRT, Material.COBBLESTONE};

        for (Material mat : materials) {
            Block solidBlock = mock(Block.class);
            when(solidBlock.getType()).thenReturn(mat);
            defenseBlocks.put(solidBlock, mock(DefenseBlock.class));
        }

        when(beacon.getDefenseBlocks()).thenReturn(defenseBlocks);

        int removed = listener.cleanupStaleDefenseBlocks(beacon);

        assertEquals(0, removed, "Should not remove any solid blocks");
        assertEquals(4, defenseBlocks.size(), "All solid blocks should remain");
    }

    /** Test cleanupStaleDefenseBlocks tracks correct levels being removed. */
    @Test
    void testCleanupStaleDefenseBlocksTracksLevels() {
        HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();

        // Create AIR block at high level
        Block airBlock = mock(Block.class);
        when(airBlock.getType()).thenReturn(Material.AIR);
        when(airBlock.getX()).thenReturn(100);
        when(airBlock.getY()).thenReturn(70);
        when(airBlock.getZ()).thenReturn(200);

        DefenseBlock highLevelDefense = mock(DefenseBlock.class);
        when(highLevelDefense.getLevel()).thenReturn(10);

        defenseBlocks.put(airBlock, highLevelDefense);
        when(beacon.getDefenseBlocks()).thenReturn(defenseBlocks);
        when(beacon.getName()).thenReturn("100, 200");

        int removed = listener.cleanupStaleDefenseBlocks(beacon);

        assertEquals(1, removed, "Should remove the high-level AIR block");
        assertTrue(defenseBlocks.isEmpty(), "Defense blocks map should be empty after cleanup");
    }
}
