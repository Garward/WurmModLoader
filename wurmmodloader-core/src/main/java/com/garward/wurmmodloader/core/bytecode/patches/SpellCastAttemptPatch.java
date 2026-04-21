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
 * Fires {@link com.garward.wurmmodloader.api.events.spell.SpellCastAttemptEvent} at
 * the top of every {@code Spell.run(...)} overload. A cancelled event returns
 * {@code true} immediately, signalling the action layer that the cast completed
 * (so the UI releases without further work).
 */
public final class SpellCastAttemptPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellCastAttemptPatch.class.getName());
    private static final String SPELL_CLASS = "com.wurmonline.server.spells.Spell";

    @Override
    public String targetClassName() {
        return SPELL_CLASS;
    }

    @Override
    public String methodName() {
        return "run";
    }

    @Override
    public String methodDescriptor() {
        return null;
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSpell = classPool.get(SPELL_CLASS);

            if (ctSpell.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SpellCastAttemptPatch - Spell class already frozen");
                return;
            }

            String proxy = ProxyServerHook.class.getName();
            int patched = 0;

            for (CtMethod m : ctSpell.getDeclaredMethods()) {
                if (!m.getName().equals("run")) continue;

                String src =
                    "{\n" +
                    "    if (" + proxy + ".fireSpellCastAttemptEvent(\n" +
                    "        this.number, this.name,\n" +
                    "        $1 == null ? -1L : $1.getWurmId(),\n" +
                    "        $1 == null ? \"\" : $1.getName()\n" +
                    "    )) {\n" +
                    "        return true;\n" +
                    "    }\n" +
                    "}\n";

                m.insertBefore(src);
                patched++;
            }

            LOGGER.info("Registered SpellCastAttemptEvent patch — guarded " + patched + " Spell.run overload(s)");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellCastAttemptPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellCastAttemptPatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_CAST_ATTEMPT);
    }
}
