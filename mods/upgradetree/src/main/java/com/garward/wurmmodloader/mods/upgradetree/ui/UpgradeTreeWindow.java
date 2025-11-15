package com.garward.wurmmodloader.mods.upgradetree.ui;

import com.garward.wurmmodloader.api.ui.UIWindow;
import com.garward.wurmmodloader.api.ui.UIWindowBuilder;
import com.garward.wurmmodloader.api.events.ModQueryEvent;
import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.core.ui.WindowManager;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.mods.upgradetree.Upgrade;
import com.garward.wurmmodloader.mods.upgradetree.UpgradeTreeManager;
import com.wurmonline.server.creatures.Creature;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modern UI for the Upgrade Tree using the UIWindow API.
 *
 * <p>This replaces the old ModQuestion-based UI with a cleaner, more maintainable
 * implementation using the framework's UIWindow.builder() API.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Multi-page navigation (Previous/Next)</li>
 *   <li>Radio buttons for selecting upgrades to unlock</li>
 *   <li>Status indicators: [AVAILABLE], [LOCKED], [UNLOCKED]</li>
 *   <li>Requirement checking and display</li>
 *   <li>Zero BML knowledge required!</li>
 * </ul>
 *
 * @author WurmModLoader Examples
 * @since 1.0.0
 */
public class UpgradeTreeWindow {

    private static final Logger logger = Logger.getLogger(UpgradeTreeWindow.class.getName());

    private final Creature player;
    private int currentPage = 0;
    private static final int UPGRADES_PER_PAGE = 5;

    /**
     * Creates a new upgrade tree window for the given player.
     *
     * @param player The player viewing the upgrades
     */
    public UpgradeTreeWindow(Creature player) {
        this.player = player;
    }

    /**
     * Shows the upgrade tree window.
     */
    public void show() {
        showCurrentPage();
    }

    /**
     * Displays the current page of upgrades.
     */
    private void showCurrentPage() {
        UpgradeTreeManager manager = UpgradeTreeManager.getInstance();

        // Get player data
        Set<String> playerUpgrades = manager.getPlayerUpgrades(player.getWurmId());
        int unlockedCount = playerUpgrades.size();

        // Get all upgrades as list for pagination
        List<Upgrade> allUpgrades = new ArrayList<>(manager.getAllUpgrades());
        int totalUpgrades = allUpgrades.size();
        int totalPages = (int) Math.ceil((double) totalUpgrades / UPGRADES_PER_PAGE);

        // Calculate page bounds
        int startIdx = currentPage * UPGRADES_PER_PAGE;
        int endIdx = Math.min(startIdx + UPGRADES_PER_PAGE, totalUpgrades);
        List<Upgrade> pageUpgrades = allUpgrades.subList(startIdx, endIdx);

        // Build window using UIWindow API
        UIWindow window = buildWindow(pageUpgrades, playerUpgrades, currentPage + 1, totalPages, unlockedCount, totalUpgrades);

        // Display the window
        WindowManager.open(player, window);
    }

    /**
     * Builds the UIWindow for the current page.
     *
     * @param pageUpgrades The upgrades to display on this page
     * @param playerUpgrades The upgrades the player has already unlocked
     * @param pageNum Current page number (1-indexed for display)
     * @param totalPages Total number of pages
     * @param unlockedCount Number of unlocked upgrades
     * @param totalUpgrades Total number of upgrades
     * @return The built UIWindow
     */
    private UIWindow buildWindow(List<Upgrade> pageUpgrades, Set<String> playerUpgrades,
                                  int pageNum, int totalPages, int unlockedCount, int totalUpgrades) {
        UpgradeTreeManager manager = UpgradeTreeManager.getInstance();

        // Get current power level from PowerScaling (using generic event API)
        int currentPower = 0;
        ModQueryEvent powerQuery = new ModQueryEvent("powerscaling:power_level");
        powerQuery.set("playerWurmId", player.getWurmId());
        EventBus.getInstance().post(powerQuery);

        if (powerQuery.isHandled()) {
            currentPower = powerQuery.getInt("powerLevel");
        } else {
            logger.log(Level.WARNING, "PowerScaling mod not responding to power queries (is it loaded?)");
        }

        // Start building the window
        UIWindowBuilder builder = UIWindow.builder("Upgrade Tree")
            .width(600)
            .height(500)
            .addHeader("Upgrade Tree - Page " + pageNum + " / " + totalPages)
            .addText("")
            .addText("Available Power: " + currentPower, true)
            .addText("Unlocked: " + unlockedCount + " / " + totalUpgrades)
            .addText("");

        // Add each upgrade on this page
        for (Upgrade upgrade : pageUpgrades) {
            boolean unlocked = playerUpgrades.contains(upgrade.getId());
            boolean canUnlock = !unlocked && manager.meetsRequirements(player.getWurmId(), upgrade);

            // Status indicator
            String status = unlocked ? "[UNLOCKED]" : (canUnlock ? "[AVAILABLE]" : "[LOCKED]");

            // Radio button for available upgrades, or bold text for others
            if (canUnlock) {
                builder.addRadioButton("upgrade", upgrade.getId(), status + " " + upgrade.getName());
            } else {
                builder.addText(status + " " + upgrade.getName(), true);
            }

            // Description and cost
            builder.addText("  " + upgrade.getDescription());
            builder.addText("  Cost: " + upgrade.getCost() + " power | Tier: " + upgrade.getTier());

            // Requirements
            if (upgrade.hasRequirements()) {
                StringBuilder reqText = new StringBuilder("  Requires: ");
                if (!upgrade.getRequirements().isEmpty()) {
                    reqText.append(String.join(", ", upgrade.getRequirements()));
                }
                if (upgrade.getMinUpgradesRequired() > 0) {
                    if (!upgrade.getRequirements().isEmpty()) {
                        reqText.append(" + ");
                    }
                    reqText.append(upgrade.getMinUpgradesRequired()).append(" total upgrades");
                }
                builder.addText(reqText.toString());
            }

            builder.addText(""); // Spacer between upgrades
        }

        // Add navigation and action buttons
        builder.addSeparator();

        // Unlock button
        builder.addButton("unlock", "UNLOCK SELECTED");

        // Navigation buttons
        if (currentPage > 0) {
            builder.addButton("prevpage", "<< Previous");
        }
        if (currentPage < totalPages - 1) {
            builder.addButton("nextpage", "Next >>");
        }

        // Set up the answer handler
        builder.onAnswer((p, answers) -> handleAnswer(answers));

        return builder.build();
    }

    /**
     * Handles user interaction with the window.
     *
     * @param answers The form data submitted by the player
     */
    private void handleAnswer(Properties answers) {
        String prevPage = answers.getProperty("prevpage");
        String nextPage = answers.getProperty("nextpage");
        String unlock = answers.getProperty("unlock");

        // Navigation
        if (prevPage != null && currentPage > 0) {
            currentPage--;
            showCurrentPage();
            return;
        }

        if (nextPage != null) {
            UpgradeTreeManager manager = UpgradeTreeManager.getInstance();
            int totalUpgrades = manager.getAllUpgrades().size();
            int totalPages = (int) Math.ceil((double) totalUpgrades / UPGRADES_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                showCurrentPage();
                return;
            }
        }

        // Unlock action
        if (unlock != null) {
            String selectedUpgrade = answers.getProperty("upgrade");
            if (selectedUpgrade != null && !selectedUpgrade.isEmpty()) {
                handleUnlock(selectedUpgrade);
            } else {
                player.getCommunicator().sendNormalServerMessage("Please select an upgrade first.");
            }
            // Refresh the window
            showCurrentPage();
        }
    }

    /**
     * Handles unlocking an upgrade.
     *
     * @param upgradeId The ID of the upgrade to unlock
     */
    private void handleUnlock(String upgradeId) {
        UpgradeTreeManager manager = UpgradeTreeManager.getInstance();

        // Verify the upgrade exists
        Upgrade upgrade = null;
        for (Upgrade u : manager.getAllUpgrades()) {
            if (u.getId().equals(upgradeId)) {
                upgrade = u;
                break;
            }
        }

        if (upgrade == null) {
            player.getCommunicator().sendNormalServerMessage("Error: Upgrade not found.");
            logger.warning("Player " + player.getName() + " tried to unlock unknown upgrade: " + upgradeId);
            return;
        }

        // Check if already unlocked
        if (manager.getPlayerUpgrades(player.getWurmId()).contains(upgradeId)) {
            player.getCommunicator().sendNormalServerMessage("You already have this upgrade.");
            return;
        }

        // Check requirements
        if (!manager.meetsRequirements(player.getWurmId(), upgrade)) {
            player.getCommunicator().sendNormalServerMessage("You don't meet the requirements for this upgrade.");
            return;
        }

        // Check and deduct power cost via PowerScaling generic event API
        ModActionEvent spendAction = new ModActionEvent("powerscaling:spend_power");
        spendAction.set("playerWurmId", player.getWurmId());
        spendAction.set("amount", upgrade.getCost());
        spendAction.set("reason", "Upgrade: " + upgrade.getName());
        EventBus.getInstance().post(spendAction);

        if (spendAction.isCancelled()) {
            String reason = spendAction.getCancelReason();
            player.getCommunicator().sendNormalServerMessage(
                "Cannot unlock upgrade: " + (reason != null ? reason : "Unknown error"));
            return;
        }

        if (!spendAction.isHandled()) {
            logger.log(Level.SEVERE, "PowerScaling mod not responding (is it loaded?)");
            player.getCommunicator().sendNormalServerMessage(
                "Error: PowerScaling mod not installed. Cannot unlock upgrades.");
            return;
        }

        boolean spendSuccess = spendAction.getBoolean("success");
        if (!spendSuccess) {
            player.getCommunicator().sendNormalServerMessage(
                "Failed to spend power. Please try again.");
            return;
        }

        // Unlock the upgrade
        boolean unlockSuccess = manager.unlockUpgrade(player.getWurmId(), upgradeId);

        if (unlockSuccess) {
            player.getCommunicator().sendSafeServerMessage(
                "Unlocked: " + upgrade.getName() + "! " + upgrade.getDescription() +
                " (Spent " + upgrade.getCost() + " power)");
            logger.info("Player " + player.getName() + " unlocked upgrade: " + upgradeId +
                       " (cost: " + upgrade.getCost() + " power)");
        } else {
            player.getCommunicator().sendNormalServerMessage(
                "Failed to unlock upgrade. Please try again.");
            // Note: Power was already spent - this is an error condition
            // In a production system, you'd want to refund the power here
            logger.severe("Upgrade unlock failed after power was spent! Player: " + player.getName() +
                         ", Upgrade: " + upgradeId);
        }
    }
}
