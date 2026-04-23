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
 * Patches {@code MovementScheme.getSpeedModifier()} with a post-hook that
 * fires {@code CreatureMovementSpeedEvent} and replaces the return with the
 * event's (possibly-modified) speed. The scheme's {@code creature} field is
 * package-private; accessed via its public getter.
 */
public final class CreatureMovementSpeedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureMovementSpeedPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.creatures.MovementScheme"; }
    @Override public String methodName()       { return "getSpeedModifier"; }
    @Override public String methodDescriptor() { return "()F"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            // $0 is MovementScheme; the creature field is accessible directly
            // from the same package (Javassist-generated code runs inside the
            // patched class's own body, so field access is unrestricted).
            String code =
                "{\n" +
                "    try {\n" +
                "        $_ = " + proxy + ".fireCreatureMovementSpeedEvent($0.creature, $_);\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire CreatureMovementSpeedEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertAfter(code);
            LOGGER.info("Registered CreatureMovementSpeedPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureMovementSpeedPatch", e);
        }
    }

    @Override public int priority() { return 45; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_MOVEMENT_SPEED);
    }
}
