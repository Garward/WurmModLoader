package com.garward.wurmmodloader.api.events.movement;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired whenever the server sends a movement update to the player's own client
 * (i.e., the movement packet for the controlled player).
 *
 * This represents the broadcast of the player's movement state,
 * not the internal position change (use CreaturePositionUpdatedEvent for that).
 */
public final class PlayerMovementBroadcastEvent extends Event {

    private final Creature player;
    private final float x;
    private final float y;
    private final float z;
    private final float rotation;
    private final boolean moving;

    public PlayerMovementBroadcastEvent(
            Creature player,
            float x,
            float y,
            float z,
            float rotation,
            boolean moving
    ) {
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.moving = moving;
    }

    public Creature getPlayer() {
        return player;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getRotation() {
        return rotation;
    }

    public boolean isMoving() {
        return moving;
    }
}
