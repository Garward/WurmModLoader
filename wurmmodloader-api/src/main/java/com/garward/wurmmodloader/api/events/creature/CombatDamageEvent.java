package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;


import com.wurmonline.server.creatures.Creature;

/**
 * Fired before damage is applied in combat.
 *
 * <p>This event is fired from CombatEngine.addWound() before damage calculations.
 * Mods can modify damage or cancel the event entirely.</p>
 *
 * <p><strong>Note:</strong> This is a cancellable event. Setting cancelled = true
 * will prevent the wound from being applied.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CombatDamageEvent extends CancellableEvent {

    private final Creature attacker;
    private final Creature defender;
    private double damage;
    private final byte woundType;
    private final int bodyPart;

    public CombatDamageEvent(Creature attacker, Creature defender, double damage, byte woundType, int bodyPart) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.woundType = woundType;
        this.bodyPart = bodyPart;
    }

    /**
     * Get the attacking creature.
     *
     * @return The attacker (may be null for environmental damage)
     */
    public Creature getAttacker() {
        return attacker;
    }

    /**
     * Get the defending creature.
     *
     * @return The defender
     */
    public Creature getDefender() {
        return defender;
    }

    /**
     * Get the current damage amount.
     *
     * @return Damage value
     */
    public double getDamage() {
        return damage;
    }

    /**
     * Set the damage amount.
     * Mods can use this to scale damage up or down.
     *
     * @param damage New damage value
     */
    public void setDamage(double damage) {
        this.damage = damage;
    }

    /**
     * Get the wound type.
     *
     * @return Wound type constant
     */
    public byte getWoundType() {
        return woundType;
    }

    /**
     * Get the body part being hit.
     *
     * @return Body part ID
     */
    public int getBodyPart() {
        return bodyPart;
    }

    /**
     * Check if attacker is a player.
     *
     * @return true if attacker is a player
     */
    public boolean isPlayerAttack() {
        return attacker != null && attacker.isPlayer();
    }

    /**
     * Check if defender is a player.
     *
     * @return true if defender is a player
     */
    public boolean isPlayerDefending() {
        return defender != null && defender.isPlayer();
    }
}
