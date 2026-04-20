package com.garward.wurmmodloader.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gotti.wurmunlimited.modloader.interfaces.ModEntry;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.garward.wurmmodloader.api.httpserver.ModHttpServer;

/**
 * Framework singleton implementing {@link ModHttpServer}.
 *
 * <p>Lifecycle is owned by {@link HttpServerSubsystem}: {@code configure()} runs at
 * framework boot, {@code start()} fires on {@code ServerStartedEvent},
 * {@code stop()} on {@code ServerStoppingEvent}. A JVM shutdown hook is installed
 * as a last-resort backstop so the port is released even on hard exit.
 *
 * @since 1.0.0
 */
public class ModHttpServerImpl implements ModHttpServer {

    private static class Handler {
        Pattern modName;
        Pattern pattern;
        Function<String, InputStream> handler;
    }

    private static final Logger logger = Logger.getLogger(ModHttpServerImpl.class.getName());

    private static ModHttpServerImpl instance;

    private Map<Object, String> modEntries = new HashMap<>();
    private List<Handler> handlers = new ArrayList<>();

    private int port = 0;
    private int publicServerPort = 0;
    private String publicServerAddress = null;
    private String internalServerAddress = null;
    private int maxThreads = 50;
    private boolean allowEphemeralPort = true;

    private volatile PackServer packServer;

    public static synchronized ModHttpServerImpl getInstance() {
        if (instance == null) {
            instance = new ModHttpServerImpl();
        }
        return instance;
    }

    public void addModEntry(ModEntry<?> entry) {
        modEntries.put(entry.getWurmMod(), entry.getName());
    }

    /** Register a mod by raw instance + name. Works for either ModEntry flavor. */
    public void addModEntry(Object wurmMod, String name) {
        if (wurmMod != null && name != null) {
            modEntries.put(wurmMod, name);
        }
    }

    /**
     * Configure the server from a typed {@link HttpServerConfig}.
     */
    public void configure(HttpServerConfig cfg) {
        this.port = cfg.serverPort;
        this.publicServerAddress = cfg.publicServerAddress;
        this.publicServerPort = cfg.publicServerPort;
        this.internalServerAddress = cfg.internalServerAddress;
        this.maxThreads = cfg.maxThreads;
        this.allowEphemeralPort = cfg.allowEphemeralPort;
    }

    @Override
    public String serve(WurmServerMod mod, Pattern regex, Function<String, InputStream> dataSupplier) {
        String modName = modEntries.get(Objects.requireNonNull(mod, "mod must not be null"));
        if (modName == null) {
            logger.log(Level.WARNING, String.format("Mod %s is unknown", mod));
            return null;
        }
        return addHandler(modName, regex, dataSupplier);
    }

    /**
     * Serve content for a mod by name (event-based API).
     */
    public String serve(String modName, Pattern regex, Function<String, InputStream> dataSupplier) {
        return addHandler(modName, regex, dataSupplier);
    }

    private String addHandler(String modName, Pattern regex, Function<String, InputStream> dataSupplier) {
        Handler handler = new Handler();
        handler.modName = Pattern.compile("^/" + Pattern.quote(modName) + "(?<path>(/.*|$))");
        handler.pattern = regex;
        handler.handler = dataSupplier;
        this.handlers.add(handler);
        return "/" + modName + "/";
    }

    @Override
    public String serve(WurmServerMod mod, Pattern regex, Supplier<InputStream> dataSupplier) {
        return serve(mod, regex, path -> dataSupplier.get());
    }

    private InputStream readFile(Path file) {
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String serve(WurmServerMod mod, Pattern regex, Path file) {
        return serve(mod, regex, path -> readFile(file));
    }

    /**
     * Start the HTTP server with retry + optional ephemeral-port fallback.
     *
     * <p>Port-in-use hardening: up to 3 attempts on the configured port with
     * exponentially-backed-off waits (500 / 1000 / 2000 ms). If the port is still
     * held and {@code allowEphemeralPort=true}, one final attempt is made with
     * port 0. The actual bound URL is always published via {@link #getUri()} so
     * downstream consumers don't need to know the port.
     */
    public synchronized void start() throws IOException {
        if (this.packServer != null) {
            logger.info("[HttpServer] Already running, skipping start");
            return;
        }

        int[] backoffMs = {500, 1000, 2000};
        IOException lastError = null;

        for (int attempt = 0; attempt < backoffMs.length; attempt++) {
            try {
                logger.info("[HttpServer] Start attempt " + (attempt + 1)
                        + "/" + backoffMs.length + " on port " + port);
                this.packServer = createServer(port);
                logger.info("[HttpServer] Started on bound port " + packServer.getBoundPort());
                return;
            } catch (BindException e) {
                lastError = e;
                logger.warning("[HttpServer] Port " + port + " unavailable (attempt "
                        + (attempt + 1) + "): " + e.getMessage());
                sleepQuiet(backoffMs[attempt]);
            } catch (IOException e) {
                // Not a bind error — rethrow immediately.
                throw e;
            }
        }

        if (allowEphemeralPort && port != 0) {
            logger.warning("[HttpServer] Falling back to ephemeral port after "
                    + backoffMs.length + " failed attempts on port " + port);
            try {
                this.packServer = createServer(0);
                logger.warning("[HttpServer] Started on ephemeral port " + packServer.getBoundPort()
                        + " (consumers should resolve URL via httpserver:get_uri)");
                return;
            } catch (IOException e) {
                lastError = e;
            }
        }

        logger.log(Level.SEVERE, "[HttpServer] Failed to start after retries", lastError);
        throw lastError != null ? lastError : new IOException("HTTP server failed to start");
    }

    private PackServer createServer(int bindPort) throws IOException {
        return new PackServer(bindPort, publicServerAddress, publicServerPort,
                internalServerAddress, maxThreads) {
            @Override
            protected InputStream getStream(String path) {
                return handle(path);
            }
        };
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop the HTTP server. Idempotent — safe to call from both the normal
     * shutdown-event path and the JVM shutdown hook.
     */
    public synchronized void stop() throws IOException {
        if (this.packServer != null) {
            try {
                this.packServer.stop();
            } finally {
                this.packServer = null;
            }
        }
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    private InputStream handle(String path) {
        for (Handler handler : handlers) {
            final Matcher matcher = handler.modName.matcher(path);
            if (!matcher.matches()) {
                continue;
            }

            final String subpath = matcher.group("path");
            final Matcher subpathMatcher = handler.pattern.matcher(subpath);
            if (subpathMatcher.matches()) {
                String p = subpath;
                try {
                    p = subpathMatcher.group("path");
                } catch (IllegalArgumentException e) {
                    // named group "path" absent — use full subpath
                }
                return handler.handler.apply(p);
            }
        }
        return null;
    }

    @Override
    public boolean isRunning() {
        return packServer != null;
    }

    @Override
    public URI getUri() throws URISyntaxException {
        if (packServer != null) {
            return packServer.getUri();
        }
        return null;
    }
}
