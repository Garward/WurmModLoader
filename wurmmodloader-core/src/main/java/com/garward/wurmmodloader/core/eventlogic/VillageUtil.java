package com.garward.wurmmodloader.core.eventlogic;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Framework utility for village and location operations.
 *
 * <p>This utility provides safe, framework-level access to Wurm's village system
 * and tile/zone operations without requiring mods to import Wurm classes directly.</p>
 *
 * <p><strong>Thread Safety:</strong> Thread-safe. Village operations are synchronized
 * by Wurm's internal village management.</p>
 *
 * @since 1.0.0
 */
public final class VillageUtil {

    private static final Logger LOGGER = Logger.getLogger(VillageUtil.class.getName());

    private VillageUtil() {
        // Utility class
    }

    /**
     * Get creature's citizen village (the village they belong to).
     *
     * @param creature The creature
     * @return Citizen village, or null if not a citizen
     */
    public static Village getCitizenVillage(Creature creature) {
        if (creature == null) {
            return null;
        }
        try {
            return creature.getCitizenVillage();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get tile at specified coordinates.
     *
     * @param tileX X coordinate
     * @param tileY Y coordinate
     * @param onSurface true for surface, false for cave
     * @return Tile, or null if unavailable
     */
    public static VolaTile getTile(int tileX, int tileY, boolean onSurface) {
        try {
            return Zones.getTileOrNull(tileX, tileY, onSurface);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get tile at " + tileX + "," + tileY, e);
            return null;
        }
    }

    /**
     * Get village at a tile.
     *
     * @param tile The tile
     * @return Village at tile, or null if no village
     */
    public static Village getVillageAtTile(VolaTile tile) {
        if (tile == null) {
            return null;
        }
        try {
            return tile.getVillage();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if village covers a specific tile.
     *
     * @param village The village
     * @param tileX X coordinate
     * @param tileY Y coordinate
     * @return true if village covers the tile
     */
    public static boolean covers(Village village, int tileX, int tileY) {
        if (village == null) {
            return false;
        }
        try {
            return village.covers(tileX, tileY);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if village is enemy to creature.
     *
     * @param village The village
     * @param creature The creature
     * @return true if village is enemy to creature
     */
    public static boolean isEnemy(Village village, Creature creature) {
        if (village == null || creature == null) {
            return false;
        }
        try {
            return village.isEnemy(creature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get village ID.
     *
     * @param village The village
     * @return Village ID, or -1 if null
     */
    public static int getVillageId(Village village) {
        if (village == null) {
            return -1;
        }
        try {
            return village.getId();
        } catch (Exception e) {
            return -1;
        }
    }
}
