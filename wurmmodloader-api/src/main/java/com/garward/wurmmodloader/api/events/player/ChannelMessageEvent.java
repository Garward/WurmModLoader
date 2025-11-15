package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when a message is sent in a channel.
 *
 * <p>This event represents messages sent through various communication channels
 * in the game (global chat, alliance chat, etc.). Handlers can inspect, modify,
 * or cancel the message.
 *
 * <p>This event is <b>cancellable</b>. If cancelled, the message will not be
 * broadcast to the channel.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onChannelMessage(ChannelMessageEvent event) {
 *     if (event.getMessage().startsWith("!help")) {
 *         // Send help information
 *         event.setCancelled(true); // Prevent broadcasting the command
 *         sendHelpMessage(event.getChannelId());
 *     }
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code ChannelMessageListener} interface. New mods should use this event
 * instead of the listener interface.
 *
 * @since 1.0.0
 * @see PlayerMessageEvent
 */
public class ChannelMessageEvent extends Event {

    private final Object communicator; // Using Object for Communicator
    private String message;
    private final int channelId;

    /**
     * Creates a new ChannelMessageEvent.
     *
     * @param communicator the communicator sending the message
     * @param message the message content
     * @param channelId the channel ID
     */
    public ChannelMessageEvent(Object communicator, String message, int channelId) {
        super(true); // Cancellable
        this.communicator = communicator;
        this.message = message;
        this.channelId = channelId;
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
     * <p>Handlers can modify the message before it is broadcast.
     *
     * @param message the new message content
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the channel ID.
     *
     * @return the channel ID
     */
    public int getChannelId() {
        return channelId;
    }
}
