package com.wasteofplastic.beaconz;

public class BeaconPair implements Comparable<Object> {
    private BeaconObj beacon1;
    private BeaconObj beacon2;
    private Long timestamp;
    /**
     * @param beacon1
     * @param beacon2
     */
    public BeaconPair(BeaconObj beacon1, BeaconObj beacon2, Long timestamp) {
        this.beacon1 = beacon1;
        this.beacon2 = beacon2;
        this.timestamp = timestamp;
    }
    /**
     * @return the beacon1
     */
    public BeaconObj getBeacon1() {
        return beacon1;
    }
    /**
     * @param beacon1 the beacon1 to set
     */
    public void setBeacon1(BeaconObj beacon1) {
        this.beacon1 = beacon1;
    }
    /**
     * @return the beacon2
     */
    public BeaconObj getBeacon2() {
        return beacon2;
    }
    /**
     * @param beacon2 the beacon2 to set
     */
    public void setBeacon2(BeaconObj beacon2) {
        this.beacon2 = beacon2;
    }
    
    @Override
    public int compareTo(Object o) {
        if (!(o instanceof BeaconPair)) {
            throw new ClassCastException("A BeaconPair object expected.");             
        }
        long otherTimestamp = ((BeaconPair) o).getTimeStamp();
        return (int)(this.timestamp - otherTimestamp);
    }
    
    public long getTimeStamp() {
        return timestamp;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BeaconPair)) {
            return false;             
        } 
        BeaconPair otherPair = (BeaconPair)o;
        if ((otherPair.getBeacon1().equals(this.beacon1) && otherPair.getBeacon2().equals(this.beacon2))
                || (otherPair.getBeacon1().equals(this.beacon2) && otherPair.getBeacon2().equals(this.beacon1))) {
            return true;
        }
        return false;
    }
}
