package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Region;
import com.wasteofplastic.beaconz.Settings;

import net.kyori.adventure.text.Component;

class PlayerTeleportListenerTest extends CommonTestBase {

    private PlayerTeleportListener ptl;
    
    @Mock
    private Region lobby;
    @Mock
    private Region gameRegion;
    @Mock
    private World otherWorld;
    @Mock
    private Location lobbyLocation;
    @Mock
    private Location gameLocation;
    @Mock
    private Location otherLocation;

    private UUID playerUUID;
    private static final String GAME_NAME = "TestGame";

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();

        // Initialize Lang strings needed for teleport listener
        Lang.titleBeaconzNews = "Beaconz News";
        Lang.errorNotInGame = "You are not in the game '[game]'! Going to the lobby...";
        Lang.teleportDoNotMove = "Do not move, teleporting in [number] seconds!";
        Lang.teleportYouMoved = "You moved! Cancelling teleport!";

        // Initialize Settings
        Settings.teleportDelay = 3;

        // Setup player UUID
        playerUUID = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUUID);

        // Setup locations
        when(location.getWorld()).thenReturn(world);
        when(lobbyLocation.getWorld()).thenReturn(world);
        when(gameLocation.getWorld()).thenReturn(world);
        when(otherLocation.getWorld()).thenReturn(otherWorld);

        when(location.toVector()).thenReturn(new Vector(100, 64, 100));
        when(lobbyLocation.toVector()).thenReturn(new Vector(0, 64, 0));
        when(gameLocation.toVector()).thenReturn(new Vector(500, 64, 500));

        // Setup lobby
        when(mgr.getLobby()).thenReturn(lobby);
        when(lobby.getSpawnPoint()).thenReturn(lobbyLocation);
        when(lobbyLocation.clone()).thenReturn(lobbyLocation);
        when(lobby.isPlayerInRegion(player)).thenReturn(false);
        when(mgr.isLocationInLobby(lobbyLocation)).thenReturn(true);
        when(mgr.isLocationInLobby(location)).thenReturn(false);
        when(mgr.isLocationInLobby(gameLocation)).thenReturn(false);
        when(mgr.isPlayerInLobby(player)).thenReturn(false);

        // Setup game
        when(game.getName()).thenReturn(GAME_NAME);
        when(game.getRegion()).thenReturn(gameRegion);
        when(game.hasPlayer(player)).thenReturn(true);
        when(game.getGamemode()).thenReturn("strategy");
        when(game.isGameRestart()).thenReturn(false);
        when(game.isOver()).thenReturn(false);
        when(mgr.getGame(gameLocation)).thenReturn(game);
        when(mgr.getGame(location)).thenReturn(null);
        when(mgr.getGame(lobbyLocation)).thenReturn(null);

        ptl = new PlayerTeleportListener(plugin);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#PlayerTeleportListener(com.wasteofplastic.beaconz.Beaconz)}.
     */
    @Test
    void testPlayerTeleportListener() {
       assertNotNull(ptl);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     * Tests player entering Beaconz world - should save name, send to lobby, and clear potion effects.
     */
    @Test
    void testOnWorldEnter() {
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getActivePotionEffects()).thenReturn(new ArrayList<>());
        when(messages.getMessages(playerUUID)).thenReturn(null);

        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, otherWorld);
        ptl.onWorldEnter(event);

        // Verify player name is saved
        verify(nameStore).savePlayerName("TestPlayer", playerUUID);

        // Verify player is sent to lobby if not already there
        verify(lobby).tpToRegionSpawn(player, true);
        verify(lobby).enterLobby(player);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     * Tests entering world with pending messages - messages should be sent.
     */
    @Test
    void testOnWorldEnterWithMessages() {
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getActivePotionEffects()).thenReturn(new ArrayList<>());

        List<String> messages = new ArrayList<>();
        messages.add("Message 1");
        messages.add("Message 2");
        when(this.messages.getMessages(playerUUID)).thenReturn(messages);

        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, otherWorld);
        ptl.onWorldEnter(event);

        verify(nameStore).savePlayerName("TestPlayer", playerUUID);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     * Tests entering world when player is already in lobby - should not teleport again.
     */
    @Test
    void testOnWorldEnterAlreadyInLobby() {
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getActivePotionEffects()).thenReturn(new ArrayList<>());
        when(messages.getMessages(playerUUID)).thenReturn(null);
        when(mgr.isPlayerInLobby(player)).thenReturn(true);

        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, otherWorld);
        ptl.onWorldEnter(event);

        verify(nameStore).savePlayerName("TestPlayer", playerUUID);
        verify(lobby, never()).tpToRegionSpawn(any(), anyBoolean());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onWorldEnter(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     * Tests entering world with potion effects - effects should be removed.
     */
    @Test
    void testOnWorldEnterWithPotionEffects() {
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("TestPlayer");

        List<PotionEffect> effects = new ArrayList<>();
        effects.add(new PotionEffect(PotionEffectType.SPEED, 100, 1));
        effects.add(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
        when(player.getActivePotionEffects()).thenReturn(effects);
        when(messages.getMessages(playerUUID)).thenReturn(null);

        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, otherWorld);
        ptl.onWorldEnter(event);

        verify(player).removePotionEffect(PotionEffectType.SPEED);
        verify(player).removePotionEffect(PotionEffectType.REGENERATION);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onWorldExit(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     * Tests player exiting Beaconz world - should remove from standingOn map, clear scoreboard, and remove potion effects.
     */
    @Test
    void testOnWorldExit() {
        when(player.getWorld()).thenReturn(otherWorld);

        List<PotionEffect> effects = new ArrayList<>();
        effects.add(new PotionEffect(PotionEffectType.SPEED, 100, 1));
        when(player.getActivePotionEffects()).thenReturn(effects);

        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        ptl.onWorldExit(event);

        verify(player).setScoreboard(any(Scoreboard.class));
        verify(player).removePotionEffect(PotionEffectType.SPEED);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onWorldExit(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     * Tests exiting to same world - should not process.
     */
    @Test
    void testOnWorldExitSameWorld() {
        when(player.getWorld()).thenReturn(otherWorld);

        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, otherWorld);
        ptl.onWorldExit(event);

        verify(player, never()).setScoreboard(any());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport from non-Beaconz world - should not process.
     */
    @Test
    void testOnTeleportFromOtherWorld() {
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(otherWorld);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, location);
        ptl.onTeleport(event);

        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport within same game - should be ignored.
     */
    @Test
    void testOnTeleportWithinSameGame() {
        Location from = mock(Location.class);
        Location to = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(to.getWorld()).thenReturn(world);
        when(mgr.getGame(from)).thenReturn(game);
        when(mgr.getGame(to)).thenReturn(game);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, to);
        ptl.onTeleport(event);

        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport from game to lobby without delay (OP player) - should not be delayed.
     */
    @Test
    void testOnTeleportFromGameToLobbyAsOP() {
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(500, 64, 500));
        when(mgr.getGame(from)).thenReturn(game);
        when(mgr.getGame(lobbyLocation)).thenReturn(null);
        when(mgr.isLocationInLobby(from)).thenReturn(false);
        when(mgr.isLocationInLobby(lobbyLocation)).thenReturn(true);
        when(player.isOp()).thenReturn(true);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, lobbyLocation);
        ptl.onTeleport(event);

        // OP players teleport immediately
        verify(player, never()).sendMessage(ChatColor.RED + Lang.teleportDoNotMove.replace("[number]", String.valueOf(Settings.teleportDelay)));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport into game - should restore inventory and enter region.
     */
    @Test
    void testOnTeleportIntoGame() {
        when(mgr.getGame(gameLocation)).thenReturn(game);
        when(mgr.isLocationInLobby(location)).thenReturn(false);
        when(game.hasPlayer(player)).thenReturn(true);
        when(store.getInventory(player, GAME_NAME)).thenReturn(gameLocation);
        when(gameRegion.findSafeSpot(gameLocation, 20)).thenReturn(gameLocation);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, location, gameLocation);
        ptl.onTeleport(event);

        verify(store).getInventory(player, GAME_NAME);
        verify(gameRegion).enter(player);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport into game for minigame mode - should give starting kit.
     */
    @Test
    void testOnTeleportIntoMinigame() {
        when(mgr.getGame(gameLocation)).thenReturn(game);
        when(mgr.isLocationInLobby(location)).thenReturn(false);
        when(game.hasPlayer(player)).thenReturn(true);
        when(game.getGamemode()).thenReturn("minigame");
        when(store.getInventory(player, GAME_NAME)).thenReturn(gameLocation);
        when(gameRegion.findSafeSpot(gameLocation, 20)).thenReturn(gameLocation);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, location, gameLocation);
        ptl.onTeleport(event);

        verify(gameRegion).enter(player);
        verify(game).giveStartingKit(player);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport into game when player is not part of game - should redirect to lobby.
     */
    @Test
    void testOnTeleportIntoGameNotInGame() {
        when(mgr.getGame(gameLocation)).thenReturn(game);
        when(mgr.isLocationInLobby(location)).thenReturn(false);
        when(game.hasPlayer(player)).thenReturn(false);
        when(player.isOp()).thenReturn(false);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, location, gameLocation);
        ptl.onTeleport(event);

        verify(player).sendMessage(any(Component.class));
        assertEquals(lobbyLocation, event.getTo());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport into lobby - should restore lobby inventory.
     */
    @Test
    void testOnTeleportIntoLobby() {
        when(mgr.isLocationInLobby(lobbyLocation)).thenReturn(true);
        when(mgr.isLocationInLobby(location)).thenReturn(false);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, location, lobbyLocation);
        ptl.onTeleport(event);

        verify(store).getInventory(player, "Lobby");
        verify(lobby).enterLobby(player);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport from lobby to game - should store lobby inventory and exit lobby.
     */
    @Test
    void testOnTeleportFromLobbyToGame() {
        when(mgr.isLocationInLobby(lobbyLocation)).thenReturn(true);
        when(mgr.isLocationInLobby(gameLocation)).thenReturn(false);
        when(mgr.getGame(gameLocation)).thenReturn(game);
        when(game.hasPlayer(player)).thenReturn(true);
        when(store.getInventory(player, GAME_NAME)).thenReturn(gameLocation);
        when(gameRegion.findSafeSpot(gameLocation, 20)).thenReturn(gameLocation);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, lobbyLocation, gameLocation);
        ptl.onTeleport(event);

        verify(store).storeInventory(player, "Lobby", lobbyLocation);
        verify(lobby).exit(player);
        verify(gameRegion).enter(player);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport within lobby - should be ignored.
     */
    @Test
    void testOnTeleportWithinLobby() {
        Location from = mock(Location.class);
        Location to = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(to.getWorld()).thenReturn(world);
        when(mgr.isLocationInLobby(from)).thenReturn(true);
        when(mgr.isLocationInLobby(to)).thenReturn(true);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, to);
        ptl.onTeleport(event);

        assertFalse(event.isCancelled());
        verify(store, never()).storeInventory(any(), anyString(), any());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#setDirectTeleportPlayer(java.util.UUID)}.
     * Tests setting direct teleport player - allows immediate teleport without delay.
     */
    @Test
    void testSetDirectTeleportPlayer() {
        UUID testUUID = UUID.randomUUID();
        ptl.setDirectTeleportPlayer(testUUID);

        // Verify that the player is added to direct teleport list by testing teleport behavior
        when(player.getUniqueId()).thenReturn(testUUID);
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(500, 64, 500));
        when(mgr.getGame(from)).thenReturn(game);
        when(mgr.getGame(lobbyLocation)).thenReturn(null);
        when(mgr.isLocationInLobby(from)).thenReturn(false);
        when(mgr.isLocationInLobby(lobbyLocation)).thenReturn(true);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, lobbyLocation);
        ptl.onTeleport(event);

        // Should teleport immediately without delay
        verify(gameRegion).exit(player);
        verify(store).storeInventory(player, GAME_NAME, from);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport from game during game restart - should allow immediate teleport.
     */
    @Test
    void testOnTeleportDuringGameRestart() {
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(500, 64, 500));
        when(mgr.getGame(from)).thenReturn(game);
        when(mgr.getGame(lobbyLocation)).thenReturn(null);
        when(mgr.isLocationInLobby(from)).thenReturn(false);
        when(mgr.isLocationInLobby(lobbyLocation)).thenReturn(true);
        when(game.isGameRestart()).thenReturn(true);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, lobbyLocation);
        ptl.onTeleport(event);
        verify(gameRegion).exit(player);
        verify(store).storeInventory(player, GAME_NAME, from);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerTeleportListener#onTeleport(org.bukkit.event.player.PlayerTeleportEvent)}.
     * Tests teleport from game when game is over - should allow immediate teleport.
     */
    @Test
    void testOnTeleportWhenGameIsOver() {
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(500, 64, 500));
        when(mgr.getGame(from)).thenReturn(game);
        when(mgr.getGame(lobbyLocation)).thenReturn(null);
        when(mgr.isLocationInLobby(from)).thenReturn(false);
        when(mgr.isLocationInLobby(lobbyLocation)).thenReturn(true);
        when(game.isOver()).thenReturn(true);

        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, lobbyLocation);
        ptl.onTeleport(event);

        verify(gameRegion).exit(player);
        verify(store).storeInventory(player, GAME_NAME, from);
    }

}
