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

import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Lang;
import com.wasteofplastic.beaconz.Settings;

import net.kyori.adventure.text.Component;

/**
 * Handles lobby events
 * 
 * @author tastybento
 *
 */
public class LobbyListener extends BeaconzPluginDependent implements Listener {

    private final static boolean DEBUG = false;

    /**
     * @param plugin
     */
    public LobbyListener(Beaconz plugin) {
        super(plugin);
    }

    /**
     * Handles using signs in the lobby to join games
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSignClick(final PlayerInteractEvent event) {
        // We are only interested in hitting signs
        if (!event.hasBlock()) {
            return;
        }
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }
        if (!Tag.SIGNS.isTagged(event.getClickedBlock().getType())) {
            return;
        }
        // Check world
        if (!event.getClickedBlock().getWorld().equals(getBeaconzWorld())) {
            return; 
        }
        if (getGameMgr().getLobby().isPlayerInRegion(event.getPlayer())) {
            // Check for accidental creative mode hitting
            if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                event.getPlayer().sendMessage(Lang.adminUseSurvival);
                event.setCancelled(true);
            }
            Sign sign = (Sign) event.getClickedBlock().getState();
            @NotNull SignSide side = sign.getSide(Side.FRONT);
            if (side.line(0).equals(Lang.adminSignKeyword)) {
                for (int i = 1; i < 4; i++) {
                    Component gamename = side.line(i);
                    if (getGameMgr().getGame(gamename) != null) {
                        if (getGameMgr().getGame(gamename).isOver()) {
                            event.getPlayer().sendMessage(Lang.scoreGameOver);
                        } else {
                            getGameMgr().getGame(gamename).join(event.getPlayer());
                        }
                        return;
                    }
                }
                event.getPlayer().sendMessage(Lang.errorNotReady);
                event.getPlayer().sendMessage(Lang.errorNoSuchGame);
            }
        }
    } 

    /**
     * Tells admin if the game sign has been placed successfully
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSignPlace(final SignChangeEvent event) {
        // Check world
        if (!event.getBlock().getWorld().equals(getBeaconzWorld())) {
            return; 
        }
        if (getGameMgr().getLobby().isPlayerInRegion(event.getPlayer())) {
            if (event.line(0).contains(Lang.adminSignKeyword)) {
                for (int i = 1; i < 4; i++) {
                    if (getGameMgr().getGame(event.line(i)) != null) {
                        event.getPlayer().sendMessage(Lang.adminGameSignPlaced.append(Component.text(" - ").append(event.line(i))));
                        return;
                    }
                }
                event.getPlayer().sendMessage(Lang.errorNoSuchGame);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (DEBUG) {
            getLogger().info(e.getEventName());
        }
        // If not at spawn, return, or if grid is not loaded yet.
        if (!getGameMgr().getLobby().contains(e.getLocation())) {
            return;
        }

        if (!Settings.allowLobbyEggs && (e.getSpawnReason().equals(SpawnReason.SPAWNER_EGG) || e.getSpawnReason().equals(SpawnReason.EGG))) {
            e.setCancelled(true);
            return;
        }

        // Deal with mobs
        if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime) {        
            if (!Settings.allowLobbyMobSpawn) {
                // Mobs not allowed to spawn
                e.setCancelled(true);
                return;
            }
        }

        // If animals can spawn, check if the spawning is natural, or
        // egg-induced
        if (e.getEntity() instanceof Animals) {
            if (!Settings.allowLobbyAnimalSpawn) {
                // Animals are not allowed to spawn
                e.setCancelled(true);
            }
        }
    }  

}
