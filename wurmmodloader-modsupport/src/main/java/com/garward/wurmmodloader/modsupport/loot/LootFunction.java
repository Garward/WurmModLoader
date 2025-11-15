package com.garward.wurmmodloader.modsupport.loot;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

/**
 * Functional interface for loot generation functions.
 *
 * @param <T> The type of value this function produces
 * @since 1.0.0
 */
@FunctionalInterface
public interface LootFunction<T> {
    /**
     * Apply this function to generate a value based on the dead creature and killer.
     *
     * @param deadCreature The creature that was killed
     * @param killer The player who killed the creature
     * @return The generated value
     */
    T apply(Creature deadCreature, Player killer);
}
