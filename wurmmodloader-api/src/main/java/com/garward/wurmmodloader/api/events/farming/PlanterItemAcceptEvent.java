package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired from {@code PlanterBehaviour.getBehavioursFor} and
 * {@code PlanterBehaviour.action} at the two gate checks — {@code Item.isRaw()}
 * (menu-build) and {@code Item.isSpice()} (action) — that decide whether the
 * herb item held by the player can be planted in a planter rack.
 *
 * <p>The event exposes the vanilla result and lets listeners flip it true so
 * custom "potable" items (mod-added seeds, fruit pits, etc.) register as
 * plantable without touching bytecode.</p>
 *
 * <p>{@link Kind#RAW} fires from the menu-build path; {@link Kind#SPICE}
 * fires from the action path. A listener that wants a new potable to work
 * end-to-end usually accepts both.</p>
 */
public class PlanterItemAcceptEvent extends Event {

    public enum Kind { RAW, SPICE }

    private final Creature performer;
    private final Item herb;
    private final Item planter;
    private final Kind kind;
    private final boolean vanillaAccepted;

    private boolean accepted;

    public PlanterItemAcceptEvent(Creature performer, Item herb, Item planter,
                                  Kind kind, boolean vanillaAccepted) {
        this.performer = performer;
        this.herb = herb;
        this.planter = planter;
        this.kind = kind;
        this.vanillaAccepted = vanillaAccepted;
        this.accepted = vanillaAccepted;
    }

    public Creature getPerformer()        { return performer; }
    public Item getHerb()                 { return herb; }
    public Item getPlanter()              { return planter; }
    public Kind getKind()                 { return kind; }
    public boolean isVanillaAccepted()    { return vanillaAccepted; }

    public boolean isAccepted()           { return accepted; }
    public void setAccepted(boolean v)    { this.accepted = v; }
}
