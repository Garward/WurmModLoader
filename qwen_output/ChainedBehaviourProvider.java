package com.garward.wurmmodloader.modsupport.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.mesh.Tiles.TileBorderDirection;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;
import com.wurmonline.server.structures.BridgePart;
import com.wurmonline.server.structures.Fence;
import com.wurmonline.server.structures.Floor;
import com.wurmonline.server.structures.Wall;

/**
 * A {@link BehaviourProvider} implementation that chains multiple behaviour providers together.
 * 
 * <p>This class allows multiple {@link BehaviourProvider} instances to be combined in a chain,
 * where each provider in the chain is queried for behaviours. The results from all providers
 * are merged together to form a complete list of available actions.</p>
 * 
 * <p>The chaining mechanism ensures that if one provider throws an exception during behaviour
 * retrieval, it will be removed from the chain and subsequent calls will not include it,
 * providing fault tolerance for the behaviour system.</p>
 * 
 * <p>Usage example:
 * <pre>{@code
 * List<BehaviourProvider> providers = Arrays.asList(
 *     new CustomBehaviourProvider1(),
 *     new CustomBehaviourProvider2()
 * );
 * BehaviourProvider chained = new ChainedBehaviourProvider(wrappedProvider, providers);
 * }</pre>
 * </p>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. All operations should
 * be performed on the same thread or properly synchronized by the caller.</p>
 * 
 * <p><strong>Lifecycle:</strong> Instances should be created during mod initialization
 * and remain active for the duration of the server session.</p>
 * 
 * @since 1.0.0
 * @see BehaviourProvider
 */
public class ChainedBehaviourProvider implements BehaviourProvider {
	private Iterable<BehaviourProvider> behaviourProviders;
	private List<BehaviourProvider> prov;

	/**
	 * Constructs a new ChainedBehaviourProvider with the specified wrapped provider and list of providers.
	 * 
	 * @param wrapped the initial behaviour provider to wrap
	 * @param behaviourProviders the list of behaviour providers to chain
	 * @since 1.0.0
	 */
	public ChainedBehaviourProvider(BehaviourProvider wrapped, List<BehaviourProvider> behaviourProviders) {
		this.prov = behaviourProviders;
		this.behaviourProviders = new ChainedBehaviourProviders(wrapped, behaviourProviders);
	}

	/**
	 * Merges two lists of ActionEntry objects.
	 * 
	 * <p>If the first list is null, returns the second list. If the second list is null,
	 * returns the first list. Otherwise, adds all entries from the second list to the first.</p>
	 * 
	 * @param list the first list of ActionEntry objects, may be null
	 * @param entries the second list of ActionEntry objects, may be null
	 * @return the merged list, or one of the input lists if the other is null
	 * @since 1.0.0
	 */
	private List<ActionEntry> merge(List<ActionEntry> list, List<ActionEntry> entries) {
		if (list == null) {
			return entries;
		} else if (entries == null) {
			return list;
		} else {
			list.addAll(entries);
			return list;
		}
	}
	
	/**
	 * Calls the provided function on each behaviour provider in the chain.
	 * 
	 * <p>This method iterates through all behaviour providers in the chain and applies
	 * the provided function to each one. Results are merged together. If any provider
	 * throws an exception, it is logged and that provider is removed from the chain.</p>
	 * 
	 * @param code the function to apply to each behaviour provider
	 * @return a merged list of ActionEntry objects from all providers
	 * @since 1.0.0
	 */
	private List<ActionEntry> call(Function<BehaviourProvider, List<ActionEntry>> code) {
		List<ActionEntry> list = null;
		for (BehaviourProvider behaviourProvider : behaviourProviders) {
			try {
				list = merge(list, code.apply(behaviourProvider));
			} catch (Exception e) {
				Logger.getLogger(ChainedBehaviourProvider.class.getName()).log(Level.SEVERE, e.getMessage(), e);
				prov.remove(behaviourProvider);
			}
		}
		return list;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, boolean, BridgePart)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature aPerformer, boolean aOnSurface, BridgePart aBridgePart) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(aPerformer, aOnSurface, aBridgePart));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, boolean, BridgePart)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature aPerformer, Item item, boolean aOnSurface, BridgePart aBridgePart) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(aPerformer, item, aOnSurface, aBridgePart));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, boolean, Floor)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature creature, Item item, boolean onSurface, Floor floor) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(creature, item, onSurface, floor));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, boolean, Floor)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, boolean onSurface, Floor floor) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, onSurface, floor));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Creature)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Creature target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Fence)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Fence target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int planetId) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, planetId));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, int, int, boolean, boolean, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@SuppressWarnings("deprecation")
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, tilex, tiley, onSurface, corner, tile));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, int, int, boolean, boolean, int, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, tilex, tiley, onSurface, corner, tile, heightOffset));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, int, int, boolean, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, tilex, tiley, onSurface, tile));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, int, int, boolean, int, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, int tile, int dir) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, tilex, tiley, onSurface, tile, dir));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, int, int, boolean, TileBorderDirection, boolean, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, int tilex, int tiley, boolean onSurface, TileBorderDirection dir, boolean border, int heightOffset) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, tilex, tiley, onSurface, dir, border, heightOffset));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int planetId) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, object, planetId));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, int, int, boolean, boolean, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@SuppressWarnings("deprecation")
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, boolean corner, int tile) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, object, tilex, tiley, onSurface, corner, tile));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, int, int, boolean, boolean, int, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, boolean corner, int tile, int heightOffset) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, object, tilex, tiley, onSurface, corner, tile, heightOffset));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, int, int, boolean, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, int tile) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, object, tilex, tiley, onSurface, tile));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, int, int, boolean, int, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, int tile, int dir) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, object, tilex, tiley, onSurface, tile, dir));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, int, int, boolean, TileBorderDirection, boolean, int)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item object, int tilex, int tiley, boolean onSurface, TileBorderDirection dir, boolean border, int heightOffset) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, object, tilex, tiley, onSurface, dir, border, heightOffset));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, Creature)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Creature target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, subject, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, Fence)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Fence target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, subject, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, Item)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, subject, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, Skill)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Skill skill) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, subject, skill));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, Wall)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Wall target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, subject, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item, Wound)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Wound target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, subject, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Item)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Item target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, long)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, long target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Skill)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Skill skill) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, skill));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Wall)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Wall target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, target));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Calls {@link BehaviourProvider#getBehavioursFor(Creature, Wound)}
	 * on each provider in the chain and merges the results.</p>
	 * 
	 * @since 1.0.0
	 */
	@Override
	public List<ActionEntry> getBehavioursFor(Creature performer, Wound target) {
		return call(behaviourProvider -> behaviourProvider.getBehavioursFor(performer, target));
	}

	/**
	 * An iterable implementation that chains a wrapped behaviour provider with a collection of providers.
	 * 
	 * <p>This class provides an iterator that first returns the wrapped provider, then
	 * iterates through the provided collection of behaviour providers.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This class is not thread-safe.</p>
	 * 
	 * @since 1.0.0
	 */
	private static class ChainedBehaviourProviders implements Iterable<BehaviourProvider> {

		private final BehaviourProvider wrapped;
		private final Iterable<BehaviourProvider> iterable;

		/**
		 * Constructs a new ChainedBehaviourProviders with the specified wrapped provider and collection.
		 * 
		 * @param wrapped the initial behaviour provider to wrap
		 * @param behaviourProviders the collection of behaviour providers to chain
		 * @since 1.0.0
		 */
		public ChainedBehaviourProviders(BehaviourProvider wrapped, Collection<BehaviourProvider> behaviourProviders) {
			this.wrapped = wrapped;
			this.iterable = behaviourProviders;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * <p>Returns an iterator that first yields the wrapped provider, then the providers
		 * from the provided iterable.</p>
		 * 
		 * @since 1.0.0
		 */
		@Override
		public Iterator<BehaviourProvider> iterator() {

			return new Iterator<BehaviourProvider>() {
				
				BehaviourProvider first = wrapped;
				
				Iterator<BehaviourProvider> iterator = iterable == null ? Collections.emptyIterator() : iterable.iterator();

				/**
				 * {@inheritDoc}
				 * 
				 * @since 1.0.0
				 */
				@Override
				public boolean hasNext() {
					if (first != null) {
						return true;
					} else {
						return iterator.hasNext();
					}
				}

				/**
				 * {@inheritDoc}
				 * 
				 * @since 1.0.0
				 */
				@Override
				public BehaviourProvider next() {
					if (first != null) {
						try {
							return first;
						} finally {
							first = null;
						}
					} else {
						return iterator.next();
					}
				}
			};
		}
	}
}