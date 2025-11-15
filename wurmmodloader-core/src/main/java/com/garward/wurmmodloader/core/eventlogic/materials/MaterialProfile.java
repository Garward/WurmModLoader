package com.garward.wurmmodloader.core.eventlogic.materials;

import com.garward.wurmmodloader.api.events.combat.weapon.WeaponStatQueryEvent;
import com.garward.wurmmodloader.api.events.item.material.MaterialBonusEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wurmonline.server.items.Materials;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Immutable data object describing how a specific material should influence
 * the various material-related events surfaced by the modloader.
 */
public final class MaterialProfile {

    private final byte materialId;

    private final Double damageModifier;
    private final Double decayModifier;
    private final Double improveModifier;
    private final Double repairTimeModifier;
    private final Double materialBonusModifier;
    private final Double actionTimeModifier;
    private final Double actionSpeedModifier;
    private final Double skillDifficultyModifier;
    private final Double skillPowerModifier;

    private final Map<MaterialBonusEvent.BonusType, Double> bonusMultipliers;
    private final Map<WeaponStatQueryEvent.StatType, Double> weaponStatMultipliers;
    private final Map<Byte, Double> enchantmentMultipliers;

    private MaterialProfile(Builder builder) {
        this.materialId = builder.materialId;
        this.damageModifier = builder.damageModifier;
        this.decayModifier = builder.decayModifier;
        this.improveModifier = builder.improveModifier;
        this.repairTimeModifier = builder.repairTimeModifier;
        this.materialBonusModifier = builder.materialBonusModifier;
        this.actionTimeModifier = builder.actionTimeModifier;
        this.actionSpeedModifier = builder.actionSpeedModifier;
        this.skillDifficultyModifier = builder.skillDifficultyModifier;
        this.skillPowerModifier = builder.skillPowerModifier;
        this.bonusMultipliers = Collections.unmodifiableMap(new EnumMap<>(builder.bonusMultipliers));
        this.weaponStatMultipliers = Collections.unmodifiableMap(new EnumMap<>(builder.weaponStatMultipliers));
        this.enchantmentMultipliers = Collections.unmodifiableMap(new HashMap<>(builder.enchantmentMultipliers));
    }

    public byte getMaterialId() {
        return materialId;
    }

    public OptionalDouble damageModifier() {
        return asOptional(damageModifier);
    }

    public OptionalDouble decayModifier() {
        return asOptional(decayModifier);
    }

    public OptionalDouble improveModifier() {
        return asOptional(improveModifier);
    }

    public OptionalDouble repairTimeModifier() {
        return asOptional(repairTimeModifier);
    }

    public OptionalDouble materialBonusModifier() {
        return asOptional(materialBonusModifier);
    }

    public OptionalDouble actionTimeModifier() {
        return asOptional(actionTimeModifier);
    }

    public OptionalDouble actionSpeedModifier() {
        return asOptional(actionSpeedModifier);
    }

    public OptionalDouble skillDifficultyModifier() {
        return asOptional(skillDifficultyModifier);
    }

    public OptionalDouble skillPowerModifier() {
        return asOptional(skillPowerModifier);
    }

    public OptionalDouble bonusMultiplier(MaterialBonusEvent.BonusType type) {
        Double value = bonusMultipliers.get(type);
        return asOptional(value);
    }

    public OptionalDouble weaponStatMultiplier(WeaponStatQueryEvent.StatType type) {
        Double value = weaponStatMultipliers.get(type);
        return asOptional(value);
    }

    public OptionalDouble enchantmentMultiplier(byte enchantment) {
        Double value = enchantmentMultipliers.get(enchantment);
        return asOptional(value);
    }

    private OptionalDouble asOptional(Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    /**
     * Creates a {@link MaterialProfile} from a JSON object. The schema expects one of the two keys
     * {@code materialId} (numeric) or {@code material} (string matching {@link Materials#getMaterialName(byte)}).
     * Remaining numeric fields are optional.
     */
    public static MaterialProfile fromJson(JsonObject json) {
        Builder builder = builder(parseMaterialId(json));

        builder.damageModifier(getNullableDouble(json, "damageModifier"));
        builder.decayModifier(getNullableDouble(json, "decayModifier"));
        builder.improveModifier(getNullableDouble(json, "improveModifier"));
        builder.repairTimeModifier(getNullableDouble(json, "repairTimeModifier"));
        builder.materialBonusModifier(getNullableDouble(json, "materialBonusModifier"));
        builder.actionTimeModifier(getNullableDouble(json, "actionTimeModifier"));
        builder.actionSpeedModifier(getNullableDouble(json, "actionSpeedModifier"));
        builder.skillDifficultyModifier(getNullableDouble(json, "skillDifficultyModifier"));
        builder.skillPowerModifier(getNullableDouble(json, "skillPowerModifier"));

        if (json.has("bonusMultipliers") && json.get("bonusMultipliers").isJsonObject()) {
            JsonObject obj = json.getAsJsonObject("bonusMultipliers");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                MaterialBonusEvent.BonusType type = parseEnum(MaterialBonusEvent.BonusType.class, entry.getKey());
                if (type != null && entry.getValue().isJsonPrimitive()) {
                    builder.bonusMultiplier(type, entry.getValue().getAsDouble());
                }
            }
        }

        if (json.has("weaponStats") && json.get("weaponStats").isJsonObject()) {
            JsonObject obj = json.getAsJsonObject("weaponStats");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                WeaponStatQueryEvent.StatType type = parseEnum(WeaponStatQueryEvent.StatType.class, entry.getKey());
                if (type != null && entry.getValue().isJsonPrimitive()) {
                    builder.weaponStatMultiplier(type, entry.getValue().getAsDouble());
                }
            }
        }

        if (json.has("spellPowerMultipliers") && json.get("spellPowerMultipliers").isJsonObject()) {
            JsonObject obj = json.getAsJsonObject("spellPowerMultipliers");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    try {
                        byte enchant = (byte) Integer.parseInt(entry.getKey());
                        builder.enchantmentMultiplier(enchant, entry.getValue().getAsDouble());
                    } catch (NumberFormatException ignored) {
                        // skip invalid enchant keys
                    }
                }
            }
        }

        return builder.build();
    }

    private static byte parseMaterialId(JsonObject json) {
        if (json.has("materialId") && json.get("materialId").isJsonPrimitive()) {
            return (byte) json.get("materialId").getAsInt();
        }
        if (json.has("material") && json.get("material").isJsonPrimitive()) {
            String name = json.get("material").getAsString();
            return Materials.convertMaterialStringIntoByte(name);
        }
        throw new IllegalArgumentException("Material profile is missing required \"materialId\" or \"material\" field: " + json);
    }

    private static Double getNullableDouble(JsonObject json, String key) {
        if (json.has(key) && json.get(key).isJsonPrimitive()) {
            return json.get(key).getAsDouble();
        }
        return null;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Builder builder(byte materialId) {
        return new Builder(materialId);
    }

    public static final class Builder {

        private final byte materialId;
        private Double damageModifier;
        private Double decayModifier;
        private Double improveModifier;
        private Double repairTimeModifier;
        private Double materialBonusModifier;
        private Double actionTimeModifier;
        private Double actionSpeedModifier;
        private Double skillDifficultyModifier;
        private Double skillPowerModifier;

        private final EnumMap<MaterialBonusEvent.BonusType, Double> bonusMultipliers = new EnumMap<>(MaterialBonusEvent.BonusType.class);
        private final EnumMap<WeaponStatQueryEvent.StatType, Double> weaponStatMultipliers = new EnumMap<>(WeaponStatQueryEvent.StatType.class);
        private final Map<Byte, Double> enchantmentMultipliers = new HashMap<>();

        private Builder(byte materialId) {
            this.materialId = materialId;
        }

        public Builder damageModifier(Double value) {
            this.damageModifier = value;
            return this;
        }

        public Builder decayModifier(Double value) {
            this.decayModifier = value;
            return this;
        }

        public Builder improveModifier(Double value) {
            this.improveModifier = value;
            return this;
        }

        public Builder repairTimeModifier(Double value) {
            this.repairTimeModifier = value;
            return this;
        }

        public Builder materialBonusModifier(Double value) {
            this.materialBonusModifier = value;
            return this;
        }

        public Builder actionTimeModifier(Double value) {
            this.actionTimeModifier = value;
            return this;
        }

        public Builder actionSpeedModifier(Double value) {
            this.actionSpeedModifier = value;
            return this;
        }

        public Builder skillDifficultyModifier(Double value) {
            this.skillDifficultyModifier = value;
            return this;
        }

        public Builder skillPowerModifier(Double value) {
            this.skillPowerModifier = value;
            return this;
        }

        public Builder bonusMultiplier(MaterialBonusEvent.BonusType type, Double value) {
            if (type != null && value != null) {
                this.bonusMultipliers.put(type, value);
            }
            return this;
        }

        public Builder weaponStatMultiplier(WeaponStatQueryEvent.StatType type, Double value) {
            if (type != null && value != null) {
                this.weaponStatMultipliers.put(type, value);
            }
            return this;
        }

        public Builder enchantmentMultiplier(byte enchantment, Double value) {
            if (value != null) {
                this.enchantmentMultipliers.put(enchantment, value);
            }
            return this;
        }

        public MaterialProfile build() {
            return new MaterialProfile(this);
        }
    }
}
