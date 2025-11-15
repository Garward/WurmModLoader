package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;

/**
 * Fires when an Action is being created and the fatigue flag is being determined.
 *
 * <p>Allows mods to override whether an action causes fatigue. This is useful for
 * mods that want to allow certain actions to be performed while mounted or under
 * other conditions that normally cause fatigue.</p>
 *
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Allow harvesting while mounted</li>
 *   <li>Remove fatigue from certain skill actions</li>
 *   <li>Conditional fatigue based on player buffs/items</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class ActionFatigueEvent extends Event {

    private final Creature performer;
    private final long subject;
    private final long target;
    private final short action;
    private boolean fatigue;

    /**
     * Creates a new ActionFatigueEvent.
     *
     * @param performer The creature performing the action (cannot be null)
     * @param subject The subject of the action (item/creature ID)
     * @param target The target of the action (tile/item/creature ID)
     * @param action The action ID from {@link com.wurmonline.server.behaviours.Actions}
     * @param fatigue Whether the action will cause fatigue (default value)
     * @throws IllegalArgumentException if performer is null
     */
    public ActionFatigueEvent(Creature performer, long subject, long target, short action, boolean fatigue) {
        if (performer == null) {
            throw new IllegalArgumentException("performer cannot be null");
        }
        this.performer = performer;
        this.subject = subject;
        this.target = target;
        this.action = action;
        this.fatigue = fatigue;
    }

    /**
     * Gets the creature performing the action.
     *
     * @return The performer (never null)
     */
    public Creature getPerformer() {
        return performer;
    }

    /**
     * Gets the subject of the action.
     *
     * @return The subject ID
     */
    public long getSubject() {
        return subject;
    }

    /**
     * Gets the target of the action.
     *
     * @return The target ID
     */
    public long getTarget() {
        return target;
    }

    /**
     * Gets the action ID.
     *
     * @return The action ID from {@link com.wurmonline.server.behaviours.Actions}
     */
    public short getAction() {
        return action;
    }

    /**
     * Gets whether the action will cause fatigue.
     *
     * @return true if the action causes fatigue
     */
    public boolean isFatigue() {
        return fatigue;
    }

    /**
     * Sets whether the action will cause fatigue.
     *
     * <p>Mods can call this to override the default fatigue behavior.</p>
     *
     * @param fatigue true if the action should cause fatigue
     */
    public void setFatigue(boolean fatigue) {
        this.fatigue = fatigue;
    }

    /**
     * Checks if the performer is mounted on a vehicle.
     *
     * @return true if the performer is on a vehicle
     */
    public boolean isPerformerMounted() {
        return performer.getVehicle() != -10L;
    }
}
