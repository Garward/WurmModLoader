package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Wraps CombatHandler.checkShield so mods can inspect and override shield checks/damage.
 */
public final class ShieldCheckPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(ShieldCheckPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.CombatHandler";
    }

    @Override
    public String methodName() {
        return "checkShield";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)F";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCombatHandler = classPool.get(targetClassName());
            CtClass creatureClass = classPool.get("com.wurmonline.server.creatures.Creature");
            CtClass itemClass = classPool.get("com.wurmonline.server.items.Item");
            CtClass shieldEventClass = classPool.get("com.garward.wurmmodloader.api.events.combat.shield.ShieldCheckEvent");

            CtMethod checkShield = ctCombatHandler.getDeclaredMethod("checkShield");
            checkShield.addLocalVariable("_wmlShieldEvent", shieldEventClass);
            checkShield.addLocalVariable("_wmlShieldDefender", creatureClass);
            checkShield.addLocalVariable("_wmlShieldWeapon", itemClass);

            String eventCall = String.format(
                "{ _wmlShieldDefender = $1; _wmlShieldWeapon = $2;"
                    + "  _wmlShieldEvent = %s.fireShieldCheckEvent(this.creature, _wmlShieldDefender, _wmlShieldWeapon, _wmlShieldDefender.getShield());"
                    + "  if (_wmlShieldEvent != null) {"
                    + "    if (_wmlShieldEvent.shouldRefundShieldUse() && _wmlShieldDefender.getCombatHandler().usedShieldThisRound > 0) {"
                    + "      _wmlShieldDefender.getCombatHandler().usedShieldThisRound--;"
                    + "    }"
                    + "    if (_wmlShieldEvent.isCancelled()) {"
                    + "      return _wmlShieldEvent.hasBlockPercentOverride() ? _wmlShieldEvent.getBlockPercent() : 0.0f;"
                    + "    }"
                    + "  }"
                    + "}",
                ProxyServerHook.class.getName()
            );
            checkShield.insertBefore(eventCall);

            checkShield.insertAfter("{ if (_wmlShieldEvent != null && _wmlShieldEvent.hasBlockPercentOverride()) { $_ = _wmlShieldEvent.getBlockPercent(); } }");

            checkShield.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if ("com.wurmonline.server.items.Item".equals(m.getClassName()) && "setDamage".equals(m.getMethodName())) {
                        m.replace(String.format(
                            "{ double __delta = $1 - $0.getDamage();"
                                + "  double __result = %s.fireShieldDamageEvent(_wmlShieldDefender, this.creature, (com.wurmonline.server.items.Item)$0, __delta);"
                                + "  if (!Double.isNaN(__result)) {"
                                + "    $1 = (float)($0.getDamage() + __result);"
                                + "    $_ = $proceed($$);"
                                + "  } else {"
                                + "    $_ = false;"
                                + "  } }",
                            ProxyServerHook.class.getName()
                        ));
                    }
                }
            });

            LOGGER.info("[BytecodePatch] Registered ShieldCheckPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install ShieldCheckPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_SHIELD);
    }
}
