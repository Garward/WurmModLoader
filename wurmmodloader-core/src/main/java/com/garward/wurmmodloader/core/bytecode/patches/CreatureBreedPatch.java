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
 * Hooks into MethodsCreatures.breed() to fire CreatureBreedEvent.
 * Allows mods to detect breeding attempts and check for inbreeding.
 */
public final class CreatureBreedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CreatureBreedPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.MethodsCreatures";
    }

    @Override
    public String methodName() {
        return "breed";
    }

    @Override
    public String methodDescriptor() {
        // boolean breed(Creature performer, Creature target, short breedType, Action action, float counter)
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;SLcom/wurmonline/server/behaviours/Action;F)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctMethodsCreatures = classPool.get(targetClassName());

            // Check if class is already frozen (loaded by legacy mod)
            if (ctMethodsCreatures.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureBreedPatch - MethodsCreatures class already frozen (likely by legacy mod)");
                LOGGER.warning("[BytecodePatch] CreatureBreedEvent will not fire - consider porting legacy mod to event system");
                return;
            }

            CtMethod breedMethod = ctMethodsCreatures.getMethod(methodName(), methodDescriptor());

            String proxyClass = ProxyServerHook.class.getName();

            // Fire event BEFORE breeding action completes
            // If cancelled, return false to prevent breeding
            StringBuilder eventCode = new StringBuilder();
            eventCode.append("{\n");
            eventCode.append("    try {\n");
            eventCode.append("        boolean cancelled = ").append(proxyClass).append(".fireCreatureBreedEvent(\n");
            eventCode.append("            $1,\n");  // performer
            eventCode.append("            $2,\n");  // target
            eventCode.append("            $3,\n");  // breedType
            eventCode.append("            $4,\n");  // action
            eventCode.append("            $5\n");   // counter
            eventCode.append("        );\n");
            eventCode.append("        if (cancelled) {\n");
            eventCode.append("            return false;\n");
            eventCode.append("        }\n");
            eventCode.append("    } catch (Exception e) {\n");
            eventCode.append("        e.printStackTrace();\n");
            eventCode.append("    }\n");
            eventCode.append("}\n");

            breedMethod.insertBefore(eventCode.toString());
            LOGGER.info("Registered CreatureBreedEvent patch");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CreatureBreedPatch", e);
        } catch (RuntimeException e) {
            // Catch "class is frozen" errors from legacy mods
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping CreatureBreedPatch - " + e.getMessage());
                LOGGER.warning("[BytecodePatch] CreatureBreedEvent will not fire - legacy mod conflict");
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 70;  // After creature death (60)
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.CREATURE_BREED);
    }
}
