package com.garward.wurmmodloader.examples.noopbackend;

import com.garward.wurmmodloader.api.database.DatabaseBackend;
import com.garward.wurmmodloader.api.database.Dialect;
import com.wurmonline.server.database.NoOpConnectionFactory;
import com.wurmonline.server.database.WurmDatabaseSchema;
import com.wurmonline.server.database.migrations.NoOpMigrationStrategy;

import java.util.logging.Logger;

/**
 * Reference {@link DatabaseBackend} implementation. Demonstrates the four
 * touch points any real backend needs:
 *
 * <ol>
 *   <li>{@link #getName()}  — identifier shown in logs.</li>
 *   <li>{@link #getDialect()} — picks the SQL rewrite profile applied to
 *       vanilla WU SQL. {@link Dialect#SQLITE} here because the NoOp uses the
 *       bundled SQLite driver. A Postgres backend would return
 *       {@link Dialect#POSTGRES}.</li>
 *   <li>{@link #createConnectionFactory(Object)} — one factory per schema.
 *       Must return a subclass of
 *       {@code com.wurmonline.server.database.ConnectionFactory}; because that
 *       constructor is package-private, the subclass must live in the
 *       {@code com.wurmonline.server.database} package too.</li>
 *   <li>{@link #createMigrationStrategy()} — returns a
 *       {@code MigrationStrategy}. Because {@code MigrationResult}'s factories
 *       are package-private, the strategy lives in
 *       {@code com.wurmonline.server.database.migrations}.</li>
 * </ol>
 */
public final class NoOpDatabaseBackend implements DatabaseBackend {

    private static final Logger logger = Logger.getLogger(NoOpDatabaseBackend.class.getName());

    @Override
    public String getName() {
        return "noop";
    }

    @Override
    public Dialect getDialect() {
        return Dialect.SQLITE;
    }

    @Override
    public Object createConnectionFactory(Object schema) {
        WurmDatabaseSchema s = (WurmDatabaseSchema) schema;
        logger.info("[NoOpBackend] createConnectionFactory(schema=" + s + ")");
        return new NoOpConnectionFactory(s);
    }

    @Override
    public Object createMigrationStrategy() {
        logger.info("[NoOpBackend] createMigrationStrategy()");
        return new NoOpMigrationStrategy();
    }
}
