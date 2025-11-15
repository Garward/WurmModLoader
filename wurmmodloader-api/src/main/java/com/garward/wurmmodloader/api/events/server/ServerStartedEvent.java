package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when the server has finished starting and is ready to accept players.
 *
 * <p>This event is fired after all mods have been initialized and the server
 * has completed its startup sequence. At this point, the world is loaded,
 * all systems are initialized, and the server is listening for connections.
 *
 * <p>This event is <b>not cancellable</b>.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onServerStarted(ServerStartedEvent event) {
 *     logger.info("Server is now ready for players!");
 *     // Perform post-startup tasks like loading configuration,
 *     // scheduling tasks, or registering dynamic content
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code ServerStartedListener} interface. New mods should use this event
 * instead of the listener interface.
 *
 * @since 1.0.0
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
