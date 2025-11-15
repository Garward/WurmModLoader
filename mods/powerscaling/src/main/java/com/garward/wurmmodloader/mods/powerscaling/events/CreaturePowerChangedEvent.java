package com.garward.wurmmodloader.mods.powerscaling.events;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired whenever a creature's power level changes.
 *
 * <p>This event fires when power is recalculated due to:
 * <ul>
 *   <li>Age-based growth (creature survived longer)</li>
 *   <li>Zone changes (moved between different power zones)</li>
 *   <li>Settlement changes (village created/disbanded nearby)</li>
 *   <li>Admin commands (manual power override)</li>
 * </ul>
 *
 * <p><strong>Cannot be cancelled.</strong> The power change has already occurred.
 * Use this event to react to changes (update UI, spawn effects, adjust stats).</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onCreaturePowerChanged(CreaturePowerChangedEvent event) {
 *     // Log tier changes
 *     if (event.tierChanged()) {
 *         logger.info(String.format("%s evolved: %s -> %s",
 *             event.getCreature().getName(),
 *             event.getOldTier(),
 *             event.getNewTier()));
 *     }
 *
 *     // Spawn visual effect for significant power increases
 *     if (event.getNewPower() - event.getOldPower() >= 100) {
 *         SpawnPowerupEffect(event.getCreature());
 *     }
 * }
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CreaturePowerChangedEvent extends Event {

    /**
     * Reason for the power change.
     */
    public enum PowerChangeReason {
        /** Power increased due to aging/survival time */
        AGING,

        /** Power changed due to moving between zones */
        ZONE_CHANGE,

        /** Power changed due to settlement creation/disbanding */
        SETTLEMENT_CHANGE,

        /** Power manually set via admin command */
        ADMIN_COMMAND,

        /** Power changed for other reasons */
        OTHER
    }

    private final Creature creature;
    private final int oldPower;
    private final int newPower;
    private final String oldTier;
    private final String newTier;
    private final PowerChangeReason reason;

    /**
     * Create a new creature power changed event.
     *
     * @param creature The creature whose power changed
     * @param oldPower The previous power level
     * @param newPower The new power level
     * @param oldTier The previous tier name
     * @param newTier The new tier name
     * @param reason The reason for the power change
     */
    public CreaturePowerChangedEvent(Creature creature, int oldPower, int newPower,
                                     String oldTier, String newTier, PowerChangeReason reason) {
        this.creature = creature;
        this.oldPower = oldPower;
        this.newPower = newPower;
        this.oldTier = oldTier;
        this.newTier = newTier;
        this.reason = reason;
    }

    /**
     * Get the creature whose power changed.
     *
     * @return The creature
     */
    public Creature getCreature() {
        return creature;
    }

    /**
     * Get the previous power level.
     *
     * @return The old power level
     */
    public int getOldPower() {
        return oldPower;
    }

    /**
     * Get the new power level.
     *
     * @return The new power level
     */
    public int getNewPower() {
        return newPower;
    }

    /**
     * Get the power difference (positive for increase, negative for decrease).
     *
     * @return The power change amount
     */
    public int getPowerDifference() {
        return newPower - oldPower;
    }

    /**
     * Get the previous tier name.
     *
     * @return The old tier (e.g., "Normal", "Elite")
     */
    public String getOldTier() {
        return oldTier;
    }

    /**
     * Get the new tier name.
     *
     * @return The new tier (e.g., "Mythical", "Legendary")
     */
    public String getNewTier() {
        return newTier;
    }

    /**
     * Check if the creature crossed a tier threshold.
     *
     * @return true if the tier changed
     */
    public boolean tierChanged() {
        return !oldTier.equals(newTier);
    }

    /**
     * Get the reason for the power change.
     *
     * @return The change reason
     */
    public PowerChangeReason getReason() {
        return reason;
    }

    /**
     * Check if this is a player.
     *
     * @return true if the creature is a player
     */
    public boolean isPlayer() {
        return creature.isPlayer();
    }

    /**
     * Check if power increased.
     *
     * @return true if power went up
     */
    public boolean isPowerIncrease() {
        return newPower > oldPower;
    }

    /**
     * Check if power decreased.
     *
     * @return true if power went down
     */
    public boolean isPowerDecrease() {
        return newPower < oldPower;
    }
}
