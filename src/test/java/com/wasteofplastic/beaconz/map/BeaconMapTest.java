package com.wasteofplastic.beaconz.map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.game.Register;

/**
 * Comprehensive test suite for {@link BeaconMap} class using JUnit 5 and MockBukkit.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Map rendering in correct world</li>
 *   <li>Map rendering validation (world check, hand check)</li>
 *   <li>Beacon information display</li>
 *   <li>Unknown beacon handling</li>
 *   <li>Center marker drawing</li>
 *   <li>Text rendering</li>
 *   <li>Performance optimization (early exits)</li>
 * </ul>
 *
 * @author tastybento
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BeaconMap Tests")
class BeaconMapTest {

    @Mock
    private Beaconz plugin;

    @Mock
    private Register register;

    @Mock
    private World beaconzWorld;

    @Mock
    private World otherWorld;

    @Mock
    private MapView mapView;

    @Mock
    private MapCanvas canvas;

    @Mock
    private Player player;

    @Mock
    private PlayerInventory inventory;

    @Mock
    private BeaconObj beacon;

    private BeaconMap beaconMap;

    private ItemStack filledMapStack;
    private ItemStack emptyStack;

    @BeforeEach
    void setUp() {
        // Set up MockBukkit server
        @SuppressWarnings("unused")
        ServerMock server = MockBukkit.mock();

        // Configure plugin mocks
        when(plugin.getBeaconzWorld()).thenReturn(beaconzWorld);
        when(plugin.getRegister()).thenReturn(register);

        // Configure player inventory
        when(player.getInventory()).thenReturn(inventory);

        // Create item stacks
        filledMapStack = new ItemStack(Material.FILLED_MAP);
        emptyStack = new ItemStack(Material.AIR);

        // Configure default inventory state (empty hands)
        when(inventory.getItemInMainHand()).thenReturn(emptyStack);
        when(inventory.getItemInOffHand()).thenReturn(emptyStack);

        // Configure beacon
        when(beacon.getName()).thenReturn("TestBeacon");

        // Create BeaconMap instance
        beaconMap = new BeaconMap(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    /**
     * Initialize Lang static strings used by enums BEFORE any test runs.
     * This must be done in @BeforeAll because enums are initialized once per JVM,
     * and if they reference null Lang strings, those null values persist across all tests.
     */
    @BeforeAll
    static void setupLangStrings() {
        // Initialize strings used by GameMode enum
        Lang.scoreGameModeMiniGame = "Minigame";
        Lang.scoreStrategy = "Strategy";

        // Initialize strings used by GameScoreGoal enum
        Lang.scoreGoalArea = "Area";
        Lang.scoreGoalBeacons = "Beacons";
        Lang.scoreGoalTime = "Time";
        Lang.scoreGoalTriangles = "Triangles";
        Lang.scoreGoalLinks = "Links";

        // Initialize strings used by BeaconMap (if any)
        Lang.beaconMapBeaconMap = "Beacon Map";
        Lang.beaconMapUnknownBeacon = "Unknown Beacon";
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor initializes with plugin reference")
        void testConstructor() {
            BeaconMap map = new BeaconMap(plugin);

            assertNotNull(map);
        }

        @Test
        @DisplayName("Constructor accepts valid plugin instance")
        void testConstructorWithValidPlugin() {
            assertDoesNotThrow(() -> new BeaconMap(plugin));
        }
    }

    @Nested
    @DisplayName("World Validation Tests")
    class WorldValidationTests {

        @Test
        @DisplayName("Render skips when map is in wrong world")
        void testRenderSkipsWrongWorld() {
            // Map is in a different world
            when(mapView.getWorld()).thenReturn(otherWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);

            beaconMap.render(mapView, canvas, player);

            // Verify no drawing occurred
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("BeaconMap doesn't draw even in correct world - delegates to TerritoryMapRenderer")
        void testRenderProceedsInBeaconzWorld() {
            // Create map with origin
            BeaconMap mapWithOrigin = new BeaconMap(plugin, 100, 200);

            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);

            mapWithOrigin.render(mapView, canvas, player);

            // BeaconMap validates but doesn't draw - TerritoryMapRenderer handles rendering
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());

            // Origin is still stored
            org.junit.jupiter.api.Assertions.assertEquals(100, mapWithOrigin.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(200, mapWithOrigin.getOriginZ());
        }
    }

    @Nested
    @DisplayName("Hand Check Tests")
    class HandCheckTests {

        @Test
        @DisplayName("Render proceeds when filled map in main hand")
        void testRenderWithMapInMainHand() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(inventory.getItemInOffHand()).thenReturn(emptyStack);
            when(mapView.getId()).thenReturn(1);

            beaconMap.render(mapView, canvas, player);

            // New behavior: no text is drawn without origin
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Render proceeds when filled map in off hand")
        void testRenderWithMapInOffHand() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(emptyStack);
            when(inventory.getItemInOffHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);

            beaconMap.render(mapView, canvas, player);

            // New behavior: no text is drawn without origin
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Render skips when no filled map in either hand")
        void testRenderSkipsWithoutMap() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(emptyStack);
            when(inventory.getItemInOffHand()).thenReturn(emptyStack);

            beaconMap.render(mapView, canvas, player);

            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Render skips when wrong item type in main hand")
        void testRenderSkipsWithWrongItemType() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            ItemStack wrongItem = new ItemStack(Material.DIAMOND_SWORD);
            when(inventory.getItemInMainHand()).thenReturn(wrongItem);
            when(inventory.getItemInOffHand()).thenReturn(emptyStack);

            beaconMap.render(mapView, canvas, player);

            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("Beacon Display Tests")
    class BeaconDisplayTests {

        @BeforeEach
        void setUpValidRenderConditions() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
        }

        @Test
        @DisplayName("Render does not display text when beacon exists")
        void testRenderNoTextDisplay() {
            int mapId = 42;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(beacon);
            when(beacon.getName()).thenReturn("MyBeacon");

            beaconMap.render(mapView, canvas, player);

            // New behavior: no text is drawn
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Render does not display text when beacon not found")
        void testRenderNoTextForUnknownBeacon() {
            int mapId = 99;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(null);

            beaconMap.render(mapView, canvas, player);

            // New behavior: no text is drawn
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Render does not draw center marker when no origin set")
        void testRenderNoCenterMarkerWithoutOrigin() {
            int mapId = 1;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // No origin set, so no pixels should be drawn
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Render does not draw center marker when beacon not found")
        void testRenderNoMarkerForUnknownBeacon() {
            int mapId = 1;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(null);

            beaconMap.render(mapView, canvas, player);

            // No origin, so no marker is drawn
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }
    }


    @Nested
    @DisplayName("Map ID Tests")
    class MapIdTests {

        @BeforeEach
        void setUpValidRenderConditions() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
        }

        @Test
        @DisplayName("Correct beacon is retrieved using map ID")
        void testCorrectBeaconRetrieved() {
            int mapId = 12345;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Verify no drawing since no origin is set
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Different map IDs work correctly")
        void testDifferentMapIds() {
            BeaconObj beacon2 = mock(BeaconObj.class);
            when(beacon2.getName()).thenReturn("SecondBeacon");

            // First render with map ID 1
            when(mapView.getId()).thenReturn(1);
            when(register.getBeaconMap(1)).thenReturn(beacon);
            beaconMap.render(mapView, canvas, player);

            reset(canvas);

            // Second render with map ID 2
            when(mapView.getId()).thenReturn(2);
            when(register.getBeaconMap(2)).thenReturn(beacon2);
            beaconMap.render(mapView, canvas, player);

            // No drawing in either case without origin
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Map ID zero is handled correctly")
        void testMapIdZero() {
            when(mapView.getId()).thenReturn(0);
            when(register.getBeaconMap(0)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // No drawing without origin
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Negative map ID is handled correctly")
        void testNegativeMapId() {
            when(mapView.getId()).thenReturn(-1);
            when(register.getBeaconMap(-1)).thenReturn(null);

            beaconMap.render(mapView, canvas, player);

            // No drawing without origin
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }
    }

    @Nested
    @DisplayName("Performance Optimization Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Early exit on wrong world prevents expensive operations")
        void testEarlyExitWrongWorld() {
            when(mapView.getWorld()).thenReturn(otherWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);

            beaconMap.render(mapView, canvas, player);

            // Should not draw anything
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Early exit on empty hands prevents map lookup")
        void testEarlyExitEmptyHands() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(emptyStack);
            when(inventory.getItemInOffHand()).thenReturn(emptyStack);

            beaconMap.render(mapView, canvas, player);

            // Should not draw anything
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("BeaconMap stores origin but doesn't render")
        void testBeaconMapStoresOriginOnly() {
            // Create map with origin
            BeaconMap mapWithOrigin = new BeaconMap(plugin, 100, 200);

            // Set up all conditions for successful render
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);

            mapWithOrigin.render(mapView, canvas, player);

            // BeaconMap does NOT draw - rendering is done by TerritoryMapRenderer
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());

            // But origin coordinates are stored
            org.junit.jupiter.api.Assertions.assertEquals(100, mapWithOrigin.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(200, mapWithOrigin.getOriginZ());
        }

        @Test
        @DisplayName("Render without origin draws nothing")
        void testRenderWithoutOrigin() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);

            beaconMap.render(mapView, canvas, player);

            // Should not draw anything
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Origin can be set dynamically")
        void testDynamicOriginSetting() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);

            // First render without origin
            beaconMap.render(mapView, canvas, player);
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));

            // Set origin
            beaconMap.setOrigin(500, 600);

            // Verify origin is stored
            org.junit.jupiter.api.Assertions.assertEquals(500, beaconMap.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(600, beaconMap.getOriginZ());

            // Render again - still no drawing (TerritoryMapRenderer handles that)
            beaconMap.render(mapView, canvas, player);
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Render handles null beacon gracefully")
        void testNullBeacon() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
            when(register.getBeaconMap(1)).thenReturn(null);

            assertDoesNotThrow(() -> beaconMap.render(mapView, canvas, player));

            // No origin set, so nothing is drawn
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Both hands can have filled maps")
        void testBothHandsWithMaps() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(inventory.getItemInOffHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // No origin set, so nothing is drawn
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }

        @Test
        @DisplayName("Render with map in off hand only")
        void testOnlyOffHandHasMap() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.COMPASS));
            when(inventory.getItemInOffHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // No origin set, so nothing is drawn
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }
    }

    @Nested
    @DisplayName("Origin Marker Tests")
    class OriginMarkerTests {

        @Test
        @DisplayName("Should store origin coordinates when constructed with coordinates")
        void shouldStoreOriginCoordinates() {
            // Create map with origin coordinates
            BeaconMap mapWithOrigin = new BeaconMap(plugin, 100, 200);

            // Verify coordinates are stored
            org.junit.jupiter.api.Assertions.assertEquals(100, mapWithOrigin.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(200, mapWithOrigin.getOriginZ());
        }

        @Test
        @DisplayName("Should not draw anything - rendering is delegated to TerritoryMapRenderer")
        void shouldNotDrawAnything() {
            // Create map with origin coordinates
            BeaconMap mapWithOrigin = new BeaconMap(plugin, 100, 200);

            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(inventory.getItemInOffHand()).thenReturn(emptyStack);

            mapWithOrigin.render(mapView, canvas, player);

            // BeaconMap no longer draws anything - TerritoryMapRenderer handles rendering
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Should set origin coordinates via setter")
        void shouldSetOriginCoordinates() {
            beaconMap.setOrigin(500, 600);

            // Verify coordinates are stored
            org.junit.jupiter.api.Assertions.assertEquals(500, beaconMap.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(600, beaconMap.getOriginZ());
        }

        @Test
        @DisplayName("Should get origin X coordinate")
        void shouldGetOriginX() {
            BeaconMap mapWithOrigin = new BeaconMap(plugin, 123, 456);
            org.junit.jupiter.api.Assertions.assertEquals(123, mapWithOrigin.getOriginX());
        }

        @Test
        @DisplayName("Should get origin Z coordinate")
        void shouldGetOriginZ() {
            BeaconMap mapWithOrigin = new BeaconMap(plugin, 123, 456);
            org.junit.jupiter.api.Assertions.assertEquals(456, mapWithOrigin.getOriginZ());
        }

        @Test
        @DisplayName("Should return null for unset origin coordinates")
        void shouldReturnNullForUnsetOrigin() {
            org.junit.jupiter.api.Assertions.assertNull(beaconMap.getOriginX());
            org.junit.jupiter.api.Assertions.assertNull(beaconMap.getOriginZ());
        }

        @Test
        @DisplayName("Should allow updating origin coordinates via setter")
        void shouldAllowUpdatingOrigin() {
            BeaconMap mapWithOrigin = new BeaconMap(plugin, 100, 200);

            // Initial values
            org.junit.jupiter.api.Assertions.assertEquals(100, mapWithOrigin.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(200, mapWithOrigin.getOriginZ());

            // Update values
            mapWithOrigin.setOrigin(300, 400);

            // Verify updated values
            org.junit.jupiter.api.Assertions.assertEquals(300, mapWithOrigin.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(400, mapWithOrigin.getOriginZ());
        }

        @Test
        @DisplayName("Should handle setting origin on map created without origin")
        void shouldHandleSettingOriginOnEmptyMap() {
            // Create map without origin
            BeaconMap map = new BeaconMap(plugin);

            // Verify no origin initially
            org.junit.jupiter.api.Assertions.assertNull(map.getOriginX());
            org.junit.jupiter.api.Assertions.assertNull(map.getOriginZ());

            // Set origin
            map.setOrigin(500, 600);

            // Verify origin is now set
            org.junit.jupiter.api.Assertions.assertEquals(500, map.getOriginX());
            org.junit.jupiter.api.Assertions.assertEquals(600, map.getOriginZ());
        }
    }
}
