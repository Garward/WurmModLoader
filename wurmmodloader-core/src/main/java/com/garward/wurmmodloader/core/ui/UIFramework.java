package com.garward.wurmmodloader.core.ui;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.core.event.EventBus;

import java.util.logging.Logger;

/**
 * Main entry point for the UI framework.
 *
 * <p>This class handles initialization of the UI system components:
 * <ul>
 *   <li>ContextMenuRegistry - Context menu management</li>
 *   <li>WindowManager - Window display system</li>
 * </ul>
 *
 * <p>The framework is automatically initialized when the server starts.</p>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public class UIFramework {

    private static final Logger logger = Logger.getLogger(UIFramework.class.getName());
    private static volatile boolean initialized = false;

    /**
     * Initializes the UI framework.
     * This is called automatically by the framework during server startup.
     */
    public static synchronized void initialize() {
        if (initialized) {
            logger.warning("UIFramework already initialized");
            return;
        }

        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("Initializing WurmModLoader UI Framework...");
        logger.info("═══════════════════════════════════════════════════════════════");

        try {
            // Initialize ContextMenuRegistry
            ContextMenuRegistry.getInstance().initialize();
            logger.info("✓ ContextMenuRegistry initialized");

            // WindowManager is stateless, no initialization needed
            logger.info("✓ WindowManager ready");

            initialized = true;

            logger.info("");
            logger.info("UI Framework Components:");
            logger.info("  • UIWindow - Fluent BML window builder");
            logger.info("  • WindowManager - Simplified window display");
            logger.info("  • ContextMenuRegistry - Dynamic menu registration");
            logger.info("  • MenuEntry - Context menu builder with visibility filters");
            logger.info("");
            logger.info("WurmModLoader UI Framework - Active");
            logger.info("═══════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            logger.severe("Failed to initialize UI Framework: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("UI Framework initialization failed", e);
        }
    }

    /**
     * Checks if the UI framework has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Auto-initialization listener.
     * This is registered by the framework core and ensures the UI system
     * is ready before mods start using it.
     */
    public static class AutoInitializer {

        private static volatile boolean registered = false;

        /**
         * Registers the auto-initializer with the event bus.
         * This should be called early in the framework bootstrap.
         */
        public static synchronized void register() {
            if (registered) {
                return;
            }

            EventBus.getInstance().register(new AutoInitializer());
            registered = true;
            logger.info("UI Framework auto-initializer registered");
        }

        @SubscribeEvent
        public void onServerStarted(ServerStartedEvent event) {
            UIFramework.initialize();
        }
    }
}
