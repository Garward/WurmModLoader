package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired after vanilla's {@code Spell.precondition(...)} check inside each
 * {@code Spell.run(...)} overload. Lets mods either force-allow a cast that vanilla
 * rejected, or force-deny a cast that vanilla accepted, without subclassing each
 * spell.
 *
 * <p>Target identification is best-effort: when the precondition variant takes a
 * concrete entity (Creature/Item/Wound) we pass its {@code getWurmId()}; tile
 * variants use {@code -1L} since tile coordinates don't map to a single ID.
 * {@link #getTargetType()} distinguishes the variants for listeners that care.</p>
 */
public class SpellPreconditionEvent extends Event {

    public enum TargetType { CREATURE, ITEM, WOUND, TILE, TILE_BORDER }

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final String casterName;
    private final long targetId;
    private final TargetType targetType;
    private final boolean originalAllowed;
    private boolean modifiedAllowed;

    public SpellPreconditionEvent(int spellId, String spellName,
                                  long casterId, String casterName,
                                  long targetId, TargetType targetType,
                                  boolean originalAllowed) {
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.casterName = casterName;
        this.targetId = targetId;
        this.targetType = targetType;
        this.originalAllowed = originalAllowed;
        this.modifiedAllowed = originalAllowed;
    }

    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    public long getCasterId() { return casterId; }
    public String getCasterName() { return casterName; }
    public long getTargetId() { return targetId; }
    public TargetType getTargetType() { return targetType; }
    public boolean getOriginalAllowed() { return originalAllowed; }
    public boolean getModifiedAllowed() { return modifiedAllowed; }
    public void setAllowed(boolean allowed) { this.modifiedAllowed = allowed; }
}
