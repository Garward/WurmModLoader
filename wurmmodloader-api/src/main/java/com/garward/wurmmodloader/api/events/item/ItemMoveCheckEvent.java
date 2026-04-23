package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@link Item#moveToItem(Creature, long, boolean)}. Lets
 * bulk-container, cargo, and capacity mods veto the move before vanilla runs.
 *
 * <p>Cancelling returns {@code false} from {@code moveToItem}. If
 * {@link #setDenyReason(String)} is set, the patch will send the player an
 * alert with that text.</p>
 */
public class ItemMoveCheckEvent extends CancellableEvent {

    private final Item item;
    private final Creature mover;
    private final long targetId;
    private final boolean lastMove;
    private String denyReason;

    public ItemMoveCheckEvent(Item item, Creature mover, long targetId, boolean lastMove) {
        this.item = item;
        this.mover = mover;
        this.targetId = targetId;
        this.lastMove = lastMove;
    }

    /** The item being moved. */
    public Item getItem() {
        return item;
    }

    /** The creature performing the move. */
    public Creature getMover() {
        return mover;
    }

    /** The destination container's wurm id. */
    public long getTargetId() {
        return targetId;
    }

    /** Whether this is the last item in a batch move. */
    public boolean isLastMove() {
        return lastMove;
    }

    public String getDenyReason() {
        return denyReason;
    }

    public void setDenyReason(String denyReason) {
        this.denyReason = denyReason;
    }
}
