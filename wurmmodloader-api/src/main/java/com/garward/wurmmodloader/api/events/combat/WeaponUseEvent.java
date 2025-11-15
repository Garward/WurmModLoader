package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when the internal swing timer for a weapon is updated on a creature.
 * Mods can adjust the stored timer value before it is written.
 */
public class WeaponUseEvent extends Event {

    private final Creature creature;
    private final Item weapon;
    private final float previousValue;
    private float newValue;

    public WeaponUseEvent(Creature creature, Item weapon, float previousValue, float newValue) {
        this.creature = creature;
        this.weapon = weapon;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    public Creature getCreature() {
        return creature;
    }

    public Item getWeapon() {
        return weapon;
    }

    public float getPreviousValue() {
        return previousValue;
    }

    public float getNewValue() {
        return newValue;
    }

    public void setNewValue(float newValue) {
        this.newValue = newValue;
    }
}
