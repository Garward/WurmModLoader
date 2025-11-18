package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Injects config sync at the BEGINNING of Servers.loadAllServers().
 *
 * This ensures our YAML config is synced to the database BEFORE the server
 * reads values from the database, so the server uses the correct values.
 *
 * Hooks Servers.loadAllServers(boolean) and injects sync code at the start,
 * only running on first load (when reload == false).
 */
public final class ServerConfigLoadPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ServerConfigLoadPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.Servers";
    }

    @Override
    public String methodName() {
        return "loadAllServers";
    }

    @Override
    public String methodDescriptor() {
        return "(Z)V"; // boolean reload -> void
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctServers = classPool.get(targetClassName());

            if (ctServers.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping ServerConfigLoadPatch - Servers class already frozen");
                return;
            }

            // Get the loadAllServers method
            CtMethod loadAllServersMethod = ctServers.getMethod(methodName(), methodDescriptor());

            String proxyClass = com.garward.wurmmodloader.modloader.server.ProxyServerHook.class.getName();

            // Inject at the BEGINNING of loadAllServers() - before it reads from database
            // CRITICAL: Wrap in try-catch to make this completely non-fatal
            String code =
                "if (!$1) { " +  // Only run on first load (reload == false)
                "  try { " +
                "    " + proxyClass + ".getInstance().syncServerConfigBeforeLoad(); " +
                "  } catch (Throwable e) { " +  // Catch Throwable to catch ExceptionInInitializerError too
                "    System.err.println(\"[ServerConfigLoadPatch] Config sync failed (non-fatal): \" + e.getMessage()); " +
                "    e.printStackTrace(); " +
                "  } " +
                "}";

            loadAllServersMethod.insertBefore(code);

            LOGGER.info("[BytecodePatch] ✅ Injected config sync at beginning of Servers.loadAllServers()");
            LOGGER.info("[BytecodePatch] Registered ServerConfigLoadPatch successfully");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ServerConfigLoadPatch", e);
        }
    }

    @Override
    public int priority() {
        return 200; // Very high priority - must run before server reads config
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.emptyList(); // No conflicts - patches Server constructor, not ServerLauncher
    }
}
