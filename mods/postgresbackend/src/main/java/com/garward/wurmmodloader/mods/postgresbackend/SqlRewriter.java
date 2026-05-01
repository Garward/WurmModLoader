package com.garward.wurmmodloader.mods.postgresbackend;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-processes vanilla Wurm SQL strings before they hit the Postgres driver.
 *
 * <p>Wurm's hand-rolled {@code *DbStrings} classes assemble UPDATE statements by
 * appending fragments and occasionally emit the same column twice in a single
 * SET clause (e.g. {@code SET ..., CREATIONSTATE=0, ..., CREATIONSTATE=?}).
 * SQLite tolerates this — last assignment wins. Postgres rejects it with
 * {@code ERROR: multiple assignments to same column}.
 *
 * <p>The rewriter is intentionally narrow: it only touches UPDATE statements
 * with duplicate column names in their top-level SET clause, and preserves the
 * <em>last</em> assignment in original order — matching SQLite's behaviour and
 * keeping the parameter index unchanged for the JDBC binding code that runs
 * afterwards. Anything more invasive (DDL rewriting, AUTOINCREMENT swaps, etc.)
 * stays in the dialect layer above.
 */
public final class SqlRewriter {

    private SqlRewriter() {}

    private static final Pattern UPDATE_SET = Pattern.compile(
            "(?is)^\\s*UPDATE\\s+.+?\\s+SET\\s+");

    /**
     * Returns {@code sql} unchanged unless it is an UPDATE statement whose SET
     * clause assigns the same column more than once, in which case all but the
     * last assignment for each duplicated column are dropped.
     *
     * <p>The method is null-safe and never throws — on any unexpected shape it
     * returns the original string so the driver sees exactly what Wurm built.
     */
    public static String rewrite(String sql) {
        if (sql == null) return null;
        try {
            Matcher m = UPDATE_SET.matcher(sql);
            if (!m.find()) return sql;
            int setStart = m.end();
            int setEnd = findSetEnd(sql, setStart);
            String setClause = sql.substring(setStart, setEnd);
            List<String> assignments = splitTopLevel(setClause);
            String rewritten = dedupeKeepLast(assignments);
            if (rewritten == null) return sql;            // no dupes
            return sql.substring(0, setStart) + rewritten + sql.substring(setEnd);
        } catch (RuntimeException unexpected) {
            return sql;
        }
    }

    /** Locates the index after the SET clause: the next top-level WHERE/RETURNING/end. */
    private static int findSetEnd(String sql, int from) {
        int depth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        int len = sql.length();
        for (int i = from; i < len; i++) {
            char c = sql.charAt(i);
            if (inSingle) {
                if (c == '\'' && (i + 1 >= len || sql.charAt(i + 1) != '\'')) inSingle = false;
                else if (c == '\'') i++;
                continue;
            }
            if (inDouble) {
                if (c == '"') inDouble = false;
                continue;
            }
            if (c == '\'') { inSingle = true; continue; }
            if (c == '"')  { inDouble = true; continue; }
            if (c == '(')  { depth++; continue; }
            if (c == ')')  { if (depth > 0) depth--; continue; }
            if (depth == 0 && (c == 'W' || c == 'w') && matchesKeyword(sql, i, "WHERE")) return i;
            if (depth == 0 && (c == 'R' || c == 'r') && matchesKeyword(sql, i, "RETURNING")) return i;
            if (depth == 0 && c == ';') return i;
        }
        return len;
    }

    private static boolean matchesKeyword(String s, int at, String kw) {
        int kl = kw.length();
        if (at + kl > s.length()) return false;
        for (int i = 0; i < kl; i++) {
            if (Character.toUpperCase(s.charAt(at + i)) != kw.charAt(i)) return false;
        }
        if (at > 0 && isIdentChar(s.charAt(at - 1))) return false;
        if (at + kl < s.length() && isIdentChar(s.charAt(at + kl))) return false;
        return true;
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Splits a SET clause at top-level commas, ignoring commas inside (), '', "". */
    private static List<String> splitTopLevel(String clause) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        int start = 0;
        for (int i = 0; i < clause.length(); i++) {
            char c = clause.charAt(i);
            if (inSingle) {
                if (c == '\'' && (i + 1 >= clause.length() || clause.charAt(i + 1) != '\'')) inSingle = false;
                else if (c == '\'') i++;
                continue;
            }
            if (inDouble) { if (c == '"') inDouble = false; continue; }
            if (c == '\'') { inSingle = true; continue; }
            if (c == '"')  { inDouble = true; continue; }
            if (c == '(')  { depth++; continue; }
            if (c == ')')  { if (depth > 0) depth--; continue; }
            if (c == ',' && depth == 0) {
                out.add(clause.substring(start, i));
                start = i + 1;
            }
        }
        out.add(clause.substring(start));
        return out;
    }

    /**
     * Returns a rebuilt SET clause keeping only the last occurrence of each
     * duplicated column, or {@code null} if no duplicates were found.
     */
    private static String dedupeKeepLast(List<String> assignments) {
        // Map column-name → last index it appeared at.
        LinkedHashMap<String, Integer> lastIdx = new LinkedHashMap<>();
        boolean dupeFound = false;
        for (int i = 0; i < assignments.size(); i++) {
            String col = columnOf(assignments.get(i));
            if (col == null) continue;                  // unparseable — skip dedup, keep
            if (lastIdx.containsKey(col)) dupeFound = true;
            lastIdx.put(col, i);
        }
        if (!dupeFound) return null;
        boolean[] keep = new boolean[assignments.size()];
        for (int idx : lastIdx.values()) keep[idx] = true;
        // Anything we couldn't parse a column name from also gets kept.
        for (int i = 0; i < assignments.size(); i++) {
            if (columnOf(assignments.get(i)) == null) keep[i] = true;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < assignments.size(); i++) {
            if (!keep[i]) continue;
            if (!first) sb.append(',');
            sb.append(assignments.get(i));
            first = false;
        }
        return sb.toString();
    }

    /** Extracts the column name from an assignment fragment like " CREATIONSTATE = ? ". */
    private static String columnOf(String assignment) {
        int eq = assignment.indexOf('=');
        if (eq < 0) return null;
        String left = assignment.substring(0, eq).trim();
        if (left.isEmpty()) return null;
        // Strip optional table qualifier "T.COL".
        int dot = left.lastIndexOf('.');
        if (dot >= 0 && dot < left.length() - 1) left = left.substring(dot + 1);
        // Quoted identifier?
        if (left.startsWith("\"") && left.endsWith("\"") && left.length() >= 2) {
            return left.substring(1, left.length() - 1);
        }
        for (int i = 0; i < left.length(); i++) {
            if (!isIdentChar(left.charAt(i))) return null;
        }
        return left.toUpperCase();
    }
}
