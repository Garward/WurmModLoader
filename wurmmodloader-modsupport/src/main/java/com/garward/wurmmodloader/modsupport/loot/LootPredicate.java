package com.garward.wurmmodloader.modsupport.loot;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

/**
 * Functional interface for loot predicates.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface LootPredicate {
    /**
     * Test if the loot conditions are met for the given creature and killer.
     *
     * @param deadCreature The creature that was killed
     * @param killer The player who killed the creature
     * @return true if conditions are met, false otherwise
     */
    boolean test(Creature deadCreature, Player killer);

    /**
     * Return a composed predicate that represents a logical AND of this predicate and another.
     *
     * @param other The other predicate to AND with this one
     * @return A composed predicate
     */
    default LootPredicate and(LootPredicate other) {
        return (c, k) -> test(c, k) && other.test(c, k);
    }
}
