package com.garward.wurmmodloader.mods.postgresbackend;

import com.garward.wurmmodloader.api.database.DatabaseBackendRegistry;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.database.DatabaseBackendBootstrapEvent;
import com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent;
import com.garward.wurmmodloader.api.events.server.ServerStoppingEvent;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Postgres backend mod entry point.
 *
 * <p>Boot order:</p>
 * <ol>
 *   <li>{@link #configure(Properties)} — load config.</li>
 *   <li>{@link #onSelect(DatabaseBackendSelectionEvent)} — if mode=embedded,
 *       start the embedded Postgres process (lazy-downloads the binary on
 *       first boot), then register {@link PostgresDatabaseBackend}.</li>
 *   <li>{@link #onBootstrap(DatabaseBackendBootstrapEvent)} — {@code CREATE
 *       SCHEMA IF NOT EXISTS} for all 9 WurmDatabaseSchema entries.</li>
 *   <li>{@link #onServerStopping(ServerStoppingEvent)} — stop the embedded
 *       server cleanly.</li>
 * </ol>
 */
public class PostgresBackendMod implements WurmServerMod, Configurable {

    private static final Logger logger = Logger.getLogger(PostgresBackendMod.class.getName());

    private static PostgresConfig CONFIG;
    private static final PostgresServer SERVER = new PostgresServer();

    static PostgresConfig getConfig() {
        if (CONFIG == null) {
            throw new IllegalStateException("PostgresBackendMod config not loaded yet");
        }
        return CONFIG;
    }

    @Override
    public void configure(Properties properties) {
        CONFIG = PostgresConfig.fromProperties(properties);
        logger.info("[PostgresBackend] mode=" + CONFIG.mode);
    }

    @SubscribeEvent
    public void onSelect(DatabaseBackendSelectionEvent event) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("pgjdbc driver not on mod classpath", e);
        }
        if (CONFIG.mode == PostgresConfig.Mode.EMBEDDED) {
            try {
                SERVER.start(CONFIG);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[PostgresBackend] Failed to start embedded Postgres", e);
                throw new RuntimeException("Embedded Postgres startup failed", e);
            }
            // Stop Postgres from a JVM shutdown hook rather than from
            // ServerStoppingEvent: Wurm's own shutdown code continues to open DB
            // connections after mod handlers run (final flushes, DbConnector
            // close-outs), so stopping in the event handler causes
            // "Connection refused" on the tail end of shutdown. JVM shutdown
            // hooks fire only after main() returns and all event listeners are done.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    SERVER.stop();
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "[PostgresBackend] Shutdown-hook stop threw", t);
                }
            }, "postgresbackend-stop"));
        } else {
            logger.info("[PostgresBackend] External mode — connecting to "
                + CONFIG.host + ":" + CONFIG.port + "/" + CONFIG.database
                + " as " + CONFIG.user);
        }

        resolveWorldDatabase();
        // If the world's sqlite scaffold was just rebuilt (by the create-world /
        // rebuild-dbs helpers), drop any pre-existing per-world Postgres DB so
        // the fresh SQLite actually takes effect on import. Without this, a
        // rebuilt world still boots against the previous run's Postgres state.
        maybeDropStaleWorldDatabase();
        // Always ensure the target DB exists — covers both the per-world case
        // (CREATE DATABASE wurm_<suffix>) and the no-world fallback (CREATE DATABASE wurm).
        boolean dbJustCreated = ensureDatabaseExists(CONFIG.effectiveDatabase());
        if (dbJustCreated) {
            // Fresh DB has empty schemas; Wurm's Flyway migrations/ folders are empty
            // (WU ships no SQL for Postgres), so nothing would create the SERVERS etc.
            // tables. Seed from the world's scrubbed SQLite scaffold — rebuild-dbs.sh
            // already populated it with the default schema + SERVERS row.
            scheduleSqliteSeedFromWorldFolder();
        }

        logger.info("[PostgresBackend] Registering PostgresDatabaseBackend (database="
            + CONFIG.effectiveDatabase() + ")");
        DatabaseBackendRegistry.register(new PostgresDatabaseBackend());
    }

    /**
     * Read the launch-world token captured by DelegatedLauncher and flip the effective
     * database to a per-world one. Each world folder maps to its own Postgres database
     * (e.g. start=Gartopolis → database {@code wurm_gartopolis}) so map/players/items
     * no longer leak between worlds sharing a single Postgres cluster.
     *
     * <p>Ensures the target DB exists by connecting to {@link PostgresConfig#adminDatabase}
     * and issuing {@code CREATE DATABASE} when missing. No-op if no launch world was
     * captured (GUI mode, direct CLI without {@code start=}).
     */
    private void resolveWorldDatabase() {
        String world = System.getProperty("wurmmodloader.launchWorldFolder", "").trim();
        if (world.isEmpty()) {
            logger.warning("[PostgresBackend] No wurmmodloader.launchWorldFolder set — "
                + "falling back to base database '" + CONFIG.database + "'. "
                + "Multiple worlds sharing this Postgres cluster will collide.");
            return;
        }
        String suffix = PostgresConfig.sanitizeWorldSuffix(world);
        if (suffix.isEmpty()) {
            logger.warning("[PostgresBackend] World '" + world + "' sanitized to empty suffix — "
                + "falling back to base database '" + CONFIG.database + "'");
            return;
        }
        CONFIG.worldSuffix = suffix;
        logger.info("[PostgresBackend] Using per-world database: " + CONFIG.effectiveDatabase()
            + " (world=" + world + ")");
    }

    /** Returns true if the database was just created, false if it already existed. */
    private boolean ensureDatabaseExists(String dbName) {
        Properties props = new Properties();
        props.setProperty("user", CONFIG.user);
        if (CONFIG.password != null && !CONFIG.password.isEmpty()) {
            props.setProperty("password", CONFIG.password);
        }
        try (Connection conn = DriverManager.getConnection(CONFIG.adminUrl(), props);
             Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery(
                 "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")) {
            if (rs.next()) {
                logger.info("[PostgresBackend] Database '" + dbName + "' already exists");
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed checking for database '" + dbName + "' via "
                + CONFIG.adminUrl(), e);
        }
        try (Connection conn = DriverManager.getConnection(CONFIG.adminUrl(), props);
             Statement st = conn.createStatement()) {
            // CREATE DATABASE doesn't accept parameters; dbName came from our own sanitizer.
            st.execute("CREATE DATABASE \"" + dbName + "\"");
            logger.info("[PostgresBackend] Created database '" + dbName + "'");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed creating database '" + dbName + "'", e);
        }
    }

    /**
     * Honor the {@code .wurmmodloader-fresh-world} marker that
     * {@code wurmmodloader-rebuild-dbs.sh/.bat} drops into a world's {@code
     * sqlite/} directory. The marker means the user just rebuilt the SQLite
     * scaffold and expects the next boot to start truly fresh. Drop the stale
     * per-world Postgres DB (and its import marker) so the normal
     * {@link #ensureDatabaseExists}/{@link #scheduleSqliteSeedFromWorldFolder}
     * path recreates + re-imports from the new SQLite.
     *
     * <p>No-op when no marker is present, when no launch world is set, or
     * when there is no existing per-world DB to drop.
     */
    private void maybeDropStaleWorldDatabase() {
        String world = System.getProperty("wurmmodloader.launchWorldFolder", "").trim();
        if (world.isEmpty()) return;
        java.io.File marker = new java.io.File(System.getProperty("user.dir"),
            world + "/sqlite/.wurmmodloader-fresh-world");
        if (!marker.isFile()) return;

        String dbName = CONFIG.effectiveDatabase();
        logger.info("[PostgresBackend] Detected " + marker
            + " — dropping any stale '" + dbName + "' before re-import.");

        Properties props = new Properties();
        props.setProperty("user", CONFIG.user);
        if (CONFIG.password != null && !CONFIG.password.isEmpty()) {
            props.setProperty("password", CONFIG.password);
        }
        try (Connection conn = DriverManager.getConnection(CONFIG.adminUrl(), props);
             Statement st = conn.createStatement()) {
            // Kick any existing sessions off the target DB so DROP doesn't fail
            // with "database is being accessed by other users".
            st.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
                + "WHERE datname = '" + dbName + "' AND pid <> pg_backend_pid()");
            st.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
            logger.info("[PostgresBackend] Dropped database '" + dbName + "'");
        } catch (Exception e) {
            throw new RuntimeException("Failed dropping stale database '" + dbName + "'", e);
        }

        // Also clear the import marker so maybeImportFromSqlite re-runs.
        try {
            Path mig = migratedMarker();
            if (Files.deleteIfExists(mig)) {
                logger.info("[PostgresBackend] Removed stale import marker " + mig);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "[PostgresBackend] Couldn't delete migration marker", e);
        }

        // Consume the marker so subsequent boots don't keep dropping.
        if (!marker.delete()) {
            logger.warning("[PostgresBackend] Could not delete " + marker
                + " — next boot will drop the DB again.");
        }
    }

    /**
     * Point {@code CONFIG.migrateFromSqlite} at the live world's sqlite scaffold
     * so {@link #maybeImportFromSqlite()} (invoked from {@code onBootstrap} after
     * schemas are created) populates the fresh Postgres DB with the WU schema +
     * default SERVERS/template rows that rebuild-dbs.sh laid down. Per-world
     * marker so each new world gets its own one-shot import.
     */
    private void scheduleSqliteSeedFromWorldFolder() {
        String world = System.getProperty("wurmmodloader.launchWorldFolder", "").trim();
        if (world.isEmpty()) {
            logger.warning("[PostgresBackend] New database but no launch world — skipping SQLite seed. "
                + "Tables will be missing until you set migrateFromSqlite manually.");
            return;
        }
        java.io.File sqliteDir = new java.io.File(System.getProperty("user.dir"), world + "/sqlite");
        if (!sqliteDir.isDirectory()) {
            logger.warning("[PostgresBackend] New database but " + sqliteDir
                + " missing — skipping SQLite seed.");
            return;
        }
        CONFIG.migrateFromSqlite = sqliteDir.getAbsolutePath();
        logger.info("[PostgresBackend] Scheduled SQLite seed from " + CONFIG.migrateFromSqlite
            + " → " + CONFIG.effectiveDatabase());
    }

    @SubscribeEvent
    public void onBootstrap(DatabaseBackendBootstrapEvent event) {
        logger.info("[PostgresBackend] Bootstrap — ensuring 9 schemas exist in database '"
            + CONFIG.effectiveDatabase() + "'");
        ensureSchemas();
        maybeImportFromSqlite();
        SchemaRepair.repair(CONFIG);
        PostgresBackup.startSchedule(CONFIG);
    }

    private static Path migratedMarker() {
        // Per-DB marker so each world's one-shot import is tracked independently.
        return Paths.get("mods/postgresbackend/.migrated." + CONFIG.effectiveDatabase());
    }

    private void maybeImportFromSqlite() {
        if (CONFIG.migrateFromSqlite == null || CONFIG.migrateFromSqlite.isEmpty()) {
            return;
        }
        Path marker = migratedMarker();
        if (Files.exists(marker)) {
            logger.info("[PostgresBackend] migrateFromSqlite set but " + marker
                + " exists — skipping import.");
            return;
        }
        Path sqliteDir = Paths.get(CONFIG.migrateFromSqlite);
        if (!Files.isDirectory(sqliteDir)) {
            logger.severe("[PostgresBackend] migrateFromSqlite path is not a directory: " + sqliteDir);
            throw new RuntimeException("migrateFromSqlite path invalid: " + sqliteDir);
        }
        logger.info("[PostgresBackend] Auto-importing SQLite → Postgres from " + sqliteDir);
        try {
            int tables = SqliteImporter.importAll(CONFIG, sqliteDir);
            logger.info("[PostgresBackend] Import complete — " + tables + " tables copied.");
            Files.createDirectories(marker.getParent());
            Files.write(marker,
                ("imported from " + sqliteDir + " at " + java.time.Instant.now() + "\n").getBytes());
            logger.info("[PostgresBackend] Wrote marker " + marker + " — remove to re-run.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[PostgresBackend] SQLite import failed", e);
            throw new RuntimeException("SQLite import failed", e);
        }
    }

    private static void createCastIfMissing(Statement st, String src, String tgt, String fnSig) throws java.sql.SQLException {
        // CREATE CAST has no IF NOT EXISTS; swallow 42710 (duplicate_object).
        try {
            st.execute("CREATE CAST (" + src + " AS " + tgt + ") WITH FUNCTION " + fnSig + " AS IMPLICIT");
        } catch (java.sql.SQLException e) {
            if (!"42710".equals(e.getSQLState())) throw e;
        }
    }

    private void ensureSchemas() {
        Properties props = new Properties();
        props.setProperty("user", CONFIG.user);
        if (CONFIG.password != null && !CONFIG.password.isEmpty()) {
            props.setProperty("password", CONFIG.password);
        }
        try (Connection conn = DriverManager.getConnection(CONFIG.baseUrl(), props);
             Statement st = conn.createStatement()) {
            for (com.wurmonline.server.database.WurmDatabaseSchema s
                    : com.wurmonline.server.database.WurmDatabaseSchema.values()) {
                String name = s.getDatabase().toLowerCase();
                st.execute("CREATE SCHEMA IF NOT EXISTS " + name);
                logger.info("[PostgresBackend]   schema ensured: " + name);
            }
            // SQLite's type system is loose — Wurm declares many columns as
            // INTEGER/BIGINT but binds them via setBoolean() (e.g. CREATURES.STEALTH).
            // Postgres rejects this with "column X is of type bigint but expression
            // is of type boolean". Install implicit casts both directions so Wurm's
            // mixed typing works unchanged.
            st.execute(
                "CREATE OR REPLACE FUNCTION public._wurm_bool_to_bigint(boolean) " +
                "RETURNS bigint LANGUAGE sql IMMUTABLE AS " +
                "$$ SELECT CASE WHEN $1 THEN 1::bigint ELSE 0::bigint END $$");
            st.execute(
                "CREATE OR REPLACE FUNCTION public._wurm_bool_to_int(boolean) " +
                "RETURNS integer LANGUAGE sql IMMUTABLE AS " +
                "$$ SELECT CASE WHEN $1 THEN 1 ELSE 0 END $$");
            createCastIfMissing(st, "boolean", "bigint",  "public._wurm_bool_to_bigint(boolean)");
            createCastIfMissing(st, "boolean", "integer", "public._wurm_bool_to_int(boolean)");
            logger.info("[PostgresBackend]   boolean→integer implicit casts installed");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[PostgresBackend] Failed ensuring schemas", e);
            throw new RuntimeException("Postgres schema bootstrap failed", e);
        }
    }

    private static final Path EXPORTED_MARKER = Paths.get("mods/postgresbackend/.exported");

    private void maybeExportToSqlite() {
        if (CONFIG.exportToSqlite == null || CONFIG.exportToSqlite.isEmpty()) {
            return;
        }
        if (Files.exists(EXPORTED_MARKER)) {
            logger.info("[PostgresBackend] exportToSqlite set but " + EXPORTED_MARKER
                + " exists — skipping export.");
            return;
        }
        Path targetDir = Paths.get(CONFIG.exportToSqlite);
        logger.info("[PostgresBackend] Exporting Postgres → SQLite into " + targetDir
            + " (revert snapshot)");
        try {
            int tables = SqliteExporter.exportAll(CONFIG, targetDir);
            logger.info("[PostgresBackend] Export complete — " + tables + " tables written.");
            Files.createDirectories(EXPORTED_MARKER.getParent());
            Files.write(EXPORTED_MARKER,
                ("exported to " + targetDir.toAbsolutePath() + " at " + java.time.Instant.now() + "\n").getBytes());
            logger.info("[PostgresBackend] Wrote marker " + EXPORTED_MARKER + " — remove to re-run.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[PostgresBackend] SQLite export failed", e);
            // Don't rethrow — we're on the shutdown path and the user still
            // needs the Postgres process to stop cleanly below.
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Export while the server is still up and Postgres is still accepting
        // connections — we get a consistent snapshot of the data Wurm just
        // finished flushing. The Postgres process itself is stopped later by
        // the JVM shutdown hook registered in onSelect, *after* all of Wurm's
        // own shutdown code has finished touching the database.
        if (CONFIG != null) {
            // Order matters: stop the recurring backup scheduler first so a
            // pending interval dump doesn't race shutdown, then take a final
            // shutdown snapshot if requested (while Postgres is still up),
            // then do the optional SQLite export.
            PostgresBackup.stopSchedule();
            PostgresBackup.runShutdownBackup(CONFIG);
            maybeExportToSqlite();
        }
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
