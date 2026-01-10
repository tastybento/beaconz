package com.wasteofplastic.beaconz;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.math.NumberUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Game parameters
 */
public class Params {
    public enum GameMode {
        STRATEGY, MINIGAME
    }

    public enum GameScoreGoal {
        ALL, AREA, BEACONS, TIME, TRIANGLES, LINKS
    }

    private GameMode gamemode;
    private Integer size;
    private Integer teams;
    private GameScoreGoal goal;
    private Integer goalvalue;
    private Integer countdown;
    private List<GameScoreGoal> scoretypes;
    private Double distribution;

    /**
     * @param gamemode "minigame" or "persistent"
     * @param size size of game (length of edge)
     * @param teams number of teams to create
     * @param goal type of winning condition
     * @param goalvalue threshold value to win
     * @param countdown timer duration in seconds (0 = unlimited)
     * @param scoretypes list of score types to track
     * @param distribution beacon placement distribution factor (0.0-1.0)
     */
    public Params(GameMode gamemode, Integer size, Integer teams, GameScoreGoal goal, Integer goalvalue,
            Integer countdown, List<GameScoreGoal> scoretypes, Double distribution) {
        this.gamemode = gamemode;
        this.size = size;
        this.teams = teams;
        this.goal = goal;
        this.goalvalue = goalvalue;
        this.countdown = countdown;
        this.scoretypes = scoretypes;
        this.distribution = distribution;
    }
    /**
     * Create a default set of game parameters
     */
    public Params() {
        this.gamemode = GameMode.STRATEGY;
        this.size = 20000;
        this.teams = 2;
        this.goal = GameScoreGoal.AREA;
        this.goalvalue = 3000000;
        this.countdown = 0;
        this.scoretypes = List.of(GameScoreGoal.AREA);
        this.distribution = 0.2D;
    }

    /**
     * @return the gamemode
     */
    public GameMode getGamemode() {
        if (gamemode == null) {
            return Settings.gamemode;
        }
        return gamemode;
    }
    /**
     * @param gamemode the gamemode to set
     */
    public void setGamemode(GameMode gamemode) {
        this.gamemode = gamemode;
    }
    /**
     * @return the size
     */
    public Integer getSize() {
        if (size == null) {
            return Settings.borderSize;
        }
        return size;
    }
    /**
     * @param size the size to set
     */
    public void setSize(Integer size) {
        this.size = size;
    }
    /**
     * @return the teams
     */
    public Integer getTeams() {
        if (teams == null) {
            return Settings.defaultTeamNumber;
        }
        return teams;
    }
    /**
     * @param teams the teams to set
     */
    public void setTeams(Integer teams) {
        this.teams = teams;
    }
    /**
     * @return the goal
     */
    public GameScoreGoal getGoal() {
        if (goal == null) {
            return getGamemode() == GameMode.STRATEGY ? Settings.strategyGoal : Settings.minigameGoal;
        }
        return goal;
    }
    /**
     * @param goal the goal to set
     */
    public void setGoal(GameScoreGoal goal) {
        this.goal = goal;
    }
    /**
     * @return the goalvalue
     */
    public Integer getGoalvalue() {
        if (goalvalue == null) {
            return getGamemode() == GameMode.STRATEGY ? Settings.strategyGoalValue : Settings.minigameGoalValue;
        }
        return goalvalue;
    }
    /**
     * @param goalvalue the goalvalue to set
     */
    public void setGoalvalue(Integer goalvalue) {
        this.goalvalue = goalvalue;
    }
    /**
     * @return the countdown
     */
    public Integer getCountdown() {
        if (countdown == null) {
            return getGamemode() == GameMode.STRATEGY ? Settings.strategyTimer : Settings.minigameTimer;
        }
        return countdown;
    }
    /**
     * @param countdown the countdown to set
     */
    public void setCountdown(Integer countdown) {
        this.countdown = countdown;
    }
    /**
     * @return the scoretypes
     */
    public List<GameScoreGoal> getScoretypes() {
        if (scoretypes == null) {
            return getGamemode() == GameMode.STRATEGY ? Settings.strategyScoreTypes : Settings.minigameScoreTypes;
        }
        return scoretypes;
    }
    /**
     * @param scoretypes the scoretypes to set
     */
    public void setScoretypes(List<GameScoreGoal> scoretypes) {
        this.scoretypes = scoretypes;
    }
    /**
     * @return the distribution
     */
    public Double getDistribution() {
        if (distribution == null) {
            return Settings.distribution;
        }
        return distribution;
    }
    /**
     * @param distribution the distribution to set
     */
    public void setDistribution(Double distribution) {
        this.distribution = distribution;
    }

    /*
     * <p><b>Validation Rules:</b>
     * <ul>
     *   <li><b>gamemode:</b> must be "strategy" or "minigame"</li>
     *   <li><b>size:</b> must be a positive number</li>
     *   <li><b>teams:</b> must be a number between 2 and 14</li>
     *   <li><b>goal:</b> must be "area", "beacons", "links", or "triangles"</li>
     *   <li><b>goalvalue:</b> must be a positive number</li>
     *   <li><b>countdown:</b> must be a number (0 for unlimited)</li>
     *   <li><b>scoretypes:</b> must be dash-separated combination of goal types</li>
     *   <li><b>distribution:</b> must be a decimal between 0.01 and 0.99</li>
     * </ul>
     *
     * <p><b>Format Requirements:</b>
     * All parameters must be in "parameter:value" format. Missing colons or
     * values will result in an error.
     *
     * @param args array of "parameter:value" strings to validate
     * @return error message describing all problems found, or empty string if valid
     */
    public Params(String[] args) throws IOException {
        Component errormsg = Component.empty();

        // Check that *ALL* arguments are valid parms
        for (String arg : args) {
            String parm = null;
            String value = null;
            try {
                // Parse parameter:value format
                parm = arg.split(":")[0];
                value = arg.split(":")[1];
            } catch (Exception e) {
                // Silently handle split exceptions - will be caught below
            }

            // Validate parameter format
            if (parm == null || value == null) {
                errormsg.append(Lang.adminParmsArgumentsPairs);
            } else {
                // Validate specific parameter values
                switch (parm.toLowerCase()) {
                case "gamemode":
                    this.gamemode = switch (value.toLowerCase()) {
                    case "strategy" -> GameMode.STRATEGY;
                    case "minigame" -> GameMode.MINIGAME;
                    default -> {
                        errormsg.append(Component.text("<< 'gamemode:' has to be either 'strategy' or 'minigame' >>"));
                        yield this.gamemode; // Keep existing value or set to a default
                    }
                    };
                    break;
                case "size":
                    if (!NumberUtils.isNumber(value)) {
                        errormsg.append(Component.text("<< 'size:' value must be a number >>"));
                    } else {
                        this.size = Integer.valueOf(value);
                    }
                    break;
                case "teams":
                    if (!NumberUtils.isNumber(value)) {
                        errormsg.append(Component.text("<< 'team:' value must be a number >>"));
                    }
                    this.teams = NumberUtils.toInt(value);
                    if (teams < 2 || teams > 14) {
                        errormsg.append(Component.text("<< 'team:' value must be between 2 and 14 >>"));
                    }
                    break;
                case "goal":
                    this.goal = switch (value.toLowerCase()) {
                    case "area" -> GameScoreGoal.AREA;
                    case "triangles" -> GameScoreGoal.TRIANGLES;
                    case "links" -> GameScoreGoal.LINKS;
                    default -> {
                        errormsg.append(Component.text("<< 'goal:' has to be one of 'area', 'beacons', 'links' or 'triangles' >>"));
                        yield this.goal; // Keep existing value or set to a default
                    }
                    };
                    break;
                case "goalvalue":
                    if (!NumberUtils.isNumber(value)) {
                        errormsg.append(Component.text("<< 'goalvalue:' value must be a number >>"));
                    }
                    int number2 = NumberUtils.toInt(value);
                    if (number2 < 0) {
                        errormsg.append(Component.text("<< 'goalvalue:' value cannot be negative >>"));
                    }
                    break;
                case "countdown":
                    if (!NumberUtils.isNumber(value)) {
                        errormsg.append(Component.text("<< 'countdown:' value must be a number >>"));
                    }
                    break;
                case "scoretypes":
                    Component stmsg = Component.text("<< 'scoretypes:' must be a list of the goals to display on the scoreboard, such as 'area-beacons'. Possible goals are 'area', 'beacons', 'links' and 'triangles' >>");
                    value = value.replace(" ", "");
                    String[] stypes = value.split("-");
                    if (stypes.length == 0) {
                        errormsg.append(stmsg);
                    } else {
                        for (String stype : stypes) {
                            this.scoretypes.add(
                                    switch (value.toLowerCase()) {
                                    case "area" -> GameScoreGoal.AREA;
                                    case "beacons" -> GameScoreGoal.BEACONS;
                                    case "triangles" -> GameScoreGoal.TRIANGLES;
                                    case "links" -> GameScoreGoal.LINKS;
                                    default -> {
                                        errormsg.append(stmsg);
                                        yield null;
                                    }
                                    });
                        }
                    }
                    break;
                    case "distribution":
                        if (!NumberUtils.isNumber(value)) {
                            errormsg.append(Component.text("<< 'distribution must be a number between 0.01 and 0.99 >>"));
                        }
                        this.distribution = NumberUtils.toDouble(value);
                        if (distribution == 0) {
                            errormsg.append(Component.text("<< 'distribution must be a number between 0.01 and 0.99 >>"));
                        }
                        break;
                    default:
                        errormsg.append(Lang.adminParmsDoesNotExist.replaceText("[name]", Component.text(parm)));
                        break;
                    }
                }
            }

            if (!errormsg.equals(Component.empty())) {
                String plainText = PlainTextComponentSerializer.plainText().serialize(errormsg);
                throw new IOException(plainText);
            }
        }
    }
