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
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Hooks Creature.canUseWithEquipment to allow mods to veto incompatible equipment when mounting.
 */
public final class MountEquipmentCheckPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(MountEquipmentCheckPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "canUseWithEquipment";
    }

    @Override
    public String methodDescriptor() {
        return "()Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());
            CtMethod canUseMethod = ctCreature.getMethod(methodName(), methodDescriptor());

            canUseMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("isCreatureWearableOnly".equals(m.getMethodName())) {
                        String code = String.format(
                            "{ $_ = $proceed($$); if ($_ && %s.fireMountEquipmentCheckEvent(this, subjectItem)) { return false; } }",
                            ProxyServerHook.class.getName()
                        );
                        m.replace(code);
                    }
                }
            });

            LOGGER.info("[BytecodePatch] Registered MountEquipmentCheckPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install MountEquipmentCheckPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.VEHICLE_EQUIPMENT_CHECK);
    }
}
