package com.garward.wurmmodloader.api.capability;

/**
 * Interface for objects that can provide capabilities.
 *
 * <p>The framework injects this interface into Player, Creature, Item, and Tile
 * classes via Javassist hooks. Mods should not implement this interface directly.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * Player player = ...;
 * RPGStats stats = player.getCapability(RPGStatsCapability.INSTANCE);
 * stats.addXP(100);
 * // Framework automatically saves to database on dirty flush
 * }</pre>
 *
 * @author WurmModLoader Team
 * @since 1.0.0 (Phase 5.5)
 */
public interface ICapabilityProvider {

    /**
     * Get a capability from this object.
     *
     * <p>If the capability hasn't been accessed before, this will:
     * <ol>
     *   <li>Try to load from database</li>
     *   <li>If not found, create default instance via {@link Capability#createDefaultInstance()}</li>
     *   <li>Mark as dirty for next database flush</li>
     * </ol></p>
     *
     * <p>Thread-safe for concurrent access.</p>
     *
     * @param capability The capability to get
     * @param <T> The data type
     * @return The capability instance (never null)
     */
    <T> T getCapability(Capability<T> capability);

    /**
     * Check if this object has a capability attached.
     *
     * <p>Returns true if the capability has been accessed at least once
     * (either loaded from DB or created as default).</p>
     *
     * @param capability The capability to check
     * @param <T> The data type
     * @return true if the capability is present
     */
    <T> boolean hasCapability(Capability<T> capability);
}
