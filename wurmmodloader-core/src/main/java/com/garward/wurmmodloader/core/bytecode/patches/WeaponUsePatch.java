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
 * Lets mods adjust weapon timer bookkeeping inside Creature.deductFromWeaponUsed.
 */
public final class WeaponUsePatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(WeaponUsePatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.creatures.Creature";
    }

    @Override
    public String methodName() {
        return "deductFromWeaponUsed";
    }

    @Override
    public String methodDescriptor() {
        return "(Lcom/wurmonline/server/items/Item;F)F";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool classPool = hookManager.getClassPool();
            CtClass ctCreature = classPool.get(targetClassName());
            CtClass itemClass = classPool.get("com.wurmonline.server.items.Item");

            CtMethod deduct = ctCreature.getDeclaredMethod("deductFromWeaponUsed", new CtClass[] { itemClass, CtClass.floatType });
            deduct.addLocalVariable("_wmlPrevWeaponTime", CtClass.floatType);
            deduct.insertBefore(
                "{ " +
                "java.lang.Float _wmlPrev = (java.lang.Float)this.weaponsUsed.get($1);" +
                " _wmlPrevWeaponTime = _wmlPrev != null ? _wmlPrev.floatValue() : $2;" +
                " }");
            deduct.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (!"put".equals(m.getMethodName())) {
                        return;
                    }

                    if (!"java.util.concurrent.ConcurrentHashMap".equals(m.getClassName())) {
                        return;
                    }

                    StringBuilder replacement = new StringBuilder();
                    replacement.append("{");
                    replacement.append(" com.garward.wurmmodloader.api.events.combat.WeaponUseEvent _evt = ")
                        .append(ProxyServerHook.class.getName())
                        .append(".fireWeaponUseEvent(this, (com.wurmonline.server.items.Item)$1, _wmlPrevWeaponTime, ((java.lang.Float)$2).floatValue());");
                    replacement.append(" java.lang.Float _newVal = java.lang.Float.valueOf(_evt.getNewValue());");
                    replacement.append(" $2 = _newVal;");
                    replacement.append(" $_ = $proceed($$);");
                    replacement.append(" }");

                    m.replace(replacement.toString());
                }
            });

            LOGGER.info("[BytecodePatch] Registered WeaponUsePatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install WeaponUsePatch", e);
        }
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.singleton(BytecodeConflictKeys.COMBAT_WEAPON_USE);
    }
}
