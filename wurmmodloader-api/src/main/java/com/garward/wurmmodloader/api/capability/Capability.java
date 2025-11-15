package com.garward.wurmmodloader.api.capability;

import com.garward.wurmmodloader.api.registry.ResourceLocation;

/**
 * Capability interface for attaching custom data to game objects.
 *
 * <p>Capabilities provide a type-safe, namespaced way to attach custom data
 * to Players, Creatures, Items, and Tiles. The framework handles persistence,
 * lazy loading, and dirty tracking automatically.</p>
 *
 * <p><strong>Example:</strong></p>
 * <pre>{@code
 * public class RPGStatsCapability implements Capability<RPGStats> {
 *     public static final RPGStatsCapability INSTANCE = new RPGStatsCapability();
 *     private static final ResourceLocation ID = new ResourceLocation("mymod", "rpg_stats");
 *
 *     public Class<RPGStats> getType() { return RPGStats.class; }
 *     public ResourceLocation getId() { return ID; }
 *     public RPGStats createDefaultInstance() { return new RPGStats(); }
 *     public String serialize(RPGStats stats) { return gson.toJson(stats); }
 *     public RPGStats deserialize(String data) { return gson.fromJson(data, RPGStats.class); }
 * }
 * }</pre>
 *
 * @param <T> The type of data this capability provides
 * @author WurmModLoader Team
 * @since 1.0.0 (Phase 5.5)
 */
public interface Capability<T> {

    /**
     * Get the data type this capability provides.
     *
     * @return The class of the data type
     */
    Class<T> getType();

    /**
     * Get the unique identifier for this capability.
     *
     * <p>This must be unique across all mods. Use your mod ID as the namespace
     * to prevent conflicts.</p>
     *
     * @return The resource location identifier
     */
    ResourceLocation getId();

    /**
     * Create a new default instance of this capability's data.
     *
     * <p>Called when a capability is first accessed on an object that doesn't
     * have existing data. Should return a valid, initialized instance.</p>
     *
     * @return A new default instance
     */
    T createDefaultInstance();

    /**
     * Serialize capability data for persistence.
     *
     * <p>Convert the data instance to a string for storage in the database.
     * Common formats: JSON, comma-separated values, custom format.</p>
     *
     * <p>The serialized form should be stable across mod versions if possible.</p>
     *
     * @param instance The data instance to serialize
     * @return Serialized string representation
     */
    String serialize(T instance);

    /**
     * Deserialize capability data from storage.
     *
     * <p>Convert a serialized string back into a data instance. This is called
     * when loading data from the database.</p>
     *
     * <p>Should handle legacy formats gracefully if the data structure changes
     * across mod versions.</p>
     *
     * @param data The serialized string
     * @return The deserialized data instance
     */
    T deserialize(String data);
}
