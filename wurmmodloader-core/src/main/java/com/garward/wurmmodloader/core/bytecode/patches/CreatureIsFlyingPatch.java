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
 * Patches {@code Creature.isFlying()} — vanilla returns a flat {@code false}
 * stub — with a post-hook that fires {@link
 * com.garward.wurmmodloader.api.events.creature.CreatureIsFlyingEvent} and
 * replaces the return with the event's (possibly flipped) flag.
 */
public final class CreatureIsFlyingPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureIsFlyingPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.creatures.Creature"; }
    @Override public String methodName()       { return "isFlying"; }
    @Override public String methodDescriptor() { return "()Z"; }

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
                "        $_ = " + proxy + ".fireCreatureIsFlyingEvent($0, $_);\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire CreatureIsFlyingEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertAfter(code);
            LOGGER.info("Registered CreatureIsFlyingPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureIsFlyingPatch", e);
        }
    }

    @Override public int priority() { return 45; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_IS_FLYING);
    }
}
