package com.wasteofplastic.beaconz.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import net.kyori.adventure.text.Component;

/**
 * Comprehensive test suite for ItemRewardParser.
 * Tests modern item parsing including simple items, potions, and items with display names.
 */
class ItemRewardParserTest {

    private ServerMock server;
    private ItemRewardParser parser;
    private Logger logger;

    @BeforeEach
    void setUp() {
        MockBukkit.unmock();
        server = MockBukkit.mock();
        logger = Logger.getLogger("ItemRewardParserTest");
        parser = new ItemRewardParser(logger);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Simple Item Parsing Tests")
    class SimpleItemTests {

        @Test
        @DisplayName("Should parse simple item with quantity")
        void testSimpleItem() {
            ItemStack item = parser.parseReward("DIAMOND:5");

            assertNotNull(item);
            assertEquals(Material.DIAMOND, item.getType());
            assertEquals(5, item.getAmount());
        }

        @Test
        @DisplayName("Should parse item with quantity of 1")
        void testSingleItem() {
            ItemStack item = parser.parseReward("EMERALD:1");

            assertNotNull(item);
            assertEquals(Material.EMERALD, item.getType());
            assertEquals(1, item.getAmount());
        }

        @Test
        @DisplayName("Should parse item with large quantity")
        void testLargeQuantity() {
            ItemStack item = parser.parseReward("GOLD_INGOT:64");

            assertNotNull(item);
            assertEquals(Material.GOLD_INGOT, item.getType());
            assertEquals(64, item.getAmount());
        }

        @Test
        @DisplayName("Should handle lowercase material names")
        void testLowercaseMaterial() {
            ItemStack item = parser.parseReward("diamond:3");

            assertNotNull(item);
            assertEquals(Material.DIAMOND, item.getType());
            assertEquals(3, item.getAmount());
        }

        @Test
        @DisplayName("Should handle mixed case material names")
        void testMixedCaseMaterial() {
            ItemStack item = parser.parseReward("DiAmOnD:2");

            assertNotNull(item);
            assertEquals(Material.DIAMOND, item.getType());
            assertEquals(2, item.getAmount());
        }

        @Test
        @DisplayName("Should handle extra whitespace")
        void testExtraWhitespace() {
            ItemStack item = parser.parseReward("  DIAMOND  :  5  ");

            assertNotNull(item);
            assertEquals(Material.DIAMOND, item.getType());
            assertEquals(5, item.getAmount());
        }
    }

    @Nested
    @DisplayName("Potion Parsing Tests")
    class PotionTests {

        @Test
        @DisplayName("Should parse regular potion")
        void testRegularPotion() {
            ItemStack item = parser.parseReward("POTION:HEALING:2");

            assertNotNull(item);
            assertEquals(Material.POTION, item.getType());
            assertEquals(2, item.getAmount());

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.HEALING, meta.getBasePotionType());
        }

        @Test
        @DisplayName("Should parse splash potion")
        void testSplashPotion() {
            ItemStack item = parser.parseReward("SPLASH_POTION:POISON:1");

            assertNotNull(item);
            assertEquals(Material.SPLASH_POTION, item.getType());
            assertEquals(1, item.getAmount());

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.POISON, meta.getBasePotionType());
        }

        @Test
        @DisplayName("Should parse lingering potion")
        void testLingeringPotion() {
            ItemStack item = parser.parseReward("LINGERING_POTION:REGENERATION:3");

            assertNotNull(item);
            assertEquals(Material.LINGERING_POTION, item.getType());
            assertEquals(3, item.getAmount());

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.REGENERATION, meta.getBasePotionType());
        }

        @Test
        @DisplayName("Should parse strong healing potion")
        void testStrongPotion() {
            ItemStack item = parser.parseReward("POTION:STRONG_HEALING:2");

            assertNotNull(item);
            assertEquals(Material.POTION, item.getType());

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.STRONG_HEALING, meta.getBasePotionType());
        }

        @Test
        @DisplayName("Should parse long regeneration potion")
        void testLongPotion() {
            ItemStack item = parser.parseReward("POTION:LONG_REGENERATION:1");

            assertNotNull(item);
            assertEquals(Material.POTION, item.getType());

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.LONG_REGENERATION, meta.getBasePotionType());
        }

        @Test
        @DisplayName("Should handle water bottles")
        void testWaterBottle() {
            ItemStack item = parser.parseReward("POTION:WATER:1");

            assertNotNull(item);
            assertEquals(Material.POTION, item.getType());

            PotionMeta meta = (PotionMeta) item.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.WATER, meta.getBasePotionType());
        }
    }

    @Nested
    @DisplayName("Item with Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("Should parse item with display name")
        void testItemWithDisplayName() {
            ItemStack item = parser.parseReward("DIAMOND_SWORD:1:Excalibur");

            assertNotNull(item);
            assertEquals(Material.DIAMOND_SWORD, item.getType());
            assertEquals(1, item.getAmount());

            ItemMeta meta = item.getItemMeta();
            assertNotNull(meta);
            assertEquals(Component.text("Excalibur"), meta.displayName());
        }

        @Test
        @DisplayName("Should parse item with multi-word display name")
        void testMultiWordDisplayName() {
            ItemStack item = parser.parseReward("DIAMOND_PICKAXE:1:Super Miner 3000");

            assertNotNull(item);
            assertEquals(Material.DIAMOND_PICKAXE, item.getType());

            ItemMeta meta = item.getItemMeta();
            assertNotNull(meta);
            assertEquals(Component.text("Super Miner 3000"), meta.displayName());
        }

        @Test
        @DisplayName("Should handle display name with special characters")
        void testDisplayNameWithSpecialChars() {
            ItemStack item = parser.parseReward("GOLDEN_APPLE:1:★ Golden Delicious ★");

            assertNotNull(item);
            assertEquals(Material.GOLDEN_APPLE, item.getType());

            ItemMeta meta = item.getItemMeta();
            assertNotNull(meta);
            assertEquals(Component.text("★ Golden Delicious ★"), meta.displayName());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return null for invalid material")
        void testInvalidMaterial() {
            ItemStack item = parser.parseReward("INVALID_MATERIAL:5");
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for invalid quantity")
        void testInvalidQuantity() {
            ItemStack item = parser.parseReward("DIAMOND:abc");
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for negative quantity")
        void testNegativeQuantity() {
            ItemStack item = parser.parseReward("DIAMOND:-5");
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for zero quantity")
        void testZeroQuantity() {
            ItemStack item = parser.parseReward("DIAMOND:0");
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for empty string")
        void testEmptyString() {
            ItemStack item = parser.parseReward("");
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for null string")
        void testNullString() {
            ItemStack item = parser.parseReward(null);
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for invalid format (missing quantity)")
        void testMissingQuantity() {
            ItemStack item = parser.parseReward("DIAMOND");
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for invalid potion type")
        void testInvalidPotionType() {
            ItemStack item = parser.parseReward("POTION:INVALID_TYPE:1");
            assertNull(item);
        }

        @Test
        @DisplayName("Should return null for potion with missing type")
        void testPotionMissingType() {
            ItemStack item = parser.parseReward("POTION:1");
            assertNull(item);
        }
    }

    @Nested
    @DisplayName("List Parsing Tests")
    class ListParsingTests {

        @Test
        @DisplayName("Should parse list of simple items")
        void testSimpleItemList() {
            List<String> rewards = Arrays.asList(
                "DIAMOND:5",
                "EMERALD:3",
                "GOLD_INGOT:10"
            );

            List<ItemStack> items = parser.parseRewards(rewards);

            assertEquals(3, items.size());
            assertEquals(Material.DIAMOND, items.get(0).getType());
            assertEquals(5, items.get(0).getAmount());
            assertEquals(Material.EMERALD, items.get(1).getType());
            assertEquals(3, items.get(1).getAmount());
            assertEquals(Material.GOLD_INGOT, items.get(2).getType());
            assertEquals(10, items.get(2).getAmount());
        }

        @Test
        @DisplayName("Should parse mixed list with items and potions")
        void testMixedList() {
            List<String> rewards = Arrays.asList(
                "DIAMOND:5",
                "POTION:HEALING:2",
                "SPLASH_POTION:POISON:1"
            );

            List<ItemStack> items = parser.parseRewards(rewards);

            assertEquals(3, items.size());
            assertEquals(Material.DIAMOND, items.get(0).getType());
            assertEquals(Material.POTION, items.get(1).getType());
            assertEquals(Material.SPLASH_POTION, items.get(2).getType());
        }

        @Test
        @DisplayName("Should skip invalid entries in list")
        void testSkipInvalidEntries() {
            List<String> rewards = Arrays.asList(
                "DIAMOND:5",
                "INVALID:3",
                "EMERALD:2"
            );

            List<ItemStack> items = parser.parseRewards(rewards);

            assertEquals(2, items.size());
            assertEquals(Material.DIAMOND, items.get(0).getType());
            assertEquals(Material.EMERALD, items.get(1).getType());
        }

        @Test
        @DisplayName("Should handle empty list")
        void testEmptyList() {
            List<ItemStack> items = parser.parseRewards(Arrays.asList());
            assertTrue(items.isEmpty());
        }

        @Test
        @DisplayName("Should handle null list")
        void testNullList() {
            List<ItemStack> items = parser.parseRewards(null);
            assertTrue(items.isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should parse item with very long display name")
        void testVeryLongDisplayName() {
            String longName = "A".repeat(100);
            ItemStack item = parser.parseReward("DIAMOND:" + "1:" + longName);

            assertNotNull(item);
            assertEquals(Material.DIAMOND, item.getType());
            ItemMeta meta = item.getItemMeta();
            assertNotNull(meta);
            assertEquals(Component.text(longName), meta.displayName());
        }

        @Test
        @DisplayName("Should handle colons in display name")
        void testColonsInDisplayName() {
            // Only splits on first 2 colons, rest becomes display name
            ItemStack item = parser.parseReward("DIAMOND:1:Name:With:Colons");

            assertNotNull(item);
            assertEquals(Material.DIAMOND, item.getType());
            ItemMeta meta = item.getItemMeta();
            assertNotNull(meta);
            // Should include everything after second colon
            assertEquals(Component.text("Name:With:Colons"), meta.displayName());
        }

        @Test
        @DisplayName("Should handle empty display name")
        void testEmptyDisplayName() {
            ItemStack item = parser.parseReward("DIAMOND:1:");

            assertNotNull(item);
            assertEquals(Material.DIAMOND, item.getType());
            // Empty display name should not be set
        }
    }

    @Nested
    @DisplayName("Real-world Config Examples")
    class RealWorldExamples {

        @Test
        @DisplayName("Should parse config.yml link rewards example")
        void testLinkRewardsExample() {
            List<String> rewards = Arrays.asList("EMERALD:1");
            List<ItemStack> items = parser.parseRewards(rewards);

            assertEquals(1, items.size());
            assertEquals(Material.EMERALD, items.get(0).getType());
            assertEquals(1, items.get(0).getAmount());
        }

        @Test
        @DisplayName("Should parse newbie kit examples")
        void testNewbieKitExamples() {
            List<String> rewards = Arrays.asList(
                "DIAMOND_PICKAXE:1",
                "BREAD:2"
            );
            List<ItemStack> items = parser.parseRewards(rewards);

            assertEquals(2, items.size());
            assertEquals(Material.DIAMOND_PICKAXE, items.get(0).getType());
            assertEquals(1, items.get(0).getAmount());
            assertEquals(Material.BREAD, items.get(1).getType());
            assertEquals(2, items.get(1).getAmount());
        }

        @Test
        @DisplayName("Should parse commented examples from config")
        void testCommentedExamples() {
            List<String> rewards = Arrays.asList(
                "EMERALD:1",
                "DIAMOND:5",
                "POTION:STRONG_HEALING:2",
                "SPLASH_POTION:POISON:1",
                "LINGERING_POTION:REGENERATION:3",
                "DIAMOND_SWORD:1:Excalibur"
            );

            List<ItemStack> items = parser.parseRewards(rewards);

            assertEquals(6, items.size());

            // Verify each item
            assertEquals(Material.EMERALD, items.get(0).getType());
            assertEquals(Material.DIAMOND, items.get(1).getType());
            assertEquals(Material.POTION, items.get(2).getType());
            assertEquals(Material.SPLASH_POTION, items.get(3).getType());
            assertEquals(Material.LINGERING_POTION, items.get(4).getType());
            assertEquals(Material.DIAMOND_SWORD, items.get(5).getType());

            // Verify display name on last item
            ItemMeta meta = items.get(5).getItemMeta();
            assertNotNull(meta);
            assertEquals(Component.text("Excalibur"), meta.displayName());
        }
    }
}
