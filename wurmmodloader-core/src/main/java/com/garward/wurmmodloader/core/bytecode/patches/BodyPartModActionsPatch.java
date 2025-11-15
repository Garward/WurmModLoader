package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.*;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Enables ModActions support for body part menus by delegating unknown action IDs
 * to registered ActionPerformer handlers.
 *
 * <p><strong>Problem:</strong> BodyPartBehaviour.action() only handles hardcoded vanilla
 * action IDs (1, 91, 389, etc.). When a custom ModAction is clicked, the action ID
 * falls through unhandled, causing server crash.</p>
 *
 * <p><strong>Solution:</strong> Inject code at the START of action() methods to check
 * for ModActions (ID >= 900) and delegate to registered performers, returning early
 * to prevent vanilla code from handling unknown IDs.</p>
 *
 * <p><strong>Impact:</strong> This enables mods to add custom buttons to the body menu
 * (right-click on body) that actually work when clicked, unlocking a major UI capability
 * for Wurm modding.</p>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public final class BodyPartModActionsPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(BodyPartModActionsPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.BodyPartBehaviour";
    }

    @Override
    public String methodName() {
        // We need to patch BOTH action() method overloads
        return null; // Will use custom apply logic
    }

    @Override
    public String methodDescriptor() {
        return null; // Will use custom apply logic
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctBodyPartBehaviour = classPool.get(targetClassName());

            // Defrost if frozen (critical for bytecode modification)
            boolean wasFrozen = ctBodyPartBehaviour.isFrozen();
            if (wasFrozen) {
                LOGGER.severe("[BytecodePatch] BodyPartBehaviour WAS FROZEN - defrosting now");
                ctBodyPartBehaviour.defrost();
            } else {
                LOGGER.severe("[BytecodePatch] BodyPartBehaviour was NOT frozen");
            }

            // Patch method 1: action(Action, Creature, Item, Item, short, float)
            LOGGER.severe("[BytecodePatch] About to patch action(WITH SOURCE)");
            patchActionWithSource(ctBodyPartBehaviour);
            LOGGER.severe("[BytecodePatch] Successfully patched action(WITH SOURCE)");

            // Patch method 2: action(Action, Creature, Item, short, float)
            LOGGER.severe("[BytecodePatch] About to patch action(WITHOUT SOURCE)");
            patchActionWithoutSource(ctBodyPartBehaviour);
            LOGGER.severe("[BytecodePatch] Successfully patched action(WITHOUT SOURCE)");

            LOGGER.info("[BytecodePatch] Registered BodyPartModActionsPatch - enables custom body menu actions!");
        } catch (NotFoundException | CannotCompileException e) {
            LOGGER.severe("[BytecodePatch] EXCEPTION during patch: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Unable to install BodyPartModActionsPatch", e);
        }
    }

    /**
     * Patches: action(Action act, Creature performer, Item source, Item target, short action, float counter)
     *
     * Injects code at the START of the method to intercept ModActions (ID >= 900) before
     * vanilla code tries to handle them.
     */
    private void patchActionWithSource(CtClass ctBodyPartBehaviour) throws NotFoundException, CannotCompileException {
        String descriptor = "(Lcom/wurmonline/server/behaviours/Action;" +
                           "Lcom/wurmonline/server/creatures/Creature;" +
                           "Lcom/wurmonline/server/items/Item;" +
                           "Lcom/wurmonline/server/items/Item;" +
                           "SF)Z";

        CtMethod actionMethod = ctBodyPartBehaviour.getMethod("action", descriptor);

        // Inject ModActions routing code
        String code =
            "{\n" +
            "    com.garward.wurmmodloader.modsupport.actions.ActionPerformerBase performer = " +
            "        com.garward.wurmmodloader.modsupport.actions.ModActions.getActionPerformer($1);\n" +
            "    if (performer != null) {\n" +
            "        boolean result = performer.action($1, $2, $3, $4, $5, $6);\n" +
            "        return result;\n" +
            "    }\n" +
            "}\n";

        // Insert at the BEGINNING of the method
        try {
            LOGGER.severe("[BytecodePatch] Calling insertBefore() on action(WITH SOURCE)...");
            actionMethod.insertBefore(code);
            LOGGER.severe("[BytecodePatch] insertBefore() succeeded for action(WITH SOURCE)");
        } catch (CannotCompileException e) {
            LOGGER.severe("[BytecodePatch] CannotCompileException in insertBefore(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        LOGGER.info("[BytecodePatch] Patched BodyPartBehaviour.action(Action, Creature, Item, Item, short, float)");
    }

    /**
     * Patches: action(Action act, Creature performer, Item target, short action, float counter)
     *
     * Injects code at the START of the method to intercept ModActions (ID >= 900) before
     * vanilla code tries to handle them.
     */
    private void patchActionWithoutSource(CtClass ctBodyPartBehaviour) throws NotFoundException, CannotCompileException {
        String descriptor = "(Lcom/wurmonline/server/behaviours/Action;" +
                           "Lcom/wurmonline/server/creatures/Creature;" +
                           "Lcom/wurmonline/server/items/Item;" +
                           "SF)Z";

        CtMethod actionMethod = ctBodyPartBehaviour.getMethod("action", descriptor);

        // Inject ModActions routing code
        String code =
            "{\n" +
            "    com.garward.wurmmodloader.modsupport.actions.ActionPerformerBase performer = " +
            "        com.garward.wurmmodloader.modsupport.actions.ModActions.getActionPerformer($1);\n" +
            "    if (performer != null) {\n" +
            "        boolean result = performer.action($1, $2, $3, $4, $5);\n" +
            "        return result;\n" +
            "    }\n" +
            "}\n";

        // Insert at the BEGINNING of the method
        try {
            LOGGER.severe("[BytecodePatch] Calling insertBefore() on action(WITHOUT SOURCE)...");
            actionMethod.insertBefore(code);
            LOGGER.severe("[BytecodePatch] insertBefore() succeeded for action(WITHOUT SOURCE)");
        } catch (CannotCompileException e) {
            LOGGER.severe("[BytecodePatch] CannotCompileException in insertBefore(): " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        LOGGER.info("[BytecodePatch] Patched BodyPartBehaviour.action(Action, Creature, Item, short, float)");
    }

    @Override
    public int priority() {
        return 100; // High priority - critical for mod UI functionality
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.BODY_MENU_ACTIONS);
    }
}
