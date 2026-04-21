package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired from {@code Spell.getDifficulty(boolean)} on every caster difficulty lookup,
 * before the result feeds into {@code Skill.skillCheck}. Listeners may tune per-spell
 * difficulty to reflect gear, faction, deity alignment, or meditation path.
 *
 * <p>Only fires for the caster's own difficulty. Defender resist rolls use a separate
 * skill chain and do not route through {@code getDifficulty} — a future
 * {@code SpellResistEvent} will cover that surface.</p>
 *
 * <p>Fires multiple times per cast (vanilla calls {@code getDifficulty} for the
 * mind-speed check and the main cast check). Listeners must be deterministic.</p>
 */
public class SpellDifficultyEvent extends Event {

    private final int spellId;
    private final String spellName;
    private final int originalDifficulty;
    private final boolean forItem;
    private int modifiedDifficulty;

    public SpellDifficultyEvent(int spellId, String spellName,
                                int originalDifficulty, boolean forItem) {
        this.spellId = spellId;
        this.spellName = spellName;
        this.originalDifficulty = originalDifficulty;
        this.forItem = forItem;
        this.modifiedDifficulty = originalDifficulty;
    }

    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    public int getOriginalDifficulty() { return originalDifficulty; }

    /** Matches the {@code forItem} argument vanilla passes — doubles difficulty for creature-item enchants. */
    public boolean isForItem() { return forItem; }

    public int getModifiedDifficulty() { return modifiedDifficulty; }

    public void setModifiedDifficulty(int difficulty) {
        this.modifiedDifficulty = Math.max(1, difficulty);
    }

    public void multiply(double factor) {
        this.modifiedDifficulty = Math.max(1, (int) (this.modifiedDifficulty * factor));
    }

    public void add(int delta) {
        this.modifiedDifficulty = Math.max(1, this.modifiedDifficulty + delta);
    }
}
