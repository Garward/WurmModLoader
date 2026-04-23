package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Patches {@code Item.moveToItem(Creature, long, boolean)} to fire
 * {@code ItemMoveCheckEvent}. Cancellation returns {@code false} and delivers
 * an optional deny-reason alert to the mover.
 */
public final class ItemMoveCheckPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemMoveCheckPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Item";
    }

    @Override
    public String methodName() {
        return "moveToItem";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;JZ)Z";
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
            CtMethod method = ctItem.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireItemMoveCheckEvent(this, $1, $2, $3)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire ItemMoveCheckEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered ItemMoveCheckPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ItemMoveCheckPatch", e);
        }
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.ITEM_MOVE_CHECK);
    }
}
