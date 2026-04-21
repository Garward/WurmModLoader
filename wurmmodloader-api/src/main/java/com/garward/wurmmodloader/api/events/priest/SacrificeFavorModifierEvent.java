package com.garward.wurmmodloader.api.events.priest;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired after {@code MethodsReligion.getFavorModifier(Deity, Item)} — the per-deity
 * multiplier applied to an item's sacrifice value. Vanilla returns e.g. 2.0 for
 * wood-affinity deities sacrificing wood, 3.0 for food-affinity deities with
 * high-nutrition food, 1.0 otherwise.
 *
 * <p>Listeners can add custom affinity rules (e.g., new deity that favors gems,
 * a seasonal event that doubles all modifiers, a per-faith-tier scaling) without
 * patching each case branch individually.</p>
 */
public class SacrificeFavorModifierEvent extends Event {

    private final int deityNumber;   // -1 if deity null
    private final long itemId;
    private final int templateId;
    private final float originalModifier;
    private float modifiedModifier;

    public SacrificeFavorModifierEvent(int deityNumber, long itemId, int templateId, float originalModifier) {
        this.deityNumber = deityNumber;
        this.itemId = itemId;
        this.templateId = templateId;
        this.originalModifier = originalModifier;
        this.modifiedModifier = originalModifier;
    }

    public int getDeityNumber() { return deityNumber; }
    public long getItemId() { return itemId; }
    public int getTemplateId() { return templateId; }
    public float getOriginalModifier() { return originalModifier; }
    public float getModifiedModifier() { return modifiedModifier; }
    public void setModifiedModifier(float modifier) { this.modifiedModifier = modifier; }
    public void multiply(double factor) { this.modifiedModifier = (float)(this.modifiedModifier * factor); }
}
