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
 * Wraps every {@code this.precondition(...)} call inside {@code Spell.run(...)}
 * overloads with {@link com.garward.wurmmodloader.api.events.spell.SpellPreconditionEvent}.
 * Listeners can override vanilla's per-spell allow/deny decision — handy for
 * realm-specific bans, GM overrides, and location-based gating that shouldn't
 * require subclassing every Spell.
 *
 * <p>Vanilla's {@code precondition} has five signatures differing by target type
 * (Creature, Item, Wound, tile coords, tile-border coords). The patch dispatches
 * on argument count + last-arg type at instrument time and passes a target ID
 * where available.</p>
 */
public final class SpellPreconditionPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(SpellPreconditionPatch.class.getName());
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
                LOGGER.warning("[BytecodePatch] Skipping SpellPreconditionPatch - Spell class already frozen");
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
                        if (!"precondition".equals(mc.getMethodName())) return;

                        // Dispatch by descriptor — 5 precondition overloads, each
                        // takes (Skill, Creature, <target>). Last-arg form decides
                        // how we derive targetId + TargetType.
                        String sig;
                        try {
                            sig = mc.getSignature();
                        } catch (Exception e) {
                            return;
                        }

                        String targetExpr;
                        String targetType;
                        if (sig.contains("Lcom/wurmonline/server/creatures/Creature;)")) {
                            targetExpr = "$3 == null ? -1L : $3.getWurmId()";
                            targetType = "CREATURE";
                        } else if (sig.contains("Lcom/wurmonline/server/items/Item;)")) {
                            targetExpr = "$3 == null ? -1L : $3.getWurmId()";
                            targetType = "ITEM";
                        } else if (sig.contains("Lcom/wurmonline/server/bodys/Wound;)")) {
                            targetExpr = "$3 == null || $3.getCreature() == null ? -1L : $3.getCreature().getWurmId()";
                            targetType = "WOUND";
                        } else if (sig.endsWith("III)Z")) {
                            // (Skill, Creature, int, int, int)
                            targetExpr = "-1L";
                            targetType = "TILE";
                        } else if (sig.contains("IIII") || sig.contains("TileBorderDirection")) {
                            targetExpr = "-1L";
                            targetType = "TILE_BORDER";
                        } else {
                            return;
                        }

                        String replacement =
                            "{\n" +
                            "    boolean __orig = $proceed($$);\n" +
                            "    $_ = " + proxy + ".fireSpellPreconditionEvent(\n" +
                            "        this.number, this.name,\n" +
                            "        $2 == null ? -1L : $2.getWurmId(),\n" +
                            "        $2 == null ? \"\" : $2.getName(),\n" +
                            "        " + targetExpr + ",\n" +
                            "        \"" + targetType + "\",\n" +
                            "        __orig\n" +
                            "    );\n" +
                            "}";
                        mc.replace(replacement);
                        edits[0]++;
                    }
                });
            }

            LOGGER.info("Registered SpellPreconditionEvent patch — wrapped " + edits[0]
                + " precondition call site(s)");

        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install SpellPreconditionPatch", e);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("frozen")) {
                LOGGER.warning("[BytecodePatch] Skipping SpellPreconditionPatch - " + e.getMessage());
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
        return Collections.singleton(BytecodeConflictKeys.SPELL_PRECONDITION);
    }
}
