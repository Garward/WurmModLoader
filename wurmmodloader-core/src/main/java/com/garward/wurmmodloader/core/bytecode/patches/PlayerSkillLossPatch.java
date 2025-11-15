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
 * Injects PlayerSkillLossEvent when a player is about to lose skills on death.
 * Allows mods to prevent or modify death skill loss.
 */
public final class PlayerSkillLossPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(PlayerSkillLossPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "punishSkills";
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
            CtClass ctCreature = classPool.get(targetClassName());
            CtMethod method = ctCreature.getDeclaredMethod(methodName());

            // Insert at beginning of method
            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        if (").append(ProxyServerHook.class.getName())
                .append(".firePlayerSkillLossEvent(this)) {\n");
            code.append("            return;\n");
            code.append("        }\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"Failed to fire PlayerSkillLossEvent\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            method.insertBefore(code.toString());
            LOGGER.info("Registered PlayerSkillLossEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install PlayerSkillLossPatch", e);
        }
    }

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.PLAYER_SKILL_LOSS);
    }
}
