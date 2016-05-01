package com.wasteofplastic.beaconz;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

/**
 * Class to visualize a line with blocks for a team. Runs async.
 * @author tastybento
 *
 */
public class LineVisualizer extends BeaconzPluginDependent {
    private final static int BLOCKS_TO_SET = 100;
    private Point2D current;
    private Iterator<Point2D> it;

    public LineVisualizer(Beaconz beaconzPlugin, Line2D line, final Team ownership) {
        super(beaconzPlugin);
        it = new LineIterator(line);
        // Run a repeating task to set a number of blocks on this line. When the line is done, stop
        new BukkitRunnable() {

            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                int count = 0;
                //getLogger().info("Ownership: " + ownership.getDisplayName());
                Game game = getGameMgr().getGame(ownership);
                if (game == null) {
                    return;
                }
                MaterialData md = game.getScorecard().getBlockID(ownership);
                while(it.hasNext() && count++ < BLOCKS_TO_SET) {
                    current = it.next();
                    Block b = getBeaconzWorld().getBlockAt((int)current.getX(), getBeaconzWorld().getMaxHeight()-1, (int)current.getY());
                    b.setTypeIdAndData(md.getItemTypeId(), md.getData(), false);
                }
                if (!it.hasNext()) {
                    // Cancel task
                    this.cancel();
                }
            }
        }.runTaskTimer(beaconzPlugin, 0, 5L);
    }

}
