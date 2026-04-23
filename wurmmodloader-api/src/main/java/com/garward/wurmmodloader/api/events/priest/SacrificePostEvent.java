package com.garward.wurmmodloader.api.events.priest;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired immediately after a successful {@code MethodsReligion.sacrifice(Action,
 * Creature, Item)} call (altar sacrifice). Provides a post-hook for mods that
 * need to observe or react to altar sacrifices without interfering with the
 * existing {@code SACRIFICE_*} pre-hook conflict keys.
 *
 * <p>Not cancellable — the sacrifice has already completed when this fires.</p>
 */
public class SacrificePostEvent extends Event {

    private final Action action;
    private final Creature performer;
    private final Item altar;
    private final boolean done;

    public SacrificePostEvent(Action action, Creature performer, Item altar, boolean done) {
        this.action = action;
        this.performer = performer;
        this.altar = altar;
        this.done = done;
    }

    public Action getAction()       { return action; }
    public Creature getPerformer()  { return performer; }
    public Item getAltar()          { return altar; }
    /** The boolean that vanilla {@code sacrifice} is about to return. */
    public boolean isDone()         { return done; }
}
