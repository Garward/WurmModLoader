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
 * Wraps {@code Spell.getCastingTime(Creature)} so mods can tune individual spells'
 * casting time via {@link com.garward.wurmmodloader.api.events.spell.SpellCastingTimeEvent}.
 *
 * <p>Layered via {@code insertAfter} so any upstream rewrites (notably the
 * {@code action_timer} vanilla fix) resolve first — listeners see the post-scaled value
 * as the original and may further override it.</p>
 */
public final class SpellCastingTimePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellCastingTimePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.spells.Spell";
    }

    @Override
    public String methodName() {
        return "getCastingTime";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;)I";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSpell = classPool.get(targetClassName());

            if (ctSpell.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SpellCastingTimePatch - Spell class already frozen");
                return;
            }

            CtMethod method = ctSpell.getMethod(methodName(), methodDescriptor());
            String proxy = ProxyServerHook.class.getName();

            String src =
                "{\n" +
                "    $_ = " + proxy + ".fireSpellCastingTimeEvent(\n" +
                "        this.number,\n" +
                "        this.name,\n" +
                "        $1 == null ? -1L : $1.getWurmId(),\n" +
                "        $1 == null ? \"\" : $1.getName(),\n" +
                "        $_\n" +
                "    );\n" +
                "}\n";

            method.insertAfter(src);
            LOGGER.info("Registered SpellCastingTimeEvent patch for Spell.getCastingTime");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellCastingTimePatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellCastingTimePatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_CAST_TIME);
    }
}
