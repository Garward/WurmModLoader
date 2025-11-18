package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches Communicator.sendMove(...) to fire PlayerMovementBroadcastEvent
 * whenever the server sends a movement packet to the player's own client.
 *
 * NOTE: Verify the actual sendMove signature in your decompiled WU server and
 * adjust the parameter list + descriptor accordingly.
 */
public final class PlayerMovementBroadcastPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(PlayerMovementBroadcastPatch.class.getName());

    @Override
    public String targetClassName() {
        // Adjust if your decompile shows a different FQN (e.g. players.PlayerCommunicator)
        return "com.wurmonline.server.creatures.Communicator";
    }

    @Override
    public String methodName() {
        return "sendMove";
    }

    @Override
    public String methodDescriptor() {
        // TODO: VERIFY THIS AGAINST YOUR DECOMPILE
        // Example assumption: sendMove(float x, float y, float z, float rot, boolean moving)
        // F = float, F = float, F = float, F = float, Z = boolean → (FFFFZ)V
        return "(FFFFZ)V";
    }

    @Override
    public String displayName() {
        return "PlayerMovementBroadcastPatch (Communicator.sendMove)";
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singletonList(BytecodeConflictKeys.PLAYER_MOVEMENT_BROADCAST);
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctClass = classPool.get(targetClassName());

            if (ctClass.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping PlayerMovementBroadcastPatch - Communicator class frozen");
                return;
            }

            // If descriptor mismatch happens, you can switch to the overload-taking variant:
            // CtMethod method = ctClass.getDeclaredMethod(methodName(), new CtClass[]{ ... });
            CtMethod method = ctClass.getDeclaredMethod(methodName());

            String proxyClass = com.garward.wurmmodloader.modloader.server.ProxyServerHook.class.getName();

            // Assume params:
            // this  -> Communicator
            // $1    -> float x
            // $2    -> float y
            // $3    -> float z
            // $4    -> float rot
            // $5    -> boolean moving
            //
            // You can tweak argument mapping once you see the real signature.
            method.insertAfter(
                "{ " +
                proxyClass + ".firePlayerMovementBroadcastEvent(" +
                "this, $1, $2, $3, $4, $5" +
                "); }"
            );

            LOGGER.info("[BytecodePatch] Registered PlayerMovementBroadcastPatch successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to install PlayerMovementBroadcastPatch", e);
        }
    }
}
