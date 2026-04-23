package com.garward.wurmmodloader.httpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.wurmonline.server.Server;

/**
 * Thin wrapper around JDK's built-in {@link HttpServer} with port-in-use hardening.
 *
 * <p>Historical issue: {@code BindException: Address already in use} on restart
 * because the previous JVM's socket lingered in TIME_WAIT. We harden via:
 * <ol>
 *   <li>Best-effort SO_REUSEADDR on the underlying ServerSocketChannel (via reflection;
 *       swallowed if it fails — not load-bearing).</li>
 *   <li>Retry with backoff at the call site ({@link ModHttpServerImpl#start()}).</li>
 *   <li>Immediate {@code stop(0)} on shutdown for fast socket release.</li>
 * </ol>
 *
 * @since 1.0.0
 */
@SuppressWarnings("restriction")
public abstract class PackServer {

    private static final Logger logger = Logger.getLogger(PackServer.class.getName());

    private final HttpServer httpServer;

    private final String publicServerAddress;
    private final int publicServerPort;

    private final ExecutorService executor;

    public PackServer(int port, String publicServerAddress, int publicServerPort,
                      String internalServerAddress, int maxThreads) throws IOException {

        this.publicServerAddress = publicServerAddress;
        this.publicServerPort = publicServerPort;

        InetAddress addr;
        if (internalServerAddress == null) {
            addr = InetAddress.getByAddress(Server.getInstance().getExternalIp());
        } else {
            addr = InetAddress.getByName(internalServerAddress);
        }

        executor = new ThreadPoolExecutor(0, maxThreads, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());

        InetSocketAddress address = new InetSocketAddress(addr, port);

        // Create unbound, attempt SO_REUSEADDR, then bind — belt-and-braces against
        // TIME_WAIT lingering from a previous JVM.
        httpServer = HttpServer.create();
        tryEnableReuseAddr(httpServer);
        httpServer.bind(address, 0);

        httpServer.setExecutor(executor);
        httpServer.createContext("/", new HttpHandler() {

            @Override
            public void handle(HttpExchange paramHttpExchange) throws IOException {
                String path = paramHttpExchange.getRequestURI().toString();

                logger.fine("Got request " + path);

                try (InputStream stream = getStream(paramHttpExchange.getRequestURI().getPath())) {
                    if (stream != null) {
                        paramHttpExchange.getResponseHeaders().add("Cache-control", "max-age=31556926");
                        paramHttpExchange.sendResponseHeaders(200, 0);
                        try (OutputStream os = paramHttpExchange.getResponseBody()) {
                            int n = 0;
                            byte[] buffer = new byte[8192];
                            while (n != -1) {
                                n = stream.read(buffer);
                                if (n > 0) {
                                    os.write(buffer, 0, n);
                                }
                            }
                        }
                        return;
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
                paramHttpExchange.sendResponseHeaders(404, -1);
            }
        });

        httpServer.start();
        logger.info("[HttpServer] Bound to " + addr + ":" + httpServer.getAddress().getPort());
    }

    /**
     * Attempt to enable SO_REUSEADDR on the underlying ServerSocketChannel.
     *
     * <p>{@link HttpServer} does not expose socket options directly; we reflect to the
     * internal server socket channel. If the reflection fails (different JDK version,
     * module access denied, etc.) we silently fall back — the retry loop at the caller
     * is the real safety net.
     */
    private static void tryEnableReuseAddr(HttpServer server) {
        try {
            // Walk declared fields looking for a ServerSocketChannel.
            Class<?> clazz = server.getClass();
            while (clazz != null) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (ServerSocketChannel.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        ServerSocketChannel ch = (ServerSocketChannel) f.get(server);
                        if (ch != null) {
                            ch.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);
                            logger.fine("[HttpServer] SO_REUSEADDR enabled on server socket");
                            return;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
            logger.fine("[HttpServer] Could not locate ServerSocketChannel for SO_REUSEADDR; relying on retry backoff");
        } catch (Throwable t) {
            logger.log(Level.FINE, "[HttpServer] SO_REUSEADDR reflection failed (non-fatal): " + t.getMessage());
        }
    }

    protected abstract InputStream getStream(String path) throws IOException;

    public URI getUri() throws URISyntaxException {
        String address = publicServerAddress;
        if (publicServerAddress == null) {
            address = httpServer.getAddress().getHostString();
        }

        int port = publicServerPort;
        if (port == 0) {
            port = httpServer.getAddress().getPort();
        }

        return new URI("http", null, address, port, "/", null, null);
    }

    public int getBoundPort() {
        return httpServer.getAddress().getPort();
    }

    public void stop() throws IOException {
        // stop(0) = immediate — do NOT wait for in-flight exchanges. Any straggling
        // broken-pipe writes are harmless and release the socket immediately for the
        // next JVM to re-bind.
        logger.info("[HttpServer] Stopping (immediate release)");
        httpServer.stop(0);
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
