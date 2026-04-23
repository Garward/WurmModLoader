package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired from {@code TileDirtBehaviour.action} at the point where vanilla is
 * about to call {@code source.destroyItem()} on the dirt/sand/peat pile used
 * for terraforming. Listeners can set {@link #setConsumed(boolean)} to
 * {@code true} to indicate "I've handled consumption my own way" — the proxy
 * will then skip the vanilla destroy and deduct one template weight from the
 * source item instead.
 *
 * <p>Leaving {@link #isConsumed()} {@code false} preserves vanilla (destroy
 * the pile entirely per action).</p>
 *
 * <p>Hook site: called once per tick where vanilla would have destroyed the
 * dirt pile. The source Item is the dirt pile itself.</p>
 */
public class TileDirtConsumeEvent extends Event {

    private final Action action;
    private final Creature performer;
    private final Item source;

    private boolean consumed;

    public TileDirtConsumeEvent(Action action, Creature performer, Item source) {
        this.action = action;
        this.performer = performer;
        this.source = source;
    }

    public Action getAction()             { return action; }
    public Creature getPerformer()        { return performer; }
    public Item getSource()               { return source; }

    public boolean isConsumed()           { return consumed; }
    public void setConsumed(boolean v)    { this.consumed = v; }
}
