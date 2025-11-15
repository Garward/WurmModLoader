package com.garward.wurmmodloader.core.eventlogic;

import com.wurmonline.server.creatures.Creature;

/**
 * Framework utility for resolving wurm IDs to Creature objects.
 *
 * <p>This utility handles the complexity of Wurm's dual registry system
 * (Players and Creatures) so that mods never need to access Wurm classes directly.</p>
 *
 * @since 1.0.0
 */
public final class CreatureResolver {

    private CreatureResolver() {
        // Utility class
    }

    /**
     * Resolve a wurm ID to a Creature object.
     *
     * <p>Tries Players registry first (for player characters), then falls back
     * to Creatures registry (for NPCs and player-controlled pets).</p>
     *
     * @param wurmId The wurm ID to resolve
     * @return The creature, or null if not found
     */
    public static Creature getCreatureOrNull(long wurmId) {
        if (wurmId == -10L) {
            return null; // Invalid ID
        }

        // Try Players first (for player characters)
        try {
            Creature creature = com.wurmonline.server.Players.getInstance().getPlayerOrNull(wurmId);
            if (creature != null) {
                return creature;
            }
        } catch (Exception e) {
            // Players registry not available or error - fall through to Creatures
        }

        // Fall back to Creatures registry (for NPCs)
        try {
            return com.wurmonline.server.creatures.Creatures.getInstance().getCreatureOrNull(wurmId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a wurm ID belongs to a player character.
     *
     * @param wurmId The wurm ID to check
     * @return true if this is a player character
     */
    public static boolean isPlayer(long wurmId) {
        Creature creature = getCreatureOrNull(wurmId);
        return creature != null && creature.isPlayer();
    }
}
