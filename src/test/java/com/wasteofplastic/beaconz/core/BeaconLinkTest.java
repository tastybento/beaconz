package com.wasteofplastic.beaconz.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Line2D;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link BeaconLink}.
 *
 * @author tastybento
 */
public class BeaconLinkTest {

    private BeaconObj beacon1;
    private BeaconObj beacon2;
    private BeaconObj beacon3;

    @BeforeEach
    public void setUp() {
        // Create test beacons at different coordinates
        beacon1 = new BeaconObj(null, 0, 64, 0, null);
        beacon2 = new BeaconObj(null, 10, 64, 10, null);
        beacon3 = new BeaconObj(null, 20, 64, 20, null);
    }

    /**
     * Test method for {@link BeaconLink#BeaconLink(BeaconObj, BeaconObj, Long)}.
     */
    @Test
    public void testConstructorWithTimestamp() {
        long timestamp = 1234567890L;
        BeaconLink link = new BeaconLink(beacon1, beacon2, timestamp);

        assertNotNull(link);
        assertThat(link.getBeacon1(), is(beacon1));
        assertThat(link.getBeacon2(), is(beacon2));
        assertThat(link.getTimeStamp(), is(timestamp));
    }

    /**
     * Test method for {@link BeaconLink#BeaconLink(BeaconObj, BeaconObj)}.
     */
    @Test
    public void testConstructorWithoutTimestamp() {
        long before = System.currentTimeMillis();
        BeaconLink link = new BeaconLink(beacon1, beacon2);
        long after = System.currentTimeMillis();

        assertNotNull(link);
        assertThat(link.getBeacon1(), is(beacon1));
        assertThat(link.getBeacon2(), is(beacon2));
        assertTrue(link.getTimeStamp() >= before && link.getTimeStamp() <= after);
    }

    /**
     * Test method for {@link BeaconLink#getBeacon1()}.
     */
    @Test
    public void testGetBeacon1() {
        BeaconLink link = new BeaconLink(beacon1, beacon2);
        assertThat(link.getBeacon1(), is(beacon1));
    }

    /**
     * Test method for {@link BeaconLink#getBeacon2()}.
     */
    @Test
    public void testGetBeacon2() {
        BeaconLink link = new BeaconLink(beacon1, beacon2);
        assertThat(link.getBeacon2(), is(beacon2));
    }

    /**
     * Test method for {@link BeaconLink#getTimeStamp()}.
     */
    @Test
    public void testGetTimeStamp() {
        long timestamp = 9876543210L;
        BeaconLink link = new BeaconLink(beacon1, beacon2, timestamp);
        assertThat(link.getTimeStamp(), is(timestamp));
    }

    /**
     * Test method for {@link BeaconLink#equals(Object)}.
     */
    @Test
    public void testEquals() {
        BeaconLink link1 = new BeaconLink(beacon1, beacon2, 1000L);
        BeaconLink link2 = new BeaconLink(beacon1, beacon2, 2000L);

        // Same beacons in same order should be equal
        assertThat(link1, is(link2));
    }

    /**
     * Test method for {@link BeaconLink#equals(Object)} with reversed beacons.
     */
    @Test
    public void testEqualsReversed() {
        BeaconLink link1 = new BeaconLink(beacon1, beacon2);
        BeaconLink link2 = new BeaconLink(beacon2, beacon1);

        // Same beacons in different order should be equal
        assertThat(link1, is(link2));
    }

    /**
     * Test method for {@link BeaconLink#equals(Object)} with different beacons.
     */
    @Test
    public void testNotEquals() {
        BeaconLink link1 = new BeaconLink(beacon1, beacon2);
        BeaconLink link2 = new BeaconLink(beacon1, beacon3);

        // Different beacons should not be equal
        assertThat(link1, is(not(link2)));
    }

    /**
     * Test method for {@link BeaconLink#compareTo(Object)}.
     */
    @Test
    public void testCompareTo() {
        BeaconLink earlier = new BeaconLink(beacon1, beacon2, 1000L);
        BeaconLink later = new BeaconLink(beacon1, beacon2, 2000L);

        assertTrue(earlier.compareTo(later) < 0);
        assertTrue(later.compareTo(earlier) > 0);
    }

    /**
     * Test method for {@link BeaconLink#compareTo(Object)} with same timestamp.
     */
    @Test
    public void testCompareToEqual() {
        BeaconLink link1 = new BeaconLink(beacon1, beacon2, 1000L);
        BeaconLink link2 = new BeaconLink(beacon1, beacon3, 1000L);

        assertThat(link1.compareTo(link2), is(0));
    }

    /**
     * Test method for {@link BeaconLink#compareTo(Object)} with wrong type.
     */
    @Test
    public void testCompareToWrongType() {
        BeaconLink link = new BeaconLink(beacon1, beacon2);
        assertThrows(ClassCastException.class, () -> link.compareTo("not a link"));
    }

    /**
     * Test method for {@link BeaconLink#getLine()}.
     */
    @Test
    public void testGetLine() {
        BeaconLink link = new BeaconLink(beacon1, beacon2);
        Line2D line = link.getLine();

        assertNotNull(line);
        assertThat(line.getX1(), is(0.0));
        assertThat(line.getY1(), is(0.0));
        assertThat(line.getX2(), is(10.0));
        assertThat(line.getY2(), is(10.0));
    }

    /**
     * Test method for {@link BeaconLink#getReverseLine()}.
     */
    @Test
    public void testGetReverseLine() {
        BeaconLink link = new BeaconLink(beacon1, beacon2);
        Line2D reverseLine = link.getReverseLine();

        assertNotNull(reverseLine);
        assertThat(reverseLine.getX1(), is(10.0));
        assertThat(reverseLine.getY1(), is(10.0));
        assertThat(reverseLine.getX2(), is(0.0));
        assertThat(reverseLine.getY2(), is(0.0));
    }

    /**
     * Test method for {@link BeaconLink#getOwner()}.
     */
    @Test
    public void testGetOwnerNull() {
        BeaconLink link = new BeaconLink(beacon1, beacon2);
        // Both beacons have null ownership
        assertNull(link.getOwner());
    }
}
