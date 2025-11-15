package com.garward.wurmmodloader.api.events.combat.shield;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when a shield is about to receive damage as part of block resolution.
 * Mods may adjust or nullify the damage applied to the shield.
 */
public class ShieldDamageEvent extends CancellableEvent {

    private final Creature defender;
    private final Creature attacker;
    private final Item shield;
    private double damage;

    public ShieldDamageEvent(Creature defender, Creature attacker, Item shield, double damage) {
        this.defender = defender;
        this.attacker = attacker;
        this.shield = shield;
        this.damage = damage;
    }

    public Creature getDefender() {
        return defender;
    }

    public Creature getAttacker() {
        return attacker;
    }

    public Item getShield() {
        return shield;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }
}
