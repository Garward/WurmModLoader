package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when an item's enchantment strings are about to be sent to a player.
 *
 * <p>This event is called AFTER the examine text and allows mods to send additional
 * colored message lines to the player's communicator. This is perfect for adding
 * custom item stats, damage calculations, or other information that should appear
 * as separate colored messages.</p>
 *
 * <p>Unlike {@link ItemExamineEvent} which modifies the examine text itself, this event
 * is called specifically for sending additional colored messages through
 * the Communicator's message system.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 * @see ItemExamineEvent
 */
public class ItemEnchantmentStringsEvent extends Event {

    private final Item item;
    private final Creature examiner;

    public ItemEnchantmentStringsEvent(Item item, Creature examiner) {
        this.item = item;
        this.examiner = examiner;
    }

    /**
     * Get the item being examined.
     *
     * @return The item
     */
    public Item getItem() {
        return item;
    }

    /**
     * Get the creature examining the item.
     *
     * @return The examiner
     */
    public Creature getExaminer() {
        return examiner;
    }
}
