package com.garward.wurmmodloader.modsupport.creatures;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.garward.wurmmodloader.modloader.internal.ReflectionUtil;
import com.garward.wurmmodloader.modloader.internal.classhooks.HookException;

import com.wurmonline.server.Server;
import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Traits;

/**
 * Utility class for managing creature traits in Wurm Unlimited.
 * 
 * <p>This class provides functionality for retrieving and calculating creature traits,
 * including breeding mechanics, trait inheritance, and trait selection algorithms.
 * It supports both regular traits and color traits, with special handling for
 * inbreeding penalties and PvP server restrictions.</p>
 * 
 * <p>Usage example for getting creature traits:
 * <pre><code>
 * Creature creature = // ... obtain creature reference
 * long traits = ModTraits.getTraits(creature);
 * if ((traits & (1L << ModTraits.TRAIT_FIERCLY)) != 0) {
 *     // Creature has the Fiercly trait
 * }
 * </code></pre></p>
 * 
 * <p>Usage example for calculating new traits for breeding:
 * <pre><code>
 * double breederSkill = 75.0;
 * boolean isInbred = false;
 * long motherTraits = mother.getTraits();
 * long fatherTraits = father.getTraits();
 * long newTraits = ModTraits.calcNewTraits(breederSkill, isInbred, motherTraits, fatherTraits, 
 *                                          ModTraits.REGULAR_TRAITS, ModTraits.COLOR_TRAITS);
 * </code></pre></p>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe as all methods are stateless
 * and operate on provided parameters. The static fields are immutable after class
 * initialization.</p>
 * 
 * @since 1.0.0
 * @see Traits
 * @see Creature
 */
public class ModTraits {
	
	/**
	 * Trait indicating a creature is fiercely aggressive.
	 * @since 1.0.0
	 */
	public static final int TRAIT_FIERCLY = 0;
	
	/**
	 * Trait indicating a creature is exceptionally fast.
	 * @since 1.0.0
	 */
	public static final int TRAIT_FLEETER = 1;
	
	/**
	 * Trait indicating a creature has high durability.
	 * @since 1.0.0
	 */
	public static final int TRAIT_TOUGH_BUGGER = 2;
	
	/**
	 * Trait indicating a creature has exceptional body strength.
	 * @since 1.0.0
	 */
	public static final int TRAIT_STRONG_BODY = 3;
	
	/**
	 * Trait indicating a creature has lightning-fast reflexes.
	 * @since 1.0.0
	 */
	public static final int TRAIT_LIGHTNING = 4;
	
	/**
	 * Trait indicating a creature can carry more weight.
	 * @since 1.0.0
	 */
	public static final int TRAIT_CARRY_MORE = 5;
	
	/**
	 * Trait indicating a creature has exceptionally strong legs.
	 * @since 1.0.0
	 */
	public static final int TRAIT_STRONG_LEGS = 6;
	
	/**
	 * Trait indicating a creature has heightened senses.
	 * @since 1.0.0
	 */
	public static final int TRAIT_KEEN_SENSES = 7;
	
	/**
	 * Trait indicating a creature has malformed legs (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_MALFORMED_LEGS = 8;
	
	/**
	 * Trait indicating a creature has legs of different lengths (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_DIFFERENT_LENGTH = 9;
	
	/**
	 * @deprecated Use {@link #TRAIT_DIFFERENT_LENGTH} instead
	 * @since 1.0.0
	 */
	@Deprecated
	public static final int TRAIT_DIFFERNENT_LENGTH = TRAIT_DIFFERENT_LENGTH;
	
	/**
	 * Trait indicating a creature is overly aggressive (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_OVERLY_AGGRESSIVE = 10;
	
	/**
	 * Trait indicating a creature is unmotivated (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_UNMOTIVATED = 11;
	
	/**
	 * Trait indicating a creature has strong willpower.
	 * @since 1.0.0
	 */
	public static final int TRAIT_STRONG_WILLED = 12;
	
	/**
	 * Trait indicating a creature is prone to illness (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_ILLNESS = 13;
	
	/**
	 * Trait indicating a creature is constantly hungry (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_CONSTANTLY_HUNGRY = 14;
	
	/**
	 * Trait indicating a creature is feeble and unhealthy (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_FEEBLE_AND_UNHEALTHY = 19;
	
	/**
	 * Trait indicating a creature is strong and healthy.
	 * @since 1.0.0
	 */
	public static final int TRAIT_STRONG_AND_HEALTHY = 20;
	
	/**
	 * Trait indicating a creature has spark/energy.
	 * @since 1.0.0
	 */
	public static final int TRAIT_SPARK = 21;
	
	/**
	 * Trait indicating a creature is corrupted (PvP only).
	 * @since 1.0.0
	 */
	public static final int TRAIT_CORRUPTED = 22;
	
	/**
	 * Trait indicating a creature is affected by rift (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_RIFT = 27;
	
	/**
	 * Trait indicating a creature is a traitor (negative trait).
	 * @since 1.0.0
	 */
	public static final int TRAIT_TRAITOR = 28;
	
	/**
	 * Trait indicating a creature is from Valrei.
	 * @since 1.0.0
	 */
	public static final int TRAIT_VALREI = 29;
	
	/**
	 * Trait indicating a creature is bred (automatically set).
	 * @since 1.0.0
	 */
	public static final int TRAIT_IS_BRED = 63;
	
	/**
	 * Brown color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_BROWN = 15;
	
	/**
	 * Gold color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_GOLD = 16;
	
	/**
	 * Black color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_BLACK = 17;
	
	/**
	 * White color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_WHITE = 18;
	
	/**
	 * Piebald pinto color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_PIEBALD_PINTO = 24;
	
	/**
	 * Blood bay color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_BLOOD_BAY = 25;
	
	/**
	 * Ebony black color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_EBONY_BLACK = 23;
	
	/**
	 * Skewbald pinto color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_SKEWBALD_PINTO = 30;
	
	/**
	 * Gold buckskin color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_GOLD_BUCKSKIN = 31;
	
	/**
	 * Black silver color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_BLACK_SILVER = 32;
	
	/**
	 * Appaloosa color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_APPALOOSA = 33;
	
	/**
	 * Chestnut color trait.
	 * @since 1.0.0
	 */
	public static final int COLOR_CHESTNUT = 34;
	
	/**
	 * Bitmask representing all regular (non-color) traits.
	 * @since 1.0.0
	 */
	public static final int REGULAR_TRAITS =
			1 << TRAIT_FIERCLY |
			1 << TRAIT_FLEETER |
			1 << TRAIT_TOUGH_BUGGER |
			1 << TRAIT_STRONG_BODY |
			1 << TRAIT_LIGHTNING |
			1 << TRAIT_CARRY_MORE |
			1 << TRAIT_STRONG_LEGS |
			1 << TRAIT_KEEN_SENSES |
			1 << TRAIT_MALFORMED_LEGS |
			1 << TRAIT_DIFFERENT_LENGTH |
			1 << TRAIT_OVERLY_AGGRESSIVE |
			1 << TRAIT_UNMOTIVATED |
			1 << TRAIT_STRONG_WILLED |
			1 << TRAIT_ILLNESS |
			1 << TRAIT_CONSTANTLY_HUNGRY |
			1 << TRAIT_FEEBLE_AND_UNHEALTHY |
			1 << TRAIT_STRONG_AND_HEALTHY |
			1 << TRAIT_SPARK;
	
	/**
	 * Bitmask representing all color traits.
	 * @since 1.0.0
	 */
	public static final int COLOR_TRAITS =
			1 << COLOR_BROWN |
			1 << COLOR_GOLD |
			1 << COLOR_BLACK |
			1 << COLOR_WHITE |
			1 << COLOR_PIEBALD_PINTO |
			1 << COLOR_BLOOD_BAY |
			1 << COLOR_EBONY_BLACK |
			1 << COLOR_SKEWBALD_PINTO |
			1 << COLOR_GOLD_BUCKSKIN |
			1 << COLOR_BLACK_SILVER |
			1 << COLOR_APPALOOSA |
			1 << COLOR_CHESTNUT |
			0;
	
	private static final Logger LOGGER = Logger.getLogger(Traits.class.getName());

	private static Method creatureGetTraits;
	static {
		try {
			creatureGetTraits = ReflectionUtil.getMethod(Creature.class, "getTraits");
		} catch (NoSuchMethodException e) {
			throw new HookException(e);
		}
	}

	/**
	 * Retrieves the traits of a creature using reflection.
	 * 
	 * <p>This method accesses the private {@code getTraits()} method of the {@link Creature}
	 * class to obtain the creature's traits as a 64-bit integer bitmask.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it operates
	 * on the provided creature parameter and does not modify any shared state.</p>
	 * 
	 * @param creature the creature whose traits to retrieve
	 * @return the traits as a 64-bit integer bitmask
	 * @throws HookException if reflection fails to access the creature's traits
	 * @since 1.0.0
	 * @see #calcNewTraits(double, boolean, long, long, long, long)
	 */
	public static long getTraits(Creature creature) {
		try {
			return ReflectionUtil.callPrivateMethod(creature, creatureGetTraits, new Object[] {});
		} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
			throw new HookException(e);
		}
	}

	/**
	 * Calculate new traits for a creature offspring.
	 * 
	 * <p>Traits that are passed along by mother and father are preferred. Breeding skill 
	 * determines the number of traits to create. Inbreeding increases the chance of 
	 * negative traits.</p>
	 * 
	 * <p>Trait bits are encoded in a 64-bit number where each bit represents a specific trait.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it operates
	 * solely on the provided parameters and does not modify any shared state.</p>
	 * 
	 * @param breederSkill breeding skill of breeder (affects number of traits)
	 * @param inbred is the new creature inbred (increases chance of negative traits)
	 * @param mothertraits traits of the mother creature
	 * @param fathertraits traits of the father creature
	 * @param regulartraits bitmask of available regular (non-color) traits
	 * @param colortraits bitmask of available color traits
	 * @return calculated traits as a 64-bit integer bitmask
	 * @since 1.0.0
	 * @see #calcNewTraits(Random, double, boolean, long, long, long, long, boolean)
	 * @see Traits
	 */
	public static long calcNewTraits(final double breederSkill, final boolean inbred, final long mothertraits, final long fathertraits, final long regulartraits, final long colortraits) {
		final Random rand = new Random();
		return calcNewTraits(rand, breederSkill, inbred, mothertraits, fathertraits, regulartraits, colortraits, Servers.isThisAPvpServer());
	}

	/**
	 * Calculate new traits for a creature offspring with a specified random number generator.
	 * 
	 * <p>Traits that are passed along by mother and father are preferred. Breeding skill 
	 * determines the number of traits to create. Inbreeding increases the chance of 
	 * negative traits.</p>
	 * 
	 * <p>Trait bits are encoded in a 64-bit number where each bit represents a specific trait.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it operates
	 * solely on the provided parameters and does not modify any shared state.</p>
	 * 
	 * @param rand random number generator to use for trait selection
	 * @param breederSkill breeding skill of breeder (affects number of traits)
	 * @param inbred is the new creature inbred (increases chance of negative traits)
	 * @param mothertraits traits of the mother creature
	 * @param fathertraits traits of the father creature
	 * @param regulartraits bitmask of available regular (non-color) traits
	 * @param colortraits bitmask of available color traits
	 * @param isThisAPvpServer whether this is a PvP server (affects corrupted trait)
	 * @return calculated traits as a 64-bit integer bitmask
	 * @since 1.0.0
	 * @see #calcNewTraits(Random, double, boolean, long, long, long, long, boolean, TraitsInfo)
	 * @see Traits
	 */
	public static long calcNewTraits(Random rand, final double breederSkill, final boolean inbred, final long mothertraits, final long fathertraits, final long regulartraits, final long colortraits, boolean isThisAPvpServer) {
		TraitsInfo traitsInfo = new TraitsInfo() {
			@Override
			public boolean isTraitNegative(int trait) {
				return Traits.isTraitNegative(trait);
			}
			@Override
			public boolean isTraitNeutral(int trait) {
				return Traits.isTraitNeutral(trait);
			}
		};
		return calcNewTraits(rand, breederSkill, inbred, mothertraits, fathertraits, regulartraits, colortraits, isThisAPvpServer, traitsInfo);
	}
	
	/**
	 * Calculate new traits for a creature offspring with full customization options.
	 * 
	 * <p>Traits that are passed along by mother and father are preferred. Breeding skill 
	 * determines the number of traits to create. Inbreeding increases the chance of 
	 * negative traits.</p>
	 * 
	 * <p>Trait bits are encoded in a 64-bit number where each bit represents a specific trait.
	 * The algorithm works as follows:
	 * <ol>
	 * <li>Inherit traits that both parents have (50% chance)</li>
	 * <li>Inherit traits that mother has (30% chance)</li>
	 * <li>Inherit traits that father has (20% chance)</li>
	 * <li>Add new traits from available pool based on remaining points</li>
	 * <li>Select final traits based on weighted probabilities</li>
	 * </ol></p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it operates
	 * solely on the provided parameters and does not modify any shared state.</p>
	 * 
	 * @param rand random number generator to use for trait selection
	 * @param breederSkill breeding skill of breeder (affects number of traits, 1-8 traits based on skill/10)
	 * @param inbred is the new creature inbred (increases chance of negative traits by 10%)
	 * @param mothertraits traits of the mother creature
	 * @param fathertraits traits of the father creature
	 * @param regulartraits bitmask of available regular (non-color) traits
	 * @param colortraits bitmask of available color traits
	 * @param isThisAPvpServer whether this is a PvP server (affects corrupted trait availability)
	 * @param traitsInfo interface to determine if traits are negative or neutral
	 * @return calculated traits as a 64-bit integer bitmask
	 * @since 1.0.0
	 * @see TraitsInfo
	 * @see Traits
	 */
	public static long calcNewTraits(Random rand, final double breederSkill, final boolean inbred, final long mothertraits, final long fathertraits, final long regulartraits, final long colortraits, boolean isThisAPvpServer, TraitsInfo traitsInfo) {
		
		final BitSet motherSet = new BitSet(64);
		final BitSet fatherSet = new BitSet(64);
		final BitSet childSet = new BitSet(64);
		final BitSet availableSet = new BitSet(64);
		
		final int maxTraits = Math.min(8, Math.max(1, (int) (breederSkill / 10.0)));
		final int maxPoints = maxTraits * 60;
		
		int allocated = 0;
		final Map<Integer, Integer> newSet = new HashMap<Integer, Integer>();
		final List<Integer> availableTraits = new ArrayList<Integer>();
		
		setTraitBits(fathertraits, fatherSet);
		setTraitBits(mothertraits, motherSet);
		setTraitBits(regulartraits | colortraits, availableSet);
		
		for (int bitIndex = 0; bitIndex < 64; ++bitIndex) {
			if (!availableSet.get(bitIndex))
				continue;
			
			availableTraits.add(bitIndex);
			if (motherSet.get(bitIndex) && fatherSet.get(bitIndex)) {
				int num = 50;
				if (inbred && traitsInfo.isTraitNegative(bitIndex)) {
					num += 10;
				}
				newSet.put(bitIndex, num);
				if (!traitsInfo.isTraitNeutral(bitIndex)) {
					allocated += 50;
				}
				availableTraits.remove((Object) bitIndex);
			} else if (motherSet.get(bitIndex)) {
				int num = 30;
				if (inbred && traitsInfo.isTraitNegative(bitIndex)) {
					num += 10;
				}
				newSet.put(bitIndex, num);
				if (!traitsInfo.isTraitNeutral(bitIndex)) {
					allocated += 30;
				}
				availableTraits.remove((Object) bitIndex);
			} else if (fatherSet.get(bitIndex)) {
				int num = 20;
				if (inbred && traitsInfo.isTraitNegative(bitIndex)) {
					num += 10;
				}
				newSet.put(bitIndex, num);
				if (!traitsInfo.isTraitNeutral(bitIndex)) {
					allocated += 20;
				}
				availableTraits.remove((Object) bitIndex);
			}
		}
		
		final int left = maxPoints - allocated;
		float traitsLeft = 0.0f;
		if (left > 0) {
			traitsLeft = left / 50.0f;
			if (traitsLeft - (int) traitsLeft > 0.0f) {
				++traitsLeft;
			}
			for (int x = 0; x < (int) traitsLeft; ++x) {
				if (rand.nextBoolean()) {
					int num2 = 20;
					final Integer newTrait = availableTraits.remove(rand.nextInt(availableTraits.size()));
					if (traitsInfo.isTraitNegative(newTrait)) {
						num2 -= maxTraits;
						if (inbred) {
							num2 += 10;
						}
					}
					if (traitsInfo.isTraitNeutral(newTrait)) {
						--x;
					}
					newSet.put(newTrait, num2);
				}
			}
			traitsLeft = maxTraits;
		} else {
			traitsLeft = Math.max(Math.min(newSet.size(), maxTraits), 3 + Server.rand.nextInt(3));
		}
		for (int t = 0; t < traitsLeft && !newSet.isEmpty(); ++t) {
			final Integer selected = pickOneTrait(rand, newSet);
			if (selected >= 0) {
				if (selected != 22 && selected != 27) {
					childSet.set(selected, true);
					newSet.remove(selected);
					if (traitsInfo.isTraitNeutral(selected)) {
						--t;
					}
				}
			} else {
				LOGGER.log(Level.WARNING, "Failed to select a trait from a map of size " + newSet.size());
			}
		}
		if (!isThisAPvpServer) {
			childSet.clear(22);
		} else if (fatherSet.get(22) || motherSet.get(22)) {
			childSet.set(22);
		}
		childSet.set(63, true);
		return getTraitBits(childSet);
	}

	/**
	 * Selects one trait from a weighted map of traits.
	 * 
	 * <p>This method implements a weighted random selection algorithm where traits
	 * with higher weights have a proportionally higher chance of being selected.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it operates
	 * solely on the provided parameters and does not modify any shared state.</p>
	 * 
	 * @param rand random number generator to use for selection
	 * @param traitMap map of trait IDs to their selection weights
	 * @return the selected trait ID, or -1 if selection fails
	 * @since 1.0.0
	 */
	static Integer pickOneTrait(final Random rand, final Map<Integer, Integer> traitMap) {
		int chance = 0;
		for (final Map.Entry<Integer, Integer> entry : traitMap.entrySet()) {
			chance += entry.getValue();
		}
		if (chance == 0 || chance < 0) {
			LOGGER.log(Level.INFO, "Trait rand=" + chance + " should not be <=0! Size of map is " + traitMap.size());
			return -1;
		}
		final int selectedTrait = rand.nextInt(chance);
		chance = 0;
		for (final Map.Entry<Integer, Integer> entry2 : traitMap.entrySet()) {
			chance += entry2.getValue();
			if (chance > selectedTrait) {
				return entry2.getKey();
			}
		}
		return -1;
	}

	/**
	 * Converts a 64-bit traits value into a BitSet representation.
	 * 
	 * <p>Each bit in the 64-bit value corresponds to a position in the BitSet,
	 * where a value of 1 indicates the trait is present.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it operates
	 * solely on the provided parameters and does not modify any shared state.</p>
	 * 
	 * @param bits 64-bit integer representing traits as bit flags
	 * @param toSet BitSet to populate with trait information
	 * @return the populated BitSet
	 * @since 1.0.0
	 * @see #getTraitBits(BitSet)
	 */
	static BitSet setTraitBits(final long bits, final BitSet toSet) {
		for (int x = 0; x < 64; ++x) {
			if (x == 0) {
				if ((bits & 0x1L) == 0x1L) {
					toSet.set(x, true);
				} else {
					toSet.set(x, false);
				}
			} else if ((bits >> x & 0x1L) == 0x1L) {
				toSet.set(x, true);
			} else {
				toSet.set(x, false);
			}
		}
		return toSet;
	}

	/**
	 * Converts a BitSet representation of traits into a 64-bit value.
	 * 
	 * <p>Each set bit in the BitSet corresponds to a bit position in the 64-bit
	 * result, where set bits indicate the presence of traits.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> This method is thread-safe as it operates
	 * solely on the provided parameters and does not modify any shared state.</p>
	 * 
	 * @param bitsProvided BitSet containing trait information
	 * @return 64-bit integer representing traits as bit flags
	 * @since 1.0.0
	 * @see #setTraitBits(long, BitSet)
	 */
	static long getTraitBits(final BitSet bitsProvided) {
		return bitsProvided.toLongArray()[0];
	}

	/**
	 * Interface for determining trait properties during trait calculation.
	 * 
	 * <p>This interface allows customization of how traits are classified as
	 * negative or neutral, enabling mods to define their own trait behaviors.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> Implementations should be thread-safe
	 * as they may be called from multiple threads concurrently.</p>
	 * 
	 * @since 1.0.0
	 * @see Traits#isTraitNegative(int)
	 * @see Traits#isTraitNeutral(int)
	 */
	public interface TraitsInfo {

		/**
		 * Determines if a trait is considered negative.
		 * 
		 * @param trait the trait ID to check
		 * @return true if the trait is negative, false otherwise
		 * @since 1.0.0
		 */
		boolean isTraitNegative(final int trait);

		/**
		 * Determines if a trait is considered neutral (doesn't count toward trait limit).
		 * 
		 * @param trait the trait ID to check
		 * @return true if the trait is neutral, false otherwise
		 * @since 1.0.0
		 */
		boolean isTraitNeutral(final int trait);

	}

}