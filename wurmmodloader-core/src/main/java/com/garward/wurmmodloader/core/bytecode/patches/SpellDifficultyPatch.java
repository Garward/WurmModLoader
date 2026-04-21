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
 * Wraps {@code Spell.getDifficulty(boolean)} so mods can tune per-spell difficulty via
 * {@link com.garward.wurmmodloader.api.events.spell.SpellDifficultyEvent}. Every caster
 * skillCheck in the {@code run(...)} overloads routes through this getter — defender
 * resist rolls do not, so this hook is caster-only by construction.
 */
public final class SpellDifficultyPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellDifficultyPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.spells.Spell";
    }

    @Override
    public String methodName() {
        return "getDifficulty";
    }

    @Override
    public String methodDescriptor() {
        return "(Z)I";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSpell = classPool.get(targetClassName());

            if (ctSpell.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SpellDifficultyPatch - Spell class already frozen");
                return;
            }

            CtMethod method = ctSpell.getMethod(methodName(), methodDescriptor());
            String proxy = ProxyServerHook.class.getName();

            String src =
                "{\n" +
                "    $_ = " + proxy + ".fireSpellDifficultyEvent(\n" +
                "        this.number, this.name, $_, $1\n" +
                "    );\n" +
                "}\n";

            method.insertAfter(src);
            LOGGER.info("Registered SpellDifficultyEvent patch for Spell.getDifficulty");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellDifficultyPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellDifficultyPatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_DIFFICULTY);
    }
}
