package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;


import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when a creature is about to drop an item.
 *
 * <p>This is a cancellable event. Mods can prevent the drop by calling
 * {@link #cancel()}.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ItemDropEvent extends CancellableEvent {

    private final Item item;
    private final Creature dropper;
    private final boolean onGround;

    public ItemDropEvent(Item item, Creature dropper, boolean onGround) {
        this.item = item;
        this.dropper = dropper;
        this.onGround = onGround;
    }

    /**
     * Get the item being dropped.
     *
     * @return The item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Get the creature dropping the item.
     *
     * @return The dropper
     */
    public Creature getDropper() {
        return dropper;
    }

    /**
     * Check if the dropper is a player.
     *
     * @return true if dropper is a player
     */
    public boolean isPlayerDropping() {
        return dropper != null && dropper.isPlayer();
    }

    /**
     * Check if the item is being dropped on the ground (intentional drop).
     *
     * <p>When true, this is an intentional drop action by the player/creature.
     * When false, the item is being moved to a container (including death drops to corpse).</p>
     *
     * @return true if dropping on ground, false if dropping to container/corpse
     */
    public boolean isOnGround() {
        return onGround;
    }

    /**
     * Check if this drop is from death (item going to corpse).
     *
     * <p>This is the inverse of {@link #isOnGround()}. When a creature dies,
     * items are dropped with onGround=false (into the corpse container).</p>
     *
     * @return true if this is a death drop (going to corpse), false if intentional drop
     */
    public boolean isDeathDrop() {
        return !onGround;
    }
}
