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

public class CmdHandler extends BeaconzPluginDependent implements CommandExecutor {

    public CmdHandler(Beaconz beaconzPlugin) {
        super(beaconzPlugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Test commands
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("/" + label + " help - this help");
            sender.sendMessage("/" + label + " - teleport to the beaconz world and join a team");
            sender.sendMessage("/" + label + " distribution <fraction> - sets the distribution of beacons temporarily");
            sender.sendMessage("/" + label + " join <team name> - join a team (red or blue)");
            sender.sendMessage("/" + label + " list - lists all the known beacons");
            sender.sendMessage("/" + label + " score - lists the score");
            sender.sendMessage("/" + label + " claim UNOWNED/RED/BLUE - force-claims a beacon");
            sender.sendMessage("/" + label + " link x z - force-links a beacon you are standing on to one at x,z");
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("go")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only available to players");
                return true;
            }
            Player player = (Player)sender;

            player.sendMessage(ChatColor.GREEN + "Teleporting you to the world...");
            // Teleport teams to different locations
            Location teleportTo = getBeaconzWorld().getSpawnLocation();
            BlockFace blockFace = BlockFace.NORTH;
            Set<Team> teams = getScorecard().getScoreboard().getTeams();
            // We allow up to 8 teams
            int direction = 0;
            for (Team team : teams) {
                if (team.equals(getScorecard().getTeam(player))) {
                    switch (direction) {
                    case 0:
                        blockFace = BlockFace.NORTH;
                        break;
                    case 1:
                        blockFace = BlockFace.SOUTH;
                        break;
                    case 2:
                        blockFace = BlockFace.EAST;
                        break;
                    case 3:
                        blockFace = BlockFace.WEST;
                        break;
                    case 4:
                        blockFace = BlockFace.NORTH_EAST;
                        break;
                    case 5:
                        blockFace = BlockFace.NORTH_WEST;
                        break;
                    case 6:
                        blockFace = BlockFace.SOUTH_EAST;
                        break;
                    case 7:
                        blockFace = BlockFace.SOUTH_WEST;
                        break;
                    }
                }
                direction++;
            }
            teleportTo = teleportTo.getBlock().getRelative(blockFace, Settings.size / 4).getLocation();
            teleportTo = getBeaconzWorld().getHighestBlockAt(teleportTo).getLocation().add(0.5, 0, 0.5);
            teleportTo.getBlock().getRelative(BlockFace.DOWN).setType(Material.BEDROCK);
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
                    //teleportTo = getBeaconzWorld().getHighestBlockAt(x, z).getLocation();
                    //teleportTo.getChunk().load();
                    // Seach the chunk in this area
                    ChunkSnapshot searchChunk = getBeaconzWorld().getChunkAt(x/16, z/16).getChunkSnapshot();
                    for (int xx = 0; xx < 16; xx++) {
                        for (int zz = 0; zz < 16; zz++) {
                            teleportTo = getBeaconzWorld().getBlockAt(x + xx, searchChunk.getHighestBlockYAt(xx, zz), z +zz).getLocation();
                            if (isSafeLocation(teleportTo)) {
                                found = true;
                                break;
                            }
                        }
                    }
                } while (!found);
            }
            player.teleport(teleportTo);
            if (getScorecard().getTeam(player) == null) {
                Random rand = new Random();
                teams = getScorecard().getScoreboard().getTeams();
                int r = rand.nextInt(teams.size());
                for (Team t: teams) {
                    if (r-- == 0) {
                        t.addPlayer(player);
                        player.sendMessage("You are now a member of " + t.getDisplayName() + " team!");
                        getBeaconzPlugin().getServer().dispatchCommand(getBeaconzPlugin().getServer().getConsoleSender(),
                                "title " + player.getName() + " title {text:\"" + t.getDisplayName() + " team!\", color:gold}");
                        break;
                    }
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("distribution")) {
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
            // Join team
            if (args[0].equalsIgnoreCase("join")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only available to players");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " join [" + getScorecard().getTeamListString() + "]");
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
        if (args[0].equalsIgnoreCase("score")) {
            // list known beacons
            sender.sendMessage("Scores:");
            for (Team faction : getRegister().getScore().keySet()) {
                sender.sendMessage(faction.getDisplayName() + " :" + getRegister().getScore().get(faction) + " blocks");
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
                    team = getScorecard().getTeam(args[1]);
                    if (team == null) {
                        sender.sendMessage(ChatColor.RED + "claim UNOWNED/RED/BLUE");
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
        return true;
    }

    /**
     * Checks if this location is safe for a player to teleport to. Used by
     * warps and boat exits Unsafe is any liquid or air and also if there's no
     * space
     * 
     * @param location
     *            - Location to be checked
     * @return true if safe, otherwise false
     */
    public static boolean isSafeLocation(final Location location) {
        if (location == null) {
            return false;
        }
        // TODO: improve the safe location finding.
        //Bukkit.getLogger().info("DEBUG: " + l.toString());
        final Block ground = location.getBlock().getRelative(BlockFace.DOWN);
        final Block space1 = location.getBlock();
        final Block space2 = location.getBlock().getRelative(BlockFace.UP);
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
        MaterialData materialData = ground.getState().getData();
        if (materialData instanceof SimpleAttachableMaterialData) {
            //Bukkit.getLogger().info("DEBUG: trapdoor/button/tripwire hook etc.");
            if (materialData instanceof TrapDoor) {
                TrapDoor trapDoor = (TrapDoor)materialData;
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
