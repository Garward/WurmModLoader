package com.garward.wurmmodloader.core.registry;

import com.garward.wurmmodloader.api.registry.Registry;
import com.garward.wurmmodloader.api.registry.ResourceLocation;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * A simple thread-safe implementation of {@link Registry}.
 *
 * <p>This implementation uses a {@link ConcurrentHashMap} for storage, providing
 * thread-safe access during and after registration. Once frozen, the registry
 * becomes immutable and will reject any further registration attempts.</p>
 *
 * <p>Thread Safety: This class is fully thread-safe. Multiple threads can
 * safely register objects concurrently, though it's recommended to complete
 * all registrations from a single thread during mod initialization.</p>
 *
 * @param <T> the type of objects stored in this registry
 * @since 1.0.0
 */
public class SimpleRegistry<T> implements Registry<T> {

    private final String registryName;
    private final Map<ResourceLocation, T> entries;
    private final AtomicBoolean frozen;

    /**
     * Constructs a new SimpleRegistry with the specified name.
     *
     * @param registryName the name/type identifier for this registry
     * @throws IllegalArgumentException if registryName is null or empty
     * @since 1.0.0
     */
    public SimpleRegistry(String registryName) {
        if (registryName == null || registryName.isEmpty()) {
            throw new IllegalArgumentException("Registry name cannot be null or empty");
        }
        this.registryName = registryName;
        this.entries = new ConcurrentHashMap<>();
        this.frozen = new AtomicBoolean(false);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this registry is frozen
     */
    @Override
    public void register(ResourceLocation key, T value) {
        Objects.requireNonNull(key, "Registration key cannot be null");
        Objects.requireNonNull(value, "Registration value cannot be null");

        if (frozen.get()) {
            throw new IllegalStateException(
                "Cannot register to frozen registry '" + registryName + "': " + key
            );
        }

        T existing = entries.putIfAbsent(key, value);
        if (existing != null) {
            throw new IllegalStateException(
                "Duplicate registration in registry '" + registryName + "': " + key +
                " (existing: " + existing + ", new: " + value + ")"
            );
        }
    }

    @Override
    public Optional<T> get(ResourceLocation key) {
        Objects.requireNonNull(key, "Lookup key cannot be null");
        return Optional.ofNullable(entries.get(key));
    }

    @Override
    public Set<ResourceLocation> keySet() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    @Override
    public Stream<T> values() {
        return entries.values().stream();
    }

    @Override
    public String getRegistryName() {
        return registryName;
    }

    @Override
    public void freeze() {
        if (!frozen.compareAndSet(false, true)) {
            throw new IllegalStateException("Registry '" + registryName + "' is already frozen");
        }
    }

    @Override
    public boolean isFrozen() {
        return frozen.get();
    }

    @Override
    public String toString() {
        return "SimpleRegistry{" +
            "name='" + registryName + '\'' +
            ", size=" + entries.size() +
            ", frozen=" + frozen.get() +
            '}';
    }
}
