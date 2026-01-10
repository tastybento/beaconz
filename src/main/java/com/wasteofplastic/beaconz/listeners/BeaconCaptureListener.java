/*
 * Copyright (c) 2015 - 2025 tastybento
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
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
            getLogger().info("DEBUG: " + event.getEventName() + " BeaconCaptureListener");
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
                if (beacon.isNotClear() && (beacon.getOwnership() == null || !beacon.getOwnership().equals(team))) {
                    // You can't capture an uncleared beacon
                    player.sendMessage(Lang.errorClearAroundBeacon.color(NamedTextColor.GOLD));
                    event.setCancelled(true);
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
            player.sendMessage(Lang.errorYouCannotDoThat.color(NamedTextColor.RED));
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
                    if (beacon.isNotClear()) {
                        // You can't capture an uncleared beacon
                        player.sendMessage(Lang.errorClearAroundBeacon.color(NamedTextColor.RED));
                        event.setCancelled(true);
                        return;
                    }
                    if (DEBUG) {
                        getLogger().info("DEBUG: obsidian");
                        //Claiming unowned beacon
                        getLogger().info("DEBUG: team = " + team);
                        getLogger().info("DEBUG: team = " + team.displayName().toString());
                        getLogger().info("DEBUG: block ID = " + game.getScorecard().getBlockID(team));
                    }
                    if (game != null) {
                        block.setType(game.getScorecard().getBlockID(team));
                    }
                    // TODO block.setData(game.getScorecard().getBlockID(team).getData());
                    // Register the beacon to this team
                    getRegister().setBeaconOwner(beacon,team);
                    player.sendMessage(Lang.beaconYouCapturedABeacon.color(NamedTextColor.GREEN));
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
                            player.sendMessage(Lang.beaconYouCannotDestroyYourOwnBeacon.color(NamedTextColor.RED));
                            event.setCancelled(true);
                            return;
                        }
                        // Check that the beacon is clear of blocks
                        if (beacon.isNotClear()) {
                            // You can't capture an uncleared beacon
                            player.sendMessage(Lang.errorClearAroundBeacon.color(NamedTextColor.GREEN));
                            event.setCancelled(true);
                            return;
                        }
                        // Enemy team has lost a beacon!
                        // Taunt other teams
                        getMessages().tellOtherTeams(team, Lang.beaconTeamDestroyed.replaceText("[team1]", team.displayName())
                                .replaceText("[team2]", beaconTeam.displayName()).color(NamedTextColor.RED));
                        getMessages().tellTeam(player, Lang.beaconPlayerDestroyed.replaceText("[player]", player.displayName()).replaceText("[team]", beaconTeam.displayName()));
                        player.sendMessage(Lang.beaconYouDestroyed.replaceText("[team]", beaconTeam.displayName()).color(NamedTextColor.GREEN));
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
                    if (BeaconLinkListener.testForExp(player, Settings.beaconMineExpRequired)) {
                        player.sendMessage(Lang.errorNotEnoughExperience.color(NamedTextColor.RED));
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
                            if (en.getValue().getType().equals(Material.FILLED_MAP)) {
                                giveBeaconMap(player,beacon);                                
                            } else {
                                player.getWorld().dropItem(event.getPlayer().getLocation(), en.getValue());
                                if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                    beacon.resetHackTimer();
                                    player.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN).append(Component.text(" ").append(Lang.beaconIsExhausted.replaceText("[minutes]", Component.text(String.valueOf(Settings.mineCoolDown/60000))))));
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1F, 1F);
                                } else {
                                    player.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN));
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                                }
                            }
                            // Remove exp
                            BeaconLinkListener.removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            player.sendMessage(Lang.generalFailure.color(NamedTextColor.RED));
                        }
                    } else {
                        // Enemy
                        int value = rand.nextInt(Settings.enemyGoodies.lastKey()) + 1;
                        Entry<Integer, ItemStack> en = Settings.enemyGoodies.ceilingEntry(value);
                        if (en != null && en.getValue() != null) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());
                            if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                beacon.resetHackTimer();
                                player.sendMessage(Lang.generalSuccess.append(Lang.beaconIsExhausted.replaceText("[minutes]", Component.text(String.valueOf(Settings.mineCoolDown/60000)))));
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1F, 1F);
                            } else {
                                player.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN));
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                            }
                            // Remove exp
                            BeaconLinkListener.removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            player.getWorld().spawnEntity(player.getLocation(),EntityType.ENDERMITE);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMITE_AMBIENT, 1F, 1F);
                            player.sendMessage(Lang.generalFailure.append(Component.text(" Watch out!")).color(NamedTextColor.RED));
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
                                amplifier = Integer.parseInt(split[1]);
                                if (DEBUG)
                                    getLogger().info("DEBUG: Amplifier is " + amplifier);
                            }
                            PotionEffectType potionEffectType = PotionEffectType.getByName(split[0]);
                            if (potionEffectType != null) {
                                player.addPotionEffect(new PotionEffect(potionEffectType, num,amplifier));
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1F, 1F);
                                if (DEBUG)
                                    getLogger().info("DEBUG: Applying " + potionEffectType + ":" + amplifier + " for " + num + " ticks");
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
        // Create the MapView
        MapView mapView = Bukkit.createMap(getBeaconzWorld());
        mapView.setCenterX(beacon.getX());
        mapView.setCenterZ(beacon.getZ());
        mapView.setScale(Scale.NORMAL);
        mapView.getRenderers().clear();
        mapView.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
        mapView.addRenderer(new BeaconMap(getBeaconzPlugin()));

        // Use FILLED_MAP, not MAP
        ItemStack newMap = new ItemStack(Material.FILLED_MAP);

        // Update the Meta
        if (newMap.getItemMeta() instanceof MapMeta meta) {
            meta.displayName(Component.text("Beacon map for " + beacon.getName()));

            // This connects the ItemStack to the custom MapView and ID
            meta.setMapView(mapView); 

            newMap.setItemMeta(meta);
        }

        // Give to player
        ItemStack offHand = player.getInventory().getItemInOffHand();
        getLogger().info("offhand = " + offHand);

        if (!offHand.getType().equals(Material.AIR)) {
            HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(newMap);
            if (!leftOvers.isEmpty()) {
                player.sendMessage(Lang.errorInventoryFull.color(NamedTextColor.RED));
                for (ItemStack item: leftOvers.values()) {
                    player.getWorld().dropItem(player.getLocation(), item);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 0.5F);
                }
            }
        } else {
            player.getInventory().setItemInOffHand(newMap);
        }

        // Register in system
        getRegister().addBeaconMap(mapView.getId(), beacon);

    }
}
