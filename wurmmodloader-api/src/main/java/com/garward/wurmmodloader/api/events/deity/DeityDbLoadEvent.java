package com.garward.wurmmodloader.api.events.deity;

import com.garward.wurmmodloader.api.events.base.Event;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fired when a deity's data is being loaded from wurmdeities.db (DEITIES table).
 *
 * <p>This event allows mods to:
 * <ul>
 *   <li>Load custom column data from the DEITIES table</li>
 *   <li>Initialize mod-specific data when deities are loaded from database</li>
 *   <li>Handle missing columns gracefully (e.g., new mod installation)</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onDeityDbLoad(DeityDbLoadEvent event) {
 *     try {
 *         // Load custom data (returns default if column doesn't exist)
 *         float spellPower = event.getFloat("SPELL_POWER_MULT", 1.0f);
 *         float favorRegen = event.getFloat("FAVOR_REGEN_RATE", 1.0f);
 *         int maxFollowers = event.getInt("MAX_FOLLOWERS", -1);
 *
 *         // Apply custom deity modifiers
 *         DeityModifiers modifiers = getModifiers(event.getDeityId());
 *         modifiers.setSpellPowerMultiplier(spellPower);
 *         modifiers.setFavorRegenRate(favorRegen);
 *         modifiers.setMaxFollowers(maxFollowers);
 *
 *     } catch (SQLException e) {
 *         // Column doesn't exist yet (new mod installation)
 *         logger.fine("Custom deity columns not found, will be added on first save");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This event fires during server startup
 * when deities are loaded from the database.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 * @see DeityDbSaveEvent
 */
public class DeityDbLoadEvent extends Event {

    private final int deityId;
    private final String deityName;
    private final ResultSet resultSet;
    private final Map<String, Object> loadedData;

    /**
     * Create a new deity database load event.
     *
     * @param deityId The deity's ID
     * @param deityName The deity's name
     * @param resultSet The SQL result set containing deity data
     */
    public DeityDbLoadEvent(int deityId, String deityName, ResultSet resultSet) {
        this.deityId = deityId;
        this.deityName = deityName;
        this.resultSet = resultSet;
        this.loadedData = new HashMap<>();
    }

    /**
     * Get the deity's ID.
     *
     * @return The deity ID
     */
    public int getDeityId() {
        return deityId;
    }

    /**
     * Get the deity's name.
     *
     * @return The deity name
     */
    public String getDeityName() {
        return deityName;
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
