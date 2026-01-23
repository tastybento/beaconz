/*
 * Copyright (c) 2015 - 2025 tastybento
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wasteofplastic.beaconz.core;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.scoreboard.Team;

/**
 * Represents a 2D triangular control field in the Beaconz game.
 *
 * <p>A TriangleField is a strategic area formed by connecting three beacons owned by
 * the same team. These fields are a core gameplay mechanic that allows teams to claim
 * territory and score points based on the area controlled.
 *
 * <p><b>Game Mechanics:</b>
 * <ul>
 *   <li>Created when a team successfully links three of their beacons</li>
 *   <li>Each triangle represents controlled territory for a team</li>
 *   <li>Contributes to team score based on the triangular area (in square blocks)</li>
 *   <li>Can overlap with other triangles (multiple levels of control)</li>
 *   <li>Used to determine which team controls specific map locations</li>
 * </ul>
 *
 * <p><b>Coordinate System:</b>
 * Triangle fields are 2D constructs using only X and Z coordinates (horizontal plane).
 * Point2D.getX() represents Minecraft's X coordinate (east-west), and Point2D.getY()
 * represents Minecraft's Z coordinate (north-south). The vertical Y axis is ignored
 * as fields span all heights.
 *
 * <p><b>Geometry:</b>
 * <ul>
 *   <li>Three vertices (points a, b, c) define the triangle corners</li>
 *   <li>Three sides connect the vertices</li>
 *   <li>Area is calculated using the determinant formula: |det/2|</li>
 *   <li>Uses Java AWT Polygon for efficient containment checks</li>
 * </ul>
 *
 * <p><b>Equality:</b>
 * Two TriangleFields are considered equal if they have the same three vertices,
 * regardless of order or ownership. This means a triangle at points (A,B,C) equals
 * a triangle at points (B,C,A) or (C,A,B).
 *
 * <p><b>Collision Detection:</b>
 * The class provides methods to check:
 * <ul>
 *   <li>If a point is inside the triangle</li>
 *   <li>If another triangle overlaps this one</li>
 *   <li>If sides intersect with another triangle's sides</li>
 *   <li>If any vertex of another triangle is inside this triangle</li>
 * </ul>
 *
 * <p><b>Usage in Game:</b>
 * Stored in {@link com.wasteofplastic.beaconz.game.Register#getTriangleFields()} and used by:
 * <ul>
 *   <li>Scoring system to calculate triangle-based scores</li>
 *   <li>Player movement listener to detect territory changes</li>
 *   <li>Game state persistence for saving/loading triangles</li>
 * </ul>
 *
 * <p><b>Immutability:</b>
 * The vertices (a, b, c) and calculated properties (area, sides) are immutable.
 * Only the owner can be changed after construction.
 *
 * @author tastybento
 * @see BeaconLink
 * @see com.wasteofplastic.beaconz.game.Register
 */
public class TriangleField {

    /** The team that owns this triangle field */
    public Team owner;

    /** First vertex of the triangle (immutable) */
    public final Point2D a;

    /** Second vertex of the triangle (immutable) */
    public final Point2D b;

    /** Third vertex of the triangle (immutable) */
    public final Point2D c;

    /** The calculated area of the triangle in square blocks (immutable) */
    public final double area;

    /** AWT Polygon representation for efficient containment checks */
    private final Polygon triangle;

    /** Set of three Line2D objects representing the triangle's sides */
    private final Set<Line2D> sides;

    /**
     * Constructs a new triangular field from three points.
     *
     * <p>This constructor performs several initialization steps:
     * <ol>
     *   <li>Stores the owner team</li>
     *   <li>Creates vertices from the three points (order matters for rendering)</li>
     *   <li>Builds an AWT Polygon for efficient containment checks</li>
     *   <li>Constructs three Line2D sides connecting the vertices</li>
     *   <li>Calculates the area using the determinant formula</li>
     * </ol>
     *
     * <p><b>Area Calculation:</b><br>
     * Uses the determinant formula for triangle area:
     * <pre>
     * area = |det(a, b, c)| / 2
     * where det = a.x(b.y - c.y) + b.x(c.y - a.y) + c.x(a.y - b.y)
     * </pre>
     *
     * <p><b>Important Note:</b>
     * Fields are 2D constructs. Only X and Z coordinates are used:
     * <ul>
     *   <li>Point2D.getX() = Minecraft X coordinate (east-west)</li>
     *   <li>Point2D.getY() = Minecraft Z coordinate (north-south)</li>
     *   <li>Minecraft Y coordinate (height) is completely ignored</li>
     * </ul>
     *
     * <p><b>Sides Created:</b>
     * <ul>
     *   <li>Side 1: a → b</li>
     *   <li>Side 2: b → c</li>
     *   <li>Side 3: c → a</li>
     * </ul>
     *
     * @param point1 first vertex of the triangle (becomes vertex 'a')
     * @param point2 second vertex of the triangle (becomes vertex 'b')
     * @param point3 third vertex of the triangle (becomes vertex 'c')
     * @param owner the team that owns this triangle field
     */
    public TriangleField(Point2D point1, Point2D point2, Point2D point3, Team owner) {
        this.owner = owner;
        this.triangle = new Polygon();
        this.triangle.addPoint((int)point1.getX(), (int)point1.getY());
        this.triangle.addPoint((int)point2.getX(), (int)point2.getY());
        this.triangle.addPoint((int)point3.getX(), (int)point3.getY());
        this.a = point1;
        this.b = point2;
        this.c = point3;
        this.sides = new HashSet<>();
        sides.add(new Line2D.Double(a,b));
        sides.add(new Line2D.Double(b,c));
        sides.add(new Line2D.Double(c,a));
        double d = (a.getX() * (b.getY() - c.getY()) + b.getX() * (c.getY() - a.getY())
                + c.getX() * (a.getY() - b.getY())) / 2D;
        this.area = Math.abs(d);
    }

    /**
     * Generates a hash code for this triangle field.
     *
     * <p>The hash code is based on:
     * <ul>
     *   <li>The owner team</li>
     *   <li>All three vertices (a, b, c)</li>
     * </ul>
     *
     * <p><b>Implementation Note:</b>
     * This hash code includes the owner, which means two geometrically identical
     * triangles with different owners will have different hash codes. This is
     * inconsistent with the {@link #equals(Object)} method which ignores ownership.
     *
     * <p><b>Warning:</b> This violates the equals-hashCode contract. Two objects
     * that are equal according to {@link #equals(Object)} may have different hash
     * codes, which can cause issues when using TriangleField in hash-based collections.
     *
     * @return hash code value for this triangle
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((a == null) ? 0 : a.hashCode());
        result = prime * result + ((b == null) ? 0 : b.hashCode());
        result = prime * result + ((c == null) ? 0 : c.hashCode());
        return result;
    }

    /**
     * Determines if this triangle equals another object.
     *
     * <p>Two TriangleFields are considered equal if they have the same three vertices,
     * regardless of:
     * <ul>
     *   <li>Vertex order (A,B,C) equals (B,C,A) equals (C,A,B)</li>
     *   <li>Owner team (ownership is ignored in equality check)</li>
     * </ul>
     *
     * <p><b>Algorithm:</b>
     * Checks that each of this triangle's vertices (a, b, c) appears somewhere
     * in the other triangle's vertices. If all three vertices match (in any order),
     * the triangles are equal.
     *
     * <p><b>Examples:</b>
     * <ul>
     *   <li>Triangle(A,B,C) equals Triangle(A,B,C) ✓</li>
     *   <li>Triangle(A,B,C) equals Triangle(C,A,B) ✓</li>
     *   <li>Triangle(A,B,C) equals Triangle(B,A,C) ✓</li>
     *   <li>Triangle(A,B,C) does NOT equal Triangle(A,B,D) ✗</li>
     * </ul>
     *
     * <p><b>TODO:</b> Handle null vertices properly. Current implementation may
     * throw NullPointerException if any vertex is null.
     *
     * @param obj the object to compare with
     * @return true if obj is a TriangleField with the same three vertices (in any order)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TriangleField other)) {
            return false;
        }
        // TODO: Handle nulls
        // Fields are equal as long as the three points are the same
        if (!a.equals(other.a) && !a.equals(other.b) && !a.equals(other.c)) {
            return false;
        }
        if (!b.equals(other.a) && !b.equals(other.b) && !b.equals(other.c)) {
            return false;
        }
        return c.equals(other.a) || c.equals(other.b) || c.equals(other.c);
    }

    /**
     * Gets the team that owns this triangle field.
     *
     * @return the owning team, or null if unowned
     */
    public Team getOwner() {
        return owner;
    }

    /**
     * Sets the team that owns this triangle field.
     *
     * <p>This is the only mutable property of a TriangleField. Ownership can
     * change if a triangle is captured by another team or abandoned.
     *
     * @param owner the new owning team (may be null to mark as unowned)
     */
    public void setOwner(Team owner) {
        this.owner = owner;
    }

    /**
     * Gets the area of the triangle in square blocks.
     *
     * <p>The area is calculated during construction using the determinant formula
     * and returned as an integer (fractional blocks are truncated).
     *
     * @return the triangle's area in square blocks (whole number)
     */
    public int getArea() {
        return (int) area;
    }

    /**
     * Checks if a specific coordinate is inside this triangle.
     *
     * <p>Uses the AWT Polygon's efficient containment algorithm to determine
     * if the point (x, y) is within the triangle's boundaries.
     *
     * <p><b>Coordinate Note:</b>
     * The y parameter represents the Minecraft Z coordinate (north-south),
     * not the vertical Y axis.
     *
     * @param x the X coordinate (east-west) in blocks
     * @param y the Z coordinate (north-south) in blocks
     * @return the owner team if the point is inside this triangle, null otherwise
     */
    public Team contains(int x, int y) {
        if (triangle.contains(x,y)) {
            return owner;
        }
        return null;
    }

    /**
     * Checks if a Point2D is inside this triangle.
     *
     * <p>This is a convenience method that delegates to the AWT Polygon's
     * contains method.
     *
     * <p><b>Coordinate Note:</b>
     * Point2D.getX() = Minecraft X coordinate (east-west)<br>
     * Point2D.getY() = Minecraft Z coordinate (north-south)
     *
     * @param point the point to check for containment
     * @return true if the point is inside this triangle, false otherwise
     */
    public boolean contains(Point2D point) {
        return triangle.contains(point);
    }

    /**
     * Returns a string representation of this triangle field.
     *
     * <p>The format is: "x1:z1:x2:z2:x3:z3:teamName"
     *
     * <p><b>Example:</b>
     * "100:200:150:250:125:300:RedTeam"
     *
     * <p>This format is suitable for:
     * <ul>
     *   <li>Logging and debugging</li>
     *   <li>Serialization to configuration files</li>
     *   <li>Display in admin commands</li>
     * </ul>
     *
     * @return colon-separated string of vertices and owner name
     */
    @Override
    public String toString() {
        return (int)a.getX() + ":" + (int)a.getY() + ":" + (int)b.getX() + ":" + (int)b.getY() + ":"
                + (int)c.getX() + ":" + (int)c.getY() + ":" + owner.getName();
    }

    /**
     * Checks if a specific point is one of the vertices of this triangle.
     *
     * <p>This is useful for:
     * <ul>
     *   <li>Determining if a beacon is part of this triangle</li>
     *   <li>Validating triangle construction</li>
     *   <li>Finding connected triangles that share a vertex</li>
     * </ul>
     *
     * @param point the point to check
     * @return true if the point equals vertex a, b, or c; false otherwise
     */
    public boolean hasVertex(Point2D point) {
        return a.equals(point) || b.equals(point) || c.equals(point);
    }

    /**
     * Gets the three sides of this triangle.
     *
     * <p>Returns a set of Line2D objects representing:
     * <ul>
     *   <li>Line from a to b</li>
     *   <li>Line from b to c</li>
     *   <li>Line from c to a</li>
     * </ul>
     *
     * <p>These sides are used for:
     * <ul>
     *   <li>Intersection detection with other triangles</li>
     *   <li>Distance calculations</li>
     *   <li>Rendering triangle boundaries</li>
     * </ul>
     *
     * @return immutable set of three Line2D objects representing the triangle's sides
     */
    public Set<Line2D> getSides() {
        return sides;
    }

    /**
     * Checks if this triangle contains or overlaps with another triangle.
     *
     * <p>Returns true if ANY vertex of the other triangle is inside this triangle.
     * This is used to detect overlapping control fields.
     *
     * <p><b>Important:</b> This is NOT a complete overlap check. It only checks
     * if vertices are inside. Two triangles can overlap without any vertices
     * being inside each other (if their edges intersect). For complete overlap
     * detection, combine this with {@link #intersects(TriangleField)}.
     *
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Detecting multiple levels of control (overlapping friendly triangles)</li>
     *   <li>Validating new triangle placement</li>
     *   <li>Scoring calculations for nested control fields</li>
     * </ul>
     *
     * @param triangle2 the triangle to check for containment
     * @return true if any vertex of triangle2 is inside this triangle
     */
    public boolean contains(TriangleField triangle2) {
        return triangle.contains(triangle2.a) || triangle.contains(triangle2.b) || triangle.contains(triangle2.c);
    }

    /**
     * Gets the internal AWT Polygon representation of this triangle.
     *
     * <p>The Polygon is used internally for efficient containment checks using
     * Java's AWT geometry algorithms.
     *
     * <p><b>Warning:</b> The returned Polygon is mutable. Modifying it will
     * corrupt the triangle's geometry. Consider this method package-private
     * or return a copy in future versions.
     *
     * @return the AWT Polygon representing this triangle
     */
    public Polygon getTriangle() {
        return triangle;
    }

    /**
     * Checks if this triangle's sides intersect with another triangle's sides.
     *
     * <p>This method performs a complete edge-to-edge intersection test,
     * checking all 9 possible side combinations (3 sides × 3 sides).
     *
     * <p><b>Algorithm:</b>
     * <ol>
     *   <li>Iterate through all three sides of this triangle</li>
     *   <li>For each side, check against all three sides of the other triangle</li>
     *   <li>Return true immediately if any pair of sides intersect</li>
     * </ol>
     *
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Detecting triangle collisions during link creation</li>
     *   <li>Validating triangle placement (no crossing enemy triangles)</li>
     *   <li>Finding contested territory between teams</li>
     * </ul>
     *
     * <p><b>Note:</b> This check is complementary to {@link #contains(TriangleField)}.
     * Two triangles can intersect without containing each other's vertices.
     *
     * @param triangle2 the triangle to check for intersection
     * @return true if any side of this triangle intersects any side of triangle2
     */
    public boolean intersects(TriangleField triangle2) {
        // Check if the sides of the triangles intersect
        for (Line2D side: sides) {
            for (Line2D checkSide: triangle2.getSides()) {
                if (side.intersectsLine(checkSide)) {
                    return true;
                }
            }
        }
        return false;
    }

}
