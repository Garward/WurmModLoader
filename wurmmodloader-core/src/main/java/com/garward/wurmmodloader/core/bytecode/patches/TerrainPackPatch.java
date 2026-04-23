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
 * Patches {@code Terraforming.pack(Creature, Item, int, int, boolean, int, float, Action)}
 * — package-private entry for packed-dirt creation — to fire {@code TerrainPackEvent}.
 * Cancellation returns {@code false}.
 */
public final class TerrainPackPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TerrainPackPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.Terraforming"; }
    @Override public String methodName()       { return "pack"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIZIFLcom/wurmonline/server/behaviours/Action;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getDeclaredMethod(methodName(),
                new CtClass[]{
                    classPool.get("com.wurmonline.server.creatures.Creature"),
                    classPool.get("com.wurmonline.server.items.Item"),
                    CtClass.intType, CtClass.intType,
                    CtClass.booleanType,
                    CtClass.intType,
                    CtClass.floatType,
                    classPool.get("com.wurmonline.server.behaviours.Action")
                });

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireTerrainPackEvent($1, $2, $3, $4, $5, $6, $7, $8)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire TerrainPackEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered TerrainPackPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install TerrainPackPatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.TERRAIN_PACK);
    }
}
