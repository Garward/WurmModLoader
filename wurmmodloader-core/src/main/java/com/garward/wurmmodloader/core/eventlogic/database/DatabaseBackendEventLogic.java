package com.garward.wurmmodloader.core.eventlogic.database;

import com.garward.wurmmodloader.api.database.DatabaseBackend;
import com.garward.wurmmodloader.api.database.DatabaseBackendRegistry;
import com.garward.wurmmodloader.api.database.Dialect;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;
import com.wurmonline.server.DbConnector;
import com.wurmonline.server.database.ConnectionFactory;
import com.wurmonline.server.database.WrappingConnectionFactory;
import com.wurmonline.server.database.WurmDatabaseSchema;
import com.wurmonline.server.database.migrations.MigrationStrategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event-logic helper for the database-backend SPI.
 *
 * <p>Keeps all reflection / class-surgery required to swap in a mod-registered
 * {@link DatabaseBackend} out of the bytecode patch so the patch stays thin and
 * readable. Called from {@code DbConnectorInitPatch} after
 * {@code DbConnector.initialize(...)} has run its vanilla logic.</p>
 *
 * <p>If no backend is registered, {@link #applyRegisteredBackendIfAny()} is a no-op
 * and vanilla SQLite / MySQL behavior is preserved.</p>
 */
public final class DatabaseBackendEventLogic {

    private static final Logger LOGGER = Logger.getLogger(DatabaseBackendEventLogic.class.getName());

    private DatabaseBackendEventLogic() {}

    /**
     * If a {@link DatabaseBackend} is registered, replaces {@code DbConnector.CONNECTORS}
     * and {@code DbConnector.MIGRATION_STRATEGY} with the backend's factories. The vanilla
     * initialize() has already run by this point, so we're overwriting its output.
     *
     * <p>Failures are logged and swallowed so a broken backend does not prevent server boot —
     * vanilla config remains in place.</p>
     */
    public static void applyRegisteredBackendIfAny() {
        DatabaseBackend backend = DatabaseBackendRegistry.getRegistered();
        if (backend == null) {
            return;
        }

        LOGGER.info("[DatabaseBackendEventLogic] Overriding vanilla DB config with backend: "
            + backend.getName());

        try {
            ProxyServerHook.fireDatabaseBackendBootstrapEvent(backend);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                "[DatabaseBackendEventLogic] DatabaseBackendBootstrapEvent handler threw — "
                + "proceeding with backend install (per-schema connects may fail if bootstrap was required)",
                t);
        }

        try {
            EnumMap<WurmDatabaseSchema, DbConnector> overrideMap =
                new EnumMap<>(WurmDatabaseSchema.class);

            Constructor<DbConnector> ctor = DbConnector.class.getDeclaredConstructor(
                ConnectionFactory.class, String.class);
            ctor.setAccessible(true);

            for (WurmDatabaseSchema schema : WurmDatabaseSchema.values()) {
                Object factoryObj = backend.createConnectionFactory(schema);
                if (factoryObj == null) {
                    LOGGER.warning("[DatabaseBackendEventLogic] Backend " + backend.getName()
                        + " returned null factory for schema " + schema + " — skipping");
                    continue;
                }
                if (!(factoryObj instanceof ConnectionFactory)) {
                    LOGGER.severe("[DatabaseBackendEventLogic] Backend " + backend.getName()
                        + " returned factory of wrong type for schema " + schema + ": "
                        + factoryObj.getClass().getName() + " (must extend ConnectionFactory)");
                    return;
                }
                ConnectionFactory rawFactory = (ConnectionFactory) factoryObj;
                Dialect dialect = backend.getDialect();
                ConnectionFactory factory = new WrappingConnectionFactory(rawFactory, dialect);
                DbConnector connector = ctor.newInstance(factory, schema.name());
                overrideMap.put(schema, connector);
            }

            Object migrationObj = backend.createMigrationStrategy();
            if (!(migrationObj instanceof MigrationStrategy)) {
                LOGGER.severe("[DatabaseBackendEventLogic] Backend " + backend.getName()
                    + " returned migration strategy of wrong type: "
                    + (migrationObj == null ? "null" : migrationObj.getClass().getName())
                    + " (must implement MigrationStrategy)");
                return;
            }

            Field connectorsField = DbConnector.class.getDeclaredField("CONNECTORS");
            connectorsField.setAccessible(true);
            connectorsField.set(null, overrideMap);

            Field migrationField = DbConnector.class.getDeclaredField("MIGRATION_STRATEGY");
            migrationField.setAccessible(true);
            migrationField.set(null, migrationObj);

            LOGGER.info("[DatabaseBackendEventLogic] Installed backend " + backend.getName()
                + " with " + overrideMap.size() + " connector(s)");
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE,
                "[DatabaseBackendEventLogic] Failed to install backend " + backend.getName()
                + " — vanilla DB config remains active", t);
        }
    }
}
