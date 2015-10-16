package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
import java.util.Random;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.material.TrapDoor;
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
	if (args[0].equalsIgnoreCase("distribution")) {
	    if (args.length == 2) {
		try {
		    double dist = Double.valueOf(args[1]);
		    if (dist > 0D && dist < 1D) {
			Settings.distribution = dist;
			sender.sendMessage(ChatColor.GREEN + "Setting beacon distribution to " + dist);
			return true;
		    }
		} catch (Exception e) {}
	    }
	    sender.sendMessage(ChatColor.RED + label + "distribution <fraction> - must be less than 1");
	    return true;  
	} else
	if (args[0].equalsIgnoreCase("go")) {
	    if (!(sender instanceof Player)) {
		sender.sendMessage("Only available to players");
		return true;
	    }
	    Player player = (Player)sender;
	    if (!player.getWorld().equals(Beaconz.getBeaconzWorld())) {
		player.sendMessage(ChatColor.GREEN + "Teleporting you to the world...");
		Location teleportTo = Beaconz.getBeaconzWorld().getSpawnLocation();
		boolean found = false;
		if (Settings.randomSpawn) {
		    Random rand = new Random();
		    int range = Settings.size > 0 ? (Settings.size/2):50000;
		    do {
			int x = rand.nextInt(range);
			x = rand.nextBoolean() ? x: -x;
			x = x + Settings.xCenter;
			int z = rand.nextInt(range);
			z = rand.nextBoolean() ? z: -z;
			z = z + Settings.zCenter;
			//teleportTo = plugin.getBeaconzWorld().getHighestBlockAt(x, z).getLocation();
			//teleportTo.getChunk().load();
			// Seach the chunk in this area
			ChunkSnapshot searchChunk = Beaconz.getBeaconzWorld().getChunkAt(x/16, z/16).getChunkSnapshot();
			for (int xx = 0; xx < 16; xx++) {
			    for (int zz = 0; zz < 16; zz++) {
				teleportTo = Beaconz.beaconzWorld.getBlockAt(x + xx, searchChunk.getHighestBlockYAt(xx, zz), z +zz).getLocation();
				if (isSafeLocation(teleportTo)) {
				    found = true;
				    break;
				}
			    }
			}
		    } while (!found);
		}
		player.teleport(teleportTo);
		if (plugin.getScorecard().getTeam(player) == null) {
		    Random rand = new Random();
		    Set<Team> teams = plugin.getScorecard().getScoreboard().getTeams();
		    int r = rand.nextInt(teams.size());
		    for (Team t: teams) {
			if (r-- == 0) {
			    t.addPlayer(player);
			    player.sendMessage("You are now a member of " + t.getDisplayName() + " team!");
			    break;
			}
		    }    
		}
		return true;
	    }
	    return true;
	} else
	    // Join team
	    if (args[0].equalsIgnoreCase("join")) {
		if (!(sender instanceof Player)) {
		    sender.sendMessage("Only available to players");
		    return true;
		}
		if (args.length != 2) {
		    sender.sendMessage(ChatColor.RED + "/" + label + " join [" + plugin.getScorecard().getTeamListString() + "]");
		    return true;
		}
		// Check if this is a known team name
		Team team = plugin.getScorecard().getTeam(args[1]);
		if (team == null) {
		    sender.sendMessage(ChatColor.RED + "/" + label + " join [" + plugin.getScorecard().getTeamListString() + "]");
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

    /**
     * Checks if this location is safe for a player to teleport to. Used by
     * warps and boat exits Unsafe is any liquid or air and also if there's no
     * space
     * 
     * @param l
     *            - Location to be checked
     * @return true if safe, otherwise false
     */
    public static boolean isSafeLocation(final Location l) {
	if (l == null) {
	    return false;
	}
	// TODO: improve the safe location finding.
	//Bukkit.getLogger().info("DEBUG: " + l.toString());
	final Block ground = l.getBlock().getRelative(BlockFace.DOWN);
	final Block space1 = l.getBlock();
	final Block space2 = l.getBlock().getRelative(BlockFace.UP);
	//Bukkit.getLogger().info("DEBUG: ground = " + ground.getType());
	//Bukkit.getLogger().info("DEBUG: space 1 = " + space1.getType());
	//Bukkit.getLogger().info("DEBUG: space 2 = " + space2.getType());
	// Portals are not "safe"
	if (space1.getType() == Material.PORTAL || ground.getType() == Material.PORTAL || space2.getType() == Material.PORTAL
		|| space1.getType() == Material.ENDER_PORTAL || ground.getType() == Material.ENDER_PORTAL || space2.getType() == Material.ENDER_PORTAL) {
	    return false;
	}
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
	MaterialData md = ground.getState().getData();
	if (md instanceof SimpleAttachableMaterialData) {
	    //Bukkit.getLogger().info("DEBUG: trapdoor/button/tripwire hook etc.");
	    if (md instanceof TrapDoor) {
		TrapDoor trapDoor = (TrapDoor)md;
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
	// Check that the space is not solid
	// The isSolid function is not fully accurate (yet) so we have to
	// check
	// a few other items
	// isSolid thinks that PLATEs and SIGNS are solid, but they are not
	if (space1.getType().isSolid() && !space1.getType().equals(Material.SIGN_POST) && !space1.getType().equals(Material.WALL_SIGN)) {
	    return false;
	}
	if (space2.getType().isSolid()&& !space2.getType().equals(Material.SIGN_POST) && !space2.getType().equals(Material.WALL_SIGN)) {
	    return false;
	}
	// Safe
	//Bukkit.getLogger().info("DEBUG: safe!");
	return true;
    }
}
