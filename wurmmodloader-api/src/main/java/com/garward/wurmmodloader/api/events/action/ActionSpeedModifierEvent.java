package com.garward.wurmmodloader.api.events.action;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired when stamina-based speed modifiers are applied to an action.
 * Allows mods to multiply or override the stamina modifier result.
 */
public class ActionSpeedModifierEvent extends Event {

    private final Creature performer;
    private final Item source;
    private final float baseModifier;
    private float modifier;

    public ActionSpeedModifierEvent(Creature performer, Item source, float baseModifier) {
        this.performer = performer;
        this.source = source;
        this.baseModifier = baseModifier;
        this.modifier = baseModifier;
    }

    public Creature getPerformer() {
        return performer;
    }

    public Item getSource() {
        return source;
    }

    public float getBaseModifier() {
        return baseModifier;
    }

    public float getModifier() {
        return modifier;
    }

    public void setModifier(float modifier) {
        this.modifier = modifier;
    }
}
