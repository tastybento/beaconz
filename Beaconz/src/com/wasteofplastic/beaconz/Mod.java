package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
import java.util.UUID;

public class Mod implements Artifact {
    private ArtifactType type;
    private UUID placedBy;
    private Point2D location;
    
    public Mod(ArtifactType type, UUID placedBy, Point2D location2) {
        this.type = type;
        this.placedBy = placedBy;
        this.location = location2;
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
    
}
