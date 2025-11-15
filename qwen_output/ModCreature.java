package com.garward.wurmmodloader.modsupport.creatures;

import com.garward.wurmmodloader.modsupport.CreatureTemplateBuilder;
import com.garward.wurmmodloader.modsupport.vehicles.ModVehicleBehaviour;

import com.wurmonline.server.creatures.Traits;

/**
 * Interface for modded creatures that allows customization of creature templates, vehicle behaviors,
 * trait handling, and breeding mechanics.
 *
 * <p>This interface provides a comprehensive set of methods that mod authors can implement to define
 * custom creatures with unique behaviors, appearances, and mechanics. It serves as the primary entry
 * point for creature customization in the mod loader system.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * public class CustomHorse implements ModCreature {
 *     @Override
 *     public CreatureTemplateBuilder createCreateTemplateBuilder() {
 *         return new CreatureTemplateBuilder("mod.horse")
 *             .name("Mystic Horse")
 *             .description("A horse with magical abilities")
 *     }
 *
 *     @Override
 *     public String getTraitName(int trait) {
 *         if (trait == 1) return "Glowing";
 *         return null;
 *     }
 *
 *     @Override
 *     public boolean hasTraits() {
 *         return true;
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong> Implementations of this interface are typically instantiated
 * during server startup when creature templates are being registered. The methods are called
 * during creature creation, breeding, and trait assignment processes.</p>
 *
 * @since 1.0.0
 * @see CreatureTemplateBuilder
 * @see ModVehicleBehaviour
 * @see Traits
 */
public interface ModCreature {

	/**
	 * Creates and returns a {@link CreatureTemplateBuilder} for this creature.
	 *
	 * <p>This method is called during creature template registration to obtain the builder
	 * that defines the base properties, skills, and characteristics of the creature.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method may be called from multiple threads during
	 * server initialization, so implementations should be thread-safe or stateless.</p>
	 *
	 * @return a {@link CreatureTemplateBuilder} instance configured for this creature
	 * @since 1.0.0
	 * @see CreatureTemplateBuilder
	 */
	CreatureTemplateBuilder createCreateTemplateBuilder();
	
	/**
	 * Returns the vehicle behavior for this creature, if it can be used as a vehicle.
	 *
	 * <p>If this creature can be ridden or used as a vehicle, this method should return
	 * a {@link ModVehicleBehaviour} implementation that defines how the creature behaves
	 * as a vehicle. If the creature cannot be used as a vehicle, this method returns {@code null}.</p>
	 *
	 * <p>The default implementation returns {@code null}, indicating this creature is not a vehicle.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method may be called from multiple threads,
	 * so implementations should be thread-safe or stateless.</p>
	 *
	 * @return a {@link ModVehicleBehaviour} instance or {@code null} if this creature is not a vehicle
	 * @since 1.0.0
	 * @see ModVehicleBehaviour
	 */
	default ModVehicleBehaviour getVehicleBehaviour() {
		return null;
	}
	
	/**
	 * Adds custom encounters for this creature type.
	 *
	 * <p>This method is called to register special encounter events or behaviors for this creature.
	 * Implementations can add custom spawning rules, special interactions, or other encounter-related
	 * functionality.</p>
	 *
	 * <p>The default implementation does nothing.</p>
	 *
	 * <p><strong>Lifecycle:</strong> This method is typically called during server initialization
	 * after creature templates have been registered but before the world is fully loaded.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method is called from the main server thread during
	 * initialization, so thread safety is not typically a concern.</p>
	 *
	 * @since 1.0.0
	 */
	default public void addEncounters() {
	}
	
	/**
	 * Returns the name for a specific trait value.
	 *
	 * <p>This method is called to get the display name for a creature trait. Traits are used
	 * to define special characteristics or abilities that creatures can possess.</p>
	 *
	 * <p>The default implementation returns {@code null}, indicating no custom trait names.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method may be called from multiple threads during
	 * gameplay, so implementations should be thread-safe or stateless.</p>
	 *
	 * @param trait the trait value to get the name for
	 * @return the name of the trait, or {@code null} if this trait is not handled by this creature
	 * @since 1.0.0
	 * @see #hasTraits()
	 * @see Traits
	 */
	default String getTraitName(int trait) {
		return null;
	}
	
	/**
	 * Returns the color name for a specific trait value.
	 *
	 * <p>This method is called to get the color display name for a creature trait. This is used
	 * when displaying trait information in the client interface.</p>
	 *
	 * <p>The default implementation returns {@code null}, indicating no custom color names.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method may be called from multiple threads during
	 * gameplay, so implementations should be thread-safe or stateless.</p>
	 *
	 * @param trait the trait value to get the color name for
	 * @return the color name of the trait, or {@code null} if this trait is not handled by this creature
	 * @since 1.0.0
	 * @see #hasTraits()
	 * @see Traits
	 */
	default String getColourName(int trait) {
		return null;
	}
	
	/**
	 * Assigns traits to a creature using the provided {@link TraitsSetter}.
	 *
	 * <p>This method is called when a new creature is created to assign initial traits.
	 * Implementations can use the {@link TraitsSetter} to set specific trait values.</p>
	 *
	 * <p>The default implementation does nothing.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method is typically called from the main server thread
	 * during creature creation, so thread safety is not typically a concern.</p>
	 *
	 * @param traitsSetter the {@link TraitsSetter} used to assign trait values
	 * @since 1.0.0
	 * @see TraitsSetter
	 * @see #hasTraits()
	 */
	default void assignTraits(TraitsSetter traitsSetter) {
	}

	/**
	 * Indicates whether this creature type supports traits.
	 *
	 * <p>This method is used to determine if the creature system should attempt to assign
	 * traits to creatures of this type. If this method returns {@code false}, trait-related
	 * methods will not be called for this creature type.</p>
	 *
	 * <p>The default implementation returns {@code false}, indicating no trait support.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method may be called from multiple threads,
	 * so implementations should be thread-safe or stateless.</p>
	 *
	 * @return {@code true} if this creature supports traits, {@code false} otherwise
	 * @since 1.0.0
	 * @see #getTraitName(int)
	 * @see #getColourName(int)
	 * @see #assignTraits(TraitsSetter)
	 * @see Traits
	 */
	default boolean hasTraits() {
		return false;
	}

	/**
	 * Calculates new trait values for offspring based on parent traits and breeding skill.
	 *
	 * <p>This method is called during the breeding process to determine what traits the offspring
	 * creature should inherit. It takes into account the breeder's skill, whether the parents
	 * are inbred, and the trait values of both parents.</p>
	 *
	 * <p>The default implementation delegates to {@link Traits#calcNewTraits(double, boolean, long, long)},
	 * which provides the standard Wurm Online breeding mechanics.</p>
	 *
	 * <p><strong>Thread Safety:</strong> This method may be called from multiple threads during
	 * breeding operations, so implementations should be thread-safe or stateless.</p>
	 *
	 * @param breederSkill the skill level of the breeder (higher skill increases chance of good traits)
	 * @param inbred {@code true} if the parents are closely related, {@code false} otherwise
	 * @param mothertraits the trait bitfield of the mother
	 * @param fathertraits the trait bitfield of the father
	 * @return a long value representing the calculated trait bitfield for the offspring
	 * @since 1.0.0
	 * @see Traits#calcNewTraits(double, boolean, long, long)
	 */
	default long calcNewTraits(double breederSkill, boolean inbred, long mothertraits, long fathertraits) {
		return Traits.calcNewTraits(breederSkill, inbred, mothertraits, fathertraits);
	}
}