package com.wasteofplastic.beaconz.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.block.Block;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link DefenseBlock}.
 *
 * @author tastybento
 */
public class DefenseBlockTest {

    private Block block1;
    private Block block2;
    private UUID playerUuid;

    @BeforeEach
    public void setUp() {
        // Create mock blocks
        block1 = mock(Block.class);
        block2 = mock(Block.class);

        playerUuid = UUID.randomUUID();
    }

    /**
     * Test method for {@link DefenseBlock#DefenseBlock(Block, int, UUID)}.
     */
    @Test
    public void testConstructorWithUUID() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);

        assertNotNull(defense);
        assertThat(defense.getBlock(), is(block1));
        assertThat(defense.getLevel(), is(5));
        assertThat(defense.getPlacer(), is(playerUuid));
    }

    /**
     * Test method for {@link DefenseBlock#DefenseBlock(Block, int, String)}.
     */
    @Test
    public void testConstructorWithValidUUIDString() {
        String uuidString = playerUuid.toString();
        DefenseBlock defense = new DefenseBlock(block1, 10, uuidString);

        assertNotNull(defense);
        assertThat(defense.getBlock(), is(block1));
        assertThat(defense.getLevel(), is(10));
        assertThat(defense.getPlacer(), is(playerUuid));
    }

    /**
     * Test method for {@link DefenseBlock#DefenseBlock(Block, int, String)} with invalid UUID.
     */
    @Test
    public void testConstructorWithInvalidUUIDString() {
        DefenseBlock defense = new DefenseBlock(block1, 3, "not-a-valid-uuid");

        assertNotNull(defense);
        assertThat(defense.getBlock(), is(block1));
        assertThat(defense.getLevel(), is(3));
        assertThat(defense.getPlacer(), is(nullValue()));
    }

    /**
     * Test method for {@link DefenseBlock#getBlock()}.
     */
    @Test
    public void testGetBlock() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);
        assertThat(defense.getBlock(), is(block1));
    }

    /**
     * Test method for {@link DefenseBlock#setBlock(Block)}.
     */
    @Test
    public void testSetBlock() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);
        defense.setBlock(block2);
        assertThat(defense.getBlock(), is(block2));
    }

    /**
     * Test method for {@link DefenseBlock#getLevel()}.
     */
    @Test
    public void testGetLevel() {
        DefenseBlock defense = new DefenseBlock(block1, 7, playerUuid);
        assertThat(defense.getLevel(), is(7));
    }

    /**
     * Test method for {@link DefenseBlock#setLevel(int)}.
     */
    @Test
    public void testSetLevel() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);
        defense.setLevel(15);
        assertThat(defense.getLevel(), is(15));
    }

    /**
     * Test method for {@link DefenseBlock#getPlacer()}.
     */
    @Test
    public void testGetPlacer() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);
        assertThat(defense.getPlacer(), is(playerUuid));
    }

    /**
     * Test method for {@link DefenseBlock#setPlacer(UUID)}.
     */
    @Test
    public void testSetPlacer() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);
        UUID newUuid = UUID.randomUUID();
        defense.setPlacer(newUuid);
        assertThat(defense.getPlacer(), is(newUuid));
    }

    /**
     * Test method for {@link DefenseBlock#equals(Object)} with matching block.
     */
    @Test
    public void testEqualsWithSameBlock() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);

        // Should equal the block it contains
        assertTrue(defense.equals(block1));
    }

    /**
     * Test method for {@link DefenseBlock#equals(Object)} with different block.
     */
    @Test
    public void testEqualsWithDifferentBlock() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);

        // Should not equal a different block
        assertFalse(defense.equals(block2));
    }

    /**
     * Test method for {@link DefenseBlock#equals(Object)} with null.
     */
    @Test
    public void testEqualsWithNull() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);
        assertFalse(defense.equals(null));
    }

    /**
     * Test method for {@link DefenseBlock#equals(Object)} with non-Block object.
     */
    @Test
    public void testEqualsWithNonBlock() {
        DefenseBlock defense = new DefenseBlock(block1, 5, playerUuid);
        assertFalse(defense.equals("not a block"));
        assertFalse(defense.equals(42));
    }

    /**
     * Test that different DefenseBlocks with same block are equal.
     */
    @Test
    public void testEqualsSameBlockDifferentLevels() {
        DefenseBlock defense1 = new DefenseBlock(block1, 5, playerUuid);
        DefenseBlock defense2 = new DefenseBlock(block1, 10, UUID.randomUUID());

        // Both should equal block1
        assertTrue(defense1.equals(block1));
        assertTrue(defense2.equals(block1));
    }

    /**
     * Test level boundaries.
     */
    @Test
    public void testLevelBoundaries() {
        DefenseBlock defense = new DefenseBlock(block1, 0, playerUuid);
        assertThat(defense.getLevel(), is(0));

        defense.setLevel(100);
        assertThat(defense.getLevel(), is(100));

        defense.setLevel(-1);
        assertThat(defense.getLevel(), is(-1));
    }

    /**
     * Test null UUID handling.
     */
    @Test
    public void testNullUUID() {
        DefenseBlock defense = new DefenseBlock(block1, 5, (UUID) null);
        assertThat(defense.getPlacer(), is(nullValue()));

        defense.setPlacer(playerUuid);
        assertThat(defense.getPlacer(), is(playerUuid));

        defense.setPlacer(null);
        assertThat(defense.getPlacer(), is(nullValue()));
    }
}
