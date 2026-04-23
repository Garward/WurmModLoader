package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.math.TilePos;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code CaveTileBehaviour.mine(Action, Creature, Item,
 * int, int, short, float, int, TilePos)}. The main dispatch for cave mining
 * actions — picking, widening, deepening, reinforcing, creating exits, etc.
 *
 * <p>Cancellation returns {@code true} from {@code mine} (matches vanilla's
 * "done / abort" semantics used by the mining action pipeline). Listeners that
 * want the vanilla flow to proceed must leave the event un-cancelled.</p>
 */
public class CaveMineEvent extends CancellableEvent {

    private final Action action;
    private final Creature performer;
    private final Item source;
    private final int tileX;
    private final int tileY;
    private final short mineAction;
    private final float counter;
    private final int dir;
    private final TilePos digTilePos;

    public CaveMineEvent(Action action, Creature performer, Item source, int tileX, int tileY,
                         short mineAction, float counter, int dir, TilePos digTilePos) {
        this.action = action;
        this.performer = performer;
        this.source = source;
        this.tileX = tileX;
        this.tileY = tileY;
        this.mineAction = mineAction;
        this.counter = counter;
        this.dir = dir;
        this.digTilePos = digTilePos;
    }

    public Action getAction()      { return action; }
    public Creature getPerformer() { return performer; }
    public Item getSource()        { return source; }
    public int getTileX()          { return tileX; }
    public int getTileY()          { return tileY; }
    public short getMineAction()   { return mineAction; }
    public float getCounter()      { return counter; }
    public int getDir()            { return dir; }
    public TilePos getDigTilePos() { return digTilePos; }
}
