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
 * Wraps every {@code this.doEffect(...)} and {@code this.doNegativeEffect(...)} call
 * inside the {@code Spell.run(...)} overloads with a cancellable
 * {@link com.garward.wurmmodloader.api.events.spell.SpellEffectEvent}. Cancelling skips
 * the vanilla effect entirely — the hook mods need to swap in replacement behavior
 * without rewriting whole run methods.
 *
 * <p>The last {@code double} argument to both methods is the spell power (post-trim
 * and post-SpellPowerEvent). Extracted via {@code $1 / $2 / ...} positional args —
 * the power is always the second argument.</p>
 */
public final class SpellEffectPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellEffectPatch.class.getName());
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
                LOGGER.warning("[BytecodePatch] Skipping SpellEffectPatch - Spell class already frozen");
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
                        String name = mc.getMethodName();
                        boolean negative;
                        if ("doEffect".equals(name)) {
                            negative = false;
                        } else if ("doNegativeEffect".equals(name)) {
                            negative = true;
                        } else {
                            return;
                        }

                        // doEffect(Skill, double, Creature, ...) — power = $2, performer = $3.
                        String replacement =
                            "{\n" +
                            "    if (!" + proxy + ".fireSpellEffectEvent(\n" +
                            "        this.number, this.name,\n" +
                            "        $3 == null ? -1L : $3.getWurmId(),\n" +
                            "        $3 == null ? \"\" : $3.getName(),\n" +
                            "        $2, " + negative + "\n" +
                            "    )) {\n" +
                            "        $_ = $proceed($$);\n" +
                            "    }\n" +
                            "}";
                        mc.replace(replacement);
                        edits[0]++;
                    }
                });
            }

            LOGGER.info("Registered SpellEffectEvent patch — wrapped " + edits[0]
                + " doEffect/doNegativeEffect call site(s)");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellEffectPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellEffectPatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_EFFECT);
    }
}
