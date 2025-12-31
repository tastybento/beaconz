/*
 * Copyright (c) 2015 - 2025 tastybento
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

package com.wasteofplastic.beaconz.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import com.wasteofplastic.beaconz.Beaconz;
import com.wasteofplastic.beaconz.BeaconzPluginDependent;

public class WorldListener extends BeaconzPluginDependent implements Listener {

    String regeneratingChunk = null;
    
    /**
     * Ensures that if the world is initialized, the beaconz populator is used.
     * @param plugin
     */
    public WorldListener(Beaconz plugin) {
        super(plugin);
    }

    /* 
     * not used anymore...
     *      
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInit(WorldInitEvent event)
    {
        //Bukkit.getLogger().info("On World Init called");
        if (event.getWorld().equals(getBeaconzWorld())) {
            if (!getBeaconzWorld().getPopulators().contains(getBlockPopulator())) {
                event.getWorld().getPopulators().add(getBlockPopulator());
            }
        }
    }
    */
    
    /**
     * When a chunk is loaded in the world, we check if it needs to be populated with beacons
     * The block at ch(0,0,0) is a sign that has a game's CreateTime
     * If the sign's info doesn't match the current game's CreateTime, the chunk needs to be populated
     * After a chunk is populated, we update the sign
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onChunkLoad(ChunkLoadEvent event)
    {           
        //senderMsg(Bukkit.getConsoleSender(), "LOADING CHUNK: " + event.getChunk().getX() + ":" + event.getChunk().getZ());
        // We only deal with events on the Beaconz World
        if (event.getWorld().equals(getBeaconzWorld()) && !getBeaconzPlugin().ignoreChunkLoad) {
            boolean regen = false;
            int cX = event.getChunk().getX() << 4;
            int cZ = event.getChunk().getZ() << 4;
            
            //senderMsg(Bukkit.getConsoleSender(), "CHECKING CHUNK: " + event.getChunk().getX() + ":" + event.getChunk().getZ());
            
            if (regeneratingChunk != null && regeneratingChunk.equals(event.getChunk().getX() + ":" + event.getChunk().getZ())) {  
                // The regenertingChunk flag indicates that this method has just regenerated this chunk (see below)
                // When a chunk is regenerated, it is reloaded, triggering this event again
                // As the chunk is freshly generated, we need to populate it and set its ID
                
                // Clear the flag
                regeneratingChunk = null;
                
                // Populate the chunk
                getBlockPopulator().populate(event.getWorld(), null, event.getChunk());
            
                // Set the chunk's ID
                String gCT = " " + getGameMgr().getGame(cX, cZ).getCreateTime().toString();
                Block bl = event.getChunk().getBlock(7, 1, 7);
                bl.setType(Material.OAK_WALL_SIGN, false);
                Sign sign = (Sign) bl.getState();
                sign.setLine(0, gCT);
                sign.update();  
                
            } else {
                // Check if it's a game chunk and if it needs to be regenerated
                if (event.getWorld().equals(getBeaconzWorld()) 
                        && getGameMgr() != null
                        && getGameMgr().getGame(cX, cZ) != null
                        && getGameMgr().getRegion(cX, cZ) != getGameMgr().getLobby()) {                    
                    
                    // It's a game chunk, see if it needs to be regenerated - compare the chunk ID sign to the game's Create Time            
                    Block bl = event.getChunk().getBlock(7, 1, 7);
                    String gCT = " " + getGameMgr().getGame(cX, cZ).getCreateTime().toString();   
                    
                    // Check the chunk's ID sign
                    if (!bl.getType().equals(Material.OAK_WALL_SIGN)) {
                        // There's no sign, it must be the first time the game is being loaded; let's regen
                        regen = true;
                        
                    } else {                
                        // Block 0,1,0 contains a sign, check if it matches game creation time
                        Sign sign = (Sign) bl.getState();                    
                        if (!sign.getLine(0).equals(gCT)) {
                            // Need to regenerate the chunk
                            regen = true;                                      
                        }
                    }
                    
                    // Regenerate if needed... it's best if the regenerate call is the last thing in this method,
                    // since it will trigger another chunk load event for the same chunk...
                    if (regen) {
                        //senderMsg(Bukkit.getConsoleSender(), ChatColor.GREEN + "REGENERATING CHUNK: " + event.getChunk().getX() + ":" + event.getChunk().getZ() + " gCT = " + gCT);
                        regeneratingChunk = event.getChunk().getX() + ":" + event.getChunk().getZ();
                        event.getWorld().regenerateChunk(event.getChunk().getX(), event.getChunk().getZ());                  
                    }                                   
                }   
            }                                 
        }
    }       
}
