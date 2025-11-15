package com.garward.wurmmodloader.api.events.combat.weapon;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.combat.Weapon;
import com.wurmonline.server.items.Item;

/**
 * Fired when material-based weapon statistics (damage, speed, parry, armour damage, bash, etc.) are queried.
 */
public class WeaponStatQueryEvent extends Event {

    public enum StatType {
        DAMAGE,
        SPEED,
        PARRY,
        ARMOUR_DAMAGE,
        BASH,
        OTHER
    }

    private final Weapon weapon;
    private final Item itemTemplate;
    private final byte material;
    private final StatType statType;
    private final double baseValue;
    private double value;

    public WeaponStatQueryEvent(Weapon weapon, Item itemTemplate, byte material, StatType statType, double baseValue) {
        this.weapon = weapon;
        this.itemTemplate = itemTemplate;
        this.material = material;
        this.statType = statType;
        this.baseValue = baseValue;
        this.value = baseValue;
    }

    public Weapon getWeapon() {
        return weapon;
    }

    /**
     * @return Optional item reference used for this query, may be {@code null} for template-only data.
     */
    public Item getItemTemplate() {
        return itemTemplate;
    }

    public byte getMaterial() {
        return material;
    }

    public StatType getStatType() {
        return statType;
    }

    public double getBaseValue() {
        return baseValue;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
