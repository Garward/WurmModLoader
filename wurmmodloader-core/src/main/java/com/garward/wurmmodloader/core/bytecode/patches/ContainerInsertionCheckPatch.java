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
 * Patches the private {@code Item.testInsertHollowItem(Item, boolean)} check
 * to fire {@code ContainerInsertionCheckEvent}. Cancellation returns
 * {@code false} (insertion rejected).
 */
public final class ContainerInsertionCheckPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ContainerInsertionCheckPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.items.Item"; }
    @Override public String methodName()       { return "testInsertHollowItem"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/items/Item;Z)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            // private method on Item — use getDeclaredMethod.
            CtMethod method = ct.getDeclaredMethod(methodName());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireContainerInsertionCheckEvent(this, $1, $2)) {\n" +
                "            return false;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire ContainerInsertionCheckEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered ContainerInsertionCheckPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ContainerInsertionCheckPatch", e);
        }
    }

    @Override public int priority() { return 50; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CONTAINER_INSERTION_CHECK);
    }
}
