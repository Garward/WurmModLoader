package com.garward.wurmmodloader.api.ui;

import java.util.function.Consumer;

/**
 * Represents a context menu entry that can be displayed in body menus.
 *
 * <p>Menu entries can have visibility filters and click handlers, making it easy
 * to create dynamic context menus without manually managing action IDs.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>
 * MenuEntry entry = MenuEntry.builder("View Power Stats")
 *     .onlyFor(player -> hasPermission(player, "powerscaling.viewstats"))
 *     .onClick(player -> {
 *         // Open stats window
 *         UIWindow window = createStatsWindow(player);
 *         WindowManager.open(player, window);
 *     })
 *     .build();
 *
 * ContextMenuRegistry.register("PowerScaling", MenuTarget.BODY, entry);
 * </pre>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public interface MenuEntry {

    /**
     * Gets the label displayed in the menu.
     * @return The menu label
     */
    String getLabel();

    /**
     * Gets the action verb (e.g., "viewing", "opening").
     * @return The action verb
     */
    String getActionVerb();

    /**
     * Checks if this menu entry should be visible for the given player.
     *
     * @param player The player viewing the menu
     * @return true if visible, false otherwise
     */
    boolean isVisibleFor(Object player);

    /**
     * Handles the action when this menu entry is clicked.
     *
     * @param player The player who clicked
     */
    void onClick(Object player);

    /**
     * Gets the internal action ID for this menu entry.
     * @return The action ID
     */
    int getActionId();

    /**
     * Sets the internal action ID (called by framework).
     * @param actionId The action ID
     */
    void setActionId(int actionId);

    /**
     * Gets the mod name that registered this entry.
     * @return The mod name
     */
    String getModName();

    /**
     * Sets the mod name (called by framework).
     * @param modName The mod name
     */
    void setModName(String modName);

    /**
     * Creates a new menu entry builder.
     *
     * @param label The menu label
     * @return A new builder instance
     */
    static MenuEntryBuilder builder(String label) {
        return new MenuEntryBuilder(label);
    }

    /**
     * Builder for constructing MenuEntry instances.
     */
    class MenuEntryBuilder {
        private final String label;
        private String actionVerb = "viewing";
        private VisibilityPredicate visibilityPredicate = player -> true;
        private Consumer<Object> clickHandler;

        MenuEntryBuilder(String label) {
            this.label = label;
        }

        /**
         * Sets the action verb (e.g., "opening", "checking").
         * @param verb The action verb
         * @return This builder
         */
        public MenuEntryBuilder actionVerb(String verb) {
            this.actionVerb = verb;
            return this;
        }

        /**
         * Sets a visibility filter.
         *
         * @param predicate The predicate to check
         * @return This builder
         */
        public MenuEntryBuilder onlyFor(VisibilityPredicate predicate) {
            this.visibilityPredicate = predicate;
            return this;
        }

        /**
         * Only visible to player's own body.
         *
         * @return This builder
         */
        public MenuEntryBuilder onlyOwnBody() {
            return onlyFor(player -> true); // Already filtered by framework
        }

        /**
         * Sets the click handler.
         *
         * @param handler The handler to invoke
         * @return This builder
         */
        public MenuEntryBuilder onClick(Consumer<Object> handler) {
            this.clickHandler = handler;
            return this;
        }

        /**
         * Opens a window when clicked.
         *
         * @param window The window to open
         * @return This builder
         */
        public MenuEntryBuilder opensWindow(UIWindow window) {
            return onClick(player -> {
                try {
                    Class<?> wmClass = Class.forName("com.garward.wurmmodloader.core.ui.WindowManager");
                    java.lang.reflect.Method openMethod = wmClass.getMethod("open", Object.class, UIWindow.class);
                    openMethod.invoke(null, player, window);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to open window", e);
                }
            });
        }

        /**
         * Builds the MenuEntry.
         *
         * @return The constructed menu entry
         */
        public MenuEntry build() {
            return new SimpleMenuEntry(label, actionVerb, visibilityPredicate, clickHandler);
        }
    }

    /**
     * Simple implementation of MenuEntry.
     */
    class SimpleMenuEntry implements MenuEntry {
        private final String label;
        private final String actionVerb;
        private final VisibilityPredicate visibilityPredicate;
        private final Consumer<Object> clickHandler;
        private int actionId = -1;
        private String modName;

        SimpleMenuEntry(String label, String actionVerb, VisibilityPredicate visibilityPredicate,
                       Consumer<Object> clickHandler) {
            this.label = label;
            this.actionVerb = actionVerb;
            this.visibilityPredicate = visibilityPredicate;
            this.clickHandler = clickHandler;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getActionVerb() {
            return actionVerb;
        }

        @Override
        public boolean isVisibleFor(Object player) {
            return visibilityPredicate.test(player);
        }

        @Override
        public void onClick(Object player) {
            if (clickHandler != null) {
                clickHandler.accept(player);
            }
        }

        @Override
        public int getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(int actionId) {
            this.actionId = actionId;
        }

        @Override
        public String getModName() {
            return modName;
        }

        @Override
        public void setModName(String modName) {
            this.modName = modName;
        }
    }
}
