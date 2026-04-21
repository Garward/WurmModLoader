package com.garward.wurmmodloader.mods.postgresbackend;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
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
import java.util.regex.Pattern;

/**
 * In-process SQLite → Postgres importer. Java port of
 * {@code tools/db-consolidator/db_consolidator.py}, stripped down to the
 * auto-migrate case:
 *
 * <ul>
 *   <li>Always applies (no dry-run).</li>
 *   <li>Always drop-and-recreates the target schemas (fresh import).</li>
 *   <li>Always does all 9 WurmDatabaseSchema files; missing ones warn + skip.</li>
 * </ul>
 *
 * <p>Invoked once from {@link PostgresBackendMod#onBootstrap}. A marker file
 * written on success prevents re-runs even if the config flag is left in place.
 * Intentionally uses batch {@code INSERT}s rather than {@code COPY} — slower,
 * but driver-agnostic and produces clean error messages when a coercion fails.</p>
 */
public final class SqliteImporter {

    private static final Logger logger = Logger.getLogger(SqliteImporter.class.getName());
    private static final int BATCH_SIZE = 1000;

    /** SQLite filename stem → Postgres schema name. Matches {@code WurmDatabaseSchema.getDatabase().toLowerCase()}. */
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

    /**
     * Orchestrates the import. {@code pgConfig} provides connection info for
     * the live Postgres (embedded or external); {@code sqliteDir} is the
     * directory containing the 9 {@code wurm*.db} files.
     *
     * @return number of tables imported across all schemas
     */
    public static int importAll(PostgresConfig pgConfig, Path sqliteDir) throws SQLException {
        ensureDriver();
        logger.info("[PostgresBackend][import] source SQLite dir = " + sqliteDir.toAbsolutePath());

        Properties pgProps = new Properties();
        pgProps.setProperty("user", pgConfig.user);
        if (pgConfig.password != null && !pgConfig.password.isEmpty()) {
            pgProps.setProperty("password", pgConfig.password);
        }

        int totalTables = 0;
        long totalRows = 0;

        try (Connection pg = DriverManager.getConnection(pgConfig.baseUrl(), pgProps)) {
            pg.setAutoCommit(false);
            for (Map.Entry<String, String> e : WU_SCHEMAS.entrySet()) {
                String stem = e.getKey();
                String pgSchema = e.getValue();
                Path dbFile = sqliteDir.resolve(stem + ".db");
                if (!Files.isRegularFile(dbFile)) {
                    logger.warning("[PostgresBackend][import]   SKIP " + stem + " (file missing: " + dbFile + ")");
                    continue;
                }
                SchemaResult r = importOne(pg, pgSchema, dbFile);
                totalTables += r.tables;
                totalRows += r.rows;
                pg.commit();
                logger.info(String.format("[PostgresBackend][import]   %-14s %d tables, %d rows",
                    pgSchema, r.tables, r.rows));
            }
        }

        logger.info("[PostgresBackend][import] DONE — " + totalTables + " tables, "
            + totalRows + " rows imported across " + WU_SCHEMAS.size() + " schemas");
        return totalTables;
    }

    private static SchemaResult importOne(Connection pg, String pgSchema, Path sqliteFile) throws SQLException {
        SchemaResult result = new SchemaResult();

        String jdbcUrl = "jdbc:sqlite:" + sqliteFile.toAbsolutePath();
        try (Connection sq = DriverManager.getConnection(jdbcUrl)) {
            // Wipe + recreate schema to guarantee a clean slate.
            try (Statement st = pg.createStatement()) {
                st.execute("DROP SCHEMA IF EXISTS " + pgSchema + " CASCADE");
                st.execute("CREATE SCHEMA " + pgSchema);
            }

            List<String> tableNames = listTables(sq);
            for (String tableName : tableNames) {
                TableSpec spec = introspectTable(sq, tableName);
                createTable(pg, pgSchema, spec);
                createIndexes(pg, pgSchema, spec);
                long rows = copyRows(sq, pg, pgSchema, spec);
                String idCol = rowidAliasColumn(spec);
                if (idCol != null) resetIdentitySequence(pg, pgSchema, spec.name, idCol);
                result.tables++;
                result.rows += rows;
            }
        }
        return result;
    }

    private static List<String> listTables(Connection sq) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement st = sq.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
            while (rs.next()) {
                String n = rs.getString(1);
                String lc = n.toLowerCase(Locale.ROOT);
                if (lc.startsWith("sqlite_")) continue;
                if (SKIP_TABLES_EXACT.contains(lc)) continue;
                out.add(n);
            }
        }
        return out;
    }

    private static TableSpec introspectTable(Connection sq, String tableName) throws SQLException {
        TableSpec spec = new TableSpec();
        spec.name = tableName;

        // Columns + PK via PRAGMA table_info.
        try (Statement st = sq.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(\"" + tableName + "\")")) {
            Map<Integer, String> pk = new LinkedHashMap<>();
            while (rs.next()) {
                ColumnSpec c = new ColumnSpec();
                c.name = rs.getString("name");
                c.sqliteType = rs.getString("type");
                c.notNull = rs.getInt("notnull") != 0;
                c.pgType = mapType(c.sqliteType);
                c.defaultValue = rs.getString("dflt_value");
                spec.columns.add(c);
                int pkPos = rs.getInt("pk");
                if (pkPos > 0) pk.put(pkPos, c.name);
            }
            ArrayList<Integer> keys = new ArrayList<>(pk.keySet());
            Collections.sort(keys);
            for (Integer k : keys) spec.primaryKey.add(pk.get(k));
        }

        // Indexes via PRAGMA index_list / index_info. Skip auto and pk-origin.
        List<String[]> indexList = new ArrayList<>();
        try (Statement st = sq.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA index_list(\"" + tableName + "\")")) {
            while (rs.next()) {
                String origin = rs.getString("origin");
                if ("pk".equals(origin)) continue;
                String name = rs.getString("name");
                if (name.startsWith("sqlite_autoindex_")) continue;
                boolean unique = rs.getInt("unique") != 0;
                indexList.add(new String[]{name, String.valueOf(unique)});
            }
        }
        for (String[] entry : indexList) {
            String idxName = entry[0];
            IndexSpec idx = new IndexSpec();
            idx.name = idxName;
            idx.unique = Boolean.parseBoolean(entry[1]);
            try (Statement st = sq.createStatement();
                 ResultSet rs = st.executeQuery("PRAGMA index_info(\"" + idxName + "\")")) {
                Map<Integer, String> ordered = new LinkedHashMap<>();
                while (rs.next()) {
                    ordered.put(rs.getInt("seqno"), rs.getString("name"));
                }
                ArrayList<Integer> keys = new ArrayList<>(ordered.keySet());
                Collections.sort(keys);
                for (Integer k : keys) idx.columns.add(ordered.get(k));
            }
            if (!idx.columns.isEmpty()) spec.indexes.add(idx);
        }
        return spec;
    }

    private static void createTable(Connection pg, String pgSchema, TableSpec spec) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(ident(pgSchema)).append('.').append(ident(spec.name)).append(" (\n");
        boolean first = true;
        String rowidAlias = rowidAliasColumn(spec);
        for (ColumnSpec c : spec.columns) {
            if (!first) sql.append(",\n");
            sql.append("  ").append(ident(c.name)).append(' ').append(c.pgType);
            if (rowidAlias != null && rowidAlias.equalsIgnoreCase(c.name)) {
                // SQLite's INTEGER PRIMARY KEY is a rowid alias: auto-assigned on
                // INSERT when the client omits it. Wurm's INSERT code relies on
                // this for tables like gmmessages/creatures. Translate to
                // Postgres IDENTITY — BY DEFAULT lets the importer preserve
                // historical IDs, sequence is reset to max(col)+1 after copy.
                sql.append(" GENERATED BY DEFAULT AS IDENTITY");
            } else if (c.defaultValue != null) {
                // SQLite returns dflt_value pre-quoted for strings, raw for
                // numerics, and bare keywords like CURRENT_TIMESTAMP. SQLite's
                // quirky parser accepts DOUBLE-quoted string literals (DEFAULT ""),
                // but Postgres reads "" as a zero-length delimited identifier.
                // Rewrite double-quoted string defaults to single-quoted form.
                sql.append(" DEFAULT ").append(translateDefault(c.defaultValue, c.pgType));
            }
            if (c.notNull) sql.append(" NOT NULL");
            first = false;
        }
        if (!spec.primaryKey.isEmpty() && pkColumnsAreNotNull(spec)) {
            sql.append(",\n  PRIMARY KEY (");
            for (int i = 0; i < spec.primaryKey.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(ident(spec.primaryKey.get(i)));
            }
            sql.append(')');
        } else if (!spec.primaryKey.isEmpty()) {
            logger.warning("[PostgresBackend]   " + spec.name
                + ": skipping PRIMARY KEY — SQLite source allows NULL in PK columns "
                + spec.primaryKey);
        }
        sql.append("\n)");
        try (Statement st = pg.createStatement()) {
            st.execute(sql.toString());
        }
    }

    /**
     * Returns the name of the rowid-alias column (SQLite's {@code INTEGER
     * PRIMARY KEY}) if this table has one, else null. Such columns are
     * auto-assigned by SQLite on INSERT; in Postgres we translate them to
     * {@code GENERATED BY DEFAULT AS IDENTITY}.
     */
    private static String rowidAliasColumn(TableSpec spec) {
        if (spec.primaryKey.size() != 1) return null;
        String pk = spec.primaryKey.get(0);
        for (ColumnSpec c : spec.columns) {
            if (c.name.equalsIgnoreCase(pk)) {
                String t = c.sqliteType == null ? "" : c.sqliteType.trim().toUpperCase(Locale.ROOT);
                // Only exact "INTEGER" is a rowid alias. "BIGINT", "INT8", etc. are not.
                return "INTEGER".equals(t) ? c.name : null;
            }
        }
        return null;
    }

    private static void resetIdentitySequence(Connection pg, String pgSchema, String table, String col) throws SQLException {
        // Advance the auto-created identity sequence past the highest preserved
        // id so subsequent Wurm INSERTs don't collide. pg_get_serial_sequence
        // handles IDENTITY columns too despite the legacy name.
        // ident() lowercases everything, so the actual Postgres table/column
        // names are lowercase too. pg_get_serial_sequence's string args are
        // case-sensitive — pass them lowercased to match.
        String lcSchema = pgSchema.toLowerCase(Locale.ROOT);
        String lcTable = table.toLowerCase(Locale.ROOT);
        String lcCol = col.toLowerCase(Locale.ROOT);
        String sql =
            "SELECT setval(pg_get_serial_sequence('" + lcSchema + "." + lcTable + "', '" + lcCol + "'), " +
            "       COALESCE((SELECT MAX(" + ident(col) + ") FROM " + ident(pgSchema) + "." + ident(table) + "), 0) + 1, " +
            "       false)";
        try (Statement st = pg.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            logger.warning("[PostgresBackend]   " + table + "." + col
                + ": failed to reset identity sequence — " + e.getMessage());
        }
    }

    private static boolean pkColumnsAreNotNull(TableSpec spec) {
        for (String pkName : spec.primaryKey) {
            for (ColumnSpec c : spec.columns) {
                if (c.name.equalsIgnoreCase(pkName) && !c.notNull) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void createIndexes(Connection pg, String pgSchema, TableSpec spec) throws SQLException {
        for (IndexSpec idx : spec.indexes) {
            String idxName = (pgSchema + "_" + spec.name + "_" + idx.name).toLowerCase(Locale.ROOT);
            StringBuilder sql = new StringBuilder();
            sql.append("CREATE ").append(idx.unique ? "UNIQUE INDEX " : "INDEX ")
                .append("IF NOT EXISTS ").append(ident(idxName))
                .append(" ON ").append(ident(pgSchema)).append('.').append(ident(spec.name))
                .append(" (");
            for (int i = 0; i < idx.columns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(ident(idx.columns.get(i)));
            }
            sql.append(')');
            try (Statement st = pg.createStatement()) {
                st.execute(sql.toString());
            }
        }
    }

    private static long copyRows(Connection sq, Connection pg, String pgSchema, TableSpec spec) throws SQLException {
        StringBuilder select = new StringBuilder("SELECT ");
        StringBuilder insert = new StringBuilder("INSERT INTO ")
            .append(ident(pgSchema)).append('.').append(ident(spec.name)).append(" (");
        StringBuilder qs = new StringBuilder();
        for (int i = 0; i < spec.columns.size(); i++) {
            if (i > 0) { select.append(", "); insert.append(", "); qs.append(", "); }
            select.append('"').append(spec.columns.get(i).name).append('"');
            insert.append(ident(spec.columns.get(i).name));
            qs.append('?');
        }
        select.append(" FROM \"").append(spec.name).append('"');
        insert.append(") VALUES (").append(qs).append(')');

        long count = 0;
        try (Statement sel = sq.createStatement();
             ResultSet rs = sel.executeQuery(select.toString());
             PreparedStatement ps = pg.prepareStatement(insert.toString())) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            int inBatch = 0;
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    bind(ps, i, v, spec.columns.get(i - 1).pgType);
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

    private static void bind(PreparedStatement ps, int idx, Object v, String pgType) throws SQLException {
        if (v == null) {
            ps.setNull(idx, Types.NULL);
            return;
        }
        switch (pgType) {
            case "BIGINT":
                if (v instanceof Number) ps.setLong(idx, ((Number) v).longValue());
                else ps.setLong(idx, Long.parseLong(v.toString().trim()));
                break;
            case "BOOLEAN":
                if (v instanceof Boolean) ps.setBoolean(idx, (Boolean) v);
                else if (v instanceof Number) ps.setBoolean(idx, ((Number) v).intValue() != 0);
                else {
                    String s = v.toString().trim().toLowerCase(Locale.ROOT);
                    ps.setBoolean(idx, s.equals("1") || s.equals("true") || s.equals("t") || s.equals("yes"));
                }
                break;
            case "DOUBLE PRECISION":
            case "NUMERIC":
                if (v instanceof Number) ps.setDouble(idx, ((Number) v).doubleValue());
                else ps.setDouble(idx, Double.parseDouble(v.toString().trim()));
                break;
            case "BYTEA":
                if (v instanceof byte[]) ps.setBytes(idx, (byte[]) v);
                else ps.setBytes(idx, v.toString().getBytes());
                break;
            case "TIMESTAMP":
                ps.setTimestamp(idx, parseTimestamp(v));
                break;
            case "DATE":
                ps.setDate(idx, parseDate(v));
                break;
            case "TIME":
                ps.setTime(idx, parseTime(v));
                break;
            default: // TEXT
                ps.setString(idx, v.toString());
        }
    }

    private static java.sql.Timestamp parseTimestamp(Object v) {
        if (v instanceof java.sql.Timestamp) return (java.sql.Timestamp) v;
        if (v instanceof java.util.Date) return new java.sql.Timestamp(((java.util.Date) v).getTime());
        if (v instanceof Number) return new java.sql.Timestamp(((Number) v).longValue());
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        // numeric epoch (millis or seconds)
        try {
            long n = Long.parseLong(s);
            return new java.sql.Timestamp(s.length() <= 10 ? n * 1000L : n);
        } catch (NumberFormatException ignored) {}
        // ISO-ish: "yyyy-MM-dd HH:mm:ss[.fff]" or "yyyy-MM-ddTHH:mm:ss"
        String norm = s.replace('T', ' ');
        if (norm.length() == 10) norm = norm + " 00:00:00";
        return java.sql.Timestamp.valueOf(norm);
    }

    private static java.sql.Date parseDate(Object v) {
        if (v instanceof java.sql.Date) return (java.sql.Date) v;
        if (v instanceof java.util.Date) return new java.sql.Date(((java.util.Date) v).getTime());
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return new java.sql.Date(Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        return java.sql.Date.valueOf(s.length() >= 10 ? s.substring(0, 10) : s);
    }

    private static java.sql.Time parseTime(Object v) {
        if (v instanceof java.sql.Time) return (java.sql.Time) v;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        return java.sql.Time.valueOf(s);
    }

    // ------------------------------------------------------------------
    // Type mapping (mirrors the Python version)
    // ------------------------------------------------------------------

    private static final Pattern P_INT    = Pattern.compile("^(int|integer|bigint|tinyint|smallint|mediumint|int2|int4|int8)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_BOOL   = Pattern.compile("^(bool|boolean|bit)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_REAL   = Pattern.compile("^(real|float|double|double\\s+precision|float4|float8)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_NUM    = Pattern.compile("^(numeric|decimal)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_BYTES  = Pattern.compile("^(blob|bytea|binary|varbinary)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TEXT   = Pattern.compile("^(text|clob|varchar|char|nvarchar|nchar|character\\s+varying)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_TS     = Pattern.compile("^(timestamp|datetime)$", Pattern.CASE_INSENSITIVE);

    private static String mapType(String declared) {
        if (declared == null) return "TEXT";
        String t = declared.trim().replaceAll("\\s*\\(.*\\)$", "").replaceAll("^(?i)(unsigned|signed)\\s+", "").trim();
        if (t.isEmpty()) return "TEXT";
        if (P_INT.matcher(t).matches())   return "BIGINT";
        if (P_BOOL.matcher(t).matches())  return "BOOLEAN";
        if (P_REAL.matcher(t).matches())  return "DOUBLE PRECISION";
        if (P_NUM.matcher(t).matches())   return "NUMERIC";
        if (P_BYTES.matcher(t).matches()) return "BYTEA";
        if (P_TEXT.matcher(t).matches())  return "TEXT";
        if (P_TS.matcher(t).matches())    return "TIMESTAMP";
        if (t.equalsIgnoreCase("date"))   return "DATE";
        if (t.equalsIgnoreCase("time"))   return "TIME";
        return "TEXT";
    }

    /**
     * Rewrite a SQLite default-value literal into a Postgres-compatible form.
     * SQLite accepts double-quoted strings ({@code DEFAULT "foo"}); Postgres
     * would parse those as quoted identifiers and choke (especially on the
     * empty-string {@code ""} which is an invalid zero-length identifier).
     */
    private static String translateDefault(String dflt, String pgType) {
        if (dflt == null) return null;
        String t = dflt.trim();
        if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') {
            String inner = t.substring(1, t.length() - 1).replace("\"\"", "\"");
            return "'" + inner.replace("'", "''") + "'";
        }
        // Postgres BOOLEAN columns can't default to integer 0/1 literals —
        // SQLite stores booleans as int and the column DDL still says "DEFAULT 0".
        if ("BOOLEAN".equals(pgType)) {
            if ("0".equals(t)) return "false";
            if ("1".equals(t)) return "true";
        }
        return dflt;
    }

    private static String ident(String s) {
        String lc = s.toLowerCase(Locale.ROOT);
        return "\"" + lc.replace("\"", "\"\"") + "\"";
    }

    private static void ensureDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("sqlite-jdbc not on classpath — build error", e);
        }
    }

    // ------------------------------------------------------------------
    // Inner types
    // ------------------------------------------------------------------

    private static final class ColumnSpec {
        String name;
        String sqliteType;
        boolean notNull;
        String pgType;
        String defaultValue;
    }

    private static final class IndexSpec {
        String name;
        boolean unique;
        final List<String> columns = new ArrayList<>();
    }

    private static final class TableSpec {
        String name;
        final List<ColumnSpec> columns = new ArrayList<>();
        final List<String> primaryKey = new ArrayList<>();
        final List<IndexSpec> indexes = new ArrayList<>();
    }

    private static final class SchemaResult {
        int tables;
        long rows;
    }

    private SqliteImporter() {}
}
