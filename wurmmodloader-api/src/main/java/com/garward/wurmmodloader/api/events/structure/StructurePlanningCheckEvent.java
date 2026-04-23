package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code MethodsStructure.canPlanStructureAt(...)}.
 * Lets mods add placement constraints (kingdom, deed, terrain, permission)
 * before vanilla checks run.
 *
 * <p>Cancellation returns {@code false} from {@code canPlanStructureAt}. Set
 * {@link #setDenyReason(String)} to deliver an alert to the performer.</p>
 */
public class StructurePlanningCheckEvent extends CancellableEvent {

    private final Creature performer;
    private final Item tool;
    private final int tileX;
    private final int tileY;
    private final int tile;
    private String denyReason;

    public StructurePlanningCheckEvent(Creature performer, Item tool, int tileX, int tileY, int tile) {
        this.performer = performer;
        this.tool = tool;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tile = tile;
    }

    public Creature getPerformer() { return performer; }
    public Item getTool()           { return tool; }
    public int getTileX()           { return tileX; }
    public int getTileY()           { return tileY; }
    public int getTile()            { return tile; }

    public String getDenyReason()                 { return denyReason; }
    public void setDenyReason(String denyReason)  { this.denyReason = denyReason; }
}
