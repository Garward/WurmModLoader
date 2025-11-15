package com.garward.wurmmodloader.api.events.combat.shield;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the start of {@code CombatHandler#checkShield} before block resolution.
 * Cancelling the event skips the vanilla shield check and returns {@link #getBlockPercent()}.
 */
public class ShieldCheckEvent extends CancellableEvent {

    private final Creature defender;
    private final Creature attacker;
    private final Item incomingWeapon;
    private final Item shield;
    private float blockPercent = Float.NaN;
    private boolean refundShieldUse;

    public ShieldCheckEvent(Creature defender, Creature attacker, Item incomingWeapon, Item shield) {
        this.defender = defender;
        this.attacker = attacker;
        this.incomingWeapon = incomingWeapon;
        this.shield = shield;
    }

    public Creature getDefender() {
        return defender;
    }

    public Creature getAttacker() {
        return attacker;
    }

    public Item getIncomingWeapon() {
        return incomingWeapon;
    }

    public Item getShield() {
        return shield;
    }

    /**
     * @return {@code true} if the handler requested the combat handler to refund the shield-use counter.
     */
    public boolean shouldRefundShieldUse() {
        return refundShieldUse;
    }

    public void setRefundShieldUse(boolean refundShieldUse) {
        this.refundShieldUse = refundShieldUse;
    }

    /**
     * @return The override block percent, or {@link Float#NaN} if vanilla logic should decide.
     */
    public float getBlockPercent() {
        return blockPercent;
    }

    /**
     * Sets an explicit block percent to return if the event is cancelled.
     */
    public void setBlockPercent(float blockPercent) {
        this.blockPercent = blockPercent;
    }

    public boolean hasBlockPercentOverride() {
        return !Float.isNaN(blockPercent);
    }
}
