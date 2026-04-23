package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.CancellableEvent;

import com.wurmonline.server.items.Item;

/**
 * Fired at the entry of {@code Item.testInsertHollowItem(Item, boolean)}, the
 * private check that gates placing items into containers/hollow items. Lets
 * container-capacity, bag-filter, and storage mods veto insertion.
 *
 * <p>Cancellation returns {@code false} from {@code testInsertHollowItem}
 * (insertion rejected).</p>
 */
public class ContainerInsertionCheckEvent extends CancellableEvent {

    private final Item container;
    private final Item incoming;
    private final boolean testItemCount;

    public ContainerInsertionCheckEvent(Item container, Item incoming, boolean testItemCount) {
        this.container = container;
        this.incoming = incoming;
        this.testItemCount = testItemCount;
    }

    public Item getContainer()      { return container; }
    public Item getIncoming()       { return incoming; }
    public boolean isTestItemCount(){ return testItemCount; }
}
