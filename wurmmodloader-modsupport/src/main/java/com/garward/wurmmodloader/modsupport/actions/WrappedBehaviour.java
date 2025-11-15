package com.garward.wurmmodloader.modsupport.actions;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.ReflectionUtil;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.Behaviour;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.structures.Wall;

/**
 * A wrapper for {@link Behaviour} that allows multiple {@link ActionPerformer}s to be called
 * in sequence before optionally calling the original server {@link Behaviour}.
 * 
 * <p>This class is responsible for managing the execution flow of actions in the game. It provides
 * a mechanism to chain multiple action performers together and control whether the original server
 * behavior should be executed after the custom performers have run.</p>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Create a wrapped behaviour with custom action performers
 * List<ActionPerformer> performers = Arrays.asList(
 *     new CustomActionPerformer1(),
 *     new CustomActionPerformer2()
 * );
 * WrappedBehaviour wrapped = new WrappedBehaviour(originalBehaviour, true, performers);
 * 
 * // Use the wrapped behaviour in your mod
 * Behaviour.setBehaviour(creature, wrapped);
 * 
 * // Control propagation from within ActionPerformer
 * WrappedBehaviour.setServerPropagation(behaviour, false); // Prevent server propagation
 * }</pre>
 * 
 * <p><strong>Lifecycle:</strong> WrappedBehaviour instances are typically created during mod initialization
 * and remain active for the duration of the server session. They should be thread-safe as they may
 * be accessed from multiple game threads.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe. All methods can be called from any thread,
 * though typically they will be called from the game server's main thread or action processing threads.</p>
 * 
 * @since 1.0.0
 * @see ActionPerformer
 * @see ActionPerformerChain
 * @see ModActions
 */
class WrappedBehaviour extends Behaviour {
	
	/** Flag if the action should be propagated to the server */
	private boolean serverPropagation;
	/** Flag if the action should be propagated to the next action performers */
	private boolean actionPerformerPropagation;
	/** Default return value for action() methods */
	private boolean actionReturnValue;
	
	private Behaviour behaviour;
	private List<ActionPerformer> actionPerformers;
	
	private static Field fBehaviour;
	static {
		try {
			fBehaviour = ReflectionUtil.getField(Action.class, "behaviour");
		} catch (NoSuchFieldException e) {
			throw new HookException(e);
		}
	}
	
	/**
	 * Creates a new WrappedBehaviour with the specified parameters.
	 * 
	 * @param behaviour The original server behaviour to potentially wrap
	 * @param defaultActionReturnValue The default return value for action methods
	 * @param actionPerformers List of action performers to execute in sequence
	 * @since 1.0.0
	 */
	public WrappedBehaviour(Behaviour behaviour, boolean defaultActionReturnValue, List<ActionPerformer> actionPerformers) {
		this.serverPropagation = true;
		this.actionPerformerPropagation = true;
		this.actionReturnValue = defaultActionReturnValue;
		this.behaviour = behaviour;
		this.actionPerformers = actionPerformers;
	}

	/**
	 * Creates a new WrappedBehaviour with default action return value set to true.
	 * 
	 * @param behaviour The original server behaviour to potentially wrap
	 * @param actionPerformers List of action performers to execute in sequence
	 * @deprecated Use {@link #WrappedBehaviour(Behaviour, boolean, List)} instead
	 * @since 1.0.0
	 */
	@Deprecated
	public WrappedBehaviour(Behaviour behaviour, List<ActionPerformer> actionPerformers) {
		this(behaviour, true, actionPerformers);
	}
	
	/**
	 * Handles action with source item and tile coordinates.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param corner Whether the action is on a corner
	 * @param tile The tile type
	 * @param heightOffset Height offset for the action
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, tilex, tiley, onSurface, corner, tile, heightOffset, num, counter));
	}

	/**
	 * Handles action with tile coordinates.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param corner Whether the action is on a corner
	 * @param tile The tile type
	 * @param heightOffset Height offset for the action
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, tilex, tiley, onSurface, corner, tile, heightOffset, num, counter));
	}

	/**
	 * Handles action with tile coordinates (simplified version).
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param tile The tile type
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, tilex, tiley, onSurface, tile, num, counter));
	}

	/**
	 * Handles action with source item and tile coordinates.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param heightOffset Height offset for the action
	 * @param tile The tile type
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, tilex, tiley, onSurface, heightOffset, tile, num, counter));
	}

	/**
	 * Handles action with planet coordinates.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param planetId The planet identifier
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int planetId, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, planetId, num, counter));
	}

	/**
	 * Handles action with source item and planet coordinates.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param planetId The planet identifier
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int planetId, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, planetId, num, counter));
	}

	/**
	 * Handles action between source and target items.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param target The target item for the action
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, target, num, counter));
	}

	/**
	 * Handles action targeting a wound.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The target wound
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Wound target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, target, num, counter));
	}

	/**
	 * Handles action with source item targeting a wound.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param target The target wound
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Wound target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, target, num, counter));
	}

	/**
	 * Handles action targeting an item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The target item
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, target, num, counter));
	}

	/**
	 * Handles action with source item targeting a creature.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param target The target creature
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, target, num, counter));
	}

	/**
	 * Handles action targeting a creature.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The target creature
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, target, num, counter));
	}

	/**
	 * Handles action with source item targeting a wall.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param target The target wall
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Wall target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, target, num, counter));
	}

	/**
	 * Handles action targeting a wall.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The target wall
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Wall target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, target, num, counter));
	}

	/**
	 * Handles action with source item targeting a fence.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param target The target fence
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, boolean onSurface, Fence target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, onSurface, target, num, counter));
	}

	/**
	 * Handles action targeting a fence.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param target The target fence
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, boolean onSurface, Fence target, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, onSurface, target, num, counter));
	}

	/**
	 * Handles action with source item and skill.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param skill The skill involved in the action
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Skill skill, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, skill, num, counter));
	}

	/**
	 * Handles action with skill.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param skill The skill involved in the action
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Skill skill, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, skill, num, counter));
	}

	/**
	 * Handles action with source item targeting a floor.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param target The target floor
	 * @param encodedTile Encoded tile information
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, onSurface, target, encodedTile, num, counter));
	}

	/**
	 * Handles action targeting a floor.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param floor The target floor
	 * @param encodedTile Encoded tile information
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, onSurface, floor, encodedTile, num, counter));
	}

	/**
	 * Handles action with source item and tile border.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param heightOffset Height offset for the action
	 * @param dir Direction of the tile border
	 * @param borderId Border identifier
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, tilex, tiley, onSurface, heightOffset, dir, borderId, num, counter));
	}

	/**
	 * Handles action with tile border.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param dir Direction of the tile border
	 * @param borderId Border identifier
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, tilex, tiley, onSurface, dir, borderId, num, counter));
	}

	/**
	 * Handles action with multiple target items.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param targets Array of target items
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item[] targets, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, targets, num, counter));
	}

	/**
	 * Handles action targeting a bridge part.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param bridgePart The target bridge part
	 * @param encodedTile Encoded tile information
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, onSurface, bridgePart, encodedTile, num, counter));
	}

	/**
	 * Handles action with item targeting a bridge part.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param item The source item for the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param bridgePart The target bridge part
	 * @param encodedTile Encoded tile information
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item item, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, item, onSurface, bridgePart, encodedTile, num, counter));
	}

	/**
	 * Handles action with tile coordinates and direction.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param tile The tile type
	 * @param dir Direction identifier
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, tilex, tiley, onSurface, tile, dir, num, counter));
	}

	/**
	 * Handles action with source item, tile coordinates and direction.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The source item for the action
	 * @param tilex X coordinate of the tile
	 * @param tiley Y coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param heightOffset Height offset for the action
	 * @param tile The tile type
	 * @param dir Direction identifier
	 * @param num Action number identifier
	 * @param counter Action progress counter
	 * @return true if the action is completed, false if it should continue
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, int dir, short num, float counter) {
		return action(action, actionPerformer -> actionPerformer.action(action, performer, source, tilex, tiley, onSurface, heightOffset, tile, dir, num, counter));
	}

	/**
	 * Get the default action() method return value. This is true (finish action) for any custom actions
	 * and false (continue action) for any server actions.
	 * 
	 * @param action Action
	 * @return true if the action is added by a mod, false if it's a default action
	 * @since 1.0.0
	 */
	public static boolean getDefaultActionReturnValue(Action action) {
		return action.getNumber() > ModActions.getLastServerActionId();
	}

	/**
	 * Call the ActionPerformer
	 * @param action Action
	 * @param code Lambda with call to correct action method on the ActionPerformer
	 * @return true if the action is done, false if it should continue
	 * @since 1.0.0
	 */
	private boolean action(Action action, Predicate<Behaviour> code) {
		boolean actionResult = false;
		boolean propagateToServer = this.serverPropagation;

		final boolean defaultReturnValue = getDefaultActionReturnValue(action);
		final Behaviour actionBehaviour = action.getBehaviour();
		for (ActionPerformer actionPerformer : actionPerformers) {
			try {
				WrappedBehaviour wrapped = new WrappedBehaviour(actionBehaviour, defaultReturnValue, Collections.emptyList());
				setActionBehaviour(action, wrapped);

				// Set default server propagation to false. This was the default for the first version where
				// a direct return from the action() method would not call any other ActionPerformers or the
				// server behaviour
				wrapped.serverPropagation = false;

				// Call the actionPerformer
				final boolean result = code.test(new ActionPerformerBehaviour(actionPerformer));

				// Set the action() method return value. The action will be finished if any action performer wants to finish it.
				actionResult |= result;

				// Set the server propagation. The action will not propagate if any action performer wants to not propagate it.
				propagateToServer &= wrapped.serverPropagation;

				// If the action performer wants to stop propagation to other action performers then break out
				if (!wrapped.actionPerformerPropagation) {
					break;
				}
			} catch (Exception e) {
				// Log the error and remove the faulty action performer
				Logger.getLogger(WrappedBehaviour.class.getName()).log(Level.SEVERE, e.getMessage(), e);
				actionPerformers.remove(actionPerformer);
			} finally {
				setActionBehaviour(action, actionBehaviour);
			}
		}
		if (propagateToServer) {
			// Propagate the action to the server Behavior classes
			return code.test(this.behaviour) || actionResult;
		} else {
			// Don't propagate the action
			return actionResult;
		}
	}

	/**
	 * Sets the behaviour field in the action using reflection.
	 * 
	 * @param action The action to modify
	 * @param behaviour The behaviour to set
	 * @since 1.0.0
	 */
	private void setActionBehaviour(Action action, Behaviour behaviour) {
		try {
			ReflectionUtil.setPrivateField(action, fBehaviour, behaviour);
		} catch (IllegalAccessException e) {
			throw new HookException(e);
		}
	}
	
	/**
	 * Gets the original server behaviour.
	 * 
	 * @return The original behaviour that this wrapper wraps
	 * @since 1.0.0
	 */
	public Behaviour getBehaviour() {
		return behaviour;
	}
	
	/**
	 * Checks if server propagation is enabled.
	 * 
	 * @return true if the action should be propagated to the server, false otherwise
	 * @since 1.0.0
	 */
	public boolean isServerPropagation() {
		return serverPropagation;
	}
	
	/**
	 * Sets whether the action should be propagated to the server.
	 * 
	 * @param propagation true to enable server propagation, false to disable
	 * @since 1.0.0
	 */
	public void setServerPropagation(boolean propagation) {
		serverPropagation = propagation;
	}
	
	/**
	 * Unwraps a behaviour to get the original server behaviour.
	 * 
	 * @param behaviour The behaviour to unwrap
	 * @return The original server behaviour if wrapped, otherwise the behaviour itself
	 * @since 1.0.0
	 */
	public static Behaviour unwrapBehaviour(Behaviour behaviour) {
		if (behaviour instanceof WrappedBehaviour) {
			return ((WrappedBehaviour)behaviour).getBehaviour();
		}
		return behaviour;
	}
	
	/**
	 * Checks if a behaviour is a server behaviour (not wrapped with custom performers).
	 * 
	 * @param behaviour The behaviour to check
	 * @return true if the behaviour is a server behaviour, false if it's wrapped with custom performers
	 * @since 1.0.0
	 */
	public static boolean isServerBehaviour(Behaviour behaviour) {
		if (behaviour instanceof WrappedBehaviour) {
			return ((WrappedBehaviour)behaviour).actionPerformers.isEmpty();
		}
		return true;
	}

	/**
	 * Sets server propagation for a behaviour.
	 * 
	 * @param behaviour The behaviour to modify
	 * @param propagate true to enable server propagation, false to disable
	 * @since 1.0.0
	 */
	public static void setServerPropagation(Behaviour behaviour, boolean propagate) {
		if (behaviour instanceof WrappedBehaviour) {
			((WrappedBehaviour)behaviour).setServerPropagation(propagate);
		}
	}
	
	/**
	 * Set ActionPropagation flags.
	 * @param flags {@link ActionPropagation} false
	 * @return default return value for the action() method
	 * @since 1.0.0
	 */
	private boolean propagate(ActionPropagation... flags) {
		for (ActionPropagation flag : flags) {
			switch (flag) {
			case CONTINUE_ACTION:
				this.actionReturnValue = false;
				break;
			case FINISH_ACTION:
				this.actionReturnValue = true;
				break;
			case SERVER_PROPAGATION:
				this.serverPropagation = true;
				break;
			case NO_SERVER_PROPAGATION:
				this.serverPropagation = false;
				break;
			case ACTION_PERFORMER_PROPAGATION:
				this.actionPerformerPropagation = true;
				break;
			case NO_ACTION_PERFORMER_PROPAGATION:
				this.actionPerformerPropagation = false;
				break;
			}
		}
		return this.actionReturnValue;
	}
	
	/**
	 * Set ActionPropagation flags.
	 * @param behaviour Behaviour to set flags for
	 * @param flags {@link ActionPropagation} false
	 * @return default return value for the action() method
	 * @since 1.0.0
	 */
	public static boolean propagate(Behaviour behaviour, ActionPropagation... flags) {
		if (behaviour instanceof WrappedBehaviour) {
			return ((WrappedBehaviour)behaviour).propagate(flags);
		}
		return false;
	}
}