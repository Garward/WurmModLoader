package com.garward.wurmmodloader.api.database;

/**
 * SPI interface for providing a custom database backend to WurmModLoader.
 *
 * <p>Mods can implement this interface to supply a drop-in replacement for Wurm's
 * built-in SQLite / MySQL factories (for example, PostgreSQL or MariaDB). Register
 * the implementation during
 * {@link com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent}
 * via {@link DatabaseBackendRegistry#register(DatabaseBackend)}.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #createConnectionFactory(Object)} must return an instance assignable to
 *       {@code com.wurmonline.server.database.ConnectionFactory}.</li>
 *   <li>{@link #createMigrationStrategy()} must return an instance assignable to
 *       {@code com.wurmonline.server.database.migrations.MigrationStrategy}.</li>
 *   <li>Backends returning their own factories are responsible for firing
 *       {@link com.garward.wurmmodloader.api.events.database.DatabaseConnectionOpenedEvent}
 *       themselves — the framework only auto-fires that event from the vanilla Sqlite /
 *       Mysql factories it patches directly.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public final class PostgresBackend implements DatabaseBackend {
 *     @Override public String getName() { return "postgres"; }
 *
 *     @Override public Object createConnectionFactory(Object schema) {
 *         return new PostgresConnectionFactory(
 *             (com.wurmonline.server.database.WurmDatabaseSchema) schema);
 *     }
 *
 *     @Override public Object createMigrationStrategy() {
 *         return new PostgresMigrationStrategy();
 *     }
 * }
 *
 * // In a mod's server-started logic, subscribe to DatabaseBackendSelectionEvent:
 * @SubscribeEvent
 * public void onSelect(DatabaseBackendSelectionEvent event) {
 *     DatabaseBackendRegistry.register(new PostgresBackend());
 * }
 * }</pre>
 *
 * <p>Backends can declare their SQL flavor via {@link #getDialect()} so the
 * framework's {@link SqlDialectRewriter} can translate hardcoded vanilla-WU
 * fragments (e.g. {@code INSERT OR IGNORE} → {@code ON CONFLICT DO NOTHING})
 * on the way to the driver. Default is {@link Dialect#CUSTOM} (no rewriting).</p>
 *
 * @since 1.0.0
 * @see DatabaseBackendRegistry
 * @see com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent
 */
public interface DatabaseBackend {

    /**
     * @return identifier for this backend, e.g. {@code "postgres"} or {@code "mariadb"}.
     *         Used for logging and diagnostic output; not parsed.
     */
    String getName();

    /**
     * SQL dialect this backend speaks. Drives {@link SqlDialectRewriter}.
     *
     * <p>Return {@link Dialect#POSTGRES}, {@link Dialect#MYSQL}, etc. to opt into
     * framework rewriting of {@code INSERT OR IGNORE} and similar SQLite-isms.
     * Return {@link Dialect#CUSTOM} (the default) to disable rewriting — useful
     * when the backend already translates at the driver level or targets an
     * unlisted engine.</p>
     *
     * @return the dialect; default {@link Dialect#CUSTOM}
     */
    default Dialect getDialect() {
        return Dialect.CUSTOM;
    }

    /**
     * Build the {@code ConnectionFactory} implementation for the given schema.
     *
     * <p>The returned object <b>must</b> be assignable to
     * {@code com.wurmonline.server.database.ConnectionFactory}. The parameter type is
     * declared as {@link Object} to keep this API classloader-agnostic; implementations
     * should cast to {@code com.wurmonline.server.database.WurmDatabaseSchema}.</p>
     *
     * @param schema the {@code WurmDatabaseSchema} the factory should bind to
     * @return a {@code ConnectionFactory} (as {@link Object} to avoid a hard import dependency)
     */
    Object createConnectionFactory(Object schema);

    /**
     * Build the {@code MigrationStrategy} the framework should use for migrations.
     *
     * <p>The returned object <b>must</b> be assignable to
     * {@code com.wurmonline.server.database.migrations.MigrationStrategy}.</p>
     *
     * @return a {@code MigrationStrategy} (as {@link Object} to avoid a hard import dependency)
     */
    Object createMigrationStrategy();
}
