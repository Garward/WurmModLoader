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
 * Allows CombatHandler.getSpeed overloads to route through CombatSwingSpeedEvent.
 */
public final class CombatSwingSpeedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(CombatSwingSpeedPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.CombatHandler";
    }

    @Override
    public String methodName() {
        return "getSpeed";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/creatures/AttackAction;Lcom/wurmonline/server/items/Item;)F";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCombatHandler = classPool.get(targetClassName());
            CtClass attackAction = classPool.get("com.wurmonline.server.creatures.AttackAction");
            CtClass itemClass = classPool.get("com.wurmonline.server.items.Item");

            CtMethod getSpeedAttack = ctCombatHandler.getDeclaredMethod("getSpeed", new CtClass[] { attackAction, itemClass });
            getSpeedAttack.insertAfter(String.format(
                "{ $_ = %s.fireCombatSwingSpeedEvent(this.creature, $2, $_); }",
                ProxyServerHook.class.getName()
            ));

            CtMethod getSpeedItem = ctCombatHandler.getDeclaredMethod("getSpeed", new CtClass[] { itemClass });
            getSpeedItem.insertAfter(String.format(
                "{ $_ = %s.fireCombatSwingSpeedEvent(this.creature, $1, $_); }",
                ProxyServerHook.class.getName()
            ));

            LOGGER.info("[BytecodePatch] Registered CombatSwingSpeedPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install CombatSwingSpeedPatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_SWING_SPEED);
    }
}
