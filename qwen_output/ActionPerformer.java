package com.garward.wurmmodloader.modsupport.actions;

import static com.garward.wurmmodloader.modsupport.actions.ActionPropagation.ACTION_PERFORMER_PROPAGATION;
import static com.garward.wurmmodloader.modsupport.actions.ActionPropagation.NO_SERVER_PROPAGATION;
import static com.garward.wurmmodloader.modsupport.actions.ActionPropagation.SERVER_PROPAGATION;

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
 * Interface to abstract performing an action from the Behaviour classes.
 * 
 * <p>The interface defines all action() methods of Behaviour. The ModActions hook will call a 
 * registered ActionPerformer instead of the server defined Behaviour classes.</p>
 * 
 * <p>The ActionPerformer should call the super implementations 
 * (ActionPerformer.super.action(...)) to allow chaining multiple ActionPerformers and to 
 * forward the action to the original implementation.</p>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * public class MyActionPerformer implements ActionPerformer {
 *     @Override
 *     public short getActionId() {
 *         return Actions.MY_CUSTOM_ACTION;
 *     }
 *     
 *     @Override
 *     public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
 *         // Custom action logic here
 *         performer.getCommunicator().sendNormalServerMessage("Performing custom action!");
 *         
 *         // Call super to continue the chain or propagate to server behaviour
 *         return ActionPerformer.super.action(action, performer, source, target, num, counter);
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>Lifecycle:</strong> ActionPerformer instances are typically registered during mod initialization
 * and remain active for the duration of the server session. They are called in place of the original
 * Behaviour methods when an action matching their action ID is performed.</p>
 * 
 * <p><strong>Thread Safety:</strong> Implementations should be thread-safe as action methods may be called
 * from multiple threads. Avoid modifying shared state without proper synchronization.</p>
 * 
 * @since 1.0.0
 * @see ActionPerformerBase
 * @see WrappedBehaviour
 */
public interface ActionPerformer extends ActionPerformerBase {

	//
	// Tile corners
	//

	/**
	 * Performs an action on a tile corner with a source item.
	 * 
	 * <p><strong>Deprecated:</strong> Use {@link #action(Action, Creature, Item, int, int, boolean, boolean, int, int, short, float)} instead.</p>
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param corner Whether the action is on a corner
	 * @param tile The tile type
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Deprecated
	@Override
	public default boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a tile corner with a source item and height offset.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param corner Whether the action is on a corner
	 * @param tile The tile type
	 * @param heightOffset The height offset of the tile
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return action(action, performer, source, tilex, tiley, onSurface, corner, tile, num, counter);
	}

	/**
	 * Performs an action on a tile corner without a source item.
	 * 
	 * <p><strong>Deprecated:</strong> Use {@link #action(Action, Creature, int, int, boolean, boolean, int, int, short, float)} instead.</p>
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param corner Whether the action is on a corner
	 * @param tile The tile type
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Deprecated
	@Override
	public default boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a tile corner without a source item and with height offset.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param corner Whether the action is on a corner
	 * @param tile The tile type
	 * @param heightOffset The height offset of the tile
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return action(action, performer, tilex, tiley, onSurface, corner, tile, num, counter);
	}

	//
	// Tiles
	//

	/**
	 * Performs an action on a tile without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param tile The tile type
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a tile with a source item and height offset.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param heightOffset The height offset of the tile
	 * @param tile The tile type
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Planets, Tickets, Missions
	//

	/**
	 * Performs an action on a planet, ticket, or mission without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param id The ID of the planet, ticket, or mission
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, int id, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a planet, ticket, or mission with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param id The ID of the planet, ticket, or mission
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, int id, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Wounds
	//

	/**
	 * Performs an action on a wound without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The wound being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Wound target, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a wound with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param target The wound being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, Wound target, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Items
	//

	/**
	 * Performs an action on an item with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param target The item being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on an item without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The item being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item target, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on multiple items without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param targets The items being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item[] targets, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Creatures
	//

	/**
	 * Performs an action on a creature with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param target The creature being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a creature without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The creature being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Creature target, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Walls
	//

	/**
	 * Performs an action on a wall with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param target The wall being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, Wall target, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a wall without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param target The wall being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Wall target, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Fences
	//

	/**
	 * Performs an action on a fence with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param target The fence being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, boolean onSurface, Fence target, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a fence without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param target The fence being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, boolean onSurface, Fence target, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Skills
	//

	/**
	 * Performs an action on a skill with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param skill The skill being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, Skill skill, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a skill without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param skill The skill being acted upon
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Skill skill, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Floor
	//

	/**
	 * Performs an action on a floor with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param target The floor being acted upon
	 * @param encodedTile The encoded tile information
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a floor without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param floor The floor being acted upon
	 * @param encodedTile The encoded tile information
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Tiles border
	//

	/**
	 * Performs an action on a tile border with a source item and height offset.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param heightOffset The height offset of the tile
	 * @param dir The direction of the tile border
	 * @param borderId The ID of the border
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a tile border without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param dir The direction of the tile border
	 * @param borderId The ID of the border
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Bridges
	//

	/**
	 * Performs an action on a bridge without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param bridgePart The bridge part being acted upon
	 * @param encodedTile The encoded tile information
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return defaultPropagation(action);
	}

	/**
	 * Performs an action on a bridge with a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param item The item being used to perform the action
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param bridgePart The bridge part being acted upon
	 * @param encodedTile The encoded tile information
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item item, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return defaultPropagation(action);
	}

	//
	// Cave tiles
	//

	/**
	 * Performs an action on a cave tile without a source item.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param tile The tile type
	 * @param dir The direction
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir, short num, final float counter) {
		return action(action, performer, tilex, tiley, onSurface, tile, num, counter);
	}

	/**
	 * Performs an action on a cave tile with a source item and height offset.
	 * 
	 * @param action The action being performed
	 * @param performer The creature performing the action
	 * @param source The item being used to perform the action
	 * @param tilex The x-coordinate of the tile
	 * @param tiley The y-coordinate of the tile
	 * @param onSurface Whether the action is on the surface or in a cave
	 * @param heightOffset The height offset of the tile
	 * @param tile The tile type
	 * @param dir The direction
	 * @param num The action number
	 * @param counter The action counter
	 * @return true if the action should stop, false if it should continue
	 * @since 1.0.0
	 */
	@Override
	public default boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, int dir, short num, float counter) {
		return action(action, performer, source, tilex, tiley, onSurface, heightOffset, tile, num, counter);
	}

	/**
	 * Get the action id which is handled by this ActionPerformer.
	 * 
	 * <p>This method should return the specific action ID that this ActionPerformer handles.
	 * The ModActions system uses this to route the appropriate actions to this performer.</p>
	 * 
	 * @return The action ID handled by this ActionPerformer
	 * @since 1.0.0
	 */
	public short getActionId();

	/**
	 * Check if the action would directly call a servers Behaviour object. 
	 * 
	 * @param action Action
	 * @return true if the behaviour object not wrapped or directly wraps a servers Behaviour object
	 * @since 1.0.0
	 */
	public static boolean isServerBehaviour(Action action) {
		return WrappedBehaviour.isServerBehaviour(action.getBehaviour());
	}

	/**
	 * Get the servers Behaviour object which would be called at the end
	 * of the ActionPerformer chain.
	 * 
	 * @param action Action
	 * @return Behaviour object
	 * @since 1.0.0
	 */
	public static Behaviour getServerBehaviour(Action action) {
		return WrappedBehaviour.unwrapBehaviour(action.getBehaviour());
	}

	/**
	 * Prevent (or enable) the ActionPerformer chain from calling the servers Behaviour object after the chain has been processed.
	 * 
	 * <p><strong>Deprecated:</strong> Use {@link #propagate(Action, ActionPropagation...)} instead.</p>
	 * 
	 * @param action Action
	 * @param propagate true if the chain should call the servers Behaviour object, false if it should stop after the last ActionPerformer
	 * @since 1.0.0
	 */
	@Deprecated
	public static void setServerPropagation(Action action, boolean propagate) {
		if (!propagate) {
			WrappedBehaviour.propagate(action.getBehaviour(), NO_SERVER_PROPAGATION);
		}
	}

	/**
	 * Activate the default propagation for the action.
	 *
	 * @param action Action
	 * @return true if the action should stop. False if it should continue
	 * @since 1.0.0
	 */
	public default boolean defaultPropagation(Action action) {
		return propagate(action, SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);
	}

	/**
	 * Change the action propagation and return the return value for the action() method.
	 * 
	 * <p>This method allows fine-grained control over how actions propagate through the chain
	 * of ActionPerformers and whether they ultimately reach the server's original Behaviour.</p>
	 * 
	 * <p><strong>Usage Example:</strong></p>
	 * <pre>{@code
	 * // Stop propagation after this ActionPerformer
	 * return propagate(action, NO_SERVER_PROPAGATION, ACTION_PERFORMER_PROPAGATION);
	 * 
	 * // Continue to server behaviour but skip other ActionPerformers
	 * return propagate(action, SERVER_PROPAGATION);
	 * }</pre>
	 * 
	 * @param action The action being performed
	 * @param flags {@link ActionPropagation} flags controlling propagation behavior
	 * @return true is the action should stop. False if the action should continue.
	 * @since 1.0.0
	 * @see ActionPropagation
	 */
	public default boolean propagate(Action action, ActionPropagation... flags) {
		return WrappedBehaviour.propagate(action.getBehaviour(), flags);
	}
}