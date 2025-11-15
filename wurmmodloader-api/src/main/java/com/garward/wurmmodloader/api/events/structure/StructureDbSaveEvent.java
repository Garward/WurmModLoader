package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Fired when a structure's data is being saved to wurmzones.db (STRUCTURES table).
 *
 * <p>This event allows mods to:
 * <ul>
 *   <li>Add custom columns to the STRUCTURES table</li>
 *   <li>Save mod-specific data alongside vanilla structure data</li>
 *   <li>Track custom structure properties (durability, upgrades, etc.)</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onStructureDbSave(StructureDbSaveEvent event) {
 *     // Register custom columns (only happens once per column per server lifetime)
 *     event.addCustomColumn("DURABILITY_MULT", "FLOAT DEFAULT 1.0");
 *     event.addCustomColumn("TIER_LEVEL", "INT DEFAULT 0");
 *     event.addCustomColumn("UPGRADE_FLAGS", "INT DEFAULT 0");
 *
 *     // Get structure modifiers from your mod's system
 *     StructureModifiers modifiers = getModifiers(event.getStructureId());
 *
 *     // Save custom data
 *     event.setCustomData("DURABILITY_MULT", modifiers.getDurabilityMultiplier());
 *     event.setCustomData("TIER_LEVEL", modifiers.getTierLevel());
 *     event.setCustomData("UPGRADE_FLAGS", modifiers.getUpgradeFlags());
 * }
 * }</pre>
 *
 * <p><strong>Database Schema:</strong> Custom columns are added to the STRUCTURES table
 * via ALTER TABLE statements. The framework ensures each column is only added once.</p>
 *
 * <p><strong>Thread Safety:</strong> This event fires during structure save operations,
 * which can happen on various server threads.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 * @see StructureDbLoadEvent
 */
public class StructureDbSaveEvent extends Event {

    private final long structureId;
    private final String structureName;
    private final Map<String, Object> customData;
    private final Map<String, String> customColumns;

    /**
     * Create a new structure database save event.
     *
     * @param structureId The structure's Wurm ID
     * @param structureName The structure's name
     */
    public StructureDbSaveEvent(long structureId, String structureName) {
        this.structureId = structureId;
        this.structureName = structureName;
        this.customData = new HashMap<>();
        this.customColumns = new HashMap<>();
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
     * Register a custom column to be added to the STRUCTURES table.
     *
     * <p>The column will be added via ALTER TABLE if it doesn't already exist.
     * This operation is performed once per column per server lifetime.</p>
     *
     * @param columnName Column name (e.g., "DURABILITY_MULT")
     * @param columnType SQLite column type (e.g., "FLOAT DEFAULT 1.0")
     */
    public void addCustomColumn(String columnName, String columnType) {
        customColumns.put(columnName, columnType);
    }

    /**
     * Set custom data to be saved for this structure.
     *
     * <p>The column must be registered via {@link #addCustomColumn(String, String)}
     * before data can be saved to it.</p>
     *
     * @param columnName Column name
     * @param value Value to save (Integer, Long, Float, Double, String, or null)
     */
    public void setCustomData(String columnName, Object value) {
        customData.put(columnName, value);
    }

    /**
     * Get the map of custom columns to add.
     *
     * @return Map of column name to column type
     */
    public Map<String, String> getCustomColumns() {
        return customColumns;
    }

    /**
     * Get the map of custom data to save.
     *
     * @return Map of column name to value
     */
    public Map<String, Object> getCustomData() {
        return customData;
    }

    /**
     * Check if any custom data has been registered.
     *
     * @return true if custom data exists
     */
    public boolean hasCustomData() {
        return !customData.isEmpty();
    }
}
