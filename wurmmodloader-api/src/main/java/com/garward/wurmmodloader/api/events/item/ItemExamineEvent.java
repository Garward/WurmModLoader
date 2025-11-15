package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.Event;


import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when a player examines an item.
 *
 * <p>Mods can add custom text to the examine output by appending to the
 * description using {@link #addDescription(String)}.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public class ItemExamineEvent extends Event {

    private final Item item;
    private final Creature examiner;
    private final Creature owner; // Owner of body part (null if not a body part)
    private final StringBuilder additionalDescription;

    public ItemExamineEvent(Item item, Creature examiner, Creature owner) {
        this.item = item;
        this.examiner = examiner;
        this.owner = owner;
        this.additionalDescription = new StringBuilder();
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

    /**
     * Get the owner of the item (for body parts).
     *
     * <p>Returns the creature who owns this body part. For non-body-part items,
     * returns null.</p>
     *
     * @return The owner creature, or null if not a body part
     */
    public Creature getOwner() {
        return owner;
    }

    /**
     * Add additional description text to the examine output.
     *
     * <p>Text will be appended to the default examine text.</p>
     *
     * @param text Text to add
     */
    public void addDescription(String text) {
        if (text != null && !text.isEmpty()) {
            if (additionalDescription.length() > 0) {
                additionalDescription.append("\n");
            }
            additionalDescription.append(text);
        }
    }

    /**
     * Get all additional description text added by mods.
     *
     * @return Additional description text, or empty string if none
     */
    public String getAdditionalDescription() {
        return additionalDescription.toString();
    }

    /**
     * Check if any additional description was added.
     *
     * @return true if mods added description text
     */
    public boolean hasAdditionalDescription() {
        return additionalDescription.length() > 0;
    }
}
