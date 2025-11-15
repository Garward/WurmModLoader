package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired when a creature attempts to perform an attack in combat.
 * This is the main combat loop event that fires before vanilla attack logic.
 *
 * <p>This event allows complete override of vanilla combat mechanics.
 * Cancelling this event prevents vanilla combat processing but does not
 * automatically handle the attack - the event handler should perform the attack
 * logic and set the result.</p>
 *
 * <p><strong>Note:</strong> This is fired from CombatHandler.attack() at the
 * very beginning of the attack loop, before any vanilla combat calculations.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CombatAttackEvent extends CancellableEvent {

    private final Creature attacker;
    private final Creature defender;
    private final int combatCounter;
    private final boolean opportunity;
    private final float actionCounter;
    private final Action action;
    private boolean result = false;

    /**
     * Creates a new CombatAttackEvent.
     *
     * @param attacker The attacking creature
     * @param defender The defending creature
     * @param combatCounter Combat round counter
     * @param opportunity True if this is an opportunity attack
     * @param actionCounter Action timing counter
     * @param action The Action object for this attack
     */
    public CombatAttackEvent(Creature attacker, Creature defender, int combatCounter,
                             boolean opportunity, float actionCounter, Action action) {
        this.attacker = attacker;
        this.defender = defender;
        this.combatCounter = combatCounter;
        this.opportunity = opportunity;
        this.actionCounter = actionCounter;
        this.action = action;
    }

    /**
     * Get the attacking creature.
     *
     * @return The attacker
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
     * Get the combat counter.
     *
     * @return Combat round counter
     */
    public int getCombatCounter() {
        return combatCounter;
    }

    /**
     * Check if this is an opportunity attack.
     *
     * @return True if opportunity attack
     */
    public boolean isOpportunity() {
        return opportunity;
    }

    /**
     * Get the action counter for timing.
     *
     * @return Action counter value
     */
    public float getActionCounter() {
        return actionCounter;
    }

    /**
     * Get the Action object for this attack.
     *
     * @return The Action
     */
    public Action getAction() {
        return action;
    }

    /**
     * Get the result of the attack.
     * When event is cancelled, this determines the return value
     * of the attack method.
     *
     * @return True if attack should be considered successful
     */
    public boolean getResult() {
        return result;
    }

    /**
     * Set the result of the attack.
     *
     * @param result Attack success/failure
     */
    public void setResult(boolean result) {
        this.result = result;
    }
}
