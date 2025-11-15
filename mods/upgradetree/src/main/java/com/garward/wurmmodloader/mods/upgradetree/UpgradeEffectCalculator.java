package com.garward.wurmmodloader.mods.upgradetree;

import java.util.Set;

/**
 * Calculates cumulative bonuses from all unlocked upgrades.
 * Server owners configure effect values in JSON, this class sums them up.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class UpgradeEffectCalculator {

    /**
     * Get total damage multiplier from upgrades.
     * Stacks additively (1.0 = base, 1.15 = +15% damage).
     *
     * @param playerWurmId Player wurm ID
     * @return Damage multiplier
     */
    public static double getDamageMultiplier(long playerWurmId) {
        return getEffectSum(playerWurmId, "damage_multiplier", 1.0);
    }

    /**
     * Get flat critical hit chance from upgrades.
     * Returns raw chance value (0.0 to 1.0).
     *
     * @param playerWurmId Player wurm ID
     * @return Crit chance (0.15 = 15% chance)
     */
    public static double getCritChance(long playerWurmId) {
        return getEffectSum(playerWurmId, "crit_chance", 0.0);
    }

    /**
     * Get damage reduction percentage from upgrades.
     * Returns raw reduction value (0.0 to 1.0).
     *
     * @param playerWurmId Player wurm ID
     * @return Damage reduction (0.20 = 20% reduction)
     */
    public static double getDamageReduction(long playerWurmId) {
        return getEffectSum(playerWurmId, "damage_reduction", 0.0);
    }

    /**
     * Get armor penetration percentage from upgrades.
     * Returns raw penetration value (0.0 to 1.0).
     *
     * @param playerWurmId Player wurm ID
     * @return Armor penetration (0.30 = ignore 30% armor)
     */
    public static double getArmorPenetration(long playerWurmId) {
        return getEffectSum(playerWurmId, "armor_pen", 0.0);
    }

    /**
     * Get skill gain multiplier from upgrades.
     * Stacks additively (1.0 = base, 1.25 = +25% skill gain).
     *
     * @param playerWurmId Player wurm ID
     * @return Skill gain multiplier
     */
    public static double getSkillGainMultiplier(long playerWurmId) {
        return getEffectSum(playerWurmId, "skill_gain", 1.0);
    }

    /**
     * Get harvest bonus chance from upgrades.
     * Returns raw chance value (0.0 to 1.0).
     *
     * @param playerWurmId Player wurm ID
     * @return Harvest bonus chance (0.20 = 20% chance for double resources)
     */
    public static double getHarvestBonus(long playerWurmId) {
        return getEffectSum(playerWurmId, "harvest_bonus", 0.0);
    }

    /**
     * Get block chance modifier from upgrades.
     * Returns raw chance value (0.0 to 1.0).
     *
     * @param playerWurmId Player wurm ID
     * @return Block chance bonus (0.15 = +15% block chance)
     */
    public static double getBlockChance(long playerWurmId) {
        return getEffectSum(playerWurmId, "block_chance", 0.0);
    }

    /**
     * Get healing amount multiplier from upgrades.
     * Used by PowerScaling healing system.
     *
     * @param playerWurmId Player wurm ID
     * @return Healing multiplier (1.5 = +50% healing)
     */
    public static double getHealingMultiplier(long playerWurmId) {
        return getEffectSum(playerWurmId, "healing_amount", 1.0);
    }

    /**
     * Get healing cooldown multiplier from upgrades.
     * Used by PowerScaling healing system.
     *
     * @param playerWurmId Player wurm ID
     * @return Cooldown multiplier (0.5 = heal twice as often)
     */
    public static double getHealingCooldownMultiplier(long playerWurmId) {
        double reduction = getEffectSum(playerWurmId, "healing_cooldown", 0.0);
        return Math.max(0.1, 1.0 - reduction); // Cap at 90% reduction
    }

    // ========================================================================
    // Internal Helper Methods
    // ========================================================================

    /**
     * Sum all effect values of a given type from player's unlocked upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @param effectType Effect type to sum
     * @param baseValue Base value (returned if no upgrades)
     * @return Summed effect value
     */
    private static double getEffectSum(long playerWurmId, String effectType, double baseValue) {
        UpgradeTreeManager manager = UpgradeTreeManager.getInstance();
        Set<String> playerUpgrades = manager.getPlayerUpgrades(playerWurmId);

        double total = baseValue;

        for (String upgradeId : playerUpgrades) {
            Upgrade upgrade = manager.getUpgrade(upgradeId);
            if (upgrade == null) continue;

            for (UpgradeEffect effect : upgrade.getEffects()) {
                if (effect.getType().equals(effectType)) {
                    double value = effect.getDouble("value", 0.0);

                    // For multipliers (base 1.0), add the bonus
                    // For flat values (base 0.0), add directly
                    if (baseValue == 1.0) {
                        total += value; // e.g., 1.0 + 0.15 = 1.15 (15% bonus)
                    } else {
                        total += value; // e.g., 0.0 + 0.15 = 0.15 (15% flat)
                    }
                }
            }
        }

        return total;
    }
}
