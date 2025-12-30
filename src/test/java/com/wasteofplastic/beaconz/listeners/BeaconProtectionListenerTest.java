package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;

class BeaconProtectionListenerTest extends CommonTestBase {
    private MockedStatic<Bukkit> bukkitStatic;

    private BeaconProtectionListener listener;

    @Mock private Block block;
    @Mock private Block belowBlock;
    @Mock private BlockSpreadEvent blockSpreadEvent;
    @Mock private BlockPistonExtendEvent pistonExtendEvent;
    @Mock private BlockPistonRetractEvent pistonRetractEvent;
    @Mock private PlayerBucketEmptyEvent bucketEmptyEvent;
    @Mock private BlockDispenseEvent dispenseEvent;
    @Mock private BlockFromToEvent liquidFlowEvent;
    @Mock private BlockPlaceEvent blockPlaceEvent;
    @Mock private BlockDamageEvent blockDamageEvent;
    @Mock private EntityExplodeEvent explodeEvent;
    @Mock private EntityDamageByEntityEvent edByEntityEvent;
    @Mock private EntityDamageEvent edEvent;
    @Mock private InventoryOpenEvent inventoryOpenEvent;
    @Mock private Inventory inventory;
    @Mock private InventoryHolder invHolder;
    @Mock private Location location;
    @Mock private Location toLocation;
    @Mock private BeaconObj beacon;
    @Mock private Game game;
    @Mock private Chicken chicken;
    @Mock private Minecart minecart;
    @Mock private LeashHitch leash;

    @BeforeEach
    void init() {
        // Provide a mocked Server so constructor logic that inspects online players does not NPE
        //org.bukkit.Server server = mock(org.bukkit.Server.class);
        when(plugin.getServer()).thenReturn(server);
        //when(server.getOnlinePlayers()).thenReturn(java.util.Collections.emptyList());

        // Mock Bukkit static calls used by constructor
        bukkitStatic = org.mockito.Mockito.mockStatic(Bukkit.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkitStatic.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkitStatic.when(Bukkit::getServer).thenReturn(server);

        listener = new BeaconProtectionListener(plugin);
        // Common Lang strings
        Lang.errorClearAroundBeacon = "errorClearAroundBeacon";
        Lang.errorYouCannotDoThat = "errorYouCannotDoThat";
        Lang.errorYouCannotBuildThere = "errorYouCannotBuildThere";
        Lang.beaconCannotPlaceLiquids = "beaconCannotPlaceLiquids";
        Lang.triangleThisBelongsTo = "triangleThisBelongsTo [team]";
    }

    @org.junit.jupiter.api.AfterEach
    void tearDownBukkit() {
        if (bukkitStatic != null) {
            bukkitStatic.close();
        }
    }

    // Constructor
    @Test
    void testBeaconProtectionListenerConstructs() {
        assertNotNull(listener);
    }

    // onBeaconDamage
    @Test
    void testOnBeaconDamageWrongWorld() {
        World other = mock(World.class);
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(other);
        listener.onBeaconDamage(blockDamageEvent);
        verify(plugin.getRegister(), never()).getBeacon(any());
    }

    @Test
    void testOnBeaconDamageLobbyNonOp() {
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(false);
        listener.onBeaconDamage(blockDamageEvent);
        verify(blockDamageEvent).setCancelled(true);
    }

    @Test
    void testOnBeaconDamageNoTeamNonOp() {
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(false);
        listener.onBeaconDamage(blockDamageEvent);
        verify(blockDamageEvent).setCancelled(true);
    }

    @Test
    void testOnBeaconDamageUnclearedCaptureBlocked() {
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        when(plugin.getRegister().getBeacon(block)).thenReturn(beacon);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(belowBlock);
        when(belowBlock.getType()).thenReturn(Material.BEACON);
        when(plugin.getRegister().isBeacon(belowBlock)).thenReturn(true);
        when(beacon.isNotClear()).thenReturn(true);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        listener.onBeaconDamage(blockDamageEvent);
        verify(blockDamageEvent).setCancelled(true);
        verify(player).sendMessage(eq(ChatColor.GOLD + Lang.errorClearAroundBeacon));
    }

    @Test
    void testOnBeaconDamageNotBeaconReturns() {
        when(blockDamageEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(blockDamageEvent.getPlayer()).thenReturn(player);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        when(plugin.getRegister().getBeacon(block)).thenReturn(null);
        listener.onBeaconDamage(blockDamageEvent);
        verify(blockDamageEvent, never()).setCancelled(true);
    }

    // onExplode
    @Test
    void testOnExplodeRemovesBeaconsFromDamageList() {
        when(explodeEvent.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        Block beaconBlock = mock(Block.class);
        Block otherBlock = mock(Block.class);
        when(explodeEvent.blockList()).thenReturn(new java.util.ArrayList<>(java.util.List.of(beaconBlock, otherBlock)));
        when(plugin.getRegister().isBeacon(beaconBlock)).thenReturn(true);
        when(plugin.getRegister().isBeacon(otherBlock)).thenReturn(false);
        listener.onExplode(explodeEvent);
        assertEquals(1, explodeEvent.blockList().size());
        assertTrue(explodeEvent.blockList().contains(otherBlock));
    }

    // onBlockSpread
    @Test
    void testOnBlockSpreadNonLeavesSetToAir() {
        Block spreadBlock = mock(Block.class);
        when(blockSpreadEvent.getBlock()).thenReturn(spreadBlock);
        when(spreadBlock.getWorld()).thenReturn(world);
        when(spreadBlock.getType()).thenReturn(Material.STONE);
        when(spreadBlock.getY()).thenReturn(66);
        when(spreadBlock.getX()).thenReturn(100);
        when(spreadBlock.getZ()).thenReturn(100);
        when(plugin.getRegister().getBeaconAt(100,100)).thenReturn(beacon);
        when(beacon.getY()).thenReturn(64);
        listener.onBlockSpread(blockSpreadEvent);
        verify(spreadBlock).setType(Material.AIR);
    }

    // onPistonPush
    @Test
    void testOnPistonPushBeaconBlockCancelled() {
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        Block pushed = mock(Block.class);
        when(plugin.getRegister().isBeacon(pushed)).thenReturn(true);
        when(pistonExtendEvent.getBlocks()).thenReturn(java.util.List.of(pushed));
        listener.onPistonPush(pistonExtendEvent);
        verify(pistonExtendEvent).setCancelled(true);
    }

    @Test
    void testOnPistonPushIntoAboveBeaconCancelled() {
        when(pistonExtendEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        Block moving = mock(Block.class);
        Block destination = mock(Block.class);
        when(pistonExtendEvent.getBlocks()).thenReturn(java.util.List.of(moving));
        when(pistonExtendEvent.getDirection()).thenReturn(BlockFace.UP);
        when(moving.getRelative(BlockFace.UP)).thenReturn(destination);
        when(destination.getX()).thenReturn(100);
        when(destination.getZ()).thenReturn(100);
        when(destination.getY()).thenReturn(66);
        when(plugin.getRegister().getBeaconAt(100,100)).thenReturn(beacon);
        when(beacon.getY()).thenReturn(64);
        listener.onPistonPush(pistonExtendEvent);
        verify(pistonExtendEvent).setCancelled(true);
    }

    // onPistonPull
    @Test
    void testOnPistonPullBeaconBlockCancelled() {
        when(pistonRetractEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        Block pulled = mock(Block.class);
        when(plugin.getRegister().isBeacon(pulled)).thenReturn(true);
        when(pistonRetractEvent.getBlocks()).thenReturn(java.util.List.of(pulled));
        listener.onPistonPull(pistonRetractEvent);
        verify(pistonRetractEvent).setCancelled(true);
    }

    // onBucketEmpty
    @Test
    void testOnBucketEmptyAboveBeaconCancelled() {
        when(bucketEmptyEvent.getBlockClicked()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(bucketEmptyEvent.getBlockFace()).thenReturn(BlockFace.UP);
        when(bucketEmptyEvent.getPlayer()).thenReturn(player);
        Block target = mock(Block.class);
        when(block.getRelative(BlockFace.UP)).thenReturn(target);
        when(target.getX()).thenReturn(100);
        when(target.getZ()).thenReturn(100);
        when(target.getLocation()).thenReturn(location);
        when(plugin.getRegister().getBeaconAt(100,100)).thenReturn(beacon);
        when(beacon.getY()).thenReturn(64);
        when(block.getY()).thenReturn(64);
        listener.onBucketEmpty(bucketEmptyEvent);
        verify(bucketEmptyEvent).setCancelled(true);
        verify(bucketEmptyEvent.getPlayer()).sendMessage(eq(ChatColor.RED + Lang.beaconCannotPlaceLiquids));
    }

    @Test
    void testOnBucketEmptyIsAboveBeaconLocationCancelled() {
        when(bucketEmptyEvent.getBlockClicked()).thenReturn(block);
        when(bucketEmptyEvent.getPlayer()).thenReturn(player);
        when(block.getWorld()).thenReturn(world);
        when(bucketEmptyEvent.getBlockFace()).thenReturn(BlockFace.NORTH);
        Block target = mock(Block.class);
        when(block.getRelative(BlockFace.NORTH)).thenReturn(target);
        when(target.getLocation()).thenReturn(location);
        when(plugin.getRegister().isAboveBeacon(location)).thenReturn(true);
        listener.onBucketEmpty(bucketEmptyEvent);
        verify(bucketEmptyEvent).setCancelled(true);
    }

    // onDispense
    @Test
    void testOnDispenseNonLiquidReturns() {
        when(dispenseEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(dispenseEvent.getItem()).thenReturn(item);
        listener.onDispense(dispenseEvent);
        verify(plugin.getRegister(), never()).isAboveBeacon(any());
    }

    // onLiquidFlow
    @Test
    void testOnLiquidFlowHorizontalIntoAboveBeaconCancelled() {
        Block from = mock(Block.class);
        Block to = mock(Block.class);
        when(liquidFlowEvent.getBlock()).thenReturn(from);
        when(liquidFlowEvent.getToBlock()).thenReturn(to);
        when(from.getWorld()).thenReturn(world);
        when(to.getX()).thenReturn(101);
        when(to.getZ()).thenReturn(100);
        when(from.getX()).thenReturn(100);
        when(from.getZ()).thenReturn(100);
        when(to.getY()).thenReturn(66);
        when(plugin.getRegister().getBeaconAt(101,100)).thenReturn(beacon);
        when(beacon.getY()).thenReturn(64);
        listener.onLiquidFlow(liquidFlowEvent);
        verify(liquidFlowEvent).setCancelled(true);
    }

    @Test
    void testOnLiquidFlowOutsideGameCancelled() {
        Block from = mock(Block.class);
        Block to = mock(Block.class);
        when(liquidFlowEvent.getBlock()).thenReturn(from);
        when(liquidFlowEvent.getToBlock()).thenReturn(to);
        when(from.getWorld()).thenReturn(world);
        when(to.getX()).thenReturn(101);
        when(to.getZ()).thenReturn(100);
        when(from.getX()).thenReturn(100);
        when(from.getZ()).thenReturn(100);
        when(plugin.getRegister().getBeaconAt(101,100)).thenReturn(null);
        when(mgr.getGame(from.getLocation())).thenReturn(null);
        listener.onLiquidFlow(liquidFlowEvent);
        verify(liquidFlowEvent).setCancelled(true);
    }

    // onBlockPlace
    @Test
    void testOnBlockPlaceLobbyNonOpCancelled() {
        when(blockPlaceEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(blockPlaceEvent.getPlayer()).thenReturn(player);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(false);
        listener.onBlockPlace(blockPlaceEvent);
        verify(blockPlaceEvent).setCancelled(true);
    }

    @Test
    void testOnBlockPlaceOutsideGameCancelledWithMessage() {
        when(blockPlaceEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(blockPlaceEvent.getPlayer()).thenReturn(player);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(player.isOp()).thenReturn(false);
        when(mgr.getGame(block.getLocation())).thenReturn(null);
        listener.onBlockPlace(blockPlaceEvent);
        verify(blockPlaceEvent).setCancelled(true);
        verify(player).sendMessage(eq(ChatColor.RED + Lang.errorYouCannotDoThat));
    }

    @Test
    void testOnBlockPlaceOnBeaconCancelled() {
        when(blockPlaceEvent.getBlock()).thenReturn(block);
        when(block.getWorld()).thenReturn(world);
        when(blockPlaceEvent.getPlayer()).thenReturn(player);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);
        when(player.isOp()).thenReturn(false);
        when(mgr.getGame(block.getLocation())).thenReturn(game);
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        when(block.getX()).thenReturn(100);
        when(block.getZ()).thenReturn(100);
        when(block.getY()).thenReturn(66);
        when(plugin.getRegister().getBeaconAt(100,100)).thenReturn(beacon);
        when(beacon.getY()).thenReturn(64);
        listener.onBlockPlace(blockPlaceEvent);
        verify(blockPlaceEvent).setCancelled(true);
        verify(player).sendMessage(eq(ChatColor.RED + Lang.errorYouCannotBuildThere));
    }

    // onEntityDamage (EntityDamageByEntityEvent)
    @Test
    void testOnEntityDamageAnimalsAttackedByEnemyPlayerCancelled() {
        when(edByEntityEvent.getEntity()).thenReturn(chicken);
        when(chicken.getWorld()).thenReturn(world);
        when(chicken.getLocation()).thenReturn(location);
        when(plugin.getRegister().getBeaconAt(location)).thenReturn(beacon);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        Player enemy = mock(Player.class);
        when(edByEntityEvent.getDamager()).thenReturn(enemy);
        when(mgr.getGame(enemy.getLocation())).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam(enemy)).thenReturn(team);
        // Player's team differs from beacon ownership
        listener.onEntityDamage(edByEntityEvent);
        verify(edByEntityEvent).setCancelled(true);
        verify(enemy).sendMessage(anyString());
    }

    @Test
    void testOnEntityDamageAnimalsAttackedByNonPlayerCancelled() {
        when(edByEntityEvent.getEntity()).thenReturn(chicken);
        when(chicken.getWorld()).thenReturn(world);
        when(chicken.getLocation()).thenReturn(location);
        when(plugin.getRegister().getBeaconAt(location)).thenReturn(beacon);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        Object nonPlayer = new Object();
        // Mockito can't mock Object as damager; set a non-player by not stubbing getDamager as Player
        when(edByEntityEvent.getDamager()).thenReturn(null);
        listener.onEntityDamage(edByEntityEvent);
        verify(edByEntityEvent).setCancelled(true);
    }

    // onEntityDamage (EntityDamageEvent)
    @Test
    void testOnEntityDamageAnimalsProtectedOnBeacon() {
        when(edEvent.getEntity()).thenReturn(chicken);
        when(chicken.getWorld()).thenReturn(world);
        when(chicken.getLocation()).thenReturn(location);
        when(plugin.getRegister().getBeaconAt(location)).thenReturn(beacon);
        listener.onEntityDamage(edEvent);
        verify(edEvent).setCancelled(true);
    }

    @Test
    void testOnEntityDamageLeashProtectedOnBeacon() {
        when(edEvent.getEntity()).thenReturn(leash);
        when(leash.getWorld()).thenReturn(world);
        when(leash.getLocation()).thenReturn(location);
        when(plugin.getRegister().getBeaconAt(location)).thenReturn(beacon);
        listener.onEntityDamage(edEvent);
        verify(edEvent).setCancelled(true);
    }

    // onInventoryOpen
    @Test
    void testOnInventoryOpenNonPlayerInventoryEnemyBeaconCancelled() {
        when(inventoryOpenEvent.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        when(inventoryOpenEvent.getInventory()).thenReturn(inventory);
        when(inventory.getType()).thenReturn(InventoryType.CHEST);
        when(inventory.getHolder()).thenReturn(invHolder);
        when(inventory.getLocation()).thenReturn(location);
        when(mgr.getGame(player.getLocation())).thenReturn(game);
        when(game.getScorecard()).thenReturn(scorecard);
        when(scorecard.getTeam(player)).thenReturn(team);
        when(plugin.getRegister().getBeaconAt(location)).thenReturn(beacon);
        when(beacon.getOwnership()).thenReturn(otherTeam);
        listener.onInventoryOpen(inventoryOpenEvent);
        verify(inventoryOpenEvent).setCancelled(true);
        verify(player).sendMessage(anyString());
    }

    // getStandingOn
    @Test
    void testGetStandingOnNotNull() {
        assertNotNull(BeaconProtectionListener.getStandingOn());
    }
}
