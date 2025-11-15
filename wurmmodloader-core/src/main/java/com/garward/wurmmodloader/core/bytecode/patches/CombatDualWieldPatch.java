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

/**
 * Surfaces dual-wield strikes through CombatDualWieldEvent before the off-hand attack executes.
 */
public final class CombatDualWieldPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CombatDualWieldPatch.class.getName());

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
        return "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)Z";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool pool = hookManager.getClassPool();
            CtClass ctCombatHandler = pool.get(targetClassName());
            CtMethod attack = ctCombatHandler.getMethod(
                methodName(),
                methodDescriptor()
            );

            String code = "if ($3) {"
                + "  com.garward.wurmmodloader.api.events.combat.CombatDualWieldEvent _wmlEvt = "
                + ProxyServerHook.class.getName()
                + ".fireCombatDualWieldEvent(this.creature, $1, $2, 0f);"
                + "  if (_wmlEvt.isCancelled()) { return false; }"
                + "  $2 = _wmlEvt.getOffhandWeapon();"
                + " }";
            attack.insertBefore(code);

            LOGGER.info("[BytecodePatch] Registered CombatDualWieldPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CombatDualWieldPatch", e);
        }
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_DUAL_WIELD);
    }
}
