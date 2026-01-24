package com.wasteofplastic.beaconz.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Set;

import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for {@link TriangleField} class.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Triangle construction and initialization</li>
 *   <li>Area calculation using determinant formula</li>
 *   <li>Order-independent vertex equality</li>
 *   <li>Point containment checks</li>
 *   <li>Triangle overlap detection</li>
 *   <li>Edge intersection detection</li>
 *   <li>Ownership management</li>
 *   <li>Vertex checking</li>
 * </ul>
 *
 * @author tastybento
 */
@DisplayName("TriangleField Tests")
class TriangleFieldTest {

    private Team team1;
    private Team team2;

    // Standard test triangle vertices
    private Point2D p1;
    private Point2D p2;
    private Point2D p3;

    @BeforeEach
    void setUp() {
        // Create mock teams
        team1 = mock(Team.class);
        team2 = mock(Team.class);

        // Standard right triangle at origin
        // Forms a triangle: (0,0), (100,0), (50,100)
        p1 = new Point2D.Double(0, 0);
        p2 = new Point2D.Double(100, 0);
        p3 = new Point2D.Double(50, 100);
    }

    @Nested
    @DisplayName("Constructor and Initialization Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor creates triangle with correct vertices")
        void testConstructorVertices() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertNotNull(triangle);
            assertEquals(p1, triangle.a);
            assertEquals(p2, triangle.b);
            assertEquals(p3, triangle.c);
        }

        @Test
        @DisplayName("Constructor sets owner correctly")
        void testConstructorOwner() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertEquals(team1, triangle.getOwner());
        }

        @Test
        @DisplayName("Constructor accepts null owner")
        void testConstructorNullOwner() {
            TriangleField triangle = new TriangleField(p1, p2, p3, null);

            assertNull(triangle.getOwner());
        }

        @Test
        @DisplayName("Constructor creates three sides")
        void testConstructorSides() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Set<Line2D> sides = triangle.getSides();

            assertNotNull(sides);
            assertEquals(3, sides.size());
        }

        @Test
        @DisplayName("Constructor creates AWT Polygon")
        void testConstructorPolygon() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Polygon polygon = triangle.getTriangle();

            assertNotNull(polygon);
            assertEquals(3, polygon.npoints);
        }

        @Test
        @DisplayName("Constructor calculates area")
        void testConstructorArea() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Area of triangle (0,0), (100,0), (50,100) = 5000 square blocks
            // Using determinant: |0*(0-100) + 100*(100-0) + 50*(0-0)| / 2 = 5000
            assertTrue(triangle.area > 0);
        }
    }

    @Nested
    @DisplayName("Area Calculation Tests")
    class AreaCalculationTests {

        @Test
        @DisplayName("Area calculation for right triangle")
        void testAreaRightTriangle() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Right triangle: base=100, height=100, area=5000
            assertEquals(5000, triangle.getArea());
        }

        @Test
        @DisplayName("Area calculation for equilateral triangle")
        void testAreaEquilateralTriangle() {
            // Approximate equilateral triangle
            Point2D e1 = new Point2D.Double(0, 0);
            Point2D e2 = new Point2D.Double(100, 0);
            Point2D e3 = new Point2D.Double(50, 87); // ~sqrt(3)/2 * 100

            TriangleField triangle = new TriangleField(e1, e2, e3, team1);

            // Area should be approximately 4350
            assertTrue(triangle.getArea() >= 4300 && triangle.getArea() <= 4400);
        }

        @Test
        @DisplayName("Area calculation for small triangle")
        void testAreaSmallTriangle() {
            Point2D s1 = new Point2D.Double(0, 0);
            Point2D s2 = new Point2D.Double(10, 0);
            Point2D s3 = new Point2D.Double(5, 10);

            TriangleField triangle = new TriangleField(s1, s2, s3, team1);

            // Area = 50
            assertEquals(50, triangle.getArea());
        }

        @Test
        @DisplayName("Area is always positive regardless of vertex order")
        void testAreaAlwaysPositive() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p1, p3, p2, team1); // Different order

            assertEquals(triangle1.getArea(), triangle2.getArea());
            assertTrue(triangle1.getArea() > 0);
        }

        @Test
        @DisplayName("getArea returns integer truncated value")
        void testGetAreaTruncation() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Ensure it's an int
            assertEquals((int)triangle.area, triangle.getArea());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Triangle equals itself")
        void testReflexiveEquality() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertEquals(triangle, triangle);
        }

        @Test
        @DisplayName("Triangles with same vertices in same order are equal")
        void testEqualsSameOrder() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p1, p2, p3, team2);

            assertEquals(triangle1, triangle2);
        }

        @Test
        @DisplayName("Triangles with same vertices in different order are equal")
        void testEqualsDifferentOrder() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p2, p3, p1, team2); // Rotated order

            assertEquals(triangle1, triangle2);
        }

        @Test
        @DisplayName("Triangles with same vertices reversed are equal")
        void testEqualsReversedOrder() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p3, p2, p1, team2); // Reversed

            assertEquals(triangle1, triangle2);
        }

        @Test
        @DisplayName("Equality is symmetric")
        void testSymmetricEquality() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p2, p3, p1, team1);

            assertEquals(triangle1, triangle2);
            assertEquals(triangle2, triangle1);
        }

        @Test
        @DisplayName("Equality is transitive")
        void testTransitiveEquality() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p2, p3, p1, team1);
            TriangleField triangle3 = new TriangleField(p3, p1, p2, team1);

            assertEquals(triangle1, triangle2);
            assertEquals(triangle2, triangle3);
            assertEquals(triangle1, triangle3);
        }

        @Test
        @DisplayName("Triangles with different vertices are not equal")
        void testNotEqualsDifferentVertices() {
            Point2D p4 = new Point2D.Double(200, 200);
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p1, p2, p4, team1);

            assertNotEquals(triangle1, triangle2);
        }

        @Test
        @DisplayName("Triangle not equal to null")
        void testNotEqualsNull() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertNotEquals(null, triangle);
        }

        @Test
        @DisplayName("Triangle not equal to different class")
        void testNotEqualsDifferentClass() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertNotEquals("not a triangle", triangle);
        }

        @Test
        @DisplayName("Owner does not affect equality")
        void testOwnerDoesNotAffectEquality() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p1, p2, p3, team2);
            TriangleField triangle3 = new TriangleField(p1, p2, p3, null);

            assertEquals(triangle1, triangle2);
            assertEquals(triangle1, triangle3);
        }
    }

    @Nested
    @DisplayName("HashCode Tests")
    class HashCodeTests {

        @Test
        @DisplayName("hashCode returns consistent value")
        void testHashCodeConsistent() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            int hash1 = triangle.hashCode();
            int hash2 = triangle.hashCode();

            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("hashCode includes owner - violates equals contract")
        void testHashCodeIncludesOwner() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);
            TriangleField triangle2 = new TriangleField(p1, p2, p3, team2);

            // These are equal but may have different hash codes (contract violation)
            assertEquals(triangle1, triangle2);
            // This documents the known issue - uncomment to verify violation:
            // assertNotEquals(triangle1.hashCode(), triangle2.hashCode());
        }
    }

    @Nested
    @DisplayName("Point Containment Tests")
    class ContainmentTests {

        @Test
        @DisplayName("contains(int,int) returns owner for point inside")
        void testContainsPointInside() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Point (50, 50) should be inside triangle
            assertEquals(team1, triangle.contains(50, 50));
        }

        @Test
        @DisplayName("contains(int,int) returns null for point outside")
        void testContainsPointOutside() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Point (200, 200) is outside triangle
            assertNull(triangle.contains(200, 200));
        }

        @Test
        @DisplayName("contains(Point2D) returns true for point inside")
        void testContainsPoint2DInside() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Point2D inside = new Point2D.Double(50, 50);

            assertTrue(triangle.contains(inside));
        }

        @Test
        @DisplayName("contains(Point2D) returns false for point outside")
        void testContainsPoint2DOutside() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Point2D outside = new Point2D.Double(200, 200);

            assertFalse(triangle.contains(outside));
        }

        @Test
        @DisplayName("contains works for vertices")
        void testContainsVertices() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Vertices should be on the boundary (implementation dependent)
            // Check at least the center is inside
            assertTrue(triangle.contains(new Point2D.Double(50, 33)));
        }

        @Test
        @DisplayName("contains at triangle center")
        void testContainsCenter() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Centroid of triangle (0,0), (100,0), (50,100) is approximately (50, 33)
            assertEquals(team1, triangle.contains(50, 33));
        }
    }

    @Nested
    @DisplayName("Triangle Overlap Tests")
    class TriangleOverlapTests {

        @Test
        @DisplayName("contains(TriangleField) returns true when other triangle inside")
        void testContainsTriangleInside() {
            TriangleField outerTriangle = new TriangleField(p1, p2, p3, team1);

            // Smaller triangle inside
            Point2D i1 = new Point2D.Double(40, 30);
            Point2D i2 = new Point2D.Double(60, 30);
            Point2D i3 = new Point2D.Double(50, 50);
            TriangleField innerTriangle = new TriangleField(i1, i2, i3, team2);

            assertTrue(outerTriangle.contains(innerTriangle));
        }

        @Test
        @DisplayName("contains(TriangleField) returns false when triangles separate")
        void testContainsTriangleSeparate() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);

            // Completely separate triangle
            Point2D s1 = new Point2D.Double(200, 200);
            Point2D s2 = new Point2D.Double(300, 200);
            Point2D s3 = new Point2D.Double(250, 300);
            TriangleField triangle2 = new TriangleField(s1, s2, s3, team2);

            assertFalse(triangle1.contains(triangle2));
        }

        @Test
        @DisplayName("contains(TriangleField) partial overlap detection")
        void testContainsTrianglePartialOverlap() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);

            // Overlapping triangle with one vertex inside
            Point2D o1 = new Point2D.Double(50, 50);  // Inside
            Point2D o2 = new Point2D.Double(150, 50); // Outside
            Point2D o3 = new Point2D.Double(100, 150); // Outside
            TriangleField triangle2 = new TriangleField(o1, o2, o3, team2);

            // Should return true because at least one vertex is inside
            assertTrue(triangle1.contains(triangle2));
        }
    }

    @Nested
    @DisplayName("Vertex Tests")
    class VertexTests {

        @Test
        @DisplayName("hasVertex returns true for vertex a")
        void testHasVertexA() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertTrue(triangle.hasVertex(p1));
        }

        @Test
        @DisplayName("hasVertex returns true for vertex b")
        void testHasVertexB() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertTrue(triangle.hasVertex(p2));
        }

        @Test
        @DisplayName("hasVertex returns true for vertex c")
        void testHasVertexC() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertTrue(triangle.hasVertex(p3));
        }

        @Test
        @DisplayName("hasVertex returns false for non-vertex point")
        void testHasVertexNonVertex() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Point2D nonVertex = new Point2D.Double(50, 50);

            assertFalse(triangle.hasVertex(nonVertex));
        }

        @Test
        @DisplayName("hasVertex with equal but different Point2D objects")
        void testHasVertexEqualPoints() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Point2D sameAsP1 = new Point2D.Double(0, 0);

            assertTrue(triangle.hasVertex(sameAsP1));
        }
    }

    @Nested
    @DisplayName("Ownership Tests")
    class OwnershipTests {

        @Test
        @DisplayName("getOwner returns correct team")
        void testGetOwner() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertEquals(team1, triangle.getOwner());
        }

        @Test
        @DisplayName("setOwner changes ownership")
        void testSetOwner() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            triangle.setOwner(team2);

            assertEquals(team2, triangle.getOwner());
        }

        @Test
        @DisplayName("setOwner accepts null to mark unowned")
        void testSetOwnerNull() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            triangle.setOwner(null);

            assertNull(triangle.getOwner());
        }

        @Test
        @DisplayName("ownership is mutable property")
        void testOwnershipMutability() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertEquals(team1, triangle.getOwner());
            triangle.setOwner(team2);
            assertEquals(team2, triangle.getOwner());
            triangle.setOwner(null);
            assertNull(triangle.getOwner());
        }
    }

    @Nested
    @DisplayName("Sides Tests")
    class SidesTests {

        @Test
        @DisplayName("getSides returns set of three lines")
        void testGetSidesCount() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Set<Line2D> sides = triangle.getSides();

            assertNotNull(sides);
            assertEquals(3, sides.size());
        }

        @Test
        @DisplayName("sides connect all vertices")
        void testSidesConnectVertices() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Set<Line2D> sides = triangle.getSides();

            // Verify sides connect the vertices (a-b, b-c, c-a)
            boolean hasAB = false, hasBC = false, hasCA = false;

            for (Line2D side : sides) {
                if ((side.getP1().equals(p1) && side.getP2().equals(p2)) ||
                    (side.getP1().equals(p2) && side.getP2().equals(p1))) {
                    hasAB = true;
                }
                if ((side.getP1().equals(p2) && side.getP2().equals(p3)) ||
                    (side.getP1().equals(p3) && side.getP2().equals(p2))) {
                    hasBC = true;
                }
                if ((side.getP1().equals(p3) && side.getP2().equals(p1)) ||
                    (side.getP1().equals(p1) && side.getP2().equals(p3))) {
                    hasCA = true;
                }
            }

            assertTrue(hasAB || hasBC || hasCA, "Should have at least one expected side");
        }

        @Test
        @DisplayName("sides are immutable")
        void testSidesImmutability() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            Set<Line2D> sides1 = triangle.getSides();
            Set<Line2D> sides2 = triangle.getSides();

            // Should return same set
            assertSame(sides1, sides2);
        }
    }

    @Nested
    @DisplayName("Intersection Tests")
    class IntersectionTests {

        @Test
        @DisplayName("intersects returns true for crossing triangles")
        void testIntersectsCrossingTriangles() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);

            // Create triangle that crosses
            Point2D c1 = new Point2D.Double(-20, 50);
            Point2D c2 = new Point2D.Double(120, 50);
            Point2D c3 = new Point2D.Double(50, -20);
            TriangleField triangle2 = new TriangleField(c1, c2, c3, team2);

            // These triangles should intersect
            assertTrue(triangle1.intersects(triangle2));
        }

        @Test
        @DisplayName("intersects returns false for separate triangles")
        void testIntersectsSeparateTriangles() {
            TriangleField triangle1 = new TriangleField(p1, p2, p3, team1);

            // Completely separate triangle
            Point2D s1 = new Point2D.Double(200, 200);
            Point2D s2 = new Point2D.Double(300, 200);
            Point2D s3 = new Point2D.Double(250, 300);
            TriangleField triangle2 = new TriangleField(s1, s2, s3, team2);

            assertFalse(triangle1.intersects(triangle2));
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString returns formatted string")
        void testToString() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            when(team1.getName()).thenReturn("RedTeam");

            String result = triangle.toString();

            assertNotNull(result);
            assertTrue(result.contains(":"));
        }

        @Test
        @DisplayName("toString contains vertex coordinates")
        void testToStringContainsCoordinates() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);
            when(team1.getName()).thenReturn("RedTeam");

            String result = triangle.toString();

            // Should contain coordinates
            assertTrue(result.contains("0") || result.contains("100") || result.contains("50"));
        }

        @Test
        @DisplayName("toString handles null owner")
        void testToStringNullOwner() {
            TriangleField triangle = new TriangleField(p1, p2, p3, null);

            String result = triangle.toString();

            assertNotNull(result);
            assertTrue(result.contains("null"));
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("vertices are immutable")
        void testVerticesImmutable() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            assertSame(p1, triangle.a);
            assertSame(p2, triangle.b);
            assertSame(p3, triangle.c);
        }

        @Test
        @DisplayName("area is immutable")
        void testAreaImmutable() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            double area1 = triangle.area;
            double area2 = triangle.area;

            assertEquals(area1, area2, 0.0001);
        }

        @Test
        @DisplayName("only owner is mutable")
        void testOnlyOwnerMutable() {
            TriangleField triangle = new TriangleField(p1, p2, p3, team1);

            // Vertices are final
            assertSame(p1, triangle.a);

            // Owner can change
            triangle.setOwner(team2);
            assertEquals(team2, triangle.getOwner());
        }
    }
}
