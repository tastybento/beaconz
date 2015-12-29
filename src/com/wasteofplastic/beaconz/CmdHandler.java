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

import java.util.HashMap;
import java.util.Random;

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
import org.bukkit.inventory.ItemStack;
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
        switch (args.length) {
        // Just the beaconz command
        case 0:
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only available to players");
                return true;
            }
            Player player = (Player)sender;

            player.sendMessage(ChatColor.GREEN + "Teleporting you to the world...");
            boolean newPlayer = !getScorecard().inTeam(player);
            // Set the team
            Team team = getScorecard().getTeam(player);
            player.sendMessage("You are now a member of " + team.getDisplayName() + " team!");
            getBeaconzPlugin().getServer().dispatchCommand(getBeaconzPlugin().getServer().getConsoleSender(),
                    "title " + player.getName() + " title {text:\"" + team.getDisplayName() + " team!\", color:gold}");
            // Teleport teams to different locations
            Location teleportTo = getScorecard().getTeamSpawnPoint(team);
            boolean found = false;
            if (Settings.randomSpawn) {
                Random rand = new Random();
                int range = Settings.borderSize > 0 ? (Settings.borderSize/2):50000;
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
            // Give newbie kit
            if (newPlayer) {
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
            return true;
        // One argument after the beaconz command
        case 1:
            // Help command
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("/" + label + " help - this help");
                sender.sendMessage("/" + label + " - teleport to the beaconz world and join a team");
                sender.sendMessage("/" + label + " score - show the team scores");
            } else if (args[0].equalsIgnoreCase("score")) {
                // list known beacons
                sender.sendMessage(ChatColor.AQUA + "Scores:");
                for (Team t : getScorecard().getScoreboard().getTeams()) {
                    sender.sendMessage(ChatColor.AQUA + t.getDisplayName() + ChatColor.AQUA + ": " + getScorecard().getScore(t) + " blocks");
                }
                return true;
            }
            break;
            // More than one argument
        default:
            sender.sendMessage(ChatColor.RED + "Error - unknown command. Do /" + label + " help");
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
