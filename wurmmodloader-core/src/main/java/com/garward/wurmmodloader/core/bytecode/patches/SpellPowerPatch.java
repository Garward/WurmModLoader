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
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Wraps every call to {@code Spell.trimPower(Creature, double)} inside the 5
 * {@code Spell.run(...)} overloads with a firing of
 * {@link com.garward.wurmmodloader.api.events.spell.SpellPowerEvent}. This is the
 * central lever for balance mods — every "make this spell stronger/weaker" patch in
 * spellcraft-style mods hooks this point.
 *
 * <p>Patches call sites (not {@code trimPower} itself) so the event can include the
 * containing spell's id/name via the enclosing method's {@code this}.</p>
 */
public final class SpellPowerPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellPowerPatch.class.getName());
    private static final String SPELL_CLASS = "com.wurmonline.server.spells.Spell";
    private static final String TRIM_POWER = "trimPower";

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
        return null; // match all overloads
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSpell = classPool.get(SPELL_CLASS);

            if (ctSpell.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SpellPowerPatch - Spell class already frozen");
                return;
            }

            String proxy = ProxyServerHook.class.getName();

            int[] edits = {0};
            for (CtMethod m : ctSpell.getDeclaredMethods()) {
                if (!m.getName().equals("run")) continue;
                m.instrument(new ExprEditor() {
                    @Override
                    public void edit(MethodCall mc) throws CannotCompileException {
                        if (!SPELL_CLASS.equals(mc.getClassName())) return;
                        if (!TRIM_POWER.equals(mc.getMethodName())) return;
                        // $_ = original trimPower result; re-wrap through event.
                        // $1 = Creature performer, $2 = double rawPower (unused — we use $_).
                        String replacement =
                            "{\n" +
                            "    $_ = $proceed($$);\n" +
                            "    $_ = " + proxy + ".fireSpellPowerEvent(\n" +
                            "        this.number, this.name,\n" +
                            "        $1 == null ? -1L : $1.getWurmId(),\n" +
                            "        $1 == null ? \"\" : $1.getName(),\n" +
                            "        $_\n" +
                            "    );\n" +
                            "}";
                        mc.replace(replacement);
                        edits[0]++;
                    }
                });
            }

            LOGGER.info("Registered SpellPowerEvent patch — wrapped " + edits[0]
                + " trimPower call site(s) in Spell.run overloads");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellPowerPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellPowerPatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_POWER);
    }
}
