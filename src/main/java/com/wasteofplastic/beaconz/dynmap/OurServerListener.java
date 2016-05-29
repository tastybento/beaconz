package com.wasteofplastic.beaconz.dynmap;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
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


public class OurServerListener extends BeaconzPluginDependent implements Listener {
    private Beaconz plugin;
    private Plugin dynmap;
    private DynmapAPI api;
    private MarkerAPI markerapi;
    private int updatesPerTick = 20;
    private File dynmapFile;
    private YamlConfiguration cfg;
    private MarkerSet set;
    private long updperiod;
    private boolean use3d;
    private String infowindow;
    private AreaStyle defstyle;
    private Map<String, AreaStyle> cusstyle;
    private Map<String, AreaStyle> cuswildstyle;
    private Map<String, AreaStyle> ownerstyle;
    private boolean stop;
    private static final String DEF_INFOWINDOW = "<div class=\"infowindow\">Team <span style=\"font-weight:bold;\">%ownername%</span><br /></div>";
    private Set<TriangleField> trianglesToDo;

    private Map<String, AreaMarker> resareas = new HashMap<String, AreaMarker>();
    protected LinkedHashMap<String, Game> gamesToDo;


    public OurServerListener(Beaconz plugin, Plugin dynmap) {
        super(plugin);
        this.plugin = plugin;
        this.dynmap = dynmap;
        api = (DynmapAPI)dynmap; /* Get API */
        dynmapFile = new File(getBeaconzPlugin().getDataFolder(),"dynmap.yml");
        if (!dynmapFile.exists()) {
            getBeaconzPlugin().saveResource("dynmap.yml", false);
        }
        cfg = new YamlConfiguration();
        if (dynmapFile.exists()) {
            try {
                cfg.load(dynmapFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        activate(dynmap);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin p = event.getPlugin();
        String name = p.getDescription().getName();
        if(name.equals("dynmap")) {
            if(dynmap.isEnabled())
                activate(dynmap);
        }
    }

    private void activate(Plugin dynmap) {
        /* Now, get markers API */
        markerapi = api.getMarkerAPI();
        if(markerapi == null) {
            getLogger().severe("Error loading dynmap marker API!");
            return;
        }
        set = markerapi.getMarkerSet("dynmap.markerset");
        if(set == null)
            set = markerapi.createMarkerSet("dynamp.markerset", cfg.getString("layer.name", "Beaconz"), null, false);
        else
            set.setMarkerSetLabel(cfg.getString("layer.name", "Beaconz"));
        if(set == null) {
            getLogger().severe("Error creating marker set");
            return;
        }
        int minzoom = cfg.getInt("layer.minzoom", 0);
        if(minzoom > 0)
            set.setMinZoom(minzoom);
        set.setLayerPriority(cfg.getInt("layer.layerprio", 10));
        set.setHideByDefault(cfg.getBoolean("layer.hidebydefault", false));
        use3d = cfg.getBoolean("use3dregions", false);
        infowindow = cfg.getString("infowindow", DEF_INFOWINDOW);
        updatesPerTick = cfg.getInt("updates-per-tick", 20);

        /* Get style information */
        defstyle = new AreaStyle(cfg, "trianglestyle");
        cusstyle = new HashMap<String, AreaStyle>();
        ownerstyle = new HashMap<String, AreaStyle>();
        cuswildstyle = new HashMap<String, AreaStyle>();
        ConfigurationSection sect = cfg.getConfigurationSection("ownerstyle");
        if(sect != null) {
            Set<String> ids = sect.getKeys(false);

            for(String id : ids) {
                ownerstyle.put(id, new AreaStyle(cfg, "ownerstyle." + id, defstyle));
            }
        }
        /* Set up update job - based on period */
        int per = cfg.getInt("update.period", 300);
        if(per < 15) per = 15;
        updperiod = per*20;
        stop = false;

        new BukkitRunnable() {

            @Override
            public void run() {
                //getLogger().info("DEBUG: running outer loop");

                for (Game game : getGameMgr().getGames().values()) {
                    handleGames(game);
                }
                trianglesToDo = new HashSet<TriangleField>(getRegister().getTriangleFields());
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        //getLogger().info("DEBUG: running inner loop");
                        Map<String,AreaMarker> newmap = new HashMap<String,AreaMarker>(); /* Build new map */

                        // Clone triangle fields
                        if (trianglesToDo.isEmpty()) {
                            //getLogger().info("DEBUG: cancelling inner loop");
                            this.cancel();
                            return;
                        }
                        if (stop) {
                            this.cancel();
                            return;
                        }
                        int i = 0;
                        //getLogger().info("DEBUG: There are " + trianglesToDo.size() + " triangles left");
                        Iterator<TriangleField> it = trianglesToDo.iterator();
                        while (it.hasNext() && i < updatesPerTick) {
                            i++;
                            handleTriangle(it.next(), newmap);
                            it.remove();
                        }
                    }

                }.runTaskTimer(plugin, 40L, 1L);

            }

        }.runTaskTimer(plugin, 0L, updperiod);

        getLogger().info("Beaconz dynmap is activated");
    }

    private static class AreaStyle {
        String strokecolor;
        String unownedstrokecolor;
        double strokeopacity;
        int strokeweight;
        String fillcolor;
        double fillopacity;
        String label;

        AreaStyle(FileConfiguration cfg, String path, AreaStyle def) {
            strokecolor = cfg.getString(path+".strokeColor", def.strokecolor);
            unownedstrokecolor = cfg.getString(path+".unownedStrokeColor", def.unownedstrokecolor);
            strokeopacity = cfg.getDouble(path+".strokeOpacity", def.strokeopacity);
            strokeweight = cfg.getInt(path+".strokeWeight", def.strokeweight);
            fillcolor = cfg.getString(path+".fillColor", def.fillcolor);
            fillopacity = cfg.getDouble(path+".fillOpacity", def.fillopacity);
            label = cfg.getString(path+".label", null);
        }

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
     * Show the game area
     * @param game
     */
    private void handleGames(Game game) {
        // TODO Auto-generated method stub
        World world = getBeaconzWorld();
        String name = game.getName();
        double[] x = new double[4];
        double[] z = new double[4];
        Point2D[] corners = game.getRegion().getCorners();
        int xMin = (int) corners[0].getX();
        int xMax = (int) corners[1].getX();
        int zMin = (int) corners[0].getY();
        int zMax = (int) corners[1].getY();
        x[0] = xMin+1; z[0] = zMin+1;
        x[1] = xMax-1; z[1] = zMin+1;
        x[3] = xMin+1; z[3] = zMax-1;  
        x[2] = xMax-1; z[2] = zMax-1;

        String markerid = world.getName() + "_" + name;
        AreaMarker m = resareas.remove(markerid); /* Existing area? */
        if(m == null) {
            m = set.createAreaMarker(markerid, name, false, world.getName(), x, z, false);
            if(m == null)
                return;
        } else {
            m.setCornerLocations(x, z); /* Replace corner locations */
            m.setLabel(name);   /* Update label */
        }
        if(use3d) { /* If 3D? */
            m.setRangeY(world.getMaxHeight(), 0);
        }
        /* Set line and fill properties */
        addStyle(name, world.getName(), m, game.getName());

        /* Build popup */
        String desc = formatInfoWindow(game.getName(), m);

        m.setDescription(desc); /* Set popup */
    }

    /* Handle triangles */
    private void handleTriangle(TriangleField triangle, Map<String, AreaMarker> newmap) {
        World world = getBeaconzWorld();
        if (triangle.getOwner() == null) {
            return;
        }
        String name = triangle.getOwner().toString();
        double[] x = new double[3];
        double[] z = new double[3];
        x[0] = triangle.a.getX(); z[0] = triangle.a.getY();
        x[1] = triangle.b.getX(); z[1] = triangle.b.getY();
        x[2] = triangle.c.getX(); z[2] = triangle.c.getY();

        String markerid = world.getName() + "_" + triangle.toString();
        AreaMarker m = resareas.remove(markerid); /* Existing area? */
        if(m == null) {
            m = set.createAreaMarker(markerid, name, false, world.getName(), x, z, false);
            if(m == null)
                return;
        } else {
            m.setCornerLocations(x, z); /* Replace corner locations */
            m.setLabel(name);   /* Update label */
        }
        if(use3d) { /* If 3D? */
            m.setRangeY(world.getMaxHeight()-1, world.getMaxHeight()-2);
        }
        /* Set line and fill properties */
        addStyle(name, world.getName(), m, triangle.getOwner().getName());

        /* Build popup */
        String desc = formatInfoWindow(triangle.getOwner().getName(), m);

        m.setDescription(desc); /* Set popup */

        /* Add to map */
        newmap.put(markerid, m);
    }

    /**
     * Adds a style
     * @param resid
     * @param worldid
     * @param m
     * @param name
     */
    private void addStyle(String resid, String worldid, AreaMarker m, String name) {
        AreaStyle as = null;
        if(!ownerstyle.isEmpty()) {
            //info("DEBUG: ownerstyle is not empty " + getServer().getOfflinePlayer(island.getOwner()).getName());
            as = ownerstyle.get(name);
            /*
        if (as != null) {
        info("DEBUG: fill color = " + as.fillcolor);
        info("DEBUG: stroke color = " + as.strokecolor);
        }*/
        }
        if(as == null) {
            //info("DEBUG: as = is null - using default style");
            as = defstyle;
        }
        int sc = 0xFF0000;
        int fc = 0xFF0000;
        try {
            sc = Integer.parseInt(as.strokecolor.substring(1), 16);
            fc = Integer.parseInt(as.fillcolor.substring(1), 16);
        } catch (NumberFormatException nfx) {
        }
        /*
    if (sc == 0xFF0000) {
        info("DEBUG: stroke is red");
    } else {
        info("DEBUG: stroke is not red");
    }*/
        m.setLineStyle(as.strokeweight, as.strokeopacity, sc);
        m.setFillStyle(as.fillopacity, fc);
        if(as.label != null) {
            m.setLabel(as.label);
        }
    }

    private String formatInfoWindow(String name, AreaMarker m) {
        String v = "<div class=\"infowindow\">"+infowindow+"</div>";
        v = v.replace("%ownername%", name);
        return v;
    }
}

