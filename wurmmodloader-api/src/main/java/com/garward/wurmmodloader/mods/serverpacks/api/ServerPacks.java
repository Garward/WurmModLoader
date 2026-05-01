package com.garward.wurmmodloader.mods.serverpacks.api;

import java.nio.file.Path;

/**
 * @deprecated Use {@link com.garward.wurmmodloader.api.serverpacks.ServerPacks}
 * and {@link com.garward.wurmmodloader.api.serverpacks.ServerPackOptions}
 * instead. This package-path is preserved as a one-release source/binary
 * compatibility alias for mods that compiled against the community
 * {@code mods/serverpacks} jar.
 *
 * <p>The framework's
 * {@code com.garward.wurmmodloader.core.serverpacks.ServerPackHost}
 * implements both this legacy interface and the new one so existing mod
 * binaries (notably {@code mods/WyvernMods/ServerPackHandler}, which
 * resolves this class by name via reflection) keep linking.
 */
@Deprecated
public interface ServerPacks {

    /**
     * @deprecated Use the top-level
     * {@link com.garward.wurmmodloader.api.serverpacks.ServerPackOptions}.
     */
    @Deprecated
    enum ServerPackOptions {
        PREPEND,
        FORCE,
        ;

        public boolean isIn(ServerPackOptions... options) {
            if (options != null) {
                for (ServerPackOptions option : options) {
                    if (option == this) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    void addServerPack(Path path, ServerPackOptions... options);

    void addServerPack(String name, Path path, ServerPackOptions... options);

    void addServerPack(byte[] data, ServerPackOptions... options);

    void addServerPack(String name, byte[] data, ServerPackOptions... options);

    /**
     * Returns a legacy-shaped view of the framework {@code ServerPackHost}.
     * The returned object is a {@code LegacyServerPacksAdapter} that
     * translates legacy enum values to the new ones at the boundary.
     */
    static ServerPacks getInstance() {
        try {
            Class<?> adapterCls = Class.forName(
                "com.garward.wurmmodloader.core.serverpacks.LegacyServerPacksAdapter");
            return (ServerPacks) adapterCls.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
