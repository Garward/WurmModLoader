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
 * Fires TameAttemptEvent (CHARM) on CharmAnimal.precondition, and
 * TameCompleteEvent on CharmAnimal.doEffect after the pet binding succeeds.
 */
public final class CharmAnimalPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CharmAnimalPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.spells.CharmAnimal";
    }

    @Override public String methodName() { return "precondition"; }
    @Override public String methodDescriptor() {
        return "(Lcom/wurmonline/server/skills/Skill;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ct = classPool.get(targetClassName());

            if (ct.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CharmAnimalPatch - CharmAnimal already frozen");
                return;
            }

            String proxy = ProxyServerHook.class.getName();

            CtMethod precondition = ct.getMethod(methodName(), methodDescriptor());
            precondition.insertBefore(
                "{ try {" +
                "    if (" + proxy + ".fireTameAttemptEventCharm($2, $3)) {" +
                "        return false;" +
                "    }" +
                "  } catch (Throwable t) { t.printStackTrace(); } }"
            );

            CtMethod doEffect = ct.getMethod(
                "doEffect",
                "(Lcom/wurmonline/server/skills/Skill;DLcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;)V"
            );
            // Fire after the method completes so setPet/setDominator have run.
            doEffect.insertAfter(
                "{ try {" +
                "    if ($2 > 0.0D && $4.isDominated() && $4.getDominator() == $3) {" +
                "        " + proxy + ".fireTameCompleteEventCharm($3, $4, $2);" +
                "    }" +
                "  } catch (Throwable t) { t.printStackTrace(); } }"
            );

            LOGGER.info("[BytecodePatch] Registered CharmAnimalPatch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CharmAnimalPatch", e);
        }
    }

    @Override public int priority() { return 75; }

    @Override public Collection<String> conflictKeys() {
        return Arrays.asList(BytecodeConflictKeys.TAME_CHARM);
    }
}
