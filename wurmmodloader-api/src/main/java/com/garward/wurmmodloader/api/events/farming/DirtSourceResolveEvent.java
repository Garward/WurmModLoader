package com.garward.wurmmodloader.api.events.farming;

import com.garward.wurmmodloader.api.events.base.Event;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

/**
 * Fired from inside {@code Flattening.useDirt} and {@code Flattening.checkUseDirt}
 * every time vanilla is about to resolve the dirt/sand item the performer is
 * holding to apply (via {@code performer.getCarriedItem(templateId)}).
 * Listeners may redirect the lookup to a different container — a cart, crate,
 * BSB, dragged vessel — by replacing {@link #getResolvedItem()}.
 *
 * <p>Leaving {@link #getResolvedItem()} as {@code vanillaFound} keeps the
 * vanilla path unchanged.</p>
 */
public class DirtSourceResolveEvent extends Event {

    public enum Context { USE_DIRT, CHECK_USE_DIRT }

    private final Creature performer;
    private final int templateId;
    private final Item vanillaFound;
    private final Context context;

    private Item resolvedItem;

    public DirtSourceResolveEvent(Creature performer, int templateId, Item vanillaFound, Context context) {
        this.performer = performer;
        this.templateId = templateId;
        this.vanillaFound = vanillaFound;
        this.context = context;
        this.resolvedItem = vanillaFound;
    }

    public Creature getPerformer()  { return performer; }
    public int getTemplateId()      { return templateId; }
    public Item getVanillaFound()   { return vanillaFound; }
    public Context getContext()     { return context; }

    public Item getResolvedItem()            { return resolvedItem; }
    public void setResolvedItem(Item item)   { this.resolvedItem = item; }
}
