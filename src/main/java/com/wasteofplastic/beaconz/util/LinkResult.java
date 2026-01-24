/*
 * Copyright (c) 2015 - 2026 tastybento
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wasteofplastic.beaconz.util;

import java.awt.geom.Line2D;

/**
 * Provides the result of a linking of beacons.
 * Provides the number of fields made, the number that were failed to be made,
 * whether the linking was successful or not and the resulting Line2D link.
 * @author tastybento
 *
 */
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
