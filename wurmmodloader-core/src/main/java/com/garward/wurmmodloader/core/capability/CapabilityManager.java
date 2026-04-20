package com.garward.wurmmodloader.core.capability;

import com.garward.wurmmodloader.api.capability.Capability;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.player.PlayerLogoutEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityLoadedEvent;
import com.garward.wurmmodloader.api.events.server.ServerPollEvent;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.event.EventBus;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for all capabilities in the framework.
 *
 * <p>Handles registration, storage, and retrieval of capability data.
 * Provides automatic persistence via CapabilityDatabase.</p>
 *
 * <p>This is a singleton that is initialized during server startup.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0 (Phase 5.5)
 */
public class CapabilityManager {

    private static final Logger logger = Logger.getLogger(CapabilityManager.class.getName());
    private static final CapabilityManager INSTANCE = new CapabilityManager();

    // Registered capabilities by target type
    private final Set<Capability<?>> playerCapabilities = ConcurrentHashMap.newKeySet();
    private final Set<Capability<?>> creatureCapabilities = ConcurrentHashMap.newKeySet();
    private final Set<Capability<?>> itemCapabilities = ConcurrentHashMap.newKeySet();
    private final Set<Capability<?>> tileCapabilities = ConcurrentHashMap.newKeySet();

    // Storage for attached capabilities
    // Key: target object ID (wurmId), Value: Map of capability -> instance
    private final Map<Long, Map<Capability<?>, Object>> playerCapabilityData = new ConcurrentHashMap<>();
    private final Map<Long, Map<Capability<?>, Object>> creatureCapabilityData = new ConcurrentHashMap<>();
    private final Map<Long, Map<Capability<?>, Object>> itemCapabilityData = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Capability<?>, Object>> tileCapabilityData = new ConcurrentHashMap<>();

    // Database for persistence
    private CapabilityDatabase database;
    private boolean initialized = false;

    // Periodic save cadence. ServerPollEvent fires ~5s; 6 ticks ≈ 30s between flushes.
    private static final int SAVE_EVERY_N_POLLS = 6;
    private int pollsSinceSave = 0;

    private CapabilityManager() {
        // Private constructor for singleton
    }

    public static CapabilityManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize the capability manager with database.
     * Called during server startup.
     */
    public void initialize() {
        if (initialized) {
            logger.warning("CapabilityManager already initialized");
            return;
        }

        try {
            // Initialize database in mods/wurmmodloader/ directory
            String dbPath = Paths.get("mods", "wurmmodloader", "capabilities.db").toString();
            this.database = new CapabilityDatabase(dbPath);
            this.initialized = true;

            // Subscribe to periodic save + logout hooks so disk state tracks memory.
            EventBus.getInstance().register(this);

            logger.info("CapabilityManager initialized with database: " + dbPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize CapabilityManager", e);
            throw new RuntimeException("CapabilityManager initialization failed", e);
        }
    }

    /**
     * Register a capability for Players.
     */
    public <T> void registerPlayerCapability(Capability<T> capability) {
        if (playerCapabilities.add(capability)) {
            logger.info("Registered player capability: " + capability.getId());
        }
    }

    /**
     * Register a capability for Creatures.
     */
    public <T> void registerCreatureCapability(Capability<T> capability) {
        if (creatureCapabilities.add(capability)) {
            logger.info("Registered creature capability: " + capability.getId());
        }
    }

    /**
     * Register a capability for Items.
     */
    public <T> void registerItemCapability(Capability<T> capability) {
        if (itemCapabilities.add(capability)) {
            logger.info("Registered item capability: " + capability.getId());
        }
    }

    /**
     * Register a capability for Tiles.
     */
    public <T> void registerTileCapability(Capability<T> capability) {
        if (tileCapabilities.add(capability)) {
            logger.info("Registered tile capability: " + capability.getId());
        }
    }

    /**
     * Get a capability from a player.
     *
     * @param wurmId The player's wurm ID
     * @param capability The capability to get
     * @return The capability instance (never null)
     */
    public <T> T getPlayerCapability(long wurmId, Capability<T> capability) {
        checkInitialized();
        if (!playerCapabilities.contains(capability)) {
            throw new IllegalArgumentException("Capability not registered for players: " + capability.getId());
        }
        return getCapability(playerCapabilityData, wurmId, capability, "player");
    }

    /**
     * Get a capability from a creature.
     */
    public <T> T getCreatureCapability(long wurmId, Capability<T> capability) {
        checkInitialized();
        if (!creatureCapabilities.contains(capability)) {
            throw new IllegalArgumentException("Capability not registered for creatures: " + capability.getId());
        }
        return getCapability(creatureCapabilityData, wurmId, capability, "creature");
    }

    /**
     * Get a capability from an item.
     */
    public <T> T getItemCapability(long wurmId, Capability<T> capability) {
        checkInitialized();
        if (!itemCapabilities.contains(capability)) {
            throw new IllegalArgumentException("Capability not registered for items: " + capability.getId());
        }
        return getCapability(itemCapabilityData, wurmId, capability, "item");
    }

    /**
     * Get a capability from a tile.
     */
    public <T> T getTileCapability(int tileId, Capability<T> capability) {
        checkInitialized();
        if (!tileCapabilities.contains(capability)) {
            throw new IllegalArgumentException("Capability not registered for tiles: " + capability.getId());
        }
        return getCapabilityForTile(tileCapabilityData, tileId, capability);
    }

    /**
     * Check if a player has a capability attached.
     */
    public <T> boolean hasPlayerCapability(long wurmId, Capability<T> capability) {
        Map<Capability<?>, Object> caps = playerCapabilityData.get(wurmId);
        return caps != null && caps.containsKey(capability);
    }

    /**
     * Check if a creature has a capability attached.
     */
    public <T> boolean hasCreatureCapability(long wurmId, Capability<T> capability) {
        Map<Capability<?>, Object> caps = creatureCapabilityData.get(wurmId);
        return caps != null && caps.containsKey(capability);
    }

    /**
     * Check if an item has a capability attached.
     */
    public <T> boolean hasItemCapability(long wurmId, Capability<T> capability) {
        Map<Capability<?>, Object> caps = itemCapabilityData.get(wurmId);
        return caps != null && caps.containsKey(capability);
    }

    /**
     * Check if a tile has a capability attached.
     */
    public <T> boolean hasTileCapability(int tileId, Capability<T> capability) {
        Map<Capability<?>, Object> caps = tileCapabilityData.get(tileId);
        return caps != null && caps.containsKey(capability);
    }

    @SuppressWarnings("unchecked")
    private <T> T getCapability(Map<Long, Map<Capability<?>, Object>> storage,
                                long id,
                                Capability<T> capability,
                                String entityType) {
        // Get or create capability map for this entity
        Map<Capability<?>, Object> caps = storage.computeIfAbsent(id, k -> new ConcurrentHashMap<>());

        // Get or load capability instance
        Object instance = caps.get(capability);
        if (instance == null) {
            // Try to load from database
            String serialized = database.loadCapability(id, capability.getId(), entityType);
            if (serialized != null) {
                try {
                    instance = capability.deserialize(serialized);
                    caps.put(capability, instance);
                    logger.fine("Loaded " + entityType + " capability " + capability.getId() + " for ID " + id);
                    fireLoadedEvent(entityType, id, capability, instance);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to deserialize capability, creating default", e);
                    instance = null; // Fall through to create default
                }
            }

            // Create default instance if not found in DB
            if (instance == null) {
                instance = capability.createDefaultInstance();
                caps.put(capability, instance);

                // Mark dirty for saving
                database.markDirty(id, capability.getId(), entityType);
                logger.fine("Created default " + entityType + " capability " + capability.getId() + " for ID " + id);
            }
        }

        return (T) instance;
    }

    @SuppressWarnings("unchecked")
    private <T> T getCapabilityForTile(Map<Integer, Map<Capability<?>, Object>> storage,
                                       int id,
                                       Capability<T> capability) {
        // Same as getCapability but for Integer keys (tile IDs)
        Map<Capability<?>, Object> caps = storage.computeIfAbsent(id, k -> new ConcurrentHashMap<>());

        Object instance = caps.get(capability);
        if (instance == null) {
            String serialized = database.loadCapability(id, capability.getId(), "tile");
            if (serialized != null) {
                try {
                    instance = capability.deserialize(serialized);
                    caps.put(capability, instance);
                    fireLoadedEvent("tile", id, capability, instance);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to deserialize tile capability, creating default", e);
                    instance = null;
                }
            }

            if (instance == null) {
                instance = capability.createDefaultInstance();
                caps.put(capability, instance);
                database.markDirty(id, capability.getId(), "tile");
            }
        }

        return (T) instance;
    }

    private void fireLoadedEvent(String entityType, long id, Capability<?> capability, Object instance) {
        try {
            CapabilityLoadedEvent.EntityType type;
            switch (entityType) {
                case "player":   type = CapabilityLoadedEvent.EntityType.PLAYER; break;
                case "creature": type = CapabilityLoadedEvent.EntityType.CREATURE; break;
                case "item":     type = CapabilityLoadedEvent.EntityType.ITEM; break;
                case "tile":     type = CapabilityLoadedEvent.EntityType.TILE; break;
                default: return;
            }
            EventBus.getInstance().post(new CapabilityLoadedEvent(type, id, capability, instance));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to post CapabilityLoadedEvent", e);
        }
    }

    /**
     * Persist every loaded capability to the database.
     *
     * <p>Writes are full overwrites: for each cached entity we serialize the
     * current instance and INSERT OR REPLACE. This is acceptable at the
     * expected scale (player counts in the dozens, not thousands) and avoids
     * the broken dirty-tracking design that used to live here.</p>
     */
    public void saveAllDirty() {
        if (!initialized) {
            return;
        }
        try {
            long start = System.nanoTime();
            int written = 0;
            for (Map.Entry<Long, Map<Capability<?>, Object>> e : playerCapabilityData.entrySet()) {
                written += writeAll(e.getKey(), e.getValue(), "player");
            }
            for (Map.Entry<Long, Map<Capability<?>, Object>> e : creatureCapabilityData.entrySet()) {
                written += writeAll(e.getKey(), e.getValue(), "creature");
            }
            for (Map.Entry<Long, Map<Capability<?>, Object>> e : itemCapabilityData.entrySet()) {
                written += writeAll(e.getKey(), e.getValue(), "item");
            }
            for (Map.Entry<Integer, Map<Capability<?>, Object>> e : tileCapabilityData.entrySet()) {
                written += writeAll((long) e.getKey(), e.getValue(), "tile");
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            if (written > 0) {
                logger.fine("Saved " + written + " capabilities in " + elapsed + "ms");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to persist capabilities", e);
        }
    }

    @SuppressWarnings("unchecked")
    private int writeAll(long id, Map<Capability<?>, Object> caps, String entityType) {
        int n = 0;
        for (Map.Entry<Capability<?>, Object> e : caps.entrySet()) {
            try {
                Capability<Object> cap = (Capability<Object>) e.getKey();
                String serialized = cap.serialize(e.getValue());
                database.saveCapability(id, cap.getId(), entityType, serialized);
                n++;
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to save " + entityType + " capability "
                    + e.getKey().getId() + " for id " + id, ex);
            }
        }
        return n;
    }

    /**
     * Periodic persistence hook. Fires every {@link #SAVE_EVERY_N_POLLS} server polls.
     */
    @SubscribeEvent
    public void onServerPoll(ServerPollEvent event) {
        if (!initialized) return;
        if (++pollsSinceSave < SAVE_EVERY_N_POLLS) return;
        pollsSinceSave = 0;
        saveAllDirty();
    }

    /**
     * Player logout hook. Flushes and evicts that player's cached capabilities
     * so hot data doesn't accumulate for offline players.
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerLogoutEvent event) {
        if (!initialized) return;
        try {
            Object p = event.getPlayer();
            if (p instanceof com.wurmonline.server.creatures.Creature) {
                unloadPlayer(((com.wurmonline.server.creatures.Creature) p).getWurmId());
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Capability logout flush failed", t);
        }
    }

    /**
     * Unload capabilities for a player (when they log out).
     * Saves dirty data before unloading.
     */
    public void unloadPlayer(long wurmId) {
        Map<Capability<?>, Object> caps = playerCapabilityData.remove(wurmId);
        if (caps != null) {
            saveCapabilities(wurmId, caps, "player");
            logger.fine("Unloaded player capabilities for ID " + wurmId);
        }
    }

    /**
     * Unload capabilities for a creature.
     */
    public void unloadCreature(long wurmId) {
        Map<Capability<?>, Object> caps = creatureCapabilityData.remove(wurmId);
        if (caps != null) {
            saveCapabilities(wurmId, caps, "creature");
        }
    }

    /**
     * Unload capabilities for an item.
     */
    public void unloadItem(long wurmId) {
        Map<Capability<?>, Object> caps = itemCapabilityData.remove(wurmId);
        if (caps != null) {
            saveCapabilities(wurmId, caps, "item");
        }
    }

    @SuppressWarnings("unchecked")
    private void saveCapabilities(long id, Map<Capability<?>, Object> caps, String entityType) {
        caps.forEach((cap, instance) -> {
            try {
                Capability<Object> capCast = (Capability<Object>) cap;
                String serialized = capCast.serialize(instance);
                database.saveCapability(id, cap.getId(), entityType, serialized);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to save capability " + cap.getId() + " for " + entityType + " " + id, e);
            }
        });
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("CapabilityManager not initialized");
        }
    }

    /**
     * Shutdown the capability manager and close database.
     */
    public void shutdown() {
        if (initialized) {
            try {
                // Save all remaining dirty data
                saveAllDirty();

                // Close database
                if (database != null) {
                    database.close();
                }

                initialized = false;
                logger.info("CapabilityManager shut down");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error during CapabilityManager shutdown", e);
            }
        }
    }
}
