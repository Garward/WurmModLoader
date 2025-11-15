package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;

/**
 * Fires when a priest restriction check is performed (e.g., during crafting actions).
 *
 * <p>Allows mods to override whether priest restrictions apply to specific actions.
 * Normally priests cannot perform certain crafting actions (improve, polish, temper),
 * but mods can override this by modifying the result.</p>
 *
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Remove all priest crafting restrictions</li>
 *   <li>Allow specific priest types to craft certain items</li>
 *   <li>Conditional restrictions based on deity or location</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class PriestRestrictionCheckEvent extends Event {

    private final Creature creature;
    private final String context;
    private boolean isPriest;

    /**
     * Creates a new PriestRestrictionCheckEvent.
     *
     * @param creature The creature being checked (cannot be null)
     * @param context The context of the check (e.g., "improve", "polish", "temper")
     * @param isPriest The default priest status
     * @throws IllegalArgumentException if creature is null
     */
    public PriestRestrictionCheckEvent(Creature creature, String context, boolean isPriest) {
        if (creature == null) {
            throw new IllegalArgumentException("creature cannot be null");
        }
        this.creature = creature;
        this.context = context;
        this.isPriest = isPriest;
    }

    /**
     * Gets the creature being checked.
     *
     * @return The creature (never null)
     */
    public Creature getCreature() {
        return creature;
    }

    /**
     * Gets the context of the restriction check.
     *
     * @return The context string (e.g., "improve", "polish", "temper")
     */
    public String getContext() {
        return context;
    }

    /**
     * Gets whether priest restrictions should apply.
     *
     * @return true if the creature should be treated as a priest (restrictions apply)
     */
    public boolean isPriest() {
        return isPriest;
    }

    /**
     * Sets whether priest restrictions should apply.
     *
     * <p>Set to false to remove priest restrictions for this action.</p>
     *
     * @param isPriest true if restrictions should apply
     */
    public void setPriest(boolean isPriest) {
        this.isPriest = isPriest;
    }
}
