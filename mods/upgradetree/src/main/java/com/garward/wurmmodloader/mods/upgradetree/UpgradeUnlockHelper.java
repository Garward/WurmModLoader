package com.garward.wurmmodloader.mods.upgradetree;

import com.garward.wurmmodloader.api.events.ModActionEvent;
import com.garward.wurmmodloader.core.event.EventBus;
import com.wurmonline.server.creatures.Creature;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared unlock-attempt logic used by both the BML window
 * ({@code UpgradeTreeWindow}) and the declarativeui window
 * ({@code UpgradeTreeWindowDeclarative}).
 */
public final class UpgradeUnlockHelper {

    private static final Logger logger = Logger.getLogger(UpgradeUnlockHelper.class.getName());

    private UpgradeUnlockHelper() {}

    public static void attemptUnlock(Creature player, String upgradeId) {
        UpgradeTreeManager manager = UpgradeTreeManager.getInstance();

        Upgrade upgrade = null;
        for (Upgrade u : manager.getAllUpgrades()) {
            if (u.getId().equals(upgradeId)) { upgrade = u; break; }
        }
        if (upgrade == null) {
            player.getCommunicator().sendNormalServerMessage("Error: Upgrade not found.");
            logger.warning("Player " + player.getName() + " tried to unlock unknown upgrade: " + upgradeId);
            return;
        }

        if (manager.getPlayerUpgrades(player.getWurmId()).contains(upgradeId)) {
            player.getCommunicator().sendNormalServerMessage("You already have this upgrade.");
            return;
        }

        if (!manager.meetsRequirements(player.getWurmId(), upgrade)) {
            player.getCommunicator().sendNormalServerMessage("You don't meet the requirements for this upgrade.");
            return;
        }

        ModActionEvent spend = new ModActionEvent("powerscaling:spend_power");
        spend.set("playerWurmId", player.getWurmId());
        spend.set("amount", upgrade.getCost());
        spend.set("reason", "Upgrade: " + upgrade.getName());
        EventBus.getInstance().post(spend);

        if (spend.isCancelled()) {
            String reason = spend.getCancelReason();
            player.getCommunicator().sendNormalServerMessage(
                "Cannot unlock upgrade: " + (reason != null ? reason : "Unknown error"));
            return;
        }
        if (!spend.isHandled()) {
            logger.log(Level.SEVERE, "PowerScaling mod not responding (is it loaded?)");
            player.getCommunicator().sendNormalServerMessage(
                "Error: PowerScaling mod not installed. Cannot unlock upgrades.");
            return;
        }
        if (!spend.getBoolean("success")) {
            player.getCommunicator().sendNormalServerMessage("Failed to spend power. Please try again.");
            return;
        }

        if (manager.unlockUpgrade(player.getWurmId(), upgradeId)) {
            player.getCommunicator().sendSafeServerMessage(
                "Unlocked: " + upgrade.getName() + "! " + upgrade.getDescription()
                + " (Spent " + upgrade.getCost() + " power)");
            logger.info("Player " + player.getName() + " unlocked upgrade: " + upgradeId
                + " (cost: " + upgrade.getCost() + " power)");
        } else {
            player.getCommunicator().sendNormalServerMessage(
                "Failed to unlock upgrade. Please try again.");
            logger.severe("Upgrade unlock failed after power was spent! Player: "
                + player.getName() + ", Upgrade: " + upgradeId);
        }
    }

    public static void describe(Creature player, String upgradeId) {
        UpgradeTreeManager manager = UpgradeTreeManager.getInstance();
        for (Upgrade u : manager.getAllUpgrades()) {
            if (u.getId().equals(upgradeId)) {
                boolean unlocked = manager.getPlayerUpgrades(player.getWurmId()).contains(upgradeId);
                String status = unlocked ? "[UNLOCKED]"
                    : (manager.meetsRequirements(player.getWurmId(), u) ? "[AVAILABLE]" : "[LOCKED]");
                player.getCommunicator().sendNormalServerMessage(
                    status + " " + u.getName() + " — " + u.getDescription()
                    + " (Cost: " + u.getCost() + " power, Tier " + u.getTier() + ")");
                return;
            }
        }
    }
}
