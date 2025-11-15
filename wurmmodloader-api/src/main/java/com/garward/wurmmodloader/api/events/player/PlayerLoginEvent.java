package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when a player attempts to log in to the server.
 *
 * <p>This event is fired before the player is fully logged in. Handlers can
 * inspect the player and optionally cancel the login (e.g., for ban checks,
 * maintenance mode, or custom authentication).
 *
 * <p>This event is <b>cancellable</b>. If cancelled, the player will not be
 * allowed to log in.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent(priority = EventPriority.HIGH)
 * public void onPlayerLogin(PlayerLoginEvent event) {
 *     Player player = event.getPlayer();
 *     if (isServerInMaintenanceMode()) {
 *         event.setCancelled(true);
 *         event.setKickMessage("Server is undergoing maintenance");
 *     }
 * }
 * }</pre>
 *
 * <h2>Legacy Compatibility</h2>
 * <p>This event is automatically fired for mods implementing the legacy
 * {@code PlayerLoginListener} interface. New mods should use this event
 * instead of the listener interface.
 *
 * @since 1.0.0
 * @see PlayerLogoutEvent
 */
public class PlayerLoginEvent extends Event {

    private final Object player; // Using Object for now since we don't have Player class in API
    private String kickMessage = "Login denied";

    /**
     * Creates a new PlayerLoginEvent.
     *
     * @param player the player attempting to log in
     */
    public PlayerLoginEvent(Object player) {
        super(true); // Cancellable
        this.player = player;
    }

    /**
     * Returns the player attempting to log in.
     *
     * @return the player
     */
    public Object getPlayer() {
        return player;
    }

    /**
     * Sets the message to display to the player if login is denied.
     *
     * <p>This message is only used if the event is cancelled.
     *
     * @param message the kick message
     */
    public void setKickMessage(String message) {
        this.kickMessage = message != null ? message : "Login denied";
    }

    /**
     * Returns the kick message that will be displayed if login is denied.
     *
     * @return the kick message
     */
    public String getKickMessage() {
        return kickMessage;
    }
}
