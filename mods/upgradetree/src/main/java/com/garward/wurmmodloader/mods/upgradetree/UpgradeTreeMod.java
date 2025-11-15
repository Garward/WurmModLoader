package com.garward.wurmmodloader.mods.upgradetree;

import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.ui.MenuEntry;
import com.garward.wurmmodloader.api.ui.MenuTarget;
import com.garward.wurmmodloader.core.ui.ContextMenuRegistry;
import com.garward.wurmmodloader.mods.upgradetree.ui.UpgradeTreeWindow;
import com.garward.wurmmodloader.modloader.interfaces.Configurable;
import com.garward.wurmmodloader.modloader.interfaces.WurmServerMod;
import com.wurmonline.server.creatures.Creature;

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
     * Opens the upgrade tree window for a player.
     *
     * @param player The player to show the window to
     */
    private void openUpgradeTreeWindow(Creature player) {
        logger.info("Opening upgrade tree for player: " + player.getName());

        UpgradeTreeWindow window = new UpgradeTreeWindow(player);
        window.show();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
