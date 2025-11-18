package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Injects a hook into Communicator.sendMoveCreature(...) so we can fire
 * MovementBroadcastEvent whenever movement data is sent to a client.
 */
public final class MovementBroadcastPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(MovementBroadcastPatch.class.getName());

    @Override
    public String targetClassName() {
        // If decompile shows this actually lives in players.PlayerCommunicator,
        // just swap this to the correct FQN.
        return "com.wurmonline.server.creatures.Communicator";
    }

    @Override
    public String methodName() {
        return "sendMoveCreature";
    }

    @Override
    public String methodDescriptor() {
        // sendMoveCreature(long id, float x, float y, int rot, boolean keepMoving)
        // J = long, F = float, F = float, I = int, Z = boolean
        return "(JFFIZ)V";
    }

    @Override
    public String displayName() {
        return "MovementBroadcastPatch (Communicator.sendMoveCreature)";
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singletonList(BytecodeConflictKeys.MOVEMENT_BROADCAST);
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;

        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctClass = classPool.get(targetClassName());

            if (ctClass.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping MovementBroadcastPatch - class frozen");
                return;
            }

            CtMethod method = ctClass.getDeclaredMethod(methodName());

            String proxyClass = com.garward.wurmmodloader.modloader.server
                    .ProxyServerHook.class.getName();

            // Params in this method:
            // this  -> Communicator
            // $1    -> long id
            // $2    -> float x
            // $3    -> float y
            // $4    -> int rot
            // $5    -> boolean keepMoving
            //
            // We fire AFTER the vanilla packet is queued.
            method.insertAfter(
                "{ " +
                "  " + proxyClass + ".fireMovementBroadcastEvent(" +
                "    this," +      // communicator
                "    $1," +        // id
                "    $2," +        // x
                "    $3," +        // y
                "    $4," +        // rot
                "    $5" +         // moving flag
                "  );" +
                "}"
            );

            LOGGER.info("[BytecodePatch] Registered MovementBroadcastPatch successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to install MovementBroadcastPatch", e);
        }
    }
}
