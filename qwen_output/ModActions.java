package com.garward.wurmmodloader.modsupport.actions;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

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

/**
 * Manages custom actions and behaviours for Wurm Unlimited mods.
 * 
 * <p>This class provides the infrastructure for registering custom actions and behaviours
 * that can be used by mods to extend the game's functionality. It handles the registration
 * of {@link ActionEntry} objects, {@link ActionPerformer} implementations, and 
 * {@link BehaviourProvider} implementations.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Register a custom action
 * ActionEntry actionEntry = new ActionEntry((short) ModActions.getNextActionId(), true, 
 *     false, "My Custom Action", "", (short) 1, (short) 1, (short) 1);
 * ModActions.registerAction(actionEntry);
 * 
 * // Register a custom action performer
 * ModActions.registerActionPerformer(new MyCustomActionPerformer());
 * 
 * // Register a custom behaviour provider
 * ModActions.registerBehaviourProvider(new MyCustomBehaviourProvider());
 * }</pre>
 * 
 * <p><strong>Lifecycle:</strong> This class should be initialized during mod loading
 * by calling {@link #init()}. Action and behaviour registrations should occur
 * after initialization but before the game world is fully loaded.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe for registration operations
 * due to the use of concurrent collections. However, the {@link #init()} method should
 * only be called from the main server thread during mod initialization.</p>
 * 
 * @since 1.0.0
 */
public class ModActions {
	
	private static boolean inited = false;
	
	private static short lastServerActionId = 0;
	
	private static List<BehaviourProvider> behaviourProviders = new CopyOnWriteArrayList<>();
	private static ConcurrentHashMap<Short, ActionPerformerChain> actionPerformers = new ConcurrentHashMap<>();
	
	/**
	 * Gets the next available action ID for custom actions.
	 * 
	 * <p>This method returns the current length of the {@link Actions#actionEntrys} array,
	 * which represents the next available slot for a new action entry.</p>
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
	 * <p>This method sets {@code lastServerActionId} to the index of the last
	 * action entry in the {@link Actions#actionEntrys} array minus one.</p>
	 * 
	 * @since 1.0.0
	 */
	private static void initLastServerActionId() {
		if (lastServerActionId == 0) {
			lastServerActionId = (short) (Actions.actionEntrys.length - 1);
		}
	}
	
	/**
	 * Gets the last server action ID.
	 * 
	 * <p>This method returns the ID of the last action registered by the base game,
	 * which is useful for determining the boundary between vanilla and modded actions.</p>
	 * 
	 * @return the last server action ID
	 * @since 1.0.0
	 */
	public static short getLastServerActionId() {
		initLastServerActionId();
		return lastServerActionId;
	}
	
	/**
	 * Registers a new action entry.
	 * 
	 * <p>This method extends the {@link Actions#actionEntrys} array to include the
	 * provided action entry at the next available position. The action entry's number
	 * must match the current length of the array.</p>
	 * 
	 * <pre>{@code
	 * ActionEntry actionEntry = new ActionEntry((short) ModActions.getNextActionId(), 
	 *     true, false, "My Action", "", (short) 1, (short) 1, (short) 1);
	 * ModActions.registerAction(actionEntry);
	 * }</pre>
	 * 
	 * @param actionEntry the action entry to register
	 * @throws RuntimeException if the action entry's number doesn't match the expected index
	 * @since 1.0.0
	 */
	public static void registerAction(ActionEntry actionEntry) {
		
		initLastServerActionId();
		
		short number = actionEntry.getNumber();
		
		if (Actions.actionEntrys.length != number) {
			throw new RuntimeException(String.format("Trying to register an action with the wrong action number. Expected %d, got %d", Actions.actionEntrys.length, number));
		}
		
		ActionEntry[] newArray = Arrays.copyOf(Actions.actionEntrys, number + 1);
		newArray[number] = actionEntry;
		
		try {
			ReflectionUtil.setPrivateField(Actions.class, ReflectionUtil.getField(Actions.class, "actionEntrys"), newArray);
		} catch (IllegalAccessException | IllegalArgumentException | ClassCastException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Registers both action performer and behaviour provider from a {@link ModAction}.
	 * 
	 * <p>This convenience method registers both components of a {@link ModAction}
	 * by calling {@link #registerActionPerformer(ActionPerformer)} and 
	 * {@link #registerBehaviourProvider(BehaviourProvider)} with the respective
	 * components from the provided {@link ModAction}.</p>
	 * 
	 * @param testAction the mod action containing both performer and provider to register
	 * @since 1.0.0
	 */
	public static void registerAction(ModAction testAction) {
		registerActionPerformer(testAction.getActionPerformer());
		
		registerBehaviourProvider(testAction.getBehaviourProvider());
	}
	
	/**
	 * Registers an action performer for a specific action ID.
	 * 
	 * <p>This method associates an {@link ActionPerformer} with its action ID. When
	 * an action with that ID is executed, the registered performer will be invoked.
	 * Multiple performers can be registered for the same action ID, and they will
	 * be executed in a chain.</p>
	 * 
	 * <pre>{@code
	 * ModActions.registerActionPerformer(new MyCustomActionPerformer());
	 * }</pre>
	 * 
	 * @param actionPerformer the action performer to register, or null to do nothing
	 * @since 1.0.0
	 */
	public static void registerActionPerformer(ActionPerformer actionPerformer) {
		if (actionPerformer != null) {
			short actionId = actionPerformer.getActionId();
			actionPerformers.computeIfAbsent(actionId, num -> new ActionPerformerChain(num)).addActionPerformer(actionPerformer);
		}
	}
	
	/**
	 * Registers a behaviour provider.
	 * 
	 * <p>This method adds a {@link BehaviourProvider} to the list of providers that
	 * can modify or extend the default game behaviours. Each provider is consulted
	 * in order when behaviours are requested for game objects.</p>
	 * 
	 * <pre>{@code
	 * ModActions.registerBehaviourProvider(new MyCustomBehaviourProvider());
	 * }</pre>
	 * 
	 * @param behaviourProvider the behaviour provider to register, or null to do nothing
	 * @since 1.0.0
	 */
	public static void registerBehaviourProvider(BehaviourProvider behaviourProvider) {
		if (behaviourProvider != null && !behaviourProviders.contains(behaviourProvider)) {
			behaviourProviders.add(behaviourProvider);
		}
	}

	/**
	 * Initializes the mod actions system by applying necessary bytecode modifications.
	 * 
	 * <p>This method sets up the hooks required for custom actions and behaviours to
	 * function. It modifies the {@link Actions} and {@link com.wurmonline.server.behaviours.BehaviourDispatcher}
	 * classes to redirect calls to the modded implementations.</p>
	 * 
	 * <p><strong>Lifecycle:</strong> This method should be called once during mod
	 * initialization, typically in the mod's {@code onServerStarted()} method.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method should only be called from the
	 * main server thread during initialization.</p>
	 * 
	 * @throws RuntimeException if bytecode modification fails
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
	 * Gets the action performer for a given action.
	 * 
	 * <p>This method retrieves the registered {@link ActionPerformerBase} for the
	 * specified action based on its action ID. If no performer is registered,
	 * returns null.</p>
	 * 
	 * @param action the action for which to get the performer
	 * @return the registered action performer, or null if none is registered
	 * @since 1.0.0
	 */
	public static ActionPerformerBase getActionPerformer(Action action) {
		short actionId = action.getActionEntry().getNumber();
		return actionPerformers.get(actionId);
	}
	
	/**
	 * Gets a behaviour provider for a given behaviour.
	 * 
	 * <p>This method creates a chained behaviour provider that combines the
	 * provided behaviour with all registered {@link BehaviourProvider} instances.
	 * If no providers are registered, returns null.</p>
	 * 
	 * @param behaviour the base behaviour to wrap
	 * @return a chained behaviour provider, or null if no providers are registered
	 * @since 1.0.0
	 */
	public static BehaviourProvider getBehaviourProvider(Behaviour behaviour) {
		if (behaviourProviders == null || behaviourProviders.isEmpty()) {
			return null;
		}
		
		return new ChainedBehaviourProvider(new WrappedBehaviourProvider(behaviour), behaviourProviders);
	}
	
	/**
	 * Gets a skill from a creature, returning null if the skill doesn't exist.
	 * 
	 * <p>This utility method safely retrieves a skill from a creature's skill list.
	 * If the creature doesn't have the specified skill, it returns null instead
	 * of throwing a {@link NoSuchSkillException}.</p>
	 * 
	 * @param creature the creature from which to get the skill
	 * @param skillId the ID of the skill to retrieve
	 * @return the skill if it exists, or null if it doesn't
	 * @since 1.0.0
	 */
	public static Skill getSkillOrNull(Creature creature, int skillId) {
		try {
			return creature.getSkills().getSkill(skillId);
		} catch (NoSuchSkillException e) {
			return null;
		}
	}
}