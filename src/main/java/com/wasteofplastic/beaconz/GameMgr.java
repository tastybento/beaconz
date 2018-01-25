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

public class GameMgr extends BeaconzPluginDependent {

    private Beaconz plugin;
    private Region lobby;
    private LinkedHashMap<Point2D[], Region> regions;
    private LinkedHashMap<String, Game> games;
    private String gamemode;
    private Integer gamedistance;
    private Double gamedistribution;
    private Integer nbr_teams;
    private String gamegoal;
    private Integer gamegoalvalue;
    private Integer timer;
    private String scoretypes;

    /**
     * GameMgr handles the creation and destruction of games and regions
     */
    public GameMgr(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
        this.plugin = beaconzPlugin;
        regions = new LinkedHashMap<Point2D[], Region>();
        games = new LinkedHashMap<String, Game>();
        setGameDefaultParms();
        loadAllGames();
        if (lobby == null) {
            createLobby();
        }
    }

    /**
     * Handles plugin Reload
     * When plugin gets reloaded, on-going games are not changed
     * ...except for the scorecard
     */
    public void reload() {
        saveAllGames();
        setGameDefaultParms();
        loadAllGames();
        /*
        createLobby();
        for (Game g : getGames().values()) {
            g.reload();
        }
         */
    }

    /**
     * Saves games to file
     */
    public void saveAllGames() {
        File gamesFile = new File(getBeaconzPlugin().getDataFolder(),"games.yml");
        YamlConfiguration gamesYml = YamlConfiguration.loadConfiguration(gamesFile);
        // Backup the games file just in case
        if (gamesFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"games.old");
            gamesFile.renameTo(backup);
        }
        // Save the lobby
        if (lobby != null) {
            gamesYml.set("lobby.region", ptsToStrCoord(lobby.corners()));
            //getLogger().info("lobby spawnpoint: " + lobby.getSpawnPoint());
            gamesYml.set("lobby.spawn", Beaconz.getStringLocation(lobby.getSpawnPoint()));
        }
        // Now save to file
        try {
            gamesYml.save(gamesFile);
        } catch (IOException e) {
            getLogger().severe("Problem saving lobby in games file!");
            e.printStackTrace();
        }
        // Save the games
        for (Game game: games.values()) {
            game.save();
            // Save game name, region and all parameters
            /*
            String path = "game." + game.getName();
            gamesYml.set(path + ".region", ptsToStrCoord(game.getRegion().getCorners()));
            gamesYml.set(path + ".gamemode", game.getGamemode());
            gamesYml.set(path + ".nbrteams", game.getNbrTeams());
            gamesYml.set(path + ".gamegoal", game.getGamegoal());
            gamesYml.set(path + ".goalvalue", game.getGamegoalvalue());
            gamesYml.set(path + ".starttime", game.getStartTime());
            gamesYml.set(path + ".createtime", game.getCreateTime());
            gamesYml.set(path + ".countdowntimer", game.getScorecard().getCountdownTimer());
            gamesYml.set(path + ".scoretypes", game.getScoretypes());
            gamesYml.set(path + ".gameOver", game.isOver());
            */
        }
    }

    /**
     * Converts into a Point2D array of 2 points into a string x1:z1:x2:z2
     * ... preserves the points order in the array
     */
    private String ptsToStrCoord(Point2D [] c) {
        return c[0].getX() + ":" + c[0].getY() + ":" + c[1].getX() + ":" + c[1].getY();
    }

    public void saveGame(String name) {
        Game game = games.get("name");
        if (game!= null) game.save();
    }

    /**
     * Loads games from file
     */
    public void loadAllGames() {
        //getLogger().info("DEBUG: loading all games");
        regions.clear();
        games.clear();
        loadGames(null);
    }

    /**
     * Loads the game from file by name, or if name is null, then all games will be loaded.
     * @param gameName
     */
    public void loadGames(String gameName) {
        // if gameName is null, loads all games, if not, just gameName

        File gamesFile = new File(getBeaconzPlugin().getDataFolder(),"games.yml");
        if (gamesFile.exists()) {
            YamlConfiguration gamesYml = new YamlConfiguration();
            try {
                gamesYml.load(gamesFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidConfigurationException e) {
                getLogger().severe("Problem with games.yml formatting");
                e.printStackTrace();
            }

            ConfigurationSection csec = gamesYml.getConfigurationSection("lobby");
            // Load the lobby when it is started if it exists
            if (gameName == null) {
                if (csec != null) {
                    //getLogger().info("DEBUG: Loading lobby from file");
                    Point2D [] corners = strCoordToPts(csec.getString("region"));
                    lobby = new Region(plugin, corners);
                    String spawn = csec.getString("spawn", "");
                    if (!spawn.isEmpty()) {
                        lobby.setSpawnPoint(Beaconz.getLocationString(spawn));
                    }
                    regions.put(corners, lobby);
                }
            }
            // got the file, get the data
            csec = gamesYml.getConfigurationSection("game");
            if (csec != null) {
                for (String gname : csec.getKeys(false)) {
                    if (gameName == null || gname.equals(gameName)) {
                        // Load the game info from games.yml
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
                        Double gdist = csec.getDouble(gname + ".gamedistribution");

                        Game game = games.get(gameName);
                        if (game != null && gameName != null) {
                            // We're loading an active game, reset the game parms and reload it
                            game.setGameParms(gm, gd, nt, gg, gv,gt, st, ct, gs, gdist);
                            game.setOver(isOver);
                            region.setGame(game);
                            game.reload();
                        } else {
                            // We're loading an existing game from file that's not currently active
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
     * Create the lobby area, with Settings.lobbyx, lobbyz and lobbyradius
     * NOTE: the server admins must ensure that the lobby area is safe
     * after all, they will be making their own lobbies...
     * OR we need to have the option of loading different schematics
     */
    public void createLobby() {
        //getLogger().info("DEBUG: creating lobby");
        Integer rad = Settings.lobbyradius;
        Point2D ctr = new Point2D.Double(Settings.lobbyx, Settings.lobbyz);

        // The admin is responsible for ensuring that the lobby area is safe
        // If the user increased the radius of the lobby in config.yml, it might invade existing game regions
        // Just to be safe, let's check that and try smaller areas
        // Check that lobby area is FREE. Admin is responsible for making sure it is safe
        if (!checkAreaFree(ctr, rad)) {
            getLogger().warning("Lobby area wasn't free. Trying smaller area.");
            for (int i = rad-16; i > 0; i =- 16) {
                rad = i;
                if (checkAreaFree(ctr, i)) {
                    getLogger().info("Found smaller area that is free. Radius = " + i);
                    break;
                }
            }
        }

        if (rad >= 16) {
            //getLogger().info("Creating lobby at Ctr:Rad [" + ctr.getX() + ", " + ctr.getY() + "] : " + rad);
            Point2D c1 = new Point2D.Double(Settings.lobbyx + rad, Settings.lobbyz + rad);
            Point2D c2 = new Point2D.Double(Settings.lobbyx - rad, Settings.lobbyz - rad);
            Point2D[] corners = {c1, c2};
            lobby = new Region(plugin, corners);
            regions.put(corners, lobby);
        } else {
            getLogger().warning("Could not find a free area of at least 4 chunks for the lobby.");
            getLogger().warning("Creating a default lobby of 1 chunk at 0,0.");
            Point2D c1 = new Point2D.Double(8,8);
            Point2D c2 = new Point2D.Double(-8,-8);
            Point2D[] corners = {c1, c2};
            lobby = new Region(plugin, corners);
            regions.put(corners, lobby);
        }
        // Create a lobby platform
        lobby.makePlatform();
        getLogger().info("Lobby area created.");
    }

    /**
     * Create a new game, in a new region
     */
    public void newGame(String gameName) {
        
        // Get the location for creating the new region
        Point2D ctr = nextRegionLocation();
        Double radius = rup16(gamedistance / 2.0);
        if (ctr == null) {
            getLogger().warning("Could not find a location to create the next region.");
            
        } else {
            Point2D c1 = new Point2D.Double(rup16(ctr.getX() - radius), rup16(ctr.getY() - radius));
            Point2D c2 = new Point2D.Double(rup16(ctr.getX() + radius), rup16(ctr.getY() + radius));
            Point2D [] corners = {c1, c2};
            
            //getLogger().info("GameMgr.newRegion - about to create new region at " + "[" + c1.getX() + ", " + c1.getY() + "] and [" + c2.getX() + ", " + c2.getY() + "]");
            
            // Tell WorldListener not to process these chunk loads
            getBeaconzPlugin().ignoreChunkLoad = true;
            
            // Make new region and send players there
            Region region = new Region(plugin, corners);
            region.sendAllPlayersToLobby(false);
            
            // Tell WorldListener to process chunk loads again, and unload the region's chunks so they will reload and regenerate once accessed in the game 
            getBeaconzPlugin().ignoreChunkLoad = false;
            region.unloadRegionChunks();

            // Have the region, create the game
            Game game = null;
            Boolean nametaken = (getGames().get(gameName) != null);
            if (region == null || nametaken || gameName == null) {
                getLogger().warning("Could not create new game.");
            } else {
                game = new Game(plugin, gamedistance, region, gameName, gamemode, nbr_teams, gamegoal, gamegoalvalue, timer, scoretypes, gamedistribution);
                games.put(gameName, game);
                regions.put(region.corners(), region);
            }
        }
    }

    /**
     * nextRegionLocation will find a free spot to create a new region
     * The region size is given by gamedistance
     * Region boundaries must match chunk boundaries, otherwise region.regenerate has a problem
     * When a good region candidate is found, reject it if its surface is more than 40% water or lava
     * NOTE: nextRegionLocation and its methods DO NOT trigger any chunk loads
     */
    public Point2D nextRegionLocation() {
        Point2D newregionctr = null;
        Double gradius = rup16(gamedistance / 2.0);

        // For each region already defined, try to find an empty area at "distance" blocks from its center
        // Ignore the lobby as a seed
        // Leave a 512-block wide "safety zone" between regions because a region is 32 chunks big and
        // regeneration is done by deleting regions.
        // at this point the regions map should never be null, because the lobby region should always exist
        if (regions != null) {
            if (regions.size() == 1 && regions.containsValue(lobby)) {
                // There are no game regions yet, try to create one at the world center
                Point2D rctr = new Point2D.Double(Settings.xCenter, Settings.zCenter);
                newregionctr = goodNeighbor(rctr, gradius);
            } else {
                for (Point2D[] key : regions.keySet()) {
                    Region region = regions.get(key);
                    if (region != lobby) {
                        //getLogger().info("GameMgr.nextRegionLocation - processing region at " + region.getCenter());
                        newregionctr = goodNeighbor(region.getCenter(), region.getRadius() + 512D + gradius);
                        if (newregionctr != null) {
                            break;
                        }                        
                    }
                }                
            }
        }

        // If the regions around the existing regions are not OK, try 10 other locations at random
        if (newregionctr == null) {
            for (int i = 0; i <10; i++) {
                Random rand = new Random();
                Integer r = rand.nextInt(gamedistance * 100);
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
     * goodNeighbor
     * Checks neighboring regions (to the one at rctr) to see if they are free and good
     * @param rctr - the center of the original region
     * @param distance - how far from rctr the center must be the center of any region being tested
     * @returns the center of the first safe region it finds
     */
    public Point2D goodNeighbor(Point2D rctr, Double distance) {
        //getLogger().info("GameMgr.goodNeighbor =================== from origin [" + rctr.getX() + ", " + rctr.getY() + "]" + " and distance = " + distance);
        Point2D location = null;
        // now try all four sides
        Point2D upctr = new Point2D.Double(rup16(rctr.getX()), rup16(rctr.getY() + distance));
        Point2D downctr = new Point2D.Double(rup16(rctr.getX()), rup16(rctr.getY() - distance));
        Point2D rightctr = new Point2D.Double(rup16(rctr.getX() + distance), rup16(rctr.getY()));
        Point2D leftctr = new Point2D.Double(rup16(rctr.getX() - distance), rup16(rctr.getY()));

        Double radius = rup16(gamedistance / 2.0);
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
     * isAreaFree
     * Determines whether an area around a point overlaps with any existing regions
     * ... and if it's a safe area for a new region
     */
    public Boolean isAreaFree(Point2D ctr, Double radius) {
        // pt1 **must be** the upper left corner and pt2 **must be** the lower right corner
        Point2D pt1 = new Point2D.Double(ctr.getX() - radius, ctr.getY() - radius);
        Point2D pt2 = new Point2D.Double(ctr.getX() + radius, ctr.getY() + radius);
        return isAreaFree(pt1, pt2);
    }

    public Boolean isAreaFree(Point2D pt1, Point2D pt2) {
        // pt1 **must be** the upper left corner and pt2 **must be** the lower right corner
        Boolean safe = true;
        Point2D lowerleft = pt1;
        Point2D upperleft = new Point2D.Double(pt1.getX(), pt2.getY());
        Point2D upperright = pt2;
        Point2D lowerright = new Point2D.Double(pt2.getX(), pt1.getY());
        //getLogger().info("IsAreaFree - checking: [" + lowerleft.getX() + ":" + lowerleft.getY() + "] to [" + upperright.getX() + ":" + upperright.getY() + "]");
        for (Point2D[] key : regions.keySet()) { 
            Region reg = regions.get(key);
            //getLogger().info("against region " + reg.displayCoords());
            if (reg.containsPoint(lowerleft) || reg.containsPoint(upperleft) || reg.containsPoint(lowerright) || reg.containsPoint(upperright)) {
                safe = false;
                break;
            }
            //getLogger().info("safe: " + safe);
        }
        return safe;
    }


    /**
     * isAreaSafe
     * Determines whether an area has enough non-water surface to be used for a game region
     * Checks 1/10 of the chunks in the region; if half of them are NOT ocean biomes, the area is good
     * The old isAreaSafe was essentially useless. This one should do what we need
     * Note: world.getBiome() does NOT trigger a chunk load!
     */
    public Boolean isAreaSafe (Point2D ctr, Double radius) {
        int unsafeBiomes = 0+0;
        int maxBadChunks = (int)(radius*radius)/1280;
        int increment = 50; // in order to catch approx. 1/10 of the chunks in the area    
        int minx = (int) (rup16(ctr.getX() - radius)/1);
        int minz = (int) (rup16(ctr.getY() - radius)/1);
        int maxx = (int) (rup16(ctr.getX() + radius)/1);
        int maxz = (int) (rup16(ctr.getY() + radius)/1);
        outerloop:
        for (Integer x = minx; x <= maxx; x = x + increment) {
            for (Integer z = minz; z <= maxz; z = z  + increment) {
                if (getBeaconzWorld().getBiome(x, z).equals(Biome.DEEP_OCEAN) || getBeaconzWorld().getBiome(x, z).equals(Biome.OCEAN)) {
                    unsafeBiomes ++;
                }
                if (unsafeBiomes > maxBadChunks) {
                    break outerloop;
                }
            }
        }
        return (unsafeBiomes <= maxBadChunks);
    }
    
    /**
     * isAreaSafe
     * Determines whether an area consists of less than a certain percentage of water or lava
     * For large regions, this can take too long and crash the server,
     * so the radius is capped at 128 blocks. This way we can ensure there is at least
     * a 256x256 region that's safe
     */
   /*
    public Boolean isAreaSafe (Point2D ctr, Double radius, Double percentage) {
        Integer totalblocks = 1;
        Integer unsafeblocks = 0;

        if (radius <= 128.0) {            
            Integer minx = (int) (rup16(ctr.getX() - radius)/1);
            Integer minz = (int) (rup16(ctr.getY() - radius)/1);
            Integer maxx = (int) (rup16(ctr.getX() + radius)/1);
            Integer maxz = (int) (rup16(ctr.getY() + radius)/1);

            //getLogger().info("GameMgr.isAreaSafe - at Ctr:Rad [" + ctr.getX() + ", " + ctr.getY() + "] : " + radius);

            for (Integer x = minx; x <= maxx; x++) {
                for (Integer z = minz; z <= maxz; z++) {
                    //Settings.populate.add(new Pair(((x/16)-1) , ((z/16)-1)));
                    int y = getBeaconzWorld().getHighestBlockYAt(x, z);
                    Block block = getBeaconzWorld().getBlockAt(x, y, z);
                    Block bottomblock = getBeaconzWorld().getBlockAt(x, y-1, z);
                    //getLogger().info("GameMgr.isAreaSafe - checking blocks at " + cX + ":" + cY + ":" + cZ + " -- Type: " + block.getType() + " and " + bottomblock.getType());
                    totalblocks++;
                    if (block.isLiquid() || bottomblock.isLiquid()) {
                        unsafeblocks++;
                    }
                }
            }             
        }

        Settings.populate.clear();
        //getLogger().info("GameMgr.isAreaSafe - totalblocks: " + totalblocks + " unsafe blocks: " + unsafeblocks + " (" + (unsafeblocks * 1.0) / (totalblocks * 1.0) + ")");
        return ((unsafeblocks * 1.0) / (totalblocks * 1.0)) < percentage;       
    }
    */



    /**
     * Sets the default parameters to be used for new games
     */
    public void setGameDefaultParms() {
        setGameDefaultParms(null, null, null, null, null, null, null, null);
    }
    public void setGameDefaultParms(String mode, Integer gdistance, Integer nteams, String ggoal, Integer gvalue, Integer gtimer, String stypes, Double distribution) {
        gamemode = mode != null ? mode : Settings.gamemode;
        gamedistance = gdistance != null ? gdistance : Settings.gameDistance;
        gamedistribution = distribution!= null ? distribution : Settings.distribution;
        nbr_teams = nteams != null ? nteams : Settings.defaultTeamNumber;
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
     * Gets a player's team, based on his CURRENT LOCATION
     * Does more robust checking than scorecard.getTeam
     * Warns player if not in team or if null scoreboard and sends player (not op) to lobby
     */
    public Team getPlayerTeam(Player player) {
        Team team = null;
        Scorecard sc = getSC(player);
        if (sc != null && sc.getTeam(player) != null) {
            team = sc.getTeam(player);
        }
        if (team == null) {
            if (player.isOp()) {
                // player.sendMessage(ChatColor.RED + "You are not in a team!");
            } else {
                if (!isPlayerInLobby(player)) {
                    //player.sendMessage(ChatColor.RED + "Player " + player.getName() + " must join a team to play in this world");
                    getLobby().tpToRegionSpawn(player,true);
                }
            }
        }
        return team;
    }


    /**
     * Returns a region, based on different criteria:
     * a point in the region, a location in the region, the region's game, a player in the region
     */

    /**
     * Get the region based on the location
     * @param loc
     * @return Region or null if none found
     */
    public Region getRegion (Location loc) {
        return getRegion(loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Returns the region for coord x,z
     * @param x
     * @param z
     * @return Region where x,z is inside
     */
    public Region getRegion (int x, int z) {
        // Returns the region that contains a point
        Region region = null;
        if (regions != null) {
            //getLogger().info("DEBUG: regions does not = null and is of size " + regions.size());
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
     * Returns a game, based on different criteria
     */

    /**
     * Return the game from the team
     * @param team
     * @return Game or null if none
     */
    public Game getGame (Team team) {
        Game game = null;
        if (games != null) {
            for (Game g : games.values()) {
                if (g.getScorecard().getTeamMembers().keySet().contains(team)) {
                    game = g;
                    break;
                }
            }
        }
        return game;
    }

    /**
     * Get the game from the game name
     * @param gamename
     * @return Game or null if none
     */
    public Game getGame (String gamename) {
        return games.get(gamename);
    }

    /**
     * Get game from a Point in the game area
     * @param point
     * @return Game or null if none
     */
    public Game getGame (Point2D point) {
        return getGame(getRegion((int)point.getX(), (int)point.getY()));
    }

    /**
     * Get game from an integer coord
     * @param x
     * @param z
     * @return Game or null if none
     */
    public Game getGame (int x, int z) {
        return getGame(getRegion(x, z));
    }

    /** Get game from a Location
     * @param loc
     * @return Game or null if none
     */
    public Game getGame (Location loc) {
        return getGame(getRegion(loc.getBlockX(), loc.getBlockZ()));
    }

    /**
     * Get game from the first point on a link
     * @param link
     * @return Game or null if none
     */
    public Game getGame (Line2D link) {
        return getGame(getRegion((int)link.getX1(), (int)link.getY1()));
    }

    /**
     * Get game from a region.
     * @param region
     * @return Game or null if none
     */
    public Game getGame(Region region) {
        Game game = null;
        if (region != null) game = region.getGame();
        return game;
    }

    /**
     * Return a game's ScoreCard, based on different criteria
     */

    /**
     * Get scorecard based on player's position
     * @param player
     * @return scorcard or null if none
     */
    public Scorecard getSC(Player player) {
        return getSC(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
    }

    /**
     * Get scorecard based on a point coord
     * @param point
     * @return Scorecard or null if none
     */
    public Scorecard getSC(Point2D point) {
        return getSC((int)point.getX(), (int)point.getY());
    }

    /**
     * @param x
     * @param z
     * @return Scorecard or null if none
     */
    public Scorecard getSC(int x, int z) {
        Scorecard sc = null;
        Game game = getGame(x, z);
        if (game != null) sc = game.getScorecard();
        return sc;
    }

    /**
     * Get scorecard for this team
     * @param team
     * @return scorecard or null if team isn't known
     */
    public Scorecard getSC(Team team) {
        Game game = getGame(team);
        if (game != null) {
            return game.getScorecard();
        }
        return null;
    }
    
    /**
     * @param location
     * @return Scorecard or null if none
     */
    public Scorecard getSC(Location location) {
        return getSC(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Return the lobby
     */
    public Region getLobby() {
        return lobby;
    }

    /**
     * @return the regions map
     */
    public LinkedHashMap<Point2D[], Region> getRegions() {
        return regions;
    }

    /**
     * @return the games map
     */
    public LinkedHashMap<String, Game> getGames() {
        return games;
    }

    /**
     * Determines whether a player is in the lobby
     */
    public Boolean isPlayerInLobby(Player player) {
        return lobby.isPlayerInRegion(player);
    }

    /**
     * @param location
     * @return true if in lobby
     */
    public Boolean isLocationInLobby(Location location) {
        return lobby.containsPoint(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Check if an area is free
     * (not invading other areas)
     */
    public Boolean checkAreaFree (Point2D center, Integer rad) {
        Boolean free = true;
        Point2D lowerleft = new Point2D.Double(center.getX() - rad, center.getY() - rad);;
        Point2D upperleft = new Point2D.Double(center.getX()  - rad, center.getY() + rad);
        Point2D upperright = new Point2D.Double(center.getX()  + rad, center.getY() + rad);
        Point2D lowerright = new Point2D.Double(center.getX()  + rad, center.getY() - rad);

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
     * Rounds a number up (or down, if negative) to an even chunk (16) boundary
     */
    public Double rup16 (double x) {
        Double rnd;
        if (x < 0) {
            rnd = (((int) x - 8) / 16) * 16.0;
        } else {
            rnd = (((int) x + 8) / 16) * 16.0;
        }
        return rnd;
    }

    /**
     * Returns an array of two points
     * given two sets of X,Z coordinates in either format:
     * String "x1:z1:x2:z2" - or - int x1, int z1, int x2, int z2
     * Currently not used
     */
    public Point2D [] regionCorners(String c) {
        return regionCorners(Integer.valueOf(c.split(":")[0]),
                Integer.valueOf(c.split(":")[1]),
                Integer.valueOf(c.split(":")[2]),
                Integer.valueOf(c.split(":")[3]));
    }
    public Point2D [] regionCorners(int x1, int z1, int x2, int z2) {
        Point2D [] corners = {new Point2D.Double(x1,z1), new Point2D.Double(x2,z2)};
        return corners;
    }

    /**
     * Converts a string x1:z1:x2:z2 into a Point2D array
     * that meets the condition x1 <= x2
     * Currently not used
     */
    private Point2D [] strCoordToPts(String c) {
        Double x1 = Double.valueOf(c.split(":")[0]);
        Double z1 = Double.valueOf(c.split(":")[1]);
        Double x2 = Double.valueOf(c.split(":")[2]);
        Double z2 = Double.valueOf(c.split(":")[3]);
        Double a1 = x1 < x2 ? x1 : x2;
        Double b1 = x1 < x2 ? z1 : z2;
        Double a2 = x1 < x2 ? x2 : x1;
        Double b2 = x1 < x2 ? z2 : z1;
        return new Point2D [] {new Point2D.Double(a1,b1), new Point2D.Double(a2,b2)};
    }

    /**
     * Deletes the game
     * @param sender
     * @param game
     */
    public void delete(CommandSender sender, Game game) {          
        // Remove inventories
        getBeaconzStore().removeGame(game.getName());
        // End and remove game
        game.delete();
        // Remove game from register
        games.remove(game.getName());
        // Clear the current register for the region
        getRegister().clear(game.getRegion());
        // Unload the region
        game.getRegion().unloadRegionChunks();        
        // Remove region
        regions.remove(game.getRegion().corners());
    }
    
}