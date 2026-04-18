package com.garward.wurmmodloader.core.eventlogic;

import com.wurmonline.server.creatures.Creature;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Event logic for creature death handling.
 *
 * <p>This class contains ALL the complex logic for determining who killed a creature,
 * which was previously embedded in the CreatureDeathPatch bytecode patch. Following
 * the WurmModLoader architecture rule: <strong>NO LOGIC IN PATCHES</strong>.</p>
 *
 * <h2>Architecture Pattern:</h2>
 * <pre>
 * Bytecode Patch (CLEAN - no logic)
 *     ↓ passes raw data
 * ProxyServerHook.fireCreatureDeathEvent()
 *     ↓ calls EventLogic
 * CreatureDeathEventLogic.determineKiller() ← ALL LOGIC HERE
 *     ↓ returns result
 * ServerHook fires event with computed killer
 * </pre>
 *
 * @since 1.0.0
 */
public final class CreatureDeathEventLogic {

    private static final Logger LOGGER = Logger.getLogger(CreatureDeathEventLogic.class.getName());

    /**
     * Enable verbose debug logging.
     * Set via system property: -DdeathEventDebug=true
     */
    private static final boolean DEBUG = Boolean.getBoolean("deathEventDebug");

    private CreatureDeathEventLogic() {
        // Utility class
    }

    /**
     * Determine the killer of a creature using damage tracking and attacker lists.
     *
     * <p>This method encapsulates the killer determination logic that was previously
     * scattered across 70 lines of bytecode patch injection. It uses a two-phase approach:</p>
     *
     * <ol>
     *   <li><strong>Phase 1:</strong> Check damage tracking map for top damage dealer</li>
     *   <li><strong>Phase 2:</strong> Fall back to victim's latestAttackers array if needed</li>
     * </ol>
     *
     * @param victim The creature that died
     * @param damageMap Map of attackerId → total damage dealt (may be null or empty)
     * @return The killer, or null if cannot be determined
     */
    public static Creature determineKiller(Creature victim, Map<Long, Double> damageMap) {
        if (victim == null) {
            return null;
        }

        if (DEBUG) {
            String dmgInfo = (damageMap == null) ? "null" :
                            (damageMap.isEmpty() ? "empty" : damageMap.size() + " entries");
            LOGGER.info("[CreatureDeathEventLogic] " + victim.getName() + " died, damageMap: " + dmgInfo);
        }

        // Phase 1: Try to find killer from damage tracking
        Creature killer = findKillerFromDamageTracking(damageMap);

        // Phase 2: Fall back to latestAttackers if damage tracking didn't work
        if (killer == null) {
            killer = findKillerFromLatestAttackers(victim);
        }

        if (DEBUG && killer != null) {
            LOGGER.info("[CreatureDeathEventLogic] Determined killer: " + killer.getName() +
                       " (player=" + killer.isPlayer() + ")");
        }

        return killer;
    }

    /**
     * Capture the attackers map from a creature using reflection.
     *
     * <p>This must be called BEFORE Creature.die() executes, as die() clears the attackers map.
     * Used by LootManager to determine who should receive loot.</p>
     *
     * @param victim The creature about to die
     * @return Copy of the attackers map, or null if cannot be captured
     */
    public static Map<Long, Long> captureAttackersMap(Creature victim) {
        if (victim == null) {
            return null;
        }

        try {
            java.lang.reflect.Field attackersField =
                com.wurmonline.server.creatures.Creature.class.getDeclaredField("attackers");
            attackersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Long, Long> attackersMap = (Map<Long, Long>) attackersField.get(victim);

            if (attackersMap != null && !attackersMap.isEmpty()) {
                return new java.util.HashMap<>(attackersMap);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.warning("[CreatureDeathEventLogic] Failed to capture attackers map: " + e.getMessage());
        }

        return null;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Find the killer from damage tracking map.
     *
     * <p>Damage tracking is maintained by the framework and records the total damage
     * each attacker dealt to the victim. The attacker who dealt the most damage is
     * considered the killer.</p>
     *
     * @param damageMap Map of attackerId → total damage dealt
     * @return The top damage dealer, or null if map is empty or no valid attacker found
     */
    private static Creature findKillerFromDamageTracking(Map<Long, Double> damageMap) {
        if (damageMap == null || damageMap.isEmpty()) {
            return null;
        }

        long topDamagerId = -1L;
        double maxDamage = 0.0;

        // Find the attacker who dealt the most damage
        for (Map.Entry<Long, Double> entry : damageMap.entrySet()) {
            Long attackerId = entry.getKey();
            Double damage = entry.getValue();

            if (DEBUG) {
                LOGGER.info("[CreatureDeathEventLogic]   Attacker " + attackerId + " dealt " + damage + " damage");
            }

            if (damage != null && damage > maxDamage) {
                maxDamage = damage;
                topDamagerId = attackerId;
            }
        }

        // Resolve the attacker ID to a Creature object
        if (topDamagerId != -1L) {
            Creature killer = CreatureResolver.getCreatureOrNull(topDamagerId);

            if (DEBUG) {
                String killerInfo = (killer != null) ?
                    killer.getName() + " (player=" + killer.isPlayer() + ")" : "null";
                LOGGER.info("[CreatureDeathEventLogic]   CreatureResolver.getCreatureOrNull(" +
                           topDamagerId + ") -> " + killerInfo);
            }

            return killer;
        }

        return null;
    }

    /**
     * Find the killer from the victim's latestAttackers array.
     *
     * <p>This is a fallback method used when damage tracking is unavailable or empty.
     * The latestAttackers array is maintained by Wurm's vanilla combat system and
     * contains the most recent attackers in reverse chronological order.</p>
     *
     * @param victim The creature that died
     * @return The first valid attacker from the array, or null if none found
     */
    private static Creature findKillerFromLatestAttackers(Creature victim) {
        long[] attackerIds = victim.getLatestAttackers();

        if (DEBUG) {
            String attackerInfo = (attackerIds == null) ? "null" : attackerIds.length + " attackers";
            LOGGER.info("[CreatureDeathEventLogic]   getLatestAttackers(): " + attackerInfo);
        }

        if (attackerIds != null && attackerIds.length > 0) {
            long attackerId = attackerIds[0];

            if (DEBUG) {
                LOGGER.info("[CreatureDeathEventLogic]     Using attacker[0]: " + attackerId);
            }

            Creature killer = CreatureResolver.getCreatureOrNull(attackerId);

            if (DEBUG) {
                String killerInfo = (killer != null) ?
                    killer.getName() + " (player=" + killer.isPlayer() + ")" : "null";
                LOGGER.info("[CreatureDeathEventLogic]     Resolved to: " + killerInfo);
            }

            return killer;
        }

        return null;
    }
}
