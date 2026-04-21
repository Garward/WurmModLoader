package com.garward.wurmmodloader.api.events.spell;

import com.garward.wurmmodloader.api.events.base.Event;

/**
 * Fired immediately after {@code Spell.trimPower(...)} resolves the final spell power,
 * before it's handed to {@code doEffect} / {@code doNegativeEffect}. Listeners may
 * scale the power up or down to build "stronger/weaker" spell mods without per-spell
 * bytecode surgery. This is the central lever nearly every historical spell-balance
 * mod (spellcraft Titanforged, etc.) needs.
 *
 * <p>Power is the skill-check result post-trim: typically in the range [-100, 100] where
 * negative values indicate a failed channel. Scaling into the negative region forces a
 * failure path; pushing it above 100 usually clamps inside {@code doEffect}.</p>
 */
public class SpellPowerEvent extends Event {

    private final int spellId;
    private final String spellName;
    private final long casterId;
    private final String casterName;
    private final double originalPower;
    private double modifiedPower;

    public SpellPowerEvent(int spellId, String spellName,
                           long casterId, String casterName,
                           double originalPower) {
        this.spellId = spellId;
        this.spellName = spellName;
        this.casterId = casterId;
        this.casterName = casterName;
        this.originalPower = originalPower;
        this.modifiedPower = originalPower;
    }

    public int getSpellId() { return spellId; }
    public String getSpellName() { return spellName; }
    public long getCasterId() { return casterId; }
    public String getCasterName() { return casterName; }
    public double getOriginalPower() { return originalPower; }
    public double getModifiedPower() { return modifiedPower; }

    public void setModifiedPower(double power) {
        this.modifiedPower = power;
    }

    /** Multiply the current power by a factor (e.g. 1.5 for +50%). */
    public void multiply(double factor) {
        this.modifiedPower = this.modifiedPower * factor;
    }

    /** Add a flat bonus to the current power (can be negative). */
    public void add(double bonus) {
        this.modifiedPower = this.modifiedPower + bonus;
    }
}
