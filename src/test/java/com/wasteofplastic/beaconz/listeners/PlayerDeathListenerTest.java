package com.wasteofplastic.beaconz.listeners;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.wasteofplastic.beaconz.Region;

class PlayerDeathListenerTest extends CommonTestBase {
    
    private static final String GAMENAME = "GameName";
    private PlayerDeathListener pdl;
    @Mock
    private Region lobby;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        when(mgr.getLobby()).thenReturn(lobby);
        when(location.getWorld()).thenReturn(world);
        when(location.clone()).thenReturn(location);
        when(lobby.getSpawnPoint()).thenReturn(location);
        // Player starts in the lobby
        when(lobby.isPlayerInRegion(player)).thenReturn(true);
        // Game
        when(mgr.getGame(location)).thenReturn(game);
        pdl = new PlayerDeathListener(plugin);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerDeathListener#PlayerDeathListener(com.wasteofplastic.beaconz.Beaconz)}.
     */
    @Test
    void testPlayerDeathListener() {
        assertNotNull(pdl);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerDeathListener#onDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnDeathInLobby() {
        @NotNull
        List<ItemStack> drops = List.of();
        int newExp = 10;
        @SuppressWarnings("removal")
        PlayerDeathEvent event = new PlayerDeathEvent(player, DamageSource.builder(DamageType.ARROW).build(), drops, 0, newExp, 0, 0, null);
        pdl.onDeath(event);
        verify(store).storeInventory(player, PlayerDeathListener.LOBBY, location);
        //verify(store).clearItems(player, PlayerDeathListener.LOBBY, location);
        verify(store).setExp(player, PlayerDeathListener.LOBBY, newExp);
        verify(store).setHealth(player, PlayerDeathListener.LOBBY, player.getAttribute(Attribute.MAX_HEALTH).getValue());
        verify(store).setFood(player, PlayerDeathListener.LOBBY,  20); 
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerDeathListener#onDeath(org.bukkit.event.entity.PlayerDeathEvent)}.
     */
    @Test
    void testOnDeathInGame() {
        when(lobby.isPlayerInRegion(player)).thenReturn(false);
        when(game.getName()).thenReturn(GAMENAME);
        when(game.getGamemode()).thenReturn("minigame");
        Team team = mock(Team.class);
        when(game.getScorecard()).thenReturn(scorecard);
        Region region = mock(Region.class);
        when(game.getRegion()).thenReturn(region);
        when(scorecard.getTeam(player)).thenReturn(team);     
        when(scorecard.getTeamSpawnPoint(team)).thenReturn(location);
        when(mgr.getGame(GAMENAME)).thenReturn(game);
        @NotNull
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(Material.ACACIA_BOAT));
        int newExp = 10;
        @SuppressWarnings("removal")
        PlayerDeathEvent event = new PlayerDeathEvent(player, DamageSource.builder(DamageType.ARROW).build(), drops, 0, newExp, 0, 0, null);
        pdl.onDeath(event);
        verify(store).storeInventory(player, GAMENAME, location);
        verify(store).clearItems(player, GAMENAME, location);
        verify(store).setExp(player, GAMENAME, newExp);
        verify(store).setHealth(player, GAMENAME, player.getAttribute(Attribute.MAX_HEALTH).getValue());
        verify(store).setFood(player, GAMENAME,  20); 
        assertTrue(drops.isEmpty());
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerDeathListener#onRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    void testOnRespawn() {
        @SuppressWarnings("removal")
        Location other = mock(Location.class);
        when(other.getWorld()).thenReturn(mock(World.class));
        PlayerRespawnEvent event = new PlayerRespawnEvent(player, other, false);
        pdl.onRespawn(event);
        assertNull(event.getRespawnLocation());
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerDeathListener#onRespawn(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    void testOnRespawnLobbyDeath() {
        this.testOnDeathInLobby();
        @SuppressWarnings("removal")
        PlayerRespawnEvent event = new PlayerRespawnEvent(player, location, false);
        pdl.onRespawn(event);
        assertEquals(location, event.getRespawnLocation());
        verify((store).getInventory(event.getPlayer(), PlayerDeathListener.LOBBY));
    }

}
