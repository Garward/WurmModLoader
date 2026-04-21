package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when {@code ServerLauncher.runServer} returns — i.e. when the
 * server's <b>core</b> initialization has completed.
 *
 * <p><b>Important:</b> despite the name, this is <b>not</b> the "server is
 * fully operational" moment. Several asynchronous subsystems finish after
 * this event fires: Steam connect, database connection pool warmup, and
 * the vanilla {@code CommandReader} thread. If you need to run post-startup
 * work that depends on any of those being live (DB syncs, migrations,
 * connection-pool-dependent tasks), subscribe to {@link ServerFullyReadyEvent}
 * instead — that fires when {@code CommandReader.run} begins, after all
 * async subsystems have settled.
 *
 * <p>Use {@code ServerStartedEvent} for work that only needs world + mod
 * init to be complete. Use {@link ServerFullyReadyEvent} for work that
 * needs the server to be truly idle-ready.
 *
 * <p>This event is <b>not cancellable</b>.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onServerStarted(ServerStartedEvent event) {
 *     // World + mods are initialized. Safe to register dynamic content
 *     // that doesn't need the DB pool or Steam to be fully live.
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code ServerStartedListener} interface. New mods should use this event
 * (or {@link ServerFullyReadyEvent}) instead of the listener interface.
 *
 * @since 1.0.0
 * @see ServerFullyReadyEvent
 * @see ServerStoppingEvent
 */
public class ServerStartedEvent extends Event {

    /**
     * Creates a new ServerStartedEvent.
     */
    public ServerStartedEvent() {
        super(false); // Not cancellable
    }
}
