package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.google.common.collect.HashBiMap;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Messages;
import com.wasteofplastic.beaconz.Region;
import com.wasteofplastic.beaconz.TinyDB;

import net.kyori.adventure.text.Component;

/**
 * Tests for {@link PlayerJoinLeaveListener} covering join/leave flows into/out of the Beaconz world.
 * Each test documents the branch being exercised.
 */
class PlayerJoinLeaveListenerTest extends CommonTestBase {

    private PlayerJoinLeaveListener listener;
    private MockedStatic<com.wasteofplastic.beaconz.listeners.BeaconProtectionListener> protectionStatic;

    @BeforeEach
    void setUpListener() {
        listener = new PlayerJoinLeaveListener(plugin);
        // Mock TinyDB name store
        TinyDB nameStore = mock(TinyDB.class);
        when(plugin.getNameStore()).thenReturn(nameStore);
        // Game and scorecard
        when(game.getScorecard()).thenReturn(scorecard);
        // Lobby
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
    }

    @AfterEach
    void tearDownStatics() {
        if (protectionStatic != null) {
            protectionStatic.close();
        }
    }

    /** Sanity check: constructor builds. */
    @Test
    void testConstructor() {
        assertNotNull(listener);
    }

    /**
     * onJoin: player not in Beaconz world -> no lobby/game logic executed.
     */
    @Test
    void testOnJoinOtherWorld() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text(""));
        World otherWorld = mock(World.class);
        when(player.getWorld()).thenReturn(otherWorld);

        listener.onJoin(event);

        verify(mgr, never()).getGame(any(Team.class));
    }

    /**
     * onJoin: game null (no game) -> teleport to lobby and enter lobby.
     */
    @Test
    void testOnJoinNoGameSendsToLobby() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text(""));
        when(player.getWorld()).thenReturn(world);
        when(mgr.getGame(player.getLocation())).thenReturn(null);

        listener.onJoin(event);

        verify(mgr.getLobby()).tpToRegionSpawn(player, true);
        verify(mgr.getLobby()).enterLobby(player);
    }

    /**
     * onJoin: game over -> teleport to lobby and enter lobby.
     */
    @Test
    void testOnJoinGameOver() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text(""));
        when(player.getWorld()).thenReturn(world);
        when(mgr.getGame(player.getLocation())).thenReturn(game);
        when(game.isOver()).thenReturn(true);

        listener.onJoin(event);

        verify(mgr.getLobby()).tpToRegionSpawn(player, true);
        verify(mgr.getLobby()).enterLobby(player);
    }

    /**
     * onJoin: game restarting -> teleport to lobby and enter lobby.
     */
    @Test
    void testOnJoinGameRestart() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text(""));
        when(player.getWorld()).thenReturn(world);
        when(mgr.getGame(player.getLocation())).thenReturn(game);
        when(game.isOver()).thenReturn(false);
        when(game.isGameRestart()).thenReturn(true);

        listener.onJoin(event);

        verify(mgr.getLobby()).tpToRegionSpawn(player, true);
        verify(mgr.getLobby()).enterLobby(player);
    }

    /**
     * onJoin: game active but player not in a team -> send to lobby.
     */
    @Test
    void testOnJoinInGameNoTeam() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text(""));
        when(player.getWorld()).thenReturn(world);
        when(mgr.getGame(player.getLocation())).thenReturn(game);
        when(game.isOver()).thenReturn(false);
        when(game.isGameRestart()).thenReturn(false);
        when(scorecard.getTeam(player)).thenReturn(null);

        listener.onJoin(event);

        verify(mgr.getLobby()).tpToRegionSpawn(player, true);
        verify(mgr.getLobby()).enterLobby(player);
    }

    /**
     * onJoin: game active and player in a team -> join game at last location.
     */
    @Test
    void testOnJoinInGameWithTeam() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text(""));
        when(player.getWorld()).thenReturn(world);
        when(mgr.getGame(player.getLocation())).thenReturn(game);
        when(game.isOver()).thenReturn(false);
        when(game.isGameRestart()).thenReturn(false);
        when(game.getScorecard().getTeam(player)).thenReturn(team);

        listener.onJoin(event);

        verify(game).join(player, false);
        verify(mgr.getLobby(), never()).tpToRegionSpawn(any(), anyBoolean());
    }

    /**
     * onJoin: queued messages exist -> schedules news delivery and clears messages.
     */
    @Test
    void testOnJoinQueuedMessages() {
        // Mock messages store
        Messages messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);
        List<String> queued = List.of("msg1", "msg2");
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(messages.getMessages(uuid)).thenReturn(queued);
        when(player.getWorld()).thenReturn(world);
        when(mgr.getGame(player.getLocation())).thenReturn(null);

        // Scheduler capture
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);

        PlayerJoinEvent event = new PlayerJoinEvent(player, Component.text(""));
        listener.onJoin(event);
        // Advance the scheduler by 40 ticks
        server.getScheduler().performTicks(40L);
        verify(player).sendMessage(Lang.titleBeaconzNews);
        verify(messages).clearMessages(uuid);
    }

    /**
     * onLeave: always removes player from standingOn map and clears potion effects if in Beaconz world.
     */
    @Test
    void testOnLeaveRemovesEffectsAndStanding() {
        PlayerQuitEvent event = new PlayerQuitEvent(player, Component.text(""), QuitReason.DISCONNECTED);
        // Mock static BeaconProtectionListener.getStandingOn()
        protectionStatic = mockStatic(com.wasteofplastic.beaconz.listeners.BeaconProtectionListener.class);
        var standing = HashBiMap.create();
        UUID uuid = UUID.randomUUID();
        standing.put(uuid, new Object());
        when(player.getUniqueId()).thenReturn(uuid);
        protectionStatic.when(com.wasteofplastic.beaconz.listeners.BeaconProtectionListener::getStandingOn)
                .thenReturn(standing);

        // Potion effects clearing when in Beaconz world
        PotionEffect effect = mock(PotionEffect.class);
        when(player.getActivePotionEffects()).thenReturn(List.of(effect));
        when(player.getWorld()).thenReturn(world);
        when(mgr.getGame(player.getLocation())).thenReturn(null);

        listener.onLeave(event);

        assertFalse(standing.containsKey(uuid));
        verify(player).removePotionEffect(effect.getType());
    }
}
