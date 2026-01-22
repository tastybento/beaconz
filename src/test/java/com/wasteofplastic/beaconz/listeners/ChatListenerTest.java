package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.wasteofplastic.beaconz.game.Scorecard;
import com.wasteofplastic.beaconz.config.Settings;

/**
 * Tests for {@link ChatListener} covering team chat routing, spying, and broadcast fallbacks.
 */
class ChatListenerTest extends CommonTestBase {

    private ChatListener listener;
    private MockedStatic<Bukkit> mockedBukkit;
    private BukkitScheduler scheduler;

    @BeforeEach
    void setUpChat() {
        scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        mockedBukkit = Mockito.mockStatic(Bukkit.class, Mockito.RETURNS_DEEP_STUBS);
        mockedBukkit.when(Bukkit::getMinecraftVersion).thenReturn("1.21.10");
        mockedBukkit.when(Bukkit::getBukkitVersion).thenReturn("");
        mockedBukkit.when(Bukkit::getServer).thenReturn(server);
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

        listener = new ChatListener(plugin);
        Settings.teamChat = true; // default enabled for tests that expect scheduling
    }

    @AfterEach
    void tearDownChat() {
        if (mockedBukkit != null) {
            mockedBukkit.close();
        }
    }

    /** Construction sanity. */
    @Test
    void testChatListenerConstructs() {
        assertNotNull(listener);
    }

    /**
     * onChat returns early when either beacon world or player is null. Ensures no cancellation/scheduling occurs.
     */
    @Test
    void testOnChatNullWorldOrPlayer() {
        Settings.teamChat = true;
        when(plugin.getBeaconzWorld()).thenReturn(null); // null world triggers early return
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);
        listener.onChat(event);
        verify(event, never()).setCancelled(true);
        verify(scheduler, never()).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * onChat does nothing when team chat is disabled or player is in another world (no cancel, no schedule).
     */
    @Test
    void testOnChatTeamChatDisabledOrOtherWorld() {
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(player.getWorld()).thenReturn(world);
        Settings.teamChat = false;
        listener.onChat(event);
        verify(event, never()).setCancelled(anyBoolean());
        verify(scheduler, never()).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * onChat in Beaconz world with team chat enabled cancels the event and schedules teamChat runnable.
     */
    @Test
    void testOnChatSchedulesTeamChat() {
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn("hello");
        when(player.getWorld()).thenReturn(world);

        listener.onChat(event);

        verify(event).setCancelled(true);
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
    }

    /**
     * teamChat: player in team with another online member -> team-only delivery and spy notification.
     */
    @Test
    void testTeamChatTeamMembersOnlineWithSpy() {
        // Setup team membership
        Scorecard sc = mock(Scorecard.class);
        when(mgr.getSC(player)).thenReturn(sc);
        when(sc.getTeam(player)).thenReturn(team);
        when(team.getDisplayName()).thenReturn("TeamA");

        // Team members: self + another online member
        PlayerMock teammate = new PlayerMock(server, "Mate", UUID.randomUUID());
        server.addPlayer(teammate);
        when(team.getPlayers()).thenReturn(Set.of(player, teammate));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getDisplayName()).thenReturn("Sender");

        // Spy setup
        UUID spyId = UUID.randomUUID();
        PlayerMock spy = new PlayerMock(server, "Spy", spyId);        
        server.addPlayer(spy);
        listener.toggleSpy(spyId);

        // Capture and execute runnable
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);
        when(player.getWorld()).thenReturn(world);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn("msg");
        var runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        listener.onChat(event);
        verify(scheduler).runTask(any(), runnableCaptor.capture());
        runnableCaptor.getValue().run();

        // Verify teammate got team chat (spy gets spy tag)
        teammate.assertSaid(ChatColor.LIGHT_PURPLE + "[TeamA]<Sender> msg");
        spy.assertSaid(ChatColor.RED + "[TCSpy] " + ChatColor.WHITE + "msg");
    }

    /**
     * teamChat: player in team with no other online members -> broadcast to everyone except sender.
     */
    @Test
    void testTeamChatNoOtherMembersBroadcasts() {
        Scorecard sc = mock(Scorecard.class);
        when(mgr.getSC(player)).thenReturn(sc);
        when(sc.getTeam(player)).thenReturn(team);
        when(team.getDisplayName()).thenReturn("TeamA");
        when(team.getPlayers()).thenReturn(Set.of(player));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getDisplayName()).thenReturn("Sender");

        PlayerMock other = new PlayerMock(server, "Other", UUID.randomUUID());
        server.addPlayer(other);

        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);

        when(player.getWorld()).thenReturn(world);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn("solo");
        var runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        listener.onChat(event);
        verify(scheduler).runTask(any(), runnableCaptor.capture());
        runnableCaptor.getValue().run();

        other.assertSaid(ChatColor.LIGHT_PURPLE + "[TeamA]<Sender> solo");
    }

    /**
     * teamChat: player not in a team -> default broadcast using event format.
     */
    @Test
    void testTeamChatPlayerWithoutTeam() {
        Scorecard sc = mock(Scorecard.class);
        when(mgr.getSC(player)).thenReturn(sc);
        when(sc.getTeam(player)).thenReturn(null);
        when(player.getDisplayName()).thenReturn("Sender");

        PlayerMock other = new PlayerMock(server, "Other", UUID.randomUUID());
        server.addPlayer(other);

        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);
        when(player.getWorld()).thenReturn(world);
        when(event.getPlayer()).thenReturn(player);
        when(event.getMessage()).thenReturn("global");
        when(event.getFormat()).thenReturn("SenderFormat");
        var runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        listener.onChat(event);
        verify(scheduler).runTask(any(), runnableCaptor.capture());
        runnableCaptor.getValue().run();

        other.assertSaid("SenderFormat: global");
    }
}

