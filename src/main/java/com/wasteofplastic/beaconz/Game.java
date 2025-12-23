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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.listeners.BeaconLinkListener;
import com.wasteofplastic.beaconz.map.BeaconMap;
import com.wasteofplastic.beaconz.map.TerritoryMapRenderer;

public class Game extends BeaconzPluginDependent {

    private final Region region;
    private final String gameName;
    private final Scorecard scorecard;
    private String gamemode;
    private int gamedistance;
    private double gamedistribution;
    private int nbr_teams;
    private String gamegoal;
    private int gamegoalvalue;
    private int countdowntimer;
    private Long startTime;
    private Long gameCreateTime;
    private String scoretypes;
    private boolean gameRestart;
    private boolean isOver;


    /**
     * Each game belongs to only one Region; a game cannot exist without a region
     * A game has: name, region, scorecard (with teams)
     *
     * @param beaconzPlugin
     */
    public Game(Beaconz beaconzPlugin, int gamedistance, Region region, String gameName, String gamemode, int nbr_teams, String gamegoal, int gamegoalvalue, int countdowntimer, String scoretypes, Double distribution) {
        super(beaconzPlugin);
        this.region = region;
        this.gameName = gameName;
        this.startTime = ((System.currentTimeMillis()+500)/1000)*1000;
        this.gameCreateTime = System.currentTimeMillis();
        region.setGame(this);
        setGameParms(gamemode, gamedistance, nbr_teams, gamegoal, gamegoalvalue, countdowntimer, startTime, gameCreateTime, scoretypes, distribution);
        // Now create the scorecard
        scorecard = new Scorecard(beaconzPlugin, this);
    }

    /**
     * Handles a plugin reload
     * When plugin gets reloaded, on-going games are not changed
     * ...except for the scorecard
     */
    public void reload() {
        scorecard.reload();
    }

    /**
     * Resets an existing game
     * The idea is that all current beacons will be removed
     * and the region will regenerate "fresh"
     * THIS IS NO LONGER USED - REGENERATION OF A REGION IS DONE WHEN A GAME IS **DELETED** AND THEREFORE HANDLED BY THE GAMEMGR
     * @param sender
     */
    public void regenerate_THIS_IS_NO_LONGER_USED_AND_CAN_BE_REMOVED(CommandSender sender) {
        // Set restart flag as true
        gameRestart = true;
        
        // Move all players in game to lobby
        getRegion().sendAllPlayersToLobby(false);
        
        // Delete the teams
        scorecard.deleteTeamMembers();
        
        // Handle maps 
        Iterator<Integer> it = getRegister().getBeaconMapIndex().iterator();
        while (it.hasNext()) {
            int index = it.next();
            MapView map = Bukkit.getMap(index);
            if (map != null && (map.getWorld().equals(getBeaconzWorld()) && getRegion().containsPoint(map.getCenterX(), map.getCenterZ()))) {
                for (MapRenderer renderer : map.getRenderers()) {
                    if (renderer instanceof TerritoryMapRenderer || renderer instanceof BeaconMap) {
                        map.removeRenderer(renderer);
                    }
                }
                it.remove();
            }
        }
        // Remove all beacons
        for (BeaconObj beacon : getRegister().getBeaconRegister().values()) {
            if (this.getRegion().containsBeacon(beacon)) {
                getRegister().removeBeaconOwnership(beacon, true); 
            }
        }
        
        //region.regenerate(sender, ""); // -- no longer used - chunks will be regenerated as needed when loaded
        
        
        // Handle other loose ends
        getRegister().saveRegister();
        startTime = ((System.currentTimeMillis()+500)/1000)*1000;
        scorecard.reload();
        getBeaconzStore().removeGame(gameName);
        save();
        
        // Set restart flag back to false
        gameRestart = false;
        // Game is over
        isOver = true;
    }

    /**
     * Restarts the game
     * The idea is that all beacons remain in place and the game restarts with the same teams
     */
    public void restart() {
        // Set restart flag as true
        gameRestart = true;
        
        // Move all players in game to lobby, do not save inventory
        getRegion().sendAllPlayersToLobby(false);
        
        //TODO - figure out how to give players starting kits when they come in, since they will already be in teams...
        
        // set all beacons to "unowned"
        for (BeaconObj beacon : getRegister().getBeaconRegister().values()) {
            if (this.getRegion().containsBeacon(beacon)) {
                getRegister().removeBeaconOwnership(beacon, true);
            }
        }
        // then restart the scoreboard
        startTime = ((System.currentTimeMillis()+500)/1000)*1000;
        scorecard.reload();
        gameRestart = false;
        isOver = false;
    }

    /**
     * @return the gameRestart
     */
    public boolean isGameRestart() {
        return gameRestart;
    }

    /**
     * Pauses the game
     */
    public void pause() {
        scorecard.pause();
    }
    /**
     * Resumes the game
     */
    public void resume() {
        scorecard.resume();
    }
    /**
     * Force-ends the game
     */
    public void forceEnd() {
        //getLogger().info("DEBUG: force End called");
        isOver = true;
        scorecard.endGame();
    }

    /**
     * Saves the game to file.
     * Game loading is handled by the GameMgr
     */
    public void save() {
        File gamesFile = new File(getBeaconzPlugin().getDataFolder(),"games.yml");
        YamlConfiguration gamesYml = YamlConfiguration.loadConfiguration(gamesFile);

        // Backup the games file just in case
        /*
        if (gamesFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"games.old");
            gamesFile.renameTo(backup);
        }
         */
        // Save game name, region and all parameters
        String path = "game." + gameName;
        gamesYml.set(path + ".region", ptsToStrCoord(region.corners()));
        gamesYml.set(path + ".gamemode", gamemode);
        gamesYml.set(path + ".gamedistance", gamedistance);
        gamesYml.set(path + ".nbrteams", nbr_teams);
        gamesYml.set(path + ".gamegoal", gamegoal);
        gamesYml.set(path + ".goalvalue", gamegoalvalue);
        gamesYml.set(path + ".starttime", startTime);
        gamesYml.set(path + ".creationtime", this.gameCreateTime);
        gamesYml.set(path + ".countdowntimer", scorecard.getCountdownTimer());
        gamesYml.set(path + ".scoretypes", scoretypes);
        gamesYml.set(path + ".gameOver", isOver);
        gamesYml.set(path + ".gamedistribution", gamedistribution);

        // Now save to file
        try {
            gamesYml.save(gamesFile);
        } catch (IOException e) {
            getLogger().severe("Problem saving games file for " + gameName + "!");
            e.printStackTrace();
        }

        // Save the teams
        scorecard.saveTeamMembers();
    }

    /**
     * Deletes the game
     */
    public void delete() {
        
        // End the game if it's active
        if (!isOver) forceEnd();
        
        // Delete the teams
        scorecard.deleteTeamMembers();
        
        // Teleport any players in the game to the lobby, do not save inventory
        region.sendAllPlayersToLobby(false);
        
        // Prepare files 
        File gamesFile = new File(getBeaconzPlugin().getDataFolder(),"games.yml");
        YamlConfiguration gamesYml = YamlConfiguration.loadConfiguration(gamesFile);

        // Backup the games file just in case
        if (gamesFile.exists()) {
            File backup = new File(getBeaconzPlugin().getDataFolder(),"games.old");
            gamesFile.renameTo(backup);
        }

        // Get ready to delete game name, region and all parameters from file
        String path = "game." + gameName;
        gamesYml.set(path, null);

        // Now save to file
        try {
            gamesYml.save(gamesFile);
        } catch (IOException e) {
            getLogger().severe("Problem saving games file!");
            e.printStackTrace();
        }

    }

    /**
     * Handles players joining and leaving the game
     * @param player
     */
    public void join(Player player) {
        join(player, true);
    }

    /**
     * Handles players joining and leaving the game
     * @param player
     * @param goHome - if true, player will be teleported to the team home position
     */
    public void join(Player player, boolean goHome) {
        // Player is either joining for the first time or returning to the game
        //getLogger().info("DEBUG: player join home = " + goHome);
        boolean newPlayer = !scorecard.inTeam(player);
        if (newPlayer) {
            senderMsg(player, ChatColor.GREEN + Lang.titleWelcomeToGame.replace("[name]", ChatColor.YELLOW + gameName));
        } else {
            senderMsg(player, ChatColor.GREEN + Lang.titleWelcomeBackToGame.replace("[name]", ChatColor.YELLOW + gameName));
        }

        // Assign a team if player doesn't have one
        scorecard.assignTeam(player);

        // Take him to his team's home in the game
        if (goHome) {
            //getLogger().info("DEBUG: go home");
            scorecard.sendPlayersHome(player, false); // this triggers PlayerTeleportListener, which calls region.enter()
        } else {
            region.enter(player); // if the player is not teleporting, we have to process region.enter()
        }

        // Update scores
        scorecard.refreshScores();

        // Give newbie kit
        if (newPlayer) {
            giveStartingKit(player);
        }
    }

    /**
     * Gives player a starting kit, clears inventory first
     * For minigame games, gives player starting XP
     * @param player
     */
    public void giveStartingKit(Player player) {
        if (gamemode.equals("minigame")) {
            BeaconLinkListener.setTotalExperience(player, Settings.initialXP);
        }
        player.getInventory().clear();
        for (ItemStack item : Settings.newbieKit) {
            HashMap<Integer, ItemStack> tooBig = player.getInventory().addItem(item);
            if (!tooBig.isEmpty()) {
                for (ItemStack items : tooBig.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), items);
                }
            }
        }
    }
    
    /**
     * Kicks all player from a game
     */
    public void kickAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            leave(null, player);
        }
    }
    /**
     * Kicks one player from a game
     * @param sender
     * @param player
     */
    public void kick(CommandSender sender, Player player) {
        leave(sender, player);
    }

    /**
     * Removes player from this game
     * @param player
     */
    public void leave(CommandSender sender, Player player) {
        Team team = scorecard.getTeam(player);
        if (team != null) {
            // Player will no longer be a part of the game (not just exiting the region)
            scorecard.removeTeamPlayer(player);
            player.setScoreboard(scorecard.getManager().getNewScoreboard());
            // Remove any maps
            for (ItemStack item: player.getInventory()) {
                if (item != null && item.getType().equals(Material.MAP)) {
                    if (getRegister().getBeaconMap(item.getDurability()) != null) {
                        getRegister().removeBeaconMap(item.getDurability());
                    }
                }
            }            
            senderMsg(sender, ChatColor.GREEN + Lang.generalSuccess);
        } else {
            senderMsg(sender, ChatColor.RED + Lang.errorNotInGame.replace("[game]", this.gameName));
        }
        getRegion().sendToLobby(player, false);
    }
    
    /**
     * Returns the game's name
     */
    public String getName() {
        return gameName;
    }
    /**
     * Returns the game's region
     */
    public Region getRegion() {
        return region;
    }
    /**
     * Returns the game's scorecard
     */
    public Scorecard getScorecard() {
        return scorecard;
    }
    /**
     * Return the game's parameters
     */
    public String getGamemode() {return gamemode;}
    public int getGamedistance() {return gamedistance;}
    public double getGamedistribution() {return gamedistribution;}
    public int getNbrTeams() {return nbr_teams;}
    public String getGamegoal() {return gamegoal;}
    public int getGamegoalvalue() {return gamegoalvalue;}
    public int getCountdownTimer() {return countdowntimer;}
    public Long getStartTime() {return startTime;}
    public Long getCreateTime() {return gameCreateTime;}
    public String getScoretypes() {return scoretypes;}


    /**
     * Sets the game's parameters
     * @param gamemode
     * @param nbr_teams
     * @param gamegoal
     * @param gamegoalvalue
     * @param countdowntimer
     * @param startTime
     * @param scoretypes
     */
    public void setGameParms(String gamemode, int gdistance, int nbr_teams, String gamegoal, int gamegoalvalue, int countdowntimer, Long startTime, Long createTime, String scoretypes, double distribution) {
        this.gamemode = gamemode;
        this.gamedistance = gdistance;
        this.nbr_teams = nbr_teams;
        this.gamegoal = gamegoal;
        this.gamegoalvalue = gamegoalvalue;
        this.countdowntimer = countdowntimer;
        this.startTime = startTime;
        this.gameCreateTime = createTime;
        this.scoretypes = scoretypes;
        this.gamedistribution = distribution;
    }
    public void setGamemode(String gm) {gamemode = gm;}
    public void setGamedistance(int gd) {gamedistance = gd;}
    public void setNbrTeams(int tn) {nbr_teams = tn;}
    public void setGamegoal(String gg) {gamegoal = gg;}
    public void setGamegoalvalue(int gv) {gamegoalvalue = gv;}
    public void setCountdownTimer(int cd) {countdowntimer = cd;}
    public void setStartTime(Long stt) {startTime = stt;}
    public void setScoretypes(String sct) {scoretypes = sct;}
    public void setGamedistribution(double gdist) {gamedistribution = gdist;}

    /**
     * Converts into a Point2D array of 2 points into a string x1:z1:x2:z2
     * ... preserves the points order in the array
     */
    private String ptsToStrCoord(Point2D [] c) {
        return c[0].getX() + ":" + c[0].getY() + ":" + c[1].getX() + ":" + c[1].getY();
    }

    /**
     * Converts a location to a simple string representation
     * If location is null, returns empty string
     */
    static public String getStringLocation(final Location l) {
        if (l == null || l.getWorld() == null) {
            return "";
        }
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" + Float.floatToIntBits(l.getYaw()) + ":" + Float.floatToIntBits(l.getPitch());
    }

    /**
     * Checks if this game has this player in it or not
     * @param player
     * @return true if player is a part of the game
     */
    public boolean hasPlayer(Player player) {
        return scorecard.inTeam(player);
    }

    /**
     * @return true if game is over
     */
    public boolean isOver() {
        return isOver;
    }

    /**
     * @param isOver when the game is over
     */
    public void setOver(boolean isOver) {
        //getLogger().info("DEBUG: game setOver = " + isOver);
        this.isOver = isOver;
    }

    public void leave(Player player) {
        leave(player, player);      
    }

}
