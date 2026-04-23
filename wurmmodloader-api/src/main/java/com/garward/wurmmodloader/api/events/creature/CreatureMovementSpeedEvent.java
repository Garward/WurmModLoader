package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired at the tail of {@code MovementScheme.getSpeedModifier()}. Lets
 * movement/buff/debuff mods scale the final creature movement speed without
 * patching the vanilla modifier list.
 *
 * <p>Listeners read {@link #getSpeed()} and call {@link #setSpeed(float)} to
 * override. The returned value replaces the vanilla result. Not cancellable.</p>
 */
public class CreatureMovementSpeedEvent extends Event {

    private final Creature creature;
    private float speed;

    public CreatureMovementSpeedEvent(Creature creature, float speed) {
        this.creature = creature;
        this.speed = speed;
    }

    public Creature getCreature() { return creature; }
    public float getSpeed()       { return speed; }
    public void setSpeed(float s) { this.speed = s; }
}
