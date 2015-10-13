package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class CmdHandler implements CommandExecutor {

    private Beaconz plugin;



    /**
     * @param plugin
     */
    public CmdHandler(Beaconz plugin) {
	this.plugin = plugin;
    }



    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	// Test commands
	// Join team
	if (args[0].equalsIgnoreCase("join")) {
	    if (!(sender instanceof Player)) {
		sender.sendMessage("Only available to players");
		return true;
	    }
	    if (args.length != 2) {
		sender.sendMessage(ChatColor.RED + "/" + label + " join [" + plugin.getScorecard().listTeams() + "]");
		return true;
	    }
	    // Check if this is a known team name
	    Team team = plugin.getScorecard().getTeam(args[1]);
	    if (team == null) {
		sender.sendMessage(ChatColor.RED + "/" + label + " join [" + plugin.getScorecard().listTeams() + "]");
		return true;
	    }
	    team.addPlayer((Player)sender);
	    sender.sendMessage(ChatColor.GREEN + "You joined " + team.getDisplayName());
	    return true;
	}
	if (args[0].equalsIgnoreCase("list")) {
	    // list known beacons
	    sender.sendMessage("Known beacons:");
	    int count = 0;
	    for (BeaconObj p : plugin.getRegister().getBeaconRegister().values()) {
		count++;
		sender.sendMessage(p.getLocation().toString() + " Owner:" + (p.getOwnership() == null ? "unowned":p.getOwnership().getDisplayName()) + " Links " + p.getLinks().size());
	    }
	    if (count == 0) {
		sender.sendMessage("None");
	    }
	    return true;
	}
	if (args[0].equalsIgnoreCase("score")) {
	    // list known beacons
	    sender.sendMessage("Scores:");
	    for (Team faction : plugin.getRegister().getScore().keySet()) {
		sender.sendMessage(faction.getDisplayName() + " :" + plugin.getRegister().getScore().get(faction) + " blocks");
	    }
	    return true;
	}
	if (args[0].equalsIgnoreCase("claim")) {
	    // See if the player is on a beacon
	    if (sender instanceof Player) {
		if (args.length != 2) {
		    sender.sendMessage(ChatColor.RED + "claim UNOWNED/RED/BLUE");
		    return true;
		}
		Team team = null;
		if (!args[1].equalsIgnoreCase("unowned")) {
		    team = plugin.getScorecard().getTeam(args[1]);
		    if (team == null) {
			sender.sendMessage(ChatColor.RED + "claim UNOWNED/RED/BLUE");
			return true;
		    }
		}
		Player p = (Player)sender;
		Point2D newClaim = new Point2D.Double(p.getLocation().getBlockX(),p.getLocation().getBlockZ());
		p.sendMessage("Claiming beacon at " + newClaim);
		if (plugin.getRegister().getBeaconRegister().containsKey(newClaim)) {   
		    // Claim a beacon
		    plugin.getRegister().getBeaconRegister().get(newClaim).setOwnership(team);
		    p.sendMessage(ChatColor.GREEN + "Set ownership to " + args[1]);
		} else {
		    p.sendMessage(ChatColor.RED + "You are not standing on a beacon");
		}
	    } else {
		sender.sendMessage("Only players can claim beacons");
	    }
	    return true;
	}
	if (args[0].equalsIgnoreCase("link")) {
	    if (!(sender instanceof Player)) {
		sender.sendMessage("Only players can link beacons");
	    }
	    // Link a beacon to another beacon
	    if (args.length != 3) {
		sender.sendMessage(ChatColor.RED + "link x z");
		return true;
	    }
	    Player p = (Player)sender;
	    Point2D from = new Point2D.Double(p.getLocation().getBlockX(), p.getLocation().getBlockZ());
	    if (plugin.getRegister().getBeaconRegister().containsKey(from)) {
		BeaconObj start = plugin.getRegister().getBeaconRegister().get(from);
		if (start.getOutgoing() == 8) {
		    sender.sendMessage(ChatColor.RED + "beacon has 8 outbound links already!");
		    return true;
		}
		Point2D to = new Point2D.Double(Integer.valueOf(args[1]), Integer.valueOf(args[2]));
		if (to.equals(from)) {
		    p.sendMessage("You cannot link a beacon to itself!");
		    return true;
		}
		p.sendMessage("Linking beacon from " + from.getX() + " " + from.getY() + " to " + to.getX() + " " + to.getY());
		if (plugin.getRegister().getBeaconRegister().containsKey(to)) {
		    // Link a beacon

		    BeaconObj end = plugin.getRegister().getBeaconRegister().get(to);
		    boolean result = start.addOutboundLink(end);
		    p.sendMessage(ChatColor.GREEN + "Link created!");
		    p.sendMessage(ChatColor.GREEN + "This beacon now has " + start.getOutgoing() + " links");
		    p.sendMessage(ChatColor.GREEN +  plugin.getRegister().getBeaconRegister().get(from).getLinks().toString());
		    if (result) {
			p.sendMessage(ChatColor.GREEN + "Control field created! New score = " + plugin.getRegister().getScore());
		    }
		} else {
		    p.sendMessage(ChatColor.RED + "Destination is not a beacon");
		}
	    } else {
		p.sendMessage(ChatColor.RED + "You are not standing on a beacon");
	    }
	}
	return true;
    }

}
