package com.garward.wurmmodloader.api.events.database;

import com.garward.wurmmodloader.api.database.DatabaseBackend;
import com.garward.wurmmodloader.api.database.DatabaseBackendRegistry;
import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired by the framework immediately after a {@link DatabaseBackend} wins
 * first-wins registration, and <b>before</b> any per-schema
 * {@code ConnectionFactory} is instantiated.
 *
 * <p><b>Fired when:</b> {@code DatabaseBackendEventLogic.applyRegisteredBackendIfAny()}
 * observes a non-null registered backend. If no backend is registered, this
 * event does not fire. Fires exactly once per process.</p>
 *
 * <p><b>Use this to:</b> do pre-connection bootstrap work the backend needs
 * before its own factories try to connect. The canonical case is a fresh
 * Postgres cluster: WU will immediately open nine per-schema connections to
 * nine databases ({@code wurmlogin}, {@code wurmitems}, {@code wurmplayers},
 * …), and they must already exist. Subscribe to this event, open a single
 * admin connection, run {@code CREATE DATABASE IF NOT EXISTS} nine times,
 * and close it.</p>
 *
 * <p>The event exposes the registered backend via {@link #getBackend()} so a
 * single handler can branch on name / dialect if the mod ships multiple
 * backends. The event is not cancellable — by the time it fires, the
 * backend has already won. If your bootstrap throws, the framework logs
 * the error and proceeds; per-schema connections will then likely fail,
 * which is usually the correct failure mode (clearer than a silent skip).</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onBootstrap(DatabaseBackendBootstrapEvent event) {
 *     if (!"postgres".equals(event.getBackend().getName())) return;
 *     try (Connection admin = DriverManager.getConnection(
 *              "jdbc:postgresql://host/postgres", user, pw)) {
 *         admin.setAutoCommit(true);
 *         for (WurmDatabaseSchema s : WurmDatabaseSchema.values()) {
 *             try (Statement st = admin.createStatement()) {
 *                 st.execute("CREATE DATABASE IF NOT EXISTS \"wurm" +
 *                            s.name().toLowerCase() + "\"");
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see DatabaseBackend
 * @see DatabaseBackendRegistry
 * @see DatabaseBackendSelectionEvent
 */
public class DatabaseBackendBootstrapEvent extends Event {

    private final DatabaseBackend backend;

    public DatabaseBackendBootstrapEvent(DatabaseBackend backend) {
        super(false);
        this.backend = backend;
    }

    /**
     * @return the backend that won registration; never {@code null}
     */
    public DatabaseBackend getBackend() {
        return backend;
    }
}
