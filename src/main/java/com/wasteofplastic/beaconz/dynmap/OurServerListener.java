package com.wasteofplastic.beaconz.dynmap;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;
import com.wasteofplastic.beaconz.Game;
import com.wasteofplastic.beaconz.TriangleField;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Integrates the Beaconz plugin with Dynmap to visualize game areas and team territories.
 * <p>
 * This listener creates and manages area markers on the Dynmap that represent:
 * <ul>
 *   <li>Game boundaries - rectangular areas showing the playable game zones</li>
 *   <li>Triangle fields - triangular territories owned by teams</li>
 * </ul>
 * <p>
 * The class handles:
 * <ul>
 *   <li>Loading configuration from dynmap.yml</li>
 *   <li>Initializing the Dynmap marker API</li>
 *   <li>Creating and updating area markers with team-specific styling</li>
 *   <li>Periodic updates to keep the map synchronized with game state</li>
 *   <li>Dynamic activation when Dynmap is enabled</li>
 * </ul>
 * <p>
 * Updates are processed incrementally to avoid server lag, with configurable updates-per-tick
 * to balance performance and responsiveness.
 *
 * @author tastybento
 */
public class OurServerListener extends BeaconzPluginDependent implements Listener {
    /** Reference to the main Beaconz plugin instance */
    private final Beaconz plugin;

    /** Reference to the Dynmap plugin */
    private final Plugin dynmap;

    /** Dynmap API instance for accessing Dynmap functionality */
    private final DynmapAPI api;

    /** Number of triangle field updates to process per server tick (configurable for performance tuning) */
    private int updatesPerTick = 20;

    /** Configuration loaded from dynmap.yml containing style and display settings */
    private final YamlConfiguration cfg;

    /** The Dynmap marker set that contains all Beaconz area markers */
    private MarkerSet set;

    /** Whether to use 3D rendering for regions (shows height range) */
    private boolean use3d;

    /** HTML template for the info window popup shown when clicking a marker */
    private String infowindow;

    /** Default style configuration for areas without team-specific styling */
    private AreaStyle defstyle;

    /** Map of team names to their custom area styles (colors, opacity, etc.) */
    private Map<String, AreaStyle> teamstyle;

    /** Flag to stop the update tasks when the plugin is disabled */
    private boolean stop;

    /** Default HTML template for info windows if not specified in config */
    private static final String DEF_INFOWINDOW = "<div class=\"infowindow\">Team <span style=\"font-weight:bold;\">%teamname%</span><br /></div>";

    /** Set of triangle fields waiting to be processed in the incremental update loop */
    private Set<TriangleField> trianglesToDo;

    /** Map of marker IDs to AreaMarker objects for tracking existing markers */
    private final Map<String, AreaMarker> resareas = new HashMap<>();

    /** Map of games that need to be processed (currently unused but available for future use) */
    protected LinkedHashMap<String, Game> gamesToDo;

    /**
     * Constructs a new OurServerListener and initializes the Dynmap integration.
     * <p>
     * This constructor:
     * <ul>
     *   <li>Stores references to the Beaconz and Dynmap plugins</li>
     *   <li>Gets the Dynmap API instance</li>
     *   <li>Loads or creates the dynmap.yml configuration file</li>
     *   <li>Activates the Dynmap integration</li>
     * </ul>
     *
     * @param plugin the Beaconz plugin instance
     * @param dynmap the Dynmap plugin instance
     */
    public OurServerListener(Beaconz plugin, Plugin dynmap) {
        super(plugin);
        this.plugin = plugin;
        this.dynmap = dynmap;

        // Cast Dynmap plugin to API to access marker functionality
        api = (DynmapAPI)dynmap; /* Get API */

        // Check if dynmap.yml exists, create it from template if not
        File dynmapFile = new File(getBeaconzPlugin().getDataFolder(), "dynmap.yml");
        if (!dynmapFile.exists()) {
            getBeaconzPlugin().saveResource("dynmap.yml", false);
        }

        // Load configuration from dynmap.yml
        cfg = new YamlConfiguration();
        if (dynmapFile.exists()) {
            try {
                cfg.load(dynmapFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Initialize the Dynmap integration
        activate(dynmap);
    }

    /**
     * Event handler that activates Dynmap integration when Dynmap is enabled.
     * <p>
     * This allows the plugin to work correctly if Dynmap is loaded after Beaconz,
     * or if Dynmap is reloaded while the server is running.
     *
     * @param event the plugin enable event
     */
    @EventHandler(priority=EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin p = event.getPlugin();
        String name = p.getDescription().getName();

        // Check if the enabled plugin is Dynmap
        if(name.equals("dynmap")) {
            if(dynmap.isEnabled())
                activate(dynmap);
        }
    }

    /**
     * Activates the Dynmap integration by initializing marker sets, loading styles, and starting update tasks.
     * <p>
     * This method:
     * <ul>
     *   <li>Retrieves the Dynmap MarkerAPI</li>
     *   <li>Creates or retrieves the Beaconz marker set</li>
     *   <li>Configures layer properties (min zoom, priority, visibility)</li>
     *   <li>Loads style configurations for teams and default areas</li>
     *   <li>Starts periodic update tasks to refresh markers</li>
     * </ul>
     * <p>
     * The update process runs in two phases:
     * <ol>
     *   <li>A periodic task (configurable interval) that clears old markers and processes game boundaries</li>
     *   <li>An incremental task that processes triangle fields in batches to avoid lag</li>
     * </ol>
     *
     * @param dynmap the Dynmap plugin instance to integrate with
     */
    private void activate(Plugin dynmap) {
        /* Now, get markers API */
        MarkerAPI markerapi = api.getMarkerAPI();
        if(markerapi == null) {
            getLogger().severe("Error loading dynmap marker API!");
            return;
        }

        // Get or create the marker set for Beaconz
        set = markerapi.getMarkerSet("dynmap.markerset");
        if(set == null)
            set = markerapi.createMarkerSet("dynamp.markerset", cfg.getString("layer.name", "Beaconz"), null, false);
        else
            set.setMarkerSetLabel(cfg.getString("layer.name", "Beaconz"));
        if(set == null) {
            getLogger().severe("Error creating marker set");
            return;
        }

        // Configure marker layer properties from config
        int minzoom = cfg.getInt("layer.minzoom", 0);
        if(minzoom > 0)
            set.setMinZoom(minzoom);
        set.setLayerPriority(cfg.getInt("layer.layerprio", 10));
        set.setHideByDefault(cfg.getBoolean("layer.hidebydefault", false));

        // Load display settings
        use3d = cfg.getBoolean("use3dregions", false);
        infowindow = cfg.getString("infowindow", DEF_INFOWINDOW);
        updatesPerTick = cfg.getInt("updates-per-tick", 20);

        /* Get style information */
        // Load default style for areas
        defstyle = new AreaStyle(cfg, "trianglestyle");

        // Load team-specific styles
        teamstyle = new HashMap<>();
        ConfigurationSection sect = cfg.getConfigurationSection("teamstyle");
        if(sect != null) {
            Set<String> ids = sect.getKeys(false);

            for(String id : ids) {
                teamstyle.put(id, new AreaStyle(cfg, "teamstyle." + id, defstyle));
            }
        }

        /* Set up update job - based on period */
        // Get update period from config (minimum 15 seconds)
        int per = cfg.getInt("update.period", 300);
        if(per < 15) per = 15;
        long updperiod = per * 20L; // Convert seconds to ticks
        stop = false;

        // Main periodic update task - clears and rebuilds all markers
        new BukkitRunnable() {

            @Override
            public void run() {
                // Clear existing markers from the map
                for (AreaMarker am : resareas.values()) {
                    am.deleteMarker();
                }

                // Process all game boundaries (rectangular areas)
                for (Game game : getGameMgr().getGames().values()) {
                    handleGames(game);
                }

                // Prepare triangle fields for incremental processing
                trianglesToDo = new HashSet<>(getRegister().getTriangleFields());

                // Incremental update task - processes triangles in batches to avoid lag
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Stop if no more triangles to process
                        if (trianglesToDo.isEmpty()) {
                            this.cancel();
                            return;
                        }

                        // Stop if plugin is being disabled
                        if (stop) {
                            this.cancel();
                            return;
                        }

                        // Process up to updatesPerTick triangles this tick
                        int i = 0;
                        Iterator<TriangleField> it = trianglesToDo.iterator();
                        while (it.hasNext() && i < updatesPerTick) {
                            i++;
                            handleTriangle(it.next());
                            it.remove();
                        }
                    }

                }.runTaskTimer(plugin, 40L, 1L); // Start after 2 seconds, run every tick

            }

        }.runTaskTimer(plugin, 0L, updperiod); // Start immediately, repeat at configured interval

        getLogger().info("Beaconz dynmap is activated");
    }

    /**
     * Inner class that encapsulates styling information for area markers.
     * <p>
     * This class stores color, opacity, and line weight settings that determine
     * how team territories and game areas appear on the Dynmap.
     */
    private static class AreaStyle {
        /** Hex color code for the border line (e.g., "#FF0000" for red) */
        final String strokecolor;

        /** Hex color code for the border line when area is unowned */
        final String unownedstrokecolor;

        /** Opacity of the border line (0.0 to 1.0) */
        final double strokeopacity;

        /** Thickness of the border line in pixels */
        final int strokeweight;

        /** Hex color code for the fill color inside the area */
        final String fillcolor;

        /** Opacity of the fill color (0.0 to 1.0) */
        final double fillopacity;

        /** Optional custom label for the area */
        String label;

        /**
         * Constructs an AreaStyle from configuration with fallback to default values.
         * <p>
         * This constructor is used for team-specific styles that can inherit
         * from the default style if a value is not specified.
         *
         * @param cfg the configuration file to read from
         * @param path the configuration path prefix (e.g., "teamstyle.red")
         * @param def the default AreaStyle to fall back to for unspecified values
         */
        AreaStyle(FileConfiguration cfg, String path, AreaStyle def) {
            strokecolor = cfg.getString(path+".strokeColor", def.strokecolor);
            unownedstrokecolor = cfg.getString(path+".unownedStrokeColor", def.unownedstrokecolor);
            strokeopacity = cfg.getDouble(path+".strokeOpacity", def.strokeopacity);
            strokeweight = cfg.getInt(path+".strokeWeight", def.strokeweight);
            fillcolor = cfg.getString(path+".fillColor", def.fillcolor);
            fillopacity = cfg.getDouble(path+".fillOpacity", def.fillopacity);
            label = cfg.getString(path+".label", null);
        }

        /**
         * Constructs an AreaStyle from configuration with hardcoded default values.
         * <p>
         * This constructor is used for the base default style. If values are not
         * found in the configuration, reasonable defaults are used (red color scheme).
         *
         * @param cfg the configuration file to read from
         * @param path the configuration path prefix (e.g., "trianglestyle")
         */
        AreaStyle(FileConfiguration cfg, String path) {
            strokecolor = cfg.getString(path+".strokeColor", "#FF0000");
            unownedstrokecolor = cfg.getString(path+".unownedStrokeColor", "#00FF00");
            strokeopacity = cfg.getDouble(path+".strokeOpacity", 0.8);
            strokeweight = cfg.getInt(path+".strokeWeight", 3);
            fillcolor = cfg.getString(path+".fillColor", "#FF0000");
            fillopacity = cfg.getDouble(path+".fillOpacity", 0.35);
        }
    }

    /**
     * Creates or updates an area marker for a game's boundary on the Dynmap.
     * <p>
     * This method:
     * <ul>
     *   <li>Retrieves the game's rectangular boundary coordinates</li>
     *   <li>Creates a new area marker or updates an existing one</li>
     *   <li>Applies styling and creates an info popup</li>
     * </ul>
     * <p>
     * Game boundaries are rectangular areas that define the playable game zone.
     *
     * @param game the Game object containing boundary information
     */
    private void handleGames(Game game) {
        World world = getBeaconzWorld();
        String name = PlainTextComponentSerializer.plainText().serialize(game.getName());

        // Arrays to hold the 4 corner coordinates of the rectangular game boundary
        double[] x = new double[4];
        double[] z = new double[4];

        // Get the corner points of the game region
        Point2D[] corners = game.getRegion().corners();
        int xMin = (int) corners[0].getX();
        int xMax = (int) corners[1].getX();
        int zMin = (int) corners[0].getY();
        int zMax = (int) corners[1].getY();

        // Set up the 4 corners (slightly inset from edges to avoid overlapping)
        x[0] = xMin+1; z[0] = zMin+1;
        x[1] = xMax-1; z[1] = zMin+1;
        x[3] = xMin+1; z[3] = zMax-1;  
        x[2] = xMax-1; z[2] = zMax-1;

        // Generate unique marker ID from world name and game name
        String markerid = world.getName() + "_" + name;
        AreaMarker m = resareas.remove(markerid); /* Existing area? */
        if(m == null) {
            // Create new marker if it doesn't exist
            m = set.createAreaMarker(markerid, name, false, world.getName(), x, z, false);
            if(m == null)
                return;
        } else {
            // Update existing marker with new coordinates and label
            m.setCornerLocations(x, z); /* Replace corner locations */
            m.setLabel(name);   /* Update label */
        }

        // Set vertical range if 3D mode is enabled
        if(use3d) { /* If 3D? */
            m.setRangeY(world.getMaxHeight(), 0);
        }

        /* Set line and fill properties */
        addStyle(name, world.getName(), m, name);

        /* Build popup */
        String desc = formatInfoWindow(name, m);

        m.setDescription(desc); /* Set popup */
    }
    
    /**
     * Creates or updates an area marker for a triangle field on the Dynmap.
     * <p>
     * This method:
     * <ul>
     *   <li>Retrieves the triangle's three vertex coordinates</li>
     *   <li>Creates a new area marker or updates an existing one</li>
     *   <li>Applies team-specific styling based on the owner</li>
     *   <li>Creates an info popup showing team ownership</li>
     * </ul>
     * <p>
     * Only owned triangles are displayed; unowned triangles are skipped.
     * Triangles are rendered at near-max world height if 3D mode is enabled.
     *
     * @param triangle the TriangleField to display on the map
     */
    /* Handle triangles */
    private void handleTriangle(TriangleField triangle) {
        // Skip triangles that don't have an owner
        World world = getBeaconzWorld();
        if (triangle.getOwner() == null) {
            return;
        }

        // Get the team name of the triangle owner
        String name = triangle.getOwner().toString();

        // Arrays to hold the 3 vertex coordinates of the triangle
        double[] x = new double[3];
        double[] z = new double[3];
        x[0] = triangle.a.getX(); z[0] = triangle.a.getY();
        x[1] = triangle.b.getX(); z[1] = triangle.b.getY();
        x[2] = triangle.c.getX(); z[2] = triangle.c.getY();

        // Generate unique marker ID from world name and triangle object
        String markerid = world.getName() + "_" + triangle;
        AreaMarker m = resareas.remove(markerid); /* Existing area? */
        if(m == null) {
            // Create new triangular marker if it doesn't exist
            m = set.createAreaMarker(markerid, name, false, world.getName(), x, z, false);
            if(m == null)
                return;
        } else {
            // Update existing marker with new coordinates and label
            m.setCornerLocations(x, z); /* Replace corner locations */
            m.setLabel(name);   /* Update label */
        }

        // Set vertical range if 3D mode is enabled (near max height for visibility)
        if(use3d) { /* If 3D? */
            m.setRangeY(world.getMaxHeight()-1, world.getMaxHeight()-2);
        }

        /* Set line and fill properties */
        addStyle(name, world.getName(), m, triangle.getOwner().getName());

        /* Build popup */
        String desc = formatInfoWindow(triangle.getOwner().getName(), m);

        m.setDescription(desc); /* Set popup */

        /* Add to map */
        resareas.put(markerid, m);
    }

    /**
     * Applies visual styling to an area marker based on team configuration.
     * <p>
     * This method:
     * <ul>
     *   <li>Looks up team-specific styling from the configuration</li>
     *   <li>Falls back to default styling if no team-specific style is found</li>
     *   <li>Parses hex color codes for stroke and fill colors</li>
     *   <li>Applies line weight, opacity, and colors to the marker</li>
     *   <li>Optionally applies a custom label if configured</li>
     * </ul>
     *
     * @param resid the resource ID (unused but kept for potential future use)
     * @param worldid the world ID (unused but kept for potential future use)
     * @param m the AreaMarker to apply styling to
     * @param name the team name to look up styling for
     */
    private void addStyle(String resid, String worldid, AreaMarker m, String name) {
        AreaStyle as = null;

        // Try to find team-specific styling
        if(!teamstyle.isEmpty()) {
            //info("DEBUG: ownerstyle is not empty " + getServer().getOfflinePlayer(island.getOwner()).getName());
            as = teamstyle.get(name);
            /*
        if (as != null) {
        info("DEBUG: fill color = " + as.fillcolor);
        info("DEBUG: stroke color = " + as.strokecolor);
        }*/
        }

        // Fall back to default style if no team-specific style found
        if(as == null) {
            //info("DEBUG: as = is null - using default style");
            as = defstyle;
        }

        // Parse hex color strings to integer values (default to red if parsing fails)
        int sc = 0xFF0000; // Stroke color
        int fc = 0xFF0000; // Fill color
        try {
            sc = Integer.parseInt(as.strokecolor.substring(1), 16); // Skip '#' and parse hex
            fc = Integer.parseInt(as.fillcolor.substring(1), 16);
        } catch (NumberFormatException nfx) {
            // If parsing fails, colors remain at default red (0xFF0000)
        }

        /*
    if (sc == 0xFF0000) {
        info("DEBUG: stroke is red");
    } else {
        info("DEBUG: stroke is not red");
    }*/

        // Apply the stroke (border) styling: weight, opacity, and color
        m.setLineStyle(as.strokeweight, as.strokeopacity, sc);

        // Apply the fill (interior) styling: opacity and color
        m.setFillStyle(as.fillopacity, fc);

        // Apply custom label if one is defined in the style
        if(as.label != null) {
            m.setLabel(as.label);
        }
    }

    /**
     * Formats the HTML content for a marker's info window popup.
     * <p>
     * This method takes the configured info window template and replaces
     * placeholder variables with actual values. Currently supports:
     * <ul>
     *   <li>%teamname% - replaced with the team name</li>
     * </ul>
     *
     * @param name the team name to display
     * @param m the AreaMarker (currently unused but available for future template variables)
     * @return the formatted HTML string for the popup window
     */
    private String formatInfoWindow(String name, AreaMarker m) {
        String v = "<div class=\"infowindow\">"+infowindow+"</div>";
        v = v.replace("%teamname%", name);
        return v;
    }
}

