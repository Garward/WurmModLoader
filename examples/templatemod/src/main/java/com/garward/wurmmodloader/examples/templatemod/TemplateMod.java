package com.garward.wurmmodloader.examples.templatemod;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.server.ServerStartedEvent;
import com.garward.wurmmodloader.api.ui.MenuEntry;
import com.garward.wurmmodloader.api.ui.MenuTarget;
import com.garward.wurmmodloader.core.ui.ContextMenuRegistry;
import com.garward.wurmmodloader.modloader.interfaces.WurmMod;
import com.wurmonline.server.creatures.Creature;

import java.util.logging.Logger;

/**
 * Simple template mod that demonstrates the UI API.
 *
 * This serves as a minimal example of:
 * - WurmMod interface implementation
 * - Context menu registration using the UI API
 * - Custom button that sends messages to the player
 * - Multi-page BML questionnaire with navigation
 *
 * When a player right-clicks their body, they'll see two buttons:
 * - "Simple Example": Sends a simple message
 * - "Questionnaire": Opens a multi-page questionnaire demonstrating BmlBuilder
 */
public class TemplateMod implements WurmMod {

    private static final Logger logger = Logger.getLogger(TemplateMod.class.getName());

    @Override
    public void preInit() {
        logger.info("TemplateMod: preInit called");
    }

    /**
     * Register context menu buttons when server starts.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        logger.info("TemplateMod: Registering UI examples...");

        // Simple message button
        MenuEntry exampleButton = MenuEntry.builder("Simple Example")
            .actionVerb("clicking")
            .onClick(player -> handleExampleClick((Creature) player))
            .build();

        // Questionnaire button
        MenuEntry questionnaireButton = MenuEntry.builder("Questionnaire")
            .actionVerb("opening")
            .onClick(player -> openQuestionnaire((Creature) player))
            .build();

        // Register both buttons for body menus
        ContextMenuRegistry registry = ContextMenuRegistry.getInstance();
        registry.register("TemplateMod", MenuTarget.BODY, exampleButton);
        registry.register("TemplateMod", MenuTarget.BODY, questionnaireButton);

        logger.info("TemplateMod: UI examples registered successfully");
    }

    /**
     * Handle the Simple Example button click.
     * Sends examine text and a thank you message to the player.
     */
    private void handleExampleClick(Creature player) {
        String playerName = player.getName();

        // Send examine text (like when examining an item)
        player.getCommunicator().sendNormalServerMessage(
            "You examine yourself and feel a sense of wonder.");

        // Send the thank you message in event text style
        player.getCommunicator().sendSafeServerMessage(
            "Thank you for clicking the Example Button " + playerName + "!");

        logger.info("Example button clicked by: " + playerName);
    }

    /**
     * Opens the multi-page questionnaire example.
     * Demonstrates UIWindow API, dropdowns, and page navigation.
     */
    private void openQuestionnaire(Creature player) {
        logger.info("Opening questionnaire for: " + player.getName());

        // Create and show the questionnaire using the modern UI API
        QuestionnaireExample questionnaire = new QuestionnaireExample(player);
        questionnaire.show();
    }
}
