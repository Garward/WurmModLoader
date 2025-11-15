package com.garward.wurmmodloader.api.ui;

import java.util.Properties;

/**
 * Represents a UI window that can be displayed to a player.
 *
 * <p>This is a high-level abstraction over Wurm's Question system that makes it easy
 * to create and display BML windows without writing raw BML strings or implementing
 * ModQuestion interfaces.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>
 * UIWindow window = UIWindow.builder("Power Stats")
 *     .width(400).height(300)
 *     .addText("Total Power: " + power, true) // bold
 *     .addText("Damage: " + damageMultiplier + "x")
 *     .onAnswer((player, answers) -> {
 *         // Handle button clicks
 *     })
 *     .build();
 *
 * WindowManager.open(player, window);
 * </pre>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public interface UIWindow {

    /**
     * Gets the title of this window.
     * @return The window title
     */
    String getTitle();

    /**
     * Gets the width of this window.
     * @return The window width in pixels
     */
    int getWidth();

    /**
     * Gets the height of this window.
     * @return The window height in pixels
     */
    int getHeight();

    /**
     * Gets the BML content for this window.
     *
     * @param player The player viewing the window (for dynamic content)
     * @return The BML string to display
     */
    String getBML(Object player);

    /**
     * Handles answer callback when player submits the window.
     *
     * @param player The player who submitted
     * @param answers The form answers
     */
    void onAnswer(Object player, Properties answers);

    /**
     * Creates a new window builder.
     *
     * @param title The window title
     * @return A new builder instance
     */
    static UIWindowBuilder builder(String title) {
        return new UIWindowBuilder(title);
    }
}
