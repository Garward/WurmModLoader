package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

public class CreaturePositionUpdatedEvent extends Event {

    private final Creature creature;
    private final float newX;
    private final float newY;
    private final float newZ;
    private final float newRot;
    private final long newBridgeId;

    public CreaturePositionUpdatedEvent(
            Creature creature,
            float newX,
            float newY,
            float newZ,
            float newRot,
            long newBridgeId
    ) {
        this.creature = creature;
        this.newX = newX;
        this.newY = newY;
        this.newZ = newZ;
        this.newRot = newRot;
        this.newBridgeId = newBridgeId;
    }

    public Creature getCreature() { return creature; }
    public float getX() { return newX; }
    public float getY() { return newY; }
    public float getZ() { return newZ; }
    public float getRot() { return newRot; }
    public long getBridgeId() { return newBridgeId; }
}
