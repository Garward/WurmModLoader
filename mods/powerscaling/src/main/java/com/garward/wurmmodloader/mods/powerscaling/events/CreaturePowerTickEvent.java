package com.garward.wurmmodloader.mods.powerscaling.events;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired during periodic power growth calculations (aging system).
 *
 * <p>This event fires when the power growth ticker runs (default: every 5 minutes)
 * for creatures that gain power over time. Mods can:</p>
 * <ul>
 *   <li>Cancel the event to prevent aging for specific creatures</li>
 *   <li>Modify the age growth multiplier to speed up/slow down growth</li>
 *   <li>React to growth ticks (e.g., spawn particles, log statistics)</li>
 * </ul>
 *
 * <p><strong>Can be cancelled.</strong> Cancelling prevents this creature from
 * aging during this tick.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onCreaturePowerTick(CreaturePowerTickEvent event) {
 *     // Prevent aging in sanctuary zones
 *     if (isInSanctuaryZone(event.getCreature())) {
 *         event.cancel();
 *     }
 *
 *     // Double aging speed for creatures in rift zones
 *     if (isInRiftZone(event.getCreature())) {
 *         event.setAgeGrowthMultiplier(event.getAgeGrowthMultiplier() * 2.0);
 *     }
 * }
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CreaturePowerTickEvent extends CancellableEvent {

    private final Creature creature;
    private final int currentPower;
    private double ageGrowthMultiplier;

    /**
     * Create a new creature power tick event.
     *
     * @param creature The creature being processed
     * @param currentPower The creature's current power level
     * @param ageGrowthMultiplier The current age growth multiplier (modifiable)
     */
    public CreaturePowerTickEvent(Creature creature, int currentPower, double ageGrowthMultiplier) {
        this.creature = creature;
        this.currentPower = currentPower;
        this.ageGrowthMultiplier = ageGrowthMultiplier;
    }

    /**
     * Get the creature being processed.
     *
     * @return The creature
     */
    public Creature getCreature() {
        return creature;
    }

    /**
     * Get the creature's current power level (before growth).
     *
     * @return The current power
     */
    public int getCurrentPower() {
        return currentPower;
    }

    /**
     * Get the age growth multiplier.
     *
     * <p>This is the multiplier applied to the creature's base power based on
     * how long it has survived. Default formula: 1.0 + (ageInDays × growthRate)</p>
     *
     * @return The age growth multiplier
     */
    public double getAgeGrowthMultiplier() {
        return ageGrowthMultiplier;
    }

    /**
     * Set the age growth multiplier.
     *
     * <p>Modify this to speed up or slow down aging for this creature during
     * this tick. Values:
     * <ul>
     *   <li>0.0 = No aging</li>
     *   <li>1.0 = Normal aging</li>
     *   <li>2.0 = Double aging speed</li>
     * </ul>
     *
     * @param ageGrowthMultiplier The new age growth multiplier
     */
    public void setAgeGrowthMultiplier(double ageGrowthMultiplier) {
        this.ageGrowthMultiplier = ageGrowthMultiplier;
    }

    /**
     * Check if this is a player.
     *
     * @return true if the creature is a player
     */
    public boolean isPlayer() {
        return creature.isPlayer();
    }
}
