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
 * Injects SpecialMoveSendEvent when special move UI is sent to player.
 * Allows mods to customize special move systems.
 */
public final class SpecialMoveSendPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpecialMoveSendPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.CombatHandler";
    }

    @Override
    public String methodName() {
        return "sendSpecialMoves";
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
            CtClass ctCombatHandler = classPool.get(targetClassName());
            CtMethod method = ctCombatHandler.getDeclaredMethod(methodName());

            // Insert at beginning of method
            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        if (").append(ProxyServerHook.class.getName())
                .append(".fireSpecialMoveSendEvent(this.creature)) {\n");
            code.append("            return;\n");
            code.append("        }\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"Failed to fire SpecialMoveSendEvent\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            method.insertBefore(code.toString());
            LOGGER.info("Registered SpecialMoveSendEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpecialMoveSendPatch", e);
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_SPECIAL_MOVE_SEND);
    }
}
