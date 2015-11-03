package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scoreboard.Team;

/**
 * Enables quick finding of beacons
 * This is the database of all game elements in the world and is saved and loaded to filesystem
 * @author tastybento
 *
 */
public class Register {
    private Beaconz plugin;
    private HashMap<Short, BeaconObj> beaconMaps = new HashMap<Short, BeaconObj>();
    private HashMap<Point2D, BeaconObj> beaconRegister = new HashMap<Point2D, BeaconObj>();
    private Set<TriangleField> triangleFields = new HashSet<TriangleField>();
    private HashMap<Team, Integer> score = new HashMap<Team, Integer>();
    private HashMap<Team, Set<Line2D>> links = new HashMap<Team, Set<Line2D>>();

    /**
     * @param plugin
     */
    public Register(Beaconz plugin) {
	this.plugin = plugin;
    }

    public void saveRegister() {
	// Beacons
	int count = 0;
	for (BeaconObj beacon : beaconRegister.values()) {
	    String owner = "unowned";
	    if (beacon.getOwnership() != null) {
		owner = beacon.getOwnership().getName();
	    }
	    plugin.getConfig().set("beacon." + count + ".location", beacon.getX() + ":" + beacon.getY() + ":" + beacon.getZ() 
		    + ":" + owner);
	    List<String> beaconLinks = new ArrayList<String>();
	    for (BeaconObj linkedBeacon: beacon.getLinks()) {
		beaconLinks.add((int)linkedBeacon.getLocation().getX() +":" + (int)linkedBeacon.getLocation().getY());
	    }
	    plugin.getConfig().set("beacon." + count + ".links", beaconLinks);
	    if (beacon.getId() != null) {
		plugin.getConfig().set("beacon." + count + ".id", beacon.getId());
	    }
	    count++;
	}
	/*
	// Score
	for (FactionType faction: score.keySet()) {
	    plugin.getConfig().set("score." + faction.toString(), score.get(faction).toString());  
	}
	// Control Fields
	List<String> list = new ArrayList<String>();
	for (ControlField cf: controlFields) {
	    list.add(cf.toString());
	}
	if (!list.isEmpty()) {
	    plugin.getConfig().set("controlfields",list);
	}
	 */
	plugin.saveConfig();
    }

    /**
     * Loads game info
     */
    public void loadRegister() {
	//beacons
	beaconRegister.clear();
	beaconMaps.clear();
	HashMap<BeaconObj, List<String>> beaconLinks = new HashMap<BeaconObj, List<String>>();
	FileConfiguration config = plugin.getConfig();
	ConfigurationSection configSec = config.getConfigurationSection("beacon");
	if (configSec != null) {
	    for (String beacon : configSec.getValues(false).keySet()) {
		String info = configSec.getString(beacon + ".location","");
		String[] args = info.split(":");
		if (!info.isEmpty() && args.length == 4) {
		    int x = Integer.valueOf(args[0]);
		    int y = Integer.valueOf(args[1]);
		    int z = Integer.valueOf(args[2]);
		    BeaconObj newBeacon = new BeaconObj(plugin, x,y,z , plugin.getScorecard().getTeam(args[3]));
		    // Check for links
		    beaconLinks.put(newBeacon, configSec.getStringList(beacon + ".links"));

		    beaconRegister.put(new Point2D.Double(x, z), newBeacon);
		    // Map id
		    if (configSec.contains(beacon + ".id")) {
			addBeaconMap((short)configSec.getInt(beacon + ".id"), newBeacon);
		    }
		    plugin.getLogger().info("DEBUG: loaded beacon at " + x + "," + y + "," + z);
		}
	    }
	}
	// Add links
	for (BeaconObj p: beaconLinks.keySet()) {
	    for (String link : beaconLinks.get(p)) {
		String[] args = link.split(":");
		BeaconObj dest = beaconRegister.get(new Point2D.Double(Double.valueOf(args[0]), Double.valueOf(args[1])));
		if (dest != null) {
		    p.addLink(dest);
		}
	    }
	}
	// Score and control fields should be automatically created
    }

    public void addBeaconLink(Team faction, Line2D link) {
	Set<Line2D> linkSet = new HashSet<Line2D>();
	if (links.containsKey(faction)) {
	    linkSet = links.get(faction);
	}
	linkSet.add(link);
	links.put(faction, linkSet);
	// Check if a field has been made

    }

    /**
     * Deletes any links that starts
     * @param faction
     * @param point
     */
    public void deleteBeaconLinks(Team faction, Point2D point) {
	Set<Line2D> linkSet = new HashSet<Line2D>();
	if (links.containsKey(faction)) {
	    linkSet = links.get(faction);
	}
	Iterator<Line2D> it = linkSet.iterator();
	while (it.hasNext()) {
	    Line2D line = it.next();
	    if (line.getP1().equals(point) || line.getP2().equals(point)) {
		// Devisualize - TODO: make async or something
		for (Iterator<Point2D> lineIt = new LineIterator(line); lineIt.hasNext();) {
		    Point2D current = lineIt.next();
		    Block b = Beaconz.getBeaconzWorld().getBlockAt((int)current.getX(), Beaconz.getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
		    b.setType(Material.AIR);
		}
		it.remove();
	    }
	}
    }


    /**
     * Return all the links for this team
     * @param faction
     * @return set of links or empty set if none
     */
    public Set<Line2D> getFactionLinks(Team faction) {
	if (links.get(faction) == null) {
	    return new HashSet<Line2D>();
	}
	return links.get(faction);
    }

    /**
     * Registers a beacon at a 2D point
     * @param owner
     * @param location
     */
    public void addBeacon(Team owner, int x, int y, int z) {
	// Create a beacon
	Point2D location = new Point2D.Double(x,z);
	plugin.getLogger().info("DEBUG: registered beacon at " + location + " status " + owner);
	BeaconObj p = new BeaconObj(plugin, x, y, z, owner);
	beaconRegister.put(location, p);
    }

    /**
     * Creates a triangular field covering the world between three beacons
     * @param point2d
     * @param point2d2
     * @param point2d3
     * @return 
     */
    public Boolean addTriangle(Point2D point2d, Point2D point2d2, Point2D point2d3, Team owner)  throws IllegalArgumentException {
	plugin.getLogger().info("DEBUG: Adding triangle at " + point2d + " " + point2d2 + " " + point2d3);
	// Check that locations are known beacons
	if (beaconRegister.containsKey(point2d) && beaconRegister.containsKey(point2d2) && beaconRegister.containsKey(point2d3)) {
	    plugin.getLogger().info("DEBUG: All three beacons are in the register");
	    // Check the beacons are all owned by the same faction
	    if (beaconRegister.get(point2d).getOwnership().equals(owner)
		    && beaconRegister.get(point2d2).getOwnership().equals(owner)
		    && beaconRegister.get(point2d3).getOwnership().equals(owner)) {
		plugin.getLogger().info("DEBUG: All beacons are owned by same faction");
		TriangleField cf = new TriangleField(point2d, point2d2, point2d3, owner);
		// Check to see if this control field would overlap enemy-held beacons
		for (Entry<Point2D,BeaconObj> beacon : plugin.getRegister().getBeaconRegister().entrySet()) {
		    if (!beacon.getValue().getOwnership().equals(owner)) {
			// Check enemy beacons
			if (cf.contains(beacon.getKey())) {
			    plugin.getLogger().info("DEBUG: Enemy beacon found inside potential control field, not making control field");
			    return false;
			}
		    }
		}
		if (triangleFields.add(cf)) {
		    plugin.getLogger().info("DEBUG: Added control field!");
		    // New control field, add to score
		    if (score.containsKey(owner)) {
			int s = score.get(owner);
			s += cf.getArea();
			score.put(owner, s);
		    } else {
			score.put(owner, cf.getArea());
		    }
		    plugin.getLogger().info("DEBUG: New score is " + cf.getArea());
		    return true;
		} else {
		    plugin.getLogger().info("DEBUG: Control field already exists");
		}
	    } else {
		plugin.getLogger().info("DEBUG: beacons are not owned by the same faction");
		throw new IllegalArgumentException("beacons are not owned by the same faction");
	    }
	} else {
	    plugin.getLogger().info("DEBUG: Location argument is not a beacon");
	    throw new IllegalArgumentException("Location argument is not a beacon");
	}
	return false;
    }

    /**
     * @param faction
     * @return score for faction
     */
    public int getScore(Team faction) {
	if (score.containsKey(faction)) {
	    return score.get(faction);
	}
	return 0;
    }


    /**
     * @return the score
     */
    public HashMap<Team, Integer> getScore() {
	return score;
    }

    /**
     * Set the score for a faction
     * @param faction
     * @param score
     */
    public void setScore(Team faction, int score) {
	this.score.put(faction, score);
    }    

    /**
     * @return the beaconRegister
     */
    public HashMap<Point2D, BeaconObj> getBeaconRegister() {
	return beaconRegister;
    }

    /**
     * @return the triangleFields
     */
    public Set<TriangleField> getTriangleFields() {
	return triangleFields;
    }

    /**
     * Checks if a block is part of a natural beacon
     * @param b
     * @return true if it is part of a beacon, false if not
     */
    public boolean isBeacon(Block b) {
	return getBeacon(b) == null ? false:true;
    }

    /**
     * Returns beacons by index
     * @param index
     * @return beacon at index
     */
    public BeaconObj getBeacon(int index) {
	if (index >= 0 && index < beaconMaps.size()) {
	    return beaconMaps.get(index);
	}
	return null;
    }
    /**
     * Gets the beacon connected to b
     * @param b
     * @return BeaconObj or null if none
     */
    public BeaconObj getBeacon(Block b) {
	plugin.getLogger().info("DEBUG: material = " + b.getType());
	// Quick check
	if (!b.getType().equals(Material.BEACON) && !b.getType().equals(Material.DIAMOND_BLOCK) 
		&& !b.getType().equals(Material.OBSIDIAN) && !b.getType().equals(Material.STAINED_GLASS)) {
	    return null;
	}
	plugin.getLogger().info("DEBUG: correct material");
	Point2D point = new Point2D.Double(b.getLocation().getBlockX(),b.getLocation().getBlockZ());
	plugin.getLogger().info("DEBUG: checking point " + point);

	// Check glass or obsidian
	if (b.getType().equals(Material.OBSIDIAN) || b.getType().equals(Material.STAINED_GLASS)) {
	    Block below = b.getRelative(BlockFace.DOWN);
	    if (!below.getType().equals(Material.BEACON)) {
		plugin.getLogger().info("DEBUG: no beacon below here");
		return null;
	    }
	    point = new Point2D.Double(below.getLocation().getBlockX(),below.getLocation().getBlockZ());
	    // Beacon below
	    if (beaconRegister.containsKey(point)) {
		plugin.getLogger().info("DEBUG: found in register");
		return beaconRegister.get(point);
	    } else {
		plugin.getLogger().info("DEBUG: not found in register");
		return null;
	    }
	}
	// Check beacons
	if (b.getType().equals(Material.BEACON)) {
	    if (beaconRegister.containsKey(point)) {
		plugin.getLogger().info("DEBUG: found in register");
		return beaconRegister.get(point);
	    } else {
		plugin.getLogger().info("DEBUG: not found in register. Known points are:");
		for (Point2D points : beaconRegister.keySet()) {
		    plugin.getLogger().info("DEBUG: " + points);
		}
		return null;
	    }
	}
	// Check the pyramid around the beacon
	// Look for a beacon
	for (int modX = -1; modX < 2; modX++) {
	    for (int modZ = -1; modZ < 2; modZ++) {
		Block test = b.getRelative(modX, 1, modZ);
		if (test.getType().equals(Material.BEACON)) {
		    point = new Point2D.Double(test.getLocation().getBlockX(),test.getLocation().getBlockZ());
		    if (beaconRegister.containsKey(point)) {
			return beaconRegister.get(point);
		    }
		}
	    }
	}
	return null;
    }
    /**
     * Removes this block from faction ownership and makes it unowned
     * @param b
     */
    public void deleteBeacon(Block test) {
	Point2D point = new Point2D.Double(test.getLocation().getBlockX(),test.getLocation().getBlockZ());
	if (beaconRegister.containsKey(point)) {
	    BeaconObj beacon = beaconRegister.get(point);
	    Team oldOwner = beacon.getOwnership();
	    beacon.setOwnership(null);
	    //TODO remove all the links and delete any score from the team that just lost it
	    for (BeaconObj linkedBeacon : beacon.getLinks()) {
		linkedBeacon.removeLink(beacon);
	    }
	    beacon.removeLinks();
	    // Get any control triangles that have been removed because of this
	    Iterator<TriangleField> it = triangleFields.iterator();
	    while (it.hasNext()) {
		TriangleField triangle = it.next();
		if (triangle.hasVertex(point)) {
		    plugin.getLogger().info("DEBUG: this beaon was part of a triangle");
		    // Remove score
		    if (score.containsKey(oldOwner)) {
			int sc = triangle.getArea();
			plugin.getLogger().info("DEBUG: Removing score " + sc + " from " + oldOwner.getDisplayName() 
				+ " team's score of " + score.get(beacon.getOwnership()));
			int newScore = score.get(oldOwner) - sc;
			score.put(oldOwner,newScore);
		    }
		    // Remove triangle
		    it.remove();
		}
	    }
	}

    }

    /**
     * Registers a beacon at location
     * @param team
     * @param location
     */
    public void addBeacon(Team team, Location location) {
	addBeacon(team, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * @return the beaconMaps
     */
    public BeaconObj getBeaconMap(Short index) {
	return beaconMaps.get(index);
    }

    /**
     * @param beaconMaps the beaconMaps to set
     */
    public void addBeaconMap(Short index, BeaconObj beacon) {
	beacon.setId(index);
	plugin.getLogger().info("DEBUG: storing beacon map # " + index + " for beacon at "+ beacon.getLocation());
	this.beaconMaps.put(index, beacon);
    }

    /**
     * Gets a set of all the control triangles at this location. Can be overlapping triangles.
     * @param x
     * @param y
     * @return list of triangles at this location
     */
    public List<TriangleField> getTriangle(int x, int y) {
	// TODO: Brute force check - in the future, will need to be indexed better
	List<TriangleField> result = new ArrayList<TriangleField>();
	for (TriangleField tri: triangleFields) {
	    if (tri.contains(x, y) != null) {
		result.add(tri);
	    }
	}
	return result;
    }


    /**
     * Returns the beacon at x,z or null if there is none
     * @param x
     * @param z
     * @return beacon object
     */
    public BeaconObj getBeaconAt(int x, int z) {
	Point2D point = new Point2D.Double((double)x,(double)z);
	return beaconRegister.get(point);
    }

}
