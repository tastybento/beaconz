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
     * Processes players coming directly into the game
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
    public void onJoin(final PlayerJoinEvent event) {
        if (event.getPlayer().getWorld().equals(getBeaconzWorld())) {
            final Player player = event.getPlayer();
            final UUID playerUUID = player.getUniqueId();
            // Check if game is still in progress
            Game game = getGameMgr().getGame(event.getPlayer().getLocation());
            if (game == null) {
                // Send player to BeaconzWorld lobby area
                getGameMgr().getLobby().tpToRegionSpawn(player);
            } else {
                // Join the game but stay at the last location
                game.join(player, false);
            }
            // Check messages
            // Load any messages for the player
            if (DEBUG)
                getLogger().info("DEBUG: Checking messages for " + player.getName());
            final List<String> messages = getMessages().getMessages(playerUUID);
            if (messages != null) {
                // plugin.getLogger().info("DEBUG: Messages waiting!");
                getServer().getScheduler().runTaskLater(getBeaconzPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        player.sendMessage(ChatColor.AQUA + Lang.titleBeaconzNews);
                        int i = 1;
                        for (String message : messages) {
                            player.sendMessage(i++ + ": " + message);
                        }
                        // Clear the messages
                        getMessages().clearMessages(playerUUID);
                    }
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
    }

}
