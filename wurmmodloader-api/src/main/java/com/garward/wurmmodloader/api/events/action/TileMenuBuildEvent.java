package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import java.util.List;

/**
 * Fired from {@code BehaviourDispatcher.requestActionForTiles} after the
 * vanilla {@code RequestParam} has been built but before it returns. Listeners
 * see the target tile coords + source tool and can inject/remove
 * {@link ActionEntry} entries in-place into the live list.
 *
 * <p>This is earlier than {@link ActionMenuBuildEvent} (which fires at the
 * Communicator sink) and preserves target context, making it the right place
 * for area-action menu injection driven by tile type or source tool.</p>
 */
public class TileMenuBuildEvent extends Event {

    private final Creature performer;
    private final long target;
    private final boolean onSurface;
    private final Item source;
    private final List<ActionEntry> availableActions;
    private String helpString;

    public TileMenuBuildEvent(Creature performer, long target, boolean onSurface,
                              Item source, List<ActionEntry> availableActions,
                              String helpString) {
        this.performer = performer;
        this.target = target;
        this.onSurface = onSurface;
        this.source = source;
        this.availableActions = availableActions;
        this.helpString = helpString;
    }

    public Creature getPerformer()               { return performer; }
    public long getTarget()                      { return target; }
    public boolean isOnSurface()                 { return onSurface; }
    public Item getSource()                      { return source; }
    public List<ActionEntry> getAvailableActions() { return availableActions; }
    public String getHelpString()                { return helpString; }
    public void setHelpString(String helpString) { this.helpString = helpString; }
}
