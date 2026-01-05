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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.generator.BeaconzChunkGen;

/**
 * Manages the lifecycle and spatial organization of Beaconz games and regions.
 *
 * <p>This class serves as the central controller for game creation, deletion, persistence,
 * and region management. It maintains a coordinate-based mapping of game regions and ensures
 * that regions don't overlap in the game world.</p>
 *
 * <p><b>Key Concepts:</b></p>
 * <ul>
 *   <li><b>Region:</b> A bounded rectangular area in the world (aligned to chunk boundaries)
 *       where a game takes place or where the lobby exists</li>
 *   <li><b>Game:</b> An instance of a Beaconz match with teams, objectives, and scoring</li>
 *   <li><b>Lobby:</b> A special region where players wait between games</li>
 * </ul>
 *
 * <p>Regions are stored with their corner coordinates as keys, and games are stored
 * by their string names. The class ensures regions maintain a 512-block safety buffer
 * between each other to prevent interference during region regeneration.</p>
 *
 * @see Game
 * @see Region
 * @see Scorecard
 */
public class GameMgr extends BeaconzPluginDependent {

    private final Beaconz plugin;
    /** The lobby region where players gather between games */
    private Region lobby;
    /** Map of region corner coordinates to Region objects for spatial lookup */
    private final LinkedHashMap<Point2D[], Region> regions;
    /** Map of game names to active Game instances */
    private final LinkedHashMap<String, Game> games;

    // Default game configuration parameters
    private String gamemode;
    private Integer gamedistance;
    private Double gamedistribution;
    private Integer nbr_teams;
    private String gamegoal;
    private Integer gamegoalvalue;
    private Integer timer;
    private String scoretypes;

    /**
     * Constructs a new GameMgr and initializes the game system.
     *
     * <p>On construction, this manager:</p>
     * <ol>
     *   <li>Initializes empty game and region collections</li>
     *   <li>Loads default game parameters from configuration</li>
     *   <li>Attempts to load saved games from disk</li>
     *   <li>Creates a lobby region if one doesn't exist</li>
     * </ol>
     *
     * @param beaconzPlugin the main plugin instance
     */
    public GameMgr(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        this.plugin = beaconzPlugin;
        regions = new LinkedHashMap<>();
        games = new LinkedHashMap<>();
        setGameDefaultParms();
        loadAllGames();
        if (lobby == null) {
            createLobby();
        }
    }

    /**
     * Reloads all game configurations from disk without affecting ongoing games.
     *
     * <p>This method is called when the plugin is reloaded via command. It preserves
     * the state of active games but updates their configuration parameters and
     * regenerates scorecards with new settings.</p>
     */
    public void reload() {
        saveAllGames();
        setGameDefaultParms();
        loadAllGames();
    }

    /**
     * Persists all games and the lobby to disk.
     *
     * <p>Creates a backup of the existing games.yml file before saving.
     * The lobby region and spawn point are saved separately from game regions.
     * Each active game delegates to its own save method for detailed persistence.</p>
     *
     * @see Game#save()
     */
    public void saveAllGames() {
        File gamesFile = new File(getBeaconzPlugin().getDataFolder(),"games.yml");
        YamlConfiguration gamesYml = YamlConfiguration.loadConfiguration(gamesFile);

        // Backup the games file just in case of corruption
        if (gamesFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"games.old");
            gamesFile.renameTo(backup);
        }

        // Save the lobby region and spawn point
        if (lobby != null) {
            gamesYml.set("lobby.region", ptsToStrCoord(lobby.corners()));
            gamesYml.set("lobby.spawn", Beaconz.getStringLocation(lobby.getSpawnPoint()));
        }

        // Write lobby data to file
        try {
            gamesYml.save(gamesFile);
        } catch (IOException e) {
            getLogger().severe("Problem saving lobby in games file!");
            e.printStackTrace();
        }

        // Delegate to each game to save its own data
        for (Game game: games.values()) {
            game.save();
        }
    }

    /**
     * Converts a pair of Point2D coordinates into a colon-separated string.
     *
     * <p>Format: "x1:z1:x2:z2" where (x1,z1) and (x2,z2) are the region corners.</p>
     *
     * @param c array of two Point2D objects representing region corners
     * @return formatted string representation of coordinates
     */
    private String ptsToStrCoord(Point2D [] c) {
        // Note: Point2D.y is used for Minecraft's Z-axis coordinate
        return c[0].getX() + ":" + c[0].getY() + ":" + c[1].getX() + ":" + c[1].getY();
    }

    /**
     * Saves a specific game by name to disk.
     *
     * <p><b>Note:</b> Currently has a bug - uses string literal "name" instead of
     * the parameter value.</p>
     *
     * @param name the name of the game to save
     */
    public void saveGame(String name) {
        // Look up game by name (was incorrectly using literal "name")
        Game game = games.get(name);
        if (game != null) {
            game.save();
        }
    }

    /**
     * Clears current game state and reloads all games from disk.
     *
     * <p>This is a convenience method that clears all in-memory games and regions,
     * then delegates to {@link #loadGames(String)} with null to load everything.</p>
     */
    public void loadAllGames() {
        regions.clear();
        games.clear();
        loadGames(null);
    }

    /**
     * Loads game data from the games.yml file.
     *
     * <p>If gameName is null, loads all games and the lobby. Otherwise, loads only
     * the specified game. When loading an active game (one already in memory),
     * the game parameters are updated and the game is reloaded. When loading an
     * inactive game, a new Game object is created and registered.</p>
     *
     * <p>The lobby is only loaded when gameName is null (during full initialization).</p>
     *
     * @param gameName the specific game to load, or null to load all games
     */
    public void loadGames(String gameName) {
        File gamesFile = new File(getBeaconzPlugin().getDataFolder(),"games.yml");
        if (gamesFile.exists()) {
            YamlConfiguration gamesYml = new YamlConfiguration();
            try {
                gamesYml.load(gamesFile);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                getLogger().severe("Problem with games.yml formatting");
                e.printStackTrace();
            }

            ConfigurationSection csec = gamesYml.getConfigurationSection("lobby");
            // Load the lobby only during full initialization
            if (gameName == null) {
                if (csec != null) {
                    // Reconstruct lobby region from saved coordinates
                    Point2D [] corners = strCoordToPts(csec.getString("region"));
                    lobby = new Region(plugin, corners);
                    String spawn = csec.getString("spawn", "");
                    if (!spawn.isEmpty()) {
                        lobby.setSpawnPoint(Beaconz.getLocationString(spawn));
                    }
                    regions.put(corners, lobby);
                }
            }
            // Load game configurations
            csec = gamesYml.getConfigurationSection("game");
            if (csec != null) {
                for (String gname : csec.getKeys(false)) {
                    // Either load all games or just the specified one
                    if (gameName == null || gname.equals(gameName)) {
                        // Extract all game parameters from YAML
                        Point2D [] corners = strCoordToPts(csec.getString(gname + ".region"));
                        Region region = new Region(plugin, corners);
                        String gm   = csec.getString(gname + ".gamemode");
                        int gd   = csec.getInt(gname + ".gamedistance");
                        int nt  = csec.getInt(gname + ".nbrteams");
                        String gg   = csec.getString(gname + ".gamegoal");
                        int gv  = csec.getInt(gname + ".goalvalue");
                        Long st  = csec.getLong(gname + ".starttime");
                        Long ct  = csec.getLong(gname + ".createtime");
                        int gt  = csec.getInt(gname + ".countdowntimer");
                        String gs   = csec.getString(gname + ".scoretypes");
                        boolean isOver = csec.getBoolean(gname + ".gameOver");
                        double gdist = csec.getDouble(gname + ".gamedistribution");

                        Game game = games.get(gameName);
                        if (game != null && gameName != null) {
                            // Updating an active game - refresh its parameters and reload
                            game.setGameParms(gm, gd, nt, gg, gv,gt, st, ct, gs, gdist);
                            game.setOver(isOver);
                            region.setGame(game);
                            game.reload();
                        } else {
                            // Loading a saved game that isn't currently active
                            regions.put(corners, region);
                            game = new Game(plugin, gd, region, gname, gm, nt, gg, gv, gt, gs, gdist);
                            game.setOver(isOver);
                            games.put(gname, game);
                        }
                    }
                }
            }
        }
    }


    /**
     * Creates and initializes the lobby region at configured coordinates.
     *
     * <p>The lobby is created at the location specified in config.yml (Settings.lobbyx,
     * Settings.lobbyz) with the configured radius. If the desired area overlaps with
     * existing regions, the method attempts to find a smaller suitable area in 16-block
     * (chunk) decrements.</p>
     *
     * <p>If no suitable area can be found (minimum 16 blocks radius), a default 1-chunk
     * lobby is created at world origin (0,0).</p>
     *
     * <p>After creating the region, a platform is generated in the lobby area.</p>
     *
     * @see Settings#lobbyx
     * @see Settings#lobbyz
     * @see Settings#lobbyradius
     */
    public void createLobby() {
        Integer rad = Settings.lobbyradius;
        Point2D ctr = new Point2D.Double(Settings.lobbyx, Settings.lobbyz);

        // Verify the configured lobby area doesn't overlap existing regions
        if (!checkAreaFree(ctr, rad)) {
            getLogger().warning("Lobby area wasn't free. Trying smaller area.");
            // Try progressively smaller radii in 16-block (1 chunk) increments
            for (int i = rad-16; i > 0; i =- 16) {
                rad = i;
                if (checkAreaFree(ctr, i)) {
                    getLogger().info("Found smaller area that is free. Radius = " + i);
                    break;
                }
            }
        }

        if (rad >= 16) {
            // Create lobby with the determined radius
            Point2D c1 = new Point2D.Double(Settings.lobbyx + rad, Settings.lobbyz + rad);
            Point2D c2 = new Point2D.Double(Settings.lobbyx - rad, Settings.lobbyz - rad);
            Point2D[] corners = {c1, c2};
            lobby = new Region(plugin, corners);
            regions.put(corners, lobby);
        } else {
            // Fallback: create minimal lobby at world origin
            getLogger().warning("Could not find a free area of at least 4 chunks for the lobby.");
            getLogger().warning("Creating a default lobby of 1 chunk at 0,0.");
            Point2D c1 = new Point2D.Double(8,8);
            Point2D c2 = new Point2D.Double(-8,-8);
            Point2D[] corners = {c1, c2};
            lobby = new Region(plugin, corners);
            regions.put(corners, lobby);
        }
        // Generate the physical lobby platform in the world
        lobby.makePlatform();
        getLogger().info("Lobby area created.");
    }

    /**
     * Creates a new game instance with a newly generated region.
     *
     * <p>The creation process:</p>
     * <ol>
     *   <li>Finds a suitable location for the new region using {@link #nextRegionLocation()}</li>
     *   <li>Creates a Region aligned to chunk boundaries</li>
     *   <li>Temporarily disables chunk load processing to prevent premature world generation</li>
     *   <li>Teleports any players in the area to the lobby</li>
     *   <li>Unloads the region's chunks so they regenerate when the game starts</li>
     *   <li>Creates a Game instance and registers it</li>
     * </ol>
     *
     * <p>If a suitable location cannot be found or the game name is already taken,
     * the creation fails and a warning is logged.</p>
     *
     * @param gameName unique identifier for the new game
     */
    public void newGame(String gameName) {

        // Get the location for creating the new region
        Point2D ctr = nextRegionLocation();
        Double radius = rup16(gamedistance / 2.0);
        if (ctr == null) {
            getLogger().warning("Could not find a location to create the next region.");

        } else {
            // Calculate region corners aligned to chunk boundaries
            Point2D c1 = new Point2D.Double(rup16(ctr.getX() - radius), rup16(ctr.getY() - radius));
            Point2D c2 = new Point2D.Double(rup16(ctr.getX() + radius), rup16(ctr.getY() + radius));
            Point2D [] corners = {c1, c2};

            // Temporarily disable chunk load processing to prevent premature terrain generation
            getBeaconzPlugin().ignoreChunkLoad = true;

            // Create the region and evacuate any players currently in that area
            Region region = new Region(plugin, corners);
            region.sendAllPlayersToLobby(false);

            // Re-enable chunk load processing and unload chunks so they regenerate later
            getBeaconzPlugin().ignoreChunkLoad = false;
            region.unloadRegionChunks();

            // Validate game creation preconditions and create the game
            boolean nametaken = (getGames().get(gameName) != null);
            if (region == null || nametaken || gameName == null) {
                getLogger().warning("Could not create new game.");
            } else {
                // Create the game with current default parameters
                Game game = new Game(plugin, gamedistance, region, gameName, gamemode, nbr_teams, gamegoal, gamegoalvalue, timer, scoretypes, gamedistribution);
                games.put(gameName, game);
                regions.put(region.corners(), region);
            }
        }
    }

    /**
     * Finds an available location in the world for a new game region.
     *
     * <p>The search algorithm:</p>
     * <ol>
     *   <li>If only the lobby exists, try to place the first game at world center</li>
     *   <li>For each existing game region, check the four cardinal directions at
     *       (region_radius + 512 + new_game_radius) distance</li>
     *   <li>If no suitable location is found near existing regions, try 10 random
     *       locations within a 100x game distance area</li>
     * </ol>
     *
     * <p>All coordinates are aligned to chunk boundaries (multiples of 16) to ensure
     * proper region regeneration. The 512-block buffer prevents interference during
     * region deletion and regeneration.</p>
     *
     * <p><b>Important:</b> This method does NOT trigger chunk loads - it only performs
     * geometric calculations.</p>
     *
     * @return coordinates for the center of a suitable region, or null if none found
     */
    public Point2D nextRegionLocation() {
        Point2D newregionctr = null;
        Double gradius = rup16(gamedistance / 2.0);

        // Try to place near existing regions with a safety buffer
        if (regions != null) {
            if (regions.size() == 1 && regions.containsValue(lobby)) {
                // First game region - try world center
                Point2D rctr = new Point2D.Double(Settings.xCenter, Settings.zCenter);
                newregionctr = goodNeighbor(rctr, gradius);
            } else {
                // Check adjacent to each existing game region
                for (Point2D[] key : regions.keySet()) {
                    Region region = regions.get(key);
                    if (region != lobby) {
                        // Try placing at region_radius + 512-block buffer + new_region_radius
                        newregionctr = goodNeighbor(region.getCenter(), region.getRadius() + 512D + gradius);
                        if (newregionctr != null) {
                            break;
                        }                        
                    }
                }                
            }
        }

        // Fallback: try random locations if adjacent placement failed
        if (newregionctr == null) {
            for (int i = 0; i <10; i++) {
                Random rand = new Random();
                int r = rand.nextInt(gamedistance * 100);
                Point2D rctr = new Point2D.Double(r, r);
                newregionctr = goodNeighbor(rctr, gradius);
                if (newregionctr != null) {
                    break;
                }
            }
        }
        return newregionctr;
    }

    /**
     * Tests the four cardinal directions around a point for suitable region placement.
     *
     * <p>For a given center point, this method checks locations at the specified distance
     * in all four directions (up/north, down/south, left/west, right/east). The first
     * location that is both free (no overlap with existing regions) and safe (not too
     * much water/lava) is returned.</p>
     *
     * @param rctr the center point to search around
     * @param distance how far from rctr to place the test region centers
     * @return coordinates of the first suitable location found, or null if none suitable
     */
    public Point2D goodNeighbor(Point2D rctr, Double distance) {
        Point2D location = null;
        // Calculate test positions in all four cardinal directions
        Point2D upctr = new Point2D.Double(rup16(rctr.getX()), rup16(rctr.getY() + distance));
        Point2D downctr = new Point2D.Double(rup16(rctr.getX()), rup16(rctr.getY() - distance));
        Point2D rightctr = new Point2D.Double(rup16(rctr.getX() + distance), rup16(rctr.getY()));
        Point2D leftctr = new Point2D.Double(rup16(rctr.getX() - distance), rup16(rctr.getY()));

        Double radius = rup16(gamedistance / 2.0);
        // Check each direction in order: up, right, down, left
        if (isAreaFree(upctr, radius) && isAreaSafe(upctr, radius)) {
            location = upctr;
        } else {
            if (isAreaFree(rightctr, radius) && isAreaSafe(rightctr, radius)) {
                location = rightctr;
            } else {
                if (isAreaFree(downctr, radius) && isAreaSafe(downctr, radius)) {
                    location = downctr;
                } else {
                    if (isAreaFree(leftctr, radius) && isAreaSafe(leftctr, radius)) {
                        location = leftctr;
                    }
                }
            }
        }

        return location;
    }

    /**
     * Checks if a circular area around a center point overlaps with any existing regions.
     *
     * <p>Convenience method that converts center/radius to corner points and delegates
     * to {@link #isAreaFree(Point2D, Point2D)}.</p>
     *
     * @param ctr the center of the area to check
     * @param radius the radius of the area
     * @return true if the area doesn't overlap any existing regions
     */
    public Boolean isAreaFree(Point2D ctr, Double radius) {
        // Convert center/radius to corner points
        Point2D pt1 = new Point2D.Double(ctr.getX() - radius, ctr.getY() - radius);
        Point2D pt2 = new Point2D.Double(ctr.getX() + radius, ctr.getY() + radius);
        return isAreaFree(pt1, pt2);
    }

    /**
     * Checks if a rectangular area defined by corner points overlaps with any existing regions.
     *
     * <p>Tests whether any of the four corners of the test area fall within existing regions.
     * This is sufficient to detect overlap because regions are rectangular.</p>
     *
     * @param pt1 upper-left corner of the test area
     * @param pt2 lower-right corner of the test area
     * @return true if none of the corners fall within existing regions
     */
    public Boolean isAreaFree(Point2D pt1, Point2D pt2) {
        boolean safe = true;
        // Calculate all four corners of the test rectangle
        Point2D upperleft = new Point2D.Double(pt1.getX(), pt2.getY());
        Point2D lowerright = new Point2D.Double(pt2.getX(), pt1.getY());

        // Check if any corner falls within an existing region
        for (Point2D[] key : regions.keySet()) {
            Region reg = regions.get(key);
            if (reg.containsPoint(pt1) || reg.containsPoint(upperleft) || reg.containsPoint(lowerright) || reg.containsPoint(pt2)) {
                safe = false;
                break;
            }
        }
        return safe;
    }


    /**
     * Determines if an area has sufficient land (non-ocean) for a game region.
     *
     * <p>Samples approximately 10% of the chunks in the area by checking every 50th block.
     * If more than half of the sampled chunks are ocean biomes, the area is considered
     * unsafe (too much water). This prevents creating game regions in primarily oceanic areas.</p>
     *
     * <p><b>Note:</b> Uses {@link org.bukkit.World#getHighestBlockAt(int, int)} which
     * may trigger chunk generation for unloaded chunks.</p>
     *
     * @param ctr the center of the area to check
     * @param radius the radius of the area to check
     * @return true if the area has enough land for a playable game region
     */
    public Boolean isAreaSafe (Point2D ctr, Double radius) {
        int unsafeBiomes = 0;
        // Calculate threshold: area / 1280 gives roughly half of 10% sample
        int maxBadChunks = (int)(radius*radius)/1280;
        int increment = 50; // Sample every 50th block (approx 1/10 of chunks)

        // Calculate area boundaries
        int minx = (int) (rup16(ctr.getX() - radius)/1);
        int minz = (int) (rup16(ctr.getY() - radius)/1);
        int maxx = (int) (rup16(ctr.getX() + radius)/1);
        int maxz = (int) (rup16(ctr.getY() + radius)/1);

        // Sample the area and count ocean biomes
        outerloop:
            for (int x = minx; x <= maxx; x = x + increment) {
                for (int z = minz; z <= maxz; z = z  + increment) {
                    Biome biome = getBeaconzWorld().getHighestBlockAt(x, z).getBiome();
                    if (BeaconzChunkGen.OCEANS.contains(biome)) {
                        unsafeBiomes ++;
                    }
                    // Early exit if we've found too many ocean chunks
                    if (unsafeBiomes > maxBadChunks) {
                        break outerloop;
                    }
                }
            }
        return (unsafeBiomes <= maxBadChunks);
    }

    /**
     * Sets default parameters for new games to configured values.
     *
     * <p>Loads all defaults from the Settings configuration. This is called during
     * initialization and reload to refresh default values.</p>
     */
    public void setGameDefaultParms() {
        setGameDefaultParms(null, null, null, null, null, null, null, null);
    }

    /**
     * Sets default parameters for new games with optional overrides.
     *
     * <p>For any parameter that is null, the corresponding value from Settings is used.
     * Non-null parameters override the configuration values. Game mode affects which
     * default values are selected (minigame vs strategy mode have different defaults).</p>
     *
     * @param mode game mode ("minigame" or "strategy")
     * @param gdistance distance/size of game regions in blocks
     * @param nteams number of teams in new games
     * @param ggoal win condition type
     * @param gvalue win condition threshold value
     * @param gtimer countdown timer duration
     * @param stypes score types to track
     * @param distribution beacon distribution pattern
     */
    public void setGameDefaultParms(String mode, Integer gdistance, Integer nteams, String ggoal, Integer gvalue, Integer gtimer, String stypes, Double distribution) {
        gamemode = mode != null ? mode : Settings.gamemode;
        gamedistance = gdistance != null ? gdistance : Settings.gameDistance;
        gamedistribution = distribution!= null ? distribution : Settings.distribution;
        nbr_teams = nteams != null ? nteams : Settings.defaultTeamNumber;

        // Select appropriate defaults based on game mode
        String defaultgoal = Settings.minigameGoal;
        if(gamemode.equals("strategy")) defaultgoal = Settings.strategyGoal;
        gamegoal = ggoal != null ? ggoal : defaultgoal;

        Integer defaultgvalue = Settings.minigameGoalValue;
        if(gamemode.equals("strategy")) defaultgvalue = Settings.strategyGoalValue;
        gamegoalvalue = gvalue != null ? gvalue : defaultgvalue;

        Integer defaulttimer = Settings.minigameTimer;
        if(gamemode.equals("strategy")) defaulttimer = Settings.strategyTimer;
        timer = gtimer != null ? gtimer : defaulttimer;

        String defaultsct = Settings.minigameScoreTypes;
        if(gamemode.equals("strategy")) defaultsct = Settings.strategyScoreTypes;
        scoretypes = stypes!= null ? stypes: defaultsct;
    }

    /**
     * Gets a player's team based on their current location in the world.
     *
     * <p>This method performs more comprehensive checking than {@link Scorecard#getTeam(Player)}
     * by verifying the player is in a valid game region. If the player is not in a team
     * or the scorecard is null, and they're not in the lobby and not an operator, they
     * are teleported to the lobby.</p>
     *
     * <p>Operators are exempt from the lobby teleport to allow server administration.</p>
     *
     * @param player the player to check
     * @return the player's team, or null if they're not on a team
     */
    public Team getPlayerTeam(Player player) {
        Team team = null;
        Scorecard sc = getSC(player);
        // Try to get team from the scorecard if it exists
        if (sc != null && sc.getTeam(player) != null) {
            team = sc.getTeam(player);
        }
        // If no team and player is not an admin and not in lobby, send them to lobby
        if (team == null && !player.isOp()&& !isPlayerInLobby(player)) {
                getLobby().tpToRegionSpawn(player,true);
            }
        
        return team;
    }


    /**
     * Gets the region containing a specific location.
     *
     * @param loc the location to check
     * @return the Region containing this location, or null if not in any region
     */
    public Region getRegion (Location loc) {
        return getRegion(loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Gets the region containing specific world coordinates.
     *
     * <p>Iterates through all registered regions to find one that contains the point.
     * This is the core spatial lookup method used by other getRegion overloads.</p>
     *
     * @param x the x-coordinate in blocks
     * @param z the z-coordinate in blocks
     * @return the Region containing this point, or null if not in any region
     */
    public Region getRegion (int x, int z) {
        Region region = null;
        if (regions != null) {
            for (Region reg : regions.values()) {
                if (reg.containsPoint(x, z)) { 
                    region = reg;
                    break;
                }
            }
        }
        return region;
    }

    /**
     * Gets the game instance for a specific team.
     *
     * <p>Searches all active games to find which one contains the specified team.</p>
     *
     * @param team the team to look up
     * @return the Game this team belongs to, or null if team not found
     */
    public Game getGame (Team team) {
        Game game = null;
        if (games != null) {
            for (Game g : games.values()) {
                if (g.getScorecard().getTeamMembers().containsKey(team)) {
                    game = g;
                    break;
                }
            }
        }
        return game;
    }

    /**
     * Gets a game by its registered name.
     *
     * @param gamename the unique name of the game
     * @return the Game with this name, or null if not found
     */
    public Game getGame (String gamename) {
        return games.get(gamename);
    }

    /**
     * Gets the game at a specific 2D point in the world.
     *
     * @param point the coordinates to check
     * @return the Game at this location, or null if not in any game region
     */
    public Game getGame (Point2D point) {
        return getGame(getRegion((int)point.getX(), (int)point.getY()));
    }

    /**
     * Gets the game at specific integer coordinates.
     *
     * @param x the x-coordinate in blocks
     * @param z the z-coordinate in blocks
     * @return the Game at this location, or null if not in any game region
     */
    public Game getGame (int x, int z) {
        return getGame(getRegion(x, z));
    }

    /**
     * Gets the game at a specific Bukkit location.
     *
     * @param loc the location to check
     * @return the Game at this location, or null if not in any game region
     */
    public Game getGame (Location loc) {
        return getGame(getRegion(loc.getBlockX(), loc.getBlockZ()));
    }

    /**
     * Gets the game for a line segment (typically a beacon link).
     *
     * <p>Uses the first point of the line to determine which game region it belongs to.</p>
     *
     * @param link a line segment, typically representing a beacon connection
     * @return the Game containing the start of this link, or null if not in any game
     */
    public Game getGame (Line2D link) {
        return getGame(getRegion((int)link.getX1(), (int)link.getY1()));
    }

    /**
     * Gets the game associated with a specific region.
     *
     * @param region the region to query
     * @return the Game in this region, or null if the region has no game or is null
     */
    public Game getGame(Region region) {
        Game game = null;
        if (region != null) game = region.getGame();
        return game;
    }

    /**
     * Gets the scorecard for the game at a player's current location.
     *
     * @param player the player whose location determines which scorecard to retrieve
     * @return the Scorecard for the game at the player's location, or null if not in a game
     */
    public Scorecard getSC(Player player) {
        return getSC(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
    }

    /**
     * Gets the scorecard for the game at a 2D point.
     *
     * @param point the coordinates to check
     * @return the Scorecard for the game at this point, or null if not in a game region
     */
    public Scorecard getSC(Point2D point) {
        return getSC((int)point.getX(), (int)point.getY());
    }

    /**
     * Gets the scorecard for the game at specific coordinates.
     *
     * <p>This is the core scorecard lookup method used by other getSC overloads.</p>
     *
     * @param x the x-coordinate in blocks
     * @param z the z-coordinate in blocks
     * @return the Scorecard for the game at these coordinates, or null if not in a game
     */
    public Scorecard getSC(int x, int z) {
        Scorecard sc = null;
        Game game = getGame(x, z);
        if (game != null) sc = game.getScorecard();
        return sc;
    }

    /**
     * Gets the scorecard for a specific team.
     *
     * @param team the team to look up
     * @return the Scorecard managing this team, or null if team not found
     */
    public Scorecard getSC(Team team) {
        Game game = getGame(team);
        if (game != null) {
            return game.getScorecard();
        }
        return null;
    }

    /**
     * Gets the scorecard for the game at a specific location.
     *
     * @param location the location to check
     * @return the Scorecard for the game at this location, or null if not in a game
     */
    public Scorecard getSC(Location location) {
        return getSC(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Gets the lobby region where players wait between games.
     *
     * @return the lobby Region
     */
    public Region getLobby() {
        return lobby;
    }

    /**
     * Gets the map of all regions indexed by their corner coordinates.
     *
     * @return LinkedHashMap mapping region corners to Region objects
     */
    public LinkedHashMap<Point2D[], Region> getRegions() {
        return regions;
    }

    /**
     * Gets the map of all active games indexed by name.
     *
     * @return LinkedHashMap mapping game names to Game objects
     */
    public LinkedHashMap<String, Game> getGames() {
        return games;
    }

    /**
     * Checks if a player is currently in the lobby region.
     *
     * @param player the player to check
     * @return true if the player is in the lobby
     */
    public Boolean isPlayerInLobby(Player player) {
        return lobby.isPlayerInRegion(player);
    }

    /**
     * Checks if a location is within the lobby region.
     *
     * @param location the location to check
     * @return true if the location is in the lobby
     */
    public Boolean isLocationInLobby(Location location) {
        return lobby.containsPoint(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Checks if a circular area is free from overlap with existing regions.
     *
     * <p>Tests whether the four corners of a square bounding box around the circle
     * overlap with any existing regions. Used primarily during lobby creation.</p>
     *
     * @param center the center of the circular area
     * @param rad the radius in blocks
     * @return true if no corners fall within existing regions
     */
    public Boolean checkAreaFree (Point2D center, Integer rad) {
        boolean free = true;
        // Calculate the four corners of the bounding square
        Point2D lowerleft = new Point2D.Double(center.getX() - rad, center.getY() - rad);
        Point2D upperleft = new Point2D.Double(center.getX()  - rad, center.getY() + rad);
        Point2D upperright = new Point2D.Double(center.getX()  + rad, center.getY() + rad);
        Point2D lowerright = new Point2D.Double(center.getX()  + rad, center.getY() - rad);

        // Check if any corner falls in an existing region
        for (Point2D[] key : regions.keySet()) {
            Region reg = regions.get(key);
            if (reg.containsPoint(lowerleft) || reg.containsPoint(upperleft) || reg.containsPoint(lowerright) || reg.containsPoint(upperright)) {
                free = false;
                break;
            }
        }
        return free;
    }

    /**
     * Rounds a coordinate to the nearest chunk boundary (multiple of 16).
     *
     * <p>Regions must align to chunk boundaries for proper regeneration. This method
     * ensures coordinates snap to chunk boundaries with the following behavior:
     * <ul>
     *   <li>Positive numbers: add 8, then floor-divide by 16 (rounds to nearest, favoring lower boundary)</li>
     *   <li>Negative numbers: floor-divide by 16 directly (rounds down toward negative infinity)</li>
     *   <li>Zero: stays at zero</li>
     * </ul>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>rup16(10) = 16.0 → floor((10+8)/16) = floor(1.125) = 1 → 16</li>
     *   <li>rup16(20) = 16.0 → floor((20+8)/16) = floor(1.75) = 1 → 16</li>
     *   <li>rup16(-10) = -16.0 → floor(-10/16) = floor(-0.625) = -1 → -16</li>
     *   <li>rup16(-20) = -32.0 → floor(-20/16) = floor(-1.25) = -2 → -32</li>
     * </ul>
     *
     * @param x the coordinate to round
     * @return the coordinate rounded to the nearest chunk boundary
     */
    public double rup16 (double x) {
        if (x < 0) {
            // For negative: floor-divide directly (no offset)
            // This rounds down toward more negative values
            return Math.floor(x / 16.0) * 16.0;
        } else {
            // For positive: add 8 then floor-divide
            // This rounds to nearest chunk boundary
            return Math.floor((x + 8) / 16.0) * 16.0;
        }
    }

    /**
     * Parses a coordinate string into a Point2D array.
     *
     * <p>Converts "x1:z1:x2:z2" format into a two-element array of Point2D objects.</p>
     *
     * @param c coordinate string in format "x1:z1:x2:z2"
     * @return array of two Point2D objects representing corners
     */
    public Point2D [] regionCorners(String c) {
        return regionCorners(Integer.parseInt(c.split(":")[0]),
                Integer.parseInt(c.split(":")[1]),
                Integer.parseInt(c.split(":")[2]),
                Integer.parseInt(c.split(":")[3]));
    }

    /**
     * Creates a Point2D array from four coordinate values.
     *
     * @param x1 first x-coordinate
     * @param z1 first z-coordinate
     * @param x2 second x-coordinate
     * @param z2 second z-coordinate
     * @return array of two Point2D objects representing corners
     */
    public Point2D [] regionCorners(int x1, int z1, int x2, int z2) {
        return new Point2D[]{new Point2D.Double(x1,z1), new Point2D.Double(x2,z2)};
    }

    /**
     * Parses a coordinate string into normalized corner points.
     *
     * <p>Converts "x1:z1:x2:z2" into a Point2D array where the first point has
     * the smaller x-coordinate and the second has the larger x-coordinate. This
     * normalization ensures consistent region representation regardless of input order.</p>
     *
     * @param c coordinate string in format "x1:z1:x2:z2"
     * @return normalized array where corners[0].x <= corners[1].x
     */
    private Point2D [] strCoordToPts(String c) {
        double x1 = Double.parseDouble(c.split(":")[0]);
        double z1 = Double.parseDouble(c.split(":")[1]);
        double x2 = Double.parseDouble(c.split(":")[2]);
        double z2 = Double.parseDouble(c.split(":")[3]);
        // Normalize so first point has smaller x-coordinate
        Double a1 = x1 < x2 ? x1 : x2;
        Double b1 = x1 < x2 ? z1 : z2;
        Double a2 = x1 < x2 ? x2 : x1;
        Double b2 = x1 < x2 ? z2 : z1;
        return new Point2D [] {new Point2D.Double(a1,b1), new Point2D.Double(a2,b2)};
    }

    /**
     * Completely removes a game and its region from the world.
     *
     * <p>The deletion process:</p>
     * <ol>
     *   <li>Removes player inventories associated with the game</li>
     *   <li>Ends the game and cleans up its resources</li>
     *   <li>Removes the game from the games registry</li>
     *   <li>Clears the beacon register for the region</li>
     *   <li>Unloads all chunks in the region</li>
     *   <li>Removes the region from the regions registry</li>
     * </ol>
     *
     * @param sender the command sender initiating the deletion (for feedback messages)
     * @param game the game to delete
     */
    public void delete(CommandSender sender, Game game) {          
        // Remove all saved player inventories for this game
        getBeaconzStore().removeGame(game.getName());
        // End the game and clean up its state
        game.delete();
        // Unregister the game
        games.remove(game.getName());
        // Clear beacon ownership tracking for this region
        getRegister().clear(game.getRegion());
        // Unload the region's chunks to free memory
        game.getRegion().unloadRegionChunks();
        // Remove the region from spatial tracking
        regions.remove(game.getRegion().corners());
    }

}


