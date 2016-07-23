package com.wasteofplastic.beaconz;

import java.util.UUID;

import org.bukkit.block.Block;

public class DefenseBlock implements Comparable {
    private UUID placer;
    private int level;
    private Block block;
    
    public DefenseBlock(Block block, int level, UUID uuid) {
        this.block = block;
        this.level = level;
        this.placer = uuid;
    }
    
    public DefenseBlock(Block block, int level, String uuid) {
        this.block = block;
        this.level = level;
        try {
            this.placer = UUID.fromString(uuid);
        } catch (Exception e) {
            this.placer = null;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Block)) {
            return false;
        }
        Block block = (Block)obj;
        return block.equals(this.block);
    }
    
    /**
     * @return the placer
     */
    public UUID getPlacer() {
        return placer;
    }
    /**
     * @param placer the placer to set
     */
    public void setPlacer(UUID placer) {
        this.placer = placer;
    }
    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }
    /**
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
    }
    /**
     * @return the block
     */
    public Block getBlock() {
        return block;
    }
    /**
     * @param block the block to set
     */
    public void setBlock(Block block) {
        this.block = block;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
