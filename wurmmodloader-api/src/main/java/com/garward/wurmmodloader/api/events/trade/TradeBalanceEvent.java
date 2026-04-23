package com.garward.wurmmodloader.api.events.trade;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.creatures.TradeHandler;

/**
 * Fired at the entry of {@link TradeHandler#balance()}. Cancelling skips the
 * default vanilla balance pass, letting merchant mods (BuyerMerchant, Crafter,
 * CustomTrader, Banker, DeliveryContracts, ToolPurchaser, ...) replace the
 * pricing logic entirely.
 *
 * <p>Listeners that only want to adjust pricing after vanilla runs should
 * subscribe at {@link com.garward.wurmmodloader.api.events.base.EventPriority#MONITOR}
 * and read/modify state via reflection on the handler.</p>
 */
public class TradeBalanceEvent extends CancellableEvent {

    private final TradeHandler tradeHandler;

    public TradeBalanceEvent(TradeHandler tradeHandler) {
        this.tradeHandler = tradeHandler;
    }

    public TradeHandler getTradeHandler() {
        return tradeHandler;
    }
}
