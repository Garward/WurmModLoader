package com.garward.wurmmodloader.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Typed configuration for the framework HTTP server subsystem.
 *
 * <p>Loaded from {@code config/wurmmodloader-http.properties} under the server root
 * (alongside vanilla Wurm's {@code config/logging.properties}). If the file is absent,
 * sensible defaults are used — stock deployments continue to work without any new
 * config file.
 *
 * @since 1.0.0
 */
public final class HttpServerConfig {

    private static final Logger logger = Logger.getLogger(HttpServerConfig.class.getName());

    /** Configured bind port. 0 = pick ephemeral. */
    public final int serverPort;
    /** Announced host in public URLs (null = use bind host). */
    public final String publicServerAddress;
    /** Announced port in public URLs. 0 = use bound port. */
    public final int publicServerPort;
    /** Interface to bind to (null = server's external IP). */
    public final String internalServerAddress;
    /** Max concurrent downloads. */
    public final int maxThreads;
    /** If true, fall back to an ephemeral port after {@code serverPort} exhausts retries. */
    public final boolean allowEphemeralPort;

    private HttpServerConfig(int serverPort, String publicServerAddress, int publicServerPort,
                             String internalServerAddress, int maxThreads,
                             boolean allowEphemeralPort) {
        this.serverPort = serverPort;
        this.publicServerAddress = publicServerAddress;
        this.publicServerPort = publicServerPort;
        this.internalServerAddress = internalServerAddress;
        this.maxThreads = maxThreads;
        this.allowEphemeralPort = allowEphemeralPort;
    }

    public static HttpServerConfig defaults() {
        return new HttpServerConfig(0, null, 0, null, 50, true);
    }

    /**
     * Load from the given properties file. Missing file → defaults. Malformed file
     * → log a warning and fall back to defaults (do not fail boot).
     */
    public static HttpServerConfig loadFromProperties(Path propertiesFile) {
        if (propertiesFile == null || !Files.exists(propertiesFile)) {
            logger.info("[HttpServerConfig] No config/wurmmodloader-http.properties found; using defaults");
            return defaults();
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile)) {
            props.load(in);
        } catch (IOException e) {
            logger.log(Level.WARNING, "[HttpServerConfig] Failed to read " + propertiesFile + "; using defaults", e);
            return defaults();
        }

        return fromProperties(props);
    }

    public static HttpServerConfig fromProperties(Properties props) {
        int serverPort = parseInt(props, "serverPort", 0);
        int publicServerPort = parseInt(props, "publicServerPort", 0);
        String publicServerAddress = trimOrNull(props.getProperty("publicServerAddress"));
        String internalServerAddress = trimOrNull(props.getProperty("internalServerAddress"));
        int maxThreads = parseInt(props, "maxThreads", 50);
        boolean allowEphemeralPort = Boolean.parseBoolean(
                props.getProperty("allowEphemeralPort", "true"));

        HttpServerConfig cfg = new HttpServerConfig(serverPort, publicServerAddress,
                publicServerPort, internalServerAddress, maxThreads, allowEphemeralPort);
        logger.info("[HttpServerConfig] " + cfg);
        return cfg;
    }

    private static int parseInt(Properties props, String key, int defaultValue) {
        String v = props.getProperty(key);
        if (v == null || v.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            logger.warning("[HttpServerConfig] Invalid integer for " + key + "=" + v + "; using " + defaultValue);
            return defaultValue;
        }
    }

    private static String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public String toString() {
        return "HttpServerConfig{serverPort=" + serverPort
                + ", publicServerAddress=" + publicServerAddress
                + ", publicServerPort=" + publicServerPort
                + ", internalServerAddress=" + internalServerAddress
                + ", maxThreads=" + maxThreads
                + ", allowEphemeralPort=" + allowEphemeralPort + "}";
    }
}
