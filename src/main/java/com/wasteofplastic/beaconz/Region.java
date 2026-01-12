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
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
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

import com.wasteofplastic.beaconz.Params.GameMode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;

/**
 * Region instantiates the various regions in the world
 * A region belongs to a game - except for the lobby, which is a region with no game
 * A region is defined by two sets of X:Z coordinates and encompasses all Ys
 * A region can be created and recreated freely; region.populate is not in the constructor and must be called explicitly
 */
public class Region extends BeaconzPluginDependent {

    private final Beaconz plugin;
    private final Point2D [] corners;
    private Location spawnPoint;
    //private long progress;
    private Game game = null;
    
    // Define the Title timing (Fade-in, Stay, Fade-out)
    Title.Times times = Title.Times.times(
            Duration.ofMillis(500),  // 10 ticks
            Duration.ofMillis(3000), // 60 ticks
            Duration.ofMillis(500)   // 10 ticks
    );

    // Title object to show
    Title title = Title.title(
            Lang.titleWelcome.color(Lang.titleWelcomeColor), 
            Lang.titleSubTitle.color(Lang.titleSubTitleColor), 
            times
    );


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
            int rad = getRadius();
            setSpawnPoint(getCenter(), rad);

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
     * Creates the corner-most beacons so that the map can be theoretically be covered entirely (almost)
     * Also regenerates the chunk
     * This is only for creating new regions. Existing regions have their corner beacons loaded with Register.loadRegister().
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
        return new Point2D[]{new Point2D.Double(xMin, zMin), new Point2D.Double(xMax, zMax)};
    }

    /**
     * Returns the region's corners as a Point2D []
     */
    public Point2D [] corners() {
        return corners;
    }

    /**
     * Displays the barrier blocks if player is within a distance of the barrier
     * @param player player to show to
     * @param radius size of barrier
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
     * Returns the region's center point
     */
    public Point2D getCenter() {
        double x = (corners[0].getX() + corners[1].getX()) / 2.0;
        double z = (corners[0].getY() + corners[1].getY()) / 2.0;
        return new Point2D.Double(x,z);
    }

    /**
     * Returns the region's radius
     */
    public int getRadius() {
        return (int) Math.abs((corners[0].getX() - corners[1].getX()) / 2D);
    }

    /**
     * Returns the region's coordinates in a pretty display string
     */
    public String displayCoords() {
        return "center: [" +
                (int) getCenter().getX() + ":" + (int) getCenter().getY() + "] - corners: [" +
                (int) corners()[0].getX() + ":" + (int) corners()[0].getY() + "] and [" +
                (int) corners()[1].getX() + ":" + (int) corners()[1].getY() + "]";
    }

    /**
     * Determines whether a beacon or point lie inside the region
     */
    public Boolean containsBeacon(BeaconObj beacon) {
        return containsPoint(beacon.getX(), beacon.getY());
    }
    public Boolean containsPoint(Point2D point) {
        return (containsPoint((int)point.getX(),(int)point.getY()));
    }
    public Boolean containsPoint(int x, int z) {
        // Make sure the coords are in the right order, although they should be..
        int xMin = (int)corners[0].getX();
        int zMin = (int)corners[0].getY();
        int xMax = (int)corners[1].getX();
        int zMax = (int)corners[1].getY();
        //getLogger().info("Contains: x: " + x + " z: " + z + " corner1: " + corners[0].getX() + ":" + corners[0].getY() + " corner2: " + corners[1].getX() + ":" + corners[1].getY());
        return (xMin <= x && xMax >= x && zMin <= z && zMax >= z);
        //return contains;
    }

    /**
     * Determines whether a player is inside the region
     */
    public Boolean isPlayerInRegion(Player player) {
        return containsPoint(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
    }

    /**
     * Sets the region's spawn point
     */
    public void setSpawnPoint(Point2D point, Integer radius) {
        int y = getBeaconzWorld().getHighestBlockYAt((int) point.getX(), (int) point.getY());
        setSpawnPoint(new Location(getBeaconzWorld(), point.getX(), y, point.getY()), radius);
    }

    /**
     * Sets a safe spawn point in a region
     * @param loc
     * @param radius
     */
    public void setSpawnPoint(Location loc, Integer radius){
        spawnPoint = findSafeSpot(loc, radius);
    }

    /**
     * Sets the spawn point without finding a safe spot
     * @param loc
     */
    public void setSpawnPoint(Location loc) {
        spawnPoint = loc;
    }

    /**
     * Returns the region's spawn point
     */
    public Location getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * Teleports a player to the region's spawn point
     * @param player
     * @param directly - if true player will go to spawn directly and not experience the delay teleport
     */
    public void tpToRegionSpawn(Player player, boolean directly) {
        if (directly) {
            getBeaconzPlugin().getTeleportListener().setDirectTeleportPlayer(player.getUniqueId());
        }
        player.teleport(spawnPoint);
        // Remove any Mobs around the area
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Monster) {
                entity.remove();
            }
        }
    }

    /**
     * Handles player entering the Lobby
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
     * Handles player Exit event
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
     * Handles player Enter event
     */
    public void enter(Player player) {
        // Show scoreboard
        Component teamname = Component.text("no");
        Team team = game.getScorecard().getTeam(player);
        if (team != null) {
            teamname = team.displayName();
        }
        if (Settings.useScoreboard) {
            player.setScoreboard(game.getScorecard().getScoreboard());
        } else {
            player.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
        }

        // Welcome player in chat
        player.sendMessage(Lang.titleWelcome.color(NamedTextColor.GREEN));
        player.sendMessage(Lang.startYoureAMember
                .replaceText(builder -> builder.matchLiteral("[name]").replacement(Component.text("")))
                .append(teamname).color(NamedTextColor.AQUA));
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
     * Find the nearest safe spot to a given point, within a radius
     * The radius is arbitrarily limited to 20 blocks
     * If no safe location within radius is found, this creates a safe spot by converting the block at location to bedrock
     */
    public Location findSafeSpot (Location location, Integer radius) {

        Location safeloc = null;

        // First tell WorldListener to ignore these chunk loads
        getBeaconzPlugin().ignoreChunkLoad = true;

        // Check actual first location
        if (isLocationSafe(location)) {
            safeloc = location;
        }
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

        // If ground is AIR, then this is either not good, or they are on slab,
        // stair, etc.
        if (ground.getType() == Material.AIR) {
            Bukkit.getLogger().info("DEBUG: air");
            return false;
        }
        // Liquid is unsafe
        if (ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
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
     * @return the game or null if this is the lobby
     */
    public Game getGame() {
        return game;
    }

    /**
     * @param game the game to set
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Makes a platform in the sky
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
     * Converts block face direction to radial degrees. Returns 0 if block face
     * is not radial.
     * 
     * @param face
     * @return degrees
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
     * Check if location is in this region
     * @param location
     * @return true or false
     */
    public boolean contains(Location location) {
        World world = location.getWorld();
        if (world == null || !world.equals(getBeaconzWorld())) {
            return false;
        }
        return containsPoint(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Sends all players in this region to the lobby, optionally saving inventory.
     * @param saveInv true to save player inventory before teleporting
     */
    public void sendAllPlayersToLobby(Boolean saveInv) {
        for (Player player : getServer().getOnlinePlayers()) {
            sendToLobby(player, saveInv);
        }
    }

    /**
     * Sends player to lobby, if he is in this region
     * @param player
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
