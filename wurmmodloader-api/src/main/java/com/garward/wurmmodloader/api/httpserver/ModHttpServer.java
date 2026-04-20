package com.garward.wurmmodloader.api.httpserver;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

/**
 * Public API for the framework-owned HTTP server subsystem.
 *
 * <p>The HTTP server is now a first-class framework subsystem (see
 * {@code com.garward.wurmmodloader.httpserver.HttpServerSubsystem}) rather than a mod.
 * Mods should obtain an instance via {@link #getInstance()} and register endpoints
 * through {@link #serve}, or alternatively use the event-based API:
 * {@code ModQueryEvent("httpserver:get_uri")},
 * {@code ModQueryEvent("httpserver:is_running")},
 * {@code ModActionEvent("httpserver:register_endpoint")}.
 *
 * <p>Legacy mods that compiled against
 * {@code org.gotti.wurmunlimited.mods.httpserver.api.ModHttpServer} continue to work
 * via a thin bridge — that interface now extends this one.
 *
 * @since 1.0.0
 */
public interface ModHttpServer {

    /**
     * Serve data under /modname/path.
     *
     * @param mod Mod to serve the URL for. The modname will be used as an URL prefix.
     * @param regex Regex to match "path".
     * @param dataSupplier Supplies an {@link InputStream} for "path". If "regex" contains
     *                     a named group "path" (e.g. {@code (?<path>.*)}) then the group's
     *                     content will be used as the parameter instead.
     * @return "/modname/" prefix or null.
     */
    String serve(WurmServerMod mod, Pattern regex, Function<String, InputStream> dataSupplier);

    /**
     * Serve data under /modname/path.
     *
     * @param mod Mod to serve the URL for. The modname will be used as an URL prefix.
     * @param regex Regex to match "path".
     * @param dataSupplier Supplies an {@link InputStream}.
     * @return "/modname/" prefix or null.
     */
    String serve(WurmServerMod mod, Pattern regex, Supplier<InputStream> dataSupplier);

    /**
     * Serve a file under /modname/path.
     *
     * @param mod Mod to serve the URL for. The modname will be used as an URL prefix.
     * @param regex Regex to match "path".
     * @param file File to serve.
     * @return "/modname/" prefix or null.
     */
    String serve(WurmServerMod mod, Pattern regex, Path file);

    /**
     * Check if the HTTP server is running.
     *
     * @return true if it is running.
     */
    boolean isRunning();

    /**
     * Get the server base URI.
     *
     * @return Server base URI.
     * @throws URISyntaxException if the URI cannot be constructed.
     */
    URI getUri() throws URISyntaxException;

    /**
     * Get an instance of the ModHttpServer.
     *
     * @return instance of ModHttpServer.
     */
    static ModHttpServer getInstance() {
        try {
            Class<?> implClass = Class.forName("com.garward.wurmmodloader.httpserver.ModHttpServerImpl");
            return (ModHttpServer) implClass.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "HttpServerSubsystem not initialized — is wurmmodloader-core on the classpath?", e);
        }
    }
}
