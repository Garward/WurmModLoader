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
 * Patches {@code Creature.getMountSpeedPercent(boolean)} to fire {@code
 * MountSpeedPercentEvent} at entry. If any listener calls {@code
 * setPercent(float)}, the patched method returns that value verbatim;
 * otherwise vanilla's saddle/horseshoe/trait math runs as normal.
 */
public final class MountSpeedPercentPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(MountSpeedPercentPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "getMountSpeedPercent";
    }

    @Override
    public String methodDescriptor() {
        return "(Z)F";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());
            if (ctCreature.isFrozen()) {
                ctCreature.defrost();
            }
            CtMethod method = ctCreature.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        Object _result = " + proxy + ".fireMountSpeedPercentEvent(this, $1);\n" +
                "        if (_result != null) {\n" +
                "            return ((Float) _result).floatValue();\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire MountSpeedPercentEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered MountSpeedPercentPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install MountSpeedPercentPatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_MOUNT_SPEED_PERCENT);
    }
}
