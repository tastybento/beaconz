package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;

public class LinkResult {
    // The number of triangles successfully made
    private int fieldsMade;
    // Whether the operation was a success or not
    private boolean success;
    // The number of triangles that could have been made but hit a snag, e.g., overlaping enemy line/triangle
    private int fieldsFailedToMake;
    // The link itself
    private Line2D link;
    
    /**
     * @param fieldsMade
     * @param success
     * @param fieldsFailedToMake
     */
    public LinkResult(int fieldsMade, boolean success,
            int fieldsFailedToMake) {
        this.fieldsMade = fieldsMade;
        this.success = success;
        this.fieldsFailedToMake = fieldsFailedToMake;
        this.link = null;
    }
    
    /**
     * @param fieldsMade
     * @param success
     * @param fieldsFailedToMake
     * @param link
     */
    public LinkResult(int fieldsMade, boolean success,
            int fieldsFailedToMake, Line2D link) {
        this.fieldsMade = fieldsMade;
        this.success = success;
        this.fieldsFailedToMake = fieldsFailedToMake;
        this.link = link;
    }
    
    /**
     * @return the fieldsMade
     */
    public int getFieldsMade() {
        return fieldsMade;
    }
    /**
     * @param fieldsMade the fieldsMade to set
     */
    public void setFieldsMade(int fieldsMade) {
        this.fieldsMade = fieldsMade;
    }
    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }
    /**
     * @param success the success to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    /**
     * @return the fieldsFailedToMake
     */
    public int getFieldsFailedToMake() {
        return fieldsFailedToMake;
    }
    /**
     * @param fieldsFailedToMake the fieldsFailedToMake to set
     */
    public void setFieldsFailedToMake(int fieldsFailedToMake) {
        this.fieldsFailedToMake = fieldsFailedToMake;
    }

    /**
     * @return the link
     */
    public Line2D getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(Line2D link) {
        this.link = link;
    }
    
}
