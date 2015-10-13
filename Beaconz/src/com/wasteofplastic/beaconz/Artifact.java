package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
import java.util.UUID;

public interface Artifact {
    /**
     * @return the type
     */
    public ArtifactType getType();
    
    /**
     * @param type the type to set
     */
    public void setType(ArtifactType type);
    
    /**
     * @return the placedBy
     */
    public UUID getPlacedBy();

    /**
     * @param placedBy the placedBy to set
     */
    public void setPlacedBy(UUID placedBy);
    
    /**
     * @return the location
     */
    public Point2D getLocation(); 
    
    /**
     * @param location the location to set
     */
    public void setLocation(Point2D location);
    
}
