package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code Flattening.flatten(Creature, Item, int, int, int,
 * float, Action)} — the public-ish 7-arg dispatcher used for flatten actions
 * (tile and border flattening both route through this variant).
 *
 * <p>Cancellation returns {@code false} from {@code flatten} (action aborts).</p>
 */
public class TerrainFlattenEvent extends CancellableEvent {

    private final Creature performer;
    private final Item tool;
    private final int tile;
    private final int tileX;
    private final int tileY;
    private final float counter;
    private final Action action;

    public TerrainFlattenEvent(Creature performer, Item tool, int tile, int tileX, int tileY,
                               float counter, Action action) {
        this.performer = performer;
        this.tool = tool;
        this.tile = tile;
        this.tileX = tileX;
        this.tileY = tileY;
        this.counter = counter;
        this.action = action;
    }

    public Creature getPerformer() { return performer; }
    public Item getTool()          { return tool; }
    public int getTile()           { return tile; }
    public int getTileX()          { return tileX; }
    public int getTileY()          { return tileY; }
    public float getCounter()      { return counter; }
    public Action getAction()      { return action; }
}
