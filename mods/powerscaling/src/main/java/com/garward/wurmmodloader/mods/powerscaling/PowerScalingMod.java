package com.garward.wurmmodloader.mods.powerscaling;

import com.garward.wurmmodloader.api.events.creature.CombatDamageEvent;
import com.garward.wurmmodloader.api.events.creature.CreatureDeathEvent;
import com.garward.wurmmodloader.api.events.creature.CreatureSpawnEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.player.BodyMenuPopulateEvent;
import com.garward.wurmmodloader.api.events.item.ItemExamineEvent;
import com.garward.wurmmodloader.api.events.item.ItemEnchantmentStringsEvent;
import com.garward.wurmmodloader.api.events.ModQueryEvent;
import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.mods.powerscaling.events.CreaturePowerInitializedEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.server.ServerPollEvent;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.garward.wurmmodloader.modsupport.actions.ActionEntryBuilder;
import com.garward.wurmmodloader.mods.powerscaling.capability.PowerLevelCapability;
import com.garward.wurmmodloader.mods.powerscaling.creature.CreaturePowerCapability;
import com.garward.wurmmodloader.mods.powerscaling.creature.CreaturePowerData;
import com.garward.wurmmodloader.core.eventlogic.PlayerUtil;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.PreInitable;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Power Scaling Mod - Core power level and combat scaling system.
 *
 * <p>Provides exponential power progression for players and creatures:</p>
 * <ul>
 *   <li>Player power from character level + kills + achievements + quests</li>
 *   <li>Creature power from CR * type multipliers (champion, unique, titan)</li>
 *   <li>Combat scaling: damage, defense, HP all scale with power level</li>
 *   <li>Every 25 power levels doubles combat effectiveness</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> Works with SoulboundGear, MaterialSystem, and
 * UpgradeTree to create comprehensive Power Fantasy RPG progression.</p>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class PowerScalingMod implements WurmServerMod, PreInitable, Configurable {

    private static final Logger logger = Logger.getLogger(PowerScalingMod.class.getName());

    // Action IDs
    public static int ACTION_VIEW_POWER_STATS;

    private Path configPath;

    private PowerScalingConfig config;
    private PowerScalingManager manager;
    private com.garward.wurmmodloader.mods.powerscaling.creature.PowerGrowthTicker growthTicker;

    // ========================================================================
    // Lifecycle
    // ========================================================================

    /**
     * Configure mod from properties file.
     */
    @Override
    public void configure(Properties properties) {
        logger.info("PowerScalingMod: Loading configuration...");

        // Get config file path (powerscaling.config in mod directory)
        String modDir = System.getProperty("user.dir");
        configPath = Paths.get(modDir, "mods", "powerscaling.config");

        logger.info("PowerScalingMod: Config path: " + configPath);
    }

    /**
     * Hook Wurm classes for power scaling integration.
     */
    @Override
    public void preInit() {
        logger.info("PowerScalingMod: preInit - Loading configuration and registering actions...");

        try {
            // Initialize config ONLY in preInit - everything else happens in onServerStarted
            PowerScalingConfig.initialize(configPath);
            config = PowerScalingConfig.getInstance();

            logger.info("PowerScalingMod: Configuration loaded successfully");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load PowerScaling configuration", e);
            throw new RuntimeException("PowerScaling initialization failed", e);
        }
    }

    // ========================================================================
    // Event Subscribers (using framework events)
    // ========================================================================

    /**
     * Add "Power Fantasy" button to body menu.
     * Now supported thanks to BodyPartModActionsPatch!
     */
    @SubscribeEvent
    public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
        // Only add to player's own body
        if (!event.getBodyPart().isBodyPartAttached()) {
            return;
        }

        if (event.getBodyPart().getOwnerId() != event.getPerformer().getWurmId()) {
            return;
        }

        logger.info("[BodyMenu] Adding Power Fantasy menu entry");

        // Add our custom action (retrieved from ModActions without importing ActionEntry)
        event.addMenuItem(ModActions.getAction(ACTION_VIEW_POWER_STATS));
    }

    /**
     * Handle creature death - process loot, track kills and award power.
     */
    @SubscribeEvent
    public void onCreatureDeath(CreatureDeathEvent event) {
        logger.info(String.format("[DEBUG] onCreatureDeath fired: victim=%s (player=%s), killer=%s, isPlayerKill=%s",
            event.getVictim().getName(), event.getVictim().isPlayer(),
            event.getKiller() != null ? event.getKiller().getName() : "null",
            event.isPlayerKill()));

        // Process loot rules for all creature deaths
        com.garward.wurmmodloader.modsupport.loot.LootManager.onCreatureDeath(event);

        // Only track player kills of NPCs (skip if manager not initialized yet)
        if (manager != null && event.isPlayerKill() && !event.isPlayerDeath()) {
            logger.info("[DEBUG] Calling manager.onCreatureKilled()");
            manager.onCreatureKilled(event.getKiller(), event.getVictim());
        }
    }

    /**
     * Handle creature examine - append power stats dynamically.
     */
    @SubscribeEvent
    public void onCreatureExamine(com.garward.wurmmodloader.api.events.creature.CreatureExamineEvent event) {
        // Skip if manager not initialized yet or if examining a player
        if (manager == null || event.getCreature().isPlayer()) {
            return;
        }

        // Skip if power-in-examine is disabled
        if (!config.isShowCreaturePowerInExamine()) {
            return;
        }

        // Get current power level (calculated dynamically based on location)
        int powerLevel = manager.getCreaturePowerLevel(event.getCreature());
        if (powerLevel <= 0) {
            return;
        }

        // Calculate multipliers using same formula as combat
        float damageMultiplier = manager.getDamageMultiplier(powerLevel);
        float defenseMultiplier = manager.getDefenseMultiplier(powerLevel);

        // Convert to bonus percentages
        int damageBonus = (int) ((damageMultiplier - 1.0f) * 100);
        int defenseBonus = (int) ((defenseMultiplier - 1.0f) * 100);

        // Append power stats to examine text
        StringBuilder powerText = new StringBuilder("\n");
        powerText.append("[Power: ").append(powerLevel).append("]");
        if (damageBonus > 0 || defenseBonus > 0) {
            powerText.append(" +").append(damageBonus).append("% damage");
            powerText.append(", +").append(defenseBonus).append("% defense");
        }

        event.appendText(powerText.toString());
    }

    /**
     * Handle combat damage - apply power scaling.
     */
    @SubscribeEvent
    public void onCombatDamage(CombatDamageEvent event) {
        // Skip if manager not initialized yet
        if (manager == null) {
            return;
        }

        // Mark players in combat for healing system
        if (event.isPlayerAttack()) {
            manager.markInCombat(event.getAttacker());
            if (config.isDebugLogging()) {
                logger.info("[Combat] Marked attacker " + event.getAttacker().getName() + " in combat");
            }
        }
        if (event.isPlayerDefending()) {
            manager.markInCombat(event.getDefender());
            if (config.isDebugLogging()) {
                logger.info("[Combat] Marked defender " + event.getDefender().getName() + " in combat");
            }
        }

        // Calculate power levels
        int attackerPower = 0;
        if (event.getAttacker() != null) {
            attackerPower = event.getAttacker().isPlayer()
                ? manager.getPlayerPowerLevel(event.getAttacker().getWurmId())
                : manager.getCreaturePowerLevel(event.getAttacker());
        }

        int defenderPower = event.getDefender().isPlayer()
            ? manager.getPlayerPowerLevel(event.getDefender().getWurmId())
            : manager.getCreaturePowerLevel(event.getDefender());

        // Apply power scaling
        float attackBonus = manager.getDamageMultiplier(attackerPower);
        float defenseReduction = manager.getDefenseMultiplier(defenderPower);

        // Apply upgrade tree modifiers for players
        double upgradeDamageMultiplier = 1.0;
        double upgradeDefenseMultiplier = 1.0;
        boolean isCriticalHit = false;

        // Attacker upgrades (only for players)
        if (event.getAttacker() != null && event.getAttacker().isPlayer()) {
            long attackerId = event.getAttacker().getWurmId();

            // Damage multiplier from upgrades (if UpgradeTree mod is loaded)
            upgradeDamageMultiplier = UpgradeTreeIntegration.getDamageMultiplier(attackerId);

            // Critical hit chance (if UpgradeTree mod is loaded)
            double critChance = UpgradeTreeIntegration.getCritChance(attackerId);
            if (critChance > 0 && Math.random() < critChance) {
                isCriticalHit = true;
                upgradeDamageMultiplier *= 2.0; // Crits deal 2x damage
            }

            // Armor penetration (reduces defender's defense, if UpgradeTree mod is loaded)
            double armorPen = UpgradeTreeIntegration.getArmorPenetration(attackerId);
            if (armorPen > 0) {
                // Reduce defender's defense multiplier by armor pen %
                defenseReduction *= (1.0 - armorPen);
                defenseReduction = Math.max(1.0f, defenseReduction); // Minimum 1.0x defense
            }
        }

        // Defender upgrades (only for players)
        if (event.getDefender().isPlayer()) {
            long defenderId = event.getDefender().getWurmId();

            // Defense multiplier from upgrades (if UpgradeTree mod is loaded)
            upgradeDefenseMultiplier = UpgradeTreeIntegration.getDefenseMultiplier(defenderId);
        }

        // Calculate final damage
        double scaledDamage = (event.getDamage() * attackBonus * upgradeDamageMultiplier)
                            / (defenseReduction * upgradeDefenseMultiplier);
        event.setDamage(scaledDamage);

        // Log if enabled
        if (config.isLogCombatCalculations() && event.getAttacker() != null) {
            String critInfo = isCriticalHit ? " [CRITICAL HIT!]" : "";
            logger.info(String.format("Combat: %s (power %d, upgrade dmg %.2fx) vs %s (power %d, upgrade def %.2fx), damage: %.1f%s",
                event.getAttacker().getName(), attackerPower, upgradeDamageMultiplier,
                event.getDefender().getName(), defenderPower, upgradeDefenseMultiplier,
                scaledDamage, critInfo));
        }

        // Send critical hit message to attacker
        if (isCriticalHit && event.getAttacker() != null && event.getAttacker().isPlayer()) {
            PlayerUtil.sendCombatMessage(event.getAttacker(), "Critical hit!");
        }
    }

    /**
     * Handle creature spawn - assign power levels.
     */
    @SubscribeEvent
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Skip if manager not initialized yet
        if (manager == null) {
            return;
        }

        // Skip players
        if (event.isPlayer()) {
            return;
        }

        // Check if we should assign creature power
        if (!config.isAssignCreaturePowerOnSpawn()) {
            return;
        }

        // Use dynamic power system if enabled
        if (config.isUseDynamicCreaturePower()) {
            assignDynamicCreaturePower(event.getCreature());
        } else {
            // Use legacy CR-based power system
            manager.assignCreaturePowerOnSpawn(event.getCreature());
        }
    }

    /**
     * Assign dynamic power to a creature using the new system.
     *
     * <p>This uses the CreaturePowerCapability to store:
     * <ul>
     *   <li>Randomized base power</li>
     *   <li>Spawn timestamp (for age-based growth)</li>
     *   <li>Spawn location (for settlement suppression)</li>
     * </ul>
     *
     * @param creature The creature to assign power to
     */
    private void assignDynamicCreaturePower(com.wurmonline.server.creatures.Creature creature) {
        try {
            // Get or create capability
            CreaturePowerData data =
                com.garward.wurmmodloader.core.capability.CapabilityManager.getInstance()
                    .getCreatureCapability(creature.getWurmId(), CreaturePowerCapability.INSTANCE);

            if (data == null) {
                logger.warning("Failed to get CreaturePowerCapability for " + creature.getName());
                return;
            }

            // Initialize if not already done
            if (!data.isInitialized()) {
                data.initialize(creature, config);

                // Get final power and tier
                int finalPower = data.getCachedPower(creature, config);
                PowerTier tier = data.getTier(creature, config);

                // Fire initialization event
                CreaturePowerInitializedEvent event = new CreaturePowerInitializedEvent(
                    creature,
                    data.basePower,
                    finalPower,
                    tier.getName()
                );
                // TODO: Fire event through event bus when available
                // For now, just log

                if (config.isLogPowerAssignments()) {
                    logger.info(String.format(
                        "[Dynamic Power] Assigned to '%s': base=%d, final=%d, tier=%s",
                        creature.getName(), data.basePower, finalPower, tier.getName()
                    ));
                }

                // Apply visual effects (Phase 4)
                com.garward.wurmmodloader.mods.powerscaling.creature.CreatureVisualEffects
                    .applyVisualEffects(creature, tier, config);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to assign dynamic power to " + creature.getName(), e);
        }
    }

    // ========================================================================
    // Capability Registration (Phase 5.5)
    // ========================================================================

    /**
     * Register PowerLevel capability for players and CreaturePower capability for creatures.
     * This replaces the old database-backed persistence system (~800 lines)
     * with the framework's automatic capability system.
     */
    @SubscribeEvent
    public void onCapabilityRegistration(CapabilityRegistrationEvent event) {
        event.registerPlayerCapability(PowerLevelCapability.INSTANCE);
        logger.info("Registered PowerLevel capability - using framework persistence");

        // Register dynamic creature power capability (Phase 1)
        event.registerCreatureCapability(CreaturePowerCapability.INSTANCE);
        logger.info("Registered CreaturePower capability - dynamic power system available");
    }

    // ========================================================================
    // Event Handlers
    // ========================================================================

    /**
     * Log power scaling system startup and start healing system.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Register custom actions (must be done after server starts)
        logger.info("Registering custom actions...");
        ModActions.init();
        ACTION_VIEW_POWER_STATS = ModActions.getNextActionId();
        ModActions.registerAction(new ActionEntryBuilder(
            (short) ACTION_VIEW_POWER_STATS,
            "Power Fantasy",
            "viewing"
        ).build());
        ModActions.registerActionPerformer(new com.garward.wurmmodloader.mods.powerscaling.actions.ViewStatsActionPerformer());
        logger.info("PowerScalingMod: Registered View Power Stats action (ID: " + ACTION_VIEW_POWER_STATS + ")");

        logger.info("Initializing PowerScalingManager...");
        PowerScalingManager.initialize(config);
        manager = PowerScalingManager.getInstance();
        logger.info("PowerScalingManager initialized - using framework capabilities");

        // Initialize UpgradeTree integration (optional)
        UpgradeTreeIntegration.initialize();

        // Initialize settlement registry (Phase 2)
        if (config.isUseDynamicCreaturePower()) {
            logger.info("Initializing SettlementRegistry...");
            com.garward.wurmmodloader.mods.powerscaling.settlement.SettlementRegistry
                .getInstance()
                .initialize(config);
            logger.info("SettlementRegistry initialized: " +
                com.garward.wurmmodloader.mods.powerscaling.settlement.SettlementRegistry
                    .getInstance()
                    .getDebugStats());

            // Initialize growth ticker (Phase 3)
            logger.info("Initializing PowerGrowthTicker...");
            growthTicker = new com.garward.wurmmodloader.mods.powerscaling.creature.PowerGrowthTicker(config);
            logger.info(String.format("PowerGrowthTicker initialized (interval: %ds, max per tick: %d)",
                config.getGrowthTickIntervalSeconds(),
                config.getMaxCreaturesPerTick()));
        }

        logger.info("Registering metal loot rules...");
        try {
            MetalLootIntegration.registerLootRules();
            logger.info("Metal loot integration enabled");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to register metal loot rules", e);
        }

        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("Power Scaling Mod - Active");
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("");
        logger.info("Power Fantasy RPG progression system loaded!");
        logger.info("");
        logger.info("Player Power Sources:");
        logger.info("  • Base Power: Character level × " + config.getBasePowerMultiplier());
        logger.info("  • Kill Power: Kills with diminishing returns");
        logger.info("  • Achievement Power: Bonus from achievements");
        logger.info("  • Quest Power: Bonus from quest completion");
        logger.info("");
        logger.info("Creature Power Scaling:");
        logger.info("  • Base: CR × " + config.getCrPowerMultiplier());
        logger.info("  • Champions: " + config.getChampionPowerMultiplier() + "x multiplier");
        logger.info("  • Uniques: " + config.getUniquePowerMultiplier() + "x multiplier");
        logger.info("  • Titans: " + config.getTitanPowerMultiplier() + "x multiplier");
        logger.info("");
        logger.info("Combat Scaling:");
        logger.info("  • Damage: +" + (config.getDamagePerPowerLevel() * 100) + "% per power level");
        logger.info("  • Defense: +" + (config.getDefensePerPowerLevel() * 100) + "% per power level");
        logger.info("  • HP: +" + (config.getHpPerPowerLevel() * 100) + "% per power level");
        logger.info("");
        logger.info("Every 25 power levels doubles your combat effectiveness!");
        logger.info("");
        logger.info("Persistence: Framework Capabilities (automatic)");
        logger.info("");

        // Start healing system AFTER server is fully loaded
        // Combat tracking is handled by onCombatDamage event
        manager.startHealingSystem();
        logger.info("Out-of-combat healing system enabled");
    }

    /**
     * Handle server poll event for age-based growth ticker.
     */
    @SubscribeEvent
    public void onServerPoll(ServerPollEvent event) {
        // Process age-based power growth
        if (config != null && config.isUseDynamicCreaturePower() && growthTicker != null) {
            growthTicker.tick();
        }
    }

    /**
     * Handle item examine event - add power stats when examining body.
     */
    @SubscribeEvent
    public void onItemExamine(ItemExamineEvent event) {
        // Skip if manager not initialized yet
        if (manager == null) {
            return;
        }

        // Only process body parts that are attached
        if (!event.getItem().isBodyPartAttached()) {
            return;
        }

        // Get the owner of the body part
        long ownerId = event.getItem().getOwnerId();
        if (ownerId == -10L) { // -10 is the "no owner" ID
            return;
        }

        // Get owner from framework (already resolved)
        // Note: Can't use explicit Creature type (avoids importing Wurm class)
        Object ownerObj = event.getOwner();
        if (ownerObj == null) {
            return; // Not a body part or owner not found
        }

        // Cast to creature for method calls
        com.wurmonline.server.creatures.Creature owner =
            (com.wurmonline.server.creatures.Creature) ownerObj;

        // Get power level based on owner type
        int totalPower = 0;
        if (owner.isPlayer()) {
            totalPower = manager.getPlayerPowerLevel(owner.getWurmId());
        } else {
            totalPower = manager.getCreaturePowerLevel(owner);
        }

        if (totalPower <= 0) {
            return; // No power to display
        }

        float damageMultiplier = manager.getDamageMultiplier(totalPower);
        float defenseMultiplier = manager.getDefenseMultiplier(totalPower);
        float hpMultiplier = manager.getHpMultiplier(totalPower);

        // Build power stats description
        StringBuilder powerStats = new StringBuilder();
        powerStats.append("\n");
        powerStats.append("═══ Power Scaling Stats ═══\n");
        powerStats.append(String.format("Total Power Level: %d\n", totalPower));
        powerStats.append("\n");
        powerStats.append("Combat Bonuses:\n");
        powerStats.append(String.format("  Damage: %.1fx\n", damageMultiplier));
        powerStats.append(String.format("  Defense: %.1fx\n", defenseMultiplier));
        powerStats.append(String.format("  HP: %.1fx\n", hpMultiplier));
        powerStats.append("\n");
        powerStats.append("Every 25 power levels doubles your combat effectiveness!");

        // Add to examine output
        event.addDescription(powerStats.toString());

        if (config.isDebugLogging()) {
            logger.info(String.format("[ItemExamine] Added power stats for owner %d (power: %d)",
                ownerId, totalPower));
        }
    }

    /**
     * Handle item enchantment strings event - add power-scaled weapon damage.
     * This shows the ACTUAL damage you'll do with power scaling applied.
     */
    @SubscribeEvent
    public void onItemEnchantmentStrings(ItemEnchantmentStringsEvent event) {
        // Skip if manager not initialized yet
        if (manager == null) {
            return;
        }

        // Only process weapons for players
        if (!event.getItem().isWeapon() || !event.getExaminer().isPlayer()) {
            return;
        }

        try {
            // Get player power level
            int totalPower = manager.getPlayerPowerLevel(event.getExaminer().getWurmId());
            if (totalPower <= 0) {
                return; // No power scaling to show
            }

            // Get damage multiplier
            float damageMultiplier = manager.getDamageMultiplier(totalPower);

            // Try to get DUSKombat's base damage calculation if available
            double baseDamage = 0;
            try {
                // Use reflection to call DUSKombat's damage calculation
                Class<?> damageMethodsClass = Class.forName("mod.piddagoras.duskombat.DamageMethods");
                java.lang.reflect.Method getBaseWeaponDamage = damageMethodsClass.getMethod(
                    "getBaseWeaponDamage",
                    com.wurmonline.server.creatures.Creature.class,
                    com.wurmonline.server.creatures.Creature.class,
                    com.wurmonline.server.items.Item.class,
                    boolean.class
                );
                baseDamage = (Double) getBaseWeaponDamage.invoke(null,
                    event.getExaminer(), null, event.getItem(), true);
            } catch (ClassNotFoundException e) {
                // DUSKombat not loaded - skip
                return;
            } catch (Exception e) {
                logger.warning("Failed to get DUSKombat base damage: " + e.getMessage());
                return;
            }

            // Calculate power-scaled damage
            double scaledDamage = baseDamage * damageMultiplier;

            // Send colored message showing power-scaled damage
            java.util.ArrayList<com.wurmonline.shared.util.MulticolorLineSegment> segments =
                new java.util.ArrayList<>();

            // Use same color scheme as DUSKombat
            byte COLOR_ORANGE = 7;
            byte COLOR_YELLOW = 8;
            byte COLOR_WHITE = 0;

            segments.add(new com.wurmonline.shared.util.MulticolorLineSegment(
                "Power Scaled Damage: ", COLOR_ORANGE));
            segments.add(new com.wurmonline.shared.util.MulticolorLineSegment(
                String.format("%d", (int)scaledDamage), COLOR_YELLOW));
            segments.add(new com.wurmonline.shared.util.MulticolorLineSegment(
                String.format(" (%.1fx from power level %d)", damageMultiplier, totalPower), COLOR_WHITE));

            PlayerUtil.sendColoredMessage(event.getExaminer(), segments);

            if (config.isDebugLogging()) {
                logger.info(String.format("[ItemEnchant] Player %s weapon damage: base=%.1f, scaled=%.1f (%.1fx)",
                    event.getExaminer().getName(), baseDamage, scaledDamage, damageMultiplier));
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to show power-scaled weapon damage", e);
        }
    }

    // ========================================================================
    // Shutdown
    // ========================================================================

    // ========================================================================
    // Public API Events (for other mods to query/modify power)
    // ========================================================================

    /**
     * Handle generic query events from other mods.
     * Provides PowerScaling API through generic event system.
     *
     * <p>Supported queries:</p>
     * <ul>
     *   <li>powerscaling:power_level - Get player's current power level</li>
     * </ul>
     */
    @SubscribeEvent
    public void onModQuery(ModQueryEvent event) {
        if (manager == null) {
            return;
        }

        if (event.getEventType().equals("powerscaling:power_level")) {
            try {
                long playerId = event.getLong("playerWurmId");
                int power = manager.getPlayerPowerLevel(playerId);
                event.set("powerLevel", power);
                event.setHandled(true);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to handle powerscaling:power_level query", e);
            }
        }
    }

    /**
     * Handle generic action events from other mods.
     * Provides PowerScaling API through generic event system.
     *
     * <p>Supported actions:</p>
     * <ul>
     *   <li>powerscaling:spend_power - Spend power (cancellable if insufficient)</li>
     * </ul>
     */
    @SubscribeEvent
    public void onModAction(ModActionEvent event) {
        if (manager == null) {
            event.setCancelled(true);
            event.setCancelReason("PowerScaling not initialized");
            return;
        }

        if (event.getEventType().equals("powerscaling:spend_power")) {
            try {
                long playerId = event.getLong("playerWurmId");
                int amount = event.getInt("amount");
                String reason = event.getString("reason");

                if (amount <= 0) {
                    event.setCancelled(true);
                    event.setCancelReason("Invalid amount");
                    return;
                }

                int currentPower = manager.getPlayerPowerLevel(playerId);

                if (currentPower < amount) {
                    event.setCancelled(true);
                    event.setCancelReason("Not enough power (have " + currentPower + ", need " + amount + ")");
                    return;
                }

                boolean success = manager.spendPower(playerId, amount);

                if (success) {
                    int remaining = manager.getPlayerPowerLevel(playerId);
                    event.set("success", true);
                    event.set("remainingPower", remaining);
                    event.setHandled(true);
                    logger.info(String.format("Player %d spent %d power for: %s (remaining: %d)",
                        playerId, amount, reason, remaining));
                } else {
                    event.setCancelled(true);
                    event.setCancelReason("Failed to spend power");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to handle powerscaling:spend_power action", e);
                event.setCancelled(true);
                event.setCancelReason("Internal error: " + e.getMessage());
            }
        }
    }

    // ========================================================================
    // DUSKombat Integration (Optional - only works if DUSKombat is loaded)
    // ========================================================================

    /**
     * Integrate with DUSKombat's damage calculation.
     * Scales damage based on player power level.
     */
    @SubscribeEvent
    public void onDUSKombatDamage(ModActionEvent event) {
        if (!event.getEventType().equals("duskombat:calculate_damage")) {
            return;
        }

        if (manager == null) {
            return;
        }

        try {
            long attackerId = event.getLong("attackerId");
            int powerLevel = manager.getPlayerPowerLevel(attackerId);

            // Scale damage by 5% per power level
            double damageMultiplier = 1.0 + (powerLevel * 0.05);

            // Read current multiplier and stack on top
            double currentMultiplier = (Double) event.get("damageMultiplier");
            event.set("damageMultiplier", currentMultiplier * damageMultiplier);
            event.setHandled(true);

            logger.fine(String.format("Player %d power bonus: %.2f%% (level %d)",
                attackerId, (damageMultiplier - 1.0) * 100, powerLevel));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to integrate with DUSKombat damage", e);
        }
    }

    /**
     * Integrate with DUSKombat's item tooltips.
     * Shows power damage bonus on weapon examine.
     */
    @SubscribeEvent
    public void onDUSKombatTooltip(ModQueryEvent event) {
        if (!event.getEventType().equals("duskombat:item_tooltip")) {
            return;
        }

        if (manager == null) {
            return;
        }

        try {
            // Only add tooltip for weapons
            if (!event.getBoolean("isWeapon")) {
                return;
            }

            long playerId = event.getLong("playerId");
            int baseDamage = event.getInt("baseDamage");
            int powerLevel = manager.getPlayerPowerLevel(playerId);

            if (powerLevel == 0) {
                return; // No power, no tooltip
            }

            // Calculate bonus damage
            int bonusDamage = (int) (baseDamage * powerLevel * 0.05);

            // Add custom tooltip line
            List<String> lines = (List<String>) event.get("tooltipLines");
            if (lines == null) {
                lines = new ArrayList<>();
            }
            lines.add(String.format("Power Bonus (Lvl %d): +%d damage (+%.0f%%)",
                powerLevel, bonusDamage, powerLevel * 5.0));

            event.set("tooltipLines", lines);
            event.setHandled(true);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to integrate with DUSKombat tooltip", e);
        }
    }

    /**
     * Cleanup on server shutdown.
     */
    public void onServerShutdown() {
        logger.info("PowerScalingMod: Shutting down...");

        if (manager != null) {
            manager.shutdown();
        }

        // Capability database is managed by framework
        logger.info("PowerScalingMod: Shutdown complete");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
