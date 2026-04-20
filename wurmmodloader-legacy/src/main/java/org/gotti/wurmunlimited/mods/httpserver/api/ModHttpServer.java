package org.gotti.wurmunlimited.mods.httpserver.api;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

/**
 * Legacy bridge for {@code org.gotti.wurmunlimited.mods.httpserver.api.ModHttpServer}.
 *
 * <p>The HTTP server was promoted from a mod into a framework subsystem. This interface
 * exists solely for binary compatibility with community mods built against the legacy
 * ago package path — it delegates to the modern framework-API interface
 * {@link com.garward.wurmmodloader.api.httpserver.ModHttpServer}.
 *
 * <p>New mods should use {@link com.garward.wurmmodloader.api.httpserver.ModHttpServer}
 * directly, or the event-based API ({@code ModQueryEvent}/{@code ModActionEvent}).
 *
 * @since 1.0.0
 */
public interface ModHttpServer {

    String serve(WurmServerMod mod, Pattern regex, Function<String, InputStream> dataSupplier);

    String serve(WurmServerMod mod, Pattern regex, Supplier<InputStream> dataSupplier);

    String serve(WurmServerMod mod, Pattern regex, Path file);

    boolean isRunning();

    URI getUri() throws URISyntaxException;

    /**
     * Get an instance wrapping the framework HTTP server.
     */
    static ModHttpServer getInstance() {
        final com.garward.wurmmodloader.api.httpserver.ModHttpServer delegate =
                com.garward.wurmmodloader.api.httpserver.ModHttpServer.getInstance();
        return new ModHttpServer() {
            @Override
            public String serve(WurmServerMod mod, Pattern regex, Function<String, InputStream> dataSupplier) {
                return delegate.serve(mod, regex, dataSupplier);
            }

            @Override
            public String serve(WurmServerMod mod, Pattern regex, Supplier<InputStream> dataSupplier) {
                return delegate.serve(mod, regex, dataSupplier);
            }

            @Override
            public String serve(WurmServerMod mod, Pattern regex, Path file) {
                return delegate.serve(mod, regex, file);
            }

            @Override
            public boolean isRunning() {
                return delegate.isRunning();
            }

            @Override
            public URI getUri() throws URISyntaxException {
                return delegate.getUri();
            }
        };
    }
}
