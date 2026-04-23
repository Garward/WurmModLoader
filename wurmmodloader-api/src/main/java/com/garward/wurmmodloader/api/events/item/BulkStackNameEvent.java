package com.garward.wurmmodloader.api.events.item;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.items.Item;

/**
 * Fired from {@code Item.AddBulkItem} and {@code Item.AddBulkItemToCrate} at
 * the {@code getActualName()} call site used to decide whether items stack
 * under the same bulk row. Listeners can canonicalize the name to unify
 * rows that vanilla would otherwise keep separate.
 *
 * <p>Typical use: strip the {@code "pile of "} prefix so a dug "pile of dirt"
 * stacks with a modded "dirt" of the same template id into the same bulk row,
 * instead of creating a second row per pile.</p>
 */
public class BulkStackNameEvent extends Event {

    private final Item item;
    private final String vanillaName;

    private String resolvedName;

    public BulkStackNameEvent(Item item, String vanillaName) {
        this.item = item;
        this.vanillaName = vanillaName;
        this.resolvedName = vanillaName;
    }

    public Item getItem()                 { return item; }
    public String getVanillaName()        { return vanillaName; }

    public String getResolvedName()       { return resolvedName; }
    public void setResolvedName(String n) { this.resolvedName = n; }
}
