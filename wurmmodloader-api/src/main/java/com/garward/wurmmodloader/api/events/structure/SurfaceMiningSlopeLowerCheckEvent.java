package com.garward.wurmmodloader.api.events.structure;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired from inside {@code TileRockBehaviour.mine} at the per-tick "do we
 * actually lower the rock this swing or just chip away?" decision point —
 * vanilla rolls {@code Server.rand.nextFloat() < chance}.
 *
 * <p>Listeners may set {@link #setOverride(Boolean)} to a non-null value to
 * force the outcome regardless of the natural roll. This is the central hook
 * the legacy SurfaceMiningFix family of mods reaches for: Azbantium-Fist
 * enchant, Azbantium pickaxe, "always lower slope" config, etc.</p>
 *
 * <p>Override semantics:
 * <ul>
 *   <li>{@code null} (default) — vanilla roll runs unchanged</li>
 *   <li>{@code Boolean.TRUE} — slope lowers this tick, ignore the roll</li>
 *   <li>{@code Boolean.FALSE} — chip-away outcome, ignore the roll</li>
 * </ul>
 */
public class SurfaceMiningSlopeLowerCheckEvent extends Event {

    private final Creature performer;
    private final Item source;
    private final float naturalChance;
    private Boolean override;

    public SurfaceMiningSlopeLowerCheckEvent(Creature performer, Item source, float naturalChance) {
        this.performer = performer;
        this.source = source;
        this.naturalChance = naturalChance;
    }

    public Creature getPerformer() { return performer; }
    public Item getSource()        { return source; }
    /** The vanilla-computed [0..1] chance the slope lowers this tick. */
    public float getNaturalChance() { return naturalChance; }

    public Boolean getOverride() { return override; }
    public void setOverride(Boolean override) { this.override = override; }
}
