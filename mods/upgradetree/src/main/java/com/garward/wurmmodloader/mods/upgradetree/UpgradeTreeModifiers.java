package com.garward.wurmmodloader.mods.upgradetree;

/**
 * Public API for other mods to query upgrade tree modifiers.
 * Used by PowerScaling to apply combat and healing bonuses from upgrades.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class UpgradeTreeModifiers {

    /** Vanilla cap — one pet unless an upgrade says otherwise. */
    public static final int VANILLA_PET_SLOTS = 1;

    // ========================================================================
    // Pet Class Modifiers
    // ========================================================================

    /**
     * Total pet slots the player may own. Vanilla base (1) plus any
     * {@code max_pet_slots} effect values.
     */
    public static int getMaxPetSlots(long playerWurmId) {
        return VANILLA_PET_SLOTS + UpgradeEffectCalculator.getBonusPetSlots(playerWurmId);
    }

    /** Outgoing damage multiplier applied to the player's pets. */
    public static double getPetDamageMultiplier(long playerWurmId) {
        return UpgradeEffectCalculator.getPetDamageMultiplier(playerWurmId);
    }

    /** Incoming damage multiplier applied to the player's pets. 1.0 is no change. */
    public static double getPetDamageTakenMultiplier(long playerWurmId) {
        return UpgradeEffectCalculator.getPetDamageTakenMultiplier(playerWurmId);
    }

    /** True iff any unlocked upgrade grants {@code tame_tier_unlock} for this tier. */
    public static boolean canTameTier(long playerWurmId, String tier) {
        return UpgradeEffectCalculator.getUnlockedTameTiers(playerWurmId).contains(tier);
    }


    // ========================================================================
    // Combat Modifiers
    // ========================================================================

    /**
     * Get total damage multiplier from upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @return Damage multiplier (1.0 = base, 1.15 = +15% damage)
     */
    public static double getDamageMultiplier(long playerWurmId) {
        return UpgradeEffectCalculator.getDamageMultiplier(playerWurmId);
    }

    /**
     * Get defense/damage reduction multiplier from upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @return Defense multiplier (1.0 = base, 1.20 = +20% defense)
     */
    public static double getDefenseMultiplier(long playerWurmId) {
        double reduction = UpgradeEffectCalculator.getDamageReduction(playerWurmId);
        return 1.0 + reduction; // Convert reduction to multiplier
    }

    /**
     * Get critical hit chance from upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @return Crit chance (0.0 to 1.0, where 0.15 = 15% chance)
     */
    public static double getCritChance(long playerWurmId) {
        return UpgradeEffectCalculator.getCritChance(playerWurmId);
    }

    /**
     * Get armor penetration from upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @return Armor penetration (0.0 to 1.0, where 0.30 = ignore 30% armor)
     */
    public static double getArmorPenetration(long playerWurmId) {
        return UpgradeEffectCalculator.getArmorPenetration(playerWurmId);
    }

    // ========================================================================
    // Healing Modifiers
    // ========================================================================

    /**
     * Get healing amount multiplier from upgrades.
     * Used by PowerScaling healing system.
     *
     * @param playerWurmId Player wurm ID
     * @return Multiplier (1.0 = no bonus, 1.5 = +50% healing)
     */
    public static double getHealingMultiplier(long playerWurmId) {
        return UpgradeEffectCalculator.getHealingMultiplier(playerWurmId);
    }

    /**
     * Get healing cooldown multiplier from upgrades.
     * Used by PowerScaling healing system.
     *
     * @param playerWurmId Player wurm ID
     * @return Multiplier (1.0 = no bonus, 0.5 = heal twice as often)
     */
    public static double getHealingCooldownMultiplier(long playerWurmId) {
        return UpgradeEffectCalculator.getHealingCooldownMultiplier(playerWurmId);
    }

    // ========================================================================
    // Skill & Utility Modifiers
    // ========================================================================

    /**
     * Get skill gain multiplier from upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @return Skill gain multiplier (1.0 = base, 1.25 = +25% skill gain)
     */
    public static double getSkillGainMultiplier(long playerWurmId) {
        return UpgradeEffectCalculator.getSkillGainMultiplier(playerWurmId);
    }

    /**
     * Get harvest bonus chance from upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @return Harvest bonus chance (0.0 to 1.0, where 0.20 = 20% chance for double resources)
     */
    public static double getHarvestBonus(long playerWurmId) {
        return UpgradeEffectCalculator.getHarvestBonus(playerWurmId);
    }

    /**
     * Get block chance bonus from upgrades.
     *
     * @param playerWurmId Player wurm ID
     * @return Block chance bonus (0.0 to 1.0, where 0.15 = +15% block chance)
     */
    public static double getBlockChance(long playerWurmId) {
        return UpgradeEffectCalculator.getBlockChance(playerWurmId);
    }
}
