package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when an off-hand weapon is about to perform a dual-wield strike.
 * Cancelling this event prevents the extra attack.
 */
public class CombatDualWieldEvent extends CancellableEvent {

    private final Creature attacker;
    private final Creature defender;
    private Item offhandWeapon;
    private float timeDelta;

    public CombatDualWieldEvent(Creature attacker, Creature defender, Item offhandWeapon, float timeDelta) {
        this.attacker = attacker;
        this.defender = defender;
        this.offhandWeapon = offhandWeapon;
        this.timeDelta = timeDelta;
    }

    public Creature getAttacker() {
        return attacker;
    }

    public Creature getDefender() {
        return defender;
    }

    public Item getOffhandWeapon() {
        return offhandWeapon;
    }

    public void setOffhandWeapon(Item offhandWeapon) {
        this.offhandWeapon = offhandWeapon;
    }

    /**
     * @return Time since the last off-hand swing, in seconds.
     */
    public float getTimeDelta() {
        return timeDelta;
    }

    public void setTimeDelta(float timeDelta) {
        this.timeDelta = timeDelta;
    }
}
