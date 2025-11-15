package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when a player sends a message.
 *
 * <p>This event is fired for various types of player communication, including
 * chat messages, commands, and other text input. Handlers can inspect, modify,
 * or cancel the message.
 *
 * <p>This event is <b>cancellable</b>. If cancelled, the message will not be
 * processed or broadcast.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent(priority = EventPriority.HIGH)
 * public void onPlayerMessage(PlayerMessageEvent event) {
 *     String message = event.getMessage();
 *     if (containsProfanity(message)) {
 *         event.setCancelled(true);
 *         // Optionally send a warning to the player
 *     }
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code PlayerMessageListener} interface. New mods should use this event
 * instead of the listener interface.
 *
 * @since 1.0.0
 * @see ChannelMessageEvent
 */
public class PlayerMessageEvent extends Event {

    private final Object communicator; // Using Object for Communicator
    private String message;
    private final String title;

    /**
     * Creates a new PlayerMessageEvent.
     *
     * @param communicator the communicator sending the message
     * @param message the message content
     * @param title the message title/channel
     */
    public PlayerMessageEvent(Object communicator, String message, String title) {
        super(true); // Cancellable
        this.communicator = communicator;
        this.message = message;
        this.title = title;
    }

    /**
     * Returns the communicator sending the message.
     *
     * @return the communicator
     */
    public Object getCommunicator() {
        return communicator;
    }

    /**
     * Returns the message content.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message content.
     *
     * <p>Handlers can modify the message before it is processed.
     *
     * @param message the new message content
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the message title or channel.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }
}
