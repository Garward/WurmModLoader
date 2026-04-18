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
 * Fires CreatureDeathEvent and PlayerDeathEvent when creatures die.
 *
 * <p><strong>Architecture:</strong> This patch is CLEAN - it contains NO LOGIC.
 * All killer determination, attacker resolution, and damage tracking logic lives in
 * {@link com.garward.wurmmodloader.core.eventlogic.CreatureDeathEventLogic}.</p>
 *
 * <h2>What This Patch Does:</h2>
 * <ol>
 *   <li><strong>Before die():</strong> Captures attacker map (needed for loot)</li>
 *   <li><strong>After die():</strong> Retrieves damage tracking and fires event</li>
 * </ol>
 *
 * <h2>What This Patch Does NOT Do:</h2>
 * <ul>
 *   <li>❌ No reflection (delegated to EventLogic)</li>
 *   <li>❌ No loops (delegated to EventLogic)</li>
 *   <li>❌ No conditionals (delegated to ProxyServerHook)</li>
 *   <li>❌ No killer determination (delegated to EventLogic)</li>
 * </ul>
 *
 * @since 1.0.0
 * @see com.garward.wurmmodloader.core.eventlogic.CreatureDeathEventLogic
 */
public final class CreatureDeathPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureDeathPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "die";
    }

    @Override
    public String methodDescriptor() {
        return "(ZLjava/lang/String;)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctCreature.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureDeathPatch - Creature class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] CreatureDeathEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            CtMethod dieMethod = ctCreature.getMethod(methodName(), methodDescriptor());

            String proxyClass = ProxyServerHook.class.getName();

            // ✅ CLEAN: Capture attackers BEFORE die() clears them
            // NO LOGIC - just routing to ProxyServerHook -> EventLogic
            dieMethod.insertBefore(
                "{ " +
                "  try {" +
                "    " + proxyClass + ".captureAttackersViaLogic(this);" +
                "  } catch (Throwable t) {" +
                "    t.printStackTrace();" +
                "  }" +
                "}"
            );

            // ✅ CLEAN: Fire event AFTER die() completes
            // NO LOGIC - just routing to ProxyServerHook -> EventLogic
            dieMethod.insertAfter(
                "{ " +
                "  try {" +
                "    java.util.Map damageMap = " + proxyClass + ".getAndRemoveDamageTracking(this.getWurmId());" +
                "    " + proxyClass + ".fireCreatureDeathEventWithLogic(this, damageMap);" +
                "  } catch (Throwable t) {" +
                "    t.printStackTrace();" +
                "  }" +
                "}"
            );

            LOGGER.info("[BytecodePatch] Registered CreatureDeathPatch successfully (clean architecture)");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureDeathPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureDeathPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] CreatureDeathEvent will not fire - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_DEATH);
    }
}
