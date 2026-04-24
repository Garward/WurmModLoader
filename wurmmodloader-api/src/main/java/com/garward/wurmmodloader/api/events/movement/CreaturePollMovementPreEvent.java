package com.garward.wurmmodloader.api.events.movement;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired <b>before</b> {@code GenericCreatureAI.pollMovement(Creature, long)}
 * runs. Companion to {@link CreaturePollMovementEvent} (the post-hook) —
 * listeners that need hard priority over vanilla's target-acquisition + idle
 * wandering cancel this event to skip pollMovement entirely for the tick.
 *
 * <p>Typical use: patrol / scheduled-route mods that want to inject their own
 * path via {@code Creature.startPathingToTile(...)} and not let vanilla
 * overwrite it with random wander or reactive aggro.</p>
 *
 * <p>When cancelled, the patched method returns {@code true} — signaling to
 * the AI tick loop that movement was handled, so the caller won't fall through
 * to alternative movement logic.</p>
 */
public class CreaturePollMovementPreEvent extends Event {

    private final Creature creature;
    private final long delta;

    public CreaturePollMovementPreEvent(Creature creature, long delta) {
        super(true);
        this.creature = creature;
        this.delta = delta;
    }

    public Creature getCreature() { return creature; }
    public long getDelta()        { return delta; }
}
