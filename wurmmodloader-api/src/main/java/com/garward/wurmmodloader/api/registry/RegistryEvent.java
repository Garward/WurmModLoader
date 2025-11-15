package com.garward.wurmmodloader.api.registry;

import java.util.Objects;

/**
 * Base class for all registry-related events in the WurmModLoader system.
 *
 * <p>Registry events are fired during different phases of registry management,
 * such as registration, freezing, or post-initialization. These events allow mods
 * to react to registry changes in a controlled and thread-safe manner.</p>
 *
 * <p>Event lifecycle:
 * <ol>
 *   <li>Events are fired during mod initialization phase</li>
 *   <li>Events are processed in namespace order (mod ID)</li>
 *   <li>Events are immutable after construction</li>
 * </ol>
 * </p>
 *
 * <p>Thread Safety: This class is immutable after construction and thread-safe
 * for concurrent access. Subclasses must maintain this guarantee.</p>
 *
 * @since 1.0.0
 */
public abstract class RegistryEvent {
    private final Registry<?> registry;
    private final String registryName;

    /**
     * Constructs a new registry event for the specified registry.
     *
     * @param registry the registry associated with this event (must not be null)
     * @throws IllegalArgumentException if registry is null
     * @since 1.0.0
     */
    protected RegistryEvent(Registry<?> registry) {
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
        this.registryName = registry.getRegistryName();
    }

    /**
     * Gets the registry associated with this event.
     *
     * @return the registry (never null)
     * @since 1.0.0
     */
    public Registry<?> getRegistry() {
        return registry;
    }

    /**
     * Gets the name/identifier of the registry.
     *
     * @return the registry name (never null)
     * @since 1.0.0
     */
    public String getRegistryName() {
        return registryName;
    }
}