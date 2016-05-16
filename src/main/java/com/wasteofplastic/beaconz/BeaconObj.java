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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scoreboard.Team;

/**
 * Represents a beacon
 * @author tastybento
 *
 */
public class BeaconObj extends BeaconzPluginDependent {
    private Point2D location;
    private int x;
    private int z;
    private int height;
    private long hackTimer;
    private Team ownership;
    //private Set<BeaconObj> links;
    private Integer id = null;
    private boolean newBeacon = true;
    private static final List<BlockFace> FACES = new ArrayList<BlockFace>(Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST));
    private HashMap<Block, Integer> defenseBlocks = new HashMap<Block, Integer>();
    private HashMap<BeaconObj,Long> links = new HashMap<BeaconObj,Long>();

    /**
     * Represents a beacon
     * @param plugin
     * @param x
     * @param y
     * @param z
     * @param owner
     */
    public BeaconObj(Beaconz plugin, int x, int y, int z, Team owner) {
        super(plugin);
        this.location = new Point2D.Double(x,z);
        this.x = x;
        this.z = z;
        this.height = y;
        this.hackTimer = System.currentTimeMillis();
        this.ownership = owner;
        this.links.clear();
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
    public Point2D getPoint() {
        return location;
    }

    /**
     * @return the Bukkit location of this beacon
     */
    public Location getLocation() {
        return new Location(getBeaconzWorld(),location.getX(),height,location.getY());
    }

    /**
     * @return the beacons this beacon is directly linked to
     */
    public Set<BeaconObj> getLinks() {
        return links.keySet();
    }

    /**
     * Add a link from this beacon to another beacon - and another one back
     * @param destination
     * @param player
     * @return true if control field made, false if the max outbound limit is reached or the link already exists
     */
    public LinkResult addOutboundLink(BeaconObj destination) {
        return addOutboundLink(destination, null);
    }

    /**
     * Add a link from this beacon to another beacon - and another one back and set the time that this was done
     * @param destination
     * @param linkTime
     * @return true if control field made, false if the max outbound limit is reached or the link already exists
     */
    public LinkResult addOutboundLink(BeaconObj destination, Long linkTime) {
        //getLogger().info("DEBUG: Trying to add link");
        // There is a max of 8 outgoing links allowed
        if (links.size() == 8) {
            //getLogger().info("DEBUG: outbound link limit reached");
            return new LinkResult(0,false,0);
        }
        Line2D newLink = new Line2D.Double(this.location, destination.getPoint());
        // Add link to this beacon
        if (linkTime == null) {
            linkTime = System.currentTimeMillis();
        }
        if (!links.containsKey(destination)) {
            links.put(destination,linkTime);
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
        return addLink(starter, null);
    }

    /**
     * Adds a beacon link and check if any fields are made as a result
     * Called by another beacon
     * @param starter
     * @return true if a field is made, false if not
     */
    public LinkResult addLink(BeaconObj starter, Long linkTime) {
        //getLogger().info("DEBUG: Adding link back from " + this.location + " to " + starter.getLocation());
        if (links.containsKey(starter)) {
            return new LinkResult(0, false, 0); 
        }
        if (linkTime == null) {
            linkTime = System.currentTimeMillis();
        }
        links.put(starter, linkTime);
        int fieldsMade = 0;
        int fieldsFailed = 0;
        //getLogger().info("DEBUG: link added");
        // Check to see if we have a triangle
        //getLogger().info("DEBUG: Checking for triangles");
        // Run through each of the beacons this beacon is directly linked to
        for (BeaconObj directlyLinkedBeacon : links.keySet()) {
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
                            if (getRegister().addTriangle(starter.getPoint(), this.getPoint(),
                                    directlyLinkedBeacon.getPoint(), ownership)) {
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
        Line2D line = new Line2D.Double(starter.getPoint(), location);
        // Make sure line is counted in the Register
        getRegister().addBeaconLink(ownership, line);
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
        Line2D line = new Line2D.Double(beacon.getPoint(), location);
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

        // remove the link
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
     * Checks if a beacon base is clear of blocks and above the blocks all the way to the sky
     * @return true if clear, false if not
     */
    public boolean isClear() {
        Block beacon = getBeaconzWorld().getBlockAt((int)location.getX(), height, (int)location.getY());
        //getLogger().info("DEBUG: block y = " + beacon.getY() + " " + beacon.getLocation());
        for (BlockFace face: FACES) {
            Block block = beacon.getRelative(face);
            //getLogger().info("DEBUG: highest block at " + block.getX() + "," + block.getZ() + " y = " + getHighestBlockYAt(block.getX(), block.getZ()));
            if (block.getY() != getHighestBlockYAt(block.getX(), block.getZ())) {
                return false;
            }
        }
        // Check all the defense blocks too
        for (Point2D point: getRegister().getDefensesAtBeacon(this)) {
            beacon = getBeaconzWorld().getBlockAt((int)point.getX(), height, (int)point.getY());
            if (beacon.getY() != getHighestBlockYAt((int)point.getX(), (int)point.getY())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tracks important defense blocks
     * @param block
     * @param levelRequired
     */
    public void addDefenseBlock(Block block, int levelRequired) {
        defenseBlocks.put(block, levelRequired);
    }

    /**
     * Removes the defense block
     * @param block
     */
    public void removeDefenseBlock(Block block) {
        defenseBlocks.remove(block);
    }

    /**
     * @return the defenseBlocks
     */
    public HashMap<Block, Integer> getDefenseBlocks() {
        return defenseBlocks;
    }

    /**
     * @param defenseBlocks the defenseBlocks to set
     */
    public void setDefenseBlocks(HashMap<Block, Integer> defenseBlocks) {
        this.defenseBlocks = defenseBlocks;
    }

    /**
     * @param beacon
     * @return the time when this link was made
     */
    public long getLinkTime(BeaconObj beacon) {
        return links.get(beacon);
    }

}
