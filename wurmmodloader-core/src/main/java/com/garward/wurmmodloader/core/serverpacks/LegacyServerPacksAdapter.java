package com.garward.wurmmodloader.core.serverpacks;

import com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks;

import java.nio.file.Path;

/**
 * Adapter that exposes the framework {@link ServerPackHost} through the
 * <em>legacy</em> {@code com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks}
 * interface. Used by {@code ServerPacks.getInstance()} on the deprecated
 * package path so old mod jars (e.g. {@code WyvernMods/ServerPackHandler})
 * keep linking after the Scope B promotion.
 *
 * <p>Translates the legacy nested {@link ServerPacks.ServerPackOptions} enum
 * values into the new top-level
 * {@link com.garward.wurmmodloader.api.serverpacks.ServerPackOptions} by name.
 * Both enums declare PREPEND and FORCE so the mapping is total.
 */
@SuppressWarnings("deprecation")
public final class LegacyServerPacksAdapter implements ServerPacks {

    private static final LegacyServerPacksAdapter INSTANCE = new LegacyServerPacksAdapter();

    public static LegacyServerPacksAdapter getInstance() {
        return INSTANCE;
    }

    private LegacyServerPacksAdapter() {}

    @Override
    public void addServerPack(Path path, ServerPacks.ServerPackOptions... options) {
        ServerPackHost.getInstance().addServerPack(path, mapLegacy(options));
    }

    @Override
    public void addServerPack(String name, Path path, ServerPacks.ServerPackOptions... options) {
        ServerPackHost.getInstance().addServerPack(name, path, mapLegacy(options));
    }

    @Override
    public void addServerPack(byte[] data, ServerPacks.ServerPackOptions... options) {
        ServerPackHost.getInstance().addServerPack(data, mapLegacy(options));
    }

    @Override
    public void addServerPack(String name, byte[] data, ServerPacks.ServerPackOptions... options) {
        ServerPackHost.getInstance().addServerPack(name, data, mapLegacy(options));
    }

    private static com.garward.wurmmodloader.api.serverpacks.ServerPackOptions[] mapLegacy(
            ServerPacks.ServerPackOptions[] in) {
        if (in == null || in.length == 0) {
            return new com.garward.wurmmodloader.api.serverpacks.ServerPackOptions[0];
        }
        com.garward.wurmmodloader.api.serverpacks.ServerPackOptions[] out =
            new com.garward.wurmmodloader.api.serverpacks.ServerPackOptions[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = com.garward.wurmmodloader.api.serverpacks.ServerPackOptions.valueOf(in[i].name());
        }
        return out;
    }
}
