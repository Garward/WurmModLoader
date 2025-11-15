package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.*;

/**
 * Fires CreatureSpawnEvent when Creature.setWurmId completes.
 */
public final class CreatureSpawnPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureSpawnPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "setWurmId";
    }

    @Override
    public String methodDescriptor() {
        return "(JFFFI)Lcom/wurmonline/server/creatures/Creature;";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());
            CtMethod setWurmId = ctCreature.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".fireCreatureSpawnEvent(this);\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"WurmModLoader.CreatureSpawnPatch\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"Failed to fire CreatureSpawnEvent\", e);\n" +
                "}";

            setWurmId.insertAfter(code);
            LOGGER.info("[BytecodePatch] Registered CreatureSpawnEvent patch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureSpawnPatch", e);
        }
    }

    @Override
    public int priority() {
        return 50; // mid-range — safe to run before item/AI events
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_SPAWN);
    }
}
