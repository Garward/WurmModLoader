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

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Honors {@code ServerEntry.getActionTimer()} in action paths that vanilla Wurm hardcodes
 * their durations in. Ported from bdew's TimerFix (GPL-2.0), minus the opinionated spell
 * blacklist. Per-category minimum caps for spells, picking, and breeding are exposed via
 * {@link VanillaFixesSettings}.
 *
 * <p>Each patched action divides its hardcoded duration by {@code getActionTimer()} so a
 * server running e.g. {@code actionTimer=4.0} actually gets 4× faster prayer, meditation,
 * alchemy, forage, breed, flatten, sacrifice, destroy, sow, spell-cast, and coloring —
 * the same way non-bugged actions already scale.</p>
 *
 * <p>A corrected call site with {@code actionTimer=1.0} is behaviorally identical to
 * vanilla (the division is a no-op).</p>
 */
public final class ActionTimerFix implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ActionTimerFix.class.getName());
    private static final String SETTINGS_KEY = "action_timer";
    private static final String HOOKS = "com.garward.wurmmodloader.core.bytecode.patches.vanillafixes.ActionTimerHooks";

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.behaviours.Flattening";
    }

    @Override
    public String methodName() {
        return "flatten";
    }

    @Override
    public String methodDescriptor() {
        return null;
    }

    @Override
    public void apply(Object hookManagerObj) {
        if (!VanillaFixesSettings.isEnabled(SETTINGS_KEY)) {
            LOGGER.info("[VanillaFix] action_timer disabled via config");
            return;
        }

        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool cp = hookManager.getClassPool();

        int applied = 0;
        applied += tryFamily("FLATTEN", () -> FlattenPatcher.patchFlatten(cp));
        applied += tryFamily("SPELLS", () -> patchSpells(cp));
        applied += tryFamily("DESTROY", () -> patchDestroy(cp));
        applied += tryFamily("PRAY", () -> patchPray(cp));
        applied += tryFamily("SACRIFICE", () -> patchSacrifice(cp));
        applied += tryFamily("SOW", () -> patchSow(cp));
        applied += tryFamily("MEDITATE", () -> patchMeditate(cp));
        applied += tryFamily("ALCHEMY", () -> patchAlchemy(cp));
        applied += tryFamily("FORAGE", () -> patchForage(cp));
        applied += tryFamily("BREED", () -> patchBreed(cp));
        applied += tryFamily("MISC", () -> patchMisc(cp));
        applied += tryFamily("PICK_MIN", () -> patchPickMin(cp));

        LOGGER.info("[VanillaFix] action_timer: " + applied + "/12 patch families applied");
    }

    @Override
    public int priority() {
        return 45;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.VANILLAFIX_ACTION_TIMER);
    }

    @Override
    public String displayName() {
        return "ActionTimerFix";
    }

    // ==== families ====

    private static void patchSpells(ClassPool cp) throws NotFoundException, CannotCompileException {
        cp.getCtClass("com.wurmonline.server.spells.Spell")
            .getMethod("getCastingTime", "(Lcom/wurmonline/server/creatures/Creature;)I")
            .insertAfter("return " + HOOKS + ".getCastingTime(this, $_);");
    }

    private static void patchDestroy(ClassPool cp) throws NotFoundException, CannotCompileException {
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsStructure",
            "destroyWall",
            "(SLcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/structures/Wall;ZF)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsStructure",
            "destroyFence",
            "(SLcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/structures/Fence;ZF)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsStructure",
            "destroyFloor",
            "(SLcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/structures/IFloor;F)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "destroyItem",
            "(ILcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;ZF)Z",
            true, true, false, 0);
    }

    private static void patchPray(ClassPool cp) throws NotFoundException, CannotCompileException {
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsReligion",
            "pray",
            "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;F)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsReligion",
            "pray",
            "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;F)Z",
            true, true, false, 0);
    }

    private static void patchSacrifice(ClassPool cp) throws NotFoundException, CannotCompileException {
        cp.getCtClass("com.wurmonline.server.behaviours.MethodsReligion")
            .getMethod("sacrifice", "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z")
            .instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("currentSecond")) {
                        m.replace("if ($proceed()>1) $_=$proceed()*com.wurmonline.server.Servers.localServer.getActionTimer(); else $_=$proceed();");
                    } else if (m.getMethodName().equals("sendActionControl")) {
                        m.replace("$proceed($1,$2,(int)($3/com.wurmonline.server.Servers.localServer.getActionTimer()));");
                    }
                }
            });
    }

    private static void patchSow(ClassPool cp) throws NotFoundException, CannotCompileException {
        applyEdit(cp, "com.wurmonline.server.behaviours.TileDirtBehaviour",
            "action",
            "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;IIZIISF)Z",
            true, true, false, 0);
    }

    private static void patchMeditate(ClassPool cp) throws NotFoundException, CannotCompileException {
        applyEdit(cp, "com.wurmonline.server.players.Cults",
            "meditate",
            "(Lcom/wurmonline/server/creatures/Creature;ILcom/wurmonline/server/behaviours/Action;FLcom/wurmonline/server/items/Item;)Z",
            true, true, false, 0);
    }

    private static void patchAlchemy(ClassPool cp) throws NotFoundException, CannotCompileException {
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "smear",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z",
            false, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "createOil",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z",
            false, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "createSalve",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z",
            false, true, false, 0);
    }

    private static void patchForage(ClassPool cp) throws NotFoundException, CannotCompileException {
        applyEdit(cp, "com.wurmonline.server.behaviours.TileBehaviour",
            "forage",
            "(Lcom/wurmonline/server/behaviours/Action;Lcom/wurmonline/server/creatures/Creature;IIIBF)Z",
            true, true, false, 0);
    }

    private static void patchBreed(ClassPool cp) throws NotFoundException, CannotCompileException {
        int minBreed = VanillaFixesSettings.actionTimerMinBreed() * 10;
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsCreatures",
            "breed",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Creature;SLcom/wurmonline/server/behaviours/Action;F)Z",
            true, true, false, minBreed);
    }

    private static void patchMisc(ClassPool cp) throws NotFoundException, CannotCompileException {
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsStructure",
            "colorWall",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/structures/Wall;Lcom/wurmonline/server/behaviours/Action;)Z",
            true, false, true, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsStructure",
            "removeColor",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/structures/Wall;Lcom/wurmonline/server/behaviours/Action;)Z",
            true, false, true, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsStructure",
            "colorFence",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/structures/Fence;Lcom/wurmonline/server/behaviours/Action;)Z",
            true, false, true, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "colorItem",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;Z)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "improveColor",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "removeColor",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;Z)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "string",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "stringRod",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;)Z",
            true, true, false, 0);
        applyEdit(cp, "com.wurmonline.server.behaviours.MethodsItems",
            "unstringBow",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z",
            true, true, false, 0);
    }

    private static void patchPickMin(ClassPool cp) throws NotFoundException, CannotCompileException {
        int minPick = VanillaFixesSettings.actionTimerMinPick();
        if (minPick <= 0) return;
        cp.getCtClass("com.wurmonline.server.behaviours.Actions")
            .getMethod("getPickActionTime",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/skills/Skill;Lcom/wurmonline/server/items/Item;D)I")
            .insertAfter("return Math.max($_, " + (minPick * 10) + ");");
    }

    // ==== core rewriter ====

    /**
     * Rewrites {@code sendActionControl(_,_,t)}, {@code setTimeLeft(t)}, and/or
     * {@code getCounterAsFloat()} calls inside a method so the timer value is divided
     * by {@code getActionTimer()}. Optionally clamps to {@code minCapTenths} (units of
     * tenths of a second, matching Wurm's internal representation).
     */
    private static void applyEdit(ClassPool cp, String cls, String method, String descr,
                                  boolean sendActionControlPatch, boolean setTimeLeftPatch,
                                  boolean getCounterAsFloatPatch, int minCapTenths)
            throws NotFoundException, CannotCompileException {
        cp.getCtClass(cls).getMethod(method, descr).instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if (sendActionControlPatch && m.getMethodName().equals("sendActionControl")) {
                    m.replace("$proceed($1,$2,java.lang.Math.max((int)($3/com.wurmonline.server.Servers.localServer.getActionTimer()), " + minCapTenths + "));");
                } else if (setTimeLeftPatch && m.getMethodName().equals("setTimeLeft")) {
                    m.replace("$proceed(java.lang.Math.max((int)($1/com.wurmonline.server.Servers.localServer.getActionTimer()), " + minCapTenths + "));");
                } else if (getCounterAsFloatPatch && m.getMethodName().equals("getCounterAsFloat")) {
                    m.replace("$_ = java.lang.Math.max($proceed() * com.wurmonline.server.Servers.localServer.getActionTimer(), " + minCapTenths + "f);");
                }
            }
        });
    }

    private interface PatchStep {
        void run() throws Exception;
    }

    private static int tryFamily(String name, PatchStep step) {
        try {
            step.run();
            return 1;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[VanillaFix/ActionTimer] " + name + " skipped: " + t.getMessage(), t);
            return 0;
        }
    }
}
