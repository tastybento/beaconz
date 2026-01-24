package com.wasteofplastic.beaconz.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.game.Game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Comprehensive test suite for {@link Region} class.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Region construction and initialization</li>
 *   <li>Corner coordinate normalization</li>
 *   <li>Containment checks (points, beacons, players, locations)</li>
 *   <li>Center and radius calculations</li>
 *   <li>Spawn point management</li>
 *   <li>Location safety checks</li>
 *   <li>BlockFace to yaw conversion</li>
 * </ul>
 *
 * @author tastybento
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Region Tests")
class RegionTest {

    @Mock
    private Beaconz plugin;

    @Mock
    private World beaconzWorld;

    @Mock
    private Game game;

    private Point2D[] corners;

    /**
     * Initialize Lang static fields used by Region class BEFORE any test runs.
     * Region constructor initializes a Title field that references Lang.titleWelcome,
     * Lang.titleSubTitle, and their colors.
     */
    @BeforeAll
    static void setupLangFields() {
        // Initialize Component fields used by Region
        Lang.titleWelcome = Component.text("Welcome to Beaconz!");
        Lang.titleSubTitle = Component.text("Capture beacons and control territory");
        Lang.titleLobbyInfo = Component.text("Lobby Info");
        Lang.actionsHitSign = Component.text("Hit sign to join");
        Lang.startYoureAMember = Component.text("You're a member of team");
        Lang.startObjective = Component.text("Objective");
        Lang.startMostObjective = Component.text("Get the most");

        // Initialize TextColor fields
        Lang.titleWelcomeColor = NamedTextColor.GOLD;
        Lang.titleSubTitleColor = NamedTextColor.AQUA;
    }

    @BeforeEach
    void setUp() {
        // Set up plugin to return beaconz world
        // Use lenient() for beaconzWorld because static method tests don't need it
        lenient().when(plugin.getBeaconzWorld()).thenReturn(beaconzWorld);

        // Default corners for a 1000x1000 region centered at origin
        corners = new Point2D[] {
            new Point2D.Double(-500, -500),  // Min corner
            new Point2D.Double(500, 500)      // Max corner
        };
    }

    @Nested
    @DisplayName("Constructor and Initialization Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor creates region with normalized corners")
        void testConstructorNormalizesCorners() {
            Region region = new Region(plugin, corners);

            assertNotNull(region);
            assertNotNull(region.corners());
            assertEquals(2, region.corners().length);
        }

        @Test
        @DisplayName("Constructor with reversed corners normalizes them")
        void testConstructorWithReversedCorners() {
            // Provide corners in wrong order
            Point2D[] reversedCorners = new Point2D[] {
                new Point2D.Double(500, 500),    // Max first
                new Point2D.Double(-500, -500)   // Min second
            };

            Region region = new Region(plugin, reversedCorners);
            Point2D[] result = region.corners();

            // Should be normalized to min first, max second
            assertEquals(-500.0, result[0].getX());
            assertEquals(-500.0, result[0].getY());
            assertEquals(500.0, result[1].getX());
            assertEquals(500.0, result[1].getY());
        }

        @Test
        @DisplayName("Initialize with newGame true sets spawn point")
        void testInitializeWithNewGame() {
            Region region = new Region(plugin, corners);

            assertNotNull(region.getSpawnPoint());
            assertEquals(0.0, region.getSpawnPoint().getX());
            assertEquals(0.0, region.getSpawnPoint().getZ());
        }
    }

    @Nested
    @DisplayName("Corner Normalization Tests")
    class CornerNormalizationTests {

        @Test
        @DisplayName("setCorners normalizes reversed X coordinates")
        void testSetCornersNormalizesX() {
            Region region = new Region(plugin, corners);

            // Provide max X before min X
            Point2D[] result = region.setCorners(100.0, 0.0, 0.0, 100.0);

            assertEquals(0.0, result[0].getX());
            assertEquals(100.0, result[1].getX());
        }

        @Test
        @DisplayName("setCorners normalizes reversed Z coordinates")
        void testSetCornersNormalizesZ() {
            Region region = new Region(plugin, corners);

            // Provide max Z before min Z
            Point2D[] result = region.setCorners(0.0, 100.0, 100.0, 0.0);

            assertEquals(0.0, result[0].getY());
            assertEquals(100.0, result[1].getY());
        }

        @Test
        @DisplayName("setCorners handles already normalized coordinates")
        void testSetCornersWithNormalizedCoords() {
            Region region = new Region(plugin, corners);

            Point2D[] result = region.setCorners(0.0, 0.0, 100.0, 100.0);

            assertEquals(0.0, result[0].getX());
            assertEquals(0.0, result[0].getY());
            assertEquals(100.0, result[1].getX());
            assertEquals(100.0, result[1].getY());
        }

        @Test
        @DisplayName("setCorners with Point2D array normalizes corners")
        void testSetCornersWithArray() {
            Region region = new Region(plugin, corners);

            Point2D[] input = new Point2D[] {
                new Point2D.Double(100, 100),
                new Point2D.Double(0, 0)
            };

            Point2D[] result = region.setCorners(input);

            assertEquals(0.0, result[0].getX());
            assertEquals(100.0, result[1].getX());
        }
    }

    @Nested
    @DisplayName("Center and Radius Calculation Tests")
    class CenterRadiusTests {

        @Test
        @DisplayName("getCenter returns correct center point")
        void testGetCenter() {
            Region region = new Region(plugin, corners);
            Point2D center = region.getCenter();

            assertEquals(0.0, center.getX(), 0.001);
            assertEquals(0.0, center.getY(), 0.001);
        }

        @Test
        @DisplayName("getCenter with asymmetric region")
        void testGetCenterAsymmetric() {
            Point2D[] asymmetricCorners = new Point2D[] {
                new Point2D.Double(100, 200),
                new Point2D.Double(300, 600)
            };

            Region region = new Region(plugin, asymmetricCorners);
            Point2D center = region.getCenter();

            assertEquals(200.0, center.getX(), 0.001);
            assertEquals(400.0, center.getY(), 0.001);
        }

        @Test
        @DisplayName("getRadius returns correct radius")
        void testGetRadius() {
            Region region = new Region(plugin, corners);

            assertEquals(500, region.getRadius());
        }

        @Test
        @DisplayName("getRadius with different sized region")
        void testGetRadiusDifferentSize() {
            Point2D[] smallCorners = new Point2D[] {
                new Point2D.Double(0, 0),
                new Point2D.Double(200, 200)
            };

            Region region = new Region(plugin, smallCorners);

            assertEquals(100, region.getRadius());
        }
    }

    @Nested
    @DisplayName("Containment Check Tests")
    class ContainmentTests {

        @Test
        @DisplayName("containsPoint returns true for point inside region")
        void testContainsPointInside() {
            Region region = new Region(plugin, corners);

            assertTrue(region.containsPoint(0, 0));
            assertTrue(region.containsPoint(-250, 250));
            assertTrue(region.containsPoint(499, 499));
        }

        @Test
        @DisplayName("containsPoint returns false for point outside region")
        void testContainsPointOutside() {
            Region region = new Region(plugin, corners);

            assertFalse(region.containsPoint(-501, 0));
            assertFalse(region.containsPoint(501, 0));
            assertFalse(region.containsPoint(0, -501));
            assertFalse(region.containsPoint(0, 501));
        }

        @Test
        @DisplayName("containsPoint returns true for point on boundary")
        void testContainsPointOnBoundary() {
            Region region = new Region(plugin, corners);

            assertTrue(region.containsPoint(-500, -500));
            assertTrue(region.containsPoint(500, 500));
            assertTrue(region.containsPoint(-500, 500));
            assertTrue(region.containsPoint(500, -500));
        }

        @Test
        @DisplayName("containsPoint with Point2D object")
        void testContainsPointWithPoint2D() {
            Region region = new Region(plugin, corners);

            assertTrue(region.containsPoint(new Point2D.Double(0, 0)));
            assertFalse(region.containsPoint(new Point2D.Double(1000, 1000)));
        }

        @Test
        @DisplayName("containsBeacon returns true for beacon inside")
        void testContainsBeaconInside() {
            Region region = new Region(plugin, corners);
            BeaconObj beacon = mock(BeaconObj.class);
            when(beacon.getX()).thenReturn(0);
            when(beacon.getY()).thenReturn(0);

            assertTrue(region.containsBeacon(beacon));
        }

        @Test
        @DisplayName("containsBeacon returns false for beacon outside")
        void testContainsBeaconOutside() {
            Region region = new Region(plugin, corners);
            BeaconObj beacon = mock(BeaconObj.class);
            when(beacon.getX()).thenReturn(1000);
            when(beacon.getY()).thenReturn(1000);

            assertFalse(region.containsBeacon(beacon));
        }

        @Test
        @DisplayName("isPlayerInRegion returns true for player inside")
        void testIsPlayerInRegionInside() {
            Region region = new Region(plugin, corners);
            Player player = mock(Player.class);
            Location loc = mock(Location.class);
            when(player.getLocation()).thenReturn(loc);
            when(loc.getBlockX()).thenReturn(0);
            when(loc.getBlockZ()).thenReturn(0);

            assertTrue(region.isPlayerInRegion(player));
        }

        @Test
        @DisplayName("isPlayerInRegion returns false for player outside")
        void testIsPlayerInRegionOutside() {
            Region region = new Region(plugin, corners);
            Player player = mock(Player.class);
            Location loc = mock(Location.class);
            when(player.getLocation()).thenReturn(loc);
            when(loc.getBlockX()).thenReturn(1000);
            when(loc.getBlockZ()).thenReturn(1000);

            assertFalse(region.isPlayerInRegion(player));
        }

        @Test
        @DisplayName("contains(Location) returns true for location inside region")
        void testContainsLocationInside() {
            Region region = new Region(plugin, corners);
            Location loc = mock(Location.class);
            when(loc.getWorld()).thenReturn(beaconzWorld);
            when(loc.getBlockX()).thenReturn(0);
            when(loc.getBlockZ()).thenReturn(0);

            assertTrue(region.contains(loc));
        }

        @Test
        @DisplayName("contains(Location) returns false for wrong world")
        void testContainsLocationWrongWorld() {
            Region region = new Region(plugin, corners);
            Location loc = mock(Location.class);
            World otherWorld = mock(World.class);
            when(loc.getWorld()).thenReturn(otherWorld);
            // These won't be called due to early exit, use lenient()
            lenient().when(loc.getBlockX()).thenReturn(0);
            lenient().when(loc.getBlockZ()).thenReturn(0);

            assertFalse(region.contains(loc));
        }

        @Test
        @DisplayName("contains(Location) returns false for null world")
        void testContainsLocationNullWorld() {
            Region region = new Region(plugin, corners);
            Location loc = mock(Location.class);
            when(loc.getWorld()).thenReturn(null);

            assertFalse(region.contains(loc));
        }
    }

    @Nested
    @DisplayName("Spawn Point Tests")
    class SpawnPointTests {

        @Test
        @DisplayName("getSpawnPoint returns initial spawn point")
        void testGetSpawnPoint() {
            Region region = new Region(plugin, corners);
            Location spawn = region.getSpawnPoint();

            assertNotNull(spawn);
            assertEquals(0.0, spawn.getX());
            assertEquals(0.0, spawn.getZ());
        }

        @Test
        @DisplayName("setSpawnPoint sets location directly")
        void testSetSpawnPointDirect() {
            Region region = new Region(plugin, corners);
            Location newSpawn = new Location(beaconzWorld, 100, 64, 100);

            region.setSpawnPoint(newSpawn);

            assertSame(newSpawn, region.getSpawnPoint());
        }

        @Test
        @DisplayName("Should set spawn point from Point2D coordinates")
        void testSetSpawnPointWithPoint2D() {
            Region region = new Region(plugin, corners);
            @NotNull
            ServerMock server = MockBukkit.mock();
            // Use MockBukkit's actual world instead of a mock
            World world = server.addSimpleWorld("beaconz_world");
            when(plugin.getBeaconzWorld()).thenReturn(world);
            
            // Create a real location using MockBukkit's world
            Point2D point = new Point2D.Double(500, 500);
            int radius = 10;
            
            // Set a solid block at the highest point so findSafeSpot works
            Location highestLoc = new Location(world, point.getX(), 64, point.getY());
            world.getBlockAt(highestLoc).setType(Material.STONE);
            
            region.setSpawnPoint(point, radius);
            
            Location spawn = region.getSpawnPoint();
            assertNotNull(spawn);
            assertEquals(point.getX(), spawn.getX());
            assertEquals(point.getY(), spawn.getZ()); // Point2D.Y maps to Z coordinate
            MockBukkit.unmock();
        }
    }

    @Nested
    @DisplayName("Location Safety Tests")
    class LocationSafetyTests {

        @Test
        @DisplayName("isLocationSafe returns false for null location")
        void testIsLocationSafeNull() {
            Region region = new Region(plugin, corners);

            assertFalse(region.isLocationSafe(null));
        }

        // Note: Full safety tests require Tag initialization which is complex in unit tests.
        // The isLocationSafe method checks for:
        // - Passable spaces (tested below)
        // - Liquid blocks (tested below)
        // - Portal blocks (requires Tag - integration test)
        // - Unsafe blocks (fences, signs, leaves, etc - requires Tag - integration test)

        @Test
        @DisplayName("isLocationSafe returns false for liquid ground")
        void testIsLocationSafeLiquidGround() {
            Region region = new Region(plugin, corners);
            Location loc = mock(Location.class);
            Block ground = mock(Block.class);
            Block space1 = mock(Block.class);
            Block space2 = mock(Block.class);

            when(loc.getBlock()).thenReturn(space1);
            when(space1.getRelative(BlockFace.DOWN)).thenReturn(ground);
            when(space1.getRelative(BlockFace.UP)).thenReturn(space2);

            when(ground.isPassable()).thenReturn(false);
            when(ground.isLiquid()).thenReturn(true);  // Liquid ground
            when(space1.isPassable()).thenReturn(true);
            when(space2.isPassable()).thenReturn(true);

            assertFalse(region.isLocationSafe(loc));
        }

        @Test
        @DisplayName("isLocationSafe returns false for non-passable space")
        void testIsLocationSafeNonPassableSpace() {
            Region region = new Region(plugin, corners);
            Location loc = mock(Location.class);
            Block ground = mock(Block.class);
            Block space1 = mock(Block.class);
            Block space2 = mock(Block.class);

            when(loc.getBlock()).thenReturn(space1);
            when(space1.getRelative(BlockFace.DOWN)).thenReturn(ground);
            when(space1.getRelative(BlockFace.UP)).thenReturn(space2);

            // When space1 is not passable, check exits early
            when(space1.isPassable()).thenReturn(false);  // Blocked space
            // These won't be checked due to early exit
            lenient().when(ground.isPassable()).thenReturn(false);
            lenient().when(ground.isLiquid()).thenReturn(false);
            lenient().when(space2.isPassable()).thenReturn(true);

            assertFalse(region.isLocationSafe(loc));
        }
    }

    @Nested
    @DisplayName("Game Association Tests")
    class GameAssociationTests {

        @Test
        @DisplayName("getGame returns null initially")
        void testGetGameInitially() {
            Region region = new Region(plugin, corners);

            assertNull(region.getGame());
        }

        @Test
        @DisplayName("setGame and getGame work correctly")
        void testSetAndGetGame() {
            Region region = new Region(plugin, corners);

            region.setGame(game);

            assertSame(game, region.getGame());
        }

        @Test
        @DisplayName("setGame can set null game")
        void testSetGameNull() {
            Region region = new Region(plugin, corners);
            region.setGame(game);

            region.setGame(null);

            assertNull(region.getGame());
        }
    }

    @Nested
    @DisplayName("BlockFace to Yaw Conversion Tests")
    class BlockFaceToYawTests {

        @Test
        @DisplayName("blockFaceToFloat converts cardinal directions correctly")
        void testBlockFaceToFloatCardinal() {
            assertEquals(180.0f, Region.blockFaceToFloat(BlockFace.SOUTH));
            assertEquals(270.0f, Region.blockFaceToFloat(BlockFace.WEST));
            assertEquals(0.0f, Region.blockFaceToFloat(BlockFace.NORTH));
            assertEquals(90.0f, Region.blockFaceToFloat(BlockFace.EAST));
        }

        @Test
        @DisplayName("blockFaceToFloat converts diagonal directions correctly")
        void testBlockFaceToFloatDiagonal() {
            assertEquals(135.0f, Region.blockFaceToFloat(BlockFace.SOUTH_EAST));
            assertEquals(225.0f, Region.blockFaceToFloat(BlockFace.SOUTH_WEST));
            assertEquals(45.0f, Region.blockFaceToFloat(BlockFace.NORTH_EAST));
            assertEquals(315.0f, Region.blockFaceToFloat(BlockFace.NORTH_WEST));
        }

        @Test
        @DisplayName("blockFaceToFloat returns 0 for non-horizontal directions")
        void testBlockFaceToFloatNonHorizontal() {
            assertEquals(0.0f, Region.blockFaceToFloat(BlockFace.UP));
            assertEquals(0.0f, Region.blockFaceToFloat(BlockFace.DOWN));
            assertEquals(0.0f, Region.blockFaceToFloat(BlockFace.SELF));
        }

        @Test
        @DisplayName("blockFaceToFloat handles all 16 compass directions")
        void testBlockFaceToFloatAllDirections() {
            assertEquals(22.5f, Region.blockFaceToFloat(BlockFace.NORTH_NORTH_EAST));
            assertEquals(67.5f, Region.blockFaceToFloat(BlockFace.EAST_NORTH_EAST));
            assertEquals(157.5f, Region.blockFaceToFloat(BlockFace.SOUTH_SOUTH_EAST));
            assertEquals(202.5f, Region.blockFaceToFloat(BlockFace.SOUTH_SOUTH_WEST));
            assertEquals(247.5f, Region.blockFaceToFloat(BlockFace.WEST_SOUTH_WEST));
            assertEquals(292.5f, Region.blockFaceToFloat(BlockFace.WEST_NORTH_WEST));
            assertEquals(337.5f, Region.blockFaceToFloat(BlockFace.NORTH_NORTH_WEST));
        }
    }

    @Nested
    @DisplayName("Display and Utility Tests")
    class DisplayUtilityTests {

        @Test
        @DisplayName("displayCoords returns formatted string")
        void testDisplayCoords() {
            Region region = new Region(plugin, corners);
            String display = region.displayCoords();

            assertNotNull(display);
            assertTrue(display.contains("center"));
            assertTrue(display.contains("corners"));
            assertTrue(display.contains("0:0"));
            assertTrue(display.contains("-500:-500"));
            assertTrue(display.contains("500:500"));
        }

        @Test
        @DisplayName("corners() returns corner array")
        void testCorners() {
            Region region = new Region(plugin, corners);
            Point2D[] result = region.corners();

            assertNotNull(result);
            assertEquals(2, result.length);
            assertEquals(-500.0, result[0].getX());
            assertEquals(500.0, result[1].getX());
        }
    }
}
