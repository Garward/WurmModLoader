package com.garward.wurmmodloader.core.icon;

import com.garward.wurmmodloader.api.serverpacks.ServerPackOptions;
import com.garward.wurmmodloader.api.serverpacks.ServerPacks;
import com.garward.wurmmodloader.core.serverpacks.ServerPackHost;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static helper used by {@link IconPackGenerator} and {@link FrameworkIconsPack}
 * to publish icon-pack JARs through the framework-owned
 * {@link ServerPackHost}.
 *
 * <p>Before the Scope B promotion of serverpacks, this class lived in a
 * different classloader from the {@code mods/serverpacks} jar and reached
 * the registration API via {@link Class#forName} + reflective method
 * lookup. With serverpacks now part of the framework, the call collapses to
 * a direct {@link ServerPacks#addServerPack} invocation.
 *
 * @since 1.0.0
 */
public final class IconPackServerPacksBridge {

    private static final Logger logger = Logger.getLogger(IconPackServerPacksBridge.class.getName());

    private IconPackServerPacksBridge() {}

    /**
     * Register an icon pack JAR under the historical {@code "iconpack"} name.
     * Used by the iconzz-style pipeline in {@link IconPackGenerator}.
     */
    public static void registerIconPack(Path jarFile) {
        registerIconPack("iconpack", jarFile);
    }

    /**
     * Register a named pack (e.g. {@code "framework-icons"}). Pack is loaded
     * into memory and registered with PREPEND + FORCE so it wins priority
     * against vanilla and forces clients to refresh.
     */
    public static void registerIconPack(String packName, Path jarFile) {
        try {
            byte[] packData = Files.readAllBytes(jarFile);
            ServerPacks packs = ServerPackHost.getInstance();
            packs.addServerPack(packName, packData,
                ServerPackOptions.PREPEND, ServerPackOptions.FORCE);
            logger.info("✓ Registered " + packName + " with serverpacks (PREPEND + FORCE): "
                + jarFile.getFileName() + " (" + packData.length + " bytes)");
        } catch (Exception e) {
            logger.log(Level.WARNING,
                "Failed to register " + packName + " with serverpacks", e);
        }
    }
}
