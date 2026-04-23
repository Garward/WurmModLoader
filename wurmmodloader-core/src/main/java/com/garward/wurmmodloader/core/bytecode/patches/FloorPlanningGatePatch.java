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
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Bypass hook for {@code MethodsStructure.floorPlan}'s floor-type skill gate.
 * Rewrites {@code FloorBehaviour.getRequiredBuildSkillForFloorType(...)} to
 * return {@code Integer.MIN_VALUE} when a {@code StructureGateCheckEvent}
 * listener bypasses the check.
 */
public final class FloorPlanningGatePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(FloorPlanningGatePatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.MethodsStructure"; }
    @Override public String methodName()       { return "floorPlan"; }
    @Override public String methodDescriptor() { return ""; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool cp = hookManager.getClassPool();
            CtClass ct = cp.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getDeclaredMethod("floorPlan");
            final String proxy = ProxyServerHook.class.getName();

            method.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.behaviours.FloorBehaviour".equals(mc.getClassName())
                            && "getRequiredBuildSkillForFloorType".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ $_ = $proceed($$); " +
                            "  if (" + proxy + ".fireStructureGateCheckEvent(" +
                            "         performer, \"FLOOR\", \"SKILL\", 0)) { " +
                            "      $_ = Integer.MIN_VALUE; } }");
                    }
                }
            });

            LOGGER.info("Registered FloorPlanningGatePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install FloorPlanningGatePatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.STRUCTURE_GATE_FLOOR_PLAN);
    }
}
