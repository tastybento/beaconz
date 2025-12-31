package com.wasteofplastic.beaconz;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;


/**
 * @author tastybento
 *
 */
public class BeaconObjTest {

    /**
     * Test method for {@link com.wasteofplastic.beaconz.BeaconObj#getX()}.
     */
    @Test
    public void testGetX() {
        BeaconObj beacon = new BeaconObj(null, 10, 70, -10, null);
        assertThat(beacon.getX(), is(10));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.BeaconObj#getY()}.
     */
    @Test
    public void testGetY() {
        BeaconObj beacon = new BeaconObj(null, 10, 70, -10, null);
        assertThat(beacon.getY(), is(70));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.BeaconObj#getZ()}.
     */
    @Test
    public void testGetZ() {
        BeaconObj beacon = new BeaconObj(null, 10, 70, -10, null);
        assertThat(beacon.getZ(), is(-10));
    }

    /**
     * Test method for {@link com.wasteofplastic.beaconz.BeaconObj#getPoint()}.
     */
    @Test
    public void testGetPoint() {
        BeaconObj beacon = new BeaconObj(null, 10, 70, -10, null);
        Point2D point = new Point2D.Double(10,-10);
        assertThat(beacon.getPoint(), is(point));
    }

}
