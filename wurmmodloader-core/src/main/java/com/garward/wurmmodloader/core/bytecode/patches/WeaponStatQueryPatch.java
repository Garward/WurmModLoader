package com.garward.wurmmodloader.core.bytecode.patches;

import com.garward.wurmmodloader.api.bytecode.BytecodeConflictKeys;
import com.garward.wurmmodloader.api.bytecode.BytecodePatch;
import com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent;
import com.garward.wurmmodloader.modloader.server.ProxyServerHook;

import java.util.Arrays;
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
 * Routes vanilla weapon/material lookups through WeaponStatQueryEvent so mods can scale stats without
 * directly patching {@link com.wurmonline.server.combat.Weapon}.
 */
public final class WeaponStatQueryPatch implements BytecodePatch {

    private static final Logger LOGGER = Logger.getLogger(WeaponStatQueryPatch.class.getName());

    @Override
    public String targetClassName() {
        return "com.wurmonline.server.combat.Weapon";
    }

    @Override
    public String methodName() {
        return "getMaterialDamageBonus"; // placeholder, we patch multiple methods via apply()
    }

    @Override
    public String methodDescriptor() {
        return "(B)D";
    }

    @Override
    public void apply(Object hookManagerObj) {
        HookManager hookManager = (HookManager) hookManagerObj;
        try {
            ClassPool pool = hookManager.getClassPool();
            CtClass ctWeapon = pool.get(targetClassName());

            instrumentMaterialDamage(ctWeapon);
            instrumentMaterialArmourDamage(ctWeapon);
            instrumentMaterialParry(ctWeapon);
            instrumentBaseSpeed(ctWeapon);

            LOGGER.info("[BytecodePatch] Registered WeaponStatQueryPatch successfully.");
        } catch (NotFoundException | CannotCompileException e) {
            throw new IllegalStateException("Unable to install WeaponStatQueryPatch", e);
        }
    }

    private void instrumentMaterialDamage(CtClass ctWeapon) throws NotFoundException, CannotCompileException {
        CtMethod method = ctWeapon.getDeclaredMethod("getMaterialDamageBonus");
        String code = String.format(
            "{ $_ = %s.fireWeaponStatEvent(null, null, $1, %s.StatType.DAMAGE, $_); }",
            ProxyServerHook.class.getName(),
            WeaponStatQueryEvent.class.getName()
        );
        method.insertAfter(code);
    }

    private void instrumentMaterialArmourDamage(CtClass ctWeapon) throws NotFoundException, CannotCompileException {
        CtMethod method = ctWeapon.getDeclaredMethod("getMaterialArmourDamageBonus");
        String code = String.format(
            "{ $_ = %s.fireWeaponStatEvent(null, null, $1, %s.StatType.ARMOUR_DAMAGE, $_); }",
            ProxyServerHook.class.getName(),
            WeaponStatQueryEvent.class.getName()
        );
        method.insertAfter(code);
    }

    private void instrumentMaterialParry(CtClass ctWeapon) throws NotFoundException, CannotCompileException {
        CtMethod method = ctWeapon.getDeclaredMethod("getMaterialParryBonus");
        String code = String.format(
            "{ double _wmlVal = $_; _wmlVal = %s.fireWeaponStatEvent(null, null, $1, %s.StatType.PARRY, _wmlVal); $_ = (float)_wmlVal; }",
            ProxyServerHook.class.getName(),
            WeaponStatQueryEvent.class.getName()
        );
        method.insertAfter(code);
    }

    private void instrumentBaseSpeed(CtClass ctWeapon) throws NotFoundException, CannotCompileException {
        CtMethod method = ctWeapon.getDeclaredMethod("getBaseSpeedForWeapon");
        String code = String.format(
            "{ byte _wmlMat = ($1 != null) ? $1.getMaterial() : (byte)0; double _wmlVal = $_; "
                + "_wmlVal = %s.fireWeaponStatEvent(null, $1, _wmlMat, %s.StatType.SPEED, _wmlVal);"
                + " $_ = (float)_wmlVal; }",
            ProxyServerHook.class.getName(),
            WeaponStatQueryEvent.class.getName()
        );
        method.insertAfter(code);
    }

    @Override
    public int priority() {
        return 55;
    }

    @Override
    public Collection<String> conflictKeys() {
        return Collections.unmodifiableList(Arrays.asList(
            BytecodeConflictKeys.WEAPON_MATERIAL_DAMAGE,
            BytecodeConflictKeys.WEAPON_MATERIAL_ARMOUR_DAMAGE,
            BytecodeConflictKeys.WEAPON_MATERIAL_PARRY,
            BytecodeConflictKeys.WEAPON_BASE_SPEED
        ));
    }
}
