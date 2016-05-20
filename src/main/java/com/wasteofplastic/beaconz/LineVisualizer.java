package com.wasteofplastic.beaconz;

import java.awt.geom.Point2D;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Class to visualize a line with blocks for a team. Runs async.
 * @author tastybento
 *
 */
public class LineVisualizer extends BeaconzPluginDependent {
    private final static int BLOCKS_TO_SET = 100;
    private Point2D current;
    private Iterator<Point2D> it;
    private Iterator<Point2D> it2;

    public LineVisualizer(Beaconz beaconzPlugin, final BeaconLink beaconPair, final boolean addLink) {
        super(beaconzPlugin);
        it = new LineIterator(beaconPair.getLine());
        it2 = new LineIterator(beaconPair.getReverseLine());
        // Run a repeating task to set a number of blocks on this line. When the line is done, stop
        new BukkitRunnable() {

            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                int count = 0;
                //getLogger().info("Ownership: " + ownership.getDisplayName());
                Game game = getGameMgr().getGame(beaconPair.getOwner());
                if (game == null) {
                    return;
                }
                // Set air to the team's block
                MaterialData from = new MaterialData(Material.AIR);
                MaterialData to = game.getScorecard().getBlockID(beaconPair.getOwner());
                if (!addLink) {
                    // Removal
                    from = to;
                    to = new MaterialData(Material.AIR);
                }
                while(it.hasNext() && count++ < BLOCKS_TO_SET) {
                    current = it.next();
                    Block b = getBeaconzWorld().getBlockAt((int)current.getX(), getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
                    if (b.getType().equals(from.getItemType()) && b.getData() == from.getData()) {
                        b.setType(to.getItemType());
                        b.setData(to.getData());
                    }
                }
                while(it2.hasNext() && count++ < BLOCKS_TO_SET) {
                    current = it2.next();
                    Block b = getBeaconzWorld().getBlockAt((int)current.getX(), getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
                    if (b.getType().equals(from.getItemType()) && b.getData() == from.getData()) {
                        b.setType(to.getItemType());
                        b.setData(to.getData());
                    }
                }
                if (!it.hasNext() && !it2.hasNext()) {
                    // Cancel task
                    this.cancel();
                }
            }
        }.runTaskTimer(beaconzPlugin, 0, 5L);
    }

}
