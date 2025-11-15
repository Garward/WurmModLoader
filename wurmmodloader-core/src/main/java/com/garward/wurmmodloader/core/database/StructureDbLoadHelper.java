package com.garward.wurmmodloader.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for DbStructurePatch to fire load events with database access.
 *
 * <p><strong>Why This Exists:</strong></p>
 * <p>Javassist has difficulty accessing local variables (like ResultSet rs) in try blocks
 * when using line-based or method-based insertion. Instead of fighting with Javassist's
 * scoping rules, we create this helper that re-queries the database to get a fresh ResultSet.</p>
 *
 * <p><strong>Performance Note:</strong></p>
 * <p>This does an extra database query, but structure loading is not a hot path
 * (structures are loaded once at startup and cached). The slight performance cost
 * is acceptable compared to the complexity of trying to capture the original ResultSet.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class StructureDbLoadHelper {

    private static final Logger LOGGER = Logger.getLogger(StructureDbLoadHelper.class.getName());

    private static final String GET_STRUCTURE = "SELECT * FROM STRUCTURES WHERE WURMID=?";

    /**
     * Fire StructureDbLoadEvent with a fresh database query.
     *
     * <p>Called from bytecode-patched DbStructure.load() method via insertAfter().</p>
     *
     * @param structureId The structure's wurm ID
     * @param structureName The structure's name
     */
    public static void fireLoadEvent(long structureId, String structureName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // Get a fresh database connection and query for this structure
            conn = getDatabaseConnection();
            ps = conn.prepareStatement(GET_STRUCTURE);
            ps.setLong(1, structureId);
            rs = ps.executeQuery();

            if (rs.next()) {
                // Fire the event with the ResultSet
                com.garward.wurmmodloader.modloader.server.ProxyServerHook.fireStructureDbLoadEvent(
                    structureId, structureName, rs);
            } else {
                LOGGER.warning("[StructureDbLoadHelper] Structure " + structureId +
                    " not found in database when firing load event");
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[StructureDbLoadHelper] Failed to fire StructureDbLoadEvent for structure " +
                structureId, e);
        } finally {
            // Clean up database resources
            closeQuietly(rs);
            closeQuietly(ps);
            returnConnection(conn);
        }
    }

    /**
     * Get database connection using reflection to avoid classloader issues.
     *
     * <p>We use DatabaseConnectionUtil which handles classloader isolation properly.</p>
     */
    private static Connection getDatabaseConnection() throws SQLException {
        // Structures are stored in wurmzones.db
        return DatabaseConnectionUtil.getZonesDbConnection();
    }

    /**
     * Return database connection using Wurm's connection pool.
     */
    private static void returnConnection(Connection conn) {
        if (conn != null) {
            try {
                // Use reflection to call DbConnector.returnConnection()
                Class<?> dbConnectorClass = Class.forName("com.wurmonline.server.DbConnector");
                java.lang.reflect.Method returnMethod = dbConnectorClass.getMethod("returnConnection", Connection.class);
                returnMethod.invoke(null, conn);
            } catch (Exception e) {
                // Fallback: just close the connection
                try {
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.FINE, "Failed to close connection", ex);
                }
            }
        }
    }

    /**
     * Close ResultSet quietly (no exceptions thrown).
     */
    private static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.log(Level.FINE, "Failed to close ResultSet", e);
            }
        }
    }

    /**
     * Close PreparedStatement quietly (no exceptions thrown).
     */
    private static void closeQuietly(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                LOGGER.log(Level.FINE, "Failed to close PreparedStatement", e);
            }
        }
    }
}
