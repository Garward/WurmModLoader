package com.garward.wurmmodloader.mods.postgresbackend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Reverse of {@link SqliteImporter}: dumps the 9 Postgres schemas back into
 * {@code wurm*.db} SQLite files. One-shot, opt-in via config flag
 * {@code exportToSqlite=<dir>}; a {@code .exported} marker prevents re-runs.
 *
 * <p>Produces files that can be dropped straight into a world's
 * {@code <world>/sqlite/} directory to revert to the vanilla SQLite
 * backend after uninstalling this mod.</p>
 *
 * <p>Flyway/internal bookkeeping tables are skipped — SQLite-side migrations
 * will run on first boot under the vanilla backend.</p>
 */
public final class SqliteExporter {

    private static final Logger logger = Logger.getLogger(SqliteExporter.class.getName());
    private static final int BATCH_SIZE = 1000;

    /** Postgres schema name → SQLite filename stem (reverse of importer). */
    private static final Map<String, String> WU_SCHEMAS;
    static {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("wurmlogin",     "wurmlogin");
        m.put("wurmitems",     "wurmitems");
        m.put("wurmplayers",   "wurmplayers");
        m.put("wurmcreatures", "wurmcreatures");
        m.put("wurmdeities",   "wurmdeities");
        m.put("wurmzones",     "wurmzones");
        m.put("wurmeconomy",   "wurmeconomy");
        m.put("wurmlogs",      "wurmlogs");
        m.put("wurmtemplates", "wurmtemplates");
        WU_SCHEMAS = Collections.unmodifiableMap(m);
    }

    private static final Set<String> SKIP_TABLES_EXACT = new LinkedHashSet<>(
        Arrays.asList("flyway_schema_history", "schema_version"));

    public static int exportAll(PostgresConfig pgConfig, Path targetDir) throws SQLException, java.io.IOException {
        ensureSqliteDriver();
        Files.createDirectories(targetDir);
        logger.info("[PostgresBackend][export] target SQLite dir = " + targetDir.toAbsolutePath());

        Properties pgProps = new Properties();
        pgProps.setProperty("user", pgConfig.user);
        if (pgConfig.password != null && !pgConfig.password.isEmpty()) {
            pgProps.setProperty("password", pgConfig.password);
        }

        int totalTables = 0;
        long totalRows = 0;

        try (Connection pg = DriverManager.getConnection(pgConfig.baseUrl(), pgProps)) {
            for (Map.Entry<String, String> e : WU_SCHEMAS.entrySet()) {
                String pgSchema = e.getKey();
                String stem     = e.getValue();
                Path dbFile = targetDir.resolve(stem + ".db");
                // Overwrite any prior export.
                Files.deleteIfExists(dbFile);
                SchemaResult r = exportOne(pg, pgSchema, dbFile);
                totalTables += r.tables;
                totalRows += r.rows;
                logger.info(String.format("[PostgresBackend][export]   %-14s %d tables, %d rows → %s",
                    pgSchema, r.tables, r.rows, dbFile.getFileName()));
            }
        }

        logger.info("[PostgresBackend][export] DONE — " + totalTables + " tables, "
            + totalRows + " rows exported across " + WU_SCHEMAS.size() + " schemas");
        return totalTables;
    }

    private static SchemaResult exportOne(Connection pg, String pgSchema, Path sqliteFile) throws SQLException {
        SchemaResult result = new SchemaResult();
        String jdbcUrl = "jdbc:sqlite:" + sqliteFile.toAbsolutePath();
        try (Connection sq = DriverManager.getConnection(jdbcUrl)) {
            sq.setAutoCommit(false);
            List<String> tables = listTables(pg, pgSchema);
            for (String table : tables) {
                TableSpec spec = introspectTable(pg, pgSchema, table);
                createSqliteTable(sq, spec);
                createSqliteIndexes(sq, spec);
                long rows = copyRows(pg, pgSchema, sq, spec);
                result.tables++;
                result.rows += rows;
            }
            writeSchemaVersion(sq, pgSchema);
            sq.commit();
        }
        return result;
    }

    /**
     * Synthesize Flyway 3.x SCHEMA_VERSION so that vanilla SQLite-mode Wurm
     * sees all migrations as already applied and skips them. Without this, the
     * exported schema is at the latest version but Flyway's bookkeeping is empty,
     * so it tries to re-run v6/v9/etc against tables that already have those
     * columns and crashes. Scans {@code dist/migrations/<schema>/v*.sql} and
     * computes Flyway's CRC32 (line-by-line, UTF-8, BOM-stripped) for each.
     */
    private static void writeSchemaVersion(Connection sq, String pgSchema) throws SQLException {
        try (Statement st = sq.createStatement()) {
            st.execute("CREATE TABLE \"SCHEMA_VERSION\" ("
                + "\"installed_rank\" INT NOT NULL PRIMARY KEY, "
                + "\"version\" VARCHAR(50), "
                + "\"description\" VARCHAR(200) NOT NULL, "
                + "\"type\" VARCHAR(20) NOT NULL, "
                + "\"script\" VARCHAR(1000) NOT NULL, "
                + "\"checksum\" INT, "
                + "\"installed_by\" VARCHAR(100) NOT NULL, "
                + "\"installed_on\" TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now')), "
                + "\"execution_time\" INT NOT NULL, "
                + "\"success\" BOOLEAN NOT NULL)");
            st.execute("CREATE INDEX \"SCHEMA_VERSION_s_idx\" ON \"SCHEMA_VERSION\" (\"success\")");
        }

        // Strip 'wurm' prefix → migrations subdir name (wurmcreatures → creatures).
        String stem = pgSchema.startsWith("wurm") ? pgSchema.substring(4) : pgSchema;
        Path migDir = java.nio.file.Paths.get("dist", "migrations", stem);

        String insert = "INSERT INTO \"SCHEMA_VERSION\" "
            + "(installed_rank, version, description, type, script, checksum, "
            + "installed_by, execution_time, success) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = sq.prepareStatement(insert)) {
            int rank = 1;
            // Baseline row, always present.
            ps.setInt(1, rank++);
            ps.setString(2, "1");
            ps.setString(3, "<< Flyway Baseline >>");
            ps.setString(4, "BASELINE");
            ps.setString(5, "<< Flyway Baseline >>");
            ps.setNull(6, Types.INTEGER);
            ps.setString(7, "");
            ps.setInt(8, 0);
            ps.setInt(9, 1);
            ps.executeUpdate();

            if (!Files.isDirectory(migDir)) return;
            List<Path> sqlFiles = new ArrayList<>();
            try (java.util.stream.Stream<Path> s = Files.list(migDir)) {
                s.filter(p -> {
                    String n = p.getFileName().toString();
                    return n.startsWith("v") && n.endsWith(".sql");
                }).forEach(sqlFiles::add);
            } catch (java.io.IOException io) {
                throw new SQLException("listing migrations dir " + migDir, io);
            }
            sqlFiles.sort((a, b) -> Integer.compare(parseVersion(a), parseVersion(b)));
            for (Path f : sqlFiles) {
                String fname = f.getFileName().toString();
                int version = parseVersion(f);
                String desc = parseDescription(fname);
                int checksum;
                try {
                    checksum = flywayChecksum(f);
                } catch (java.io.IOException io) {
                    throw new SQLException("checksumming " + f, io);
                }
                ps.setInt(1, rank++);
                ps.setString(2, Integer.toString(version));
                ps.setString(3, desc);
                ps.setString(4, "SQL");
                ps.setString(5, fname);
                ps.setInt(6, checksum);
                ps.setString(7, "");
                ps.setInt(8, 0);
                ps.setInt(9, 1);
                ps.executeUpdate();
            }
        }
    }

    /** Parses leading version number from {@code vN__description.sql}. Returns 0 if malformed. */
    private static int parseVersion(Path f) {
        String n = f.getFileName().toString();
        int us = n.indexOf("__");
        if (us < 2 || n.charAt(0) != 'v') return 0;
        try { return Integer.parseInt(n.substring(1, us)); }
        catch (NumberFormatException e) { return 0; }
    }

    /** {@code v3__cooking_update.sql → "cooking update"}. */
    private static String parseDescription(String fname) {
        int us = fname.indexOf("__");
        if (us < 0) return fname;
        String tail = fname.substring(us + 2);
        if (tail.endsWith(".sql")) tail = tail.substring(0, tail.length() - 4);
        return tail.replace('_', ' ');
    }

    /**
     * Flyway 3.x FileSystemResource.getChecksum: CRC32 over the UTF-8 bytes of
     * each line (newlines stripped), BOM filtered from the first line.
     */
    private static int flywayChecksum(Path file) throws java.io.IOException {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(Files.newInputStream(file), "UTF-8"))) {
            String line = br.readLine();
            if (line != null) {
                if (!line.isEmpty() && line.charAt(0) == '﻿') line = line.substring(1);
                do {
                    crc.update(line.getBytes("UTF-8"));
                } while ((line = br.readLine()) != null);
            }
        }
        return (int) crc.getValue();
    }

    private static List<String> listTables(Connection pg, String pgSchema) throws SQLException {
        List<String> out = new ArrayList<>();
        String q = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        try (PreparedStatement ps = pg.prepareStatement(q)) {
            ps.setString(1, pgSchema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String n = rs.getString(1);
                    if (SKIP_TABLES_EXACT.contains(n.toLowerCase(Locale.ROOT))) continue;
                    out.add(n);
                }
            }
        }
        return out;
    }

    private static TableSpec introspectTable(Connection pg, String pgSchema, String tableName) throws SQLException {
        TableSpec spec = new TableSpec();
        spec.name = tableName;

        String colQ =
            "SELECT column_name, data_type, is_nullable, column_default " +
            "FROM information_schema.columns " +
            "WHERE table_schema = ? AND table_name = ? " +
            "ORDER BY ordinal_position";
        try (PreparedStatement ps = pg.prepareStatement(colQ)) {
            ps.setString(1, pgSchema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ColumnSpec c = new ColumnSpec();
                    c.name = rs.getString("column_name");
                    c.pgType = rs.getString("data_type");
                    c.notNull = "NO".equalsIgnoreCase(rs.getString("is_nullable"));
                    c.sqliteType = pgToSqliteType(c.pgType);
                    c.defaultValue = pgDefaultToSqlite(rs.getString("column_default"), c.pgType);
                    spec.columns.add(c);
                }
            }
        }

        // Primary key (ordered by ordinal_position from pg_constraint → pg_attribute).
        DatabaseMetaData md = pg.getMetaData();
        try (ResultSet rs = md.getPrimaryKeys(null, pgSchema, tableName)) {
            Map<Integer, String> pk = new LinkedHashMap<>();
            while (rs.next()) {
                pk.put((int) rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME"));
            }
            ArrayList<Integer> keys = new ArrayList<>(pk.keySet());
            Collections.sort(keys);
            for (Integer k : keys) spec.primaryKey.add(pk.get(k));
        }

        // Secondary indexes (skip PK-origin duplicates).
        try (ResultSet rs = md.getIndexInfo(null, pgSchema, tableName, false, false)) {
            LinkedHashMap<String, IndexSpec> byName = new LinkedHashMap<>();
            while (rs.next()) {
                String idxName = rs.getString("INDEX_NAME");
                if (idxName == null) continue;
                String col = rs.getString("COLUMN_NAME");
                if (col == null) continue;
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                // Drop the PK index — we'll emit it via CREATE TABLE PRIMARY KEY.
                if (!spec.primaryKey.isEmpty() && idxName.endsWith("_pkey")) continue;
                IndexSpec idx = byName.get(idxName);
                if (idx == null) {
                    idx = new IndexSpec();
                    idx.name = idxName;
                    idx.unique = !nonUnique;
                    byName.put(idxName, idx);
                }
                idx.columns.add(col);
            }
            spec.indexes.addAll(byName.values());
        }
        return spec;
    }

    private static void createSqliteTable(Connection sq, TableSpec spec) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(quote(spec.name)).append(" (\n");
        boolean first = true;
        for (ColumnSpec c : spec.columns) {
            if (!first) sql.append(",\n");
            sql.append("  ").append(quote(c.name)).append(' ').append(c.sqliteType);
            if (c.notNull) sql.append(" NOT NULL");
            if (c.defaultValue != null) sql.append(" DEFAULT ").append(c.defaultValue);
            first = false;
        }
        if (!spec.primaryKey.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (");
            for (int i = 0; i < spec.primaryKey.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(quote(spec.primaryKey.get(i)));
            }
            sql.append(')');
        }
        sql.append("\n)");
        try (Statement st = sq.createStatement()) {
            st.execute(sql.toString());
        }
    }

    private static void createSqliteIndexes(Connection sq, TableSpec spec) throws SQLException {
        int n = 0;
        for (IndexSpec idx : spec.indexes) {
            // Re-derive an index name scoped to the table to avoid collisions between files.
            String idxName = (spec.name + "_idx_" + (n++)).toLowerCase(Locale.ROOT);
            StringBuilder sql = new StringBuilder();
            sql.append("CREATE ").append(idx.unique ? "UNIQUE INDEX " : "INDEX ")
                .append(quote(idxName)).append(" ON ").append(quote(spec.name)).append(" (");
            for (int i = 0; i < idx.columns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(quote(idx.columns.get(i)));
            }
            sql.append(')');
            try (Statement st = sq.createStatement()) {
                st.execute(sql.toString());
            }
        }
    }

    private static long copyRows(Connection pg, String pgSchema, Connection sq, TableSpec spec) throws SQLException {
        StringBuilder select = new StringBuilder("SELECT ");
        StringBuilder insert = new StringBuilder("INSERT INTO ").append(quote(spec.name)).append(" (");
        StringBuilder qs = new StringBuilder();
        for (int i = 0; i < spec.columns.size(); i++) {
            if (i > 0) { select.append(", "); insert.append(", "); qs.append(", "); }
            select.append('"').append(spec.columns.get(i).name).append('"');
            insert.append(quote(spec.columns.get(i).name));
            qs.append('?');
        }
        select.append(" FROM \"").append(pgSchema).append("\".\"").append(spec.name).append('"');
        insert.append(") VALUES (").append(qs).append(')');

        long count = 0;
        try (Statement sel = pg.createStatement();
             ResultSet rs = sel.executeQuery(select.toString());
             PreparedStatement ps = sq.prepareStatement(insert.toString())) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            int inBatch = 0;
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    bindSqlite(ps, i, v);
                }
                ps.addBatch();
                if (++inBatch >= BATCH_SIZE) {
                    ps.executeBatch();
                    inBatch = 0;
                }
                count++;
            }
            if (inBatch > 0) ps.executeBatch();
        }
        return count;
    }

    private static void bindSqlite(PreparedStatement ps, int idx, Object v) throws SQLException {
        if (v == null) {
            ps.setNull(idx, Types.NULL);
            return;
        }
        // SQLite is dynamically typed; the driver handles most boxed values.
        // Normalise Boolean → 0/1 so the result matches what Wurm originally wrote
        // (SQLite schema used BIGINT columns for booleans).
        if (v instanceof Boolean) {
            ps.setInt(idx, ((Boolean) v) ? 1 : 0);
            return;
        }
        if (v instanceof java.sql.Timestamp) {
            ps.setString(idx, v.toString());
            return;
        }
        if (v instanceof byte[]) {
            ps.setBytes(idx, (byte[]) v);
            return;
        }
        ps.setObject(idx, v);
    }

    // Postgres data_type → permissive SQLite affinity.
    private static String pgToSqliteType(String pg) {
        if (pg == null) return "TEXT";
        String t = pg.toLowerCase(Locale.ROOT);
        if (t.contains("int"))                                       return "BIGINT";
        if (t.equals("boolean"))                                     return "BOOLEAN";
        if (t.startsWith("double") || t.equals("real") || t.equals("float"))
                                                                     return "DOUBLE";
        if (t.equals("numeric") || t.equals("decimal"))              return "NUMERIC";
        if (t.equals("bytea"))                                       return "BLOB";
        if (t.startsWith("timestamp") || t.equals("date") || t.equals("time"))
                                                                     return "TEXT";
        return "TEXT";
    }

    /**
     * Translate a Postgres {@code column_default} expression to a literal usable
     * inside a SQLite {@code DEFAULT} clause. Returns {@code null} when the
     * expression should be omitted (NULL, sequences, anything we can't safely
     * round-trip — SQLite will fall back to NULL or NOT NULL enforcement).
     *
     * <p>Handles the shapes Postgres emits via information_schema.column_default:
     * bare numerics ({@code 0}, {@code -10}, {@code (-10)}); typed casts
     * ({@code '0'::bigint}, {@code 'foo'::character varying}); booleans
     * ({@code true}/{@code false} → {@code 1}/{@code 0}); and NULL casts.
     * Sequences ({@code nextval(...)}) and other function calls are dropped.</p>
     */
    private static String pgDefaultToSqlite(String pgDefault, String pgType) {
        if (pgDefault == null) return null;
        String s = pgDefault.trim();
        if (s.isEmpty()) return null;

        // Strip a trailing ::<type> cast (may itself be quoted/multi-word like 'character varying').
        int castIdx = s.lastIndexOf("::");
        if (castIdx > 0) s = s.substring(0, castIdx).trim();

        // Drop redundant outer parens (Postgres wraps negatives: (-10)).
        while (s.length() >= 2 && s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')') {
            s = s.substring(1, s.length() - 1).trim();
        }

        if (s.isEmpty()) return null;
        if (s.equalsIgnoreCase("NULL")) return null;

        // Sequences / function calls — can't represent these in a static SQLite default.
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("nextval(")) return null;

        if (lower.equals("true"))  return "1";
        if (lower.equals("false")) return "0";

        // Already a quoted string literal — keep as-is. Doubled '' inside is the
        // SQL standard escape and works in SQLite too.
        if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
            return s;
        }

        // Numeric literal (optional leading sign, digits, optional decimal/exponent).
        if (s.matches("[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
            return s;
        }

        // Anything else (e.g. a function call, expression we don't recognise) — skip
        // rather than risk emitting invalid SQLite syntax. NOT NULL will still hold.
        return null;
    }

    private static String quote(String ident) { return "\"" + ident.replace("\"", "\"\"") + "\""; }

    private static void ensureSqliteDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("sqlite-jdbc missing from mod classpath", e);
        }
    }

    // --- tiny value types (duplicated from SqliteImporter to avoid cross-class coupling) ---

    private static final class SchemaResult { int tables; long rows; }

    private static final class TableSpec {
        String name;
        final List<ColumnSpec> columns = new ArrayList<>();
        final List<String> primaryKey = new ArrayList<>();
        final List<IndexSpec> indexes = new ArrayList<>();
    }

    private static final class ColumnSpec {
        String name;
        String pgType;
        String sqliteType;
        boolean notNull;
        String defaultValue;  // already translated to a SQLite-safe literal, or null
    }

    private static final class IndexSpec {
        String name;
        boolean unique;
        final List<String> columns = new ArrayList<>();
    }
}
