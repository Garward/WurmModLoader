package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired when the server is about to send special move information to a creature.
 * This is typically used to customize the special move UI sent to players.
 *
 * <p>Cancelling this event prevents the vanilla special move UI from being sent,
 * allowing mods to send custom special move data.</p>
 *
 * <p><strong>Note:</strong> This is fired from CombatHandler.sendSpecialMoves().</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class SpecialMoveSendEvent extends CancellableEvent {

    private final Creature creature;

    /**
     * Creates a new SpecialMoveSendEvent.
     *
     * @param creature The creature receiving special move information
     */
    public SpecialMoveSendEvent(Creature creature) {
        this.creature = creature;
    }

    /**
     * Get the creature receiving special move information.
     *
     * @return The creature
     */
    public Creature getCreature() {
        return creature;
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
