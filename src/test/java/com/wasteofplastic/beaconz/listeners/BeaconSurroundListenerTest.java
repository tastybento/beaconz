package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import com.wasteofplastic.beaconz.BeaconObj;

/**
 * Tests BeaconSurroundListener behaviors for block damage protection, explosion filtering, and tree growth prevention near beacons.
 */
class BeaconSurroundListenerTest extends BeaconzListenerTestBase {

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.BeaconSurroundListener#BeaconSurroundListener(com.wasteofplastic.beaconz.Beaconz)}.
     */
    @Test
    void testBeaconSurroundListenerConstructs() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        assertNotNull(listener);
    }

    /** Damage in non-beacon world is ignored entirely. */
    @Test
    void testOnBeaconDamageWrongWorld() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        BlockDamageEvent event = mock(BlockDamageEvent.class);
        Block otherBlock = mock(Block.class);
        World otherWorld = mock(World.class);
        when(event.getBlock()).thenReturn(otherBlock);
        when(otherBlock.getWorld()).thenReturn(otherWorld);

        listener.onBeaconDamage(event);

        verify(register, never()).getNearbyBeacons(any(), anyInt());
        verify(event, never()).setCancelled(anyBoolean());
    }

    /** Non-protected materials are allowed to be damaged. */
    @Test
    void testOnBeaconDamageNonProtectedMaterial() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        BlockDamageEvent event = mock(BlockDamageEvent.class);
        Block target = mock(Block.class);
        when(event.getBlock()).thenReturn(target);
        when(target.getWorld()).thenReturn(world);
        when(target.getType()).thenReturn(Material.OAK_PLANKS); // not in protectedMaterials

        listener.onBeaconDamage(event);

        verify(register, never()).getNearbyBeacons(any(), anyInt());
        verify(event, never()).setCancelled(anyBoolean());
    }

    /** Protected material but no nearby beacons -> no cancellation. */
    @Test
    void testOnBeaconDamageNoNearbyBeacons() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        BlockDamageEvent event = mock(BlockDamageEvent.class);
        Block target = mock(Block.class);
        when(event.getBlock()).thenReturn(target);
        when(target.getWorld()).thenReturn(world);
        when(target.getType()).thenReturn(Material.STONE);
        when(target.getLocation()).thenReturn(location);
        when(register.getNearbyBeacons(location, 10)).thenReturn(List.of());

        listener.onBeaconDamage(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    /** Above/beacon-height or far below range: damage allowed. */
    @Test
    void testOnBeaconDamageAboveOrBelowRangeAllowed() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        BlockDamageEvent event = mock(BlockDamageEvent.class);
        Block target = mock(Block.class);
        BeaconObj beaconObj = mock(BeaconObj.class);
        when(event.getBlock()).thenReturn(target);
        when(target.getWorld()).thenReturn(world);
        when(target.getType()).thenReturn(Material.STONE);
        when(target.getLocation()).thenReturn(location);
        when(target.getY()).thenReturn(80); // well above
        when(beaconObj.getY()).thenReturn(70);
        when(register.getNearbyBeacons(location, 10)).thenReturn(List.of(beaconObj));

        listener.onBeaconDamage(event);
        verify(event, never()).setCancelled(anyBoolean());

        // Far below the protected range (below lowestY - RANGE)
        when(target.getY()).thenReturn(50); // with lowestY 70, RANGE 10 => below 60 triggers allow
        listener.onBeaconDamage(event);
        verify(event, never()).setCancelled(true);
    }

    /** Within protected vertical band and near beacon: damage cancelled. */
    @Test
    void testOnBeaconDamageWithinRangeCancelled() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        BlockDamageEvent event = mock(BlockDamageEvent.class);
        Block target = mock(Block.class);
        Player p = mock(Player.class);
        ItemStack hand = new ItemStack(Material.IRON_PICKAXE);
        when(event.getBlock()).thenReturn(target);
        when(event.getPlayer()).thenReturn(p);
        when(p.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(hand);
        when(target.getWorld()).thenReturn(world);
        when(world.getMaxHeight()).thenReturn(320);
        when(p.getWorld()).thenReturn(world);
        when(target.getType()).thenReturn(Material.STONE);
        when(target.getLocation()).thenReturn(location);
        when(target.getY()).thenReturn(68); // lowestY=70 -> within protected band (>=60 and <69)
        BeaconObj beaconObj = mock(BeaconObj.class);
        when(beaconObj.getY()).thenReturn(70);
        when(register.getNearbyBeacons(location, 10)).thenReturn(List.of(beaconObj));

        listener.onBeaconDamage(event);

        verify(event).setCancelled(true);
    }

    /** Explosion in wrong world leaves list untouched. */
    @Test
    void testOnExplodeWrongWorld() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        World otherWorld = mock(World.class);
        when(event.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(otherWorld);

        listener.onExplode(event);

        verify(register, never()).getNearbyBeacons(any(), anyInt());
    }

    /** Protected blocks near beacons are removed from explosion damage list. */
    @Test
    void testOnExplodeRemovesProtectedNearBeacon() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        when(event.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        Block protectedBlock = mock(Block.class);
        Block otherBlock = mock(Block.class);
        when(protectedBlock.getType()).thenReturn(Material.STONE);
        when(otherBlock.getType()).thenReturn(Material.OAK_PLANKS);
        when(protectedBlock.getLocation()).thenReturn(location);
        when(otherBlock.getLocation()).thenReturn(location);
        when(event.blockList()).thenReturn(new java.util.ArrayList<>(List.of(protectedBlock, otherBlock)));
        BeaconObj beaconObj = mock(BeaconObj.class);
        when(beaconObj.getY()).thenReturn(70);
        when(register.getNearbyBeacons(location, 10)).thenReturn(List.of(beaconObj));
        when(world.getMaxHeight()).thenReturn(256);

        listener.onExplode(event);

        assertEquals(1, event.blockList().size());
        assertTrue(event.blockList().contains(otherBlock));
    }

    /** Tree growth above a beacon cancels the event. */
    @Test
    void testOnTreeGrowAboveBeaconCancelled() {
        BeaconSurroundListener listener = new BeaconSurroundListener(plugin);
        StructureGrowEvent event = mock(StructureGrowEvent.class);
        when(event.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(world);
        BlockState state = mock(BlockState.class);
        Location stateLoc = mock(Location.class);
        when(state.getLocation()).thenReturn(stateLoc);
        when(register.isAboveBeacon(stateLoc)).thenReturn(true);
        when(event.getBlocks()).thenReturn(List.of(state));

        listener.onTreeGrow(event);

        verify(event).setCancelled(true);
    }
}
