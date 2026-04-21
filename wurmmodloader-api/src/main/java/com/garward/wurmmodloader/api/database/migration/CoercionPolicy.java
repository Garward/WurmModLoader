package com.garward.wurmmodloader.api.database.migration;

/**
 * Governs how {@link DatabaseMigrator} handles source→target type mismatches.
 *
 * <p>SQLite's type affinity is permissive: a column declared {@code INTEGER}
 * can legally contain a string like {@code "42"} or {@code ""}. Stricter
 * targets (Postgres, MariaDB with {@code STRICT_ALL_TABLES}) refuse those
 * rows. The policy picks which side to trust.</p>
 *
 * @since 1.0.0
 */
public enum CoercionPolicy {
    /** Pass values through unchanged. Target rejection → migration error. */
    STRICT,
    /**
     * Coerce when target metadata says the column is numeric/boolean and the
     * source value is a non-matching {@link String}: parse it, or substitute
     * {@code NULL} if parsing fails and the column is nullable. Logs every
     * coercion for audit.
     */
    LENIENT
}
