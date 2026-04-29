package com.garward.wurmmodloader.api.events.trade;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.items.Trade;

/**
 * Fired at the entry of {@link Trade#addShopDiff(long)} before the vanilla
 * {@code shopDiff += money} accumulation. Lets economy mods drain or
 * inflate the currency that flows through NPC traders without re-patching
 * the call site.
 *
 * <p>{@link #getMoney()} starts at the original argument; mutate via
 * {@link #setMoney(long)} to change what vanilla will add. {@link
 * #getCurrentShopDiff()} is a read-only snapshot of {@code Trade.shopDiff}
 * before this call.</p>
 *
 * <p>Example — drain 80% of trader income (Wyvern voidTraderMoney):
 * <pre>{@code
 * @SubscribeEvent
 * public void onShopDiff(ShopDiffEvent event) {
 *     if (event.getMoney() > 0
 *             && event.getMoney() + event.getCurrentShopDiff() > 0) {
 *         long newDiff = (event.getMoney() + event.getCurrentShopDiff()) / 5;
 *         event.setMoney(-event.getCurrentShopDiff() + newDiff);
 *     }
 * }
 * }</pre>
 */
public class ShopDiffEvent extends Event {

    private final Trade trade;
    private final long currentShopDiff;
    private long money;

    public ShopDiffEvent(Trade trade, long money, long currentShopDiff) {
        this.trade = trade;
        this.money = money;
        this.currentShopDiff = currentShopDiff;
    }

    public Trade getTrade() {
        return trade;
    }

    /**
     * The vanilla {@code Trade.shopDiff} value as it stood before this
     * {@code addShopDiff} call. Read-only snapshot.
     */
    public long getCurrentShopDiff() {
        return currentShopDiff;
    }

    /**
     * The amount about to be added to {@code shopDiff}. Starts at the
     * original argument; mutate via {@link #setMoney(long)}.
     */
    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }
}
