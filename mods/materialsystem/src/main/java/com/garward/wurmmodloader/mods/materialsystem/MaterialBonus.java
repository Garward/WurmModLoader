package com.garward.wurmmodloader.mods.materialsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Bonuses granted by a material when infused into a soulbound item.
 *
 * <p>Immutable class representing all possible bonuses a material can grant.
 * Use the Builder pattern to construct instances.</p>
 *
 * @author Garward
 * @version 1.0.0
 */
public final class MaterialBonus {

    // Flat bonuses (added directly)
    private final float baseDamage;

    // Percentage bonuses (multiplicative)
    private final float damagePercent;
    private final float attackSpeedPercent;
    private final float critChancePercent;
    private final float durabilityPercent;
    private final float parryPercent;

    // Elemental damage (separate damage types)
    private final Map<String, Float> elementalDamage;

    // Special effects
    private final SpecialEffect specialEffect;

    // Visual effects
    private final String visualEffect;

    /**
     * Private constructor - use Builder.
     */
    private MaterialBonus(Builder builder) {
        this.baseDamage = builder.baseDamage;
        this.damagePercent = builder.damagePercent;
        this.attackSpeedPercent = builder.attackSpeedPercent;
        this.critChancePercent = builder.critChancePercent;
        this.durabilityPercent = builder.durabilityPercent;
        this.parryPercent = builder.parryPercent;
        this.elementalDamage = Collections.unmodifiableMap(new HashMap<>(builder.elementalDamage));
        this.specialEffect = builder.specialEffect;
        this.visualEffect = builder.visualEffect;
    }

    // Getters

    public float getBaseDamage() {
        return baseDamage;
    }

    public float getDamagePercent() {
        return damagePercent;
    }

    public float getAttackSpeedPercent() {
        return attackSpeedPercent;
    }

    public float getCritChancePercent() {
        return critChancePercent;
    }

    public float getDurabilityPercent() {
        return durabilityPercent;
    }

    public float getParryPercent() {
        return parryPercent;
    }

    public Map<String, Float> getElementalDamage() {
        return elementalDamage;
    }

    public SpecialEffect getSpecialEffect() {
        return specialEffect;
    }

    public String getVisualEffect() {
        return visualEffect;
    }

    /**
     * Builder for MaterialBonus.
     */
    public static class Builder {
        private float baseDamage = 0.0f;
        private float damagePercent = 0.0f;
        private float attackSpeedPercent = 0.0f;
        private float critChancePercent = 0.0f;
        private float durabilityPercent = 0.0f;
        private float parryPercent = 0.0f;
        private Map<String, Float> elementalDamage = new HashMap<>();
        private SpecialEffect specialEffect = SpecialEffect.NONE;
        private String visualEffect = null;

        public Builder baseDamage(float value) {
            this.baseDamage = value;
            return this;
        }

        public Builder damagePercent(float value) {
            this.damagePercent = value;
            return this;
        }

        public Builder attackSpeedPercent(float value) {
            this.attackSpeedPercent = value;
            return this;
        }

        public Builder critChancePercent(float value) {
            this.critChancePercent = value;
            return this;
        }

        public Builder durabilityPercent(float value) {
            this.durabilityPercent = value;
            return this;
        }

        public Builder parryPercent(float value) {
            this.parryPercent = value;
            return this;
        }

        public Builder elementalDamage(String type, float value) {
            this.elementalDamage.put(type, value);
            return this;
        }

        public Builder specialEffect(SpecialEffect effect) {
            this.specialEffect = effect;
            return this;
        }

        public Builder visualEffect(String effect) {
            this.visualEffect = effect;
            return this;
        }

        public MaterialBonus build() {
            return new MaterialBonus(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MaterialBonus{");
        if (baseDamage != 0) sb.append("baseDamage=").append(baseDamage).append(", ");
        if (damagePercent != 0) sb.append("damagePercent=").append(damagePercent).append(", ");
        if (attackSpeedPercent != 0) sb.append("attackSpeedPercent=").append(attackSpeedPercent).append(", ");
        if (critChancePercent != 0) sb.append("critChancePercent=").append(critChancePercent).append(", ");
        if (!elementalDamage.isEmpty()) sb.append("elementalDamage=").append(elementalDamage).append(", ");
        if (specialEffect != SpecialEffect.NONE) sb.append("specialEffect=").append(specialEffect).append(", ");
        if (visualEffect != null) sb.append("visualEffect=").append(visualEffect);
        sb.append("}");
        return sb.toString();
    }
}
