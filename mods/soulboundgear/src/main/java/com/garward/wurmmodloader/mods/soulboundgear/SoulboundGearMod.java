package com.garward.wurmmodloader.mods.soulboundgear;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.creature.CreatureDeathEvent;
import com.garward.wurmmodloader.api.events.item.ItemDropEvent;
import com.garward.wurmmodloader.api.events.item.ItemExamineEvent;
import com.garward.wurmmodloader.api.events.item.ItemTradeEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.mods.soulboundgear.actions.BindSoulAction;
import com.garward.wurmmodloader.mods.soulboundgear.actions.CheckSoulboundStatusAction;
import com.garward.wurmmodloader.mods.soulboundgear.capability.SoulboundItemCapability;
import com.garward.wurmmodloader.mods.soulboundgear.hooks.ItemExamineHook;
import com.garward.wurmmodloader.mods.soulboundgear.hooks.ItemTransferHook;
import com.garward.wurmmodloader.mods.soulboundgear.hooks.XPAwardHook;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Soulbound Gear Mod
 *
 * <p>Allows players to bind items to their soul at deity altars. Soulbound
 * items gain XP from kills, level up, and can be customized with upgrade tree
 * nodes and material infusions.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Bind items at deity altars (cannot be traded/dropped)</li>
 *   <li>Items gain XP from creature kills</li>
 *   <li>Level up system (1-20) with configurable curves</li>
 *   <li>Upgrade points to unlock tree nodes</li>
 *   <li>Material infusion slots for elemental damage</li>
 *   <li>Custom names and descriptions</li>
 *   <li>Batched database writes for performance</li>
 * </ul>
 *
 * <p><strong>Database:</strong></p>
 * <ul>
 *   <li>soulbound_items - Item progression data</li>
 *   <li>soulbound_xp_batch - Dirty tracking for batched writes</li>
 * </ul>
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class SoulboundGearMod implements WurmServerMod, PreInitable, Configurable {

    private static final Logger logger = Logger.getLogger(SoulboundGearMod.class.getName());

    /**
     * Configure mod from properties file.
     */
    @Override
    public void configure(Properties properties) {
        logger.info("SoulboundGearMod: Loading configuration...");
        SoulboundConfig.initialize(properties);
        logger.info("Configuration loaded from framework-provided properties");
    }

    /**
     * Initialize mod before server startup.
     * Initializes manager and registers custom actions.
     * Event hooks and persistence are handled by the framework.
     */
    @Override
    public void preInit() {
        logger.info("SoulboundGearMod: Initializing...");

        try {
            // Initialize manager (using capabilities instead of database)
            SoulboundConfig config = SoulboundConfig.getInstance();
            SoulboundGearManager.initialize(config);
            logger.info("SoulboundGearManager initialized (using framework capabilities)");

            // Initialize ModActions system
            ModActions.init();

            // Register BindSoulAction
            BindSoulAction bindAction = new BindSoulAction();
            ModActions.registerAction(bindAction);
            logger.info("Registered BindSoulAction");

            // Register CheckSoulboundStatusAction
            CheckSoulboundStatusAction statusAction = new CheckSoulboundStatusAction();
            ModActions.registerAction(statusAction);
            logger.info("Registered CheckSoulboundStatusAction");

            // Note: Creature death hooks are handled by framework events (CreatureDeathEvent)
            // Note: Item persistence is handled by framework capabilities

            logger.info("SoulboundGearMod: Actions registered, using framework capabilities");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize mod", e);
            throw new RuntimeException("Mod initialization failed", e);
        }
    }

    /**
     * Register SoulboundItem capability for items.
     * This replaces the old database-backed persistence system (~600 lines)
     * with the framework's automatic capability system.
     */
    @SubscribeEvent
    public void onCapabilityRegistration(CapabilityRegistrationEvent event) {
        event.registerItemCapability(SoulboundItemCapability.INSTANCE);
        logger.info("Registered SoulboundItem capability - using framework persistence");
    }

    /**
     * Handle creature death events for XP award to soulbound weapons.
     * Uses framework event system instead of manual hooks.
     */
    @SubscribeEvent
    public void onCreatureDeath(CreatureDeathEvent event) {
        // Award XP for player kills of NPCs (not player vs player)
        if (event.isPlayerKill() && !event.isPlayerDeath()) {
            XPAwardHook.onCreatureDeath(event.getVictim(), event.getKiller());
        }
    }

    /**
     * Handle item examine events to show soulbound information.
     */
    @SubscribeEvent
    public void onItemExamine(ItemExamineEvent event) {
        logger.info("[DEBUG] onItemExamine called for item: " + event.getItem().getName() +
                    " (ID: " + event.getItem().getWurmId() + ") by " + event.getExaminer().getName());

        try {
            SoulboundGearManager manager = SoulboundGearManager.getInstance();
            java.util.Optional<SoulboundItem> soulbound = manager.getItem(event.getItem());

            logger.info("[DEBUG] Soulbound check result: " + (soulbound.isPresent() ? "BOUND" : "NOT BOUND"));

            if (soulbound.isPresent()) {
                // Build soulbound info text
                SoulboundItem item = soulbound.get();
                SoulboundConfig config = SoulboundConfig.getInstance();

                StringBuilder sb = new StringBuilder("\n\n=== SOULBOUND ===\n");

                // Owner
                try {
                    com.wurmonline.server.players.Player owner =
                        com.wurmonline.server.Players.getInstance().getPlayerOrNull(item.getOwnerWurmId());
                    if (owner != null) {
                        sb.append("Bound to: ").append(owner.getName()).append("\n");
                    }
                } catch (Exception e) {
                    sb.append("Bound to: Player ID ").append(item.getOwnerWurmId()).append("\n");
                }

                // Level and XP
                sb.append("Level: ").append(item.getLevel());
                if (item.getLevel() < config.getMaxItemLevel()) {
                    int xpNeeded = config.getXPRequiredForLevel(item.getLevel() + 1);
                    sb.append(" (").append(item.getCurrentXP()).append("/").append(xpNeeded).append(" XP)\n");
                } else {
                    sb.append(" (MAX)\n");
                }

                // Upgrade points
                if (item.getUpgradePoints() > 0) {
                    sb.append("Upgrade Points: ").append(item.getUpgradePoints()).append("\n");
                }

                // Statistics
                if (item.getKills() > 0) {
                    sb.append("Kills: ").append(item.getKills()).append("\n");
                }

                // Material infusions
                if (!item.getMaterialInfusions().isEmpty()) {
                    sb.append("Infusions: ").append(item.getMaterialInfusions().size())
                      .append("/").append(config.getMaxInfusionSlots()).append("\n");
                }

                String description = sb.toString();
                logger.info("[DEBUG] Adding soulbound description (length: " + description.length() + "):\n" + description);
                event.addDescription(description);
                logger.info("[DEBUG] Description added. Event has additional description: " + event.hasAdditionalDescription());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error adding soulbound info to examine text", e);
        }
    }

    /**
     * Handle item trade events to prevent trading soulbound items.
     */
    @SubscribeEvent
    public void onItemTrade(ItemTradeEvent event) {
        if (!ItemTransferHook.canTransferItem(event.getItem(), event.getGiver())) {
            event.cancel();
        }
    }

    /**
     * Handle item drop events to prevent dropping soulbound items.
     */
    @SubscribeEvent
    public void onItemDrop(ItemDropEvent event) {
        // Allow death drops (items go to corpse), block intentional drops
        boolean allowDeath = event.isDeathDrop();
        if (!ItemTransferHook.canDropItem(event.getItem(), event.getDropper(), allowDeath)) {
            event.cancel();
        }
    }

    /**
     * Server startup complete - log configuration summary.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("Soulbound Gear Mod - Ready");
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("");
        logger.info("Configuration Summary:");
        logger.info("  Max Item Level: " + SoulboundConfig.getInstance().getMaxItemLevel());
        logger.info("  Base XP/Kill: " + SoulboundConfig.getInstance().getBaseXPPerKill());
        logger.info("  Max Infusion Slots: " + SoulboundConfig.getInstance().getMaxInfusionSlots());
        logger.info("  Persistence: Framework Capabilities (automatic)");
        logger.info("");
        logger.info("How to Use:");
        logger.info("  1. Take a weapon to a deity altar");
        logger.info("  2. Right-click altar and select 'Bind Soul'");
        logger.info("  3. Item becomes soulbound and gains XP from kills");
        logger.info("  4. Use upgrade points to unlock tree nodes");
        logger.info("  5. Infuse materials for elemental damage");
        logger.info("");
        logger.info("Commands (Future):");
        logger.info("  /soulbound info - View soulbound item stats");
        logger.info("  /soulbound tree - View upgrade tree");
        logger.info("");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
