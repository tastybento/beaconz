package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;

import org.bukkit.scoreboard.Team;

public class BeaconLink implements Comparable<Object> {
    private final BeaconObj beacon1;
    private final BeaconObj beacon2;
    private final Long timestamp;
    private final Team owner;

    /**
     * Create a beacon link at a specific time
     * @param beacon1
     * @param beacon2
     * @param timestamp
     */
    public BeaconLink(BeaconObj beacon1, BeaconObj beacon2, Long timestamp) {
        this.beacon1 = beacon1;
        this.beacon2 = beacon2;
        this.timestamp = timestamp;
        this.owner = beacon1.getOwnership();
    }
    
    /**
     * Create a beacon link at time now
     * @param beacon1
     * @param beacon2
     */
    public BeaconLink(BeaconObj beacon1, BeaconObj beacon2) {
        this.beacon1 = beacon1;
        this.beacon2 = beacon2;
        this.timestamp = System.currentTimeMillis();
        this.owner = beacon1.getOwnership();
    }

    /**
     * @return the beacon1
     */
    public BeaconObj getBeacon1() {
        return beacon1;
    }

    /**
     * @return the beacon2
     */
    public BeaconObj getBeacon2() {
        return beacon2;
    }
    
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof BeaconLink)) {
            throw new ClassCastException("A BeaconPair object expected.");             
        }
        long otherTimestamp = ((BeaconLink) o).getTimeStamp();
        return (int)(this.timestamp - otherTimestamp);
    }
    
    public long getTimeStamp() {
        return timestamp;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BeaconLink)) {
            return false;             
        } 
        BeaconLink otherPair = (BeaconLink)o;
        if ((otherPair.getBeacon1().equals(this.beacon1) && otherPair.getBeacon2().equals(this.beacon2))
                || (otherPair.getBeacon1().equals(this.beacon2) && otherPair.getBeacon2().equals(this.beacon1))) {
            return true;
        }
        return false;
    }
    
    public Team getOwner() {
        return this.owner;
    }
    
    public Line2D getLine() {
        return new Line2D.Double(this.beacon1.getPoint(), this.beacon2.getPoint());
    }
    
    public Line2D getReverseLine() {
        return new Line2D.Double(this.beacon2.getPoint(), this.beacon1.getPoint());
    }
}
