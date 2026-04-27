package com.garward.wurmmodloader.mods.upgradetree;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.api.events.server.CapabilityRegistrationEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.ui.MenuEntry;
import com.garward.wurmmodloader.api.ui.MenuTarget;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.core.ui.ContextMenuRegistry;
import com.garward.wurmmodloader.modcomm.Channel;
import com.garward.wurmmodloader.modcomm.ModComm;
import com.garward.wurmmodloader.modcomm.PlayerModConnection;
import com.garward.wurmmodloader.mods.upgradetree.pets.PetEventHandlers;
import com.garward.wurmmodloader.mods.upgradetree.pets.PlayerPetsCapability;
import com.garward.wurmmodloader.mods.upgradetree.ui.UpgradeTreeWindow;
import com.garward.wurmmodloader.mods.upgradetree.ui.UpgradeTreeWindowDeclarative;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.players.Player;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Upgrade Tree Mod
 *
 * Spend power levels (from PowerScaling) to unlock permanent upgrades.
 * Creates strategic risk/reward gameplay: temporary weakness for long-term power.
 *
 * Features:
 * - JSON-configurable upgrades (server owners can customize)
 * - Tier-based progression (foundation → specialized → ultimate)
 * - Multiple paths (offense, defense, utility)
 * - Data-driven effect system
 *
 * Integration:
 * - Requires PowerScaling mod for power level management
 * - Uses event system for effect application
 *
 * @author Power Fantasy RPG Team
 * @version 1.0.0
 */
public class UpgradeTreeMod implements WurmServerMod, Configurable {

    private static final Logger logger = Logger.getLogger(UpgradeTreeMod.class.getName());

    /**
     * Configure mod from properties file.
     */
    @Override
    public void configure(Properties properties) {
        logger.info("[UpgradeTree] Loading configuration...");
        // Configuration loaded from upgrades.json
    }


    /**
     * Initialize upgrade tree system and register UI.
     */
    @SubscribeEvent
    public void onCapabilityRegistration(CapabilityRegistrationEvent event) {
        event.registerPlayerCapability(PlayerPetsCapability.INSTANCE);
        logger.info("[UpgradeTree] Registered PlayerPets capability");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("Upgrade Tree Mod - Initializing");
        logger.info("═══════════════════════════════════════════════════════════════");

        try {
            // Initialize database
            UpgradeTreeManager.getInstance().initDatabase();

            // Load upgrades from JSON config
            File configFile = new File("mods/upgradetree/upgrades.json");
            if (!configFile.exists()) {
                logger.warning("[UpgradeTree] Config file not found: " + configFile.getAbsolutePath());
                logger.warning("[UpgradeTree] Creating default config...");
                // TODO: Copy default config from resources
                return;
            }

            UpgradeTreeManager.getInstance().loadUpgradesFromConfig(configFile);

            // Register UI using modern API
            registerUI();

            // Wire pet-class event handlers (TameAttempt/TameComplete/PetReleased/CombatDamage)
            EventBus.getInstance().register(new PetEventHandlers());
            logger.info("[UpgradeTree] Registered pet-class event handlers");

            logger.info("[UpgradeTree] System ready!");
            logger.info("[UpgradeTree] Loaded " + UpgradeTreeManager.getInstance().getAllUpgrades().size() + " upgrades");
            logger.info("");
            logger.info("Spend power levels to unlock permanent upgrades!");
            logger.info("Right-click body → UpgradeTree → View Upgrades");
            logger.info("");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[UpgradeTree] Failed to initialize", e);
        }

        logger.info("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Register UI menu entries using the modern UI API.
     */
    private void registerUI() {
        logger.info("[UpgradeTree] Registering UI menu entries...");

        // Create menu entry using modern API
        MenuEntry viewUpgradesEntry = MenuEntry.builder("View Upgrades")
            .actionVerb("viewing")
            .onClick(player -> openUpgradeTreeWindow((Creature) player))
            .build();

        // Register with the context menu system
        ContextMenuRegistry.getInstance().register("UpgradeTree", MenuTarget.BODY, viewUpgradesEntry);

        logger.info("[UpgradeTree] UI registered successfully");
    }

    /**
     * Opens the upgrade tree window for a player. Players whose client has the
     * declarativeui channel ({@code com.garward.ui}) get the rich graph
     * window; everyone else falls back to the BML question window so vanilla
     * clients still work.
     */
    private void openUpgradeTreeWindow(Creature player) {
        logger.info("Opening upgrade tree for player: " + player.getName());

        if (hasDeclarativeUiChannel(player)) {
            new UpgradeTreeWindowDeclarative(player).show();
        } else {
            new UpgradeTreeWindow(player).show();
        }
    }

    private static boolean hasDeclarativeUiChannel(Creature creature) {
        if (!(creature instanceof Player)) return false;
        try {
            PlayerModConnection conn = ModComm.getPlayerConnectionPublic((Player) creature);
            if (conn == null || !conn.isActive() || conn.getChannels() == null) return false;
            for (Channel ch : conn.getChannels()) {
                if ("com.garward.ui".equals(ch.getName())) return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Handles {@code ui:action} events emitted by declarativeui when our
     * window's buttons are clicked. Recognised actions:
     * <ul>
     *   <li>{@code unlock:<id>} — attempt to unlock the upgrade and refresh</li>
     *   <li>{@code info:<id>} — locked/unlocked node click; surface details in chat</li>
     *   <li>{@code tree:close} — dismiss the window</li>
     * </ul>
     */
    @SubscribeEvent
    public void onUiAction(ModActionEvent event) {
        if (!"ui:action".equals(event.getEventType())) return;
        if (!UpgradeTreeWindowDeclarative.WINDOW_ID.equals(event.getString("windowId"))) return;

        Object playerObj = event.get("player");
        if (!(playerObj instanceof Creature)) return;
        Creature creature = (Creature) playerObj;
        String action = event.getString("action");
        if (action == null) return;

        if ("tree:close".equals(action)) {
            new UpgradeTreeWindowDeclarative(creature).close();
            return;
        }

        if (action.startsWith("unlock:")) {
            String upgradeId = action.substring("unlock:".length());
            UpgradeUnlockHelper.attemptUnlock(creature, upgradeId);
            new UpgradeTreeWindowDeclarative(creature).show();
            return;
        }

        if (action.startsWith("info:")) {
            String upgradeId = action.substring("info:".length());
            UpgradeUnlockHelper.describe(creature, upgradeId);
        }
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
