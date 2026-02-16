package com.wasteofplastic.beaconz.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Handles clicks in the score GUI inventory.
 * Prevents item movement and closes the inventory when clicking outside.
 */
public class ScoreGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Get the inventory title
        String inventoryTitle = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        // Check if this is a score GUI by looking for "Scores" in the title
        // The default title is "Scores - <game>" with formatting codes (§)
        // Even if the locale is changed, it should still contain "Score" or similar
        if (!inventoryTitle.contains("§") || !inventoryTitle.toLowerCase().contains("score")) {
            return; // Not our score GUI
        }

        // Cancel the event to prevent item movement in score GUI
        event.setCancelled(true);

        // Close inventory if clicked outside the inventory area
        if (event.getClickedInventory() == null || event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
            player.closeInventory();
        }
    }
}

