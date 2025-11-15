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
 * Hooks into prayer/faith methods to fire PrayerFaithEvent.
 * Allows mods to modify prayer counts and time delays (for unlimited prayers).
 */
public final class PrayerFaithPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(PrayerFaithPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.players.DbPlayerInfo";
    }

    @Override
    public String methodName() {
        return "setNumFaith";
    }

    @Override
    public String methodDescriptor() {
        return "(BJ)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctDbPlayerInfo = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctDbPlayerInfo.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping PrayerFaithPatch - DbPlayerInfo class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] PrayerFaithEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            CtMethod setNumFaithMethod = ctDbPlayerInfo.getMethod(methodName(), methodDescriptor());

            String proxyClass = ProxyServerHook.class.getName();

            // Fire event at the start of setNumFaith to allow mods to modify values
            StringBuilder eventCode = new StringBuilder();
            eventCode.append("{\n");
            eventCode.append("    Object[] result = ").append(proxyClass).append(".firePrayerFaithEvent(\n");
            eventCode.append("        this.wurmId,\n");
            eventCode.append("        $1,  // numFaith parameter\n");
            eventCode.append("        $2   // lastFaith parameter\n");
            eventCode.append("    );\n");
            eventCode.append("    $1 = ((Byte) result[0]).byteValue();\n");
            eventCode.append("    $2 = ((Long) result[1]).longValue();\n");
            eventCode.append("    this.numFaith = $1;\n");
            eventCode.append("    this.lastFaith = $2;\n");
            eventCode.append("}\n");

            setNumFaithMethod.insertBefore(eventCode.toString());

            LOGGER.info("Registered PrayerFaithEvent patch for setNumFaith");

            // Also hook checkPrayerFaith to fire event before the check
            try {
                CtClass ctPlayerInfo = classPool.get("com.wurmonline.server.players.PlayerInfo");
                if (!ctPlayerInfo.isFrozen()) {
                    CtMethod checkMethod = ctPlayerInfo.getMethod("checkPrayerFaith", "()Z");

                    StringBuilder checkEventCode = new StringBuilder();
                    checkEventCode.append("{\n");
                    checkEventCode.append("    Object[] result = ").append(proxyClass).append(".firePrayerFaithEvent(\n");
                    checkEventCode.append("        this.wurmId,\n");
                    checkEventCode.append("        this.numFaith,\n");
                    checkEventCode.append("        this.lastFaith\n");
                    checkEventCode.append("    );\n");
                    checkEventCode.append("    this.numFaith = ((Byte) result[0]).byteValue();\n");
                    checkEventCode.append("    this.lastFaith = ((Long) result[1]).longValue();\n");
                    checkEventCode.append("}\n");

                    checkMethod.insertBefore(checkEventCode.toString());

                    LOGGER.info("Registered PrayerFaithEvent patch for checkPrayerFaith");
                }
            } catch (NotFoundException e) {
                LOGGER.warning("Could not find PlayerInfo.checkPrayerFaith method");
            }

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install PrayerFaithPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping PrayerFaithPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] PrayerFaithEvent will not fire - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 75;  // After most other patches
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.PRAYER_FAITH);
    }
}
