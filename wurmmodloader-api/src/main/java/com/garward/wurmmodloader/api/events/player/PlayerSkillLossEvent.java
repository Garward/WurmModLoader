package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired when a player is about to lose skills due to death.
 * This allows mods to prevent or modify skill loss on death.
 *
 * <p>Cancelling this event prevents all skill loss from death.</p>
 *
 * <p><strong>Note:</strong> This is fired from Creature.punishSkills() at the
 * beginning of death skill loss processing.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class PlayerSkillLossEvent extends CancellableEvent {

    private final Creature creature;
    private final boolean hasResurrectionStone;

    /**
     * Creates a new PlayerSkillLossEvent.
     *
     * @param creature The creature losing skills
     * @param hasResurrectionStone True if creature has a resurrection stone
     */
    public PlayerSkillLossEvent(Creature creature, boolean hasResurrectionStone) {
        this.creature = creature;
        this.hasResurrectionStone = hasResurrectionStone;
    }

    /**
     * Get the creature losing skills.
     *
     * @return The creature
     */
    public Creature getCreature() {
        return creature;
    }

    /**
     * Check if the creature has a resurrection stone.
     * If true, vanilla would not apply skill loss.
     *
     * @return True if resurrection stone is present
     */
    public boolean hasResurrectionStone() {
        return hasResurrectionStone;
    }

    /**
     * Check if the creature is a player.
     *
     * @return True if creature is a player
     */
    public boolean isPlayer() {
        return creature.isPlayer();
    }
}
