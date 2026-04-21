package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired immediately before {@code Spell.doEffect(...)} or
 * {@code Spell.doNegativeEffect(...)} runs. Cancellable — a cancelled event skips
 * the vanilla effect entirely, letting mods replace spell behavior without rewriting
 * the whole run method. This is how historical mods (spellcraft Smite, Cure*, Heal,
 * LightOfFo rewrites) can hook via the framework instead of setBody-ing each spell.
 *
 * <p>{@link #isNegative()} distinguishes the success path (false, {@code doEffect})
 * from the fizzle path (true, {@code doNegativeEffect}). Most replacement mods only
 * care about the success path.</p>
 */
public class SpellEffectEvent extends Event {

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final String casterName;
    private final double power;
    private final boolean negative;

    public SpellEffectEvent(int spellId, String spellName,
                            long casterId, String casterName,
                            double power, boolean negative) {
        super(true);
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.casterName = casterName;
        this.power = power;
        this.negative = negative;
    }

    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    public long getCasterId() { return casterId; }
    public String getCasterName() { return casterName; }

    /** Final power handed to the effect (post-trim, post-SpellPowerEvent). */
    public double getPower() { return power; }

    /** True when this is the fail path ({@code doNegativeEffect}). */
    public boolean isNegative() { return negative; }
}
