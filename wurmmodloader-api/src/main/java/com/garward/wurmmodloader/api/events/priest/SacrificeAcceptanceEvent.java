package com.garward.wurmmodloader.api.events.priest;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when vanilla checks whether an item can be sacrificed on an altar — i.e.
 * {@code MethodsReligion.canBeSacrificed(Item)}. Vanilla's rule rejects artifacts,
 * unique items, no-drop, no-trade, locked, and indestructible items (with narrow
 * coin/key/lock exceptions). Listeners can override either direction.
 *
 * <p>Use cases: allow modded ritual items that vanilla would reject, ban dupe-prone
 * items per-server, per-deity acceptance rules, event-specific sacrificial items.</p>
 */
public class SacrificeAcceptanceEvent extends Event {

    private final long itemId;
    private final int templateId;
    private final boolean originalAccepted;
    private boolean modifiedAccepted;

    public SacrificeAcceptanceEvent(long itemId, int templateId, boolean originalAccepted) {
        this.itemId = itemId;
        this.templateId = templateId;
        this.originalAccepted = originalAccepted;
        this.modifiedAccepted = originalAccepted;
    }

    public long getItemId() { return itemId; }
    public int getTemplateId() { return templateId; }
    public boolean getOriginalAccepted() { return originalAccepted; }
    public boolean getModifiedAccepted() { return modifiedAccepted; }
    public void setAccepted(boolean accepted) { this.modifiedAccepted = accepted; }
}
