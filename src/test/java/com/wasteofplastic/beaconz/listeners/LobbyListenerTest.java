package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Region;

class LobbyListenerTest extends BeaconzListenerTestBase {

    private LobbyListener ll;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        ll = new LobbyListener(plugin);
        
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#LobbyListener(com.wasteofplastic.beaconz.Beaconz)}.
     */
    @Test
    void testLobbyListener() {
        assertNotNull(ll);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickNoBlock() {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, null, null, BlockFace.UP);
        ll.onSignClick(event);
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.DENY, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickHasBlockNoLeftClickBlock() {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_AIR, null, block, BlockFace.UP);
        ll.onSignClick(event);
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickNotASign() {
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickWrongWorld() {
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getWorld()).thenReturn(mock(World.class));
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickNotInRegion() {
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getWorld()).thenReturn(world);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
        ll.onSignClick(event);
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickCreativeModeNotSign() {
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getWorld()).thenReturn(world);
        
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        Sign sign = mock(Sign.class);
        when(sign.getLine(0)).thenReturn("");
        when(block.getState()).thenReturn(sign);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player).sendMessage(ChatColor.RED + Lang.adminUseSurvival);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNotReady);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNoSuchGame);
        
        Result r = event.useItemInHand();
        assertEquals(Result.DENY, r);
        r = event.useInteractedBlock();
        assertEquals(Result.DENY, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickSurvivalModeNotSign() {
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getWorld()).thenReturn(world);
        
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        Sign sign = mock(Sign.class);
        when(sign.getLine(0)).thenReturn("");
        when(block.getState()).thenReturn(sign);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNotReady);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNoSuchGame);
        
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickSurvivalModeCorrectSignMissingContent() {
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getWorld()).thenReturn(world);
        
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        Sign sign = mock(Sign.class);
        when(sign.getLine(0)).thenReturn(Lang.adminSignKeyword.toLowerCase());
        when(block.getState()).thenReturn(sign);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player).sendMessage(ChatColor.RED + Lang.errorNotReady);
        verify(player).sendMessage(ChatColor.RED + Lang.errorNoSuchGame);
        
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickSurvivalModeCorrectSignWithContentGameOver() {
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getWorld()).thenReturn(world);
        
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        Sign sign = mock(Sign.class);
        when(sign.getLine(0)).thenReturn(Lang.adminSignKeyword.toLowerCase());
        when(sign.getLine(1)).thenReturn("gameName");
        when(block.getState()).thenReturn(sign);
        
        when(mgr.getGame(anyString())).thenReturn(game);
        // Game is over
        when(game.isOver()).thenReturn(true);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player).sendMessage(ChatColor.RED + Lang.scoreGameOver);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNotReady);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNoSuchGame);
        verify(game, never()).join(player);
        
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignClick(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    void testOnSignClickSurvivalModeCorrectSignWithContentGameOn() {
        when(block.getType()).thenReturn(Material.OAK_SIGN);
        when(block.getWorld()).thenReturn(world);
        
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        Sign sign = mock(Sign.class);
        when(sign.getLine(0)).thenReturn(Lang.adminSignKeyword.toLowerCase());
        when(sign.getLine(1)).thenReturn("gameName");
        when(block.getState()).thenReturn(sign);
        
        when(mgr.getGame(anyString())).thenReturn(game);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.scoreGameOver);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNotReady);
        verify(player, never()).sendMessage(ChatColor.RED + Lang.errorNoSuchGame);
        verify(game).join(player);
        
        Result r = event.useItemInHand();
        assertEquals(Result.DEFAULT, r);
        r = event.useInteractedBlock();
        assertEquals(Result.ALLOW, r);
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignPlace(org.bukkit.event.block.SignChangeEvent)}.
     */
    @Test
    void testOnSignPlaceWrongWorld() {
        Region lobby = mock(Region.class);

        @NotNull
        String[] lines = {"line1", "line2"};
        when(block.getWorld()).thenReturn(mock(World.class));
        SignChangeEvent event = new SignChangeEvent(block, player, lines );
        ll.onSignPlace(event);
        verify(block).getWorld();
        verify(lobby, never()).isPlayerInRegion(any());
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignPlace(org.bukkit.event.block.SignChangeEvent)}.
     */
    @Test
    void testOnSignPlaceGameExists() {
        Region lobby = mock(Region.class);

        @NotNull
        String[] lines = {Lang.adminSignKeyword, "gamename"};
        when(block.getWorld()).thenReturn(world);
        SignChangeEvent event = new SignChangeEvent(block, player, lines );
        
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);

        when(mgr.getGame(anyString())).thenReturn(game);
        
        ll.onSignPlace(event);
        verify(block).getWorld();
        verify(lobby).isPlayerInRegion(any());
        verify(player).sendMessage(ChatColor.GREEN + Lang.adminGameSignPlaced + " - gamename");
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignPlace(org.bukkit.event.block.SignChangeEvent)}.
     */
    @Test
    void testOnSignPlaceGameDoesNotExists() {
        Region lobby = mock(Region.class);

        @NotNull
        String[] lines = {Lang.adminSignKeyword, "fakename", "", "", ""};
        when(block.getWorld()).thenReturn(world);
        SignChangeEvent event = new SignChangeEvent(block, player, lines );
        
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        
        ll.onSignPlace(event);
        verify(block).getWorld();
        verify(lobby).isPlayerInRegion(any());
        verify(player, never()).sendMessage(ChatColor.GREEN + Lang.adminGameSignPlaced + " - gamename");
        verify(player).sendMessage(ChatColor.RED + Lang.errorNoSuchGame);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    void testOnMobSpawn() {
        Slime slime = mock(Slime.class);
        Sheep sheep = mock(Sheep.class);
        Zombie zombie = mock(Zombie.class);
        
        when(slime.getWorld()).thenReturn(world);
        when(slime.getLocation()).thenReturn(location);
        when(sheep.getWorld()).thenReturn(world);
        when(sheep.getLocation()).thenReturn(location);
        when(zombie.getWorld()).thenReturn(world);
        when(zombie.getLocation()).thenReturn(location);
        Region lobby = mock(Region.class);
        when(lobby.contains(location)).thenReturn(true);
        when(mgr.getLobby()).thenReturn(lobby);
        CreatureSpawnEvent event = new CreatureSpawnEvent(slime, SpawnReason.NATURAL);
        ll.onMobSpawn(event);
        assertTrue(event.isCancelled());
        event = new CreatureSpawnEvent(sheep, SpawnReason.NATURAL);
        ll.onMobSpawn(event);
        assertTrue(event.isCancelled());
        event = new CreatureSpawnEvent(zombie, SpawnReason.NATURAL);
        ll.onMobSpawn(event);
        assertTrue(event.isCancelled());
    }
    
}
