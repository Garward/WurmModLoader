package com.garward.wurmmodloader.api.events.player;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.List;

/**
 * Fired when the player's body context menu is being populated.
 *
 * <p>This event allows mods to add custom menu entries to the body right-click menu,
 * enabling "mod menus" and character management interfaces.</p>
 *
 * <p><strong>Use Cases:</strong></p>
 * <ul>
 *   <li>Power scaling / RPG stats viewer</li>
 *   <li>Skill tree / upgrade menus</li>
 *   <li>Achievement trackers</li>
 *   <li>Quest logs</li>
 *   <li>Custom character management UIs</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>
 * {@literal @}SubscribeEvent
 * public void onBodyMenuPopulate(BodyMenuPopulateEvent event) {
 *     // Add single menu item
 *     event.addMenuItem(new ActionEntry(
 *         myActionId,
 *         "View Power Scaling",
 *         "viewing stats",
 *         new int[0]
 *     ));
 *
 *     // Add submenu with multiple items
 *     List&lt;ActionEntry&gt; submenuItems = new ArrayList&lt;&gt;();
 *     submenuItems.add(new ActionEntry(actionId1, "Stats", "viewing"));
 *     submenuItems.add(new ActionEntry(actionId2, "Upgrades", "viewing"));
 *     event.addSubmenu("Power Fantasy", submenuItems);
 * }
 * </pre>
 *
 * <p><strong>Hook Location:</strong> {@code BodyPartBehaviour.getBehavioursFor()}
 * immediately before returning the menu list.</p>
 *
 * @author Power Fantasy RPG Team
 * @since 1.0.0
 */
public class BodyMenuPopulateEvent extends Event {

    private final Creature performer;
    private final Item bodyPart;
    private final List<ActionEntry> menuEntries;

    /**
     * Create a new body menu populate event.
     *
     * @param performer The player opening the body menu
     * @param bodyPart The body item being right-clicked
     * @param menuEntries The current menu entries (modifiable)
     */
    public BodyMenuPopulateEvent(Creature performer, Item bodyPart, List<ActionEntry> menuEntries) {
        this.performer = performer;
        this.bodyPart = bodyPart;
        this.menuEntries = menuEntries;
    }

    /**
     * Get the player opening the body menu.
     *
     * @return The performer
     */
    public Creature getPerformer() {
        return performer;
    }

    /**
     * Get the body item being right-clicked.
     *
     * @return The body part item
     */
    public Item getBodyPart() {
        return bodyPart;
    }

    /**
     * Get the modifiable list of menu entries.
     *
     * <p>Mods can directly add ActionEntry objects to this list.</p>
     *
     * @return The menu entries list
     */
    public List<ActionEntry> getMenuEntries() {
        return menuEntries;
    }

    /**
     * Add a single menu item to the body menu.
     *
     * <p>This is a convenience method for adding a single ActionEntry.</p>
     *
     * @param entry The action entry to add
     */
    public void addMenuItem(ActionEntry entry) {
        menuEntries.add(entry);
    }

    /**
     * Add a submenu with multiple items.
     *
     * <p>This creates a submenu header with the specified name and adds all
     * submenu items beneath it.</p>
     *
     * <p><strong>Submenu Pattern:</strong> Wurm uses negative action IDs to indicate
     * submenus. The magnitude of the negative number indicates how many items
     * are in the submenu.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * ActionEntry header = new ActionEntry((short)(-3), "My Menu", "managing");
     * ActionEntry item1 = new ActionEntry(100, "Option 1", "selecting");
     * ActionEntry item2 = new ActionEntry(101, "Option 2", "selecting");
     * ActionEntry item3 = new ActionEntry(102, "Option 3", "selecting");
     * </pre>
     *
     * @param submenuName The name of the submenu header
     * @param items The items to add under the submenu
     */
    public void addSubmenu(String submenuName, List<ActionEntry> items) {
        if (items == null || items.isEmpty()) {
            return; // Don't add empty submenus
        }

        // Create submenu header with negative size
        ActionEntry submenuHeader = new ActionEntry(
            (short)(-items.size()),
            submenuName,
            submenuName.toLowerCase()
        );

        // Add header first
        menuEntries.add(submenuHeader);

        // Add all submenu items
        menuEntries.addAll(items);
    }

    /**
     * Check if the performer is the owner of the body.
     *
     * <p>Usually true (players right-click their own body), but could be false
     * if examining another player's corpse or removed body part.</p>
     *
     * @return true if performer owns this body
     */
    public boolean isOwnBody() {
        return performer.getWurmId() == bodyPart.getOwnerId();
    }

    /**
     * Check if this is the attached body (vs a removed body part).
     *
     * <p>Most mods should only show menus for the attached body.</p>
     *
     * @return true if this is the attached body
     */
    public boolean isBodyAttached() {
        return bodyPart.isBodyPartAttached();
    }
}
