package com.garward.wurmmodloader.api.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates vanilla-WU SQL fragments into the target {@link Dialect}.
 *
 * <p>WU hardcodes SQLite-isms (all 16 known sites are {@code INSERT OR IGNORE})
 * and MySQL-isms ({@code INSERT IGNORE}) in {@code static final String} fields.
 * A custom backend whose dialect is neither SQLite nor MySQL needs those
 * rewritten before they hit the driver. Rather than patch 16 call sites, the
 * backend wraps its {@link Connection} via {@link #wrap(Connection, Dialect)}
 * and every {@code prepareStatement(...)} / {@code executeUpdate(...)} string
 * passes through {@link #rewrite(String, Dialect)} first.</p>
 *
 * <h2>Known rewrites</h2>
 * <ul>
 *   <li>{@code INSERT OR IGNORE INTO <tbl> ... VALUES (...)} →
 *       {@code INSERT INTO <tbl> ... VALUES (...) ON CONFLICT DO NOTHING} (Postgres)</li>
 *   <li>{@code INSERT OR IGNORE INTO <tbl> ... VALUES (...)} →
 *       {@code INSERT IGNORE INTO <tbl> ... VALUES (...)} (MySQL / MariaDB)</li>
 *   <li>{@code INSERT IGNORE INTO <tbl> (SELECT ...)} →
 *       {@code INSERT INTO <tbl> SELECT ... ON CONFLICT DO NOTHING} (Postgres)</li>
 *   <li>{@code INSERT OR IGNORE INTO <tbl> SELECT ...} →
 *       {@code INSERT INTO <tbl> SELECT ... ON CONFLICT DO NOTHING} (Postgres)</li>
 *   <li>{@code INSERT OR REPLACE INTO <tbl> (<col1>, <col2>, ...) VALUES (...)} →
 *       {@code INSERT INTO <tbl> (<col1>, <col2>, ...) VALUES (...) ON CONFLICT (<col1>)
 *       DO UPDATE SET <col2> = EXCLUDED.<col2>, ...} (Postgres). The conflict
 *       target is assumed to be the <b>first</b> column — matches the convention
 *       used across WU and its modded SQL. Composite-PK or non-first-column-PK
 *       tables must be rewritten by hand.</li>
 * </ul>
 *
 * <p>{@link Dialect#SQLITE}, {@link Dialect#CUSTOM}, and unknown strings pass
 * through untouched.</p>
 *
 * @since 1.0.0
 */
public final class SqlDialectRewriter {

    private static final Pattern INSERT_OR_IGNORE =
        Pattern.compile("(?i)\\bINSERT\\s+OR\\s+IGNORE\\b");
    /**
     * Matches {@code INSERT OR REPLACE INTO <tbl> (<cols>) VALUES (<vals>)}.
     * Group 1 = table, group 2 = column list, group 3 = value list.
     */
    private static final Pattern INSERT_OR_REPLACE_COLS =
        Pattern.compile("(?is)\\bINSERT\\s+OR\\s+REPLACE\\s+INTO\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)");
    /** Bare {@code INSERT OR REPLACE} (no column list) — we can't rewrite without knowing the PK. */
    private static final Pattern INSERT_OR_REPLACE_BARE =
        Pattern.compile("(?i)\\bINSERT\\s+OR\\s+REPLACE\\b");
    private static final Pattern INSERT_IGNORE =
        Pattern.compile("(?i)\\bINSERT\\s+IGNORE\\b");
    private static final Pattern INSERT_IGNORE_SELECT_PARENS =
        Pattern.compile("(?i)\\bINSERT\\s+IGNORE\\s+INTO\\s+(\\w+)\\s*\\(\\s*SELECT\\b(.*?)\\)\\s*$",
            Pattern.DOTALL);
    /**
     * Targets SQLite's {@code MAX(literal, expr)} scalar usage (e.g.
     * {@code LocalSupplyDemand.INCREASE_ALL_DEMANDS}) without touching the
     * aggregate {@code MAX(column)} form. The literal-first heuristic is
     * deliberate: aggregates never start with a numeric constant.
     */
    private static final Pattern MAX_SCALAR_LITERAL_FIRST =
        Pattern.compile("(?i)\\bMAX\\(\\s*(-?\\d+(?:\\.\\d+)?)\\s*,");
    private static final Pattern PURGEBADACTIONS_MYSQL =
        Pattern.compile(
            "(?i)DELETE\\s+FROM\\s+TICKETACTIONS\\s+USING\\s+TICKETACTIONS\\s+"
            + "LEFT\\s+JOIN\\s+TICKETS\\s+ON\\s+TICKETS\\.TICKETID\\s*=\\s*"
            + "TICKETACTIONS\\.TICKETID\\s+WHERE\\s+TICKETS\\.TICKETID\\s+IS\\s+NULL\\s*;?\\s*",
            Pattern.DOTALL);

    private SqlDialectRewriter() {}

    /**
     * Translate {@code sql} for the target {@code dialect}. Returns the original
     * string unchanged if no known pattern matches or if {@code dialect} opts out.
     */
    public static String rewrite(String sql, Dialect dialect) {
        if (sql == null || dialect == null) return sql;
        switch (dialect) {
            case SQLITE:
            case CUSTOM:
                return sql;
            case MYSQL:
            case MARIADB:
                return rewriteForMysql(sql);
            case POSTGRES:
                return rewriteForPostgres(sql);
            default:
                return sql;
        }
    }

    private static String rewriteForMysql(String sql) {
        Matcher m = INSERT_OR_IGNORE.matcher(sql);
        String out = m.find() ? m.replaceAll("INSERT IGNORE") : sql;
        // MySQL's equivalent of "INSERT OR REPLACE" is just "REPLACE INTO",
        // which has the same semantics (delete-then-insert on PK/UNIQUE conflict).
        if (INSERT_OR_REPLACE_BARE.matcher(out).find()) {
            out = INSERT_OR_REPLACE_BARE.matcher(out).replaceAll("REPLACE");
        }
        return out;
    }

    /**
     * Translate a matched {@code INSERT OR REPLACE INTO <tbl> (<cols>) VALUES (<vals>)}
     * into Postgres upsert form. The first column is assumed to be the conflict
     * target — see the class javadoc for the rationale and limitation.
     */
    private static String rewriteOrReplaceForPostgres(String fullSql, Matcher m) {
        String table = m.group(1);
        String[] cols = splitAndTrim(m.group(2));
        String values = m.group(3).trim();

        if (cols.length == 0) return fullSql; // malformed — let driver complain

        String conflictCol = cols[0];
        StringBuilder setClause = new StringBuilder();
        for (int i = 1; i < cols.length; i++) {
            if (setClause.length() > 0) setClause.append(", ");
            setClause.append(cols[i]).append(" = EXCLUDED.").append(cols[i]);
        }

        String rewritten;
        if (setClause.length() == 0) {
            // Single-column insert — nothing to update on conflict.
            rewritten = "INSERT INTO " + table + " (" + String.join(", ", cols)
                + ") VALUES (" + values + ") ON CONFLICT (" + conflictCol + ") DO NOTHING";
        } else {
            rewritten = "INSERT INTO " + table + " (" + String.join(", ", cols)
                + ") VALUES (" + values + ") ON CONFLICT (" + conflictCol
                + ") DO UPDATE SET " + setClause;
        }

        // Preserve anything the caller appended after the matched region
        // (e.g. trailing semicolon or RETURNING clause).
        String tail = fullSql.substring(m.end()).trim();
        if (!tail.isEmpty()) {
            rewritten = rewritten + " " + tail;
        }
        return rewritten;
    }

    private static String[] splitAndTrim(String csv) {
        String[] parts = csv.split(",");
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }

    private static String rewriteForPostgres(String sql) {
        String out = sql;

        if (PURGEBADACTIONS_MYSQL.matcher(out).matches()) {
            return "DELETE FROM TICKETACTIONS WHERE NOT EXISTS ("
                + "SELECT 1 FROM TICKETS WHERE TICKETS.TICKETID = TICKETACTIONS.TICKETID)";
        }

        Matcher orReplace = INSERT_OR_REPLACE_COLS.matcher(out);
        if (orReplace.find()) {
            return rewriteOrReplaceForPostgres(out, orReplace);
        }
        // INSERT OR REPLACE without a column list can't be rewritten safely —
        // we don't know the conflict target. Let it fall through; Postgres
        // will error and the caller will see it clearly.

        Matcher parens = INSERT_IGNORE_SELECT_PARENS.matcher(out);
        if (parens.find()) {
            out = "INSERT INTO " + parens.group(1) + " SELECT" + parens.group(2)
                + " ON CONFLICT DO NOTHING";
            return out;
        }
        if (INSERT_OR_IGNORE.matcher(out).find()) {
            out = INSERT_OR_IGNORE.matcher(out).replaceAll("INSERT");
            if (!out.toUpperCase().contains("ON CONFLICT")) {
                out = out.trim();
                if (out.endsWith(";")) {
                    out = out.substring(0, out.length() - 1) + " ON CONFLICT DO NOTHING;";
                } else {
                    out = out + " ON CONFLICT DO NOTHING";
                }
            }
        } else if (INSERT_IGNORE.matcher(out).find()) {
            out = INSERT_IGNORE.matcher(out).replaceAll("INSERT");
            if (!out.toUpperCase().contains("ON CONFLICT")) {
                out = out + " ON CONFLICT DO NOTHING";
            }
        }

        if (MAX_SCALAR_LITERAL_FIRST.matcher(out).find()) {
            out = MAX_SCALAR_LITERAL_FIRST.matcher(out).replaceAll("GREATEST($1,");
        }

        return out;
    }

    /**
     * Wrap a JDBC {@link Connection} so every SQL string passed to
     * {@code prepareStatement}, {@code prepareCall}, and the
     * {@code Statement.execute*(String, ...)} family is rewritten for
     * {@code dialect} before hitting the driver.
     *
     * <p>{@link Dialect#CUSTOM} and {@link Dialect#SQLITE} return the connection
     * unwrapped (nothing to rewrite). All other dialects return a JDK dynamic
     * proxy delegating to the original connection.</p>
     *
     * @param connection the raw driver connection (must not be null)
     * @param dialect    the dialect to rewrite for
     * @return the connection, possibly wrapped
     */
    public static Connection wrap(Connection connection, Dialect dialect) {
        if (connection == null) return null;
        if (dialect == null || dialect == Dialect.CUSTOM || dialect == Dialect.SQLITE) {
            return connection;
        }
        return (Connection) Proxy.newProxyInstance(
            SqlDialectRewriter.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new RewritingHandler(connection, dialect)
        );
    }

    private static final class RewritingHandler implements InvocationHandler {
        private final Connection delegate;
        private final Dialect dialect;

        RewritingHandler(Connection delegate, Dialect dialect) {
            this.delegate = delegate;
            this.dialect = dialect;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0) {
                String name = method.getName();
                if (("prepareStatement".equals(name) || "prepareCall".equals(name))
                    && args[0] instanceof String) {
                    args[0] = rewrite((String) args[0], dialect);
                }
            }
            Object result = method.invoke(delegate, args);
            if (result instanceof Statement && !(result instanceof PreparedStatement)) {
                return Proxy.newProxyInstance(
                    SqlDialectRewriter.class.getClassLoader(),
                    new Class<?>[]{Statement.class},
                    new StatementRewritingHandler((Statement) result, dialect));
            }
            return result;
        }
    }

    private static final class StatementRewritingHandler implements InvocationHandler {
        private final Statement delegate;
        private final Dialect dialect;

        StatementRewritingHandler(Statement delegate, Dialect dialect) {
            this.delegate = delegate;
            this.dialect = dialect;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof String) {
                String name = method.getName();
                if (name.startsWith("execute") || "addBatch".equals(name)) {
                    args[0] = rewrite((String) args[0], dialect);
                }
            }
            return method.invoke(delegate, args);
        }
    }
}
