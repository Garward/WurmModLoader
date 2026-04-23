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
 * Patches {@code Players.resetFaithGain()} (the daily tick clearing per-player
 * faith-gain counters) to fire {@code FaithGainResetEvent}. Cancellation skips
 * the call to {@code PlayerInfoFactory.resetFaithGain()}; replacement mods
 * perform their own reset inside the handler.
 */
public final class FaithGainResetPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(FaithGainResetPatch.class.getName());

    @Override public String targetClassName()  { return "com.wurmonline.server.Players"; }
    @Override public String methodName()       { return "resetFaithGain"; }
    @Override public String methodDescriptor() { return "()V"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());
            if (ct.isFrozen()) ct.defrost();

            CtMethod method = ct.getMethod(methodName(), methodDescriptor());

            String proxy = ProxyServerHook.class.getName();
            String code =
                "{\n" +
                "    try {\n" +
                "        if (" + proxy + ".fireFaithGainResetEvent()) {\n" +
                "            return;\n" +
                "        }\n" +
                "    } catch (Exception _e) {\n" +
                "        java.util.logging.Logger.getLogger(\"ProxyServerHook\")\n" +
                "            .log(java.util.logging.Level.WARNING,\n" +
                "                 \"Failed to fire FaithGainResetEvent\", _e);\n" +
                "    }\n" +
                "}\n";

            method.insertBefore(code);
            LOGGER.info("Registered FaithGainResetPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install FaithGainResetPatch", e);
        }
    }

    @Override public int priority() { return 50; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.FAITH_GAIN_RESET);
    }
}
