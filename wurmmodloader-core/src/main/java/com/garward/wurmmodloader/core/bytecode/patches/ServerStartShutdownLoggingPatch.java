package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Diagnostic-only: logs the full caller stack of every {@code Server.startShutdown(int, String)}
 * invocation. Pinpoints which mod scheduled a shutdown — vanilla {@code checkShutDown} only
 * broadcasts at fixed thresholds (40m / 20m / ... / 3s), so a {@code startShutdown(0, ...)}
 * fires {@code shutDown()} silently 25 ms later.
 */
public final class ServerStartShutdownLoggingPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ServerStartShutdownLoggingPatch.class.getName());

    @Override public String targetClassName() { return "com.wurmonline.server.Server"; }
    @Override public String methodName()      { return "startShutdown"; }
    @Override public String methodDescriptor(){ return "(ILjava/lang/String;)V"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctServer = classPool.get(targetClassName());
            CtMethod m = ctServer.getMethod(methodName(), methodDescriptor());

            String code =
                "try {\n" +
                "    Throwable t = new Throwable(\"Server.startShutdown(\" + $1 + \"s, '\" + $2 + \"') invoked\");\n" +
                "    java.util.logging.Logger.getLogger(\"com.garward.wurmmodloader.debug.ShutdownForensics\")\n" +
                "        .log(java.util.logging.Level.WARNING,\n" +
                "             \"[ShutdownForensics] Server.startShutdown(\" + $1 + \"s, '\" + $2 + \"') — caller stack follows\",\n" +
                "             t);\n" +
                "} catch (Throwable ignore) {}\n";

            m.insertBefore(code);
            LOGGER.info("[BytecodePatch] Registered ServerStartShutdownLoggingPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ServerStartShutdownLoggingPatch", e);
        }
    }

    @Override public int priority() { return 90; }
    @Override public Collection<String> conflictKeys() { return Collections.singleton("server.lifecycle.startShutdown"); }
}
