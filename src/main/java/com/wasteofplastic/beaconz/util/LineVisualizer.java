package com.wasteofplastic.beaconz.util;

import java.awt.geom.Point2D;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.core.BeaconLink;
import com.wasteofplastic.beaconz.game.Game;

/**
 * Class to visualize a line with blocks for a team. Runs async.
 * @author tastybento
 *
 */
public class LineVisualizer extends BeaconzPluginDependent {
    private final static int BLOCKS_TO_SET = 100;
    private Point2D current;
    private final Iterator<Point2D> it;
    private final Iterator<Point2D> it2;

    public LineVisualizer(Beaconz beaconzPlugin, final BeaconLink beaconPair, final boolean addLink) {
        super(beaconzPlugin);
        it = new LineIterator(beaconPair.getLine());
        it2 = new LineIterator(beaconPair.getReverseLine());
        // Run a repeating task to set a number of blocks on this line. When the line is done, stop
        new BukkitRunnable() {

            @Override
            public void run() {
                int count = 0;
                Game game = getGameMgr().getGame(beaconPair.getOwner());
                if (game == null) {
                    return;
                }
                // Set air to the team's block
                final Material from = Material.AIR;
                Material to = game.getScorecard().getBlockID(beaconPair.getOwner());
                if (!addLink) {
                    // Removal - need to reassign, so we can't make 'to' final here
                    to = from;
                }
                final Material toFinal = addLink ? to : Material.AIR;
                final Material fromFinal = addLink ? Material.AIR : game.getScorecard().getBlockID(beaconPair.getOwner());

                // Process first iterator
                while(it.hasNext() && count++ < BLOCKS_TO_SET) {
                    current = it.next();
                    final int x = (int)current.getX();
                    final int z = (int)current.getY();
                    final int y = getBeaconzWorld().getMaxHeight() - 1;
                    Location loc = new Location(getBeaconzWorld(), x, y, z);

                    // Load chunk asynchronously and set block when loaded
                    getBeaconzWorld().getChunkAtAsync(loc).thenAccept(chunk -> {
                        Block b = getBeaconzWorld().getBlockAt(x, y, z);
                        if (b.getType().equals(fromFinal)) {
                            b.setType(toFinal);
                        }
                    });
                }

                // Process second iterator
                while(it2.hasNext() && count++ < BLOCKS_TO_SET) {
                    current = it2.next();
                    final int x = (int)current.getX();
                    final int z = (int)current.getY();
                    final int y = getBeaconzWorld().getMaxHeight() - 1;
                    Location loc = new Location(getBeaconzWorld(), x, y, z);

                    // Load chunk asynchronously and set block when loaded
                    getBeaconzWorld().getChunkAtAsync(loc).thenAccept(chunk -> {
                        Block b = getBeaconzWorld().getBlockAt(x, y, z);
                        if (b.getType().equals(fromFinal)) {
                            b.setType(toFinal);
                        }
                    });
                }

                if (!it.hasNext() && !it2.hasNext()) {
                    // Cancel task
                    this.cancel();
                }
            }
        }.runTaskTimer(beaconzPlugin, 0, 5L);
    }

}
