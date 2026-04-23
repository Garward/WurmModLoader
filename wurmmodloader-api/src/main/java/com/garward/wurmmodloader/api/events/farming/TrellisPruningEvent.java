package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code TrellisBehaviour.prune(...)}. Lets farming mods
 * veto or observe trellis pruning before vanilla runs.
 *
 * <p>Cancellation returns {@code false} from {@code prune} (action aborts).</p>
 */
public class TrellisPruningEvent extends CancellableEvent {

    private final Action action;
    private final Creature performer;
    private final Item sickle;
    private final Item trellis;
    private final float counter;

    public TrellisPruningEvent(Action action, Creature performer, Item sickle,
                               Item trellis, float counter) {
        this.action = action;
        this.performer = performer;
        this.sickle = sickle;
        this.trellis = trellis;
        this.counter = counter;
    }

    public Action getAction()      { return action; }
    public Creature getPerformer() { return performer; }
    public Item getSickle()        { return sickle; }
    public Item getTrellis()       { return trellis; }
    public float getCounter()      { return counter; }
}
