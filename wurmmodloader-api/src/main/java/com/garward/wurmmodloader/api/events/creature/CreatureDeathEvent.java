package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;


import com.wurmonline.server.creatures.Creature;

/**
 * Fired when a creature dies.
 *
 * <p>This event is fired from Creature.die() after the creature has died.
 * Both player and NPC deaths fire this event.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CreatureDeathEvent extends Event {

    private final Creature victim;
    private final Creature killer; // May be null if no killer

    public CreatureDeathEvent(Creature victim, Creature killer) {
        this.victim = victim;
        this.killer = killer;
    }

    /**
     * Get the creature that died.
     *
     * @return The dead creature
     */
    public Creature getVictim() {
        return victim;
    }

    /**
     * Get the creature that killed the victim.
     *
     * @return The killer, or null if death was not caused by another creature
     */
    public Creature getKiller() {
        return killer;
    }

    /**
     * Check if the victim was a player.
     *
     * @return true if victim was a player
     */
    public boolean isPlayerDeath() {
        return victim != null && victim.isPlayer();
    }

    /**
     * Check if the killer was a player.
     *
     * @return true if killer was a player (false if no killer)
     */
    public boolean isPlayerKill() {
        return killer != null && killer.isPlayer();
    }
}
