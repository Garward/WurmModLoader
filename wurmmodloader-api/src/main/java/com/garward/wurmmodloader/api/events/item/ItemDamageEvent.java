package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

/**
 * Fired when an item is about to take damage.
 * Allows modification of damage amount or prevention of damage entirely.
 *
 * Use cases:
 * - Harden spell: Reduce damage based on enchantment power
 * - Item protection mechanics
 * - Durability modifiers
 */
public class ItemDamageEvent extends CancellableEvent {
    private final long itemId;
    private final String itemName;
    private float originalDamage;
    private float modifiedDamage;
    private final float currentDamage;

    public ItemDamageEvent(long itemId, String itemName, float damage, float currentDamage) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.originalDamage = damage;
        this.modifiedDamage = damage;
        this.currentDamage = currentDamage;
    }

    /**
     * Get the item's unique ID.
     */
    public long getItemId() {
        return itemId;
    }

    /**
     * Get the item's name.
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Get the original damage amount before modifications.
     */
    public float getOriginalDamage() {
        return originalDamage;
    }

    /**
     * Get the current modified damage amount.
     */
    public float getModifiedDamage() {
        return modifiedDamage;
    }

    /**
     * Set the damage amount. Use this to reduce or increase damage.
     */
    public void setModifiedDamage(float damage) {
        this.modifiedDamage = Math.max(0, damage);
    }

    /**
     * Get the item's current damage value (before this damage is applied).
     */
    public float getCurrentDamage() {
        return currentDamage;
    }

    /**
     * Multiply damage by a factor (e.g., 0.5 for 50% reduction).
     */
    public void multiplyDamage(float multiplier) {
        this.modifiedDamage = Math.max(0, this.modifiedDamage * multiplier);
    }

    /**
     * Reduce damage by a flat amount.
     */
    public void reduceDamage(float reduction) {
        this.modifiedDamage = Math.max(0, this.modifiedDamage - reduction);
    }
}
