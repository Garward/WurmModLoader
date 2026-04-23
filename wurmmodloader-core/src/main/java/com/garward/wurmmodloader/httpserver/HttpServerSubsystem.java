package com.garward.wurmmodloader.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.api.events.ModQueryEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.server.ServerStoppingEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.modloader.interfaces.ModEntry;

/**
 * Framework-owned HTTP server subsystem. Replaces the legacy {@code HttpServerMod}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Load {@link HttpServerConfig} from {@code wurmmodloader-http.properties}.</li>
 *   <li>Configure {@link ModHttpServerImpl} singleton and register on the event bus.</li>
 *   <li>Subscribe to {@link ServerStartedEvent} / {@link ServerStoppingEvent} to drive
 *       start/stop.</li>
 *   <li>Serve the event-based public API ({@code httpserver:get_uri},
 *       {@code httpserver:is_running}, {@code httpserver:register_endpoint}).</li>
 *   <li>Install a JVM shutdown hook as a last-resort backstop for port release on
 *       crash / kill -9.</li>
 * </ul>
 *
 * <p>Installed from {@link com.garward.wurmmodloader.serverlauncher.DelegatedLauncher}
 * alongside other framework subsystems.
 *
 * @since 1.0.0
 */
public final class HttpServerSubsystem {

    private static final Logger logger = Logger.getLogger(HttpServerSubsystem.class.getName());
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static HttpServerSubsystem instance;

    private HttpServerSubsystem() {}

    /**
     * Install the subsystem. Idempotent.
     *
     * <p>Must be called AFTER the event bus exists (post-ServerHook) and BEFORE the
     * port actually binds (binding is deferred to ServerStartedEvent).
     */
    public static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            logger.fine("[HttpServer] Subsystem already installed, skipping");
            return;
        }

        logger.info("[HttpServer] Installing subsystem...");

        // 1. Load config
        Path configPath = resolveConfigPath();
        HttpServerConfig cfg = HttpServerConfig.loadFromProperties(configPath);

        // 2. Configure impl
        ModHttpServerImpl impl = ModHttpServerImpl.getInstance();
        impl.configure(cfg);

        // 3. Register on event bus (handlers fire via @SubscribeEvent)
        instance = new HttpServerSubsystem();
        EventBus.getInstance().register(instance);

        // 4. Last-resort JVM shutdown hook — ensures port release even on crash/kill -9.
        // Idempotent with the normal ServerStoppingEvent path.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (impl.isRunning()) {
                    logger.info("[HttpServer] JVM shutdown hook: releasing port");
                    impl.stop();
                }
            } catch (IOException e) {
                // best-effort; do not throw from shutdown hook
                logger.log(Level.WARNING, "[HttpServer] Shutdown hook stop failed", e);
            }
        }, "HttpServerSubsystem-ShutdownHook"));

        logger.info("[HttpServer] Subsystem installed");
    }

    /**
     * Register a mod entry so {@link com.garward.wurmmodloader.api.httpserver.ModHttpServer#serve}
     * calls that pass a WurmServerMod can resolve the mod's name. Called by ModLoader
     * after each mod's {@code init()}.
     */
    public static void register(ModEntry<?> entry) {
        if (entry == null) return;
        ModHttpServerImpl.getInstance().addModEntry(entry.getWurmMod(), entry.getName());
    }

    private static Path resolveConfigPath() {
        String serverDir = System.getProperty("wurmmodloader.serverdir");
        if (serverDir == null || serverDir.isEmpty()) {
            serverDir = System.getProperty("user.dir", ".");
        }
        return Paths.get(serverDir).resolve("config").resolve("wurmmodloader-http.properties");
    }

    // === Event subscribers ===

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            ModHttpServerImpl.getInstance().start();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[HttpServer] Failed to start: " + e.getMessage(), e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        try {
            ModHttpServerImpl.getInstance().stop();
        } catch (IOException e) {
            logger.log(Level.WARNING, "[HttpServer] Error during stop: " + e.getMessage(), e);
        }
    }

    /**
     * Handle queries from other mods about HTTP server state.
     *
     * <p>Supported query types:
     * <ul>
     *   <li>{@code httpserver:get_uri} → sets "uri" to the base URI.</li>
     *   <li>{@code httpserver:is_running} → sets "running" to a Boolean.</li>
     * </ul>
     */
    @SubscribeEvent
    public void onModQuery(ModQueryEvent event) {
        try {
            String type = event.getEventType();
            if ("httpserver:get_uri".equals(type)) {
                try {
                    event.set("uri", ModHttpServerImpl.getInstance().getUri());
                    event.setHandled(true);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error getting HTTP server URI", e);
                }
            } else if ("httpserver:is_running".equals(type)) {
                event.set("running", ModHttpServerImpl.getInstance().isRunning());
                event.setHandled(true);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling ModQueryEvent: " + event.getEventType(), e);
        }
    }

    /**
     * Handle actions from other mods.
     *
     * <p>Supported action types:
     * <ul>
     *   <li>{@code httpserver:register_endpoint} — inputs {@code modName (String)},
     *       {@code pattern (Pattern)}, {@code handler (Function<String, InputStream>)};
     *       output {@code prefix (String)}.</li>
     * </ul>
     */
    @SubscribeEvent
    public void onModAction(ModActionEvent event) {
        try {
            if ("httpserver:register_endpoint".equals(event.getEventType())) {
                String modName = event.getString("modName");
                Pattern pattern = (Pattern) event.get("pattern");
                @SuppressWarnings("unchecked")
                Function<String, InputStream> handler = (Function<String, InputStream>) event.get("handler");

                if (modName == null || pattern == null || handler == null) {
                    logger.warning("httpserver:register_endpoint requires modName, pattern, and handler");
                    return;
                }

                String prefix = ModHttpServerImpl.getInstance().serve(modName, pattern, handler);
                event.set("prefix", prefix);
                event.setHandled(true);

                logger.info("Registered HTTP endpoint for mod '" + modName + "' at prefix: " + prefix);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error handling ModActionEvent: " + event.getEventType(), e);
        }
    }
}
