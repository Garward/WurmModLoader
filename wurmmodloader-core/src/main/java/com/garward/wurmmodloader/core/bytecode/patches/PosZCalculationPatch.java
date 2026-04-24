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
 * Patches {@code Zones.calculatePosZ(float, float, VolaTile, boolean, boolean,
 * float, Creature, long)} — the central Z-reconciliation function — with a
 * post-hook that fires {@link
 * com.garward.wurmmodloader.api.events.movement.PosZCalculationEvent} and
 * replaces the return with the event's (possibly overridden) Z.
 *
 * <p>The method is static, so we reference args by number starting at
 * {@code $1}. {@code $0} does not exist for static methods.</p>
 */
public final class PosZCalculationPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(PosZCalculationPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.zones.Zones"; }
    @Override public String methodName()       { return "calculatePosZ"; }
    @Override public String methodDescriptor() {
        return "(FFLcom/wurmonline/server/zones/VolaTile;ZZFLcom/wurmonline/server/creatures/Creature;J)F";
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
            String code =
                "{\n" +
                "    try {\n" +
                "        $_ = " + proxy + ".firePosZCalculationEvent(" +
                "$1, $2, $3, $4, $5, $6, $7, $8, $_);\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire PosZCalculationEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertAfter(code);
            LOGGER.info("Registered PosZCalculationPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install PosZCalculationPatch", e);
        }
    }

    @Override public int priority() { return 45; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ZONES_POS_Z);
    }
}
