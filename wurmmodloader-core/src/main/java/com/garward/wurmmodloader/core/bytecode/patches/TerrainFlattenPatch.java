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
 * Patches {@code Flattening.flatten(Creature, Item, int, int, int, float, Action)}
 * — the 7-arg public-ish entry used for tile + border flattening — to fire
 * {@code TerrainFlattenEvent}. Cancellation returns {@code false}.
 */
public final class TerrainFlattenPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TerrainFlattenPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.Flattening"; }
    @Override public String methodName()       { return "flatten"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIFLcom/wurmonline/server/behaviours/Action;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            // 7-arg entry is package-private — use getDeclaredMethod.
            CtMethod method = ct.getDeclaredMethod(methodName(),
                new CtClass[]{
                    classPool.get("com.wurmonline.server.creatures.Creature"),
                    classPool.get("com.wurmonline.server.items.Item"),
                    CtClass.intType, CtClass.intType, CtClass.intType,
                    CtClass.floatType,
                    classPool.get("com.wurmonline.server.behaviours.Action")
                });

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireTerrainFlattenEvent($1, $2, $3, $4, $5, $6, $7)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire TerrainFlattenEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered TerrainFlattenPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install TerrainFlattenPatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.TERRAIN_FLATTEN);
    }
}
