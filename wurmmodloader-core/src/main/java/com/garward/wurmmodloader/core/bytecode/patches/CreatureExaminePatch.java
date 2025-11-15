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
 * Injects CreatureExamineEvent via Creature.examine().
 */
public final class CreatureExaminePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureExaminePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "examine";
    }

    @Override
    public String methodDescriptor() {
        return "()Ljava/lang/String;";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctCreature.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureExaminePatch - Creature class already frozen (likely by legacy mod)");
                return;
            }

            CtMethod examine = ctCreature.getMethod(methodName(), methodDescriptor());

            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        $_ = ").append(ProxyServerHook.class.getName())
                .append(".fireCreatureExamineEvent(this, $_);\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        e.printStackTrace();\n");
            code.append("    }\n");
            code.append("}\n");

            examine.insertAfter(code.toString());
            LOGGER.info("Registered CreatureExamineEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureExaminePatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureExaminePatch - " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_EXAMINE);
    }
}
