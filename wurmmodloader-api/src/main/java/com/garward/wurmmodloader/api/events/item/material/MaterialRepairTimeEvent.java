package com.garward.wurmmodloader.api.events.item.material;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.items.Item;

/**
 * Fired when repair time modifiers are derived from an item's material.
 */
public class MaterialRepairTimeEvent extends Event {

    private final Item item;
    private final byte material;
    private final float baseTimeModifier;
    private float timeModifier;

    public MaterialRepairTimeEvent(Item item, byte material, float baseTimeModifier) {
        this.item = item;
        this.material = material;
        this.baseTimeModifier = baseTimeModifier;
        this.timeModifier = baseTimeModifier;
    }

    public Item getItem() {
        return item;
    }

    public byte getMaterial() {
        return material;
    }

    public float getBaseTimeModifier() {
        return baseTimeModifier;
    }

    public float getTimeModifier() {
        return timeModifier;
    }

    public void setTimeModifier(float timeModifier) {
        this.timeModifier = timeModifier;
    }
}
