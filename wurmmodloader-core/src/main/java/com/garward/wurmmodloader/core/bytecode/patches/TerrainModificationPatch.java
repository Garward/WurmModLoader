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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches {@code Terraforming.dig(...)} to fire {@code TerrainModificationEvent}.
 * Wurm exposes two overloads (8-arg and 9-arg with {@code toPile}); both are
 * package-private static boolean methods. Cancellation returns {@code false}
 * from the dig call so the action aborts before any mesh mutation.
 */
public final class TerrainModificationPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(TerrainModificationPatch.class.getName());

    private static final String DESC_8 =
        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIFZLcom/wurmonline/mesh/MeshIO;)Z";
    private static final String DESC_9 =
        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIIFZLcom/wurmonline/mesh/MeshIO;Z)Z";

    @Override public String targetClassName() { return "com.wurmonline.server.behaviours.Terraforming"; }
    @Override public String methodName()      { return "dig"; }
    @Override public String methodDescriptor(){ return DESC_8; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireTerrainModificationEvent($1, $2, $3, $4, $5, $6, $7)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire TerrainModificationEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            int patched = 0;
            for (String desc : Arrays.asList(DESC_8, DESC_9)) {
                try {
                    CtMethod m = ct.getMethod(methodName(), desc);
                    m.insertBefore(code);
                    patched++;
                } catch (NotFoundException nfe) {
                    LOGGER.fine("Terraforming.dig overload not present: " + desc);
                }
            }
            if (patched == 0) {
                throw new IllegalStateException("Terraforming.dig overloads not found");
            }
            LOGGER.info("Registered TerrainModificationPatch (" + patched + " overload(s))");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install TerrainModificationPatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.TERRAIN_MODIFICATION);
    }
}
