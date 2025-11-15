package com.garward.wurmmodloader.api.events.server;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired on each server tick (poll).
 *
 * <p>This event is fired very frequently (multiple times per second) on the
 * main server thread. Handlers should be extremely lightweight to avoid
 * impacting server performance.
 *
 * <p>This event is <b>not cancellable</b>.
 *
 * <h2>Performance Warning</h2>
 * <p><b>WARNING:</b> This event fires on every server tick. Heavy processing
 * in event handlers can severely degrade server performance. Consider using
 * periodic tasks or scheduled executors instead for non-critical work.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent(priority = EventPriority.LOW)
 * public void onServerPoll(ServerPollEvent event) {
 *     // Only do lightweight checks or increment counters
 *     tickCounter++;
 *     if (tickCounter % 100 == 0) {
 *         // Perform less frequent work every 100 ticks
 *         performPeriodicCheck();
 *     }
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code ServerPollListener} interface. New mods should use this event
 * instead of the listener interface.
 *
 * @since 1.0.0
 * @see ServerStartedEvent
 */
public class ServerPollEvent extends Event {

    /**
     * Creates a new ServerPollEvent.
     */
    public ServerPollEvent() {
        super(false); // Not cancellable
    }
}
