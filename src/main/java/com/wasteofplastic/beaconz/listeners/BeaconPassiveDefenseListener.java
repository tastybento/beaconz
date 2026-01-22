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

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scoreboard.Team;

import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.core.DefenseBlock;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.game.Scorecard;
import com.wasteofplastic.beaconz.config.Settings;

import net.kyori.adventure.text.Component;

/**
 * Manages passive defense mechanics for beacons including block placement, defense removal,
 * and protection from various threats.
 *
 * <p>This listener handles:
 * <ul>
 *   <li>Defense block placement around beacons with level requirements</li>
 *   <li>Defense block removal with top-down enforcement</li>
 *   <li>Beacon range extension using emerald blocks</li>
 *   <li>Beacon locking mechanics</li>
 *   <li>Protection from explosions, pistons, and liquid flow</li>
 * </ul>
 *
 * <p>Defense blocks must be placed and removed according to specific rules:
 * <ul>
 *   <li>Only placed on owned beacons</li>
 *   <li>Require appropriate player experience levels</li>
 *   <li>Must be removed top-down by attacking teams</li>
 *   <li>Cannot exceed configured height limits</li>
 * </ul>
 *
 * @author tastybento
 */
public class BeaconPassiveDefenseListener extends BeaconzPluginDependent implements Listener {

    /**
     * Maximum distance squared (in blocks) an emerald block can be placed from the beacon center
     * for range extension. Value of 64 equals 8 blocks (8²).
     */
    private static final double MAX_EXTENSION_DISTANCE_SQUARED = 64.0;

    /**
     * Debug flag for additional logging. Set to false in production.
     */
    private static final boolean DEBUG = false;

    /**
     * Creates a new beacon passive defense listener.
     *
     * @param plugin the Beaconz plugin instance
     */
    public BeaconPassiveDefenseListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Protects beacon structures from explosion damage.
     *
     * <p>Removes any blocks above beacons from the explosion's affected block list,
     * preventing damage to beacon defenses and the beacon itself.
     *
     * @param event the entity explosion event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (isNotBeaconzWorld(event.getLocation().getWorld())) {
            return;
        }

        // Remove any blocks that are above beacons from the explosion
        event.blockList().removeIf(block -> getRegister().isAboveBeacon(block.getLocation()));
    }

    /**
     * Prevents pistons from pushing blocks into beacon-protected areas.
     *
     * <p>Cancels piston extension if any block would be pushed to a location
     * above a beacon, preserving beacon defenses from piston manipulation.
     *
     * @param event the piston extend event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (isNotBeaconzWorld(event.getBlock().getWorld())) {
            return;
        }

        for (Block block : event.getBlocks()) {
            Block destination = block.getRelative(event.getDirection());
            if (getRegister().isAboveBeacon(destination.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents sticky pistons from pulling blocks from beacon-protected areas.
     *
     * <p>Cancels piston retraction if any block being pulled is above a beacon,
     * preventing removal of beacon defenses via sticky piston.
     *
     * @param event the piston retract event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        if (isNotBeaconzWorld(event.getBlock().getWorld())) {
            return;
        }

        for (Block block : event.getBlocks()) {
            if (getRegister().isAboveBeacon(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Handles block placement around beacons, enforcing placement rules for defense blocks,
     * range extensions, and locking blocks.
     *
     * <p>Validates:
     * <ul>
     *   <li>Player is in the Beaconz world and in a game</li>
     *   <li>Block placement location and type</li>
     *   <li>Player's team ownership of the beacon</li>
     *   <li>Player's experience level for defense blocks</li>
     *   <li>Height restrictions for defenses</li>
     * </ul>
     *
     * @param event the block place event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isNotBeaconzWorld(event.getBlock().getWorld())) {
            return;
        }

        Player player = event.getPlayer();

        // Check lobby restrictions
        if (!validateLobbyAccess(player, event)) {
            return;
        }

        // Validate player team membership
        Scorecard scorecard = getGameMgr().getSC(player);
        Optional<Team> team = validatePlayerTeam(player, scorecard, event);
        if (team.isEmpty()) {
            return;
        }

        Block block = event.getBlock();
        Optional<BeaconObj> adjacentBeacon = findAdjacentBeacon(block);

        // Check for range extension block (emerald)
        if (block.getType() == Material.EMERALD_BLOCK && adjacentBeacon.isPresent()) {
            handleRangeExtension(player, block, adjacentBeacon.get(), team.get(), event);
            return;
        }

        // Check for locking block
        Material lockingBlock = getLockingBlockMaterial();
        if (block.getType() == lockingBlock && adjacentBeacon.isPresent()) {
            handleLockingBlock(player, block, adjacentBeacon.get(), team.get());
        }

        // Handle defensive block placement
        BeaconObj beacon = getRegister().getBeaconAt(new Point2D.Double(block.getX(), block.getZ()));
        if (beacon == null || beacon.getOwnership() == null || beacon.getY() > block.getY()) {
            return;
        }

        handleDefenseBlockPlacement(player, block, beacon, team.get(), event);
    }

    /**
     * Validates lobby access for block interactions.
     * Only operators can interact with blocks in the lobby.
     *
     * @param player the player attempting to interact with a block
     * @param event the cancellable event to cancel if invalid
     * @return true if validation passed, false if event was cancelled or player is in lobby
     */
    private boolean validateLobbyAccess(Player player, org.bukkit.event.Cancellable event) {
        if (getGameMgr().isPlayerInLobby(player)) {
            if (DEBUG) {
                getLogger().info("DEBUG: In lobby");
            }
            if (!player.isOp()) {
                event.setCancelled(true);
                return false;
            }
            return false; // Op can interact, but don't process further
        }
        return true;
    }

    /**
     * Validates that a player is on a team and in a game.
     *
     * @param player the player to validate
     * @param scorecard the player's scorecard
     * @param event the cancellable event to cancel if invalid
     * @return the player's team, or empty if validation failed
     */
    private Optional<Team> validatePlayerTeam(Player player, Scorecard scorecard, org.bukkit.event.Cancellable event) {
        if (scorecard == null || scorecard.getTeam(player) == null) {
            if (DEBUG) {
                getLogger().info("DEBUG: Scorecard is null or player's team is null");
            }
            if (!player.isOp()) {
                event.setCancelled(true);
                player.sendMessage(Lang.errorYouMustBeInAGame);
                getGameMgr().getLobby().tpToRegionSpawn(player, true);
                return Optional.empty();
            }
            player.sendMessage(Lang.errorYouMustBeInATeam);
            return Optional.empty();
        }
        return Optional.ofNullable(scorecard.getTeam(player));
    }

    /**
     * Finds a beacon adjacent to the given block by checking all four cardinal directions.
     *
     * @param block the block to check around
     * @return the adjacent beacon
     */
    private Optional<BeaconObj> findAdjacentBeacon(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block adjacent = block.getRelative(face);
            BeaconObj beacon = getRegister().getBeaconAt(new Point2D.Double(adjacent.getX(), adjacent.getZ()));
            if (beacon != null) {
                return Optional.of(beacon);
            }
        }
        return Optional.empty();
    }

    /**
     * Handles placement of emerald blocks for beacon range extension.
     *
     * @param player the player placing the block
     * @param block the emerald block being placed
     * @param beacon the adjacent beacon
     * @param team the player's team
     * @param event the block place event to cancel if invalid
     */
    private void handleRangeExtension(Player player, Block block, BeaconObj beacon, Team team, BlockPlaceEvent event) {
        // Check block is at the right height (one below beacon)
        if (block.getY() + 1 != beacon.getY()) {
            return;
        }

        // Verify team ownership
        if (beacon.getOwnership() == null || !beacon.getOwnership().equals(team)) {
            player.sendMessage(Lang.beaconYouCanOnlyExtend);
            event.setCancelled(true);
            return;
        }

        // Check distance from beacon center
        if (block.getLocation().distanceSquared(beacon.getLocation()) > MAX_EXTENSION_DISTANCE_SQUARED) {
            player.sendMessage(Lang.beaconCannotBeExtended);
            event.setCancelled(true);
            return;
        }

        // Ensure area above is clear
        int highestBlockY = getHighestBlockYAt(block.getX(), block.getZ());
        if (DEBUG) {
            getLogger().info("DEBUG: highest block y = " + highestBlockY + " difference = " + (highestBlockY - beacon.getY()));
        }

        if (highestBlockY > beacon.getY()) {
            player.sendMessage(Lang.errorClearAboveBeacon);
            event.setCancelled(true);
            return;
        }

        // Register the extension
        getRegister().addBeaconDefenseBlock(block.getLocation(), beacon);
        player.sendMessage(Lang.beaconExtended);
    }

    /**
     * Gets the configured locking block material.
     *
     * @return the locking block material, defaults to EMERALD_BLOCK if not configured
     */
    private Material getLockingBlockMaterial() {
        Material material = Material.getMaterial(Settings.lockingBlock.toUpperCase());
        return material != null ? material : Material.EMERALD_BLOCK;
    }

    /**
     * Handles placement of locking blocks on beacons.
     * Locking blocks must be placed directly above the beacon by the owning team.
     *
     * @param player the player placing the block
     * @param block the locking block being placed
     * @param beacon the adjacent beacon
     * @param team the player's team
     */
    private void handleLockingBlock(Player player, Block block, BeaconObj beacon, Team team) {
        // Only process if team owns the beacon
        if (beacon.getOwnership() == null || !beacon.getOwnership().equals(team)) {
            return;
        }
        
        // Check if block is placed directly above beacon's base (within 2 blocks in x and z)
        if (Math.abs(block.getX() - beacon.getX()) < 2 && Math.abs(block.getZ() - beacon.getZ()) < 2) {
            int blocksNeeded = beacon.nbrToLock(block.getY());

            if (blocksNeeded == 0) {
                player.sendMessage(Lang.beaconLockedJustNow
                        .replaceText(builder -> builder.matchLiteral("[lockingBlock]")
                                .replacement(Component.text(Settings.lockingBlock.toLowerCase()))));
            } else if (beacon.isLocked()) {
                player.sendMessage(Lang.beaconLockedAlready
                        .replaceText(builder -> builder.matchLiteral("[lockingBlock]")
                                .replacement(Component.text(Settings.lockingBlock.toLowerCase()))));
            } else {
                player.sendMessage(Lang.beaconLockedWithNMoreBlocks
                        .replaceText(builder -> builder.matchLiteral("[number]")
                                .replacement(Component.text(blocksNeeded))));
            }
        }
    }

    /**
     * Handles placement of defense blocks on owned beacons.
     *
     * @param player the player placing the block
     * @param block the defense block being placed
     * @param beacon the beacon being defended
     * @param team the player's team
     * @param event the block place event to cancel if invalid
     */
    private void handleDefenseBlockPlacement(Player player, Block block, BeaconObj beacon, Team team, BlockPlaceEvent event) {
        // Verify team ownership
        if (!beacon.getOwnership().equals(team)) {
            player.sendMessage(Lang.errorCanOnlyPlaceBlocks);
            event.setCancelled(true);
            return;
        }

        // Check height restrictions
        if (beacon.getY() + Settings.defenseHeight - 1 < block.getY()) {
            player.sendMessage(Lang.errorCanOnlyPlaceBlocksUpTo
                    .replaceText(builder -> builder.matchLiteral("[value]")
                            .replacement(Component.text(String.valueOf(Settings.defenseHeight)))));
            event.setCancelled(true);
            return;
        }

        // Verify player has required experience level
        int level = block.getY() - beacon.getY();
        int levelRequired = getRequiredLevel(level);

        if (levelRequired < 0) {
            return; // Invalid level configuration
        }

        if (player.getLevel() < levelRequired) {
            player.sendMessage(Lang.errorYouNeedToBeLevel
                    .replaceText(builder -> builder.matchLiteral("[value]")
                            .replacement(Component.text(String.valueOf(levelRequired)))));
            event.setCancelled(true);
            return;
        }

        // Send placement confirmation message
        sendDefensePlacementMessage(player, block, levelRequired);

        // Register the defense block
        beacon.addDefenseBlock(block, levelRequired, player.getUniqueId());
    }

    /**
     * Gets the required experience level for placing a defense block at a given height.
     *
     * @param level the height level above the beacon
     * @return the required experience level, or -1 if configuration is missing
     */
    private int getRequiredLevel(int level) {
        if (DEBUG) {
            getLogger().info("DEBUG: level = " + level);
        }

        try {
            return Settings.defenseLevels.get(level);
        } catch (Exception e) {
            getLogger().severe("Defense level for height " + level + " does not exist!");
            return -1;
        }
    }

    /**
     * Sends appropriate placement message to the player based on block type.
     *
     * @param player the player who placed the block
     * @param block the block that was placed
     * @param levelRequired the experience level required for this placement
     */
    private void sendDefensePlacementMessage(Player player, Block block, int levelRequired) {
        Component levelPlaced = levelRequired > 0
                ? Component.text(" [").append(Lang.generalLevel).append(Component.text(" " + levelRequired + "]"))
                : Component.text("");

        // Check if it's a link block
        if (Settings.linkBlocks.containsKey(block.getType())) {
            player.sendMessage(Lang.beaconLinkBlockPlaced.replaceText(
                    builder -> builder.matchLiteral("[range]")
                            .replacement(Component.text(String.valueOf(Settings.linkBlocks.get(block.getType()))))));
        }

        // Send defense placement message
        Component message = Lang.defenseText.get(block.getType());
        if (message == null) {
            player.sendMessage(Lang.beaconDefensePlaced.append(levelPlaced));
        } else {
            player.sendMessage(message.append(levelPlaced));
        }
    }

    /**
     * Handles breaking of defense blocks placed around beacons.
     *
     * <p>Enforces top-down removal for attacking teams and ownership rules for defending teams.
     * Also handles link block removal and beacon unlinking mechanics.
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBeaconBreak(BlockBreakEvent event) {
        if (DEBUG) {
            getLogger().info("BPD DEBUG: " + event.getEventName());
        }

        if (isNotBeaconzWorld(event.getBlock().getWorld())) {
            if (DEBUG) {
                getLogger().info("DEBUG: not right world");
            }
            return;
        }
        
        Player player = event.getPlayer();

        // Check lobby restrictions
        if (!validateLobbyAccess(player, event)) {
            return;
        }

        // Validate player team membership
        Scorecard scorecard = getGameMgr().getSC(player);
        if (DEBUG) {
            getLogger().info("DEBUG: scorecard = " + scorecard);
        }

        Optional<Team> team = validatePlayerTeam(player, scorecard, event);
        if (team.isEmpty()) {
            return;
        }

        // Check if this is a defense block
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);

        if (!isDefenseBlock(block, beacon)) {
            return;
        }

        DefenseBlock defenseBlock = beacon.getDefenseBlocks().get(block);

        // Handle based on team ownership
        if (team.get().equals(beacon.getOwnership())) {
            if (!handleOwnTeamDefenseInteraction(player, defenseBlock, event)) {
                return; // Event was cancelled
            }
        } else {
            if (!handleEnemyTeamDefenseInteraction(player, beacon, defenseBlock, event, true)) {
                return; // Event was cancelled
            }
        }

        // Block will be broken - clean up and handle link blocks
        beacon.removeDefenseBlock(block);
        handleLinkBlockBreak(player, block, beacon, team.get());
    }


    /**
     * Checks if a block is a defense block on an owned beacon.
     *
     * @param block the block to check
     * @param beacon the beacon to check against (may be null)
     * @return true if this is a defense block, false otherwise
     */
    private boolean isDefenseBlock(Block block, BeaconObj beacon) {
        if (beacon == null || beacon.getOwnership() == null) {
            if (DEBUG) {
                getLogger().info("DEBUG: This is not a beacon");
            }
            return false;
        }

        // Check if block is above beacon height
        if (block.getY() < beacon.getHeight()) {
            if (DEBUG) {
                getLogger().info("DEBUG: below beacon");
            }
            return false;
        }
        
        // Check if this block is registered as a defense block
        if (!beacon.getDefenseBlocks().containsKey(block)) {
            if (DEBUG) {
                getLogger().info("DEBUG: not defense block");
            }
            return false;
        }

        return true;
    }

    /**
     * Handles defense block interaction (break/damage) by the owning team.
     * Checks if the player placed the block or has sufficient level to remove it.
     *
     * @param player the player interacting with the block
     * @param defenseBlock the defense block data
     * @param event the cancellable event to cancel if invalid
     * @return true if the interaction is allowed, false if event was cancelled
     */
    private boolean handleOwnTeamDefenseInteraction(Player player, DefenseBlock defenseBlock, org.bukkit.event.Cancellable event) {
        // Check ownership - players can only remove their own blocks unless they have extra levels
        if (defenseBlock.getPlacer() != null && !defenseBlock.getPlacer().equals(player.getUniqueId())) {
            if (Settings.removaldelta < 0) {
                // Cannot remove other players' blocks
                player.sendMessage(Lang.errorYouCannotRemoveOtherPlayersBlocks);
                event.setCancelled(true);
                return false;
            } else if (Settings.removaldelta > 0) {
                // Need extra levels to remove other players' blocks
                int requiredLevel = Settings.removaldelta + defenseBlock.getLevel();
                if (player.getLevel() < requiredLevel) {
                    player.sendMessage(Lang.errorYouNeedToBeLevel
                            .replaceText(builder -> builder.matchLiteral("[value]")
                                    .replacement(Component.text(String.valueOf(requiredLevel)))));
                    event.setCancelled(true);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Handles defense block interaction (break/damage) by an enemy team.
     * Enforces top-down removal and level requirements.
     *
     * @param player the player interacting with the block
     * @param beacon the beacon being attacked
     * @param defenseBlock the defense block data
     * @param event the cancellable event to cancel if invalid
     * @param checkLock whether to check if the beacon is locked (for breaking, not damage)
     * @return true if the interaction is allowed, false if event was cancelled
     */
    private boolean handleEnemyTeamDefenseInteraction(Player player, BeaconObj beacon, DefenseBlock defenseBlock,
                                                      org.bukkit.event.Cancellable event, boolean checkLock) {
        // Check if beacon is locked (only for breaking, not damage)
        if (checkLock && beacon.isLocked()) {
            player.sendMessage(Lang.beaconLocked);
            event.setCancelled(true);
            return false;
        }

        // Get highest level - for break use beacon method, for damage cleanup AIR blocks first
        int highestLevel = checkLock ? beacon.getHighestBlockLevel() : cleanupAndFindHighestLevel(beacon);

        // Check player has required level
        if (player.getLevel() < highestLevel) {
            player.sendMessage(Lang.errorYouNeedToBeLevel
                    .replaceText(builder -> builder.matchLiteral("[value]")
                            .replacement(Component.text(String.valueOf(highestLevel)))));
            event.setCancelled(true);
            return false;
        }

        if (DEBUG) {
            getLogger().info("DEBUG: highest block is " + highestLevel);
        }

        // Enforce top-down removal
        int blockLevel = defenseBlock.getLevel();
        if (blockLevel < highestLevel) {
            player.sendMessage(Lang.beaconDefenseRemoveTopDown);
            event.setCancelled(true);
            return false;
        }

        return true;
    }

    /**
     * Handles link block breaking, including link removal and score updates.
     *
     * @param player the player who broke the block
     * @param block the block that was broken
     * @param beacon the beacon the block was on
     * @param team the player's team
     */
    private void handleLinkBlockBreak(Player player, Block block, BeaconObj beacon, Team team) {
        if (!Settings.linkBlocks.containsKey(block.getType())) {
            return;
        }

        // Notify player
        player.sendMessage(Lang.beaconLinkBlockBroken
                .replaceText(builder -> builder.matchLiteral("[range]")
                        .replacement(Component.text(String.valueOf(Settings.linkBlocks.get(block.getType()))))));

        // Destroy the block if configured
        if (Settings.destroyLinkBlocks) {
            block.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
            block.setType(Material.AIR);
        }

        // Remove longest link if configured
        if (Settings.removeLongestLink && beacon.removeLongestLink()) {
            player.sendMessage(Lang.beaconLinkLost);
            // Update score
            Scorecard scorecard = getGameMgr().getGame(team).getScorecard();
            scorecard.refreshScores(team);
            scorecard.refreshSBdisplay(team);
        }
    }

    /**
     * Handles damage to, but not breakage of, defense blocks placed around beacons.
     *
     * <p>Warns players about top-down removal requirements and plays warning sounds
     * for link blocks that will be destroyed.
     *
     * @param event the block damage event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDefenseDamage(BlockDamageEvent event) {
        if (DEBUG) {
            getLogger().info("DEBUG: " + event.getEventName());
        }

        if (isNotBeaconzWorld(event.getBlock().getWorld())) {
            return;
        }

        Player player = event.getPlayer();

        // Check lobby restrictions
        if (!validateLobbyAccess(player, event)) {
            return;
        }

        // Validate player team membership
        Scorecard scorecard = getGameMgr().getSC(player);
        Optional<Team> team = validatePlayerTeam(player, scorecard, event);
        if (team.isEmpty()) {
            return;
        }

        // Check if this is a defense block
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeaconAt(new Point2D.Double(block.getX(), block.getZ()));

        if (!isDefenseBlock(block, beacon)) {
            return;
        }

        // Warn about link block destruction
        if (Settings.destroyLinkBlocks && Settings.linkBlocks.containsKey(block.getType())) {
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 2F);
            player.sendMessage(Lang.beaconAmplifierBlocksCannotBeRecovered);
        }

        DefenseBlock defenseBlock = beacon.getDefenseBlocks().get(block);

        // Handle based on team ownership
        if (team.get().equals(beacon.getOwnership())) {
            handleOwnTeamDefenseInteraction(player, defenseBlock, event);
        } else {
            handleEnemyTeamDefenseInteraction(player, beacon, defenseBlock, event, false);
        }
    }

    /**
     * Cleans up defense blocks that have been removed (are now AIR) and finds the highest remaining level.
     * This handles blocks removed by creative mode or gravity.
     *
     * @param beacon the beacon to clean up
     * @return the highest defense block level remaining
     */
    private int cleanupAndFindHighestLevel(BeaconObj beacon) {
        int highestLevel = 0;
        Iterator<Entry<Block, DefenseBlock>> iterator = beacon.getDefenseBlocks().entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<Block, DefenseBlock> entry = iterator.next();
            if (entry.getKey().getType() == Material.AIR) {
                // Clean up blocks removed by creative mode or gravity
                iterator.remove();
            } else {
                highestLevel = Math.max(highestLevel, entry.getValue().getLevel());
            }
        }

        return highestLevel;
    }

    /**
     * Prevents liquid from flowing onto or above beacon structures.
     *
     * <p>Cancels the flow event if the destination block is above a beacon,
     * protecting beacon defenses from water and lava damage.
     *
     * @param event the block flow event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockFlow(final BlockFromToEvent event) {
        if (DEBUG) {
            getLogger().info("DEBUG: " + event.getEventName());
        }

        if (!event.getBlock().isLiquid() || isNotBeaconzWorld(event.getBlock().getWorld())) {
            if (DEBUG) {
                getLogger().info("DEBUG: not right world or not liquid");
            }
            return;
        }

        if (getRegister().isAboveBeacon(event.getToBlock().getLocation())) {
            event.setCancelled(true);
            if (DEBUG) {
                getLogger().info("DEBUG: stopping flow");
            }
        }
    }

    /**
     * Checks if the given world is NOT the Beaconz game world.
     *
     * @param world the world to check
     * @return true if this is NOT the Beaconz world, false if it is the Beaconz world
     */
    private boolean isNotBeaconzWorld(World world) {
        return world == null || !world.equals(getBeaconzWorld());
    }

}
