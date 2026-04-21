package com.garward.wurmmodloader.api.database.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One-shot bulk copy from one or more vanilla-WU SQLite files into the
 * registered {@link com.garward.wurmmodloader.api.database.DatabaseBackend}'s
 * target connections.
 *
 * <h2>Scope</h2>
 * <ul>
 *   <li>Enumerates every user table in the source, excluding SQLite/Flyway
 *       metadata ({@code sqlite_sequence}, {@code flyway_schema_history},
 *       {@code schema_version}). Opt in/out per source via
 *       {@link Builder#skipTables(Set)} / {@link Builder#onlyTables(Set)}.</li>
 *   <li>Assumes the target schema already exists (Flyway-driven or hand-built)
 *       with matching table / column names. This is a data-copy, not a DDL
 *       generator.</li>
 *   <li>Introspects <b>target</b> column metadata to type-coerce SQLite's
 *       loose affinity into stricter JDBC types — see {@link CoercionPolicy}.</li>
 *   <li>Wraps each table in a transaction on the target, batches inserts
 *       ({@link Builder#batchSize(int)}, default 1000). No cross-table
 *       referential-integrity guarantee — disable FK checks upstream if your
 *       target enforces them mid-migration.</li>
 * </ul>
 *
 * <h2>Non-goals</h2>
 * <ul>
 *   <li>Live migration. Stop the server, copy the SQLite files to a safe
 *       location, point this utility at the copies.</li>
 *   <li>Incremental / resumable copy. A failed table aborts only that table;
 *       rerunning will re-insert rows unless the target enforces uniqueness.</li>
 *   <li>Schema translation. Drop / rename / retype should happen in your
 *       Flyway migrations, not here.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MigrationReport report = DatabaseMigrator.builder()
 *     .addSource("players", DriverManager.getConnection("jdbc:sqlite:wurmplayers.db"),
 *                () -> backendFactoryFor(PLAYERS).createConnection())
 *     .addSource("items",   DriverManager.getConnection("jdbc:sqlite:wurmitems.db"),
 *                () -> backendFactoryFor(ITEMS).createConnection())
 *     .coercion(CoercionPolicy.LENIENT)
 *     .batchSize(500)
 *     .build()
 *     .run();
 *
 * if (report.hasFailures()) {
 *     report.failures().forEach(System.err::println);
 * }
 * }</pre>
 *
 * <p>The caller owns the source / target {@link Connection}s; the migrator
 * does not close them. Each source is processed sequentially.</p>
 *
 * @since 1.0.0
 */
public final class DatabaseMigrator {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigrator.class.getName());

    /** SQLite + Flyway metadata tables that are always skipped. */
    private static final Set<String> DEFAULT_SKIP;
    static {
        Set<String> s = new HashSet<>();
        s.add("sqlite_sequence");
        s.add("sqlite_stat1");
        s.add("sqlite_stat4");
        s.add("flyway_schema_history");
        s.add("schema_version");
        DEFAULT_SKIP = Collections.unmodifiableSet(s);
    }

    public interface TargetConnectionSupplier {
        Connection get() throws SQLException;
    }

    private static final class Source {
        final String label;
        final Connection source;
        final TargetConnectionSupplier target;
        final Set<String> onlyTables;
        final Set<String> extraSkip;

        Source(String label, Connection source, TargetConnectionSupplier target,
               Set<String> onlyTables, Set<String> extraSkip) {
            this.label = label;
            this.source = source;
            this.target = target;
            this.onlyTables = onlyTables;
            this.extraSkip = extraSkip;
        }
    }

    public static final class Builder {
        private final List<Source> sources = new ArrayList<>();
        private CoercionPolicy coercion = CoercionPolicy.LENIENT;
        private int batchSize = 1000;
        private Set<String> globalOnly;
        private Set<String> globalSkip = new HashSet<>();

        public Builder addSource(String label, Connection sqliteSource,
                                 TargetConnectionSupplier targetSupplier) {
            return addSource(label, sqliteSource, targetSupplier, null, null);
        }

        public Builder addSource(String label, Connection sqliteSource,
                                 TargetConnectionSupplier targetSupplier,
                                 Set<String> onlyTables, Set<String> skipTables) {
            if (label == null || sqliteSource == null || targetSupplier == null) {
                throw new IllegalArgumentException("label, sqliteSource, targetSupplier required");
            }
            sources.add(new Source(label, sqliteSource, targetSupplier,
                onlyTables == null ? null : lowercase(onlyTables),
                skipTables == null ? Collections.emptySet() : lowercase(skipTables)));
            return this;
        }

        public Builder coercion(CoercionPolicy p) { this.coercion = p; return this; }
        public Builder batchSize(int n) { this.batchSize = Math.max(1, n); return this; }
        public Builder onlyTables(Set<String> tables) { this.globalOnly = lowercase(tables); return this; }
        public Builder skipTables(Set<String> tables) { this.globalSkip = lowercase(tables); return this; }

        public DatabaseMigrator build() {
            if (sources.isEmpty()) throw new IllegalStateException("no sources added");
            return new DatabaseMigrator(this);
        }
    }

    public static Builder builder() { return new Builder(); }

    private final List<Source> sources;
    private final CoercionPolicy coercion;
    private final int batchSize;
    private final Set<String> globalOnly;
    private final Set<String> globalSkip;

    private DatabaseMigrator(Builder b) {
        this.sources = new ArrayList<>(b.sources);
        this.coercion = b.coercion;
        this.batchSize = b.batchSize;
        this.globalOnly = b.globalOnly;
        this.globalSkip = b.globalSkip;
    }

    public MigrationReport run() {
        MigrationReport report = new MigrationReport();
        for (Source s : sources) {
            LOGGER.info("[DatabaseMigrator] Starting source: " + s.label);
            List<String> tables;
            try {
                tables = enumerateTables(s.source);
            } catch (SQLException e) {
                report.add(new MigrationReport.Entry(s.label, "<enumerate>",
                    0, 0, 0, 0, e));
                continue;
            }
            for (String table : tables) {
                String lower = table.toLowerCase(Locale.ROOT);
                if (DEFAULT_SKIP.contains(lower)) continue;
                if (globalSkip.contains(lower)) continue;
                if (s.extraSkip.contains(lower)) continue;
                if (globalOnly != null && !globalOnly.contains(lower)) continue;
                if (s.onlyTables != null && !s.onlyTables.contains(lower)) continue;
                migrateTable(s, table, report);
            }
        }
        return report;
    }

    private List<String> enumerateTables(Connection source) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement st = source.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    private void migrateTable(Source s, String table, MigrationReport report) {
        long read = 0, inserted = 0, coerced = 0, skipped = 0;
        Connection target = null;
        boolean savedAutoCommit = true;
        try {
            target = s.target.get();
            savedAutoCommit = target.getAutoCommit();
            target.setAutoCommit(false);

            Map<String, ColumnSpec> targetCols = introspectTarget(target, table);
            if (targetCols.isEmpty()) {
                report.add(new MigrationReport.Entry(s.label, table, 0, 0, 0, 0,
                    new SQLException("target table '" + table + "' has no columns (missing?)")));
                return;
            }

            try (Statement srcStmt = s.source.createStatement();
                 ResultSet rs = srcStmt.executeQuery("SELECT * FROM \"" + table + "\"")) {
                ResultSetMetaData md = rs.getMetaData();
                List<String> colNames = new ArrayList<>();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    String c = md.getColumnName(i);
                    if (targetCols.containsKey(c.toLowerCase(Locale.ROOT))) {
                        colNames.add(c);
                    } else {
                        LOGGER.fine("[DatabaseMigrator] " + s.label + "." + table
                            + " source column '" + c + "' not in target; dropping");
                    }
                }
                if (colNames.isEmpty()) {
                    report.add(new MigrationReport.Entry(s.label, table, 0, 0, 0, 0,
                        new SQLException("no overlapping columns between source and target")));
                    return;
                }

                String insert = buildInsert(table, colNames);
                try (PreparedStatement ps = target.prepareStatement(insert)) {
                    int pending = 0;
                    while (rs.next()) {
                        read++;
                        for (int i = 0; i < colNames.size(); i++) {
                            String col = colNames.get(i);
                            Object raw = rs.getObject(col);
                            ColumnSpec spec = targetCols.get(col.toLowerCase(Locale.ROOT));
                            CoerceResult cr = coerce(raw, spec);
                            if (cr.coerced) coerced++;
                            if (cr.value == null && raw != null && !spec.nullable) {
                                skipped++;
                                ps.clearParameters();
                                pending = -1;
                                break;
                            }
                            if (cr.value == null) ps.setNull(i + 1, spec.jdbcType);
                            else ps.setObject(i + 1, cr.value, spec.jdbcType);
                        }
                        if (pending < 0) { pending = 0; continue; }
                        ps.addBatch();
                        pending++;
                        if (pending >= batchSize) {
                            inserted += flush(ps);
                            pending = 0;
                        }
                    }
                    if (pending > 0) inserted += flush(ps);
                }
            }
            target.commit();
            report.add(new MigrationReport.Entry(s.label, table, read, inserted, coerced, skipped, null));
            LOGGER.info(String.format(
                "[DatabaseMigrator] %s.%s copied read=%d inserted=%d coerced=%d skipped=%d",
                s.label, table, read, inserted, coerced, skipped));
        } catch (Throwable t) {
            if (target != null) {
                try { target.rollback(); } catch (SQLException ignored) {}
            }
            report.add(new MigrationReport.Entry(s.label, table, read, inserted, coerced, skipped, t));
            LOGGER.log(Level.WARNING, "[DatabaseMigrator] " + s.label + "." + table + " failed", t);
        } finally {
            if (target != null) {
                try { target.setAutoCommit(savedAutoCommit); } catch (SQLException ignored) {}
            }
        }
    }

    private long flush(PreparedStatement ps) throws SQLException {
        int[] counts = ps.executeBatch();
        long n = 0;
        for (int c : counts) if (c >= 0) n += c; else n++;
        return n;
    }

    private String buildInsert(String table, List<String> cols) {
        StringBuilder sb = new StringBuilder("INSERT INTO \"").append(table).append("\" (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(cols.get(i)).append('"');
        }
        sb.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        sb.append(')');
        return sb.toString();
    }

    private static final class ColumnSpec {
        final int jdbcType;
        final boolean nullable;
        ColumnSpec(int t, boolean n) { this.jdbcType = t; this.nullable = n; }
    }

    private Map<String, ColumnSpec> introspectTarget(Connection target, String table) throws SQLException {
        Map<String, ColumnSpec> out = new LinkedHashMap<>();
        DatabaseMetaData md = target.getMetaData();
        String[] variants = new String[]{table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)};
        Set<String> tried = new TreeSet<>();
        for (String variant : variants) {
            if (!tried.add(variant)) continue;
            try (ResultSet rs = md.getColumns(null, null, variant, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    int type = rs.getInt("DATA_TYPE");
                    boolean nullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                    out.put(name.toLowerCase(Locale.ROOT), new ColumnSpec(type, nullable));
                }
            }
            if (!out.isEmpty()) return out;
        }
        return out;
    }

    private static final class CoerceResult {
        final Object value;
        final boolean coerced;
        CoerceResult(Object v, boolean c) { this.value = v; this.coerced = c; }
    }

    private CoerceResult coerce(Object raw, ColumnSpec spec) {
        if (raw == null) return new CoerceResult(null, false);
        if (coercion == CoercionPolicy.STRICT) return new CoerceResult(raw, false);
        if (!(raw instanceof String)) return new CoerceResult(raw, false);
        String s = ((String) raw).trim();
        switch (spec.jdbcType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.BIGINT:
                if (s.isEmpty()) return new CoerceResult(null, true);
                try { return new CoerceResult(Long.parseLong(s), true); }
                catch (NumberFormatException e) { return new CoerceResult(null, true); }
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.DECIMAL:
            case Types.NUMERIC:
                if (s.isEmpty()) return new CoerceResult(null, true);
                try { return new CoerceResult(Double.parseDouble(s), true); }
                catch (NumberFormatException e) { return new CoerceResult(null, true); }
            case Types.BOOLEAN:
            case Types.BIT:
                if (s.isEmpty()) return new CoerceResult(null, true);
                if ("1".equals(s) || "true".equalsIgnoreCase(s)) return new CoerceResult(Boolean.TRUE, true);
                if ("0".equals(s) || "false".equalsIgnoreCase(s)) return new CoerceResult(Boolean.FALSE, true);
                return new CoerceResult(null, true);
            default:
                return new CoerceResult(raw, false);
        }
    }

    private static Set<String> lowercase(Set<String> in) {
        Set<String> out = new HashSet<>();
        for (String s : in) out.add(s.toLowerCase(Locale.ROOT));
        return out;
    }

}
