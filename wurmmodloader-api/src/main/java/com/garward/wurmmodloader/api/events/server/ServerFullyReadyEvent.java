package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired at the <b>true</b> "server is fully ready" moment — when the
 * vanilla {@code CommandReader} thread begins its console read loop.
 *
 * <p>This is strictly later than {@link ServerStartedEvent}, which only
 * signals that {@code ServerLauncher.runServer} has returned (core init
 * complete). Several asynchronous subsystems finish afterwards: Steam
 * connect, the DB connection pool warming up, the console reader spinning
 * up, etc. By the time this event fires, all of them are live.
 *
 * <p>Use this event — not {@link ServerStartedEvent} — for post-startup
 * work that needs a fully settled server: database syncs/migrations, pool
 * warmup checks, background scheduler startup, anything that historically
 * needed a "delay N seconds after ServerStarted" workaround.
 *
 * <p><b>Ordering (definitive):</b>
 * <ol>
 *   <li>Mod {@code preInit()} / {@code init()}</li>
 *   <li>World + DB pool initialization</li>
 *   <li>Legacy {@code ServerStartedListener.onServerStarted()} fires</li>
 *   <li>{@link ServerStartedEvent} fires (core init complete)</li>
 *   <li>Steam connect, misc async subsystems settle</li>
 *   <li>{@code CommandReader.run} begins → <b>this event fires</b></li>
 * </ol>
 *
 * <p>This event is <b>not cancellable</b>.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onServerFullyReady(ServerFullyReadyEvent event) {
 *     // DB pool is hot, all subsystems are up — safe to sync config,
 *     // run migrations, open long-lived connections, etc.
 *     databaseConfigSync.run();
 * }
 * }</pre>
 *
 * @since 1.1.0
 * @see ServerStartedEvent
 * @see ServerStoppingEvent
 */
public class ServerFullyReadyEvent extends Event {

    public ServerFullyReadyEvent() {
        super(false); // Not cancellable
    }
}
