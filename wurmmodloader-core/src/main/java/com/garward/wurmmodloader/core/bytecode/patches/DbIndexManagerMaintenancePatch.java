package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Skips {@link com.wurmonline.server.utils.DbIndexManager}'s MySQL-only
 * maintenance paths when a custom {@link com.garward.wurmmodloader.api.database.DatabaseBackend}
 * is registered whose {@link com.garward.wurmmodloader.api.database.Dialect}
 * is neither {@code SQLITE}, {@code MYSQL}, nor {@code MARIADB}.
 *
 * <p>Vanilla WU's {@code createIndexes()}, {@code removeIndexes()}, and
 * {@code repairDatabaseTables()} early-return on SQLite and otherwise run
 * {@code ALTER TABLE ... ADD INDEX ... (col)} / {@code REPAIR TABLE ...} —
 * syntax that only MySQL and MariaDB accept. Postgres (and any other
 * non-MySQL backend) errors on those. This patch inserts an additional
 * early-return in front of each method, guarded on the registered backend's
 * dialect. Vanilla MySQL users keep the vanilla index-creation path.</p>
 *
 * <p>Non-MySQL backends are expected to create and maintain indexes in their
 * own Flyway migration files. The framework does not try to translate
 * {@code ADD INDEX} DDL across dialects.</p>
 */
public final class DbIndexManagerMaintenancePatch implements BytecodePatch {

    private static final Logger LOGGER =
        Logger.getLogger(DbIndexManagerMaintenancePatch.class.getName());

    private static final String TARGET = "com.wurmonline.server.utils.DbIndexManager";
    private static final String[] METHODS = {"createIndexes", "removeIndexes", "repairDatabaseTables"};

    @Override public String targetClassName() { return TARGET; }
    @Override public String methodName()       { return "createIndexes"; }
    @Override public String methodDescriptor() { return "()V"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();
        int patched = 0;
        try {
            CtClass ctClass = classPool.get(TARGET);
            for (String m : METHODS) {
                CtMethod method;
                try {
                    method = ctClass.getDeclaredMethod(m);
                } catch (NotFoundException nfe) {
                    LOGGER.warning("[BytecodePatch] " + TARGET + "." + m
                        + " not found — skipping (WU version mismatch?)");
                    continue;
                }
                method.insertBefore(buildGuard(m));
                patched++;
            }
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install DbIndexManagerMaintenancePatch", e);
        }
        LOGGER.info("[BytecodePatch] Registered DbIndexManagerMaintenancePatch ("
            + patched + "/" + METHODS.length + " method(s))");
    }

    private String buildGuard(String methodLabel) {
        // Javassist source is plain Java — no diamond, no var.
        // We consult DatabaseBackendRegistry reflectively via direct call.
        // Dialect comparison uses enum name() to avoid any classloader ambiguity.
        return
            "{" +
            "  com.garward.wurmmodloader.api.database.DatabaseBackend __b =" +
            "      com.garward.wurmmodloader.api.database.DatabaseBackendRegistry.getRegistered();" +
            "  if (__b != null) {" +
            "    com.garward.wurmmodloader.api.database.Dialect __d = __b.getDialect();" +
            "    if (__d != null) {" +
            "      String __n = __d.name();" +
            "      if (!\"SQLITE\".equals(__n) && !\"MYSQL\".equals(__n) && !\"MARIADB\".equals(__n)) {" +
            "        java.util.logging.Logger.getLogger(\"" + TARGET + "\")" +
            "            .info(\"[DbIndexManager] Skipping " + methodLabel +
                     " — custom backend dialect \" + __n +" +
                     " \" owns index maintenance via its own migration files\");" +
            "        return;" +
            "      }" +
            "    }" +
            "  }" +
            "}";
    }

    @Override public int priority() { return 75; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.unmodifiableCollection(
            Arrays.asList(BytecodeConflictKeys.DB_INDEX_MAINTENANCE));
    }
}
