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
import javassist.expr.ExprEditor;
import javassist.expr.Handler;

/**
 * Diagnostic-only: logs the throwable caught by every {@code catch (Throwable)}
 * handler inside {@code Server.run()}.
 *
 * <p>Vanilla {@code Server.run} ends with a panic-catch that calls
 * {@code this.shutDown()} directly when anything bubbles out of the main loop —
 * <em>bypassing</em> {@code startShutdown} and therefore the
 * {@link ServerStartShutdownLoggingPatch} forensics. The result was that a
 * mod-thrown {@code IllegalAccessError} could shut the server down with the
 * modloader log showing only the {@code Server.shutDown()} stack and no link
 * to the trigger.
 *
 * <p>This patch funnels every Throwable handler in {@code Server.run} through
 * the {@code ShutdownForensics} logger so panic-driven shutdowns produce the
 * same forensic trail as scheduled ones.
 */
public final class ServerRunPanicLoggingPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ServerRunPanicLoggingPatch.class.getName());
    private static final String FORENSICS_LOGGER = "com.garward.wurmmodloader.debug.ShutdownForensics";

    @Override public String targetClassName() { return "com.wurmonline.server.Server"; }
    @Override public String methodName()      { return "run"; }
    @Override public String methodDescriptor(){ return "()V"; }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctServer = classPool.get(targetClassName());
            CtMethod m = ctServer.getMethod(methodName(), methodDescriptor());

            final int[] instrumented = { 0 };
            m.instrument(new ExprEditor() {
                @Override
                public void edit(Handler h) throws CannotCompileException {
                    if (h.isFinally()) return;
                    String typeName;
                    try {
                        CtClass type = h.getType();
                        if (type == null) return;
                        typeName = type.getName();
                    } catch (NotFoundException nfe) {
                        return;
                    }
                    if (!"java.lang.Throwable".equals(typeName)
                            && !"java.lang.Error".equals(typeName)
                            && !"java.lang.Exception".equals(typeName)
                            && !"java.lang.RuntimeException".equals(typeName)) {
                        return;
                    }
                    String code =
                        "try {\n" +
                        "    java.util.logging.Logger.getLogger(\"" + FORENSICS_LOGGER + "\")\n" +
                        "        .log(java.util.logging.Level.SEVERE,\n" +
                        "             \"[ShutdownForensics] Server.run caught \" + $1.getClass().getName()\n" +
                        "             + \" — panic-shutdown likely imminent\",\n" +
                        "             (Throwable) $1);\n" +
                        "} catch (Throwable __ignore) {}\n";
                    h.insertBefore(code);
                    instrumented[0]++;
                }
            });
            LOGGER.info("[BytecodePatch] Registered ServerRunPanicLoggingPatch successfully ("
                    + instrumented[0] + " catch handler(s) instrumented).");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ServerRunPanicLoggingPatch", e);
        }
    }

    @Override public int priority() { return 90; }
    @Override public Collection<String> conflictKeys() {
        return Collections.singleton("server.lifecycle.runPanicLogging");
    }
}
