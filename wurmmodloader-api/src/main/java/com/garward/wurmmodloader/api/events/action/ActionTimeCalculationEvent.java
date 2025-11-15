package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired whenever an action timer is calculated (standard, quick, slow, etc.).
 * Mods can scale the resulting action length before it is returned.
 */
public class ActionTimeCalculationEvent extends Event {

    private final Creature performer;
    private final Item source;
    private final Item target;
    private final float baseTime;
    private float time;

    public ActionTimeCalculationEvent(Creature performer, Item source, Item target, float baseTime) {
        this.performer = performer;
        this.source = source;
        this.target = target;
        this.baseTime = baseTime;
        this.time = baseTime;
    }

    public Creature getPerformer() {
        return performer;
    }

    public Item getSource() {
        return source;
    }

    public Item getTarget() {
        return target;
    }

    public float getBaseTime() {
        return baseTime;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }
}
