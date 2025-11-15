package com.garward.wurmmodloader.modloader.interfaces;

/**
 * Listener interface for server shutdown events.
 * <p>
 * Mods implementing this interface will be notified when the server is about
 * to shut down. This is the last opportunity for mods to perform cleanup,
 * save data, or close resources before the server terminates.
 * </p>
 * <p>
 * Use this listener when you need to:
 * <ul>
 *   <li>Save mod state or configuration before shutdown</li>
 *   <li>Close database connections or file handles</li>
 *   <li>Stop background threads or scheduled tasks</li>
 *   <li>Flush caches or buffers to disk</li>
 *   <li>Send notifications about server shutdown</li>
 * </ul>
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * public class MyMod implements WurmServerMod, ServerShutdownListener {
 *     @Override
 *     public void onServerShutdown() {
 *         System.out.println("Server shutting down, saving data...");
 *         saveModData();
 *         closeConnections();
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * <b>Important:</b> Keep shutdown tasks quick. The server may forcefully
 * terminate if shutdown takes too long. Critical operations should be
 * performed within a reasonable timeout (typically under 30 seconds).
 * </p>
 * <p>
 * <b>Thread Safety:</b> This method is called from the server's main thread.
 * If your mod has background threads, ensure they are properly stopped or
 * joined in this method.
 * </p>
 *
 * @see WurmServerMod
 * @see ServerStartedListener
 * @since 1.0.0
 */
public interface ServerShutdownListener {

	/**
	 * Called before the server shutdown is initiated.
	 * <p>
	 * This method is invoked when the server begins its shutdown sequence,
	 * before players are disconnected and before server resources are released.
	 * </p>
	 * <p>
	 * At this point:
	 * <ul>
	 *   <li>Server is still running</li>
	 *   <li>Database connections are still active</li>
	 *   <li>Players may still be connected</li>
	 *   <li>File I/O is still possible</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <b>Best Practices:</b>
	 * <ul>
	 *   <li>Save all critical data first</li>
	 *   <li>Close resources gracefully</li>
	 *   <li>Log any errors during shutdown</li>
	 *   <li>Avoid long-running operations</li>
	 *   <li>Handle exceptions to prevent blocking shutdown</li>
	 * </ul>
	 * </p>
	 */
	void onServerShutdown();

}
