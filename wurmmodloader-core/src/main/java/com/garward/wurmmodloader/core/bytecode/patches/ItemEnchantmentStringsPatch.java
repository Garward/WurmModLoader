package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Injects ItemEnchantmentStringsEvent via Item.sendEnchantmentStrings().
 *
 * <p>This event is fired when enchantment strings are about to be sent to the player,
 * allowing mods to send custom colored messages about item stats, damage, etc.</p>
 */
public final class ItemEnchantmentStringsPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemEnchantmentStringsPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Item";
    }

    @Override
    public String methodName() {
        return "sendEnchantmentStrings";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Communicator;)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctItem = classPool.get(targetClassName());
            if (ctItem.isFrozen()) {
                ctItem.defrost();
            }
            CtMethod sendEnchantmentStrings = ctItem.getMethod(methodName(), methodDescriptor());

            // Fire event at the end of the method to allow mods to send additional messages
            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ItemEnchantmentStringsPatch\")\n");
            code.append("            .info(\"[PATCH DEBUG] sendEnchantmentStrings called on item: \" + this.getTemplate().getName());\n");
            code.append("        com.wurmonline.server.creatures.Creature examiner = ").append("$1.getPlayer();\n");
            code.append("        java.util.logging.Logger.getLogger(\"ItemEnchantmentStringsPatch\")\n");
            code.append("            .info(\"[PATCH DEBUG] getPlayer() returned: \" + (examiner == null ? \"NULL\" : examiner.getName()));\n");
            code.append("        if (examiner != null) {\n");
            code.append("            java.util.logging.Logger.getLogger(\"ItemEnchantmentStringsPatch\")\n");
            code.append("                .info(\"[PATCH DEBUG] About to fire event for \" + examiner.getName());\n");
            code.append("            ").append(ProxyServerHook.class.getName())
                .append(".fireItemEnchantmentStringsEvent(this, examiner);\n");
            code.append("            java.util.logging.Logger.getLogger(\"ItemEnchantmentStringsPatch\")\n");
            code.append("                .info(\"[PATCH DEBUG] Event fired successfully\");\n");
            code.append("        } else {\n");
            code.append("            java.util.logging.Logger.getLogger(\"ItemEnchantmentStringsPatch\")\n");
            code.append("                .info(\"[PATCH DEBUG] Examiner was NULL, skipping event\");\n");
            code.append("        }\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ItemEnchantmentStringsPatch\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"[PATCH DEBUG] Exception in ItemEnchantmentStringsPatch\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            sendEnchantmentStrings.insertAfter(code.toString());
            LOGGER.info("Registered ItemEnchantmentStringsEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ItemEnchantmentStringsPatch", e);
        }
    }

    @Override
    public int priority() {
        return 46;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ITEM_ENCHANTMENT_STRINGS);
    }
}
