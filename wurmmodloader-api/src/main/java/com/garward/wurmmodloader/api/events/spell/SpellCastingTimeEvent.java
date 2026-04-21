package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired from {@code Spell.getCastingTime} after all internal scaling (including the
 * framework's {@code action_timer} vanilla fix) has resolved a final casting time in
 * seconds. Listeners may override the value to tune individual spells without writing
 * bytecode — replaces the need for a per-spell blacklist in action-timer mods.
 *
 * <p>Fires once per call to {@code getCastingTime}, which vanilla invokes multiple times
 * per cast (tick checks, action-control updates, completion check). Listeners must be
 * deterministic; side effects here will fire repeatedly.</p>
 */
public class SpellCastingTimeEvent extends Event {

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final String casterName;
    private final int originalTime;
    private int modifiedTime;

    public SpellCastingTimeEvent(int spellId, String spellName,
                                 long casterId, String casterName,
                                 int originalTime) {
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.casterName = casterName;
        this.originalTime = originalTime;
        this.modifiedTime = originalTime;
    }

    public int getSpellId() {
        return spellId;
    }

    public String getSpellName() {
        return spellName;
    }

    public long getCasterId() {
        return casterId;
    }

    public String getCasterName() {
        return casterName;
    }

    /** Casting time in seconds as computed by vanilla + any upstream framework fixes. */
    public int getOriginalTime() {
        return originalTime;
    }

    public int getModifiedTime() {
        return modifiedTime;
    }

    /** Override the casting time. Clamped to a minimum of 1 second. */
    public void setModifiedTime(int seconds) {
        this.modifiedTime = Math.max(1, seconds);
    }

    /** Multiply the current modified time by a factor (e.g. 2.0 to double). */
    public void multiply(double factor) {
        this.modifiedTime = Math.max(1, (int) (this.modifiedTime * factor));
    }
}
