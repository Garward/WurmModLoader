package com.garward.wurmmodloader.modsupport.creatures;

/**
 * Interface for setting trait bits on creatures in the Wurm server environment.
 *
 * <p>This interface provides a mechanism for mod authors to manipulate creature traits
 * by setting individual bits in the creature's trait bitfield. Implementations of this
 * interface are typically provided by the mod loader and made available to mods during
 * creature initialization or modification phases.</p>
 *
 * <p><strong>Purpose:</strong> This interface allows fine-grained control over creature
 * characteristics by directly manipulating the underlying bit representation of traits.
 * Each bit in the trait field represents a specific characteristic or ability of the creature.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Example implementation usage
 * public class CreatureModifier {
 *     public void makeCreatureAggressive(TraitsSetter traitsSetter) {
 *         // Set the aggressive trait bit (assuming bit 3 represents aggression)
 *         traitsSetter.setTraitBit(3, true);
 *     }
 *
 *     public void removeFlyingAbility(TraitsSetter traitsSetter) {
 *         // Clear the flying trait bit (assuming bit 7 represents flight capability)
 *         traitsSetter.setTraitBit(7, false);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Lifecycle:</strong> Instances of this interface are typically available during
 * creature creation, loading, or modification events. The exact lifecycle depends on the
 * mod loader implementation and when creature traits can be legally modified.</p>
 *
 * <p><strong>Thread Safety:</strong> Implementations are not required to be thread-safe.
 * This interface should only be used from the main server thread or during safe modification
 * windows as defined by the mod loader.</p>
 *
 * @since 1.0.0
 * @see com.garward.wurmmodloader.modsupport.creatures.CreatureModifier
 */
public interface TraitsSetter {
    
    /**
     * Sets or clears a specific bit in the creature's trait bitfield.
     *
     * <p>This method allows direct manipulation of individual trait bits. Each bit position
     * corresponds to a specific trait or characteristic of the creature. Setting a bit to
     * {@code true} typically enables the associated trait, while setting it to {@code false}
     * disables it.</p>
     *
     * <p><strong>Implementation Note:</strong> Implementations should validate the bit index
     * to ensure it falls within the valid range for the creature's trait bitfield.</p>
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * // Enable trait at bit position 5
     * traitsSetter.setTraitBit(5, true);
     *
     * // Disable trait at bit position 2
     * traitsSetter.setTraitBit(2, false);
     * }</pre>
     *
     * @param bitIndex the zero-based index of the bit to set or clear (0-31 typically)
     * @param value {@code true} to set the bit (enable trait), {@code false} to clear it (disable trait)
     * @throws IllegalArgumentException if the bitIndex is negative or exceeds the maximum
     *         supported bit position for creature traits
     * @throws IllegalStateException if called outside of a valid creature modification context
     * @since 1.0.0
     */
    void setTraitBit(int bitIndex, boolean value);
}