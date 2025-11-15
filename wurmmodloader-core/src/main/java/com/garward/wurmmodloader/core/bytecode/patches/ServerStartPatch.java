package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Ensures {@link ProxyServerHook#fireOnServerStarted()} runs after the server launcher completes.
 */
public final class ServerStartPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ServerStartPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.ServerLauncher";
    }

    @Override
    public String methodName() {
        return "runServer";
    }

    @Override
    public String methodDescriptor() {
        return "(ZZ)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctServerLauncher = classPool.get(targetClassName());
            CtMethod runServer = ctServerLauncher.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".getInstance().fireOnServerStarted();\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + ServerStartPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"Failed to fire server started event\", e);\n" +
                "}\n";

            runServer.insertAfter(code);
            LOGGER.info("[BytecodePatch] Registered ServerStartPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ServerStartPatch", e);
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SERVER_LIFECYCLE_START);
    }
}
