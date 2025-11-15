package com.garward.wurmmodloader.api.events.skill;

import com.garward.wurmmodloader.api.events.base.Event;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.Skill;

/**
 * Fired at the start of {@code Skill#checkAdvance} before the skill roll is processed.
 * Lets mods adjust the difficulty/power of the tick.
 */
public class SkillAdvanceEvent extends Event {

    private final Skill skill;
    private final Item item;
    private double difficulty;
    private double power;

    public SkillAdvanceEvent(Skill skill, Item item, double difficulty, double power) {
        this.skill = skill;
        this.item = item;
        this.difficulty = difficulty;
        this.power = power;
    }

    public Skill getSkill() {
        return skill;
    }

    public Item getItem() {
        return item;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(double difficulty) {
        this.difficulty = difficulty;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }
}
