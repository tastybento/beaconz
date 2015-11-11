package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;
import org.bukkit.scoreboard.Team;

public class BeaconObj extends BeaconzPluginDependent {
    private Point2D location;
    private int x;
    private int z;
    private int height;
    private long hackTimer;
    private Team ownership;
    private List<Resonator> resonators;
    private List<Mod> mods;
    private Set<BeaconObj> links;
    private int outgoing;
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
        this.resonators = new ArrayList<Resonator>();
        this.mods = new ArrayList<Mod>();
        this.links = new HashSet<BeaconObj>();
        this.outgoing = 0;
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
     * @return true if control field made, false if the max outbound limit is reached or the link already exists
     */
    public boolean addOutboundLink(BeaconObj destination) {
        getLogger().info("DEBUG: Trying to add link");
        // There is a max of 8 outgoing links allowed
        if (this.outgoing == 8) {
            getLogger().info("DEBUG: outbound link limit reached");
            return false;
        }
        Line2D newLink = new Line2D.Double(this.location, destination.getLocation());
        // Add link to this beacon
        if (links.add(destination)) {
            getLogger().info("DEBUG: Adding link from " + this.location + " to " + destination.getLocation());
            // Add to global register for quick lookup
            getRegister().addBeaconLink(ownership, newLink);
            // Increase the total links by one
            outgoing++;
            // Tell the destination beacon to add a link back
            getLogger().info("DEBUG: Telling dest to add link");
            return destination.addLink(this);
        }
        return false;
    }

    /**
     * Adds a beacon link and check if any fields are made as a result
     * Called by another beacon
     * @param starter
     * @return true if a field is made, false if not
     */
    public boolean addLink(BeaconObj starter) {
        getLogger().info("DEBUG: Adding link back from " + this.location + " to " + starter.getLocation());
        Boolean result = links.add(starter);
        if (result) {
            getLogger().info("DEBUG: link added");
            // Visualize
            //List<Point2D> points = new ArrayList<Point2D>();
            Line2D line = new Line2D.Double(starter.getLocation(), location);
            Point2D current;
            for (Iterator<Point2D> it = new LineIterator(line); it.hasNext();) {
                current = it.next();
                Block b = Beaconz.getBeaconzWorld().getBlockAt((int)current.getX(), Beaconz.getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
                if (b.getType().equals(Material.AIR)) {
                    MaterialData md = getScorecard().getBlockID(ownership);
                    b.setType(md.getItemType());
                    b.setData(md.getData());
                }
            }
            // Reset result
            result = false;
            // Check to see if we have a triangle
            getLogger().info("DEBUG: Checking for triangles");
            // Run through each of the beacons this beacon is directly linked to
            for (BeaconObj directlyLinkedBeacon : links) {
                getLogger().info("DEBUG: Checking links from " + directlyLinkedBeacon.getLocation());
                // See if any of the beacons linked to our direct links link back to us
                for (BeaconObj indirectlyLinkedBeacon : directlyLinkedBeacon.getLinks()) {
                    if (!indirectlyLinkedBeacon.equals(this)) {
                        getLogger().info("DEBUG: " + directlyLinkedBeacon.getLocation() + " => " + indirectlyLinkedBeacon.getLocation());
                        if (indirectlyLinkedBeacon.equals(starter)) {
                            getLogger().info("DEBUG: Triangle found! ");
                            // We have a winner
                            try {
                                // Result is true if the triangle is made okay
                                result = getRegister().addTriangle(starter.getLocation(), this.getLocation(),
                                        directlyLinkedBeacon.getLocation(), ownership);
                            } catch (IllegalArgumentException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            // There could be more than one, so continue
                        }
                    }
                }
            }
        }
        return result;
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
     * @return the resonators
     */
    public List<Resonator> getResonators() {
        return resonators;
    }

    public boolean addResonator(Resonator resonator) {
        // Check that this player can add a resonator or not
        if (resonators.size() < 9) {
            resonators.add(resonator);
            return true;
        }
        return false;
    }

    /**
     * @param resonators the resonators to set
     */
    public void setResonators(List<Resonator> resonators) {
        this.resonators = resonators;
    }

    /**
     * @return the mods
     */
    public List<Mod> getMods() {
        return mods;
    }

    public boolean addMod(Mod mod) {
        // Players can only add up to 2 mods each, there can be a max of 4 mods total
        if (mods.size() == 4) {
            return false;
        }
        int playerCount = 0;
        for (Mod m : mods) {
            if (m.getPlacedBy().equals(mod.getPlacedBy())) {
                playerCount++;
                if (playerCount == 2) {
                    return false;
                }
            }
        }
        // Less than two
        mods.add(mod);
        return true;
    }

    public void removeMod(Mod mod) {
        mods.remove(mod);
    }

    /**
     * @return the outgoing
     */
    public int getOutgoing() {
        return outgoing;
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
            Block b = Beaconz.getBeaconzWorld().getBlockAt((int)current.getX(), Beaconz.getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
            if (!b.getType().equals(Material.AIR)) {
                b.setType(Material.AIR);
            }
        }
        // TODO: One block is being missed. It's a rounding issue. Need to make the line inclusive of these end points
        Beaconz.getBeaconzWorld().getBlockAt(x, Beaconz.getBeaconzWorld().getMaxHeight()-1, z).setType(Material.AIR);
        Beaconz.getBeaconzWorld().getBlockAt(beacon.getX(), Beaconz.getBeaconzWorld().getMaxHeight()-1, beacon.getZ()).setType(Material.AIR);
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
        Block beacon = Beaconz.getBeaconzWorld().getBlockAt((int)location.getX(), height, (int)location.getY());
        getLogger().info("DEBUG: block type is " + beacon.getType());
        getLogger().info("DEBUG: location is " + (int)location.getX() + " " + height + " " + (int)location.getY());
        if (beacon.getRelative(BlockFace.NORTH).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.SOUTH).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.EAST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.WEST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.NORTH_WEST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.NORTH_EAST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.SOUTH_WEST).getType().equals(Material.AIR)
                && beacon.getRelative(BlockFace.SOUTH_EAST).getType().equals(Material.AIR)) {
            return true;
        }		
        return false;
    }

}
