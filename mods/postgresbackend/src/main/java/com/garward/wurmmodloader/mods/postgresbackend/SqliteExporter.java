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
            sq.commit();
        }
        return result;
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
            "SELECT column_name, data_type, is_nullable " +
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
    }

    private static final class IndexSpec {
        String name;
        boolean unique;
        final List<String> columns = new ArrayList<>();
    }
}
