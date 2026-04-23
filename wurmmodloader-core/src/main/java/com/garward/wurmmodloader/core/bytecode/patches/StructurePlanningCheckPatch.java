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
 * Patches {@code MethodsStructure.canPlanStructureAt(Creature, Item, int, int, int)}
 * to fire {@code StructurePlanningCheckEvent}. Cancellation returns {@code false}
 * from the check; an optional deny-reason is sent to the performer as an alert.
 */
public final class StructurePlanningCheckPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(StructurePlanningCheckPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.MethodsStructure"; }
    @Override public String methodName()       { return "canPlanStructureAt"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;III)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireStructurePlanningCheckEvent($1, $2, $3, $4, $5)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire StructurePlanningCheckEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered StructurePlanningCheckPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install StructurePlanningCheckPatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.STRUCTURE_PLANNING_CHECK);
    }
}
