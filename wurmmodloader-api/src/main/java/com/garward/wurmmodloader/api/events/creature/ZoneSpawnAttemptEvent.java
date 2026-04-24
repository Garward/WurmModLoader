package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.zones.Zone;

/**
 * Fired before {@code Zone.spawnCreature(int tx, int ty, boolean spawnKingdom)}
 * runs its validation + template-selection pipeline. This is the master pre-spawn
 * hook for natural land spawning — cancel it and no creature is placed at that
 * tile. Fires independently of the post-init {@link CreatureSpawnEvent}, which
 * only runs once a creature actually exists.
 *
 * <p>Does not fire for sea creatures (separate vanilla path) or for creatures
 * placed directly by GMs / bred offspring.</p>
 *
 * <p>Cancellable: calling {@link #cancel()} skips the whole spawn attempt.</p>
 */
public class ZoneSpawnAttemptEvent extends Event {

    private final Zone zone;
    private final int tileX;
    private final int tileY;
    private final boolean spawnKingdom;

    public ZoneSpawnAttemptEvent(Zone zone, int tileX, int tileY, boolean spawnKingdom) {
        super(true);
        this.zone = zone;
        this.tileX = tileX;
        this.tileY = tileY;
        this.spawnKingdom = spawnKingdom;
    }

    public Zone getZone()            { return zone; }
    public int getTileX()            { return tileX; }
    public int getTileY()            { return tileY; }
    public boolean isSpawnKingdom()  { return spawnKingdom; }
}
