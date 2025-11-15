package com.garward.wurmmodloader.mods.soulboundgear.examples;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.ui.MenuEntry;
import com.garward.wurmmodloader.api.ui.MenuTarget;
import com.garward.wurmmodloader.api.ui.UIWindow;
import com.garward.wurmmodloader.core.ui.ContextMenuRegistry;
import com.garward.wurmmodloader.core.ui.WindowManager;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.Properties;

/**
 * Example: SoulboundGear mod using the new UI API.
 *
 * <p>This demonstrates how mods can create item context menus and
 * interactive confirmation dialogs using the UI API.</p>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public class SoulboundGearUIExample {

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ContextMenuRegistry registry = ContextMenuRegistry.getInstance();

        // "Bind Soul" menu entry
        MenuEntry bindSoulEntry = MenuEntry.builder("Bind Soul")
            .actionVerb("binding")
            .onClick(player -> showBindSoulConfirmation((Creature) player))
            .build();

        // "Check Soulbound Status" menu entry
        MenuEntry checkStatusEntry = MenuEntry.builder("Check Soulbound Status")
            .actionVerb("checking")
            .onClick(player -> showSoulboundStatus((Creature) player))
            .build();

        // Register for body menu
        registry.register("SoulboundGear", MenuTarget.BODY, bindSoulEntry);
        registry.register("SoulboundGear", MenuTarget.BODY, checkStatusEntry);
    }

    /**
     * Shows a confirmation dialog for binding soul.
     */
    private void showBindSoulConfirmation(Creature player) {
        UIWindow confirmWindow = UIWindow.builder("Bind Soul Confirmation")
            .width(400)
            .height(250)
            .addHeader("Bind Your Soul")
            .addText("Are you sure you want to bind your soul?")
            .addText("")
            .addText("Effects:")
            .addText("  • Equipment becomes soulbound")
            .addText("  • Items cannot be traded")
            .addText("  • Items return to you on death")
            .addText("")
            .addButton("confirm", "Confirm")
            .addButton("cancel", "Cancel")
            .onAnswer((p, answers) -> {
                if (answers.containsKey("confirm")) {
                    bindSoul((Creature) p);
                } else {
                    ((Creature) p).getCommunicator().sendNormalServerMessage(
                        "Soul binding cancelled.");
                }
            })
            .build();

        WindowManager.open(player, confirmWindow);
    }

    /**
     * Shows soulbound items status.
     */
    private void showSoulboundStatus(Creature player) {
        // Get soulbound items (example)
        int soulboundCount = countSoulboundItems(player);

        UIWindow statusWindow = UIWindow.builder("Soulbound Status")
            .width(400)
            .height(300)
            .addHeader("Soulbound Gear Status")
            .addText("Player: " + player.getName())
            .addText("Soul Bound: " + (isSoulBound(player) ? "Yes" : "No"))
            .addText("")
            .addText("Soulbound Items: " + soulboundCount)
            .addSeparator()
            .addText("Benefits:")
            .addText("  ✓ Items return on death")
            .addText("  ✓ Cannot be stolen")
            .addText("  ✓ Permanent ownership")
            .build();

        WindowManager.open(player, statusWindow);
    }

    /**
     * Example: Multi-step wizard UI.
     */
    private void showBindingSoulWizard(Creature player) {
        // Step 1: Introduction
        showWizardStep1(player);
    }

    private void showWizardStep1(Creature player) {
        UIWindow step1 = UIWindow.builder("Soul Binding - Step 1 of 3")
            .width(450)
            .height(300)
            .addHeader("Welcome to Soul Binding")
            .addText("This wizard will guide you through binding your soul.")
            .addText("")
            .addText("Step 1: Choose your soul type")
            .addDropdown("soulType", "Soul Type",
                "Warrior Soul", "Crafter Soul", "Explorer Soul")
            .addText("")
            .addButton("next", "Next")
            .onAnswer((p, answers) -> {
                String soulType = answers.getProperty("soulType");
                showWizardStep2((Creature) p, soulType);
            })
            .build();

        WindowManager.open(player, step1);
    }

    private void showWizardStep2(Creature player, String soulType) {
        UIWindow step2 = UIWindow.builder("Soul Binding - Step 2 of 3")
            .width(450)
            .height(350)
            .addHeader("Configure Soul Binding")
            .addText("Soul Type: " + soulType)
            .addText("")
            .addText("Step 2: Choose protection level")
            .addDropdown("protection", "Protection Level",
                "Basic", "Enhanced", "Maximum")
            .addText("")
            .addText("Enter binding phrase:")
            .addInput("phrase", "Phrase", "My soulbound gear")
            .addText("")
            .addButton("next", "Next")
            .addButton("back", "Back")
            .onAnswer((p, answers) -> {
                if (answers.containsKey("back")) {
                    showWizardStep1((Creature) p);
                } else {
                    String protection = answers.getProperty("protection");
                    String phrase = answers.getProperty("phrase");
                    showWizardStep3((Creature) p, soulType, protection, phrase);
                }
            })
            .build();

        WindowManager.open(player, step2);
    }

    private void showWizardStep3(Creature player, String soulType, String protection, String phrase) {
        UIWindow step3 = UIWindow.builder("Soul Binding - Step 3 of 3")
            .width(450)
            .height(400)
            .addHeader("Confirm Soul Binding")
            .addText("Please review your choices:")
            .addText("")
            .addText("Soul Type: " + soulType, true)
            .addText("Protection: " + protection, true)
            .addText("Phrase: " + phrase, true)
            .addText("")
            .addSeparator()
            .addText("This action is permanent and cannot be undone!")
            .addText("")
            .addButton("confirm", "Confirm Binding")
            .addButton("back", "Back")
            .onAnswer((p, answers) -> {
                if (answers.containsKey("back")) {
                    showWizardStep2((Creature) p, soulType);
                } else {
                    completeSoulBinding((Creature) p, soulType, protection, phrase);
                }
            })
            .build();

        WindowManager.open(player, step3);
    }

    private void completeSoulBinding(Creature player, String soulType, String protection, String phrase) {
        // Perform binding
        bindSoul(player);

        // Show success message
        UIWindow success = UIWindow.builder("Soul Binding Complete")
            .width(400)
            .height(250)
            .addHeader("Success!")
            .addText("Your soul has been bound successfully.")
            .addText("")
            .addText("Type: " + soulType)
            .addText("Protection: " + protection)
            .addText("")
            .addText("Your items are now protected!")
            .build();

        WindowManager.open(player, success);
    }

    // ========================================================================
    // Placeholder Methods
    // ========================================================================

    private void bindSoul(Creature player) {
        // Implementation
        player.getCommunicator().sendNormalServerMessage("Your soul has been bound!");
    }

    private boolean isSoulBound(Creature player) {
        // Implementation
        return false;
    }

    private int countSoulboundItems(Creature player) {
        // Implementation
        return 0;
    }
}
