package com.wurmonline.server.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Reference {@link ConnectionFactory} subclass for the NoOp backend example.
 *
 * <p>Lives in {@code com.wurmonline.server.database} because the
 * {@code ConnectionFactory(String, WurmDatabaseSchema)} constructor is
 * package-private. Any backend that wants to build a real {@code ConnectionFactory}
 * subclass without reflection must place it in this package — a small price
 * to pay for the framework-owned pool / health lifecycle.
 *
 * <p>This demo writes one SQLite file per schema into the JVM temp dir. A real
 * Postgres backend would build a {@code jdbc:postgresql://host:5432/wurm<schema>}
 * URL here and delegate to HikariCP / the Postgres driver.
 */
public final class NoOpConnectionFactory extends ConnectionFactory {

    private static final Logger logger = Logger.getLogger(NoOpConnectionFactory.class.getName());

    private final WurmDatabaseSchema schema;

    public NoOpConnectionFactory(WurmDatabaseSchema schema) {
        super(buildUrl(schema), schema);
        this.schema = schema;
    }

    private static String buildUrl(WurmDatabaseSchema schema) {
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "wurmmodloader-noop");
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
            // Driver will surface the real error on first connect.
        }
        return "jdbc:sqlite:" + dir.resolve("noop-" + schema.name().toLowerCase() + ".db");
    }

    @Override
    public Connection createConnection() throws SQLException {
        logger.fine("[NoOpBackend] createConnection(schema=" + schema + ", url=" + getUrl() + ")");
        return DriverManager.getConnection(getUrl());
    }

    @Override
    public boolean isValid(Connection conn) throws SQLException {
        return conn != null && !conn.isClosed() && conn.isValid(1);
    }

    @Override
    public boolean isStale(long ageMillis, Connection conn) throws SQLException {
        return false;
    }
}
