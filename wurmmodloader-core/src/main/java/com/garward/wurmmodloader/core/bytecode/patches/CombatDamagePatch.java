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
 * Injects CombatDamageEvent before vanilla damage calculation.
 */
public final class CombatDamagePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CombatDamagePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.combat.CombatEngine";
    }

    @Override
    public String methodName() {
        return "addWound";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;"
            + "Lcom/wurmonline/server/creatures/Creature;BIDFLjava/lang/String;"
            + "Lcom/wurmonline/server/combat/Battle;FFZZZZ)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCombatEngine = classPool.get(targetClassName());
            CtMethod addWound = ctCombatEngine.getMethod(methodName(), methodDescriptor());

            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        $5 = ").append(ProxyServerHook.class.getName())
                .append(".fireCombatDamageEvent($1, $2, $5, $3, $4);\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"Failed to fire CombatDamageEvent\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            addWound.insertBefore(code.toString());
            LOGGER.info("Registered CombatDamageEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CombatDamagePatch", e);
        }
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_DAMAGE);
    }
}
