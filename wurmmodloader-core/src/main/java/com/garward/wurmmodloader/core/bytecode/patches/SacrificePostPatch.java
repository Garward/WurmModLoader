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
 * Patches {@code MethodsReligion.sacrifice(Action, Creature, Item)} (altar
 * variant) with a post-hook that fires {@code SacrificePostEvent}. Not
 * cancellable — observers only. The existing {@code SACRIFICE_*} pre-hook
 * conflict keys cover acceptance/favor pre-checks.
 */
public final class SacrificePostPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SacrificePostPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.MethodsReligion"; }
    @Override public String methodName()       { return "sacrifice"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z";
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
                "        " + proxy + ".fireSacrificePostEvent($1, $2, $3, ($_));\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire SacrificePostEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            // Post-hook: use insertAfter so the event fires after sacrifice resolves.
            method.insertAfter(code);
            LOGGER.info("Registered SacrificePostPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SacrificePostPatch", e);
        }
    }

    @Override public int priority() { return 50; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SACRIFICE_POST);
    }
}
