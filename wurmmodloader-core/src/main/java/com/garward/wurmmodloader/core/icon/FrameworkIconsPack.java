package com.garward.wurmmodloader.core.icon;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds {@code mods/serverpacks/framework-icons.jar} from the
 * {@link ModIconScanner} buffer and registers it with the {@code serverpacks}
 * mod via {@link IconPackServerPacksBridge}.
 *
 * <p>JAR layout: {@code <modname>/<relpath>.png}, exactly matching the
 * {@code pack:framework-icons/<modname>/<relpath>.png} URIs that
 * {@link IconRegistry#register(String, String)} stores. The client's
 * {@code PackAssetResolver} (the {@code pack:} URI handler) resolves through
 * the pack chain by name + path, so the JAR's entries map 1:1 to URIs the
 * registry already publishes.
 *
 * <p>Rebuilds fresh on every boot — there's no incremental path. The pack is
 * tiny (32×32 PNGs × N icons), and a stale entry in a long-lived JAR would
 * cause silent ID drift for items in the DB. Writing a new JAR every boot
 * keeps the registry, on-disk pack, and announced manifest in lockstep.
 *
 * <p>Skips emission entirely when the registry has no entries — registering an
 * empty pack would still make clients fetch it and burn cache slots.
 *
 * @since 0.4.1
 */
public final class FrameworkIconsPack {

    private static final Logger logger = Logger.getLogger(FrameworkIconsPack.class.getName());

    /** Pack name as registered with serverpacks; matches the {@code pack:} URI prefix. */
    public static final String PACK_NAME = "framework-icons";

    /** On-disk staging location. Lives under mods/serverpacks/ for parity with other packs. */
    private static final Path PACK_FILE = Paths.get("mods", "serverpacks", "framework-icons.jar");

    private static volatile boolean synthesized = false;

    private FrameworkIconsPack() {
        throw new AssertionError("FrameworkIconsPack should not be instantiated");
    }

    /**
     * Build + write + register the framework-icons pack. Idempotent within a
     * boot — second call is a no-op so multiple lifecycle hooks can call this
     * without causing duplicate registration.
     *
     * <p>If {@link IconPackServerPacksBridge} has not yet seen the
     * {@code serverpacks} mod load, this still writes the JAR to disk; the
     * bridge logs at FINE and the JAR can be picked up via a later
     * registration call. In practice the timing is fine because this method
     * runs from {@code fireOnItemTemplatesCreated}, well after all mods init.
     */
    public static synchronized void synthesizeAndRegister() {
        if (synthesized) {
            return;
        }
        synthesized = true;

        List<ModIconScanner.ScannedIcon> scan = ModIconScanner.getLastScan();
        if (scan == null || scan.isEmpty()) {
            logger.info("[FrameworkIconsPack] No scanned mod icons; skipping pack emission");
            return;
        }

        try {
            Path parent = PACK_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Delete-and-recreate to keep stale entries from accumulating.
            Files.deleteIfExists(PACK_FILE);

            try (JarOutputStream jar = new JarOutputStream(
                    new BufferedOutputStream(new FileOutputStream(PACK_FILE.toFile())))) {
                for (ModIconScanner.ScannedIcon icon : scan) {
                    String entry = icon.modName + "/" + icon.relPath;
                    jar.putNextEntry(new JarEntry(entry));
                    jar.write(icon.bytes);
                    jar.closeEntry();
                }
            }

            long size = Files.size(PACK_FILE);
            logger.info("[FrameworkIconsPack] Wrote " + PACK_FILE + " (" + scan.size()
                + " icons, " + size + " bytes)");

            IconPackServerPacksBridge.registerIconPack(PACK_NAME, PACK_FILE);
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                "[FrameworkIconsPack] Failed to synthesize " + PACK_FILE, e);
        } catch (Exception e) {
            logger.log(Level.WARNING,
                "[FrameworkIconsPack] Failed to register pack with serverpacks", e);
        }
    }

    /**
     * Reset the synthesized flag — used by reload paths and tests.
     */
    public static synchronized void resetForTest() {
        synthesized = false;
    }
}
