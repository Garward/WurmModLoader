package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when combat rating is calculated.
 * Allows modification of combat rating bonuses.
 *
 * Use cases:
 * - Prowess spell: Increase combat rating based on jewelry enchantment
 * - Combat rating modifiers from equipment
 * - Skill bonuses in combat
 */
public class CombatRatingEvent extends Event {
    private final long creatureId;
    private final String creatureName;
    private final float originalRating;
    private float modifiedRating;

    public CombatRatingEvent(long creatureId, String creatureName, float rating) {
        this.creatureId = creatureId;
        this.creatureName = creatureName;
        this.originalRating = rating;
        this.modifiedRating = rating;
    }

    /**
     * Get the creature's unique ID.
     */
    public long getCreatureId() {
        return creatureId;
    }

    /**
     * Get the creature's name.
     */
    public String getCreatureName() {
        return creatureName;
    }

    /**
     * Get the original combat rating before modifications.
     */
    public float getOriginalRating() {
        return originalRating;
    }

    /**
     * Get the current modified combat rating.
     */
    public float getModifiedRating() {
        return modifiedRating;
    }

    /**
     * Set the modified combat rating.
     */
    public void setModifiedRating(float rating) {
        this.modifiedRating = rating;
    }

    /**
     * Add to the combat rating.
     */
    public void addRating(float bonus) {
        this.modifiedRating += bonus;
    }

    /**
     * Multiply rating by a factor.
     */
    public void multiplyRating(float multiplier) {
        this.modifiedRating *= multiplier;
    }
}
