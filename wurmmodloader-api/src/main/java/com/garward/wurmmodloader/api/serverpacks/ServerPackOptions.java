package com.garward.wurmmodloader.api.serverpacks;

/**
 * Options used when registering a server pack via {@link ServerPacks}.
 *
 * <p>Extracted to its own file as part of the framework-owned promotion of
 * the serverpacks subsystem (Scope B). The original enum lived nested in
 * {@code com.garward.wurmmodloader.mods.serverpacks.api.ServerPacks} in the
 * community mod; that nested form is preserved as a deprecated alias for
 * mods compiled against the older jar.
 *
 * @since 0.10.2
 */
public enum ServerPackOptions {
    /**
     * Prepend the pack to the list of packs on the client. Useful when the
     * pack should win priority against vanilla / earlier packs.
     */
    PREPEND,

    /**
     * Instruct the client to force the download regardless of cache state.
     */
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
