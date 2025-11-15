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
 * Intercepts MountAction creation in VehicleBehaviour to fire vehicle mount events for item-based mounts (carts/boats).
 */
public final class VehicleMountItemPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(VehicleMountItemPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.VehicleBehaviour";
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
            CtClass ctVehicleBehaviour = classPool.get(targetClassName());

            ctVehicleBehaviour.instrument(new ExprEditor() {
                @Override
                public void edit(NewExpr e) throws CannotCompileException {
                    if ("com.wurmonline.server.creatures.MountAction".equals(e.getClassName())) {
                        String code = String.format(
                            "{ if (%s.fireVehicleMountEventItem(performer, $2, $3, $4, $5)) { return true; } $_ = $proceed($$); }",
                            ProxyServerHook.class.getName()
                        );
                        e.replace(code);
                    }
                }
            });

            LOGGER.info("[BytecodePatch] Registered VehicleMountItemPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install VehicleMountItemPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.VEHICLE_MOUNT_ITEM);
    }

    @Override
    public String displayName() {
        return targetClassName() + "#<mount action instrumentation>";
    }
}
