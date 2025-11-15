package com.garward.wurmmodloader.mods.powerscaling;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Optional integration with UpgradeTree mod using reflection.
 *
 * <p>This avoids compile-time circular dependency:
 * - UpgradeTree depends on PowerScaling (for power spending)
 * - PowerScaling optionally uses UpgradeTree (for upgrade bonuses)
 *
 * <p>Using reflection allows PowerScaling to work with or without UpgradeTree installed.
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class UpgradeTreeIntegration {

    private static final Logger logger = Logger.getLogger(UpgradeTreeIntegration.class.getName());

    private static Class<?> modifiersClass;
    private static Method getDamageMultiplierMethod;
    private static Method getDefenseMultiplierMethod;
    private static Method getCritChanceMethod;
    private static Method getArmorPenetrationMethod;
    private static Method getHealingMultiplierMethod;
    private static Method getHealingCooldownMultiplierMethod;
    private static boolean initialized = false;
    private static boolean available = false;

    /**
     * Initialize UpgradeTree integration (call once at startup).
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        try {
            modifiersClass = Class.forName("com.garward.wurmmodloader.mods.upgradetree.UpgradeTreeModifiers");
            getDamageMultiplierMethod = modifiersClass.getMethod("getDamageMultiplier", long.class);
            getDefenseMultiplierMethod = modifiersClass.getMethod("getDefenseMultiplier", long.class);
            getCritChanceMethod = modifiersClass.getMethod("getCritChance", long.class);
            getArmorPenetrationMethod = modifiersClass.getMethod("getArmorPenetration", long.class);
            getHealingMultiplierMethod = modifiersClass.getMethod("getHealingMultiplier", long.class);
            getHealingCooldownMultiplierMethod = modifiersClass.getMethod("getHealingCooldownMultiplier", long.class);
            available = true;
            logger.info("[PowerScaling] UpgradeTree integration enabled - upgrade bonuses will be applied");
        } catch (ClassNotFoundException e) {
            logger.info("[PowerScaling] UpgradeTree mod not found - upgrade bonuses disabled");
        } catch (Exception e) {
            logger.warning("[PowerScaling] Failed to initialize UpgradeTree integration: " + e.getMessage());
        }
    }

    /**
     * Check if UpgradeTree is available.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Get damage multiplier from upgrades (returns 1.0 if not available).
     */
    public static double getDamageMultiplier(long playerWurmId) {
        if (!available) return 1.0;
        try {
            return (double) getDamageMultiplierMethod.invoke(null, playerWurmId);
        } catch (Exception e) {
            logger.warning("[PowerScaling] Failed to get damage multiplier: " + e.getMessage());
            return 1.0;
        }
    }

    /**
     * Get defense multiplier from upgrades (returns 1.0 if not available).
     */
    public static double getDefenseMultiplier(long playerWurmId) {
        if (!available) return 1.0;
        try {
            return (double) getDefenseMultiplierMethod.invoke(null, playerWurmId);
        } catch (Exception e) {
            logger.warning("[PowerScaling] Failed to get defense multiplier: " + e.getMessage());
            return 1.0;
        }
    }

    /**
     * Get crit chance from upgrades (returns 0.0 if not available).
     */
    public static double getCritChance(long playerWurmId) {
        if (!available) return 0.0;
        try {
            return (double) getCritChanceMethod.invoke(null, playerWurmId);
        } catch (Exception e) {
            logger.warning("[PowerScaling] Failed to get crit chance: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get armor penetration from upgrades (returns 0.0 if not available).
     */
    public static double getArmorPenetration(long playerWurmId) {
        if (!available) return 0.0;
        try {
            return (double) getArmorPenetrationMethod.invoke(null, playerWurmId);
        } catch (Exception e) {
            logger.warning("[PowerScaling] Failed to get armor penetration: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get healing multiplier from upgrades (returns 1.0 if not available).
     */
    public static double getHealingMultiplier(long playerWurmId) {
        if (!available) return 1.0;
        try {
            return (double) getHealingMultiplierMethod.invoke(null, playerWurmId);
        } catch (Exception e) {
            logger.warning("[PowerScaling] Failed to get healing multiplier: " + e.getMessage());
            return 1.0;
        }
    }

    /**
     * Get healing cooldown multiplier from upgrades (returns 1.0 if not available).
     */
    public static double getHealingCooldownMultiplier(long playerWurmId) {
        if (!available) return 1.0;
        try {
            return (double) getHealingCooldownMultiplierMethod.invoke(null, playerWurmId);
        } catch (Exception e) {
            logger.warning("[PowerScaling] Failed to get healing cooldown multiplier: " + e.getMessage());
            return 1.0;
        }
    }
}
