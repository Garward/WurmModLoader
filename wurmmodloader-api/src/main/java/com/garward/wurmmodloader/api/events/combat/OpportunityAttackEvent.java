package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired right before a successful opportunity attack is executed.
 *
 * <p>Listeners may cancel the event to skip the free attack or tweak the
 * combat/action counters that will be used when {@link
 * com.wurmonline.server.creatures.CombatHandler#attack(Creature, int, boolean,
 * float, com.wurmonline.server.behaviours.Action)} is invoked.</p>
 */
public class OpportunityAttackEvent extends CancellableEvent {

    private final Creature defender;
    private final Creature trespasser;
    private final double skillResult;
    private final double difficulty;
    private final byte opportunityCounter;
    private final int usedOpportunityAttacks;
    private int combatCounter;
    private float actionCounter;

    public OpportunityAttackEvent(Creature defender,
                                  Creature trespasser,
                                  double skillResult,
                                  double difficulty,
                                  byte opportunityCounter,
                                  int usedOpportunityAttacks,
                                  int combatCounter,
                                  float actionCounter) {
        this.defender = defender;
        this.trespasser = trespasser;
        this.skillResult = skillResult;
        this.difficulty = difficulty;
        this.opportunityCounter = opportunityCounter;
        this.usedOpportunityAttacks = usedOpportunityAttacks;
        this.combatCounter = combatCounter;
        this.actionCounter = actionCounter;
    }

    public Creature getDefender() {
        return defender;
    }

    public Creature getTrespasser() {
        return trespasser;
    }

    /**
     * @return The raw result of the mind speed skill check (positive indicates success).
     */
    public double getSkillResult() {
        return skillResult;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public byte getOpportunityCounter() {
        return opportunityCounter;
    }

    public int getUsedOpportunityAttacks() {
        return usedOpportunityAttacks;
    }

    public int getCombatCounter() {
        return combatCounter;
    }

    public void setCombatCounter(int combatCounter) {
        this.combatCounter = combatCounter;
    }

    public float getActionCounter() {
        return actionCounter;
    }

    public void setActionCounter(float actionCounter) {
        this.actionCounter = actionCounter;
    }
}
