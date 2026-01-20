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

/**
 * Listener class that handles all beacon capture, destruction, and mining mechanics.
 * <p>
 * This class manages the core gameplay loop of the Beaconz plugin by handling:
 * <ul>
 *   <li>Beacon capture - claiming unowned beacons by breaking the obsidian capture block</li>
 *   <li>Beacon destruction - destroying enemy beacons to reset them to unclaimed state</li>
 *   <li>Beacon mining - extracting resources from beacons by breaking pyramid blocks</li>
 *   <li>Cooldown enforcement - preventing abuse through hack/mine cooldown timers</li>
 *   <li>Experience requirements - ensuring players have sufficient XP for mining</li>
 *   <li>Reward distribution - giving items based on team ownership and luck</li>
 *   <li>Beacon map creation - providing players with custom maps centered on captured beacons</li>
 * </ul>
 *
 * The capture system works as follows:
 * <ol>
 *   <li>Players must clear blocks above the beacon before capture</li>
 *   <li>Breaking obsidian on an unowned beacon claims it for the player's team</li>
 *   <li>Breaking a team's colored block on an enemy beacon destroys it</li>
 *   <li>Breaking pyramid blocks on owned beacons can yield resources (mining)</li>
 * </ol>
 *
 * Mining mechanics:
 * <ul>
 *   <li>Requires experience points to mine</li>
 *   <li>Different rewards for friendly vs enemy beacons</li>
 *   <li>Cooldown timer prevents rapid mining of the same beacon</li>
 *   <li>Penalty system applies negative effects when cooldown is active</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class BeaconCaptureListener extends BeaconzPluginDependent implements Listener {

    /** Debug flag for verbose logging of capture mechanics */
    private final static boolean DEBUG = false;

    /**
     * Constructs a new BeaconCaptureListener.
     * <p>
     * Initializes the listener to handle beacon capture, destruction, and mining events.
     *
     * @param plugin The Beaconz plugin instance
     */
    public BeaconCaptureListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles damage events to beacon blocks (before actual breakage occurs).
     * <p>
     * This event is triggered when a player starts to break a beacon block but before
     * it's fully broken. It serves two main purposes:
     * <ol>
     *   <li>Validates that the player has proper permissions and team membership</li>
     *   <li>Checks if the beacon is clear of obstructions before allowing capture attempts</li>
     *   <li>Verifies beacon structural integrity</li>
     * </ol>
     *
     * Players attempting to capture beacons must ensure:
     * <ul>
     *   <li>They are not in the lobby (safe zone)</li>
     *   <li>They are assigned to a team</li>
     *   <li>The beacon area is clear of blocks above it</li>
     *   <li>The beacon is not already owned by their team</li>
     * </ul>
     *
     * @param event The BlockDamageEvent containing information about the damaged block and player
     * @see #onBeaconBreak(BlockBreakEvent) for actual capture/destruction mechanics
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName() + " BeaconCaptureListener");

        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        Player player = event.getPlayer();

        // Lobby protection - only Ops can break blocks in the lobby area
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return; // Ops have full access
            } else {
                event.setCancelled(true); // Non-ops cannot break blocks in lobby
                return;
            }
        }

        // Verify the player is assigned to a team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            // Player is not on a team
            if (player.isOp()) {
                return; // Ops can still break blocks even without a team
            } else {
                event.setCancelled(true); // Non-ops without a team cannot break blocks
                return;
            }
        }

        // Check if the damaged block is part of a beacon structure
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return; // Not a beacon block, allow normal processing
        }

        // Verify the beacon's structural integrity (checks if pyramid is intact)
        beacon.checkIntegrity();

        // Check if this is the capture block (obsidian/colored block directly above the beacon)
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            // Verify this is actually a registered beacon, not just a random beacon block
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                // Check if the beacon area is clear of obstructions
                // Beacons must be cleared before capture unless already owned by the player's team
                if (beacon.isNotClear() && (beacon.getOwnership() == null || !beacon.getOwnership().equals(team))) {
                    // Beacon has blocks above it - must be cleared first
                    player.sendMessage(Lang.errorClearAroundBeacon.color(NamedTextColor.GOLD));
                    event.setCancelled(true);
                }
            }
        } else {
            // Player is attempting to damage a different part of the beacon structure
            // (pyramid blocks, etc.) - this is handled by onBeaconBreak
        }

    }

    /**
     * Handles the actual breakage of beacon blocks - the core gameplay mechanic.
     * <p>
     * This complex method handles three distinct scenarios:
     * <ol>
     *   <li><b>Beacon Capture</b> - Breaking obsidian on an unowned beacon claims it</li>
     *   <li><b>Beacon Destruction</b> - Breaking a team's colored block on an enemy beacon destroys it</li>
     *   <li><b>Beacon Mining</b> - Breaking pyramid blocks on owned beacons yields resources</li>
     * </ol>
     *
     * <h3>Capture Mechanics:</h3>
     * <ul>
     *   <li>Breaking obsidian on an unowned beacon claims it for the player's team</li>
     *   <li>The obsidian is replaced with the team's colored block</li>
     *   <li>Player receives a custom map centered on the beacon</li>
     *   <li>Beacon must be clear of obstructions before capture</li>
     * </ul>
     *
     * <h3>Destruction Mechanics:</h3>
     * <ul>
     *   <li>Breaking an enemy team's colored block destroys their beacon</li>
     *   <li>Beacon reverts to obsidian (unclaimed state)</li>
     *   <li>All teams are notified of the destruction</li>
     *   <li>Players standing on the beacon are removed from tracking</li>
     * </ul>
     *
     * <h3>Mining Mechanics:</h3>
     * <ul>
     *   <li>Requires experience points to mine</li>
     *   <li>Different reward tables for friendly vs enemy beacons</li>
     *   <li>Chance-based system determines which item is dropped</li>
     *   <li>Cooldown timer prevents rapid mining (exhaustion)</li>
     *   <li>Penalty potion effects applied if cooldown is active</li>
     * </ul>
     *
     * @param event The BlockBreakEvent containing the broken block and player information
     * @see #giveBeaconMap(Player, BeaconObj) for map creation
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconBreak(BlockBreakEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: BeaconCaputreListner " + event.getEventName() );

        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        Player player = event.getPlayer();

        // Lobby protection - only Ops can break blocks in the lobby area
        if (getGameMgr().isPlayerInLobby(player)) {
            if (DEBUG)
                getLogger().info("DEBUG: Player in in lobby");
            if (player.isOp()) {
                return; // Ops have full access
            } else {
                event.setCancelled(true); // Non-ops cannot break blocks in lobby
                return;
            }
        }

        // Prevent breakage of blocks outside any active game area
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

        // Get the player's team assignment
        // Safely handle cases where game or scorecard might be null
        final Team team = (game != null && game.getScorecard() != null) ? game.getScorecard().getTeam(player) : null;
        if (team == null && !player.isOp()) {
            event.setCancelled(true); // Players without teams can't break beacon blocks
            return;
        }

        // Check if the broken block is part of a beacon structure
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return; // Not a beacon block - allow normal breakage
        }

        // Cancel the event initially - we'll handle block changes manually
        // This prevents the block from dropping as an item
        event.setCancelled(true);

        // Determine if this is the capture block (directly above the beacon block)
        // The capture block is either obsidian (unclaimed) or a team's colored block (claimed)
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            if (DEBUG)
                getLogger().info("DEBUG:beacon below");

            // Verify this is actually a registered beacon in our system
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                if (DEBUG)
                    getLogger().info("DEBUG: registered beacon");

                // SCENARIO 1: BEACON CAPTURE (breaking obsidian on unowned beacon)
                if (block.getType().equals(Material.OBSIDIAN)) {
                    // Verify the beacon area is clear before allowing capture
                    if (beacon.isNotClear()) {
                        player.sendMessage(Lang.errorClearAroundBeacon.color(NamedTextColor.RED));
                        event.setCancelled(true);
                        return;
                    }
                    if (DEBUG) {
                        getLogger().info("DEBUG: obsidian");
                        getLogger().info("DEBUG: team = " + team);
                        getLogger().info("DEBUG: team = " + team.displayName().toString());
                        getLogger().info("DEBUG: block ID = " + game.getScorecard().getBlockID(team));
                    }

                    // Replace obsidian with team's colored block to mark ownership
                    if (game != null) {
                        block.setType(game.getScorecard().getBlockID(team));
                    }

                    // Register the beacon ownership in the system
                    getRegister().setBeaconOwner(beacon, team);

                    // Notify the player of successful capture
                    player.sendMessage(Lang.beaconYouCapturedABeacon.color(NamedTextColor.GREEN));

                    // Give the player a custom map centered on this beacon
                    giveBeaconMap(player, beacon);

                    // Persist the capture to disk for safety
                    getRegister().saveRegister();

                } else {
                    // SCENARIO 2: BEACON DESTRUCTION or UNKNOWN BLOCK
                    if (DEBUG)
                        getLogger().info("DEBUG: another block");

                    // Check if the beacon has a known team owner
                    final Team beaconTeam = beacon.getOwnership();
                    if (beaconTeam != null) {
                        if (DEBUG)
                            getLogger().info("DEBUG: known team block");

                        // Prevent teams from destroying their own beacons
                        if (team != null && team.equals(beaconTeam)) {
                            player.sendMessage(Lang.beaconYouCannotDestroyYourOwnBeacon.color(NamedTextColor.RED));
                            event.setCancelled(true);
                            return;
                        }

                        // Verify the beacon area is clear before allowing destruction
                        if (beacon.isNotClear()) {
                            player.sendMessage(Lang.errorClearAroundBeacon.color(NamedTextColor.GREEN));
                            event.setCancelled(true);
                            return;
                        }

                        // Enemy team has successfully destroyed this beacon!

                        // Notify all other teams about the destruction (taunt message)
                        if (team != null) {
                            getMessages().tellOtherTeams(team, Lang.beaconTeamDestroyed
                                    .replaceText(builder -> builder.matchLiteral("[team1]").replacement(team.displayName()))
                                    .replaceText(builder -> builder.matchLiteral("[team2]").replacement(beaconTeam.displayName()))
                                    .color(NamedTextColor.RED));
                        }
                        
                        // Notify the destroyer's team
                        getMessages().tellTeam(player, Lang.beaconPlayerDestroyed
                                .replaceText(builder -> builder.matchLiteral("[player]").replacement(player.displayName()))
                                .replaceText(builder -> builder.matchLiteral("[team]").replacement(beaconTeam.displayName())));

                        // Notify the player who destroyed it
                        player.sendMessage(Lang.beaconYouDestroyed
                                .replaceText(builder -> builder.matchLiteral("[team]").replacement(beaconTeam.displayName()))
                                .color(NamedTextColor.GREEN));

                        // Play dramatic destruction sound
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, 1F, 1F);

                        // Remove beacon ownership from the system
                        getRegister().removeBeaconOwnership(beacon);

                        // Reset the block to obsidian (unclaimed state)
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);

                        // Clean up: Remove any players standing on this beacon from tracking
                        if (BeaconProtectionListener.getStandingOn().containsValue(beacon)) {
                            BeaconProtectionListener.getStandingOn().inverse().remove(beacon);
                        }
                    } else {
                        // Unknown team block or corruption - reset to obsidian
                        getRegister().removeBeaconOwnership(beacon);
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);
                        if (DEBUG)
                            getLogger().info("DEBUG: unknown team block");
                    }
                }
            }
        } else {
            // SCENARIO 3: BEACON MINING (breaking pyramid blocks on owned beacons)
            // This is for breaking other parts of the beacon structure (not the capture block)

            // Only allow mining on beacons that have an owner
            if (beacon.getOwnership() != null) {
                // Check cooldown timer - beacons can become "exhausted" after mining
                // isNewBeacon() returns true for freshly captured beacons (no cooldown)
                if (beacon.isNewBeacon() || System.currentTimeMillis() > beacon.getHackTimer() + Settings.mineCoolDown) {
                    // Cooldown has expired or beacon is new - mining is allowed

                    if (DEBUG)
                        getLogger().info("DEBUG: player has " + player.getTotalExperience() + " and needs " + Settings.beaconMineExpRequired);

                    // Verify the player has enough experience points to mine
                    // testForExp returns true if player LACKS sufficient experience
                    if (BeaconLinkListener.testForExp(player, Settings.beaconMineExpRequired)) {
                        player.sendMessage(Lang.errorNotEnoughExperience.color(NamedTextColor.RED));
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1F, 1F);
                        return;
                    }

                    // Player has enough experience - proceed with mining
                    Random rand = new Random();

                    // Mining rewards differ based on beacon ownership
                    if (beacon.getOwnership().equals(team)) {
                        // FRIENDLY BEACON MINING - mining your own team's beacon

                        // Roll for reward using weighted chance system
                        // teamGoodies is a TreeMap where keys are cumulative weights
                        int value = rand.nextInt(Settings.teamGoodies.lastKey()) + 1;
                        Entry<Integer, ItemStack> en = Settings.teamGoodies.ceilingEntry(value);

                        if (en != null && en.getValue() != null) {
                            // Successfully determined a reward

                            if (en.getValue().getType().equals(Material.FILLED_MAP)) {
                                // Special case: reward is a beacon map
                                giveBeaconMap(player, beacon);
                            } else {
                                // Normal item reward - drop it at player location
                                player.getWorld().dropItem(event.getPlayer().getLocation(), en.getValue());

                                // Roll for exhaustion - chance that beacon becomes temporarily mined out
                                if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                    // Beacon is now exhausted - set cooldown timer
                                    beacon.resetHackTimer();

                                    player.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN)
                                            .append(Component.text(" "))
                                            .append(Lang.beaconIsExhausted
                                                    .replaceText(builder -> builder.matchLiteral("[minutes]")
                                                            .replacement(Component.text(String.valueOf(Settings.mineCoolDown/60000))))));
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1F, 1F);
                                } else {
                                    // No exhaustion - player can mine again
                                    player.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN));
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                                }
                            }
                            // Deduct the experience cost for mining
                            BeaconLinkListener.removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            // Reward lookup failed (shouldn't happen with proper config)
                            player.sendMessage(Lang.generalFailure.color(NamedTextColor.RED));
                        }
                    } else {
                        // ENEMY BEACON MINING - mining an enemy team's beacon

                        // Roll for reward using weighted chance system
                        // enemyGoodies typically has different (often worse) rewards than teamGoodies
                        int value = rand.nextInt(Settings.enemyGoodies.lastKey()) + 1;
                        Entry<Integer, ItemStack> en = Settings.enemyGoodies.ceilingEntry(value);

                        if (en != null && en.getValue() != null) {
                            // Successfully determined a reward - drop naturally (with physics)
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());

                            // Roll for exhaustion
                            if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                // Beacon is now exhausted - set cooldown timer
                                beacon.resetHackTimer();

                                player.sendMessage(Lang.generalSuccess
                                        .append(Lang.beaconIsExhausted
                                                .replaceText(builder -> builder.matchLiteral("[minutes]")
                                                        .replacement(Component.text(String.valueOf(Settings.mineCoolDown/60000))))));
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1F, 1F);
                            } else {
                                // No exhaustion - player can mine again
                                player.sendMessage(Lang.generalSuccess.color(NamedTextColor.GREEN));
                                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1F, 1F);
                            }
                            // Deduct the experience cost for mining
                            BeaconLinkListener.removeExp(player, Settings.beaconMineExpRequired);
                        } else {
                            // Reward lookup failed - spawn hostile endermite as punishment
                            player.getWorld().spawnEntity(player.getLocation(), EntityType.ENDERMITE);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMITE_AMBIENT, 1F, 1F);
                            player.sendMessage(Lang.generalFailure.append(Component.text(" Watch out!")).color(NamedTextColor.RED));
                        }
                    }
                } else {
                    // COOLDOWN PENALTY - beacon is still exhausted from previous mining

                    // Calculate how many ticks remain on the cooldown
                    // Convert milliseconds to ticks (1 tick = 50ms)
                    int num = (int) (beacon.getHackTimer() + Settings.mineCoolDown - System.currentTimeMillis()) / 50;

                    // Apply configured penalty potion effects for attempting to mine during cooldown
                    for (String effect : Settings.minePenalty) {
                        // Parse effect string format: "EFFECT_NAME:AMPLIFIER"
                        String[] split = effect.split(":");
                        if (split.length == 2) {
                            int amplifier = 1;

                            // Extract amplifier value if provided
                            if (NumberUtils.isNumber(split[1])) {
                                amplifier = Integer.parseInt(split[1]);
                                if (DEBUG)
                                    getLogger().info("DEBUG: Amplifier is " + amplifier);
                            }

                            // Look up the potion effect type by name
                            PotionEffectType potionEffectType = PotionEffectType.getByName(split[0]);

                            if (potionEffectType != null) {
                                // Apply the penalty effect for the remaining cooldown duration
                                player.addPotionEffect(new PotionEffect(potionEffectType, num, amplifier));

                                // Play sound effect to indicate penalty
                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1F, 1F);

                                if (DEBUG)
                                    getLogger().info("DEBUG: Applying " + potionEffectType + ":" + amplifier + " for " + num + " ticks");
                            }
                        } else {
                            // Invalid configuration format - log warning
                            getLogger().warning("Unknown hack cooldown effect" + effect);
                        }

                    }
                }
            }
        }
    }

    /**
     * Creates and gives a custom beacon map to the player.
     * <p>
     * This method generates a special in-game map that:
     * <ul>
     *   <li>Is centered on the specified beacon's coordinates</li>
     *   <li>Uses custom renderers to display territory and beacon information</li>
     *   <li>Has a custom display name identifying the beacon</li>
     *   <li>Is automatically given to the player (offhand first, then inventory)</li>
     * </ul>
     *
     * The map creation process:
     * <ol>
     *   <li>Create a new MapView centered on the beacon</li>
     *   <li>Attach custom renderers (territory display and beacon markers)</li>
     *   <li>Create a FILLED_MAP item with appropriate metadata</li>
     *   <li>Give to player's offhand if empty, otherwise add to inventory</li>
     *   <li>Register the map in the system for future reference</li>
     * </ol>
     *
     * If the player's offhand is occupied and inventory is full, the map is dropped
     * at the player's location with a pickup sound effect.
     *
     * @param player The player receiving the beacon map
     * @param beacon The beacon to center the map on
     */
    private void giveBeaconMap(Player player, BeaconObj beacon) {
        // Create a new MapView for the Beaconz world
        MapView mapView = Bukkit.createMap(getBeaconzWorld());

        // Center the map on the beacon's coordinates
        mapView.setCenterX(beacon.getX());
        mapView.setCenterZ(beacon.getZ());

        // Set normal zoom level for best beacon visibility
        mapView.setScale(Scale.NORMAL);

        // Remove default renderers and add custom ones
        mapView.getRenderers().clear();
        mapView.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin())); // Shows territory ownership
        mapView.addRenderer(new BeaconMap(getBeaconzPlugin()));            // Shows beacon locations

        // Create the physical map item (use FILLED_MAP, not deprecated MAP)
        ItemStack newMap = new ItemStack(Material.FILLED_MAP);

        // Configure the map's metadata
        if (newMap.getItemMeta() instanceof MapMeta meta) {
            // Set a custom display name identifying which beacon this map shows
            meta.displayName(Component.text("Beacon map for " + beacon.getName()));

            // Connect the ItemStack to our custom MapView
            // This ensures the map displays our custom renderers
            meta.setMapView(mapView);

            // Apply the metadata to the item
            newMap.setItemMeta(meta);
        }

        // Attempt to give the map to the player
        ItemStack offHand = player.getInventory().getItemInOffHand();
        getLogger().info("offhand = " + offHand);

        if (!offHand.getType().equals(Material.AIR)) {
            // Offhand is occupied - try to add to main inventory
            HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(newMap);

            if (!leftOvers.isEmpty()) {
                // Inventory is full - notify player and drop the map
                player.sendMessage(Lang.errorInventoryFull.color(NamedTextColor.RED));

                for (ItemStack item: leftOvers.values()) {
                    // Drop each leftover item at the player's location
                    player.getWorld().dropItem(player.getLocation(), item);

                    // Play pickup sound to draw attention to the dropped map
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 0.5F);
                }
            }
        } else {
            // Offhand is empty - place map directly in offhand for immediate viewing
            player.getInventory().setItemInOffHand(newMap);
        }

        // Register the map in the system for tracking and future reference
        // This allows the plugin to associate this map ID with this specific beacon
        getRegister().addBeaconMap(mapView.getId(), beacon);

    }
}
