/*
 * Copyright (c) 2015 - 2026 tastybento
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wasteofplastic.beaconz.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import net.kyori.adventure.text.Component;

/**
 * Modern item reward parser supporting multiple configuration formats.
 *
 * <p>Supported formats:
 * <ul>
 *   <li><b>Simple:</b> {@code MATERIAL:QUANTITY} - e.g., "DIAMOND:5"</li>
 *   <li><b>Potion:</b> {@code POTION:POTION_TYPE:QUANTITY} - e.g., "POTION:STRONG_HEALING:2"</li>
 *   <li><b>Splash Potion:</b> {@code SPLASH_POTION:POTION_TYPE:QUANTITY} - e.g., "SPLASH_POTION:POISON:1"</li>
 *   <li><b>Lingering Potion:</b> {@code LINGERING_POTION:POTION_TYPE:QUANTITY} - e.g., "LINGERING_POTION:REGENERATION:3"</li>
 *   <li><b>With Display Name:</b> {@code MATERIAL:QUANTITY:Display Name} - e.g., "DIAMOND_SWORD:1:Excalibur"</li>
 * </ul>
 *
 * <p>This replaces the legacy durability-based format which is no longer supported in modern Minecraft.
 *
 * @since 2.0.0
 */
public class ItemRewardParser {

    private final Logger logger;

    /**
     * Creates a new ItemRewardParser.
     *
     * @param logger Logger for error reporting
     */
    public ItemRewardParser(Logger logger) {
        this.logger = logger;
    }

    /**
     * Parses a list of item reward strings into ItemStacks.
     *
     * @param rewardStrings List of reward configuration strings
     * @return List of successfully parsed ItemStacks (may be empty if all failed)
     */
    public List<ItemStack> parseRewards(List<String> rewardStrings) {
        List<ItemStack> items = new ArrayList<>();

        if (rewardStrings == null || rewardStrings.isEmpty()) {
            return items;
        }

        for (String rewardString : rewardStrings) {
            try {
                ItemStack item = parseReward(rewardString);
                if (item != null) {
                    items.add(item);
                }
            } catch (Exception e) {
                logger.severe("Failed to parse reward: " + rewardString);
                logger.severe("Error: " + e.getMessage());
            }
        }

        return items;
    }

    /**
     * Parses a single reward string into an ItemStack.
     *
     * @param rewardString The reward configuration string
     * @return ItemStack or null if parsing fails
     */
    public ItemStack parseReward(String rewardString) {
        if (rewardString == null || rewardString.trim().isEmpty()) {
            logger.warning("Empty reward string provided");
            return null;
        }

        String[] parts = rewardString.trim().split(":", 3);

        if (parts.length < 2) {
            logger.severe("Invalid reward format: " + rewardString);
            logger.severe("Expected format: MATERIAL:QUANTITY or MATERIAL:QUANTITY:DisplayName");
            return null;
        }

        // Parse material
        String materialName = parts[0].trim().toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            logger.severe("Unknown material: " + materialName);
            suggestSimilarMaterials(materialName);
            return null;
        }

        // Check if material is a potion type
        if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION) {
            return parsePotionReward(material, parts);
        }

        // Parse quantity
        int quantity;
        try {
            quantity = Integer.parseInt(parts[1].trim());
            if (quantity <= 0) {
                logger.severe("Quantity must be positive: " + rewardString);
                return null;
            }
        } catch (NumberFormatException e) {
            logger.severe("Invalid quantity in reward: " + rewardString);
            return null;
        }

        // Create item stack
        ItemStack item = new ItemStack(material, quantity);

        // Apply display name if provided (3rd part)
        if (parts.length == 3 && !parts[2].trim().isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(parts[2].trim()));
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * Parses a potion reward.
     * Format: POTION_MATERIAL:POTION_TYPE:QUANTITY
     *
     * @param potionMaterial The potion material (POTION, SPLASH_POTION, or LINGERING_POTION)
     * @param parts The split reward string parts
     * @return ItemStack of the potion or null if parsing fails
     */
    private ItemStack parsePotionReward(Material potionMaterial, String[] parts) {
        if (parts.length < 3) {
            logger.severe("Invalid potion format. Expected: " + potionMaterial + ":POTION_TYPE:QUANTITY");
            logger.severe("Example: POTION:STRONG_HEALING:2");
            listAvailablePotionTypes();
            return null;
        }

        // Parse potion type
        String potionTypeName = parts[1].trim().toUpperCase();
        PotionType potionType;
        try {
            potionType = PotionType.valueOf(potionTypeName);
        } catch (IllegalArgumentException e) {
            logger.severe("Unknown potion type: " + potionTypeName);
            listAvailablePotionTypes();
            return null;
        }

        // Parse quantity
        int quantity;
        try {
            quantity = Integer.parseInt(parts[2].trim());
            if (quantity <= 0) {
                logger.severe("Quantity must be positive");
                return null;
            }
        } catch (NumberFormatException e) {
            logger.severe("Invalid quantity for potion: " + parts[2]);
            return null;
        }

        // Create potion
        ItemStack potion = new ItemStack(potionMaterial, quantity);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(potionType);
            potion.setItemMeta(meta);
        }

        return potion;
    }

    /**
     * Suggests similar materials when an unknown material is provided.
     *
     * @param input The unknown material name
     */
    private void suggestSimilarMaterials(String input) {
        if (input.length() < 3) {
            return;
        }

        String prefix = input.substring(0, 3).toUpperCase();
        List<String> suggestions = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.isItem() && material.name().startsWith(prefix)) {
                suggestions.add(material.name());
                if (suggestions.size() >= 5) {
                    break;
                }
            }
        }

        if (!suggestions.isEmpty()) {
            logger.severe("Did you mean one of these? " + String.join(", ", suggestions));
        }
    }

    /**
     * Lists all available potion types.
     */
    private void listAvailablePotionTypes() {
        StringBuilder potionTypes = new StringBuilder("Available potion types: ");
        for (PotionType type : PotionType.values()) {
            potionTypes.append(type.name()).append(", ");
        }
        logger.severe(potionTypes.substring(0, potionTypes.length() - 2));
    }
}
