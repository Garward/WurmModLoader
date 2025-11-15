package com.garward.wurmmodloader.examples.templatemod;

import com.garward.wurmmodloader.api.ui.UIWindow;
import com.garward.wurmmodloader.core.ui.WindowManager;
import com.wurmonline.server.creatures.Creature;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Example of a multi-page questionnaire using the modern UI API.
 *
 * <p>This demonstrates the WurmModLoader UI API for building interactive forms
 * WITHOUT needing any BML knowledge. Everything is done through the high-level
 * {@link UIWindow} builder API.</p>
 *
 * <p><strong>Key Features Demonstrated:</strong></p>
 * <ul>
 *   <li>Multi-page navigation with Previous/Next buttons</li>
 *   <li>Dropdown selections for multiple choice questions</li>
 *   <li>State preservation across pages</li>
 *   <li>Dynamic responses based on user selections</li>
 *   <li>Zero BML knowledge required!</li>
 * </ul>
 *
 * <p><strong>How to Use:</strong></p>
 * <pre>
 * // From your mod's menu click handler:
 * QuestionnaireExample questionnaire = new QuestionnaireExample(player);
 * questionnaire.show();
 * </pre>
 *
 * @author WurmModLoader Examples
 * @since 1.0.0
 * @see UIWindow
 * @see WindowManager
 */
public class QuestionnaireExample {

    private static final Logger logger = Logger.getLogger(QuestionnaireExample.class.getName());

    private final Creature player;
    private int currentPage = 0;
    private static final int TOTAL_PAGES = 2;

    // Store selections across pages
    private String page1Selection = null;
    private String page2Selection = null;

    /**
     * Creates a new questionnaire for the given player.
     *
     * @param player The player who will see the questionnaire
     */
    public QuestionnaireExample(Creature player) {
        this.player = player;
    }

    /**
     * Shows the questionnaire window to the player.
     *
     * <p>Call this method to open the first page of the questionnaire.</p>
     */
    public void show() {
        showCurrentPage();
    }

    /**
     * Displays the current page of the questionnaire.
     *
     * <p>This method builds the appropriate page content using the UIWindow API
     * and displays it using WindowManager.</p>
     */
    private void showCurrentPage() {
        UIWindow window;

        if (currentPage == 0) {
            window = buildPage1();
        } else if (currentPage == 1) {
            window = buildPage2();
        } else {
            // Should never happen, but just in case
            logger.warning("Invalid page number: " + currentPage);
            return;
        }

        // Display the window using WindowManager
        WindowManager.open(player, window);
    }

    /**
     * Builds the first page of the questionnaire.
     *
     * <p>Demonstrates using UIWindow.builder() to create a form with:
     * <ul>
     *   <li>Header text with page indicator</li>
     *   <li>Dropdown selection for multiple choice</li>
     *   <li>Navigation button (Next only, since this is page 1)</li>
     *   <li>Answer handler that saves the selection and navigates</li>
     * </ul></p>
     *
     * @return The built UIWindow for page 1
     */
    private UIWindow buildPage1() {
        return UIWindow.builder("Example Questionnaire - Page 1 of " + TOTAL_PAGES)
            .width(500)
            .height(350)
            .addHeader("Question 1: What is your favorite color?")
            .addText("")
            .addDropdown("choice", "Select an option:",
                "Example Option 1",
                "Example Option 2",
                "Example Option 3")
            .addText("")
            .addText(page1Selection != null ? "Current selection: " + page1Selection : "No selection yet")
            .addText("")
            .addSeparator()
            .addButton("next", "Next >>")
            .onAnswer((p, answers) -> handlePage1Answer(answers))
            .build();
    }

    /**
     * Builds the second page of the questionnaire.
     *
     * <p>Demonstrates:
     * <ul>
     *   <li>Different question with different options</li>
     *   <li>Both Previous and Submit buttons</li>
     *   <li>State preservation from previous page</li>
     * </ul></p>
     *
     * @return The built UIWindow for page 2
     */
    private UIWindow buildPage2() {
        return UIWindow.builder("Example Questionnaire - Page 2 of " + TOTAL_PAGES)
            .width(500)
            .height(400)
            .addHeader("Question 2: What is your skill level?")
            .addText("")
            .addDropdown("choice", "Select an option:",
                "Advanced Option A",
                "Advanced Option B",
                "Advanced Option C")
            .addText("")
            .addText(page2Selection != null ? "Current selection: " + page2Selection : "No selection yet")
            .addText("")
            .addSeparator()
            .addText("Previous answers:")
            .addText(page1Selection != null ? "  Question 1: " + page1Selection : "  Question 1: (not answered)")
            .addText("")
            .addButton("previous", "<< Previous")
            .addButton("submit", "Submit")
            .onAnswer((p, answers) -> handlePage2Answer(answers))
            .build();
    }

    /**
     * Handles answers from page 1.
     *
     * <p>This method demonstrates:
     * <ul>
     *   <li>Extracting form data from the Properties object</li>
     *   <li>Saving state for later</li>
     *   <li>Navigating to the next page</li>
     *   <li>Reopening the window (required to show updated content)</li>
     * </ul></p>
     *
     * @param answers The form data submitted by the player
     */
    private void handlePage1Answer(Properties answers) {
        // Check which button was clicked
        String nextButton = answers.getProperty("next");

        if (nextButton != null) {
            // Save the selection from the dropdown
            String choice = answers.getProperty("choice");
            if (choice != null && !choice.isEmpty()) {
                page1Selection = choice;
                logger.info("Page 1 selection saved: " + choice);
            }

            // Navigate to next page
            currentPage = 1;
            showCurrentPage();
        }
    }

    /**
     * Handles answers from page 2.
     *
     * <p>This method demonstrates:
     * <ul>
     *   <li>Handling multiple button types (Previous vs Submit)</li>
     *   <li>Backward navigation</li>
     *   <li>Final submission logic</li>
     * </ul></p>
     *
     * @param answers The form data submitted by the player
     */
    private void handlePage2Answer(Properties answers) {
        // Check which button was clicked
        String previousButton = answers.getProperty("previous");
        String submitButton = answers.getProperty("submit");

        if (previousButton != null) {
            // Save current selection before going back
            String choice = answers.getProperty("choice");
            if (choice != null && !choice.isEmpty()) {
                page2Selection = choice;
            }

            // Navigate to previous page
            currentPage = 0;
            showCurrentPage();

        } else if (submitButton != null) {
            // Save final selection
            String choice = answers.getProperty("choice");
            if (choice != null && !choice.isEmpty()) {
                page2Selection = choice;
            }

            // Process final submission
            handleFinalSubmit();
        }
    }

    /**
     * Handles the final submission of the questionnaire.
     *
     * <p>This sends a personalized thank you message to the player
     * showing all their selections.</p>
     */
    private void handleFinalSubmit() {
        String playerName = player.getName();

        // Build response message
        StringBuilder response = new StringBuilder();
        response.append("Thank you for completing the questionnaire, ").append(playerName).append("!\n\n");
        response.append("Your responses:\n");

        if (page1Selection != null) {
            response.append("  Question 1: ").append(page1Selection).append("\n");
        } else {
            response.append("  Question 1: (not answered)\n");
        }

        if (page2Selection != null) {
            response.append("  Question 2: ").append(page2Selection).append("\n");
        } else {
            response.append("  Question 2: (not answered)\n");
        }

        // Send as event message (white text in events tab)
        player.getCommunicator().sendSafeServerMessage(response.toString());

        logger.info("Questionnaire completed by " + playerName +
                   " - Page1: " + page1Selection + ", Page2: " + page2Selection);
    }
}
