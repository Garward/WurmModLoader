package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.api.events.item.material.MaterialBonusEvent;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Instruments miscellaneous material bonus helpers so they surface through MaterialBonusEvent.
 */
public final class MaterialBonusExtendedPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(MaterialBonusExtendedPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.items.Item"; // primary target; Locates patched separately in apply()
    }

    @Override
    public String methodName() {
        return "getMaterialCreationBonus";
    }

    @Override
    public String methodDescriptor() {
        return "(B)F";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool pool = hookManager.getClassPool();
            CtClass ctItem = pool.get(targetClassName());
            if (ctItem.isFrozen()) {
                ctItem.defrost();
            }
            CtClass ctLocates = pool.get("com.wurmonline.server.behaviours.Locates");
            if (ctLocates.isFrozen()) {
                ctLocates.defrost();
            }

            instrumentItemStaticBonus(ctItem, "getMaterialCreationBonus", MaterialBonusEvent.BonusType.CREATION);
            instrumentItemStaticBonus(ctItem, "getMaterialLockpickBonus", MaterialBonusEvent.BonusType.LOCKPICK);
            instrumentItemStaticBonus(ctItem, "getMaterialAnchorBonus", MaterialBonusEvent.BonusType.ANCHOR);
            instrumentPendulumBonus(ctLocates);
            instrumentSpellBonus(ctItem);

            LOGGER.info("[BytecodePatch] Registered MaterialBonusExtendedPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install MaterialBonusExtendedPatch", e);
        }
    }

    private void instrumentItemStaticBonus(CtClass ctItem, String methodName, MaterialBonusEvent.BonusType type)
        throws NotFoundException, CannotCompileException {
        CtMethod method = ctItem.getDeclaredMethod(methodName);
        String code = String.format(
            "{ double _wml = $_; _wml = %s.fireMaterialBonusEvent(%s.%s, null, $1, _wml); $_ = (float)_wml; }",
            ProxyServerHook.class.getName(),
            MaterialBonusEvent.BonusType.class.getName(),
            type.name()
        );
        method.insertAfter(code);
    }

    private void instrumentPendulumBonus(CtClass ctLocates) throws NotFoundException, CannotCompileException {
        CtMethod method = ctLocates.getDeclaredMethod("getMaterialPendulumModifier");
        String code = String.format(
            "{ double _wml = $_; _wml = %s.fireMaterialBonusEvent(%s.BonusType.PENDULUM, null, $1, _wml); $_ = (float)_wml; }",
            ProxyServerHook.class.getName(),
            MaterialBonusEvent.class.getName()
        );
        method.insertAfter(code);
    }

    private void instrumentSpellBonus(CtClass ctItem) throws NotFoundException, CannotCompileException {
        CtMethod method = ctItem.getDeclaredMethod("getBonusForSpellEffect");
        String code = String.format(
            "{ double _wml = $_; %s.SpellContext _ctx = new %s.SpellContext(this, $1);"
                + "  _wml = %s.fireMaterialBonusEvent(%s.BonusType.SPELL_POWER, _ctx, this.getMaterial(), _wml);"
                + "  $_ = (float)_wml; }",
            MaterialBonusEvent.class.getName(),
            MaterialBonusEvent.class.getName(),
            ProxyServerHook.class.getName(),
            MaterialBonusEvent.class.getName()
        );
        method.insertAfter(code);
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public Collection<String> conflictKeys() {
        List<String> keys = Arrays.asList(
            BytecodeConflictKeys.MATERIAL_CREATION_BONUS,
            BytecodeConflictKeys.MATERIAL_LOCKPICK_BONUS,
            BytecodeConflictKeys.MATERIAL_ANCHOR_BONUS,
            BytecodeConflictKeys.MATERIAL_PENDULUM_BONUS,
            BytecodeConflictKeys.MATERIAL_SPELL_POWER_BONUS
        );
        return Collections.unmodifiableList(keys);
    }
}
