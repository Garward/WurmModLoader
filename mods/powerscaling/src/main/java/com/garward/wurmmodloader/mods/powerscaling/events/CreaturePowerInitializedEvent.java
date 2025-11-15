package com.garward.wurmmodloader.mods.powerscaling.events;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired when a creature is first assigned a power level.
 *
 * <p>This event is fired after the power calculation is complete and the
 * creature's power capability has been initialized. It allows other mods
 * to react to power assignment (e.g., spawn visual effects, modify stats,
 * log debug information).</p>
 *
 * <p><strong>Cannot be cancelled.</strong> To modify power before assignment,
 * listen to other events in the calculation chain.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * @SubscribeEvent
 * public void onCreaturePowerInitialized(CreaturePowerInitializedEvent event) {
 *     if (event.getFinalPower() >= 2000) {
 *         // Spawn epic particles for legendary creatures
 *         SpawnParticleEffect(event.getCreature(), "legendary_aura");
 *     }
 * }
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CreaturePowerInitializedEvent extends Event {

    private final Creature creature;
    private final int basePower;
    private final int finalPower;
    private final String tierName;

    /**
     * Create a new creature power initialized event.
     *
     * @param creature The creature that was assigned power
     * @param basePower The randomized base power value
     * @param finalPower The final calculated power (after multipliers)
     * @param tierName The power tier name (e.g., "Normal", "Elite", "Legendary")
     */
    public CreaturePowerInitializedEvent(Creature creature, int basePower, int finalPower, String tierName) {
        this.creature = creature;
        this.basePower = basePower;
        this.finalPower = finalPower;
        this.tierName = tierName;
    }

    /**
     * Get the creature that was assigned power.
     *
     * @return The creature
     */
    public Creature getCreature() {
        return creature;
    }

    /**
     * Get the randomized base power value (before multipliers).
     *
     * @return The base power
     */
    public int getBasePower() {
        return basePower;
    }

    /**
     * Get the final calculated power level.
     *
     * <p>This includes all multipliers: base power × settlement × age × zone.</p>
     *
     * @return The final power level
     */
    public int getFinalPower() {
        return finalPower;
    }

    /**
     * Get the power tier name.
     *
     * @return The tier name (e.g., "Normal", "Elite", "Mythical", "Legendary", "Godlike")
     */
    public String getTierName() {
        return tierName;
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
