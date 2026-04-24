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
 * Patches protected {@code CreatureAI.creatureMovementTick(Creature, boolean)}
 * with a pre-hook that fires {@link
 * com.garward.wurmmodloader.api.events.movement.CreatureMovementTickEvent}.
 * Returns early (skipping the entire position-update tick) if the event is
 * cancelled. Hot path — fires per tick per moving creature.
 */
public final class CreatureMovementTickPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureMovementTickPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.creatures.ai.CreatureAI"; }
    @Override public String methodName()       { return "creatureMovementTick"; }
    @Override public String methodDescriptor() { return "(Lcom/wurmonline/server/creatures/Creature;Z)V"; }

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
                "        if (" + proxy + ".fireCreatureMovementTickEvent($1, $2)) return;\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire CreatureMovementTickEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered CreatureMovementTickPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureMovementTickPatch", e);
        }
    }

    @Override public int priority() { return 45; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_MOVEMENT_TICK);
    }
}
