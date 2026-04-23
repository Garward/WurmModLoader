package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.List;

/**
 * Fired after either {@code CaveTileBehaviour.getBehavioursFor} overload
 * returns. Listeners receive the (mutable) list of context-menu entries and
 * may add, remove, or reorder before the server hands it to the client.
 *
 * <p>For the no-source overload, {@link #getSource()} is {@code null}.</p>
 *
 * <p>This is the event to use instead of patching cave-tile menus directly —
 * most "unlock X action on cave tile" mods (e.g. cavedwellingstweaks) can
 * subscribe here and append their own {@code ActionEntry}.</p>
 */
public class CaveTileGetBehavioursEvent extends Event {

    private final Creature performer;
    private final Item source;        // nullable
    private final int tileX;
    private final int tileY;
    private final boolean onSurface;
    private final int tile;
    private final int dir;
    private final List<ActionEntry> entries;

    public CaveTileGetBehavioursEvent(Creature performer, Item source, int tileX, int tileY,
                                      boolean onSurface, int tile, int dir,
                                      List<ActionEntry> entries) {
        this.performer = performer;
        this.source = source;
        this.tileX = tileX;
        this.tileY = tileY;
        this.onSurface = onSurface;
        this.tile = tile;
        this.dir = dir;
        this.entries = entries;
    }

    public Creature getPerformer()      { return performer; }
    public Item getSource()             { return source; }
    public int getTileX()               { return tileX; }
    public int getTileY()               { return tileY; }
    public boolean isOnSurface()        { return onSurface; }
    public int getTile()                { return tile; }
    public int getDir()                 { return dir; }
    public List<ActionEntry> getEntries() { return entries; }
}
