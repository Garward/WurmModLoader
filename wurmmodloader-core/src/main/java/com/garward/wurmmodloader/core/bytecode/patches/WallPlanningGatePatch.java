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
 * Bypass hook for {@code MethodsStructure.planWallAt}'s skill gate. The vanilla
 * code compares {@code FloorBehaviour.getRequiredBuildSkillForFloorLevel(...)}
 * against craft-skill knowledge; we rewrite that call to return
 * {@code Integer.MIN_VALUE} when a {@code StructureGateCheckEvent} listener
 * bypasses the check, letting the comparison pass.
 */
public final class WallPlanningGatePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(WallPlanningGatePatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.MethodsStructure"; }
    @Override public String methodName()       { return "planWallAt"; }
    @Override public String methodDescriptor() { return ""; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool cp = hookManager.getClassPool();
            CtClass ct = cp.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getDeclaredMethod("planWallAt");
            final String proxy = ProxyServerHook.class.getName();

            method.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if ("com.wurmonline.server.behaviours.FloorBehaviour".equals(mc.getClassName())
                            && "getRequiredBuildSkillForFloorLevel".equals(mc.getMethodName())) {
                        mc.replace(
                            "{ $_ = $proceed($$); " +
                            "  if (" + proxy + ".fireStructureGateCheckEvent(" +
                            "         aPerformer, \"WALL\", \"SKILL\", heightOffset)) { " +
                            "      $_ = Integer.MIN_VALUE; } }");
                    }
                }
            });

            LOGGER.info("Registered WallPlanningGatePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install WallPlanningGatePatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.STRUCTURE_GATE_WALL_PLAN);
    }
}
