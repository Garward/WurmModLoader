package com.garward.wurmmodloader.core.bytecode.patches.vanillafixes;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Catch-all safety net around {@code Npc.getMoveTarget(int)}.
 *
 * <p>{@link VanillaNextIntGuardsFix} floors every {@code Random.nextInt(n)} inside
 * {@code Npc.getMoveTarget} at 1, preventing the classic {@code
 * IllegalArgumentException: bound must be positive}. But several call sites in that
 * method look like {@code array[Server.rand.nextInt(array.length)]} — when the array
 * is empty, the guarded {@code nextInt(1)} returns 0 and the access still throws
 * {@code ArrayIndexOutOfBoundsException: 0}, spamming logs on every creature tick in
 * zones where the degenerate array shape is stable.</p>
 *
 * <p>Rather than audit every indexed-random site inside the 400-line method (a moving
 * target across WU updates), this patch wraps the whole method body in a try/catch
 * for {@link ArrayIndexOutOfBoundsException} and {@link IllegalArgumentException},
 * returning {@code null}. {@code Creature.startPathing} already null-checks the
 * return value — a null just means "no pathing this tick", which is the same graceful
 * outcome the vanilla code path already handles on hundreds of other creatures where
 * conditions didn't line up.</p>
 *
 * <p>Confirmed stack this suppresses:</p>
 * <pre>
 *   ArrayIndexOutOfBoundsException: 0
 *     at com.wurmonline.server.creatures.Npc.getMoveTarget(Npc.java:...)
 *     at com.wurmonline.server.creatures.Creature.startPathing(...)
 *     at com.wurmonline.server.creatures.Npc.doSomething(...)
 *     at com.wurmonline.server.creatures.Npc.pollNPC(...)
 * </pre>
 *
 * <p>Runs at priority 40 — <em>after</em> {@link VanillaNextIntGuardsFix} (priority 50)
 * has finished wrapping nextInt calls. The order ensures we catch residual failures
 * the nextInt guards cannot prevent.</p>
 */
public final class NpcMoveTargetGuardFix implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(NpcMoveTargetGuardFix.class.getName());
    private static final String SETTINGS_KEY = "npc_move_target_guard";

    @Override public String targetClassName() { return "com.wurmonline.server.creatures.Npc"; }
    @Override public String methodName()       { return "getMoveTarget"; }
    @Override public String methodDescriptor() { return null; }

    @Override
    public void apply(Object hookManagerObj) {
        if (!VanillaFixesSettings.isEnabled(SETTINGS_KEY)) {
            LOGGER.info("[VanillaFix] NpcMoveTargetGuard disabled via wurmmodloader.vanilla_fixes." + SETTINGS_KEY);
            return;
        }

        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();

        try {
            CtClass npcClass = classPool.get(targetClassName());
            CtClass aioobe = classPool.get("java.lang.ArrayIndexOutOfBoundsException");
            CtClass iae   = classPool.get("java.lang.IllegalArgumentException");

            int wrapped = 0;
            for (CtMethod method : npcClass.getDeclaredMethods()) {
                if (!method.getName().equals(methodName())) continue;
                method.addCatch("return null;", aioobe);
                method.addCatch("return null;", iae);
                wrapped++;
            }

            if (wrapped == 0) {
                LOGGER.warning("[VanillaFix] NpcMoveTargetGuard — no getMoveTarget overloads found");
            } else {
                LOGGER.info("[VanillaFix] NpcMoveTargetGuard: wrapped "
                        + wrapped + " Npc.getMoveTarget overload(s) with AIOOBE/IAE catch");
            }
        } catch (NotFoundException e) {
            LOGGER.log(Level.WARNING, "[VanillaFix] NpcMoveTargetGuard — class lookup failed; skipping", e);
        } catch (CannotCompileException e) {
            LOGGER.log(Level.WARNING, "[VanillaFix] NpcMoveTargetGuard — addCatch failed; skipping", e);
        }
    }

    @Override public int priority() { return 40; }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.VANILLAFIX_NPC_MOVE_TARGET_GUARD);
    }

    @Override public String displayName() { return "NpcMoveTargetGuardFix"; }
}
