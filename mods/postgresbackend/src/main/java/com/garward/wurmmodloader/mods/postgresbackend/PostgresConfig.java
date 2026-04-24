package com.garward.wurmmodloader.mods.postgresbackend;

import java.util.Properties;

/**
 * Typed holder for the mod configuration. Two modes:
 *
 * <ul>
 *   <li><b>embedded</b> (default) — the mod manages a Postgres process in
 *       {@link #embeddedDataDir}. Server owner does nothing but edit a password
 *       if they want one. {@code host} / {@code port} / {@code database} /
 *       {@code user} / {@code password} are populated at startup from the
 *       running embedded server.</li>
 *   <li><b>external</b> — connect to an existing Postgres cluster using the
 *       fields from the config file.</li>
 * </ul>
 *
 * Fields are mutable because the embedded server overrides the connection
 * details post-construction once the subprocess is listening.
 */
public final class PostgresConfig {

    public enum Mode { EMBEDDED, EXTERNAL }

    // Mode + external connection params
    public Mode mode;
    public String host;
    public int port;
    /** Base/prefix database name. Effective DB = {@code database + "_" + worldSuffix} when a
     *  world folder is resolved from {@code wurmmodloader.launchWorldFolder}. */
    public String database;
    /** Admin DB used only for CREATE DATABASE. Stays "postgres" in embedded, configurable in external. */
    public String adminDatabase = "postgres";
    /** Sanitized launch-world token; empty = fall back to {@link #database} verbatim. */
    public String worldSuffix = "";
    public String user;
    public String password;
    public String sslmode;

    // Embedded-only params
    public int embeddedPort;
    public String embeddedDataDir;
    public String embeddedBinariesDir;
    public String embeddedPostgresVersion;

    // One-shot SQLite import. Empty = no-op. Set to the world's sqlite/
    // directory path and the mod will import on next boot, then write a
    // marker file so subsequent boots skip the import even if this stays set.
    public String migrateFromSqlite;

    // One-shot Postgres → SQLite export. Empty = disabled (the default, so
    // users can verify Postgres is working before creating a revert snapshot).
    // Set to a directory path and on next boot the mod dumps all 9 schemas
    // back into wurm*.db files there, then writes a marker file so subsequent
    // boots skip the export. Uninstall path: remove the mod + copy these
    // files back into the world's sqlite/ directory.
    public String exportToSqlite;

    // When true (default), every clean shutdown re-exports all 9 schemas back into
    // the live world's <world>/sqlite/ directory, so disabling the postgres mod
    // later boots cleanly off SQLite with the latest state. Ignored when
    // exportToSqlite is set explicitly. Set to false to skip (large worlds where
    // shutdown export latency matters more than failover).
    public boolean autoExportToWorldSqlite;

    // --- auto-backup --------------------------------------------------------
    // Opt-in scheduled pg_dump snapshots. See postgresbackend.config for the
    // full docstring. All fields are ignored when autoBackupEnabled=false.
    public boolean autoBackupEnabled;
    public String  autoBackupDir;
    public int     autoBackupIntervalHours;
    public boolean autoBackupOnStartup;
    public boolean autoBackupOnShutdown;
    public int     autoBackupRetain;
    /** Optional override for pg_dump location. Empty = auto-detect (embedded binaries or PATH). */
    public String  pgDumpPath;

    private PostgresConfig() {}

    public static PostgresConfig fromProperties(Properties p) {
        PostgresConfig c = new PostgresConfig();
        c.mode = parseMode(p.getProperty("mode", "embedded"));
        c.host     = p.getProperty("host", "localhost");
        c.port     = Integer.parseInt(p.getProperty("port", "5432"));
        c.database = p.getProperty("database", "wurm");
        c.adminDatabase = p.getProperty("adminDatabase", "postgres");
        c.user     = p.getProperty("user", "postgres");
        c.password = p.getProperty("password", "");
        c.sslmode  = p.getProperty("sslmode", "disable");

        c.embeddedPort         = Integer.parseInt(p.getProperty("embeddedPort", "5433"));
        c.embeddedDataDir      = p.getProperty("embeddedDataDir", "mods/postgresbackend/data");
        c.embeddedBinariesDir  = p.getProperty("embeddedBinariesDir", "mods/postgresbackend/binaries");
        c.embeddedPostgresVersion = p.getProperty("embeddedPostgresVersion", "16.2.0");
        c.migrateFromSqlite = p.getProperty("migrateFromSqlite", "").trim();
        c.exportToSqlite    = p.getProperty("exportToSqlite", "").trim();
        c.autoExportToWorldSqlite = Boolean.parseBoolean(p.getProperty("autoExportToWorldSqlite", "true"));

        c.autoBackupEnabled        = Boolean.parseBoolean(p.getProperty("autoBackupEnabled", "false"));
        c.autoBackupDir            = p.getProperty("autoBackupDir", "mods/postgresbackend/backups").trim();
        c.autoBackupIntervalHours  = Integer.parseInt(p.getProperty("autoBackupIntervalHours", "24").trim());
        c.autoBackupOnStartup      = Boolean.parseBoolean(p.getProperty("autoBackupOnStartup", "true"));
        c.autoBackupOnShutdown     = Boolean.parseBoolean(p.getProperty("autoBackupOnShutdown", "false"));
        c.autoBackupRetain         = Integer.parseInt(p.getProperty("autoBackupRetain", "7").trim());
        c.pgDumpPath               = p.getProperty("pgDumpPath", "").trim();
        return c;
    }

    private static Mode parseMode(String s) {
        switch (s.trim().toLowerCase()) {
            case "embedded": return Mode.EMBEDDED;
            case "external": return Mode.EXTERNAL;
            default: throw new IllegalArgumentException("mode must be 'embedded' or 'external', got: " + s);
        }
    }

    /** Effective database name: base + "_" + sanitized world folder, or just base if no world resolved. */
    public String effectiveDatabase() {
        if (worldSuffix == null || worldSuffix.isEmpty()) return database;
        return database + "_" + worldSuffix;
    }

    /** Base URL for the per-world Postgres database; per-schema selection is done via SET search_path. */
    public String baseUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + effectiveDatabase() + "?sslmode=" + sslmode;
    }

    /** URL targeting the admin database (never world-scoped). Used for CREATE DATABASE. */
    public String adminUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + adminDatabase + "?sslmode=" + sslmode;
    }

    /** Postgres identifier rules: lowercase, [a-z0-9_], must not start with a digit, max 63 chars. */
    public static String sanitizeWorldSuffix(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase();
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_') {
                b.append(ch);
            } else {
                b.append('_');
            }
        }
        String out = b.toString();
        if (!out.isEmpty() && Character.isDigit(out.charAt(0))) out = "_" + out;
        if (out.length() > 55) out = out.substring(0, 55); // leave headroom for "<database>_" prefix
        return out;
    }
}
