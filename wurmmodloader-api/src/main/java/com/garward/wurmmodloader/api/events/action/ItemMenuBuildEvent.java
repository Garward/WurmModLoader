package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.List;

/**
 * Fired from {@code BehaviourDispatcher.requestActionForItemsBodyIdsCoinIds}
 * after the vanilla {@code RequestParam} has been built but before it returns.
 * Listeners see the target item-id + source tool and can mutate the live
 * action list in place.
 *
 * <p>This is earlier than {@link ActionMenuBuildEvent} and preserves target
 * context — use it for item-targeted area-action injection.</p>
 *
 * <p>The target item may or may not resolve via {@code Items.getItem(targetId)};
 * listeners must handle {@code NoSuchItemException}.</p>
 */
public class ItemMenuBuildEvent extends Event {

    private final Creature performer;
    private final long targetId;
    private final Item source;
    private final List<ActionEntry> availableActions;
    private String helpString;

    public ItemMenuBuildEvent(Creature performer, long targetId, Item source,
                              List<ActionEntry> availableActions, String helpString) {
        this.performer = performer;
        this.targetId = targetId;
        this.source = source;
        this.availableActions = availableActions;
        this.helpString = helpString;
    }

    public Creature getPerformer()               { return performer; }
    public long getTargetId()                    { return targetId; }
    public Item getSource()                      { return source; }
    public List<ActionEntry> getAvailableActions() { return availableActions; }
    public String getHelpString()                { return helpString; }
    public void setHelpString(String helpString) { this.helpString = helpString; }
}
