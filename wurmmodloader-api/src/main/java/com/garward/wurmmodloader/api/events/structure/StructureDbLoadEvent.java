package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fired when a structure's data is being loaded from wurmzones.db (STRUCTURES table).
 *
 * <p>This event allows mods to:
 * <ul>
 *   <li>Load custom column data from the STRUCTURES table</li>
 *   <li>Initialize mod-specific data when structures are loaded from database</li>
 *   <li>Handle missing columns gracefully (e.g., new mod installation)</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onStructureDbLoad(StructureDbLoadEvent event) {
 *     try {
 *         // Load custom data (returns default if column doesn't exist)
 *         float durabilityMult = event.getFloat("DURABILITY_MULT", 1.0f);
 *         int tierLevel = event.getInt("TIER_LEVEL", 0);
 *         int upgradeFlags = event.getInt("UPGRADE_FLAGS", 0);
 *
 *         // Apply custom structure modifiers
 *         StructureModifiers modifiers = getModifiers(event.getStructureId());
 *         modifiers.setDurabilityMultiplier(durabilityMult);
 *         modifiers.setTierLevel(tierLevel);
 *         modifiers.setUpgradeFlags(upgradeFlags);
 *
 *     } catch (SQLException e) {
 *         // Column doesn't exist yet (new mod installation)
 *         logger.fine("Custom structure columns not found, will be added on first save");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This event fires during structure load operations,
 * which typically happen during server startup or when structures are dynamically loaded.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 * @see StructureDbSaveEvent
 */
public class StructureDbLoadEvent extends Event {

    private final long structureId;
    private final String structureName;
    private final ResultSet resultSet;
    private final Map<String, Object> loadedData;

    /**
     * Create a new structure database load event.
     *
     * @param structureId The structure's Wurm ID
     * @param structureName The structure's name
     * @param resultSet The SQL result set containing structure data
     */
    public StructureDbLoadEvent(long structureId, String structureName, ResultSet resultSet) {
        this.structureId = structureId;
        this.structureName = structureName;
        this.resultSet = resultSet;
        this.loadedData = new HashMap<>();
    }

    /**
     * Get the structure's Wurm ID.
     *
     * @return The structure ID
     */
    public long getStructureId() {
        return structureId;
    }

    /**
     * Get the structure's name.
     *
     * @return The structure name
     */
    public String getStructureName() {
        return structureName;
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
