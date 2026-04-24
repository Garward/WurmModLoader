package com.garward.wurmmodloader.api.events.movement;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired before {@code CreatureAI.creatureMovementTick(Creature, boolean)} —
 * the sole place a creature's position actually updates per tick. This is the
 * convergence point for pathed movement, random walks, and every other
 * AI-driven step; velocity + Z reconciliation + `moved()` callbacks all flow
 * through the method body after this event.
 *
 * <p>Cancellable: calling {@link #cancel()} skips the entire tick for this
 * creature (no position change, no move broadcast). Useful for freezing
 * creatures, custom movement overrides, or stamina-gated pauses.</p>
 *
 * <p>Hot path — fires every tick per moving creature. Keep listeners cheap.</p>
 */
public class CreatureMovementTickEvent extends Event {

    private final Creature creature;
    private final boolean rotateFromBlocker;

    public CreatureMovementTickEvent(Creature creature, boolean rotateFromBlocker) {
        super(true);
        this.creature = creature;
        this.rotateFromBlocker = rotateFromBlocker;
    }

    public Creature getCreature()         { return creature; }
    public boolean isRotateFromBlocker()  { return rotateFromBlocker; }
}
