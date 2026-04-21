package com.garward.wurmmodloader.mods.postgresbackend;

import com.garward.wurmmodloader.api.database.DatabaseBackend;
import com.garward.wurmmodloader.api.database.Dialect;
import com.wurmonline.server.database.PostgresConnectionFactory;
import com.wurmonline.server.database.WurmDatabaseSchema;
import com.wurmonline.server.database.migrations.PostgresMigrationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class PostgresDatabaseBackend implements DatabaseBackend {

    private static final Logger logger = Logger.getLogger(PostgresDatabaseBackend.class.getName());

    private final List<PostgresConnectionFactory> factories = new ArrayList<>();

    @Override public String getName() { return "postgres"; }

    @Override public Dialect getDialect() { return Dialect.POSTGRES; }

    @Override
    public Object createConnectionFactory(Object schema) {
        WurmDatabaseSchema s = (WurmDatabaseSchema) schema;
        PostgresConnectionFactory f = new PostgresConnectionFactory(s, PostgresBackendMod.getConfig());
        factories.add(f);
        logger.info("[PostgresBackend] createConnectionFactory(schema=" + s + ")");
        return f;
    }

    @Override
    public Object createMigrationStrategy() {
        logger.info("[PostgresBackend] createMigrationStrategy() — " + factories.size() + " factories");
        return new PostgresMigrationStrategy(new ArrayList<>(factories));
    }
}
