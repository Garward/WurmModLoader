package com.garward.wurmmodloader.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Utility for getting database connections without hardcoding world folder paths.
 *
 * <p><strong>CRITICAL:</strong> Never hardcode database paths! World folders can be:
 * <ul>
 *   <li>Adventure</li>
 *   <li>Riverweave</li>
 *   <li>Creative</li>
 *   <li>Any custom name</li>
 * </ul>
 *
 * <p>Always use Wurm's {@code DbConnector} class which resolves the active world
 * folder path dynamically based on server configuration.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // CORRECT - Uses DbConnector (world-agnostic)
 * Connection conn = DatabaseConnectionUtil.getCreatureDbConnection();
 *
 * // WRONG - Hardcoded path (breaks when world folder changes)
 * String path = "Adventure/sqlite/wurmcreatures.db"; // DON'T DO THIS!
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class DatabaseConnectionUtil {

    private static final Logger logger = Logger.getLogger(DatabaseConnectionUtil.class.getName());

    /**
     * Get connection to wurmcreatures.db (active world folder).
     *
     * <p>Uses {@code DbConnector.getCreatureDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getCreatureDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getCreatureDbCon");
    }

    /**
     * Get connection to wurmplayers.db (active world folder).
     *
     * <p>Uses {@code DbConnector.getPlayerDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getPlayerDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getPlayerDbCon");
    }

    /**
     * Get connection to wurmzones.db (active world folder).
     *
     * <p>Uses {@code DbConnector.getZonesDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getZonesDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getZonesDbCon");
    }

    /**
     * Get connection to wurmdeities.db (active world folder).
     *
     * <p>Uses {@code DbConnector.getDeityDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getDeityDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getDeityDbCon");
    }

    /**
     * Get connection to wurmitems.db (active world folder).
     *
     * <p>Uses {@code DbConnector.getItemDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getItemDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getItemDbCon");
    }

    /**
     * Get connection to wurmeconomy.db (active world folder).
     *
     * <p>Uses {@code DbConnector.getEconomyDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getEconomyDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getEconomyDbCon");
    }

    /**
     * Get connection to wurmlogs.db (active world folder).
     *
     * <p>Uses {@code DbConnector.getLogsDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getLogsDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getLogsDbCon");
    }

    /**
     * Get connection to wurmlogin.db (contains SERVERS table).
     *
     * <p>Uses {@code DbConnector.getLoginDbCon()} via reflection.</p>
     *
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getLoginDbConnection() throws SQLException {
        return getConnectionViaDbConnector("getLoginDbCon");
    }

    /**
     * Get connection via DbConnector using reflection.
     *
     * <p>Uses reflection to avoid class loading issues during framework initialization.
     * DbConnector automatically resolves the active world folder path.</p>
     *
     * @param methodName DbConnector method name (e.g., "getCreatureDbCon")
     * @return Database connection
     * @throws SQLException if connection fails
     */
    private static Connection getConnectionViaDbConnector(String methodName) throws SQLException {
        try {
            Class<?> dbConnectorClass = Class.forName("com.wurmonline.server.DbConnector");
            java.lang.reflect.Method method = dbConnectorClass.getMethod(methodName);
            return (Connection) method.invoke(null);
        } catch (Exception e) {
            throw new SQLException("Failed to get database connection via DbConnector." + methodName + "()", e);
        }
    }

    /**
     * Check if database connections are available.
     *
     * <p>Tests if DbConnector is ready to provide connections.
     * Use this before attempting database operations during early server startup.</p>
     *
     * @return true if database is ready, false otherwise
     */
    public static boolean isDatabaseReady() {
        Connection testConn = null;
        java.sql.Statement stmt = null;
        java.sql.ResultSet rs = null;

        try {
            // Get a connection from the pool
            testConn = getLoginDbConnection();

            if (testConn == null) {
                logger.warning("[DatabaseConnectionUtil] Database not ready - connection is NULL");
                return false;
            }

            // Actually TEST the connection with a simple query
            // Just checking isClosed() doesn't work - the connection can report as open but be dead
            stmt = testConn.createStatement();
            rs = stmt.executeQuery("SELECT 1");

            boolean hasResult = rs.next();
            if (!hasResult) {
                logger.warning("[DatabaseConnectionUtil] Database not ready - test query returned no results");
                return false;
            }

            logger.info("[DatabaseConnectionUtil] Database is ready - test query succeeded");
            return true;

        } catch (SQLException e) {
            // Database not ready - connection pool might have stale/closed connections
            logger.warning("[DatabaseConnectionUtil] Database not ready - SQL error during test query: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Database not ready yet (expected during early startup)
            logger.warning("[DatabaseConnectionUtil] Database not ready - exception: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                logger.warning("[DatabaseConnectionUtil] Caused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            return false;
        } finally {
            // Clean up test resources (but NOT the connection - let the pool manage it)
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    /**
     * Close a database connection safely.
     *
     * @param conn Connection to close (can be null)
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.fine("Failed to close connection: " + e.getMessage());
            }
        }
    }
}
