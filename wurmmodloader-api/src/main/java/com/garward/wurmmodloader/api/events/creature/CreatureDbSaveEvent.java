package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fired when a creature's status is being saved to wurmcreatures.db.
 *
 * <p>This event allows mods to:
 * <ul>
 *   <li>Add custom columns to the CREATURES table (via ALTER TABLE on first save)</li>
 *   <li>Save mod-specific data alongside vanilla creature data</li>
 *   <li>Use the same batching system as Wurm's CreatureStatusBatcher</li>
 * </ul>
 *
 * <p><strong>Database Column Management:</strong></p>
 * <p>Mods should add columns using {@link #addCustomColumn(String, String)} which
 * automatically handles ALTER TABLE statements. Columns are added only once per server lifetime.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onCreatureDbSave(CreatureDbSaveEvent event) {
 *     // Add custom columns (only executes ALTER TABLE once)
 *     event.addCustomColumn("POWER_BASE", "INTEGER DEFAULT 0");
 *     event.addCustomColumn("POWER_SPAWN_TIME", "BIGINT DEFAULT 0");
 *
 *     // Save custom data
 *     int powerLevel = getPowerLevel(event.getCreature().getWurmId());
 *     event.setCustomData("POWER_BASE", powerLevel);
 *     event.setCustomData("POWER_SPAWN_TIME", System.currentTimeMillis());
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This event fires from CreatureStatusBatcher's
 * flush thread. Custom column additions are synchronized internally.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 * @see CreatureDbLoadEvent
 */
public class CreatureDbSaveEvent extends Event {

    private final Creature creature;
    private final long wurmId;
    private final Map<String, Object> customData;
    private final Map<String, String> customColumns;

    /**
     * Create a new creature database save event.
     *
     * @param creature The creature being saved
     */
    public CreatureDbSaveEvent(Creature creature) {
        this.creature = creature;
        this.wurmId = creature.getWurmId();
        this.customData = new HashMap<>();
        this.customColumns = new HashMap<>();
    }

    /**
     * Get the creature being saved.
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
     * Add a custom column to the CREATURES table.
     *
     * <p>This will execute ALTER TABLE on first save if the column doesn't exist.
     * Subsequent calls for the same column name are ignored (no-op).</p>
     *
     * <p><strong>Column Type Examples:</strong></p>
     * <ul>
     *   <li>INTEGER DEFAULT 0</li>
     *   <li>BIGINT DEFAULT 0</li>
     *   <li>FLOAT DEFAULT 0.0</li>
     *   <li>VARCHAR(100) DEFAULT ''</li>
     *   <li>TEXT DEFAULT ''</li>
     * </ul>
     *
     * @param columnName Column name (uppercase recommended, e.g., "POWER_BASE")
     * @param columnType SQLite column type with optional DEFAULT (e.g., "INTEGER DEFAULT 0")
     */
    public void addCustomColumn(String columnName, String columnType) {
        customColumns.put(columnName, columnType);
    }

    /**
     * Set custom data to be saved for this creature.
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
     * @param stmt Prepared statement for UPDATE CREATURES
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
                stmt.setNull(index++, java.sql.Types.NULL);
            } else {
                stmt.setString(index++, value.toString());
            }
        }
        return index;
    }
}
