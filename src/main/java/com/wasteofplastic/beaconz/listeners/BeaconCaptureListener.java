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

package com.wasteofplastic.beaconz.listeners;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Settings;
import com.wasteofplastic.beaconz.map.BeaconMap;
import com.wasteofplastic.beaconz.map.TerritoryMapRenderer;

public class BeaconCaptureListener extends BeaconzPluginDependent implements Listener {

    private final static boolean DEBUG = false;

    public BeaconCaptureListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles damage to, but not breakage of a beacon. Warns players to clear a beacon before
     * capture can occur. See block break event for capture.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        // Only Ops can break or place blocks in the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }

        // Get the player's team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }

        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return;
        }
        // Check that the integrity of the beacon is sound
        beacon.checkIntegrity();
        // Check for obsidian/glass breakage - i.e., capture
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            // Check if this is a real beacon
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                // It is a real beacon
                // Check that the beacon is clear of blocks
                if (!beacon.isClear() && (beacon.getOwnership() == null || !beacon.getOwnership().equals(team))) {
                    // You can't capture an uncleared beacon
                    player.sendMessage(ChatColor.GOLD + Lang.errorClearAroundBeacon);
                    event.setCancelled(true);
                    return;
                }
            }
        } else {
            // Attempt to break another part of the beacon
        }

    }

    /**
     * Handle breakage of the top part of a beacon
     * @param event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconBreak(BlockBreakEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        if (DEBUG)
            getLogger().info("DEBUG: This is a beacon");
        Player player = event.getPlayer();

        // Only Ops can break blocks in the lobby
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return;
            } else {
                event.setCancelled(true);
                return;
            }
        }
        
        // Prevent breakage of blocks outside the game area
        Game game = getGameMgr().getGame(event.getBlock().getLocation());
        if (DEBUG) {
            if (game == null) {
                getLogger().info("DEBUG: game = null");
            } else {
                getLogger().info("DEBUG: game name = " + game.getName());
            }
        }
        if (game == null && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + Lang.errorYouCannotDoThat);
            return;
        }
        // Get the player's team
        Team team = null;
        if (game != null && game.getScorecard() != null) team = game.getScorecard().getTeam(player);
        if (team == null && !player.isOp()) {
            event.setCancelled(true);
            return;
        }

        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return;
        }
        // Cancel any breakage (if it IS a beacon)
        event.setCancelled(true);
        // Check for obsidian/glass breakage - i.e., capture
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            if (DEBUG)
                getLogger().info("DEBUG:beacon below");
            // Check if this is a real beacon
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                if (DEBUG)
                    getLogger().info("DEBUG: registered beacon");
                // It is a real beacon
                if (block.getType().equals(Material.OBSIDIAN)) {
                    // Check that the beacon is clear of blocks
                    if (!beacon.isClear()) {
                        // You can't capture an uncleared beacon
                        player.sendMessage(ChatColor.RED + Lang.errorClearAroundBeacon);
                        event.setCancelled(true);
                        return;
                    }
                    if (DEBUG) {
                        getLogger().info("DEBUG: obsidian");
                        //Claiming unowned beacon
                        getLogger().info("DEBUG: team = " + team);
                        getLogger().info("DEBUG: team = " + team.getDisplayName());
                        getLogger().info("DEBUG: block ID = " + game.getScorecard().getBlockID(team));
                    }
                    block.setType(game.getScorecard().getBlockID(team).getItemType());
                    block.setData(game.getScorecard().getBlockID(team).getData());
                    // Register the beacon to this team
                    getRegister().setBeaconOwner(beacon,team);
                    player.sendMessage(ChatColor.GREEN + Lang.beaconYouCapturedABeacon);
                    giveBeaconMap(player,beacon);
                    // Save for safety
                    getRegister().saveRegister();
                } else {
                    if (DEBUG)
                        getLogger().info("DEBUG: another block");
                    Team beaconTeam = beacon.getOwnership();
                    if (beaconTeam != null) {
                        if (DEBUG)
                            getLogger().info("DEBUG: known team block");
                        if (team.equals(beaconTeam)) {
                            // You can't destroy your own beacon
                            player.sendMessage(ChatColor.RED + Lang.beaconYouCannotDestroyYourOwnBeacon);
                            event.setCancelled(true);
                            return;
                        }
                        // Check that the beacon is clear of blocks
                        if (!beacon.isClear()) {
                            // You can't capture an uncleared beacon
                            player.sendMessage(ChatColor.RED + Lang.errorClearAroundBeacon);
                            event.setCancelled(true);
                            return;
                        }
                        // Enemy team has lost a beacon!
                        // Taunt other teams
                        getMessages().tellOtherTeams(team, ChatColor.RED + (Lang.beaconTeamDestroyed.replace("[team1]", team.getDisplayName()).replace("[team2]", beaconTeam.getDisplayName())));
                        getMessages().tellTeam(player, (Lang.beaconPlayerDestroyed.replace("[player]", player.getDisplayName()).replace("[team]", beaconTeam.getDisplayName())));
                        player.sendMessage(ChatColor.GREEN + Lang.beaconYouDestroyed.replace("[team]", beaconTeam.getDisplayName()));
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, 1F, 1F);
                        getRegister().removeBeaconOwnership(beacon);
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);
                        // Remove any standers
                        if (BeaconProtectionListener.getStandingOn().containsValue(beacon)) {
                            BeaconProtectionListener.getStandingOn().inverse().remove(beacon);
                        }
                    } else {
                        getRegister().removeBeaconOwnership(beacon);
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);
                        if (DEBUG)
                            getLogger().info("DEBUG: unknown team block");
                    }
                }
            }
        } else {
            // Attempt to break another part of the beacon
            // Only do on owned beacons
            if (beacon.getOwnership() != null) {
                // Check for cool down, if it's still cooling down, don't do anything
                if (beacon.isNewBeacon() || System.currentTimeMillis() > beacon.getHackTimer() + Settings.mineCoolDown) {
                    // Give something to the player if they have enough experience
                    // Remove experience
                    if (DEBUG)
                        getLogger().info("DEBUG: player has " + player.getTotalExperience() + " and needs " + Settings.beaconMineExpRequired);
                    if (!BeaconLinkListener.testForExp(player, Settings.beaconMineExpRequired)) {
                        player.sendMessage(ChatColor.RED + Lang.errorNotEnoughExperience);
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1F, 1F);
                        return;
                    }
                    Random rand = new Random();
                    if (beacon.getOwnership().equals(team)) {
                        // Own team
                        //getLogger().info("DEBUG: own team");
                        /*
                         * DEBUG code
                        for (Entry<Integer, ItemStack> ent : Settings.teamGoodies.entrySet()) {
                            getLogger().info("DEBUG: " + ent.getKey() + " " + ent.getValue());
                        }*/
                        int value = rand.nextInt(Settings.teamGoodies.lastKey()) + 1;
                        //getLogger().info("DEBUG: value = " + value);
                        Entry<Integer, ItemStack> en = Settings.teamGoodies.ceilingEntry(value);
                        //getLogger().info("DEBUG: en = " + en);
                        if (en != null && en.getValue() != null) {
                            if (en.getValue().getType().equals(Material.MAP)) {
                                giveBeaconMap(player,beacon);                                
                            } else {
                                player.getWorld().dropItem(event.getPlayer().getLocation(), en.getValue());
                                if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                    beacon.resetHackTimer();
                                    player.sendMessage(ChatColor.GREEN + Lang.generalSuccess + " " + Lang.beaconIsExhausted.replace("[minutes]", String.valueOf(Settings.mineCoolDown/60000)));
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDERCHEST_CLOSE, 1F, 1F);
                                } else {
                                    player.sendMessage(ChatColor.GREEN + Lang.generalSuccess);
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                                }
                            }
                            // Remove exp
                            BeaconLinkListener.removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            player.sendMessage(ChatColor.RED + Lang.generalFailure);
                        }
                    } else {
                        // Enemy
                        int value = rand.nextInt(Settings.enemyGoodies.lastKey()) + 1;
                        Entry<Integer, ItemStack> en = Settings.enemyGoodies.ceilingEntry(value);
                        if (en != null && en.getValue() != null) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());
                            if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                beacon.resetHackTimer();
                                player.sendMessage(ChatColor.GREEN + Lang.generalSuccess + Lang.beaconIsExhausted.replace("[minutes]", String.valueOf(Settings.mineCoolDown/60000)));
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENDERCHEST_CLOSE, 1F, 1F);
                            } else {
                                player.sendMessage(ChatColor.GREEN + Lang.generalSuccess);
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                            }
                            // Remove exp
                            BeaconLinkListener.removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            player.getWorld().spawnEntity(player.getLocation(),EntityType.ENDERMITE);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMITE_AMBIENT, 1F, 1F);
                            player.sendMessage(ChatColor.RED + Lang.generalFailure + " Watch out!");
                        }
                    }
                } else {
                    // Damage player
                    int num = (int) (beacon.getHackTimer() + Settings.mineCoolDown - System.currentTimeMillis())/50;
                    for (String effect : Settings.minePenalty) {
                        String[] split = effect.split(":");
                        if (split.length == 2) {
                            int amplifier = 1;
                            if (NumberUtils.isNumber(split[1])) {
                                amplifier = Integer.valueOf(split[1]);
                                if (DEBUG)
                                    getLogger().info("DEBUG: Amplifier is " + amplifier);
                            }
                            PotionEffectType potionEffectType = PotionEffectType.getByName(split[0]);
                            if (potionEffectType != null) {
                                player.addPotionEffect(new PotionEffect(potionEffectType, num,amplifier));
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1F, 1F);
                                if (DEBUG)
                                    getLogger().info("DEBUG: Applying " + potionEffectType.toString() + ":" + amplifier + " for " + num + " ticks");
                            }
                        } else {
                            getLogger().warning("Unknown hack cooldown effect" + effect);
                        }


                    }
                }
            }
        }
    }

    /**
     * Puts a beacon map in the player's main hand
     * @param player
     * @param beacon
     */
    @SuppressWarnings("deprecation")
    private void giveBeaconMap(Player player, BeaconObj beacon) {
        // Make a map!
        player.sendMessage(ChatColor.GREEN + Lang.beaconYouHaveAMap);
        MapView map = Bukkit.createMap(getBeaconzWorld());
        //map.setWorld(getBeaconzWorld());
        map.setCenterX(beacon.getX());
        map.setCenterZ(beacon.getZ());
        map.getRenderers().clear();
        map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
        map.addRenderer(new BeaconMap(getBeaconzPlugin()));
        ItemStack newMap = new ItemStack(Material.MAP);
        newMap.setDurability(map.getId());
        ItemMeta meta = newMap.getItemMeta();
        meta.setDisplayName("Beacon map for " + beacon.getName());
        newMap.setItemMeta(meta);
        // Each map is unique and the durability defines the map ID, register it
        getRegister().addBeaconMap(map.getId(), beacon);
        //getLogger().info("DEBUG: beacon id = " + beacon.getId());
        // Put map into hand
        //ItemStack inHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        player.getInventory().setItemInOffHand(newMap);
        //player.getInventory().setItemInOffHand(inHand);
        if (offHand != null && !offHand.getType().equals(Material.AIR)) {
            HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(offHand);
            if (!leftOvers.isEmpty()) {
                player.sendMessage(ChatColor.RED + Lang.errorInventoryFull);
                for (ItemStack item: leftOvers.values()) {
                    player.getWorld().dropItem(player.getLocation(), item);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 0.5F);
                }
            }
        }
    }

}
