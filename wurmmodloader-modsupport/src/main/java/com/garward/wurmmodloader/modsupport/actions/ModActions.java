package com.garward.wurmmodloader.modsupport.actions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.garward.wurmmodloader.modloader.internal.ReflectionUtil;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookManager;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.Behaviour;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.skills.NoSuchSkillException;
import com.wurmonline.server.skills.Skill;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import java.util.logging.Logger;

/**
 * Manages custom actions and behaviours for mods in the Wurm Unlimited server.
 *
 * This class provides functionality to register custom actions and behaviours that can be used by mods.
 * It handles the integration of modded actions into the game's action system through bytecode manipulation
 * and runtime hooking. The class maintains registries of action performers and behaviour providers,
 * allowing mods to extend the game's functionality seamlessly.
 *
 * Usage example:
 * <pre>{@code
 * // Register a custom action
 * ActionEntry actionEntry = new ActionEntry((short) ModActions.getNextActionId(), "Custom Action", "performing custom action", new int[]{});
 * ModActions.registerAction(actionEntry);
 *
 * // Register a custom action performer
 * ModActions.registerActionPerformer(new CustomActionPerformer(actionEntry.getNumber()));
 *
 * // Register a behaviour provider
 * ModActions.registerBehaviourProvider(new CustomBehaviourProvider());
 * }</pre>
 *
 * Thread Safety: This class uses thread-safe collections (CopyOnWriteArrayList and ConcurrentHashMap)
 * for its internal data structures, making it safe for concurrent access from multiple threads.
 * However, the initialization process should only be called once during server startup.
 *
 * Lifecycle: This class should be initialized during the mod loading phase before any game actions
 * are processed. The init() method sets up necessary hooks and should be called once during server startup.
 *
 * @since 1.0.0
 */
public class ModActions {

	private static final Logger logger = Logger.getLogger(ModActions.class.getName());

	private static boolean inited = false;

	private static short lastServerActionId = 0;

	private static List<BehaviourProvider> behaviourProviders = new CopyOnWriteArrayList<>();
	private static ConcurrentHashMap<Short, ActionPerformerChain> actionPerformers = new ConcurrentHashMap<>();

	/**
	 * Gets the next available action ID for registering custom actions.
	 *
	 * This method returns the current length of the Actions.actionEntrys array,
	 * which represents the next available slot for a new action entry.
	 *
	 * @return the next available action ID
	 * @since 1.0.0
	 */
	public static int getNextActionId() {
		return Actions.actionEntrys.length;
	}

	/**
	 * Initializes the last server action ID if not already set.
	 *
	 * This method ensures that the lastServerActionId field is properly initialized
	 * by setting it to the length of the Actions.actionEntrys array minus one.
	 *
	 * @since 1.0.0
	 */
	private static void initLastServerActionId() {
		if (lastServerActionId == 0) {
			lastServerActionId = (short) (Actions.actionEntrys.length - 1);
		}
	}

	/**
	 * Gets the last server action ID that was registered.
	 *
	 * Returns the highest action ID currently registered in the server's action system.
	 * This value represents the boundary between vanilla actions and custom mod actions.
	 *
	 * @return the last server action ID
	 * @since 1.0.0
	 */
	public static short getLastServerActionId() {
		initLastServerActionId();
		return lastServerActionId;
	}

	/**
	 * Registers a new action entry in the server's action system.
	 *
	 * This method adds a new ActionEntry to the global Actions.actionEntrys array.
	 * The action entry must have the correct action number corresponding to the next
	 * available slot in the array, otherwise a RuntimeException will be thrown.
	 *
	 * @param actionEntry the ActionEntry to register
	 * @throws RuntimeException if the action number doesn't match the expected next index
	 * @since 1.0.0
	 */
	public static void registerAction(ActionEntry actionEntry) {

		initLastServerActionId();

		short number = actionEntry.getNumber();

		try {
			// CLASSLOADER-SAFE EXPANSION: Use HookManager's loader, NEVER reference Wurm classes directly!
			// Direct references like Action.class cause the class to be loaded by URLClassLoader
			// We must load ALL Wurm classes through HookManager's Javassist Loader
			ClassLoader gameClassLoader = HookManager.getInstance().getLoader();
			Class<?> actionsClass = Class.forName("com.wurmonline.server.behaviours.Actions", true, gameClassLoader);

			// Get actionEntrys from the GAME classloader's Actions class
			java.lang.reflect.Field actionEntrysField = actionsClass.getDeclaredField("actionEntrys");
			actionEntrysField.setAccessible(true);
			ActionEntry[] gameArray = (ActionEntry[]) actionEntrysField.get(null);

			// DIAGNOSTIC: Log classloaders and array identity
			logger.severe("==================== CLASSLOADER DIAGNOSTIC ====================");
			logger.severe("[ModActions] ModActions.class classloader: " + ModActions.class.getClassLoader());
			logger.severe("[ModActions] HookManager's game classloader: " + gameClassLoader);
			logger.severe("[ModActions] Game Actions.actionEntrys length: " + gameArray.length);
			logger.severe("[ModActions] Game array identity: " + System.identityHashCode(gameArray));
			logger.severe("[ModActions] Registering action " + number + " via HookManager's classloader");
			logger.severe("===============================================================");

			if (gameArray.length != number) {
				throw new RuntimeException(String.format("Trying to register an action with the wrong action number. Expected %d, got %d", gameArray.length, number));
			}

			// Expand the GAME classloader's array
			ActionEntry[] newArray = Arrays.copyOf(gameArray, number + 1);
			newArray[number] = actionEntry;
			actionEntrysField.set(null, newArray);

			logger.severe("[ModActions] Successfully expanded GAME actionEntrys from " + gameArray.length + " to " + newArray.length);
			logger.severe("[ModActions] New array identity: " + System.identityHashCode(newArray));

			// ALSO expand parallel arrays that index by action ID
			expandStringArrayField(actionsClass, "actionStrings", number + 1);
			expandStringArrayField(actionsClass, "verbStrings", number + 1);

		} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Failed to register action via game classloader", e);
		}
	}

	/**
	 * Expands a parallel String array field in the Actions class to accommodate custom actions.
	 */
	private static void expandStringArrayField(Class<?> actionsClass, String fieldName, int minSize) {
		try {
			java.lang.reflect.Field field = actionsClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			Object obj = field.get(null);

			if (obj instanceof String[]) {
				String[] oldArray = (String[]) obj;
				if (oldArray.length < minSize) {
					String[] newArray = Arrays.copyOf(oldArray, minSize);
					field.set(null, newArray);
					logger.info("[ModActions] Expanded " + fieldName + " from " + oldArray.length + " to " + newArray.length);
				}
			}
		} catch (NoSuchFieldException e) {
			// Field doesn't exist in this WU version - safe to skip
			logger.fine("[ModActions] Field " + fieldName + " not found - skipping");
		} catch (IllegalAccessException e) {
			logger.warning("[ModActions] Could not expand " + fieldName + ": " + e.getMessage());
		}
	}

	/**
	 * Registers a complete mod action including both action performer and behaviour provider.
	 *
	 * This convenience method registers both components of a ModAction - the action performer
	 * responsible for executing the action and the behaviour provider responsible for determining
	 * when the action should be available.
	 *
	 * @param testAction the ModAction containing both performer and provider components
	 * @since 1.0.0
	 */
	public static void registerAction(ModAction testAction) {
		registerActionPerformer(testAction.getActionPerformer());

		registerBehaviourProvider(testAction.getBehaviourProvider());
	}

	/**
	 * Registers an action performer for handling custom actions.
	 *
	 * Adds an ActionPerformer to the registry, which will be invoked when actions
	 * with the corresponding action ID are executed. If an action performer with
	 * the same action ID already exists, the new performer will be added to a chain
	 * to be executed sequentially.
	 *
	 * @param actionPerformer the ActionPerformer to register, or null to do nothing
	 * @since 1.0.0
	 */
	public static void registerActionPerformer(ActionPerformer actionPerformer) {
		if (actionPerformer != null) {
			short actionId = actionPerformer.getActionId();
			actionPerformers.computeIfAbsent(actionId, num -> new ActionPerformerChain(num)).addActionPerformer(actionPerformer);
		}
	}

	/**
	 * Registers a behaviour provider for determining action availability.
	 *
	 * Adds a BehaviourProvider to the registry, which will be consulted when determining
	 * what actions should be available for a given context. Each provider is only added
	 * once to prevent duplicates in the processing chain.
	 *
	 * @param behaviourProvider the BehaviourProvider to register, or null to do nothing
	 * @since 1.0.0
	 */
	public static void registerBehaviourProvider(BehaviourProvider behaviourProvider) {
		if (behaviourProvider != null && !behaviourProviders.contains(behaviourProvider)) {
			behaviourProviders.add(behaviourProvider);
		}
	}

	/**
	 * Initializes the mod actions system by setting up necessary bytecode hooks.
	 *
	 * This method performs the core setup for the mod actions system by:
	 * 1. Removing the final modifier from the Actions.actionEntrys field
	 * 2. Instrumenting BehaviourDispatcher methods to intercept behaviour requests
	 * 3. Instrumenting Action.poll() method to intercept action execution
	 *
	 * This method should only be called once during server startup. Subsequent calls
	 * will return immediately without performing any operations.
	 *
	 * @throws RuntimeException if bytecode manipulation fails
	 * @since 1.0.0
	 */
	public static void init() {
		if (inited)
			return;
		
		try {
			final ClassPool classPool = HookManager.getInstance().getClassPool();
			
			CtClass ctActions = classPool.get("com.wurmonline.server.behaviours.Actions");
			CtField ctActionEntrys = ctActions.getField("actionEntrys");
			ctActionEntrys.setModifiers(Modifier.clear(ctActionEntrys.getModifiers(), Modifier.FINAL));
			
			CtClass ctBehaviourDispatcher  = classPool.get("com.wurmonline.server.behaviours.BehaviourDispatcher");
			for (CtMethod method : ctBehaviourDispatcher.getDeclaredMethods()) {
				method.instrument(new ExprEditor() {
					@Override
					public void edit(MethodCall m) throws CannotCompileException {
						if (m.getClassName().equals("com.wurmonline.server.behaviours.Behaviour") && m.getMethodName().equals("getBehavioursFor")) {
							String code =
									"{\n" +
									"    org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider behaviourProvider = org.gotti.wurmunlimited.modsupport.actions.ModActions.getBehaviourProvider($0);\n" +
									"    if (behaviourProvider != null) {\n" +
									"        $_ = behaviourProvider.getBehavioursFor($$);\n" +
									"    } else {\n" +
									"        $_ = $proceed($$);\n" +
									"    }\n" +
									"}\n";
							m.replace(code);
						}
					}
				});
			}
			
			ctBehaviourDispatcher.getDeclaredMethod("requestActionForSkillIds").instrument(new ExprEditor() {
				
				@Override
				public void edit(FieldAccess f) throws CannotCompileException {
					if (f.getClassName().equals("com.wurmonline.server.behaviours.BehaviourDispatcher") && f.getFieldName().equals("emptyActions")) {
						String code =
								"{\n" +
								"    com.wurmonline.server.creatures.Creature creature = comm.getPlayer();\n" +
								"    com.wurmonline.server.behaviours.Behaviour behaviour = com.wurmonline.server.behaviours.Action.getBehaviour(target, creature.isOnSurface());\n" +
								"    org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider behaviourProvider = org.gotti.wurmunlimited.modsupport.actions.ModActions.getBehaviourProvider(behaviour);\n" +
								"    com.wurmonline.server.skills.Skill skill = org.gotti.wurmunlimited.modsupport.actions.ModActions.getSkillOrNull(creature, skillid);\n" +
								"    if (skill != null && behaviourProvider != null) {\n" +
								"        $_ = behaviourProvider.getBehavioursFor(creature, skill);\n" +
								"    } else {\n" +
								"        $_ = $proceed();\n" +
								"    }\n" +
								"}\n";
						f.replace(code);
					}
				}
				
			});
			
			classPool.get("com.wurmonline.server.behaviours.Action").getMethod("poll", "()Z").instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					if (m.getClassName().equals("com.wurmonline.server.behaviours.Behaviour") && m.getMethodName().equals("action")) {
						String code =
								"{\n" +
								"    org.gotti.wurmunlimited.modsupport.actions.ActionPerformerBase actionPerformer = org.gotti.wurmunlimited.modsupport.actions.ModActions.getActionPerformer(this);\n" +
								"    if (actionPerformer != null) {\n" +
								"        $_ = actionPerformer.action($$);\n" +
								"    } else {\n" +
								"        $_ = $proceed($$);\n" +
								"    }\n" +
								"}\n";
						m.replace(code);
					}
				}
			});
			
			
			
			inited = true;
		} catch (NotFoundException | CannotCompileException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the action performer associated with a specific action.
	 *
	 * Looks up the ActionPerformerBase instance registered for the given action's ID.
	 * If no performer is registered for that ID, returns null.
	 *
	 * @param action the Action to look up the performer for
	 * @return the registered ActionPerformerBase, or null if none found
	 * @since 1.0.0
	 */
	public static ActionPerformerBase getActionPerformer(Action action) {
		short actionId = action.getActionEntry().getNumber();
		return actionPerformers.get(actionId);
	}

	/**
	 * Retrieves a chained behaviour provider for a given behaviour.
	 *
	 * Creates a composite behaviour provider that combines the original behaviour
	 * with all registered mod behaviour providers. This allows mods to extend
	 * or override the default behaviour without completely replacing it.
	 *
	 * @param behaviour the base Behaviour to wrap
	 * @return a ChainedBehaviourProvider combining all registered providers, or null if none registered
	 * @since 1.0.0
	 */
	public static BehaviourProvider getBehaviourProvider(Behaviour behaviour) {
		if (behaviourProviders == null || behaviourProviders.isEmpty()) {
			return null;
		}

		return new ChainedBehaviourProvider(new WrappedBehaviourProvider(behaviour), behaviourProviders);
	}

	/**
	 * Retrieves a skill from a creature, returning null instead of throwing an exception.
	 *
	 * Attempts to retrieve a skill by ID from a creature's skill collection. Unlike the
	 * standard getSkill() method, this method returns null if the skill doesn't exist
	 * rather than throwing a NoSuchSkillException.
	 *
	 * @param creature the Creature to retrieve the skill from
	 * @param skillId the ID of the skill to retrieve
	 * @return the requested Skill, or null if the creature doesn't have that skill
	 * @since 1.0.0
	 */
	public static Skill getSkillOrNull(Creature creature, int skillId) {
		try {
			return creature.getSkills().getSkill(skillId);
		} catch (NoSuchSkillException e) {
			return null;
		}
	}

	/**
	 * Get a registered action entry by action ID.
	 *
	 * <p>This allows mods to retrieve ActionEntry objects without importing the Wurm class directly.
	 * Useful for adding actions to event menus (e.g., BodyMenuPopulateEvent.addMenuItem()).</p>
	 *
	 * <p><strong>Example:</strong></p>
	 * <pre>{@code
	 * @SubscribeEvent
	 * public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
	 *     // No need to import ActionEntry!
	 *     event.addMenuItem(ModActions.getAction(MY_ACTION_ID));
	 * }
	 * }</pre>
	 *
	 * @param actionId The action ID
	 * @return The ActionEntry, or null if not found
	 * @since 1.0.0
	 */
	public static ActionEntry getAction(int actionId) {
		try {
			if (actionId < 0 || actionId >= Actions.actionEntrys.length) {
				return null;
			}
			return Actions.actionEntrys[actionId];
		} catch (Exception e) {
			logger.warning("Failed to get action " + actionId + ": " + e.getMessage());
			return null;
		}
	}
}
