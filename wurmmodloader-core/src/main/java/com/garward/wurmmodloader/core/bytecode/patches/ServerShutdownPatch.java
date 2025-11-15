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
 * Calls {@link ProxyServerHook#fireOnServerShutdown()} before the server shuts down.
 */
public final class ServerShutdownPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ServerShutdownPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.Server";
    }

    @Override
    public String methodName() {
        return "shutDown";
    }

    @Override
    public String methodDescriptor() {
        return "()V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctServer = classPool.get(targetClassName());
            CtMethod shutDown = ctServer.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".getInstance().fireOnServerShutdown();\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + ServerShutdownPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"Failed to fire server shutdown event\", e);\n" +
                "}\n";

            shutDown.insertBefore(code);
            LOGGER.info("[BytecodePatch] Registered ServerShutdownPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ServerShutdownPatch", e);
        }
    }

    @Override
    public int priority() {
        return 90;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SERVER_LIFECYCLE_SHUTDOWN);
    }
}
