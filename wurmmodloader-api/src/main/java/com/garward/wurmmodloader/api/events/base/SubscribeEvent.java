package com.garward.wurmmodloader.api.events.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber.
 *
 * <p>Methods annotated with {@code @SubscribeEvent} will be automatically
 * registered as event handlers when the containing object is registered with
 * the event bus. The method must:
 * <ul>
 *   <li>Have exactly one parameter</li>
 *   <li>The parameter type must extend {@link Event}</li>
 *   <li>Return void</li>
 *   <li>Be public or protected</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * public class MyMod {
 *
 *     @SubscribeEvent
 *     public void onServerStarted(ServerStartedEvent event) {
 *         System.out.println("Server started!");
 *     }
 *
 *     @SubscribeEvent(priority = EventPriority.HIGH)
 *     public void onItemUse(ItemUseEvent event) {
 *         if (shouldCancelItemUse(event)) {
 *             event.setCancelled(true);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Priority</h2>
 * <p>Use the {@link #priority()} attribute to control the order in which
 * event handlers are called. Handlers with higher priority (lower numeric value)
 * are called first.
 *
 * <h2>Error Handling</h2>
 * <p>If an event handler throws an exception, it will be logged and other
 * handlers will continue to be called. The exception will not propagate to
 * the event poster.
 *
 * @since 1.0.0
 * @see Event
 * @see EventPriority
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {

    /**
     * The priority of this event handler.
     *
     * <p>Handlers with higher priority (lower numeric value) are called first.
     * Default is {@link EventPriority#NORMAL}.
     *
     * @return the event priority
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * Whether to receive cancelled events.
     *
     * <p>By default, handlers are not called for events that have been cancelled
     * by a higher-priority handler. Set this to {@code true} to receive cancelled
     * events anyway.
     *
     * @return {@code true} to receive cancelled events
     */
    boolean receiveCancelled() default false;
}
