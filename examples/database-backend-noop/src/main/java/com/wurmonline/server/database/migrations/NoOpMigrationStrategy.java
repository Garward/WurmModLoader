package com.wurmonline.server.database.migrations;

import org.flywaydb.core.api.MigrationVersion;

import java.util.logging.Logger;

/**
 * Reference {@link MigrationStrategy} for the NoOp backend example.
 *
 * <p>Lives in {@code com.wurmonline.server.database.migrations} because
 * {@link MigrationResult#newSuccess(MigrationVersion, MigrationVersion, int)}
 * and {@link MigrationResult#newError(String)} are package-private. A
 * production backend that actually runs Flyway would live here too — see
 * {@link SqliteMigrationStrategy} for the canonical multi-schema pattern.
 *
 * <p>This no-op reports zero pending migrations and returns a successful
 * {@code EMPTY → EMPTY} result. That's enough to satisfy the framework's
 * migration phase so Wurm boot continues.
 */
public final class NoOpMigrationStrategy implements MigrationStrategy {

    private static final Logger logger = Logger.getLogger(NoOpMigrationStrategy.class.getName());

    @Override
    public boolean hasPendingMigrations() {
        return false;
    }

    @Override
    public MigrationResult migrate() {
        logger.info("[NoOpBackend] migrate() — no-op, returning empty success");
        return MigrationResult.newSuccess(MigrationVersion.EMPTY, MigrationVersion.EMPTY, 0);
    }
}
