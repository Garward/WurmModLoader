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

        logger.info("[PostgresBackend] Registering PostgresDatabaseBackend");
        DatabaseBackendRegistry.register(new PostgresDatabaseBackend());
    }

    @SubscribeEvent
    public void onBootstrap(DatabaseBackendBootstrapEvent event) {
        logger.info("[PostgresBackend] Bootstrap — ensuring 9 schemas exist in database '"
            + CONFIG.database + "'");
        ensureSchemas();
        maybeImportFromSqlite();
        SchemaRepair.repair(CONFIG);
        PostgresBackup.startSchedule(CONFIG);
    }

    private static final Path MIGRATED_MARKER = Paths.get("mods/postgresbackend/.migrated");

    private void maybeImportFromSqlite() {
        if (CONFIG.migrateFromSqlite == null || CONFIG.migrateFromSqlite.isEmpty()) {
            return;
        }
        if (Files.exists(MIGRATED_MARKER)) {
            logger.info("[PostgresBackend] migrateFromSqlite set but " + MIGRATED_MARKER
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
            Files.createDirectories(MIGRATED_MARKER.getParent());
            Files.write(MIGRATED_MARKER,
                ("imported from " + sqliteDir + " at " + java.time.Instant.now() + "\n").getBytes());
            logger.info("[PostgresBackend] Wrote marker " + MIGRATED_MARKER + " — remove to re-run.");
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
