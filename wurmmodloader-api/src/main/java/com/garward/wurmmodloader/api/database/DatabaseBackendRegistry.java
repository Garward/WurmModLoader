package com.garward.wurmmodloader.api.database;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Process-wide registry for the single active {@link DatabaseBackend}.
 *
 * <p>At most one backend can be registered per server process. <b>First registration
 * wins</b> — subsequent calls to {@link #register(DatabaseBackend)} are ignored and
 * logged as a warning. This means mod load order determines the winner; if two mods
 * ship conflicting backends, the earlier-loaded one takes effect.</p>
 *
 * <p>The intended lifecycle:</p>
 * <ol>
 *   <li>The framework fires
 *       {@link com.garward.wurmmodloader.api.events.database.DatabaseBackendSelectionEvent}
 *       from inside {@code DbConnector.initialize()}.</li>
 *   <li>Mods observing the event call {@link #register(DatabaseBackend)}.</li>
 *   <li>The framework then consults {@link #getRegistered()} and, if non-null,
 *       replaces the vanilla factory / migration strategy with the backend's.</li>
 * </ol>
 *
 * @since 1.0.0
 */
public final class DatabaseBackendRegistry {

    private static final Logger LOGGER = Logger.getLogger(DatabaseBackendRegistry.class.getName());
    private static final AtomicReference<DatabaseBackend> BACKEND = new AtomicReference<>();

    private DatabaseBackendRegistry() {}

    /**
     * Register a backend. First call wins.
     *
     * @param backend the backend to register; must not be null
     * @return {@code true} if this backend was installed, {@code false} if one was already registered
     */
    public static boolean register(DatabaseBackend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("DatabaseBackend must not be null");
        }
        if (BACKEND.compareAndSet(null, backend)) {
            LOGGER.info("[DatabaseBackendRegistry] Registered backend: " + backend.getName()
                + " (" + backend.getClass().getName() + ")");
            return true;
        }
        DatabaseBackend existing = BACKEND.get();
        LOGGER.warning("[DatabaseBackendRegistry] Ignoring duplicate backend registration: "
            + backend.getName() + " (" + backend.getClass().getName()
            + ") — already registered: " + (existing != null ? existing.getName() : "null")
            + ". First registered wins.");
        return false;
    }

    /**
     * @return the currently registered backend, or {@code null} if none
     */
    public static DatabaseBackend getRegistered() {
        return BACKEND.get();
    }

    /**
     * Clears the registered backend. Intended for tests only.
     */
    public static void clearForTesting() {
        BACKEND.set(null);
    }
}
