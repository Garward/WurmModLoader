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
 * Wraps every {@code Deity.getSpellsTargetting*} call inside the Behaviour classes
 * that build right-click spell menus. The returned {@code Spell[]} is routed through
 * {@link ProxyServerHook#filterSpellVisibility} which fires a cancellable
 * {@link com.garward.wurmmodloader.api.events.spell.SpellVisibilityEvent} per spell.
 *
 * <p>Five call sites across five Behaviour classes: CreatureBehaviour, ItemBehaviour,
 * WoundBehaviour, TileBehaviour, BodyPartBehaviour. Local variable names (performer,
 * target, object) come from WU's debug info.</p>
 */
public final class SpellVisibilityPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellVisibilityPatch.class.getName());
    private static final String DEITY_CLASS = "com.wurmonline.server.deities.Deity";

    private static final class Site {
        final String behaviourClass;
        final String targetExpr;   // Javassist source expression yielding target wurmId
        final String targetType;   // enum name
        Site(String behaviourClass, String targetExpr, String targetType) {
            this.behaviourClass = behaviourClass;
            this.targetExpr = targetExpr;
            this.targetType = targetType;
        }
    }

    // target local-var names taken directly from vanilla decompiled source
    private static final Site[] SITES = new Site[] {
        new Site("com.wurmonline.server.behaviours.CreatureBehaviour",
                 "target == null ? -1L : target.getWurmId()", "CREATURE"),
        new Site("com.wurmonline.server.behaviours.ItemBehaviour",
                 "target == null ? -1L : target.getWurmId()", "ITEM"),
        new Site("com.wurmonline.server.behaviours.WoundBehaviour",
                 "target == null || target.getCreature() == null ? -1L : target.getCreature().getWurmId()",
                 "WOUND"),
        new Site("com.wurmonline.server.behaviours.TileBehaviour",
                 "-1L", "TILE"),
        new Site("com.wurmonline.server.behaviours.BodyPartBehaviour",
                 "object == null ? -1L : object.getWurmId()", "CREATURE"),
    };

    @Override
    public String targetClassName() {
        // Primary target (used for conflict warnings only — this patch instruments 5 classes)
        return "com.wurmonline.server.behaviours.CreatureBehaviour";
    }

    @Override
    public String methodName() {
        return "getBehavioursFor";
    }

    @Override
    public String methodDescriptor() {
        return null;
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        ClassPool classPool = hookManager.getClassPool();
        String proxy = ProxyServerHook.class.getName();
        int totalWrapped = 0;

        for (Site site : SITES) {
            try {
                CtClass ctBehaviour = classPool.get(site.behaviourClass);
                if (ctBehaviour.isFrozen()) {
                    LOGGER.warning("[BytecodePatch] Skipping SpellVisibilityPatch for "
                            + site.behaviourClass + " - class already frozen");
                    continue;
                }
                int[] perClass = {0};
                for (CtMethod m : ctBehaviour.getDeclaredMethods()) {
                    m.instrument(new ExprEditor() {
                        @Override
                        public void edit(MethodCall mc) throws CannotCompileException {
                            if (!DEITY_CLASS.equals(mc.getClassName())) return;
                            String mn = mc.getMethodName();
                            if (!mn.startsWith("getSpellsTargetting")) return;
                            mc.replace(
                                "{\n" +
                                "    com.wurmonline.server.spells.Spell[] __v = $proceed($$);\n" +
                                "    $_ = " + proxy + ".filterSpellVisibility(\n" +
                                "        __v, performer, " + site.targetExpr + ", \"" + site.targetType + "\"\n" +
                                "    );\n" +
                                "}");
                            perClass[0]++;
                        }
                    });
                }
                totalWrapped += perClass[0];
                if (perClass[0] == 0) {
                    LOGGER.warning("SpellVisibilityPatch: no getSpellsTargetting* calls found in "
                            + site.behaviourClass);
                }
            } catch (NotFoundException | CannotCompileException e) {
                throw new IllegalStateException("Unable to install SpellVisibilityPatch on "
                        + site.behaviourClass, e);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                    LOGGER.warning("[BytecodePatch] Skipping SpellVisibilityPatch for "
                            + site.behaviourClass + " - " + e.getMessage());
                } else {
                    throw e;
                }
            }
        }

        LOGGER.info("Registered SpellVisibilityEvent patch — wrapped " + totalWrapped
                + " getSpellsTargetting* call site(s) across " + SITES.length + " Behaviour classes");
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.SPELL_VISIBILITY);
    }
}
