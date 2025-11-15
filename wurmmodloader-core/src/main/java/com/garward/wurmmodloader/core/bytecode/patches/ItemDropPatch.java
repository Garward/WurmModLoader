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
 * Allows mods to veto item drops via ItemDropEvent.
 */
public final class ItemDropPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemDropPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Item";
    }

    @Override
    public String methodName() {
        return "dropItem";
    }

    @Override
    public String methodDescriptor() {
        return "(JZ)Lcom/wurmonline/server/items/Item;";
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
            CtMethod dropItem = ctItem.getMethod(methodName(), methodDescriptor());

            StringBuilder code = new StringBuilder();
            code.append("{\n");
            code.append("    try {\n");
            code.append("        boolean cancelled = ").append(ProxyServerHook.class.getName())
                .append(".fireItemDropEvent($1, this.getOwnerId(), $2);\n");
            code.append("        if (cancelled) {\n");
            code.append("            throw new com.wurmonline.server.NoSuchItemException(\"Item drop prevented by mod\");\n");
            code.append("        }\n");
            code.append("    } catch (com.wurmonline.server.NoSuchItemException e) {\n");
            code.append("        throw e;\n");
            code.append("    } catch (Exception e) {\n");
            code.append("        e.printStackTrace();\n");
            code.append("    }\n");
            code.append("}\n");

            dropItem.insertBefore(code.toString());
            LOGGER.info("Registered ItemDropEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ItemDropPatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ITEM_DROP);
    }
}
