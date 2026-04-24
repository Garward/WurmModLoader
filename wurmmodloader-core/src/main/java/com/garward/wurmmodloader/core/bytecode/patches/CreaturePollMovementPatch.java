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
 * Patches protected {@code GenericCreatureAI.pollMovement(Creature, long)} —
 * the target-acquisition + idle-pathing loop used by most vanilla mobs — with
 * a post-hook that fires {@link
 * com.garward.wurmmodloader.api.events.movement.CreaturePollMovementEvent}
 * and replaces the return with the event's (possibly overridden) flag.
 *
 * <p>Only fires for subclasses of GenericCreatureAI; Fish / TowerGuard / etc.
 * that don't extend it won't hit this event.</p>
 */
public final class CreaturePollMovementPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreaturePollMovementPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.creatures.ai.scripts.GenericCreatureAI"; }
    @Override public String methodName()       { return "pollMovement"; }
    @Override public String methodDescriptor() { return "(Lcom/wurmonline/server/creatures/Creature;J)Z"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getDeclaredMethod(methodName());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        $_ = " + proxy + ".fireCreaturePollMovementEvent($1, $2, $_);\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire CreaturePollMovementEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertAfter(code);
            LOGGER.info("Registered CreaturePollMovementPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreaturePollMovementPatch", e);
        }
    }

    @Override public int priority() { return 45; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_POLL_MOVEMENT);
    }
}
