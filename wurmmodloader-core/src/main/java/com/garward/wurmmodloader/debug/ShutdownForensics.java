package com.garward.wurmmodloader.debug;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Captures the trigger of any JVM exit and writes it to a dedicated log file,
 * so "the server cleanly shut off with no info as to why" no longer happens.
 *
 * <p>Four pieces of instrumentation:
 * <ol>
 *   <li>Default uncaught-exception handler — logs SEVERE with thread + stack
 *       so silent thread deaths (event bus workers, mod threads) are visible.</li>
 *   <li>Signal handlers for {@code SIGINT}, {@code SIGTERM}, {@code SIGHUP}
 *       (via reflective {@code sun.misc.Signal}) — chains to the prior handler
 *       so default JVM behavior still fires.</li>
 *   <li>A JVM shutdown hook that dumps every live thread's stack.</li>
 *   <li>A static entry point ({@link #logServerShutdownCaller()}) called by the
 *       legacy {@code Server.shutDown()} bytecode hook to record who inside
 *       Wurm initiated the shutdown.</li>
 * </ol>
 *
 * <p>Output goes to {@code logs/wurmmodloader-shutdown.log} (rotating, 1MB×3),
 * and is also forwarded to the main wurmmodloader log via parent handlers.
 */
public final class ShutdownForensics {

    public static final String LOGGER_NAME = "com.garward.wurmmodloader.debug.ShutdownForensics";
    private static final Logger LOG = Logger.getLogger(LOGGER_NAME);

    private static volatile boolean installed = false;
    private static volatile boolean shutdownDumped = false;

    private ShutdownForensics() {}

    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        configureFileHandler();
        installUncaughtHandler();
        installSignalHandlers();
        installShutdownHook();
        LOG.info("[ShutdownForensics] Installed");
    }

    // -------------------------------------------------------------------------
    // 1. Dedicated rotating file handler in <serverdir>/logs/
    // -------------------------------------------------------------------------
    private static void configureFileHandler() {
        try {
            String rootOverride = System.getProperty("wurmmodloader.serverdir", ".");
            Path serverRoot = Paths.get(rootOverride).toAbsolutePath().normalize();
            Path logsDir = serverRoot.resolve("logs");
            Files.createDirectories(logsDir);
            String pattern = logsDir.resolve("wurmmodloader-shutdown.%g.log").toString();

            FileHandler fh = new FileHandler(pattern, 1024 * 1024, 3, true);
            fh.setFormatter(new SimpleFormatter());
            fh.setEncoding(StandardCharsets.UTF_8.name());
            fh.setLevel(Level.ALL);

            LOG.addHandler(fh);
            LOG.setLevel(Level.ALL);
            LOG.setUseParentHandlers(true); // also flow to main log

            String msg = "[ShutdownForensics] Forensics log enabled at " + pattern;
            System.out.println(msg);
            LOG.info(msg);
        } catch (IOException | SecurityException e) {
            LOG.log(Level.WARNING, "Failed to configure shutdown forensics log file", e);
        }
    }

    // -------------------------------------------------------------------------
    // 2. Default uncaught exception handler
    // -------------------------------------------------------------------------
    private static void installUncaughtHandler() {
        Thread.UncaughtExceptionHandler prior = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable ex) -> {
            try {
                LOG.log(Level.SEVERE,
                    "[ShutdownForensics] Uncaught exception in thread '" + t.getName()
                        + "' (id=" + t.getId() + ", daemon=" + t.isDaemon() + ")",
                    ex);
            } catch (Throwable ignore) {
                // never let our handler throw
            }
            if (prior != null) {
                try {
                    prior.uncaughtException(t, ex);
                } catch (Throwable ignore) {
                    // swallow — must not interfere with JVM teardown
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // 3. Signal handlers (reflective so JDK 21 module restrictions don't bite)
    // -------------------------------------------------------------------------
    private static void installSignalHandlers() {
        registerSignal("INT");
        registerSignal("TERM");
        registerSignal("HUP");
    }

    private static void registerSignal(String name) {
        try {
            Class<?> signalCls = Class.forName("sun.misc.Signal");
            Class<?> handlerCls = Class.forName("sun.misc.SignalHandler");

            Object signal = signalCls.getConstructor(String.class).newInstance(name);
            Method handleMethod = signalCls.getMethod("handle", signalCls, handlerCls);
            Method handlerInvoke = handlerCls.getMethod("handle", signalCls);

            // We need the prior handler to chain — but Signal.handle returns it,
            // so install with a forwarding proxy whose target is filled in after.
            Object[] priorHolder = new Object[1];

            InvocationHandler proxyHandler = (proxy, method, args) -> {
                if ("handle".equals(method.getName()) && args != null && args.length == 1) {
                    try {
                        LOG.warning("[ShutdownForensics] Received SIG" + name
                            + " — shutdown imminent (chaining to prior handler)");
                    } catch (Throwable ignore) {}
                    Object prior = priorHolder[0];
                    if (prior != null) {
                        try {
                            handlerInvoke.invoke(prior, args[0]);
                        } catch (Throwable t) {
                            try {
                                LOG.log(Level.WARNING,
                                    "Prior signal handler for SIG" + name + " threw", t);
                            } catch (Throwable ignore) {}
                        }
                    }
                    return null;
                }
                if ("toString".equals(method.getName())) {
                    return "ShutdownForensics-SignalHandler[" + name + "]";
                }
                return null;
            };

            Object newHandler = Proxy.newProxyInstance(
                handlerCls.getClassLoader(),
                new Class<?>[] { handlerCls },
                proxyHandler);

            priorHolder[0] = handleMethod.invoke(null, signal, newHandler);
        } catch (Throwable t) {
            LOG.warning("[ShutdownForensics] Could not install handler for SIG" + name
                + " (signal-trigger detection disabled for this signal): " + t.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 4. JVM shutdown hook with full thread dump
    // -------------------------------------------------------------------------
    private static void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (shutdownDumped) {
                return;
            }
            shutdownDumped = true;
            try {
                LOG.warning("[ShutdownForensics] === JVM SHUTDOWN INITIATED ===");
                Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
                LOG.warning("[ShutdownForensics] Active threads at shutdown: " + all.size());

                StringBuilder sb = new StringBuilder(4096);
                sb.append("[ShutdownForensics] Thread dump:\n");
                for (Map.Entry<Thread, StackTraceElement[]> e : all.entrySet()) {
                    Thread th = e.getKey();
                    StackTraceElement[] stack = e.getValue();
                    sb.append("  \"").append(th.getName()).append("\"")
                      .append(" tid=").append(th.getId())
                      .append(" state=").append(th.getState())
                      .append(" daemon=").append(th.isDaemon())
                      .append('\n');
                    for (StackTraceElement el : stack) {
                        sb.append("      at ").append(el).append('\n');
                    }
                    if (stack.length == 0) {
                        sb.append("      (no stack frames)\n");
                    }
                }
                LOG.warning(sb.toString());
                LOG.warning("[ShutdownForensics] === END OF THREAD DUMP ===");

                // Flush our handlers so the dump survives even if other hooks block.
                for (java.util.logging.Handler h : LOG.getHandlers()) {
                    try { h.flush(); } catch (Throwable ignore) {}
                }
            } catch (Throwable t) {
                try {
                    LOG.log(Level.SEVERE, "[ShutdownForensics] Forensics shutdown hook failed", t);
                } catch (Throwable ignore) {}
            }
        }, "WurmModLoader-ShutdownForensics"));
    }

    // -------------------------------------------------------------------------
    // 5. Caller-stack capture from Wurm's Server.shutDown() bytecode hook
    // -------------------------------------------------------------------------
    public static void logServerShutdownCaller() {
        try {
            Throwable trace = new Throwable("Server.shutDown() invoked");
            LOG.log(Level.WARNING,
                "[ShutdownForensics] Wurm Server.shutDown() called — caller stack follows",
                trace);
        } catch (Throwable ignore) {}
    }
}
