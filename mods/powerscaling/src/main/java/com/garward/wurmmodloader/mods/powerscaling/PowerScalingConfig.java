package com.garward.wurmmodloader.mods.powerscaling;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration loader for PowerScaling mod.
 *
 * <p>Loads all combat scaling parameters and provides type-safe accessors.
 * Values are loaded once on initialization and cached for performance.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public final class PowerScalingConfig {

    private static final Logger logger = Logger.getLogger(PowerScalingConfig.class.getName());
    private static final String CONFIG_FILE = "powerscaling.config";

    // Singleton instance
    private static PowerScalingConfig instance;

    private final Properties properties = new Properties();

    // Player Power Scaling
    private final float basePowerMultiplier;
    private final float killPowerRate;
    private final int killDiminishingThreshold;
    private final int maxPlayerPowerLevel;

    // Creature Power Scaling
    private final float crPowerMultiplier;
    private final float championPowerMultiplier;
    private final float uniquePowerMultiplier;
    private final float titanPowerMultiplier;

    // Combat Scaling
    private final float damagePerPowerLevel;
    private final float defensePerPowerLevel;
    private final float hpPerPowerLevel;

    // Tier Combat Multipliers — applied multiplicatively on top of the
    // per-power-level linear scaling. Lets tier thresholds actually bite
    // (Normal→Elite adds a flat kick beyond the point-per-point curve).
    private final float normalTierMultiplier;
    private final float eliteTierMultiplier;
    private final float mythicalTierMultiplier;
    private final float legendaryTierMultiplier;
    private final float godlikeTierMultiplier;

    // Critical Hits
    private final float baseCritChance;
    private final float critMultiplier;

    // Attack Speed
    private final float minAttackSpeedMultiplier;
    private final float maxAttackSpeedMultiplier;

    // Status Effect Display
    private final boolean showPowerLevelStatusEffect;
    private final int statusEffectUpdateInterval;
    private final boolean showDetailedPowerBreakdown;

    // Power Level Tiers
    private final int noviceTier;
    private final int adeptTier;
    private final int expertTier;
    private final int masterTier;

    // Performance Tuning
    private final boolean cachePlayerPowerLevels;
    private final int powerLevelCacheSize;
    private final int powerLevelSyncInterval;
    private final int forceSaveInterval;

    // Integration Options
    private final boolean integrateWithSoulboundGear;
    private final boolean applyElementalDamage;
    private final boolean integrateWithUpgradeTree;

    // Creature Spawn
    private final boolean assignCreaturePowerOnSpawn;
    private final int minCreaturePowerLevel;
    private final int maxCreaturePowerLevel;
    private final boolean showCreaturePowerInExamine;

    // Balancing Tweaks
    private final boolean pvpPowerScalingEnabled;
    private final float pvpPowerScalingFactor;
    private final float bossPowerBonus;
    private final float riftCreaturePowerBonus;

    // Database Settings
    private final String databasePath;
    private final boolean enableWAL;
    private final int busyTimeoutMs;

    // Debug Options
    private final boolean debugLogging;
    private final boolean logPowerCalculations;
    private final boolean logCombatCalculations;
    private final boolean logCreaturePowerAssignments;

    // Healing System
    private final boolean enableOutOfCombatRegen;
    private final int outOfCombatDelay;
    private final int outOfCombatRegenInterval;
    private final int outOfCombatBaseHealing;
    private final int outOfCombatPowerScaling;
    private final float villageHealingMultiplier;
    private final float guardTowerHealingMultiplier;
    private final float wildernessHealingMultiplier;
    private final float enemyTerritoryHealingMultiplier;
    private final boolean showHealingMessages;
    private final boolean showHealingEffects;

    // Metal Loot System
    private final boolean enableMetalLoot;
    private final float metalLootBaseQL;
    private final float metalLootPowerMultiplier;
    private final float metalLootQLVariance;
    private final int championBonusAmount;
    private final int uniqueBonusAmount;

    // Dynamic Creature Power System (Phase 1)
    private final boolean useDynamicCreaturePower;
    private final int creatureBaseMin;
    private final int creatureBaseMax;
    private final double ageGrowthRate;
    private final float ageGrowthMaxMultiplier;
    private final int growthTickIntervalSeconds;

    // Settlement Suppression
    private final boolean villageSuppressionEnabled;
    private final float villageSuppression;
    private final int villageSafeRadius;
    private final boolean includeDeedsAsVillages;

    // Visual Feedback
    private final boolean modelScalingEnabled;
    private final float modelScaleMin;
    private final float modelScaleMax;
    private final boolean nameSuffixEnabled;

    // Performance
    private final boolean dynamicCacheEnabled;
    private final long cacheTTL;
    private final int maxCreaturesPerTick;

    // Debug - Dynamic System
    private final boolean logPowerAssignments;
    private final boolean logGrowthTicks;

    /**
     * Private constructor - loads config from file.
     *
     * @param configPath Path to config file
     * @throws IOException If config file cannot be read
     */
    private PowerScalingConfig(Path configPath) throws IOException {
        this(loadFromFile(configPath));
    }

    private static Properties loadFromFile(Path configPath) throws IOException {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
            p.load(fis);
        }
        return p;
    }

    private PowerScalingConfig(Properties source) {
        properties.putAll(source);

        // Parse and cache all values
        // Player Power Scaling
        basePowerMultiplier = getFloatProperty("basePowerMultiplier", 1.0f);
        killPowerRate = getFloatProperty("killPowerRate", 0.01f);
        killDiminishingThreshold = getIntProperty("killDiminishingThreshold", 1000);
        maxPlayerPowerLevel = getIntProperty("maxPlayerPowerLevel", 100);

        // Creature Power Scaling
        crPowerMultiplier = getFloatProperty("crPowerMultiplier", 1.0f);
        championPowerMultiplier = getFloatProperty("championPowerMultiplier", 2.0f);
        uniquePowerMultiplier = getFloatProperty("uniquePowerMultiplier", 5.0f);
        titanPowerMultiplier = getFloatProperty("titanPowerMultiplier", 10.0f);

        // Combat Scaling
        damagePerPowerLevel = getFloatProperty("damagePerPowerLevel", 0.02f);
        defensePerPowerLevel = getFloatProperty("defensePerPowerLevel", 0.01f);
        hpPerPowerLevel = getFloatProperty("hpPerPowerLevel", 0.05f);

        // Tier Combat Multipliers — conservative defaults, tune via config.
        normalTierMultiplier    = getFloatProperty("normalTierMultiplier",    1.0f);
        eliteTierMultiplier     = getFloatProperty("eliteTierMultiplier",     1.25f);
        mythicalTierMultiplier  = getFloatProperty("mythicalTierMultiplier",  1.6f);
        legendaryTierMultiplier = getFloatProperty("legendaryTierMultiplier", 2.0f);
        godlikeTierMultiplier   = getFloatProperty("godlikeTierMultiplier",   3.0f);

        // Critical Hits
        baseCritChance = getFloatProperty("baseCritChance", 0.05f);
        critMultiplier = getFloatProperty("critMultiplier", 2.0f);

        // Attack Speed
        minAttackSpeedMultiplier = getFloatProperty("minAttackSpeedMultiplier", 0.5f);
        maxAttackSpeedMultiplier = getFloatProperty("maxAttackSpeedMultiplier", 3.0f);

        // Status Effect Display
        showPowerLevelStatusEffect = getBooleanProperty("showPowerLevelStatusEffect", true);
        statusEffectUpdateInterval = getIntProperty("statusEffectUpdateInterval", 60000);
        showDetailedPowerBreakdown = getBooleanProperty("showDetailedPowerBreakdown", true);

        // Power Level Tiers
        noviceTier = getIntProperty("noviceTier", 25);
        adeptTier = getIntProperty("adeptTier", 50);
        expertTier = getIntProperty("expertTier", 75);
        masterTier = getIntProperty("masterTier", 100);

        // Performance Tuning
        cachePlayerPowerLevels = getBooleanProperty("cachePlayerPowerLevels", true);
        powerLevelCacheSize = getIntProperty("powerLevelCacheSize", 1000);
        powerLevelSyncInterval = getIntProperty("powerLevelSyncInterval", 30000);
        forceSaveInterval = getIntProperty("forceSaveInterval", 300000);

        // Integration Options
        integrateWithSoulboundGear = getBooleanProperty("integrateWithSoulboundGear", true);
        applyElementalDamage = getBooleanProperty("applyElementalDamage", true);
        integrateWithUpgradeTree = getBooleanProperty("integrateWithUpgradeTree", true);

        // Creature Spawn
        assignCreaturePowerOnSpawn = getBooleanProperty("assignCreaturePowerOnSpawn", true);
        minCreaturePowerLevel = getIntProperty("minCreaturePowerLevel", 1);
        maxCreaturePowerLevel = getIntProperty("maxCreaturePowerLevel", 1000);
        showCreaturePowerInExamine = getBooleanProperty("showCreaturePowerInExamine", true);

        // Balancing Tweaks
        pvpPowerScalingEnabled = getBooleanProperty("pvpPowerScalingEnabled", false);
        pvpPowerScalingFactor = getFloatProperty("pvpPowerScalingFactor", 0.5f);
        bossPowerBonus = getFloatProperty("bossPowerBonus", 1.5f);
        riftCreaturePowerBonus = getFloatProperty("riftCreaturePowerBonus", 2.0f);

        // Database Settings
        databasePath = getStringProperty("databasePath", "powerscaling.db");
        enableWAL = getBooleanProperty("enableWAL", true);
        busyTimeoutMs = getIntProperty("busyTimeoutMs", 5000);

        // Debug Options
        debugLogging = getBooleanProperty("debugLogging", false);
        logPowerCalculations = getBooleanProperty("logPowerCalculations", false);
        logCombatCalculations = getBooleanProperty("logCombatCalculations", false);
        logCreaturePowerAssignments = getBooleanProperty("logCreaturePowerAssignments", false);

        // Healing System
        enableOutOfCombatRegen = getBooleanProperty("enableOutOfCombatRegen", true);
        outOfCombatDelay = getIntProperty("outOfCombatDelay", 10000);
        outOfCombatRegenInterval = getIntProperty("outOfCombatRegenInterval", 5000);
        outOfCombatBaseHealing = getIntProperty("outOfCombatBaseHealing", 5000);
        outOfCombatPowerScaling = getIntProperty("outOfCombatPowerScaling", 50);
        villageHealingMultiplier = getFloatProperty("villageHealingMultiplier", 2.0f);
        guardTowerHealingMultiplier = getFloatProperty("guardTowerHealingMultiplier", 1.5f);
        wildernessHealingMultiplier = getFloatProperty("wildernessHealingMultiplier", 1.0f);
        enemyTerritoryHealingMultiplier = getFloatProperty("enemyTerritoryHealingMultiplier", 0.5f);
        showHealingMessages = getBooleanProperty("showHealingMessages", true);
        showHealingEffects = getBooleanProperty("showHealingEffects", true);

        // Metal Loot System
        enableMetalLoot = getBooleanProperty("enableMetalLoot", true);
        metalLootBaseQL = getFloatProperty("metalLootBaseQL", 30.0f);
        metalLootPowerMultiplier = getFloatProperty("metalLootPowerMultiplier", 0.65f);
        metalLootQLVariance = getFloatProperty("metalLootQLVariance", 5.0f);
        championBonusAmount = getIntProperty("championBonusAmount", 2);
        uniqueBonusAmount = getIntProperty("uniqueBonusAmount", 4);

        // Dynamic Creature Power System (Phase 1)
        useDynamicCreaturePower = getBooleanProperty("powerscaling.creatures.useDynamicSystem", false);
        creatureBaseMin = getIntProperty("powerscaling.creatures.baseMin", 100);
        creatureBaseMax = getIntProperty("powerscaling.creatures.baseMax", 1000);
        ageGrowthRate = getDoubleProperty("powerscaling.creatures.ageGrowthRate", 0.002);
        ageGrowthMaxMultiplier = getFloatProperty("powerscaling.creatures.ageGrowthMaxMultiplier", 3.0f);
        growthTickIntervalSeconds = getIntProperty("powerscaling.creatures.growthTickIntervalSeconds", 300);

        // Settlement Suppression
        villageSuppressionEnabled = getBooleanProperty("powerscaling.villages.suppressionEnabled", true);
        villageSuppression = getFloatProperty("powerscaling.villages.suppressionFactor", 0.8f);
        villageSafeRadius = getIntProperty("powerscaling.villages.safeRadius", 100);
        includeDeedsAsVillages = getBooleanProperty("powerscaling.villages.includeDeedsAsVillages", true);

        // Visual Feedback
        modelScalingEnabled = getBooleanProperty("powerscaling.visual.enableModelScaling", true);
        modelScaleMin = getFloatProperty("powerscaling.visual.modelScaleMin", 0.8f);
        modelScaleMax = getFloatProperty("powerscaling.visual.modelScaleMax", 2.0f);
        nameSuffixEnabled = getBooleanProperty("powerscaling.visual.enableNameSuffixes", true);

        // Performance
        dynamicCacheEnabled = getBooleanProperty("powerscaling.performance.cacheEnabled", true);
        cacheTTL = getLongProperty("powerscaling.performance.cacheTTL", 30000L);
        maxCreaturesPerTick = getIntProperty("powerscaling.performance.maxCreaturesPerTick", 100);

        // Debug - Dynamic System
        logPowerAssignments = getBooleanProperty("powerscaling.debug.logPowerAssignments", false);
        logGrowthTicks = getBooleanProperty("powerscaling.debug.logGrowthTicks", false);

        logger.info("PowerScalingConfig loaded successfully");
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
            throw new IllegalStateException("PowerScalingConfig already initialized");
        }
        instance = new PowerScalingConfig(configPath);
    }

    /** Initialize from properties already loaded by the framework's Configurable callback. */
    public static void initialize(Properties properties) {
        if (instance != null) {
            throw new IllegalStateException("PowerScalingConfig already initialized");
        }
        instance = new PowerScalingConfig(properties);
    }

    /**
     * Get singleton instance.
     *
     * @return Config instance
     * @throws IllegalStateException If not initialized
     */
    public static PowerScalingConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PowerScalingConfig not initialized");
        }
        return instance;
    }

    // ========================================================================
    // Property Accessors
    // ========================================================================

    private String stripInlineComment(String value) {
        if (value == null) {
            return null;
        }
        int commentIndex = value.indexOf('#');
        if (commentIndex >= 0) {
            value = value.substring(0, commentIndex);
        }
        return value.trim();
    }

    private String getStringProperty(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        return stripInlineComment(value);
    }

    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = stripInlineComment(properties.getProperty(key));
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Invalid integer for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private float getFloatProperty(String key, float defaultValue) {
        try {
            String value = stripInlineComment(properties.getProperty(key));
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Invalid float for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = stripInlineComment(properties.getProperty(key));
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private double getDoubleProperty(String key, double defaultValue) {
        try {
            String value = stripInlineComment(properties.getProperty(key));
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Invalid double for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    private long getLongProperty(String key, long defaultValue) {
        try {
            String value = stripInlineComment(properties.getProperty(key));
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warning("Invalid long for " + key + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    // ========================================================================
    // Public Getters - Player Power
    // ========================================================================

    public float getBasePowerMultiplier() { return basePowerMultiplier; }
    public float getKillPowerRate() { return killPowerRate; }
    public int getKillDiminishingThreshold() { return killDiminishingThreshold; }
    public int getMaxPlayerPowerLevel() { return maxPlayerPowerLevel; }

    // ========================================================================
    // Public Getters - Creature Power
    // ========================================================================

    public float getCrPowerMultiplier() { return crPowerMultiplier; }
    public float getChampionPowerMultiplier() { return championPowerMultiplier; }
    public float getUniquePowerMultiplier() { return uniquePowerMultiplier; }
    public float getTitanPowerMultiplier() { return titanPowerMultiplier; }

    // ========================================================================
    // Public Getters - Combat Scaling
    // ========================================================================

    public float getDamagePerPowerLevel() { return damagePerPowerLevel; }
    public float getDefensePerPowerLevel() { return defensePerPowerLevel; }
    public float getHpPerPowerLevel() { return hpPerPowerLevel; }

    public float getNormalTierMultiplier()    { return normalTierMultiplier;    }
    public float getEliteTierMultiplier()     { return eliteTierMultiplier;     }
    public float getMythicalTierMultiplier()  { return mythicalTierMultiplier;  }
    public float getLegendaryTierMultiplier() { return legendaryTierMultiplier; }
    public float getGodlikeTierMultiplier()   { return godlikeTierMultiplier;   }

    // ========================================================================
    // Public Getters - Critical Hits
    // ========================================================================

    public float getBaseCritChance() { return baseCritChance; }
    public float getCritMultiplier() { return critMultiplier; }

    // ========================================================================
    // Public Getters - Attack Speed
    // ========================================================================

    public float getMinAttackSpeedMultiplier() { return minAttackSpeedMultiplier; }
    public float getMaxAttackSpeedMultiplier() { return maxAttackSpeedMultiplier; }

    // ========================================================================
    // Public Getters - Status Effect
    // ========================================================================

    public boolean isShowPowerLevelStatusEffect() { return showPowerLevelStatusEffect; }
    public int getStatusEffectUpdateInterval() { return statusEffectUpdateInterval; }
    public boolean isShowDetailedPowerBreakdown() { return showDetailedPowerBreakdown; }

    // ========================================================================
    // Public Getters - Tiers
    // ========================================================================

    public int getNoviceTier() { return noviceTier; }
    public int getAdeptTier() { return adeptTier; }
    public int getExpertTier() { return expertTier; }
    public int getMasterTier() { return masterTier; }

    // ========================================================================
    // Public Getters - Performance
    // ========================================================================

    public boolean isCachePlayerPowerLevels() { return cachePlayerPowerLevels; }
    public int getPowerLevelCacheSize() { return powerLevelCacheSize; }
    public int getPowerLevelSyncInterval() { return powerLevelSyncInterval; }
    public int getForceSaveInterval() { return forceSaveInterval; }

    // ========================================================================
    // Public Getters - Integration
    // ========================================================================

    public boolean isIntegrateWithSoulboundGear() { return integrateWithSoulboundGear; }
    public boolean isApplyElementalDamage() { return applyElementalDamage; }
    public boolean isIntegrateWithUpgradeTree() { return integrateWithUpgradeTree; }

    // ========================================================================
    // Public Getters - Creature Spawn
    // ========================================================================

    public boolean isAssignCreaturePowerOnSpawn() { return assignCreaturePowerOnSpawn; }
    public int getMinCreaturePowerLevel() { return minCreaturePowerLevel; }
    public int getMaxCreaturePowerLevel() { return maxCreaturePowerLevel; }
    public boolean isShowCreaturePowerInExamine() { return showCreaturePowerInExamine; }

    // ========================================================================
    // Public Getters - Balancing
    // ========================================================================

    public boolean isPvpPowerScalingEnabled() { return pvpPowerScalingEnabled; }
    public float getPvpPowerScalingFactor() { return pvpPowerScalingFactor; }
    public float getBossPowerBonus() { return bossPowerBonus; }
    public float getRiftCreaturePowerBonus() { return riftCreaturePowerBonus; }

    // ========================================================================
    // Public Getters - Database
    // ========================================================================

    public String getDatabasePath() { return databasePath; }
    public boolean isEnableWAL() { return enableWAL; }
    public int getBusyTimeoutMs() { return busyTimeoutMs; }

    // ========================================================================
    // Public Getters - Debug
    // ========================================================================

    public boolean isDebugLogging() { return debugLogging; }
    public boolean isLogPowerCalculations() { return logPowerCalculations; }
    public boolean isLogCombatCalculations() { return logCombatCalculations; }
    public boolean isLogCreaturePowerAssignments() { return logCreaturePowerAssignments; }

    // ========================================================================
    // Public Getters - Healing System
    // ========================================================================

    public boolean isEnableOutOfCombatRegen() { return enableOutOfCombatRegen; }
    public int getOutOfCombatDelay() { return outOfCombatDelay; }
    public int getOutOfCombatRegenInterval() { return outOfCombatRegenInterval; }
    public int getOutOfCombatBaseHealing() { return outOfCombatBaseHealing; }
    public int getOutOfCombatPowerScaling() { return outOfCombatPowerScaling; }
    public float getVillageHealingMultiplier() { return villageHealingMultiplier; }
    public float getGuardTowerHealingMultiplier() { return guardTowerHealingMultiplier; }
    public float getWildernessHealingMultiplier() { return wildernessHealingMultiplier; }
    public float getEnemyTerritoryHealingMultiplier() { return enemyTerritoryHealingMultiplier; }
    public boolean isShowHealingMessages() { return showHealingMessages; }
    public boolean isShowHealingEffects() { return showHealingEffects; }

    // ========================================================================
    // Public Getters - Metal Loot System
    // ========================================================================

    public boolean isEnableMetalLoot() { return enableMetalLoot; }
    public float getMetalLootBaseQL() { return metalLootBaseQL; }
    public float getMetalLootPowerMultiplier() { return metalLootPowerMultiplier; }
    public float getMetalLootQLVariance() { return metalLootQLVariance; }
    public int getChampionBonusAmount() { return championBonusAmount; }
    public int getUniqueBonusAmount() { return uniqueBonusAmount; }

    // ========================================================================
    // Public Getters - Dynamic Creature Power System
    // ========================================================================

    public boolean isUseDynamicCreaturePower() { return useDynamicCreaturePower; }
    public int getCreatureBaseMin() { return creatureBaseMin; }
    public int getCreatureBaseMax() { return creatureBaseMax; }
    public double getAgeGrowthRate() { return ageGrowthRate; }
    public float getAgeGrowthMaxMultiplier() { return ageGrowthMaxMultiplier; }
    public int getGrowthTickIntervalSeconds() { return growthTickIntervalSeconds; }

    // ========================================================================
    // Public Getters - Settlement Suppression
    // ========================================================================

    public boolean isVillageSuppressionEnabled() { return villageSuppressionEnabled; }
    public float getVillageSuppression() { return villageSuppression; }
    public int getVillageSafeRadius() { return villageSafeRadius; }
    public boolean isIncludeDeedsAsVillages() { return includeDeedsAsVillages; }

    // ========================================================================
    // Public Getters - Visual Feedback
    // ========================================================================

    public boolean isModelScalingEnabled() { return modelScalingEnabled; }
    public float getModelScaleMin() { return modelScaleMin; }
    public float getModelScaleMax() { return modelScaleMax; }
    public boolean isNameSuffixEnabled() { return nameSuffixEnabled; }

    // ========================================================================
    // Public Getters - Performance
    // ========================================================================

    public boolean isDynamicCacheEnabled() { return dynamicCacheEnabled; }
    public long getCacheTTL() { return cacheTTL; }
    public int getMaxCreaturesPerTick() { return maxCreaturesPerTick; }

    // ========================================================================
    // Public Getters - Debug (Dynamic System)
    // ========================================================================

    public boolean isLogPowerAssignments() { return logPowerAssignments; }
    public boolean isLogGrowthTicks() { return logGrowthTicks; }

    /**
     * Calculate player power level from components.
     *
     * @param characterLevel Player character level
     * @param totalKills Total creature kills
     * @param achievementPower Bonus from achievements
     * @param questPower Bonus from quests
     * @return Calculated power level
     */
    public int calculatePlayerPowerLevel(int characterLevel, int totalKills,
                                         int achievementPower, int questPower) {
        // Base power from level
        float basePower = characterLevel * basePowerMultiplier;

        // Kill power with diminishing returns
        float diminishingFactor = 1.0f / (1.0f + ((float) totalKills / killDiminishingThreshold));
        float killPower = totalKills * killPowerRate * diminishingFactor;

        // Total power
        int totalPower = (int) (basePower + killPower) + achievementPower + questPower;

        // Cap at max
        return Math.min(totalPower, maxPlayerPowerLevel);
    }

    /**
     * Calculate creature power level from CR and type.
     *
     * @param creatureCR Creature combat rating
     * @param isChampion true if champion
     * @param isUnique true if unique
     * @param isTitan true if titan (custom)
     * @return Calculated power level
     */
    public int calculateCreaturePowerLevel(float creatureCR, boolean isChampion,
                                           boolean isUnique, boolean isTitan) {
        float basePower = creatureCR * crPowerMultiplier;

        float typeMultiplier = 1.0f;
        if (isTitan) {
            typeMultiplier = titanPowerMultiplier;
        } else if (isUnique) {
            typeMultiplier = uniquePowerMultiplier;
        } else if (isChampion) {
            typeMultiplier = championPowerMultiplier;
        }

        int powerLevel = (int) (basePower * typeMultiplier);

        // Clamp to valid range
        return Math.max(minCreaturePowerLevel, Math.min(powerLevel, maxCreaturePowerLevel));
    }

    /**
     * Get tier name for a power level.
     *
     * @param powerLevel Power level
     * @return Tier name (Novice, Adept, Expert, Master, Legendary)
     */
    public String getTierName(int powerLevel) {
        if (powerLevel < noviceTier) {
            return "Novice";
        } else if (powerLevel < adeptTier) {
            return "Novice";
        } else if (powerLevel < expertTier) {
            return "Adept";
        } else if (powerLevel < masterTier) {
            return "Expert";
        } else if (powerLevel < masterTier + 1) {
            return "Master";
        } else {
            return "Legendary";
        }
    }

    /**
     * Log all configuration values (for debugging).
     */
    private void logConfiguration() {
        logger.info("=== PowerScaling Configuration ===");
        logger.info("Base Power Multiplier: " + basePowerMultiplier);
        logger.info("Kill Power Rate: " + killPowerRate);
        logger.info("CR Power Multiplier: " + crPowerMultiplier);
        logger.info("Damage Per Power Level: " + damagePerPowerLevel);
        logger.info("Defense Per Power Level: " + defensePerPowerLevel);
        logger.info("HP Per Power Level: " + hpPerPowerLevel);
        logger.info(String.format("Tier Multipliers: Normal=%.2f Elite=%.2f Mythical=%.2f Legendary=%.2f Godlike=%.2f",
                normalTierMultiplier, eliteTierMultiplier, mythicalTierMultiplier,
                legendaryTierMultiplier, godlikeTierMultiplier));
        logger.info("Base Crit Chance: " + baseCritChance);
        logger.info("Show Power Status Effect: " + showPowerLevelStatusEffect);
        logger.info("Integrate with SoulboundGear: " + integrateWithSoulboundGear);
        logger.info("===================================");
    }
}
