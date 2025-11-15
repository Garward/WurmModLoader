package com.garward.wurmmodloader.modsupport.loot;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

import java.util.Set;

/**
 * Functional interface for loot triggers that run once per creature death.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface LootTrigger {
    /**
     * Trigger this loot action for the dead creature and all matching killers.
     *
     * @param deadCreature The creature that was killed
     * @param killers The set of players who killed the creature
     */
    void trigger(Creature deadCreature, Set<Player> killers);
}
