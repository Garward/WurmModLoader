package com.garward.wurmmodloader.api.serverpacks;

import java.nio.file.Path;

/**
 * Public, framework-owned API for registering server-distributed asset packs.
 *
 * <p>Server packs are JAR-shaped asset bundles announced to connected clients
 * over the {@code com.garward.serverpacks} ModComm channel and served via HTTP
 * through the framework's HTTP subsystem. Clients fetch them on connect, write
 * them into their resource chain, and then resolve {@code pack:<name>/<path>}
 * URIs through the merged chain.
 *
 * <p>This interface used to live in the community {@code serverpacks} mod at
 * {@code com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks}. It was
 * promoted into the framework API jar to remove the cross-classloader
 * reflection required when framework code (icon packs, declarative UI) needed
 * to publish packs.
 *
 * <p>The legacy package-path remains available as a deprecated alias for one
 * release — see {@code com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks}.
 *
 * @since 0.10.2
 */
public interface ServerPacks {

    /**
     * Register a pack from disk under an auto-generated SHA-1-derived name.
     *
     * @param path absolute path to the pack file (typically a JAR)
     * @param options optional flags ({@link ServerPackOptions#PREPEND},
     *                {@link ServerPackOptions#FORCE})
     */
    void addServerPack(Path path, ServerPackOptions... options);

    /**
     * Register a pack from disk under an explicit name. The {@code name} is
     * used as the pack identifier in the HTTP URL and in the announce
     * manifest, so it must not contain {@code . / % ? #}.
     */
    void addServerPack(String name, Path path, ServerPackOptions... options);

    /**
     * Register an in-memory pack under an auto-generated SHA-1-derived name.
     */
    void addServerPack(byte[] data, ServerPackOptions... options);

    /**
     * Register an in-memory pack under an explicit name.
     */
    void addServerPack(String name, byte[] data, ServerPackOptions... options);

    /**
     * Returns the framework-owned singleton instance.
     *
     * <p>Initialized during {@code ServerHook.fireOnServerStarted}; returns
     * {@code null} if called before then. Callers in mod code typically run
     * after server start (event handlers, item-template hooks) so this is
     * safe.
     */
    static ServerPacks getInstance() {
        try {
            Class<?> hostCls = Class.forName(
                "com.garward.wurmmodloader.core.serverpacks.ServerPackHost");
            return (ServerPacks) hostCls.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException e) {
            // Pre-init or framework not on classpath — return null so callers
            // can null-check rather than catch a linkage error.
            return null;
        }
    }
}
