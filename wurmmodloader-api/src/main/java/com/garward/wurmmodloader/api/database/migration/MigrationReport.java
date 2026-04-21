package com.garward.wurmmodloader.api.database.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-table outcome of a {@link DatabaseMigrator} run.
 *
 * @since 1.0.0
 */
public final class MigrationReport {

    public static final class Entry {
        public final String sourceLabel;
        public final String table;
        public final long rowsRead;
        public final long rowsInserted;
        public final long rowsCoerced;
        public final long rowsSkipped;
        public final Throwable error;

        Entry(String sourceLabel, String table, long rowsRead, long rowsInserted,
              long rowsCoerced, long rowsSkipped, Throwable error) {
            this.sourceLabel = sourceLabel;
            this.table = table;
            this.rowsRead = rowsRead;
            this.rowsInserted = rowsInserted;
            this.rowsCoerced = rowsCoerced;
            this.rowsSkipped = rowsSkipped;
            this.error = error;
        }

        public boolean isSuccess() { return error == null; }

        @Override
        public String toString() {
            String tail = error == null ? "" : " FAILED: " + error;
            return String.format(
                "[%s.%s] read=%d inserted=%d coerced=%d skipped=%d%s",
                sourceLabel, table, rowsRead, rowsInserted, rowsCoerced, rowsSkipped, tail);
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    void add(Entry e) { entries.add(e); }

    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }

    public long totalRowsInserted() {
        long n = 0;
        for (Entry e : entries) n += e.rowsInserted;
        return n;
    }

    public boolean hasFailures() {
        for (Entry e : entries) if (!e.isSuccess()) return true;
        return false;
    }

    public List<Entry> failures() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) if (!e.isSuccess()) out.add(e);
        return out;
    }
}
