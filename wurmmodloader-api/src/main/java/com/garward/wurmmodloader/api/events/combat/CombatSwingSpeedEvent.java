package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired before swing speed is clamped during combat speed calculations.
 * Mods may scale the computed swing time before it is applied.
 */
public class CombatSwingSpeedEvent extends Event {

    private final Creature attacker;
    private final Item weapon;
    private final float baseSpeed;
    private float swingSpeed;

    public CombatSwingSpeedEvent(Creature attacker, Item weapon, float baseSpeed) {
        this.attacker = attacker;
        this.weapon = weapon;
        this.baseSpeed = baseSpeed;
        this.swingSpeed = baseSpeed;
    }

    public Creature getAttacker() {
        return attacker;
    }

    public Item getWeapon() {
        return weapon;
    }

    public float getBaseSpeed() {
        return baseSpeed;
    }

    public float getSwingSpeed() {
        return swingSpeed;
    }

    public void setSwingSpeed(float swingSpeed) {
        this.swingSpeed = swingSpeed;
    }
}
