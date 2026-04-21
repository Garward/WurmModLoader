package com.garward.wurmmodloader.api.database;

import java.lang.reflect.Method;

/**
 * Runtime helper for extracting the {@code WurmDatabaseSchema} from an
 * arbitrary {@code Migrator} subclass without a compile-time dependency on
 * the subclass type. Used by the bytecode-injected code in
 * {@code DbMigrationPatch} so third-party DatabaseBackend implementations
 * (e.g. PostgresMigrator) can participate in migration events without being
 * known to the framework.
 */
public final class MigratorSchemaReflector {

    private MigratorSchemaReflector() {}

    public static Object resolve(Object migrator) {
        if (migrator == null) return null;
        try {
            Method m = migrator.getClass().getMethod("getSchema");
            return m.invoke(migrator);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
