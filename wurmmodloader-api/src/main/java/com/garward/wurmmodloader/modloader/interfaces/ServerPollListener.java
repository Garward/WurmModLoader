package com.garward.wurmmodloader.modloader.interfaces;

	/**
	 * Interface for server poll event listeners in the Wurm mod loader system.
	 * 
	 * <p>This interface defines the contract for mods that need to perform actions
	 * on every server poll cycle. Implementing classes can register with the mod
	 * loader to receive periodic callbacks, allowing them to execute logic at
	 * regular intervals synchronized with the server's main loop.</p>
	 * 
	 * <p>Usage example:
	 * <pre>@code
	 * public class MyModPollListener implements ServerPollListener {
	 *     {@literal @}Override
	 *     public void onServerPoll() {
	 *         // Perform periodic tasks here
	 *         checkPlayerConditions();
	 *         updateCustomSystems();
	 *     }
	 * }
	 * </pre></p>
	 * 
	 * <p>Thread-safety: Implementations should be thread-safe as this method
	 * may be called from the server's main thread or other threads depending
	 * on the mod loader implementation.</p>
	 * 
	 * <p>Lifecycle: Instances of this interface should be registered with the
	 * mod loader during the initialization phase and will receive callbacks
	 * for the duration of the server's runtime.</p>
	 * 
	 * @since 1.0.0
	 */
public interface ServerPollListener {
	
	/**
	 * Called on every server poll cycle.
	 * 
	 * <p>This method is invoked periodically by the mod loader during each
	 * server poll iteration. The frequency of these calls depends on the
	 * server's configuration and current load, typically occurring every
	 * few milliseconds to several seconds.</p>
	 * 
	 * <p>Implementations can use this method to perform periodic tasks such as:
	 * <ul>
	 *   <li>Checking player conditions or states</li>
	 *   <li>Updating custom game systems</li>
	 *   <li>Processing queued actions</li>
	 *   <li>Maintaining internal mod state</li>
	 * </ul></p>
	 * 
	 * <p>Note: This method should return quickly to avoid blocking the server's
	 * main thread. Long-running operations should be queued for background
	 * processing or implemented with asynchronous patterns.</p>
	 * 
	 * @since 1.0.0
	 */
	public void onServerPoll();

}
