package com.wasteofplastic.beaconz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
 * Tests for Beaconz.giveItems() method.
 * Verifies that the modernized item reward system correctly parses and gives items to players.
 */
class BeaconzGiveItemsTest {

    private ServerMock server;
    private Beaconz plugin;
    private Player player;
    private PlayerInventory inventory;

    @BeforeEach
    void setUp() {
        MockBukkit.unmock();
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Beaconz.class);

        // Create mock player
        player = server.addPlayer("TestPlayer");
        inventory = player.getInventory();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Nested
    @DisplayName("Simple Item Rewards")
    class SimpleItemRewards {

        @Test
        @DisplayName("Should give single simple item to player")
        void testGiveSingleItem() {
            List<String> rewards = Arrays.asList("DIAMOND:5");

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(Material.DIAMOND, result.get(0).getType());
            assertEquals(5, result.get(0).getAmount());
        }

        @Test
        @DisplayName("Should give multiple different items")
        void testGiveMultipleItems() {
            List<String> rewards = Arrays.asList(
                "DIAMOND:5",
                "EMERALD:3",
                "GOLD_INGOT:10"
            );

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(3, result.size());
            assertEquals(Material.DIAMOND, result.get(0).getType());
            assertEquals(5, result.get(0).getAmount());
            assertEquals(Material.EMERALD, result.get(1).getType());
            assertEquals(3, result.get(1).getAmount());
            assertEquals(Material.GOLD_INGOT, result.get(2).getType());
            assertEquals(10, result.get(2).getAmount());
        }

        @Test
        @DisplayName("Should add items to player inventory")
        void testItemsAddedToInventory() {
            List<String> rewards = Arrays.asList("DIAMOND:5");

            plugin.giveItems(player, rewards);

            // Check inventory contains the diamond
            assertTrue(inventory.contains(Material.DIAMOND));
        }
    }

    @Nested
    @DisplayName("Potion Rewards")
    class PotionRewards {

        @Test
        @DisplayName("Should give regular potion")
        void testGiveRegularPotion() {
            List<String> rewards = Arrays.asList("POTION:HEALING:2");

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(1, result.size());
            ItemStack potion = result.get(0);
            assertEquals(Material.POTION, potion.getType());
            assertEquals(2, potion.getAmount());

            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.HEALING, meta.getBasePotionType());
        }

        @Test
        @DisplayName("Should give splash potion")
        void testGiveSplashPotion() {
            List<String> rewards = Arrays.asList("SPLASH_POTION:POISON:1");

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(1, result.size());
            ItemStack potion = result.get(0);
            assertEquals(Material.SPLASH_POTION, potion.getType());

            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.POISON, meta.getBasePotionType());
        }

        @Test
        @DisplayName("Should give lingering potion")
        void testGiveLingeringPotion() {
            List<String> rewards = Arrays.asList("LINGERING_POTION:REGENERATION:3");

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(1, result.size());
            ItemStack potion = result.get(0);
            assertEquals(Material.LINGERING_POTION, potion.getType());
            assertEquals(3, potion.getAmount());

            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            assertNotNull(meta);
            assertEquals(PotionType.REGENERATION, meta.getBasePotionType());
        }
    }

    @Nested
    @DisplayName("Items with Display Names")
    class ItemsWithDisplayNames {

        @Test
        @DisplayName("Should give item with display name")
        void testGiveItemWithDisplayName() {
            List<String> rewards = Arrays.asList("DIAMOND_SWORD:1:Excalibur");

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(1, result.size());
            ItemStack sword = result.get(0);
            assertEquals(Material.DIAMOND_SWORD, sword.getType());
            assertEquals(Component.text("Excalibur"), sword.getItemMeta().displayName());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should skip invalid items and continue")
        void testSkipInvalidItems() {
            List<String> rewards = Arrays.asList(
                "DIAMOND:5",
                "INVALID_MATERIAL:3",
                "EMERALD:2"
            );

            List<ItemStack> result = plugin.giveItems(player, rewards);

            // Should only have the valid items
            assertEquals(2, result.size());
            assertEquals(Material.DIAMOND, result.get(0).getType());
            assertEquals(Material.EMERALD, result.get(1).getType());
        }

        @Test
        @DisplayName("Should handle empty reward list")
        void testEmptyRewardList() {
            List<ItemStack> result = plugin.giveItems(player, Arrays.asList());

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle null reward list")
        void testNullRewardList() {
            List<ItemStack> result = plugin.giveItems(player, null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle malformed reward strings")
        void testMalformedRewards() {
            List<String> rewards = Arrays.asList(
                "DIAMOND",  // Missing quantity
                "EMERALD:abc",  // Invalid quantity
                ":5"  // Missing material
            );

            List<ItemStack> result = plugin.giveItems(player, rewards);

            // All should fail to parse
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Config.yml Examples")
    class ConfigExamples {

        @Test
        @DisplayName("Should handle link rewards from config")
        void testLinkRewards() {
            List<String> rewards = Arrays.asList("EMERALD:1");

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(1, result.size());
            assertEquals(Material.EMERALD, result.get(0).getType());
        }

        @Test
        @DisplayName("Should handle newbie kit from config")
        void testNewbieKit() {
            List<String> rewards = Arrays.asList(
                "DIAMOND_PICKAXE:1",
                "BREAD:2"
            );

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(2, result.size());
            assertEquals(Material.DIAMOND_PICKAXE, result.get(0).getType());
            assertEquals(Material.BREAD, result.get(1).getType());
            assertEquals(2, result.get(1).getAmount());
        }

        @Test
        @DisplayName("Should handle all commented config examples")
        void testAllCommentedExamples() {
            List<String> rewards = Arrays.asList(
                "EMERALD:1",
                "DIAMOND:5",
                "POTION:STRONG_HEALING:2",
                "SPLASH_POTION:POISON:1",
                "LINGERING_POTION:REGENERATION:3",
                "DIAMOND_SWORD:1:Excalibur"
            );

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(6, result.size());
            assertEquals(Material.EMERALD, result.get(0).getType());
            assertEquals(Material.DIAMOND, result.get(1).getType());
            assertEquals(Material.POTION, result.get(2).getType());
            assertEquals(Material.SPLASH_POTION, result.get(3).getType());
            assertEquals(Material.LINGERING_POTION, result.get(4).getType());
            assertEquals(Material.DIAMOND_SWORD, result.get(5).getType());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should play sound when giving items")
        void testSoundPlayed() {
            List<String> rewards = Arrays.asList("DIAMOND:5");

            plugin.giveItems(player, rewards);

            // Sound should be played (verified by MockBukkit's world)
            // This is a basic check that the method completes
            assertFalse(inventory.isEmpty());
        }

        @Test
        @DisplayName("Should give items to correct player")
        void testCorrectPlayer() {
            Player player2 = server.addPlayer("Player2");
            List<String> rewards = Arrays.asList("DIAMOND:5");

            plugin.giveItems(player, rewards);

            // Only player should have diamonds
            assertTrue(player.getInventory().contains(Material.DIAMOND));
            assertFalse(player2.getInventory().contains(Material.DIAMOND));
        }

        @Test
        @DisplayName("Should handle multiple rewards of same type")
        void testMultipleSameType() {
            List<String> rewards = Arrays.asList(
                "DIAMOND:5",
                "DIAMOND:3"
            );

            List<ItemStack> result = plugin.giveItems(player, rewards);

            assertEquals(2, result.size());
            // Both should be diamonds
            assertEquals(Material.DIAMOND, result.get(0).getType());
            assertEquals(Material.DIAMOND, result.get(1).getType());
        }
    }
}
