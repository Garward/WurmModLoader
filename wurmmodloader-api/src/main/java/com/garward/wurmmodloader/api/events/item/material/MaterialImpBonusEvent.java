package com.garward.wurmmodloader.api.events.item.material;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.items.Item;

/**
 * Fired when the material-specific improve bonus is calculated during item improvement.
 */
public class MaterialImpBonusEvent extends Event {

    private final Item item;
    private final byte material;
    private final double baseBonus;
    private double bonus;

    public MaterialImpBonusEvent(Item item, byte material, double baseBonus) {
        this.item = item;
        this.material = material;
        this.baseBonus = baseBonus;
        this.bonus = baseBonus;
    }

    public Item getItem() {
        return item;
    }

    public byte getMaterial() {
        return material;
    }

    public double getBaseBonus() {
        return baseBonus;
    }

    public double getBonus() {
        return bonus;
    }

    public void setBonus(double bonus) {
        this.bonus = bonus;
    }
}
