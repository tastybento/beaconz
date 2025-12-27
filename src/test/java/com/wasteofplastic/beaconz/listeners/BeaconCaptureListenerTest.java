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

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
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
import com.wasteofplastic.beaconz.GameMgr;
import com.wasteofplastic.beaconz.Register;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

/**
 * @author tastybento
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
        
       
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception {
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.BeaconCaptureListener#BeaconCaptureListener(com.wasteofplastic.beaconz.Beaconz)}.
     */
    @Test
    void testBeaconCaptureListener() {
        assertNotNull(bcl);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.BeaconCaptureListener#onBeaconDamage(org.bukkit.event.block.BlockDamageEvent)}.
     */
    @Test
    void testOnBeaconDamageHappyPath() {
        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);
        verify(beacon).checkIntegrity();
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBeaconDamageNotInWorld() {
        World otherWorld = org.mockito.Mockito.mock(World.class);
        when(block.getWorld()).thenReturn(otherWorld);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBeaconDamagePlayerInLobbyNonOp() {
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(false);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnBeaconDamagePlayerInLobbyOp() {
        when(mgr.isPlayerInLobby(player)).thenReturn(true);
        when(player.isOp()).thenReturn(true);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBeaconDamageNoTeamNonOp() {
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(false);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnBeaconDamageNoTeamOp() {
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(true);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register, never()).getBeacon(block);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBeaconDamageNotBeaconBlock() {
        when(register.getBeacon(block)).thenReturn(null);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(register).getBeacon(block);
        verify(beacon, never()).checkIntegrity();
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBeaconDamageBlockNotAboveBeacon() {
        when(beaconBlock.getType()).thenReturn(Material.OBSIDIAN);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(beacon).checkIntegrity();
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBeaconDamageNotRegisteredBeacon() {
        when(register.isBeacon(beaconBlock)).thenReturn(false);

        BlockDamageEvent e = new BlockDamageEvent(player, block, BlockFace.EAST, item, false);
        bcl.onBeaconDamage(e);

        verify(beacon).checkIntegrity();
        assertFalse(e.isCancelled());
    }

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

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.BeaconCaptureListener#onBeaconBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    @Disabled("Not yet implemented")
    void testOnBeaconBreak() {
        fail("Not yet implemented");
    }

}
