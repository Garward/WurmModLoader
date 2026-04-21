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
 * Wraps the defender resist roll inside
 * {@code Spell.run(Creature performer, Creature target, float counter)} with
 * {@link com.garward.wurmmodloader.api.events.spell.SpellResistEvent}.
 *
 * <p>The resist roll is the ONLY {@code Skill.skillCheck} in that overload where
 * the receiver chain starts with {@code target} (all other skillChecks roll for the
 * attacker). ExprEditor can't inspect the receiver, but it visits method calls in
 * deterministic bytecode order — the 4th {@code Skill.skillCheck} in this overload
 * is the defender roll (lines 539/555/558/578/588/623 in vanilla's source order).
 * Since WU is frozen, this ordinal is stable.</p>
 */
public final class SpellResistPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellResistPatch.class.getName());
    private static final String SPELL_CLASS = "com.wurmonline.server.spells.Spell";
    private static final String SKILL_CLASS = "com.wurmonline.server.skills.Skill";
    private static final String CREATURE_RUN_DESC =
        "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;F)Z";
    private static final int DEFENDER_SKILLCHECK_ORDINAL = 3; // 0-indexed: 4th call

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
        return CREATURE_RUN_DESC;
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctSpell = classPool.get(SPELL_CLASS);

            if (ctSpell.isFrozen()) {
                LOGGER.warning("[BytecodePatch] Skipping SpellResistPatch - Spell class already frozen");
                return;
            }

            String proxy = ProxyServerHook.class.getName();
            CtMethod run = ctSpell.getMethod("run", CREATURE_RUN_DESC);

            int[] ordinal = {0};
            int[] wrapped = {0};
            run.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall mc) throws CannotCompileException {
                    if (!SKILL_CLASS.equals(mc.getClassName())) return;
                    if (!"skillCheck".equals(mc.getMethodName())) return;
                    int current = ordinal[0]++;
                    if (current != DEFENDER_SKILLCHECK_ORDINAL) return;
                    mc.replace(
                        "{\n" +
                        "    double __orig = $proceed($$);\n" +
                        "    $_ = " + proxy + ".fireSpellResistEvent(\n" +
                        "        this.number, this.name,\n" +
                        "        performer == null ? -1L : performer.getWurmId(),\n" +
                        "        target == null ? -1L : target.getWurmId(),\n" +
                        "        this.difficulty, __orig\n" +
                        "    );\n" +
                        "}");
                    wrapped[0]++;
                }
            });

            if (wrapped[0] != 1) {
                LOGGER.warning("SpellResistPatch: expected to wrap exactly 1 skillCheck, wrapped "
                        + wrapped[0] + " (defender ordinal may have drifted)");
            } else {
                LOGGER.info("Registered SpellResistEvent patch — wrapped defender skillCheck in Spell.run(Creature,Creature,float)");
            }

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellResistPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellResistPatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_RESIST);
    }
}
