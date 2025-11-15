package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.*;

/**
 * Fires BodyMenuPopulateEvent when body context menu is being populated.
 *
 * <p>This patch allows mods to add custom entries to the body right-click menu
 * (the menu that appears when examining yourself).</p>
 *
 * <p><strong>Hook Location:</strong> {@code BodyPartBehaviour.getBehavioursFor()}
 * immediately before returning the menu list.</p>
 *
 * <p><strong>Enabled Features:</strong></p>
 * <ul>
 *   <li>Mod menus (Power Scaling, Upgrade Trees, etc.)</li>
 *   <li>Custom character management UIs</li>
 *   <li>Quest logs, achievement trackers</li>
 *   <li>Any player-specific interface accessed via body menu</li>
 * </ul>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public final class BodyMenuPopulatePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(BodyMenuPopulatePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.BodyPartBehaviour";
    }

    @Override
    public String methodName() {
        return "getBehavioursFor";
    }

    @Override
    public String methodDescriptor() {
        // public List<ActionEntry> getBehavioursFor(Creature performer, Item object)
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Ljava/util/List;";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctBodyPartBehaviour = classPool.get(targetClassName());
            CtMethod getBehavioursFor = ctBodyPartBehaviour.getMethod(methodName(), methodDescriptor());

            // Insert at the end of method (insertAfter with $_) to modify return value
            String code =
                "try {\n" +
                "    java.util.logging.Logger.getLogger(\"BodyMenuPatch\").info(\n" +
                "        \"[BYTECODE] getBehavioursFor() completed - checking if body part attached\");\n" +
                "    if ($2.isBodyPartAttached()) {\n" + // $2 = object parameter
                "        java.util.logging.Logger.getLogger(\"BodyMenuPatch\").info(\n" +
                "            \"[BYTECODE] Body part attached - firing event\");\n" +
                "        " + ProxyServerHook.class.getName() + ".fireBodyMenuPopulateEvent(\n" +
                "            $1, $2, $_\n" + // $1 = performer, $2 = object, $_ = return value
                "        );\n" +
                "        java.util.logging.Logger.getLogger(\"BodyMenuPatch\").info(\n" +
                "            \"[BYTECODE] Event fired successfully\");\n" +
                "    } else {\n" +
                "        java.util.logging.Logger.getLogger(\"BodyMenuPatch\").info(\n" +
                "            \"[BYTECODE] Body part not attached - skipping event\");\n" +
                "    }\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"WurmModLoader.BodyMenuPopulatePatch\")\n" +
                "        .log(java.util.logging.Level.SEVERE,\n" +
                "             \"[BYTECODE] CRASH in fireBodyMenuPopulateEvent\", e);\n" +
                "    throw e;\n" +
                "}";

            // Insert after method execution, before return
            getBehavioursFor.insertAfter(code);

            LOGGER.info("[BytecodePatch] Registered BodyMenuPopulateEvent patch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install BodyMenuPopulatePatch", e);
        }
    }

    @Override
    public int priority() {
        return 50; // mid-range — UI events are generally safe
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.BODY_MENU);
    }
}
