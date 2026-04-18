package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches Item.setDamage() to fire ItemDamageEvent.
 * Allows mods to modify or prevent item damage.
 *
 * Targets:
 * - com.wurmonline.server.items.DbItem.setDamage(float)
 * - com.wurmonline.server.items.TempItem.setDamage(float)
 */
public class ItemDamagePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemDamagePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.DbItem"; // Primary target
    }

    @Override
    public String methodName() {
        return "setDamage";
    }

    @Override
    public String methodDescriptor() {
        return "(F)Z"; // Returns boolean, not void
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();

            // Patch both DbItem and TempItem (concrete implementations)
            patchItemClass(classPool, "com.wurmonline.server.items.DbItem");
            patchItemClass(classPool, "com.wurmonline.server.items.TempItem");

            LOGGER.info("[BytecodePatch] Registered ItemDamagePatch successfully");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ItemDamagePatch", e);
        }
    }

    private void patchItemClass(ClassPool classPool, String className) throws NotFoundException, CannotCompileException {
        CtClass ctItem = classPool.get(className);
        CtMethod setDamageMethod = ctItem.getDeclaredMethod("setDamage", new CtClass[]{CtPrimitiveType.floatType});

        setDamageMethod.insertBefore(
            "{ " +
            "  $1 = " + ProxyServerHook.class.getName() + ".fireItemDamageEvent(" +
            "    this.getWurmId(), " +
            "    this.getName(), " +
            "    $1, " +
            "    this.getDamage()" +
            "  ); " +
            "}"
        );

        LOGGER.info("[BytecodePatch] Patched " + className + ".setDamage()");
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton("ITEM_DAMAGE");
    }
}
