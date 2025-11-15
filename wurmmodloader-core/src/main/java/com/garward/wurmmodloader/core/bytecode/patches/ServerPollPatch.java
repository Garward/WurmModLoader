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
 * Fires the server poll event each tick when player polling runs.
 */
public final class ServerPollPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ServerPollPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.Players";
    }

    @Override
    public String methodName() {
        return "pollPlayers";
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
            CtClass ctPlayers = classPool.get(targetClassName());
            CtMethod pollPlayers = ctPlayers.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".getInstance().fireOnServerPoll();\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + ServerPollPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"Failed to fire server poll event\", e);\n" +
                "}\n";

            pollPlayers.insertBefore(code);
            LOGGER.info("[BytecodePatch] Registered ServerPollPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ServerPollPatch", e);
        }
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SERVER_POLL);
    }
}
