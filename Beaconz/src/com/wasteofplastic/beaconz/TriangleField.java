package com.wasteofplastic.beaconz;

import java.awt.Polygon;
import java.awt.geom.Point2D;

import org.bukkit.scoreboard.Team;

/**
 * Represents a 2D triangular space that a faction can own
 *  
 * @author tastybento
 *
 */
public class TriangleField {
    public Team faction;
    public Point2D a;
    public Point2D b;
    public Point2D c;
    public double area;
    private Polygon triangle;

    /**
     * Fields are 2D. Only x and z coordinates count
     * @param point2d
     * @param point2d2
     * @param point2d3
     * @param owner
     */
    public TriangleField(Point2D point2d, Point2D point2d2, Point2D point2d3, Team owner) {
        this.faction = owner;
        this.triangle = new Polygon();
        this.triangle.addPoint((int)point2d.getX(), (int)point2d.getY());
        this.triangle.addPoint((int)point2d2.getX(), (int)point2d2.getY());
        this.triangle.addPoint((int)point2d3.getX(), (int)point2d3.getY());
        this.a = point2d;
        this.b = point2d2;
        this.c = point2d3;
        System.out.println("DEBUG: Control field made " + a.toString() + " " + b.toString() + " " + c.toString());
        double d = (a.getX() * (b.getY() - c.getY()) + b.getX() * (c.getY() - a.getY())
            + c.getX() * (a.getY() - b.getY())) / 2D;
        this.area = Math.abs(d);
        System.out.println("DEBUG: area = " + area);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        //System.out.println("DEBUG: hashCode " + a.toString() + " " + b.toString() + " " + c.toString());
        final int prime = 31;
        int result = 1;
        result = prime * result + ((faction == null) ? 0 : faction.hashCode());
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
     * @return the faction
     */
    public Team getFaction() {
    return faction;
    }

    /**
     * @param faction the faction to set
     */
    public void setFaction(Team faction) {
    this.faction = faction;
    }
    
    public int getArea() {
    return (int) area;
    }

    public Team contains(int x, int y) {
        if (triangle.contains(x,y)) {
            return faction;
        }
        return null;
    }
    
    public boolean contains(Point2D point) {
    return triangle.contains(point);
    }
    
    @Override
    public String toString() {
    return (int)a.getX() + ":" + (int)a.getY() + ":" + (int)b.getX() + ":" + (int)b.getY() + ":"
        + (int)c.getX() + ":" + (int)c.getY() + ":" + faction.getName();
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


}
