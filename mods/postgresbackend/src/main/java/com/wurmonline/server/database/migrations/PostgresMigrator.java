package com.wurmonline.server.database.migrations;

import com.wurmonline.server.database.PostgresConnectionFactory;
import com.wurmonline.server.database.WurmDatabaseSchema;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.api.MigrationVersion;

/**
 * Flyway wrapper for a single Postgres schema. Reads SQL from
 * {@code migrations/<schema>/} on disk (the same layout Wurm already ships
 * for SQLite) and targets the matching Postgres schema via
 * {@code flyway.setSchemas()}.
 *
 * <p>If the schema has tables but no {@code SCHEMA_VERSION} row (the case
 * after an SQLite → Postgres import), baseline at the highest migration
 * version on disk so Flyway treats imported data as already-migrated.</p>
 */
public final class PostgresMigrator extends Migrator<PostgresConnectionFactory> {

    private static final Pattern VERSION_PREFIX = Pattern.compile("^v(\\d+)__", Pattern.CASE_INSENSITIVE);

    private final WurmDatabaseSchema schema;

    public PostgresMigrator(PostgresConnectionFactory factory) {
        super(factory, Collections.singletonList(migrationsDir(factory.getSchema())), flyway -> {
            // Flyway's default classloader is the one that loaded Flyway (Wurm's
            // classpath). pgjdbc lives in the mod URLClassLoader, so without this
            // Flyway's DriverDataSource can't find org.postgresql.Driver.
            flyway.setClassLoader(PostgresMigrator.class.getClassLoader());
            flyway.setDataSource(factory.getUrl(), factory.getUser(), factory.getPassword(), new String[0]);
            flyway.setSchemas(factory.getSchemaName());
            int head = scanHeadVersion(migrationsDir(factory.getSchema()));
            if (head > 0) {
                flyway.setBaselineOnMigrate(true);
                flyway.setBaselineVersion(MigrationVersion.fromVersion(String.valueOf(head)));
            }
        });
        this.schema = factory.getSchema();
    }

    public WurmDatabaseSchema getSchema() {
        return schema;
    }

    private static Path migrationsDir(WurmDatabaseSchema schema) {
        return new File("migrations/" + schema.getMigration()).toPath();
    }

    private static int scanHeadVersion(Path dir) {
        File d = dir.toFile();
        if (!d.isDirectory()) return 0;
        File[] files = d.listFiles();
        if (files == null) return 0;
        int max = 0;
        for (File f : files) {
            Matcher m = VERSION_PREFIX.matcher(f.getName());
            if (m.find()) {
                try {
                    int v = Integer.parseInt(m.group(1));
                    if (v > max) max = v;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }
}
