package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fires when a crop is being harvested and the harvest quantity is being calculated.
 *
 * <p>Allows mods to modify the harvest quantity before items are created.</p>
 *
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Increase harvest yield based on skill/traits</li>
 *   <li>Bonus harvests from special tools or buffs</li>
 *   <li>Modify quantity based on tile quality or crop age</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CropHarvestEvent extends Event {

    private final Creature performer;
    private final int tilex;
    private final int tiley;
    private final boolean onSurface;
    private final int tile;
    private final float counter;
    private final Item tool;
    private int quantity;

    /**
     * Creates a new CropHarvestEvent.
     *
     * @param performer The creature harvesting (cannot be null)
     * @param tilex The tile X coordinate
     * @param tiley The tile Y coordinate
     * @param onSurface Whether the tile is on surface
     * @param tile The tile type
     * @param counter The action counter
     * @param tool The tool being used (may be null)
     * @param quantity The base harvest quantity (before modifications)
     * @throws IllegalArgumentException if performer is null
     */
    public CropHarvestEvent(Creature performer, int tilex, int tiley, boolean onSurface,
                            int tile, float counter, Item tool, int quantity) {
        if (performer == null) {
            throw new IllegalArgumentException("performer cannot be null");
        }
        this.performer = performer;
        this.tilex = tilex;
        this.tiley = tiley;
        this.onSurface = onSurface;
        this.tile = tile;
        this.counter = counter;
        this.tool = tool;
        this.quantity = quantity;
    }

    /**
     * Gets the creature harvesting.
     *
     * @return The performer (never null)
     */
    public Creature getPerformer() {
        return performer;
    }

    /**
     * Gets the tile X coordinate.
     *
     * @return The X coordinate
     */
    public int getTileX() {
        return tilex;
    }

    /**
     * Gets the tile Y coordinate.
     *
     * @return The Y coordinate
     */
    public int getTileY() {
        return tiley;
    }

    /**
     * Checks if the tile is on surface.
     *
     * @return true if on surface
     */
    public boolean isOnSurface() {
        return onSurface;
    }

    /**
     * Gets the tile type.
     *
     * @return The tile type
     */
    public int getTile() {
        return tile;
    }

    /**
     * Gets the action counter.
     *
     * @return The counter value
     */
    public float getCounter() {
        return counter;
    }

    /**
     * Gets the tool being used.
     *
     * @return The tool, or null if none
     */
    public Item getTool() {
        return tool;
    }

    /**
     * Gets the harvest quantity.
     *
     * @return The quantity to harvest
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Sets the harvest quantity.
     *
     * <p>Mods can increase or decrease the harvest yield.</p>
     *
     * @param quantity The new quantity (should be >= 0)
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Adds to the harvest quantity.
     *
     * <p>Convenience method for bonus harvests.</p>
     *
     * @param bonus The amount to add to quantity
     */
    public void addQuantity(int bonus) {
        this.quantity += bonus;
    }
}
