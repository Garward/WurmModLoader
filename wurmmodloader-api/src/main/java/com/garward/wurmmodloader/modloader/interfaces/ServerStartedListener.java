package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Listener interface for server startup completion events.
 * <p>
 * Mods implementing this interface will be notified when the Wurm server
 * has finished starting up and is ready to accept player connections.
 * This is the latest point in the mod loading lifecycle and is called
 * after all initialization is complete.
 * </p>
 * <p>
 * Use this listener when you need to:
 * <ul>
 *   <li>Perform operations that require a fully initialized server</li>
 *   <li>Access server state that is only available after startup</li>
 *   <li>Schedule recurring tasks or timers</li>
 *   <li>Initialize systems that depend on all mods being loaded</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyMod implements WurmServerMod, ServerStartedListener {
 *     @Override
 *     public void onServerStarted() {
 *         System.out.println("Server is now ready!");
 *         // Schedule tasks, access server state, etc.
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Lifecycle order:</b>
 * <ol>
 *   <li>{@link WurmServerMod#preInit()}</li>
 *   <li>{@link WurmServerMod#init()}</li>
 *   <li>{@link ItemTemplatesCreatedListener#onItemTemplatesCreated()}</li>
 *   <li><b>{@link #onServerStarted()} ← You are here</b></li>
 * </ol>
 * </p>
 *
 * @see WurmServerMod
 * @see ServerShutdownListener
 * @since 1.0.0
 */
public interface ServerStartedListener {

	/**
	 * Called when the server has finished starting up and is ready to accept connections.
	 * <p>
	 * At this point:
	 * <ul>
	 *   <li>All mods have been loaded and initialized</li>
	 *   <li>All item and creature templates are available</li>
	 *   <li>The server is ready to accept player connections</li>
	 *   <li>All world data has been loaded</li>
	 * </ul>
	 * </p>
	 * <p>
	 * This is a safe place to access any server state or templates, as everything
	 * is fully initialized at this point.
	 * </p>
	 */
	void onServerStarted();

}
