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
 * Lets mods scale the stamina modifier returned by Actions.getStaminaModiferFor.
 */
public final class ActionSpeedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ActionSpeedPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.Actions";
    }

    @Override
    public String methodName() {
        return "getStaminaModiferFor";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;I)F";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctActions = classPool.get(targetClassName());
            CtMethod staminaMethod = ctActions.getDeclaredMethod("getStaminaModiferFor");

            staminaMethod.insertAfter(String.format(
                "{ $_ = %s.fireActionSpeedEvent($1, $2, $_); }",
                ProxyServerHook.class.getName()
            ));

            LOGGER.info("[BytecodePatch] Registered ActionSpeedPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ActionSpeedPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ACTION_STAMINA);
    }
}
