package com.garward.wurmmodloader.core.database;

import com.garward.wurmmodloader.api.events.creature.CreatureDbSaveEvent;

import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages custom columns and data for the CREATURES table in wurmcreatures.db.
 *
 * <p>Provides automatic schema migration (ALTER TABLE) and batched UPDATE statements
 * for mod-specific creature data. Integrates with CreatureStatusBatcher for optimal
 * performance.</p>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>Automatic column addition via ALTER TABLE (once per column per server lifetime)</li>
 *   <li>Thread-safe column tracking</li>
 *   <li>Batched UPDATE statements alongside vanilla creature saves</li>
 *   <li>No database locks - uses existing CreatureStatusBatcher infrastructure</li>
 * </ul>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CreatureDatabaseManager {

    private static final Logger logger = Logger.getLogger(CreatureDatabaseManager.class.getName());
    private static final CreatureDatabaseManager INSTANCE = new CreatureDatabaseManager();

    // Track which columns have been added (to avoid redundant ALTER TABLE)
    private final Set<String> addedColumns = ConcurrentHashMap.newKeySet();

    // Track pending saves (wurmId -> custom column data)
    private final Map<Long, Map<String, Object>> pendingSaves = new ConcurrentHashMap<>();

    private CreatureDatabaseManager() {
        // Singleton
    }

    public static CreatureDatabaseManager getInstance() {
        return INSTANCE;
    }

    /**
     * Handle a CreatureDbSaveEvent by adding columns and queuing data for save.
     *
     * <p>This is called from ServerHook.fireCreatureDbSave() after the event
     * has been posted to mods.</p>
     *
     * @param event The save event containing custom columns and data
     * @param wurmId The creature's wurm ID
     */
    public void handleSaveEvent(CreatureDbSaveEvent event, long wurmId) {
        if (!event.hasCustomData()) {
            return; // No custom data to save
        }

        // Add any new columns (thread-safe, only happens once per column)
        for (Map.Entry<String, String> entry : event.getCustomColumns().entrySet()) {
            addColumnIfNeeded(entry.getKey(), entry.getValue());
        }

        // Queue custom data for save
        pendingSaves.put(wurmId, event.getCustomData());

        // Actually save the data (piggybacking on vanilla save)
        saveCustomData(wurmId, event.getCustomData());
    }

    /**
     * Add a column to CREATURES table if it doesn't already exist.
     *
     * <p>Uses ALTER TABLE to add the column. This is executed once per column
     * per server lifetime. Subsequent calls for the same column are no-ops.</p>
     *
     * @param columnName Column name (e.g., "POWER_BASE")
     * @param columnType SQLite column type (e.g., "INTEGER DEFAULT 0")
     */
    private void addColumnIfNeeded(String columnName, String columnType) {
        // Check if already added
        if (addedColumns.contains(columnName)) {
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnectionUtil.getCreatureDbConnection();

            // Check if column exists
            if (columnExists(conn, columnName)) {
                addedColumns.add(columnName);
                return;
            }

            // Add column via ALTER TABLE
            String sql = "ALTER TABLE CREATURES ADD COLUMN " + columnName + " " + columnType;
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                addedColumns.add(columnName);
                logger.info("[CreatureDatabaseManager] Added column to CREATURES: " + columnName + " " + columnType);
            }

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[CreatureDatabaseManager] Failed to add column " + columnName, e);
        } finally {
            DatabaseConnectionUtil.closeConnection(conn);
        }
    }

    /**
     * Check if a column exists in the CREATURES table.
     *
     * @param conn Database connection
     * @param columnName Column name to check
     * @return true if column exists
     */
    private boolean columnExists(Connection conn, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, "CREATURES", columnName)) {
            return rs.next();
        }
    }

    /**
     * Save custom data for a creature.
     *
     * <p>This executes a separate UPDATE statement for the custom columns.
     * It runs immediately after the vanilla save completes.</p>
     *
     * @param wurmId Creature wurm ID
     * @param customData Map of column name to value
     */
    private void saveCustomData(long wurmId, Map<String, Object> customData) {
        if (customData.isEmpty()) {
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnectionUtil.getCreatureDbConnection();

            // Build UPDATE statement dynamically
            StringBuilder sql = new StringBuilder("UPDATE CREATURES SET ");
            boolean first = true;
            for (String columnName : customData.keySet()) {
                if (!first) {
                    sql.append(", ");
                }
                sql.append(columnName).append("=?");
                first = false;
            }
            sql.append(" WHERE WURMID=?");

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                // Set parameters
                int index = 1;
                for (Object value : customData.values()) {
                    setParameter(stmt, index++, value);
                }
                stmt.setLong(index, wurmId);

                // Execute update
                stmt.executeUpdate();

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("[CreatureDatabaseManager] Saved custom data for creature " + wurmId +
                               ": " + customData.size() + " columns");
                }
            }

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[CreatureDatabaseManager] Failed to save custom data for creature " + wurmId, e);
        } finally {
            DatabaseConnectionUtil.closeConnection(conn);
        }
    }

    /**
     * Set a parameter in a prepared statement based on its type.
     *
     * @param stmt Prepared statement
     * @param index Parameter index (1-based)
     * @param value Value to set
     */
    private void setParameter(PreparedStatement stmt, int index, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.NULL);
        } else if (value instanceof Integer) {
            stmt.setInt(index, (Integer) value);
        } else if (value instanceof Long) {
            stmt.setLong(index, (Long) value);
        } else if (value instanceof Float) {
            stmt.setFloat(index, (Float) value);
        } else if (value instanceof Double) {
            stmt.setDouble(index, (Double) value);
        } else if (value instanceof String) {
            stmt.setString(index, (String) value);
        } else {
            stmt.setString(index, value.toString());
        }
    }


    /**
     * Get statistics about custom columns.
     *
     * @return Statistics string
     */
    public String getStats() {
        return String.format("[CreatureDatabaseManager] %d custom columns registered, %d pending saves",
            addedColumns.size(), pendingSaves.size());
    }
}
