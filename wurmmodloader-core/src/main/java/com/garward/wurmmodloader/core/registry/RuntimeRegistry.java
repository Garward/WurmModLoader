package com.garward.wurmmodloader.core.registry;

import java.util.*;
import java.util.function.Consumer;

/**
 * Generic runtime registry for modular systems (e.g., bytecode patches, event logic handlers).
 */
public class RuntimeRegistry<T> {
    private final String name;
    private final List<T> entries = new ArrayList<>();
    private boolean frozen = false;
    private final Consumer<T> initializer;

    public RuntimeRegistry(String name, Consumer<T> initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    public synchronized void register(T entry) {
        if (frozen) throw new IllegalStateException("Registry " + name + " is frozen!");
        entries.add(entry);
    }

    public synchronized void initializeAll() {
        entries.forEach(initializer);
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public String getName() { return name; }
    public List<T> getEntries() { return Collections.unmodifiableList(entries); }
}
