package com.garward.wurmmodloader.modsupport.loot;

import com.garward.wurmmodloader.api.events.creature.CreatureDeathEvent;
import com.wurmonline.server.Players;
import com.wurmonline.server.WurmId;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manager for creature death loot rules.
 *
 * <p>This system allows mods to register loot rules that trigger when creatures die.
 * Rules can have various requirements and generate items or trigger other actions.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // In mod's init() or onServerStarted():
 * LootRule rule = LootRule.create()
 *     .requireCreature(c -> !c.isPlayer())
 *     .chance(0.5f)
 *     .addDrop(LootDrop.create(ItemList.coinSilver).ql(50f).repeat(3));
 * LootManager.add(rule);
 *
 * // In mod's event handler:
 * @SubscribeEvent
 * public void onCreatureDeath(CreatureDeathEvent event) {
 *     LootManager.onCreatureDeath(event);
 * }
 * }</pre>
 *
 * <p><strong>Debug Logging:</strong></p>
 * <p>Enable verbose loot debug logging by adding {@code -DlootDebug=true} to your server launch arguments.
 * This will log detailed information about:
 * <ul>
 *   <li>Creature deaths and attacker tracking</li>
 *   <li>Damage dealt and damage taken statistics</li>
 *   <li>Top damage dealer and top damage taker per fight</li>
 *   <li>Loot rule matching and execution</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
public class LootManager {
    private static final List<LootRule> rules = new LinkedList<LootRule>();

    public static final Logger logger = Logger.getLogger("LootManager");

    /**
     * Enable verbose loot debug logging.
     * Set via system property: -DlootDebug=true
     */
    private static final boolean DEBUG = Boolean.getBoolean("lootDebug");

    private static Field attackersField;

    static {
        try {
            attackersField = Creature.class.getDeclaredField("attackers");
            attackersField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            logger.log(Level.SEVERE, "Failed to access Creature.attackers field - loot system will not work", e);
        }
    }

    /**
     * Add new loot rule.
     *
     * @param rule The rule to add
     */
    public static void add(LootRule rule) {
        rules.add(rule);
    }

    /**
     * Process creature death event and apply loot rules.
     *
     * <p>This method extracts attacker information from the creature and
     * applies all registered loot rules.</p>
     *
     * @param event The creature death event
     */
    public static void onCreatureDeath(CreatureDeathEvent event) {
        Creature dead = event.getVictim();
        if (DEBUG) {
            logger.info(String.format("[LootManager] onCreatureDeath called for %s (template=%d)",
                dead.getName(), dead.getTemplateId()));
        }

        Map<Long, Long> attackers = getAttackersMap(dead);
        if (DEBUG) {
            logger.info(String.format("[LootManager] Found %d attackers, %d registered rules",
                attackers != null ? attackers.size() : 0, rules.size()));
        }

        if (attackers == null || attackers.isEmpty()) {
            if (DEBUG) {
                logger.info("[LootManager] No attackers, skipping loot");
            }
            return;
        }

        processCreatureDeath(dead, attackers);
    }

    /**
     * Get the attackers map from a creature.
     * First tries to retrieve captured attackers (stored before die() cleared them),
     * then falls back to reflection.
     *
     * @param creature The creature
     * @return The attackers map, or empty map if unable to access
     */
    @SuppressWarnings("unchecked")
    private static Map<Long, Long> getAttackersMap(Creature creature) {
        // Try captured attackers first (stored by CreatureDeathPatch before die() cleared them)
        try {
            Class<?> proxyClass = Class.forName("com.garward.wurmmodloader.modloader.server.ProxyServerHook");
            java.lang.reflect.Method getMethod = proxyClass.getMethod("getAndRemoveCapturedAttackers", long.class);
            Map<Long, Long> captured = (Map<Long, Long>) getMethod.invoke(null, creature.getWurmId());
            if (captured != null && !captured.isEmpty()) {
                if (DEBUG) {
                    logger.info(String.format("[LootManager] Retrieved %d captured attackers", captured.size()));
                }
                return captured;
            }
        } catch (Exception e) {
            // Fall back to reflection
        }

        // Fallback: Try reflection (will be empty if die() already cleared attackers)
        if (attackersField == null) {
            return new HashMap<Long, Long>();
        }

        try {
            Object value = attackersField.get(creature);
            if (value instanceof Map) {
                return (Map<Long, Long>) value;
            }
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Failed to access attackers field for creature", e);
        }

        return new HashMap<Long, Long>();
    }

    /**
     * Internal method to process creature death with attacker information.
     *
     * <p>Filters attackers to only include players who damaged the creature
     * within the last 10 minutes, then applies all registered loot rules.</p>
     *
     * @param dead The dead creature
     * @param attackers Map of attacker IDs to last damage timestamp
     */
    private static void processCreatureDeath(Creature dead, Map<Long, Long> attackers) {
        long now = System.currentTimeMillis();

        // Filter for player attackers who damaged within last 10 minutes
        Set<Player> killers = attackers.entrySet().stream()
                .filter(e -> now - e.getValue() < 600000L && WurmId.getType(e.getKey()) == 0)
                .flatMap(e -> {
                    Player p = Players.getInstance().getPlayerOrNull(e.getKey());
                    return p != null ? Stream.of(p) : Stream.empty();
                })
                .collect(Collectors.toSet());

        if (DEBUG) {
            logger.info(String.format("[LootManager] Processed attackers: found %d player killers", killers.size()));
        }

        if (killers.isEmpty()) {
            if (DEBUG) {
                logger.info("[LootManager] No player killers found, skipping loot rules");
            }
            return;
        }

        // Retrieve and log damage tracking
        if (DEBUG) {
            Map<Long, Double> damageMap = getDamageTracking(dead.getWurmId());
            if (!damageMap.isEmpty()) {
                logger.info(String.format("[LootManager] Damage tracking: %d attackers dealt damage", damageMap.size()));

                Player topDamageDealer = getTopDamageDealer(dead.getWurmId(), killers);
                if (topDamageDealer != null) {
                    double damageDealt = damageMap.get(topDamageDealer.getWurmId());
                    logger.info(String.format("[LootManager] Top damage dealer: %s (%.1f damage dealt)",
                        topDamageDealer.getName(), damageDealt));
                }

                Player topDamageTaker = getTopDamageTaker(dead.getWurmId(), killers);
                if (topDamageTaker != null) {
                    double damageTaken = getDamageTakenByPlayer(topDamageTaker.getWurmId(), dead.getWurmId());
                    logger.info(String.format("[LootManager] Top damage taker: %s (%.1f damage taken)",
                        topDamageTaker.getName(), damageTaken));
                }
            }
        }

        // Apply all loot rules
        if (DEBUG) {
            logger.info(String.format("[LootManager] Processing %d loot rules", rules.size()));
        }
        rules.forEach(rule -> {
            if (rule.checkCreature(dead)) {
                if (DEBUG) {
                    logger.info("[LootManager] Rule matched creature, executing...");
                }
                rule.run(dead, killers);
            }
        });
    }

    /**
     * Clear all registered loot rules.
     *
     * <p>This is primarily for testing purposes.</p>
     */
    public static void clearRules() {
        rules.clear();
    }

    /**
     * Get the number of registered loot rules.
     *
     * @return The number of rules
     */
    public static int getRuleCount() {
        return rules.size();
    }

    /**
     * Get damage tracking for a creature death.
     * Returns a map of all attackers (players and creatures) and the damage they dealt.
     *
     * <p><strong>Note:</strong> This includes damage from ALL attackers, including pets/zombies.
     * Use {@link #getTopDamageDealer(long, Set, boolean)} for filtered results.</p>
     *
     * <p><strong>Example - Proportional loot distribution:</strong></p>
     * <pre>{@code
     * Map<Long, Double> damageMap = LootManager.getDamageTracking(creatureId);
     * double totalDamage = damageMap.values().stream().mapToDouble(d -> d).sum();
     *
     * killers.forEach(player -> {
     *     double playerDamage = damageMap.getOrDefault(player.getWurmId(), 0.0);
     *     double contribution = playerDamage / totalDamage;
     *     int lootAmount = (int)(contribution * 100);
     *     // Scale loot by damage contribution
     * });
     * }</pre>
     *
     * @param creatureId The creature's Wurm ID
     * @return Map of attacker ID to total damage dealt (includes players, pets, NPCs), or empty map if no tracking available
     */
    public static Map<Long, Double> getDamageTracking(long creatureId) {
        try {
            Class<?> proxyClass = Class.forName("com.garward.wurmmodloader.modloader.server.ProxyServerHook");
            java.lang.reflect.Method getMethod = proxyClass.getMethod("getAndRemoveDamageTracking", long.class);
            @SuppressWarnings("unchecked")
            Map<Long, Double> damage = (Map<Long, Double>) getMethod.invoke(null, creatureId);
            return damage != null ? damage : new HashMap<Long, Double>();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to retrieve damage tracking", e);
            return new HashMap<Long, Double>();
        }
    }

    /**
     * Get the player who dealt the most damage to a creature.
     * Only counts direct damage from the player - pets/zombies are ignored.
     *
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * LootRule.create()
     *     .requireCreature(c -> !c.isPlayer())
     *     .addTrigger((creature, killers) -> {
     *         Player topDealer = LootManager.getTopDamageDealer(creature.getWurmId(), killers);
     *         if (topDealer != null) {
     *             // Give loot to player who dealt most direct damage
     *             giveLoot(topDealer);
     *         }
     *     });
     * }</pre>
     *
     * @param creatureId The creature's Wurm ID
     * @param killers Set of players who participated in the kill (from loot trigger)
     * @return The player who dealt the most direct damage, or null if no damage tracked
     * @see #getTopDamageDealer(long, Set, boolean) For including pet/zombie damage
     */
    public static Player getTopDamageDealer(long creatureId, Set<Player> killers) {
        return getTopDamageDealer(creatureId, killers, false);
    }

    /**
     * Get the player who dealt the most damage to a creature, with option to include pet/zombie damage.
     *
     * <p>When {@code includePets} is true, damage dealt by a player's pets, zombies, and other
     * dominated creatures is attributed to the player. This is determined by checking
     * {@code creature.getLeader()} for each attacking creature.</p>
     *
     * <p><strong>Example - Traditional loot (pets ignored):</strong></p>
     * <pre>{@code
     * Player topDealer = LootManager.getTopDamageDealer(creatureId, killers, false);
     * // Only counts direct player damage
     * }</pre>
     *
     * <p><strong>Example - Pet-friendly loot (pets count toward owner):</strong></p>
     * <pre>{@code
     * Player topDealer = LootManager.getTopDamageDealer(creatureId, killers, true);
     * // Player gets credit for their pet's damage
     * }</pre>
     *
     * <p><strong>Example - Mixed approach:</strong></p>
     * <pre>{@code
     * // Award primary loot to top direct damage dealer
     * Player directTop = LootManager.getTopDamageDealer(creatureId, killers, false);
     * givePrimaryLoot(directTop);
     *
     * // Award bonus loot to top overall (including pets)
     * Player overallTop = LootManager.getTopDamageDealer(creatureId, killers, true);
     * giveBonusLoot(overallTop);
     * }</pre>
     *
     * @param creatureId The creature's Wurm ID
     * @param killers Set of players who participated in the kill (from loot trigger)
     * @param includePets If true, attributes pet/zombie damage to their owners; if false, only counts direct player damage
     * @return The player who dealt the most damage (optionally including their pets), or null if no damage tracked
     */
    public static Player getTopDamageDealer(long creatureId, Set<Player> killers, boolean includePets) {
        Map<Long, Double> damageMap = getDamageTracking(creatureId);
        if (damageMap.isEmpty()) {
            return null;
        }

        // Find player with highest damage (optionally including their pets)
        return killers.stream()
            .max((p1, p2) -> {
                double d1 = getPlayerDamage(p1, creatureId, damageMap, includePets);
                double d2 = getPlayerDamage(p2, creatureId, damageMap, includePets);
                return Double.compare(d1, d2);
            })
            .orElse(null);
    }

    /**
     * Get total damage dealt by a player, optionally including their pets/zombies.
     *
     * @param player The player
     * @param creatureId The creature that was damaged
     * @param damageMap Pre-fetched damage tracking map
     * @param includePets If true, includes damage from player's pets/zombies
     * @return Total damage dealt
     */
    private static double getPlayerDamage(Player player, long creatureId, Map<Long, Double> damageMap, boolean includePets) {
        double playerDamage = damageMap.getOrDefault(player.getWurmId(), 0.0);

        if (!includePets) {
            return playerDamage;
        }

        // Add damage from pets/zombies owned by this player
        try {
            // Get all creatures that dealt damage
            double petDamage = damageMap.entrySet().stream()
                .filter(e -> {
                    long attackerId = e.getKey();
                    // Check if this attacker is a creature (not player) owned by this player
                    if (WurmId.getType(attackerId) != 0) { // Not a player
                        try {
                            Creature creature = com.wurmonline.server.creatures.Creatures.getInstance()
                                .getCreatureOrNull(attackerId);
                            if (creature != null && !creature.isPlayer()) {
                                // Check if creature has this player as leader/owner
                                long leaderId = creature.getLeader() != null ?
                                    creature.getLeader().getWurmId() : -1L;
                                return leaderId == player.getWurmId();
                            }
                        } catch (Exception ignored) {
                            // Creature might be dead/gone
                        }
                    }
                    return false;
                })
                .mapToDouble(Map.Entry::getValue)
                .sum();

            return playerDamage + petDamage;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to calculate pet damage", e);
            return playerDamage;
        }
    }

    /**
     * Get damage taken by a specific player from a specific creature.
     * Useful for calculating individual player contributions or tanking rewards.
     *
     * <p><strong>Note:</strong> This should be called BEFORE {@link #getDamageTracking(long)}
     * clears the creature's damage tracking.</p>
     *
     * <p><strong>Example - Custom tanking reward:</strong></p>
     * <pre>{@code
     * killers.forEach(player -> {
     *     double damageTaken = LootManager.getDamageTakenByPlayer(player.getWurmId(), creatureId);
     *     if (damageTaken > 100.0) {
     *         // Give tanking bonus to players who absorbed significant damage
     *         giveTankingBonus(player, damageTaken);
     *     }
     * });
     * }</pre>
     *
     * @param playerId The player's Wurm ID
     * @param creatureId The creature's Wurm ID
     * @return Total damage taken by the player from this creature, or 0.0 if none tracked
     */
    public static double getDamageTakenByPlayer(long playerId, long creatureId) {
        Map<Long, Double> damageTaken = getDamageTracking(playerId);
        return damageTaken.getOrDefault(creatureId, 0.0);
    }

    /**
     * Get the player who took the most damage from a creature.
     * This identifies the "tank" who absorbed the most hits, useful for tanking rewards.
     *
     * <p><strong>Example - Reward the tank:</strong></p>
     * <pre>{@code
     * LootRule.create()
     *     .requireCreature(c -> !c.isPlayer())
     *     .addTrigger((creature, killers) -> {
     *         Player tank = LootManager.getTopDamageTaker(creature.getWurmId(), killers);
     *         if (tank != null) {
     *             // Give special tanking reward
     *             giveTankingReward(tank);
     *         }
     *
     *         Player dps = LootManager.getTopDamageDealer(creature.getWurmId(), killers);
     *         if (dps != null) {
     *             // Give DPS reward
     *             giveDpsReward(dps);
     *         }
     *     });
     * }</pre>
     *
     * @param creatureId The creature's Wurm ID
     * @param killers Set of players who participated in the kill (from loot trigger)
     * @return The player who took the most damage from this creature, or null if no damage tracked
     */
    public static Player getTopDamageTaker(long creatureId, Set<Player> killers) {
        // Find player who took most damage from this creature
        return killers.stream()
            .max((p1, p2) -> {
                double d1 = getDamageTakenByPlayer(p1.getWurmId(), creatureId);
                double d2 = getDamageTakenByPlayer(p2.getWurmId(), creatureId);
                return Double.compare(d1, d2);
            })
            .orElse(null);
    }
}
