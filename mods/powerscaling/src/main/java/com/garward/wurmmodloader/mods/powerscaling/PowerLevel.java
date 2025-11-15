package com.garward.wurmmodloader.mods.powerscaling;

/**
 * Represents a player's power level data.
 *
 * <p>Mutable data model for player power progression. Tracks base power,
 * kill power, achievements, and quest bonuses. Changes are persisted to
 * database via PowerScalingManager.</p>
 *
 * <p><strong>Thread Safety:</strong> NOT thread-safe. Callers must synchronize
 * access or use PowerScalingManager's locking.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class PowerLevel {

    // ========================================================================
    // Identity
    // ========================================================================

    /** Wurm ID of the player (Creature.wurmId) */
    private final long playerWurmId;

    // ========================================================================
    // Power Components
    // ========================================================================

    /** Base power from character level */
    private int basePower;

    /** Accumulated kill power (before diminishing returns) */
    private float killPowerAccumulated;

    /** Total creature kills */
    private int totalKills;

    /** Bonus power from achievements */
    private int achievementPower;

    /** Bonus power from quests */
    private int questPower;

    /** Power spent on upgrades (reduces available power) */
    private int spentPower;

    // ========================================================================
    // Cached Calculated Values
    // ========================================================================

    /** Cached total power level (recalculated when components change) */
    private transient int cachedPowerLevel;

    /** Flag indicating cached power level needs recalculation */
    private transient boolean powerLevelDirty = true;

    // ========================================================================
    // Metadata
    // ========================================================================

    /** Unix timestamp of last update */
    private long lastUpdated;

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Create a new power level for a player.
     *
     * @param playerWurmId Player wurm ID
     */
    public PowerLevel(long playerWurmId) {
        this.playerWurmId = playerWurmId;
        this.basePower = 0;
        this.killPowerAccumulated = 0.0f;
        this.totalKills = 0;
        this.achievementPower = 0;
        this.questPower = 0;
        this.spentPower = 0;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Constructor for loading from database (includes all fields).
     *
     * @param playerWurmId Player wurm ID
     * @param basePower Base power from level
     * @param killPowerAccumulated Accumulated kill power
     * @param totalKills Total kills
     * @param achievementPower Achievement bonus
     * @param questPower Quest bonus
     * @param spentPower Power spent on upgrades
     * @param lastUpdated Last update timestamp
     */
    public PowerLevel(long playerWurmId, int basePower, float killPowerAccumulated,
                     int totalKills, int achievementPower, int questPower, int spentPower,
                     long lastUpdated) {
        this.playerWurmId = playerWurmId;
        this.basePower = basePower;
        this.killPowerAccumulated = killPowerAccumulated;
        this.totalKills = totalKills;
        this.achievementPower = achievementPower;
        this.questPower = questPower;
        this.spentPower = spentPower;
        this.lastUpdated = lastUpdated;
    }

    // ========================================================================
    // Getters
    // ========================================================================

    public long getPlayerWurmId() { return playerWurmId; }
    public int getBasePower() { return basePower; }
    public float getKillPowerAccumulated() { return killPowerAccumulated; }
    public int getTotalKills() { return totalKills; }
    public int getAchievementPower() { return achievementPower; }
    public int getQuestPower() { return questPower; }
    public int getSpentPower() { return spentPower; }
    public long getLastUpdated() { return lastUpdated; }

    // ========================================================================
    // Setters
    // ========================================================================

    public void setBasePower(int basePower) {
        this.basePower = basePower;
        this.powerLevelDirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setKillPowerAccumulated(float killPowerAccumulated) {
        this.killPowerAccumulated = killPowerAccumulated;
        this.powerLevelDirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
        this.powerLevelDirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setAchievementPower(int achievementPower) {
        this.achievementPower = achievementPower;
        this.powerLevelDirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setQuestPower(int questPower) {
        this.questPower = questPower;
        this.powerLevelDirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setSpentPower(int spentPower) {
        this.spentPower = spentPower;
        this.powerLevelDirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Spend power (add to spent power total).
     *
     * @param amount Amount of power to spend
     * @return true if player has enough power, false otherwise
     */
    public boolean spendPower(int amount) {
        // Check if player has enough available power
        int currentPower = getPowerLevel();
        if (currentPower < amount) {
            return false;
        }

        // Add to spent power
        this.spentPower += amount;
        this.powerLevelDirty = true;
        this.lastUpdated = System.currentTimeMillis();
        return true;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // ========================================================================
    // Kill Tracking
    // ========================================================================

    /**
     * Add a creature kill and update power.
     * Applies diminishing returns automatically.
     *
     * @param characterLevel Player's current character level
     */
    public void addKill(int characterLevel) {
        totalKills++;

        // Update base power from level
        PowerScalingConfig config = PowerScalingConfig.getInstance();
        basePower = (int) (characterLevel * config.getBasePowerMultiplier());

        // Add kill power (diminishing returns applied in getPowerLevel())
        killPowerAccumulated += config.getKillPowerRate();

        powerLevelDirty = true;
        lastUpdated = System.currentTimeMillis();
    }

    /**
     * Update base power based on character level.
     *
     * @param characterLevel Player's current character level
     */
    public void updateBasePower(int characterLevel) {
        PowerScalingConfig config = PowerScalingConfig.getInstance();
        int newBasePower = (int) (characterLevel * config.getBasePowerMultiplier());

        if (newBasePower != basePower) {
            basePower = newBasePower;
            powerLevelDirty = true;
            lastUpdated = System.currentTimeMillis();
        }
    }

    // ========================================================================
    // Power Level Calculation
    // ========================================================================

    /**
     * Get the calculated total power level.
     * Power level is cached and only recalculated when dirty.
     *
     * @return Total power level
     */
    public int getPowerLevel() {
        if (powerLevelDirty || cachedPowerLevel == 0) {
            cachedPowerLevel = calculatePowerLevel();
            powerLevelDirty = false;
        }
        return cachedPowerLevel;
    }

    /**
     * Calculate power level from components.
     *
     * @return Calculated power level
     */
    private int calculatePowerLevel() {
        PowerScalingConfig config = PowerScalingConfig.getInstance();

        // Base power (already scaled by basePowerMultiplier)
        float totalPower = basePower;

        // Kill power with diminishing returns
        float diminishingFactor = 1.0f / (1.0f + ((float) totalKills / config.getKillDiminishingThreshold()));
        float killPower = killPowerAccumulated * diminishingFactor;
        totalPower += killPower;

        // Add bonuses
        totalPower += achievementPower;
        totalPower += questPower;

        // Subtract spent power (power invested in upgrades)
        totalPower -= spentPower;

        // Ensure power doesn't go below 0
        int powerLevel = Math.max(0, (int) totalPower);

        // Cap at max
        return Math.min(powerLevel, config.getMaxPlayerPowerLevel());
    }

    /**
     * Force recalculation of power level on next access.
     */
    public void invalidatePowerLevelCache() {
        powerLevelDirty = true;
    }

    // ========================================================================
    // Power Breakdown (for UI display)
    // ========================================================================

    /**
     * Get power level broken down by component.
     *
     * @return Map of component name → power contribution
     */
    public java.util.Map<String, Integer> getPowerBreakdown() {
        java.util.Map<String, Integer> breakdown = new java.util.LinkedHashMap<>();

        // Base power
        breakdown.put("Base Power (from level)", basePower);

        // Kill power
        PowerScalingConfig config = PowerScalingConfig.getInstance();
        float diminishingFactor = 1.0f / (1.0f + ((float) totalKills / config.getKillDiminishingThreshold()));
        int killPower = (int) (killPowerAccumulated * diminishingFactor);
        breakdown.put("Kill Power (" + totalKills + " kills)", killPower);

        // Achievement power
        if (achievementPower > 0) {
            breakdown.put("Achievement Power", achievementPower);
        }

        // Quest power
        if (questPower > 0) {
            breakdown.put("Quest Power", questPower);
        }

        // Spent power (shown as negative)
        if (spentPower > 0) {
            breakdown.put("Spent on Upgrades", -spentPower);
        }

        return breakdown;
    }

    // ========================================================================
    // Utility
    // ========================================================================

    @Override
    public String toString() {
        return String.format("PowerLevel{player=%d, power=%d, base=%d, kills=%d, achieve=%d, quest=%d, spent=%d}",
                playerWurmId, getPowerLevel(), basePower, totalKills, achievementPower, questPower, spentPower);
    }

    /**
     * Format power level for display.
     *
     * @return Human-readable power level string
     */
    public String toDisplayString() {
        PowerScalingConfig config = PowerScalingConfig.getInstance();
        int powerLevel = getPowerLevel();
        String tier = config.getTierName(powerLevel);

        StringBuilder sb = new StringBuilder();
        sb.append("Power Level: ").append(powerLevel).append(" (").append(tier).append(")\n\n");

        // Breakdown
        for (java.util.Map.Entry<String, Integer> entry : getPowerBreakdown().entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Combat stats
        sb.append("\nCombat Stats:\n");
        float damageMult = 1.0f + (powerLevel * config.getDamagePerPowerLevel());
        float defenseMult = 1.0f + (powerLevel * config.getDefensePerPowerLevel());
        float hpMult = 1.0f + (powerLevel * config.getHpPerPowerLevel());

        sb.append("  Damage: +").append(String.format("%.0f", (damageMult - 1.0f) * 100)).append("%\n");
        sb.append("  Defense: +").append(String.format("%.0f", (defenseMult - 1.0f) * 100)).append("%\n");
        sb.append("  HP: +").append(String.format("%.0f", (hpMult - 1.0f) * 100)).append("%\n");

        return sb.toString();
    }
}
