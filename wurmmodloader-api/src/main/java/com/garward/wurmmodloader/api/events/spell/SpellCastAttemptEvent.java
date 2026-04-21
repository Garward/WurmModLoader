package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired at the top of every {@code Spell.run(...)} overload, before any vanilla
 * validation runs. Cancellable — a cancelled event terminates the cast immediately
 * as if the action completed (so the UI releases and no favor is consumed).
 *
 * <p>Use cases: location restrictions (safe zones, covenants), per-faction spell
 * bans, GM-only spells, tutorial gating.</p>
 *
 * <p>Target information is intentionally generic — the five {@code run} overloads
 * have heterogeneous targets (Item, Creature, Wound, tiles). Listeners that need
 * full target context should inspect the caster's active action rather than this
 * event.</p>
 */
public class SpellCastAttemptEvent extends Event {

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final String casterName;

    public SpellCastAttemptEvent(int spellId, String spellName,
                                 long casterId, String casterName) {
        super(true);
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.casterName = casterName;
    }

    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    public long getCasterId() { return casterId; }
    public String getCasterName() { return casterName; }
}
