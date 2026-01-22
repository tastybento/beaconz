package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
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

import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.core.Region;

import net.kyori.adventure.text.Component;

class LobbyListenerTest extends CommonTestBase {

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
        @NotNull
        SignSide side = mock(SignSide.class);
        when(sign.getSide(Side.FRONT)).thenReturn(side);
        when(side.line(0)).thenReturn(Component.text(""));
        when(block.getState()).thenReturn(sign);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player).sendMessage(Lang.adminUseSurvival);
        verify(player, never()).sendMessage(Lang.errorNotReady);
        verify(player, never()).sendMessage(Lang.errorNoSuchGame);
        
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
        @NotNull
        SignSide side = mock(SignSide.class);
        when(sign.getSide(Side.FRONT)).thenReturn(side);
        when(side.line(0)).thenReturn(Component.text(""));
        when(block.getState()).thenReturn(sign);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player, never()).sendMessage(Lang.errorNotReady);
        verify(player, never()).sendMessage(Lang.errorNoSuchGame);
        
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
        @NotNull
        SignSide side = mock(SignSide.class);
        when(sign.getSide(Side.FRONT)).thenReturn(side);
        when(side.line(0)).thenReturn(Lang.adminSignKeyword);
        when(block.getState()).thenReturn(sign);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player).sendMessage(Lang.errorNotReady);
        verify(player).sendMessage(Lang.errorNoSuchGame);
        
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
        @NotNull
        SignSide side = mock(SignSide.class);
        when(sign.getSide(Side.FRONT)).thenReturn(side);
        when(side.line(0)).thenReturn(Lang.adminSignKeyword);
        when(side.line(1)).thenReturn(Component.text("gameName"));
        when(side.line(2)).thenReturn(Component.text(""));
        when(side.line(3)).thenReturn(Component.text(""));
        when(block.getState()).thenReturn(sign);
        
        // Mock getGame to return game for line 1, null for others
        lenient().when(mgr.getGame(any(Component.class))).thenAnswer(invocation -> {
            Component comp = invocation.getArgument(0);
            // Use PlainTextComponentSerializer to get the text content
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
            // Return game only for non-empty text
            if (text != null && !text.isEmpty()) {
                return game;
            }
            return null;
        });
        // Game is over
        when(game.isOver()).thenReturn(true);
        
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player).sendMessage(Lang.scoreGameOver);
        verify(player, never()).sendMessage(Lang.errorNotReady);
        verify(player, never()).sendMessage(Lang.errorNoSuchGame);
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
        @NotNull
        SignSide side = mock(SignSide.class);
        when(sign.getSide(Side.FRONT)).thenReturn(side);
        when(side.line(0)).thenReturn(Lang.adminSignKeyword);
        when(side.line(1)).thenReturn(Component.text("gameName"));
        when(side.line(2)).thenReturn(Component.text(""));
        when(side.line(3)).thenReturn(Component.text(""));
        when(block.getState()).thenReturn(sign);
        
        // Mock getGame to return game for line 1, null for others
        lenient().when(mgr.getGame(any(Component.class))).thenAnswer(invocation -> {
            Component comp = invocation.getArgument(0);
            // Use PlainTextComponentSerializer to get the text content
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
            // Return game only for non-empty text
            if (text != null && !text.isEmpty()) {
                return game;
            }
            return null;
        });

        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, null, block, BlockFace.UP);
        ll.onSignClick(event);
        verify(player, never()).sendMessage(Lang.scoreGameOver);
        verify(player, never()).sendMessage(Lang.errorNotReady);
        verify(player, never()).sendMessage(Lang.errorNoSuchGame);
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
        when(mgr.getLobby()).thenReturn(lobby);
        List<Component> lines = List.of(Component.text("line1"), Component.text("line2"), Component.text(""), Component.text(""), Component.text(""));
        // Use a different world to trigger the early return
        World wrongWorld = mock(World.class);
        when(block.getWorld()).thenReturn(wrongWorld);
        SignChangeEvent event = new SignChangeEvent(block, player, lines, Side.FRONT);
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
        List<Component> lines = List.of(Lang.adminSignKeyword, Component.text("gamename"), Component.text(""), Component.text(""), Component.text(""));
        when(block.getWorld()).thenReturn(world);
        SignChangeEvent event = new SignChangeEvent(block, player, lines, Side.FRONT);
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);

        // Mock getGame to return game for line 1, null for others
        lenient().when(mgr.getGame(any(Component.class))).thenAnswer(invocation -> {
            Component comp = invocation.getArgument(0);
            // Use PlainTextComponentSerializer to get the text content
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
            // Return game only for non-empty text
            if (text != null && !text.isEmpty()) {
                return game;
            }
            return null;
        });

        ll.onSignPlace(event);
        verify(block).getWorld();
        verify(lobby).isPlayerInRegion(any());
        // The actual message is adminGameSignPlaced.append(Component.text(" - ").append(event.line(i)))
        verify(player).sendMessage(any(Component.class));
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.LobbyListener#onSignPlace(org.bukkit.event.block.SignChangeEvent)}.
     */
    @Test
    void testOnSignPlaceGameDoesNotExists() {
        Region lobby = mock(Region.class);

        List<Component> lines = List.of(Lang.adminSignKeyword, Component.text("fakename"), Component.text(""), Component.text(""), Component.text(""));
        when(block.getWorld()).thenReturn(world);
        SignChangeEvent event = new SignChangeEvent(block, player, lines, Side.FRONT);
        
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        
        ll.onSignPlace(event);
        verify(block).getWorld();
        verify(lobby).isPlayerInRegion(any());
        verify(player, never()).sendMessage(Lang.adminGameSignPlaced);
        verify(player).sendMessage(Lang.errorNoSuchGame);
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
