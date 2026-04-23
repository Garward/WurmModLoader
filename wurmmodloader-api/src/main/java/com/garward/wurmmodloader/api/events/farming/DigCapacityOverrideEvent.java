package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired from inside {@code Terraforming.dig} and {@code Flattening.getDirt}
 * whenever vanilla is about to consult one of its hardcoded capacity gates
 * before accepting a freshly-created dirt/sand item. Listeners may override
 * the returned value to raise or bypass pile/carry/volume caps — the pattern
 * used by BetterDig / DigInCart / MineInCart.
 *
 * <p>Three gate kinds are supported:</p>
 * <ul>
 *   <li>{@link Kind#NUM_ITEMS_NOT_COINS} — {@code Item.getNumItemsNotCoins()} on the target pile</li>
 *   <li>{@link Kind#CAN_CARRY} — {@code Creature.canCarry(weight)} on the performer</li>
 *   <li>{@link Kind#FREE_VOLUME} — {@code Item.getFreeVolume()} on the target container</li>
 * </ul>
 *
 * <p>{@link #getVanillaValue()} holds the raw vanilla result (1 for {@code true},
 * 0 for {@code false} on boolean gates). {@link #getOverrideValue()} starts
 * equal and can be mutated by listeners.</p>
 */
public class DigCapacityOverrideEvent extends Event {

    public enum Kind { NUM_ITEMS_NOT_COINS, CAN_CARRY, FREE_VOLUME }

    private final Creature performer;
    private final Item tool;
    private final Item target;
    private final Kind kind;
    private final long vanillaValue;
    private final boolean toPile;
    private final boolean dredging;

    private long overrideValue;

    public DigCapacityOverrideEvent(Creature performer, Item tool, Item target, Kind kind,
                                    long vanillaValue, boolean toPile, boolean dredging) {
        this.performer = performer;
        this.tool = tool;
        this.target = target;
        this.kind = kind;
        this.vanillaValue = vanillaValue;
        this.toPile = toPile;
        this.dredging = dredging;
        this.overrideValue = vanillaValue;
    }

    public Creature getPerformer() { return performer; }
    public Item getTool()          { return tool; }
    public Item getTarget()        { return target; }
    public Kind getKind()          { return kind; }
    public long getVanillaValue()  { return vanillaValue; }
    public boolean isToPile()      { return toPile; }
    public boolean isDredging()    { return dredging; }

    public long getOverrideValue()              { return overrideValue; }
    public void setOverrideValue(long value)    { this.overrideValue = value; }
}
