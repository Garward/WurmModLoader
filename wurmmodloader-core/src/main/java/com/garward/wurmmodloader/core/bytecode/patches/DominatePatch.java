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

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Fires TameAttemptEvent (DOMINATE) on Dominate.mayDominate, and
 * TameCompleteEvent on Dominate.dominate after the pet binding succeeds.
 */
public final class DominatePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(DominatePatch.class.getName());

    @Override public String targetClassName() { return "com.wurmonline.server.spells.Dominate"; }
    @Override public String methodName() { return "mayDominate"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());

            if (ct.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping DominatePatch - Dominate already frozen");
                return;
            }

            String proxy = ProxyServerHook.class.getName();

            CtMethod may = ct.getMethod(methodName(), methodDescriptor());
            may.insertBefore(
                "{ try {" +
                "    if (" + proxy + ".fireTameAttemptEventDominate($1, $2)) {" +
                "        return false;" +
                "    }" +
                "  } catch (Throwable t) { t.printStackTrace(); } }"
            );

            CtMethod dominate = ct.getMethod(
                "dominate",
                "(DLcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;)V"
            );
            dominate.insertAfter(
                "{ try {" +
                "    if ($3.isDominated() && $3.getDominator() == $2) {" +
                "        " + proxy + ".fireTameCompleteEventDominate($2, $3, $1);" +
                "    }" +
                "  } catch (Throwable t) { t.printStackTrace(); } }"
            );

            LOGGER.info("[BytecodePatch] Registered DominatePatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install DominatePatch", e);
        }
    }

    @Override public int priority() { return 75; }

    @Override public Collection<String> conflictKeys() {
        return Arrays.asList(BytecodeConflictKeys.TAME_DOMINATE);
    }
}
