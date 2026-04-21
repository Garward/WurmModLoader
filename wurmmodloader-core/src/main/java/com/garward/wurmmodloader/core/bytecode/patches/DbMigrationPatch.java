package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.logging.Logger;
import java.util.Collection;
import java.util.Collections;

/**
 * Bytecode patch that fires
 * {@link com.garward.wurmmodloader.api.events.database.DatabaseMigrationStartingEvent}
 * and
 * {@link com.garward.wurmmodloader.api.events.database.DatabaseMigrationCompletedEvent}
 * around the {@code migrate()} methods of vanilla per-schema migrators
 * ({@code SqliteMigrator}, {@code MysqlMigrator}).
 *
 * <p>Events fire <b>per schema</b>. For SQLite, {@code SqliteMigrationStrategy} constructs one
 * {@code SqliteMigrator} per {@code WurmDatabaseSchema} and calls each in turn — each call
 * fires a starting/completed pair with the real schema (via {@code SqliteMigrator.getSchema()}).
 * For MySQL, vanilla uses a single {@code MysqlMigrator} that drives Flyway across every schema
 * in one call (Flyway's multi-schema mode); the events fire once with
 * {@code MysqlMigrationStrategy.MIGRATION_SCHEMA} (i.e. {@code WurmDatabaseSchema.LOGIN}) as the
 * representative schema, because vanilla provides no per-schema boundary on the MySQL path.</p>
 *
 * <p>The starting event fires at method entry. The completed event fires via
 * {@code insertAfter} without the {@code asFinally} flag, so it only runs on normal return —
 * if migration throws, completed does <b>not</b> fire, preserving the "success only" contract.</p>
 */
public final class DbMigrationPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(DbMigrationPatch.class.getName());

    private static final String MIGRATOR        = "com.wurmonline.server.database.migrations.Migrator";
    private static final String SQLITE_MIGRATOR = "com.wurmonline.server.database.migrations.SqliteMigrator";
    private static final String MYSQL_MIGRATOR  = "com.wurmonline.server.database.migrations.MysqlMigrator";
    private static final String MIGRATE_DESCRIPTOR = "()Lcom/wurmonline/server/database/migrations/MigrationResult;";

    @Override public String targetClassName() { return MIGRATOR; }
    @Override public String methodName()       { return "migrate"; }
    @Override public String methodDescriptor() { return MIGRATE_DESCRIPTOR; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();

        // migrate() is declared on Migrator and inherited by every subclass
        // (Sqlite/Mysql/Postgres/...). Patching once on the base dispatches at
        // runtime via instanceof so each subclass resolves its own schema
        // without requiring a hard-coded cast.
        // Third-party migrators (e.g. PostgresMigrator in the postgresbackend mod)
        // aren't known here; fall back to null and let the fire methods skip.
        String schemaExpr =
            "($0 instanceof " + SQLITE_MIGRATOR + " ? (Object) ((" + SQLITE_MIGRATOR + ") $0).getSchema()" +
            " : ($0 instanceof " + MYSQL_MIGRATOR + " ? (Object) com.wurmonline.server.database.migrations.MysqlMigrationStrategy.MIGRATION_SCHEMA" +
            "   : (Object) com.garward.wurmmodloader.api.database.MigratorSchemaReflector.resolve($0)))";

        int patched = patchMigrator(classPool, MIGRATOR, schemaExpr);
        LOGGER.info("[BytecodePatch] Registered DbMigrationPatch (" + patched + " target(s)).");
    }

    private int patchMigrator(ClassPool classPool, String targetClass, String schemaExpr) {
        try {
            CtClass ctClass = classPool.get(targetClass);
            CtMethod migrate = ctClass.getDeclaredMethod("migrate");

            String before =
                "try {" +
                "    " + ProxyServerHook.class.getName() +
                "        .fireDatabaseMigrationStartingEvent((Object) " + schemaExpr + ");" +
                "} catch (Throwable t) {" +
                "    java.util.logging.Logger.getLogger(\"" +
                DbMigrationPatch.class.getName() + "\")" +
                "        .log(java.util.logging.Level.WARNING," +
                "             \"Failed to fire DatabaseMigrationStartingEvent\", t);" +
                "}";

            String after =
                "try {" +
                "    " + ProxyServerHook.class.getName() +
                "        .fireDatabaseMigrationCompletedEvent((Object) " + schemaExpr + ");" +
                "} catch (Throwable t) {" +
                "    java.util.logging.Logger.getLogger(\"" +
                DbMigrationPatch.class.getName() + "\")" +
                "        .log(java.util.logging.Level.WARNING," +
                "             \"Failed to fire DatabaseMigrationCompletedEvent\", t);" +
                "}";

            migrate.insertBefore(before);
            // insertAfter(code) without asFinally=true → only runs on normal return
            migrate.insertAfter(after);

            LOGGER.info("[BytecodePatch] Patched " + targetClass + ".migrate() for DatabaseMigration events");
            return 1;
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to patch " + targetClass + ".migrate()", e);
        }
    }

    @Override public int priority() { return 80; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.DB_MIGRATION);
    }
}
