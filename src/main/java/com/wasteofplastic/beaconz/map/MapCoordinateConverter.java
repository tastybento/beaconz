package com.wasteofplastic.beaconz.map;

import org.bukkit.map.MapView;

public class MapCoordinateConverter {
    private final int multiplier;
    private final int centerX;
    private final int centerZ;

    @SuppressWarnings("deprecation")
    public MapCoordinateConverter(MapView map) {
        MapView.Scale scale = map.getScale();
        int multiplier = 1;
        for (int i = 0; i < scale.getValue(); i++) {
            multiplier *= 2;
        }
        this.multiplier = multiplier;
        this.centerX = map.getCenterX();
        this.centerZ = map.getCenterZ();
    }

    public int pixelXToBlockX(byte pixelX) {
        return pixelToBlock(centerX, pixelX);
    }

    public int pixelZToBlockZ(byte pixelZ) {
        return pixelToBlock(centerZ, pixelZ);
    }

    public int blockXToPixelX(int blockX) {
        return blockToPixel(centerX, blockX);
    }

    public int blockZToPixelZ(int blockZ) {
        return blockToPixel(centerZ, blockZ);
    }

    private int pixelToBlock(int center, byte pixel) {
        return (pixel - 64) * multiplier + center;
    }

    private int blockToPixel(int center, int block) {
        return (block - center) / multiplier + 64;
    }
}
