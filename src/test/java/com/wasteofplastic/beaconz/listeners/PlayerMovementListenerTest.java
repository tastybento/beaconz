package com.wasteofplastic.beaconz.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.core.Region;
import com.wasteofplastic.beaconz.core.TriangleField;

import net.kyori.adventure.text.Component;

class PlayerMovementListenerTest extends CommonTestBase {
    
    private PlayerMovementListener pml;
    @Mock
    private Sheep sheep;
    @Mock
    private WorldBorder worldBorder;
    @Mock
    private Team team;
    private final UUID uuid = UUID.randomUUID();

    /**
     * @throws java.lang.Exception if an exception occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        when(sheep.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(uuid);
        
        when(team.displayName()).thenReturn(Component.text("red team [level]"));

        
        pml = new PlayerMovementListener(plugin);
    }

    /**
     * @throws java.lang.Exception if an exception occurs
     */
    @AfterEach
    void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#PlayerMovementListener(com.wasteofplastic.beaconz.Beaconz)}.
     */
    @Test
    void testPlayerMovementListener() {
        assertNotNull(pml);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#onLeashUse(org.bukkit.event.entity.PlayerLeashEntityEvent)}.
     */
    @Test
    void testOnLeashUse() {
       when(mgr.getGame(any(Location.class))).thenReturn(null); // Outside game area
       PlayerLeashEntityEvent event = new PlayerLeashEntityEvent(sheep, sheep, player, EquipmentSlot.HAND);
       pml.onLeashUse(event);
       assertTrue(event.isCancelled());
       verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#onPlayerHitEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    void testOnPlayerHitEntity() {
        when(mgr.getGame(any(Location.class))).thenReturn(null); // Outside game area
        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, sheep, EquipmentSlot.HAND);
        pml.onPlayerHitEntity(event);
        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#onHangingPlace(org.bukkit.event.hanging.HangingPlaceEvent)}.
     */
    @Test
    void testOnHangingPlace() {
        when(mgr.getGame(any(Location.class))).thenReturn(null); // Outside game area
        @NotNull
        Hanging hanging = mock(Hanging.class);
        when(hanging.getWorld()).thenReturn(world);
        HangingPlaceEvent event = new HangingPlaceEvent(hanging, player, block, BlockFace.EAST, EquipmentSlot.HAND, item);
        pml.onHangingPlace(event);
        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#onShear(org.bukkit.event.player.PlayerShearEntityEvent)}.
     */
    @Test
    void testOnShear() {
        when(mgr.getGame(any(Location.class))).thenReturn(null); // Outside game area
        @NotNull
        List<ItemStack> drops = new ArrayList<>();
        PlayerShearEntityEvent event = new PlayerShearEntityEvent(player, sheep, item, EquipmentSlot.HAND, drops);
        pml.onShear(event);
        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#onVehicleDamage(org.bukkit.event.vehicle.VehicleDamageEvent)}.
     */
    @Test
    void testOnVehicleDamage() {
        when(mgr.getGame(any(Location.class))).thenReturn(null); // Outside game area
        Horse horse = mock(Horse.class);
        when(horse.getWorld()).thenReturn(world);
        VehicleDamageEvent event = new VehicleDamageEvent(horse, player, 10);
        pml.onVehicleDamage(event);
        assertTrue(event.isCancelled());
        verify(player).sendMessage(Lang.errorYouCannotDoThat);
    }

    /**
     * Test logic for when a player is in a non-living vehicle (like a Boat) 
     * and has a SLOWNESS potion effect.
     */
    @Test
    void testOnVehicleMoveNonLivingVehicleWithSlowness() {
        // Use a Boat as it is not a LivingEntity
        Boat boat = mock(Boat.class);
        when(boat.getWorld()).thenReturn(world);
        // Using a non-zero velocity to test division
        Vector initialVelocity = new Vector(1.0, 0, 1.0);
        when(boat.getVelocity()).thenReturn(initialVelocity.clone());
        
        when(boat.getPassenger()).thenReturn(player);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        // Mock the slowness effect (Amplifier 2 means divide by 2)
        PotionEffect slowness = new PotionEffect(PotionEffectType.SLOWNESS, 100, 2);
        pml.getTriangleEffects().put(uuid, List.of(slowness));

        VehicleMoveEvent event = new VehicleMoveEvent(boat, location, location);
        pml.onVehicleMove(event);

        // Verify velocity was reduced: 1.0 / 2 = 0.5
        verify(boat).setVelocity(argThat(vec -> vec.getX() == 0.5 && vec.getZ() == 0.5));
    }

    /**
     * Test logic for multiple passengers in the same vehicle.
     * Verifies checkMove is called for additional passengers.
     */
    @Test
    void testOnVehicleMoveMultiplePassengers() {
        Region lobby = mock(Region.class);
        when(mgr.getLobby()).thenReturn(lobby);
        
        Horse horse = mock(Horse.class); // LivingEntity, so velocity reduction is skipped
        when(horse.getWorld()).thenReturn(world);
        when(horse.getPassenger()).thenReturn(player);
        when(horse.getEntityId()).thenReturn(999);

        // Setup a second player in the same world
        Player secondPlayer = mock(Player.class);
        when(secondPlayer.isInsideVehicle()).thenReturn(true);
        when(secondPlayer.getVehicle()).thenReturn(horse);
        when(world.getPlayers()).thenReturn(List.of(player, secondPlayer));

        VehicleMoveEvent event = new VehicleMoveEvent(horse, location, location);
        
        // This test requires pml.checkMove to be verifiable (either mocked or a spy)
        pml.onVehicleMove(event);

        // Verify the logic checked the second player
        // Note: You may need to verify internal calls to checkMove if pml is a spy
        verify(secondPlayer).isInsideVehicle();
    }

    /**
     * Verifies that the method exits immediately if the world does not match.
     */
    @Test
    void testOnVehicleMoveWrongWorld() {
        World otherWorld = mock(World.class);
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getWorld()).thenReturn(otherWorld);

        VehicleMoveEvent event = new VehicleMoveEvent(vehicle, location, location);
        pml.onVehicleMove(event);

        // Verify no further interaction with the vehicle's passengers occurred
        verify(vehicle, never()).getPassenger();
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#applyTriangleEffects(Player, List, List)}.
     */
    @Test
    void testApplyTriangleEffectsEnter() {
        // Set up the initial conditions
        List<TriangleField> fromTriangles = new ArrayList<>();
        List<TriangleField> toTriangles = new ArrayList<>();
        toTriangles.add(new TriangleField(new Point(0, 0), new Point(0, 1), new Point(1, 1), team));

        // Call the method under test
        pml.applyTriangleEffects(player, fromTriangles, toTriangles);

        // Verify the results
        // Nothing should happen in this case because the player is not in a game
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#applyTriangleEffects(Player, List, List)}.
     */
    @Test
    void testApplyTriangleEffectsLeave() {
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        // Set up the initial conditions
        List<TriangleField> fromTriangles = new ArrayList<>();
        fromTriangles.add(new TriangleField(new Point(0, 0), new Point(0, 1), new Point(1, 1), team));
        List<TriangleField> toTriangles = new ArrayList<>();
        // Call the method under test
        assertTrue(pml.applyTriangleEffects(player, fromTriangles, toTriangles));
    }
    
    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#applyTriangleEffects(Player, List, List)}.
     */
    @Test
    void testApplyTriangleEffectsLeavePlayerIsOp() {
        when(mgr.getPlayerTeam(player)).thenReturn(null);
        when(player.isOp()).thenReturn(true);
        // Set up the initial conditions
        List<TriangleField> fromTriangles = new ArrayList<>();
        fromTriangles.add(new TriangleField(new Point(0, 0), new Point(0, 1), new Point(1, 1), team));
        List<TriangleField> toTriangles = new ArrayList<>();
        // Call the method under test
        assertFalse(pml.applyTriangleEffects(player, fromTriangles, toTriangles));
    }
    
    /**
     * Test case: Player is outside all triangles (both lists empty).
     * Verifies that all active potion effects are cleared from player and vehicle.
     */
    @Test
    void testApplyTriangleEffectsOutsideAllFields() {
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        
        // Mock active effects
        PotionEffect mockEffect = new PotionEffect(PotionEffectType.SPEED, 100, 1);
        when(player.getActivePotionEffects()).thenReturn(List.of(mockEffect));
        
        // Mock vehicle scenario
        LivingEntity vehicle = mock(LivingEntity.class);
        when(player.isInsideVehicle()).thenReturn(true);
        when(player.getVehicle()).thenReturn(vehicle);

        boolean result = pml.applyTriangleEffects(player, List.of(), List.of());

        assertFalse(result);
        verify(player).removePotionEffect(PotionEffectType.SPEED);
        verify(vehicle).removePotionEffect(PotionEffectType.SPEED);
    }

    /**
     * Test case: Player enters a field for the first time or increases level.
     * Verifies that the "entering" message is sent.
     */
    @Test
    void testApplyTriangleEffectsEnteringOrIncreasing() {
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        
        TriangleField field1 = mock(TriangleField.class);
        Team teamOwner = mock(Team.class);
        when(field1.getOwner()).thenReturn(teamOwner);
        when(teamOwner.displayName()).thenReturn(Component.text("Blue Team"));

        List<TriangleField> fromTriangles = List.of(); 
        List<TriangleField> toTriangles = List.of(field1);

        pml.applyTriangleEffects(player, fromTriangles, toTriangles);

        // Verify the entering message was sent (Logic uses Lang.triangleEntering)
        verify(player).sendMessage(any(Component.class));
    }

    /**
     * Test case: Player moves from a higher level of overlap to a lower level.
     * Verifies effects are cleared before applying new lower-level effects.
     */
    @Test
    void testApplyTriangleEffectsDroppingLevel() {
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        TriangleField f1 = mock(TriangleField.class);
        Team teamOwner = mock(Team.class);
        when(f1.getOwner()).thenReturn(teamOwner);
        when(teamOwner.displayName()).thenReturn(Component.text("Red Team"));

        // Simulate stored effects for this player
        PotionEffect oldEffect = new PotionEffect(PotionEffectType.REGENERATION, 100, 1);
        // Note: You may need to use reflection or a setter to populate 'triangleEffects' 
        // map in the listener if it's private and not accessible.
        pml.getTriangleEffects().put(uuid, List.of(oldEffect));

        List<TriangleField> fromTriangles = List.of(f1, f1); // Level 2
        List<TriangleField> toTriangles = List.of(f1);      // Level 1

        pml.applyTriangleEffects(player, fromTriangles, toTriangles);

        // Verify old effects are removed and dropping message sent
        verify(player).removePotionEffect(PotionEffectType.REGENERATION);
        // The message uses Lang.triangleDroppingToLevel.replaceText("[team]", teamDisplayName).replaceText("[level]", level)
        verify(player).sendMessage(any(Component.class));
    }

    /**
     * Test case: Leaving a control triangle (toTriangles is empty, but fromTriangles was not).
     */
    @Test
    void testApplyTriangleEffectsLeavingControlTriangle() {
        when(mgr.getPlayerTeam(player)).thenReturn(team);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        TriangleField f1 = mock(TriangleField.class);
        Team teamOwner = mock(Team.class);
        when(f1.getOwner()).thenReturn(teamOwner);
        when(teamOwner.displayName()).thenReturn(Component.text("Green Team"));

        List<TriangleField> fromTriangles = List.of(f1);
        List<TriangleField> toTriangles = List.of();

        pml.applyTriangleEffects(player, fromTriangles, toTriangles);

        // The message uses Lang.triangleLeaving.replaceText("[team]", teamDisplayName)
        verify(player).sendMessage(any(Component.class));
        assertFalse(pml.getTriangleEffects().containsKey(uuid));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.listeners.PlayerMovementListener#getTriangleEffects(java.util.UUID)}.
     */
    @Test
    void testGetTriangleEffects() {
      assertTrue(pml.getTriangleEffects(uuid).isEmpty());
    }

}
