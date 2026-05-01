package com.wurmonline.server.database;

import com.garward.wurmmodloader.mods.postgresbackend.PostgresConfig;
import com.garward.wurmmodloader.mods.postgresbackend.RewritingConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * {@link ConnectionFactory} subclass for the single-database / multi-schema
 * Postgres topology. Lives in {@code com.wurmonline.server.database} because
 * the parent constructor is package-private.
 *
 * <p>Every connection issues {@code SET search_path TO <schema>} immediately
 * after connect so all subsequent unqualified SQL resolves against the right
 * schema. WU caches one connection per schema internally, so the cost is one
 * statement per cache miss — no pool needed.</p>
 */
public final class PostgresConnectionFactory extends ConnectionFactory {

    private static final Logger logger = Logger.getLogger(PostgresConnectionFactory.class.getName());

    private final PostgresConfig config;
    private final WurmDatabaseSchema schema;
    private final String searchPath;

    public PostgresConnectionFactory(WurmDatabaseSchema schema, PostgresConfig config) {
        super(config.baseUrl(), schema);
        this.schema = schema;
        this.config = config;
        this.searchPath = schema.getDatabase().toLowerCase();
    }

    public String getUser()     { return config.user; }
    public String getPassword() { return config.password; }
    public String getSchemaName() { return searchPath; }

    @Override
    public Connection createConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", config.user);
        props.setProperty("password", config.password);
        Connection raw = DriverManager.getConnection(getUrl(), props);
        try (Statement st = raw.createStatement()) {
            st.execute("SET search_path TO " + searchPath);
        }
        logger.fine("[PostgresBackend] connection opened (schema=" + schema + ", search_path=" + searchPath + ")");
        // Wrap so vanilla SQL strings (e.g. duplicate-column UPDATE SET clauses)
        // are rewritten to be Postgres-legal before they reach the driver.
        return RewritingConnection.wrap(raw);
    }

    @Override
    public boolean isValid(Connection conn) throws SQLException {
        return conn != null && !conn.isClosed() && conn.isValid(1);
    }

    @Override
    public boolean isStale(long lastUsed, Connection conn) throws SQLException {
        return System.currentTimeMillis() - lastUsed > 3600000L
            || conn != null && !conn.isValid(0);
    }
}
