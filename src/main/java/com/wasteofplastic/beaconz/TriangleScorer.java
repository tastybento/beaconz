package com.wasteofplastic.beaconz;

import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.scoreboard.Team;

/**
 * Scores triangles
 * @author tastybento
 *
 */
public class TriangleScorer {

    /**
     * Returns score for team
     * @param triangleFields
     * @param team
     * @return
     */
    public static double getScore(Set<TriangleField> triangleFields, Team team) {
        // Get the team's triangles
        Set<TriangleField> teamTriangles = new HashSet<TriangleField>();

        for (TriangleField triangle : triangleFields) {
            if (triangle.getOwner() != null && triangle.getOwner().equals(team)) {
                teamTriangles.add(triangle);
            }
        }
        //System.out.println("DEBUG: there are " + teamTriangles.size() + " team triangles");
        return getTriangleSetArea(teamTriangles);
    }

    /**
     * Returns area for a set of overlapping or non-overlapping triangles
     * @param teamTriangles
     * @return
     */
    public static double getTriangleSetArea(Set<TriangleField> teamTriangles) {
        double area = 0;
        //int count = 0;
        Set<TriangleField> alreadyCounted = new HashSet<TriangleField>();
        // Run through the list and gather the areas
        Iterator<TriangleField> mainIt = teamTriangles.iterator();
        while (mainIt.hasNext()) {
            // Grab a triangle
            TriangleField mainTriangle = mainIt.next();
            if (alreadyCounted.contains(mainTriangle)) {
                //System.out.println("DEBUG: Removing already counted triangle");
                mainIt.remove();
                continue;
            }
            //count++;
            //System.out.println("DEBUG: count = " + count);

            // Temp area that will hold all unions of this triangle
            //System.out.println("DEBUG: Triangle points " + mainTriangle.a + " " + mainTriangle.b + " " + mainTriangle.c);
            Area polyArea = new Area(mainTriangle.getTriangle());
            /*
            PathIterator pathIterator = polyArea.getPathIterator(null);
            float[] floats = new float[6];
            List<Point2D> poly = new ArrayList<Point2D>();
            while (!pathIterator.isDone()) {
                pathIterator.currentSegment(floats);
                Point2D point = new Point2D.Float(floats[0], floats[1]);
                //System.out.println("DEBUG: Initial Poly area point " + point);
                poly.add(point); 
                pathIterator.next();
            } */        
            // Remove it from the list as its area will be counted even if there are no unions
            mainIt.remove();
            // Flag to track the status of unions
            boolean noMoreUnions = true;
            // Loop until there are no more unions - due to ordering, a triangle later in the list could enable
            // a triangle earlier in the list to be unionable, so it must be do repeatedly until there are no more
            // unions.
            do {
                noMoreUnions = true;  
                //System.out.println("DEBUG: Checking this triangle against others ");
                // Iterate through all remaining triangles
                //System.out.println("DEBUG: Iterate through all remaining triangles - there are " + teamTriangles.size() + " left");
                Iterator<TriangleField> internalIt = teamTriangles.iterator();
                while (internalIt.hasNext()) {
                    TriangleField tri = internalIt.next();
                    if (alreadyCounted.contains(tri)) {
                        continue;
                    }
                    // Try to union this triangle with the others
                    //System.out.println("DEBUG: Try to union this triangle with the others");
                    Area tempUnion = new Area(polyArea);
                    /*
                    pathIterator = tempUnion.getPathIterator(null);
                    floats = new float[6];
                    poly = new ArrayList<Point2D>();
                    while (!pathIterator.isDone()) {
                        pathIterator.currentSegment(floats);
                        Point2D point = new Point2D.Float(floats[0], floats[1]);

                        System.out.println("DEBUG: Poly area point " + point);
                        poly.add(point); 
                        pathIterator.next();
                    } 
                    System.out.println("DEBUG: Test triangle points " + tri.a + " " + tri.b + " " + tri.c);
                    */
                    tempUnion.add(new Area(tri.getTriangle()));
                    /*
                    pathIterator = tempUnion.getPathIterator(null);
                    floats = new float[6];
                    poly = new ArrayList<Point2D>();
                    while (!pathIterator.isDone()) {
                        pathIterator.currentSegment(floats);
                        Point2D point = new Point2D.Float(floats[0], floats[1]);

                        System.out.println("DEBUG: Post union point " + point);
                        poly.add(point); 
                        pathIterator.next();
                    } */         
                    // If a triangle becomes part of the polygon, then it will form another polygon
                    // isSingular checks if the resulting polygon has a single path or not
                    if (tempUnion.isSingular()) {
                        //System.out.println("DEBUG: is Singular");
                        // It's a good union - remove the triangle
                        polyArea = tempUnion;
                        // Add it to the already counted set
                        alreadyCounted.add(tri);
                        noMoreUnions = false;
                    } 
                    /*else {
                        System.out.println("DEBUG: is not singular"); 
                    }*/
                }
            } while (!noMoreUnions);
            //System.out.println("DEBUG: No more unions");
            // Now add the area to the total
            //System.out.println("DEBUG: Add area to total");
            // Now calculate the area of the resulting polygon
            PathIterator pathIterator = polyArea.getPathIterator(null);
            float[] floats = new float[6];
            List<Point2D> poly = new ArrayList<Point2D>();
            while (!pathIterator.isDone()) {
                pathIterator.currentSegment(floats);
                Point2D point = new Point2D.Float(floats[0], floats[1]);

                //System.out.println("Adding point " + point);
                poly.add(point); 
                pathIterator.next();
            }            
            double pArea = polygonArea(poly.toArray(new Point2D[poly.size()]));
            //System.out.println("ploygon area = " + pArea);
            area = area + pArea;
        }

        //System.out.println("Total area = " + area);
        return area;
    }

    /**
     * Function to calculate the area of a polygon, according to the algorithm
     * defined at http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
     * 
     * @param polyPoints
     *            array of points in the polygon
     * @return area of the polygon defined by pgPoints
     */
    private static double polygonArea(Point2D[] polyPoints) {
        int i, j, n = polyPoints.length;
        double area = 0;

        for (i = 0; i < n; i++) {
            j = (i + 1) % n;
            area += polyPoints[i].getX() * polyPoints[j].getY();
            area -= polyPoints[j].getX() * polyPoints[i].getY();
        }
        area /= 2.0;
        return Math.abs(area);
    }
}
