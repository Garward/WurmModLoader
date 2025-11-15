package com.garward.wurmmodloader.modsupport.loot;

import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;

/**
 * Functional interface for loot triggers that receive the player's communicator.
 *
 * <p>This is useful for sending messages or other communication to the player.</p>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface LootCommTrigger {
    /**
     * Trigger this loot action with the player's communicator.
     *
     * @param deadCreature The creature that was killed
     * @param comm The player's communicator
     */
    void trigger(Creature deadCreature, Communicator comm);
}
