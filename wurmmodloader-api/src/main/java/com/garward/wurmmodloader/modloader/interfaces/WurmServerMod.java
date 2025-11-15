package com.garward.wurmmodloader.modloader.interfaces;

import com.garward.wurmmodloader.api.interfaces.Versioned;

/**
 * The primary interface that all Wurm Unlimited server mods must implement.
 * <p>
 * This interface provides lifecycle hooks for mod initialization and configuration.
 * Mods are loaded in two phases:
 * <ol>
 *   <li><b>preInit()</b> - Called before server initialization. This is where mods should
 *       register bytecode hooks, modify class behavior, and perform any operations that
 *       need to happen before the Wurm server initializes its core systems.</li>
 *   <li><b>init()</b> - Called during server initialization, after the Wurm server has
 *       initialized its core systems but before the server starts. This is where mods
 *       should register custom content, actions, items, and perform standard setup.</li>
 * </ol>
 * </p>
 * <p>
 * Basic mod structure:
 * <pre>{@code
 * public class MyMod implements WurmServerMod {
 *     @Override
 *     public void preInit() {
 *         // Register bytecode hooks here
 *     }
 *
 *     @Override
 *     public void init() {
 *         // Register custom content here
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * Mods can also implement additional listener interfaces to receive notifications
 * about server events:
 * <ul>
 *   <li>{@link ServerStartedListener} - Called when the server finishes starting</li>
 *   <li>{@link ItemTemplatesCreatedListener} - Called after item templates are loaded</li>
 *   <li>{@link PlayerLoginListener} - Called when a player logs in</li>
 *   <li>{@link ServerShutdownListener} - Called when the server is shutting down</li>
 * </ul>
 * </p>
 *
 * @see Versioned
 * @see ServerStartedListener
 * @see ItemTemplatesCreatedListener
 * @since 1.0.0
 */
public interface WurmServerMod extends Versioned {

	/**
	 * Called during server initialization, after core systems are loaded.
	 * <p>
	 * This method is called after {@link #preInit()} and after the Wurm server
	 * has initialized its core systems. Use this method to:
	 * <ul>
	 *   <li>Register custom items using {@code ItemTemplateBuilder}</li>
	 *   <li>Register custom creatures using {@code CreatureTemplateBuilder}</li>
	 *   <li>Register custom actions using {@code ModActions}</li>
	 *   <li>Perform standard mod setup and configuration</li>
	 * </ul>
	 * </p>
	 * <p>
	 * The default implementation does nothing. Mods should override this method
	 * if they need to perform initialization tasks.
	 * </p>
	 */
	public default void init() {
	}

	/**
	 * Called before server initialization, for registering bytecode hooks.
	 * <p>
	 * This method is called before the Wurm server initializes its core systems.
	 * It is the earliest point in the mod loading lifecycle and is specifically
	 * designed for operations that must happen before server initialization:
	 * <ul>
	 *   <li>Registering bytecode hooks using HookManager</li>
	 *   <li>Modifying server class behavior with Javassist</li>
	 *   <li>Initializing action systems with {@code ModActions.init()}</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <b>Warning:</b> Most Wurm server classes are not yet loaded at this point.
	 * Attempting to access Wurm server objects or templates will likely fail.
	 * Use {@link #init()} for standard initialization tasks.
	 * </p>
	 * <p>
	 * The default implementation does nothing. Mods should override this method
	 * only if they need to perform bytecode manipulation or early initialization.
	 * </p>
	 *
	 */
	public default void preInit() {
	}

}
