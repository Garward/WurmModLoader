package com.garward.wurmmodloader.api.events.skill;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.skills.Skill;

/**
 * Fired inside {@code Skill#doSkillGainNew} after the vanilla bonus and advance
 * multiplier have been computed but before {@code alterSkill} is invoked.
 *
 * <p>Lets mods replace the entire skill-gain multiplier formula (e.g. the
 * Wyvern hybrid skill-gain curve) without per-skill bytecode surgery, and lets
 * them force a tick where vanilla would have skipped it (or skip a tick
 * vanilla would have applied).</p>
 *
 * <p>Defaults reflect vanilla decisions: when nothing subscribes,
 * {@link #getMultiplier()} returns the vanilla advance multiplier and
 * {@link #shouldAdvance()} matches vanilla's gate
 * ({@code power >= 0 || knowledge < 20}).</p>
 */
public class SkillGainMultiplierEvent extends Event {

    private final Skill skill;
    private final double check;
    private final double power;
    private final double learnMod;
    private final float times;
    private final double skillDivider;
    private final double vanillaBonus;
    private final double vanillaMultiplier;
    private final boolean vanillaWouldAdvance;

    private double multiplier;
    private boolean shouldAdvance;

    public SkillGainMultiplierEvent(Skill skill,
                                    double check,
                                    double power,
                                    double learnMod,
                                    float times,
                                    double skillDivider,
                                    double vanillaBonus,
                                    double vanillaMultiplier,
                                    boolean vanillaWouldAdvance) {
        this.skill = skill;
        this.check = check;
        this.power = power;
        this.learnMod = learnMod;
        this.times = times;
        this.skillDivider = skillDivider;
        this.vanillaBonus = vanillaBonus;
        this.vanillaMultiplier = vanillaMultiplier;
        this.vanillaWouldAdvance = vanillaWouldAdvance;
        this.multiplier = vanillaMultiplier;
        this.shouldAdvance = vanillaWouldAdvance;
    }

    public Skill getSkill() {
        return skill;
    }

    public double getCheck() {
        return check;
    }

    public double getPower() {
        return power;
    }

    public double getLearnMod() {
        return learnMod;
    }

    public float getTimes() {
        return times;
    }

    public double getSkillDivider() {
        return skillDivider;
    }

    /**
     * The vanilla "closeness to check" bonus (1.0 to 1.1) folded into the
     * vanilla multiplier. Exposed so handlers can rebuild a custom formula
     * from the same baseline.
     */
    public double getVanillaBonus() {
        return vanillaBonus;
    }

    /**
     * The vanilla advance multiplier as it would have been passed to
     * {@code alterSkill}. Equals {@code (100 - knowledge) / (difficulty *
     * knowledge^2) * learnMod * vanillaBonus}.
     */
    public double getVanillaMultiplier() {
        return vanillaMultiplier;
    }

    /**
     * Whether vanilla logic would have called {@code alterSkill} for this
     * tick. Vanilla only advances when {@code power >= 0 || knowledge < 20}.
     */
    public boolean wouldVanillaAdvance() {
        return vanillaWouldAdvance;
    }

    /**
     * The multiplier that will be passed to {@code alterSkill}. Starts equal
     * to {@link #getVanillaMultiplier()}; mutate via {@link #setMultiplier}.
     */
    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void multiplyMultiplier(double factor) {
        this.multiplier *= factor;
    }

    /**
     * Whether {@code alterSkill} will be invoked. Starts equal to
     * {@link #wouldVanillaAdvance()}; flip via {@link #setShouldAdvance}.
     */
    public boolean shouldAdvance() {
        return shouldAdvance;
    }

    public void setShouldAdvance(boolean shouldAdvance) {
        this.shouldAdvance = shouldAdvance;
    }
}
