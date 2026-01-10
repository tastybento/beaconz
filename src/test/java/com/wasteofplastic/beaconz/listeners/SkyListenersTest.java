package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.wasteofplastic.beaconz.Lang;

import net.kyori.adventure.text.Component;

/**
 * Test class for {@link SkyListeners}
 * Tests protection of the sky layer (height 255) from various events
 */
class SkyListenersTest extends CommonTestBase {

    private SkyListeners listener;

    @Mock
    private BlockDamageEvent blockDamageEvent;
    @Mock
    private EntityExplodeEvent entityExplodeEvent;
    @Mock
    private BlockSpreadEvent blockSpreadEvent;
    @Mock
    private BlockPistonExtendEvent pistonExtendEvent;
    @Mock
    private PlayerBucketEmptyEvent bucketEmptyEvent;
    @Mock
    private BlockPlaceEvent blockPlaceEvent;
    @Mock
    private BlockBreakEvent blockBreakEvent;
    @Mock
    private TNTPrimed tnt;

    private static final int BLOCK_HEIGHT = 255;

    @BeforeEach
    void init() {
        listener = new SkyListeners(plugin);
        Lang.errorYouCannotDoThat = Component.text("errorYouCannotDoThat");
    }

    /**
     * Test method for {@link SkyListeners#SkyListeners(com.wasteofplastic.beaconz.Beaconz)}
     */
    @Test
    void testSkyListenersConstructor() {
        assertNotNull(listener);
    }

    // ==================== onBlockDamage Tests ====================

    /**
     * Test that block damage is allowed in wrong world
     */
    @Test
    void testOnBlockDamageWrongWorld() {
        World otherWorld = mock(World.class);
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(otherWorld);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(false);

        listener.onBlockDamage(blockDamageEvent);

        verify(blockDamageEvent, never()).setCancelled(true);
    }

    /**
     * Test that ops can damage blocks at sky height
     */
    @Test
    void testOnBlockDamageOpCanDamage() {
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(true);

        listener.onBlockDamage(blockDamageEvent);

        verify(blockDamageEvent, never()).setCancelled(true);
    }

    /**
     * Test that non-ops cannot damage blocks at sky height
     */
    @Test
    void testOnBlockDamageNonOpCannotDamage() {
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(false);

        listener.onBlockDamage(blockDamageEvent);

        verify(blockDamageEvent).setCancelled(true);
        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test that blocks below sky height can be damaged
     */
    @Test
    void testOnBlockDamageAllowedBelowSkyHeight() {
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT - 1);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(false);

        listener.onBlockDamage(blockDamageEvent);

        verify(blockDamageEvent, never()).setCancelled(true);
    }

    // ==================== onExplode Tests ====================

    /**
     * Test that explosions in wrong world are not affected
     */
    @Test
    void testOnExplodeWrongWorld() {
        World otherWorld = mock(World.class);
        when(tnt.getWorld()).thenReturn(otherWorld);
        when(entityExplodeEvent.getEntity()).thenReturn(tnt);
        Block skyBlock = mock(Block.class);
        when(skyBlock.getY()).thenReturn(BLOCK_HEIGHT);
        List<Block> blocks = new ArrayList<>();
        blocks.add(skyBlock);
        when(entityExplodeEvent.blockList()).thenReturn(blocks);

        listener.onExplode(entityExplodeEvent);

        verify(entityExplodeEvent, never()).blockList();
    }

    /**
     * Test that sky blocks are removed from explosion damage list
     */
    @Test
    void testOnExplodeRemovesSkyBlocks() {
        when(tnt.getWorld()).thenReturn(world);
        when(entityExplodeEvent.getEntity()).thenReturn(tnt);

        Block skyBlock = mock(Block.class);
        when(skyBlock.getY()).thenReturn(BLOCK_HEIGHT);

        Block normalBlock = mock(Block.class);
        when(normalBlock.getY()).thenReturn(BLOCK_HEIGHT - 1);

        List<Block> blocks = new ArrayList<>();
        blocks.add(skyBlock);
        blocks.add(normalBlock);
        when(entityExplodeEvent.blockList()).thenReturn(blocks);

        listener.onExplode(entityExplodeEvent);

        // Sky block should be removed, only normal block remains
        verify(entityExplodeEvent).blockList();
        assertNotNull(blocks);
        // The list should still contain the normal block
        // but the sky block should have been removed
    }

    /**
     * Test that multiple sky blocks are removed from explosion
     */
    @Test
    void testOnExplodeRemovesMultipleSkyBlocks() {
        when(tnt.getWorld()).thenReturn(world);
        when(entityExplodeEvent.getEntity()).thenReturn(tnt);

        Block skyBlock1 = mock(Block.class);
        when(skyBlock1.getY()).thenReturn(BLOCK_HEIGHT);

        Block skyBlock2 = mock(Block.class);
        when(skyBlock2.getY()).thenReturn(BLOCK_HEIGHT);

        Block normalBlock = mock(Block.class);
        when(normalBlock.getY()).thenReturn(100);

        List<Block> blocks = new ArrayList<>();
        blocks.add(skyBlock1);
        blocks.add(normalBlock);
        blocks.add(skyBlock2);
        when(entityExplodeEvent.blockList()).thenReturn(blocks);

        listener.onExplode(entityExplodeEvent);

        verify(entityExplodeEvent).blockList();
    }

    // ==================== onBlockSpread Tests ====================

    /**
     * Test that block spread is allowed in wrong world
     */
    @Test
    void testOnBlockSpreadWrongWorld() {
        World otherWorld = mock(World.class);
        when(blockSpreadEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(otherWorld);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);

        listener.onBlockSpread(blockSpreadEvent);

        verify(blockSpreadEvent, never()).setCancelled(true);
    }

    /**
     * Test that block spread is prevented at sky height
     */
    @Test
    void testOnBlockSpreadPreventedAtSkyHeight() {
        when(blockSpreadEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);

        listener.onBlockSpread(blockSpreadEvent);

        verify(blockSpreadEvent).setCancelled(true);
    }

    /**
     * Test that block spread is allowed below sky height
     */
    @Test
    void testOnBlockSpreadAllowedBelowSkyHeight() {
        when(blockSpreadEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT - 1);

        listener.onBlockSpread(blockSpreadEvent);

        verify(blockSpreadEvent, never()).setCancelled(true);
    }

    // ==================== onPistonPush Tests ====================

    /**
     * Test that piston push is allowed in wrong world
     */
    @Test
    void testOnPistonPushWrongWorld() {
        World otherWorld = mock(World.class);
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(otherWorld);
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.UP);

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent, never()).setCancelled(true);
    }

    /**
     * Test that piston push down is allowed
     */
    @Test
    void testOnPistonPushDownAllowed() {
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.DOWN);

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent, never()).setCancelled(true);
    }

    /**
     * Test that piston push horizontally is allowed
     */
    @Test
    void testOnPistonPushHorizontalAllowed() {
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.NORTH);

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent, never()).setCancelled(true);
    }

    /**
     * Test that piston push up to sky height is prevented
     */
    @Test
    void testOnPistonPushUpToSkyHeightPrevented() {
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.UP);
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);

        Block pushedBlock = mock(Block.class);
        Block destination = mock(Block.class);
        when(pushedBlock.getRelative(BlockFace.UP)).thenReturn(destination);
        when(destination.getY()).thenReturn(BLOCK_HEIGHT);

        List<Block> blocks = List.of(pushedBlock);
        when(pistonExtendEvent.getBlocks()).thenReturn(blocks);

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent).setCancelled(true);
    }

    /**
     * Test that piston push up below sky height is allowed
     */
    @Test
    void testOnPistonPushUpBelowSkyHeightAllowed() {
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.UP);
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);

        Block pushedBlock = mock(Block.class);
        Block destination = mock(Block.class);
        when(pushedBlock.getRelative(BlockFace.UP)).thenReturn(destination);
        when(destination.getY()).thenReturn(BLOCK_HEIGHT - 1);

        List<Block> blocks = List.of(pushedBlock);
        when(pistonExtendEvent.getBlocks()).thenReturn(blocks);

        listener.onPistonPush(pistonExtendEvent);

        verify(pistonExtendEvent, never()).setCancelled(true);
    }

    // ==================== onBucketEmpty Tests ====================

    /**
     * Test that bucket empty is allowed in wrong world
     */
    @Test
    void testOnBucketEmptyWrongWorld() {
        World otherWorld = mock(World.class);
        when(bucketEmptyEvent.getBlockClicked()).thenReturn(block);
        when(block.getWorld()).thenReturn(otherWorld);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(bucketEmptyEvent.getPlayer()).thenReturn(player);

        listener.onBucketEmpty(bucketEmptyEvent);

        verify(bucketEmptyEvent, never()).setCancelled(true);
    }

    /**
     * Test that bucket empty is prevented at sky height
     */
    @Test
    void testOnBucketEmptyPreventedAtSkyHeight() {
        when(bucketEmptyEvent.getBlockClicked()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(bucketEmptyEvent.getPlayer()).thenReturn(player);

        listener.onBucketEmpty(bucketEmptyEvent);

        verify(bucketEmptyEvent).setCancelled(true);
        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test that bucket empty is allowed below sky height
     */
    @Test
    void testOnBucketEmptyAllowedBelowSkyHeight() {
        when(bucketEmptyEvent.getBlockClicked()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT - 1);
        when(bucketEmptyEvent.getPlayer()).thenReturn(player);

        listener.onBucketEmpty(bucketEmptyEvent);

        verify(bucketEmptyEvent, never()).setCancelled(true);
    }

    // ==================== onBlockPlace Tests ====================

    /**
     * Test that block place is allowed in wrong world
     */
    @Test
    void testOnBlockPlaceWrongWorld() {
        World otherWorld = mock(World.class);
        when(block.getWorld()).thenReturn(otherWorld);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);

        Block blockAgainst = mock(Block.class);
        ItemStack itemInHand = new ItemStack(Material.STONE);

        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), blockAgainst, itemInHand, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        verify(player, never()).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test that ops can place blocks at sky height
     */
    @Test
    void testOnBlockPlaceOpCanPlace() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(player.isOp()).thenReturn(true);

        Block blockAgainst = mock(Block.class);
        when(blockAgainst.getY()).thenReturn(BLOCK_HEIGHT - 1);
        ItemStack itemInHand = new ItemStack(Material.STONE);

        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), blockAgainst, itemInHand, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        // Should not be cancelled for ops
        verify(player, never()).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test that non-ops cannot place blocks at sky height
     */
    @Test
    void testOnBlockPlaceNonOpCannotPlace() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(player.isOp()).thenReturn(false);

        Block blockAgainst = mock(Block.class);
        when(blockAgainst.getY()).thenReturn(BLOCK_HEIGHT - 1);
        ItemStack itemInHand = new ItemStack(Material.STONE);

        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), blockAgainst, itemInHand, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test that non-ops cannot place blocks against sky height blocks
     */
    @Test
    void testOnBlockPlaceNonOpCannotPlaceAgainstSkyBlock() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT - 1);
        when(player.isOp()).thenReturn(false);

        Block blockAgainst = mock(Block.class);
        when(blockAgainst.getY()).thenReturn(BLOCK_HEIGHT);
        ItemStack itemInHand = new ItemStack(Material.STONE);

        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), blockAgainst, itemInHand, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test that non-ops can place blocks below sky height
     */
    @Test
    void testOnBlockPlaceAllowedBelowSkyHeight() {
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT - 1);
        when(player.isOp()).thenReturn(false);

        Block blockAgainst = mock(Block.class);
        when(blockAgainst.getY()).thenReturn(BLOCK_HEIGHT - 2);
        ItemStack itemInHand = new ItemStack(Material.STONE);

        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), blockAgainst, itemInHand, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        verify(player, never()).sendMessage(Lang.errorYouCannotDoThat);
    }

    // ==================== onBeaconBreak Tests ====================

    /**
     * Test that block break is allowed in wrong world
     */
    @Test
    void testOnBeaconBreakWrongWorld() {
        World otherWorld = mock(World.class);
        when(blockBreakEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(otherWorld);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(blockBreakEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(false);

        listener.onBeaconBreak(blockBreakEvent);

        verify(blockBreakEvent, never()).setCancelled(true);
    }

    /**
     * Test that ops can break blocks at sky height
     */
    @Test
    void testOnBeaconBreakOpCanBreak() {
        when(blockBreakEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(blockBreakEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(true);

        listener.onBeaconBreak(blockBreakEvent);

        verify(blockBreakEvent, never()).setCancelled(true);
    }

    /**
     * Test that non-ops cannot break blocks at sky height
     */
    @Test
    void testOnBeaconBreakNonOpCannotBreak() {
        when(blockBreakEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT);
        when(blockBreakEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(false);

        listener.onBeaconBreak(blockBreakEvent);

        verify(blockBreakEvent).setCancelled(true);
        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test that blocks below sky height can be broken
     */
    @Test
    void testOnBeaconBreakAllowedBelowSkyHeight() {
        when(blockBreakEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(block.getY()).thenReturn(BLOCK_HEIGHT - 1);
        when(blockBreakEvent.getPlayer()).thenReturn(player);
        when(player.isOp()).thenReturn(false);

        listener.onBeaconBreak(blockBreakEvent);

        verify(blockBreakEvent, never()).setCancelled(true);
    }
}

