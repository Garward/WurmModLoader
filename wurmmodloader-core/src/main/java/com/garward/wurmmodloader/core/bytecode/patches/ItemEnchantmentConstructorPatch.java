package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Makes ItemEnchantment constructor public so mods can extend it.
 * 
 * The constructor is package-private by default, which prevents mod-loaded
 * spell classes from extending ItemEnchantment even when in the same package
 * (due to different classloaders).
 */
public class ItemEnchantmentConstructorPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemEnchantmentConstructorPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.spells.ItemEnchantment";
    }

    @Override
    public String methodName() {
        return "<init>"; // Constructor
    }

    @Override
    public String methodDescriptor() {
        return "(Ljava/lang/String;IIIIIJ)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctItemEnchantment = classPool.get(targetClassName());

            if (ctItemEnchantment.isFrozen()) {
                ctItemEnchantment.defrost();
            }

            // Find the package-private constructor
            CtConstructor constructor = ctItemEnchantment.getConstructor(methodDescriptor());

            // Make it public
            constructor.setModifiers(Modifier.PUBLIC);

            LOGGER.info("[BytecodePatch] Made ItemEnchantment constructor public");
        } catch (NotFoundException e) {
            throw new IllegalStateException("Unable to find ItemEnchantment constructor", e);
        }
    }

    @Override
    public int priority() {
        return 100; // High priority - needs to run early before mods load
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton("ITEM_ENCHANTMENT_CONSTRUCTOR");
    }
}
