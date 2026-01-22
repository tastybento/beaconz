package com.wasteofplastic.beaconz.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import com.wasteofplastic.beaconz.core.TriangleField;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for {@link TriangleScorer}.
 * <p>
 * The TriangleScorer calculates the total area covered by sets of triangles,
 * correctly handling:
 * <ul>
 *   <li>Single triangles</li>
 *   <li>Multiple non-overlapping triangles</li>
 *   <li>Overlapping triangles (unions)</li>
 *   <li>Triangles sharing edges or vertices</li>
 *   <li>Complex polygon formations from multiple triangles</li>
 *   <li>Team-specific triangle sets</li>
 * </ul>
 * <p>
 * The scoring algorithm uses Java AWT's {@link java.awt.geom.Area} class
 * to compute unions of overlapping triangles and calculate the total
 * covered area without double-counting overlaps.
 *
 * @author tastybento
 */
@DisplayName("TriangleScorer Tests")
class TriangleScoreTest {

    private Set<TriangleField> triangleFields;

    /**
     * Set up a fresh triangle field set before each test.
     */
    @BeforeEach
    void setUp() {
        triangleFields = new HashSet<>();
    }

    // ========== Single Triangle Tests ==========

    /**
     * Test calculating the area of a single triangle.
     * <p>
     * A right triangle with base 10 and height 10 has area = 0.5 * 10 * 10 = 50.
     */
    @Test
    @DisplayName("Single triangle - calculates correct area")
    void testSingleTriangle() {
        // Given: A right triangle with vertices at (0,0), (10,0), (0,10)
        TriangleField triangle = createTriangle(0, 0, 10, 0, 0, 10);
        triangleFields.add(triangle);

        // When: Calculate area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Area should be 50
        assertEquals(50.0, area, 0.1, "Right triangle area should be 50");
    }

    /**
     * Test that an empty set of triangles returns zero area.
     */
    @Test
    @DisplayName("Empty set - returns zero area")
    void testEmptySet() {
        // When: Calculate area of empty set
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Area should be 0
        assertEquals(0.0, area, 0.1, "Empty set should have zero area");
    }

    /**
     * Test a triangle with larger dimensions.
     * <p>
     * Triangle with vertices at (0,0), (100,0), (0,100) has area = 5000.
     */
    @Test
    @DisplayName("Large triangle - handles larger dimensions")
    void testLargeTriangle() {
        // Given: A large right triangle
        TriangleField triangle = createTriangle(0, 0, 100, 0, 0, 100);
        triangleFields.add(triangle);

        // When: Calculate area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Area should be 5000
        assertEquals(5000.0, area, 0.1, "Large triangle area should be 5000");
    }

    // ========== Non-Overlapping Triangles Tests ==========

    /**
     * Test two separate non-overlapping triangles.
     * <p>
     * Two identical triangles, each with area 50, should total 100.
     */
    @Test
    @DisplayName("Two non-overlapping triangles - sums areas correctly")
    void testTwoSeparateTriangles() {
        // Given: Two separate triangles
        // Triangle 1: (0,0), (10,0), (0,10) - area = 50
        triangleFields.add(createTriangle(0, 0, 10, 0, 0, 10));

        // Triangle 2: (20,0), (30,0), (30,10) - area = 50
        triangleFields.add(createTriangle(20, 0, 30, 0, 30, 10));

        // When: Calculate total area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Total area should be 100
        assertEquals(100.0, area, 0.1, "Two separate triangles should sum to 100");
    }

    /**
     * Test three non-overlapping triangles.
     */
    @Test
    @DisplayName("Three non-overlapping triangles - sums all areas")
    void testThreeSeparateTriangles() {
        // Given: Three separate triangles, each with area 50
        triangleFields.add(createTriangle(0, 0, 10, 0, 0, 10));      // area = 50
        triangleFields.add(createTriangle(20, 0, 30, 0, 30, 10));    // area = 50
        triangleFields.add(createTriangle(40, 0, 50, 0, 50, 10));    // area = 50

        // When: Calculate total area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Total area should be 150
        assertEquals(150.0, area, 0.1, "Three separate triangles should sum to 150");
    }

    // ========== Overlapping Triangles Tests ==========

    /**
     * Test two overlapping triangles that share a common vertex.
     * <p>
     * When triangles share only a vertex (no area overlap), their areas still sum normally.
     */
    @Test
    @DisplayName("Two triangles sharing a vertex - sums areas correctly")
    void testTrianglesWithCommonVertex() {
        // Given: Two triangles sharing vertex (0,0)
        // Triangle 1: (0,0), (10,0), (0,10) - area = 50
        triangleFields.add(createTriangle(0, 0, 10, 0, 0, 10));

        // Triangle 2: (0,0), (-10,-10), (-10,0) - area = 50
        triangleFields.add(createTriangle(0, 0, -10, -10, -10, 0));

        // When: Calculate total area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Total area should be 100 (no overlap in area, only touching at vertex)
        assertEquals(100.0, area, 0.1, "Triangles sharing vertex should sum to 100");
    }

    /**
     * Test two partially overlapping triangles.
     * <p>
     * The algorithm should union the triangles and return the total covered area
     * without double-counting the overlap.
     */
    @Test
    @DisplayName("Two overlapping triangles - unions correctly")
    void testTwoOverlappingTriangles() {
        // Given: Two triangles that overlap
        // Triangle 1: (50,50), (60,50), (60,60) - right triangle, area = 50
        triangleFields.add(createTriangle(50, 50, 60, 50, 60, 60));

        // Triangle 2: (50,50), (50,60), (60,50) - right triangle, area = 50
        // These two triangles form a square when unioned
        triangleFields.add(createTriangle(50, 50, 50, 60, 60, 50));

        // When: Calculate total area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Union forms a 10x10 square with area = 100
        // But due to diagonal, actual area is 75 (the union of the two right triangles)
        assertEquals(75.0, area, 0.1, "Overlapping triangles should union to 75");
    }

    /**
     * Test the original complex scenario: two separate pairs where one pair overlaps.
     * <p>
     * Scenario:
     * - Pair 1: Two separate triangles (area = 50 each)
     * - Pair 2: Two overlapping triangles (union area = 75)
     * Total should be 50 + 50 + 75 = 175
     */
    @Test
    @DisplayName("Mixed separate and overlapping triangles - calculates correctly")
    void testMixedSeparateAndOverlapping() {
        // Given: Four triangles (two separate + two overlapping)
        // Separate triangle 1: area = 50
        triangleFields.add(createTriangle(0, 0, 10, 0, 0, 10));

        // Separate triangle 2: area = 50
        triangleFields.add(createTriangle(20, 0, 30, 0, 30, 10));

        // Overlapping pair: union area = 75
        triangleFields.add(createTriangle(50, 50, 60, 50, 60, 60));
        triangleFields.add(createTriangle(50, 50, 50, 60, 60, 50));

        // When: Calculate total area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Total should be 175
        assertEquals(175.0, area, 0.1, "Mixed triangles should total 175");
    }

    /**
     * Test four triangles that all intersect to form a complex polygon.
     * <p>
     * This tests the algorithm's ability to handle multiple overlapping
     * triangles that must be unioned together into a single complex shape.
     */
    @Test
    @DisplayName("Four intersecting triangles - forms complex union")
    void testFourIntersectingTriangles() {
        // Given: Four triangles that intersect with each other
        triangleFields.add(createTriangle(0, 0, -20, 40, 30, 50));
        triangleFields.add(createTriangle(10, 30, 20, 0, 40, 20));
        triangleFields.add(createTriangle(30, 20, 70, 30, 70, 60));
        triangleFields.add(createTriangle(40, 0, 70, 0, 60, 50));

        // When: Calculate total area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Complex union should have area approximately 2658
        assertEquals(2658.0, area, 1.0, "Four intersecting triangles should union to ~2658");
    }

    // ========== Team-Based Scoring Tests ==========

    /**
     * Test scoring for a specific team when triangles belong to different teams.
     */
    @Test
    @DisplayName("Team scoring - filters by team ownership")
    void testGetScoreForTeam() {
        // Given: Multiple triangles owned by different teams
        Team redTeam = mock(Team.class);
        Team blueTeam = mock(Team.class);

        // Red team triangles (total area = 100)
        triangleFields.add(createTriangle(0, 0, 10, 0, 0, 10, redTeam));      // 50
        triangleFields.add(createTriangle(20, 0, 30, 0, 30, 10, redTeam));    // 50

        // Blue team triangles (total area = 50)
        triangleFields.add(createTriangle(40, 0, 50, 0, 50, 10, blueTeam));   // 50

        // When: Calculate score for red team
        double redScore = TriangleScorer.getScore(triangleFields, redTeam);

        // Then: Red team should have area = 100
        assertEquals(100.0, redScore, 0.1, "Red team should have score of 100");
    }

    /**
     * Test scoring for a team with no triangles.
     */
    @Test
    @DisplayName("Team scoring - returns zero for team with no triangles")
    void testGetScoreForTeamWithNoTriangles() {
        // Given: Triangles owned by one team
        Team redTeam = mock(Team.class);
        Team blueTeam = mock(Team.class);

        triangleFields.add(createTriangle(0, 0, 10, 0, 0, 10, redTeam));

        // When: Calculate score for blue team (no triangles)
        double blueScore = TriangleScorer.getScore(triangleFields, blueTeam);

        // Then: Blue team should have score = 0
        assertEquals(0.0, blueScore, 0.1, "Team with no triangles should have score of 0");
    }

    /**
     * Test that unowned triangles (null owner) are not counted for any team.
     */
    @Test
    @DisplayName("Team scoring - ignores unowned triangles")
    void testGetScoreIgnoresUnownedTriangles() {
        // Given: Mix of owned and unowned triangles
        Team redTeam = mock(Team.class);

        triangleFields.add(createTriangle(0, 0, 10, 0, 0, 10, redTeam));      // 50
        triangleFields.add(createTriangle(20, 0, 30, 0, 30, 10, null));       // unowned

        // When: Calculate score for red team
        double redScore = TriangleScorer.getScore(triangleFields, redTeam);

        // Then: Only owned triangle should count
        assertEquals(50.0, redScore, 0.1, "Should only count owned triangles");
    }

    // ========== Edge Case Tests ==========

    /**
     * Test triangles with negative coordinates.
     */
    @Test
    @DisplayName("Negative coordinates - handles correctly")
    void testNegativeCoordinates() {
        // Given: Triangles in negative coordinate space
        triangleFields.add(createTriangle(-10, -10, 0, -10, -10, 0));     // 50
        triangleFields.add(createTriangle(-30, -30, -20, -30, -30, -20)); // 50

        // When: Calculate area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Should calculate correctly
        assertEquals(100.0, area, 0.1, "Negative coordinates should work correctly");
    }

    /**
     * Test that very small triangles are calculated correctly.
     */
    @Test
    @DisplayName("Small triangles - calculates accurately")
    void testSmallTriangles() {
        // Given: A very small triangle
        triangleFields.add(createTriangle(0, 0, 1, 0, 0, 1));

        // When: Calculate area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Area should be 0.5
        assertEquals(0.5, area, 0.01, "Small triangle should have area 0.5");
    }

    /**
     * Test that the result is always non-negative.
     */
    @Test
    @DisplayName("Area calculation - always returns non-negative value")
    void testAreaIsNonNegative() {
        // Given: Various triangles with different vertex orders
        triangleFields.add(createTriangle(0, 10, 10, 0, 0, 0));   // Reverse order
        triangleFields.add(createTriangle(20, 30, 30, 20, 20, 20)); // Another order

        // When: Calculate area
        double area = TriangleScorer.getTriangleSetArea(triangleFields);

        // Then: Area should be positive
        assertTrue(area >= 0, "Area should always be non-negative");
    }

    // ========== Helper Methods ==========

    /**
     * Creates a TriangleField with no owner from six coordinates.
     *
     * @param x1 first vertex x coordinate
     * @param y1 first vertex y coordinate
     * @param x2 second vertex x coordinate
     * @param y2 second vertex y coordinate
     * @param x3 third vertex x coordinate
     * @param y3 third vertex y coordinate
     * @return a new TriangleField instance
     */
    private TriangleField createTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        return createTriangle(x1, y1, x2, y2, x3, y3, null);
    }

    /**
     * Creates a TriangleField with specified owner from six coordinates.
     *
     * @param x1 first vertex x coordinate
     * @param y1 first vertex y coordinate
     * @param x2 second vertex x coordinate
     * @param y2 second vertex y coordinate
     * @param x3 third vertex x coordinate
     * @param y3 third vertex y coordinate
     * @param owner the team that owns this triangle (can be null)
     * @return a new TriangleField instance
     */
    private TriangleField createTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Team owner) {
        Point2D point1 = new Point2D.Double(x1, y1);
        Point2D point2 = new Point2D.Double(x2, y2);
        Point2D point3 = new Point2D.Double(x3, y3);
        return new TriangleField(point1, point2, point3, owner);
    }
}
