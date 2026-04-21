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

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Bytecode patch for {@code com.wurmonline.server.DbConnector.initialize(boolean)}.
 *
 * <p>Fires {@link com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent}
 * at the top of the method so mods can register a custom {@link
 * com.garward.wurmmodloader.api.database.DatabaseBackend}. After the vanilla initialize body
 * completes (building {@code CONNECTORS} and {@code MIGRATION_STRATEGY} from the
 * SQLite/MySQL config helpers), the patch invokes
 * {@link com.garward.wurmmodloader.core.eventlogic.database.DatabaseBackendEventLogic#applyRegisteredBackendIfAny()}
 * which — if a backend is registered — swaps the static fields in DbConnector for
 * the backend's factories. If no backend is registered the override is a no-op.</p>
 *
 * <p><b>Phase ordering:</b> per {@code DatabaseOptimizer}, {@code DbConnector} loads during
 * Phase 5+. This patch must already be registered (i.e. applied) before that point.
 * Registration happens via {@code CoreBytecodePatches.registerAll()}.</p>
 */
public final class DbConnectorInitPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(DbConnectorInitPatch.class.getName());

    @Override public String targetClassName() { return "com.wurmonline.server.DbConnector"; }
    @Override public String methodName()       { return "initialize"; }
    @Override public String methodDescriptor() { return "(Z)V"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctClass = classPool.get(targetClassName());
            CtMethod initialize = ctClass.getMethod(methodName(), methodDescriptor());

            String before =
                "try {" +
                "    " + ProxyServerHook.class.getName() + ".fireDatabaseBackendSelectionEvent();" +
                "} catch (Throwable t) {" +
                "    java.util.logging.Logger.getLogger(\"" + DbConnectorInitPatch.class.getName() + "\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to fire DatabaseBackendSelectionEvent\", t);" +
                "}";

            String after =
                "try {" +
                "    com.garward.wurmmodloader.core.eventlogic.database.DatabaseBackendEventLogic" +
                "        .applyRegisteredBackendIfAny();" +
                "} catch (Throwable t) {" +
                "    java.util.logging.Logger.getLogger(\"" + DbConnectorInitPatch.class.getName() + "\")" +
                "        .log(java.util.logging.Level.WARNING, \"Failed to apply registered DatabaseBackend\", t);" +
                "}";

            initialize.insertBefore(before);
            initialize.insertAfter(after);

            LOGGER.info("[BytecodePatch] Registered DbConnectorInitPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install DbConnectorInitPatch", e);
        }
    }

    @Override public int priority() { return 90; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.DB_CONNECTOR_INIT);
    }
}
