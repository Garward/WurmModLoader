package com.garward.wurmmodloader.core.icon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Boot-time scanner for the {@code mods/<modname>/icons/**\/*.png} convention.
 *
 * <p>The dynamic-icon system pulls images out of mod folders without requiring
 * a per-mod opt-in: drop a 32×32 PNG under {@code mods/<modname>/icons/foo.png},
 * and the scanner allocates a short via {@link IconRegistry#register(String, String)}
 * with stringId {@code <modname>:foo} and packUri
 * {@code pack:framework-icons/<modname>/foo.png}. The synthesizer (DynIcons 3)
 * later consumes {@link #getLastScan()} to bake the framework-icons.jar
 * server-pack from the same byte buffers.
 *
 * <p>Validation is strict on dimensions — anything other than 32×32 is logged
 * SEVERE and skipped. Anything {@code ImageIO} can't read (corrupt PNG, wrong
 * extension renamed) is also skipped with a SEVERE.
 *
 * <p>Reserved directories (e.g. the framework's own {@code mods/WurmModLoader/}
 * state folder) are not scanned — see {@link #RESERVED_NAMES}.
 *
 * @since 0.4.1
 */
public final class ModIconScanner {

    private static final Logger logger = Logger.getLogger(ModIconScanner.class.getName());

    /** Mandated icon dimensions. Wurm's vanilla atlas is 32×32 per slot. */
    private static final int REQUIRED_DIM = 32;

    /**
     * Subdirectories of {@code mods/} that should never be scanned. These
     * either belong to the framework's own state ({@code WurmModLoader/}) or
     * are non-mod helpers that legitimately ship .png files unrelated to
     * icons.
     */
    private static final List<String> RESERVED_NAMES = Collections.unmodifiableList(
        java.util.Arrays.asList("WurmModLoader"));

    private static List<ScannedIcon> lastScan = Collections.emptyList();

    private ModIconScanner() {
        throw new AssertionError("ModIconScanner should not be instantiated");
    }

    /**
     * Walks {@code modsDir/<modname>/icons/**\/*.png}, validates each PNG, and
     * registers it with the {@link IconRegistry}. Returns the buffered
     * {@link ScannedIcon} list (also retrievable via {@link #getLastScan()}).
     *
     * <p>Idempotent across calls within one boot — re-running with the same
     * disk state returns the same list and re-registration is a no-op via
     * {@link IconRegistry#register(String, String)}'s idempotence.
     *
     * @param modsDir typically {@code Paths.get("mods")} from the server cwd
     * @return immutable list of scanned icons (empty if {@code modsDir} is missing)
     */
    public static List<ScannedIcon> scanAll(Path modsDir) {
        if (modsDir == null || !Files.isDirectory(modsDir)) {
            logger.fine("[ModIconScanner] mods dir absent: " + modsDir);
            lastScan = Collections.emptyList();
            return lastScan;
        }

        List<ScannedIcon> result = new ArrayList<>();
        try (DirectoryStream<Path> mods = Files.newDirectoryStream(modsDir, Files::isDirectory)) {
            for (Path modDir : mods) {
                String modName = modDir.getFileName().toString();
                if (RESERVED_NAMES.contains(modName)) {
                    continue;
                }
                Path iconsDir = modDir.resolve("icons");
                if (!Files.isDirectory(iconsDir)) {
                    continue;
                }
                scanModIconsDir(modName, iconsDir, result);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "[ModIconScanner] Failed to walk " + modsDir, e);
        }

        Collections.sort(result, (a, b) -> a.stringId.compareTo(b.stringId));
        lastScan = Collections.unmodifiableList(result);
        logger.info("[ModIconScanner] Registered " + lastScan.size() + " mod icon(s) from "
                    + modsDir);
        return lastScan;
    }

    /**
     * Snapshot of the most recent scan. Consumed by the framework-icons.jar
     * synthesizer (DynIcons 3) so it can write the byte payloads into the
     * server-pack without re-reading from disk.
     */
    public static List<ScannedIcon> getLastScan() {
        return lastScan;
    }

    private static void scanModIconsDir(String modName, Path iconsDir, List<ScannedIcon> out) {
        try (Stream<Path> walk = Files.walk(iconsDir)) {
            List<Path> pngs = walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".png");
                })
                .collect(Collectors.toList());

            for (Path png : pngs) {
                Path rel = iconsDir.relativize(png);
                String relStr = rel.toString().replace('\\', '/');
                String relNoExt = relStr.substring(0, relStr.length() - ".png".length());

                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(png);
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE,
                        "[ModIconScanner] " + modName + ": failed to read icons/" + relStr, ioe);
                    continue;
                }

                BufferedImage img;
                try {
                    img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE,
                        "[ModIconScanner] " + modName + ": ImageIO failed on icons/" + relStr, ioe);
                    continue;
                }
                if (img == null) {
                    logger.severe("[ModIconScanner] " + modName + ": icons/" + relStr
                                  + " is not a readable PNG");
                    continue;
                }
                if (img.getWidth() != REQUIRED_DIM || img.getHeight() != REQUIRED_DIM) {
                    logger.severe("[ModIconScanner] " + modName + ": icons/" + relStr
                                  + " is " + img.getWidth() + "x" + img.getHeight()
                                  + ", must be " + REQUIRED_DIM + "x" + REQUIRED_DIM);
                    continue;
                }

                String stringId = modName + ":" + relNoExt;
                String packUri = "pack:framework-icons/" + modName + "/" + relStr;

                short allocated;
                try {
                    allocated = IconRegistry.register(stringId, packUri);
                } catch (IllegalStateException ise) {
                    logger.log(Level.SEVERE,
                        "[ModIconScanner] icon registry full while registering " + stringId, ise);
                    return;
                }

                out.add(new ScannedIcon(modName, relStr, bytes, allocated));
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "[ModIconScanner] Failed to walk " + iconsDir, e);
        }
    }

    /**
     * Buffered scan result. The {@code bytes} payload is the raw PNG file —
     * the synthesizer writes it verbatim into the server-pack JAR.
     */
    public static final class ScannedIcon {
        public final String modName;
        /** Forward-slash-separated path under {@code icons/}, including {@code .png}. */
        public final String relPath;
        public final byte[] bytes;
        /** Wire-protocol short allocated by {@link IconRegistry#register(String, String)}. */
        public final short allocatedShort;
        /** Canonical {@code <modname>:<relpath without .png>} identifier. */
        public final String stringId;

        ScannedIcon(String modName, String relPath, byte[] bytes, short allocatedShort) {
            this.modName = modName;
            this.relPath = relPath;
            this.bytes = bytes;
            this.allocatedShort = allocatedShort;
            this.stringId = modName + ":"
                + (relPath.endsWith(".png")
                    ? relPath.substring(0, relPath.length() - ".png".length())
                    : relPath);
        }
    }
}
