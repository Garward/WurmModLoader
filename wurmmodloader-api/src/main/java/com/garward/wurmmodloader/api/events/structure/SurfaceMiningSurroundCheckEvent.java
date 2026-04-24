package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired from inside {@code TileRockBehaviour.mine} when the per-swing
 * "surrounding area must be rock or cliff" check would abort the action —
 * vanilla bails with the "The surrounding area needs to be rock before you
 * mine." message + return.
 *
 * <p>Listeners may set {@link #setOverride(Boolean)} to {@code Boolean.TRUE}
 * to bypass the requirement (mining proceeds, no message). Anything else
 * (null / FALSE) preserves vanilla behavior — the server sends the message
 * and aborts the action.</p>
 *
 * <p>This is the central hook the legacy SurfaceMiningFix family of mods
 * reaches for via the {@code noNeedToUnconverRock} option.</p>
 */
public class SurfaceMiningSurroundCheckEvent extends Event {

    private final Creature performer;
    private final Item source;
    private Boolean override;

    public SurfaceMiningSurroundCheckEvent(Creature performer, Item source) {
        this.performer = performer;
        this.source = source;
    }

    public Creature getPerformer() { return performer; }
    public Item getSource()        { return source; }

    public Boolean getOverride() { return override; }
    public void setOverride(Boolean override) { this.override = override; }
}
