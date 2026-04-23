package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired after one or more surface mesh corners at (tileX, tileY) have been
 * modified. Observation-only — consumers (livemap, minimap caches, etc.)
 * subscribe to drop cached tiles or schedule re-renders.
 *
 * <p>Mods that do bulk terraform ops should post one event per logical tile
 * edit rather than per mesh corner (adjacent tiles share corners, so 4
 * corners = 1 tile edit).</p>
 */
public class SurfaceTileChangedEvent extends Event {

    private final int tileX;
    private final int tileY;
    private final boolean onSurface;

    public SurfaceTileChangedEvent(int tileX, int tileY, boolean onSurface) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.onSurface = onSurface;
    }

    public int getTileX()       { return tileX; }
    public int getTileY()       { return tileY; }
    public boolean isOnSurface() { return onSurface; }
}
