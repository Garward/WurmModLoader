package com.garward.wurmmodloader.api.events.database;

import com.garward.wurmmodloader.api.database.DatabaseBackend;
import com.garward.wurmmodloader.api.database.DatabaseBackendRegistry;
import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired by the framework from inside {@code com.wurmonline.server.DbConnector.initialize()}
 * <b>before</b> any vanilla {@code ConnectionFactory} or {@code MigrationStrategy} is instantiated.
 *
 * <p><b>Fired when:</b> the server begins database initialization, exactly once per process.</p>
 *
 * <p><b>Use this to:</b> register a custom {@link DatabaseBackend} via
 * {@link DatabaseBackendRegistry#register(DatabaseBackend)}. This is the last safe moment to do so —
 * after this event returns, the framework reads
 * {@link DatabaseBackendRegistry#getRegistered()} and either uses the registered backend or falls
 * back to the vanilla SQLite / MySQL factories.</p>
 *
 * <p>This event is purely informational and <b>not cancellable</b>. The actual backend
 * substitution happens via the registry; the event exists only to give mods a well-defined
 * timing signal.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onDbBackendSelect(DatabaseBackendSelectionEvent event) {
 *     DatabaseBackendRegistry.register(new PostgresBackend());
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see DatabaseBackend
 * @see DatabaseBackendRegistry
 */
public class DatabaseBackendSelectionEvent extends Event {

    public DatabaseBackendSelectionEvent() {
        super(false);
    }
}
