package com.garward.wurmmodloader.api.events.movement;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.VolaTile;

/**
 * Fired at the tail of {@code Zones.calculatePosZ(...)}. This method is the
 * central reconciliation point between an entity's desired Z and the world —
 * terrain height, water surface, bridges, floors, etc. — so it's the natural
 * place to force Z overrides for flight/hover/swim mods.
 *
 * <p>The vanilla signature is:
 * <pre>
 *   public static float calculatePosZ(
 *       float posX, float posY,
 *       VolaTile tile,
 *       boolean isOnSurface, boolean floating,
 *       float currentPosZ,
 *       &#64;Nullable Creature creature,
 *       long bridgeId)
 * </pre>
 *
 * <p>{@link #getCreature()} may be null — callers pass null when calculating Z
 * for non-creature entities (e.g. items, vehicles in some paths). Listeners
 * that require a creature must null-check. Not cancellable.</p>
 */
public class PosZCalculationEvent extends Event {

    private final float posX;
    private final float posY;
    private final VolaTile tile;
    private final boolean onSurface;
    private final boolean floating;
    private final float currentPosZ;
    private final Creature creature;
    private final long bridgeId;
    private float resolvedZ;

    public PosZCalculationEvent(float posX, float posY, VolaTile tile,
                                boolean onSurface, boolean floating,
                                float currentPosZ, Creature creature,
                                long bridgeId, float resolvedZ) {
        this.posX = posX;
        this.posY = posY;
        this.tile = tile;
        this.onSurface = onSurface;
        this.floating = floating;
        this.currentPosZ = currentPosZ;
        this.creature = creature;
        this.bridgeId = bridgeId;
        this.resolvedZ = resolvedZ;
    }

    public float getPosX()          { return posX; }
    public float getPosY()          { return posY; }
    public VolaTile getTile()       { return tile; }
    public boolean isOnSurface()    { return onSurface; }
    public boolean isFloating()     { return floating; }
    public float getCurrentPosZ()   { return currentPosZ; }
    public Creature getCreature()   { return creature; }
    public long getBridgeId()       { return bridgeId; }
    public float getResolvedZ()     { return resolvedZ; }
    public void setResolvedZ(float z) { this.resolvedZ = z; }
}
