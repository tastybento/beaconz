/*
 * Copyright (c) 2015 tastybento
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

package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scoreboard.Team;

public class BeaconObj extends BeaconzPluginDependent {
    private Point2D location;
    private int x;
    private int z;
    private int height;
    private long hackTimer;
    private Team ownership;
    private Set<BeaconObj> links;
    private Integer id = null;
    private boolean newBeacon = true;

    public BeaconObj(Beaconz plugin, int x, int y, int z, Team owner) {
        super(plugin);
        this.location = new Point2D.Double(x,z);
        this.x = x;
        this.z = z;
        this.height = y;
        this.hackTimer = System.currentTimeMillis();
        this.ownership = owner;
        this.links = new HashSet<BeaconObj>();
        this.newBeacon = true;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return height;
    }
    public int getZ() {
        return z;
    }

    /**
     * @return the location of this beacon
     */
    public Point2D getLocation() {
        return location;
    }

    /**
     * @return the beacons this beacon is directly linked to
     */
    public Set<BeaconObj> getLinks() {
        return links;
    }

    /**
     * Add a link from this beacon to another beacon
     * @param destination
     * @param player 
     * @return true if control field made, false if the max outbound limit is reached or the link already exists
     */
    public LinkResult addOutboundLink(BeaconObj destination) {
        //getLogger().info("DEBUG: Trying to add link");
        // There is a max of 8 outgoing links allowed
        if (links.size() == 8) {
            //getLogger().info("DEBUG: outbound link limit reached");
            return new LinkResult(0,false,0);
        }
        Line2D newLink = new Line2D.Double(this.location, destination.getLocation());
        // Add link to this beacon
        if (links.add(destination)) {
            //getLogger().info("DEBUG: Adding link from " + this.location + " to " + destination.getLocation());
            // Add to global register for quick lookup
            getRegister().addBeaconLink(ownership, newLink);
            // Tell the destination beacon to add a link back
            //getLogger().info("DEBUG: Telling dest to add link");
            return destination.addLink(this);
        }
        return new LinkResult(0,false,0);
    }

    /**
     * Adds a beacon link and check if any fields are made as a result
     * Called by another beacon
     * @param starter
     * @return true if a field is made, false if not
     */
    public LinkResult addLink(BeaconObj starter) {
        //getLogger().info("DEBUG: Adding link back from " + this.location + " to " + starter.getLocation());
        if (!links.add(starter)) {
            return new LinkResult(0, false, 0);
        }
        int fieldsMade = 0;
        int fieldsFailed = 0;
        //getLogger().info("DEBUG: link added");
        // Check to see if we have a triangle
        //getLogger().info("DEBUG: Checking for triangles");
        // Run through each of the beacons this beacon is directly linked to
        for (BeaconObj directlyLinkedBeacon : links) {
            //getLogger().info("DEBUG: Checking links from " + directlyLinkedBeacon.getLocation());
            // See if any of the beacons linked to our direct links link back to us
            for (BeaconObj indirectlyLinkedBeacon : directlyLinkedBeacon.getLinks()) {
                if (!indirectlyLinkedBeacon.equals(this)) {
                    //getLogger().info("DEBUG: " + directlyLinkedBeacon.getLocation() + " => " + indirectlyLinkedBeacon.getLocation());
                    if (indirectlyLinkedBeacon.equals(starter)) {
                        //getLogger().info("DEBUG: Triangle found! ");
                        // We have a winner
                        try {
                            // Result is true if the triangle is made okay, otherwise, don't make the link and return false
                            if (getRegister().addTriangle(starter.getLocation(), this.getLocation(),
                                    directlyLinkedBeacon.getLocation(), ownership)) {
                                fieldsMade++;                             
                            } else {
                                fieldsFailed++;
                            }
                        } catch (IllegalArgumentException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // There could be more than one, so continue
                    }
                }
            }
        }
        // The resulting line
        Line2D line = new Line2D.Double(starter.getLocation(), location);
        // Visualize
        new LineVisualizer(this.getBeaconzPlugin(),line, ownership);
        // Return the result
        return new LinkResult(fieldsMade, true, fieldsFailed, line);
    }


    /**
     * @return the hackTimer
     */
    public long getHackTimer() {
        return hackTimer;
    }

    public void resetHackTimer() {
        this.hackTimer = System.currentTimeMillis();
        this.newBeacon = false;
    }

    /**
     * @return the ownership
     */
    public Team getOwnership() {
        return ownership;
    }

    /**
     * @param ownership the ownership to set
     */
    public void setOwnership(Team ownership) {
        this.ownership = ownership;
    }

    /**
     * @return the number of links
     */
    public int getNumberOfLinks() {
        return links.size();
    }

    /**
     * Name for this beacon based on its coordinates
     * @return
     */
    public String getName() {
        return x + ", " + z;
    }

    public void setId(int indexOf) {
        id = indexOf;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Remove link from this beacon to the specified beacon
     * @param beacon
     */
    public void removeLink(BeaconObj beacon) {
        // Devisualize the link
        //List<Point2D> points = new ArrayList<Point2D>();
        Line2D line = new Line2D.Double(beacon.getLocation(), location);
        Point2D current;
        for (Iterator<Point2D> it = new LineIterator(line); it.hasNext();) {
            current = it.next();
            Block b = getBeaconzWorld().getBlockAt((int)current.getX(), getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
            if (!b.getType().equals(Material.AIR)) {
                b.setType(Material.AIR);
            }
        }
        // TODO: One block is being missed. It's a rounding issue. Need to make the line inclusive of these end points
        getBeaconzWorld().getBlockAt(x, getBeaconzWorld().getMaxHeight()-1, z).setType(Material.AIR);
        getBeaconzWorld().getBlockAt(beacon.getX(), getBeaconzWorld().getMaxHeight()-1, beacon.getZ()).setType(Material.AIR);
        links.remove(beacon);
    }

    /**
     * Remove all links from this beacon
     */
    public void removeLinks() {
        links.clear();
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the newBeacon
     */
    public boolean isNewBeacon() {
        return newBeacon;
    }

    /**
     * Checks if a beacon base is clear of blocks
     * @return true if clear, false if not
     */
    public boolean isClear() {
        Block beacon = getBeaconzWorld().getBlockAt((int)location.getX(), height, (int)location.getY());
        //getLogger().info("DEBUG: block type is " + beacon.getType());
        //getLogger().info("DEBUG: location is " + (int)location.getX() + " " + height + " " + (int)location.getY());
        if (beacon.getRelative(BlockFace.NORTH).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.SOUTH).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.EAST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.WEST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.NORTH_WEST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.NORTH_EAST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.SOUTH_WEST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.SOUTH_EAST).getType().equals(Material.AIR)) {
            // Check all the defense blocks too
            for (Point2D point: getRegister().getDefensesAtBeacon(this)) {
                beacon = getBeaconzWorld().getBlockAt((int)point.getX(), height, (int)point.getY());
                if (!beacon.getType().equals(Material.AIR)) {
                    return false;
                }
            }
            return true;
        }		
        return false;
    }

}
