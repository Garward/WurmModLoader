package com.garward.wurmmodloader.api.events.database;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.database.WurmDatabaseSchema;

/**
 * Fired by the framework immediately after Flyway migration completes successfully for a schema.
 *
 * <p><b>Fired when:</b> {@code SqliteMigrator.migrate()} / {@code MysqlMigrator.migrate()}
 * returns normally — once per schema on the SQLite path, once (with
 * {@code WurmDatabaseSchema.LOGIN} as the representative) on the MySQL path. If migration
 * throws, this event is <b>not</b> fired.</p>
 *
 * <p><b>Use this to:</b> run post-migration work — creating non-Flyway indexes, seeding reference
 * data, warming caches, or verifying schema invariants.</p>
 *
 * <p>This event is <b>not cancellable</b>.</p>
 *
 * @since 1.0.0
 * @see DatabaseMigrationStartingEvent
 */
public class DatabaseMigrationCompletedEvent extends Event {

    private final WurmDatabaseSchema schema;

    public DatabaseMigrationCompletedEvent(WurmDatabaseSchema schema) {
        super(false);
        this.schema = schema;
    }

    /** @return the schema that was just migrated; never {@code null} */
    public WurmDatabaseSchema getSchema() {
        return schema;
    }

    @Override
    public String toString() {
        return "DatabaseMigrationCompletedEvent{schema=" + schema + "}";
    }
}
