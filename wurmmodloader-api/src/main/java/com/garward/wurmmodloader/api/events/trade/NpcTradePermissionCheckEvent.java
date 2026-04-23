package com.garward.wurmmodloader.api.events.trade;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.Creature;

/**
 * Fired immediately after {@link TradeInitiateEvent} so merchant-style mods can
 * gate access per-player (account status, bans, allowlists, funds, ...).
 *
 * <p>Set a deny reason with {@link #setDenyReason(String)} before cancelling to
 * surface a user-facing message; the patch will deliver it as a server alert.</p>
 */
public class NpcTradePermissionCheckEvent extends CancellableEvent {

    private final Creature merchant;
    private final Creature player;
    private String denyReason;

    public NpcTradePermissionCheckEvent(Creature merchant, Creature player) {
        this.merchant = merchant;
        this.player = player;
    }

    public Creature getMerchant() {
        return merchant;
    }

    public Creature getPlayer() {
        return player;
    }

    public String getDenyReason() {
        return denyReason;
    }

    public void setDenyReason(String denyReason) {
        this.denyReason = denyReason;
    }
}
