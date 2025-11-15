package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when the server is about to shut down.
 *
 * <p>This event is fired before the server begins its shutdown sequence.
 * Mods can use this event to save state, clean up resources, or perform
 * final operations before the server stops.
 *
 * <p>This event is <b>not cancellable</b>. Server shutdown cannot be prevented.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onServerStopping(ServerStoppingEvent event) {
 *     logger.info("Server is shutting down, saving mod data...");
 *     saveModConfiguration();
 *     closeConnections();
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code ServerShutdownListener} interface. New mods should use this event
 * instead of the listener interface.
 *
 * @since 1.0.0
 * @see ServerStartedEvent
 */
public class ServerStoppingEvent extends Event {

    /**
     * Creates a new ServerStoppingEvent.
     */
    public ServerStoppingEvent() {
        super(false); // Not cancellable
    }
}
