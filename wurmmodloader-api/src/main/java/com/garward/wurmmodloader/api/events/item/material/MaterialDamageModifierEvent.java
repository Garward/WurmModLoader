package com.garward.wurmmodloader.api.events.item.material;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.items.Item;

/**
 * Fired when material-based item damage reduction is queried.
 * Allows mods to override the final damage modifier.
 */
public class MaterialDamageModifierEvent extends Event {

    private final Item item;
    private final byte material;
    private final double baseModifier;
    private double modifier;

    public MaterialDamageModifierEvent(Item item, byte material, double baseModifier) {
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
