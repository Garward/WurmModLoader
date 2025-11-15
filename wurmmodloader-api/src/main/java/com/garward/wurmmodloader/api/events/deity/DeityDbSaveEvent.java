package com.garward.wurmmodloader.api.events.deity;

import com.garward.wurmmodloader.api.events.base.Event;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Fired when a deity's data is being saved to wurmdeities.db (DEITIES table).
 *
 * <p>This event allows mods to:
 * <ul>
 *   <li>Add custom columns to the DEITIES table (via ALTER TABLE on first save)</li>
 *   <li>Save mod-specific deity data alongside vanilla deity data</li>
 *   <li>Modify deity stats, favor, power, etc.</li>
 * </ul>
 *
 * <p><strong>Database Column Management:</strong></p>
 * <p>Mods should add columns using {@link #addCustomColumn(String, String)} which
 * automatically handles ALTER TABLE statements. Columns are added only once per server lifetime.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onDeityDbSave(DeityDbSaveEvent event) {
 *     // Add custom columns (only executes ALTER TABLE once)
 *     event.addCustomColumn("SPELL_POWER_MULT", "FLOAT DEFAULT 1.0");
 *     event.addCustomColumn("FAVOR_REGEN_RATE", "FLOAT DEFAULT 1.0");
 *     event.addCustomColumn("MAX_FOLLOWERS", "INTEGER DEFAULT -1");
 *
 *     // Save custom data
 *     float spellPower = getDeitySpellPower(event.getDeityId());
 *     event.setCustomData("SPELL_POWER_MULT", spellPower);
 *     event.setCustomData("FAVOR_REGEN_RATE", 1.5f);
 *     event.setCustomData("MAX_FOLLOWERS", 1000);
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This event fires when deities are saved to database,
 * typically during server startup/shutdown or when deity stats are modified.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 * @see DeityDbLoadEvent
 */
public class DeityDbSaveEvent extends Event {

    private final int deityId;
    private final String deityName;
    private final Map<String, Object> customData;
    private final Map<String, String> customColumns;

    /**
     * Create a new deity database save event.
     *
     * @param deityId The deity's ID (from DEITIES table)
     * @param deityName The deity's name
     */
    public DeityDbSaveEvent(int deityId, String deityName) {
        this.deityId = deityId;
        this.deityName = deityName;
        this.customData = new HashMap<>();
        this.customColumns = new HashMap<>();
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
     * Add a custom column to the DEITIES table.
     *
     * <p>This will execute ALTER TABLE on first save if the column doesn't exist.
     * Subsequent calls for the same column name are ignored (no-op).</p>
     *
     * <p><strong>Column Type Examples:</strong></p>
     * <ul>
     *   <li>INTEGER DEFAULT 0</li>
     *   <li>FLOAT DEFAULT 1.0</li>
     *   <li>VARCHAR(100) DEFAULT ''</li>
     *   <li>TEXT DEFAULT ''</li>
     * </ul>
     *
     * @param columnName Column name (uppercase recommended, e.g., "SPELL_POWER_MULT")
     * @param columnType SQLite column type with optional DEFAULT (e.g., "FLOAT DEFAULT 1.0")
     */
    public void addCustomColumn(String columnName, String columnType) {
        customColumns.put(columnName, columnType);
    }

    /**
     * Set custom data to be saved for this deity.
     *
     * <p>The column must have been registered via {@link #addCustomColumn(String, String)}
     * before data can be saved to it.</p>
     *
     * @param columnName Column name (must match name from addCustomColumn)
     * @param value Value to save (Integer, Long, Float, String, etc.)
     */
    public void setCustomData(String columnName, Object value) {
        customData.put(columnName, value);
    }

    /**
     * Get the map of custom columns to add (column name -> column type).
     *
     * @return Map of custom columns
     */
    public Map<String, String> getCustomColumns() {
        return customColumns;
    }

    /**
     * Get the map of custom data to save (column name -> value).
     *
     * @return Map of custom data
     */
    public Map<String, Object> getCustomData() {
        return customData;
    }

    /**
     * Check if any custom data has been registered for saving.
     *
     * @return true if custom data exists
     */
    public boolean hasCustomData() {
        return !customData.isEmpty();
    }

    /**
     * Apply custom data to a prepared statement.
     *
     * <p>This is called by the framework after the vanilla save() completes.
     * Mods should not call this directly.</p>
     *
     * @param stmt Prepared statement for UPDATE DEITIES
     * @param parameterIndex Starting parameter index (1-based)
     * @return Next parameter index after all custom columns
     * @throws SQLException if database error occurs
     */
    public int applyToStatement(PreparedStatement stmt, int parameterIndex) throws SQLException {
        int index = parameterIndex;
        for (Map.Entry<String, Object> entry : customData.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Integer) {
                stmt.setInt(index++, (Integer) value);
            } else if (value instanceof Long) {
                stmt.setLong(index++, (Long) value);
            } else if (value instanceof Float) {
                stmt.setFloat(index++, (Float) value);
            } else if (value instanceof Double) {
                stmt.setDouble(index++, (Double) value);
            } else if (value instanceof String) {
                stmt.setString(index++, (String) value);
            } else if (value == null) {
                stmt.setNull(index++, Types.NULL);
            } else {
                stmt.setString(index++, value.toString());
            }
        }
        return index;
    }
}
