package com.wasteofplastic.beaconz.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.Lang;

/**
 * Handles player join and leave events
 * @author tastybento
 *
 */
public class PlayerJoinLeaveListener extends BeaconzPluginDependent implements Listener {
    private final static boolean DEBUG = false;

    public PlayerJoinLeaveListener(Beaconz plugin) {
        super(plugin);
    }
    /**
     * Processes players coming directly into the game world
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onJoin(final PlayerJoinEvent event) {
        // Check if player is in the BeaconzWorld
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            final Player player = event.getPlayer();
            final UUID playerUUID = player.getUniqueId();
            
            // Write this player's name to the database
            getBeaconzPlugin().getNameStore().savePlayerName(player.getName(), player.getUniqueId());

            // Check if game is still in progress
            Game game = getGameMgr().getGame(event.getPlayer().getLocation());
            if (game == null || game.isOver() || game.isGameRestart()) {
                // Send player to BeaconzWorld lobby area
                if (DEBUG) {
                    getLogger().info("DEBUG: Game is " + game);
                    if (game != null) {
                        getLogger().info("DEBUG: Game is over " + game.isOver());
                        getLogger().info("DEBUG: Game is restart " + game.isGameRestart());
                    }
                }
                getGameMgr().getLobby().tpToRegionSpawn(player, true);
                getGameMgr().getLobby().enterLobby(event.getPlayer());
                
            } else {
                if (DEBUG)
                    getLogger().info("DEBUG: game exists");
                if (game.getScorecard().getTeam(player) == null) {
                    if (DEBUG)
                        getLogger().info("DEBUG: Player is not in a team");
                    // Send player to BeaconzWorld lobby area
                    getGameMgr().getLobby().tpToRegionSpawn(player, true);
                    getGameMgr().getLobby().enterLobby(event.getPlayer());
                    
                } else {
                    if (DEBUG) {
                        getLogger().info("DEBUG: Player is in team - " + game.getScorecard().getTeam(player));
                        getLogger().info("DEBUG: Player is in team - " + game.getScorecard().getTeam(player).getDisplayName());
                    }
                    // Join the game but stay at the last location
                    game.join(player, false);
                }
            }
            // Check messages
            // Load any messages for the player
            if (DEBUG)
                getLogger().info("DEBUG: Checking messages for " + player.getName());
            final List<String> messages = getMessages().getMessages(playerUUID);
            if (messages != null) {
                // plugin.getLogger().info("DEBUG: Messages waiting!");
                getServer().getScheduler().runTaskLater(getBeaconzPlugin(), () -> {
                    player.sendMessage(Lang.titleBeaconzNews);
                    int i = 1;
                    for (String message : messages) {
                        player.sendMessage(i++ + ": " + message);
                    }
                    // Clear the messages
                    getMessages().clearMessages(playerUUID);
                }, 40L);
            }
            if (DEBUG)
                getLogger().info("DEBUG: no messages");
        }
    }

    /**
     * Removes any residual beaconz effects when player logs out
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onLeave(final PlayerQuitEvent event) {
        BeaconProtectionListener.getStandingOn().remove(event.getPlayer().getUniqueId());
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            for (PotionEffect effect : event.getPlayer().getActivePotionEffects())
                event.getPlayer().removePotionEffect(effect.getType());
        }
        final Game fromGame = getGameMgr().getGame(event.getPlayer().getLocation());
        if (fromGame != null) {
            //getBeaconzStore().storeInventory(event.getPlayer(), fromGame.getName(), null);   
        }
    }

}
