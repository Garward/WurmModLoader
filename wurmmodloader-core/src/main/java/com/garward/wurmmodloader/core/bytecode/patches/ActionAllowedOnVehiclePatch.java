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
 * Patches {@code Actions.isActionAllowedOnVehicle(short)} — the central
 * whitelist checked whenever a seated/mounted player tries to perform an
 * action. {@code insertAfter} lets listeners override the vanilla verdict
 * via {@code ActionAllowedOnVehicleEvent.setAllowed(true/false)}.
 */
public final class ActionAllowedOnVehiclePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ActionAllowedOnVehiclePatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.behaviours.Actions"; }
    @Override public String methodName()       { return "isActionAllowedOnVehicle"; }
    @Override public String methodDescriptor() { return "(S)Z"; }

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
                "        $_ = " + proxy + ".fireActionAllowedOnVehicleEvent($1, $_);\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire ActionAllowedOnVehicleEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertAfter(code);
            LOGGER.info("Registered ActionAllowedOnVehiclePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ActionAllowedOnVehiclePatch", e);
        }
    }

    @Override public int priority() { return 55; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ACTION_ALLOWED_ON_VEHICLE);
    }
}
