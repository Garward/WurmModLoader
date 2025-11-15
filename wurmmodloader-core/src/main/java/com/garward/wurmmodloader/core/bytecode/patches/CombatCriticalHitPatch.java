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
 * Adjusts critical hit chance calculations so they route through CombatCriticalHitEvent.
 */
public final class CombatCriticalHitPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CombatCriticalHitPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.CombatHandler";
    }

    @Override
    public String methodName() {
        return "attack";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/AttackAction;)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool pool = hookManager.getClassPool();
            CtClass ctCombatHandler = pool.get(targetClassName());
            CtClass creatureClass = pool.get("com.wurmonline.server.creatures.Creature");

            patchAttackWithAction(pool, ctCombatHandler, creatureClass);
            patchAttackWithWeapon(pool, ctCombatHandler, creatureClass);

            LOGGER.info("[BytecodePatch] Registered CombatCriticalHitPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CombatCriticalHitPatch", e);
        }
    }

    private void patchAttackWithAction(ClassPool pool, CtClass handler, CtClass creatureClass)
        throws NotFoundException, CannotCompileException {
        CtMethod attack = handler.getMethod(
            "attack",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/AttackAction;)Z"
        );
        attack.addLocalVariable("__wmlOpponentCrit", creatureClass);
        attack.insertBefore("{ __wmlOpponentCrit = $1; }");
        attack.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if ("getCriticalChance".equals(m.getMethodName())
                    && "com.wurmonline.server.creatures.AttackValues".equals(m.getClassName())) {
                    m.replace("{ $_ = $proceed($$);"
                        + "  com.wurmonline.server.items.Item __wmlWeapon = this.creature.getPrimWeapon(!attackAction.isUsingWeapon());"
                        + "  $_ = " + ProxyServerHook.class.getName()
                        + ".fireCombatCriticalHitChanceEvent(this.creature, __wmlOpponentCrit, __wmlWeapon, attackAction, $_, false);" + " }");
                }
            }
        });
    }

    private void patchAttackWithWeapon(ClassPool pool, CtClass handler, CtClass creatureClass)
        throws NotFoundException, CannotCompileException {
        CtMethod attack = handler.getMethod(
            "attack",
            "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z"
        );
        attack.addLocalVariable("__wmlOpponentCritWeapon", creatureClass);
        attack.insertBefore("{ __wmlOpponentCritWeapon = $1; }");
        attack.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if ("getCritChanceForWeapon".equals(m.getMethodName())
                    && "com.wurmonline.server.combat.Weapon".equals(m.getClassName())) {
                    m.replace("{ $_ = $proceed($$);"
                        + "  $_ = " + ProxyServerHook.class.getName()
                        + ".fireCombatCriticalHitChanceEvent(this.creature, __wmlOpponentCritWeapon, $1, null, $_, false);"
                        + " }");
                }
            }
        });
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_CRITICAL);
    }
}
