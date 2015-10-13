package com.wasteofplastic.beaconz;

import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.scoreboard.Team;

public class BeaconListeners implements Listener {
    private Random rand = new Random();
    private Beaconz plugin;

    /**
     * @param plugin
     */
    public BeaconListeners(Beaconz plugin) {
	this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInit(WorldInitEvent event)
    {
	if (event.getWorld().equals(plugin.getBeaconzWorld())) {
	    if (!plugin.getBeaconzWorld().getPopulators().contains(plugin.getBp())) {
		event.getWorld().getPopulators().add(plugin.getBp());
	    }
	}
    }

    /**
     * Protects the underlying beacon from any damage
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconDamage(BlockDamageEvent event) {
	World world = event.getBlock().getWorld();
	if (!world.equals(plugin.getBeaconzWorld())) {
	    return;
	}
	// Check if the block is a beacon or the surrounding pyramid
	Block b = event.getBlock();
	if (plugin.getRegister().isBeacon(b)) {
	    event.setCancelled(true);
	}
    }

    /**
     * Handle breakage of the top part of a beacon
     * @param event
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBeaconBreak(BlockBreakEvent event) {
	World world = event.getBlock().getWorld();
	if (!world.equals(plugin.getBeaconzWorld())) {
	    plugin.getLogger().info("DEBUG: not right world");
	    return;
	}
	Player player = event.getPlayer();
	// Get the player's team
	Team team = plugin.getScorecard().getTeam(player);
	if (team == null) {
	    // TODO: Probably should put the player in a team
	    event.setCancelled(true);
	    player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
	    return;
	}
	// Check if the block is a beacon or the surrounding pyramid
	Block b = event.getBlock();
	if (b.getRelative(BlockFace.DOWN).getType().equals(Material.BEACON)) {
	    plugin.getLogger().info("DEBUG:beacon below");
	    // Check if this is a real beacon
	    if (plugin.getRegister().isBeacon(b.getRelative(BlockFace.DOWN))) {
		plugin.getLogger().info("DEBUG: registered beacon");
		// It is a real beacon
		if (b.getType().equals(Material.OBSIDIAN)) {
		    plugin.getLogger().info("DEBUG: obsidian");
		    //Claiming unowned beacon
		    b.setType(plugin.getScorecard().getBlockID(team).getItemType());
		    b.setData(plugin.getScorecard().getBlockID(team).getData());
		    event.setCancelled(true);
		    // Register the beacon to this team
		    plugin.getRegister().addBeacon(team, b.getLocation());
		    player.sendMessage(ChatColor.GREEN + "You captured a beacon!");
		} else {
		    plugin.getLogger().info("DEBUG: another block");
		    Team blockTeam = plugin.getScorecard().getTeamFromBlock(b);
		    if (blockTeam != null) {
			plugin.getLogger().info("DEBUG: known team block");
			if (team.equals(blockTeam)) {
			    // You can't destroy your own beacon
			    player.sendMessage(ChatColor.RED + "You cannot destroy your own beacon");
			    event.setCancelled(true);
			    return;
			}
			// Enemy team has lost a beacon!
			player.sendMessage(ChatColor.GOLD + blockTeam.getDisplayName() + " team has lost a beacon!");
			player.sendMessage(ChatColor.GOLD + "TODO: remove score and links etc.");
			plugin.getRegister().deleteBeacon(b);
			b.setType(Material.OBSIDIAN);
			event.setCancelled(true);
		    } else {
			plugin.getLogger().info("DEBUG: unknown team block");
		    }
		}
	    } 
	}
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPaperUse(final PlayerInteractEvent event) {
	plugin.getLogger().info("DEBUG: " + event.getEventName());
	if (!event.getItem().getType().equals(Material.PAPER)) {
	    return;
	}
	if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
	    return;
	}
	World world = event.getClickedBlock().getWorld();
	if (!world.equals(plugin.getBeaconzWorld())) {
	    plugin.getLogger().info("DEBUG: not right world");
	    return;
	}
	Player player = event.getPlayer();
	// Get the player's team
	Team team = plugin.getScorecard().getTeam(player);
	if (team == null) {
	    // TODO: Probably should put the player in a team
	    event.setCancelled(true);
	    player.sendMessage(ChatColor.RED + "You must be in a team to play in this world");
	    return;
	}
	// Check if the block is a beacon or the surrounding pyramid
	Block b = event.getClickedBlock();
	final BeaconObj beacon = plugin.getRegister().getBeacon(b);
	if (beacon == null) {
	    plugin.getLogger().info("DEBUG: not a beacon");
	    return;
	}
	// Check the team
	if (beacon.getOwnership() == null || !beacon.getOwnership().equals(team)) {
	    player.sendMessage(ChatColor.RED + "You must capture this beacon first!");
	    return;
	}
	// Make a map!
	player.sendMessage(ChatColor.GREEN + "You made a beacon map! Take it to another beacon to link them up!");
	int amount = event.getItem().getAmount() - 1;
	MapView map = Bukkit.createMap(plugin.beaconzWorld);
	//map.setWorld(plugin.getBeaconzWorld());
	map.setCenterX((int)beacon.getLocation().getX());
	map.setCenterZ((int)beacon.getLocation().getY());
	map.getRenderers().clear();
	map.addRenderer(new BeaconMap(plugin));
	event.getItem().setType(Material.MAP);
	event.getItem().setAmount(1);
	event.getItem().setDurability(map.getId());
	// Each map is unique and the durability defines the map ID, register it
	plugin.getRegister().addBeaconMap(map.getId(), beacon);
	plugin.getLogger().info("DEBUG: beacon id = " + beacon.getId());
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
    }

}
