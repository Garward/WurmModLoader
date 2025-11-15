package com.garward.wurmmodloader.core.icon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Server startup hook for automatic icon pack generation.
 *
 * <p>This class provides integration points for generating icon packs during
 * server startup. It should be called after all mods have registered their icons
 * but before the server starts accepting player connections.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // In server startup sequence, after item templates created:
 * IconPackServerHook.generateIconPacksOnStartup();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class IconPackServerHook {

    private static final Logger logger = Logger.getLogger(IconPackServerHook.class.getName());
    private static boolean generatedThisStartup = false;

    /**
     * Private constructor to prevent instantiation.
     */
    private IconPackServerHook() {
        throw new AssertionError("IconPackServerHook should not be instantiated");
    }

    /**
     * Generates icon packs during server startup.
     *
     * <p>This method should be called once during server startup, after all mods
     * have registered their icons. It will:</p>
     * <ol>
     *   <li>Check if any custom icons are registered</li>
     *   <li>Generate sprite sheets and manifest</li>
     *   <li>Save to httpserver directory if available</li>
     *   <li>Log generation results</li>
     * </ol>
     *
     * <p>This method is idempotent - calling it multiple times during startup
     * will only generate packs once.</p>
     *
     * @since 1.0.0
     */
    public static void generateIconPacksOnStartup() {
        if (generatedThisStartup) {
            logger.fine("Icon packs already generated this startup, skipping");
            return;
        }

        generatedThisStartup = true;

        try {
            logger.info("=== Icon Pack Generation (Iconzz Approach) ===");

            // Check if any custom icons registered
            long customIconCount = IconRegistry.countCustomIcons();
            if (customIconCount == 0) {
                logger.info("No custom icons registered, skipping pack generation");
                return;
            }

            logger.info("Found " + customIconCount + " custom icon(s) registered");

            Path outputDir = IconPackGenerator.getDefaultOutputDirectory();
            logger.info("Generating icon packs to: " + outputDir);

            // Generate icon packs (will attempt ServerPacks registration internally)
            long startTime = System.currentTimeMillis();
            IconPackGenerator.generateIconPacks(outputDir);
            long elapsed = System.currentTimeMillis() - startTime;

            logger.info("Icon pack generation completed in " + elapsed + "ms");
            logger.info("Clients can download icon packs from httpserver (if enabled)");

        } catch (Exception e) {
            logger.severe("Failed to generate icon packs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Resets the generation flag (for testing).
     *
     * @since 1.0.0
     */
    static void resetGenerationFlag() {
        generatedThisStartup = false;
    }
}
