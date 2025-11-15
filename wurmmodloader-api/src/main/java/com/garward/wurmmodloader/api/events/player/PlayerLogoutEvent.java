package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;


/**
 * Event fired when a player logs out of the server.
 *
 * <p>This event is fired after the player has disconnected. At this point,
 * the player is no longer in the game world. Mods can use this event for
 * cleanup, logging, or saving player-specific data.
 *
 * <p>This event is <b>not cancellable</b>. The player has already logged out.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @SubscribeEvent
 * public void onPlayerLogout(PlayerLogoutEvent event) {
 *     Object player = event.getPlayer();
 *     logger.info("Player logged out: " + player);
 *     savePlayerData(player);
 * }
 * }</pre>
 *
 * @since 1.0.0
 * @see PlayerLoginEvent
 */
public class PlayerLogoutEvent extends Event {

    private final Object player; // Using Object for now since we don't have Player class in API

    /**
     * Creates a new PlayerLogoutEvent.
     *
     * @param player the player who logged out
     */
    public PlayerLogoutEvent(Object player) {
        super(false); // Not cancellable
        this.player = player;
    }

    /**
     * Returns the player who logged out.
     *
     * @return the player
     */
    public Object getPlayer() {
        return player;
    }
}
