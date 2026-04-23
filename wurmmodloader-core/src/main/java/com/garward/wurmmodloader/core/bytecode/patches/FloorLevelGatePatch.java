package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Bypass hook for {@code FloorBehaviour.mayPlanAtLevel(..., sendMessage)} —
 * the shared helper used by both wall and floor planning to gate by height
 * skill. A {@code StructureGateCheckEvent} listener that flips bypass makes
 * the helper return {@code true} unconditionally.
 */
public final class FloorLevelGatePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(FloorLevelGatePatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.FloorBehaviour"; }
    @Override public String methodName()       { return "mayPlanAtLevel"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;ILcom/wurmonline/server/skills/Skill;ZZ)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool cp = hookManager.getClassPool();
            CtClass ct = cp.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getMethod(methodName(), methodDescriptor());
            final String proxy = ProxyServerHook.class.getName();

            String code =
                "{ if (" + proxy + ".fireStructureGateCheckEvent($1, \"FLOOR\", \"LEVEL\", $2)) { " +
                "      return true; " +
                "  } }";

            method.insertBefore(code);
            LOGGER.info("Registered FloorLevelGatePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install FloorLevelGatePatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.STRUCTURE_GATE_FLOOR_LEVEL);
    }
}
