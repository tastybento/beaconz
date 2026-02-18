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

package com.wasteofplastic.beaconz.commands.subcommands;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Listens for chat input when players are entering game names from the GUI.
 *
 * <p>This listener intercepts chat messages from players who have pending game
 * creation requests (initiated from the GUI). When detected, it uses the chat
 * message as the game name and completes the game creation process.
 *
 * @author tastybento
 * @since 2.0
 */
public class NewGameChatListener extends BeaconzPluginDependent implements Listener {

    private final NewGameGUI gui;
    private final NewGameCommand command;

    /**
     * Constructs a new chat listener.
     *
     * @param plugin the Beaconz plugin instance
     * @param gui the GUI handler
     * @param command the command handler
     */
    public NewGameChatListener(Beaconz plugin, NewGameGUI gui, NewGameCommand command) {
        super(plugin);
        this.gui = gui;
        this.command = command;
    }

    /**
     * Handles chat events to capture game names for pending game creation.
     *
     * @param event the chat event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        NewGameGUI.PendingGameCreation pending = gui.getPendingCreation(player);

        if (pending != null) {
            event.setCancelled(true);

            String gameName = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

            // Run on main thread since game creation involves world operations
            getBeaconzPlugin().getServer().getScheduler().runTask(getBeaconzPlugin(), () -> command.completeGameCreation(player, gameName, pending.params()));
        }
    }
}
