package com.garward.wurmmodloader.api.events.movement;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired at the tail of {@code GenericCreatureAI.pollMovement(Creature, long)}
 * — the AI subclass that drives target acquisition and idle wandering for the
 * vast majority of creatures. By the time this fires, vanilla has already
 * iterated {@code getLatestAttackers()}, sorted by distance + player
 * preference, and possibly set a target + path.
 *
 * <p>This is intentionally a post-hook on an <b>AI-subclass</b> method — it
 * does not fire for Fish, Tower Guard, or other AI impls that don't extend
 * GenericCreatureAI. Listeners that want universal coverage should also lean
 * on {@link CreatureMovementTickEvent}.</p>
 *
 * <p>Listeners may flip the result via {@link #setMoved(boolean)} (vanilla
 * returns {@code true} if it did something meaningful this tick).</p>
 */
public class CreaturePollMovementEvent extends Event {

    private final Creature creature;
    private final long delta;
    private boolean moved;

    public CreaturePollMovementEvent(Creature creature, long delta, boolean moved) {
        this.creature = creature;
        this.delta = delta;
        this.moved = moved;
    }

    public Creature getCreature()  { return creature; }
    public long getDelta()         { return delta; }
    public boolean didMove()       { return moved; }
    public void setMoved(boolean m){ this.moved = m; }
}
