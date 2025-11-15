package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;


import com.wurmonline.server.creatures.Creature;

/**
 * Fired when a creature spawns or is initialized.
 *
 * <p>This event is fired from Creature.setWurmId() during creature initialization.
 * Useful for assigning custom data to creatures on spawn.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class CreatureSpawnEvent extends Event {

    private final Creature creature;

    public CreatureSpawnEvent(Creature creature) {
        this.creature = creature;
    }

    /**
     * Get the creature that spawned.
     *
     * @return The creature
     */
    public Creature getCreature() {
        return creature;
    }

    /**
     * Check if the spawned creature is a player.
     *
     * @return true if creature is a player
     */
    public boolean isPlayer() {
        return creature != null && creature.isPlayer();
    }

    /**
     * Check if the creature is a champion.
     *
     * @return true if creature is a champion
     */
    public boolean isChampion() {
        return creature != null && creature.isChampion();
    }

    /**
     * Check if the creature is unique.
     *
     * @return true if creature is unique
     */
    public boolean isUnique() {
        return creature != null && creature.isUnique();
    }
}
