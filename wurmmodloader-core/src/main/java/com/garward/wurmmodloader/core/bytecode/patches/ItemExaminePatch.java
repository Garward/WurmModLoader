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
 * Injects ItemExamineEvent via Item.examine().
 */
public final class ItemExaminePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemExaminePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Item";
    }

    @Override
    public String methodName() {
        return "examine";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;)Ljava/lang/String;";
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
            CtMethod examine = ctItem.getMethod(methodName(), methodDescriptor());

            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ItemExaminePatch\")\n");
            code.append("            .info(\"[PATCH DEBUG] Item.examine() called on: \" + this.getTemplate().getName() + \" by \" + $1.getName());\n");
            code.append("        \n");
            code.append("        // Resolve body part owner (framework handles Wurm class access)\n");
            code.append("        com.wurmonline.server.creatures.Creature owner = null;\n");
            code.append("        if (this.isBodyPartAttached()) {\n");
            code.append("            long ownerId = this.getOwnerId();\n");
            code.append("            // Use framework CreatureResolver\n");
            code.append("            owner = com.garward.wurmmodloader.core.eventlogic.CreatureResolver.getCreatureOrNull(ownerId);\n");
            code.append("        }\n");
            code.append("        \n");
            code.append("        $_ = ").append(ProxyServerHook.class.getName())
                .append(".fireItemExamineEvent(this, $1, owner, $_);\n");
            code.append("        java.util.logging.Logger.getLogger(\"ItemExaminePatch\")\n");
            code.append("            .info(\"[PATCH DEBUG] ItemExamineEvent fired successfully\");\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        java.util.logging.Logger.getLogger(\"ItemExaminePatch\")\n");
            code.append("            .log(java.util.logging.Level.WARNING,\n");
            code.append("                 \"[PATCH DEBUG] Exception in ItemExaminePatch\", e);\n");
            code.append("    }\n");
            code.append("}\n");

            examine.insertAfter(code.toString());
            LOGGER.info("Registered ItemExamineEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ItemExaminePatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ITEM_EXAMINE);
    }
}
