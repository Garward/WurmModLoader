package com.garward.wurmmodloader.api.registry;

import java.util.Objects;

/**
 * Event for registering new entries in a registry during mod initialization.
 *
 * <p>This event provides a type-safe and convenient way to register new content
 * to a registry. It is typically fired during the mod registration phase, allowing
 * mods to add their custom items, creatures, or other game objects.</p>
 *
 * <p>Generic Type Parameters:
 * <ul>
 *   <li>{@code T} - The type of objects being registered in the registry</li>
 * </ul>
 * </p>
 *
 * <p>Thread Safety: This class is immutable after construction and thread-safe
 * for concurrent access. The {@link #register(ResourceLocation, Object)} method
 * delegates to the underlying registry's thread-safe implementation.</p>
 *
 * @since 1.0.0
 */
public class RegisterEvent<T> extends RegistryEvent {
    private final Registry<T> typedRegistry;

    /**
     * Constructs a new register event for the specified registry.
     *
     * @param registry the registry to register entries to (must not be null)
     * @throws IllegalArgumentException if registry is null
     * @since 1.0.0
     */
    public RegisterEvent(Registry<T> registry) {
        super(registry);
        this.typedRegistry = Objects.requireNonNull(registry, "Registry cannot be null");
    }

    /**
     * Gets the registry associated with this event, with type safety.
     *
     * @return the type-safe registry reference (never null)
     * @since 1.0.0
     */
    @Override
    public Registry<T> getRegistry() {
        return typedRegistry;
    }

    /**
     * Registers a new entry in the registry.
     *
     * <p>This is a convenience method that delegates to the registry's
     * {@link Registry#register(ResourceLocation, Object)} method while adding
     * additional null checks and validation.</p>
     *
     * @param key the unique resource location for this entry (must not be null)
     * @param value the object to register (must not be null)
     * @throws IllegalArgumentException if key or value is null
     * @throws IllegalStateException if the registry is already frozen
     * @since 1.0.0
     */
    public void register(ResourceLocation key, T value) {
        Objects.requireNonNull(key, "Resource location cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

        if (typedRegistry.isFrozen()) {
            throw new IllegalStateException("Cannot register to frozen registry: " + typedRegistry.getRegistryName());
        }

        typedRegistry.register(key, value);
    }

    /**
     * Example usage:
     * <pre>{@code
     * // Example: Registering custom items when the ITEMS registry event fires
     * public class MyMod {
     *     public void onRegisterItems(RegisterEvent<ItemTemplate> event) {
     *         // Create custom item
     *         ItemTemplate customSword = new ItemTemplateBuilder(...)
     *             .name("Magic Sword", "magic swords", "An enchanted blade")
     *             .build();
     *
     *         // Register using convenience method
     *         event.register(
     *             new ResourceLocation("mymod", "magic_sword"),
     *             customSword
     *         );
     *
     *         // Can also access registry directly for batch operations
     *         Registry<ItemTemplate> items = event.getRegistry();
     *         items.register(new ResourceLocation("mymod", "shield"), createShield());
     *     }
     * }
     * }</pre>
     */
}