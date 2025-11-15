package com.garward.wurmmodloader.mods.powerscaling.creature;

import com.garward.wurmmodloader.mods.powerscaling.PowerCalculator;
import com.garward.wurmmodloader.mods.powerscaling.PowerScalingConfig;
import com.garward.wurmmodloader.mods.powerscaling.PowerTier;
import com.wurmonline.server.creatures.Creature;

/**
 * Data class for creature power information.
 *
 * <p>Persistent fields are saved to database. Transient/cached fields
 * are recalculated on demand.</p>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class CreaturePowerData {
    // ========================================================================
    // Persistent Fields (saved to database)
    // ========================================================================

    /** Randomized base power assigned at spawn */
    public int basePower = 0;

    /** When the creature spawned (milliseconds since epoch) */
    public long spawnTimestamp = 0;

    /** Spawn location X (for settlement calculations) */
    public int spawnTileX = 0;

    /** Spawn location Y (for settlement calculations) */
    public int spawnTileY = 0;

    // ========================================================================
    // Transient/Cached Fields (not saved)
    // ========================================================================

    /** Cached final power (base × multipliers) */
    private transient int cachedFinalPower = 0;

    /** When the cache was last updated */
    private transient long cacheTimestamp = 0;

    // ========================================================================
    // Constructors
    // ========================================================================

    public CreaturePowerData() {
        // Default constructor for framework
    }

    public CreaturePowerData(int basePower, long spawnTimestamp, int spawnTileX, int spawnTileY) {
        this.basePower = basePower;
        this.spawnTimestamp = spawnTimestamp;
        this.spawnTileX = spawnTileX;
        this.spawnTileY = spawnTileY;
    }

    // ========================================================================
    // Public Methods
    // ========================================================================

    /**
     * Initialize power data for a newly spawned creature.
     *
     * @param creature The creature that just spawned
     * @param config Power scaling configuration
     */
    public void initialize(Creature creature, PowerScalingConfig config) {
        this.basePower = PowerCalculator.calculateInitialPower(creature, config);
        this.spawnTimestamp = System.currentTimeMillis();
        this.spawnTileX = creature.getTileX();
        this.spawnTileY = creature.getTileY();

        // Cache the initial power
        this.cachedFinalPower = basePower;
        this.cacheTimestamp = System.currentTimeMillis();
    }

    /**
     * Get cached power level (with TTL-based refresh).
     *
     * <p>If cache is stale (older than TTL), recalculates power using
     * current position and age. Otherwise returns cached value.</p>
     *
     * @param creature The creature to get power for
     * @param config Power scaling configuration
     * @return The creature's current power level
     */
    public int getCachedPower(Creature creature, PowerScalingConfig config) {
        long now = System.currentTimeMillis();
        long cacheTTL = config.getCacheTTL();

        // Check if cache is stale
        if (now - cacheTimestamp > cacheTTL) {
            // Recalculate power
            cachedFinalPower = PowerCalculator.recalculatePower(
                creature, basePower, spawnTimestamp, config
            );
            cacheTimestamp = now;
        }

        return cachedFinalPower;
    }

    /**
     * Force recalculate power (ignoring cache).
     *
     * <p>Used by growth ticker and admin commands.</p>
     *
     * @param creature The creature to recalculate
     * @param config Power scaling configuration
     * @return The recalculated power level
     */
    public int recalculatePower(Creature creature, PowerScalingConfig config) {
        cachedFinalPower = PowerCalculator.recalculatePower(
            creature, basePower, spawnTimestamp, config
        );
        cacheTimestamp = System.currentTimeMillis();
        return cachedFinalPower;
    }

    /**
     * Invalidate the cache (force refresh on next access).
     */
    public void invalidateCache() {
        cacheTimestamp = 0;
    }

    /**
     * Get the creature's power tier.
     *
     * @param creature The creature
     * @param config Power scaling configuration
     * @return The power tier
     */
    public PowerTier getTier(Creature creature, PowerScalingConfig config) {
        return PowerTier.fromPower(getCachedPower(creature, config));
    }

    /**
     * Get the age in days.
     *
     * @return Age in days (fractional)
     */
    public double getAgeInDays() {
        if (spawnTimestamp == 0) {
            return 0.0;
        }
        long ageMs = System.currentTimeMillis() - spawnTimestamp;
        return ageMs / 86400000.0; // milliseconds to days
    }

    /**
     * Check if this creature has been initialized.
     *
     * @return true if power has been assigned
     */
    public boolean isInitialized() {
        return basePower > 0 && spawnTimestamp > 0;
    }

    /**
     * Get debug string with all power data.
     *
     * @param creature The creature
     * @param config Power scaling configuration
     * @return Debug string
     */
    public String getDebugString(Creature creature, PowerScalingConfig config) {
        int currentPower = getCachedPower(creature, config);
        PowerTier tier = getTier(creature, config);
        double age = getAgeInDays();

        return String.format(
            "CreaturePower[base=%d, current=%d, tier=%s, age=%.1f days, pos=(%d,%d)]",
            basePower, currentPower, tier.getName(), age, spawnTileX, spawnTileY
        );
    }
}
