package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code Terraforming.pack(Creature, Item, int, int,
 * boolean, int, float, Action)}. Lets mods veto or observe packed-dirt
 * creation before vanilla runs.
 *
 * <p>Cancellation returns {@code false} from {@code pack} (action aborts).</p>
 */
public class TerrainPackEvent extends CancellableEvent {

    private final Creature performer;
    private final Item tool;
    private final int tileX;
    private final int tileY;
    private final boolean onSurface;
    private final int tile;
    private final float counter;
    private final Action action;

    public TerrainPackEvent(Creature performer, Item tool, int tileX, int tileY, boolean onSurface,
                            int tile, float counter, Action action) {
        this.performer = performer;
        this.tool = tool;
        this.tileX = tileX;
        this.tileY = tileY;
        this.onSurface = onSurface;
        this.tile = tile;
        this.counter = counter;
        this.action = action;
    }

    public Creature getPerformer() { return performer; }
    public Item getTool()          { return tool; }
    public int getTileX()          { return tileX; }
    public int getTileY()          { return tileY; }
    public boolean isOnSurface()   { return onSurface; }
    public int getTile()           { return tile; }
    public float getCounter()      { return counter; }
    public Action getAction()      { return action; }
}
