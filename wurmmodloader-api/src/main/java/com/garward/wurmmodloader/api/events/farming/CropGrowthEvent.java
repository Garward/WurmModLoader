package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

/**
 * Fires when a crop tile is about to be checked for growth.
 *
 * <p>Allows mods to prevent growth checks (e.g., to disable weeds or control crop aging).</p>
 *
 * <p>Crop age is encoded in the data byte: {@code (data >> 4) & 0x7}</p>
 * <ul>
 *   <li>Age 0-5: Normal crop growth stages</li>
 *   <li>Age 6: Weeds</li>
 *   <li>Age 7: Withered</li>
 * </ul>
 *
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Disable weed growth by cancelling age 6 checks</li>
 *   <li>Pause crop aging under certain conditions</li>
 *   <li>Custom crop growth rules based on season/location</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class CropGrowthEvent extends CancellableEvent {

    private final int tilex;
    private final int tiley;
    private final int tile;
    private final byte data;
    private final byte farmData;

    /**
     * Creates a new CropGrowthEvent.
     *
     * @param tilex The tile X coordinate
     * @param tiley The tile Y coordinate
     * @param tile The tile type
     * @param data The tile data byte (contains crop age)
     * @param farmData The farm data byte
     */
    public CropGrowthEvent(int tilex, int tiley, int tile, byte data, byte farmData) {
        this.tilex = tilex;
        this.tiley = tiley;
        this.tile = tile;
        this.data = data;
        this.farmData = farmData;
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
     * Gets the tile type.
     *
     * @return The tile type
     */
    public int getTile() {
        return tile;
    }

    /**
     * Gets the tile data byte.
     *
     * @return The data byte (contains crop age in bits 4-6)
     */
    public byte getData() {
        return data;
    }

    /**
     * Gets the farm data byte.
     *
     * @return The farm data
     */
    public byte getFarmData() {
        return farmData;
    }

    /**
     * Gets the crop age from the data byte.
     *
     * <p>Age is extracted as: {@code (data >> 4) & 0x7}</p>
     *
     * @return The crop age (0-7)
     */
    public int getCropAge() {
        return (data >> 4) & 0x7;
    }

    /**
     * Checks if this crop tile is weeds (age 6).
     *
     * @return true if the crop is at weed stage
     */
    public boolean isWeeds() {
        return getCropAge() == 6;
    }

    /**
     * Checks if this crop tile is withered (age 7).
     *
     * @return true if the crop is withered
     */
    public boolean isWithered() {
        return getCropAge() == 7;
    }
}
