package com.garward.wurmmodloader.modsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import com.wurmonline.server.Constants;
import com.wurmonline.server.DbConnector;

/**
 * Database utility class for managing mod support database connections and operations.
 *
 * <p>This class provides methods for connecting to a SQLite database specifically intended
 * for mod support functionality. It handles connection setup, table existence checking,
 * and ensures proper SQLite driver initialization.</p>
 *
 * <p><strong>Usage example:</strong></p>
 * <pre><code>
 * try (Connection conn = ModSupportDb.getModSupportDb()) {
 *     if (ModSupportDb.hasTable(conn, "custom_data")) {
 *         // Table exists, perform operations
 *     }
 * } catch (SQLException e) {
 *     logger.log(Level.SEVERE, "Database operation failed", e);
 * }
 * </code></pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe as all methods are
 * stateless and operate on provided parameters or static configuration.</p>
 *
 * @since 1.0.0
 */
public class ModSupportDb {

	private static final String LITE_DB_DRIVER = "org.sqlite.JDBC";

	/**
	 * Constructs a database connection string for SQLite databases.
	 *
	 * <p>This method creates a properly formatted JDBC connection string for SQLite
	 * databases, ensuring the database name is converted to lowercase as required
	 * by the implementation.</p>
	 *
	 * <p><strong>Example usage:</strong></p>
	 * <pre><code>
	 * String connectionString = ModSupportDb.getDbConnectionString("/home/wurm/server", "modsupport");
	 * // Returns: "jdbc:sqlite:/home/wurm/server/sqlite/modsupport.db"
	 * </code></pre>
	 *
	 * @param host the base directory path where the database files are stored
	 * @param db the name of the database (will be converted to lowercase)
	 * @return a properly formatted JDBC connection string for SQLite
	 * @throws RuntimeException if SQLite is not enabled in the server configuration
	 * @see #checkSqlite()
	 * @since 1.0.0
	 */
	public static String getDbConnectionString(String host, String db) {
		checkSqlite();

		return "jdbc:sqlite:" + host + "/sqlite/" + db.toLowerCase(Locale.ENGLISH) + ".db";
	}

	/**
	 * Establishes and returns a connection to the mod support SQLite database.
	 *
	 * <p>This method initializes the SQLite JDBC driver and creates a connection
	 * to the mod support database located at the configured database host.</p>
	 *
	 * <p><strong>Lifecycle note:</strong> The caller is responsible for closing
	 * the returned connection to prevent resource leaks.</p>
	 *
	 * <p><strong>Example usage:</strong></p>
	 * <pre><code>
	 * try (Connection conn = ModSupportDb.getModSupportDb()) {
	 *     // Perform database operations
	 * } catch (RuntimeException e) {
	 *     // Handle connection failure
	 * }
	 * </code></pre>
	 *
	 * @return an active database connection to the mod support database
	 * @throws RuntimeException if the connection cannot be established or if
	 *                          SQLite is not enabled in the server configuration
	 * @see #getDbConnectionString(String, String)
	 * @see #checkSqlite()
	 * @since 1.0.0
	 */
	public static Connection getModSupportDb() {
		checkSqlite();

		String dbConnection = getDbConnectionString(Constants.dbHost, "modsupport");

		try {
			Class.forName(LITE_DB_DRIVER);
			return DriverManager.getConnection(dbConnection);
		} catch (SQLException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to initialize modsupport db connection", e);
		}
	}

	/**
	 * Validates that SQLite is enabled in the current server configuration.
	 *
	 * <p>This internal method ensures that the server is configured to use SQLite
	 * databases, which is the only supported database type for mod support operations.</p>
	 *
	 * @throws RuntimeException if SQLite is not enabled in the server configuration
	 * @see DbConnector#isUseSqlite()
	 * @since 1.0.0
	 */
	private static void checkSqlite() {
		if (!DbConnector.isUseSqlite()) {
			throw new RuntimeException("Only Sqlite is currently supported");
		}
	}

	/**
	 * Checks whether a specific table exists in the provided database connection.
	 *
	 * <p>This method queries the SQLite master table to determine if a table with
	 * the specified name exists in the connected database.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method is safe to call from
	 * multiple threads when using different Connection objects. Using the same
	 * Connection object across threads is not recommended.</p>
	 *
	 * <p><strong>Example usage:</strong></p>
	 * <pre><code>
	 * try (Connection conn = ModSupportDb.getModSupportDb()) {
	 *     boolean exists = ModSupportDb.hasTable(conn, "player_extensions");
	 *     if (exists) {
	 *         // Table exists, proceed with operations
	 *     }
	 * } catch (SQLException e) {
	 *     // Handle database error
	 * }
	 * </code></pre>
	 *
	 * @param dbcon the database connection to query
	 * @param tableName the name of the table to check for existence
	 * @return {@code true} if the table exists, {@code false} otherwise
	 * @throws SQLException if a database access error occurs or the SQL statement fails
	 * @since 1.0.0
	 */
	public static boolean hasTable(Connection dbcon, String tableName) throws SQLException {
		try (PreparedStatement statement = dbcon.prepareStatement("SELECT name FROM sqlite_master WHERE type='table' AND name=?")) {
			statement.setString(1, tableName);
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					return true;
				}
			}
		}
		return false;
	}

}