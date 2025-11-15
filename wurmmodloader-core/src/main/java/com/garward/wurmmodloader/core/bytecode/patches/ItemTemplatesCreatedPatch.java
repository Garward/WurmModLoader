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
 * Fires {@link ProxyServerHook#fireOnItemTemplatesCreated()} after templates initialize.
 */
public final class ItemTemplatesCreatedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ItemTemplatesCreatedPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.ItemTemplateCreator";
    }

    @Override
    public String methodName() {
        return "initialiseItemTemplates";
    }

    @Override
    public String methodDescriptor() {
        return "()V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctTemplateCreator = classPool.get(targetClassName());
            CtMethod initialise = ctTemplateCreator.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".getInstance().fireOnItemTemplatesCreated();\n" +
                "} catch (Exception e) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + ItemTemplatesCreatedPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"Failed to fire item templates created event\", e);\n" +
                "}\n";

            initialise.insertAfter(code);
            LOGGER.info("[BytecodePatch] Registered ItemTemplatesCreatedPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ItemTemplatesCreatedPatch", e);
        }
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SERVER_LIFECYCLE_ITEM_TEMPLATES);
    }
}
