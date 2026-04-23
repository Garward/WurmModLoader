package com.garward.wurmmodloader.api.events.creature;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.CreatureTemplate;

/**
 * Fired at the tail of {@code CreatureTemplate.getColorRed/Green/Blue()}. Lets
 * appearance mods (PhobiaMod and friends) override per-channel creature colors
 * without patching every template individually.
 *
 * <p>Use {@link #getChannel()} to discriminate between RED/GREEN/BLUE. Override
 * with {@link #setValue(int)}. Not cancellable.</p>
 */
public class CreatureTemplateColorEvent extends Event {

    public enum Channel { RED, GREEN, BLUE }

    private final CreatureTemplate template;
    private final Channel channel;
    private int value;

    public CreatureTemplateColorEvent(CreatureTemplate template, Channel channel, int value) {
        this.template = template;
        this.channel = channel;
        this.value = value;
    }

    public CreatureTemplate getTemplate() { return template; }
    public Channel getChannel()           { return channel; }
    public int getValue()                 { return value; }
    public void setValue(int v)           { this.value = v; }
}
