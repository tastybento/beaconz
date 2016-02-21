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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.awt.geom.Line2D;
import java.util.*;
import java.util.Map.Entry;

public class BeaconListeners extends BeaconzPluginDependent implements Listener {

    /**
     * A bi-drectional hashmap to track players standing on beaconz
     */
    private BiMap<UUID, BeaconObj> standingOn = HashBiMap.create();
    private Beaconz plugin;

    public BeaconListeners(Beaconz plugin) {
        super(plugin);
        // Work out if players are on beacons or not
        standingOn.clear();
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(getBeaconzWorld())) {
                BeaconObj beacon = getRegister().getBeaconAt(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
                if (beacon != null) {
                    // Add players to beacon standing
                    standingOn.put(player.getUniqueId(), beacon);
                } 
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInit(WorldInitEvent event)
    {
        //Bukkit.getLogger().info("On World Init called");
        if (event.getWorld().equals(getBeaconzWorld())) {
            if (!getBeaconzWorld().getPopulators().contains(getBlockPopulator())) {
                event.getWorld().getPopulators().add(getBlockPopulator());
            }
        }
    }


    /**
     * Handles damage to, but not breakage of a beacon. Warns players to clear a beacon before
     * capture can occur. See block break event for capture.
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
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

        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);

        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return;
        }
        // Check for obsidian/glass breakage - i.e., capture
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            // Check if this is a real beacon
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                // It is a real beacon
                // Check that the beacon is clear of blocks
                if (!beacon.isClear()) {
                    // You can't capture an uncleared beacon
                    player.sendMessage(ChatColor.RED + "Clear around the beacon first!");
                    event.setCancelled(true);
                    return;
                }
            }
        } else {
            // Attempt to break another part of the beacon
        }        		

    }

    /**
     * Protects the underlying beacon from any damage
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onExplode(EntityExplodeEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getLocation().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        // Check if the block is a beacon or the surrounding pyramid and remove it from the damaged blocks
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            if (getRegister().isBeacon(it.next())) {
                it.remove();
            }
        }
    }

    /**
     * Prevents trees from growing above the beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockSpread(BlockSpreadEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlock().getX(),event.getBlock().getZ());
        if (beacon != null && beacon.getY() < event.getBlock().getY()) {
            switch (event.getBlock().getType()) {
            // Allow leaves to grow over the beacon
            case LEAVES:
            case LEAVES_2:
                break;
            default:
                // For everything else, make sure there is air
                event.getBlock().setType(Material.AIR);
            }

        }
    }

    /**
     * Prevents blocks from being piston pushed above a beacon or a piston being used to remove beacon blocks
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        for (Block b : event.getBlocks()) {
            // If any block is part of a beacon cancel it
            if (getRegister().isBeacon(b)) {
                event.setCancelled(true);
                return;
            }
            Block testBlock = b.getRelative(event.getDirection());
            BeaconObj beacon = getRegister().getBeaconAt(testBlock.getX(),testBlock.getZ());
            if (beacon != null && beacon.getY() < testBlock.getY()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevents the tipping of liquids over the beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlockClicked().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlockClicked().getX(),event.getBlockClicked().getZ());
        if (beacon != null && beacon.getY() <= event.getBlockClicked().getY()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot place liquids above a beacon!");
        }
    }

    /**
     * Processes players coming directly into the game
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(final PlayerJoinEvent event) {  
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            final Player player = event.getPlayer();
            final UUID playerUUID = player.getUniqueId();                   	
            
            // Send player to BeaconzWorld lobby area
            getGameMgr().getLobby().tpToRegionSpawn(player);
 
            // Check messages
            // Load any messages for the player
            // plugin.getLogger().info("DEBUG: Checking messages for " +
            // player.getName());
            final List<String> messages = getMessages().getMessages(playerUUID);
            if (messages != null) {
                // plugin.getLogger().info("DEBUG: Messages waiting!");
                getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        player.sendMessage(ChatColor.AQUA + "Beaconz News");
                        int i = 1;
                        for (String message : messages) {
                            player.sendMessage(i++ + ": " + message);
                        }
                        // Clear the messages
                        getMessages().clearMessages(playerUUID);
                    }
                }, 40L);
            } // else {
            // plugin.getLogger().info("no messages");
            // }
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onWorldEnter(final PlayerChangedWorldEvent event) {
        // Entering Beaconz world
    	if (event.getPlayer().getWorld().equals((getBeaconzWorld()))) {         
            // Send player to lobby
            getGameMgr().getLobby().tpToRegionSpawn(event.getPlayer());	
    	}
    }    

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldExit(final PlayerChangedWorldEvent event) {
        // Exiting Beaconz world
    	if (event.getFrom().equals((getBeaconzWorld()))) {         
            // Remove player from map and remove his scoreboard
            standingOn.remove(event.getPlayer().getUniqueId());
            event.getPlayer().setScoreboard(Bukkit.getServer().getScoreboardManager().getNewScoreboard());	
    	}
    }    

    /**
     * Prevents liquid flowing over the beacon beam
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLiquidFlow(final BlockFromToEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        // Only bother with horizontal flows
        if (event.getToBlock().getX() != event.getBlock().getX() || event.getToBlock().getZ() != event.getBlock().getZ()) {
            //getLogger().info("DEBUG: " + event.getEventName());
            BeaconObj beacon = getRegister().getBeaconAt(event.getToBlock().getX(),event.getToBlock().getZ());
            if (beacon != null && beacon.getY() < event.getToBlock().getY()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handles placing of blocks around a beacon
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        Player player = event.getPlayer();
    	// Only Ops place blocks in the lobby
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
        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);
        // Stop placing blocks on a beacon
        BeaconObj beacon = getRegister().getBeaconAt(event.getBlock().getX(),event.getBlock().getZ());
        if (beacon != null && beacon.getY() < event.getBlock().getY()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot build on top of a beacon!");
            return;
        }
    }


    /**
     * Handle breakage of the top part of a beacon
     * @param event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconBreak(BlockBreakEvent event) {
        //getLogger().info("DEBUG: " + event.getEventName());
        World world = event.getBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        //getLogger().info("DEBUG: This is a beacon");
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

        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);

        // Check if the block is a beacon or the surrounding pyramid
        Block block = event.getBlock();
        BeaconObj beacon = getRegister().getBeacon(block);
        if (beacon == null) {
            return;
        }
        // Cancel any breakage
        event.setCancelled(true);
        // Check for obsidian/glass breakage - i.e., capture
        if (block.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
            //getLogger().info("DEBUG:beacon below");
            // Check if this is a real beacon
            if (getRegister().isBeacon(block.getRelative(BlockFace.DOWN))) {
                //getLogger().info("DEBUG: registered beacon");
                // It is a real beacon
                if (block.getType().equals(Material.OBSIDIAN)) {
                    // Check that the beacon is clear of blocks
                    if (!beacon.isClear()) {
                        // You can't capture an uncleared beacon
                        player.sendMessage(ChatColor.RED + "Clear around the beacon first!");
                        event.setCancelled(true);
                        return;
                    }
                    //getLogger().info("DEBUG: obsidian");
                    //Claiming unowned beacon
                    block.setType(getGameMgr().getSC(player).getBlockID(team).getItemType());
                    block.setData(getGameMgr().getSC(player).getBlockID(team).getData());
                    // Register the beacon to this team
                    getRegister().setBeaconOwner(beacon,team);
                    player.sendMessage(ChatColor.GREEN + "You captured a beacon!");
                } else {
                    //getLogger().info("DEBUG: another block");
                    Team beaconTeam = beacon.getOwnership();
                    if (beaconTeam != null) {
                        //getLogger().info("DEBUG: known team block");
                        if (team.equals(beaconTeam)) {
                            // You can't destroy your own beacon
                            player.sendMessage(ChatColor.RED + "You cannot destroy your own beacon");
                            event.setCancelled(true);
                            return;
                        }
                        // Check that the beacon is clear of blocks
                        if (!beacon.isClear()) {
                            // You can't capture an uncleared beacon
                            player.sendMessage(ChatColor.RED + "Clear around the beacon first!");
                            event.setCancelled(true);
                            return;
                        }
                        // Enemy team has lost a beacon!
                        // Taunt other teams
                        getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team destroyed " + beaconTeam.getDisplayName() + "'s beacon!");
                        getMessages().tellTeam(player, player.getDisplayName() + " destroyed one of " + beaconTeam.getDisplayName() + "'s beacons!");
                        player.sendMessage(ChatColor.GREEN + "You destroyed " + beaconTeam.getDisplayName() + " team's beacon!");

                        getRegister().removeBeaconOwnership(beacon);
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);
                        // Remove any standers
                        if (standingOn.containsValue(beacon)) {
                            standingOn.inverse().remove(beacon);
                        }
                    } else {
                        getRegister().removeBeaconOwnership(beacon);
                        block.setType(Material.OBSIDIAN);
                        event.setCancelled(true);
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
                    //getLogger().info("DEBUG: player has " + player.getTotalExperience() + " and needs " + Settings.beaconMineExpRequired);
                    if (!testForExp(player, Settings.beaconMineExpRequired)) {
                        player.sendMessage(ChatColor.RED + "You do not have enough experience to mine this beacon!");
                        return;
                    }
                    Random rand = new Random();
                    //getLogger().info("DEBUG: random number = " + value);
                    if (beacon.getOwnership().equals(team)) {
                        // Own team
                        int value = rand.nextInt(Settings.teamGoodies.lastKey()) + 1;
                        Entry<Integer, ItemStack> en = Settings.teamGoodies.floorEntry(value);
                        if (en != null && en.getValue() != null) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());
                            if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                beacon.resetHackTimer();
                                player.sendMessage(ChatColor.GREEN + "Success! Beacon is exhausted. Try again in " + (Settings.mineCoolDown/60000) + " minute(s)");
                            } else {
                                player.sendMessage(ChatColor.GREEN + "Success!");
                            }
                        } else {
                            player.getWorld().spawnEntity(player.getLocation(),EntityType.ENDERMITE);
                            player.sendMessage(ChatColor.RED + "Failure!");
                        }
                    } else {
                        // Enemy
                        int value = rand.nextInt(Settings.enemyGoodies.lastKey()) + 1;
                        Entry<Integer, ItemStack> en = Settings.enemyGoodies.floorEntry(value);
                        if (en != null && en.getValue() != null) {
                            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), en.getValue());
                            if (rand.nextInt(100) < Settings.beaconMineExhaustChance) {
                                beacon.resetHackTimer();
                                player.sendMessage(ChatColor.GREEN + "Success! Beacon is exhausted. Try again in " + (Settings.mineCoolDown/60000) + " minute(s)");
                            } else {
                                player.sendMessage(ChatColor.GREEN + "Success!");
                            }
                        } else {
                            player.getWorld().spawnEntity(player.getLocation(),EntityType.ENDERMITE);
                            player.sendMessage(ChatColor.RED + "Failure!");
                        }
                    }
                } else {
                    // Damage player
                    //getLogger().info("DEBUG: hack cooldown " + Settings.overHackEffects);
                    int num = (int) (beacon.getHackTimer() + Settings.mineCoolDown - System.currentTimeMillis())/50;
                    for (String effect : Settings.minePenalty) {
                        String[] split = effect.split(":");
                        if (split.length == 2) {
                            int amplifier = 1;
                            if (NumberUtils.isNumber(split[1])) {
                                amplifier = Integer.valueOf(split[1]);
                                //getLogger().info("DEBUG: Amplifier is " + amplifier);
                            }
                            PotionEffectType potionEffectType = PotionEffectType.getByName(split[0]);
                            if (potionEffectType != null) {
                                player.addPotionEffect(new PotionEffect(potionEffectType, num,amplifier));
                                //getLogger().info("DEBUG: Applying " + potionEffectType.toString() + ":" + amplifier + " for " + num + " ticks");
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
     * Handles using signs in the lobby to join games
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSignClick(final PlayerInteractEvent event) {
    	if (getGameMgr().getLobby().isPlayerInRegion(event.getPlayer())) {
        	if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
        		if (event.getClickedBlock().getState() instanceof Sign) {
            		Sign sign = (Sign) event.getClickedBlock().getState();
            		if (Arrays.toString(sign.getLines()).toLowerCase().contains("beaconz")) {
            			Boolean foundgame = false;
            			for (int i = 1; i < 3; i++) {
            				String gamename = sign.getLine(i);
                			if (getGameMgr().getGame(gamename) != null) {
                				getGameMgr().getGame(gamename).join(event.getPlayer());
                				foundgame = true;
                				break;
                			}
            			}
            			if (!foundgame) {
            				event.getPlayer().sendMessage("That sign does not point to an active game");
            			}
            		}
            	}	
        	}    	    		
    	}
    }
    
    /**
     * Handles the event of hitting a beacon with paper or a map
     * @param event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPaperMapUse(final PlayerInteractEvent event) {
        //getLogger().info("DEBUG: paper map " + event.getEventName());
        if (Settings.pairLinking) {
            // Not used if pair linking is used
            return;
        }
        if (!event.hasItem()) {
            return;
        }
        if (!event.getItem().getType().equals(Material.PAPER) && !event.getItem().getType().equals(Material.MAP)) {
            return;
        }
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        World world = event.getClickedBlock().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            //getLogger().info("DEBUG: not right world");
            return;
        }
        Player player = event.getPlayer();
        // Ignore player in lobby
        if (getGameMgr().isPlayerInLobby(player)) {
        	return;
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
        // Apply triangle effects
        applyEffects(player, getRegister().getTriangle(player.getLocation().getBlockX(), player.getLocation().getBlockZ()), team);

        // Check if the block is a beacon or the surrounding pyramid
        Block b = event.getClickedBlock();
        final BeaconObj beacon = getRegister().getBeacon(b);
        if (beacon == null) {
            //getLogger().info("DEBUG: not a beacon");
            return;
        }
        // Check the team
        if (beacon.getOwnership() == null || !beacon.getOwnership().equals(team)) {
            player.sendMessage(ChatColor.RED + "You must capture this beacon first!");
            event.setCancelled(true);
            return;
        }
        if (event.getItem().getType().equals(Material.PAPER)) {
            // Make a map!
            player.sendMessage(ChatColor.GREEN + "You made a beacon map! Take it to another beacon to link them up!");
            int amount = event.getItem().getAmount() - 1;
            MapView map = Bukkit.createMap(getBeaconzWorld());
            //map.setWorld(getBeaconzWorld());
            map.setCenterX(beacon.getX());
            map.setCenterZ(beacon.getZ());
            map.getRenderers().clear();
            map.addRenderer(new BeaconMap(getBeaconzPlugin()));
            map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
            event.getItem().setType(Material.MAP);
            event.getItem().setAmount(1);
            event.getItem().setDurability(map.getId());
            // Each map is unique and the durability defines the map ID, register it
            getRegister().addBeaconMap(map.getId(), beacon);
            //getLogger().info("DEBUG: beacon id = " + beacon.getId());
            if (amount > 0) {
                HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(new ItemStack(Material.PAPER, amount));
                if (!leftOver.isEmpty()) {
                    for (ItemStack stack: leftOver.values()) {
                        player.getLocation().getWorld().dropItemNaturally(player.getLocation(), stack);
                    }
                }
            }
            ItemMeta meta = event.getItem().getItemMeta();
            meta.setDisplayName("Beacon map for " + beacon.getName());
            event.getItem().setItemMeta(meta);
            // Stop the beacon inventory opening
            event.setCancelled(true);
            return;
        } else {
            // Map!
            BeaconObj mappedBeacon = getRegister().getBeaconMap(event.getItem().getDurability());
            if (mappedBeacon == null) {
                // This is not a beacon map
                return;
            }
            event.setCancelled(true);
            if (Settings.linkDistance >= 0 && Settings.expDistance > 0) {
                // Check if the player has sufficient experience to link the beacons
                double distance = beacon.getLocation().distance(mappedBeacon.getLocation());
                distance -= Settings.linkDistance;
                if (distance > 0) {
                    if (!testForExp(player, (int)(distance/Settings.expDistance))) {
                        player.sendMessage(ChatColor.RED + "You do not have enough experience to link to this beacon!");
                        player.sendMessage(ChatColor.RED + "You can link up to " + (int)(Settings.expDistance * player.getTotalExperience()) + " blocks away.");
                        player.sendMessage(ChatColor.RED + "This beacon is " + (int)distance + " blocks away.");
                        return;
                    }
                }    
            }
            if (linkBeacons(player, team, beacon, mappedBeacon)) {
                player.sendMessage(ChatColor.GREEN + "The map disintegrates!");
                player.setItemInHand(null);
            }
        }
    }

    /**
     * Make sure all player held maps have triangle overlays. (todo: make sure all maps on item frames do as well)
     * There seem to be some bugs around this. It doesn't always take on the first try.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMapHold(final PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItem(event.getNewSlot());
        if (itemInHand == null) return;
        if (!Material.MAP.equals(itemInHand.getType())) {
            return;
        }
        if (!player.getWorld().equals(getBeaconzWorld())) {
            return;
        }
        MapView map = Bukkit.getMap(itemInHand.getDurability());
        for (MapRenderer renderer : map.getRenderers()) {
            if (renderer instanceof TerritoryMapRenderer) {
                return;
            }
        }
        map.addRenderer(new TerritoryMapRenderer(getBeaconzPlugin()));
    }

    /**
     * Tries to link two beacons
     * @param player
     * @param team
     * @param beacon
     * @param otherBeacon
     * @return true if link is made successfully
     */
    private boolean linkBeacons(Player player, Team team, BeaconObj beacon,
            BeaconObj otherBeacon) {
        if (beacon.equals(otherBeacon)) {
            player.sendMessage(ChatColor.RED + "You cannot link a beacon to itself!");
            return false;
        }
        if (beacon.getNumberOfLinks() == 8) {
            player.sendMessage(ChatColor.RED + "This beacon already has 8 outbound links!");
            return false;
        }
        // Check if this link already exists
        if (beacon.getLinks().contains(otherBeacon)) {
            player.sendMessage(ChatColor.RED + "Link already exists!");
            return false;
        }
        // Proposed link
        Line2D proposedLink = new Line2D.Double(beacon.getLocation(), otherBeacon.getLocation());
        // Check if the link crosses opposition team's links
        //getLogger().info("DEBUG: Check if the link crosses opposition team's links");
        for (Line2D line : getRegister().getEnemyLinks(team)) {
            //getLogger().info("DEBUG: checking line " + line.getP1() + " to " + line.getP2());
            if (line.intersectsLine(proposedLink)) {
                player.sendMessage(ChatColor.RED + "Link cannot cross enemy link!");
                return false;
            }
        }

        // Link the two beacons!
        LinkResult result = beacon.addOutboundLink(otherBeacon);
        if (result.isSuccess()) {
            player.sendMessage(ChatColor.GREEN + "Link created!");
            player.sendMessage(ChatColor.GREEN + "This beacon now has " + beacon.getNumberOfLinks() + " links");
            player.getWorld().playSound(player.getLocation(), Sound.FIREWORK_LARGE_BLAST, 1F, 1F);
            player.getWorld().spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
            if (Settings.pairLinking) {
                // Tell the other player if it was done via a pairing
                if (standingOn.containsValue(otherBeacon)) {
                    Player otherPlayer = getServer().getPlayer(standingOn.inverse().get(otherBeacon));
                    if (otherPlayer != null) {
                        otherPlayer.sendMessage(ChatColor.GREEN + "Link created!");

                        otherPlayer.getWorld().playSound(otherPlayer.getLocation(), Sound.FIREWORK_LARGE_BLAST, 1F, 1F);
                        otherPlayer.getWorld().spawnEntity(otherPlayer.getLocation(), EntityType.EXPERIENCE_ORB);
                    }
                    // Tell the team
                    getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " and " + otherPlayer.getDisplayName()
                            + ChatColor.GREEN + " created a link!");
                    // Taunt other teams
                    getMessages().tellOtherTeams(team, ChatColor.GOLD + team.getDisplayName() + " team made a link!");
                }
            } else {
                // Tell the team
                getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " created a link!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Link could not be created!");
            return false;
        }
        if (result.getFieldsMade() > 0) {
            if (result.getFieldsMade() == 1) {
                player.sendMessage(ChatColor.GREEN + "Triangle created! New score = " + getGameMgr().getSC(player).getScore(team, "area"));
                getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " created a triangle! New team score = " + getGameMgr().getSC(player).getScore(team, "area"));
                // Taunt other teams
                getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team made a tringle!");
            } else {
                player.sendMessage(ChatColor.GREEN + String.valueOf(result.getFieldsMade()) + " triangles created! New score = " + getGameMgr().getSC(player).getScore(team, "area"));
                getMessages().tellTeam(player, player.getDisplayName() + ChatColor.GREEN + " created " + String.valueOf(result.getFieldsMade()) + " triangles! New team score = " + getGameMgr().getSC(player).getScore(team, "area"));
                // Taunt other teams
                getMessages().tellOtherTeams(team, ChatColor.RED + team.getDisplayName() + " team made " + String.valueOf(result.getFieldsMade()) + " triangles!");
            }
            for (int i = 0; i < result.getFieldsMade(); i++) {
                player.getWorld().spawnEntity(player.getLocation(), EntityType.EXPERIENCE_ORB);
            }
        }
        if (result.getFieldsFailedToMake() > 0) {
            if (result.getFieldsFailedToMake() == 1) {
                player.sendMessage(ChatColor.RED + "One triangle could not be created because of overlapping enemy elements!");
            } else {
                player.sendMessage(ChatColor.RED + String.valueOf(result.getFieldsFailedToMake()) + " triangle could not be created because of overlapping enemy elements!");
            }
        }          
        return true;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
    	
    	// Remember that teleporting is not detected as player movement.. 
    	// If we want to catch movement by teleportation, we have to keep track of the players to-from by ourselves 
    	
        // Only proceed if there's been a change in X or Z coords
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        World world = event.getTo().getWorld();
        if (!world.equals(getBeaconzWorld())) {
            return;
        }
        Player player = event.getPlayer();
        Region regionfrom = getGameMgr().getRegion(event.getFrom());
        Region regionto = getGameMgr().getRegion(event.getTo());
        
        // Check if player is trying to leave a region by moving over a region boundary
        // And send him back to whence he came
        if (regionfrom != null && regionfrom != regionto) {
        	if (event.getFrom().distance(event.getTo()) < 2.5) {
        		float pitch = player.getLocation().getPitch();
				float yaw = player.getLocation().getYaw();
				Vector direction = player.getLocation().getDirection(); 									
				player.teleport(player.getLocation().add(event.getFrom().toVector().subtract(event.getTo().toVector()).normalize()));
				player.getLocation().setPitch(pitch);
				player.getLocation().setPitch(yaw);
				player.getLocation().setDirection(direction);
				player.sendMessage(ChatColor.YELLOW + "That's the limit of the game region, you can't go any further that way.");
				return;
        	}
        }
        
        // Check if player changed regions and process region exit and enter methods
        if (regionfrom != null && regionfrom != regionto) {
        	regionfrom.exit(player);        	
        }
        if (regionto != null && regionfrom != regionto) {
        	regionto.enter(player);
        }
        // Nothing from here on applies to Lobby...
        if (getGameMgr().isPlayerInLobby(player)) {
        	return;
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

        // Run the following if players can create links by standing on them in pairs (or more)
        if (Settings.pairLinking) {
            // Check if player is standing on a beacon
            BeaconObj beacon = getRegister().getBeaconAt(event.getTo().getBlockX(), event.getTo().getBlockZ());
            if (beacon != null) {
                // Check if the beacon is captured by this team
                if (beacon.getOwnership() != null && beacon.getOwnership().equals(team)) {
                    // Check if they are on the same beacon
                    BeaconObj currentBeacon = standingOn.get(player.getUniqueId());
                    if (currentBeacon == null || !currentBeacon.equals(beacon)) {
                        standingOn.remove(player.getUniqueId());
                        // Check if any other team players are on beacons and link to them
                        for (UUID others : standingOn.keySet()) {
                            Player otherplayer = getServer().getPlayer(others);
                            if (otherplayer != null) {
                                // Check team
                            	Team otherteam = getGameMgr().getPlayerTeam(player);
                    	    	if (otherteam != null && otherteam.equals(team)) {
                                    // Only do if beacons are different locations
                                    if (otherplayer.getLocation().getBlockX() != event.getTo().getBlockX() 
                                            && otherplayer.getLocation().getBlockZ() != event.getTo().getBlockZ()) {
                                        // Check if this link already exists
                                        if (!beacon.getLinks().contains(standingOn.get(others))) {
                                            // Try to link
                                            linkBeacons(player,team,beacon,standingOn.get(others));
                                        }
                                    }
                                }
                            }
                        }
                        // Forceput removes any other player's on this beacon
                        standingOn.forcePut(player.getUniqueId(), beacon); 
                    }
                }
            } else {
                // Remove player from map
                standingOn.remove(player.getUniqueId());
            }
        }
        // Check the From
        List<TriangleField> from = getRegister().getTriangle(event.getFrom().getBlockX(), event.getFrom().getBlockZ());
        // Check the To
        List<TriangleField> to = getRegister().getTriangle(event.getTo().getBlockX(), event.getTo().getBlockZ());
        // Outside any field
        if (from.isEmpty() && to.isEmpty()) {
            return;
        }
        // Check if to is not a triangle
        if (to.size() == 0) {
            // Leaving a control triangle
        	player.sendMessage("Leaving " + from.get(0).getOwner().getDisplayName() + "'s control area");
            return;
        }
        // Apply triangle effects
        applyEffects(player, to, team);

        // Entering a field, or moving to a stronger field
        if (from.size() < to.size()) {
        	player.sendMessage("Now entering " + to.get(0).getOwner().getDisplayName() + "'s area of control level " + to.size());
            return;
        }
        if (to.size() < from.size()) {
        	player.sendMessage(to.get(0).getOwner().getDisplayName() + "'s control level dropping to " + to.size());
            return;
        }
    }

    /**
     * Applies triangle effects to a player
     * @param player
     * @param to
     * @param team
     */
    private void applyEffects(Player player, List<TriangleField> to, Team team) {
        if (to.isEmpty() || team == null) {
            return;
        }
        // Apply bad stuff
        // Enemy team
        Team triangleOwner = to.get(0).getOwner();
        Collection<PotionEffect> effects = new ArrayList<PotionEffect>();
        if (triangleOwner != null && !triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.enemyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.enemyFieldEffects.get(i));
                }
            }
            player.addPotionEffects(effects);
        }
        effects.clear();
        // Friendly triangle
        if (triangleOwner != null && triangleOwner.equals(team)) {
            for (int i = 0; i <= to.size(); i++) {
                if (Settings.friendlyFieldEffects.containsKey(i)) {
                    effects.addAll(Settings.friendlyFieldEffects.get(i));
                }
            }
            player.addPotionEffects(effects);
        }         
    }

    /**
     * Tests if a player has the required experience to perform the action. If so, the experience
     * is deducted. This function updates the client's UI exp bar.
     * @param player
     * @param xpRequired
     * @return true if sufficient experience points otherwise false
     */
    public boolean testForExp(Player player , int xpRequired){
        int xp = player.getTotalExperience();
        if (xp >= xpRequired) {
            int total = player.getTotalExperience() - xpRequired;
            player.setTotalExperience(total);
            player.setLevel(0);
            player.setExp(0);
            for(;total > player.getExpToLevel();) {
                total -= player.getExpToLevel();
                player.setLevel(player.getLevel()+1);
            }
            float exp = (float)total / (float)player.getExpToLevel();
            player.setExp(exp);
            return true;
        }
        return false;
    }    
}
