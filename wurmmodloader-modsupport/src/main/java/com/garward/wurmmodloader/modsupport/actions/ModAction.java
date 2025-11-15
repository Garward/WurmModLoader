package com.garward.wurmmodloader.modsupport.actions;

/**
 * Interface defining a mod action that can provide behaviour and action performance capabilities.
 *
 * <p>This interface serves as a base for mod actions that can participate in the Wurm Unlimited
 * action system. It provides default methods to retrieve behaviour providers and action performers
 * from the implementing class when it implements the respective interfaces.</p>
 *
 * <p>Implementing classes can optionally implement {@link BehaviourProvider} and/or 
 * {@link ActionPerformer} interfaces to provide custom behaviour and action execution logic.</p>
 *
 * <p><strong>Usage example:</strong></p>
 * <pre>{@code
 * public class CustomModAction implements ModAction, BehaviourProvider, ActionPerformer {
 *     
 *     @Override
 *     public List<ActionEntry> getBehavioursFor(Creature performer, Item source, 
 *             Item target, short action, float counter) {
 *         // Custom behaviour implementation
 *         return Arrays.asList(new ActionEntry(...));
 *     }
 *     
 *     @Override
 *     public boolean action(Creature performer, Item source, Item target, 
 *             short action, float counter) {
 *         // Custom action performance implementation
 *         return true;
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong> Instances of implementing classes should be created during mod
 * initialization and registered with the action system. The methods in this interface may be
 * called frequently during gameplay when actions are being evaluated or executed.</p>
 *
 * <p><strong>Thread-safety:</strong> Implementations should be thread-safe as action evaluation
 * and execution may occur on different threads. The default methods in this interface are
 * thread-safe.</p>
 *
 * @since 1.0.0
 * @see BehaviourProvider
 * @see ActionPerformer
 */
public interface ModAction {
	
	/**
	 * Retrieves the behaviour provider implementation if this instance implements 
	 * {@link BehaviourProvider}, otherwise returns null.
	 *
	 * <p>This default method provides a convenient way to access the behaviour provider
	 * functionality when the implementing class also implements {@link BehaviourProvider}.</p>
	 *
	 * <p><strong>Thread-safety:</strong> This method is thread-safe as it only performs
	 * type checking and casting operations.</p>
	 *
	 * @return the {@link BehaviourProvider} implementation if this instance implements
	 *         the interface, or {@code null} if it does not
	 * @since 1.0.0
	 * @see BehaviourProvider
	 */
	default BehaviourProvider getBehaviourProvider() {
		if (this instanceof BehaviourProvider) {
			return (BehaviourProvider) this;
		} else {
			return null;
		}
	}
	
	/**
	 * Retrieves the action performer implementation if this instance implements 
	 * {@link ActionPerformer}, otherwise returns null.
	 *
	 * <p>This default method provides a convenient way to access the action performer
	 * functionality when the implementing class also implements {@link ActionPerformer}.</p>
	 *
	 * <p><strong>Thread-safety:</strong> This method is thread-safe as it only performs
	 * type checking and casting operations.</p>
	 *
	 * @return the {@link ActionPerformer} implementation if this instance implements
	 *         the interface, or {@code null} if it does not
	 * @since 1.0.0
	 * @see ActionPerformer
	 */
	default ActionPerformer getActionPerformer() {
		if (this instanceof ActionPerformer) {
			return (ActionPerformer) this;
		} else {
			return null;
		}
	}
}