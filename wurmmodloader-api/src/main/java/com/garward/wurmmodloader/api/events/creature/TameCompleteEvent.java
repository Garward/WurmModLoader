package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired after a creature has been successfully bound as a pet.
 *
 * <p>At fire time the vanilla pet slot has already been set via
 * {@code performer.setPet(target.getWurmId())} and the target's dominator
 * field points at the performer. Handlers can safely read the pet relationship.</p>
 *
 * <p>Use this to append the new pet to a multi-pet capability roster,
 * apply first-bind buffs, or broadcast custom server messages.</p>
 *
 * <p>Fires from {@code CharmAnimal.doEffect} and {@code Dominate.dominate}
 * after successful binding.</p>
 *
 * @since 1.0.0
 * @see TameAttemptEvent
 */
public class TameCompleteEvent extends Event {

    private final Creature performer;
    private final Creature target;
    private final TameAttemptEvent.Source source;
    private final double power;

    public TameCompleteEvent(Creature performer, Creature target,
                             TameAttemptEvent.Source source, double power) {
        this.performer = performer;
        this.target = target;
        this.source = source;
        this.power = power;
    }

    public Creature getPerformer() { return performer; }
    public Creature getTarget() { return target; }
    public TameAttemptEvent.Source getSource() { return source; }

    /** Spell power / action success value that drove the binding. */
    public double getPower() { return power; }
}
