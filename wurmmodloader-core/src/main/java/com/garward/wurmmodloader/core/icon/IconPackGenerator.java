package com.garward.wurmmodloader.core.icon;

import com.garward.wurmmodloader.api.icon.Icon;
import com.garward.wurmmodloader.api.registry.ResourceLocation;
import com.garward.wurmmodloader.core.registry.Registries;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Generates icon pack sprite sheets and manifests for client distribution.
 *
 * <p>This class is responsible for extracting custom icon images from mod JARs,
 * compositing them into sprite sheets (20×12 grid, 32×32 icons), and generating
 * manifest files for client-side loading.</p>
 *
 * <p>Icon Pack Structure:</p>
 * <ul>
 *   <li>Sheet 7+: Custom mod icons (IDs 1680+)</li>
 *   <li>240 icons per sheet (20 wide × 12 tall)</li>
 *   <li>32×32 pixel icons</li>
 *   <li>PNG format sprite sheets</li>
 *   <li>JSON manifest with icon mappings</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Generate icon packs after all mods loaded
 * Path outputDir = Paths.get("httpserver/iconpacks");
 * IconPackGenerator.generateIconPacks(outputDir);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class IconPackGenerator {

    private static final Logger logger = Logger.getLogger(IconPackGenerator.class.getName());

    /** Icon dimensions in pixels */
    private static final int ICON_SIZE = 32;

    /** Icons per row in sprite sheet */
    private static final int ICONS_PER_ROW = 20;

    /** Rows per sprite sheet */
    private static final int ROWS_PER_SHEET = 12;

    /** Icons per sprite sheet (20 × 12) */
    private static final int ICONS_PER_SHEET = 240;

    /** First custom icon ID */
    private static final int FIRST_CUSTOM_ICON = Icon.FIRST_CUSTOM_ICON;

    /** Sprite sheet width in pixels */
    private static final int SHEET_WIDTH = ICON_SIZE * ICONS_PER_ROW;

    /** Sprite sheet height in pixels */
    private static final int SHEET_HEIGHT = ICON_SIZE * ROWS_PER_SHEET;

    /** Vanilla sheet names in order (Iconzz pattern) */
    private static final String[] VANILLA_SHEET_NAMES = {
        "icons.png", "misc.png", "resource.png", "tools.png",
        "armor.png", "weapons.png", "resource2.png"
    };

    /** Path to graphics.jar containing vanilla sheets */
    private static final String GRAPHICS_JAR_PATH =
        "../Wurm Unlimited/WurmLauncher/packs/graphics.jar";

    /**
     * Private constructor to prevent instantiation.
     */
    private IconPackGenerator() {
        throw new AssertionError("IconPackGenerator should not be instantiated");
    }

    /**
     * Generates icon packs from all registered custom icons using Iconzz approach.
     *
     * <p>This method follows the Iconzz pattern:</p>
     * <ol>
     *   <li>Loads ALL 7 vanilla icon sheets from graphics.jar</li>
     *   <li>Composites custom icons onto vanilla sheets at calculated positions</li>
     *   <li>Saves all 7 modified sheets to JAR</li>
     *   <li>Generates mappings.txt for client loading</li>
     * </ol>
     *
     * @param outputDir directory to save generated icon packs
     * @throws IOException if file I/O fails
     * @since 1.0.0
     */
    public static void generateIconPacks(Path outputDir) throws IOException {
        logger.info("Starting icon pack generation (Iconzz approach)...");

        // Create output directory
        Files.createDirectories(outputDir);

        // Get all custom icons from registry
        List<Icon> customIcons = Registries.ICONS.values()
            .filter(Icon::isCustom)
            .sorted(Comparator.comparingInt(Icon::getIconId))
            .collect(Collectors.toList());

        if (customIcons.isEmpty()) {
            logger.info("No custom icons registered, skipping pack generation");
            return;
        }

        logger.info("Found " + customIcons.size() + " custom icons to pack");

        // Load all 7 vanilla sheets from graphics.jar
        BufferedImage[] vanillaSheets = loadVanillaSheets();
        if (vanillaSheets == null) {
            logger.severe("Failed to load vanilla sheets, cannot generate icon pack");
            return;
        }

        // Create Graphics2D contexts for compositing
        Graphics2D[] graphics = new Graphics2D[vanillaSheets.length];
        for (int i = 0; i < vanillaSheets.length; i++) {
            graphics[i] = vanillaSheets[i].createGraphics();
            graphics[i].setBackground(new Color(0, 0, 0, 0));
        }

        // Composite each custom icon onto the appropriate vanilla sheet
        int iconsComposited = 0;
        for (Icon icon : customIcons) {
            try {
                int iconId = icon.getIconId();
                int sheetIndex = iconId / ICONS_PER_SHEET;  // Which of the 7 sheets
                int xOffset = (iconId % ICONS_PER_ROW) * ICON_SIZE;
                int yOffset = ((iconId % ICONS_PER_SHEET) / ICONS_PER_ROW) * ICON_SIZE;

                if (sheetIndex >= 0 && sheetIndex < graphics.length) {
                    BufferedImage customImage = loadIconImage(icon);
                    if (customImage != null) {
                        Graphics2D g = graphics[sheetIndex];
                        g.clearRect(xOffset, yOffset, ICON_SIZE, ICON_SIZE);
                        g.drawImage(customImage, xOffset, yOffset, ICON_SIZE, ICON_SIZE, null);
                        iconsComposited++;
                        logger.fine("Composited " + icon.getId() + " at sheet " + sheetIndex +
                                   " position (" + xOffset + "," + yOffset + ")");
                    } else {
                        logger.warning("Missing icon image for " + icon.getId() +
                                     " (packFile: " + icon.getPackFile() + ")");
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to composite icon " + icon.getId() + ": " + e.getMessage());
            }
        }

        // Dispose graphics contexts
        for (Graphics2D g : graphics) {
            if (g != null) g.dispose();
        }

        // Save all 7 sheets to output directory
        for (int i = 0; i < vanillaSheets.length; i++) {
            Path sheetFile = outputDir.resolve(VANILLA_SHEET_NAMES[i]);
            ImageIO.write(vanillaSheets[i], "PNG", sheetFile.toFile());
            vanillaSheets[i].flush();
            logger.fine("Saved " + VANILLA_SHEET_NAMES[i] + " to " + sheetFile);
        }

        // Generate manifest (optional, for debugging)
        generateManifest(customIcons, outputDir);

        // Create JAR file for serverpacks distribution
        try {
            Path jarFile = createIconPackJar(outputDir);
            registerWithServerPacks(jarFile);
        } catch (Exception e) {
            logger.warning("Failed to create/register icon pack JAR: " + e.getMessage());
        }

        logger.info("Icon pack generation complete: " + iconsComposited + "/" + customIcons.size() +
                   " icons composited onto vanilla sheets");
    }

    /**
     * Loads all 7 vanilla icon sheets from graphics.jar.
     *
     * @return array of 7 BufferedImages, or null if loading fails
     */
    private static BufferedImage[] loadVanillaSheets() {
        // Try multiple possible locations for graphics.jar.
        // Override via -Dwurm.graphicsJar=<path> if none of the defaults match.
        String override = System.getProperty("wurm.graphicsJar");
        Path[] possiblePaths = override != null
            ? new Path[] { Paths.get(override) }
            : new Path[] {
                Paths.get(GRAPHICS_JAR_PATH),
                Paths.get("../Wurm Unlimited/WurmLauncher/packs/graphics.jar")
            };

        Path graphicsJar = null;
        for (Path p : possiblePaths) {
            if (Files.exists(p)) {
                graphicsJar = p;
                break;
            }
        }

        if (graphicsJar == null) {
            logger.severe("Could not find graphics.jar in any expected location");
            return null;
        }

        logger.info("Loading vanilla sheets from: " + graphicsJar);

        BufferedImage[] sheets = new BufferedImage[VANILLA_SHEET_NAMES.length];

        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(graphicsJar.toFile())) {
            for (int i = 0; i < VANILLA_SHEET_NAMES.length; i++) {
                String sheetPath = "gui/" + VANILLA_SHEET_NAMES[i];
                java.util.jar.JarEntry entry = jar.getJarEntry(sheetPath);

                if (entry != null) {
                    try (InputStream stream = jar.getInputStream(entry)) {
                        sheets[i] = ImageIO.read(stream);
                        if (sheets[i] != null) {
                            logger.fine("Loaded " + sheetPath + " (" + sheets[i].getWidth() +
                                       "x" + sheets[i].getHeight() + ")");
                        } else {
                            logger.warning("Failed to read " + sheetPath + " from JAR");
                            return null;
                        }
                    }
                } else {
                    logger.warning("Sheet not found in graphics.jar: " + sheetPath);
                    return null;
                }
            }
        } catch (IOException e) {
            logger.severe("Failed to load vanilla sheets: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        logger.info("Successfully loaded all " + sheets.length + " vanilla icon sheets");
        return sheets;
    }

    /**
     * Generates a single sprite sheet from a list of icons.
     *
     * @param sheetNumber the sheet number (7+)
     * @param icons icons to include in this sheet
     * @param outputDir output directory
     * @throws IOException if file I/O fails
     * @deprecated No longer used - Iconzz approach composites onto vanilla sheets instead
     */
    @Deprecated
    private static void generateSpriteSheet(int sheetNumber, List<Icon> icons, Path outputDir)
            throws IOException {
        logger.info("Generating sheet " + sheetNumber + " with " + icons.size() + " icons");

        // Create blank sprite sheet
        BufferedImage sheet = new BufferedImage(
            SHEET_WIDTH,
            SHEET_HEIGHT,
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = sheet.createGraphics();

        // Fill with transparent background
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, SHEET_WIDTH, SHEET_HEIGHT);
        g.setComposite(AlphaComposite.SrcOver);

        // Draw each icon at its position
        int iconsDrawn = 0;
        for (Icon icon : icons) {
            try {
                BufferedImage iconImage = loadIconImage(icon);
                if (iconImage != null) {
                    int x = icon.getColumn() * ICON_SIZE;
                    int y = icon.getRow() * ICON_SIZE;
                    g.drawImage(iconImage, x, y, ICON_SIZE, ICON_SIZE, null);
                    iconsDrawn++;
                } else {
                    logger.warning("Missing icon image for " + icon.getId() +
                                 " (packFile: " + icon.getPackFile() + ")");
                    drawPlaceholder(g, icon);
                }
            } catch (Exception e) {
                logger.warning("Failed to load icon " + icon.getId() + ": " + e.getMessage());
                drawPlaceholder(g, icon);
            }
        }

        g.dispose();

        // Save sprite sheet
        Path outputFile = outputDir.resolve("icon_sheet_" + sheetNumber + ".png");
        ImageIO.write(sheet, "PNG", outputFile.toFile());

        logger.info("Saved sheet " + sheetNumber + " to " + outputFile +
                   " (" + iconsDrawn + "/" + icons.size() + " icons drawn)");
    }

    /**
     * Loads an icon image from a mod's resources or external directories.
     *
     * @param icon the icon to load
     * @return the loaded image, or null if not found
     */
    private static BufferedImage loadIconImage(Icon icon) {
        String packFile = icon.getPackFile();
        ResourceLocation id = icon.getId();

        // Try external file paths first (for mods with icons/ directories)
        Path[] externalPaths = {
            Paths.get("mods/" + id.getNamespace() + "/icons/" + packFile),
            Paths.get("mods/" + id.getNamespace() + "/" + packFile),
            Paths.get("mods/alchemy/icons/" + packFile)  // Legacy Alchemy compatibility
        };

        for (Path path : externalPaths) {
            try {
                if (Files.exists(path)) {
                    BufferedImage image = ImageIO.read(path.toFile());
                    if (image != null) {
                        logger.fine("Loaded icon from external file: " + path);
                        return image;
                    }
                }
            } catch (IOException e) {
                // Try next path
            }
        }

        // Try JAR resource paths
        String[] resourcePaths = {
            // Standard paths
            "icons/" + packFile,
            packFile,

            // Namespaced paths
            id.getNamespace() + "/icons/" + packFile,
            "org/" + id.getNamespace() + "/icons/" + packFile,

            // Alchemy-specific paths (legacy compatibility)
            "org/arathok/wurmunlimited/mods/alchemy/icons/" + packFile,
            "mods/" + id.getNamespace() + "/icons/" + packFile
        };

        for (String path : resourcePaths) {
            try (InputStream stream = IconPackGenerator.class.getClassLoader()
                    .getResourceAsStream(path)) {
                if (stream != null) {
                    BufferedImage image = ImageIO.read(stream);
                    if (image != null) {
                        logger.fine("Loaded icon from JAR resource: " + path);
                        return image;
                    }
                }
            } catch (IOException e) {
                // Try next path
            }
        }

        // Not found in any path
        logger.warning("Icon not found in " + (externalPaths.length + resourcePaths.length) +
                      " search locations: " + packFile);
        return null;
    }

    /**
     * Draws a placeholder icon (question mark) for missing icons.
     *
     * @param g graphics context
     * @param icon the icon to draw placeholder for
     */
    private static void drawPlaceholder(Graphics2D g, Icon icon) {
        int x = icon.getColumn() * ICON_SIZE;
        int y = icon.getRow() * ICON_SIZE;

        // Draw gray background
        g.setColor(new Color(128, 128, 128, 200));
        g.fillRect(x, y, ICON_SIZE, ICON_SIZE);

        // Draw border
        g.setColor(Color.BLACK);
        g.drawRect(x, y, ICON_SIZE - 1, ICON_SIZE - 1);

        // Draw question mark
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        String text = "?";
        int textX = x + (ICON_SIZE - fm.stringWidth(text)) / 2;
        int textY = y + ((ICON_SIZE - fm.getHeight()) / 2) + fm.getAscent();

        g.setColor(Color.WHITE);
        g.drawString(text, textX, textY);
    }

    /**
     * Generates a JSON manifest file with icon mappings.
     *
     * @param icons all custom icons
     * @param outputDir output directory
     * @throws IOException if file I/O fails
     */
    private static void generateManifest(List<Icon> icons, Path outputDir) throws IOException {
        Path manifestFile = outputDir.resolve("icon_manifest.json");

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"1.0\",\n");
        json.append("  \"generator\": \"WurmModLoader IconPackGenerator\",\n");
        json.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n");
        json.append("  \"iconCount\": ").append(icons.size()).append(",\n");
        json.append("  \"icons\": [\n");

        for (int i = 0; i < icons.size(); i++) {
            Icon icon = icons.get(i);
            json.append("    {\n");
            json.append("      \"id\": \"").append(icon.getId()).append("\",\n");
            json.append("      \"iconId\": ").append(icon.getIconId()).append(",\n");
            json.append("      \"sheet\": ").append(icon.getSheetNumber()).append(",\n");
            json.append("      \"row\": ").append(icon.getRow()).append(",\n");
            json.append("      \"column\": ").append(icon.getColumn()).append(",\n");
            json.append("      \"packFile\": \"").append(icon.getPackFile()).append("\",\n");
            json.append("      \"type\": \"").append(icon.getType()).append("\"\n");
            json.append("    }");
            if (i < icons.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.write(manifestFile, json.toString().getBytes());
        logger.info("Generated manifest: " + manifestFile);
    }

    /**
     * Checks if httpserver mod is available for icon distribution.
     *
     * @return true if httpserver is available
     */
    public static boolean isHttpServerAvailable() {
        // HTTP server is now a framework subsystem (always present on the classpath).
        // Report actual runtime state so callers can skip icon-pack publishing if the
        // server failed to bind.
        try {
            Class<?> implClass = Class.forName("com.garward.wurmmodloader.httpserver.ModHttpServerImpl");
            Object impl = implClass.getMethod("getInstance").invoke(null);
            return (Boolean) implClass.getMethod("isRunning").invoke(impl);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /**
     * Gets the default output directory for icon packs.
     * Uses httpserver directory if available, otherwise uses local directory.
     *
     * @return the output directory path
     */
    public static Path getDefaultOutputDirectory() {
        // Try httpserver directory first
        Path httpserverDir = Paths.get("httpserver/iconpacks");
        if (Files.exists(httpserverDir.getParent())) {
            return httpserverDir;
        }

        // Fallback to local directory
        return Paths.get("iconpacks");
    }

    /**
     * Creates a JAR file containing all vanilla icon sheets for client distribution (Iconzz approach).
     *
     * <p>This creates a complete graphics.jar replacement with custom icons baked in.
     * The client will download this as "graphics.jar" and it will replace the vanilla graphics pack.</p>
     *
     * @param outputDir directory containing generated icon sheets
     * @return path to the created JAR file
     * @throws IOException if JAR creation fails
     */
    private static Path createIconPackJar(Path outputDir) throws IOException {
        // Create graphics.jar (replaces vanilla) instead of iconpack.jar
        Path jarFile = outputDir.resolve("graphics.jar");

        try (JarOutputStream jar = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(jarFile.toFile())))) {

            // Collect sheet mappings for mappings.txt
            StringBuilder mappings = new StringBuilder();

            // Add all 7 vanilla sheets to JAR (Iconzz approach)
            for (String sheetName : VANILLA_SHEET_NAMES) {
                Path sheetFile = outputDir.resolve(sheetName);

                if (Files.exists(sheetFile)) {
                    try {
                        // Add to JAR as gui/sheetname.png (matching vanilla mappings.txt)
                        String jarPath = "gui/" + sheetName;

                        jar.putNextEntry(new JarEntry(jarPath));
                        Files.copy(sheetFile, jar);
                        jar.closeEntry();

                        // Add to mappings.txt with tab formatting (matching vanilla format)
                        // Extract sheet property name (icons.png -> icons)
                        String propertyName = sheetName.replace(".png", "");
                        mappings.append("img.iconsheet.").append(propertyName)
                            .append("             \t= ").append(jarPath).append("\n");

                        logger.fine("Added " + sheetName + " to JAR as " + jarPath);
                    } catch (IOException e) {
                        logger.warning("Failed to add " + sheetName + " to JAR: " + e.getMessage());
                    }
                } else {
                    logger.warning("Sheet file not found: " + sheetFile);
                }
            }

            // Add mappings.txt to JAR root
            if (mappings.length() > 0) {
                jar.putNextEntry(new JarEntry("mappings.txt"));
                jar.write(mappings.toString().getBytes());
                jar.closeEntry();
                logger.info("Added mappings.txt with " + VANILLA_SHEET_NAMES.length + " icon sheet mappings");
            }
        }

        logger.info("Created icon pack JAR: " + jarFile + " (" + Files.size(jarFile) + " bytes)");
        return jarFile;
    }

    /**
     * Registers the icon pack JAR with serverpacks for automatic client distribution.
     *
     * @param jarFile path to the icon pack JAR
     */
    private static void registerWithServerPacks(Path jarFile) {
        // Use bridge to register with ServerPacks (handles classloader isolation)
        // Bridge will register immediately if ServerPacks is available,
        // or defer until ServerPacks loads via ModListener callback
        IconPackServerPacksBridge.registerIconPack(jarFile);
    }
}
