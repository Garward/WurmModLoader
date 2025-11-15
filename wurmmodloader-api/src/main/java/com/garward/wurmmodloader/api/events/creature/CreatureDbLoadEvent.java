package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fired when a creature's status is being loaded from wurmcreatures.db.
 *
 * <p>This event allows mods to:
 * <ul>
 *   <li>Load custom column data from the CREATURES table</li>
 *   <li>Initialize mod-specific data when creatures are loaded from database</li>
 *   <li>Handle missing columns gracefully (e.g., new mod installation)</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onCreatureDbLoad(CreatureDbLoadEvent event) {
 *     try {
 *         // Load custom data (returns 0 if column doesn't exist)
 *         int powerBase = event.getInt("POWER_BASE", 0);
 *         long spawnTime = event.getLong("POWER_SPAWN_TIME", 0L);
 *
 *         // Initialize creature power data
 *         CreaturePowerData data = getOrCreatePowerData(event.getWurmId());
 *         data.basePower = powerBase;
 *         data.spawnTimestamp = spawnTime;
 *
 *     } catch (SQLException e) {
 *         // Column doesn't exist yet (new mod installation)
 *         logger.fine("Power columns not found, will be added on first save");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This event fires during server startup
 * and creature spawning, on the server's main thread.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 * @see CreatureDbSaveEvent
 */
public class CreatureDbLoadEvent extends Event {

    private final Creature creature;
    private final long wurmId;
    private final ResultSet resultSet;
    private final Map<String, Object> loadedData;

    /**
     * Create a new creature database load event.
     *
     * @param creature The creature being loaded
     * @param resultSet The SQL result set containing creature data
     */
    public CreatureDbLoadEvent(Creature creature, ResultSet resultSet) {
        this.creature = creature;
        this.wurmId = creature.getWurmId();
        this.resultSet = resultSet;
        this.loadedData = new HashMap<>();
    }

    /**
     * Get the creature being loaded.
     *
     * @return The creature instance
     */
    public Creature getCreature() {
        return creature;
    }

    /**
     * Get the creature's wurm ID.
     *
     * @return The wurm ID
     */
    public long getWurmId() {
        return wurmId;
    }

    /**
     * Get an integer value from a custom column.
     *
     * @param columnName Column name
     * @param defaultValue Default value if column doesn't exist or is NULL
     * @return Column value or default
     * @throws SQLException if database error occurs (not thrown for missing columns)
     */
    public int getInt(String columnName, int defaultValue) throws SQLException {
        try {
            int value = resultSet.getInt(columnName);
            if (resultSet.wasNull()) {
                return defaultValue;
            }
            loadedData.put(columnName, value);
            return value;
        } catch (SQLException e) {
            // Column doesn't exist - return default
            return defaultValue;
        }
    }

    /**
     * Get a long value from a custom column.
     *
     * @param columnName Column name
     * @param defaultValue Default value if column doesn't exist or is NULL
     * @return Column value or default
     * @throws SQLException if database error occurs (not thrown for missing columns)
     */
    public long getLong(String columnName, long defaultValue) throws SQLException {
        try {
            long value = resultSet.getLong(columnName);
            if (resultSet.wasNull()) {
                return defaultValue;
            }
            loadedData.put(columnName, value);
            return value;
        } catch (SQLException e) {
            // Column doesn't exist - return default
            return defaultValue;
        }
    }

    /**
     * Get a float value from a custom column.
     *
     * @param columnName Column name
     * @param defaultValue Default value if column doesn't exist or is NULL
     * @return Column value or default
     * @throws SQLException if database error occurs (not thrown for missing columns)
     */
    public float getFloat(String columnName, float defaultValue) throws SQLException {
        try {
            float value = resultSet.getFloat(columnName);
            if (resultSet.wasNull()) {
                return defaultValue;
            }
            loadedData.put(columnName, value);
            return value;
        } catch (SQLException e) {
            // Column doesn't exist - return default
            return defaultValue;
        }
    }

    /**
     * Get a double value from a custom column.
     *
     * @param columnName Column name
     * @param defaultValue Default value if column doesn't exist or is NULL
     * @return Column value or default
     * @throws SQLException if database error occurs (not thrown for missing columns)
     */
    public double getDouble(String columnName, double defaultValue) throws SQLException {
        try {
            double value = resultSet.getDouble(columnName);
            if (resultSet.wasNull()) {
                return defaultValue;
            }
            loadedData.put(columnName, value);
            return value;
        } catch (SQLException e) {
            // Column doesn't exist - return default
            return defaultValue;
        }
    }

    /**
     * Get a string value from a custom column.
     *
     * @param columnName Column name
     * @param defaultValue Default value if column doesn't exist or is NULL
     * @return Column value or default
     * @throws SQLException if database error occurs (not thrown for missing columns)
     */
    public String getString(String columnName, String defaultValue) throws SQLException {
        try {
            String value = resultSet.getString(columnName);
            if (value == null) {
                return defaultValue;
            }
            loadedData.put(columnName, value);
            return value;
        } catch (SQLException e) {
            // Column doesn't exist - return default
            return defaultValue;
        }
    }

    /**
     * Get the map of loaded custom data.
     *
     * @return Map of column name to loaded value
     */
    public Map<String, Object> getLoadedData() {
        return loadedData;
    }

    /**
     * Check if any custom data was loaded.
     *
     * @return true if custom data was loaded
     */
    public boolean hasLoadedData() {
        return !loadedData.isEmpty();
    }
}
