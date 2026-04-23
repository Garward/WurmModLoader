package com.garward.wurmmodloader.mods.postgresbackend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Idempotent post-migration schema repair.
 *
 * <p>{@link SqliteImporter} skips {@code PRIMARY KEY} emission for any table
 * whose PK columns are NULL-allowed in the SQLite source (SQLite's historical
 * quirk: NULLs are permitted in PK columns). This leaves those tables without
 * a unique constraint in Postgres, breaking {@code ON CONFLICT (col)} upserts
 * used by Wurm and framework subsystems (e.g. {@code ServerConfigSync} writing
 * to {@code SERVERPROPERTIES}).</p>
 *
 * <p>This class runs on every boot after {@code ensureSchemas()} and adds
 * {@code CREATE UNIQUE INDEX IF NOT EXISTS} for a small allowlist of tables
 * where the framework or Wurm relies on upsert semantics. Safe to run
 * repeatedly; failures on individual tables (duplicates, missing table) are
 * logged but do not abort boot.</p>
 */
final class SchemaRepair {

    private static final Logger logger = Logger.getLogger(SchemaRepair.class.getName());

    /** (schema, table, column) triples that must have a unique index. */
    private static final List<String[]> REQUIRED_UNIQUE = Arrays.<String[]>asList(
        new String[]{"wurmlogin", "serverproperties", "propkey"}
    );

    /**
     * Tables Wurm queries with {@code SELECT *} but only creates lazily on
     * first write — on SQLite those SELECTs tolerate the missing table (the
     * server swallows the exception and logs a WARNING, treating the result
     * as empty). On Postgres the same swallow happens, but the boot log is
     * flooded with "relation does not exist" stack traces. We pre-create
     * these tables as empty so the SELECTs succeed cleanly.
     *
     * <p>Column layouts derived from the {@code INSERT}/{@code SELECT}
     * statements and {@code rs.get*} calls in the Wurm decompiled source
     * ({@code SteamIdBan}, {@code Recipes}, {@code RecipesByPlayer}). No
     * primary keys — Wurm's writes already guarantee uniqueness and we never
     * need ON CONFLICT semantics on these.</p>
     */
    private static final String[][] OPTIONAL_TABLES = new String[][] {
        {"wurmplayers", "banned_steam_ids",
            "STEAM_ID VARCHAR(64) NOT NULL, BANREASON VARCHAR(255), BANEXPIRY BIGINT"},
        {"wurmitems", "recipesnamed",
            "RECIPEID SMALLINT NOT NULL, NAMER VARCHAR(64)"},
        {"wurmitems", "recipesplayer",
            "PLAYERID BIGINT NOT NULL, RECIPEID SMALLINT NOT NULL, FAVOURITE BOOLEAN, NOTES VARCHAR(255)"},
        {"wurmitems", "recipeplayercookers",
            "PLAYERID BIGINT NOT NULL, RECIPEID SMALLINT NOT NULL, COOKERID SMALLINT NOT NULL"},
        {"wurmitems", "recipeplayercontainers",
            "PLAYERID BIGINT NOT NULL, RECIPEID SMALLINT NOT NULL, CONTAINERID SMALLINT NOT NULL"},
        {"wurmitems", "recipeplayeringredients",
            "PLAYERID BIGINT NOT NULL, RECIPEID SMALLINT NOT NULL, INGREDIENTID SMALLINT NOT NULL, "
            + "GROUPID SMALLINT, TEMPLATEID SMALLINT, CSTATE SMALLINT, PSTATE SMALLINT, "
            + "MATERIAL SMALLINT, REALTEMPLATEID SMALLINT"}
    };

    static void repair(PostgresConfig cfg) {
        Properties props = new Properties();
        props.setProperty("user", cfg.user);
        if (cfg.password != null && !cfg.password.isEmpty()) {
            props.setProperty("password", cfg.password);
        }
        logger.info("[PostgresBackend][repair] checking " + REQUIRED_UNIQUE.size()
            + " required unique indexes + " + OPTIONAL_TABLES.length + " optional tables");
        try (Connection conn = DriverManager.getConnection(cfg.baseUrl(), props)) {
            for (String[] tgt : REQUIRED_UNIQUE) {
                ensureUniqueIndex(conn, tgt[0], tgt[1], tgt[2]);
            }
            for (String[] tgt : OPTIONAL_TABLES) {
                ensureOptionalTable(conn, tgt[0], tgt[1], tgt[2]);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[PostgresBackend][repair] failed to open connection", e);
        }
    }

    private static void ensureOptionalTable(Connection conn, String schema, String table, String columns) {
        if (tableExists(conn, schema, table)) {
            return;
        }
        String sql = "CREATE TABLE IF NOT EXISTS " + schema + "." + table + " (" + columns + ")";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
            logger.info("[PostgresBackend][repair]   + created optional table "
                + schema + "." + table);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[PostgresBackend][repair] failed creating optional table "
                + schema + "." + table, e);
        }
    }

    private static void ensureUniqueIndex(Connection conn, String schema, String table, String column) {
        String idxName = (schema + "_" + table + "_" + column + "_uniq").toLowerCase();
        if (!tableExists(conn, schema, table)) {
            logger.warning("[PostgresBackend][repair] " + schema + "." + table
                + " does not exist — skipping (expected table is missing!)");
            return;
        }
        if (uniqueIndexExists(conn, schema, table, column)) {
            logger.info("[PostgresBackend][repair]   ✓ " + schema + "." + table + "(" + column
                + ") already has a unique index");
            return;
        }
        logger.info("[PostgresBackend][repair]   + " + schema + "." + table + "(" + column
            + ") missing unique index — adding " + idxName);
        List<String> dupes = findDuplicates(conn, schema, table, column);
        if (!dupes.isEmpty()) {
            logger.warning("[PostgresBackend][repair] " + schema + "." + table + "(" + column
                + ") has duplicate values: " + dupes + " — deduplicating (keeping newest row by ctid)");
            int removed = deduplicate(conn, schema, table, column);
            if (removed < 0) {
                logger.severe("[PostgresBackend][repair] dedup failed on " + schema + "." + table
                    + "(" + column + ") — aborting index creation. Fix manually.");
                return;
            }
            logger.info("[PostgresBackend][repair]   removed " + removed + " duplicate rows from "
                + schema + "." + table);
        }
        String sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + idxName
            + " ON " + schema + "." + table + "(" + column + ")";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
            logger.info("[PostgresBackend][repair] added unique index " + idxName
                + " on " + schema + "." + table + "(" + column + ")");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[PostgresBackend][repair] failed to create " + idxName, e);
        }
    }

    private static boolean tableExists(Connection conn, String schema, String table) {
        String sql = "SELECT 1 FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_name = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean uniqueIndexExists(Connection conn, String schema, String table, String column) {
        // Matches any unique index (or PK) with exactly one column equal to `column`.
        String sql =
            "SELECT 1 FROM pg_index i "
          + "JOIN pg_class c ON c.oid = i.indrelid "
          + "JOIN pg_namespace n ON n.oid = c.relnamespace "
          + "JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey) "
          + "WHERE n.nspname = ? AND c.relname = ? AND a.attname = ? "
          + "AND i.indisunique AND array_length(i.indkey, 1) = 1";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[PostgresBackend][repair] index-check failed for "
                + schema + "." + table + "." + column, e);
            return true; // fail closed — don't attempt to create if we can't verify
        }
    }

    private static List<String> findDuplicates(Connection conn, String schema, String table, String column) {
        List<String> out = new ArrayList<>();
        String sql = "SELECT " + column + "::text AS v, COUNT(*) AS n "
            + "FROM " + schema + "." + table + " "
            + "WHERE " + column + " IS NOT NULL "
            + "GROUP BY " + column + " HAVING COUNT(*) > 1 LIMIT 10";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(rs.getString("v") + "×" + rs.getInt("n"));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[PostgresBackend][repair] duplicate-scan failed for "
                + schema + "." + table + "." + column, e);
        }
        return out;
    }

    /**
     * Delete duplicate rows keyed by {@code column}, keeping the row with the
     * largest {@code ctid} (most recently inserted physical row). Returns the
     * number of rows removed, or -1 on failure.
     */
    private static int deduplicate(Connection conn, String schema, String table, String column) {
        String sql =
            "DELETE FROM " + schema + "." + table + " a "
          + "USING " + schema + "." + table + " b "
          + "WHERE a." + column + " = b." + column + " "
          + "AND a.ctid < b.ctid";
        try (Statement st = conn.createStatement()) {
            return st.executeUpdate(sql);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[PostgresBackend][repair] dedup SQL failed", e);
            return -1;
        }
    }

    private SchemaRepair() {}
}
