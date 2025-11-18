package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when stamina is about to be consumed.
 * Allows modification of stamina cost before it's applied.
 *
 * Use cases:
 * - Endurance spell: Reduce stamina cost based on item/jewelry enchantment
 * - Stamina cost modifiers
 * - Fatigue reduction systems
 */
public class StaminaCostEvent extends Event {
    private final long creatureId;
    private final String creatureName;
    private final int originalCost;
    private int modifiedCost;
    private final int currentStamina;
    private final String actionType;

    public StaminaCostEvent(long creatureId, String creatureName,
                           int cost, int currentStamina, String actionType) {
        this.creatureId = creatureId;
        this.creatureName = creatureName;
        this.originalCost = cost;
        this.modifiedCost = cost;
        this.currentStamina = currentStamina;
        this.actionType = actionType;
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
     * Get the original stamina cost before modifications.
     */
    public int getOriginalCost() {
        return originalCost;
    }

    /**
     * Get the current modified stamina cost.
     */
    public int getModifiedCost() {
        return modifiedCost;
    }

    /**
     * Set the stamina cost.
     */
    public void setModifiedCost(int cost) {
        this.modifiedCost = Math.max(0, cost);
    }

    /**
     * Get the creature's current stamina.
     */
    public int getCurrentStamina() {
        return currentStamina;
    }

    /**
     * Get the type of action causing stamina drain.
     */
    public String getActionType() {
        return actionType;
    }

    /**
     * Multiply stamina cost by a factor (e.g., 0.7 for 30% reduction).
     */
    public void multiplyCost(double multiplier) {
        this.modifiedCost = (int) Math.max(0, this.modifiedCost * multiplier);
    }

    /**
     * Reduce stamina cost by a flat amount.
     */
    public void reduceCost(int reduction) {
        this.modifiedCost = Math.max(0, this.modifiedCost - reduction);
    }
}
