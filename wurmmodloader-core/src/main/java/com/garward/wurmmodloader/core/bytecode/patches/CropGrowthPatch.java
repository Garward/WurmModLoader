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
 * Hooks into CropTilePoller.checkForFarmGrowth() to fire CropGrowthEvent.
 * Allows mods to cancel growth checks (e.g., to disable weeds or control crop aging).
 */
public final class CropGrowthPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CropGrowthPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.zones.CropTilePoller";
    }

    @Override
    public String methodName() {
        return "checkForFarmGrowth";
    }

    @Override
    public String methodDescriptor() {
        // void checkForFarmGrowth(int tile, int tilex, int tiley, byte type, byte aData, MeshIO currentMesh, boolean pollingSurface)
        return "(IIIBBLcom/wurmonline/mesh/MeshIO;Z)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCropTilePoller = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctCropTilePoller.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CropGrowthPatch - CropTilePoller class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] CropGrowthEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            CtMethod checkMethod = ctCropTilePoller.getMethod(methodName(), methodDescriptor());

            String proxyClass = ProxyServerHook.class.getName();

            // Fire event at the start of the method
            // If cancelled, return early to skip growth check
            // Parameters: (int tile, int tilex, int tiley, byte type, byte aData, MeshIO currentMesh, boolean pollingSurface)
            StringBuilder eventCode = new StringBuilder();
            eventCode.append("{\n");
            eventCode.append("    boolean cancelled = ").append(proxyClass).append(".fireCropGrowthEvent(\n");
            eventCode.append("        $2,  // tilex\n");
            eventCode.append("        $3,  // tiley\n");
            eventCode.append("        $1,  // tile\n");
            eventCode.append("        $4,  // type (was 'data')\n");
            eventCode.append("        $5   // aData (was 'farmData')\n");
            eventCode.append("    );\n");
            eventCode.append("    if (cancelled) {\n");
            eventCode.append("        return;\n");
            eventCode.append("    }\n");
            eventCode.append("}\n");

            checkMethod.insertBefore(eventCode.toString());

            LOGGER.info("Registered CropGrowthEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CropGrowthPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping CropGrowthPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] CropGrowthEvent will not fire - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 65;  // After creature/combat patches
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CROP_GROWTH);
    }
}
