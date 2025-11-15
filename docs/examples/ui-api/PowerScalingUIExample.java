package com.garward.wurmmodloader.mods.powerscaling.examples;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.ui.MenuEntry;
import com.garward.wurmmodloader.api.ui.MenuTarget;
import com.garward.wurmmodloader.api.ui.UIWindow;
import com.garward.wurmmodloader.core.ui.ContextMenuRegistry;
import com.garward.wurmmodloader.core.ui.WindowManager;
import com.garward.wurmmodloader.mods.powerscaling.PowerScalingManager;
import com.wurmonline.server.creatures.Creature;

/**
 * Example: PowerScaling mod using the new UI API.
 *
 * <p>This demonstrates how the PowerScaling mod can be simplified by using
 * the framework's UI API instead of manually implementing ModQuestion and
 * registering actions.</p>
 *
 * <p><strong>Before (Old Pattern):</strong></p>
 * <ul>
 *   <li>✗ Create PowerScalingStatsQuestion implements ModQuestion (50+ lines)</li>
 *   <li>✗ Create ViewStatsActionPerformer implements ActionPerformer (65+ lines)</li>
 *   <li>✗ Manually register action ID in onServerStarted</li>
 *   <li>✗ Manually subscribe to BodyMenuPopulateEvent</li>
 *   <li>✗ Build BML strings manually</li>
 * </ul>
 *
 * <p><strong>After (New API):</strong></p>
 * <ul>
 *   <li>✓ Use UIWindow.builder() for fluent BML construction (10 lines)</li>
 *   <li>✓ Use MenuEntry.builder() for context menu (5 lines)</li>
 *   <li>✓ Use ContextMenuRegistry.register() - automatic action ID management</li>
 *   <li>✓ No need to implement ModQuestion or ActionPerformer</li>
 *   <li>✓ Visibility filters built-in</li>
 * </ul>
 *
 * <p><strong>Result:</strong> Reduced from ~115 lines across 3 files to ~30 lines in 1 file!</p>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public class PowerScalingUIExample {

    /**
     * Registers the Power Stats menu entry.
     * Call this from your mod's onServerStarted() event handler.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Get registry instance
        ContextMenuRegistry registry = ContextMenuRegistry.getInstance();

        // Create menu entry with fluent API
        MenuEntry powerStatsEntry = MenuEntry.builder("Power Fantasy")
            .actionVerb("viewing")
            .onlyOwnBody()  // Only visible on player's own body
            .onClick(player -> showPowerStatsWindow((Creature) player))
            .build();

        // Register (automatic action ID management)
        registry.register("PowerScaling", MenuTarget.BODY, powerStatsEntry);

        // That's it! No need to manually:
        // - Allocate action IDs
        // - Register actions with ModActions
        // - Implement ActionPerformer
        // - Subscribe to BodyMenuPopulateEvent
        // - Implement ModQuestion
    }

    /**
     * Creates and displays the power stats window.
     *
     * @param player The player to show stats for
     */
    private void showPowerStatsWindow(Creature player) {
        PowerScalingManager manager = PowerScalingManager.getInstance();

        // Get player stats
        int totalPower = manager.getPlayerPowerLevel(player.getWurmId());
        float damageMultiplier = manager.getDamageMultiplier(totalPower);
        float defenseMultiplier = manager.getDefenseMultiplier(totalPower);
        float hpMultiplier = manager.getHpMultiplier(totalPower);

        // Build window with fluent API
        UIWindow window = UIWindow.builder("Power Scaling Stats")
            .width(400)
            .height(300)
            .addHeader("Power Scaling Stats")
            .addText("Total Power: " + totalPower)
            .addText("Damage Multiplier: " + String.format("%.1fx", damageMultiplier))
            .addText("Defense Multiplier: " + String.format("%.1fx", defenseMultiplier))
            .addText("HP Multiplier: " + String.format("%.1fx", hpMultiplier))
            .addSeparator()
            .addText("Every 25 power levels doubles your combat effectiveness!")
            .build();

        // Show window
        WindowManager.open(player, window);
    }

    /**
     * Example: Advanced window with form elements.
     */
    private void showAdvancedWindow(Creature player) {
        UIWindow window = UIWindow.builder("Power Settings")
            .width(500)
            .height(400)
            .addHeader("Configure Power Scaling")
            .addText("Choose your power source priority:")
            .addDropdown("priority", "Priority", "Combat", "Crafting", "Exploration")
            .addText("")
            .addText("Set custom power multiplier:")
            .addInput("multiplier", "Multiplier", "1.0")
            .addText("")
            .addButton("save", "Save Settings")
            .onAnswer((p, answers) -> {
                // Handle form submission
                String priority = answers.getProperty("priority");
                String multiplier = answers.getProperty("multiplier");

                Creature creature = (Creature) p;
                creature.getCommunicator().sendNormalServerMessage(
                    "Settings saved: priority=" + priority + ", multiplier=" + multiplier);
            })
            .build();

        WindowManager.open(player, window);
    }

    /**
     * Example: Menu entry with visibility filter based on power level.
     */
    private void registerAdvancedFeatureMenu() {
        MenuEntry advancedEntry = MenuEntry.builder("Advanced Power Settings")
            .actionVerb("configuring")
            .onlyFor(player -> {
                // Only visible if player has 100+ power
                Creature creature = (Creature) player;
                PowerScalingManager manager = PowerScalingManager.getInstance();
                int power = manager.getPlayerPowerLevel(creature.getWurmId());
                return power >= 100;
            })
            .onClick(player -> showAdvancedWindow((Creature) player))
            .build();

        ContextMenuRegistry.getInstance().register("PowerScaling", MenuTarget.BODY, advancedEntry);
    }

    /**
     * Example: Multiple menu entries with different visibility rules.
     */
    private void registerTieredMenus() {
        ContextMenuRegistry registry = ContextMenuRegistry.getInstance();

        // Basic stats - always visible
        MenuEntry basicStats = MenuEntry.builder("View Basic Stats")
            .onClick(player -> showBasicStatsWindow((Creature) player))
            .build();

        // Advanced stats - visible at 50+ power
        MenuEntry advancedStats = MenuEntry.builder("View Advanced Stats")
            .onlyFor(player -> checkPowerLevel(player, 50))
            .onClick(player -> showAdvancedStatsWindow((Creature) player))
            .build();

        // Legendary stats - visible at 200+ power
        MenuEntry legendaryStats = MenuEntry.builder("View Legendary Stats")
            .onlyFor(player -> checkPowerLevel(player, 200))
            .onClick(player -> showLegendaryStatsWindow((Creature) player))
            .build();

        registry.register("PowerScaling", MenuTarget.BODY, basicStats);
        registry.register("PowerScaling", MenuTarget.BODY, advancedStats);
        registry.register("PowerScaling", MenuTarget.BODY, legendaryStats);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private boolean checkPowerLevel(Object player, int minPower) {
        Creature creature = (Creature) player;
        PowerScalingManager manager = PowerScalingManager.getInstance();
        return manager.getPlayerPowerLevel(creature.getWurmId()) >= minPower;
    }

    private void showBasicStatsWindow(Creature player) {
        // Implementation...
    }

    private void showAdvancedStatsWindow(Creature player) {
        // Implementation...
    }

    private void showLegendaryStatsWindow(Creature player) {
        // Implementation...
    }
}
