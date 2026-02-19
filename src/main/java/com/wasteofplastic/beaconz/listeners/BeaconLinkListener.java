/*
 * Copyright (c) 2015 - 2026 tastybento
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

import java.awt.geom.Line2D;
import java.util.List;
import java.util.Locale;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Params.GameScoreGoal;
import com.wasteofplastic.beaconz.config.Settings;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.core.DefenseBlock;
import com.wasteofplastic.beaconz.map.TerritoryMapRenderer;
import com.wasteofplastic.beaconz.util.LinkResult;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * Listener class that handles beacon linking mechanics and map interactions.
 * <p>
 * This class manages the strategic linking system where players can connect owned beacons
 * to create triangular territory fields. The linking system includes:
 * <ul>
 *   <li>Map-based beacon linking using beacon maps</li>
 *   <li>Experience point requirements based on distance</li>
 *   <li>Link validation (crossing prevention, max links, self-linking)</li>
 *   <li>Triangle field creation when three beacons are linked</li>
 *   <li>Score updates and team notifications</li>
 *   <li>Reward distribution for successful links</li>
 *   <li>Map rendering with territory overlays</li>
 * </ul>
 *
 * <h3>Linking Mechanics:</h3>
 * <ol>
 *   <li>Player captures a beacon and receives a beacon map</li>
 *   <li>Player right-clicks another owned beacon with the map</li>
 *   <li>System validates: distance, experience, link rules</li>
 *   <li>Link is created if all validations pass</li>
 *   <li>Experience is deducted based on distance</li>
 *   <li>Triangle fields are automatically created when applicable</li>
 * </ol>
 *
 * <h3>Triangle Creation:</h3>
 * When a link completes a triangle with two other links, the enclosed area
 * becomes territory for the team, contributing to their area score.
 *
 * @author tastybento
 * @since 1.0
 */
public class BeaconLinkListener extends BeaconzPluginDependent implements Listener {

    /** Debug flag for verbose logging of link operations */
    private final static boolean DEBUG = false;

    /**
     * Constructs a new BeaconLinkListener.
     * <p>
     * Initializes the listener to handle beacon linking through map interactions
     * and experience-based distance mechanics.
     *
     * @param plugin The Beaconz plugin instance
     */
    public BeaconLinkListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles the beacon linking mechanic when a player right-clicks a beacon with a beacon map.
     * <p>
     * This is the core mechanic for creating strategic links between owned beacons. The process:
     * <ol>
     *   <li>Validates the player is holding a beacon map in their main hand</li>
     *   <li>Verifies the player is clicking an owned beacon</li>
     *   <li>Checks the map points to another owned beacon</li>
     *   <li>Validates distance, experience, and link rules</li>
     *   <li>Creates the link and any resulting triangle fields</li>
     *   <li>Deducts experience and removes the map</li>
     *   <li>Updates scores and sends notifications</li>
     * </ol>
     *
     * The linking system uses experience points as a cost based on distance, with
     * special defense blocks potentially reducing the effective distance.
     *
     * @param event The PlayerInteractEvent for right-clicking a block
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPaperMapUse(final PlayerInteractEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: paper map " + event.getEventName());

        // Verify the player is holding an item
        if (!event.hasItem()) {
            return;
        }

        // Only process filled maps (beacon maps)
        if (!event.getItem().getType().equals(Material.FILLED_MAP)) {
            return;
        }

        // Extract the map ID from the item metadata
        int mapId = -1;
        if (event.getItem().getItemMeta() instanceof MapMeta mapMeta) {
            // Check if the map has an ID associated with it
            if (mapMeta.hasMapId()) {
                mapId = mapMeta.getMapId();
            }
        }

        // Only process right-clicks on blocks (not air)
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        // Quick world check - only process events in the Beaconz world
        World world = event.getClickedBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        Player player = event.getPlayer();

        // Ignore players in the lobby (safe zone)
        if (getGameMgr().isPlayerInLobby(player)) {
            return;
        }

        // Verify the map is in the player's MAIN hand (not offhand)
        // This prevents accidental linking with offhand items
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!mainHand.equals(event.getItem())) {
            return;
        }

        // Get the player's team assignment
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            // Player has no team
            if (player.isOp()) {
                return; // Ops can interact without teams
            } else {
                event.setCancelled(true); // Non-ops need a team
                return;
            }
        }

        // Check if the clicked block is part of a beacon structure
        Block b = event.getClickedBlock();
        final BeaconObj beacon = getRegister().getBeacon(b);
        if (beacon == null) {
            if (DEBUG)
                getLogger().info("DEBUG: not a beacon");
            return;
        }

        // Verify the beacon is owned by the player's team
        // Cannot link from unowned or enemy beacons
        if (beacon.getOwnership() == null || !beacon.getOwnership().equals(team)) {
            if (DEBUG)
                getLogger().info("DEBUG: becon ownership: " + (beacon.getOwnership() == null ? "null" : beacon.getOwnership().getName()));
            player.sendMessage(Lang.beaconYouMustCapturedBeacon);
            event.setCancelled(true);
            return;
        }

        // Check if this map is a registered beacon map
        BeaconObj mappedBeacon = getRegister().getBeaconMap(mapId);
        if (mappedBeacon == null) {
            // This is not a beacon map (regular world map)
            return;
        }

        // Verify the mapped beacon is also owned by the player's team
        // Cannot link to unowned or enemy beacons
        if (mappedBeacon.getOwnership() == null || !mappedBeacon.getOwnership().equals(team)) {
            if (DEBUG) getLogger().info("DEBUG: mapped beacon ownership: " + (mappedBeacon.getOwnership() == null ? "null" : mappedBeacon.getOwnership().getName()));
            player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.beaconOriginNotOwned,
                    Placeholder.component("team", team.displayName()))
                    );
            return;
        }

        // Cancel the event to prevent default block interaction
        event.setCancelled(true);

        // Process linking with experience requirements
        if (Settings.expDistance > 0) {
            // Experience-based linking is enabled

            // Check if the beacons are within linking range
            int linkDistance = checkBeaconDistance(beacon, mappedBeacon);
            if (linkDistance > Settings.linkLimit) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.errorTooFar,
                        Placeholder.component("max"
                                , Component.text(String.valueOf(Settings.linkLimit))))
                        );
                return;
            }

            // Calculate experience cost based on distance
            int expRequired = getReqExp(beacon, mappedBeacon);
            if (expRequired > 0) {
                // Verify the player has sufficient experience
                // testForExp returns true if player LACKS sufficient experience
                if (testForExp(player, expRequired)) {
                    // Player doesn't have enough experience
                    player.sendMessage(Lang.errorNotEnoughExperience);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.beaconYouNeedThisMuchExp,
                            Placeholder.component("number"
                                    , Component.text(String.format(Locale.US, "%,d", expRequired))))
                            );
                    player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.beaconYouHaveThisMuchExp,
                            Placeholder.component("number"
                                    , Component.text(String.format(Locale.US, "%,d", player.calculateTotalExperiencePoints()))))
                            );
                    return;
                }
            }

            // Player has sufficient experience - attempt to create the link
            if (linkBeacons(player, team, beacon, mappedBeacon)) {
                // Link created successfully
                player.sendMessage(Lang.beaconTheMapDisintegrates);

                // Remove the map from player's inventory (it's been consumed)
                player.getInventory().setItemInMainHand(null);

                // Deduct the experience cost
                removeExp(player, expRequired);

                // Persist changes to disk for safety
                getRegister().saveRegister();

                // Update team scores and scoreboard display
                getGameMgr().getGame(team).getScorecard().refreshScores(team);
                getGameMgr().getGame(team).getScorecard().refreshSBdisplay(team);
            }
        } else {
            // No experience required for linking (free linking mode)
            if (linkBeacons(player, team, beacon, mappedBeacon)) {
                // Link created successfully
                player.sendMessage(Lang.beaconTheMapDisintegrates);

                // Remove the map from registry and inventory
                getRegister().removeBeaconMap(mapId);
                player.getInventory().setItemInMainHand(null);

                // Persist changes to disk for safety
                getRegister().saveRegister();

                // Update team scores and scoreboard display
                getGameMgr().getGame(team).getScorecard().refreshScores(team);
                getGameMgr().getGame(team).getScorecard().refreshSBdisplay(team);
            }
        }
    }

    /**
     * Calculates the effective linking distance between two beacons.
     * <p>
     * The distance calculation considers special defense blocks that can reduce
     * the effective distance. Each beacon's defense blocks are checked, and if they
     * are configured as "link blocks" in the settings, their reduction value is
     * subtracted from the base distance.
     * <p>
     * This allows players to strategically place certain blocks on their beacons
     * to extend their linking range.
     *
     * @param beacon The first beacon in the link
     * @param mappedBeacon The second beacon in the link
     * @return The effective distance between beacons (can be negative if many link blocks are present)
     */
    private int checkBeaconDistance(BeaconObj beacon, BeaconObj mappedBeacon) {
        // Calculate base Euclidean distance between the two beacons
        int distance = (int)beacon.getPoint().distance(mappedBeacon.getPoint());

        // Check first beacon for link-reducing defense blocks
        for (DefenseBlock block : beacon.getDefenseBlocks().values()) {
            // If this block type is configured as a link block, reduce the distance
            if (Settings.linkBlocks.containsKey(block.getBlock().getType())) {
                distance -= Settings.linkBlocks.get(block.getBlock().getType());
            }
        }

        // Check second beacon for link-reducing defense blocks
        for (DefenseBlock block : mappedBeacon.getDefenseBlocks().values()) {
            // If this block type is configured as a link block, reduce the distance
            if (Settings.linkBlocks.containsKey(block.getBlock().getType())) {
                distance -= Settings.linkBlocks.get(block.getBlock().getType());
            }
        }

        return distance;
    }

    /**
     * Calculates the experience points required to create a link between two beacons.
     * <p>
     * The experience cost is proportional to the Euclidean distance between the beacons,
     * divided by the configured experience-per-distance ratio. This creates a cost
     * that scales with how far apart the beacons are, making long-distance links
     * more expensive in terms of player experience.
     * <p>
     * Note: This uses the raw distance, not the effective distance from
     * {@link #checkBeaconDistance(BeaconObj, BeaconObj)} which considers defense blocks.
     *
     * @param beacon The first beacon in the proposed link
     * @param mappedBeacon The second beacon in the proposed link
     * @return The experience points required to create this link
     */
    public int getReqExp(BeaconObj beacon, BeaconObj mappedBeacon) {
        // Calculate base Euclidean distance between beacon centers
        double distance = beacon.getPoint().distance(mappedBeacon.getPoint());

        // Convert distance to experience cost using configured ratio
        // Settings.expDistance is the number of blocks per experience point
        return (int) (distance / Settings.expDistance);
    }

    /**
     * Ensures all held beacon maps have territory overlay renderers attached.
     * <p>
     * This handler monitors when players switch to holding a map and automatically
     * adds the TerritoryMapRenderer if it's not already present. This ensures that
     * beacon maps always display team territories and beacon locations, even if the
     * renderer was somehow removed or the map was created before the renderer system.
     * <p>
     * The territory renderer displays:
     * <ul>
     *   <li>Team-controlled triangular fields</li>
     *   <li>Beacon locations and ownership</li>
     *   <li>Links between beacons</li>
     * </ul>
     *
     * Note: There's a TODO to also handle maps in item frames.
     *
     * @param event The PlayerItemHeldEvent when a player changes held item slot
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
    public void onMapHold(final PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Get the item in the new slot the player is switching to
        ItemStack itemInHand = player.getInventory().getItem(event.getNewSlot());
        if (itemInHand == null) return;

        // Only process filled maps
        if (!Material.FILLED_MAP.equals(itemInHand.getType())) {
            return;
        }

        // Only process maps in the Beaconz world
        if (!player.getWorld().equals(getBeaconzWorld())) {
            return;
        }

        // Extract map metadata to get the map ID
        if (itemInHand.getItemMeta() instanceof MapMeta mapMeta) {

            // Check if the map has an ID associated with it
            if (mapMeta.hasMapId()) {
                int mapId = mapMeta.getMapId();
                MapView map = Bukkit.getMap(mapId);

                // Check if TerritoryMapRenderer is already attached
                for (MapRenderer renderer : map.getRenderers()) {
                    if (renderer instanceof TerritoryMapRenderer) {
                        return; // Renderer already present, no action needed
                    }
                }

                // Renderer not found - add it to display territories
                map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
            }
        }
    }

    /**
     * Attempts to create a link between two owned beacons.
     * <p>
     * This method performs comprehensive validation before creating a link:
     * <ol>
     *   <li>Prevents self-linking (beacon to itself)</li>
     *   <li>Enforces maximum links per beacon limit</li>
     *   <li>Prevents duplicate links</li>
     *   <li>Validates link doesn't cross enemy team links</li>
     * </ol>
     *
     * If all validations pass, the link is created and the system:
     * <ul>
     *   <li>Creates any triangular fields that result from the new link</li>
     *   <li>Updates team scores based on new territory</li>
     *   <li>Notifies the player and team of success</li>
     *   <li>Taunts enemy teams about new territory</li>
     *   <li>Distributes rewards to the player</li>
     *   <li>Executes configured commands</li>
     * </ul>
     *
     * <h3>Triangle Formation:</h3>
     * When three beacons are fully interconnected (each linked to the other two),
     * a triangular field is automatically created. The enclosed area becomes
     * team territory and contributes to the AREA score goal.
     *
     * @param player The player attempting to create the link
     * @param team The player's team
     * @param beacon The first beacon (the one being clicked)
     * @param otherBeacon The second beacon (from the map)
     * @return true if the link was successfully created, false otherwise
     */
    private boolean linkBeacons(Player player, Team team, BeaconObj beacon,
            BeaconObj otherBeacon) {

        // VALIDATION 1: Prevent self-linking
        if (beacon.equals(otherBeacon)) {
            player.sendMessage(Lang.beaconYouCannotLinkToSelf);
            return false;
        }

        // VALIDATION 2: Check maximum links limit
        if (beacon.getNumberOfLinks() == Settings.maxLinks) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.beaconMaxLinks,
                    Placeholder.component("number"
                            , Component.text(String.valueOf(Settings.maxLinks))))
                    );
            return false;
        }

        // VALIDATION 3: Check if this link already exists
        if (beacon.getLinks().contains(otherBeacon)) {
            player.sendMessage(Lang.beaconLinkAlreadyExists);
            return false;
        }

        // VALIDATION 4: Check if the link crosses enemy team links
        // Create a line segment representing the proposed link
        Line2D proposedLink = new Line2D.Double(beacon.getPoint(), otherBeacon.getPoint());

        if (DEBUG)
            getLogger().info("DEBUG: Check if the link crosses opposition team's links");

        // Check intersection with all enemy team links
        for (Line2D line : getRegister().getEnemyLinks(team)) {
            if (DEBUG)
                getLogger().info("DEBUG: checking line " + line.getP1() + " to " + line.getP2());

            // If the proposed link intersects an enemy link, reject it
            if (line.intersectsLine(proposedLink)) {
                player.sendMessage(Lang.beaconLinkCannotCrossEnemy);
                return false;
            }
        }

        // All validations passed - create the link!
        LinkResult result = getRegister().addBeaconLink(beacon, otherBeacon);

        if (result.isSuccess()) {
            // Link created successfully
            player.sendMessage(Lang.beaconLinkCreated);
            player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.beaconNowHasLinks,
                    Placeholder.component("number"
                            , Component.text(String.valueOf(beacon.getNumberOfLinks())))));

            // Play success sound effect
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1F, 1F);

            // Notify the player's team about the new link
            getMessages().tellTeam(player, MiniMessage.miniMessage().deserialize(Lang.beaconNameCreatedALink,
                    Placeholder.component("name"
                            , player.displayName()))
                    );
        } else {
            // Link creation failed (shouldn't happen after validations)
            player.sendMessage(Lang.beaconLinkCouldNotBeCreated);
            return false;
        }

        // Handle triangle field creation
        if (result.getFieldsMade() > 0) {
            // At least one triangular field was created

            if (result.getFieldsMade() == 1) {
                // Single triangle created
                player.sendMessage(Lang.beaconTriangleCreated.append(Component.text(" "))
                        .append(Lang.scoreNewScore).append(Component.text(" = ")
                                .append(Component.text(String.format(Locale.US, "%,d",getGameMgr().getSC(team).getScore(team, GameScoreGoal.AREA)))))
                        );

                // Notify team with new score
                getMessages().tellTeam(player, MiniMessage.miniMessage().deserialize(Lang.beaconNameCreateATriangle,
                        Placeholder.component("name"
                                , player.displayName()))
                        .append(Component.text(" ").append(Lang.scoreNewScore).append(Component.text(" = ")
                                .append(Component.text(String.format(Locale.US, "%,d", getGameMgr().getSC(team).getScore(team, GameScoreGoal.AREA))))))
                        );

                // Taunt enemy teams about the new territory
                getMessages().tellOtherTeams(team, MiniMessage.miniMessage().deserialize(Lang.beaconNameCreateATriangle,
                        Placeholder.component("name"
                                , team.displayName()))
                        );
            } else {
                // Multiple triangles created (rare but possible with certain link configurations)
                Component message = MiniMessage.miniMessage().deserialize(Lang.beaconNameCreateTriangles,
                        Placeholder.component("name"
                                , player.displayName()),
                        Placeholder.component("number"
                                , Component.text(String.valueOf(result.getFieldsMade()))));

                Component newScore = Lang.scoreNewScore.append(Component.text(" " + String.format(Locale.US, "%,d", getGameMgr().getSC(team).getScore(team, GameScoreGoal.AREA))));

                // Notify player with gold color
                player.sendMessage(message.append(Component.text(" ")).append(newScore));

                // Notify team with green color
                getMessages().tellTeam(player, message.append(Component.text(" ")).append(newScore));

                // Taunt enemy teams with red color
                getMessages().tellOtherTeams(team, message);
            }
        }

        // Handle failed triangle attempts
        if (result.getFieldsFailedToMake() > 0) {
            // Some triangles couldn't be created (e.g., overlapping with enemy territory)
            if (result.getFieldsFailedToMake() == 1) {
                player.sendMessage(Lang.triangleCouldNotMakeTriangle);
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(Lang.triangleCouldNotMakeTriangles,
                        Placeholder.component("number"
                                , Component.text(String.valueOf(result.getFieldsFailedToMake())))));
            }
        }

        // Distribute configured rewards for creating the link
        List<ItemStack> rewards = giveItems(player, Settings.linkRewards);
        if (!rewards.isEmpty()) {
            player.sendMessage(Lang.beaconYouReceivedAReward);
        }

        // Execute configured commands (e.g., additional rewards, effects)
        runCommands(player, Settings.linkCommands);

        return true;
    }


    /**
     * Tests if a player lacks the required experience to perform an action.
     * <p>
     * <b>Important:</b> This method returns {@code true} if the player does NOT have
     * enough experience, and {@code false} if they do have enough. This might seem
     * counterintuitive, but it's designed for use in conditional checks like:
     * {@code if (testForExp(player, amount)) { // player lacks exp }}
     * <p>
     *
     * @param player The player whose experience to check
     * @param xpRequired The amount of experience required
     * @return {@code true} if player has insufficient experience, {@code false} if they have enough
     */
    public static boolean testForExp(Player player , int xpRequired){
        return player.calculateTotalExperiencePoints() < xpRequired;
    }

    /**
     * Removes the specified amount of experience from a player.
     * <p>
     * This method safely deducts experience from the player's total. If the player
     * has sufficient experience, it:
     * <ol>
     *   <li>Calculates the player's current total experience</li>
     *   <li>Subtracts the required amount</li>
     *   <li>Uses {@link #setTotalExperience(Player, int)} to apply the new total</li>
     * </ol>
     *
     * If the player doesn't have enough experience, no action is taken.
     * This prevents negative experience values.
     *
     * @param player The player from whom to remove experience
     * @param xpRequired The amount of experience to remove
     */
    public static void removeExp(Player player , int xpRequired){
        int xp = player.calculateTotalExperiencePoints();
        if (xp >= xpRequired) {
            setTotalExperience(player, xp - xpRequired);
        }
    }

    /**
     * Sets a player's total experience to an exact amount.
     * <p>
     * <b>MODERNIZED:</b> This method now uses the simpler Paper API approach instead of
     * the old Essentials code. The modern implementation:
     * <ol>
     *   <li>Resets all experience to zero</li>
     *   <li>Uses {@link Player#giveExp(int)} to set the new amount</li>
     * </ol>
     *
     * The Paper API handles all the complexity of level calculation and progress
     * synchronization automatically, eliminating the need for manual calculation.
     *
     * @param player The player whose experience to set
     * @param exp The total experience amount to set
     * @throws IllegalArgumentException if exp is negative
     */
    public static void setTotalExperience(final Player player, final int exp)
    {
        // Validate input - experience cannot be negative
        if (exp < 0)
        {
            throw new IllegalArgumentException("Experience is negative!");
        }

        // Reset all experience values to zero
        player.setLevel(0);
        player.setExp(0);
        player.setTotalExperience(0);

        // Give the player the exact amount of experience
        // The Paper API handles level calculation and synchronization automatically
        if (exp > 0) {
            player.giveExp(exp);
        }
    }

    /**
     * Prevents players from placing blocks at the top of the world in the Beaconz world.
     * @param event The BlockPlaceEvent triggered when a player attempts to place a block
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                && !event.getPlayer().isOp()
                && event.getBlock().getWorld().equals(getBeaconzWorld())
                && event.getBlock().getY() > getBeaconzWorld().getMaxHeight() - 2) {
                // Prevent placing blocks at the top of the world to avoid map rendering issues
                event.setCancelled(true);
        }
    }

    /**
     * Prevents players from breaking blocks at the top of the world in the Beaconz world.
     * @param event The BlockBreakEvent triggered when a player attempts to break a block
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                && !event.getPlayer().isOp()
                && event.getBlock().getWorld().equals(getBeaconzWorld())
                && event.getBlock().getY() > getBeaconzWorld().getMaxHeight() - 2) {
            // Prevent breaking blocks at the top of the world to avoid map rendering issues
            event.setCancelled(true);
        }
    }
}
