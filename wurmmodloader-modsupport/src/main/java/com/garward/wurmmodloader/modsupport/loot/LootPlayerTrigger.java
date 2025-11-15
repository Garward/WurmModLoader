package com.garward.wurmmodloader.modsupport.loot;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

/**
 * Functional interface for loot triggers that run once per killer.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface LootPlayerTrigger {
    /**
     * Trigger this loot action for the dead creature and a single killer.
     *
     * @param deadCreature The creature that was killed
     * @param killer The player who killed the creature
     */
    void trigger(Creature deadCreature, Player killer);
}
