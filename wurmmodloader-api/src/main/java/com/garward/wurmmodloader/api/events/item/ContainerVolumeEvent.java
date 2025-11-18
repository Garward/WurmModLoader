package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when container volume or dimensions are queried.
 * Allows modification of container capacity and size.
 *
 * Use cases:
 * - Expand spell: Increase container capacity based on enchantment
 * - Container size modifiers
 * - Dynamic storage systems
 */
public class ContainerVolumeEvent extends Event {
    private final long itemId;
    private final String itemName;
    private final int originalValue;
    private int modifiedValue;
    private final VolumeType type;

    public enum VolumeType {
        VOLUME,      // Total volume in container
        SIZE_X,      // Width
        SIZE_Y,      // Height
        SIZE_Z       // Depth
    }

    public ContainerVolumeEvent(long itemId, String itemName, int originalValue, VolumeType type) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.originalValue = originalValue;
        this.modifiedValue = originalValue;
        this.type = type;
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
     * Get the original value before modifications.
     */
    public int getOriginalValue() {
        return originalValue;
    }

    /**
     * Get the current modified value.
     */
    public int getModifiedValue() {
        return modifiedValue;
    }

    /**
     * Set the modified value directly.
     */
    public void setModifiedValue(int value) {
        this.modifiedValue = Math.max(1, value);
    }

    /**
     * Get the type of volume measurement.
     */
    public VolumeType getType() {
        return type;
    }

    /**
     * Multiply the value by a factor.
     */
    public void multiplyValue(double multiplier) {
        this.modifiedValue = (int) Math.max(1, Math.min(Integer.MAX_VALUE, this.modifiedValue * multiplier));
    }

    /**
     * Add to the value.
     */
    public void addValue(int addition) {
        long newValue = (long) this.modifiedValue + addition;
        this.modifiedValue = (int) Math.max(1, Math.min(Integer.MAX_VALUE, newValue));
    }
}
