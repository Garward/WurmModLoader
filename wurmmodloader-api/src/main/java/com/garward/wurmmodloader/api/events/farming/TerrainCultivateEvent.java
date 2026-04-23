package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code Terraforming.cultivate(Creature, Item, int,
 * int, boolean, int, float)}. Lets farming mods veto or observe cultivate
 * actions before vanilla runs.
 *
 * <p>Cancellation returns {@code false} from {@code cultivate} (action
 * aborts).</p>
 */
public class TerrainCultivateEvent extends CancellableEvent {

    private final Creature performer;
    private final Item tool;
    private final int tileX;
    private final int tileY;
    private final boolean onSurface;
    private final int tile;
    private final float counter;

    public TerrainCultivateEvent(Creature performer, Item tool, int tileX, int tileY,
                                 boolean onSurface, int tile, float counter) {
        this.performer = performer;
        this.tool = tool;
        this.tileX = tileX;
        this.tileY = tileY;
        this.onSurface = onSurface;
        this.tile = tile;
        this.counter = counter;
    }

    public Creature getPerformer() { return performer; }
    public Item getTool()          { return tool; }
    public int getTileX()          { return tileX; }
    public int getTileY()          { return tileY; }
    public boolean isOnSurface()   { return onSurface; }
    public int getTile()           { return tile; }
    public float getCounter()      { return counter; }
}
