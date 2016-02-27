/*
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scoreboard.Team;

/**
 * Enables quick finding of beacons
 * This is the database of all game elements in the world and is saved and loaded to filesystem
 * @author tastybento
 *
 */
public class Register extends BeaconzPluginDependent {

    public Register(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    private HashMap<Short, BeaconObj> beaconMaps = new HashMap<Short, BeaconObj>();
    private HashMap<Point2D, BeaconObj> beaconRegister = new HashMap<Point2D, BeaconObj>();
    private Set<TriangleField> triangleFields = new HashSet<TriangleField>();
    private HashMap<Team, Set<Line2D>> links = new HashMap<Team, Set<Line2D>>();
    /**
     * Store of the blocks around a beacon. Starts as the initial 8 blocks adjacent to the
     * beacon. Can expand as players add diamond blocks to the beacon.
     */
    private HashMap<Point2D, BeaconObj> defenseBlocks = new HashMap<Point2D, BeaconObj>();
    private HashMap<BeaconObj, Set<Point2D>> defenseBlocksInverse = new HashMap<BeaconObj, Set<Point2D>>();

    public void saveRegister() {
        // Save the beacons
        File beaconzFile = new File(getBeaconzPlugin().getDataFolder(),"beaconz.yml");
        YamlConfiguration beaconzYml = new YamlConfiguration();
        // Backup the beacons file just in case
        if (beaconzFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"beaconz.old");
            beaconzFile.renameTo(backup);
        }   
        int count = 0;
        for (BeaconObj beacon : beaconRegister.values()) {
            String owner = "unowned";
            if (beacon.getOwnership() != null) {
                owner = beacon.getOwnership().getName();
            }
            beaconzYml.set("beacon." + count + ".location", beacon.getX() + ":" + beacon.getY() + ":" + beacon.getZ()
                    + ":" + owner);
            List<String> beaconLinks = new ArrayList<String>();
            for (BeaconObj linkedBeacon: beacon.getLinks()) {
                beaconLinks.add(linkedBeacon.getX() +":" + linkedBeacon.getZ());
            }
            beaconzYml.set("beacon." + count + ".links", beaconLinks);
            if (beacon.getId() != null) {
                beaconzYml.set("beacon." + count + ".id", beacon.getId());
            }
            // Save defense blocks
            List<String> defenseBlocks = new ArrayList<String>();            
            for (Point2D point: defenseBlocksInverse.get(beacon)) {
                defenseBlocks.add((int)point.getX() + ":" + (int)point.getY());
            }
            beaconzYml.set("beacon." + count + ".defenseblocks", defenseBlocks);
            count++;
        }
        try {
            beaconzYml.save(beaconzFile);
        } catch (IOException e) {
            getLogger().severe("Problem saving beacons file!");
            e.printStackTrace();
        }
    }

    /**
     * Loads register info
     */
    public void loadRegister() {
    	int count = 0;
        // Clear the data
        clear();

        File beaconzFile = new File(getBeaconzPlugin().getDataFolder(),"beaconz.yml");
        if (!beaconzFile.exists()) {
            return;
        }
        YamlConfiguration beaconzYml = new YamlConfiguration();
        try {
            beaconzYml.load(beaconzFile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            getLogger().severe("Problem with beaconz.yml formatting");
            e.printStackTrace();
        }
        //beacons
        HashMap<BeaconObj, List<String>> beaconLinks = new HashMap<BeaconObj, List<String>>();
        ConfigurationSection configSec = beaconzYml.getConfigurationSection("beacon");
        if (configSec != null) {
            for (String beacon : configSec.getValues(false).keySet()) {
                String info = configSec.getString(beacon + ".location","");
                String[] args = info.split(":");       
                if (!info.isEmpty() && args.length == 4) {
                    if (NumberUtils.isNumber(args[0]) && NumberUtils.isNumber(args[1]) && NumberUtils.isNumber(args[2])) {
                        int x = Integer.valueOf(args[0]);
                        int y = Integer.valueOf(args[1]);
                        int z = Integer.valueOf(args[2]);
                        
                        Game game = getGameMgr().getGame(x, z);                        
                        Team team = null;
                        if (!args[3].equalsIgnoreCase("unowned")) {
                            team = game.getScorecard().getTeam(args[3]);
                        }
                        BeaconObj newBeacon = addBeacon(team, x, y, z);
                        count++;
                        //BeaconObj newBeacon = new BeaconObj(getBeaconzPlugin(), x,y,z , team));
                        // Check for links
                        beaconLinks.put(newBeacon, configSec.getStringList(beacon + ".links"));
                        // Load defense blocks
                        List<String> defenseBlocks = configSec.getStringList(beacon + ".defenseblocks");
                        for (String defenseBlock : defenseBlocks) {
                            String[] args2 = defenseBlock.split(":");
                            if (args2.length == 2) {
                                if (NumberUtils.isNumber(args2[0]) && NumberUtils.isNumber(args2[1])) {
                                    int blockX = Integer.valueOf(args2[0]);
                                    int blockZ = Integer.valueOf(args2[1]);
                                    addBeaconDefenseBlock(blockX, blockZ, newBeacon);
                                }
                            }
                        }
                        //beaconRegister.put(new Point2D.Double(x, z), newBeacon);
                        // Map id
                        if (configSec.contains(beacon + ".id")) {
                            addBeaconMap((short)configSec.getInt(beacon + ".id"), newBeacon);
                        }
                        //getLogger().info("DEBUG: loaded beacon at " + x + "," + y + "," + z);
                    }
                }	
            }
        }
        // Add links        
        for (BeaconObj beacon: beaconLinks.keySet()) {
            for (String link : beaconLinks.get(beacon)) {
                String[] args = link.split(":");
                BeaconObj dest = beaconRegister.get(new Point2D.Double(Double.valueOf(args[0]), Double.valueOf(args[1])));
                if (dest != null) {
                    LinkResult result = beacon.addLink(dest);                    
                    if (result.isSuccess()) {
                        addBeaconLink(beacon.getOwnership(), result.getLink());  
                    }
                }
            }
        }
        // Score and control fields should be automatically created
    }

    /**
     * Clears all the data in the register
     */
    public void clear() {
    	clear(null);
    }
    public void clear(Region region) {
    	if (region == null) {
            beaconMaps.clear();
            beaconRegister.clear();
            triangleFields.clear();
            links.clear();    		
    	} else {
    		Iterator<Entry<Short, BeaconObj>> bmit = beaconMaps.entrySet().iterator();
    		while (bmit.hasNext()) {
    			if (region.containsBeacon(bmit.next().getValue())) {
    				bmit.remove();
    			}
    		}
    		Iterator<Entry<Point2D, BeaconObj>> brit = beaconRegister.entrySet().iterator();
    		while (brit.hasNext()) {
    			if (region.containsBeacon(brit.next().getValue())) {
    				brit.remove();
    			}
    		}    	
    		Iterator<TriangleField> trit = triangleFields.iterator();
    		while (trit.hasNext()) {
    			if (region.containsPoint(trit.next().a)) {
    				trit.remove();
    			}
    		} 
    		Iterator<Entry<Team, Set<Line2D>>> lkit = links.entrySet().iterator();
    		while (lkit.hasNext()) {
    			Team team = lkit.next().getKey();
    			if (team != null) {
    				Game game =  getGameMgr().getGame(team);
    				if (game != null && game.getRegion().equals(region)) {
    					lkit.remove();	
    				}
    			}
    		}
    	} 
    }

    public void addBeaconLink(Team team, Line2D link) {
        Set<Line2D> linkSet = new HashSet<Line2D>();
        if (links.containsKey(team)) {
            linkSet = links.get(team); 
        }
		linkSet.add(link);		
        links.put(team, linkSet);
        getGameMgr().getSC(link.getP1()).refreshScores(team, "links");  
    }

    /**
     * Deletes any links that starts
     * @param faction
     * @param point
     * NOTE: THIS ISN'T USED ANYWHERE *****************************
     */
    public void deleteBeaconLinks(Team team, Point2D point) {
        Set<Line2D> linkSet = new HashSet<Line2D>();
        if (links.containsKey(team)) {
            linkSet = links.get(team);
        }
        Iterator<Line2D> it = linkSet.iterator();
        while (it.hasNext()) {
            Line2D line = it.next();
            if (line.getP1().equals(point) || line.getP2().equals(point)) {
                // Devisualize - TODO: make async or something
                for (Iterator<Point2D> lineIt = new LineIterator(line); lineIt.hasNext();) {
                    Point2D current = lineIt.next();
                    Block b = getBeaconzWorld().getBlockAt((int)current.getX(), getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
                    b.setType(Material.AIR);
                }
                it.remove();
                Game game = getGameMgr().getGame(it.next());
                game.getScorecard().refreshScores(team, "links");
            }
        }
    }


    /**
     * Return all the links for a team
     * @param team
     * @return set of links or empty set if none
     */
    public Set<Line2D> getTeamLinks(Team team) {
        if (links.get(team) == null) {
            return new HashSet<Line2D>();
        }
        return links.get(team);
    }
    
    /**
     * Return all the beacons for a team
     * @param team
     * @return set of beacons or empty set if none
     */
    public Set<BeaconObj> getTeamBeacons(Team team) {
    	Set<BeaconObj> teambeacons = new HashSet<BeaconObj>(); 
        for (BeaconObj beacon : beaconRegister.values()) {
            if (beacon.getOwnership() != null && beacon.getOwnership().equals(team)) {
                teambeacons.add(beacon);
            }
        }
        return teambeacons;
    }
    
    /**
     * Return all the triangles for a team
     * @param team
     * @return set of triangles or empty set if none
     */
    public Set<TriangleField> getTeamTriangles(Team team) {
    	Set<TriangleField> teamtriangles = new HashSet<TriangleField>(); 
    	for (TriangleField triangle : triangleFields) {
            if (triangle.getOwner() != null && triangle.getOwner().equals(team)) {
                teamtriangles.add(triangle);
            }
        }
        return teamtriangles;
    } 
    
    /**
     * Return total area owned by team
     * @param team
     * @return total area
     */
    public Integer getTeamArea(Team team) {
    	Integer area = 0; 
    	for (TriangleField triangle : triangleFields) {
            if (triangle.getOwner() != null && triangle.getOwner().equals(team)) {
                area += triangle.getArea();
            }
        }
        return area;
    }    


    /**
     * Registers a beacon at a 3D point
     * @param owner
     * @param x
     * @param y
     * @param z
     * @return beacon that was created
     */
    public BeaconObj addBeacon(Team owner, int x, int y, int z) {
        // Create a beacon
        BeaconObj beacon = new BeaconObj(getBeaconzPlugin(), x, y, z, owner);
        //getLogger().info("DEBUG: registered beacon at " + x + "," + y + ", " + z + " owner " + owner);
        for (int xx = x-1; xx <= x + 1; xx++) {
            for (int zz = z - 1; zz <= z + 1; zz++) {
                Point2D location = new Point2D.Double(xx,zz);
                if (xx == x && zz == z) {
                    // Center square = beacon
                    beaconRegister.put(location, beacon);
                } else {
                    // Put the defensive blocks
                    defenseBlocks.put(location, beacon);
                    Set<Point2D> points = defenseBlocksInverse.get(beacon);
                    if (points == null) {
                        points = new HashSet<Point2D>();
                    }
                    points.add(location);
                    defenseBlocksInverse.put(beacon, points);
                    //getLogger().info("DEBUG: registered defense block at " + location + " status " + owner);
                }
            }
        }
        if (owner != null) {
            // New owned beacon, increment score
        	Game game = getGameMgr().getGame(x, z);
            game.getScorecard().refreshScores(owner, "beacons");
        }

        return beacon;
    } 

    /**
     * Creates a triangular field covering the world between three beacons
     * @param point2d
     * @param point2d2
     * @param point2d3
     * @return 
     */
    public Boolean addTriangle(Point2D point2d, Point2D point2d2, Point2D point2d3, Team owner)  throws IllegalArgumentException {
        //getLogger().info("DEBUG: Adding triangle at " + point2d + " " + point2d2 + " " + point2d3);
        // Check that locations are known beacons
        if (beaconRegister.containsKey(point2d) && beaconRegister.containsKey(point2d2) && beaconRegister.containsKey(point2d3)) {
            //getLogger().info("DEBUG: All three beacons are in the register");
            // Check the beacons are all owned by the same faction
            if (beaconRegister.get(point2d).getOwnership().equals(owner)
                    && beaconRegister.get(point2d2).getOwnership().equals(owner)
                    && beaconRegister.get(point2d3).getOwnership().equals(owner)) {
                //getLogger().info("DEBUG: All beacons are owned by same faction");
                TriangleField triangle = new TriangleField(point2d, point2d2, point2d3, owner);
                // Check to see if this control field would overlap enemy-held beacons
                // Allow this for now
                /*
                for (Entry<Point2D,BeaconObj> beacon : getRegister().getBeaconRegister().entrySet()) {
                    if (beacon.getValue().getOwnership() != null && !beacon.getValue().getOwnership().equals(owner)) {
                        // Check enemy beacons
                        if (cf.contains(beacon.getKey())) {
                            getLogger().info("DEBUG: Enemy beacon found inside potential control field, not making control field");
                            return false;
                        }
                    }
                }*/
                // Check if any triangle or lines intersect
                for (TriangleField triangleField : triangleFields) {
                    // Check if triangle is inside any of the known triangles
                    if (!triangle.getOwner().equals(triangleField.getOwner()) && (triangleField.contains(triangle) || triangle.contains(triangleField))) {
                        //getLogger().info("DEBUG: Enemy triangle found inside triangle!");
                        return false;
                    }
                }
                for (Entry<Team, Set<Line2D>> linkSet : links.entrySet()) {
                    if (!linkSet.getKey().equals(owner)) {
                        for (Line2D link : linkSet.getValue()) {
                            for (Line2D side : triangle.getSides()) {
                                if (side.intersectsLine(link)) {
                                    //getLogger().info("DEBUG: Enemy beacon link found inside triangle!");
                                    return false;
                                }
                            }
                        }
                    }
                }
                if (triangleFields.add(triangle)) {
                    //getLogger().info("DEBUG: Added control field!");
                    // New control field, refresh score
                	Game game = getGameMgr().getGame(point2d.getX(), point2d.getY());
                    game.getScorecard().refreshScores(owner, "area");
                    game.getScorecard().refreshScores(owner, "triangles");
                    //getLogger().info("DEBUG: New score is " + triangle.getArea());
                    return true;
                } else {
                    // getLogger().info("DEBUG: Control field already exists");
                }
            } else {
                //getLogger().info("DEBUG: beacons are not owned by the same faction");
                throw new IllegalArgumentException("beacons are not owned by the same team");
            }
        } else {
            //getLogger().info("DEBUG: Location argument is not a beacon");
            throw new IllegalArgumentException("Location argument is not a beacon");
        }
        return false;
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
     * Checks if a beacon is within the range around point
     * @param point
     * @param range
     * @return true if beacon is there, false if not
     */
    public boolean isNearBeacon(Point2D point, int range) {
        int distSquared = range*range;
        for (Point2D beacon : beaconRegister.keySet()) {
            // Distance squared check is less computationally intensive than checking the square
            if (distSquared > point.distanceSq(beacon)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a list of all nearby beacons within range
     * @param location
     * @param range
     * @return list of nearby beacons
     */
    public List<BeaconObj> getNearbyBeacons(Location location, int range) {
        int distSquared = range*range;
        List<BeaconObj> result = new ArrayList<BeaconObj>();
        Point2D point = new Point2D.Double(location.getX(), location.getZ());
        for (Point2D beacon : beaconRegister.keySet()) {
            // Distance squared check is less computationally intensive than checking the square
            if (distSquared > point.distanceSq(beacon)) {
                result.add(beaconRegister.get(beacon));
            }
        }
        return result;        
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
     * Gets the beacon connected to block.
     * @param block
     * @return BeaconObj or null if none
     */
    public BeaconObj getBeacon(Block block) {
        //getLogger().info("DEBUG: material = " + b.getType());
        // Quick check
        if (!block.getType().equals(Material.BEACON) && !block.getType().equals(Material.DIAMOND_BLOCK)
                && !block.getType().equals(Material.OBSIDIAN) && !block.getType().equals(Material.STAINED_GLASS)
                && !block.getType().equals(Material.EMERALD_BLOCK)) {
            return null;
        }
        //getLogger().info("DEBUG: correct material");
        Point2D point = new Point2D.Double(block.getLocation().getBlockX(),block.getLocation().getBlockZ());

        // Check defense blocks
        if (block.getType().equals(Material.EMERALD_BLOCK)) {
            if (defenseBlocks.containsKey(point)) {
                // Check height
                BeaconObj beacon = defenseBlocks.get(point);
                if (beacon.getY() == block.getY() + 1) {
                    // Correct height
                    return beacon;
                } else {
                    return null;
                }
            }
        }
        //getLogger().info("DEBUG: checking point " + point);

        // Check glass or obsidian
        if (block.getType().equals(Material.OBSIDIAN) || block.getType().equals(Material.STAINED_GLASS)) {
            Block below = block.getRelative(BlockFace.DOWN);
            if (!below.getType().equals(Material.BEACON)) {
                //getLogger().info("DEBUG: no beacon below here");
                return null;
            }
            point = new Point2D.Double(below.getLocation().getBlockX(),below.getLocation().getBlockZ());
            // Beacon below
            if (beaconRegister.containsKey(point)) {
                //getLogger().info("DEBUG: found in register");
                return beaconRegister.get(point);
            } else {
                //getLogger().info("DEBUG: not found in register");
                return null;
            }
        }
        // Check beacons
        if (block.getType().equals(Material.BEACON)) {
            if (beaconRegister.containsKey(point)) {
                //getLogger().info("DEBUG: found in register");
                return beaconRegister.get(point);
            } else {
                /*
                getLogger().info("DEBUG: not found in register. Known points are:");
                for (Point2D points : beaconRegister.keySet()) {
                    getLogger().info("DEBUG: " + points);
                }*/
                return null;
            }
        }
        // Check the pyramid around the beacon
        // Look for a beacon
        for (int modX = -1; modX < 2; modX++) {
            for (int modZ = -1; modZ < 2; modZ++) {
                Block test = block.getRelative(modX, 1, modZ);
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
     * Removes this beacon from team ownership and makes it unowned
     * @param beacon
     */
    public void removeBeaconOwnership(BeaconObj beacon) {
        removeBeaconOwnership(beacon, false);
    }
    public void removeBeaconOwnership(BeaconObj beacon, Boolean quiet) {
        Team oldOwner = beacon.getOwnership();
        beacon.setOwnership(null);
        
        // Remove links to the beacon (and back)
        Iterator<BeaconObj> beaconIterator = beacon.getLinks().iterator();
        while (beaconIterator.hasNext()) { 
        beaconIterator.next().removeLink(beacon);
        }
        
        // Remove links from this register 
        if (links.get(oldOwner) != null) {
        	Iterator<Line2D> linkIterator = links.get(oldOwner).iterator();
            int linkLossCount = 0;
            while (linkIterator.hasNext()) {
                Line2D link = linkIterator.next();
                if (link.getP1().equals(beacon.getLocation()) || link.getP2().equals(beacon.getLocation())) {
                    linkLossCount++;
                    linkIterator.remove();
                }
            }
            // Tell folks what's going on
            if (linkLossCount == 1 && !quiet) {
                getMessages().tellTeam(oldOwner, ChatColor.RED + "Your team lost a link!");
                getMessages().tellOtherTeams(oldOwner, ChatColor.GREEN + oldOwner.getDisplayName() + ChatColor.GREEN + " lost a link!");
            } else if (linkLossCount > 1) {
                getMessages().tellTeam(oldOwner, ChatColor.RED + "Your team lost " + linkLossCount + " links!");
                getMessages().tellOtherTeams(oldOwner, ChatColor.GREEN + oldOwner.getDisplayName() + ChatColor.GREEN + " lost " + linkLossCount + " links!");  
            }            
        }        
        
        beacon.removeLinks();        
        
        // Get any control triangles that have been removed because of this
        Iterator<TriangleField> it = triangleFields.iterator();
        while (it.hasNext()) {
            TriangleField triangle = it.next();
            if (triangle.hasVertex(beacon.getLocation())) {
                //getLogger().info("DEBUG: this beacon was part of a triangle");
                // Tell folks what's going on
                if (!quiet) {
                    getMessages().tellTeam(triangle.getOwner(), ChatColor.RED + "Your team lost a triangle worth " + triangle.getArea() + "!");
                    getMessages().tellOtherTeams(triangle.getOwner(), ChatColor.GREEN + triangle.getOwner().getDisplayName() + ChatColor.GREEN + " lost a triangle worth " + triangle.getArea() + "!");                    
                }
                // Remove triangle
                it.remove();
            }
        }
        
        // Cap the beacon with obsidian
        getBeaconzWorld().getBlockAt(beacon.getX(), beacon.getHeight() + 1, beacon.getZ()).setType(Material.OBSIDIAN);
            
        // Refresh the scores
        Scorecard sc = getGameMgr().getSC(beacon.getX(), beacon.getZ());
        if(sc!=null) sc.refreshScores(oldOwner);
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
        //getLogger().info("DEBUG: storing beacon map # " + index + " for beacon at "+ beacon.getLocation());
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

    /**
     * Sets the beacon ownership, team = null means it is unowned.
     * @param beacon
     * @param team
     */
    public void setBeaconOwner(BeaconObj beacon, Team team) {
    	Team oldowner = beacon.getOwnership();
        beacon.setOwnership(team);
        // TODO : Add other things in the future as a result of the ownership change
        Game game = getGameMgr().getGame(beacon.getX(), beacon.getZ());
		if (oldowner != null) {
			game.getScorecard().refreshScores(oldowner);      
		}
        game.getScorecard().refreshScores(team);
    }

    /**
     * Gets all enemy links not of team
     * @param team
     * @return set of links
     */
    public Set<Line2D> getEnemyLinks(Team team) {
        Set<Line2D> result = new HashSet<Line2D>();
        //getLogger().info("DEBUG: There are " + links.keySet().size() + " teams in the link database");
        for (Team opposition : links.keySet()) {
            //getLogger().info("DEBUG: checking team " + opposition.getName() + " links");
            if (!team.equals(opposition)) {
                result.addAll(links.get(opposition));
                //getLogger().info("DEBUG: added " + links.get(opposition).size() + " links");
            }
        }
        return result;
    }

    /**
     * Adds a block to the defense block register. Blocks around a beacon are automatically added.
     * @param location
     * @param beacon
     */
    public void addBeaconDefenseBlock(Location location, BeaconObj beacon) {
        addBeaconDefenseBlock(location.getBlockX(), location.getBlockZ(), beacon);
    }

    /**
     * Adds a block to the defense block register. Blocks around a beacon are automatically added.
     * @param x
     * @param z
     * @param beacon
     */
    public void addBeaconDefenseBlock(int x, int z, BeaconObj beacon) {
        Point2D point = new Point2D.Double(x,z);
        defenseBlocks.put(point, beacon);
        Set<Point2D> points = defenseBlocksInverse.get(beacon);
        if (points == null) {
            points = new HashSet<Point2D>();
        }
        points.add(point);
        defenseBlocksInverse.put(beacon, points); 
    }

    /**
     * Get the beacon associated with this defensive block
     * @param point
     * @return beacon or null if it doesn't exist
     */
    public BeaconObj getBeaconDefenseAt(Point2D point) {
        return defenseBlocks.get(point);
    }

    /**
     * Gets all the defense blocks at this beacon
     * @param beacon
     * @return Set of points
     */
    public Set<Point2D> getDefensesAtBeacon(BeaconObj beacon) {
        return defenseBlocksInverse.get(beacon);
    }
}
