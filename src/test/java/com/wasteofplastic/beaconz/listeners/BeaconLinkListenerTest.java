package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.wasteofplastic.beaconz.config.Settings;

import net.kyori.adventure.text.Component;

/**
 * Tests for BeaconLinkListener covering map-based beacon linking and experience management.
 * Extends BeaconzListenerTestBase for common test infrastructure.
 *
 * <p>Note: Some tests may skip branches that trigger Paper's Sound/MapView registry to avoid MockBukkit compatibility issues.
 */
class BeaconLinkListenerTest extends CommonTestBase {

    private BeaconLinkListener listener;

    /** Plain constant avoids calling world.getMaxHeight() inside doReturn() which confuses Mockito. */
    private static final int WORLD_MAX_HEIGHT = 320;

    @Mock
    private ItemStack mapItem;
    @Mock
    private Block clickedBlock;

    @Override
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        when(world.getMaxHeight()).thenReturn(WORLD_MAX_HEIGHT);

        listener = new BeaconLinkListener(plugin);

        // Setup Settings defaults for linking tests
        Settings.expDistance = 100.0; // 100 blocks per exp point
        Settings.linkLimit = 1000;
        Settings.maxLinks = 3;
    }

    /** Sanity check: listener construction. */
    @Test
    void testBeaconLinkListener() {
        assertNotNull(listener);
    }

    // ==================== onPaperMapUse Tests ====================

    /** Event without item: should return early. */
    @Test
    void testOnPaperMapUseNoItem() {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        // No interactions expected
        verify(register, never()).getBeacon(any());
    }

    /** Item is not a filled map: should return early. */
    @Test
    void testOnPaperMapUseNotFilledMap() {
        when(item.getType()).thenReturn(Material.PAPER);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        verify(register, never()).getBeacon(any());
    }

    /** Wrong action (not right-click block): return early. */
    @Test
    void testOnPaperMapUseWrongAction() {
        when(mapItem.getType()).thenReturn(Material.FILLED_MAP);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, mapItem, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        verify(register, never()).getBeacon(any());
    }

    /** Wrong world: return early. */
    @Test
    void testOnPaperMapUseWrongWorld() {
        when(mapItem.getType()).thenReturn(Material.FILLED_MAP);
        when(clickedBlock.getWorld()).thenReturn(mock(org.bukkit.World.class)); // different world
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, mapItem, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        verify(register, never()).getBeacon(any());
    }

    /** Player in lobby: return early. */
    @Test
    void testOnPaperMapUsePlayerInLobby() {
        when(mapItem.getType()).thenReturn(Material.FILLED_MAP);
        when(clickedBlock.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, mapItem, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        verify(register, never()).getBeacon(any());
    }

    /** Player has no team (non-op): event cancelled. */
    @Test
    void testOnPaperMapUseNoTeamNonOp() {
        when(mapItem.getType()).thenReturn(Material.FILLED_MAP);
        when(clickedBlock.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(false);
        when(inventory.getItemInMainHand()).thenReturn(mapItem);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, mapItem, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        assertSame(org.bukkit.event.Event.Result.DENY, event.useInteractedBlock());
    }

    /** Clicked block is not a beacon: return early. */
    @Test
    void testOnPaperMapUseNotBeacon() {
        when(mapItem.getType()).thenReturn(Material.FILLED_MAP);
        when(clickedBlock.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(register.getBeacon(clickedBlock)).thenReturn(null);
        when(inventory.getItemInMainHand()).thenReturn(mapItem);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, mapItem, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        // Beacon lookup happened but no further processing
        verify(register).getBeacon(clickedBlock);
    }

    /** Beacon not owned by player's team: send message and cancel. */
    @Test
    void testOnPaperMapUseBeaconNotOwned() {
        when(mapItem.getType()).thenReturn(Material.FILLED_MAP);
        when(clickedBlock.getWorld()).thenReturn(world);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(register.getBeacon(clickedBlock)).thenReturn(beacon);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        when(inventory.getItemInMainHand()).thenReturn(mapItem);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, mapItem, clickedBlock, BlockFace.UP, EquipmentSlot.HAND);

        listener.onPaperMapUse(event);

        verify(player).sendMessage(any(Component.class));
        assertSame(org.bukkit.event.Event.Result.DENY, event.useInteractedBlock());
    }

    // ==================== onMapHold Tests ====================

    /** Holding null item: return early. */
    @Test
    void testOnMapHoldNullItem() {
        when(inventory.getItem(0)).thenReturn(null);
        PlayerItemHeldEvent event = new PlayerItemHeldEvent(player, 0, 0);

        listener.onMapHold(event);

        // No exceptions, just early return
    }

    /** Holding non-map item: return early. */
    @Test
    void testOnMapHoldNotMap() {
        when(inventory.getItem(0)).thenReturn(item);
        when(item.getType()).thenReturn(Material.COMPASS);
        PlayerItemHeldEvent event = new PlayerItemHeldEvent(player, 0, 0);

        listener.onMapHold(event);

        // Early return, no map processing
    }

    /** Player in different world: return early. */
    @Test
    void testOnMapHoldDifferentWorld() {
        when(inventory.getItem(0)).thenReturn(mapItem);
        when(mapItem.getType()).thenReturn(Material.FILLED_MAP);
        when(player.getWorld()).thenReturn(mock(org.bukkit.World.class));
        PlayerItemHeldEvent event = new PlayerItemHeldEvent(player, 0, 0);

        listener.onMapHold(event);

        // World check blocks processing
    }

    // ==================== Experience Management Tests ====================

    /**
     * testForExp returns true when player lacks XP.
     * Note: The method name suggests "test if player has exp", but implementation returns true if INSUFFICIENT.
     * This is potentially confusing/incorrect - consider renaming to "lacksExp" or inverting the logic.
     */
    @Test
    void testTestForExpInsufficientXP() {
        when(player.getLevel()).thenReturn(0);
        when(player.getExp()).thenReturn(0f);
        when(player.calculateTotalExperiencePoints()).thenReturn(0);

        boolean result = BeaconLinkListener.testForExp(player, 100);

        assertTrue(result, "testForExp should return true when player lacks XP (current implementation)");
    }

    /**
     * testForExp returns false when player has enough XP.
     * Note: This is counterintuitive - the name suggests testing FOR experience, not lack thereof.
     */
    @Test
    void testTestForExpSufficientXP() {
        // Level 30 player has significant XP
        when(player.getLevel()).thenReturn(30);
        when(player.getExp()).thenReturn(0.5f);
        // Level 30 has accumulated ~1395 XP (sum of tier 1 + tier 2 formula)
        // We need to mock this to return more than 50 XP required for the test
        when(player.calculateTotalExperiencePoints()).thenReturn(1395);

        boolean result = BeaconLinkListener.testForExp(player, 50);

        assertFalse(result, "testForExp should return false when player has enough XP");
    }

    /** setTotalExperience with negative value throws exception. */
    @Test
    void testSetTotalExperienceNegative() {
        assertThrows(IllegalArgumentException.class, () -> BeaconLinkListener.setTotalExperience(player, -10));
    }

    /** setTotalExperience resets and rebuilds XP/level. */
    @Test
    void testSetTotalExperienceZero() {
        BeaconLinkListener.setTotalExperience(player, 0);

        verify(player).setExp(0);
        verify(player).setLevel(0);
        verify(player).setTotalExperience(0);
    }

    /** setTotalExperience gives XP incrementally. */
    @Test
    void testSetTotalExperiencePositive() {
        BeaconLinkListener.setTotalExperience(player, 100);

        verify(player).setExp(0);
        verify(player).setLevel(0);
        verify(player).setTotalExperience(0);
        verify(player, atLeastOnce()).giveExp(anyInt());
    }

    /** removeExp deducts XP if player has enough. */
    @Test
    void testRemoveExpSufficient() {
        when(player.getLevel()).thenReturn(10);
        when(player.getExp()).thenReturn(0.5f);
        when(player.calculateTotalExperiencePoints()).thenReturn(176); // Level 10 with 50% progress

        BeaconLinkListener.removeExp(player, 50);

        // Should call setTotalExperience which resets and then gives XP
        verify(player).setLevel(0);
        verify(player).setExp(0);  // setExp with int 0 (not float)
        verify(player).setTotalExperience(0);
        verify(player).giveExp(126); // 176 - 50 = 126
    }

    /** removeExp does nothing if player lacks XP. */
    @Test
    void testRemoveExpInsufficient() {
        when(player.getLevel()).thenReturn(0);
        when(player.getExp()).thenReturn(0f);
        when(player.calculateTotalExperiencePoints()).thenReturn(0);

        BeaconLinkListener.removeExp(player, 100);

        // No XP to remove, so no setTotalExperience call
        verify(player, never()).setExp(anyInt());
    }

    // ==================== getReqExp Tests ====================

    /** getReqExp calculates XP based on distance and Settings.expDistance. */
    @Test
    void testGetReqExpBasic() {
        when(beacon.getPoint()).thenReturn(new Point2D.Double(0, 0));
        when(otherBeacon.getPoint()).thenReturn(new Point2D.Double(300, 400)); // distance = 500

        Settings.expDistance = 100.0; // 100 blocks per XP

        int required = listener.getReqExp(beacon, otherBeacon);

        assertEquals(5, required, "500 blocks / 100 blocks per XP = 5 XP");
    }

    /**
     * getReqExp with zero expDistance setting.
     * Note: Division by zero with doubles returns Infinity, not an exception.
     * Production code should validate Settings.expDistance > 0.
     */
    @Test
    void testGetReqExpZeroDistance() {
        when(beacon.getPoint()).thenReturn(new Point2D.Double(0, 0));
        when(otherBeacon.getPoint()).thenReturn(new Point2D.Double(100, 0));

        Settings.expDistance = 0.0; // Division by zero scenario

        // Division by zero with doubles returns Infinity, cast to int becomes Integer.MAX_VALUE
        int required = listener.getReqExp(beacon, otherBeacon);

        assertTrue(required == Integer.MAX_VALUE || required == Integer.MIN_VALUE,
                   "Division by zero should produce extreme integer value (consider adding validation in production)");
    }

    /** getReqExp with adjacent beacons. */
    @Test
    void testGetReqExpAdjacentBeacons() {
        when(beacon.getPoint()).thenReturn(new Point2D.Double(0, 0));
        when(otherBeacon.getPoint()).thenReturn(new Point2D.Double(10, 0));

        Settings.expDistance = 100.0;

        int required = listener.getReqExp(beacon, otherBeacon);

        assertEquals(0, required, "10 blocks / 100 = 0 XP (rounds down)");
    }

    // ==================== Block Place/Break Event Tests ====================

    /**
     * Tests that creative mode players can place blocks at world top.
     */
    @Test
    void testOnBlockPlaceCreativeModeAllowed() {
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.CREATIVE).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "Creative mode players should be able to place blocks at world top");
    }

    /**
     * Tests that OP players can place blocks at world top regardless of game mode.
     */
    @Test
    void testOnBlockPlaceOpAllowed() {
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(true).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "OP players should be able to place blocks at world top");
    }

    /**
     * Tests that survival mode non-OP players cannot place blocks at world top in Beaconz world.
     */
    @Test
    void testOnBlockPlaceSurvivalAtTopCancelled() {
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "Non-OP survival players should not be able to place blocks at world top");
    }

    /**
     * Tests that blocks can be placed at maxHeight - 2 (just below the restricted zone).
     */
    @Test
    void testOnBlockPlaceBelowTopAllowed() {
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 3).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "Players should be able to place blocks below the restricted top zone");
    }

    /**
     * Tests that blocks can be placed in different worlds without restriction.
     */
    @Test
    void testOnBlockPlaceDifferentWorldAllowed() {
        org.bukkit.World otherWorld = mock(org.bukkit.World.class);
        doReturn(256).when(otherWorld).getMaxHeight();
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(otherWorld).when(clickedBlock).getWorld();
        doReturn(255).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "Blocks can be placed at any height in non-Beaconz worlds");
    }

    /**
     * Tests that adventure mode players are also restricted from placing blocks at world top.
     */
    @Test
    void testOnBlockPlaceAdventureModeAtTopCancelled() {
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.ADVENTURE).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "Non-OP adventure players should not be able to place blocks at world top");
    }

    /**
     * Tests that spectator mode players can't place blocks at world top (though they can't place blocks anyway).
     */
    @Test
    void testOnBlockPlaceSpectatorModeAtTopCancelled() {
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SPECTATOR).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "Non-OP spectator players should not be able to place blocks at world top");
    }

    /**
     * Tests that creative mode players can break blocks at world top.
     */
    @Test
    void testOnBlockBreakCreativeModeAllowed() {
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.CREATIVE).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(clickedBlock, player);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Creative mode players should be able to break blocks at world top");
    }

    /**
     * Tests that OP players can break blocks at world top regardless of game mode.
     */
    @Test
    void testOnBlockBreakOpAllowed() {
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(true).when(player).isOp();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(clickedBlock, player);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "OP players should be able to break blocks at world top");
    }

    /**
     * Tests that survival mode non-OP players cannot break blocks at world top in Beaconz world.
     */
    @Test
    void testOnBlockBreakSurvivalAtTopCancelled() {
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(clickedBlock, player);

        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Non-OP survival players should not be able to break blocks at world top");
    }

    /**
     * Tests that blocks can be broken at maxHeight - 3 (just below the restricted zone).
     */
    @Test
    void testOnBlockBreakBelowTopAllowed() {
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 3).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(clickedBlock, player);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Players should be able to break blocks below the restricted top zone");
    }

    /**
     * Tests that blocks can be broken in different worlds without restriction.
     */
    @Test
    void testOnBlockBreakDifferentWorldAllowed() {
        org.bukkit.World otherWorld = mock(org.bukkit.World.class);
        doReturn(256).when(otherWorld).getMaxHeight();
        doReturn(otherWorld).when(clickedBlock).getWorld();
        doReturn(255).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(clickedBlock, player);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Blocks can be broken at any height in non-Beaconz worlds");
    }

    /**
     * Tests that adventure mode players are also restricted from breaking blocks at world top.
     */
    @Test
    void testOnBlockBreakAdventureModeAtTopCancelled() {
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 1).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.ADVENTURE).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(clickedBlock, player);

        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "Non-OP adventure players should not be able to break blocks at world top");
    }

    /**
     * Tests the edge case where block is at exactly maxHeight - 2 (the boundary).
     * According to the code logic (y > maxHeight - 2), this should be allowed.
     */
    @Test
    void testOnBlockPlaceAtBoundaryAllowed() {
        doReturn(mock(org.bukkit.block.BlockState.class)).when(clickedBlock).getState();
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 2).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(
            clickedBlock, clickedBlock.getState(), clickedBlock, item, player, true, EquipmentSlot.HAND
        );

        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "Blocks at maxHeight - 2 should be allowed (boundary test)");
    }

    /**
     * Tests the edge case where block is at exactly maxHeight - 2 for breaking.
     */
    @Test
    void testOnBlockBreakAtBoundaryAllowed() {
        doReturn(world).when(clickedBlock).getWorld();
        doReturn(WORLD_MAX_HEIGHT - 2).when(clickedBlock).getY();
        doReturn(org.bukkit.GameMode.SURVIVAL).when(player).getGameMode();
        doReturn(false).when(player).isOp();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(clickedBlock, player);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled(), "Blocks at maxHeight - 2 should be allowed to break (boundary test)");
    }

    // ==================== Additional Notes ====================

    /**
     * IMPROVEMENTS & OBSERVATIONS:
     *
     * 1. testForExp naming: The method returns true when player LACKS XP, which is opposite of what the name suggests.
     *    Recommend renaming to "lacksExp" or inverting logic to "hasEnoughExp".
     *
     * 2. getReqExp division by zero: Settings.expDistance=0 causes infinity/exception. Add validation or guard clause.
     *
     * 3. Sound/MapView registry: Some branches (successful linking with sounds/map rendering) trigger Paper registry
     *    initialization that fails under MockBukkit. These paths are untested but documented.
     *
     * 4. Lang string dependencies: Tests rely on BeaconzListenerTestBase initializing Lang statics. If Lang becomes
     *    injectable/configurable, update base class accordingly.
     *
     * 5. Experience calculations: The getTotalExperience/setTotalExperience logic is complex (borrowed from Essentials).
     *    Consider extracting to a testable utility class for better unit test coverage of edge cases.
     *
     * 6. Missing coverage: linkBeacons private method has significant logic (max links, self-link, crossing checks)
     *    that's untested here. Consider making it package-private or testing via integration tests.
     *
     * 7. Method naming bug: The second onBlockPlace method in BeaconLinkListener should be named onBlockBreak.
     *    Both methods have identical logic but handle different events (BlockPlaceEvent vs BlockBreakEvent).
     *    This is a code smell and should be refactored.
     *
     * 8. Block place/break tests: Comprehensive coverage added for:
     *    - Creative/OP bypass logic
     *    - Survival/Adventure mode restrictions at world top (maxHeight - 1)
     *    - Different game modes (Creative, Survival, Adventure, Spectator)
     *    - World boundary checks (Beaconz world vs other worlds)
     *    - Boundary conditions (exactly at maxHeight - 2)
     */
}
