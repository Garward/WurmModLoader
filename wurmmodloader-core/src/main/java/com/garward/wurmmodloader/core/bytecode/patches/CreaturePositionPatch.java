package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches CreatureStatus.setPositionXYZ(...) to fire CreaturePositionUpdatedEvent
 * whenever a creature's authoritative server position changes.
 *
 * Note: Rotation and bridgeId are read from current position state since
 * setPositionXYZ only updates x/y/z coordinates.
 */
public final class CreaturePositionPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreaturePositionPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.CreatureStatus";
    }

    @Override
    public String methodName() {
        return "setPositionXYZ";
    }

    @Override
    public String methodDescriptor() {
        return "(FFF)V"; // setPositionXYZ(float, float, float)
    }

    @Override
    public String displayName() {
        return "CreaturePositionPatch (CreatureStatus.setPositionXYZ)";
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singletonList(BytecodeConflictKeys.CREATURE_POSITION);
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctClass = classPool.get(targetClassName());

            if (ctClass.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CreaturePositionPatch - CreatureStatus class already frozen");
                return;
            }

            CtMethod method = ctClass.getDeclaredMethod(
                methodName(),
                new CtClass[]{
                    CtClass.floatType,    // posX
                    CtClass.floatType,    // posY
                    CtClass.floatType     // posZ
                }
            );

            String proxyClass = com.garward.wurmmodloader.modloader.server.ProxyServerHook.class.getName();

            // Fire event AFTER position has been updated
            // Read rotation and bridgeId from current position object
            method.insertAfter(
                "{ " +
                "  " + proxyClass + ".fireCreaturePositionUpdatedEvent(" +
                "    this.statusHolder, " +           // creature (field name is statusHolder)
                "    $1, " +                           // new X
                "    $2, " +                           // new Y
                "    $3, " +                           // new Z
                "    this.getPosition().getRotation(), " +  // current rotation
                "    this.getPosition().getBridgeId()" +    // current bridgeId
                "  );" +
                "}"
            );

            LOGGER.info("[BytecodePatch] Registered CreaturePositionPatch successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to install CreaturePositionPatch", e);
        }
    }
}
