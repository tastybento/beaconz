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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class AdminCmdHandler extends BeaconzPluginDependent implements CommandExecutor {

    public AdminCmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: Make this a permission
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You must be Op to use this command");
            return true;
        }
        switch (args.length) {
        // Just the badmin command
        default:
        case 0:
            sender.sendMessage(ChatColor.GREEN + "Beaconz Admin Command");
            sender.sendMessage("/" + label + " reload - reloads the plugin settings");
            sender.sendMessage("/" + label + " setspawn [" + getScorecard().getTeamListString() +"] - sets the spawn point for team");
            sender.sendMessage("/" + label + " newgame - forces a new game to start (mini game only)");
            sender.sendMessage("/" + label + " distribution <fraction> - sets the distribution of beacons temporarily");
            sender.sendMessage("/" + label + " join [" + getScorecard().getTeamListString() +"] - join a team");
            sender.sendMessage("/" + label + " list - lists all the known beacons");
            sender.sendMessage("/" + label + " claim [unowned, " + getScorecard().getTeamListString() +"] - force-claims a beacon");
            sender.sendMessage("/" + label + " link x z - force-links a beacon you are standing on to one at x,z");
            return true;
        case 1:
            // Reload
            // TODO: This is probably not complete
            if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(ChatColor.GREEN + "Reloaded settings.");
                getRegister().saveRegister();
                getScorecard().saveTeamMembers();
                getBeaconzWorld().getWorldBorder().reset();  
                // Load config
                this.getBeaconzPlugin().loadConfig();
                // Set the world border
                this.getBeaconzPlugin().setWorldBorder();
                return true;
            } 
            // New game
            if (args[0].equalsIgnoreCase("newgame")) {
                sender.sendMessage(ChatColor.GREEN + "Building a new game - it will start shortly...");
                getBeaconzPlugin().newGame();
                return true;
            }            
            // Join team
            if (args[0].equalsIgnoreCase("join")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only available to players");
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "/" + label + " join [" + getScorecard().getTeamListString() + "]");
                return true;                
            }
            // List all known beaconz
            if (args[0].equalsIgnoreCase("list")) {
                // list known beacons
                sender.sendMessage("Known beacons:");
                int count = 0;
                for (BeaconObj p : getRegister().getBeaconRegister().values()) {
                    count++;
                    sender.sendMessage(p.getLocation().toString() + " Owner:" + (p.getOwnership() == null ? "unowned":p.getOwnership().getDisplayName()) + " Links " + p.getLinks().size());
                }
                if (count == 0) {
                    sender.sendMessage("None");
                }
                return true;
            }

            // Claim beacon
            if (args[0].equalsIgnoreCase("claim")) {
                sender.sendMessage(ChatColor.RED + "/" + label + " claim [unowned, " + getScorecard().getTeamListString() + "]");
                return true;
            }
            // Admin set team spawn with no team
            if (args[0].equalsIgnoreCase("setspawn")) {
                sender.sendMessage(ChatColor.RED + "/" + label + " setspawn [" + getScorecard().getTeamListString() +"] - sets the spawn point for team");
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You cannot execute this command from the console");
                }
                return true; 
            }
            break;
        case 2:
            // Admin set team spawn
            if (args[0].equalsIgnoreCase("setspawn")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You cannot execute this command from the console");
                    return true;
                }
                // Check team name given exists
                Team team = getScorecard().getTeam(args[1]);
                if (team == null) {
                    sender.sendMessage(ChatColor.RED + "That team does not exist! Use " + getScorecard().getTeamListString());
                    return true;
                }
                getScorecard().setTeamSpawnPoint(team, ((Player)sender).getLocation());
                sender.sendMessage(ChatColor.GREEN + "Setting " + team.getDisplayName() + "'s spawn point to your location!");
                return true;
            }
            if (args[0].equalsIgnoreCase("distribution")) {
                try {
                    double dist = Double.valueOf(args[1]);
                    if (dist > 0D && dist < 1D) {
                        Settings.distribution = dist;
                        sender.sendMessage(ChatColor.GREEN + "Setting beacon distribution to " + dist);
                        return true;
                    }
                } catch (Exception e) {}

                sender.sendMessage(ChatColor.RED + label + " distribution <fraction> - must be less than 1");
                return true;
            }

            // Join team
            if (args[0].equalsIgnoreCase("join")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only available to players");
                    return true;
                }
                // Check if this is a known team name
                Team team = getScorecard().getTeam(args[1]);
                if (team == null) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " join [" + getScorecard().getTeamListString() + "]");
                    return true;
                }
                team.addPlayer((Player)sender);
                ((Player)sender).setScoreboard(getScorecard().getScoreboard());
                sender.sendMessage(ChatColor.GREEN + "You joined " + team.getDisplayName());
                return true;
            }

            // Claim a beacon for a team
            if (args[0].equalsIgnoreCase("claim")) {
                // See if the player is on a beacon
                if (sender instanceof Player) {
                    Team team = null;
                    if (!args[1].equalsIgnoreCase("unowned")) {
                        team = getScorecard().getTeam(args[1]);
                        if (team == null) {
                            sender.sendMessage(ChatColor.RED + "/" + label + " claim [unowned, " + getScorecard().getTeamListString() + "]");
                            return true;
                        }
                    }
                    Player p = (Player)sender;
                    Point2D newClaim = new Point2D.Double(p.getLocation().getBlockX(),p.getLocation().getBlockZ());
                    p.sendMessage("Claiming beacon at " + newClaim);
                    if (getRegister().getBeaconRegister().containsKey(newClaim)) {
                        // Claim a beacon
                        getRegister().getBeaconRegister().get(newClaim).setOwnership(team);
                        p.sendMessage(ChatColor.GREEN + "Set ownership to " + args[1]);
                    } else {
                        p.sendMessage(ChatColor.RED + "You are not standing on a beacon");
                    }
                } else {
                    sender.sendMessage("Only players can claim beacons");
                }
                return true;
            }
            break;
        }
        return true;
    }
}
