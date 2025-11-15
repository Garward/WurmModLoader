package com.garward.wurmmodloader.api.events.base;

/**
 * Base class for cancellable events.
 *
 * <p>Convenience class that automatically marks the event as cancellable.
 * Subclasses can be cancelled by listeners to prevent the associated action
 * from occurring.</p>
 *
 * @author WurmModLoader Team
 * @since 1.0.0
 */
public abstract class CancellableEvent extends Event {

    /**
     * Creates a new cancellable event.
     */
    protected CancellableEvent() {
        super(true); // Always cancellable
    }
}
