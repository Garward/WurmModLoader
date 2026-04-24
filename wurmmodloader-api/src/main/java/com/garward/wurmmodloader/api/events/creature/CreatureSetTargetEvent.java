package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired before {@code Creature.setTarget(long, boolean)} assigns a new target.
 * The master target-setter for all hostile decisions — single veto point for
 * pacifism, faction filters, friendly-fire prevention, etc.
 *
 * <p>Listeners may:
 * <ul>
 *   <li>Rewrite the target via {@link #setTargetId(long)} (e.g. redirect aggro).</li>
 *   <li>Cancel entirely via {@link #cancel()} — the whole setTarget call is
 *       skipped, including vanilla broadcasts and persistence.</li>
 * </ul>
 *
 * <p>Note: vanilla has two hard-coded pre-checks that fire <i>before</i> this
 * event (prey immunity and target-switch combat-rating gate). If you need to
 * override those, you'd also have to patch them separately.</p>
 */
public class CreatureSetTargetEvent extends Event {

    /** Sentinel used on the wire to signal "cancelled" back to the bytecode patch. */
    public static final long CANCEL_SENTINEL = Long.MIN_VALUE;

    private final Creature creature;
    private long targetId;
    private final boolean switchTarget;

    public CreatureSetTargetEvent(Creature creature, long targetId, boolean switchTarget) {
        super(true);
        this.creature = creature;
        this.targetId = targetId;
        this.switchTarget = switchTarget;
    }

    public Creature getCreature()    { return creature; }
    public long getTargetId()        { return targetId; }
    public void setTargetId(long id) { this.targetId = id; }
    public boolean isSwitchTarget()  { return switchTarget; }
}
