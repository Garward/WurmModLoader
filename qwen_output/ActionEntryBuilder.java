package com.garward.wurmmodloader.modsupport.actions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;

import com.wurmonline.server.behaviours.ActionEntry;

/**
 * Builder for creating {@link ActionEntry} instances with a fluent API.
 * 
 * <p>This class provides a convenient way to construct {@link ActionEntry} objects
 * which represent actions that players can perform in the game. It follows the 
 * builder pattern to allow for flexible and readable action creation.</p>
 * 
 * <p><strong>Usage example:</strong></p>
 * <pre>{@code
 * // Initialize the builder (must be done after server starts)
 * ActionEntryBuilder.init();
 * 
 * // Create a simple action
 * ActionEntry digAction = new ActionEntryBuilder((short) 100, "Dig", "digging")
 *     .priority(5)
 *     .range(2)
 *     .build();
 *     
 * // Create an action with types
 * int[] types = {ActionTypes.ACTION_TYPE_ATTACK, ActionTypes.ACTION_TYPE_QUICK};
 * ActionEntry attackAction = new ActionEntryBuilder((short) 101, "Attack", "attacking", types)
 *     .priority(3)
 *     .range(4)
 *     .build();
 * }</pre>
 * 
 * <p><strong>Lifecycle:</strong> This builder must be initialized by calling {@link #init()}
 * after the server has started but before any action entries are built. This is typically
 * done in a mod's {@code init()} method.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. All operations should
 * be performed on the main server thread or with external synchronization.</p>
 * 
 * @since 1.0.0
 * @see ActionEntry
 * @see ActionTypes
 */
public class ActionEntryBuilder {

	private static Constructor<ActionEntry> constructor;

	private short number;

	private int priority;

	private String actionString;

	private String verbString;

	private String animationString;

	private int[] types;

	private int range;

	private boolean blockedByUseOnGroundOnly;

	/**
	 * Internal constructor. Preset values from public constructors.
	 * 
	 * @param aNumber the action number
	 * @param aPriority the action priority
	 * @param aActionString the action string
	 * @param aVerbString the verb string
	 * @param aTypes the action types array
	 * @param aRange the action range
	 * @param blockedByUseOnGroundOnly whether the action is blocked by use on ground only
	 * @throws HookException if the builder has not been initialized via {@link #init()}
	 * @since 1.0.0
	 */
	private ActionEntryBuilder(short aNumber, int aPriority, String aActionString, String aVerbString, int[] aTypes, int aRange, boolean blockedByUseOnGroundOnly) {
		if (constructor == null) {
			throw new HookException("ActionEntryBuilder should be called after the server started");
		}
		
		number(aNumber);
		priority(aPriority);
		actionString(aActionString);
		verbString(aVerbString);
		animationString(aActionString);
		types(aTypes);
		range(aRange);
		blockedByUseOnGroundOnly(blockedByUseOnGroundOnly);
	}

	/**
	 * Create builder with preset number, action and verb string.
	 * 
	 * @param number Action number
	 * @param actionString Action name (i.e. Dig)
	 * @param verbString Action verb (i.e. digging)
	 * @since 1.0.0
	 */
	public ActionEntryBuilder(short number, String actionString, String verbString) {
		this(number, 5, actionString, verbString, (int[]) null, 4, true);
	}

	/**
	 * Create builder with preset number, types, action and verb string.
	 * 
	 * @param number Action number
	 * @param actionString Action name (i.e. Dig)
	 * @param verbString Action verb (i.e. digging)
	 * @param types Action types. See {@link ActionTypes}
	 * @since 1.0.0
	 */
	public ActionEntryBuilder(short number, String actionString, String verbString, int[] types) {
		this(number, 5, actionString, verbString, types, 2, true);
	}

	/**
	 * Set the action number.
	 * 
	 * @param number the action number to set
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	private ActionEntryBuilder number(short number) {
		this.number = number;
		return this;
	}

	/**
	 * Set the priority.
	 * 
	 * <p>Priority determines the order in which actions appear in menus.
	 * Lower numbers have higher priority.</p>
	 * 
	 * @param priority the priority to set
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ActionEntryBuilder priority(int priority) {
		this.priority = priority;
		return this;
	}

	/**
	 * Set the action name. I.e. "Dig"
	 * 
	 * <p>This is the display name of the action as it appears in menus.</p>
	 * 
	 * @param actionString the action name to set
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ActionEntryBuilder actionString(String actionString) {
		this.actionString = actionString;
		return this;
	}

	/**
	 * Set the action verb. I.e. "digging"
	 * 
	 * <p>This is used for descriptive text such as "You are digging".</p>
	 * 
	 * @param verbString the action verb to set
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ActionEntryBuilder verbString(String verbString) {
		this.verbString = verbString;
		return this;
	}

	/**
	 * Set the animation string if different from the action verb.
	 * 
	 * <p>This controls which animation is played when the action is performed.
	 * If not set, it defaults to the action string.</p>
	 * 
	 * @param animationString the animation string to set
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ActionEntryBuilder animationString(String animationString) {
		this.animationString = animationString;
		return this;
	}

	/**
	 * Set action types. See {@link ActionTypes}.
	 * 
	 * <p>Action types control various behaviors of the action such as whether
	 * it can be performed quickly, if it's an attack, etc.</p>
	 * 
	 * @param types the action types to set
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 * @see ActionTypes
	 */
	public ActionEntryBuilder types(int[] types) {
		this.types = types;
		return this;
	}

	/**
	 * Set the range.
	 * 
	 * <p>Determines how far away the target can be for this action to be valid.</p>
	 * 
	 * @param range the range to set
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ActionEntryBuilder range(int range) {
		this.range = range;
		return this;
	}

	/**
	 * Set blocked by use on ground.
	 * 
	 * <p>Controls whether this action is blocked when the target is on the ground
	 * and the player has the "use on ground only" restriction.</p>
	 * 
	 * @param blockedByUseOnGroundOnly whether the action is blocked by use on ground only
	 * @return this builder instance for method chaining
	 * @since 1.0.0
	 */
	public ActionEntryBuilder blockedByUseOnGroundOnly(boolean blockedByUseOnGroundOnly) {
		this.blockedByUseOnGroundOnly = blockedByUseOnGroundOnly;
		return this;
	}

	/**
	 * Build the ActionEntry.
	 * 
	 * <p>Creates and returns the configured {@link ActionEntry} instance.
	 * This method uses reflection to invoke the private constructor of ActionEntry.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method should only be called from the main
	 * server thread as it interacts with game objects.</p>
	 * 
	 * @return the constructed ActionEntry
	 * @throws HookException if there is an error during construction
	 * @since 1.0.0
	 */
	public ActionEntry build() {
		try {
			ActionEntry actionEntry = ReflectionUtil.callPrivateConstructor(constructor, number, priority, actionString, verbString, animationString, types, range, blockedByUseOnGroundOnly);
			return actionEntry;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new HookException(e);
		}
	}

	/**
	 * Resolve ActionEntry constructor.
	 * 
	 * <p>This method must be called after the server has started but before
	 * any {@link ActionEntry} instances are built. It uses reflection to
	 * obtain a reference to the private ActionEntry constructor.</p>
	 * 
	 * <p><strong>Lifecycle:</strong> This method should be called once during
	 * mod initialization, typically in the mod's {@code init()} method.</p>
	 * 
	 * <p><strong>Usage example:</strong></p>
	 * <pre>{@code
	 * public class MyMod implements WurmServerMod {
	 *     public void init() {
	 *         ActionEntryBuilder.init();
	 *         // ... rest of initialization
	 *     }
	 * }
	 * }</pre>
	 * 
	 * @throws HookException if the ActionEntry constructor cannot be found
	 * @since 1.0.0
	 */
	public static void init() {
		Class<?>[] parameterTypes = {
				short.class /* number */,
				int.class /* priority */,
				String.class /* actionString */,
				String.class /* verbString */,
				String.class /* animationString */,
				int[].class /* types */,
				int.class /* range */,
				boolean.class /* blockedByUseOnGround */
		};
		Class<ActionEntry> clazz = ActionEntry.class;

		try {
			ActionEntryBuilder.constructor = clazz.getDeclaredConstructor(parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new HookException(e);
		}
	}
}