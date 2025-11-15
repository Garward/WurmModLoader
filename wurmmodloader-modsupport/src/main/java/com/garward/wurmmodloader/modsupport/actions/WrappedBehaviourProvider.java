package com.garward.wurmmodloader.modsupport.actions;

import java.util.List;

import com.wurmonline.mesh.Tiles.TileBorderDirection;
import com.wurmonline.server.behaviours.ActionEntry;
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
 * A wrapper implementation of {@link BehaviourProvider} that delegates all method calls to a wrapped {@link Behaviour} instance.
 *
 * <p>This class provides a convenient way to wrap an existing {@link Behaviour} object while maintaining the 
 * {@link BehaviourProvider} interface contract. It allows mod authors to intercept or modify behaviour without 
 * having to implement all methods themselves.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre><code>
 * // Create a custom behaviour
 * Behaviour customBehaviour = new MyCustomBehaviour();
 * 
 * // Wrap it with this provider
 * BehaviourProvider provider = new WrappedBehaviourProvider(customBehaviour);
 * 
 * // Use the provider in your mod
 * ModActions.registerActionProvider(provider);
 * </code></pre>
 *
 * <p><b>Thread Safety:</b> This class is thread-safe as long as the wrapped {@link Behaviour} 
 * implementation is also thread-safe. All methods simply delegate to the wrapped instance without 
 * adding any additional state.</p>
 *
 * <p><b>Lifecycle:</b> Instances should be created during mod initialization and registered 
 * with the action system. The wrapped behaviour should remain valid for the lifetime of this provider.</p>
 *
 * @since 1.0.0
 * @see BehaviourProvider
 * @see Behaviour
 */
public class WrappedBehaviourProvider implements BehaviourProvider {
	/**
	 * The wrapped behaviour instance that all method calls are delegated to.
	 * 
	 * @since 1.0.0
	 */
	private Behaviour wrapped;

	/**
	 * Constructs a new {@code WrappedBehaviourProvider} that wraps the specified {@link Behaviour}.
	 *
	 * <p>The provided behaviour will receive all method calls made to this provider's methods.</p>
	 *
	 * @param wrapped the behaviour to wrap; must not be null
	 * @throws IllegalArgumentException if {@code wrapped} is null
	 * @since 1.0.0
	 */
	public WrappedBehaviourProvider(Behaviour wrapped) {
		if (wrapped == null) {
			throw new IllegalArgumentException("Wrapped behaviour cannot be null");
		}
		this.wrapped = wrapped;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, boolean, BridgePart)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature aPerformer, boolean aOnSurface, BridgePart aBridgePart) {
		return wrapped.getBehavioursFor(aPerformer, aOnSurface, aBridgePart);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, boolean, BridgePart)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature aPerformer, Item item, boolean aOnSurface, BridgePart aBridgePart) {
		return wrapped.getBehavioursFor(aPerformer, item, aOnSurface, aBridgePart);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, boolean, Floor)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature creature, Item item, boolean onSurface, Floor floor) {
		return wrapped.getBehavioursFor(creature, item, onSurface, floor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, boolean, Floor)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, boolean onSurface, Floor floor) {
		return wrapped.getBehavioursFor(performer, onSurface, floor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Creature)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
		return wrapped.getBehavioursFor(performer, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Fence)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Fence target) {
		return wrapped.getBehavioursFor(performer, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int planetId) {
		return wrapped.getBehavioursFor(performer, planetId);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, int, int, boolean, boolean, int, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset) {
		return wrapped.getBehavioursFor(performer, tilex, tiley, onSurface, corner, tile, heightOffset);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, int, int, boolean, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile) {
		return wrapped.getBehavioursFor(performer, tilex, tiley, onSurface, tile);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, int, int, boolean, int, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir) {
		return wrapped.getBehavioursFor(performer, tilex, tiley, onSurface, tile, dir);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, int, int, boolean, TileBorderDirection, boolean, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, TileBorderDirection dir, boolean border, int heightOffset) {
		return wrapped.getBehavioursFor(performer, tilex, tiley, onSurface, dir, border, heightOffset);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int planetId) {
		return wrapped.getBehavioursFor(performer, object, planetId);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, int, int, boolean, boolean, int, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset) {
		return wrapped.getBehavioursFor(performer, object, tilex, tiley, onSurface, corner, tile, heightOffset);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, int, int, boolean, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, int tile) {
		return wrapped.getBehavioursFor(performer, object, tilex, tiley, onSurface, tile);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, int, int, boolean, int, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, int tile, int dir) {
		return wrapped.getBehavioursFor(performer, object, tilex, tiley, onSurface, tile, dir);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, int, int, boolean, TileBorderDirection, boolean, int)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, TileBorderDirection dir, boolean border, int heightOffset) {
		return wrapped.getBehavioursFor(performer, object, tilex, tiley, onSurface, dir, border, heightOffset);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, Creature)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
		return wrapped.getBehavioursFor(performer, subject, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, Fence)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Fence target) {
		return wrapped.getBehavioursFor(performer, subject, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, Item)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
		return wrapped.getBehavioursFor(performer, subject, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, Skill)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Skill skill) {
		return wrapped.getBehavioursFor(performer, subject, skill);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, Wall)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Wall target) {
		return wrapped.getBehavioursFor(performer, subject, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item, Wound)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Wound target) {
		return wrapped.getBehavioursFor(performer, subject, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Item)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
		return wrapped.getBehavioursFor(performer, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, long)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, long target) {
		return wrapped.getBehavioursFor(performer, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Skill)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Skill skill) {
		return wrapped.getBehavioursFor(performer, skill);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Wall)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Wall target) {
		return wrapped.getBehavioursFor(performer, target);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Delegates to the wrapped {@link Behaviour#getBehavioursFor(Creature, Wound)} method.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Wound target) {
		return wrapped.getBehavioursFor(performer, target);
	}

}