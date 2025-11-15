package com.garward.wurmmodloader.mods.soulboundgear;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration loader for SoulboundGear mod.
 *
 * <p>Loads all balance parameters from soulboundgear.config and provides
 * type-safe accessors. Values are loaded once on initialization and cached
 * for performance.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public final class SoulboundConfig {

    private static final Logger logger = Logger.getLogger(SoulboundConfig.class.getName());
    private static final String CONFIG_FILE = "soulboundgear.config";

    // Singleton instance
    private static SoulboundConfig instance;

    private final Properties properties = new Properties();

    // Leveling System
    private final int maxItemLevel;
    private final int baseXPRequirement;
    private final float xpCurveExponent;
    private final int upgradePointsPerLevel;

    // XP Gain
    private final int baseXPPerKill;
    private final float crXPMultiplier;
    private final float championXPMultiplier;
    private final float uniqueXPMultiplier;
    private final float titanXPMultiplier;

    // Level Bonuses
    private final float damagePerLevel;
    private final float attackSpeedPerLevel;
    private final float critChancePerLevel;
    private final float durabilityPerLevel;
    private final float parryPerLevel;

    // Binding System
    private final boolean requireAltarForBinding;
    private final boolean requireDeityMatch;
    private final boolean allowCorpseBinding;

    // Customization
    private final boolean allowCustomNames;
    private final boolean allowCustomDescriptions;
    private final int maxCustomNameLength;
    private final int maxCustomDescriptionLength;

    // Material Infusion
    private final int maxInfusionSlots;
    private final boolean allowMaterialRemoval;

    // Performance Tuning
    private final int batchWriteIntervalMs;
    private final int forceSaveIntervalMs;
    private final int cacheSize;

    // Notifications
    private final boolean notifyOnLevelUp;
    private final boolean notifyOnXPGain;
    private final int minXPForNotification;

    // Database Settings
    private final String databasePath;
    private final boolean enableWAL;
    private final int busyTimeoutMs;

    // Debug Options
    private final boolean debugLogging;
    private final boolean logXPCalculations;
    private final boolean logDatabaseOperations;

    /**
     * Private constructor - loads config from file.
     *
     * @param configPath Path to config file
     * @throws IOException If config file cannot be read
     */
    private SoulboundConfig(Path configPath) throws IOException {
        // Load properties file
        try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
            properties.load(fis);
        }

        // Parse and cache all values
        // Leveling System
        maxItemLevel = getIntProperty("maxItemLevel", 20);
        baseXPRequirement = getIntProperty("baseXPRequirement", 1000);
        xpCurveExponent = getFloatProperty("xpCurveExponent", 1.5f);
        upgradePointsPerLevel = getIntProperty("upgradePointsPerLevel", 1);

        // XP Gain
        baseXPPerKill = getIntProperty("baseXPPerKill", 10);
        crXPMultiplier = getFloatProperty("crXPMultiplier", 0.1f);
        championXPMultiplier = getFloatProperty("championXPMultiplier", 5.0f);
        uniqueXPMultiplier = getFloatProperty("uniqueXPMultiplier", 10.0f);
        titanXPMultiplier = getFloatProperty("titanXPMultiplier", 50.0f);

        // Level Bonuses
        damagePerLevel = getFloatProperty("damagePerLevel", 0.02f);
        attackSpeedPerLevel = getFloatProperty("attackSpeedPerLevel", 0.0f);
        critChancePerLevel = getFloatProperty("critChancePerLevel", 0.0f);
        durabilityPerLevel = getFloatProperty("durabilityPerLevel", 0.01f);
        parryPerLevel = getFloatProperty("parryPerLevel", 0.0f);

        // Binding System
        requireAltarForBinding = getBooleanProperty("requireAltarForBinding", true);
        requireDeityMatch = getBooleanProperty("requireDeityMatch", true);
        allowCorpseBinding = getBooleanProperty("allowCorpseBinding", false);

        // Customization
        allowCustomNames = getBooleanProperty("allowCustomNames", true);
        allowCustomDescriptions = getBooleanProperty("allowCustomDescriptions", true);
        maxCustomNameLength = getIntProperty("maxCustomNameLength", 50);
        maxCustomDescriptionLength = getIntProperty("maxCustomDescriptionLength", 200);

        // Material Infusion
        maxInfusionSlots = getIntProperty("maxInfusionSlots", 3);
        allowMaterialRemoval = getBooleanProperty("allowMaterialRemoval", false);

        // Performance Tuning
        batchWriteIntervalMs = getIntProperty("batchWriteIntervalMs", 100);
        forceSaveIntervalMs = getIntProperty("forceSaveIntervalMs", 300000);
        cacheSize = getIntProperty("cacheSize", 1000);

        // Notifications
        notifyOnLevelUp = getBooleanProperty("notifyOnLevelUp", true);
        notifyOnXPGain = getBooleanProperty("notifyOnXPGain", false);
        minXPForNotification = getIntProperty("minXPForNotification", 50);

        // Database Settings
        databasePath = getStringProperty("databasePath", "soulboundgear.db");
        enableWAL = getBooleanProperty("enableWAL", true);
        busyTimeoutMs = getIntProperty("busyTimeoutMs", 5000);

        // Debug Options
        debugLogging = getBooleanProperty("debugLogging", false);
        logXPCalculations = getBooleanProperty("logXPCalculations", false);
        logDatabaseOperations = getBooleanProperty("logDatabaseOperations", false);

        logger.info("SoulboundConfig loaded successfully");
        if (debugLogging) {
            logConfiguration();
        }
    }

    /**
     * Initialize configuration from file.
     *
     * @param configPath Path to config file
     * @throws IOException If config file cannot be read
     */
    public static void initialize(Path configPath) throws IOException {
        if (instance != null) {
            throw new IllegalStateException("SoulboundConfig already initialized");
        }
        instance = new SoulboundConfig(configPath);
    }

    /**
     * Get singleton instance.
     *
     * @return Config instance
     * @throws IllegalStateException If not initialized
     */
    public static SoulboundConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SoulboundConfig not initialized");
        }
        return instance;
    }

    // ========================================================================
    // Property Accessors
    // ========================================================================

    private String getStringProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Invalid integer for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private float getFloatProperty(String key, float defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Invalid float for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    // ========================================================================
    // Public Getters
    // ========================================================================

    // Leveling System
    public int getMaxItemLevel() { return maxItemLevel; }
    public int getBaseXPRequirement() { return baseXPRequirement; }
    public float getXpCurveExponent() { return xpCurveExponent; }
    public int getUpgradePointsPerLevel() { return upgradePointsPerLevel; }

    // XP Gain
    public int getBaseXPPerKill() { return baseXPPerKill; }
    public float getCrXPMultiplier() { return crXPMultiplier; }
    public float getChampionXPMultiplier() { return championXPMultiplier; }
    public float getUniqueXPMultiplier() { return uniqueXPMultiplier; }
    public float getTitanXPMultiplier() { return titanXPMultiplier; }

    // Level Bonuses
    public float getDamagePerLevel() { return damagePerLevel; }
    public float getAttackSpeedPerLevel() { return attackSpeedPerLevel; }
    public float getCritChancePerLevel() { return critChancePerLevel; }
    public float getDurabilityPerLevel() { return durabilityPerLevel; }
    public float getParryPerLevel() { return parryPerLevel; }

    // Binding System
    public boolean isRequireAltarForBinding() { return requireAltarForBinding; }
    public boolean isRequireDeityMatch() { return requireDeityMatch; }
    public boolean isAllowCorpseBinding() { return allowCorpseBinding; }

    // Customization
    public boolean isAllowCustomNames() { return allowCustomNames; }
    public boolean isAllowCustomDescriptions() { return allowCustomDescriptions; }
    public int getMaxCustomNameLength() { return maxCustomNameLength; }
    public int getMaxCustomDescriptionLength() { return maxCustomDescriptionLength; }

    // Material Infusion
    public int getMaxInfusionSlots() { return maxInfusionSlots; }
    public boolean isAllowMaterialRemoval() { return allowMaterialRemoval; }

    // Performance Tuning
    public int getBatchWriteIntervalMs() { return batchWriteIntervalMs; }
    public int getForceSaveIntervalMs() { return forceSaveIntervalMs; }
    public int getCacheSize() { return cacheSize; }

    // Notifications
    public boolean isNotifyOnLevelUp() { return notifyOnLevelUp; }
    public boolean isNotifyOnXPGain() { return notifyOnXPGain; }
    public int getMinXPForNotification() { return minXPForNotification; }

    // Database Settings
    public String getDatabasePath() { return databasePath; }
    public boolean isEnableWAL() { return enableWAL; }
    public int getBusyTimeoutMs() { return busyTimeoutMs; }

    // Debug Options
    public boolean isDebugLogging() { return debugLogging; }
    public boolean isLogXPCalculations() { return logXPCalculations; }
    public boolean isLogDatabaseOperations() { return logDatabaseOperations; }

    /**
     * Calculate XP required for a given level.
     *
     * @param level Target level
     * @return XP required to reach that level
     */
    public int getXPRequiredForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        return (int) (baseXPRequirement * Math.pow(level, xpCurveExponent));
    }

    /**
     * Log all configuration values (for debugging).
     */
    private void logConfiguration() {
        logger.info("=== SoulboundGear Configuration ===");
        logger.info("Max Item Level: " + maxItemLevel);
        logger.info("Base XP Requirement: " + baseXPRequirement);
        logger.info("XP Curve Exponent: " + xpCurveExponent);
        logger.info("Upgrade Points Per Level: " + upgradePointsPerLevel);
        logger.info("Base XP Per Kill: " + baseXPPerKill);
        logger.info("CR XP Multiplier: " + crXPMultiplier);
        logger.info("Champion XP Multiplier: " + championXPMultiplier);
        logger.info("Unique XP Multiplier: " + uniqueXPMultiplier);
        logger.info("Titan XP Multiplier: " + titanXPMultiplier);
        logger.info("Damage Per Level: " + damagePerLevel);
        logger.info("Batch Write Interval: " + batchWriteIntervalMs + "ms");
        logger.info("Cache Size: " + cacheSize);
        logger.info("Database Path: " + databasePath);
        logger.info("===================================");
    }
}
