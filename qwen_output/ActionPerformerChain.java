package com.garward.wurmmodloader.modsupport.actions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.structures.Wall;

/**
 * A chain of {@link ActionPerformer} instances that allows multiple handlers for a single action ID.
 * 
 * <p>This class implements the {@link ActionPerformerBase} interface and provides a mechanism to register
 * multiple {@link ActionPerformer} instances for the same action ID. When an action is performed,
 * each registered performer is called in sequence until one handles the action (returns {@code true}).
 * 
 * <p>Usage example:
 * <pre><code>
 * // Create a chain for action ID 123
 * ActionPerformerChain chain = new ActionPerformerChain((short) 123);
 * 
 * // Add performers to the chain
 * chain.addActionPerformer(new FirstActionHandler());
 * chain.addActionPerformer(new SecondActionHandler());
 * 
 * // The chain can now be registered with the mod loader
 * // When action 123 is triggered, FirstActionHandler.action() will be called first
 * // If it returns false, SecondActionHandler.action() will be called next
 * </code></pre>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe. The internal list of action performers
 * uses a {@link CopyOnWriteArrayList}, making it safe for concurrent reads and modifications.
 * 
 * <p><strong>Lifecycle:</strong> Instances should be created during mod initialization and
 * registered with the mod loader. The chain remains active for the lifetime of the server.
 * 
 * @since 1.0.0
 * @see ActionPerformer
 * @see ActionPerformerBase
 */
class ActionPerformerChain implements ActionPerformerBase {
	
	private short actionId;
	
	private List<ActionPerformer> actionPerformers = new CopyOnWriteArrayList<>();

	/**
	 * Constructs a new ActionPerformerChain for the specified action ID.
	 * 
	 * @param actionId the action ID this chain will handle
	 * @since 1.0.0
	 */
	public ActionPerformerChain(short actionId) {
		this.actionId = actionId;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return the action ID this chain handles
	 * @since 1.0.0
	 */
	@Override
	public short getActionId() {
		return actionId;
	}
	
	/**
	 * Adds an {@link ActionPerformer} to this chain.
	 * 
	 * <p>The performer's action ID must match the chain's action ID, otherwise an
	 * {@link IllegalArgumentException} is thrown.
	 * 
	 * <p>Performers are called in the order they are added to the chain.
	 * 
	 * @param actionPerformer the performer to add to the chain
	 * @throws IllegalArgumentException if the performer's action ID doesn't match this chain's action ID
	 * @since 1.0.0
	 */
	public void addActionPerformer(ActionPerformer actionPerformer) {
		if (actionPerformer.getActionId() != getActionId()) {
			throw new IllegalArgumentException("ActionId does not match actionId of ActionPerformerChain");
		}
		
		actionPerformers.add(actionPerformer);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param corner whether the action is on a corner
	 * @param tile the tile type
	 * @param heightOffset the height offset
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return wrap(action).action(action, performer, source, tilex, tiley, onSurface, corner, tile, heightOffset, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param corner whether the action is on a corner
	 * @param tile the tile type
	 * @param heightOffset the height offset
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return wrap(action).action(action, performer, tilex, tiley, onSurface, corner, tile, heightOffset, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param tile the tile type
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short num, float counter) {
		return wrap(action).action(action, performer, tilex, tiley, onSurface, tile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param heightOffset the height offset
	 * @param tile the tile type
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short num, float counter) {
		return wrap(action).action(action, performer, source, tilex, tiley, onSurface, heightOffset, tile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param planetId the planet ID
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int planetId, short num, float counter) {
		return wrap(action).action(action, performer, planetId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param planetId the planet ID
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int planetId, short num, float counter) {
		return wrap(action).action(action, performer, source, planetId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param target the target item
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
		return wrap(action).action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param target the target wound
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Wound target, short num, float counter) {
		return wrap(action).action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param target the target wound
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Wound target, short num, float counter) {
		return wrap(action).action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param target the target item
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item target, short num, float counter) {
		return wrap(action).action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param target the target creature
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
		return wrap(action).action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param target the target creature
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
		return wrap(action).action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param target the target wall
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Wall target, short num, float counter) {
		return wrap(action).action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param target the target wall
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Wall target, short num, float counter) {
		return wrap(action).action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param onSurface whether the action is on the surface
	 * @param target the target fence
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, boolean onSurface, Fence target, short num, float counter) {
		return wrap(action).action(action, performer, source, onSurface, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param onSurface whether the action is on the surface
	 * @param target the target fence
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, boolean onSurface, Fence target, short num, float counter) {
		return wrap(action).action(action, performer, onSurface, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param skill the skill associated with the action
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, Skill skill, short num, float counter) {
		return wrap(action).action(action, performer, source, skill, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param skill the skill associated with the action
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Skill skill, short num, float counter) {
		return wrap(action).action(action, performer, skill, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param onSurface whether the action is on the surface
	 * @param target the target floor
	 * @param encodedTile the encoded tile information
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
		return wrap(action).action(action, performer, source, onSurface, target, encodedTile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param onSurface whether the action is on the surface
	 * @param floor the target floor
	 * @param encodedTile the encoded tile information
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
		return wrap(action).action(action, performer, onSurface, floor, encodedTile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param heightOffset the height offset
	 * @param dir the tile border direction
	 * @param borderId the border ID
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return wrap(action).action(action, performer, source, tilex, tiley, onSurface, heightOffset, dir, borderId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param dir the tile border direction
	 * @param borderId the border ID
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return wrap(action).action(action, performer, tilex, tiley, onSurface, dir, borderId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param targets the target items
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item[] targets, short num, float counter) {
		return wrap(action).action(action, performer, targets, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param onSurface whether the action is on the surface
	 * @param bridgePart the target bridge part
	 * @param encodedTile the encoded tile information
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return wrap(action).action(action, performer, onSurface, bridgePart, encodedTile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param item the source item
	 * @param onSurface whether the action is on the surface
	 * @param bridgePart the target bridge part
	 * @param encodedTile the encoded tile information
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	public boolean action(Action action, Creature performer, Item item, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return wrap(action).action(action, performer, item, onSurface, bridgePart, encodedTile, num, counter);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param corner whether the action is on a corner
	 * @param tile the tile type
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter) {
		return wrap(action).action(action, performer, source, tilex, tiley, onSurface, corner, tile, 0, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param corner whether the action is on a corner
	 * @param tile the tile type
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter) {
		return wrap(action).action(action, performer, tilex, tiley, onSurface, corner, tile, 0, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param tile the tile type
	 * @param dir the direction
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir, short num, float counter) {
		return wrap(action).action(action, performer, tilex, tiley, onSurface, tile, dir, num, counter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param action the action being performed
	 * @param performer the creature performing the action
	 * @param source the source item for the action
	 * @param tilex the x coordinate of the tile
	 * @param tiley the y coordinate of the tile
	 * @param onSurface whether the action is on the surface
	 * @param heightOffset the height offset
	 * @param tile the tile type
	 * @param dir the direction
	 * @param num the action number
	 * @param counter the action counter
	 * @return {@code true} if the action was handled, {@code false} otherwise
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, int dir, short num, float counter) {
		return wrap(action).action(action, performer, source, tilex, tiley, onSurface, heightOffset, tile, dir, num, counter);
	}

	/**
	 * Wraps the behaviour of an action with the chain of performers.
	 * 
	 * @param action the action to wrap
	 * @return a WrappedBehaviour that executes the chain of performers
	 * @since 1.0.0
	 */
	private WrappedBehaviour wrap(Action action) {
		return new WrappedBehaviour(action.getBehaviour(), WrappedBehaviour.getDefaultActionReturnValue(action), actionPerformers);
	}

}