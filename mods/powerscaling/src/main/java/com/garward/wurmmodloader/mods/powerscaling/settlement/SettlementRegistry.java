package com.garward.wurmmodloader.mods.powerscaling.settlement;

import com.garward.wurmmodloader.mods.powerscaling.PowerScalingConfig;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for tracking settlements (villages/deeds) for power suppression.
 *
 * <p>Maintains a spatial index of all settlements and provides fast distance queries.
 * Settlements reduce creature power in nearby areas, creating "safe zones" for players.</p>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can be called from
 * multiple threads (e.g., creature spawn events).</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // Initialize during server startup
 * SettlementRegistry.getInstance().initialize(config);
 *
 * // Query distance
 * int distance = SettlementRegistry.getInstance().getDistanceToNearest(tileX, tileY);
 * }</pre>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class SettlementRegistry {

    private static final Logger logger = Logger.getLogger(SettlementRegistry.class.getName());
    private static final SettlementRegistry INSTANCE = new SettlementRegistry();

    private final SettlementIndexer indexer = new SettlementIndexer();
    private final Map<Integer, SettlementZone> settlementsById = new ConcurrentHashMap<>();

    private PowerScalingConfig config;
    private boolean initialized = false;

    private SettlementRegistry() {
        // Private constructor - singleton
    }

    public static SettlementRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize the registry and scan existing villages.
     *
     * @param config Power scaling configuration
     */
    public void initialize(PowerScalingConfig config) {
        if (initialized) {
            logger.warning("SettlementRegistry already initialized");
            return;
        }

        this.config = config;
        this.initialized = true;

        // Scan existing villages from Wurm
        scanExistingVillages();

        logger.info("SettlementRegistry initialized: " + indexer.getDebugStats());
    }

    /**
     * Scan and index all existing villages from Wurm.
     */
    private void scanExistingVillages() {
        try {
            // Get all villages from Wurm's Villages class
            Village[] villages = Villages.getVillages();

            if (villages == null || villages.length == 0) {
                logger.info("No villages found to index");
                return;
            }

            int indexed = 0;
            for (Village village : villages) {
                if (village != null) {
                    addVillage(village);
                    indexed++;
                }
            }

            logger.info(String.format("Indexed %d existing villages", indexed));

        } catch (Exception e) {
            logger.warning("Failed to scan existing villages: " + e.getMessage());
        }
    }

    /**
     * Add a village to the registry.
     *
     * @param village The village to add
     */
    public void addVillage(Village village) {
        if (!initialized) {
            logger.warning("SettlementRegistry not initialized yet");
            return;
        }

        // Calculate center point (use token location or approximate center)
        int centerX = village.getTokenX();
        int centerY = village.getTokenY();

        // Use village perimeter size as radius (approximate)
        // Default to safe radius from config if not available
        int radius = config.getVillageSafeRadius();

        SettlementZone zone = new SettlementZone(
            village.getId(),
            village.getName(),
            centerX,
            centerY,
            radius,
            SettlementZone.SettlementType.VILLAGE
        );

        // Add to index
        settlementsById.put(village.getId(), zone);
        indexer.add(zone);

        if (config.isLogPowerAssignments()) {
            logger.info(String.format("Added village to registry: %s at (%d,%d) radius=%d",
                village.getName(), centerX, centerY, radius));
        }
    }

    /**
     * Remove a village from the registry.
     *
     * @param villageId The village ID to remove
     */
    public void removeVillage(int villageId) {
        if (!initialized) {
            return;
        }

        SettlementZone zone = settlementsById.remove(villageId);
        if (zone != null) {
            indexer.remove(zone);

            if (config.isLogPowerAssignments()) {
                logger.info(String.format("Removed village from registry: %s", zone.getName()));
            }
        }
    }

    /**
     * Add a custom settlement zone (for admins/testing).
     *
     * @param zone The custom zone to add
     */
    public void addCustomZone(SettlementZone zone) {
        if (!initialized) {
            logger.warning("SettlementRegistry not initialized yet");
            return;
        }

        settlementsById.put(zone.getId(), zone);
        indexer.add(zone);

        logger.info(String.format("Added custom zone: %s", zone));
    }

    /**
     * Get the distance to the nearest settlement from a point.
     *
     * <p>This is the main method called by PowerCalculator during power calculation.</p>
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @return Distance to nearest settlement in tiles, or Integer.MAX_VALUE if none found
     */
    public int getDistanceToNearest(int tileX, int tileY) {
        if (!initialized) {
            return Integer.MAX_VALUE;
        }

        return indexer.getDistanceToNearest(tileX, tileY);
    }

    /**
     * Get the nearest settlement to a point.
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @return The nearest settlement, or null if none found
     */
    public SettlementZone getNearest(int tileX, int tileY) {
        if (!initialized) {
            return null;
        }

        return indexer.getNearest(tileX, tileY);
    }

    /**
     * Get all settlements within a distance.
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @param maxDistance Maximum distance in tiles
     * @return List of settlements within range
     */
    public List<SettlementZone> getWithinDistance(int tileX, int tileY, int maxDistance) {
        if (!initialized) {
            return new ArrayList<>();
        }

        return indexer.getWithinDistance(tileX, tileY, maxDistance);
    }

    /**
     * Get a settlement by ID.
     *
     * @param id The settlement ID
     * @return The settlement, or null if not found
     */
    public SettlementZone getById(int id) {
        return settlementsById.get(id);
    }

    /**
     * Get all registered settlements.
     *
     * @return List of all settlements
     */
    public List<SettlementZone> getAllSettlements() {
        return new ArrayList<>(settlementsById.values());
    }

    /**
     * Check if a point is within any settlement's safe zone.
     *
     * @param tileX Target X coordinate
     * @param tileY Target Y coordinate
     * @return true if within a settlement's safe radius
     */
    public boolean isWithinSafeZone(int tileX, int tileY) {
        if (!initialized) {
            return false;
        }

        int distance = indexer.getDistanceToNearest(tileX, tileY);
        return distance <= config.getVillageSafeRadius();
    }

    /**
     * Get total number of registered settlements.
     *
     * @return Settlement count
     */
    public int getSettlementCount() {
        return settlementsById.size();
    }

    /**
     * Check if the registry has been initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get debug statistics.
     *
     * @return Debug string
     */
    public String getDebugStats() {
        if (!initialized) {
            return "SettlementRegistry[not initialized]";
        }

        return String.format(
            "SettlementRegistry[settlements=%d, %s]",
            settlementsById.size(),
            indexer.getDebugStats()
        );
    }

    /**
     * Clear all settlements (for testing/reload).
     */
    public void clear() {
        settlementsById.clear();
        indexer.clear();
        logger.info("SettlementRegistry cleared");
    }

    /**
     * Reload settlements from Wurm (rescan).
     */
    public void reload() {
        clear();
        scanExistingVillages();
        logger.info("SettlementRegistry reloaded: " + getDebugStats());
    }
}
