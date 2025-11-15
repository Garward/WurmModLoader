package com.garward.wurmmodloader.core.eventlogic.materials;

import com.garward.wurmmodloader.api.events.action.ActionSpeedModifierEvent;
import com.garward.wurmmodloader.api.events.action.ActionTimeCalculationEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialBonusEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialDamageModifierEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialDecayModifierEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialImpBonusEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialRepairTimeEvent;
import com.garward.wurmmodloader.api.events.skill.SkillAdvanceEvent;

import java.util.OptionalDouble;
/**
 * Translates registered {@link MaterialProfile} definitions into runtime event adjustments.
 */
public class MaterialEventHandler {

    private final MaterialProfileRegistry registry;

    public MaterialEventHandler() {
        this(MaterialProfileRegistry.getInstance());
    }

    MaterialEventHandler(MaterialProfileRegistry registry) {
        this.registry = registry;
    }

    @SubscribeEvent
    public void onMaterialDamage(MaterialDamageModifierEvent event) {
        registry.findProfile(event.getMaterial()).ifPresent(profile -> {
            double value = scale(event.getBaseModifier(), profile.damageModifier());
            event.setModifier(value);
        });
    }

    @SubscribeEvent
    public void onMaterialDecay(MaterialDecayModifierEvent event) {
        registry.findProfile(event.getMaterial()).ifPresent(profile -> {
            double value = scale(event.getBaseModifier(), profile.decayModifier());
            event.setModifier(value);
        });
    }

    @SubscribeEvent
    public void onMaterialImp(MaterialImpBonusEvent event) {
        registry.findProfile(event.getMaterial()).ifPresent(profile -> {
            double value = scale(event.getBaseBonus(), profile.improveModifier());
            event.setBonus(value);
        });
    }

    @SubscribeEvent
    public void onMaterialRepair(MaterialRepairTimeEvent event) {
        registry.findProfile(event.getMaterial()).ifPresent(profile -> {
            float value = (float) scale(event.getBaseTimeModifier(), profile.repairTimeModifier());
            event.setTimeModifier(value);
        });
    }

    @SubscribeEvent
    public void onMaterialBonus(MaterialBonusEvent event) {
        registry.findProfile(event.getMaterial()).ifPresent(profile -> {
            OptionalDouble override = OptionalDouble.empty();
            if (event.getBonusType() == MaterialBonusEvent.BonusType.SPELL_POWER &&
                event.getContext() instanceof MaterialBonusEvent.SpellContext) {
                byte enchant = ((MaterialBonusEvent.SpellContext) event.getContext()).getEnchantment();
                override = profile.enchantmentMultiplier(enchant);
            }
            OptionalDouble multiplier = override.isPresent() ? override : profile.bonusMultiplier(event.getBonusType());
            if (multiplier.isPresent()) {
                event.setBonus(scale(event.getBaseBonus(), multiplier));
            } else {
                profile.materialBonusModifier().ifPresent(value -> event.setBonus(scale(event.getBaseBonus(), value)));
            }
        });
    }

    @SubscribeEvent
    public void onWeaponStat(WeaponStatQueryEvent event) {
        registry.findProfile(event.getMaterial()).ifPresent(profile -> {
            profile.weaponStatMultiplier(event.getStatType())
                .ifPresent(mult -> event.setValue(scale(event.getBaseValue(), mult)));
        });
    }

    @SubscribeEvent
    public void onActionTime(ActionTimeCalculationEvent event) {
        byte material = materialFromItem(event.getSource(), event.getTarget());
        registry.findProfile(material).ifPresent(profile ->
            profile.actionTimeModifier()
                .ifPresent(mult -> event.setTime((float) scale(event.getBaseTime(), mult))));
    }

    @SubscribeEvent
    public void onActionSpeed(ActionSpeedModifierEvent event) {
        byte material = materialFromItem(event.getSource(), null);
        registry.findProfile(material).ifPresent(profile ->
            profile.actionSpeedModifier()
                .ifPresent(mult -> event.setModifier((float) scale(event.getBaseModifier(), mult))));
    }

    @SubscribeEvent
    public void onSkillAdvance(SkillAdvanceEvent event) {
        byte material = event.getItem() != null ? event.getItem().getMaterial() : 0;
        registry.findProfile(material).ifPresent(profile -> {
            profile.skillDifficultyModifier()
                .ifPresent(mult -> event.setDifficulty(scale(event.getDifficulty(), mult)));
            profile.skillPowerModifier()
                .ifPresent(mult -> event.setPower(scale(event.getPower(), mult)));
        });
    }

    private double scale(double base, OptionalDouble modifier) {
        return modifier.isPresent() ? base * modifier.getAsDouble() : base;
    }

    private double scale(double base, double multiplier) {
        return base * multiplier;
    }

    private byte materialFromItem(com.wurmonline.server.items.Item primary, com.wurmonline.server.items.Item fallback) {
        if (primary != null) {
            return primary.getMaterial();
        }
        if (fallback != null) {
            return fallback.getMaterial();
        }
        return 0;
    }
}
