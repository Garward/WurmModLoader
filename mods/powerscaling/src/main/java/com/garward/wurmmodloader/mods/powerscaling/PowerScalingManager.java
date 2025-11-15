package com.garward.wurmmodloader.mods.powerscaling;

import com.garward.wurmmodloader.core.eventlogic.CreatureResolver;
import com.garward.wurmmodloader.core.eventlogic.CreatureUtil;
import com.garward.wurmmodloader.core.eventlogic.PlayerUtil;
import com.garward.wurmmodloader.core.eventlogic.VillageUtil;
import com.garward.wurmmodloader.core.eventlogic.WoundUtil;

// Minimal Wurm imports for type declarations only - use framework utilities for operations!
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.bodys.Wound;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.VolaTile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for power level tracking and combat scaling.
 *
 * <p>Manages player and creature power levels with in-memory caching and
 * batched database persistence. Integrates with SoulboundGear for bonus tracking.</p>
 *
 * <p><strong>Thread Safety:</strong> Thread-safe. Uses concurrent collections and
 * synchronization for cache operations.</p>
 *
 * @author Garward
 * @version 1.0.0
 */
public final class PowerScalingManager {

    private static final Logger logger = Logger.getLogger(PowerScalingManager.class.getName());

    // ========================================================================
    // Singleton
    // ========================================================================

    private static PowerScalingManager instance;

    /**
     * Initialize the singleton instance.
     *
     * @param config Configuration
     */
    public static synchronized void initialize(PowerScalingConfig config) {
        if (instance != null) {
            throw new IllegalStateException("PowerScalingManager already initialized");
        }
        instance = new PowerScalingManager(config);
        logger.info("PowerScalingManager initialized (using framework capabilities)");
    }

    /**
     * Get singleton instance.
     *
     * @return Manager instance
     * @throws IllegalStateException If not initialized
     */
    public static PowerScalingManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PowerScalingManager not initialized");
        }
        return instance;
    }

    // ========================================================================
    // Fields
    // ========================================================================

    private final PowerScalingConfig config;

    // In-memory cache (LRU eviction) - optional, for performance
    private final Map<Long, PowerLevel> playerCache;

    // Creature power cache (creatures don't persist, just cache for session)
    private final Map<Long, Integer> creaturePowerCache = new ConcurrentHashMap<>();

    // Healing system
    private final Map<Long, Long> lastCombatTime = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastHealTime = new ConcurrentHashMap<>();
    private Timer healingTimer;

    // ========================================================================
    // Constructor
    // ========================================================================

    private PowerScalingManager(PowerScalingConfig config) {
        this.config = config;

        // Create simple cache (framework handles persistence)
        if (config.isCachePlayerPowerLevels()) {
            this.playerCache = Collections.synchronizedMap(
                new LinkedHashMap<Long, PowerLevel>(config.getPowerLevelCacheSize(), 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, PowerLevel> eldest) {
                        // Framework auto-saves modified capabilities, no need to save here
                        return size() > config.getPowerLevelCacheSize();
                    }
                }
            );
        } else {
            this.playerCache = new ConcurrentHashMap<>();
        }

        // No sync timer needed - framework handles persistence automatically
        // Healing timer will be started by startHealingSystem() after server fully loads
        // (called from onServerStarted event to avoid class loading during preInit)
    }

    // ========================================================================
    // Timer Management
    // ========================================================================

    /**
     * Start healing timer for out-of-combat regeneration.
     * Must be called AFTER server is fully started to avoid class loading issues.
     */
    public void startHealingSystem() {
        if (!config.isEnableOutOfCombatRegen()) {
            logger.info("Out-of-combat healing disabled in config");
            return;
        }

        if (healingTimer != null) {
            logger.warning("Healing timer already started");
            return;
        }

        healingTimer = new Timer("PowerScaling-Healing", true);
        healingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                applyOutOfCombatHealing();
            }
        }, config.getOutOfCombatRegenInterval(), config.getOutOfCombatRegenInterval());

        logger.info("Started PowerScaling healing timer: interval=" + config.getOutOfCombatRegenInterval() + "ms");
    }

    // ========================================================================
    // Player Power Level API
    // ========================================================================

    /**
     * Get a player's power level.
     *
     * @param playerWurmId Player wurm ID
     * @return Power level value
     */
    public int getPlayerPowerLevel(long playerWurmId) {
        PowerLevel powerLevel = getOrLoadPowerLevel(playerWurmId);
        return powerLevel.getPowerLevel();
    }

    /**
     * Get or create a PowerLevel instance using framework capabilities.
     *
     * @param playerWurmId Player wurm ID
     * @return PowerLevel instance
     */
    public PowerLevel getOrLoadPowerLevel(long playerWurmId) {
        // Check cache first
        PowerLevel cached = playerCache.get(playerWurmId);
        if (cached != null) {
            return cached;
        }

        // Load from capability system (framework handles database automatically)
        try {
            // Get player from framework utility
            com.wurmonline.server.players.Player player = PlayerUtil.getPlayer(playerWurmId);
            if (player == null) {
                throw new IllegalStateException("Player not found: " + playerWurmId);
            }

            // Cast to ICapabilityProvider (injected by framework at runtime via Javassist)
            // Need intermediate Object cast to satisfy compiler
            com.garward.wurmmodloader.api.capability.ICapabilityProvider provider =
                (com.garward.wurmmodloader.api.capability.ICapabilityProvider) (Object) player;

            // Get capability (framework loads from database if needed)
            PowerLevel powerLevel = provider.getCapability(
                com.garward.wurmmodloader.mods.powerscaling.capability.PowerLevelCapability.INSTANCE
            );

            // Cache it for performance
            playerCache.put(playerWurmId, powerLevel);

            if (config.isDebugLogging()) {
                logger.fine("Loaded PowerLevel for player " + playerWurmId +
                           ": " + powerLevel.getPowerLevel());
            }

            return powerLevel;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load PowerLevel for player " + playerWurmId, e);
            // Return new instance as fallback
            PowerLevel fallback = new PowerLevel(playerWurmId);
            playerCache.put(playerWurmId, fallback);
            return fallback;
        }
    }

    /**
     * Update player's base power from character level.
     *
     * @param playerWurmId Player wurm ID
     * @param characterLevel Current character level
     */
    public void updatePlayerLevel(long playerWurmId, int characterLevel) {
        PowerLevel powerLevel = getOrLoadPowerLevel(playerWurmId);
        powerLevel.updateBasePower(characterLevel);
        // Framework automatically saves modified capability data

        if (config.isLogPowerCalculations()) {
            logger.info("Updated player " + playerWurmId + " level to " + characterLevel +
                       ", new power: " + powerLevel.getPowerLevel());
        }
    }

    /**
     * Handle creature kill for a player.
     *
     * @param killer Player who killed the creature
     * @param victim Creature that was killed
     */
    public void onCreatureKilled(Creature killer, Creature victim) {
        if (killer == null || !killer.isPlayer()) {
            return;
        }

        // Calculate character level using framework utility
        int characterLevel = CreatureUtil.getCharacterLevel(killer);

        long playerWurmId = killer.getWurmId();
        PowerLevel powerLevel = getOrLoadPowerLevel(playerWurmId);
        powerLevel.addKill(characterLevel);
        // Framework automatically saves modified capability data

        if (config.isLogPowerCalculations()) {
            logger.info("Player " + playerWurmId + " kill count: " + powerLevel.getTotalKills() +
                       ", power: " + powerLevel.getPowerLevel());
        }

        // Mark player in combat
        markInCombat(killer);
    }

    /**
     * Add achievement power bonus.
     *
     * @param playerWurmId Player wurm ID
     * @param bonusPower Power to add
     */
    public void addAchievementPower(long playerWurmId, int bonusPower) {
        PowerLevel powerLevel = getOrLoadPowerLevel(playerWurmId);
        int current = powerLevel.getAchievementPower();
        powerLevel.setAchievementPower(current + bonusPower);
        // Framework automatically saves modified capability data

        if (config.isDebugLogging()) {
            logger.info("Player " + playerWurmId + " gained " + bonusPower +
                       " achievement power, total: " + powerLevel.getAchievementPower());
        }
    }

    /**
     * Add quest power bonus.
     *
     * @param playerWurmId Player wurm ID
     * @param bonusPower Power to add
     */
    public void addQuestPower(long playerWurmId, int bonusPower) {
        PowerLevel powerLevel = getOrLoadPowerLevel(playerWurmId);
        int current = powerLevel.getQuestPower();
        powerLevel.setQuestPower(current + bonusPower);
        // Framework automatically saves modified capability data

        if (config.isDebugLogging()) {
            logger.info("Player " + playerWurmId + " gained " + bonusPower +
                       " quest power, total: " + powerLevel.getQuestPower());
        }
    }

    /**
     * Spend power on upgrades or other features.
     * Deducts power from the player's available power.
     *
     * @param playerWurmId Player wurm ID
     * @param amount Amount of power to spend
     * @return true if player had enough power and it was spent, false otherwise
     */
    public boolean spendPower(long playerWurmId, int amount) {
        PowerLevel powerLevel = getOrLoadPowerLevel(playerWurmId);
        boolean success = powerLevel.spendPower(amount);
        // Framework automatically saves modified capability data

        if (success && config.isDebugLogging()) {
            logger.info("Player " + playerWurmId + " spent " + amount +
                       " power, total spent: " + powerLevel.getSpentPower() +
                       ", remaining power: " + powerLevel.getPowerLevel());
        }

        return success;
    }

    // ========================================================================
    // Creature Power Level API
    // ========================================================================

    /**
     * Get a creature's power level.
     * Returns cached value or calculates from creature stats.
     *
     * @param creature Creature
     * @return Power level
     */
    public int getCreaturePowerLevel(Creature creature) {
        if (creature == null) {
            return 0;
        }

        // Try to get power from dynamic capability system FIRST (before cache)
        // This ensures we always get the latest dynamic power value
        try {
            com.garward.wurmmodloader.mods.powerscaling.creature.CreaturePowerData data =
                com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()
                    .getCreatureCapability(creature.getWurmId(),
                        com.garward.wurmmodloader.mods.powerscaling.creature.CreaturePowerCapability.INSTANCE);

            if (data != null && data.isInitialized()) {
                int powerLevel = data.getCachedPower(creature, config);
                if (config.isLogCreaturePowerAssignments()) {
                    logger.info("Using dynamic power for " + creature.getName() +
                               " (ID: " + creature.getWurmId() + "): " + powerLevel);
                }
                return powerLevel;
            }
        } catch (Exception e) {
            // Capability system not available, fall back to CR-based with cache
        }

        // Check cache for CR-based power
        Integer cached = creaturePowerCache.get(creature.getWurmId());
        if (cached != null) {
            return cached;
        }

        // Calculate and cache CR-based power
        int powerLevel = calculateCreaturePowerLevel(creature);
        if (config.isLogCreaturePowerAssignments()) {
            logger.info("Using CR-based power for " + creature.getName() +
                       " (ID: " + creature.getWurmId() + "): " + powerLevel);
        }

        creaturePowerCache.put(creature.getWurmId(), powerLevel);
        return powerLevel;
    }

    /**
     * Assign power level to creature on spawn.
     *
     * @param creature Creature that spawned
     */
    public void assignCreaturePowerOnSpawn(Creature creature) {
        if (!config.isAssignCreaturePowerOnSpawn()) {
            return;
        }

        try {
            // Skip creatures that aren't fully initialized
            if (creature == null || creature.getTemplate() == null) {
                return;
            }

            int powerLevel = calculateCreaturePowerLevel(creature);
            creaturePowerCache.put(creature.getWurmId(), powerLevel);

            if (config.isLogCreaturePowerAssignments()) {
                logger.info("Assigned power " + powerLevel + " to spawned " +
                           creature.getName() + " (CR: " + creature.getBaseCombatRating() + ")");
            }
        } catch (Exception e) {
            // Silently skip creatures that error during power assignment
            // This can happen for creatures that spawn incompletely initialized
        }
    }

    /**
     * Calculate creature power level from stats.
     *
     * @param creature Creature
     * @return Calculated power level
     */
    private int calculateCreaturePowerLevel(Creature creature) {
        try {
            // Use framework utilities instead of direct Wurm calls
            float cr = CreatureUtil.getCombatRating(creature);
            boolean isChampion = CreatureUtil.isChampion(creature);
            boolean isUnique = CreatureUtil.isUnique(creature);
            boolean isTitan = false; // Future: check custom titan flag

            return config.calculateCreaturePowerLevel(cr, isChampion, isUnique, isTitan);
        } catch (Exception e) {
            // Creature not fully initialized, use default power
            return 1;
        }
    }

    // ========================================================================
    // Combat Scaling Helpers
    // ========================================================================

    /**
     * Get damage multiplier for attacker.
     *
     * @param attackerPowerLevel Attacker's power level
     * @return Damage multiplier (1.0 = normal)
     */
    public float getDamageMultiplier(int attackerPowerLevel) {
        return 1.0f + (attackerPowerLevel * config.getDamagePerPowerLevel());
    }

    /**
     * Get defense multiplier for defender.
     *
     * @param defenderPowerLevel Defender's power level
     * @return Defense multiplier (1.0 = normal)
     */
    public float getDefenseMultiplier(int defenderPowerLevel) {
        return 1.0f + (defenderPowerLevel * config.getDefensePerPowerLevel());
    }

    /**
     * Get HP multiplier for creature/player.
     *
     * @param powerLevel Power level
     * @return HP multiplier (1.0 = normal)
     */
    public float getHpMultiplier(int powerLevel) {
        return 1.0f + (powerLevel * config.getHpPerPowerLevel());
    }

    // ========================================================================
    // SoulboundGear Integration
    // ========================================================================

    /**
     * Get combined damage bonus from power level and soulbound gear.
     *
     * @param playerWurmId Player wurm ID
     * @return Total damage multiplier
     */
    public float getTotalDamageBonus(long playerWurmId) {
        float powerLevelBonus = getDamageMultiplier(getPlayerPowerLevel(playerWurmId));

        // Integrate with SoulboundGear if enabled
        if (config.isIntegrateWithSoulboundGear()) {
            try {
                // Check if SoulboundGear is loaded
                Class.forName("com.garward.wurmmodloader.mods.soulboundgear.SoulboundGearManager");

                // Get soulbound bonuses
                // Note: This will be implemented when we add the integration hook
                // For now, just return power level bonus

            } catch (ClassNotFoundException e) {
                // SoulboundGear not loaded, just use power level
            }
        }

        return powerLevelBonus;
    }

    // ========================================================================
    // Note: Database sync no longer needed
    // ========================================================================
    // Framework capabilities system handles all persistence automatically:
    // - Lazy loading when first accessed
    // - Automatic dirty tracking when modified
    // - Batched writes every 5 seconds
    // - Thread-safe storage with ConcurrentHashMap

    // ========================================================================
    // Healing System - Out of Combat Regeneration
    // ========================================================================

    /**
     * Mark player as in combat and potentially apply healing if out of combat.
     *
     * @param player Player creature
     */
    public void markInCombat(Creature player) {
        if (player == null || !player.isPlayer()) {
            return;
        }

        long playerWurmId = player.getWurmId();

        // Check if we should apply healing before marking in combat
        if (isOutOfCombat(playerWurmId)) {
            Long lastHeal = lastHealTime.get(playerWurmId);
            long now = System.currentTimeMillis();

            // Only heal if enough time has passed since last heal
            if (lastHeal == null || (now - lastHeal) >= config.getOutOfCombatRegenInterval()) {
                applyHealingToPlayer(player);
                lastHealTime.put(playerWurmId, now);
            }
        }

        // Mark as in combat
        lastCombatTime.put(playerWurmId, System.currentTimeMillis());
    }

    /**
     * Mark player as in combat (simple version without healing check).
     *
     * @param playerWurmId Player wurm ID
     */
    public void markInCombat(long playerWurmId) {
        lastCombatTime.put(playerWurmId, System.currentTimeMillis());
    }

    /**
     * Check if player is out of combat.
     *
     * @param playerWurmId Player wurm ID
     * @return true if out of combat
     */
    public boolean isOutOfCombat(long playerWurmId) {
        Long lastCombat = lastCombatTime.get(playerWurmId);
        if (lastCombat == null) {
            return true; // Never been in combat
        }
        long timeSinceCombat = System.currentTimeMillis() - lastCombat;
        return timeSinceCombat >= config.getOutOfCombatDelay();
    }

    /**
     * Apply out-of-combat healing to all online players.
     * Called by healing timer every config.getOutOfCombatRegenInterval() ms.
     */
    private void applyOutOfCombatHealing() {
        try {
            // Get all online players using framework utility
            com.wurmonline.server.players.Player[] players = PlayerUtil.getAllPlayers();

            long now = System.currentTimeMillis();

            for (com.wurmonline.server.players.Player player : players) {
                // Skip if not out of combat
                if (!isOutOfCombat(player.getWurmId())) {
                    if (config.isDebugLogging()) {
                        Long lastCombat = lastCombatTime.get(player.getWurmId());
                        long timeSince = lastCombat != null ? (now - lastCombat) : -1;
                        logger.info("[Healing] Skipping " + player.getName() + " - still in combat (last: " +
                                   timeSince + "ms ago, need: " + config.getOutOfCombatDelay() + "ms)");
                    }
                    continue;
                }

                // Check if enough time has passed since last heal
                Long lastHeal = lastHealTime.get(player.getWurmId());
                long healInterval = config.getOutOfCombatRegenInterval();

                // Apply upgrade tree cooldown multiplier (if UpgradeTree mod is loaded)
                double cooldownMultiplier = UpgradeTreeIntegration.getHealingCooldownMultiplier(player.getWurmId());
                healInterval = (long) (healInterval * cooldownMultiplier);

                if (lastHeal != null && (now - lastHeal) < healInterval) {
                    continue;
                }

                // Apply healing
                applyHealingToPlayer(player);
                lastHealTime.put(player.getWurmId(), now);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error applying out-of-combat healing", e);
        }
    }

    /**
     * Apply healing to a single player.
     *
     * @param player Player to heal
     */
    private void applyHealingToPlayer(Creature player) {
        // Use framework utility to get wounds
        Wound[] woundArray = WoundUtil.getWoundArray(player);
        if (woundArray.length == 0) {
            return; // No wounds to heal
        }

        int powerLevel = getPlayerPowerLevel(player.getWurmId());
        double baseHealingPool = config.getOutOfCombatBaseHealing() + (powerLevel * config.getOutOfCombatPowerScaling());

        // Location-based multiplier (THE KEY MECHANIC)
        float locationMultiplier = getLocationHealingMultiplier(player);
        double healingPool = baseHealingPool * locationMultiplier;

        // Apply upgrade tree healing multiplier (if UpgradeTree mod is loaded)
        double upgradeMultiplier = UpgradeTreeIntegration.getHealingMultiplier(player.getWurmId());
        healingPool *= upgradeMultiplier;

        if (healingPool <= 0) {
            return; // No healing on enemy deeds
        }

        // Heal smallest wounds first (better player feel)
        Arrays.sort(woundArray, Comparator.comparingDouble(Wound::getSeverity));

        int healedCount = 0;
        for (Wound wound : woundArray) {
            if (healingPool <= 0) break;

            if (wound.getSeverity() <= healingPool) {
                // Fully heal this wound
                healingPool -= wound.getSeverity();
                wound.heal();
                healedCount++;

                if (config.isShowHealingMessages()) {
                    PlayerUtil.sendServerMessage(player,
                        "Your " + wound.getName() + " wound fully heals.", (byte) 2);
                }
            } else {
                // Partially heal this wound
                wound.modifySeverity((int) -healingPool);
                healedCount++;

                if (config.isShowHealingMessages()) {
                    PlayerUtil.sendServerMessage(player,
                        "Your " + wound.getName() + " wound feels better.", (byte) 2);
                }
                healingPool = 0;
            }
        }

        // Visual effect using framework utility
        if (healedCount > 0 && config.isShowHealingEffects()) {
            WoundUtil.sendHealingEffect(player);
        }
    }

    /**
     * Calculate location-based healing multiplier.
     *
     * @param player Player
     * @return Healing multiplier based on location
     */
    private float getLocationHealingMultiplier(Creature player) {
        // Check if player is in a village using framework utilities
        Village playerVillage = VillageUtil.getCitizenVillage(player);
        int tileX = CreatureUtil.getTileX(player);
        int tileY = CreatureUtil.getTileY(player);
        boolean onSurface = CreatureUtil.isOnSurface(player);
        VolaTile currentTile = VillageUtil.getTile(tileX, tileY, onSurface);

        if (currentTile == null) {
            return config.getWildernessHealingMultiplier(); // Default to wilderness
        }

        Village currentVillage = VillageUtil.getVillageAtTile(currentTile);

        // No village at current location
        if (currentVillage == null) {
            // Check if near guard tower (within range of friendly village)
            if (playerVillage != null) {
                // Check if within guard tower range using framework utility
                try {
                    if (VillageUtil.covers(playerVillage, tileX, tileY)) {
                        return config.getGuardTowerHealingMultiplier();
                    }
                } catch (Exception e) {
                    // Ignore - not in range
                }
            }
            return config.getWildernessHealingMultiplier(); // Pure wilderness
        }

        // Inside a village - check relationship
        int playerVillageId = VillageUtil.getVillageId(playerVillage);
        int currentVillageId = VillageUtil.getVillageId(currentVillage);

        if (playerVillage != null && currentVillageId == playerVillageId) {
            // Inside YOUR village
            return config.getVillageHealingMultiplier();
        } else if (VillageUtil.isEnemy(currentVillage, player)) {
            // Inside enemy village
            try {
                if (VillageUtil.covers(currentVillage, tileX, tileY)) {
                    // On enemy deed - NO REGEN
                    return 0.0f;
                }
            } catch (Exception e) {
                // Not on deed, but in enemy territory
            }
            // Enemy village territory (not on deed)
            return config.getEnemyTerritoryHealingMultiplier();
        } else {
            // Neutral/allied village
            return config.getWildernessHealingMultiplier(); // Treat as wilderness
        }
    }

    // ========================================================================
    // Shutdown
    // ========================================================================

    /**
     * Shutdown manager.
     */
    public void shutdown() {
        logger.info("Shutting down PowerScalingManager...");

        // Cancel healing timer (sync timers no longer exist)
        if (healingTimer != null) {
            healingTimer.cancel();
        }

        // Framework handles saving all capability data automatically
        logger.info("PowerScalingManager shutdown complete");
    }

    // ========================================================================
    // Utility
    // ========================================================================

    /**
     * Get top players by power level.
     *
     * <p><b>Note:</b> With capabilities system, we can only return cached players.
     * SQL queries across all players are not available without database access.</p>
     *
     * @param limit Maximum number of results
     * @return List of power levels sorted by total power (from cache only)
     */
    public List<PowerLevel> getTopPlayers(int limit) {
        List<PowerLevel> allCached;
        synchronized (playerCache) {
            allCached = new ArrayList<>(playerCache.values());
        }

        // Sort by power level descending
        allCached.sort((a, b) -> Integer.compare(b.getPowerLevel(), a.getPowerLevel()));

        // Return top N
        return allCached.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

    /**
     * Invalidate cache for a player (force reload from capabilities).
     *
     * @param playerWurmId Player wurm ID
     */
    public void invalidateCache(long playerWurmId) {
        // Framework auto-saves modified capabilities, just remove from cache
        playerCache.remove(playerWurmId);
    }

    /**
     * Get configuration.
     *
     * @return Config instance
     */
    public PowerScalingConfig getConfig() {
        return config;
    }
}
