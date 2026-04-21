package com.garward.wurmmodloader.api.events.priest;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired after {@code MethodsReligion.getFavorValue(Deity, Item)} — the base favor
 * an item would grant when sacrificed. Listeners can tune base valuations per item
 * template, rarity, material, or deity.
 *
 * <p>This is the <em>base</em> value; a separate
 * {@link SacrificeFavorModifierEvent} handles per-deity multipliers (wood/metal/cloth
 * affinities etc.). Final granted favor ≈ {@code value × modifier ÷ 1000}.</p>
 */
public class SacrificeFavorValueEvent extends Event {

    private final int deityNumber;   // -1 if deity null
    private final long itemId;
    private final int templateId;
    private final float originalValue;
    private float modifiedValue;

    public SacrificeFavorValueEvent(int deityNumber, long itemId, int templateId, float originalValue) {
        this.deityNumber = deityNumber;
        this.itemId = itemId;
        this.templateId = templateId;
        this.originalValue = originalValue;
        this.modifiedValue = originalValue;
    }

    public int getDeityNumber() { return deityNumber; }
    public long getItemId() { return itemId; }
    public int getTemplateId() { return templateId; }
    public float getOriginalValue() { return originalValue; }
    public float getModifiedValue() { return modifiedValue; }
    public void setModifiedValue(float value) { this.modifiedValue = value; }
    public void multiply(double factor) { this.modifiedValue = (float)(this.modifiedValue * factor); }
    public void add(float delta) { this.modifiedValue += delta; }
}
