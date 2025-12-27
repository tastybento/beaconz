package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
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

import com.wasteofplastic.beaconz.DefenseBlock;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Region;
import com.wasteofplastic.beaconz.Settings;

/**
 * Comprehensive tests for BeaconPassiveDefenseListener covering defense placement, breaking, and protection mechanics.
 * Extends BeaconzListenerTestBase for shared test infrastructure.
 *
 * <p>Tests cover: explosion protection, piston protection, block placement validation,
 * defense block breaking rules, damage handling, and liquid flow prevention.
 */
class BeaconPassiveDefenseListenerTest extends BeaconzListenerTestBase {

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
        Lang.errorYouMustBeInAGame = "errorYouMustBeInAGame";
        Lang.errorYouMustBeInATeam = "errorYouMustBeInATeam";
        Lang.beaconYouCanOnlyExtend = "beaconYouCanOnlyExtend";
        Lang.beaconCannotBeExtended = "beaconCannotBeExtended";
        Lang.errorClearAboveBeacon = "errorClearAboveBeacon";
        Lang.beaconExtended = "beaconExtended";
        Lang.beaconLockedJustNow = "beaconLockedJustNow [lockingBlock]";
        Lang.beaconLockedAlready = "beaconLockedAlready [lockingBlock]";
        Lang.beaconLockedWithNMoreBlocks = "beaconLockedWithNMoreBlocks [number]";
        Lang.errorCanOnlyPlaceBlocks = "errorCanOnlyPlaceBlocks";
        Lang.errorCanOnlyPlaceBlocksUpTo = "errorCanOnlyPlaceBlocksUpTo [value]";
        Lang.errorYouNeedToBeLevel = "errorYouNeedToBeLevel [value]";
        Lang.generalLevel = "Level";
        Lang.beaconLinkBlockPlaced = "beaconLinkBlockPlaced [range]";
        Lang.beaconDefensePlaced = "beaconDefensePlaced";
        Lang.beaconLinkBlockBroken = "beaconLinkBlockBroken [range]";
        Lang.beaconLinkLost = "beaconLinkLost";
        Lang.beaconDefenseRemoveTopDown = "beaconDefenseRemoveTopDown";
        Lang.errorYouCannotRemoveOtherPlayersBlocks = "errorYouCannotRemoveOtherPlayersBlocks";
        Lang.beaconLocked = "beaconLocked";
        Lang.beaconAmplifierBlocksCannotBeRecovered = "beaconAmplifierBlocksCannotBeRecovered";

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
        Lang.defenseText.put(Material.STONE, "Stone defense block");
        Lang.defenseText.put(Material.GLASS, "Glass defense block");
        Lang.defenseText.put(Material.OBSIDIAN, "Obsidian defense block");

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
        verify(player).sendMessage(anyString());
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
        verify(player).sendMessage(ChatColor.RED + Lang.errorYouMustBeInATeam);
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
        verify(player).sendMessage(ChatColor.GREEN + Lang.beaconExtended);
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
        verify(player).sendMessage(ChatColor.RED + Lang.beaconYouCanOnlyExtend);
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
        verify(player).sendMessage(contains("§cerrorYouNeedToBeLevel 5"));
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
        verify(player).sendMessage(ChatColor.RED + Lang.errorCanOnlyPlaceBlocks);
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

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(ChatColor.RED + Lang.errorYouCannotRemoveOtherPlayersBlocks);
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

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(ChatColor.YELLOW + Lang.beaconLocked);
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

        Map<Block, DefenseBlock> defenseBlocks = new HashMap<>();
        when(defenseBlock.getLevel()).thenReturn(10);
        defenseBlocks.put(block, defenseBlock);
        doReturn(defenseBlocks).when(beacon).getDefenseBlocks();

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(contains("§cerrorYouNeedToBeLevel 10"));
    }

    /** Break enemy defense block not from top: cancelled. */
    @Test
    void testOnBeaconBreakEnemyBlockNotTopDown() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(66);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
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

        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBeaconBreak(event);

        assertTrue(event.isCancelled());
        verify(player).sendMessage(ChatColor.RED + Lang.beaconDefenseRemoveTopDown);
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
        verify(player).sendMessage(ChatColor.RED + Lang.beaconDefenseRemoveTopDown);
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

    /**
     * OBSERVATIONS & IMPROVEMENTS:
     *
     * 1. Complex adjacency logic: onBlockPlace checks all 4 directions for adjacent beacons.
     *    Consider extracting to a helper method for testability and readability.
     *
     * 2. Settings dependency: Tests heavily rely on Settings static fields. Consider making
     *    Settings injectable or using a configuration object pattern.
     *
     * 3. Emerald locking logic: The locking block check has complex coordinate math that's
     *    hard to test and verify. Consider refactoring for clarity.
     *
     * 4. Top-down removal: The logic that ensures blocks are removed from highest to lowest
     *    involves iteration and comparison. Could benefit from extracting to testable method.
     *
     * 5. Sound effects: Some tests would trigger Sound.BLOCK_GLASS_BREAK which requires Paper
     *    registry. These branches are documented but not fully tested.
     *
     * 6. Defense block cleanup: onDefenseDamage removes AIR blocks from defense map during
     *    iteration - this cleanup logic could be in a separate maintenance method.
     *
     * 7. Missing test coverage: Link block removal with Settings.removeLongestLink enabled
     *    is not fully tested due to complexity of mocking beacon links.
     */
}
