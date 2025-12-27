package com.wasteofplastic.beaconz;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TriangleScoreTest {

    @Test
    public void checkTriangeScoring(){
        Set<TriangleField> triangleFields = new HashSet<TriangleField>();
        // Two separate triangles and two intersecting triangles
        int[] data = {
                0, 0, 10, 0, 0, 10, // 50
                20, 0, 30, 0, 30, 10, // +50 non overlapping
                50, 50, 60, 50, 60, 60, // overlaps with the next one
                50, 50, 50, 60, 60, 50, // +75 total
        };
        // Create some triangles
        for (int i = 0; i < data.length; i = i + 6) {
            // Get the three points
            Point2D point1 = new Point2D.Double(data[i], data[i+1]);
            Point2D point2 = new Point2D.Double(data[i+2], data[i+3]);
            Point2D point3 = new Point2D.Double(data[i+4], data[i+5]);
            TriangleField tri1 = new TriangleField(point1, point2, point3, null);
            triangleFields.add(tri1);
        }
        int area = (int)TriangleScorer.getTriangleSetArea(triangleFields);
        assertThat(area, is(175));

        // Two separate triangles
        int[] data1 = {
                0, 0, 10, 0, 0, 10, // 50
                20, 0, 30, 0, 30, 10 // +50 non overlapping
        };
        // Create some triangles
        for (int i = 0; i < data1.length; i = i + 6) {
            // Get the three points
            Point2D point1 = new Point2D.Double(data1[i], data1[i+1]);
            Point2D point2 = new Point2D.Double(data1[i+2], data1[i+3]);
            Point2D point3 = new Point2D.Double(data1[i+4], data1[i+5]);
            TriangleField tri1 = new TriangleField(point1, point2, point3, null);
            triangleFields.add(tri1);
        }
        area = (int)TriangleScorer.getTriangleSetArea(triangleFields);
        assertThat(area, is(100));

        // Two  triangles that have a common corner
        int[] data2 = {
                0, 0, 10, 0, 0, 10, // 50
                0, 0, -10, -10, -10, 0 // +50 non overlapping
        };
        // Create some triangles
        for (int i = 0; i < data2.length; i = i + 6) {
            // Get the three points
            Point2D point1 = new Point2D.Double(data2[i], data2[i+1]);
            Point2D point2 = new Point2D.Double(data2[i+2], data2[i+3]);
            Point2D point3 = new Point2D.Double(data2[i+4], data2[i+5]);
            TriangleField tri1 = new TriangleField(point1, point2, point3, null);
            triangleFields.add(tri1);
        }
        area = (int)TriangleScorer.getTriangleSetArea(triangleFields);
        assertThat(area, is(100));
        // Four triangles all interecting
        int[] data3 = {
                0, 0, -20, 40, 30,50,
                10, 30, 20,0, 40,20,
                30,20, 70,30, 70, 60,
                40,0, 70,0, 60, 50
        };
        // Create some triangles
        for (int i = 0; i < data3.length; i = i + 6) {
            // Get the three points
            Point2D point1 = new Point2D.Double(data3[i], data3[i+1]);
            Point2D point2 = new Point2D.Double(data3[i+2], data3[i+3]);
            Point2D point3 = new Point2D.Double(data3[i+4], data3[i+5]);
            TriangleField tri1 = new TriangleField(point1, point2, point3, null);
            triangleFields.add(tri1);
        }
        area = (int)TriangleScorer.getTriangleSetArea(triangleFields);
        assertThat(area, is(2658));
    }
}
