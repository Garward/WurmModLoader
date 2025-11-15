package com.garward.wurmmodloader.api.events.combat;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired when a creature attempts to execute a special move.
 * This allows mods to completely override special move handling.
 *
 * <p>Cancelling this event prevents vanilla special move processing.
 * The event handler should perform the special move logic and set the result.</p>
 *
 * <p><strong>Note:</strong> This is fired from CreatureBehaviour.handle_SPECMOVE().</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class SpecialMoveHandleEvent extends CancellableEvent {

    private final Creature performer;
    private final Creature target;
    private final short action;
    private final float counter;
    private boolean result = false;

    /**
     * Creates a new SpecialMoveHandleEvent.
     *
     * @param performer The creature performing the special move
     * @param target The target creature
     * @param action The special move action ID
     * @param counter Action counter/progress
     */
    public SpecialMoveHandleEvent(Creature performer, Creature target, short action, float counter) {
        this.performer = performer;
        this.target = target;
        this.action = action;
        this.counter = counter;
    }

    /**
     * Get the creature performing the special move.
     *
     * @return The performer
     */
    public Creature getPerformer() {
        return performer;
    }

    /**
     * Get the target creature.
     *
     * @return The target creature
     */
    public Creature getTarget() {
        return target;
    }

    /**
     * Get the special move action ID.
     *
     * @return The action ID
     */
    public short getAction() {
        return action;
    }

    /**
     * Get the action counter.
     *
     * @return The counter value
     */
    public float getCounter() {
        return counter;
    }

    /**
     * Get the result of the special move handling.
     * When event is cancelled, this determines the return value.
     *
     * @return True if special move was handled successfully
     */
    public boolean getResult() {
        return result;
    }

    /**
     * Set the result of the special move handling.
     *
     * @param result Success/failure status
     */
    public void setResult(boolean result) {
        this.result = result;
    }

    /**
     * Check if the performer is a player.
     *
     * @return True if performer is a player
     */
    public boolean isPlayer() {
        return performer.isPlayer();
    }
}
