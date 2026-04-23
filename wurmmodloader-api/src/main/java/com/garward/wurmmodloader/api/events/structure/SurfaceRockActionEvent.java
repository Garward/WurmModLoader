package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of either {@code TileRockBehaviour.action} overload — the
 * top-level dispatcher for surface-rock actions (prospecting, mining a
 * surface vein, etc.). Symmetric to {@link CaveTileActionEvent} but for
 * above-ground rock tiles.
 *
 * <p>For the 7-arg no-source overload, {@link #getSource()} is {@code null}
 * and {@link #getHeightOffset()} is {@code 0}.</p>
 *
 * <p>Cancellation returns {@code true} (action-loop done/abort semantics).</p>
 */
public class SurfaceRockActionEvent extends CancellableEvent {

    private final Action action;
    private final Creature performer;
    private final Item source;           // nullable
    private final int tileX;
    private final int tileY;
    private final boolean onSurface;
    private final int heightOffset;
    private final int tile;
    private final short actionShort;
    private final float counter;

    public SurfaceRockActionEvent(Action action, Creature performer, Item source,
                                  int tileX, int tileY, boolean onSurface, int heightOffset,
                                  int tile, short actionShort, float counter) {
        this.action = action;
        this.performer = performer;
        this.source = source;
        this.tileX = tileX;
        this.tileY = tileY;
        this.onSurface = onSurface;
        this.heightOffset = heightOffset;
        this.tile = tile;
        this.actionShort = actionShort;
        this.counter = counter;
    }

    public Action getAction()       { return action; }
    public Creature getPerformer()  { return performer; }
    public Item getSource()         { return source; }
    public int getTileX()           { return tileX; }
    public int getTileY()           { return tileY; }
    public boolean isOnSurface()    { return onSurface; }
    public int getHeightOffset()    { return heightOffset; }
    public int getTile()            { return tile; }
    public short getActionShort()   { return actionShort; }
    public float getCounter()       { return counter; }
}
