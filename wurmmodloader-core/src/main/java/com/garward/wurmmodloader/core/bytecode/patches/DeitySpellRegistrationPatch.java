package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Fires {@link com.garward.wurmmodloader.api.events.spell.DeitySpellRegistrationEvent}
 * after every {@code Deity.addSpell(Spell)} / {@code Deity.removeSpell(Spell)}. The
 * event is notification-only — vanilla state has already changed by the time listeners
 * run.
 */
public final class DeitySpellRegistrationPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(DeitySpellRegistrationPatch.class.getName());
    private static final String DEITY_CLASS = "com.wurmonline.server.deities.Deity";

    @Override
    public String targetClassName() {
        return DEITY_CLASS;
    }

    @Override
    public String methodName() {
        return "addSpell";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/spells/Spell;)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctDeity = classPool.get(DEITY_CLASS);

            if (ctDeity.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping DeitySpellRegistrationPatch - Deity class already frozen");
                return;
            }

            String proxy = ProxyServerHook.class.getName();
            String descriptor = "(Lcom/wurmonline/server/spells/Spell;)V";

            CtMethod addMethod = ctDeity.getMethod("addSpell", descriptor);
            addMethod.insertAfter(
                "{\n" +
                "    " + proxy + ".fireDeitySpellRegistrationEvent(\n" +
                "        this.number, this.name, $1.number, $1.name, true\n" +
                "    );\n" +
                "}\n");

            CtMethod removeMethod = ctDeity.getMethod("removeSpell", descriptor);
            removeMethod.insertAfter(
                "{\n" +
                "    " + proxy + ".fireDeitySpellRegistrationEvent(\n" +
                "        this.number, this.name, $1.number, $1.name, false\n" +
                "    );\n" +
                "}\n");

            LOGGER.info("Registered DeitySpellRegistrationEvent patch for Deity.addSpell/removeSpell");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install DeitySpellRegistrationPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping DeitySpellRegistrationPatch - " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.DEITY_SPELL_REGISTRATION);
    }
}
