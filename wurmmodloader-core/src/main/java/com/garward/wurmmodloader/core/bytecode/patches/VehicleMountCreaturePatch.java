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
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.NewExpr;

/**
 * Intercepts MountAction creation in MethodsCreatures to fire vehicle mount events for living mounts.
 */
public final class VehicleMountCreaturePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(VehicleMountCreaturePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.MethodsCreatures";
    }

    @Override
    public String methodName() {
        return "<mountActionHook>";
    }

    @Override
    public String methodDescriptor() {
        return "()V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctMethodsCreatures = classPool.get(targetClassName());

            ctMethodsCreatures.instrument(new ExprEditor() {
                @Override
                public void edit(NewExpr e) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.MountAction".equals(e.getClassName())) {
                        String code = String.format(
                            "{ if (%s.fireVehicleMountEventCreature(performer, $1, $3, $4, $5)) { return true; } $_ = $proceed($$); }",
                            ProxyServerHook.class.getName()
                        );
                        e.replace(code);
                    }
                }
            });

            LOGGER.info("[BytecodePatch] Registered VehicleMountCreaturePatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install VehicleMountCreaturePatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.VEHICLE_MOUNT_CREATURE);
    }

    @Override
    public String displayName() {
        return targetClassName() + "#<mount action instrumentation>";
    }
}
