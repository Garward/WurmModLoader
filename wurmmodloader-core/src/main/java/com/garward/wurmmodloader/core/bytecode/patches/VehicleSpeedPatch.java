package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Allows mods to adjust mount speed after vanilla Vehicle.calculateNewMountSpeed completes.
 */
public final class VehicleSpeedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(VehicleSpeedPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.Vehicle";
    }

    @Override
    public String methodName() {
        return "calculateNewMountSpeed";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Z)B";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctVehicle = classPool.get(targetClassName());
            CtMethod calculateMountSpeed = ctVehicle.getMethod(methodName(), methodDescriptor());

            String code =
                "{" +
                "  float modifiedSpeed = " + ProxyServerHook.class.getName() +
                "    .fireVehicleSpeedCalculationCreature(this, $1, this.getPilotId(), $2, (float)$_);" +
                "  $_ = (byte)Math.max(0.0f, Math.min(127.0f, modifiedSpeed));" +
                "}";

            calculateMountSpeed.insertAfter(code);
            LOGGER.info("[BytecodePatch] Registered VehicleSpeedPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install VehicleSpeedPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.VEHICLE_SPEED);
    }
}
