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
 * Hooks into Terraforming.harvest() to fire CropHarvestEvent.
 * Allows mods to modify harvest quantities (e.g., extra harvest bonuses).
 *
 * <p>This patch injects code right after quantity is calculated and before
 * items are created, using ExprEditor to find the "You managed to get a yield" message.</p>
 */
public final class CropHarvestPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CropHarvestPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.Terraforming";
    }

    @Override
    public String methodName() {
        return "harvest";
    }

    @Override
    public String methodDescriptor() {
        // boolean harvest(Creature performer, int tilex, int tiley, boolean onSurface, int tile, float counter, Item tool)
        return "(Lcom/wurmonline/server/creatures/Creature;IIZIFLcom/wurmonline/server/items/Item;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctTerraforming = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctTerraforming.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CropHarvestPatch - Terraforming class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] CropHarvestEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            CtMethod harvestMethod = ctTerraforming.getMethod(methodName(), methodDescriptor());

            String proxyClass = ProxyServerHook.class.getName();

            // Inject code right before the try block that creates items
            // We'll use MethodCall editor to find Crops.getCropName() as a marker
            // This call always happens and is right before the yield message
            StringBuilder hookCode = new StringBuilder();
            hookCode.append("// Fire CropHarvestEvent to allow mods to modify harvest quantity\n");
            hookCode.append("quantity = ").append(proxyClass).append(".fireCropHarvestEvent(\n");
            hookCode.append("    performer, tilex, tiley, onSurface, tile, counter, item, quantity);\n");

            // Find Crops.getCropName() call - it always executes and sets cropString variable
            // We inject right after it, modifying quantity before it's used in the message
            final boolean[] patched = {false};
            harvestMethod.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    // Find the getCropName() static method call
                    // This happens right before "You managed to get a yield" message
                    if (!patched[0] && m.getMethodName().equals("getCropName") &&
                        m.getClassName().equals("com.wurmonline.server.crops.Crops")) {
                        try {
                            // Call the original method, then inject our event code
                            // The hookCode will modify quantity variable
                            m.replace("{ $_ = $proceed($$); " + hookCode.toString() + " }");
                            patched[0] = true;
                            LOGGER.fine("Injected CropHarvestEvent after getCropName");
                        } catch (Exception e) {
                            throw new CannotCompileException("Failed to inject CropHarvestEvent", e);
                        }
                    }
                }
            });

            if (!patched[0]) {
                LOGGER.warning("[BytecodePatch] CropHarvestPatch could not find injection point - ItemFactory.createItem not found");
                LOGGER.warning("[BytecodePatch] CropHarvestEvent will not fire - this is not critical but harvest bonuses won't work");
            } else {
                LOGGER.info("Registered CropHarvestEvent patch");
            }
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CropHarvestPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping CropHarvestPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] CropHarvestEvent will not fire - legacy mod conflict");
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
        return Collections.singleton(BytecodeConflictKeys.CROP_HARVEST);
    }
}
