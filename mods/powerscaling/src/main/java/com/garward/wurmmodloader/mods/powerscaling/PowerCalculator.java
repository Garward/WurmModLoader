package com.garward.wurmmodloader.mods.powerscaling;

import com.wurmonline.server.creatures.Creature;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Generic power calculator for both creatures AND players.
 *
 * <p>This class implements the dynamic power formula from the design document:
 * <pre>
 * finalPower = basePower × settlementMultiplier × ageMultiplier × zoneMultiplier
 * </pre>
 *
 * <p><strong>For Creatures:</strong>
 * <ul>
 *   <li>basePower = random(baseMin, baseMax)</li>
 *   <li>settlementMultiplier = settlement suppression (weaker near villages)</li>
 *   <li>ageMultiplier = age-based growth (stronger over time)</li>
 *   <li>zoneMultiplier = zone-based bonuses (future expansion)</li>
 * </ul>
 *
 * <p><strong>For Players:</strong>
 * <ul>
 *   <li>basePower = level-based (configurable)</li>
 *   <li>settlementMultiplier = 1.0 (no settlement suppression for players)</li>
 *   <li>ageMultiplier = account age or similar (future expansion)</li>
 *   <li>zoneMultiplier = zone-based bonuses (future expansion)</li>
 * </ul>
 *
 * <p>The system is designed to be generic and extensible. Visual features
 * (size scaling, name suffixes) are creature-specific and handled separately.</p>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class PowerCalculator {

    private static final Logger logger = Logger.getLogger(PowerCalculator.class.getName());

    /**
     * Calculate initial power for a newly spawned creature.
     *
     * <p>This assigns a randomized base power and applies initial multipliers.
     * Age multiplier starts at 1.0 (will increase over time).</p>
     *
     * @param creature The creature that just spawned
     * @param config Power scaling configuration
     * @return The calculated initial power level
     */
    public static int calculateInitialPower(Creature creature, PowerScalingConfig config) {
        // Random base power
        int basePower = ThreadLocalRandom.current()
            .nextInt(config.getCreatureBaseMin(), config.getCreatureBaseMax() + 1);

        // Calculate multipliers
        float settlementMultiplier = calculateSettlementMultiplier(
            creature.getTileX(), creature.getTileY(), config
        );

        float ageMultiplier = 1.0f; // Just spawned, no age bonus yet
        float zoneMultiplier = 1.0f; // Future expansion

        // Final power
        int finalPower = Math.round(
            basePower * settlementMultiplier * ageMultiplier * zoneMultiplier
        );

        // Ensure valid range
        finalPower = Math.max(1, finalPower);
        finalPower = Math.min(finalPower, config.getMaxCreaturePowerLevel());

        if (config.isLogPowerAssignments()) {
            logger.info(String.format(
                "[PowerCalculator] Assigned power to '%s': basePower=%d, settlement=%.2fx, final=%d [%s]",
                creature.getName(), basePower, settlementMultiplier, finalPower,
                PowerTier.fromPower(finalPower).getName()
            ));
        }

        return finalPower;
    }

    /**
     * Recalculate power for an existing creature.
     *
     * <p>Uses stored base power and recalculates multipliers based on
     * current position and age. Used by the growth ticker and when
     * creatures move between zones.</p>
     *
     * @param creature The creature to recalculate
     * @param basePower The original randomized base power (stored)
     * @param spawnTimestamp When the creature spawned (milliseconds)
     * @param config Power scaling configuration
     * @return The recalculated power level
     */
    public static int recalculatePower(Creature creature, int basePower,
                                       long spawnTimestamp, PowerScalingConfig config) {
        // Calculate multipliers
        float settlementMultiplier = calculateSettlementMultiplier(
            creature.getTileX(), creature.getTileY(), config
        );

        float ageMultiplier = calculateAgeMultiplier(spawnTimestamp, config);
        float zoneMultiplier = 1.0f; // Future expansion

        // Final power
        int finalPower = Math.round(
            basePower * settlementMultiplier * ageMultiplier * zoneMultiplier
        );

        // Ensure valid range
        finalPower = Math.max(1, finalPower);
        finalPower = Math.min(finalPower, config.getMaxCreaturePowerLevel());

        return finalPower;
    }

    /**
     * Calculate settlement suppression multiplier.
     *
     * <p>Creatures are weaker near villages/deeds. Formula:
     * <pre>
     * if (distance >= safeRadius) return 1.0 (full power)
     * else return 1.0 - (suppression × (1.0 - distance/safeRadius))
     * </pre>
     *
     * <p>Example: 25 tiles from village with safeRadius=100, suppression=0.8:
     * <pre>
     * multiplier = 1.0 - (0.8 × (1.0 - 25/100))
     *            = 1.0 - (0.8 × 0.75)
     *            = 1.0 - 0.6
     *            = 0.4 (60% weaker)
     * </pre>
     *
     * @param tileX Creature X position
     * @param tileY Creature Y position
     * @param config Power scaling configuration
     * @return Multiplier (0.0 to 1.0)
     */
    private static float calculateSettlementMultiplier(int tileX, int tileY,
                                                       PowerScalingConfig config) {
        if (!config.isVillageSuppressionEnabled()) {
            return 1.0f;
        }

        // Get distance to nearest settlement
        int distance = com.garward.wurmmodloader.mods.powerscaling.settlement.SettlementRegistry
            .getInstance()
            .getDistanceToNearest(tileX, tileY);

        int safeRadius = config.getVillageSafeRadius();
        float suppression = config.getVillageSuppression();

        if (distance >= safeRadius) {
            return 1.0f; // Outside safe radius, full power
        }

        // Linear falloff based on distance
        float distanceRatio = (float) distance / safeRadius;
        return 1.0f - (suppression * (1.0f - distanceRatio));
    }

    /**
     * Calculate age-based growth multiplier.
     *
     * <p>Creatures become stronger over time if they survive. Formula:
     * <pre>
     * ageInDays = (currentTime - spawnTimestamp) / 86400000
     * multiplier = min(maxMultiplier, 1.0 + (ageInDays × growthRate))
     * </pre>
     *
     * <p>Example: 100 days old with growthRate=0.002, maxMultiplier=3.0:
     * <pre>
     * multiplier = 1.0 + (100 × 0.002) = 1.2 (20% stronger)
     * </pre>
     *
     * @param spawnTimestamp When the creature spawned (milliseconds)
     * @param config Power scaling configuration
     * @return Multiplier (1.0 to maxMultiplier)
     */
    private static float calculateAgeMultiplier(long spawnTimestamp, PowerScalingConfig config) {
        long ageMs = System.currentTimeMillis() - spawnTimestamp;
        double ageInDays = ageMs / 86400000.0; // milliseconds to days

        float growth = 1.0f + (float)(ageInDays * config.getAgeGrowthRate());
        return Math.min(growth, config.getAgeGrowthMaxMultiplier());
    }

    /**
     * Calculate zone multiplier (future expansion).
     *
     * <p>This will allow different zones to have power bonuses:
     * <ul>
     *   <li>Rift zones: 2.0x</li>
     *   <li>Deep caves: 1.5x</li>
     *   <li>Starter zones: 0.5x</li>
     *   <li>Default: 1.0x</li>
     * </ul>
     *
     * @param tileX Position X
     * @param tileY Position Y
     * @param config Power scaling configuration
     * @return Multiplier (default 1.0)
     */
    @SuppressWarnings("unused")
    private static float calculateZoneMultiplier(int tileX, int tileY, PowerScalingConfig config) {
        // TODO: Implement ZoneRegistry.getMultiplier()
        // For now, always return 1.0 (no zone effects)
        return 1.0f;
    }

    /**
     * Calculate player power (future expansion).
     *
     * <p>Players can use the same formula with different base values:
     * <ul>
     *   <li>basePower = level-based or skill-based</li>
     *   <li>settlementMultiplier = 1.0 (no suppression)</li>
     *   <li>ageMultiplier = account age or play time</li>
     *   <li>zoneMultiplier = zone bonuses</li>
     * </ul>
     *
     * @param player The player
     * @param config Power scaling configuration
     * @return Calculated player power
     */
    public static int calculatePlayerPower(Creature player, PowerScalingConfig config) {
        if (!player.isPlayer()) {
            throw new IllegalArgumentException("Creature is not a player");
        }

        // TODO: Implement player power formula
        // For now, use existing player power system
        // This is a placeholder for future generic power system
        return 0;
    }
}
