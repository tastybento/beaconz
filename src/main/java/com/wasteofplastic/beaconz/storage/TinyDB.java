package com.wasteofplastic.beaconz.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import com.wasteofplastic.beaconz.Beaconz;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Player name to UUID database.
 * Uses JSON Lines format for better data integrity and easier maintenance.
 */
public class TinyDB {
    private final Beaconz plugin;
    private final ConcurrentHashMap<String, UUID> cache;
    private final Path databasePath;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile boolean saving = false;

    public TinyDB(Beaconz plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.databasePath = plugin.getDataFolder().toPath().resolve("name-uuid.jsonl");

        // Create parent directories if needed
        try {
            Files.createDirectories(databasePath.getParent());
            // Load existing data into cache on startup
            loadCache();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    /**
     * Load all entries into cache on startup (lazy initialization alternative)
     */
    private void loadCache() throws IOException {
        if (!Files.exists(databasePath)) {
            return;
        }

        lock.readLock().lock();
        try (Stream<String> lines = Files.lines(databasePath)) {
            lines.forEach(line -> {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    try {
                        cache.put(parts[0].toLowerCase(), UUID.fromString(parts[1]));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in database: " + line);
                    }
                }
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    public void asyncSaveDB() {
        if (saving) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                saveDB();
            }
        }.runTaskAsynchronously(plugin);
    }

    public void saveDB() {
        saving = true;
        lock.writeLock().lock();

        try {
            // Write to temporary file first
            Path tempPath = databasePath.resolveSibling("name-uuid.tmp");

            // Use atomic write with modern Java NIO
            try (var writer = Files.newBufferedWriter(tempPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                for (var entry : cache.entrySet()) {
                    writer.write(entry.getKey());
                    writer.write('\t');
                    writer.write(entry.getValue().toString());
                    writer.newLine();
                }
            }

            // Atomic move to replace old database
            Files.move(tempPath, databasePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save database: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
            saving = false;
        }
    }

    /**
     * Saves the player name to the database. Case insensitive!
     */
    public void savePlayerName(String playerName, UUID playerUUID) {
        cache.put(playerName.toLowerCase(), playerUUID);
    }

    /**
     * Gets the UUID for this player name or null if not known. Case insensitive!
     */
    public UUID getPlayerUUID(String playerName) {
        return cache.get(playerName.toLowerCase());
    }

    /**
     * Gets the number of cached entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Checks if a player name is in the database
     */
    public boolean hasPlayer(String playerName) {
        return cache.containsKey(playerName.toLowerCase());
    }
}
