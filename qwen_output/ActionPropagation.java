package com.garward.wurmmodloader.modsupport.actions;

/**
 * Defines how an action is propagated to the server and other action performers within the Wurm modding framework.
 *
 * <p>This enumeration controls the behavior of custom actions when they are executed, determining whether they
 * should be sent to the server for processing, shared with other action performers, and whether the action
 * should be considered finished or continued.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // In a custom action implementation
 * public boolean action() {
 *     // Perform custom logic
 *     
 *     // Control propagation behavior
 *     return propagateAction(
 *         ActionPropagation.SERVER_PROPAGATION,
 *         ActionPropagation.ACTION_PERFORMER_PROPAGATION,
 *         ActionPropagation.FINISH_ACTION
 *     );
 * }
 * }</pre>
 *
 * <p>The propagation settings can be combined to achieve the desired behavior for custom actions. For instance,
 * an action might need to be sent to the server for persistence while also notifying other clients, but then
 * continuing with additional processing rather than finishing immediately.</p>
 *
 * @since 1.0.0
 * @see com.garward.wurmmodloader.modsupport.actions.ActionSupport
 */
public enum ActionPropagation {
	
	/**
	 * Propagate the action to the server for processing and persistence.
	 * 
	 * <p>When this propagation type is used, the action will be sent to the server where it can be
	 * validated, processed, and persisted according to the game's standard mechanics.</p>
	 *
	 * @since 1.0.0
	 */
	SERVER_PROPAGATION,
	
	/**
	 * Do not propagate the action to the server.
	 * 
	 * <p>Using this option means the action will only be processed locally on the client that initiated it.
	 * No server-side processing or validation will occur, and the action will not be persisted in the game world.</p>
	 *
	 * @since 1.0.0
	 */
	NO_SERVER_PROPAGATION,
	
	/**
	 * Propagate the action to other action performers (clients).
	 * 
	 * <p>This setting ensures that other clients who may be observing or participating in the same action
	 * will receive notification of the action being performed, allowing for synchronized behavior across clients.</p>
	 *
	 * @since 1.0.0
	 */
	ACTION_PERFORMER_PROPAGATION,
	
	/**
	 * Do not propagate the action to other action performers.
	 * Other action performers may have handled the action before.
	 * 
	 * <p>With this option, only the initiating client processes the action. Other clients will not be notified.
	 * Note that if other action performers had already begun processing the action before this setting was applied,
	 * they may still complete their processing.</p>
	 *
	 * @since 1.0.0
	 */
	NO_ACTION_PERFORMER_PROPAGATION,
	
	/**
	 * Finish the action. This is equivalent to returning {@code true} from the action() method.
	 * 
	 * <p>When specified, indicates that the action has completed successfully and no further processing
	 * should occur. This signals to the action system that the action cycle is complete.</p>
	 *
	 * @since 1.0.0
	 */
	FINISH_ACTION,
	
	/**
	 * Continue the action. This is equivalent to returning {@code false} from the action() method.
	 * 
	 * <p>When specified, indicates that the action requires additional processing steps. The action system
	 * will continue to process the action according to its normal flow, potentially invoking other handlers
	 * or waiting for additional input.</p>
	 *
	 * @since 1.0.0
	 */
	CONTINUE_ACTION,
}