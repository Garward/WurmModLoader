package com.garward.wurmmodloader.api.registry;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A registry for managing game objects with unique identifiers.
 *
 * <p>Registries provide centralized management of game content such as items, creatures,
 * actions, and spells. Each registry stores objects of a specific type, identified by
 * {@link ResourceLocation}s to ensure uniqueness across mods.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li>Type-safe registration with compile-time checking</li>
 *   <li>Namespace-based collision prevention (mod ID prefixes)</li>
 *   <li>Query and lookup capabilities</li>
 *   <li>Immutable after registration completes</li>
 * </ul>
 * </p>
 *
 * <p>Usage example:
 * <pre>{@code
 * Registry<ItemTemplate> items = Registries.ITEMS;
 *
 * // Register a new item
 * ItemTemplate sword = new ItemTemplateBuilder(...)
 *     .name("Magic Sword", "magic swords", "An enchanted blade")
 *     .build();
 * items.register(new ResourceLocation("mymod", "magic_sword"), sword);
 *
 * // Look up an item
 * Optional<ItemTemplate> found = items.get(new ResourceLocation("mymod", "magic_sword"));
 *
 * // Iterate all items
 * items.values().forEach(item -> System.out.println(item.getName()));
 * }</pre>
 * </p>
 *
 * <p>Thread Safety: Implementations must be thread-safe for reads after registration
 * is complete. During registration phase, external synchronization may be required.</p>
 *
 * @param <T> the type of objects stored in this registry
 * @since 1.0.0
 */
public interface Registry<T> {

    /**
     * Registers an object with the specified key.
     *
     * <p>The key must be unique within this registry. Attempting to register
     * a duplicate key will throw an exception.</p>
     *
     * @param key the unique resource location for this object
     * @param value the object to register (must not be null)
     * @throws IllegalArgumentException if key or value is null
     * @throws IllegalStateException if the key is already registered or registry is frozen
     * @since 1.0.0
     */
    void register(ResourceLocation key, T value);

    /**
     * Retrieves the object registered with the specified key.
     *
     * @param key the resource location to look up
     * @return an Optional containing the registered object, or empty if not found
     * @throws IllegalArgumentException if key is null
     * @since 1.0.0
     */
    Optional<T> get(ResourceLocation key);

    /**
     * Checks if an object is registered with the specified key.
     *
     * @param key the resource location to check
     * @return true if the key is registered, false otherwise
     * @throws IllegalArgumentException if key is null
     * @since 1.0.0
     */
    default boolean containsKey(ResourceLocation key) {
        return get(key).isPresent();
    }

    /**
     * Gets an unmodifiable set of all registered keys.
     *
     * <p>The returned set reflects the current state of the registry.
     * It should not be used for modification.</p>
     *
     * @return an unmodifiable set of all resource locations
     * @since 1.0.0
     */
    Set<ResourceLocation> keySet();

    /**
     * Returns a stream of all registered values.
     *
     * <p>This is useful for batch operations and filtering registered objects.</p>
     *
     * @return a stream of all registered values
     * @since 1.0.0
     */
    Stream<T> values();

    /**
     * Gets the total number of registered objects.
     *
     * @return the number of entries in this registry
     * @since 1.0.0
     */
    default int size() {
        return keySet().size();
    }

    /**
     * Checks if this registry is empty.
     *
     * @return true if no objects are registered, false otherwise
     * @since 1.0.0
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Gets the name/type identifier for this registry.
     *
     * <p>This is primarily used for debugging and logging purposes.</p>
     *
     * @return the registry name (e.g., "items", "creatures", "actions")
     * @since 1.0.0
     */
    String getRegistryName();

    /**
     * Freezes this registry, preventing further registrations.
     *
     * <p>After freezing, any attempt to register new objects will throw an
     * {@link IllegalStateException}. This is typically called after mod
     * initialization is complete.</p>
     *
     * @throws IllegalStateException if registry is already frozen
     * @since 1.0.0
     */
    void freeze();

    /**
     * Checks if this registry is frozen (no longer accepting registrations).
     *
     * @return true if frozen, false otherwise
     * @since 1.0.0
     */
    boolean isFrozen();
}
