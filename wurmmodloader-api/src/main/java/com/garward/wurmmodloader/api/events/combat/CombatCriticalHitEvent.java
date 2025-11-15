package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.AttackAction;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when the server is about to roll for a critical hit.
 *
 * <p>Listeners can modify the raw critical chance before the vanilla random
 * roll occurs. Returning a value greater than {@code 1.0f} guarantees a crit;
 * returning {@code 0.0f} ensures no crit.</p>
 */
public class CombatCriticalHitEvent extends Event {

    private final Creature attacker;
    private final Creature defender;
    private final Item weapon;
    private final AttackAction attackAction;
    private final boolean usingNewCombatSystem;
    private float critChance;

    public CombatCriticalHitEvent(Creature attacker,
                                  Creature defender,
                                  Item weapon,
                                  AttackAction attackAction,
                                  boolean usingNewCombatSystem,
                                  float critChance) {
        this.attacker = attacker;
        this.defender = defender;
        this.weapon = weapon;
        this.attackAction = attackAction;
        this.usingNewCombatSystem = usingNewCombatSystem;
        this.critChance = critChance;
    }

    public Creature getAttacker() {
        return attacker;
    }

    public Creature getDefender() {
        return defender;
    }

    public Item getWeapon() {
        return weapon;
    }

    /**
     * @return The {@link AttackAction} used in the new combat flow, or {@code null}
     * when the legacy attack loop is running.
     */
    public AttackAction getAttackAction() {
        return attackAction;
    }

    public boolean isUsingNewCombatSystem() {
        return usingNewCombatSystem;
    }

    public float getCritChance() {
        return critChance;
    }

    public void setCritChance(float critChance) {
        this.critChance = critChance;
    }
}
