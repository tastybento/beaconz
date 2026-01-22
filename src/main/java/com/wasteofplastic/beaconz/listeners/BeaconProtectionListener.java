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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.wasteofplastic.beaconz.core.BeaconObj;
import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.game.Game;
import com.wasteofplastic.beaconz.config.Lang;
import com.wasteofplastic.beaconz.config.Settings;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Listener class that handles protection mechanisms for beacons in the game.
 * This class manages various events to ensure beacon integrity and proper gameplay:
 * <ul>
 *   <li>Prevents unauthorized block placement/breaking near beacons</li>
 *   <li>Protects beacon structures from explosions and piston manipulation</li>
 *   <li>Prevents liquid placement above beacon beams</li>
 *   <li>Controls player interaction with beacons (damage, capture)</li>
 *   <li>Protects animals and inventories on owned beacons</li>
 *   <li>Manages player collision with beacon beams</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class BeaconProtectionListener extends BeaconzPluginDependent implements Listener {

    /** Debug flag for verbose logging */
    private final static boolean DEBUG = false;

    /**
     * A bidirectional hashmap to track which players are currently standing on beacon beams.
     * Maps player UUID to the BeaconObj they're standing on. Bidirectional allows efficient
     * lookups in both directions (player to beacon and beacon to player).
     */
    private static final BiMap<UUID, BeaconObj> standingOn = HashBiMap.create();

    /**
     * Set of player UUIDs who have been recently notified about beacon capture mechanics.
     * Used to prevent spam messages - players are temporarily added here after notification
     * and removed after 1 minute.
     */
    private final @NotNull Set<UUID> notified = new HashSet<>();

    /**
     * Constructs a new BeaconProtectionListener and initializes beacon defense mechanisms.
     * <p>
     * This constructor performs two key initialization tasks:
     * <ol>
     *   <li>Scans all online players to detect if they're currently standing on beacon beams</li>
     *   <li>Starts a repeating task that ejects players from beacon beams with a velocity push</li>
     * </ol>
     * The beam defense mechanism prevents players from blocking beacon beams by physically
     * pushing them away with a randomized velocity vector and playing a harp sound effect.
     *
     * @param plugin The Beaconz plugin instance
     */
    public BeaconProtectionListener(Beaconz plugin) {
        super(plugin);

        // Initialize the tracking map - clear any stale data from previous loads
        standingOn.clear();

        // Scan all currently online players to populate the initial standing-on-beacon state
        for (Player player : getServer().getOnlinePlayers()) {
            // Only check players in the Beaconz game world
            if (player.getWorld().equals(getBeaconzWorld())) {
                // Check if there's a beacon at the player's current X/Z coordinates
                BeaconObj beacon = getRegister().getBeaconAt(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
                if (beacon != null) {
                    // Check if player's Y coordinate is within the beacon beam's vertical range
                    // Players must be at or above the beacon's Y level and below the defense height limit
                    if (player.getLocation().getBlockY() >= beacon.getY()
                            && player.getLocation().getBlockY() < beacon.getY() + Settings.defenseHeight) {
                        // Track this player as standing on this beacon
                        standingOn.put(player.getUniqueId(), beacon);
                    }
                }
            }
        }

        // Start a repeating task that runs every second (20 ticks) to eject players from beacon beams
        new BukkitRunnable() {
            @Override
            public void run() {
                // Iterate through all players currently tracked as standing on beacons
                for (Entry<UUID, BeaconObj> entry : standingOn.entrySet()) {
                    Player player = getServer().getPlayer(entry.getKey());

                    // Verify the player is still valid, online, in the game world, and not in the lobby
                    if (player != null && player.isOnline()
                            && player.getWorld().equals(getBeaconzWorld())
                            && !getGameMgr().isPlayerInLobby(player)
                            && player.getLocation().getBlockY() > entry.getValue().getY()
                            && player.getLocation().getBlockY() < entry.getValue().getY() + Settings.defenseHeight) {

                        // Eject the player from the beacon beam with a random horizontal push and upward boost
                        Random rand = new Random();
                        // Gaussian distribution gives more natural-feeling random directions
                        // Y velocity of 1.2 provides a noticeable upward boost
                        player.setVelocity(new Vector(rand.nextGaussian(), 1.2, rand.nextGaussian()));

                        // Play a pleasant harp sound to indicate the beacon defense is active
                        getBeaconzWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1F, 1F);
                    }
                }
            }
        }.runTaskTimer(getBeaconzPlugin(), 0L, 20L); // Start immediately (0L delay), repeat every second (20 ticks)
    }

    /**
     * Handles damage events to beacon blocks (but not actual breakage).
     * <p>
     * This event is triggered when a player starts to break a block but before it's fully broken.
     * It serves two main purposes:
     * <ol>
     *   <li>Validates that players have proper permissions and team status to damage beacons</li>
     *   <li>Checks if a beacon is clear of obstructions before allowing capture attempts</li>
     *   <li>Notifies players about capture mechanics (once per minute to avoid spam)</li>
     * </ol>
     *
     * Players can only capture beacons that:
     * <ul>
     *   <li>Are not owned by their team</li>
     *   <li>Have been cleared of surrounding blocks</li>
     * </ul>
     *
     * @param event The BlockDamageEvent containing information about the damaged block and player
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());

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

        // Get the player's team assignment
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

        // Check if this is the capture block (obsidian/glass directly above the beacon)
        // This is the block players must break to capture the beacon
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            // Verify this is actually a registered beacon, not just a random beacon block
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                // Check if the beacon area is clear of obstructions
                // Uncleared beacons cannot be captured unless already owned by the team
                if (beacon.isNotClear() && (beacon.getOwnership() == null || !beacon.getOwnership().equals(team))) {
                    // Beacon has blocks in its beam - must be cleared first
                    player.sendMessage(Lang.errorClearAroundBeacon);
                    event.setCancelled(true);
                } else if (!notified.contains(player.getUniqueId())){
                    // Beacon is clear and capturable - inform the player
                    player.sendMessage(Lang.beaconBreakToOwn.color(NamedTextColor.GREEN));

                    // Add to notified set to prevent message spam
                    notified.add(player.getUniqueId());

                    // Remove from notified set after 1 minute (1200 ticks)
                    Bukkit.getScheduler().runTaskLater(beaconzPlugin,
                            () -> notified.remove(player.getUniqueId()), 1200L);
                }
            }
        } else {
            // Player is attempting to break a different part of the beacon structure
            // (pyramid blocks, etc.) - this is handled by other event handlers
        }

    }

    /**
     * Protects beacon structures from explosion damage.
     * <p>
     * This handler intercepts all explosion events and removes any beacon-related blocks
     * from the damage list. This ensures that beacons cannot be destroyed by:
     * <ul>
     *   <li>TNT explosions</li>
     *   <li>Creeper explosions</li>
     *   <li>Wither explosions</li>
     *   <li>Any other explosive damage sources</li>
     * </ul>
     *
     * Beacons can only be captured through the proper game mechanics (breaking the
     * capture block after clearing the area), not through explosions.
     *
     * @param event The EntityExplodeEvent containing the explosion location and affected blocks
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());

        // Quick world check - only process events in the Beaconz world
        World world = event.getLocation().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Remove all beacon blocks from the explosion's block list
        // This uses a lambda predicate to filter out beacon-related blocks
        event.blockList().removeIf(block -> getRegister().isBeacon(block));
    }

    /**
     * Prevents natural block spreading (like vines, grass, mycelium) above beacon beams.
     * <p>
     * This handler ensures that beacon beams remain clear by automatically converting
     * any spreading blocks above a beacon into air. This prevents:
     * <ul>
     *   <li>Vines growing down into beacon beams</li>
     *   <li>Grass/mycelium spreading above beacons</li>
     *   <li>Other natural spreading mechanics that could obstruct beams</li>
     * </ul>
     *
     * Note: There's a TODO comment suggesting special handling for leaves in the future.
     *
     * @param event The BlockSpreadEvent containing information about the spreading block
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockSpread(BlockSpreadEvent event) {
        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Check if there's a beacon at this X/Z coordinate
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlock().getX(), event.getBlock().getZ());

        // If the spreading block is above the beacon level, clear it
        if (beacon != null && beacon.getY() < event.getBlock().getY()) {
            // TODO: Future enhancement - special handling for leaf blocks
            // For now, convert any spreading block above a beacon to air
            event.getBlock().setType(Material.AIR);
        }
    }

    /**
     * Prevents pistons from being used to manipulate beacon structures or block beacon beams.
     * <p>
     * This handler prevents two types of piston abuse:
     * <ol>
     *   <li>Pushing beacon structure blocks (which would destroy the beacon)</li>
     *   <li>Pushing blocks into the space above beacons (which would block the beam)</li>
     * </ol>
     *
     * Without this protection, players could use pistons to:
     * <ul>
     *   <li>Destroy enemy beacons without proper capture mechanics</li>
     *   <li>Place blocks above beacons indirectly, bypassing placement restrictions</li>
     * </ul>
     *
     * @param event The BlockPistonExtendEvent containing the piston and affected blocks
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Check each block being pushed by the piston
        for (Block b : event.getBlocks()) {
            // Protection 1: Prevent pushing any part of a beacon structure
            if (getRegister().isBeacon(b)) {
                event.setCancelled(true);
                return;
            }

            // Protection 2: Prevent pushing blocks into the space above beacons
            // Calculate where this block will end up after being pushed
            Block testBlock = b.getRelative(event.getDirection());
            BeaconObj beacon = getRegister().getBeaconAt(testBlock.getX(), testBlock.getZ());

            // If the destination is above a beacon, cancel the push
            if (beacon != null && beacon.getY() < testBlock.getY()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents sticky pistons from pulling beacon structure blocks.
     * <p>
     * This handler ensures that sticky pistons cannot be used to pull and move
     * blocks that are part of a beacon structure. Without this protection, players
     * could use sticky pistons to:
     * <ul>
     *   <li>Pull pyramid blocks away, destroying the beacon structure</li>
     *   <li>Pull the capture block (obsidian/glass) without proper capture mechanics</li>
     *   <li>Dismantle enemy beacons without following game rules</li>
     * </ul>
     *
     * @param event The BlockPistonRetractEvent containing the piston and affected blocks
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Check each block being pulled by the sticky piston
        for (Block b : event.getBlocks()) {
            // If any block is part of a beacon structure, cancel the entire pull operation
            if (getRegister().isBeacon(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Prevents players from placing liquids (water/lava) above or near beacon beams using buckets.
     * <p>
     * This handler stops players from using buckets to place liquids that would:
     * <ul>
     *   <li>Block beacon beams directly</li>
     *   <li>Flow into beacon beam areas</li>
     *   <li>Interfere with beacon functionality</li>
     * </ul>
     *
     * The check validates both the target block and ensures it's not in the vertical
     * column above any beacon. Players receive an error message explaining why the
     * action is blocked.
     *
     * @param event The PlayerBucketEmptyEvent containing the bucket placement information
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());

        // Quick world check - only process events in the Beaconz world
        World world = event.getBlockClicked().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Calculate the target block where liquid would be placed
        // This is the clicked block plus the face direction
        Block b = event.getBlockClicked().getRelative(event.getBlockFace());

        // Check if there's a beacon at this X/Z coordinate
        BeaconObj beacon = getRegister().getBeaconAt(b.getX(), b.getZ());

        // Prevent placing liquids at or above the beacon level
        if (beacon != null && beacon.getY() <= event.getBlockClicked().getY()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.beaconCannotPlaceLiquids);
            return;
        }

        // Additional check: ensure the block isn't in the vertical beam space above any beacon
        if (getRegister().isAboveBeacon(b.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.beaconCannotPlaceLiquids);
        }
    }

    /**
     * Prevents dispensers from placing liquids (water/lava) above beacon beams.
     * <p>
     * This handler is the automated counterpart to {@link #onBucketEmpty(PlayerBucketEmptyEvent)}.
     * It prevents players from using dispensers to bypass the bucket placement restrictions.
     * Without this check, players could:
     * <ul>
     *   <li>Place a dispenser next to a beacon beam</li>
     *   <li>Fill it with water or lava buckets</li>
     *   <li>Use redstone to dispense liquids into the beam area</li>
     * </ul>
     *
     * The handler uses the modern BlockData API to determine which direction the dispenser
     * is facing, then checks if that direction points toward a beacon beam. If so, the
     * dispense action is cancelled with a sound effect.
     *
     * @param event The BlockDispenseEvent containing the dispenser and item being dispensed
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onDispense(final BlockDispenseEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());

        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        if (DEBUG)
            getLogger().info("DEBUG: " + event.getItem().getType());

        // Only check water and lava bucket dispensing
        if (!event.getItem().getType().equals(Material.WATER_BUCKET)
                && !event.getItem().getType().equals(Material.LAVA_BUCKET)) {
            return;
        }

        if (DEBUG)
            getLogger().info("DEBUG: " + event.getBlock().getType());

        // Verify the block is actually a dispenser
        if (!event.getBlock().getType().equals(Material.DISPENSER)) {
            return;
        }

        // Use modern BlockData API to get the dispenser's facing direction
        BlockData blockData = event.getBlock().getBlockData();
        if (blockData instanceof Directional directional) {
            // Calculate which block the liquid would be dispensed into
            Block b = event.getBlock().getRelative(directional.getFacing());

            if (DEBUG)
                getLogger().info("DEBUG: " + b.getLocation());

            // Check if the target block is in a beacon beam
            if (getRegister().isAboveBeacon(b.getLocation())) {
                // Play an error sound to indicate the dispense was blocked
                world.playSound(b.getLocation(), Sound.BLOCK_STONE_BREAK, 1F, 2F);
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents liquids (water/lava) from flowing into beacon beam areas.
     * <p>
     * This handler complements the bucket and dispenser checks by preventing natural
     * liquid flow mechanics from interfering with beacons. It catches:
     * <ul>
     *   <li>Water flowing from nearby sources into beacon beams</li>
     *   <li>Lava flowing into beacon areas</li>
     *   <li>Liquids spreading horizontally toward beacons</li>
     * </ul>
     *
     * The handler also prevents liquid flow outside the game area boundaries,
     * which helps maintain clean borders between different game instances.
     *
     * @param event The BlockFromToEvent containing the source and destination blocks
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLiquidFlow(final BlockFromToEvent event) {
        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Only check horizontal flows (where X or Z coordinates change)
        // Vertical flows (same X and Z) are allowed
        if (event.getToBlock().getX() != event.getBlock().getX()
                || event.getToBlock().getZ() != event.getBlock().getZ()) {

            // Check if there's a beacon at the destination coordinates
            BeaconObj beacon = getRegister().getBeaconAt(
                    event.getToBlock().getX(),
                    event.getToBlock().getZ());

            // Prevent flow into blocks above beacon level
            if (beacon != null && beacon.getY() < event.getToBlock().getY()) {
                event.setCancelled(true);
                return;
            }

            // Additional check: prevent flows outside of the game area
            // This helps maintain clean boundaries between games
            Game game = getGameMgr().getGame(event.getBlock().getLocation());
            if (game == null) {
                // Source block is outside any game area - cancel the flow
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles and restricts block placement around beacons and in game areas.
     * <p>
     * This handler enforces several important placement restrictions:
     * <ol>
     *   <li>Prevents non-ops from placing blocks in the lobby area</li>
     *   <li>Prevents players from placing blocks outside their game's boundaries</li>
     *   <li>Prevents any block placement above beacon beams</li>
     *   <li>Ensures only team members can place blocks in the game</li>
     * </ol>
     *
     * These restrictions maintain game integrity by preventing:
     * <ul>
     *   <li>Lobby griefing by regular players</li>
     *   <li>Cross-game interference</li>
     *   <li>Beacon beam blocking</li>
     *   <li>Participation by unassigned players</li>
     * </ul>
     *
     * @param event The BlockPlaceEvent containing the placed block and player information
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (DEBUG)
            getLogger().info("DEBUG: " + event.getEventName());

        // Quick world check - only process events in the Beaconz world
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        
        Player player = event.getPlayer();

        // Lobby protection - only Ops can place blocks in the lobby area
        if (getGameMgr().isPlayerInLobby(player)) {
            if (player.isOp()) {
                return; // Ops have full access
            } else {
                event.setCancelled(true); // Non-ops cannot place blocks in lobby
                return;
            }
        }
        
        // Ops have unrestricted access everywhere (useful for admin maintenance)
        if (player.isOp()) {
            return;
        }
        
        // Validate the player is within a game area
        // This assumes adjacent games are always > 5 blocks apart (see TODO)
        Game game = getGameMgr().getGame(event.getBlock().getLocation());
        if (game == null) {
            // Player is trying to place a block outside any game area
            event.setCancelled(true);
            player.sendMessage(Lang.errorYouCannotDoThat);
            return;
        }

        // Verify the player is assigned to a team
        Team team = getGameMgr().getPlayerTeam(player);
        if (team == null) {
            // Player has no team assignment - cannot place blocks
            event.setCancelled(true);
            return;
        }

        // Prevent placing blocks in the vertical column above any beacon
        BeaconObj beacon = getRegister().getBeaconAt(
                event.getBlock().getX(),
                event.getBlock().getZ());

        if (beacon != null && beacon.getY() < event.getBlock().getY()) {
            // This block would be above a beacon - not allowed
            event.setCancelled(true);
            event.getPlayer().sendMessage(Lang.errorYouCannotBuildThere);
        }
    }

    /**
     * Protects animals on owned beacons from damage by enemy players.
     * <p>
     * This handler prevents enemy teams from harming animals that are located on
     * beacons owned by other teams. This protection mechanism:
     * <ul>
     *   <li>Allows teams to safely keep animals on their beacons</li>
     *   <li>Prevents enemy players from killing livestock as a griefing tactic</li>
     *   <li>Enables beacons to serve as safe havens for animal farming</li>
     * </ul>
     *
     * Animals can only be damaged on a beacon by:
     * <ul>
     *   <li>Players from the team that owns the beacon</li>
     *   <li>Non-player damage sources are completely blocked</li>
     * </ul>
     *
     * Players who attempt to damage protected animals receive a message identifying
     * which team owns the beacon.
     *
     * @param event The EntityDamageByEntityEvent for animal damage caused by an entity
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent event) {
        // Quick world check - only process events in the Beaconz world
        World world = event.getEntity().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Only protect animal entities
        if (!(event.getEntity() instanceof Animals)) {
            return;
        }

        // Check if the animal is on a beacon with an owner
        BeaconObj beacon = getRegister().getBeaconAt(event.getEntity().getLocation());
        if (beacon != null && beacon.getOwnership() != null) {
            // Beacon has an owner - check who's doing the damage
            if (event.getDamager() instanceof Player player) {
                // Damage is from a player - verify they're on the same team as the beacon owner
                Game game = getGameMgr().getGame(player.getLocation());
                if (game != null) {
                    Team team = game.getScorecard().getTeam(player); 

                    // If the player's team doesn't match the beacon owner, protect the animal
                    if (!beacon.getOwnership().equals(team)) {
                        player.sendMessage(Lang.triangleThisBelongsTo
                                .replaceText(builder -> builder.matchLiteral("[team]")
                                        .replacement(beacon.getOwnership().displayName())));
                        event.setCancelled(true);
                    }
                }          
            } else {
                // Damage is from a non-player source (mob, environmental, etc.)
                // Always protect animals on beacons from non-player damage
                event.setCancelled(true);
            }
        }
    }

    /**
     * Provides blanket protection for animals and leash hitches on any beacon from all damage.
     * <p>
     * This handler provides a second layer of protection that complements
     * {@link #onEntityDamage(EntityDamageByEntityEvent)}. While that handler specifically
     * checks team ownership for entity-caused damage, this handler provides complete
     * immunity from ALL damage types for:
     * <ul>
     *   <li>Animals (cows, pigs, chickens, horses, etc.)</li>
     *   <li>Leash hitches (prevents leash breaking on beacons)</li>
     * </ul>
     *
     * Protected damage types include:
     * <ul>
     *   <li>Environmental damage (fall, fire, drowning, etc.)</li>
     *   <li>Suffocation damage</li>
     *   <li>Lightning strikes</li>
     *   <li>Any other damage not caused by entities</li>
     * </ul>
     *
     * This makes beacons completely safe zones for animal farming, regardless of
     * beacon ownership status.
     *
     * @param event The EntityDamageEvent for any damage to animals or leash hitches
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        // Quick world check - only process events in the Beaconz world
        World world = event.getEntity().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Only protect animals and leash hitches
        if (!(event.getEntity() instanceof Animals || event.getEntity() instanceof LeashHitch)) {
            return;
        }

        // Check if the entity is located on any beacon
        BeaconObj beacon = getRegister().getBeaconAt(event.getEntity().getLocation());
        if (beacon != null) {
            // Entity is on a beacon - provide complete damage immunity
            event.setCancelled(true);
        }
    }

    /**
     * Protects inventories (chests, horses, minecarts) on enemy beacons from access.
     * <p>
     * This handler prevents players from accessing storage containers that are located
     * on beacons owned by enemy teams. This protection applies to:
     * <ul>
     *   <li>Chests and other storage blocks</li>
     *   <li>Horse inventories (saddles, armor, etc.)</li>
     *   <li>Storage minecarts</li>
     *   <li>Any other block-based inventory</li>
     * </ul>
     *
     * The protection logic:
     * <ol>
     *   <li>Determines the physical location of the inventory</li>
     *   <li>Checks if that location is on a beacon</li>
     *   <li>Verifies the beacon is owned by a different team than the player</li>
     *   <li>Blocks access and notifies the player if unauthorized</li>
     * </ol>
     *
     * Players can only access inventories on:
     * <ul>
     *   <li>Beacons owned by their team</li>
     *   <li>Unowned beacons</li>
     *   <li>Locations not on beacons</li>
     * </ul>
     *
     * Note: There's commented-out code for triangle field protection that may be
     * implemented in the future.
     *
     * @param event The InventoryOpenEvent containing the inventory and player
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(final InventoryOpenEvent event) {
        // Quick world check - only process events in the Beaconz world
        World world = event.getPlayer().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }

        // Ignore player's own inventory
        if (event.getInventory().getType().equals(InventoryType.PLAYER)) {
            return;
        }

        // Determine the physical location of the inventory being accessed
        InventoryHolder invHolder = event.getInventory().getHolder();
        Location invLoc = null;

        // Different inventory types store their location differently
        if (invHolder instanceof Horse) {
            // Horse inventories use the horse's location
            invLoc = ((Horse) invHolder).getLocation();
        } else if (invHolder instanceof Minecart) {
            // Minecart inventories use the minecart's location
            invLoc = ((Minecart) invHolder).getLocation();
        } else {
            // Block-based inventories (chests, etc.) have a direct location
            invLoc = event.getInventory().getLocation();
        }

        // Some inventories don't have a location (virtual inventories)
        if (invLoc == null) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Game game = getGameMgr().getGame(player.getLocation());

        if (game != null) {
            // Get the player's team assignment
            Team team = game.getScorecard().getTeam(player);

            // Check if the inventory is on a beacon with an owner
            BeaconObj beacon = getRegister().getBeaconAt(invLoc);
            if (beacon != null) {
                // Verify the player's team matches the beacon owner
                if (!beacon.getOwnership().equals(team)) {
                    // Player is trying to access an enemy beacon's inventory
                    player.sendMessage(Lang.triangleThisBelongsTo
                            .replaceText(builder -> builder.matchLiteral("[team]")
                                    .replacement(beacon.getOwnership().displayName())));
                    event.setCancelled(true);
                }
            }

            // TODO: Future feature - Triangle field protection
            // This would prevent access to inventories within triangular areas
            // formed between three beacons owned by the same team
            /*
            for (TriangleField triangle : getRegister().getTriangle(invLoc.getBlockX(), invLoc.getBlockZ())) {
                if (!triangle.getOwner().equals(team)) {
                    player.sendMessage(Lang.triangleThisBelongsTo.replace("[team]", triangle.getOwner().getDisplayName()));
                    event.setCancelled(true);
                    return;
                }
            }*/
        }
    }

    /**
     * Gets the bidirectional map tracking which players are currently standing on beacon beams.
     * <p>
     * This map is used by the beacon defense mechanism to track and eject players from
     * beacon beams. Other components can use this to query:
     * <ul>
     *   <li>Which beacon a specific player is standing on (UUID → BeaconObj)</li>
     *   <li>Which player is standing on a specific beacon (BeaconObj → UUID)</li>
     * </ul>
     *
     * The bidirectional nature of the map allows efficient lookups in both directions,
     * which is useful for various game mechanics that need to coordinate between
     * player positions and beacon locations.
     *
     * @return A bidirectional map of player UUIDs to BeaconObj instances
     */
    public static BiMap<UUID, BeaconObj> getStandingOn() {
        return standingOn;
    }
    
}
