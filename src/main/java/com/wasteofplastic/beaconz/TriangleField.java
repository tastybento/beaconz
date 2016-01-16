/*
 * Copyright (c) 2015 tastybento
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

package com.wasteofplastic.beaconz;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.scoreboard.Team;

/**
 * Represents a 2D triangular space that a faction can own
 *  
 * @author tastybento
 *
 */
public class TriangleField {
    public Team owner;
    public Point2D a;
    public Point2D b;
    public Point2D c;
    public double area;
    private Polygon triangle;
    private Set<Line2D> sides;

    /**
     * Fields are 2D. Only x and z coordinates count
     * @param point2d
     * @param point2d2
     * @param point2d3
     * @param owner
     */
    public TriangleField(Point2D point2d, Point2D point2d2, Point2D point2d3, Team owner) {
        this.owner = owner;
        this.triangle = new Polygon();
        this.triangle.addPoint((int)point2d.getX(), (int)point2d.getY());
        this.triangle.addPoint((int)point2d2.getX(), (int)point2d2.getY());
        this.triangle.addPoint((int)point2d3.getX(), (int)point2d3.getY());
        this.a = point2d;
        this.b = point2d2;
        this.c = point2d3;
        this.sides = new HashSet<Line2D>();
        sides.add(new Line2D.Double(a,b));
        sides.add(new Line2D.Double(b,c));
        sides.add(new Line2D.Double(c,a));
        //System.out.println("DEBUG: Control field made " + a.toString() + " " + b.toString() + " " + c.toString());
        double d = (a.getX() * (b.getY() - c.getY()) + b.getX() * (c.getY() - a.getY())
            + c.getX() * (a.getY() - b.getY())) / 2D;
        this.area = Math.abs(d);
        //System.out.println("DEBUG: area = " + area);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        //System.out.println("DEBUG: hashCode " + a.toString() + " " + b.toString() + " " + c.toString());
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((a == null) ? 0 : a.hashCode());
        result = prime * result + ((b == null) ? 0 : b.hashCode());
        result = prime * result + ((c == null) ? 0 : c.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TriangleField)) {
            return false;
        }
        TriangleField other = (TriangleField) obj;
        // TODO: Handle nulls
        // Fields are equal as long as the three points are the same
        if (!a.equals(other.a) && !a.equals(other.b) && !a.equals(other.c)) {
            return false;
        }
        if (!b.equals(other.a) && !b.equals(other.b) && !b.equals(other.c)) {
            return false;
        }
        if (!c.equals(other.a) && !c.equals(other.b) && !c.equals(other.c)) {
            return false;
        }
        return true;
    }
    
    /**
     * @return the owner
     */
    public Team getOwner() {
    return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Team owner) {
    this.owner = owner;
    }
    
    public int getArea() {
    return (int) area;
    }

    public Team contains(int x, int y) {
        if (triangle.contains(x,y)) {
            return owner;
        }
        return null;
    }
    
    public boolean contains(Point2D point) {
    return triangle.contains(point);
    }
    
    @Override
    public String toString() {
    return (int)a.getX() + ":" + (int)a.getY() + ":" + (int)b.getX() + ":" + (int)b.getY() + ":"
        + (int)c.getX() + ":" + (int)c.getY() + ":" + owner.getName();
    }

    /**
     * Checks if a point is a vertex in this triangle
     * @param point
     * @return
     */
    public boolean hasVertex(Point2D point) {
        if (a.equals(point) || b.equals(point) || c.equals(point)) {
            return true;
        }
        return false;
    }

    /**
     * Get triangle sides
     * @return the sides of the triangle
     */
    public Set<Line2D> getSides() {
        return sides;
    }

    /**
     * Checks whether this triangle contains another triangle or overlaps
     * Will be true if any point on the triangle is inside the triangle
     * @param triangle2
     * @return
     */
    public boolean contains(TriangleField triangle2) {
        if (triangle.contains(triangle2.a) || triangle.contains(triangle2.b) || triangle.contains(triangle2.c)) {
            return true;
        }
        return false;
    }


}
