package com.garward.wurmmodloader.modloader.interfaces;

import com.wurmonline.server.players.Player;

/**
 * Listener interface for player login and logout events.
 * <p>
 * Mods implementing this interface will be notified when players log in to
 * or log out from the server. This allows mods to perform player-specific
 * initialization, tracking, or cleanup.
 * </p>
 * <p>
 * Use this listener when you need to:
 * <ul>
 *   <li>Initialize per-player mod data when a player logs in</li>
 *   <li>Track player sessions or activity</li>
 *   <li>Send welcome messages or notifications</li>
 *   <li>Clean up player-specific data when a player logs out</li>
 *   <li>Synchronize player state with external systems</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyMod implements WurmServerMod, PlayerLoginListener {
 *     private Map<Long, PlayerData> playerData = new HashMap<>();
 *
 *     @Override
 *     public void onPlayerLogin(Player player) {
 *         System.out.println(player.getName() + " has logged in!");
 *         playerData.put(player.getWurmId(), new PlayerData());
 *     }
 *
 *     @Override
 *     public void onPlayerLogout(Player player) {
 *         System.out.println(player.getName() + " has logged out!");
 *         playerData.remove(player.getWurmId());
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Thread Safety:</b> These methods are called from the server's main thread,
 * so no synchronization is required for accessing server state. However, if your
 * mod uses background threads, proper synchronization may be necessary.
 * </p>
 *
 * @see Player
 * @see WurmServerMod
 * @since 1.0.0
 */
public interface PlayerLoginListener {

	/**
	 * Called when a player successfully logs in to the server.
	 * <p>
	 * This method is invoked after the player has been authenticated and their
	 * character data has been loaded, but before the player is fully spawned
	 * in the game world.
	 * </p>
	 * <p>
	 * At this point:
	 * <ul>
	 *   <li>Player object is fully initialized</li>
	 *   <li>Player data has been loaded from database</li>
	 *   <li>Player has been authenticated</li>
	 *   <li>Player is not yet visible to other players</li>
	 * </ul>
	 * </p>
	 *
	 * @param player the player who is logging in
	 */
	void onPlayerLogin(Player player);

	/**
	 * Called when a player logs out from the server.
	 * <p>
	 * This method is invoked when a player disconnects, either voluntarily
	 * or due to connection issues. Use this to clean up any player-specific
	 * resources or save player state.
	 * </p>
	 * <p>
	 * The default implementation does nothing. Override this method if you
	 * need to perform cleanup when players log out.
	 * </p>
	 * <p>
	 * At this point:
	 * <ul>
	 *   <li>Player object is still valid</li>
	 *   <li>Player data can still be accessed</li>
	 *   <li>Player is being removed from the game world</li>
	 *   <li>Connection may already be closed</li>
	 * </ul>
	 * </p>
	 *
	 * @param player the player who is logging out
	 */
	default void onPlayerLogout(Player player) {
	}

}
