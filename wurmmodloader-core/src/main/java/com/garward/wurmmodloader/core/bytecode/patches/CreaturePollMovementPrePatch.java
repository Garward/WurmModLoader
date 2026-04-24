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
 * Patches protected {@code GenericCreatureAI.pollMovement(Creature, long)} with
 * a pre-hook that fires {@link
 * com.garward.wurmmodloader.api.events.movement.CreaturePollMovementPreEvent}.
 * When the event is cancelled, vanilla pollMovement is skipped entirely and
 * the method returns {@code true} (signaling "movement handled"). Companion to
 * {@link CreaturePollMovementPatch} — both can coexist on the same method.
 */
public final class CreaturePollMovementPrePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreaturePollMovementPrePatch.class.getName());

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
                "        if (" + proxy + ".fireCreaturePollMovementPreEvent($1, $2)) return true;\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire CreaturePollMovementPreEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered CreaturePollMovementPrePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreaturePollMovementPrePatch", e);
        }
    }

    // Priority 44 — runs just before the post-hook patch (priority 45) so that
    // if both are applied to the same method, the pre-check is visible first.
    @Override public int priority() { return 44; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_POLL_MOVEMENT_PRE);
    }
}
