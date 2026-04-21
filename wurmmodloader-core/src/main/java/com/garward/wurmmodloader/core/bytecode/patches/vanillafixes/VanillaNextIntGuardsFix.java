package com.garward.wurmmodloader.core.bytecode.patches.vanillafixes;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guards unguarded {@code Random.nextInt(computed)} call sites in vanilla Wurm that can
 * throw {@code IllegalArgumentException: bound must be positive} when the computed bound
 * is &le; 0.
 *
 * <p>Each target site uses an arithmetic expression (village dimensions, HOTA zone span,
 * parry time, spell damage bonus, infection severity, poison half-dose) that is normally
 * positive but has no floor guard. With certain data shapes (degenerate zones, zero stats,
 * 0-bonus weapons) the bound becomes 0 or negative and the call throws, spamming logs at
 * best and crashing login / pathing at worst.</p>
 *
 * <p>Fix: wrap every {@code java.util.Random.nextInt(int)} call in the named methods with
 * {@code Math.max(1, arg)}. A correctly-bounded call is unaffected (the max is a no-op);
 * a degenerate bound becomes 1, so {@code nextInt} returns 0 — equivalent to "no random
 * offset picked this tick", which is the same graceful outcome each call site already
 * handles.</p>
 *
 * <p>Confirmed stack traces this fixes:</p>
 * <pre>
 *   Npc.getMoveTarget (HOTA wander)            — nextInt(hota.getEndX() - hota.getStartX())
 *   LoginHandler.getStartTileForDeed           — nextInt(v.getEndY() + 20 - v.getStartY())
 *   CombatEngine.performAttack (parry)         — nextInt(parryTime)
 *   CombatEngine.performAttack (spell bonus)   — nextInt(weaponSpellDamageBonus)
 *   CombatEngine.addWound (infection)          — nextInt(infection)
 *   Archery.hit (poison damage)                — nextInt(half)
 * </pre>
 */
public final class VanillaNextIntGuardsFix implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(VanillaNextIntGuardsFix.class.getName());
    private static final String SETTINGS_KEY = "nextint_guards";

    private static final List<Target> TARGETS = Arrays.asList(
        new Target("com.wurmonline.server.creatures.Npc", "getMoveTarget"),
        new Target("com.wurmonline.server.LoginHandler", "getStartTileForDeed"),
        new Target("com.wurmonline.server.combat.CombatEngine", "performAttack"),
        new Target("com.wurmonline.server.combat.CombatEngine", "addWound"),
        new Target("com.wurmonline.server.combat.Archery", "hit")
    );

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Npc";
    }

    @Override
    public String methodName() {
        return "getMoveTarget";
    }

    @Override
    public String methodDescriptor() {
        return null;
    }

    @Override
    public void apply(Object hookManagerObj) {
        if (!VanillaFixesSettings.isEnabled(SETTINGS_KEY)) {
            LOGGER.info("[VanillaFix] nextInt guards disabled via wurmmodloader.vanilla_fixes." + SETTINGS_KEY);
            return;
        }

        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();

        int patchedMethods = 0;
        int patchedCalls = 0;

        for (Target target : TARGETS) {
            try {
                CtClass ctClass = classPool.get(target.className);
                if (ctClass.isFrozen()) {
                    LOGGER.warning("[VanillaFix] Skipping " + target + " — class frozen by another patch");
                    continue;
                }
                int matched = 0;
                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if (!method.getName().equals(target.methodName)) continue;
                    NextIntGuardEditor editor = new NextIntGuardEditor();
                    method.instrument(editor);
                    if (editor.count > 0) {
                        patchedMethods++;
                        patchedCalls += editor.count;
                        matched++;
                    }
                }
                if (matched == 0) {
                    LOGGER.warning("[VanillaFix] " + target + " — no nextInt calls found (method missing or already guarded?)");
                }
            } catch (NotFoundException e) {
                LOGGER.log(Level.WARNING, "[VanillaFix] " + target + " — class not found; skipping", e);
            } catch (CannotCompileException e) {
                LOGGER.log(Level.WARNING, "[VanillaFix] " + target + " — compile failed; skipping", e);
            }
        }

        LOGGER.info("[VanillaFix] nextInt guards: wrapped " + patchedCalls
            + " call(s) across " + patchedMethods + " method(s)");
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.VANILLAFIX_NEXTINT_GUARDS);
    }

    @Override
    public String displayName() {
        return "VanillaNextIntGuardsFix";
    }

    private static final class Target {
        final String className;
        final String methodName;
        Target(String c, String m) { this.className = c; this.methodName = m; }
        @Override public String toString() { return className + "#" + methodName; }
    }

    private static final class NextIntGuardEditor extends ExprEditor {
        int count;
        @Override
        public void edit(MethodCall m) throws CannotCompileException {
            if (!"nextInt".equals(m.getMethodName())) return;
            if (!"java.util.Random".equals(m.getClassName())) return;
            try {
                if (!"(I)I".equals(m.getSignature())) return;
            } catch (Throwable ignored) {
                return;
            }
            m.replace("$_ = $proceed(java.lang.Math.max(1, $1));");
            count++;
        }
    }
}
