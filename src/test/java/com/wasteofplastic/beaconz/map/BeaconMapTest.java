package com.wasteofplastic.beaconz.map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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

    private ServerMock server;

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
        server = MockBukkit.mock();

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
        @DisplayName("Render proceeds when map is in Beaconz world")
        void testRenderProceedsInBeaconzWorld() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Verify drawing occurred
            verify(canvas, atLeastOnce()).drawText(anyInt(), anyInt(), any(), anyString());
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

            verify(canvas, atLeastOnce()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Render proceeds when filled map in off hand")
        void testRenderWithMapInOffHand() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(emptyStack);
            when(inventory.getItemInOffHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);

            beaconMap.render(mapView, canvas, player);

            verify(canvas, atLeastOnce()).drawText(anyInt(), anyInt(), any(), anyString());
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
        @DisplayName("Render displays beacon name when beacon exists")
        void testRenderDisplaysBeaconName() {
            int mapId = 42;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(beacon);
            when(beacon.getName()).thenReturn("MyBeacon");

            beaconMap.render(mapView, canvas, player);

            // Verify title is drawn
            verify(canvas).drawText(eq(10), eq(10), any(), contains("Beacon Map"));

            // Verify beacon name is drawn
            verify(canvas).drawText(eq(10), eq(20), any(), contains("MyBeacon"));
        }

        @Test
        @DisplayName("Render displays unknown beacon when beacon not found")
        void testRenderDisplaysUnknownBeacon() {
            int mapId = 99;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(null);

            beaconMap.render(mapView, canvas, player);

            // Verify title is drawn
            verify(canvas).drawText(eq(10), eq(10), any(), contains("Beacon Map"));

            // Verify unknown beacon message is drawn
            verify(canvas).drawText(eq(10), eq(20), any(), contains("Unknown Beacon"));
        }

        @Test
        @DisplayName("Render draws center marker when beacon exists")
        void testRenderDrawsCenterMarker() {
            int mapId = 1;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Verify center marker (white pixel at 64, 64)
            verify(canvas).setPixelColor(64, 64, Color.WHITE);
        }

        @Test
        @DisplayName("Render does not draw center marker when beacon not found")
        void testRenderNoCenterMarkerForUnknownBeacon() {
            int mapId = 1;
            when(mapView.getId()).thenReturn(mapId);
            when(register.getBeaconMap(mapId)).thenReturn(null);

            beaconMap.render(mapView, canvas, player);

            // Verify no center marker is drawn
            verify(canvas, never()).setPixelColor(anyInt(), anyInt(), any(Color.class));
        }
    }

    @Nested
    @DisplayName("Text Rendering Tests")
    class TextRenderingTests {

        @BeforeEach
        void setUpValidRenderConditions() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
        }

        @Test
        @DisplayName("Title is rendered at correct position")
        void testTitlePosition() {
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Title should be at (10, 10)
            verify(canvas).drawText(eq(10), eq(10), any(), anyString());
        }

        @Test
        @DisplayName("Location text is rendered at correct position")
        void testLocationTextPosition() {
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Location should be at (10, 20)
            verify(canvas).drawText(eq(10), eq(20), any(), anyString());
        }

        @Test
        @DisplayName("Exactly two text lines are drawn when beacon exists")
        void testTwoTextLinesForBeacon() {
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Should draw exactly 2 text lines (title + location)
            verify(canvas, times(2)).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Exactly two text lines are drawn when beacon not found")
        void testTwoTextLinesForUnknownBeacon() {
            when(register.getBeaconMap(1)).thenReturn(null);

            beaconMap.render(mapView, canvas, player);

            // Should draw exactly 2 text lines (title + unknown message)
            verify(canvas, times(2)).drawText(anyInt(), anyInt(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("Center Marker Tests")
    class CenterMarkerTests {

        @BeforeEach
        void setUpValidRenderConditions() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
        }

        @Test
        @DisplayName("Center marker is white color")
        void testCenterMarkerColor() {
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            verify(canvas).setPixelColor(64, 64, Color.WHITE);
        }

        @Test
        @DisplayName("Center marker is at exact center coordinates")
        void testCenterMarkerPosition() {
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Map center is at (64, 64)
            verify(canvas).setPixelColor(eq(64), eq(64), any(Color.class));
        }

        @Test
        @DisplayName("Only one pixel is drawn for center marker")
        void testOnlyOnePixelDrawn() {
            when(register.getBeaconMap(1)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            // Should only draw one pixel
            verify(canvas, times(1)).setPixelColor(anyInt(), anyInt(), any(Color.class));
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

            verify(register).getBeaconMap(eq(mapId));
        }

        @Test
        @DisplayName("Different map IDs retrieve different beacons")
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

            // Verify second beacon name was used
            verify(canvas).drawText(eq(10), eq(20), any(), contains("SecondBeacon"));
        }

        @Test
        @DisplayName("Map ID zero is handled correctly")
        void testMapIdZero() {
            when(mapView.getId()).thenReturn(0);
            when(register.getBeaconMap(0)).thenReturn(beacon);

            beaconMap.render(mapView, canvas, player);

            verify(register).getBeaconMap(0);
            verify(canvas, atLeastOnce()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Negative map ID is handled correctly")
        void testNegativeMapId() {
            when(mapView.getId()).thenReturn(-1);
            when(register.getBeaconMap(-1)).thenReturn(null);

            beaconMap.render(mapView, canvas, player);

            verify(register).getBeaconMap(-1);
            verify(canvas).drawText(eq(10), eq(20), any(), contains("Unknown Beacon"));
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

            // Should not check inventory or map ID
            verify(mapView, never()).getId();
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }

        @Test
        @DisplayName("Early exit on empty hands prevents map lookup")
        void testEarlyExitEmptyHands() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(emptyStack);
            when(inventory.getItemInOffHand()).thenReturn(emptyStack);

            beaconMap.render(mapView, canvas, player);

            // Should not look up beacon or draw
            verify(mapView, never()).getId();
            verify(register, never()).getBeaconMap(anyInt());
            verify(canvas, never()).drawText(anyInt(), anyInt(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Complete render flow with all conditions met")
        void testCompleteRenderFlow() {
            // Set up all conditions for successful render
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(100);
            when(register.getBeaconMap(100)).thenReturn(beacon);
            when(beacon.getName()).thenReturn("IntegrationBeacon");

            beaconMap.render(mapView, canvas, player);

            // Verify complete rendering occurred
            verify(canvas).drawText(eq(10), eq(10), any(), contains("Beacon Map"));
            verify(canvas).drawText(eq(10), eq(20), any(), contains("IntegrationBeacon"));
            verify(canvas).setPixelColor(64, 64, Color.WHITE);
        }

        @Test
        @DisplayName("Render handles beacon with special characters in name")
        void testBeaconWithSpecialCharacters() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
            when(register.getBeaconMap(1)).thenReturn(beacon);
            when(beacon.getName()).thenReturn("Beacon-123_Test!");

            beaconMap.render(mapView, canvas, player);

            verify(canvas).drawText(eq(10), eq(20), any(), contains("Beacon-123_Test!"));
        }

        @Test
        @DisplayName("Render handles beacon with empty name")
        void testBeaconWithEmptyName() {
            when(mapView.getWorld()).thenReturn(beaconzWorld);
            when(inventory.getItemInMainHand()).thenReturn(filledMapStack);
            when(mapView.getId()).thenReturn(1);
            when(register.getBeaconMap(1)).thenReturn(beacon);
            when(beacon.getName()).thenReturn("");

            beaconMap.render(mapView, canvas, player);

            // Should still render, just with empty name
            verify(canvas).drawText(eq(10), eq(20), any(), anyString());
            verify(canvas).setPixelColor(64, 64, Color.WHITE);
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

            verify(canvas).drawText(eq(10), eq(20), any(), contains("Unknown Beacon"));
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

            // Should still render normally
            verify(canvas, times(2)).drawText(anyInt(), anyInt(), any(), anyString());
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

            verify(canvas, atLeastOnce()).drawText(anyInt(), anyInt(), any(), anyString());
        }
    }
}
