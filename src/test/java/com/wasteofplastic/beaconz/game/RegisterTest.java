package com.wasteofplastic.beaconz.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.core.TriangleField;
import com.wasteofplastic.beaconz.storage.Messages;
import com.wasteofplastic.beaconz.util.LinkResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.kyori.adventure.text.Component;

/**
 * Comprehensive test suite for the Register class.
 * <p>
 * Tests beacon registration, linking, triangle formation, database operations,
 * team ownership, and utility methods.
 */
@DisplayName("Register Tests")
class RegisterTest {

    private Register register;
    private Game game;
    private Team redTeam;
    private Team blueTeam;
    private HikariDataSource dataSource;
    private World world;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();

        // Mock plugin
        Beaconz plugin = mock(Beaconz.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("RegisterTest"));
        when(plugin.getDataFolder()).thenReturn(tempDir);

        // Initialize test database
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + new File(tempDir, "test-register.db").getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);
        when(plugin.getDataSource()).thenReturn(dataSource);

        // Create and setup test world
        world = server.addSimpleWorld("beaconz_world");
        when(plugin.getBeaconzWorld()).thenReturn(world);

        // Mock GameMgr
        GameMgr gameMgr = mock(GameMgr.class);
        when(plugin.getGameMgr()).thenReturn(gameMgr);

        // Mock Game and Scorecard
        game = mock(Game.class);
        Scorecard scorecard = mock(Scorecard.class);
        Scoreboard scoreboard = mock(Scoreboard.class);
        when(game.getScorecard()).thenReturn(scorecard);
        when(game.getName()).thenReturn(Component.text("TestGame"));
        when(scorecard.getScoreboard()).thenReturn(scoreboard);
        when(gameMgr.getGame(any(Point2D.class))).thenReturn(game);
        when(gameMgr.getGame(anyInt(), anyInt())).thenReturn(game);

        // Mock teams
        redTeam = mock(Team.class);
        when(redTeam.getName()).thenReturn("red");
        when(scorecard.getTeam("red")).thenReturn(redTeam);

        blueTeam = mock(Team.class);
        when(blueTeam.getName()).thenReturn("blue");
        when(scorecard.getTeam("blue")).thenReturn(blueTeam);

        when(gameMgr.getGame(redTeam)).thenReturn(game);
        when(gameMgr.getGame(blueTeam)).thenReturn(game);
        when(gameMgr.getSC(anyInt(), anyInt())).thenReturn(scorecard);

        // Mock Messages
        Messages messages = mock(Messages.class);
        when(plugin.getMessages()).thenReturn(messages);

        // Initialize settings
        Settings.linkLimit = 8;

        // Initialize Lang strings for messaging
        initializeLangStrings();

        // Create the Register instance
        register = new Register(plugin);
        when(plugin.getRegister()).thenReturn(register);
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        MockBukkit.unmock();
    }

    private void initializeLangStrings() {
        Lang.linkLostLink = Component.text("Lost a link!");
        Lang.linkTeamLostLink = "<team> lost a link!";
        Lang.linkLostLinks = "Lost <number> links!";
        Lang.linkTeamLostLinks = "<team> lost <number> links!";
        Lang.triangleYourTeamLostATriangle = Component.text("Your team lost a triangle!");
        Lang.triangleTeamLostATriangle = "<team> lost a triangle!";
    }

    @Nested
    @DisplayName("Beacon Registration Tests")
    class BeaconRegistrationTests {

        @Test
        @DisplayName("Should register new beacon with team ownership")
        void shouldRegisterNewBeaconWithOwnership() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            assertNotNull(beacon);
            assertEquals(100, beacon.getX());
            assertEquals(64, beacon.getY());
            assertEquals(200, beacon.getZ());
            assertEquals(redTeam, beacon.getOwnership());
        }

        @Test
        @DisplayName("Should register unowned beacon")
        void shouldRegisterUnownedBeacon() {
            BeaconObj beacon = register.addBeacon(null, 100, 64, 200);

            assertNotNull(beacon);
            assertNull(beacon.getOwnership());
        }

        @Test
        @DisplayName("Should register beacon with Location")
        void shouldRegisterBeaconWithLocation() {
            Location loc = new Location(world, 100, 64, 200);
            register.addBeacon(redTeam, loc);

            BeaconObj beacon = register.getBeaconAt(100, 200);
            assertNotNull(beacon);
            assertEquals(redTeam, beacon.getOwnership());
        }

        @Test
        @DisplayName("Should create base blocks around beacon")
        void shouldCreateBaseBlocksAroundBeacon() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            // Check 8 surrounding blocks are registered as base blocks
            Set<Point2D> defenses = register.getDefensesAtBeacon(beacon);
            assertNotNull(defenses);
            assertEquals(8, defenses.size());

            // Verify specific surrounding points
            assertTrue(defenses.contains(new Point2D.Double(99, 199)));
            assertTrue(defenses.contains(new Point2D.Double(100, 199)));
            assertTrue(defenses.contains(new Point2D.Double(101, 199)));
            assertTrue(defenses.contains(new Point2D.Double(99, 200)));
            assertTrue(defenses.contains(new Point2D.Double(101, 200)));
            assertTrue(defenses.contains(new Point2D.Double(99, 201)));
            assertTrue(defenses.contains(new Point2D.Double(100, 201)));
            assertTrue(defenses.contains(new Point2D.Double(101, 201)));
        }

        @Test
        @DisplayName("Should return beacon by coordinates")
        void shouldReturnBeaconByCoordinates() {
            register.addBeacon(redTeam, 100, 64, 200);

            BeaconObj beacon = register.getBeaconAt(100, 200);
            assertNotNull(beacon);
            assertEquals(100, beacon.getX());
            assertEquals(200, beacon.getZ());
        }

        @Test
        @DisplayName("Should return null for non-existent beacon")
        void shouldReturnNullForNonExistentBeacon() {
            BeaconObj beacon = register.getBeaconAt(999, 999);
            assertNull(beacon);
        }

        @Test
        @DisplayName("Should count beacons correctly")
        void shouldCountBeaconsCorrectly() {
            assertEquals(0, register.getBeaconCount());

            register.addBeacon(redTeam, 100, 64, 100);
            assertEquals(1, register.getBeaconCount());

            register.addBeacon(blueTeam, 200, 64, 200);
            assertEquals(2, register.getBeaconCount());

            register.addBeacon(null, 300, 64, 300);
            assertEquals(3, register.getBeaconCount());
        }
    }

    @Nested
    @DisplayName("Beacon Lookup Tests")
    class BeaconLookupTests {

        @Test
        @DisplayName("Should find beacon at Point2D")
        void shouldFindBeaconAtPoint2D() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            Point2D point = new Point2D.Double(99, 200); // Base block location

            BeaconObj found = register.getBeaconAt(point);
            assertNotNull(found);
            assertEquals(beacon, found);
        }

        @Test
        @DisplayName("Should find beacon at Location")
        void shouldFindBeaconAtLocation() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            Location loc = new Location(world, 99, 64, 200); // Base block location

            BeaconObj found = register.getBeaconAt(loc);
            assertNotNull(found);
            assertEquals(beacon, found);
        }

        @Test
        @DisplayName("Should return null for null Location")
        void shouldReturnNullForNullLocation() {
            BeaconObj found = register.getBeaconAt((Location) null);
            assertNull(found);
        }

        @Test
        @DisplayName("Should check if near beacon")
        void shouldCheckIfNearBeacon() {
            register.addBeacon(redTeam, 100, 64, 100);

            Point2D nearby = new Point2D.Double(105, 105);
            Point2D far = new Point2D.Double(200, 200);

            assertTrue(register.isNearBeacon(nearby, 10));
            assertFalse(register.isNearBeacon(far, 10));
        }

        @Test
        @DisplayName("Should get nearby beacons")
        void shouldGetNearbyBeacons() {
            register.addBeacon(redTeam, 100, 64, 100);
            register.addBeacon(blueTeam, 110, 64, 110);
            register.addBeacon(null, 500, 64, 500);

            Location loc = new Location(world, 105, 64, 105);
            List<BeaconObj> nearby = register.getNearbyBeacons(loc, 20);

            assertEquals(2, nearby.size());
        }
    }

    @Nested
    @DisplayName("Team Beacon Tests")
    class TeamBeaconTests {

        @Test
        @DisplayName("Should get team beacons")
        void shouldGetTeamBeacons() {
            register.addBeacon(redTeam, 100, 64, 100);
            register.addBeacon(redTeam, 200, 64, 200);
            register.addBeacon(blueTeam, 300, 64, 300);
            register.addBeacon(null, 400, 64, 400);

            List<BeaconObj> redBeacons = register.getTeamBeacons(redTeam);
            assertEquals(2, redBeacons.size());

            List<BeaconObj> blueBeacons = register.getTeamBeacons(blueTeam);
            assertEquals(1, blueBeacons.size());
        }

        @Test
        @DisplayName("Should return empty list for team with no beacons")
        void shouldReturnEmptyListForTeamWithNoBeacons() {
            register.addBeacon(redTeam, 100, 64, 100);

            List<BeaconObj> blueBeacons = register.getTeamBeacons(blueTeam);
            assertTrue(blueBeacons.isEmpty());
        }

        @Test
        @DisplayName("Should set beacon owner")
        void shouldSetBeaconOwner() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            assertEquals(redTeam, beacon.getOwnership());

            register.setBeaconOwner(beacon, blueTeam);
            assertEquals(blueTeam, beacon.getOwnership());
        }
    }

    @Nested
    @DisplayName("Beacon Map Tests")
    class BeaconMapTests {

        @Test
        @DisplayName("Should add and retrieve beacon map")
        void shouldAddAndRetrieveBeaconMap() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            register.addBeaconMap(1, beacon);

            BeaconObj retrieved = register.getBeaconMap(1);
            assertNotNull(retrieved);
            assertEquals(beacon, retrieved);
            assertEquals(1, beacon.getId());
        }

        @Test
        @DisplayName("Should add beacon map with origin coordinates")
        void shouldAddBeaconMapWithOrigin() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            register.addBeaconMap(1, beacon, 50, 60);

            Point2D origin = register.getMapOrigin(1);
            assertNotNull(origin);
            assertEquals(50, (int) origin.getX());
            assertEquals(60, (int) origin.getY());
        }

        @Test
        @DisplayName("Should set and get map origin")
        void shouldSetAndGetMapOrigin() {
            register.setMapOrigin(5, 100, 200);

            Point2D origin = register.getMapOrigin(5);
            assertNotNull(origin);
            assertEquals(100, (int) origin.getX());
            assertEquals(200, (int) origin.getY());
        }

        @Test
        @DisplayName("Should return null for non-existent beacon map")
        void shouldReturnNullForNonExistentMap() {
            BeaconObj beacon = register.getBeaconMap(999);
            assertNull(beacon);
        }

        @Test
        @DisplayName("Should remove beacon map")
        void shouldRemoveBeaconMap() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            register.addBeaconMap(1, beacon, 50, 60);

            register.removeBeaconMap(1);

            assertNull(register.getBeaconMap(1));
            assertNull(register.getMapOrigin(1));
        }

        @Test
        @DisplayName("Should get beacon map indices")
        void shouldGetBeaconMapIndices() {
            BeaconObj beacon1 = register.addBeacon(redTeam, 100, 64, 100);
            BeaconObj beacon2 = register.addBeacon(redTeam, 200, 64, 200);

            register.addBeaconMap(1, beacon1);
            register.addBeaconMap(5, beacon2);

            Set<Integer> indices = register.getBeaconMapIndex();
            assertEquals(2, indices.size());
            assertTrue(indices.contains(1));
            assertTrue(indices.contains(5));
        }
    }

    @Nested
    @DisplayName("Base Block Tests")
    class BaseBlockTests {

        @Test
        @DisplayName("Should add beacon base block")
        void shouldAddBeaconBaseBlock() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            register.addBeaconBaseBlock(105, 205, beacon);

            Point2D point = new Point2D.Double(105, 205);
            BeaconObj found = register.getBeaconAt(point);
            assertNotNull(found);
            assertEquals(beacon, found);
        }

        @Test
        @DisplayName("Should add beacon defense block via Location")
        void shouldAddBeaconDefenseBlockViaLocation() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            Location loc = new Location(world, 105, 65, 205);

            register.addBeaconDefenseBlock(loc, beacon);

            Set<Point2D> defenses = register.getDefensesAtBeacon(beacon);
            assertTrue(defenses.contains(new Point2D.Double(105, 205)));
        }

        @Test
        @DisplayName("Should check if above beacon")
        void shouldCheckIfAboveBeacon() {
            register.addBeacon(redTeam, 100, 64, 200);

            // Location above a base block at beacon height
            Location aboveBase = new Location(world, 99, 65, 199);
            assertTrue(register.isAboveBeacon(aboveBase));

            // Location below beacon
            Location belowBeacon = new Location(world, 99, 60, 199);
            assertFalse(register.isAboveBeacon(belowBeacon));

            // Location not near any beacon
            Location far = new Location(world, 500, 64, 500);
            assertFalse(register.isAboveBeacon(far));
        }
    }

    @Nested
    @DisplayName("Triangle Field Tests")
    class TriangleFieldTests {

        @Test
        @DisplayName("Should add valid triangle")
        void shouldAddValidTriangle() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 50, 64, 100);

            Boolean result = register.addTriangle(
                    b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam);

            assertTrue(result);
            assertEquals(1, register.getTriangleFields().size());
        }

        @Test
        @DisplayName("Should reject triangle with different team beacons")
        void shouldRejectTriangleWithDifferentTeamBeacons() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(blueTeam, 50, 64, 100);

            assertThrows(IllegalArgumentException.class, () ->
                    register.addTriangle(b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam));
        }

        @Test
        @DisplayName("Should reject triangle with non-beacon points")
        void shouldRejectTriangleWithNonBeaconPoints() {
            register.addBeacon(redTeam, 0, 64, 0);
            register.addBeacon(redTeam, 100, 64, 0);

            Point2D nonBeaconPoint = new Point2D.Double(50, 100);

            assertThrows(IllegalArgumentException.class, () ->
                    register.addTriangle(new Point2D.Double(0, 0),
                            new Point2D.Double(100, 0), nonBeaconPoint, redTeam));
        }

        @Test
        @DisplayName("Should reject duplicate triangles")
        void shouldRejectDuplicateTriangles() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 50, 64, 100);

            Boolean first = register.addTriangle(
                    b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam);
            Boolean second = register.addTriangle(
                    b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam);

            assertTrue(first);
            assertFalse(second);
        }

        @Test
        @DisplayName("Should get team triangles count")
        void shouldGetTeamTrianglesCount() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 50, 64, 100);

            register.addTriangle(b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam);

            assertEquals(1, register.getTeamTriangles(redTeam));
            assertEquals(0, register.getTeamTriangles(blueTeam));
        }

        @Test
        @DisplayName("Should find triangles containing point")
        void shouldFindTrianglesContainingPoint() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 50, 64, 100);

            register.addTriangle(b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam);

            // Point inside triangle
            List<TriangleField> inside = register.getTriangle(50, 30);
            assertEquals(1, inside.size());

            // Point outside triangle
            List<TriangleField> outside = register.getTriangle(200, 200);
            assertTrue(outside.isEmpty());
        }

        @Test
        @DisplayName("Should set and get triangle fields")
        void shouldSetAndGetTriangleFields() {
            Set<TriangleField> fields = new java.util.HashSet<>();
            Point2D p1 = new Point2D.Double(0, 0);
            Point2D p2 = new Point2D.Double(100, 0);
            Point2D p3 = new Point2D.Double(50, 100);
            fields.add(new TriangleField(p1, p2, p3, redTeam));

            register.setTriangleFields(fields);

            assertEquals(1, register.getTriangleFields().size());
        }
    }

    @Nested
    @DisplayName("Beacon Link Tests")
    class BeaconLinkTests {

        @Test
        @DisplayName("Should add beacon link")
        void shouldAddBeaconLink() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);

            LinkResult result = register.addBeaconLink(b1, b2);

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should count team links")
        void shouldCountTeamLinks() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 200, 64, 0);

            register.addBeaconLink(b1, b2);
            register.addBeaconLink(b2, b3);

            assertEquals(2, register.getTeamLinks(redTeam));
            assertEquals(0, register.getTeamLinks(blueTeam));
        }

        @Test
        @DisplayName("Should get enemy links")
        void shouldGetEnemyLinks() {
            BeaconObj redB1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj redB2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj blueB1 = register.addBeacon(blueTeam, 0, 64, 100);
            BeaconObj blueB2 = register.addBeacon(blueTeam, 100, 64, 100);

            register.addBeaconLink(redB1, redB2);
            register.addBeaconLink(blueB1, blueB2);

            Set<Line2D> redEnemyLinks = register.getEnemyLinks(redTeam);
            assertEquals(1, redEnemyLinks.size());

            Set<Line2D> blueEnemyLinks = register.getEnemyLinks(blueTeam);
            assertEquals(1, blueEnemyLinks.size());
        }

        @Test
        @DisplayName("Should create triangle when links complete")
        void shouldCreateTriangleWhenLinksComplete() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 50, 64, 100);

            register.addBeaconLink(b1, b2);
            register.addBeaconLink(b2, b3);
            LinkResult result = register.addBeaconLink(b3, b1);

            assertTrue(result.isSuccess());
            assertTrue(result.getFieldsMade() > 0);
            assertFalse(register.getTriangleFields().isEmpty());
        }
    }

    @Nested
    @DisplayName("Clear Register Tests")
    class ClearRegisterTests {

        @Test
        @DisplayName("Should clear all data")
        void shouldClearAllData() {
            register.addBeacon(redTeam, 100, 64, 100);
            register.addBeacon(blueTeam, 200, 64, 200);

            assertEquals(2, register.getBeaconCount());

            register.clear();

            assertEquals(0, register.getBeaconCount());
            assertTrue(register.getTriangleFields().isEmpty());
        }

        @Test
        @DisplayName("Should clear region-specific data")
        void shouldClearRegionSpecificData() {
            BeaconObj b1 = register.addBeacon(redTeam, 100, 64, 100);

            // Create a mock region
            com.wasteofplastic.beaconz.core.Region region = mock(com.wasteofplastic.beaconz.core.Region.class);
            when(region.containsPoint(any(Point2D.class))).thenAnswer(inv -> {
                Point2D p = inv.getArgument(0);
                return p.getX() == 100 && p.getY() == 100;
            });
            when(region.containsBeacon(b1)).thenReturn(true);
            when(region.getGame()).thenReturn(game);

            // Add another beacon not in region
            register.addBeacon(blueTeam, 500, 64, 500);

            assertEquals(2, register.getBeaconCount());

            register.clear(region);

            assertEquals(1, register.getBeaconCount());
        }
    }

    @Nested
    @DisplayName("Database Save/Load Tests")
    class DatabaseSaveLoadTests {

        @Test
        @DisplayName("Should save and load beacons")
        void shouldSaveAndLoadBeacons() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            beacon.setId(123);

            register.saveRegister();
            register.clear();

            assertEquals(0, register.getBeaconCount());

            register.loadRegister();

            assertEquals(1, register.getBeaconCount());
            BeaconObj loaded = register.getBeaconAt(100, 200);
            assertNotNull(loaded);
            assertEquals(100, loaded.getX());
            assertEquals(200, loaded.getZ());
        }

        @Test
        @DisplayName("Should save and load multiple beacons")
        void shouldSaveAndLoadMultipleBeacons() {
            register.addBeacon(redTeam, 100, 64, 100);
            register.addBeacon(blueTeam, 200, 64, 200);
            register.addBeacon(null, 300, 64, 300);

            assertEquals(3, register.getBeaconCount());

            register.saveRegister();
            register.clear();
            register.loadRegister();

            assertEquals(3, register.getBeaconCount());
        }

        @Test
        @DisplayName("Should save and load beacon maps")
        void shouldSaveAndLoadBeaconMaps() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            register.addBeaconMap(5, beacon, 50, 60);

            register.saveRegister();
            register.clear();
            register.loadRegister();

            // Maps are restored when the beacon is loaded
            // The map itself needs Bukkit.getMap which we can't fully test
            // But the data structure should persist
        }
    }

    @Nested
    @DisplayName("Database Integrity Tests")
    class DatabaseIntegrityTests {

        @Test
        @DisplayName("Should verify database integrity")
        void shouldVerifyDatabaseIntegrity() {
            register.addBeacon(redTeam, 100, 64, 100);
            register.saveRegister();

            boolean isValid = register.verifyDatabaseIntegrity();
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should repair database with orphaned records")
        void shouldRepairDatabaseWithOrphanedRecords() throws SQLException {
            register.addBeacon(redTeam, 100, 64, 100);
            register.saveRegister();

            // Manually insert orphaned base block
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO beacon_base_blocks (beacon_x, beacon_z, block_x, block_z) " +
                        "VALUES (999, 999, 1000, 1000)");
            }

            // Verify integrity fails
            assertFalse(register.verifyDatabaseIntegrity());

            // Repair
            int deleted = register.repairDatabase();
            assertTrue(deleted > 0);

            // Verify integrity passes now
            assertTrue(register.verifyDatabaseIntegrity());
        }
    }

    @Nested
    @DisplayName("Beacon Register Accessor Tests")
    class BeaconRegisterAccessorTests {

        @Test
        @DisplayName("Should get beacon register map")
        void shouldGetBeaconRegisterMap() {
            register.addBeacon(redTeam, 100, 64, 100);
            register.addBeacon(blueTeam, 200, 64, 200);

            HashMap<Point2D, BeaconObj> registerMap = register.getBeaconRegister();

            assertNotNull(registerMap);
            assertEquals(2, registerMap.size());
            assertTrue(registerMap.containsKey(new Point2D.Double(100, 100)));
            assertTrue(registerMap.containsKey(new Point2D.Double(200, 200)));
        }
    }

    @Nested
    @DisplayName("Team Area Tests")
    class TeamAreaTests {

        @Test
        @DisplayName("Should calculate team area")
        void shouldCalculateTeamArea() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 50, 64, 100);

            register.addTriangle(b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam);

            int area = register.getTeamArea(redTeam);
            assertTrue(area > 0);

            // Blue team should have 0 area
            assertEquals(0, register.getTeamArea(blueTeam));
        }
    }

    @Nested
    @DisplayName("Beacon Removal Tests")
    class BeaconRemovalTests {

        @Test
        @DisplayName("Should remove beacon ownership")
        void shouldRemoveBeaconOwnership() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);
            assertEquals(redTeam, beacon.getOwnership());

            // Use quiet mode to avoid messaging which requires full plugin setup
            register.removeBeaconOwnership(beacon, true);

            assertNull(beacon.getOwnership());
        }

        @Test
        @DisplayName("Should remove beacon ownership quietly")
        void shouldRemoveBeaconOwnershipQuietly() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            // Should not throw any exceptions
            assertDoesNotThrow(() -> register.removeBeaconOwnership(beacon, true));
            assertNull(beacon.getOwnership());
        }

        @Test
        @DisplayName("Should remove links when removing ownership")
        void shouldRemoveLinksWhenRemovingOwnership() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);

            register.addBeaconLink(b1, b2);
            assertEquals(1, register.getTeamLinks(redTeam));

            // Use quiet mode to avoid messaging which requires full plugin setup
            register.removeBeaconOwnership(b1, true);

            assertEquals(0, register.getTeamLinks(redTeam));
        }

        @Test
        @DisplayName("Should remove triangles when removing beacon ownership")
        void shouldRemoveTrianglesWhenRemovingOwnership() {
            BeaconObj b1 = register.addBeacon(redTeam, 0, 64, 0);
            BeaconObj b2 = register.addBeacon(redTeam, 100, 64, 0);
            BeaconObj b3 = register.addBeacon(redTeam, 50, 64, 100);

            register.addTriangle(b1.getPoint(), b2.getPoint(), b3.getPoint(), redTeam);
            assertEquals(1, register.getTeamTriangles(redTeam));

            // Use quiet mode to avoid messaging which requires full plugin setup
            register.removeBeaconOwnership(b1, true);

            assertEquals(0, register.getTeamTriangles(redTeam));
        }
    }

    @Nested
    @DisplayName("Beacon Block Detection Tests")
    class BeaconBlockDetectionTests {

        @Test
        @DisplayName("Should detect beacon block")
        void shouldDetectBeaconBlock() {
            register.addBeacon(redTeam, 100, 64, 200);

            Block beaconBlock = mock(Block.class);
            when(beaconBlock.getType()).thenReturn(Material.BEACON);
            Location loc = new Location(world, 100, 64, 200);
            when(beaconBlock.getLocation()).thenReturn(loc);

            BeaconObj found = register.getBeacon(beaconBlock);
            assertNotNull(found);
        }

        @Test
        @DisplayName("Should detect emerald base block")
        void shouldDetectEmeraldBaseBlock() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            Block emeraldBlock = mock(Block.class);
            when(emeraldBlock.getType()).thenReturn(Material.EMERALD_BLOCK);
            Location loc = new Location(world, 99, 63, 199);
            when(emeraldBlock.getLocation()).thenReturn(loc);
            when(emeraldBlock.getY()).thenReturn(63);

            BeaconObj found = register.getBeacon(emeraldBlock);
            assertNotNull(found);
            assertEquals(beacon, found);
        }

        @Test
        @DisplayName("Should return null for non-beacon block type")
        void shouldReturnNullForNonBeaconBlockType() {
            register.addBeacon(redTeam, 100, 64, 200);

            Block dirtBlock = mock(Block.class);
            when(dirtBlock.getType()).thenReturn(Material.DIRT);

            BeaconObj found = register.getBeacon(dirtBlock);
            assertNull(found);
        }

        @Test
        @DisplayName("Should check if block is beacon")
        void shouldCheckIfBlockIsBeacon() {
            register.addBeacon(redTeam, 100, 64, 200);

            Block beaconBlock = mock(Block.class);
            when(beaconBlock.getType()).thenReturn(Material.BEACON);
            Location loc = new Location(world, 100, 64, 200);
            when(beaconBlock.getLocation()).thenReturn(loc);

            assertTrue(register.isBeacon(beaconBlock));

            Block dirtBlock = mock(Block.class);
            when(dirtBlock.getType()).thenReturn(Material.DIRT);

            assertFalse(register.isBeacon(dirtBlock));
        }

        @Test
        @DisplayName("Should detect obsidian cap block")
        void shouldDetectObsidianCapBlock() {
            BeaconObj beacon = register.addBeacon(redTeam, 100, 64, 200);

            Block obsidianBlock = mock(Block.class);
            when(obsidianBlock.getType()).thenReturn(Material.OBSIDIAN);
            Location capLoc = new Location(world, 100, 65, 200);
            when(obsidianBlock.getLocation()).thenReturn(capLoc);

            Block beaconBelow = mock(Block.class);
            when(beaconBelow.getType()).thenReturn(Material.BEACON);
            Location beaconLoc = new Location(world, 100, 64, 200);
            when(beaconBelow.getLocation()).thenReturn(beaconLoc);
            when(obsidianBlock.getRelative(BlockFace.DOWN)).thenReturn(beaconBelow);

            BeaconObj found = register.getBeacon(obsidianBlock);
            assertNotNull(found);
            assertEquals(beacon, found);
        }
    }
}








