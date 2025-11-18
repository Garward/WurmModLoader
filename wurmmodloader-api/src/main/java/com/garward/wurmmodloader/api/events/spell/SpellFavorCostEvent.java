package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when favor is about to be consumed for a spell.
 * Allows modification of favor cost before it's deducted.
 *
 * Use cases:
 * - Acuity spell: Reduce favor cost based on jewelry enchantment
 * - Favor cost modifiers
 * - Spell efficiency bonuses
 */
public class SpellFavorCostEvent extends Event {
    private final long casterId;
    private final String casterName;
    private final int spellId;
    private final String spellName;
    private final int originalCost;
    private int modifiedCost;
    private final float currentFavor;

    public SpellFavorCostEvent(long casterId, String casterName,
                              int spellId, String spellName,
                              int cost, float currentFavor) {
        this.casterId = casterId;
        this.casterName = casterName;
        this.spellId = spellId;
        this.spellName = spellName;
        this.originalCost = cost;
        this.modifiedCost = cost;
        this.currentFavor = currentFavor;
    }

    /**
     * Get the caster's unique ID.
     */
    public long getCasterId() {
        return casterId;
    }

    /**
     * Get the caster's name.
     */
    public String getCasterName() {
        return casterName;
    }

    /**
     * Get the spell ID being cast.
     */
    public int getSpellId() {
        return spellId;
    }

    /**
     * Get the spell name.
     */
    public String getSpellName() {
        return spellName;
    }

    /**
     * Get the original favor cost before modifications.
     */
    public int getOriginalCost() {
        return originalCost;
    }

    /**
     * Get the current modified favor cost.
     */
    public int getModifiedCost() {
        return modifiedCost;
    }

    /**
     * Set the favor cost.
     */
    public void setModifiedCost(int cost) {
        this.modifiedCost = Math.max(0, cost);
    }

    /**
     * Get the caster's current favor.
     */
    public float getCurrentFavor() {
        return currentFavor;
    }

    /**
     * Multiply favor cost by a factor (e.g., 0.8 for 20% reduction).
     */
    public void multiplyCost(double multiplier) {
        this.modifiedCost = (int) Math.max(0, this.modifiedCost * multiplier);
    }

    /**
     * Reduce favor cost by a flat amount.
     */
    public void reduceCost(int reduction) {
        this.modifiedCost = Math.max(0, this.modifiedCost - reduction);
    }
}
