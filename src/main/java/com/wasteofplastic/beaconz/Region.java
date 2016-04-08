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

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.material.TrapDoor;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Region extends BeaconzPluginDependent {

    private Beaconz plugin;
    private Point2D [] corners;
    private Location spawnpoint;
    private int loopcount;
    private int chX;
    private int chZ;
    private BukkitTask task = null;
    private int totalregen;

    /** 
     * Region instantiates the various regions in the world     
     * A region belongs to a game - except for the lobby, which is a region with no game
     * A region is defined by two sets of X:Z coordinates and encompasses all Ys
     * A region can be created and recreated freely; region.populate is not in the constructor and must be called explicitly
     */
    public Region(Beaconz beaconzPlugin, Point2D[] corners) {
        super(beaconzPlugin);
        this.plugin = beaconzPlugin;
        this.corners =  setCorners(corners);
        initialize(true);
    }

    /**
     * Initializes the class without (re)constructing it
     */
    public void initialize(Boolean newGame) {    	

        if (newGame) {
            // Set the region's default spawn point; start looking at the center of the region
            double rad = getRadius();
            setSpawnPoint(getCenter(), (int) rad);

            // That's it. Regenerate() is called from GameMgr or Game
        }    	
    }

    /** 
     * Regenerates the region by regenerating its chunks
     * Cleans up the game area so it can be played again
     * Remember that regions are created so their limits match full chunks
     * Since the BeaconPopulator is registered with the World, it is used automatically when regenerating the chunks
     */
    public void regenerate(final CommandSender sender) {
        if (this != getGameMgr().getLobby()) {        	

            getLogger().info("Regenerating region at Ctr:Rad [" + this.getCenter().getX() + ", " + this.getCenter().getY() + "] : " + this.getRadius() + " ==> xMin: " + corners[0].getX() + " zMin: " +  corners[1].getX() + " xMax: " + corners[0].getY() + " zMax: " + corners[1].getY());

            // ************************************************************
            // TODO:
            // WE SHOULD DISPLAY A "% DONE" MESSAGE
            // AND BLOCK THE RESET COMMAND IF A RESET IS ALREADY RUNNING
            // **********************************************************

            // First clear the current register for the region
            getRegister().clear(this);        	        	

            // Then regenerate, in spurts of 25 chunks
            final int xMin = (int) corners[0].getX() -16;
            final int xMax = (int) corners[1].getX() +16;
            final int zMin = (int) corners[0].getY() -16;
            final int zMax = (int) corners[1].getY() +16;                  	

            totalregen = 0;
            chX = xMin;
            chZ = zMin;
            final int step = 25;
            Settings.dontpopulate.clear();

            // Regenerate the chunks in spurts of 25 
            // Can't do this asynchronously, as it relies on API calls
            task = getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
                public void run() {
                    loopcount = 0;
                    Settings.dontpopulate.clear();
                    outerloop:
                        while (chX <= xMax) {
                            while (chZ <= zMax) {
                                // Settings.dontpopulate is used to abort the populator - there's no need to repopulate the chunks, they'll get populated when loaded in-game              	
                                Settings.dontpopulate.add((chX/16) + ":" + (chZ/16));
                                getLogger().info("Regenerating chunk: " + (chX/16) + ":" + (chZ/16));
                                getBeaconzWorld().regenerateChunk(chX/16, chZ/16); 
                                loopcount++;
                                totalregen++;
                                if (loopcount >= step) {
                                    getLogger().info("Region.regenerate() -- loop break at chunks: " + (chX/16) + ":" + (chZ/16) + "----- or blocks: " + chX + ":" + chZ);
                                    break outerloop;
                                }
                                chZ = chZ + 16;
                            }
                            chZ = zMin;
                            chX = chX + 16;
                        }
                    if (chX > xMax) {                		                	
                        task.cancel();
                        finishRegenerating(sender);
                    }
                }
            }, 5L, 0L);        	
        }        
    }

    public void finishRegenerating(CommandSender sender) {
        // Wrap things up
        createCorners();
        sender.sendMessage(ChatColor.GREEN + "Reset complete. Regenerated " + totalregen + " chunks."); 
    }

    /**
     * Creates the corner-most beacons so that the map can be theoretically be covered entirely (almost)
     * This is only for creating new regions. Existing regions have their corner beacons loaded with Register.loadRegister().
     */
    public void createCorners() {

        // Check corners
        Set<Point2D> fourcorners = new HashSet<Point2D>();
        int xMin = (int) corners[0].getX();
        int xMax = (int) corners[1].getX();
        int zMin = (int) corners[0].getY();
        int zMax = (int) corners[1].getY();
        fourcorners.add(new Point2D.Double(xMin+1,zMin+1));
        fourcorners.add(new Point2D.Double(xMin+1,zMax-1));
        fourcorners.add(new Point2D.Double(xMax-1,zMin+1));
        fourcorners.add(new Point2D.Double(xMax-1,zMax-1));
        for (Point2D point : fourcorners) {
            if (!getRegister().isNearBeacon(point, 5)) {
                Block b = getBeaconzWorld().getHighestBlockAt((int)point.getX(), (int)point.getY());
                while (b.getType().equals(Material.AIR) || b.getType().equals(Material.LEAVES) || b.getType().equals(Material.LEAVES_2)) {          
                    if (b.getY() == 0) {
                        // Oops, nothing here
                        break;
                    }
                    b = b.getRelative(BlockFace.DOWN);
                }
                if (b.getY() > 3) {
                    // Create a beacon
                    //getLogger().info("DEBUG: createCorners() made a beacon at " + b.getLocation());
                    b.setType(Material.BEACON);
                    // Register the beacon
                    getRegister().addBeacon(null, b.getLocation());
                    // Add the capstone
                    b.getRelative(BlockFace.UP).setType(Material.OBSIDIAN);
                    // Create the pyramid
                    b = b.getRelative(BlockFace.DOWN);

                    // All diamond blocks for now
                    b.setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.SOUTH).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.SOUTH_EAST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.SOUTH_WEST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.EAST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.WEST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.NORTH).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.NORTH_EAST).setType(Material.DIAMOND_BLOCK);
                    b.getRelative(BlockFace.NORTH_WEST).setType(Material.DIAMOND_BLOCK);
                }
            }
        }
    }


    /** 
     * Sets the region's corners, and in the right order
     */
    public Point2D [] setCorners(Point2D [] c) {
        return setCorners(c[0].getX(), c[0].getY(), c[1].getX(), c[1].getY());
    }
    public Point2D [] setCorners(Double xMin, Double zMin, Double xMax, Double zMax) {
        if (xMin > xMax) {
            Double x = xMin;
            xMin = xMax;
            xMax = x;
        }
        if (zMin > zMax) {
            Double z = zMin;
            zMin = zMax;
            zMax = z;
        }    	
        Point2D [] c = {new Point2D.Double(xMin, zMin), new Point2D.Double(xMax, zMax)};
        return c;
    }

    /**
     * Returns the region's corners as a Point2D []
     */
    public Point2D [] corners() {
        return corners;
    }

    /**
     * Returns the region's center point  
     */
    public Point2D getCenter() {
        Double x = (corners[0].getX() + corners[1].getX()) / 2.0;
        Double z = (corners[0].getY() + corners[1].getY()) / 2.0;
        return new Point2D.Double(x,z);
    }

    /**
     * Returns the region's radius
     */
    public Double getRadius() {
        return (Math.abs(corners[0].getX()) + Math.abs(corners[1].getX())) / 2.0;
    }

    /**
     * Returns the region's coordinates in a pretty display string
     */
    public String displayCoords() {    	  
        String coords = "center: [" + 
                (int) getCenter().getX() + ":" + (int) getCenter().getY() + "] - corners: [" + 
                (int) corners()[0].getX() + ":" + (int) corners()[0].getY() + "] and [" + 
                (int) corners()[1].getX() + ":" + (int) corners()[1].getY() + "]";
        return coords;
    }

    /**
     * Determines whether a beacon or point lie inside the region
     */
    public Boolean containsBeacon(BeaconObj beacon) {
        return containsPoint(beacon.getX() + 0.0, beacon.getY() + 0.0);
    }
    public Boolean containsPoint(Point2D point) {
        return (containsPoint(point.getX(),point.getY()));   
    }
    public Boolean containsPoint(Double x, Double z) {
        // Make sure the coords are in the right order, although they should be..
        Double xMin = corners[0].getX();
        Double zMin = corners[0].getY();
        Double xMax = corners[1].getX();
        Double zMax = corners[1].getY();
        if (xMin > xMax) {
            Double a = xMin;
            xMin = xMax;
            xMax = a;
        }
        if (zMin > zMax) {
            Double b = zMin;
            zMin = zMax;
            zMax = b;
        }
        Boolean contains = (xMin <= x && xMax >= x && zMin <= z && zMax >= z);
        //getLogger().info("Contains: " + contains + " == x: " + x + " z: " + z + " corner1: " + corners[0].getX() + ":" + corners[0].getY() + " corner2: " + corners[1].getX() + ":" + corners[1].getY());
        return contains;
    }

    /**
     * Determines whether a player is inside the region
     */
    public Boolean isPlayerInRegion(Player player) {
        return containsPoint(player.getLocation().getX(), player.getLocation().getZ());
    }

    /**
     * Sets the region's spawn point
     */
    public void setSpawnPoint(Point2D point, Integer radius) {
        Double y = getBeaconzWorld().getHighestBlockYAt((int) point.getX(), (int) point.getY()) * 1.0;
        setSpawnPoint(new Location(getBeaconzWorld(), point.getX(), y, point.getY()), radius);
    }
    
    /**
     * Sets a safe spawn point in a region
     * @param loc
     * @param radius
     */
    public void setSpawnPoint(Location loc, Integer radius){
        spawnpoint = findSafeSpot(loc, radius);
    }

    /**
     * Returns the region's spawn point
     */
    public Location getSpawnPoint() {
        return spawnpoint;
    }

    /**
     * Teleports a player to the region's spawn point
     */
    public void tpToRegionSpawn(Player player) {

        player.teleport(spawnpoint);
        if (this.equals(getGameMgr().getLobby())) {
            enterLobby(player);
        }
    }

    /**
     * Handles player entering the Lobby
     */
    public void enterLobby(final Player player) {
        String titleline = "Welcome to Beaconz!";
        String subtitleline = "Join a game, defeat opposing teams!";

        // Welcome player in chat
        player.sendMessage(ChatColor.GREEN + titleline);
        player.sendMessage(ChatColor.AQUA + subtitleline);

        // Welcome player on screen        
        getServer().dispatchCommand(getServer().getConsoleSender(),
                "title " + player.getName() + " title {\"text\":\"" + titleline + "\", \"color\":\"" + "gold" + "\"}");  
        getServer().dispatchCommand(getServer().getConsoleSender(),
                "title " + player.getName() + " subtitle {\"text\":\"" + subtitleline + "\", \"color\":\"" + "gold" + "\"}");        
        // Show the lobby scoreboard - wait for title message to disappear
        if (this.equals(getGameMgr().getLobby())) {

            getServer().getScheduler().runTaskLater(plugin, new Runnable() {

                public void run() {
                    // This runs after a few seconds, so make sure that player is still in the lobby
                    if (getGameMgr().isPlayerInLobby(player)) {
                        Scoreboard sb = plugin.getServer().getScoreboardManager().getNewScoreboard();
                        Objective sbobj = null;
                        Score scoreline = null;
                        player.setScoreboard(sb);

                        try {
                            sb.clearSlot(DisplaySlot.SIDEBAR);                    
                        } catch (Exception e){ };

                        sbobj = sb.registerNewObjective("text", "dummy");
                        sbobj.setDisplaySlot(DisplaySlot.SIDEBAR);        
                        sbobj.setDisplayName(ChatColor.GREEN + "Welcome to Beaconz! ");
                        scoreline = sbobj.getScore("You are in the lobby area.");
                        scoreline.setScore(15);
                        scoreline = sbobj.getScore("Hit a sign to start a game!");
                        scoreline.setScore(14);
                        scoreline = sbobj.getScore("Beaconz is a team game where");
                        scoreline.setScore(13);       
                        scoreline = sbobj.getScore("you try to find, claim and link");
                        scoreline.setScore(12);       
                        scoreline = sbobj.getScore("naturally occuring beaconz in");
                        scoreline.setScore(11);       
                        scoreline = sbobj.getScore("the world. You can mine beaconz");
                        scoreline.setScore(10);       
                        scoreline = sbobj.getScore("for goodies and defend them");
                        scoreline.setScore(9);       
                        scoreline = sbobj.getScore("with blocks and traps.");
                        scoreline.setScore(8);  

                        player.setScoreboard(sb);

                    }
                }
            }, 60L);
        }
    }

    /** 
     * Handles player Exit event
     */
    public void exit(Player player) {
        player.setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());
    }

    /** 
     * Handles player Enter event
     */
    public void enter(Player player) {
        if (getGameMgr().isPlayerInLobby(player)) {
            enterLobby(player);
        } else {
            Game game = getGameMgr().getGame(this);
            String teamname = "no";
            Team team = game.getScorecard().getTeam(player);
            if (team != null) teamname = team.getDisplayName();
            player.setScoreboard(game.getScorecard().getScoreboard());
            //game.getScorecard().sendPlayersHome(player, true);        	        

            // Welcome player in chat
            player.sendMessage(ChatColor.GREEN + "Welcome to Beaconz! ");
            player.sendMessage(ChatColor.AQUA + "You're playing game " + game.getName() + " in " + game.getGamemode() + " mode!");
            player.sendMessage(ChatColor.AQUA + "You're a member of " + teamname + " team!");
            player.sendMessage(ChatColor.AQUA + "Your team's objective is to capture " + game.getGamegoalvalue() + " " + game.getGamegoal() + "!");

            // Welcome player on screen        
            String titleline = "Beaconz game " + game.getName() + "!";
            String subtitleline = "You're playing " + game.getGamemode() + " mode - " + teamname + " team!";
            getServer().dispatchCommand(getServer().getConsoleSender(),
                    "title " + player.getName() + " title {\"text\":\"" + titleline + "\", \"color\":\"" + "gold" + "\"}");  
            getServer().dispatchCommand(getServer().getConsoleSender(),
                    "title " + player.getName() + " subtitle {\"text\":\"" + subtitleline + "\", \"color\":\"" + "gold" + "\"}");    		
        }
    }

    /** 
     * Find the nearest safe spot to a given point, within a radius
     * The radius is arbitrarily limited to 20 blocks
     * If no safe location within radius is found, this creates a safe spot by converting the block at location to 
     */
    public Location findSafeSpot (Location location, Integer radius) {
        if (radius > 20) radius = 20;
        Location safeloc = null;
        if (location != null) {
            // look for a safe spot at location and within radius
            Block bl = location.getBlock();
            String usedxyz = "";

            // sweep in a concentric cube pattern to check for a safe spot    
            Location checkloc = null;
            outerloop:
                for (int rad = 0; rad < radius; rad++) {
                    //for (int y = -rad; y <= rad; y++) {
                    for (int z = -rad; z <= rad; z++) {
                        for (int x = -rad; x <= rad; x++) {    						
                            String coords = "#" + x + " "+ z + "#";
                            if (!usedxyz.contains(coords)) {
                                usedxyz = usedxyz + coords;    							
                                checkloc = getBeaconzWorld().getHighestBlockAt(bl.getRelative(x, 0, z).getLocation()).getLocation();
                                if (isLocationSafe(checkloc)) {
                                    safeloc = checkloc.add(0.5, 0.0, 0.5);
                                    break outerloop;    						
                                }
                            }
                        }
                    }
                    //}
                }
        }
        if (safeloc == null) {
            getLogger().info(ChatColor.RED + "Could not find a safe spot for region spawn point. Region at " + displayCoords() + ". Using default.");
            safeloc = getBeaconzWorld().getHighestBlockAt((int) location.getX(), (int) location.getZ()).getLocation();
            safeloc = safeloc.add(0.5, 0.0, 0.5);
            if (safeloc.getBlock().isLiquid() || safeloc.getBlock().isEmpty()) {
                safeloc.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
            }
        }
        return safeloc;
    }

    /**
     * Checks if this location is safe for a player to teleport to. 
     * Unsafe is any liquid or air and also if there's no space
     * 
     * @param location
     *            - Location to be checked
     * @return true if safe, otherwise false
     */
    public Boolean isLocationSafe(Location location) {
        // TODO: improve the safe location finding.
        // note: water lilies should not be safe

        if (location == null) {
            return false;
        }

        final Block ground = location.getBlock().getRelative(BlockFace.DOWN);
        final Block space1 = location.getBlock();
        final Block space2 = location.getBlock().getRelative(BlockFace.UP);

        // If ground is AIR, then this is either not good, or they are on slab,
        // stair, etc.
        if (ground.getType() == Material.AIR) {
            //Bukkit.getLogger().info("DEBUG: air");
            return false;
        }
        // Liquid is unsafe
        if (ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
            return false;
        }

        // Portals are not "safe"
        if (space1.getType() == Material.PORTAL || ground.getType() == Material.PORTAL || space2.getType() == Material.PORTAL
                || space1.getType() == Material.ENDER_PORTAL || ground.getType() == Material.ENDER_PORTAL || space2.getType() == Material.ENDER_PORTAL) {
            return false;
        }

        MaterialData materialData = ground.getState().getData();
        if (materialData instanceof SimpleAttachableMaterialData) {
            //Bukkit.getLogger().info("DEBUG: trapdoor/button/tripwire hook etc.");
            if (materialData instanceof TrapDoor) {
                TrapDoor trapDoor = (TrapDoor)materialData;
                if (trapDoor.isOpen()) {
                    //Bukkit.getLogger().info("DEBUG: trapdoor open");
                    return false;
                }
            } else {
                return false;
            }
            //Bukkit.getLogger().info("DEBUG: trapdoor closed");
        }
        if (ground.getType().equals(Material.CACTUS) || ground.getType().equals(Material.BOAT) || ground.getType().equals(Material.FENCE)
                || ground.getType().equals(Material.NETHER_FENCE) || ground.getType().equals(Material.SIGN_POST) || ground.getType().equals(Material.WALL_SIGN)) {
            // Bukkit.getLogger().info("DEBUG: cactus");
            return false;
        }
        // We don't want to be standing on leaves either
        if (ground.getType().equals(Material.LEAVES)) {
            return false;
        }      
        // Check that the space is not solid
        // The isSolid function is not fully accurate (yet) so we have to
        // check a few other items
        // isSolid thinks that PLATEs and SIGNS are solid, but they are not
        if (space1.getType().isSolid() && !space1.getType().equals(Material.SIGN_POST) && !space1.getType().equals(Material.WALL_SIGN)) {
            return false;
        }
        if (space2.getType().isSolid() && !space2.getType().equals(Material.SIGN_POST) && !space2.getType().equals(Material.WALL_SIGN)) {
            return false;
        }
        // Safe
        return true;
    }

}
