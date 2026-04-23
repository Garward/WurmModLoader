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
 * Patches the private {@code GuardPlan.pollUpkeep()} to fire
 * {@code GuardPlanPollEvent}. Cancellation returns {@code false} (no disband,
 * no drain); upkeep-replacement mods mutate the plan themselves.
 */
public final class GuardPlanPollPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(GuardPlanPollPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.villages.GuardPlan"; }
    @Override public String methodName()       { return "pollUpkeep"; }
    @Override public String methodDescriptor() { return "()Z"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            // private method — use getDeclaredMethod
            CtMethod method = ct.getDeclaredMethod(methodName());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireGuardPlanPollEvent(this)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire GuardPlanPollEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered GuardPlanPollPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install GuardPlanPollPatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.GUARDPLAN_POLL);
    }
}
