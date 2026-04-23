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
 * Patches {@code CaveTileBehaviour.mine(Action, Creature, Item, int, int, short,
 * float, int, TilePos)} — the main cave-mining dispatch — to fire
 * {@code CaveMineEvent}. Cancellation returns {@code true} (matches vanilla's
 * "done/abort" action-loop semantics used throughout the mining pipeline).
 */
public final class CaveMinePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CaveMinePatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.CaveTileBehaviour"; }
    @Override public String methodName()       { return "mine"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IISFILcom/wurmonline/math/TilePos;)Z";
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
            // NOTE: cancel returns TRUE — mine() uses "done/abort" semantics in the
            // action loop (true = stop, false = keep ticking).
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireCaveMineEvent($1, $2, $3, $4, $5, $6, $7, $8, $9)) {\n" +
                "            return true;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire CaveMineEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered CaveMinePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CaveMinePatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CAVE_MINE);
    }
}
