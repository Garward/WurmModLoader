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
// import com.wurmonline.server.creatures.Creature; // REMOVED: Import triggers class loading before ModCreatures can add callbacks
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

public class ModActions {

	private static final Logger LOGGER = Logger.getLogger(ModActions.class.getName());

	private static boolean inited = false;
	
	private static short lastServerActionId = 0;
	
	private static List<BehaviourProvider> behaviourProviders = new CopyOnWriteArrayList<>();
	private static ConcurrentHashMap<Short, ActionPerformerChain> actionPerformers = new ConcurrentHashMap<>();
	
	public static int getNextActionId() {
		try {
			// LAZY LOADING: Only use reflection-based approach after HookManager is ready
			// During early initialization (mod loading), fall back to direct reference
			if (HookManager.getInstance() != null && HookManager.getInstance().getLoader() != null) {
				ClassLoader gameClassLoader = HookManager.getInstance().getLoader();
				Class<?> actionsClass = Class.forName("com.wurmonline.server.behaviours.Actions", true, gameClassLoader);
				java.lang.reflect.Field actionEntrysField = actionsClass.getDeclaredField("actionEntrys");
				actionEntrysField.setAccessible(true);
				Object[] array = (Object[]) actionEntrysField.get(null);
				return array.length;
			} else {
				// Fallback during early initialization
				return Actions.actionEntrys.length;
			}
		} catch (Exception e) {
			// If reflection fails, fall back to direct reference
			return Actions.actionEntrys.length;
		}
	}

	private static void initLastServerActionId() {
		if (lastServerActionId == 0) {
			lastServerActionId = (short) (getNextActionId() - 1);
		}
	}
	
	public static short getLastServerActionId() {
		initLastServerActionId();
		return lastServerActionId;
	}
	
	public static void registerAction(ActionEntry actionEntry) {

		initLastServerActionId();

		short number = actionEntry.getNumber();

		try {
			// CLASSLOADER-SAFE EXPANSION: Use HookManager's loader, NEVER reference Wurm classes directly!
			// Direct references like Actions.class cause the class to be loaded by URLClassLoader
			// We must load ALL Wurm classes through HookManager's Javassist Loader
			ClassLoader gameClassLoader = HookManager.getInstance().getLoader();
			Class<?> actionsClass = Class.forName("com.wurmonline.server.behaviours.Actions", true, gameClassLoader);

			// Get actionEntrys from the GAME classloader's Actions class
			// Use Object[] to avoid any direct ActionEntry class references (100% classloader-safe)
			java.lang.reflect.Field actionEntrysField = actionsClass.getDeclaredField("actionEntrys");
			actionEntrysField.setAccessible(true);
			Object[] gameArray = (Object[]) actionEntrysField.get(null);

			// DIAGNOSTIC: Log classloaders and ALL array info BEFORE expansion
			LOGGER.severe("==================== CLASSLOADER DIAGNOSTIC (BEFORE) ====================");
			LOGGER.severe("[ModActions] ModActions.class classloader: " + ModActions.class.getClassLoader());
			LOGGER.severe("[ModActions] HookManager's game classloader: " + gameClassLoader);

			// Try to load Action class through game classloader to verify
			try {
				Class<?> actionClass = Class.forName("com.wurmonline.server.behaviours.Action", true, gameClassLoader);
				LOGGER.severe("[ModActions] Action.class.getClassLoader(): " + actionClass.getClassLoader());
			} catch (Exception e) {
				LOGGER.severe("[ModActions] Could not load Action class: " + e.getMessage());
			}

			LOGGER.severe("[ModActions] BEFORE expansion:");
			LOGGER.severe("[ModActions]   actionEntrys.length = " + gameArray.length);
			LOGGER.severe("[ModActions]   actionEntrys identity = " + System.identityHashCode(gameArray));

			// Check parallel arrays
			try {
				java.lang.reflect.Field actionStringsField = actionsClass.getDeclaredField("actionStrings");
				actionStringsField.setAccessible(true);
				Object actionStringsArray = actionStringsField.get(null);
				if (actionStringsArray != null && actionStringsArray.getClass().isArray()) {
					LOGGER.severe("[ModActions]   actionStrings.length = " + java.lang.reflect.Array.getLength(actionStringsArray));
					LOGGER.severe("[ModActions]   actionStrings identity = " + System.identityHashCode(actionStringsArray));
				}
			} catch (NoSuchFieldException e) {
				LOGGER.severe("[ModActions]   actionStrings field not found");
			}

			try {
				java.lang.reflect.Field verbStringsField = actionsClass.getDeclaredField("verbStrings");
				verbStringsField.setAccessible(true);
				Object verbStringsArray = verbStringsField.get(null);
				if (verbStringsArray != null && verbStringsArray.getClass().isArray()) {
					LOGGER.severe("[ModActions]   verbStrings.length = " + java.lang.reflect.Array.getLength(verbStringsArray));
					LOGGER.severe("[ModActions]   verbStrings identity = " + System.identityHashCode(verbStringsArray));
				}
			} catch (NoSuchFieldException e) {
				LOGGER.severe("[ModActions]   verbStrings field not found");
			}

			LOGGER.severe("[ModActions] Registering action " + number + " via HookManager's classloader");
			LOGGER.severe("===============================================================");

			if (gameArray.length != number) {
				throw new RuntimeException(String.format("Trying to register an action with the wrong action number. Expected %d, got %d", gameArray.length, number));
			}

			// Expand the GAME classloader's array (use Object[] to avoid class loading)
			Object[] newArray = Arrays.copyOf(gameArray, number + 1);
			newArray[number] = actionEntry; // actionEntry is our mod's instance
			actionEntrysField.set(null, newArray);

			LOGGER.severe("[ModActions] ==================== AFTER EXPANSION ====================");
			LOGGER.severe("[ModActions]   actionEntrys.length = " + newArray.length);
			LOGGER.severe("[ModActions]   actionEntrys identity = " + System.identityHashCode(newArray));
			LOGGER.severe("[ModActions]   actionEntrys[" + number + "] = " + (newArray[number] != null ? newArray[number] : "NULL!"));

			// ALSO expand parallel arrays that index by action ID
			expandStringArrayField(actionsClass, "actionStrings", number + 1);
			expandStringArrayField(actionsClass, "verbStrings", number + 1);

			// Re-check parallel arrays after our expansion code runs
			try {
				java.lang.reflect.Field actionStringsField = actionsClass.getDeclaredField("actionStrings");
				actionStringsField.setAccessible(true);
				Object actionStringsArray = actionStringsField.get(null);
				if (actionStringsArray != null && actionStringsArray.getClass().isArray()) {
					LOGGER.severe("[ModActions]   actionStrings.length = " + java.lang.reflect.Array.getLength(actionStringsArray));
					LOGGER.severe("[ModActions]   actionStrings identity = " + System.identityHashCode(actionStringsArray));
				}
			} catch (NoSuchFieldException e) {
				LOGGER.severe("[ModActions]   actionStrings field not found");
			}

			try {
				java.lang.reflect.Field verbStringsField = actionsClass.getDeclaredField("verbStrings");
				verbStringsField.setAccessible(true);
				Object verbStringsArray = verbStringsField.get(null);
				if (verbStringsArray != null && verbStringsArray.getClass().isArray()) {
					LOGGER.severe("[ModActions]   verbStrings.length = " + java.lang.reflect.Array.getLength(verbStringsArray));
					LOGGER.severe("[ModActions]   verbStrings identity = " + System.identityHashCode(verbStringsArray));
				}
			} catch (NoSuchFieldException e) {
				LOGGER.severe("[ModActions]   verbStrings field not found");
			}
			LOGGER.severe("[ModActions] ================================================================");

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
					LOGGER.info("[ModActions] Expanded " + fieldName + " from " + oldArray.length + " to " + newArray.length);
				}
			}
		} catch (NoSuchFieldException e) {
			// Field doesn't exist in this WU version - safe to skip
			LOGGER.fine("[ModActions] Field " + fieldName + " not found - skipping");
		} catch (IllegalAccessException e) {
			LOGGER.warning("[ModActions] Could not expand " + fieldName + ": " + e.getMessage());
		}
	}

	public static void registerAction(ModAction testAction) {
		registerActionPerformer(testAction.getActionPerformer());
		
		registerBehaviourProvider(testAction.getBehaviourProvider());
	}
	
	public static void registerActionPerformer(ActionPerformer actionPerformer) {
		if (actionPerformer != null) {
			short actionId = actionPerformer.getActionId();
			actionPerformers.computeIfAbsent(actionId, num -> new ActionPerformerChain(num)).addActionPerformer(actionPerformer);
		}
	}
	
	public static void registerBehaviourProvider(BehaviourProvider behaviourProvider) {
		if (behaviourProvider != null && !behaviourProviders.contains(behaviourProvider)) {
			behaviourProviders.add(behaviourProvider);
		}
	}

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
								"    com.garward.wurmmodloader.modsupport.actions.ActionPerformerBase actionPerformer = com.garward.wurmmodloader.modsupport.actions.ModActions.getActionPerformer(this);\n" +
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
	
	public static ActionPerformerBase getActionPerformer(Action action) {
		short actionId = action.getActionEntry().getNumber();
		return actionPerformers.get(actionId);
	}
	
	public static BehaviourProvider getBehaviourProvider(Behaviour behaviour) {
		if (behaviourProviders == null || behaviourProviders.isEmpty()) {
			return null;
		}
		
		return new ChainedBehaviourProvider(new WrappedBehaviourProvider(behaviour), behaviourProviders);
	}
	
	/**
	 * Get a skill from a creature, or null if the creature doesn't have it.
	 * Uses Object parameter to avoid loading Creature class during ModActions initialization.
	 *
	 * @param creature Creature object (as Object to avoid class loading)
	 * @param skillId Skill ID
	 * @return Skill or null
	 */
	public static Skill getSkillOrNull(Object creature, int skillId) {
		try {
			// Use reflection to avoid importing Creature (which would trigger class loading)
			Object skills = ReflectionUtil.callPrivateMethod(creature, ReflectionUtil.getMethod(creature.getClass(), "getSkills"));
			return (Skill) ReflectionUtil.callPrivateMethod(skills, skills.getClass().getMethod("getSkill", int.class), skillId);
		} catch (Exception e) {
			// NoSuchSkillException wrapped in reflection exception
			if (e.getCause() instanceof NoSuchSkillException) {
				return null;
			}
			throw new RuntimeException("Failed to get skill from creature", e);
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
	 */
	public static ActionEntry getAction(int actionId) {
		try {
			if (actionId < 0 || actionId >= Actions.actionEntrys.length) {
				return null;
			}
			return Actions.actionEntrys[actionId];
		} catch (Exception e) {
			LOGGER.warning("Failed to get action " + actionId + ": " + e.getMessage());
			return null;
		}
	}
}
