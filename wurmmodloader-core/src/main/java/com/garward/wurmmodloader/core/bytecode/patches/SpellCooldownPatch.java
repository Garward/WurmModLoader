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
 * Replaces {@code Spell.touchCooldown(Creature)} with an event-aware body so mods may
 * override cooldown durations via
 * {@link com.garward.wurmmodloader.api.events.spell.SpellCooldownEvent} without
 * per-spell bytecode surgery.
 */
public final class SpellCooldownPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellCooldownPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.spells.Spell";
    }

    @Override
    public String methodName() {
        return "touchCooldown";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;)V";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSpell = classPool.get(targetClassName());

            if (ctSpell.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SpellCooldownPatch - Spell class already frozen");
                return;
            }

            CtMethod method = ctSpell.getMethod(methodName(), methodDescriptor());
            String proxy = ProxyServerHook.class.getName();

            String body =
                "{\n" +
                "    if (this.cooldown > 0L) {\n" +
                "        long modMs = " + proxy + ".fireSpellCooldownEvent(\n" +
                "            this.number, this.name,\n" +
                "            $1 == null ? -1L : $1.getWurmId(),\n" +
                "            $1 == null ? \"\" : $1.getName(),\n" +
                "            this.cooldown\n" +
                "        );\n" +
                "        if (modMs > 0L) {\n" +
                "            com.wurmonline.server.spells.Cooldowns cd =\n" +
                "                com.wurmonline.server.spells.Cooldowns.getCooldownsFor($1.getWurmId(), true);\n" +
                "            cd.addCooldown(this.number, System.currentTimeMillis() + modMs, false);\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

            method.setBody(body);
            LOGGER.info("Registered SpellCooldownEvent patch for Spell.touchCooldown");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellCooldownPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellCooldownPatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_COOLDOWN);
    }
}
