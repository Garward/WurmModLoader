package com.garward.wurmmodloader.core.ui;

import com.garward.wurmmodloader.api.ui.UIWindow;
import com.garward.wurmmodloader.modsupport.questions.ModQuestion;
import com.garward.wurmmodloader.modsupport.questions.ModQuestions;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.questions.Question;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages the display of UI windows to players.
 *
 * <p>This class provides a high-level API for opening BML windows without
 * needing to implement ModQuestion or deal with Question boilerplate.</p>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>
 * UIWindow window = UIWindow.builder("My Window")
 *     .width(400).height(300)
 *     .addText("Hello, world!")
 *     .build();
 *
 * WindowManager.open(player, window);
 * </pre>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public class WindowManager {

    private static final Logger logger = Logger.getLogger(WindowManager.class.getName());

    // Track open windows for debugging
    private static final Map<Long, String> openWindows = new ConcurrentHashMap<>();

    private WindowManager() {
        // Static utility class
    }

    /**
     * Opens a UI window for a player.
     *
     * @param player The player to show the window to
     * @param window The window to display
     */
    public static void open(Object player, UIWindow window) {
        if (!(player instanceof Creature)) {
            throw new IllegalArgumentException("Player must be a Creature instance");
        }

        Creature creature = (Creature) player;

        if (!creature.isPlayer()) {
            logger.warning("Attempted to open window for non-player creature: " + creature.getName());
            return;
        }

        try {
            // Create question wrapper
            WindowQuestion questionWrapper = new WindowQuestion(creature, window);

            // Create Question instance
            Question question = ModQuestions.createQuestion(
                creature,
                window.getTitle(),
                "Window: " + window.getTitle(),
                creature.getWurmId(),
                questionWrapper
            );

            // Send to player
            question.sendQuestion();

            // Track open window
            openWindows.put(creature.getWurmId(), window.getTitle());

            logger.fine(String.format("Opened window '%s' for player %s",
                window.getTitle(), creature.getName()));

        } catch (Exception e) {
            logger.severe("Failed to open window '" + window.getTitle() + "': " + e.getMessage());
            e.printStackTrace();
            creature.getCommunicator().sendAlertServerMessage(
                "An error occurred while opening the window.");
        }
    }

    /**
     * Checks if a player has a window open.
     *
     * @param playerId The player's wurm ID
     * @return true if the player has a window open
     */
    public static boolean hasOpenWindow(long playerId) {
        return openWindows.containsKey(playerId);
    }

    /**
     * Gets the title of the window currently open for a player.
     *
     * @param playerId The player's wurm ID
     * @return The window title, or null if no window is open
     */
    public static String getOpenWindowTitle(long playerId) {
        return openWindows.get(playerId);
    }

    /**
     * Closes tracking for a player's window.
     * This is called automatically when the window is answered/closed.
     *
     * @param playerId The player's wurm ID
     */
    private static void closeWindow(long playerId) {
        openWindows.remove(playerId);
    }

    /**
     * ModQuestion wrapper for UIWindow.
     */
    private static class WindowQuestion implements ModQuestion {

        private final Creature player;
        private final UIWindow window;

        WindowQuestion(Creature player, UIWindow window) {
            this.player = player;
            this.window = window;
        }

        @Override
        public void sendQuestion(Question question) {
            try {
                // Build BML
                StringBuilder buf = new StringBuilder();

                // Add header
                buf.append(ModQuestions.getBmlHeader(question));

                // Add window content
                buf.append(window.getBML(player));

                // Add answer button
                buf.append(ModQuestions.createAnswerButton2(question));

                // Send BML
                player.getCommunicator().sendBml(
                    window.getWidth(),
                    window.getHeight(),
                    true,  // closeable
                    true,  // resizable
                    buf.toString(),
                    200, 200, 200,  // Background color (light gray)
                    window.getTitle()
                );

            } catch (Exception e) {
                logger.severe("Error sending window '" + window.getTitle() + "': " + e.getMessage());
                e.printStackTrace();
                player.getCommunicator().sendAlertServerMessage(
                    "An error occurred while displaying the window.");
            }
        }

        @Override
        public void answer(Question question, Properties answers) {
            try {
                // Close window tracking
                WindowManager.closeWindow(player.getWurmId());

                // Delegate to window's answer handler
                window.onAnswer(player, answers);

            } catch (Exception e) {
                logger.severe("Error handling window answer: " + e.getMessage());
                e.printStackTrace();
                player.getCommunicator().sendAlertServerMessage(
                    "An error occurred while processing your response.");
            }
        }
    }
}
