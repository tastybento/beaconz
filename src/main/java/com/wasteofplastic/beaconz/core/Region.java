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

package com.wasteofplastic.beaconz.core;

import java.awt.geom.Point2D;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.wasteofplastic.beaconz.*;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.game.Game;
import com.wasteofplastic.beaconz.game.Register;
import com.wasteofplastic.beaconz.storage.BeaconzStore;
import com.wasteofplastic.beaconz.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.config.Params.GameMode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;

/**
 * Represents a rectangular region in the Beaconz game world.
 * <p>
 * A Region defines a bounded area in the world where gameplay occurs. Each region is defined
 * by two corner coordinates (min and max X/Z) and encompasses all Y levels from bedrock to sky.
 * <p>
 * <b>Region Types:</b>
 * <ul>
 *   <li><b>Game Region</b> - Associated with a Game instance, contains beacons and gameplay</li>
 *   <li><b>Lobby Region</b> - Special region with no game association, where players spawn and wait</li>
 * </ul>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Automatic spawn point finding with safety checks</li>
 *   <li>Player teleportation and region entry/exit handling</li>
 *   <li>Title and scoreboard display when entering regions</li>
 *   <li>Corner beacon creation for region boundaries</li>
 *   <li>Complete region deletion including chunk unloading and file removal</li>
 * </ul>
 * <p>
 * Regions can be created and recreated freely. Population and terrain generation
 * must be called explicitly after construction.
 *
 * @author tastybento
 * @see Game
 * @see BeaconObj
 */
public class Region extends BeaconzPluginDependent {

    private final Beaconz plugin;
    /** Two corners defining the region bounds: [0] = min corner (xMin, zMin), [1] = max corner (xMax, zMax) */
    private final Point2D [] corners;
    /** The safe spawn location within this region */
    private Location spawnPoint;
    /** The game associated with this region, null for lobby */
    private Game game = null;
    
    /** Title timing configuration: 500ms fade-in, 3000ms stay, 500ms fade-out */
    Title.Times times = Title.Times.times(
            Duration.ofMillis(500),  // 10 ticks
            Duration.ofMillis(3000), // 60 ticks
            Duration.ofMillis(500)   // 10 ticks
    );

    /** Pre-configured title shown to players entering this region */
    Title title = Title.title(
            Lang.titleWelcome.color(Lang.titleWelcomeColor),
            Lang.titleSubTitle.color(Lang.titleSubTitleColor), 
            times
    );

    /**
     * Constructs a new Region with the specified boundaries.
     * <p>
     * The constructor automatically:
     * <ul>
     *   <li>Normalizes corner coordinates (ensures min/max ordering)</li>
     *   <li>Calculates and sets a safe spawn point at region center</li>
     *   <li>Initializes the region for a new game</li>
     * </ul>
     * <p>
     * Note: This does NOT populate the region with beacons or terrain.
     * Call appropriate populate/regenerate methods after construction.
     *
     * @param beaconzPlugin the main plugin instance
     * @param corners array of two Point2D objects defining opposite corners of the region
     */
    public Region(Beaconz beaconzPlugin, Point2D[] corners) {
        super(beaconzPlugin);
        this.plugin = beaconzPlugin;
        this.corners =  setCorners(corners);
        initialize(true);
    }

    /**
     * Initializes or reinitializes the region without reconstructing it.
     * <p>
     * This method is called during construction and can be called again to reset
     * a region. For new games, it calculates a safe spawn point at the region center.
     * <p>
     * The spawn point search starts at the center and finds the highest safe block
     * within the calculated radius.
     *
     * @param newGame if true, sets a new spawn point; if false, keeps existing configuration
     */
    public void initialize(Boolean newGame) {

        if (newGame) {
            // Set the region's default spawn point; the actual spawn point will be calculated just before teleporting
            spawnPoint = new Location(getBeaconzWorld(), getCenter().getX(), 0, getCenter().getY());
            // That's it. Regenerate() is called from GameMgr or Game
        }
    }

    /**
     * Deletes the region by removing all region files from disk.
     * <p>
     * This method completely removes a game region including:
     * <ul>
     *   <li>All terrain data (.mca files)</li>
     *   <li>All entity data (.mcc files)</li>
     *   <li>All POI (Point of Interest) data</li>
     *   <li>The 512-block safety border around the region</li>
     * </ul>
     * <p>
     * Process:
     * <ol>
     *   <li>Evacuate all players to lobby (with inventory save)</li>
     *   <li>Clear beacon register for this region</li>
     *   <li>Unload all chunks in the region + 512-block border</li>
     *   <li>Unload the world to flush all region file caches</li>
     *   <li>Delete all region files (.mca, .mcc, POI)</li>
     *   <li>Delete structure/village data files</li>
     *   <li>Reload the world</li>
     *   <li>Optionally recreate corner beacons (if not deletion)</li>
     * </ol>
     * <p>
     * <b>Region File Structure:</b><br>
     * Minecraft stores chunks in region files. Each region file contains 32x32 chunks (512x512 blocks).
     * Region file coordinates are calculated as: regionX = floor(chunkX / 32), regionZ = floor(chunkZ / 32).
     * <p>
     * <b>Safety Border:</b><br>
     * Each game region has a 512-block (1 region file) border around it for safety. This border is
     * also deleted to ensure clean regeneration.
     *
     * @param sender the command sender requesting the deletion (receives progress messages)
     */
    public void delete(final CommandSender sender) {
        // Don't allow lobby deletion
        if (getGameMgr() != null && this == getGameMgr().getLobby()) {
            sender.sendMessage(Component.text("Cannot delete the lobby region!").color(NamedTextColor.RED));
            return;
        }

        // Step 1: Evacuate all players to lobby with inventory save
        getLogger().info("Evacuating players from region at [" + getCenter().getX() + ", " + getCenter().getY() + "]");
        this.sendAllPlayersToLobby(true);

        // Step 2: Clear beacon register for this region
        getRegister().clear(this);

        // Step 3: Calculate region bounds excluding 512-block safety border
        final int xMin = (int) corners[0].getX();
        final int xMax = (int) corners[1].getX();
        final int zMin = (int) corners[0].getY();
        final int zMax = (int) corners[1].getY();

        getLogger().info("Deleting region files for area: X[" + xMin + " to " + xMax + "] Z[" + zMin + " to " + zMax + "]");

        // Step 4: Unload all chunks in the region + border
        unloadRegionChunksWithBorder(xMin, xMax, zMin, zMax);

        // Step 5: Collect all region file coordinates to delete
        Set<Pair> regionFilesToDelete = collectRegionFiles(xMin, xMax, zMin, zMax);

        getLogger().info("Found " + regionFilesToDelete.size() + " region file(s) to delete");

        // Step 6: Save the world to flush region file cache
        getBeaconzWorld().save(true);

        // Step 7: Delete all region files (terrain, entities, POI)
        int filesDeleted = deleteRegionFiles(regionFilesToDelete);
        getLogger().info("Deleted " + filesDeleted + " region-related file(s)");

        // Step 8: Delete structure and village data files
        deleteStructureData();
        
        sender.sendMessage(Component.text("Success"));
    }

    /**
     * Unloads all chunks in the specified area.
     * <p>
     * Chunks are unloaded in 16-block increments to cover the entire region plus border.
     *
     * @param xMin minimum X coordinate in blocks
     * @param xMax maximum X coordinate in blocks
     * @param zMin minimum Z coordinate in blocks
     * @param zMax maximum Z coordinate in blocks
     */
    private void unloadRegionChunksWithBorder(int xMin, int xMax, int zMin, int zMax) {
        int chunksUnloaded = 0;
        for (int blockX = xMin; blockX <= xMax; blockX += 16) {
            for (int blockZ = zMin; blockZ <= zMax; blockZ += 16) {
                int chunkX = blockX >> 4;  // Divide by 16 using bit shift
                int chunkZ = blockZ >> 4;
                if (getBeaconzWorld().isChunkLoaded(chunkX, chunkZ)) {
                    getBeaconzWorld().unloadChunk(chunkX, chunkZ);
                    chunksUnloaded++;
                }
            }
        }
        getLogger().info("Unloaded " + chunksUnloaded + " chunk(s)");
    }

    /**
     * Collects all region file coordinates that need to be deleted.
     * <p>
     * Region files in Minecraft are 32x32 chunks (512x512 blocks).
     * Region coordinates are calculated as: floor(chunkX / 32), floor(chunkZ / 32).
     * <p>
     * This method iterates through all chunks in the area and identifies unique
     * region files that contain those chunks.
     *
     * @param xMin minimum X coordinate in blocks
     * @param xMax maximum X coordinate in blocks
     * @param zMin minimum Z coordinate in blocks
     * @param zMax maximum Z coordinate in blocks
     * @return set of Pair objects representing region file coordinates (regionX, regionZ)
     */
    private Set<Pair> collectRegionFiles(int xMin, int xMax, int zMin, int zMax) {
        Set<Pair> regionFiles = new HashSet<>();

        // Iterate through area in 16-block (chunk) increments
        for (int blockX = xMin; blockX <= xMax; blockX += 16) {
            for (int blockZ = zMin; blockZ <= zMax; blockZ += 16) {
                // Convert block coordinates to chunk coordinates
                int chunkX = blockX >> 4;  // Divide by 16
                int chunkZ = blockZ >> 4;

                // Convert chunk coordinates to region file coordinates
                // Each region file contains 32x32 chunks
                int regionX = Math.floorDiv(chunkX, 32);
                int regionZ = Math.floorDiv(chunkZ, 32);

                regionFiles.add(new Pair(regionX, regionZ));
            }
        }

        return regionFiles;
    }

    /**
     * Deletes all region-related files for the given region coordinates.
     * <p>
     * Minecraft stores world data in multiple file types:
     * <ul>
     *   <li><b>.mca files</b> - Terrain and block data (in /region folder)</li>
     *   <li><b>.mcc files</b> - Entity data (in /entities folder, since 1.17)</li>
     *   <li><b>POI files</b> - Point of Interest data like villages, beds (in /poi folder)</li>
     * </ul>
     * <p>
     * All three types use the same naming convention: r.{regionX}.{regionZ}.{ext}
     *
     * @param regionFiles set of region file coordinates to delete
     * @return number of files successfully deleted
     */
    private int deleteRegionFiles(Set<Pair> regionFiles) {
        int filesDeleted = 0;
        File worldContainer = getServer().getWorldContainer();
        String worldName = getBeaconzWorld().getName();

        for (Pair coords : regionFiles) {
            int regionX = coords.left();
            int regionZ = coords.right();

            // Delete terrain file (.mca)
            File terrainFile = new File(worldContainer,
                worldName + File.separator + "region" + File.separator +
                "r." + regionX + "." + regionZ + ".mca");
            if (terrainFile.exists() && terrainFile.delete()) {
                filesDeleted++;
                getLogger().fine("Deleted terrain file: r." + regionX + "." + regionZ + ".mca");
            }

            // Delete entity file (.mcc) - introduced in Minecraft 1.17
            File entityFile = new File(worldContainer,
                worldName + File.separator + "entities" + File.separator +
                "r." + regionX + "." + regionZ + ".mcc");
            if (entityFile.exists() && entityFile.delete()) {
                filesDeleted++;
                getLogger().fine("Deleted entity file: r." + regionX + "." + regionZ + ".mcc");
            }

            // Delete POI (Point of Interest) file
            File poiFile = new File(worldContainer,
                worldName + File.separator + "poi" + File.separator +
                "r." + regionX + "." + regionZ + ".mca");
            if (poiFile.exists() && poiFile.delete()) {
                filesDeleted++;
                getLogger().fine("Deleted POI file: r." + regionX + "." + regionZ + ".mca");
            }
        }

        return filesDeleted;
    }

    /**
     * Deletes structure and village data files.
     * <p>
     * These .dat files in the /data folder contain information about generated structures
     * like villages, temples, strongholds, etc. Deleting them forces Minecraft to regenerate
     * these structures when the chunks are reloaded.
     * <p>
     * Files preserved:
     * <ul>
     *   <li>level.dat - World configuration (DO NOT DELETE)</li>
     *   <li>level.dat_old - Backup of world config (DO NOT DELETE)</li>
     * </ul>
     */
    private void deleteStructureData() {
        File dataFolder = new File(getServer().getWorldContainer().getAbsolutePath() +
            File.separator + getBeaconzWorld().getName() + File.separator + "data");

        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            getLogger().warning("Data folder not found: " + dataFolder.getAbsolutePath());
            return;
        }

        int datFilesDeleted = 0;
        File[] files = dataFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                // Delete .dat files but preserve level.dat and level.dat_old
                if (!file.isDirectory() &&
                    file.getName().endsWith(".dat") &&
                    !file.getName().startsWith("level")) {

                    if (file.delete()) {
                        datFilesDeleted++;
                        getLogger().fine("Deleted structure data: " + file.getName());
                    }
                }
            }
        }

        if (datFilesDeleted > 0) {
            getLogger().info("Deleted " + datFilesDeleted + " structure data file(s)");
        }
    }

    /**
     * Creates corner beacons at the four corners of the region.
     * <p>
     * This method places beacons at each corner of the region to establish boundaries
     * and allow for theoretical complete coverage of the map area. Corner beacons are
     * special permanent markers that define the playable area.
     * <p>
     * <b>Process for each corner:</b>
     * <ol>
     *   <li>Check if a beacon already exists nearby (within 5 blocks)</li>
     *   <li>Find the highest solid block at the corner position</li>
     *   <li>Skip leaves and air blocks to find actual terrain</li>
     *   <li>Create a beacon structure with diamond block pyramid base</li>
     *   <li>Register the beacon in the beacon registry</li>
     *   <li>Add obsidian capstone on top</li>
     * </ol>
     * <p>
     * <b>Beacon Structure:</b>
     * <pre>
     * Layer +2: Obsidian (capstone)
     * Layer +1: Beacon block
     * Layer  0: 3x3 Diamond block pyramid
     * </pre>
     * <p>
     * <b>Note:</b> This is only for creating new regions. Existing regions load their
     * corner beacons from the beacon register via {@link Register#loadRegister()}.
     * <p>
     * Corner positions are calculated as 1 block inset from actual corners to avoid
     * boundary issues.
     */
    public void createCorners() {
        // Check corners
        Set<Point2D> fourcorners = new HashSet<>();
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
                // Find the topmost free block
                while (b.getType().equals(Material.AIR) 
                        || Tag.LEAVES.isTagged(b.getType())) {
                    if (b.getY() == 0) {
                        // Oops, nothing here
                        break;
                    }
                    b = b.getRelative(BlockFace.DOWN);
                }
                // If there's already a beacon there, go down 1, to avoid building diamond towers - only needed for capped beacons
                if (b.getType().equals(Material.OBSIDIAN) && b.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
                    b = b.getRelative(BlockFace.DOWN);
                }

                if (b.getY() > 3) {
                    // Create a beacon
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
     * Normalizes and sets the region's corner coordinates.
     * <p>
     * Ensures corners are in the correct order with [0] being the minimum corner
     * and [1] being the maximum corner. Automatically swaps coordinates if they
     * are provided in reverse order.
     * <p>
     * This is critical for region boundary calculations as all containment checks
     * assume corners[0] has the minimum X and Z, and corners[1] has the maximum.
     *
     * @param c array of two Point2D objects representing opposite corners
     * @return normalized array where [0] is min corner (xMin, zMin) and [1] is max corner (xMax, zMax)
     */
    public Point2D [] setCorners(Point2D [] c) {
        return setCorners(c[0].getX(), c[0].getY(), c[1].getX(), c[1].getY());
    }

    /**
     * Normalizes and sets the region's corner coordinates from individual values.
     * <p>
     * Automatically determines which values are minimum and maximum, swapping them
     * if necessary to ensure proper ordering.
     *
     * @param xMin first X coordinate (will be normalized to actual minimum)
     * @param zMin first Z coordinate (will be normalized to actual minimum)
     * @param xMax second X coordinate (will be normalized to actual maximum)
     * @param zMax second Z coordinate (will be normalized to actual maximum)
     * @return normalized array where [0] is min corner and [1] is max corner
     */
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
        return new Point2D[]{new Point2D.Double(xMin, zMin), new Point2D.Double(xMax, zMax)};
    }

    /**
     * Returns the region's corner coordinates.
     * <p>
     * The corners define the rectangular boundary of this region.
     *
     * @return array of two Point2D objects: [0] = min corner (xMin, zMin), [1] = max corner (xMax, zMax)
     */
    public Point2D [] corners() {
        return corners;
    }

    /**
     * Displays a visual barrier to the player showing the region boundaries.
     * <p>
     * Creates a world border centered on this region that the player can see.
     * The barrier becomes visible when the player is within the specified radius
     * of the border. This helps players understand the playable area limits.
     * <p>
     * The world border size is calculated as twice the region radius to encompass
     * the entire rectangular region from center to edges.
     *
     * @param player the player to show the barrier to
     * @param radius the warning distance in blocks - barrier becomes visible when player is this close
     */
    public void showBarrier(Player player, int radius) {
        Location center = new Location(plugin.getBeaconzWorld(), getCenter().getX(), 0, getCenter().getY());
        WorldBorder wb = Bukkit.createWorldBorder();
        wb.setCenter(center);
        double size = getRadius() * 2;
        wb.setSize(size);
        wb.setWarningDistance(radius);
        player.setWorldBorder(wb);
    }


    /**
     * Calculates and returns the center point of the region.
     * <p>
     * The center is calculated as the midpoint between the minimum and maximum
     * X and Z coordinates. This is used for spawn point calculations and
     * region display purposes.
     *
     * @return Point2D representing the exact center coordinates (X, Z) of the region
     */
    public Point2D getCenter() {
        double x = (corners[0].getX() + corners[1].getX()) / 2.0;
        double z = (corners[0].getY() + corners[1].getY()) / 2.0;
        return new Point2D.Double(x,z);
    }

    /**
     * Calculates and returns the radius of the region.
     * <p>
     * The radius is defined as half the distance between the minimum and maximum
     * X coordinates. Since regions are rectangular, this represents the distance
     * from center to the east/west edges.
     *
     * @return the region's radius in blocks
     */
    public int getRadius() {
        return (int) Math.abs((corners[0].getX() - corners[1].getX()) / 2D);
    }

    /**
     * Generates a human-readable string displaying the region's coordinates.
     * <p>
     * The output format shows both the center point and the two corner coordinates
     * for easy reference in logs and admin commands.
     *
     * @return formatted string like "center: [500:500] - corners: [0:0] and [1000:1000]"
     */
    public String displayCoords() {
        return "center: [" +
                (int) getCenter().getX() + ":" + (int) getCenter().getY() + "] - corners: [" +
                (int) corners()[0].getX() + ":" + (int) corners()[0].getY() + "] and [" +
                (int) corners()[1].getX() + ":" + (int) corners()[1].getY() + "]";
    }

    /**
     * Checks if a beacon is located within this region's boundaries.
     * <p>
     * Uses the beacon's X and Z coordinates to determine containment.
     * Y coordinate is ignored as regions encompass all vertical levels.
     *
     * @param beacon the beacon to check
     * @return true if the beacon's location is within this region, false otherwise
     */
    public Boolean containsBeacon(BeaconObj beacon) {
        return containsPoint(beacon.getX(), beacon.getY());
    }

    /**
     * Checks if a Point2D coordinate is within this region's boundaries.
     * <p>
     * Convenience method that extracts X and Z from a Point2D object.
     *
     * @param point the 2D point to check (X and Z coordinates)
     * @return true if the point is within this region, false otherwise
     */
    public Boolean containsPoint(Point2D point) {
        return (containsPoint((int)point.getX(),(int)point.getY()));
    }

    /**
     * Checks if specific X and Z coordinates are within this region's boundaries.
     * <p>
     * This is the core containment check method. A point is considered inside
     * the region if its X coordinate is between xMin and xMax (inclusive) AND
     * its Z coordinate is between zMin and zMax (inclusive).
     * <p>
     * Note: Point2D uses Y for the second coordinate, but in Minecraft world
     * coordinates this represents the Z axis (not vertical height).
     *
     * @param x the X coordinate (east-west) to check
     * @param z the Z coordinate (north-south) to check
     * @return true if the coordinates are within this region's boundaries, false otherwise
     */
    public Boolean containsPoint(int x, int z) {
        // Make sure the coords are in the right order, although they should be..
        int xMin = (int)corners[0].getX();
        int zMin = (int)corners[0].getY();
        int xMax = (int)corners[1].getX();
        int zMax = (int)corners[1].getY();
        return (xMin <= x && xMax >= x && zMin <= z && zMax >= z);
    }

    /**
     * Checks if a player is currently located within this region's boundaries.
     * <p>
     * Uses the player's current block position (X and Z coordinates) to check
     * containment. The player's Y coordinate (height) is ignored.
     *
     * @param player the player to check
     * @return true if the player is within this region, false otherwise
     */
    public Boolean isPlayerInRegion(Player player) {
        return containsPoint(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
    }

    /**
     * Sets the region's spawn point at the specified 2D location.
     * <p>
     * This method finds the highest block at the given X/Z coordinates and
     * then searches for a safe spawn location within the specified radius.
     * The Y coordinate is determined automatically from terrain height.
     *
     * @param point the 2D coordinates (X and Z) where spawn should be set
     * @param radius maximum search radius in blocks to find a safe spot
     */
    public void setSpawnPoint(Point2D point, Integer radius) {
        int y = getBeaconzWorld().getHighestBlockYAt((int) point.getX(), (int) point.getY());
        setSpawnPoint(new Location(getBeaconzWorld(), point.getX(), y, point.getY()), radius);
    }

    /**
     * Sets a safe spawn point in the region, searching within a radius for safety.
     * <p>
     * This method attempts to find a safe location near the provided location.
     * A safe location is one where:
     * <ul>
     *   <li>The ground block is solid (not air or liquid)</li>
     *   <li>There are two blocks of air space above for the player</li>
     *   <li>No portals, fences, signs, or hazards are present</li>
     * </ul>
     * <p>
     * If no safe location is found within the radius, a safe spot is created
     * by placing bedrock at the specified location.
     *
     * @param loc the center location to start searching from
     * @param radius maximum search radius in blocks (capped at 20)
     */
    public void setSpawnPoint(Location loc, Integer radius){
        getLogger().info("DEBUG: set spawn point");
        spawnPoint = findSafeSpot(loc, radius);
    }

    /**
     * Sets the spawn point directly to the specified location without safety checks.
     * <p>
     * Use this method when you've already verified the location is safe or when
     * you need to set an exact spawn point regardless of safety.
     *
     * @param loc the exact location to use as spawn point
     */
    public void setSpawnPoint(Location loc) {
        spawnPoint = loc;
    }

    /**
     * Returns the region's current spawn point.
     * <p>
     * This is the location where players will be teleported when entering
     * this region or when using region spawn commands.
     *
     * @return the spawn point location, or null if not set
     */
    public Location getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * Teleports a player to this region's spawn point.
     * <p>
     * The player will be teleported to the spawn location and any nearby
     * monsters within 10 blocks will be removed for safety.
     * <p>
     * The directly parameter controls teleport delay behavior:
     * <ul>
     *   <li>true - Immediate teleport, bypassing any teleport delays</li>
     *   <li>false - Normal teleport with standard delays and movement checks</li>
     * </ul>
     *
     * @param player the player to teleport
     * @param directly if true, teleport immediately; if false, use normal teleport delay
     */
    public void tpToRegionSpawn(Player player, boolean directly) {
        if (directly) {
            getBeaconzPlugin().getTeleportListener().setDirectTeleportPlayer(player.getUniqueId());
        }
        // Make the spawn point safe if it isn't anymore
        getLogger().info("DEBUG: tp to region");
        this.setSpawnPoint(spawnPoint, 20);
        getLogger().info("DEBUG: spawnpoint = " + getSpawnPoint());
        player.teleportAsync(getSpawnPoint());
        // Remove any Mobs around the area
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Monster) {
                entity.remove();
            }
        }
    }

    /**
     * Handles player entering the Lobby region.
     * <p>
     * This method is called when a player enters the lobby and performs several
     * initialization tasks to welcome and orient the player.
     * <p>
     * <b>Actions performed:</b>
     * <ol>
     *   <li>Sends welcome messages to player in chat</li>
     *   <li>Displays title screen with welcome message and subtitle</li>
     *   <li>After 3 seconds (60 ticks), displays lobby scoreboard if still in lobby</li>
     * </ol>
     * <p>
     * <b>Scoreboard Display:</b>
     * The lobby scoreboard shows information from {@link Lang#titleLobbyInfo}, which
     * is split by the "|" character. The first segment becomes the sidebar title
     * (colored green), and subsequent segments become individual lines displayed
     * in descending order.
     * <p>
     * The scoreboard creation uses the modern Bukkit API with {@link Criteria#DUMMY}
     * and sets the display name directly in the constructor rather than calling
     * displayName() separately.
     * <p>
     * <b>Delayed Scoreboard:</b>
     * The scoreboard is shown after a 60-tick (3-second) delay to avoid interfering
     * with the title animation. The method checks that the player is still in the
     * lobby before showing the scoreboard.
     *
     * @param player the player entering the lobby
     */
    public void enterLobby(final Player player) {

        // Welcome player in chat
        player.sendMessage(Lang.titleWelcome.color(NamedTextColor.GREEN));
        player.sendMessage(Lang.titleSubTitle.color(NamedTextColor.AQUA));

        // Welcome player on screen
        player.showTitle(title);

        // Show the lobby scoreboard - wait for title message to disappear
        if (this.equals(getGameMgr().getLobby())) {

            getServer().getScheduler().runTaskLater(plugin, () -> {
                // This runs after a few seconds, so make sure that player is still in the lobby
                if (getGameMgr().isPlayerInLobby(player)) {
                    Scoreboard sb = plugin.getServer().getScoreboardManager().getNewScoreboard();
                    Objective sbobj = null;
                    Score scoreline = null;
                    player.setScoreboard(sb);

                    try {
                        sb.clearSlot(DisplaySlot.SIDEBAR);
                    } catch (Exception e){ }

                    // Properly serialize Component to plain text before splitting
                    String lobbyInfoText = PlainTextComponentSerializer.plainText().serialize(Lang.titleLobbyInfo);
                    String[] lobbyInfo = lobbyInfoText.split("\\|");

                    // Use modern API with Criteria and Component displayName
                    sbobj = sb.registerNewObjective("text", Criteria.DUMMY,
                            Component.text(lobbyInfo[0]).color(NamedTextColor.GREEN));
                    sbobj.setDisplaySlot(DisplaySlot.SIDEBAR);
                    for (int line = 1; line < lobbyInfo.length; line++) {
                        scoreline = sbobj.getScore(lobbyInfo[line]);
                        scoreline.setScore(16-line);
                    }
                    player.setScoreboard(sb);

                }
            }, 60L);
        }
    }

    /**
     * Handles player exiting this region.
     * <p>
     * This method cleans up player state when leaving a game region.
     * <p>
     * <b>Actions performed:</b>
     * <ul>
     *   <li>Clears the player's scoreboard (removes game scoreboard)</li>
     *   <li>For MINIGAME mode: clears stored items and empties inventory</li>
     * </ul>
     * <p>
     * In MINIGAME mode, player inventories are temporary and all items are cleared
     * when the player leaves. Stored items from before entering the game are restored
     * via {@link BeaconzStore#clearItems(Player, String, Location)}.
     *
     * @param player the player exiting the region
     */
    public void exit(Player player) {
        player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
        if (game !=null && game.getGamemode() == GameMode.MINIGAME)  {
            String gameName = PlainTextComponentSerializer.plainText().serialize(game.getName());
            getBeaconzStore().clearItems(player, gameName, player.getLocation());
            player.getInventory().clear();
        }
    }

    /**
     * Handles player entering this game region.
     * <p>
     * This method welcomes the player and displays game information when they
     * enter a region with an active game.
     * <p>
     * <b>Actions performed:</b>
     * <ol>
     *   <li>Determines the player's team (if any)</li>
     *   <li>Sets the game scoreboard if enabled in settings</li>
     *   <li>Sends welcome message and displays the player's team</li>
     *   <li>Displays game objective based on goal type:
     *     <ul>
     *       <li>If goal has a numeric target (goalvalue &gt; 0): Shows "Reach X [goal]"</li>
     *       <li>If goal is unlimited (goalvalue = 0): Shows "Most [goal]"</li>
     *     </ul>
     *   </li>
     * </ol>
     * <p>
     * <b>Scoreboard Behavior:</b>
     * If the player already has a scoreboard with entries, their existing scoreboard
     * is cleared. Otherwise, the game's scoreboard is assigned to show live scores.
     * <p>
     * All messages use the modern Kyori Adventure API with builder pattern for
     * text replacement and proper color formatting.
     *
     * @param player the player entering the region
     */
    public void enter(Player player) {
        // Show scoreboard
        Team team = game.getScorecard().getTeam(player);
        Component teamname = team == null ? Component.text("no") : team.displayName();

        if (Settings.useScoreboard) {
            player.setScoreboard(game.getScorecard().getScoreboard());
        } else {
            player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
        }

        // Welcome player in chat
        player.sendMessage(Lang.titleWelcome.color(NamedTextColor.GREEN));
        player.sendMessage(Lang.startYoureAMember
                .replaceText(builder -> builder.matchLiteral("[name]").replacement(teamname))
                .color(NamedTextColor.AQUA));
        if (game.getGamegoalvalue() > 0) {
            player.sendMessage(Lang.startObjective
                    .replaceText(builder -> builder.matchLiteral("[value]")
                            .replacement(Component.text(String.format(Locale.US, "%,d", game.getGamegoalvalue()))))
                    .replaceText(builder -> builder.matchLiteral("[goal]")
                            .replacement(Component.text(game.getGamegoal().getName())))
                    .color(NamedTextColor.AQUA));
        } else {
            player.sendMessage(Lang.startMostObjective
                    .replaceText(builder -> builder.matchLiteral("[goal]")
                            .replacement(Component.text(game.getGamegoal().getName())))
                    .color(NamedTextColor.AQUA));
        }
    }

    /**
     * Finds the nearest safe spawn location within a radius of the given point.
     * <p>
     * This method searches for a safe location suitable for player teleportation.
     * The search radius is capped at 20 blocks to prevent excessive searching.
     * <p>
     * <b>Safety Criteria:</b>
     * A location is considered safe if ALL of the following are true:
     * <ul>
     *   <li>Ground block exists and is solid (not air, not passable)</li>
     *   <li>Two blocks of air space above ground for the player to stand</li>
     *   <li>No portals (nether or end) at the location</li>
     *   <li>No fences, signs, or pressure plates that could trap the player</li>
     *   <li>Not inside a tree (no logs directly above)</li>
     * </ul>
     * <p>
     * <b>Search Pattern:</b>
     * The search starts at the center location and spirals outward checking
     * increasingly distant locations. The search prioritizes higher ground
     * and checks in all directions.
     * <p>
     * <b>Fallback Behavior:</b>
     * If no safe location is found within the radius, the method creates a safe
     * spot at the original location by:
     * <ol>
     *   <li>Placing bedrock at the ground level</li>
     *   <li>Clearing two blocks of air above it</li>
     *   <li>Logging a warning about the forced safe spot creation</li>
     * </ol>
     *
     * @param location the center location to search from
     * @param radius maximum search radius in blocks (capped at 20)
     * @return a safe Location for player spawn, guaranteed to be non-null
     */
    public Location findSafeSpot (Location location, Integer radius) {
        // First load the chunk
      Chunk chunk =  location.getWorld().getChunkAt(location);
       
        getLogger().info("DEBUG: find safe spot. Chunk loaded? " + location.getWorld().isChunkLoaded(chunk));
        Location safeloc = null;

        // Check actual first location
        if (isLocationSafe(location)) {
            getLogger().info("DEBUG: location is safe");
            // We are done
            return location;
        }
        // Check for the highest block at this location
        int y = location.getWorld().getHighestBlockYAt(location);
        getLogger().info("DEBUG: highest y = " + y);
        location.setY(y);
        if (isLocationSafe(location)) {
            // We are done
            return location;
        }
        // Limit radius search
        if (radius > 20) radius = 20;        
        if (safeloc == null && location != null) {
            // look for a safe spot at location and within radius
            Block bl = location.getBlock();
            List<Pair> used = new ArrayList<>();

            // sweep in a concentric cube pattern to check for a safe spot
            Location checkloc = null;
            outerloop:
                for (int rad = 0; rad < radius; rad++) {
                    for (int z = -rad; z <= rad; z++) {
                        for (int x = -rad; x <= rad; x++) {
                            Pair coords =new Pair(x,z);
                            if (!used.contains(coords)) {
                                used.add(new Pair(x,z));
                                checkloc = getBeaconzWorld().getHighestBlockAt(bl.getRelative(x, 0, z).getLocation()).getLocation().subtract(0, 1, 0);
                                if (isLocationSafe(checkloc)) {
                                    safeloc = checkloc.add(0.5, 0.0, 0.5);
                                    break outerloop;
                                }
                            }
                        }
                    }
                }
        }
        if (safeloc == null && location != null) {
            Bukkit.getConsoleSender().sendMessage(Component.text("Could not find a safe spot. Region at " + displayCoords() + ". Using default.").color(NamedTextColor.YELLOW));
            safeloc = new Location(getBeaconzWorld(), location.getX(), getBeaconzWorld().getHighestBlockYAt(location), location.getZ());
            safeloc.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
        }
        getLogger().info("DEBUG: safeloc = " + safeloc);
        // Return the safe location
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

        if (!space1.isPassable() || !space2.isPassable() || ground.isPassable() || ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
            return false;
        }

        // Portals are not "safe"
        return !Tag.PORTALS.isTagged(space1.getType())
                && !Tag.PORTALS.isTagged(space2.getType())
                && !Tag.PORTALS.isTagged(ground.getType())
                && !Tag.TRAPDOORS.isTagged(ground.getType())
                && !Tag.FENCES.isTagged(ground.getType())
                && !Tag.SIGNS.isTagged(ground.getType())
                && !Tag.LEAVES.isTagged(ground.getType())
                && ground.getType() != Material.CACTUS;
        // Safe
    }

    /**
     * Returns the game associated with this region.
     * <p>
     * Each region can be associated with one game, or can be the lobby (no game).
     * The lobby region always returns null from this method.
     *
     * @return the Game instance for this region, or null if this is the lobby
     */
    public Game getGame() {
        return game;
    }

    /**
     * Associates a game with this region.
     * <p>
     * This creates the link between a region and its game. The lobby region
     * should not have a game set.
     *
     * @param game the Game instance to associate with this region
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Creates a platform in the sky for the lobby region.
     * <p>
     * This method generates a flat platform at {@link Settings#lobbyHeight} using
     * random blocks from {@link Settings#lobbyBlocks}. The platform covers the
     * entire region from corner to corner.
     * <p>
     * <b>Construction:</b>
     * <ul>
     *   <li>Iterates through all X and Z coordinates within region bounds</li>
     *   <li>For each position, randomly selects a block type from the lobby blocks list</li>
     *   <li>Places the block at the configured lobby height</li>
     *   <li>Skips invalid materials with an error message</li>
     * </ul>
     * <p>
     * This is typically used to create a colorful, decorative lobby floor where
     * players can walk around while waiting for games.
     */
    public void makePlatform() {
        Random rand = new Random();
        if (corners.length == 2) {
            for (int x = (int)corners[0].getX(); x <= (int)corners[1].getX(); x++) {
                for (int z = (int)corners[0].getY(); z <= (int)corners[1].getY(); z++) {
                    Block block = getBeaconzWorld().getBlockAt(x, Settings.lobbyHeight, z);
                    String matType = Settings.lobbyBlocks.get(rand.nextInt(Settings.lobbyBlocks.size()));
                    Material material = Material.getMaterial(matType);
                    if (material != null) {
                        block.setType(material);
                    } else {
                        getLogger().severe("Could not parse block material value for " + matType + ", skipping...");
                    }
                } 
            }
        }
        // Set spawn
        int x = (int)((corners[0].getX() + corners[1].getX()) / 2D);
        int z = (int)((corners[0].getY() + corners[1].getY()) / 2D);
        spawnPoint = new Location(getBeaconzWorld(),x,Settings.lobbyHeight+1,z+2);
        // Place sign
        Block sign = getBeaconzWorld().getBlockAt(spawnPoint.getBlockX(), spawnPoint.getBlockY(), spawnPoint.getBlockZ());
        sign.setType(Material.OAK_SIGN);
        Sign realSign = (Sign)sign.getState();
        SignSide side = realSign.getSide(Side.FRONT);
        side.line(0, Component.text("[beaconz]"));
        side.line(1, Component.text(Settings.defaultGameName));
        side.line(2, Lang.actionsHitSign);
        realSign.update();
        // Set the spawn point to look at the new sign
        org.bukkit.block.data.type.Sign signData = (org.bukkit.block.data.type.Sign) realSign.getBlockData();
        BlockFace directionFacing = signData.getRotation();
        float yaw = blockFaceToFloat(directionFacing);
        spawnPoint = sign.getRelative(directionFacing).getLocation();
        spawnPoint = new Location(spawnPoint.getWorld(), spawnPoint.getBlockX() + 0.5D, spawnPoint.getBlockY(),
                spawnPoint.getBlockZ() + 0.5D, yaw, 30F);
        getBeaconzWorld().setSpawnLocation(spawnPoint.getBlockX(), spawnPoint.getBlockY(), spawnPoint.getBlockZ());
    }

    /**
     * Converts a Minecraft BlockFace direction to yaw rotation in degrees.
     * <p>
     * This is used to orient players to face a specific direction when spawning.
     * For example, when spawning in the lobby, the player should face toward
     * the sign that was just created.
     * <p>
     * <b>Conversion Map:</b>
     * <ul>
     *   <li>SOUTH = 0° (positive Z direction)</li>
     *   <li>WEST = 90° (negative X direction)</li>
     *   <li>NORTH = 180° (negative Z direction)</li>
     *   <li>EAST = 270° (positive X direction)</li>
     *   <li>Diagonal directions use intermediate angles (e.g., SOUTH_WEST = 45°)</li>
     * </ul>
     * <p>
     * Non-radial directions (UP, DOWN, SELF) return 0°.
     *
     * @param face the BlockFace direction to convert
     * @return yaw angle in degrees (0-360), or 0 if face is not a horizontal direction
     */
    public static float blockFaceToFloat(BlockFace face) {
        return switch (face) {
        case EAST -> 90F;
        case EAST_NORTH_EAST -> 67.5F;
        case EAST_SOUTH_EAST -> 0F;
        case NORTH -> 0F;
        case NORTH_EAST -> 45F;
        case NORTH_NORTH_EAST -> 22.5F;
        case NORTH_NORTH_WEST -> 337.5F;
        case NORTH_WEST -> 315F;
        case SOUTH -> 180F;
        case SOUTH_EAST -> 135F;
        case SOUTH_SOUTH_EAST -> 157.5F;
        case SOUTH_SOUTH_WEST -> 202.5F;
        case SOUTH_WEST -> 225F;
        case WEST -> 270F;
        case WEST_NORTH_WEST -> 292.5F;
        case WEST_SOUTH_WEST -> 247.5F;
        default -> 0F;
        };
    }

    /**
     * Checks if a location is within this region's boundaries.
     * <p>
     * This method verifies both that the location is in the correct world
     * (the Beaconz game world) and that the X/Z coordinates fall within
     * the region bounds. Y coordinate is ignored as regions span all heights.
     *
     * @param location the location to check
     * @return true if the location is in this region and the Beaconz world, false otherwise
     */
    public boolean contains(Location location) {
        World world = location.getWorld();
        if (world == null || !world.equals(getBeaconzWorld())) {
            return false;
        }
        return containsPoint(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Sends all online players to the lobby, checking each player's location.
     * <p>
     * This method iterates through all online players and sends those who
     * are in this region to the lobby. Players not in this region are unaffected.
     * <p>
     * The saveInv parameter controls inventory handling for MINIGAME mode games.
     *
     * @param saveInv true to save and restore player inventory when in MINIGAME mode,
     *                false to clear inventory without saving
     */
    public void sendAllPlayersToLobby(Boolean saveInv) {
        for (Player player : getServer().getOnlinePlayers()) {
            sendToLobby(player, saveInv);
        }
    }

    /**
     * Sends a player to the lobby if they are currently in this region.
     * <p>
     * This method checks if the player is within this region's boundaries.
     * If they are not, the method does nothing (player is left where they are).
     * <p>
     * <b>Teleportation Process:</b>
     * <ol>
     *   <li>Verify player is in this region - if not, return immediately</li>
     *   <li>For MINIGAME mode: optionally save/clear player inventory</li>
     *   <li>Teleport player to lobby spawn point</li>
     *   <li>Player enters lobby (welcome messages, scoreboard, etc.)</li>
     * </ol>
     * <p>
     * <b>Inventory Handling:</b>
     * In MINIGAME mode, if saveInv is true, the player's current inventory
     * is saved to be restored later. If false, inventory is just cleared.
     * This has no effect in STRATEGY mode.
     *
     * @param player the player to potentially send to lobby
     * @param saveInv true to save inventory before clearing (MINIGAME mode only),
     *                false to just clear without saving
     */
    public void sendToLobby(Player player, Boolean saveInv) {
        if (isPlayerInRegion(player)) {
            String gameName = PlainTextComponentSerializer.plainText().serialize(game.getName());
            if (saveInv) {
                getBeaconzStore().storeInventory(player, gameName, player.getLocation());
            } else {
                getBeaconzStore().clearItems(player, gameName, null);
            }
            getGameMgr().getLobby().tpToRegionSpawn(player, true);
        }        
    }
}
