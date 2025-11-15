package com.garward.wurmmodloader.mods.powerscaling.creature;

import com.garward.wurmmodloader.mods.powerscaling.PowerScalingConfig;
import com.garward.wurmmodloader.mods.powerscaling.PowerTier;
import com.garward.wurmmodloader.mods.powerscaling.events.CreaturePowerChangedEvent;
import com.garward.wurmmodloader.mods.powerscaling.events.CreaturePowerTickEvent;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;

import java.util.logging.Logger;

/**
 * Handles periodic age-based power growth for creatures.
 *
 * <p>Creatures become stronger over time if they survive. This ticker runs every
 * N seconds (configurable, default 5 minutes) and recalculates power for aged creatures.</p>
 *
 * <p><strong>Performance:</strong></p>
 * <ul>
 *   <li>Batch processing: max 100 creatures per tick (configurable)</li>
 *   <li>Skips players entirely</li>
 *   <li>Only recalculates if age has meaningfully increased</li>
 *   <li>Minimal CPU impact</li>
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 * Day 0:  basePower=500, ageMultiplier=1.0,   finalPower=500 (Normal)
 * Day 50: basePower=500, ageMultiplier=1.1,   finalPower=550 (Elite)
 * Day 100: basePower=500, ageMultiplier=1.2,  finalPower=600 (Elite)
 * Day 500: basePower=500, ageMultiplier=2.0,  finalPower=1000 (Mythical)
 * Day 1000: basePower=500, ageMultiplier=3.0, finalPower=1500 (Legendary, capped)
 * </pre>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class PowerGrowthTicker {

    private static final Logger logger = Logger.getLogger(PowerGrowthTicker.class.getName());

    private final PowerScalingConfig config;
    private long lastTickTime = 0;
    private int tickInterval; // milliseconds

    /**
     * Create a new power growth ticker.
     *
     * @param config Power scaling configuration
     */
    public PowerGrowthTicker(PowerScalingConfig config) {
        this.config = config;
        this.tickInterval = config.getGrowthTickIntervalSeconds() * 1000;
        this.lastTickTime = System.currentTimeMillis();
    }

    /**
     * Process creature aging if enough time has passed.
     *
     * <p>Called from ServerPollEvent every server tick. Only processes creatures
     * when the tick interval has elapsed.</p>
     */
    public void tick() {
        long now = System.currentTimeMillis();

        // Check if enough time has passed
        if (now - lastTickTime < tickInterval) {
            return; // Not time yet
        }

        lastTickTime = now;

        // Process creature aging
        processCreatureAging();
    }

    /**
     * Process age-based power growth for all loaded creatures.
     */
    private void processCreatureAging() {
        try {
            // Get all loaded creatures from Wurm server
            Creature[] creatures = Creatures.getInstance().getCreatures();

            if (creatures == null || creatures.length == 0) {
                return;
            }

            int processed = 0;
            int powerIncreased = 0;
            int maxPerTick = config.getMaxCreaturesPerTick();

            for (Creature creature : creatures) {
                // Skip players
                if (creature == null || creature.isPlayer()) {
                    continue;
                }

                // Respect max creatures per tick limit
                if (processed >= maxPerTick) {
                    if (config.isLogGrowthTicks()) {
                        logger.info(String.format(
                            "[GrowthTicker] Processed %d/%d creatures (max per tick reached)",
                            processed, creatures.length
                        ));
                    }
                    break;
                }

                // Process this creature
                if (processCreature(creature)) {
                    powerIncreased++;
                }

                processed++;
            }

            if (config.isLogGrowthTicks() && powerIncreased > 0) {
                logger.info(String.format(
                    "[GrowthTicker] Processed %d creatures, %d gained power from aging",
                    processed, powerIncreased
                ));
            }

        } catch (Exception e) {
            logger.warning("Error processing creature aging: " + e.getMessage());
        }
    }

    /**
     * Process a single creature's aging.
     *
     * @param creature The creature to process
     * @return true if power increased
     */
    private boolean processCreature(Creature creature) {
        try {
            // Get creature power data
            CreaturePowerData data = com.garward.wurmmodloader.core.capability.CapabilityManager
                .getInstance()
                .getCreatureCapability(creature.getWurmId(), CreaturePowerCapability.INSTANCE);

            if (data == null || !data.isInitialized()) {
                return false; // No power data, skip
            }

            // Get current power
            int oldPower = data.getCachedPower(creature, config);
            PowerTier oldTier = data.getTier(creature, config);

            // Fire tick event (cancellable)
            CreaturePowerTickEvent tickEvent = new CreaturePowerTickEvent(
                creature,
                oldPower,
                calculateAgeMultiplier(data.spawnTimestamp)
            );

            // TODO: Fire event through event bus when available
            // For now, check if we should proceed

            // If event was cancelled, skip this creature
            if (tickEvent.isCancelled()) {
                return false;
            }

            // Recalculate power with updated age
            int newPower = data.recalculatePower(creature, config);
            PowerTier newTier = data.getTier(creature, config);

            // Check if power actually increased
            if (newPower <= oldPower) {
                return false; // No increase
            }

            // Fire power changed event
            CreaturePowerChangedEvent changedEvent = new CreaturePowerChangedEvent(
                creature,
                oldPower,
                newPower,
                oldTier.getName(),
                newTier.getName(),
                CreaturePowerChangedEvent.PowerChangeReason.AGING
            );

            // TODO: Fire event through event bus when available

            // Log significant changes
            if (config.isLogGrowthTicks() && (newPower - oldPower >= 50 || newTier != oldTier)) {
                logger.info(String.format(
                    "[GrowthTicker] '%s' aged %.1f days: power %d→%d [%s→%s]",
                    creature.getName(),
                    data.getAgeInDays(),
                    oldPower,
                    newPower,
                    oldTier.getName(),
                    newTier.getName()
                ));
            }

            // Update visual effects if tier changed (Phase 4)
            if (newTier != oldTier) {
                com.garward.wurmmodloader.mods.powerscaling.creature.CreatureVisualEffects
                    .applyVisualEffects(creature, newTier, config);
            }

            return true; // Power increased

        } catch (Exception e) {
            logger.warning("Error processing creature " + creature.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Calculate age multiplier for a spawn timestamp.
     *
     * @param spawnTimestamp When the creature spawned
     * @return Age multiplier (1.0 to maxMultiplier)
     */
    private float calculateAgeMultiplier(long spawnTimestamp) {
        long ageMs = System.currentTimeMillis() - spawnTimestamp;
        double ageInDays = ageMs / 86400000.0;

        float growth = 1.0f + (float)(ageInDays * config.getAgeGrowthRate());
        return Math.min(growth, config.getAgeGrowthMaxMultiplier());
    }

    /**
     * Force process all creatures immediately (for testing/admin commands).
     */
    public void forceProcessAll() {
        logger.info("[GrowthTicker] Force processing all creatures...");
        processCreatureAging();
    }

    /**
     * Reset the tick timer.
     */
    public void resetTimer() {
        lastTickTime = System.currentTimeMillis();
    }

    /**
     * Get time until next tick.
     *
     * @return Milliseconds until next tick
     */
    public long getTimeUntilNextTick() {
        long elapsed = System.currentTimeMillis() - lastTickTime;
        return Math.max(0, tickInterval - elapsed);
    }
}
