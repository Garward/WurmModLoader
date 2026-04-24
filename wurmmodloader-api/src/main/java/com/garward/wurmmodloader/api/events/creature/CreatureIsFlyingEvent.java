package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired at the tail of {@code Creature.isFlying()}. Vanilla's implementation is
 * a stub that always returns {@code false} — flight mods can flip this by
 * calling {@link #setFlying(boolean)}.
 *
 * <p>Beware: vanilla AI pathing is purely 2D (PathFinder ignores posZ), and
 * several callsites use {@code isFlying()} as a bypass for terrain-steepness
 * / bridge checks. Treat this event as the "this creature ignores ground
 * height" flag, not a general 3D-movement switch. Not cancellable.</p>
 */
public class CreatureIsFlyingEvent extends Event {

    private final Creature creature;
    private boolean flying;

    public CreatureIsFlyingEvent(Creature creature, boolean flying) {
        this.creature = creature;
        this.flying = flying;
    }

    public Creature getCreature()       { return creature; }
    public boolean isFlying()           { return flying; }
    public void setFlying(boolean fly)  { this.flying = fly; }
}
