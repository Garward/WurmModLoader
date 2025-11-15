package com.garward.wurmmodloader.api.events.item.material;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.items.Item;

/**
 * Fired when determining decay speed based on an item's material.
 */
public class MaterialDecayModifierEvent extends Event {

    private final Item item;
    private final byte material;
    private final double baseModifier;
    private double modifier;

    public MaterialDecayModifierEvent(Item item, byte material, double baseModifier) {
        this.item = item;
        this.material = material;
        this.baseModifier = baseModifier;
        this.modifier = baseModifier;
    }

    public Item getItem() {
        return item;
    }

    public byte getMaterial() {
        return material;
    }

    public double getBaseModifier() {
        return baseModifier;
    }

    public double getModifier() {
        return modifier;
    }

    public void setModifier(double modifier) {
        this.modifier = modifier;
    }
}
