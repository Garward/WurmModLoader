package com.garward.wurmmodloader.api.events.movement;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fired whenever the server sends a creature movement update to a client
 * via Communicator.sendMoveCreature(...).
 *
 * This represents the *broadcast* of movement, not the internal position change.
 *
 * Notes:
 * - watcher may be null if the underlying Communicator has no player bound.
 * - This event is 2D only; vertical/z movement is handled by the Z-variant patch.
 */
public final class MovementBroadcastEvent extends Event {

    private final Creature watcher;  // Player whose client is receiving the update (may be null)
    private final long creatureId;   // WurmID of the moved creature
    private final float x;
    private final float y;
    private final int rotation;
    private final boolean moving;

    public MovementBroadcastEvent(
            Creature watcher,
            long creatureId,
            float x,
            float y,
            int rotation,
            boolean moving
    ) {
        this.watcher = watcher;
        this.creatureId = creatureId;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.moving = moving;
    }

    public Creature getWatcher() {
        return watcher;
    }

    public long getCreatureId() {
        return creatureId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getRotation() {
        return rotation;
    }

    public boolean isMoving() {
        return moving;
    }
}
