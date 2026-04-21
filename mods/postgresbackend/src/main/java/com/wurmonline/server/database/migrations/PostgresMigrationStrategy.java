package com.wurmonline.server.database.migrations;

import com.wurmonline.server.database.PostgresConnectionFactory;
import com.wurmonline.server.database.WurmDatabaseSchema;
import org.flywaydb.core.api.MigrationVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Multi-schema Postgres migration strategy. One {@link PostgresMigrator} per
 * {@link WurmDatabaseSchema}, each scoped to its own Postgres schema via
 * {@code flyway.setSchemas()}. Mirrors {@link SqliteMigrationStrategy}'s
 * aggregate-success-or-first-error semantics.
 *
 * <p>Lives in this package because {@link MigrationResult} factory methods
 * ({@code newSuccess}, {@code newError}) are package-private.</p>
 */
public final class PostgresMigrationStrategy implements MigrationStrategy {

    private static final Logger logger = Logger.getLogger(PostgresMigrationStrategy.class.getName());

    private final List<PostgresMigrator> migrators;

    public PostgresMigrationStrategy(List<PostgresConnectionFactory> factories) {
        List<PostgresMigrator> built = new ArrayList<>();
        for (PostgresConnectionFactory f : factories) {
            built.add(new PostgresMigrator(f));
        }
        this.migrators = built;
    }

    @Override
    public boolean hasPendingMigrations() {
        for (PostgresMigrator m : migrators) {
            if (m.hasPendingMigrations()) return true;
        }
        return false;
    }

    @Override
    public MigrationResult migrate() {
        MigrationVersion earliest = MigrationVersion.LATEST;
        MigrationVersion latest = MigrationVersion.EMPTY;
        int total = 0;
        LinkedHashMap<WurmDatabaseSchema, MigrationResult.MigrationSuccess> done = new LinkedHashMap<>();

        for (PostgresMigrator m : migrators) {
            MigrationResult r = m.migrate();
            if (r.isError()) {
                logProgress(done);
                return r;
            }
            MigrationResult.MigrationSuccess s = r.asSuccess();
            done.put(m.getSchema(), s);
            if (latest.compareTo(s.getVersionAfter()) < 0) latest = s.getVersionAfter();
            MigrationVersion before = s.getVersionBefore();
            if (before == null) earliest = MigrationVersion.EMPTY;
            else if (before.compareTo(earliest) < 0) earliest = before;
            total += s.getNumMigrations();
        }
        return MigrationResult.newSuccess(earliest, latest, total);
    }

    private void logProgress(Map<WurmDatabaseSchema, MigrationResult.MigrationSuccess> done) {
        if (done.isEmpty()) {
            logger.warning("[PostgresBackend] Migration failed — no schemas migrated successfully.");
            return;
        }
        logger.warning("[PostgresBackend] Migration failed mid-run. Completed so far:");
        for (Map.Entry<WurmDatabaseSchema, MigrationResult.MigrationSuccess> e : done.entrySet()) {
            MigrationResult.MigrationSuccess s = e.getValue();
            String before = s.getVersionBefore() == null ? "baseline" : s.getVersionBefore().getVersion();
            logger.warning("  " + e.getKey().name() + ": " + s.getNumMigrations()
                + " migrations from " + before + " to " + s.getVersionAfter());
        }
    }
}
