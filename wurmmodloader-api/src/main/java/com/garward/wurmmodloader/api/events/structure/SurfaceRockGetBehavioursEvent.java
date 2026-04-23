package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.List;

/**
 * Fired after either {@code TileRockBehaviour.getBehavioursFor} overload
 * returns. Listeners can add / remove / reorder menu entries for surface
 * rock tiles — the symmetric hook to {@link CaveTileGetBehavioursEvent} for
 * above-ground rock.
 *
 * <p>For the no-source overload, {@link #getSource()} is {@code null}.</p>
 */
public class SurfaceRockGetBehavioursEvent extends Event {

    private final Creature performer;
    private final Item source;         // nullable
    private final int tileX;
    private final int tileY;
    private final boolean onSurface;
    private final int tile;
    private final List<ActionEntry> entries;

    public SurfaceRockGetBehavioursEvent(Creature performer, Item source, int tileX, int tileY,
                                         boolean onSurface, int tile, List<ActionEntry> entries) {
        this.performer = performer;
        this.source = source;
        this.tileX = tileX;
        this.tileY = tileY;
        this.onSurface = onSurface;
        this.tile = tile;
        this.entries = entries;
    }

    public Creature getPerformer()        { return performer; }
    public Item getSource()               { return source; }
    public int getTileX()                 { return tileX; }
    public int getTileY()                 { return tileY; }
    public boolean isOnSurface()          { return onSurface; }
    public int getTile()                  { return tile; }
    public List<ActionEntry> getEntries() { return entries; }
}
