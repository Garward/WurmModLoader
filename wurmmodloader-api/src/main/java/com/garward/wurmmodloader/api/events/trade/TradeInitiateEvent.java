package com.garward.wurmmodloader.api.events.trade;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired when an NPC-initiated trade session is about to open. Cancelling prevents
 * the trade window from being established.
 *
 * <p>For pre-merchant permission checks (e.g. per-player gating, funds checks),
 * prefer {@link NpcTradePermissionCheckEvent} which fires immediately after this
 * one and carries merchant-specific context.</p>
 */
public class TradeInitiateEvent extends CancellableEvent {

    private final Creature npc;
    private final Creature player;

    public TradeInitiateEvent(Creature npc, Creature player) {
        this.npc = npc;
        this.player = player;
    }

    /** The NPC side of the trade (merchant, trader, banker, ...). */
    public Creature getNpc() {
        return npc;
    }

    /** The player who opened the trade. May be null if not resolvable. */
    public Creature getPlayer() {
        return player;
    }
}
