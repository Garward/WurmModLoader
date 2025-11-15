package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Removes the 'final' modifier from Actions.actionEntrys field to allow safe array expansion.
 * 
 * <p><strong>Problem:</strong> Actions.actionEntrys is declared 'public static final'.
 * The JVM aggressively optimizes final fields by caching references and inlining array lengths.
 * When ModActions uses reflection to expand the array, other threads may never see the update
 * due to Java Memory Model semantics around final fields.</p>
 * 
 * <p><strong>Solution:</strong> Remove the 'final' modifier from the field BEFORE any mods load,
 * making the array expansion visible to all threads.</p>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public final class RemoveActionsFinalModifierPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(RemoveActionsFinalModifierPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.Actions";
    }

    @Override
    public String methodName() {
        return null; // Patching field, not method
    }

    @Override
    public String methodDescriptor() {
        return null; // Patching field, not method
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctActions = classPool.get(targetClassName());

            if (ctActions.isFrozen()) {
                ctActions.defrost();
            }

            // Remove 'final' modifier from actionEntrys field
            CtField actionEntrysField = ctActions.getField("actionEntrys");
            int oldModifiers = actionEntrysField.getModifiers();

            LOGGER.warning("[BytecodePatch] Actions.actionEntrys old modifiers: " + Modifier.toString(oldModifiers));
            LOGGER.warning("[BytecodePatch] Is final? " + Modifier.isFinal(oldModifiers));
            LOGGER.warning("[BytecodePatch] Is static? " + Modifier.isStatic(oldModifiers));
            LOGGER.warning("[BytecodePatch] Is public? " + Modifier.isPublic(oldModifiers));

            // Force remove final modifier regardless
            int newModifiers = Modifier.clear(oldModifiers, Modifier.FINAL);
            actionEntrysField.setModifiers(newModifiers);

            LOGGER.warning("[BytecodePatch] Actions.actionEntrys NEW modifiers: " + Modifier.toString(newModifiers));
            LOGGER.warning("[BytecodePatch] REMOVED 'final' modifier from Actions.actionEntrys field");
            LOGGER.warning("[BytecodePatch] This allows ModActions to safely expand the array");

            LOGGER.info("[BytecodePatch] Successfully patched Actions.actionEntrys field modifiers");
        } catch (NotFoundException e) {
            throw new IllegalStateException("Unable to install RemoveActionsFinalModifierPatch", e);
        }
    }

    @Override
    public int priority() {
        return 200; // VERY HIGH PRIORITY - Must run before any mods register actions
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.emptyList();
    }
}
