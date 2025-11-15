package com.garward.wurmmodloader.core.bytecode;

import com.garward.wurmmodloader.api.bytecode.BytecodePatch;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects {@link BytecodePatch} implementations registered through {@link com.garward.wurmmodloader.core.registry.RuntimeRegistries}.
 */
public final class PatchRegistry {

    private static final List<BytecodePatch> PATCHES = new CopyOnWriteArrayList<>();

    private PatchRegistry() {}

    public static void register(BytecodePatch patch) {
        PATCHES.add(Objects.requireNonNull(patch, "patch"));
    }

    public static List<BytecodePatch> getRegisteredPatches() {
        return Collections.unmodifiableList(PATCHES);
    }

    public static void reset() {
        PATCHES.clear();
    }
}
