package com.garward.wurmmodloader.mods.soulboundgear;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for soulbound gear system.
 *
 * <p>Thread-safe singleton that uses framework capabilities for item data persistence.
 * Replaces old database/DAO pattern with automatic lazy loading and dirty tracking.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * // Bind item to player
 * SoulboundGearManager.getInstance().bindItem(itemWurmId, playerWurmId);
 *
 * // Award XP (framework auto-saves)
 * SoulboundGearManager.getInstance().awardXP(itemWurmId, 100);
 *
 * // Get item data
 * Optional&lt;SoulboundItem&gt; item = SoulboundGearManager.getInstance().getItem(itemWurmId);
 * </pre>
 *
 * @author Power Fantasy RPG Team
 * @version 2.0.0 (Capabilities Migration)
 */
public final class SoulboundGearManager {

    private static final Logger logger = Logger.getLogger(SoulboundGearManager.class.getName());

    private static SoulboundGearManager instance;

    private final SoulboundConfig config;

    // In-memory cache for performance (optional, framework handles persistence)
    private final Map<Long, SoulboundItem> cache = new ConcurrentHashMap<>();

    /**
     * Private constructor - singleton pattern.
     *
     * @param config Configuration
     */
    private SoulboundGearManager(SoulboundConfig config) {
        this.config = config;
    }

    /**
     * Initialize the manager.
     *
     * @param config Configuration
     */
    public static synchronized void initialize(SoulboundConfig config) {
        if (instance != null) {
            throw new IllegalStateException("SoulboundGearManager already initialized");
        }
        instance = new SoulboundGearManager(config);
        logger.info("SoulboundGearManager initialized (using framework capabilities)");
    }

    /**
     * Get singleton instance.
     *
     * @return Manager instance
     * @throws IllegalStateException If not initialized
     */
    public static SoulboundGearManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SoulboundGearManager not initialized");
        }
        return instance;
    }

    // ========================================================================
    // Note: Lifecycle methods no longer needed
    // ========================================================================
    // Framework capabilities system handles all persistence automatically:
    // - No batch write timers needed
    // - No dirty tracking needed
    // - Automatic saves every 5 seconds

    // ========================================================================
    // Public API - Binding
    // ========================================================================

    /**
     * Bind an item to a player, creating soulbound data.
     * Uses framework capabilities - data is automatically persisted.
     *
     * @param item Wurm Item object
     * @param ownerWurmId Player wurm ID
     * @return Created SoulboundItem
     * @throws IllegalStateException If item already bound
     */
    public synchronized SoulboundItem bindItem(com.wurmonline.server.items.Item item, long ownerWurmId) {
        long itemWurmId = item.getWurmId();

        // Check if already bound
        Optional<SoulboundItem> existing = getItem(itemWurmId);
        if (existing.isPresent() && existing.get().getOwnerWurmId() != -1) {
            throw new IllegalStateException("Item " + itemWurmId + " is already soulbound");
        }

        // Get item capability and bind it
        try {
            // Cast to ICapabilityProvider (injected by framework at runtime)
            com.garward.wurmmodloader.api.capability.ICapabilityProvider provider =
                (com.garward.wurmmodloader.api.capability.ICapabilityProvider) (Object) item;

            // Get capability
            SoulboundItem soulboundItem = provider.getCapability(
                com.garward.wurmmodloader.mods.soulboundgear.capability.SoulboundItemCapability.INSTANCE
            );

            // Update owner (framework auto-saves modified capability)
            long now = System.currentTimeMillis();
            soulboundItem.setOwnerWurmId(ownerWurmId);
            soulboundItem.setLastUsedAt(now);

            // Cache it
            cache.put(itemWurmId, soulboundItem);

            logger.info("Bound item " + itemWurmId + " to player " + ownerWurmId);

            return soulboundItem;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to bind item", e);
            throw new RuntimeException("Failed to bind item", e);
        }
    }

    /**
     * Check if an item is soulbound.
     *
     * @param itemWurmId Item wurm ID
     * @return true if soulbound
     */
    public boolean isSoulbound(long itemWurmId) {
        Optional<SoulboundItem> item = getItem(itemWurmId);
        return item.isPresent() && item.get().getOwnerWurmId() != -1;
    }

    /**
     * Unbind an item (reset to unbound state).
     * Framework auto-saves the capability.
     *
     * @param item Wurm Item object
     */
    public synchronized void unbindItem(com.wurmonline.server.items.Item item) {
        long itemWurmId = item.getWurmId();

        try {
            // Cast to ICapabilityProvider
            com.garward.wurmmodloader.api.capability.ICapabilityProvider provider =
                (com.garward.wurmmodloader.api.capability.ICapabilityProvider) (Object) item;

            SoulboundItem soulboundItem = provider.getCapability(
                com.garward.wurmmodloader.mods.soulboundgear.capability.SoulboundItemCapability.INSTANCE
            );

            soulboundItem.setOwnerWurmId(-1); // Framework auto-saves

            cache.remove(itemWurmId);
            logger.info("Unbound item " + itemWurmId);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to unbind item", e);
        }
    }

    // ========================================================================
    // Public API - Data Access
    // ========================================================================

    /**
     * Get soulbound item data using framework capabilities.
     * Loads from capability if Item object is provided.
     *
     * @param item Wurm Item object
     * @return SoulboundItem, or empty if not bound
     */
    public Optional<SoulboundItem> getItem(com.wurmonline.server.items.Item item) {
        long itemWurmId = item.getWurmId();

        // Check cache first
        SoulboundItem cached = cache.get(itemWurmId);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Load from capability system
        try {
            com.garward.wurmmodloader.api.capability.ICapabilityProvider provider =
                (com.garward.wurmmodloader.api.capability.ICapabilityProvider) (Object) item;

            SoulboundItem soulboundItem = provider.getCapability(
                com.garward.wurmmodloader.mods.soulboundgear.capability.SoulboundItemCapability.INSTANCE
            );

            // Only cache/return if actually bound (owner != -1)
            if (soulboundItem.getOwnerWurmId() != -1) {
                cache.put(itemWurmId, soulboundItem);
                return Optional.of(soulboundItem);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load soulbound item", e);
        }

        return Optional.empty();
    }

    /**
     * Get soulbound item data from cache only (by ID).
     * This method cannot load from capabilities without the Item object.
     *
     * @param itemWurmId Item wurm ID
     * @return SoulboundItem from cache, or empty if not cached
     */
    public Optional<SoulboundItem> getItem(long itemWurmId) {
        SoulboundItem cached = cache.get(itemWurmId);
        return Optional.ofNullable(cached);
    }

    /**
     * Get all soulbound items for a player.
     *
     * <p><b>Note:</b> With capabilities, we can only return cached items.
     * SQL queries across all items are not available without database access.</p>
     *
     * @param ownerWurmId Player wurm ID
     * @return List of soulbound items from cache
     */
    public List<SoulboundItem> getPlayerItems(long ownerWurmId) {
        List<SoulboundItem> result = new ArrayList<>();
        for (SoulboundItem item : cache.values()) {
            if (item.getOwnerWurmId() == ownerWurmId) {
                result.add(item);
            }
        }
        return result;
    }

    // ========================================================================
    // Public API - XP and Progression
    // ========================================================================

    /**
     * Award XP to a soulbound item.
     * Framework automatically saves modified capability data.
     *
     * @param itemWurmId Item wurm ID
     * @param xp XP amount
     * @return true if item leveled up
     */
    public boolean awardXP(long itemWurmId, int xp) {
        Optional<SoulboundItem> itemOpt = getItem(itemWurmId);
        if (!itemOpt.isPresent()) {
            logger.warning("Attempted to award XP to non-soulbound item: " + itemWurmId);
            return false;
        }

        SoulboundItem item = itemOpt.get();
        boolean leveledUp = item.addXP(xp);
        // Framework automatically saves modified capability

        if (config.isLogXPCalculations()) {
            logger.fine(String.format("Awarded %d XP to item %d (level %d, %d/%d XP)",
                xp, itemWurmId, item.getLevel(), item.getCurrentXP(),
                config.getXPRequiredForLevel(item.getLevel() + 1)));
        }

        return leveledUp;
    }

    /**
     * Award XP based on creature kill.
     *
     * @param itemWurmId Item wurm ID
     * @param creatureCR Creature combat rating
     * @param isChampion true if champion creature
     * @param isUnique true if unique creature
     * @param isTitan true if titan creature (custom)
     * @return true if item leveled up
     */
    public boolean awardKillXP(long itemWurmId, float creatureCR, boolean isChampion,
                               boolean isUnique, boolean isTitan) {
        // Calculate XP based on creature type
        int baseXP = config.getBaseXPPerKill();
        float crBonus = creatureCR * config.getCrXPMultiplier();
        float multiplier = 1.0f;

        if (isTitan) {
            multiplier = config.getTitanXPMultiplier();
        } else if (isUnique) {
            multiplier = config.getUniqueXPMultiplier();
        } else if (isChampion) {
            multiplier = config.getChampionXPMultiplier();
        }

        int totalXP = (int) ((baseXP + crBonus) * multiplier);

        // Increment kill counter
        Optional<SoulboundItem> itemOpt = getItem(itemWurmId);
        if (itemOpt.isPresent()) {
            itemOpt.get().incrementKills();
        }

        return awardXP(itemWurmId, totalXP);
    }

    // ========================================================================
    // Note: Dirty tracking and batch writes no longer needed
    // ========================================================================
    // Framework capabilities system handles all persistence automatically:
    // - Lazy loading when first accessed
    // - Automatic dirty tracking when modified
    // - Batched writes every 5 seconds
    // - Thread-safe storage with ConcurrentHashMap

    // ========================================================================
    // Cache Management
    // ========================================================================

    /**
     * Evict item from cache.
     * Framework handles persistence, just removes from memory.
     *
     * @param itemWurmId Item wurm ID
     */
    public void evictFromCache(long itemWurmId) {
        // Framework auto-saves modified capabilities, just remove from cache
        cache.remove(itemWurmId);
    }

    /**
     * Get cache size.
     *
     * @return Number of items in cache
     */
    public int getCacheSize() {
        return cache.size();
    }
}
