package com.garward.wurmmodloader.modsupport.actions;

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
 * A {@link Behaviour} implementation that delegates action performance to an {@link ActionPerformerBase}.
 *
 * <p>This class serves as a bridge between the Wurm server's {@link Behaviour} system and custom
 * action performers. It implements all the action methods from {@link Behaviour} and forwards them
 * to an encapsulated {@link ActionPerformerBase} instance. This allows mod authors to implement
 * custom action logic without directly extending {@link Behaviour}.</p>
 *
 * <p>Usage example:
 * <pre><code>
 * // Create a custom action performer
 * public class MyActionPerformer implements ActionPerformerBase {
 *     {@literal @}Override
 *     public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
 *         // Custom action logic here
 *         performer.getCommunicator().sendNormalServerMessage("Hello from custom action!");
 *         return true;
 *     }
 *
 *     {@literal @}Override
 *     public short getActionId() {
 *         return 1234; // Custom action ID
 *     }
 * }
 *
 * // Create the behaviour wrapper
 * ActionPerformerBehaviour behaviour = new ActionPerformerBehaviour(new MyActionPerformer());
 * </code></pre>
 * </p>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe as long as the provided
 * {@link ActionPerformerBase} implementation is thread-safe. All methods simply delegate to the
 * encapsulated performer without maintaining mutable state.</p>
 *
 * <p><strong>Lifecycle:</strong> Instances are typically created during mod initialization
 * and remain active for the duration of the server session.</p>
 *
 * @since 1.0.0
 * @see ActionPerformerBase
 * @see Behaviour
 */
public class ActionPerformerBehaviour extends Behaviour implements ActionPerformerBase {

	private ActionPerformerBase actionPerformer;

	/**
	 * Constructs a new ActionPerformerBehaviour with the specified action performer.
	 *
	 * @param actionPerformer the action performer to delegate to; must not be null
	 * @throws IllegalArgumentException if actionPerformer is null
	 * @since 1.0.0
	 */
	public ActionPerformerBehaviour(ActionPerformerBase actionPerformer) {
		if (actionPerformer == null) {
			throw new IllegalArgumentException("ActionPerformerBase cannot be null");
		}
		this.actionPerformer = actionPerformer;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	@SuppressWarnings("deprecation")
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter) {
		return actionPerformer.action(action, performer, source, tilex, tiley, onSurface, corner, tile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return actionPerformer.action(action, performer, source, tilex, tiley, onSurface, corner, tile, heightOffset, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	@SuppressWarnings("deprecation")
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, short num, float counter) {
		return actionPerformer.action(action, performer, tilex, tiley, onSurface, corner, tile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset, short num, float counter) {
		return actionPerformer.action(action, performer, tilex, tiley, onSurface, corner, tile, heightOffset, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, short num, float counter) {
		return actionPerformer.action(action, performer, tilex, tiley, onSurface, tile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, short num, float counter) {
		return actionPerformer.action(action, performer, source, tilex, tiley, onSurface, heightOffset, tile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, int planetId, short num, float counter) {
		return actionPerformer.action(action, performer, planetId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, int planetId, short num, float counter) {
		return actionPerformer.action(action, performer, source, planetId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, Item target, short num, float counter) {
		return actionPerformer.action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Wound target, short num, float counter) {
		return actionPerformer.action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, Wound target, short num, float counter) {
		return actionPerformer.action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item target, short num, float counter) {
		return actionPerformer.action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
		return actionPerformer.action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Creature target, short num, float counter) {
		return actionPerformer.action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, Wall target, short num, float counter) {
		return actionPerformer.action(action, performer, source, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Wall target, short num, float counter) {
		return actionPerformer.action(action, performer, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, boolean onSurface, Fence target, short num, float counter) {
		return actionPerformer.action(action, performer, source, onSurface, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, boolean onSurface, Fence target, short num, float counter) {
		return actionPerformer.action(action, performer, onSurface, target, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, Skill skill, short num, float counter) {
		return actionPerformer.action(action, performer, source, skill, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Skill skill, short num, float counter) {
		return actionPerformer.action(action, performer, skill, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, boolean onSurface, Floor target, int encodedTile, short num, float counter) {
		return actionPerformer.action(action, performer, source, onSurface, target, encodedTile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	@SuppressWarnings("deprecation")
	public boolean action(Action action, Creature performer, boolean onSurface, Floor floor, int encodedTile, short num, float counter) {
		return actionPerformer.action(action, performer, onSurface, floor, encodedTile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return actionPerformer.action(action, performer, source, tilex, tiley, onSurface, heightOffset, dir, borderId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, Tiles.TileBorderDirection dir, long borderId, short num, float counter) {
		return actionPerformer.action(action, performer, tilex, tiley, onSurface, dir, borderId, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item[] targets, short num, float counter) {
		return actionPerformer.action(action, performer, targets, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return actionPerformer.action(action, performer, onSurface, bridgePart, encodedTile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item item, boolean onSurface, BridgePart bridgePart, int encodedTile, short num, float counter) {
		return actionPerformer.action(action, performer, item, onSurface, bridgePart, encodedTile, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir, short num, final float counter) {
		return actionPerformer.action(action, performer, tilex, tiley, onSurface, tile, dir, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public boolean action(Action action, Creature performer, Item source, int tilex, int tiley, boolean onSurface, int heightOffset, int tile, int dir, short num, float counter) {
		return actionPerformer.action(action, performer, source, tilex, tiley, onSurface, heightOffset, tile, dir, num, counter);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Delegates to the encapsulated {@link ActionPerformerBase}.</p>
	 *
	 * @since 1.0.0
	 */
	@Override
	public short getActionId() {
		return actionPerformer.getActionId();
	}
}