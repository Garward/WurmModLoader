package com.garward.wurmmodloader.core.ui;

import com.garward.wurmmodloader.api.events.base.SubscribeEvent;
import com.garward.wurmmodloader.api.events.player.BodyMenuPopulateEvent;
import com.garward.wurmmodloader.api.ui.MenuEntry;
import com.garward.wurmmodloader.api.ui.MenuTarget;
import com.garward.wurmmodloader.core.event.EventBus;
import com.garward.wurmmodloader.modsupport.actions.ActionEntryBuilder;
import com.garward.wurmmodloader.modsupport.actions.ActionPerformer;
import com.garward.wurmmodloader.modsupport.actions.ActionPropagation;
import com.garward.wurmmodloader.modsupport.actions.ModActions;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for context menu entries.
 *
 * <p>This class manages registration and display of custom context menu entries
 * across different menu targets (body, item, creature, tile). It automatically
 * handles action ID allocation, action registration, and visibility filtering.</p>
 *
 * <p><strong>Submenu Organization:</strong> All mod entries are automatically
 * organized to keep the context menu clean. Each mod gets its own top-level submenu.
 * For example, when you right-click your body, you'll see:</p>
 * <pre>
 * Body Menu:
 *   Emotes &gt;
 *   Examine
 *   ...
 *   PowerScaling &gt;
 *     View Stats
 *     Configure Settings
 *   TemplateMod &gt;
 *     Simple Example
 *     Questionnaire
 *   OtherMod &gt;
 *     ...
 * </pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can be accessed
 * from multiple mods concurrently.</p>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>
 * // In mod's onServerStarted():
 * ContextMenuRegistry registry = ContextMenuRegistry.getInstance();
 *
 * MenuEntry entry = MenuEntry.builder("View Power Stats")
 *     .actionVerb("viewing")
 *     .onClick(player -> {
 *         // Show stats window
 *     })
 *     .build();
 *
 * registry.register("PowerScaling", MenuTarget.BODY, entry);
 * // This will appear under: PowerScaling &gt; View Power Stats
 * </pre>
 *
 * @author WurmModLoader Framework
 * @since 1.0.0
 */
public class ContextMenuRegistry {

    private static final Logger logger = Logger.getLogger(ContextMenuRegistry.class.getName());
    private static volatile ContextMenuRegistry instance;

    private final Map<MenuTarget, List<MenuEntry>> menuEntries = new ConcurrentHashMap<>();
    private final Map<Integer, MenuEntry> actionIdMap = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    private ContextMenuRegistry() {
        // Private constructor - use getInstance()
    }

    /**
     * Gets the singleton instance.
     *
     * @return The registry instance
     */
    public static ContextMenuRegistry getInstance() {
        if (instance == null) {
            synchronized (ContextMenuRegistry.class) {
                if (instance == null) {
                    instance = new ContextMenuRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the registry and registers event listeners.
     * This should be called once during server startup.
     */
    public synchronized void initialize() {
        if (initialized) {
            logger.warning("ContextMenuRegistry already initialized");
            return;
        }

        logger.info("Initializing ContextMenuRegistry...");

        // Initialize ModActions system
        ModActions.init();

        // Register event listener for body menus
        EventBus.getInstance().register(this);

        initialized = true;
        logger.info("ContextMenuRegistry initialized");
    }

    /**
     * Registers a menu entry.
     *
     * @param modName The name of the mod registering the entry (used for submenu grouping)
     * @param target The menu target (BODY, ITEM, etc.)
     * @param entry The menu entry to register
     */
    public void register(String modName, MenuTarget target, MenuEntry entry) {
        if (!initialized) {
            logger.warning("ContextMenuRegistry not initialized - call initialize() first!");
        }

        // Set mod name for submenu grouping
        entry.setModName(modName);

        // Allocate action ID
        int actionId = ModActions.getNextActionId();
        entry.setActionId(actionId);

        // Register action with ModActions
        ModActions.registerAction(new ActionEntryBuilder(
            (short) actionId,
            entry.getLabel(),
            entry.getActionVerb()
        ).build());

        // Register action performer
        ModActions.registerActionPerformer(new MenuActionPerformer(entry));

        // Add to registry
        menuEntries.computeIfAbsent(target, k -> new ArrayList<>()).add(entry);
        actionIdMap.put(actionId, entry);

        logger.info(String.format("[%s] Registered menu entry '%s' (ID: %d) for target %s",
            modName, entry.getLabel(), actionId, target));
    }

    /**
     * Event handler for body menu population.
     * Creates a top-level submenu for each mod: [ModName] > [Entry1, Entry2, ...]
     */
    @SubscribeEvent
    public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
        List<MenuEntry> bodyEntries = menuEntries.get(MenuTarget.BODY);
        if (bodyEntries == null || bodyEntries.isEmpty()) {
            return;
        }

        // Only add to player's own body
        if (!event.getBodyPart().isBodyPartAttached()) {
            return;
        }

        if (event.getBodyPart().getOwnerId() != event.getPerformer().getWurmId()) {
            return;
        }

        // Group entries by mod name
        Map<String, List<MenuEntry>> entriesByMod = new HashMap<>();
        for (MenuEntry entry : bodyEntries) {
            if (entry.isVisibleFor(event.getPerformer())) {
                String modName = entry.getModName() != null ? entry.getModName() : "Unknown";
                entriesByMod.computeIfAbsent(modName, k -> new ArrayList<>()).add(entry);
            }
        }

        // If no visible entries, don't add anything
        if (entriesByMod.isEmpty()) {
            return;
        }

        // Add each mod as its own top-level submenu
        // This avoids conflicts with vanilla submenus and keeps things simple
        for (Map.Entry<String, List<MenuEntry>> modEntries : entriesByMod.entrySet()) {
            String modName = modEntries.getKey();
            List<MenuEntry> entries = modEntries.getValue();

            // Convert menu entries to action entries for this mod
            List<com.wurmonline.server.behaviours.ActionEntry> actionEntries = new ArrayList<>();
            for (MenuEntry entry : entries) {
                actionEntries.add(ModActions.getAction(entry.getActionId()));
            }

            // Add this mod's submenu directly to the root menu
            // Each mod gets: [ModName] > [Entry1, Entry2, ...]
            event.addSubmenu(modName, actionEntries);
        }
    }

    /**
     * Gets all registered menu entries for a target.
     *
     * @param target The menu target
     * @return List of menu entries (unmodifiable)
     */
    public List<MenuEntry> getEntries(MenuTarget target) {
        List<MenuEntry> entries = menuEntries.get(target);
        return entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
    }

    /**
     * Gets a menu entry by action ID.
     *
     * @param actionId The action ID
     * @return The menu entry, or null if not found
     */
    public MenuEntry getEntryByActionId(int actionId) {
        return actionIdMap.get(actionId);
    }

    /**
     * Action performer that delegates to MenuEntry onClick handlers.
     */
    private static class MenuActionPerformer implements ActionPerformer {

        private final MenuEntry entry;

        MenuActionPerformer(MenuEntry entry) {
            this.entry = entry;
        }

        @Override
        public short getActionId() {
            return (short) entry.getActionId();
        }

        @Override
        public boolean action(Action action, Creature performer, Item target, short num, float counter) {
            // Verify action ID
            if (num != getActionId()) {
                return propagate(action, ActionPropagation.SERVER_PROPAGATION,
                               ActionPropagation.ACTION_PERFORMER_PROPAGATION);
            }

            // Verify target is the player's body
            if (!target.isBodyPartAttached() || target.getOwnerId() != performer.getWurmId()) {
                performer.getCommunicator().sendNormalServerMessage(
                    "You can only perform this action on your own body.");
                return propagate(action, ActionPropagation.FINISH_ACTION,
                               ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            }

            // Check visibility
            if (!entry.isVisibleFor(performer)) {
                performer.getCommunicator().sendNormalServerMessage(
                    "You cannot perform this action.");
                return propagate(action, ActionPropagation.FINISH_ACTION,
                               ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
            }

            try {
                // Invoke click handler
                entry.onClick(performer);
            } catch (Exception e) {
                logger.severe("Error executing menu action '" + entry.getLabel() + "': " + e.getMessage());
                e.printStackTrace();
                performer.getCommunicator().sendAlertServerMessage(
                    "An error occurred while performing this action.");
            }

            return propagate(action, ActionPropagation.FINISH_ACTION,
                           ActionPropagation.NO_ACTION_PERFORMER_PROPAGATION);
        }
    }
}
