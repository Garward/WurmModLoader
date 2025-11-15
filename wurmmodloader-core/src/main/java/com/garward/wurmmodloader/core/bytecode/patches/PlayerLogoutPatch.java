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
 * Posts logout events when a player disconnects.
 */
public final class PlayerLogoutPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(PlayerLogoutPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.players.Player";
    }

    @Override
    public String methodName() {
        return "logout";
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
            CtClass ctPlayer = classPool.get(targetClassName());
            CtMethod logout = ctPlayer.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".getInstance().fireOnPlayerLogout(this);\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + PlayerLogoutPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"Failed to fire player logout event\", e);\n" +
                "}\n";

            logout.insertAfter(code);
            LOGGER.info("[BytecodePatch] Registered PlayerLogoutPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install PlayerLogoutPatch", e);
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.PLAYER_LOGOUT);
    }
}
