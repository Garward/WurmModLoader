package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired once per spell when vanilla builds a right-click spell menu on a target.
 * Cancellable — cancelling drops the spell from the returned menu list.
 *
 * <p>Use cases: PvE-only spells gated in PvP zones, GM-only utility spells, tutorial
 * gating, faction/realm restrictions, per-deed permission systems.</p>
 *
 * <p>Target type is one of {@link Target#CREATURE}, {@link Target#ITEM},
 * {@link Target#WOUND}, {@link Target#TILE}. For WOUND, the target ID is the
 * wounded creature's wurmId (Wounds aren't standalone entities). For TILE, the
 * target ID is {@code -1} — tile coordinates don't map to a single ID.</p>
 */
public class SpellVisibilityEvent extends Event {

    public enum Target { CREATURE, ITEM, WOUND, TILE }

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final long targetId;
    private final Target targetType;

    public SpellVisibilityEvent(int spellId, String spellName,
                                long casterId, long targetId, Target targetType) {
        super(true);
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.targetId = targetId;
        this.targetType = targetType;
    }

    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    public long getCasterId() { return casterId; }
    public long getTargetId() { return targetId; }
    public Target getTargetType() { return targetType; }
}
