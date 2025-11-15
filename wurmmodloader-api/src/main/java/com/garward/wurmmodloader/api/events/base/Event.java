package com.garward.wurmmodloader.api.events.base;

/**
 * Base class for all events in WurmModLoader.
 *
 * <p>Events represent significant moments in the server lifecycle or gameplay
 * that mods can observe and potentially modify. Events are dispatched through
 * the {@code EventBus} to all registered listeners.
 *
 * <h2>Event Cancellation</h2>
 * <p>Some events are cancellable, meaning that a listener can prevent the
 * associated action from occurring. To make an event cancellable, pass
 * {@code true} to the constructor:
 *
 * <pre>{@code
 * public class MyEvent extends Event {
 *     public MyEvent() {
 *         super(true); // This event is cancellable
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Events are not thread-safe by default. The event bus guarantees that
 * events are dispatched on the same thread where they are posted.
 *
 * @since 1.0.0
 * @see SubscribeEvent
 */
public abstract class Event {

    private final boolean cancellable;
    private boolean cancelled = false;

    /**
     * Creates a new non-cancellable event.
     */
    protected Event() {
        this(false);
    }

    /**
     * Creates a new event.
     *
     * @param cancellable whether this event can be cancelled
     */
    protected Event(boolean cancellable) {
        this.cancellable = cancellable;
    }

    /**
     * Returns whether this event can be cancelled.
     *
     * @return {@code true} if this event supports cancellation
     */
    public boolean isCancellable() {
        return cancellable;
    }

    /**
     * Sets the cancellation state of this event.
     *
     * <p>If this event is cancelled, subsequent event handlers may not be
     * called, and the associated action may be prevented from occurring.
     *
     * @param cancel {@code true} to cancel this event
     * @throws UnsupportedOperationException if this event is not cancellable
     */
    public void setCancelled(boolean cancel) {
        if (!cancellable) {
            throw new UnsupportedOperationException(
                "Cannot cancel non-cancellable event: " + getClass().getName());
        }
        this.cancelled = cancel;
    }

    /**
     * Returns whether this event has been cancelled.
     *
     * @return {@code true} if this event has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Convenience method to cancel this event.
     * Equivalent to {@code setCancelled(true)}.
     *
     * @throws UnsupportedOperationException if this event is not cancellable
     */
    public void cancel() {
        setCancelled(true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               (cancellable ? (cancelled ? " [CANCELLED]" : " [cancellable]") : "");
    }
}
