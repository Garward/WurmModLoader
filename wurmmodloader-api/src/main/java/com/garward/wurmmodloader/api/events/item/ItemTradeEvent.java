package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;


import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when an item is about to be traded between players.
 *
 * <p>This is a cancellable event. Mods can prevent the trade by calling
 * {@link #cancel()}.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ItemTradeEvent extends CancellableEvent {

    private final Item item;
    private final Creature giver;
    private final Creature receiver;

    public ItemTradeEvent(Item item, Creature giver, Creature receiver) {
        this.item = item;
        this.giver = giver;
        this.receiver = receiver;
    }

    /**
     * Get the item being traded.
     *
     * @return The item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Get the creature giving the item.
     *
     * @return The giver
     */
    public Creature getGiver() {
        return giver;
    }

    /**
     * Get the creature receiving the item.
     *
     * @return The receiver
     */
    public Creature getReceiver() {
        return receiver;
    }

    /**
     * Check if the giver is a player.
     *
     * @return true if giver is a player
     */
    public boolean isPlayerGiving() {
        return giver != null && giver.isPlayer();
    }

    /**
     * Check if the receiver is a player.
     *
     * @return true if receiver is a player
     */
    public boolean isPlayerReceiving() {
        return receiver != null && receiver.isPlayer();
    }

    /**
     * Check if this is a player-to-player trade.
     *
     * @return true if both participants are players
     */
    public boolean isPlayerTrade() {
        return isPlayerGiving() && isPlayerReceiving();
    }
}
