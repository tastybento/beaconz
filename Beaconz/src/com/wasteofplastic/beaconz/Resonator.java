package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
import java.util.UUID;

public class Resonator implements Artifact {
    private ArtifactType type;
    private UUID placedBy;
    private Point2D location;
    // Power = l1 to l8
    private int power;
    
    /**
     * @param placedBy
     * @param location2
     * @param power
     */
    public Resonator(UUID placedBy, Point2D location2, int power) {
	this.type = ArtifactType.RESONATOR;
	this.placedBy = placedBy;
	this.location = location2;
	this.power = power;
    }
    /**
     * @return the type
     */
    public ArtifactType getType() {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType(ArtifactType type) {
        this.type = type;
    }
    /**
     * @return the placedBy
     */
    public UUID getPlacedBy() {
        return placedBy;
    }
    /**
     * @param placedBy the placedBy to set
     */
    public void setPlacedBy(UUID placedBy) {
        this.placedBy = placedBy;
    }
    /**
     * @return the location
     */
    public Point2D getLocation() {
        return location;
    }
    /**
     * @param location the location to set
     */
    public void setLocation(Point2D location) {
        this.location = location;
    }
    /**
     * @return the power
     */
    public int getPower() {
        return power;
    }
    /**
     * @param power the power to set
     */
    public void setPower(int power) {
        this.power = power;
    }
    
}
