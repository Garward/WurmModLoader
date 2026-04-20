package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired before a creature becomes a pet via charming, dominating, or taming.
 *
 * <p>Cancellable. If cancelled, the precondition check fails and the binding
 * never happens — the spell fizzles and the taming attempt is aborted.</p>
 *
 * <p>Fires from:
 * <ul>
 *   <li>{@code CharmAnimal.precondition} — source {@link Source#CHARM}</li>
 *   <li>{@code Dominate.mayDominate} — source {@link Source#DOMINATE}</li>
 *   <li>Future whip-tame actions — source {@link Source#TAME}</li>
 * </ul>
 *
 * <p>Use this to gate taming behind skill-tree unlocks, enforce custom slot
 * caps, or filter targets by creature tier / power level.</p>
 *
 * @since 1.0.0
 */
public class TameAttemptEvent extends Event {

    /** Which game mechanism is binding the creature. */
    public enum Source { CHARM, DOMINATE, TAME }

    private final Creature performer;
    private final Creature target;
    private final Source source;

    public TameAttemptEvent(Creature performer, Creature target, Source source) {
        super(true);
        this.performer = performer;
        this.target = target;
        this.source = source;
    }

    public Creature getPerformer() { return performer; }
    public Creature getTarget() { return target; }
    public Source getSource() { return source; }
}
