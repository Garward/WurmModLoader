package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired when a spell is registered to (or removed from) a deity via
 * {@code Deity.addSpell(Spell)} / {@code Deity.removeSpell(Spell)}. Non-cancellable —
 * vanilla registration has already happened by the time this event fires.
 *
 * <p>Use cases: audit/log deity spell rosters, discover third-party registrations,
 * build compatibility matrices, populate custom UI with per-deity spell lists.</p>
 */
public class DeitySpellRegistrationEvent extends Event {

    private final int deityNumber;
    private final String deityName;
    private final int spellId;
    private final String spellName;
    private final boolean added;

    public DeitySpellRegistrationEvent(int deityNumber, String deityName,
                                       int spellId, String spellName, boolean added) {
        this.deityNumber = deityNumber;
        this.deityName = deityName;
        this.spellId = spellId;
        this.spellName = spellName;
        this.added = added;
    }

    public int getDeityNumber() { return deityNumber; }
    public String getDeityName() { return deityName; }
    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    /** {@code true} for addSpell, {@code false} for removeSpell. */
    public boolean isAdded() { return added; }
}
