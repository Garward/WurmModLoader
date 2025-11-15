package com.garward.wurmmodloader.modloader.interfaces;

import com.wurmonline.server.creatures.Communicator;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
/**
 * Interface for handling player messages in the Wurm mod loader system.
 * 
 * This listener allows mods to intercept and process player messages before they are handled by the game.
 * Implementations can choose to modify, block, or allow messages to proceed through the normal processing chain.
 * 
 * <p>Usage example:
 * <pre>@code
 * public class MyMessageListener implements PlayerMessageListener {
 *     @Override
 *     public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
 *         if (message.startsWith("/mymodcommand")) {
 *             // Handle custom command
 *             return MessagePolicy.DISCARD; // Prevent further processing
 *         }
 *         return MessagePolicy.PASS; // Allow normal processing
 *     }
 * }
 * </pre>
 * 
 * <p>Thread-safety: Implementations should be thread-safe as message handling may occur on different threads.
 * 
 * @since 1.0.0
 */

public interface PlayerMessageListener {

	/**
	 * Handle a player message
	 * @param communicator Communicator
	 * @param message message
	 * @param title chat title
	 * @return MessagePolicy describing how to further process the message
	 */
	default MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
		return onPlayerMessage(communicator, message) ? MessagePolicy.DISCARD : MessagePolicy.PASS;
	}

	/**
	 * Handle a player message. Use {@link #onPlayerMessage(Communicator, String, String)} instead
	 * @param communicator Communicator
	 * @param message message
	 * @return true if the message should not be processed by the game
	 */
	@Deprecated
	boolean onPlayerMessage(Communicator communicator, String message);

}
