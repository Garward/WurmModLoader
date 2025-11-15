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
 * Posts player login events after the login handler finishes sending data.
 */
public final class PlayerLoginPatch implements BytecodePatch {

    private static final String DESCRIPTOR = "(Lcom/wurmonline/server/players/Player;)V";
    private static final Logger LOGGER = Logger.getLogger(PlayerLoginPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.LoginHandler";
    }

    @Override
    public String methodName() {
        return "sendLoggedInPeople";
    }

    @Override
    public String methodDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctLoginHandler = classPool.get(targetClassName());
            CtMethod sendLoggedInPeople = ctLoginHandler.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".getInstance().fireOnPlayerLogin($1);\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + PlayerLoginPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"Failed to fire player login event\", e);\n" +
                "}\n";

            sendLoggedInPeople.insertAfter(code);
            LOGGER.info("[BytecodePatch] Registered PlayerLoginPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install PlayerLoginPatch", e);
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.PLAYER_LOGIN);
    }
}
