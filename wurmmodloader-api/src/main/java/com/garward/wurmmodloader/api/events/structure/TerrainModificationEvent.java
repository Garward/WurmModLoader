package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code Terraforming.dig(...)}. Lets terrain/digging
 * mods (DigLikeMiningMod, MightyMattockMod, terraforming tweaks) veto or
 * observe dig actions before vanilla runs.
 *
 * <p>Cancellation returns {@code false} from {@code dig} (action aborts).</p>
 */
public class TerrainModificationEvent extends CancellableEvent {

    private final Creature performer;
    private final Item tool;
    private final int tileX;
    private final int tileY;
    private final int tile;
    private final float counter;
    private final boolean corner;

    public TerrainModificationEvent(Creature performer, Item tool, int tileX, int tileY,
                                    int tile, float counter, boolean corner) {
        this.performer = performer;
        this.tool = tool;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tile = tile;
        this.counter = counter;
        this.corner = corner;
    }

    public Creature getPerformer() { return performer; }
    public Item getTool()           { return tool; }
    public int getTileX()           { return tileX; }
    public int getTileY()           { return tileY; }
    public int getTile()            { return tile; }
    public float getCounter()       { return counter; }
    public boolean isCorner()       { return corner; }
}
