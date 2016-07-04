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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Settings {
    /**
     * World name. Currently only one world play is supported
     */
    public static String worldName;
    /**
     * Lobby coordinates and size
     */
    public static Integer lobbyx;
    public static Integer lobbyz;
    public static Integer lobbyradius;
    /**
     * The density of the random distribution of beacons in the world
     * Number should be between 0 and 1
     */
    public static Double distribution;
    /**
     * The default gamemode - "minigame" or "strategy"
     */
    public static String gamemode;
    /**
     * The default number of teams for a new game
     */
    public static Integer default_teams;
    /**
     * The default timers for the two game modes
     * 0 means no limit; any other number is a countdown
     */
    public static Integer minigameTimer;
    public static Integer strategyTimer;
    public static Boolean showTimer = true;
    /**
     * The default sidebar options for the two game modes
     */
    public static String minigameScoreTypes;
    public static String strategyScoreTypes;
    /**
     * The default goals for the two game modes
     */
    public static String minigameGoal;
    public static Integer minigameGoalValue;
    public static String strategyGoal;
    public static Integer strategyGoalValue;
    /**
    /**
     * The x central position of the world. Will also be the default (not team) spawn point
     */
    public static int xCenter;
    /**
     * The z central position of the world. Will also be the default (not team) spawn point
     */
    public static int zCenter;
    /**
     * World size. Sets world border. If zero, size is unlimited.
     */
    public static int borderSize;
    /**
     * Adjusts random number seed
     */
    public static long seedAdjustment;
    /**
     * Time in seconds that players must wait between trying to hack a beacon
     */
    public static long mineCoolDown;
    /**
     * Effect if you hack during the cooldown period. Format is Effect:Amplifier
     * Options are: Blindness, Confusion, Harm, Hunger, Slow, Slow_digging, Weakness, Wither
     * Amplifier is a number, .e.g, 0, 1
     */
    public static List<String> minePenalty;
    /**
     *   Rewards from hacking your own team beacon
     *   Format is "Id#/Material:[Durability/Qty]:Qty:%Chance"
     */
    public static TreeMap<Integer,ItemStack> teamGoodies = new TreeMap<Integer,ItemStack>();
    /**
     *   Rewards from hacking enemy team beacon
     *   Format is "Id#/Material:[Durability/Qty]:Qty:%Chance"
     */
    public static TreeMap<Integer,ItemStack> enemyGoodies = new TreeMap<Integer,ItemStack>();
    /**
     * Effects from going into enemy triangle fields. Effects are cumulative
     * Integer is the level of triangle overlap.
     */
    public static HashMap<Integer, List<PotionEffect>> enemyFieldEffects;
    /**
     * Effects from going into friendly triangle fields. Effects are cumulative
     * Integer is the level of triangle overlap.
     */
    public static HashMap<Integer, List<PotionEffect>> friendlyFieldEffects;

    /**
     * What newbies get when they join the game
     * Format is "Id#/Material:[Durability/Qty]:Qty"
     */
    public static List<ItemStack> newbieKit = new ArrayList<ItemStack>();

    /**
     * Whether teamchat is on or not
     */
    public static boolean teamChat;

    /**
     * Max height above a beacon that defenses can be built.
     */
    public static int defenseHeight;

    /**
     * Distance between repeated games. Reuses the same world between games
     */
    public static int gameDistance;

    /**
     * The experience required to mine a beacon
     */
    public static int beaconMineExpRequired;

    /**
     * Chance that the beacon becomes exhausted and enters a cool down period
     */
    public static int beaconMineExhaustChance;

    /**
     * The distance that each exp point will go when linking a beacon
     * If zero, there is no exp cost to link beacons
     */
    public static double expDistance;

    /**
     * List of levels required to build at each level around a beacon
     */
    public static List<Integer> defenseLevels;

    /**
     * List of levels required to attack at each level around a beacon
     */
    public static List<Integer> attackLevels;

    /**
     * Coordinates of the chunk we're regenerating via the "reset" command
     */
    public static Set<Pair> populate = new HashSet<Pair>();
    
    
    /**
     * Number of seconds to wait until teleporting player
     */
    public static int teleportDelay;
    
    /**
     * Maximum number of links that a beacon can have
     */
    public static int maxLinks;

    /**
     * Block type and value
     */
    public static HashMap<Material, Integer> linkBlocks;
    
    /**
     * The maximum distance the beacon can link without extending link blocks
     */
    public static int linkLimit;
    
    
    /**
     * Height for the lobby platform
     */
    public static int lobbyHeight;
    
    /**
     * List of the blocks used for the lobby platform
     */
    public static List<String> lobbyBlocks;
    
    /**
     * Default game name
     */
    public static String defaultGameName;
    
}
