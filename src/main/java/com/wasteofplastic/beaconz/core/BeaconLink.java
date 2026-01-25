package com.wasteofplastic.beaconz.core;

import java.awt.geom.Line2D;

import org.bukkit.scoreboard.Team;

/**
 * Represents a directional link between two beacons in the game.
 *
 * <p>A beacon link connects two {@link BeaconObj} instances and has the following properties:
 * <ul>
 *   <li>The link is directional from beacon1 to beacon2</li>
 *   <li>Links are owned by the team that owns beacon1</li>
 *   <li>Each link has a timestamp indicating when it was created</li>
 *   <li>Links are immutable once created</li>
 * </ul>
 *
 * <p><b>Equality:</b> Two BeaconLink objects are considered equal if they connect
 * the same two beacons, regardless of direction. That is, a link from A to B is
 * equal to a link from B to A.
 *
 * <p><b>Ordering:</b> BeaconLinks are ordered by their creation timestamp, with
 * earlier links comparing as less than later links.
 *
 * <p><b>Thread Safety:</b> This class is immutable and therefore thread-safe.
 *
 * @author tastybento
 * @since 1.0
 */
public class BeaconLink implements Comparable<Object> {

    /** The starting beacon of this link */
    private final BeaconObj beacon1;

    /** The ending beacon of this link */
    private final BeaconObj beacon2;

    /** Unix timestamp (in milliseconds) when this link was created */
    private final Long timestamp;

    /** The team that owns this link (taken from beacon1's ownership) */
    private final Team owner;

    /**
     * Creates a beacon link at a specific time.
     *
     * <p>This constructor is typically used when restoring links from persistent storage.
     *
     * @param beacon1 the starting beacon (cannot be null)
     * @param beacon2 the ending beacon (cannot be null)
     * @param timestamp the Unix timestamp in milliseconds when the link was created
     * @throws NullPointerException if beacon1 or beacon2 is null
     */
    public BeaconLink(BeaconObj beacon1, BeaconObj beacon2, Long timestamp) {
        this.beacon1 = beacon1;
        this.beacon2 = beacon2;
        this.timestamp = timestamp;
        this.owner = beacon1.getOwnership();
    }
    
    /**
     * Creates a beacon link at the current time.
     *
     * <p>This constructor is typically used when creating new links during gameplay.
     * The timestamp is automatically set to the current system time.
     *
     * @param beacon1 the starting beacon (cannot be null)
     * @param beacon2 the ending beacon (cannot be null)
     * @throws NullPointerException if beacon1 or beacon2 is null
     */
    public BeaconLink(BeaconObj beacon1, BeaconObj beacon2) {
        this.beacon1 = beacon1;
        this.beacon2 = beacon2;
        this.timestamp = System.currentTimeMillis();
        this.owner = beacon1.getOwnership();
    }

    /**
     * Gets the starting beacon of this link.
     *
     * @return the first beacon in the link (never null)
     */
    public BeaconObj getBeacon1() {
        return beacon1;
    }

    /**
     * Gets the ending beacon of this link.
     *
     * @return the second beacon in the link (never null)
     */
    public BeaconObj getBeacon2() {
        return beacon2;
    }
    
    /**
     * Compares this link to another based on creation timestamp.
     *
     * <p>Links are ordered chronologically, with earlier links comparing as less than
     * later links. This allows sorting links by creation order.
     *
     * <p><b>Note:</b> This implementation has a potential overflow issue when the
     * timestamp difference exceeds Integer.MAX_VALUE. For production use, consider
     * using Long.compare(this.timestamp, otherTimestamp) instead.
     *
     * @param o the object to compare to
     * @return a negative integer if this link was created before the other,
     *         zero if they were created at the same time (unlikely),
     *         a positive integer if this link was created after the other
     * @throws ClassCastException if o is not a BeaconLink instance
     */
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof BeaconLink)) {
            throw new ClassCastException("A BeaconPair object expected.");             
        }
        long otherTimestamp = ((BeaconLink) o).getTimeStamp();
        // Note: This cast to int can overflow for large timestamp differences
        return (int)(this.timestamp - otherTimestamp);
    }
    
    /**
     * Gets the creation timestamp of this link.
     *
     * @return the Unix timestamp in milliseconds when this link was created
     */
    public long getTimeStamp() {
        return timestamp;
    }
    
    /**
     * Determines if this link equals another object.
     *
     * <p>Two BeaconLink objects are equal if they connect the same two beacons,
     * regardless of direction. This means:
     * <ul>
     *   <li>Link(A, B) equals Link(A, B)</li>
     *   <li>Link(A, B) equals Link(B, A)</li>
     *   <li>Link(A, B) does not equal Link(A, C)</li>
     * </ul>
     *
     * <p><b>Important:</b> Timestamp and ownership are NOT considered in equality.
     * Only the connected beacons matter.
     *
     * @param o the object to compare to
     * @return true if o is a BeaconLink connecting the same two beacons (in either direction)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BeaconLink otherPair)) {
            return false;             
        }
        // Links are equal if they connect the same beacons in either direction
        return (otherPair.getBeacon1().equals(this.beacon1) && otherPair.getBeacon2().equals(this.beacon2))
                || (otherPair.getBeacon1().equals(this.beacon2) && otherPair.getBeacon2().equals(this.beacon1));
    }
    
    /**
     * Gets the team that owns this link.
     *
     * <p>The owner is determined by beacon1's ownership at the time of link creation.
     *
     * @return the team that owns this link, or null if beacon1 had no owner
     */
    public Team getOwner() {
        return this.owner;
    }
    
    /**
     * Gets a geometric line representation of this link from beacon1 to beacon2.
     *
     * <p>The line connects the centers (points) of the two beacons and can be used
     * for geometric calculations such as intersection detection.
     *
     * @return a Line2D from beacon1's point to beacon2's point
     */
    public Line2D getLine() {
        return new Line2D.Double(this.beacon1.getPoint(), this.beacon2.getPoint());
    }
    
    /**
     * Gets a geometric line representation of this link from beacon2 to beacon1.
     *
     * <p>This is the reverse direction of {@link #getLine()}. It can be useful for
     * certain geometric calculations where direction matters.
     *
     * @return a Line2D from beacon2's point to beacon1's point
     */
    public Line2D getReverseLine() {
        return new Line2D.Double(this.beacon2.getPoint(), this.beacon1.getPoint());
    }

}
