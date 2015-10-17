package com.wasteofplastic.beaconz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

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
	if (event.getWorld().equals(Beaconz.getBeaconzWorld())) {
	    if (!Beaconz.getBeaconzWorld().getPopulators().contains(plugin.getBp())) {
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
	if (!world.equals(Beaconz.getBeaconzWorld())) {
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
	if (!world.equals(Beaconz.getBeaconzWorld())) {
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
    public void onPaperMapUse(final PlayerInteractEvent event) {
	//plugin.getLogger().info("DEBUG: " + event.getEventName());
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
	if (!world.equals(Beaconz.getBeaconzWorld())) {
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
	    event.setCancelled(true);
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
	    MapView map = Bukkit.createMap(Beaconz.beaconzWorld);
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
	    // Stop the beacon inventory opening
	    event.setCancelled(true);
	    return;
	} else {
	    // Map!
	    BeaconObj mappedBeacon = plugin.getRegister().getBeaconMap(event.getItem().getDurability());
	    if (mappedBeacon == null) {
		// This is not a beacon map
		return;
	    }
	    if (beacon.equals(mappedBeacon)) {
		player.sendMessage(ChatColor.RED + "You cannot link a beacon to itself!");
		event.setCancelled(true);
		return;
	    }
	    if (beacon.getOutgoing() == 8) {
		player.sendMessage(ChatColor.RED + "This beacon already has 8 outbound links!");
		event.setCancelled(true);
		return;
	    }
	    // Link the two beacons!
	    boolean result = beacon.addOutboundLink(mappedBeacon);
	    player.sendMessage(ChatColor.GREEN + "Link created! The beacon map disintegrates!");
	    player.sendMessage(ChatColor.GREEN + "This beacon now has " + beacon.getOutgoing() + " links");
	    if (result) {
		player.sendMessage(ChatColor.GREEN + "Control field created! New score = " + plugin.getRegister().getScore(team));
	    }
	    player.setItemInHand(null);
	    player.getWorld().playSound(player.getLocation(), Sound.FIREWORK_LARGE_BLAST, 1F, 1F);
	    visualize(player, beacon, mappedBeacon, team);
	    event.setCancelled(true);
	}
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
	World world = event.getTo().getWorld();
	if (!world.equals(Beaconz.beaconzWorld)) {
	    return;
	}
	// Only proceed if there's been a change in X or Z coords
	if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
	    return;
	}
	// Get the player's team
	Team team = plugin.getScorecard().getTeam(event.getPlayer());
	if (team == null) {
	    return;
	}
	// Check the From
	List<TriangleField> from = plugin.getRegister().getTriangle(event.getFrom().getBlockX(), event.getFrom().getBlockZ());
	// Check the To
	List<TriangleField> to = plugin.getRegister().getTriangle(event.getTo().getBlockX(), event.getTo().getBlockZ());
	// Outside any field
	if (from.isEmpty() && to.isEmpty()) {
	    return;
	}
	// Check if to is not a triangle
	if (to.size() == 0) {
	    // Leaving a control triangle
	    event.getPlayer().sendMessage("Leaving " + from.get(0).getFaction().getDisplayName() + "'s control area");
	    return;
	}

	// Apply bad stuff
	// Enemy team
	/*
	 * 1 | Hunger
	 * 2 | Mining fatigue 1, hunger
	 * 3 | Weakness 1, mining fatigue 2, hunger
	 * 4 | Slowness 1, weakness 2, mining fatigue 2, hunger 2
	 * 5 | Nausea 1, slowness 2, weakness 3, mining fatigue 3, hunger 2
	 * 6 | Poison 1, nausea 1, slowness 3, weakness 3, mining fatigue 3
	 * 7 | Blindness, poison 2, slowness 4, weakness 4, mining fatigue 4
	 * 8+ | Wither*, blindness, slowness 5, weakness 5, mining fatigue 5
	 */
	Team toOwner = null;
	toOwner = to.get(0).getFaction();
	Collection<PotionEffect> effects = new ArrayList<PotionEffect>();
	if (toOwner != null && !toOwner.equals(team)) {
	    switch (to.size()) {
	    default:
	    case 8:
		effects.add(new PotionEffect(PotionEffectType.WITHER,200,to.size()-7));
	    case 7:
		effects.add(new PotionEffect(PotionEffectType.BLINDNESS,200,1));
	    case 6:
		effects.add(new PotionEffect(PotionEffectType.POISON,200,1));
	    case 5:
		effects.add(new PotionEffect(PotionEffectType.CONFUSION,200,1));
	    case 4:
		effects.add(new PotionEffect(PotionEffectType.SLOW,200,1));
	    case 3:
		effects.add(new PotionEffect(PotionEffectType.WEAKNESS,200,1));
	    case 2:
		effects.add(new PotionEffect(PotionEffectType.SLOW_DIGGING,200,1));
	    case 1:
		effects.add(new PotionEffect(PotionEffectType.HUNGER,200,1));
	    }
	    event.getPlayer().addPotionEffects(effects);
	}
	// Entering a field, or moving to a stronger field
	if (from.size() < to.size()) {
	    event.getPlayer().sendMessage("Now entering " + toOwner.getDisplayName() + "'s area of control level " + to.size());
	    return;
	}
	if (to.size() < from.size()) {
	    event.getPlayer().sendMessage(toOwner.getDisplayName() + "'s control level dropping to " + to.size());
	    return;
	}
    }

    private void visualize(Player player, BeaconObj from, BeaconObj to, Team team) {
	plugin.getLogger().info("from = " + from + " to = " + to);
	double diffX = from.getLocation().getX() - to.getLocation().getX();
	double diffZ = from.getLocation().getY() - to.getLocation().getY();
	// Normalize
	double dist = to.getLocation().distance(from.getLocation());
	diffZ /= dist;
	diffX /= dist;
	// Make the smallest one = 1
	if (diffZ < diffX) {
	    diffX = diffX/diffZ;
	    diffZ = 1;
	} else {
	    diffZ = diffZ/diffX;
	    diffX = 1;
	}
	plugin.getLogger().info("direction = " + diffX + "," + diffZ);
	// Just hack 15 blocks for now
	int index = 0;
	double x = from.getLocation().getX(); 
	double z = from.getLocation().getY();
	boolean xComplete = false;
	boolean zComplete = false;
	do {
	    index++;
	    plugin.getLogger().info("Double coords = " + x + "," + z);
	    Location loc = Beaconz.beaconzWorld.getHighestBlockAt((int)x, (int)z).getLocation();
	    plugin.getLogger().info("coords = " + loc.getBlockX() + "," + loc.getBlockY() + ", " + loc.getBlockZ());
	    MaterialData md = plugin.getScorecard().getBlockID(team);
	    //player.sendBlockChange(loc.add(new Vector(0,5,0)), md.getItemType(), md.getData());
	    player.sendBlockChange(loc.add(new Vector(0,5,0)), Material.FIRE, (byte)0);
	    //ParticleEffect.FLAME.display(0, 1, 0, 0, 10, loc.add(new Vector(0,5,0)), 100);;
	    if (!zComplete && Math.abs(z - to.getLocation().getY()) > 2) {
		z+= diffZ;  
	    } else {
		zComplete = true; 
	    }
	    if (!xComplete && Math.abs(x - to.getLocation().getX()) > 2 ) {
		x+= diffX;
	    } else {
		xComplete = true;
	    }
	} while (!zComplete && !xComplete && index < plugin.getServer().getViewDistance());
    }
}
