package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired from inside {@code Terraforming.dig} and {@code Flattening.getDirt}
 * every time vanilla is about to insert a freshly-created dirt/sand item into
 * its hardcoded destination (player inventory, dredging tool, or attached
 * vehicle). Listeners may replace the destination with any other
 * {@link Item} — a cart, a crate, a dragged vessel, a BSB, whatever.
 *
 * <p>This is the hook that lets mods like BetterDig / DigLikeMining port
 * with no bytecode of their own.</p>
 *
 * <p>Leaving {@link #getResolvedTarget()} as {@code vanillaTarget} keeps the
 * vanilla path. Setting it to {@code null} still causes the proxy to call
 * {@code vanillaTarget.insertItem(dirt, true)}; only setting it to a real
 * other container changes behavior.</p>
 */
public class DirtDestinationResolveEvent extends Event {

    public enum Context { TERRAFORMING_DIG, FLATTENING_GETDIRT }

    private final Item dirt;
    private final Creature performer;
    private final Item tool;
    private final Item vanillaTarget;
    private final boolean dredging;
    private final boolean toPile;
    private final Context context;

    private Item resolvedTarget;

    public DirtDestinationResolveEvent(Item dirt, Creature performer, Item tool,
                                       Item vanillaTarget, boolean dredging,
                                       boolean toPile, Context context) {
        this.dirt = dirt;
        this.performer = performer;
        this.tool = tool;
        this.vanillaTarget = vanillaTarget;
        this.dredging = dredging;
        this.toPile = toPile;
        this.context = context;
        this.resolvedTarget = vanillaTarget;
    }

    public Item getDirt()               { return dirt; }
    public Creature getPerformer()      { return performer; }
    public Item getTool()               { return tool; }
    public Item getVanillaTarget()      { return vanillaTarget; }
    public boolean isDredging()         { return dredging; }
    public boolean isToPile()           { return toPile; }
    public Context getContext()         { return context; }

    public Item getResolvedTarget()            { return resolvedTarget; }
    public void setResolvedTarget(Item target) { this.resolvedTarget = target; }
}
