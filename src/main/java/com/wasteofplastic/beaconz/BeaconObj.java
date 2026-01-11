/*
 * Copyright (c) 2015 - 2025 tastybento
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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

import com.destroystokyo.paper.MaterialTags;

/**
 * Represents a beacon
 * @author tastybento
 *
 */
public class BeaconObj extends BeaconzPluginDependent {
    private final Point2D location;
    private final int x;
    private final int z;
    private final int y;
    private long hackTimer;
    private Team ownership;
    //private Set<BeaconObj> links;
    private Integer id = null;
    private boolean newBeacon = true;
    private static final List<BlockFace> FACES = new ArrayList<>(Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST));
    private static final boolean DEBUG = false;
    private HashMap<Block, DefenseBlock> defenseBlocks = new HashMap<>();
    private final Set<BeaconObj> links = new HashSet<>();

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
        this.y = y;
        this.hackTimer = System.currentTimeMillis();
        this.ownership = owner;
        this.links.clear();
        this.newBeacon = true;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
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
        return new Location(getBeaconzWorld(),location.getX(),y,location.getY());
    }

    /**
     * @return the beacons directly linked from this beacon
     */
    public Set<BeaconObj> getLinks() {
        return links;
    }

    /**
     * Add a link from this beacon to another beacon and make a return link
     * @param beacon the beacon to link to
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
     * @param beacon the beacon to link to
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
        return y;
    }

    /**
     * @return the newBeacon
     */
    public boolean isNewBeacon() {
        return newBeacon;
    }

    /**
     * Checks if a beacon base is clear of blocks and above the blocks all the way to the sky
     * @return false if clear, true if not
     */
    public boolean isNotClear() {
        Block beacon = getBeaconzWorld().getBlockAt((int)location.getX(), y, (int)location.getY());
        if (DEBUG) getLogger().info("DEBUG: beacon block y = " + beacon.getY() + " " + beacon.getLocation());
        for (BlockFace face: FACES) {
            Block block = beacon.getRelative(face);
            if (DEBUG) getLogger().info("DEBUG: highest block at " + block.getX() + "," + block.getZ() + " y = " + getHighestBlockYAt(block.getX(), block.getZ()));
            if (getHighestBlockYAt(block.getX(), block.getZ()) != beacon.getY()) {
                if (DEBUG) getLogger().info("DEBUG: Beacon is not cleared");
                return true;
            }
        }
        if (DEBUG) getLogger().info("DEBUG: Checking defences");
        // Check all the defense blocks too
        Block block;
        for (Point2D point: getRegister().getDefensesAtBeacon(this)) {
            if (DEBUG) getLogger().info("DEBUG: checking = " + (int)point.getX() + "," + y + ", " + (int)point.getY());
            block = getBeaconzWorld().getBlockAt((int)point.getX(), y, (int)point.getY());
            if (DEBUG) getLogger().info("DEBUG: Block Y = " + block.getY() + " and it is " + block.getType());
            if (block.getY() != getHighestBlockYAt((int)point.getX(), (int)point.getY())) {
                if (DEBUG) getLogger().info("DEBUG: Beacon is no cleared");
                return true;
            }
        }
        if (DEBUG) getLogger().info("DEBUG: Beacon is cleared");
        return false;
    }

    /**
     * Tracks defense blocks
     * @param block
     * @param levelRequired
     * @param uuid 
     */
    public void addDefenseBlock(Block block, int levelRequired, UUID uuid) {
        defenseBlocks.put(block, new DefenseBlock(block, levelRequired, uuid));
    }
    
    /**
     * Tracks defense blocks
     * @param block
     * @param levelRequired
     * @param uuid 
     */
    public void addDefenseBlock(Block block, int levelRequired, String uuid) {
        defenseBlocks.put(block, new DefenseBlock(block, levelRequired, uuid)); 
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
    public HashMap<Block, DefenseBlock> getDefenseBlocks() {
        return defenseBlocks;
    }

    /**
     * @param defenseBlocks the defenseBlocks to set
     */
    public void setDefenseBlocks(HashMap<Block,DefenseBlock> defenseBlocks) {
        this.defenseBlocks = defenseBlocks;
    }

    /**
     * Removes the longest link. Used when a link block is removed.
     * @return true if a link was actually removed
     */
    public boolean removeLongestLink() {
        BeaconObj furthest = null;
        double maxDistance = 0;
        for (BeaconObj link: links) {
            double distance = location.distanceSq(link.getPoint());
            if (distance > maxDistance) {
                maxDistance = distance;
                furthest = link;
            }
        }
        if (furthest != null) {
            // Remove link from both ends
            furthest.removeLink(this);
            removeLink(furthest);
            // Remove any triangles related to these two beaconz
            Iterator<TriangleField> it = getRegister().getTriangleFields().iterator();
            while (it.hasNext()) {
                TriangleField triangle = it.next();
                if (triangle.hasVertex(this.location) && triangle.hasVertex(furthest.location)) {
                 // Find any players in the triangle being removed
                    for (Player player: getServer().getOnlinePlayers()) {
                        if (getBeaconzWorld().equals(player.getWorld())) {
                            if (triangle.contains(new Point2D.Double(player.getLocation().getX(), player.getLocation().getZ()))) {
                                // Player is in triangle, remove effects
                                for (PotionEffect effect : getPml().getTriangleEffects(player.getUniqueId()))
                                    player.removePotionEffect(effect.getType());
                            }
                        }
                    }                    
                    it.remove();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks the integrity of the beacon and fixes it if required
     */
    public void checkIntegrity() {
        Block b = getBeaconzWorld().getBlockAt(x, y, z);
        if (!b.getType().equals(Material.BEACON)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing beacon block!");
            b.setType(Material.BEACON);
        }
        // Check the capstone
        if (ownership == null && !b.getRelative(BlockFace.UP).getType().equals(Material.OBSIDIAN)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing capstone block!");
            b.getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
        }
        
        
        
        if (ownership != null && (!MaterialTags.STAINED_GLASS.isTagged(b.getRelative(BlockFace.UP)) && b.getRelative(BlockFace.UP).getType() != Settings.teamBlock.get(ownership))) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing team glass block!");
            b.getRelative(BlockFace.UP).setType(Settings.teamBlock.get(ownership));
        }
        // Create the pyramid
        b = b.getRelative(BlockFace.DOWN);

        // All diamond blocks for now
        if (!b.getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing diamond block!");
            b.setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.SOUTH).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing S diamond block!");
            b.getRelative(BlockFace.SOUTH).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.SOUTH_EAST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing SE diamond block!");
            b.getRelative(BlockFace.SOUTH_EAST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.SOUTH_WEST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing SW diamond block!");
            b.getRelative(BlockFace.SOUTH_WEST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.EAST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing E diamond block!");
            b.getRelative(BlockFace.EAST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.WEST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing W diamond block!");
            b.getRelative(BlockFace.WEST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.NORTH).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing N diamond block!");
            b.getRelative(BlockFace.NORTH).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.NORTH_EAST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing NE diamond block!");
            b.getRelative(BlockFace.NORTH_EAST).setType(Material.DIAMOND_BLOCK);
        }
        if (!b.getRelative(BlockFace.NORTH_WEST).getType().equals(Material.DIAMOND_BLOCK)) {
            getLogger().severe("Beacon at " + x + " " + y + " " + z + " missing SW diamond block!");
            b.getRelative(BlockFace.NORTH_WEST).setType(Material.DIAMOND_BLOCK);
        }
    }

    /** 
     * Get the level of the highest defense block placed on top of a beacon
     * @return
     */
    public int getHighestBlockLevel() {
        int highestBlock = 0;
        Iterator<Entry<Block, DefenseBlock>> it = this.getDefenseBlocks().entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, DefenseBlock> defenseBlock = it.next();
            if (defenseBlock.getKey().getType().equals(Material.AIR)) {
                // Clean up if any blocks have been removed by creatives, or moved due to gravity.
                it.remove();
            } else {
                highestBlock = Math.max(highestBlock, defenseBlock.getValue().getLevel());
            }
        } 
        return highestBlock;
    }
    

    /** 
     * Get the height of the highest defense block placed on top of a beacon
     * @return
     */
    public int getHighestBlockY() {
        int highestBlock = y;
        Iterator<Entry<Block, DefenseBlock>> it = this.getDefenseBlocks().entrySet().iterator();
        while (it.hasNext()) {
            Entry<Block, DefenseBlock> defenseBlock = it.next();
            if (defenseBlock.getKey().getType().equals(Material.AIR)) {
                // Clean up if any blocks have been removed by creatives, or moved due to gravity.
                it.remove();
            } else {
                highestBlock = Math.max(highestBlock, defenseBlock.getValue().getBlock().getY());
            }
        } 
        return highestBlock;
    }
    
    
    /**
     * Checks if any layer above the beacon matches the locking criteria
     * This is a mechanism to allow for permanent ownership of beacons
     * If any one level above the beacon has a certain number of "locking blocks", these blocks are unbreakable by all but the owner team
     * 
     * @return
     */    
    public Boolean isLocked () {
        boolean rc = false;
        int maxHeight = getHighestBlockY();
        for (int i = y; i <= maxHeight; i++) {
            if (nbrToLock(i) <= 0) {
                rc = true;
                break;
            }
        }            
     return rc;   
    }
    
    /**
     * Returns the number of "locking blocks" that must still be placed at the given height to block the beacon
     * The returned integer can be negative, indicating that more than enough blocks have been placed
     * Locking blocks could be defined in Settings, but I really think we should stick with Settings.linkRewards (default: Emerald)
     * The number of locking blocks required is proportional to the number of players in the teams. 
     * The number given in Settings applies to the largest team, all others go down proportionately.
     * 
     * @param height
     * @return
     */
    
    public int nbrToLock(int height) {               
        
        int maxLocking = Settings.nbrLockingBlocks;
        Material lockingBlock = Material.EMERALD_BLOCK;
        int missing = maxLocking;
                       
        // Make sure the height is above the beacon
        if (height >= y) {
        
            // Get the locking block material
            if (Material.getMaterial(Settings.lockingBlock.toUpperCase()) != null) {
                lockingBlock = Material.getMaterial(Settings.lockingBlock.toUpperCase());                
            }
            
            // Figure out how many locking blocks we need for the owner team
            int reqLocking = 3;
            int maxSize = ownership.getSize();
            /*for (Team t : ownership.getScoreboard().getTeams()) {
                if (t.getSize() > maxSize) {
                    maxSize = t.getSize();
                }
            }*/
            Scorecard sc = getGameMgr().getSC(ownership);
            if (sc != null) {
                for (Team t : sc.getTeams()) {
                    if (t.getSize() > maxSize) {
                        maxSize = t.getSize();
                    }                    
                }
            }
            reqLocking = maxLocking * ownership.getSize() / maxSize; // integer division...
            if (reqLocking > 8) reqLocking = 8;                      // ensure it's at most 8 blocks
            
            // See how many locking blocks are present at the given height
            int lockBlocks = 0;
            Material lockBlock = lockingBlock;
            Block b = getBeaconzWorld().getBlockAt(x, height, z);
            if (b.getType().equals(lockBlock)) missing--;
            if (b.getRelative(BlockFace.SOUTH).getType().equals(lockBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.SOUTH_EAST).getType().equals(lockBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.SOUTH_WEST).getType().equals(lockBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.EAST).getType().equals(lockBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.WEST).getType().equals(lockBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.NORTH).getType().equals(lockBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.NORTH_EAST).getType().equals(lockBlock)) lockBlocks++;
            if (b.getRelative(BlockFace.NORTH_WEST).getType().equals(lockBlock)) lockBlocks++;
            
            missing = reqLocking - lockBlocks; 
        }        
        
        return missing;
    }        
    
}
