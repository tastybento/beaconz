package com.wasteofplastic.beaconz;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.material.MaterialData;
import org.bukkit.scoreboard.Team;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Overlays all beacon triangles onto a map. The triangles blink on and off so that the terrain underneath stays
 * visible. Overlapping triangles make progressively darker colors.
 */
public class TerritoryMapRenderer extends MapRenderer {

    // For converting between map pixels and block coordinates.
    private static final EnumMap<MapView.Scale, Integer> scaleMultipliers = new EnumMap<MapView.Scale, Integer>(MapView.Scale.class);

    // cache for getMapPaletteColorForTeam
    private static Byte[][] mapPaletteColors = new Byte[16][];

    private static final int BLINK_LENGTH = 40; // one second on, one second off (20 ticks each)

    private final Beaconz beaconz;
    private int tick = 0;

    private Map<Point2D, CachedBeacon> beaconRegisterCache; // to detect if beacons have changed since last render (re-computing triangle fields is not cheap)
    private Byte[][] pixelCache = new Byte[128][]; // last render

    public TerritoryMapRenderer(Beaconz beaconz) {
        this.beaconz = beaconz;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        tick++;
        if (tick > BLINK_LENGTH) tick = 0;
        if (tick == 0) { // blink on
            Map<Point2D, CachedBeacon> newCache = makeBeaconCache();
            if (beaconRegisterCache != null && beaconRegisterCache.equals(newCache)) {
                // Beacons haven't changed since we last rendered, so we'll just use the same pixels as last time.
                renderPixelCache(canvas);
            } else {
                beaconRegisterCache = newCache;
                pixelCache = new Byte[128][];
                int count = 0;
                for (int x = 0; x < 128; x++) {
                    for (int z = 0; z < 128; z++) {
                        count ++;
                        if (count % 3 == 0) {
                            int xBlock = pixelCoordToBlockCoord(map, map.getCenterX(), (byte)x);
                            int zBlock = pixelCoordToBlockCoord(map, map.getCenterZ(), (byte)z);
                            List<TriangleField> triangles = beaconz.getRegister().getTriangle(xBlock, zBlock);
                            if (triangles != null && !triangles.isEmpty()) {
                                TriangleField triangleField = triangles.get(0);
                                MaterialData materialData = beaconz.getScorecard().getBlockID(triangleField.getOwner());
                                byte color = getMapPaletteColorForTeam(materialData.getData(), triangles.size());                                
                                canvas.setPixel(x, z, color);
                                if (pixelCache[x] == null) pixelCache[x] = new Byte[128];
                                pixelCache[x][z] = color;
                            }
                        }
                    }
                }
            }
        } else if (tick == BLINK_LENGTH / 2) { // blink off
            for (int x = 0; x < 128; x++) {
                for (int z = 0; z < 128; z++) {
                    canvas.setPixel(x, z, canvas.getBasePixel(x, z));
                }
            }
        }
    }

    private Map<Point2D, CachedBeacon> makeBeaconCache() {
        HashMap<Point2D, BeaconObj> current = beaconz.getRegister().getBeaconRegister();
        Map<Point2D, CachedBeacon> result = new HashMap<>(current.size());
        for (Map.Entry<Point2D, BeaconObj> entry : current.entrySet()) {
            result.put(entry.getKey(), new CachedBeacon(entry.getValue()));
        }
        return result;
    }

    private void renderPixelCache(MapCanvas canvas) {
        for (int x = 0; x < 128; x++) {
            if (pixelCache[x] != null) {
                for (int z = 0; z < 128; z++) {
                    if (pixelCache[x][z] != null) {
                        canvas.setPixel(x, z, pixelCache[x][z]);
                    }
                }
            }
        }
    }

    static {
        for (MapView.Scale scale : MapView.Scale.values()) {
            scaleMultipliers.put(scale, getMultiplier(scale));
        }
    }

    private static int getMultiplier(MapView.Scale scale) {
        int multiplier = 1;
        for (int i = 0; i < scale.getValue(); i++) {
            multiplier *= 2;
        }
        return multiplier;
    }

    // There's probably a bukkit api that does this already, but I couldn't find it.
    private int pixelCoordToBlockCoord(MapView mapView, int mapCenter, byte pixelToConvert) {
        int multiplier = scaleMultipliers.get(mapView.getScale());
        return (pixelToConvert - 64) * multiplier + mapCenter;
    }


    /**
     * Returns the color of the map pixel to use for a world location owned by the team with the given glassColor.
     *
     * @param glassColor a number between 0 and 15, see config.yml for complete list
     * @param numberOfTriangles how many triangles are overlapping at the location of the pixel. More triangles make
     *                          darker colors. e.g. for the red team, one triangle is bright red, two triangles is
     *                          a slightly darker red, etc. all the way to black.
     * @return color of pixel (as an index of MapPalette.colors)
     */
    private static byte getMapPaletteColorForTeam(byte glassColor, int numberOfTriangles) {
        if (mapPaletteColors[glassColor] == null) {
            DyeColor dyeColor = DyeColor.getByData(glassColor);
            Color color = dyeColor.getColor();
            List<Byte> colors = new ArrayList<>();
            byte previous = MapPalette.matchColor(0, 0, 0);
            for (double m = 1.0; m >= 0.0; m -= (1.0/256)) {
                byte b = MapPalette.matchColor((int) (m * color.getRed()), (int) (m * color.getGreen()), (int) (m * color.getBlue()));
                if (b != previous && !MapPalette.getColor(b).equals(MapPalette.getColor(previous))) {
                    colors.add(b);
                    previous = b;
                }
            }
            mapPaletteColors[glassColor] = colors.toArray(new Byte[colors.size()]);
        }
        Byte[] colors = mapPaletteColors[glassColor];
        if (numberOfTriangles >= colors.length) numberOfTriangles = colors.length - 1;
        return colors[numberOfTriangles];
    }

    private static class CachedBeacon {
        private Team owner;
        private Set<BeaconObj> links;

        public CachedBeacon(BeaconObj beaconObj) {
            this.owner = beaconObj.getOwnership();
            this.links = new HashSet<>();
            this.links.addAll(beaconObj.getLinks());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CachedBeacon that = (CachedBeacon) o;

            return links.equals(that.links) && !(owner != null ? !owner.equals(that.owner) : that.owner != null);

        }

        @Override
        public int hashCode() {
            int result = owner != null ? owner.hashCode() : 0;
            result = 31 * result + links.hashCode();
            return result;
        }
    }

}
