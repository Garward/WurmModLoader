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
 * Appends the framework's GM help addendum to the chat after vanilla
 * {@code Communicator.handleHashMessageHelp(int)} finishes printing.
 */
public final class GmHelpAddendumPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(GmHelpAddendumPatch.class.getName());

    @Override public String targetClassName() { return "com.wurmonline.server.creatures.Communicator"; }
    @Override public String methodName()       { return "handleHashMessageHelp"; }
    @Override public String methodDescriptor() { return "(I)V"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctComm = classPool.get(targetClassName());
            CtMethod m = ctComm.getMethod(methodName(), methodDescriptor());

            // Populate the searchable help index from sendDeityHelp's bytecode
            // while we already have the Communicator CtClass loaded.
            try {
                com.garward.wurmmodloader.core.console.GmHelpIndex.populateVanilla(ctComm);
            } catch (Throwable __t) {
                LOGGER.log(java.util.logging.Level.WARNING,
                    "[GmHelpAddendumPatch] populateVanilla failed", __t);
            }

            String code =
                "try {\n" +
                "    " + ProxyServerHook.class.getName() + ".gmHelpAddendumHook(this, $1);\n" +
                "} catch (Throwable __t) {\n" +
                "    java.util.logging.Logger.getLogger(\"" + GmHelpAddendumPatch.class.getName() + "\")\n" +
                "        .log(java.util.logging.Level.WARNING, \"GM help addendum threw\", __t);\n" +
                "}\n";
            m.insertAfter(code);
            LOGGER.info("[BytecodePatch] Registered GmHelpAddendumPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install GmHelpAddendumPatch", e);
        }
    }

    @Override public int priority() { return 50; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.GM_HELP_ADDENDUM);
    }
}
