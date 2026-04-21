package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired on the defender's resist skillCheck for offensive spells — currently the
 * {@code target.getSkills().getSkill(defSkill).skillCheck(...)} call inside
 * {@code Spell.run(Creature performer, Creature target, float counter)}. Listeners
 * see vanilla's rolled resist and may override it.
 *
 * <p>A positive modified result means the defender partially/fully resisted; {@code 0}
 * or negative means no resistance. The subsequent attacker skillCheck uses this value
 * as its difficulty input, so boosting it weakens the incoming spell's power.</p>
 *
 * <p>Use cases: per-creature resist bonuses (undead vs. holy), realm magic resistance,
 * anti-magic enchants, per-school resist (fire-resistant drakes vs. Fire Pillar).</p>
 */
public class SpellResistEvent extends Event {

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final long targetId;
    private final int difficulty;
    private final double originalResist;
    private double modifiedResist;

    public SpellResistEvent(int spellId, String spellName,
                            long casterId, long targetId,
                            int difficulty, double originalResist) {
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.targetId = targetId;
        this.difficulty = difficulty;
        this.originalResist = originalResist;
        this.modifiedResist = originalResist;
    }

    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    public long getCasterId() { return casterId; }
    public long getTargetId() { return targetId; }
    public int getDifficulty() { return difficulty; }
    public double getOriginalResist() { return originalResist; }
    public double getModifiedResist() { return modifiedResist; }
    public void setModifiedResist(double value) { this.modifiedResist = value; }
    public void addResist(double delta) { this.modifiedResist += delta; }
    public void multiplyResist(double factor) { this.modifiedResist *= factor; }
}
