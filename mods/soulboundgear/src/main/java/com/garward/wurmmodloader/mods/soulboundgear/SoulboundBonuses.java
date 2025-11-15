package com.garward.wurmmodloader.mods.soulboundgear;

import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.mods.materialsystem.MaterialBonus;
import com.garward.wurmmodloader.mods.materialsystem.MaterialRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Calculated bonuses for a soulbound item.
 *
 * <p>Immutable snapshot of all bonuses from level, upgrade nodes, and material
 * infusions. Used to apply enchantments to items and provide stats for combat
 * calculations.</p>
 *
 * <p><strong>Design Philosophy:</strong></p>
 * <ul>
 *   <li>Material infusions → Applied as real Wurm enchantments (visible on item)</li>
 *   <li>Level bonuses → Stored as numbers, applied in combat hooks (PowerScaling)</li>
 *   <li>Node bonuses → Future system, stored as numbers</li>
 *   <li>NOT applied as status effects (to avoid UI clutter)</li>
 * </ul>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class SoulboundBonuses {

    private static final Logger logger = Logger.getLogger(SoulboundBonuses.class.getName());

    // ========================================================================
    // Level Bonuses (from SoulboundItem.level)
    // ========================================================================

    /** Damage multiplier from levels (e.g., 1.10 = 10% bonus) */
    private final float levelDamageMultiplier;

    /** Attack speed multiplier from levels (e.g., 1.05 = 5% faster) */
    private final float levelAttackSpeedMultiplier;

    /** Critical hit chance from levels (e.g., 0.05 = 5% crit) */
    private final float levelCritChance;

    /** Durability multiplier from levels (e.g., 1.20 = 20% more durable) */
    private final float levelDurabilityMultiplier;

    /** Parry chance from levels (e.g., 0.03 = 3% parry) */
    private final float levelParryChance;

    // ========================================================================
    // Material Infusion Bonuses (from MaterialRegistry)
    // ========================================================================

    /** Base damage bonus from materials (flat, added to weapon damage) */
    private final float materialBaseDamage;

    /** Damage multiplier from materials (e.g., 1.05 = 5% bonus) */
    private final float materialDamageMultiplier;

    /** Attack speed multiplier from materials (e.g., 1.10 = 10% faster) */
    private final float materialAttackSpeedMultiplier;

    /** Critical hit chance from materials (e.g., 0.10 = 10% crit) */
    private final float materialCritChance;

    /** Durability multiplier from materials (e.g., 1.15 = 15% more durable) */
    private final float materialDurabilityMultiplier;

    /** Parry chance from materials (e.g., 0.05 = 5% parry) */
    private final float materialParryChance;

    /**
     * Elemental damage from materials.
     * Key: Damage type (e.g., "fire", "cold", "shadow", "poison")
     * Value: Total damage amount (sum of all material stacks)
     */
    private final Map<String, Float> elementalDamage;

    /**
     * Visual effects from materials.
     * List of effect names (e.g., "flames", "frost", "shadow")
     */
    private final String visualEffects;

    // ========================================================================
    // Upgrade Tree Bonuses (future system, placeholder)
    // ========================================================================

    /** Damage multiplier from upgrade nodes (e.g., 1.15 = 15% bonus) */
    private final float nodeDamageMultiplier;

    /** Attack speed multiplier from nodes (e.g., 1.08 = 8% faster) */
    private final float nodeAttackSpeedMultiplier;

    /** Critical hit chance from nodes (e.g., 0.12 = 12% crit) */
    private final float nodeCritChance;

    // ========================================================================
    // Constructor
    // ========================================================================

    private SoulboundBonuses(float levelDamageMultiplier, float levelAttackSpeedMultiplier,
                             float levelCritChance, float levelDurabilityMultiplier,
                             float levelParryChance, float materialBaseDamage,
                             float materialDamageMultiplier, float materialAttackSpeedMultiplier,
                             float materialCritChance, float materialDurabilityMultiplier,
                             float materialParryChance, Map<String, Float> elementalDamage,
                             String visualEffects, float nodeDamageMultiplier,
                             float nodeAttackSpeedMultiplier, float nodeCritChance) {
        this.levelDamageMultiplier = levelDamageMultiplier;
        this.levelAttackSpeedMultiplier = levelAttackSpeedMultiplier;
        this.levelCritChance = levelCritChance;
        this.levelDurabilityMultiplier = levelDurabilityMultiplier;
        this.levelParryChance = levelParryChance;
        this.materialBaseDamage = materialBaseDamage;
        this.materialDamageMultiplier = materialDamageMultiplier;
        this.materialAttackSpeedMultiplier = materialAttackSpeedMultiplier;
        this.materialCritChance = materialCritChance;
        this.materialDurabilityMultiplier = materialDurabilityMultiplier;
        this.materialParryChance = materialParryChance;
        this.elementalDamage = new HashMap<>(elementalDamage);
        this.visualEffects = visualEffects;
        this.nodeDamageMultiplier = nodeDamageMultiplier;
        this.nodeAttackSpeedMultiplier = nodeAttackSpeedMultiplier;
        this.nodeCritChance = nodeCritChance;
    }

    // ========================================================================
    // Calculation
    // ========================================================================

    /**
     * Calculate all bonuses for a soulbound item.
     *
     * @param item Soulbound item data
     * @return Calculated bonuses
     */
    public static SoulboundBonuses calculate(SoulboundItem item) {
        SoulboundConfig config = SoulboundConfig.getInstance();

        // Calculate level bonuses
        int level = item.getLevel();
        float levelDamageMult = 1.0f + (config.getDamagePerLevel() * level);
        float levelAttackSpeedMult = 1.0f + (config.getAttackSpeedPerLevel() * level);
        float levelCritChance = config.getCritChancePerLevel() * level;
        float levelDurabilityMult = 1.0f + (config.getDurabilityPerLevel() * level);
        float levelParryChance = config.getParryPerLevel() * level;

        // Calculate material bonuses
        float matBaseDamage = 0.0f;
        float matDamageMult = 1.0f;
        float matAttackSpeedMult = 1.0f;
        float matCritChance = 0.0f;
        float matDurabilityMult = 1.0f;
        float matParryChance = 0.0f;
        Map<String, Float> elementalDmg = new HashMap<>();
        StringBuilder visualEffectBuilder = new StringBuilder();

        for (Map.Entry<String, Integer> infusion : item.getMaterialInfusions().entrySet()) {
            String materialIdStr = infusion.getKey();
            int stacks = infusion.getValue();

            // Parse ResourceLocation from string (format: "namespace:path")
            ResourceLocation materialId;
            if (materialIdStr.contains(":")) {
                String[] parts = materialIdStr.split(":", 2);
                materialId = new ResourceLocation(parts[0], parts[1]);
            } else {
                // Assume default namespace if no colon
                materialId = new ResourceLocation("wurm", materialIdStr);
            }

            Optional<MaterialBonus> bonusOpt = MaterialRegistry.getBonus(materialId);

            if (!bonusOpt.isPresent()) {
                logger.warning("Unknown material in infusions: " + materialIdStr);
                continue;
            }

            MaterialBonus bonus = bonusOpt.get();

            // Multiply bonuses by stack count
            matBaseDamage += bonus.getBaseDamage() * stacks;
            matDamageMult += bonus.getDamagePercent() * stacks;
            matAttackSpeedMult += bonus.getAttackSpeedPercent() * stacks;
            matCritChance += bonus.getCritChancePercent() * stacks;
            matDurabilityMult += bonus.getDurabilityPercent() * stacks;
            matParryChance += bonus.getParryPercent() * stacks;

            // Accumulate elemental damage
            for (Map.Entry<String, Float> elem : bonus.getElementalDamage().entrySet()) {
                String damageType = elem.getKey();
                float damage = elem.getValue() * stacks;
                elementalDmg.put(damageType,
                    elementalDmg.getOrDefault(damageType, 0.0f) + damage);
            }

            // Accumulate visual effects
            if (bonus.getVisualEffect() != null && !bonus.getVisualEffect().isEmpty()) {
                if (visualEffectBuilder.length() > 0) {
                    visualEffectBuilder.append(",");
                }
                visualEffectBuilder.append(bonus.getVisualEffect());
            }
        }

        // Calculate upgrade tree node bonuses (placeholder for future)
        float nodeDamageMult = 1.0f;
        float nodeAttackSpeedMult = 1.0f;
        float nodeCritChance = 0.0f;

        // TODO: When UpgradeTree system is implemented, query nodes here
        // for (String nodeId : item.getUpgradeTreeNodeIds()) {
        //     UpgradeTreeNode node = UpgradeTreeRegistry.getNode(nodeId);
        //     nodeDamageMult += node.getDamageBonus();
        //     etc...
        // }

        return new SoulboundBonuses(
            levelDamageMult, levelAttackSpeedMult, levelCritChance,
            levelDurabilityMult, levelParryChance,
            matBaseDamage, matDamageMult, matAttackSpeedMult,
            matCritChance, matDurabilityMult, matParryChance,
            elementalDmg, visualEffectBuilder.toString(),
            nodeDamageMult, nodeAttackSpeedMult, nodeCritChance
        );
    }

    // ========================================================================
    // Getters
    // ========================================================================

    // Level Bonuses
    public float getLevelDamageMultiplier() { return levelDamageMultiplier; }
    public float getLevelAttackSpeedMultiplier() { return levelAttackSpeedMultiplier; }
    public float getLevelCritChance() { return levelCritChance; }
    public float getLevelDurabilityMultiplier() { return levelDurabilityMultiplier; }
    public float getLevelParryChance() { return levelParryChance; }

    // Material Bonuses
    public float getMaterialBaseDamage() { return materialBaseDamage; }
    public float getMaterialDamageMultiplier() { return materialDamageMultiplier; }
    public float getMaterialAttackSpeedMultiplier() { return materialAttackSpeedMultiplier; }
    public float getMaterialCritChance() { return materialCritChance; }
    public float getMaterialDurabilityMultiplier() { return materialDurabilityMultiplier; }
    public float getMaterialParryChance() { return materialParryChance; }
    public Map<String, Float> getElementalDamage() { return new HashMap<>(elementalDamage); }
    public String getVisualEffects() { return visualEffects; }

    // Upgrade Tree Bonuses
    public float getNodeDamageMultiplier() { return nodeDamageMultiplier; }
    public float getNodeAttackSpeedMultiplier() { return nodeAttackSpeedMultiplier; }
    public float getNodeCritChance() { return nodeCritChance; }

    // ========================================================================
    // Combined Totals (convenience methods for PowerScaling)
    // ========================================================================

    /**
     * Get total damage multiplier from all sources.
     *
     * @return Combined damage multiplier (level * material * nodes)
     */
    public float getTotalDamageMultiplier() {
        return levelDamageMultiplier * materialDamageMultiplier * nodeDamageMultiplier;
    }

    /**
     * Get total attack speed multiplier from all sources.
     *
     * @return Combined attack speed multiplier (level * material * nodes)
     */
    public float getTotalAttackSpeedMultiplier() {
        return levelAttackSpeedMultiplier * materialAttackSpeedMultiplier * nodeAttackSpeedMultiplier;
    }

    /**
     * Get total critical hit chance from all sources.
     *
     * @return Combined crit chance (sum of level + material + nodes)
     */
    public float getTotalCritChance() {
        return levelCritChance + materialCritChance + nodeCritChance;
    }

    /**
     * Get total durability multiplier from all sources.
     *
     * @return Combined durability multiplier (level * material)
     */
    public float getTotalDurabilityMultiplier() {
        return levelDurabilityMultiplier * materialDurabilityMultiplier;
    }

    /**
     * Get total parry chance from all sources.
     *
     * @return Combined parry chance (sum of level + material)
     */
    public float getTotalParryChance() {
        return levelParryChance + materialParryChance;
    }

    // ========================================================================
    // Utility
    // ========================================================================

    @Override
    public String toString() {
        return String.format(
            "SoulboundBonuses{damage=%.2fx, attackSpeed=%.2fx, crit=%.2f%%, " +
            "baseDmg=+%.0f, elemental=%s, visual=%s}",
            getTotalDamageMultiplier(),
            getTotalAttackSpeedMultiplier(),
            getTotalCritChance() * 100,
            materialBaseDamage,
            elementalDamage,
            visualEffects
        );
    }

    /**
     * Format bonuses for player display.
     *
     * @return Human-readable bonus description
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();

        // Damage
        float totalDmgMult = getTotalDamageMultiplier();
        if (totalDmgMult > 1.0f) {
            sb.append(String.format("Damage: +%.0f%%\n", (totalDmgMult - 1.0f) * 100));
        }

        // Base damage from materials
        if (materialBaseDamage > 0) {
            sb.append(String.format("Base Damage: +%.0f\n", materialBaseDamage));
        }

        // Attack speed
        float totalAtkSpeed = getTotalAttackSpeedMultiplier();
        if (totalAtkSpeed > 1.0f) {
            sb.append(String.format("Attack Speed: +%.0f%%\n", (totalAtkSpeed - 1.0f) * 100));
        }

        // Crit chance
        float totalCrit = getTotalCritChance();
        if (totalCrit > 0) {
            sb.append(String.format("Critical Chance: +%.1f%%\n", totalCrit * 100));
        }

        // Durability
        float totalDurability = getTotalDurabilityMultiplier();
        if (totalDurability > 1.0f) {
            sb.append(String.format("Durability: +%.0f%%\n", (totalDurability - 1.0f) * 100));
        }

        // Parry
        float totalParry = getTotalParryChance();
        if (totalParry > 0) {
            sb.append(String.format("Parry Chance: +%.1f%%\n", totalParry * 100));
        }

        // Elemental damage
        if (!elementalDamage.isEmpty()) {
            sb.append("Elemental Damage:\n");
            for (Map.Entry<String, Float> entry : elementalDamage.entrySet()) {
                String type = entry.getKey();
                float damage = entry.getValue();
                sb.append(String.format("  %s: +%.0f\n",
                    type.substring(0, 1).toUpperCase() + type.substring(1), damage));
            }
        }

        return sb.toString().trim();
    }
}
