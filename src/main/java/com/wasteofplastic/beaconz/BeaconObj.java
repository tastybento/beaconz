/*
 * Copyright (c) 2015 - 2016 tastybento
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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
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
    private Set<BeaconObj> links = new HashSet<BeaconObj>();

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
     * @return the beacons directly linked from this beacon
     */
    public Set<BeaconObj> getLinks() {
        return links;
    }

    /**
     * Add a link from this beacon to another beacon and make a return link
     * @param beaconPair
     * @return true if link made, false if not
     */
    public boolean addOutboundLink(BeaconObj beacon) {
        //getLogger().info("DEBUG: Trying to add link");
        // There is a max of 8 outgoing links allowed
        if (links.size() == 8) {
            //getLogger().info("DEBUG: outbound link limit reached");
            return false;
        }
        beacon.addLink(this);
        links.add(beacon);
        return true;

    }

    /**
     * Called by another beacon. Adds a link
     * @param beaconPair
     */
    public void addLink(BeaconObj beacon) {
        links.add(beacon);
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
        new LineVisualizer(this.getBeaconzPlugin(), new BeaconLink(this,beacon), false);
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

}
