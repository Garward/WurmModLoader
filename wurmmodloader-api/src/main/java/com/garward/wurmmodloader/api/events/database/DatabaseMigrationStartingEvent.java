package com.garward.wurmmodloader.api.events.database;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.database.WurmDatabaseSchema;

/**
 * Fired by the framework immediately before Flyway migration runs on a schema.
 *
 * <p><b>Fired when:</b> {@code SqliteMigrator.migrate()} or {@code MysqlMigrator.migrate()}
 * is entered — once per schema on the SQLite path. On the MySQL path vanilla runs a single
 * multi-schema Flyway pass, so the event fires once with
 * {@code MysqlMigrationStrategy.MIGRATION_SCHEMA} ({@code WurmDatabaseSchema.LOGIN}) as the
 * representative schema.</p>
 *
 * <p><b>Use this to:</b> register additional Flyway migration locations, adjust migration
 * parameters on the active backend, or take a pre-migration backup.</p>
 *
 * <p>This event is <b>not cancellable</b>.</p>
 *
 * @since 1.0.0
 * @see DatabaseMigrationCompletedEvent
 */
public class DatabaseMigrationStartingEvent extends Event {

    private final WurmDatabaseSchema schema;

    public DatabaseMigrationStartingEvent(WurmDatabaseSchema schema) {
        super(false);
        this.schema = schema;
    }

    /** @return the schema about to be migrated; never {@code null} */
    public WurmDatabaseSchema getSchema() {
        return schema;
    }

    @Override
    public String toString() {
        return "DatabaseMigrationStartingEvent{schema=" + schema + "}";
    }
}
