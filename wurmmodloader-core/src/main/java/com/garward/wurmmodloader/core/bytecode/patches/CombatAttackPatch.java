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
 * Injects CombatAttackEvent at the start of the main combat loop.
 * This allows complete override of vanilla combat mechanics.
 */
public final class CombatAttackPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CombatAttackPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.CombatHandler";
    }

    @Override
    public String methodName() {
        return "attack";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;"
            + "IZ"
            + "F"
            + "Lcom/wurmonline/server/behaviours/Action;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCombatHandler = classPool.get(targetClassName());
            CtMethod attackMethod = ctCombatHandler.getMethod(methodName(), methodDescriptor());

            // Insert at beginning of method
            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        com.garward.wurmmodloader.modloader.server.CombatAttackResult result = ")
                .append(ProxyServerHook.class.getName())
                .append(".fireCombatAttackEvent(this.creature, $1, $2, $3, $4, $5);\n");
            code.append("        if (result.cancelled) {\n");
            code.append("            return result.attackResult;\n");
            code.append("        }\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"Failed to fire CombatAttackEvent\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            attackMethod.insertBefore(code.toString());
            LOGGER.info("Registered CombatAttackEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CombatAttackPatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_ATTACK);
    }
}
