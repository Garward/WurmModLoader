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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Bytecode patch that fires
 * {@link com.garward.wurmmodloader.api.events.database.DatabaseConnectionOpenedEvent}
 * at method exit for both vanilla {@code ConnectionFactory} implementations:
 * {@code SqliteConnectionFactory.createConnection()} and
 * {@code MysqlConnectionFactory.createConnection()}.
 *
 * <p>The injected code wraps in try/catch so a misbehaving handler cannot break connection
 * acquisition. It uses {@code $_} (javassist) to reference the just-returned Connection
 * and {@code this.getSchema()} to supply the schema.</p>
 *
 * <p><b>Mod-registered backends:</b> If a mod installs a custom {@link
 * com.garward.wurmmodloader.api.database.DatabaseBackend}, the backend's own factory
 * replaces the vanilla one and is responsible for firing this event itself. The
 * framework only auto-fires it from the two vanilla factories.</p>
 */
public final class DbConnectionOpenedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(DbConnectionOpenedPatch.class.getName());

    private static final String SQLITE_FACTORY = "com.wurmonline.server.database.SqliteConnectionFactory";
    private static final String MYSQL_FACTORY  = "com.wurmonline.server.database.MysqlConnectionFactory";

    @Override public String targetClassName() { return SQLITE_FACTORY; }
    @Override public String methodName()       { return "createConnection"; }
    @Override public String methodDescriptor() { return "()Ljava/sql/Connection;"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();
        int patched = 0;
        for (String targetClass : Arrays.asList(SQLITE_FACTORY, MYSQL_FACTORY)) {
            try {
                CtClass ctClass = classPool.get(targetClass);
                CtMethod createConnection = ctClass.getMethod("createConnection", "()Ljava/sql/Connection;");

                String code =
                    "try {" +
                    "    " + ProxyServerHook.class.getName() +
                    "        .fireDatabaseConnectionOpenedEvent(this.getSchema(), $_);" +
                    "} catch (Throwable t) {" +
                    "    java.util.logging.Logger.getLogger(\"" +
                    DbConnectionOpenedPatch.class.getName() + "\")" +
                    "        .log(java.util.logging.Level.WARNING," +
                    "             \"Failed to fire DatabaseConnectionOpenedEvent\", t);" +
                    "}";

                createConnection.insertAfter(code);
                patched++;
                LOGGER.info("[BytecodePatch] Patched " + targetClass + ".createConnection() for DatabaseConnectionOpenedEvent");
            } catch (NotFoundException | CannotCompileException e) {
                throw new IllegalStateException("Unable to patch " + targetClass + ".createConnection()", e);
            }
        }
        LOGGER.info("[BytecodePatch] Registered DbConnectionOpenedPatch (" + patched + " target(s)).");
    }

    @Override public int priority() { return 80; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.DB_CONNECTION_OPENED);
    }
}
