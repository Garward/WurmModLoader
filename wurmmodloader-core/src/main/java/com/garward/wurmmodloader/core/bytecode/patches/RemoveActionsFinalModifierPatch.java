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

            // Get the actionEntrys field - use getDeclaredField to avoid parent class lookup
            CtField actionEntrysField = ctActions.getDeclaredField("actionEntrys");
            int oldModifiers = actionEntrysField.getModifiers();

            LOGGER.info("[BytecodePatch] Actions.actionEntrys old modifiers: " + Modifier.toString(oldModifiers));
            LOGGER.info("[BytecodePatch] Is final? " + Modifier.isFinal(oldModifiers));
            LOGGER.info("[BytecodePatch] Is static? " + Modifier.isStatic(oldModifiers));
            LOGGER.info("[BytecodePatch] Is public? " + Modifier.isPublic(oldModifiers));

            // Remove final modifier (even if not detected - bytecode might differ from source)
            int newModifiers = Modifier.clear(oldModifiers, Modifier.FINAL);
            actionEntrysField.setModifiers(newModifiers);

            // Verify the change was applied
            int verifyModifiers = actionEntrysField.getModifiers();
            boolean stillFinal = Modifier.isFinal(verifyModifiers);

            LOGGER.info("[BytecodePatch] Actions.actionEntrys NEW modifiers: " + Modifier.toString(newModifiers));
            LOGGER.info("[BytecodePatch] Verification - still final? " + stillFinal);

            if (stillFinal) {
                LOGGER.warning("[BytecodePatch] WARNING: Field still shows as final after modification!");
                LOGGER.warning("[BytecodePatch] This may be a Javassist bug or the field wasn't actually final");
            } else {
                LOGGER.info("[BytecodePatch] Successfully removed 'final' modifier from Actions.actionEntrys");
            }

            LOGGER.info("[BytecodePatch] ModActions can now safely expand the actionEntrys array");
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
